# Why mutable state is the problem

<div class="callout callout-book">
<p class="callout-title">In the book</p>

Day 1, *The Perils of Mutable State* through *Escapologist Mutable State*, p50–51.

</div>

## If you opened the book here

The Clojure chapter follows Chapter 2, *Threads and Locks*, and assumes its conclusion.
That conclusion, compressed: locks are correct only by discipline.
Nothing in the language tells you which lock guards which piece of data, locks do not compose — two individually correct locked operations are not a correct combined one — and deadlock is a property of the whole program rather than of any function you can review in isolation.
Functional programming is offered as a way out: if nothing is shared *and* mutable, there is nothing to guard.

## What goes wrong, concretely

Four threads, 100,000 increments each, all writing to one shared mutable slot.
The answer should be 400,000.

```clojure
{{#include ../../../src/tenfold/mutable_state.clj:race}}
```

Nothing there announces itself as dangerous, which is the point.
`long-array` reads like ordinary Clojure, but it is interop wearing a disguise: it allocates a Java array — the `new long[1]` you would write in Java — and a Java array is plain mutable memory with no protection of any kind.
`aset` then writes into it in place.
That is destructive assignment, and it is the one thing ordinary Clojure code cannot do: `conj` and `assoc` return a new value and leave the original alone.
The race needed both, and the syntax made neither obvious.

Five consecutive runs gave:

```text
157882  251585  282216  287816  147884
```

Not one of them is 400,000, no two agree, and every one of them is a plausible-looking number that a test asserting `(pos? result)` would happily accept.
Between a third and two thirds of the writes simply vanished.

<div class="callout callout-repl">
<p class="callout-title">Try it in the REPL</p>

Measured on an Apple M1 Pro (10 cores), Temurin JDK 17, Clojure 1.12.1, calling `(count-with-array)` five times in a fresh REPL.
Your numbers will differ — that is the point of the example — but the shape will not.
A machine with fewer cores loses fewer updates, and a single-core machine may lose none at all, which is precisely why this class of bug survives testing and appears in production.

</div>

The same work through an atom gives 400,000 every time:

```clojure
{{#include ../../../src/tenfold/mutable_state.clj:atom-version}}
```

## Two problems wearing one coat

The usual explanation is that `(inc (aget slot 0))` is not **atomic**: it is a read, then an add, then a write, and another thread can slip between them, so two threads read 41, both write 42, and one increment is lost.

That is true, and it is half the story.

<div class="callout callout-gap">
<p class="callout-title">The book doesn't say</p>

There are **two** independent problems here, and fixing one does not fix the other.

**Atomicity** is about interleaving: whether a read-modify-write happens as one indivisible step, or whether another thread can act in the middle of it.

**Visibility** is about whether a write by one thread is ever seen by another *at all*.
A thread may keep a value in a CPU register or a core-local cache, and the compiler and processor may reorder operations, so long as the result looks correct *to that thread*.
Nothing obliges a write to become visible elsewhere on any particular timescale — or, absent synchronisation, ever.

This is why the fix is not "make the increment atomic" but "establish a happens-before relationship", which is what `synchronized`, `volatile`, and Clojure's own reference types all do.
A visibility bug is much nastier than a lost update: the loop that never notices its stop flag does not produce a wrong number, it hangs, and it hangs only on the machine you do not own.

</div>

## Escape hatches, and what they are called

Clojure does not ban mutation.
It makes immutability the default and provides a small number of deliberate, labelled exits — which is the part worth knowing early, because you will meet all three in the wild long before the book explains them.

**An atom** is a mutable reference to an immutable value: a box you can swap the contents of.
`(atom 0)` makes one, `@a` reads it, and `(swap! a inc)` applies a pure function to the current value and stores the result.
The swap is a compare-and-set retry loop — if another thread got there first, your function is simply called again — so it is atomic, and it establishes visibility too.
The value *inside* the box is still an ordinary immutable Clojure value.
If you know Rust, the shape is `Arc<Mutex<T>>`, minus the lock and minus the risk of forgetting it.

**Interop** is calling Java from Clojure, which is ordinary and everywhere: `(StringBuilder. "ab")` constructs an object, `(.append sb "c")` calls a method on it, `(System/getProperty "user.dir")` calls a static one.
It matters here because Java objects are mostly *mutable*, so interop is the boundary at which Clojure's guarantees stop:

```clojure
{{#include ../../../src/tenfold/mutable_state.clj:interop}}
```

`conj` gave you a new vector and left the old one alone.
`.append` changed the object in place, and anyone else holding a reference to that `StringBuilder` now sees different contents than they did a moment ago.
The `long-array` in the race above is the same thing: a Java array, mutable, with no protection of any kind.

**A transient** is a mutable draft of a persistent collection.
Building a 100,000-element vector by `conj`-ing one at a time allocates a new vector each step; a transient lets you do the building in place and then hand back a normal immutable collection at the end:

```clojure
{{#include ../../../src/tenfold/mutable_state.clj:transient}}
```

`(transient [])` starts the draft, `conj!` adds to it, `persistent!` ends it.
This is not an obscure corner — it is how `into` is implemented in Clojure's own core (`(persistent! (reduce conj! (transient to) from))`), which is why `into` is so much faster than the `reduce conj` you would write by hand.

The rule is that a transient is a *local* optimisation: confined to the function that created it, never shared between threads, never used after `persistent!`, and always used through its return value rather than for its effect.

## The hard case is the state you did not know you had

The shared counter is the easy case: it is visible in review, and everyone knows to look for it.
What actually bites is mutable state you are not aware of holding.

- A collection handed out from inside a function — you return the thing itself, the caller keeps it, and now two pieces of code own one object.
- A memoisation cache added for speed, which quietly turns a pure function into a shared mutable map.
- Any Java object reached through interop: `StringBuilder`, `ArrayList`, arrays, `SimpleDateFormat` (famously not thread-safe), a database connection.
- A lazy sequence that closes over one of the above, so the mutation happens whenever the sequence is finally realised, which may be in a different thread entirely.

The common thread is that none of these *look* like shared state at the call site.
They look like a function returning a value.

<div class="callout callout-gap">
<p class="callout-title">The book doesn't say</p>

Why this chapter exists in a book about *parallelism*, as opposed to a book about correctness.

Everything from here to the end of day 2 is one move: cut a collection into chunks, process the chunks independently on different cores, combine the results.
That move is only available if the chunks cannot observe each other.
One shared mutable thing anywhere in the processing — a counter, a cache, an `ArrayList` being appended to — and the chunks must coordinate; coordination means locks; and locks are the thing you were trying to escape by parallelising in the first place.

So immutability here is not primarily a safety feature.
It is the precondition that makes the fork/join structure of day 2 legal at all.
Hold on to that when the book starts talking about `reduce`: the reason it can be parallelised and a `for` loop with an accumulator cannot is entirely in this chapter.

</div>

<div class="callout callout-rust">
<p class="callout-title">Coming up in Rust</p>

Rust makes the same argument and enforces it at compile time rather than by convention.
Its rule is aliasing *xor* mutability: you may have many readers of a value or exactly one writer, never both, and the borrow checker rejects the program otherwise.
The `Send` and `Sync` marker traits extend this across threads, so the race at the top of this chapter is not a bug you might find in testing — it is a program that does not compile.

Clojure arrives at the same destination from the other end: rather than proving that sharing is safe, it makes the shared thing immutable so the question does not arise.
Keep both in view; when `rayon` appears later, its `par_iter` is doing exactly what `r/fold` does, with the compiler rather than the programmer checking the precondition.

</div>

## What to take from this

- Losing updates and never seeing updates are different failures with different fixes.
- The dangerous mutable state is the state that does not look shared: interop objects, caches, escaped collections.
- Clojure's exits from immutability — atoms, transients, interop — are deliberate and narrow, and each has a rule attached.
- Immutability is what makes the day-2 parallelism structurally possible, not merely safer.

{{#quiz ../quizzes/mutable-state.toml}}
