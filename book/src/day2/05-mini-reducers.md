# Building a mini reducers library

<div class="callout callout-book">
<p class="callout-title">In the book</p>

Day 2, *Reducers' Internals* and *CollReduce*, p65–66.

</div>

## What the book says

To explain reducers, the book builds a simplified version of `clojure.core.reducers`.
It introduces protocols in a paragraph, shows the `CollReduce` protocol, writes `my-reduce` in terms of `coll-reduce`, and then `make-reducer`, which `reify`s `CollReduce` so that a reducing function can be transformed on its way through.

The compression is severe.
Between the protocol appearing on one page and `reify` appearing on the next, four questions go unanswered — and this chapter answers them before using any of it in anger.

## The four words this chapter turns on

The book's code uses these as if they were already familiar.
They are worth pinning down before any of it appears, because three of the four are ordinary functions wearing nouns.

| Term | What it is |
|---|---|
| **reducing function** | `(acc, v) -> acc`. The function you hand to `reduce`. Often also has a zero-arity returning a seed — `(+)` is `0`. |
| **reducible** | Something that knows how to reduce itself: it implements `CollReduce`. Every Clojure collection is one, and so is a reducer. |
| **transform function** (`transformf`) | `rf -> rf`. Takes a reducing function, returns a reducing function. One argument in; something callable with two arguments out. |
| **reducer** | The object `make-reducer` returns: a reducible plus a transform function, implementing `CollReduce`. Not a collection — a recipe. |

Two of those need a footnote.

**"Implements `CollReduce`" is looser than it sounds.** The protocol is extended to `Object`, so strictly speaking everything is a reducible and `(satisfies? CollReduce 5)` is `true`; `5` simply fails later, inside `seq`.
Read "reducible" as a statement of intent rather than a type check.

**A reducer is itself reducible.** That is the whole point of the design: `(my-map inc (my-filter odd? coll))` works because the object `my-filter` returns is something `my-map` can wrap in turn.
Nothing is computed until the outermost one is handed a reducing function.

## Protocols, before we use one

A **protocol** is a named set of function signatures with no implementations:

```clojure
{{#include ../../../src/tenfold/mini_reducers.clj:own-protocol}}
```

Three things worth noticing, because each answers a question the book leaves hanging.

**Yes, you can define your own** — `defprotocol` is all it takes, and it is ordinary Clojure, not a privileged construct.
`CollReduce` is defined exactly this way, in `clojure.core.protocols`, in eight lines you can read.

**Implementations are attached to types afterwards, and from anywhere.** `extend-protocol` above teaches `String` and `IPersistentVector` to greet — two types nobody in this namespace owns, extended without touching their source and without wrapping them.
This is the answer to "why a protocol rather than a Java interface": an interface must be declared when the class is written, and `String` was written a long time ago.

**Dispatch is on the type of the first argument, and only the first.** That is why the first parameter is conventionally called `this`, and why `(coll-reduce coll f)` puts the collection first even though `reduce` puts it last.

<div class="callout callout-gap">
<p class="callout-title">The book doesn't say</p>

`coll-reduce` is a perfectly ordinary function.
`(fn? p/coll-reduce)` is `true`, and its var carries the protocol it belongs to in its metadata:

```clojure
(meta #'clojure.core.protocols/coll-reduce)
;=> {:protocol #'clojure.core.protocols/CollReduce
;    :arglists ([coll f] [coll f val]) ...}
```

**It is not referred into your namespace for you.** The book's `my-reduce` calls `coll-reduce` with nothing to say where it came from; `clojure.core.protocols` is not one of the namespaces `clojure.core` refers by default, so your own file needs

```clojure
(:require [clojure.core.protocols :as p])
```

and then `p/coll-reduce`.
That single missing line is the most likely reason the book's code does not run when you type it in.

</div>

### What can you pass to it?

Anything at all — and this is the sharp edge.
`CollReduce` is extended to `Object`, so every value in the language "satisfies" it:

```clojure
(satisfies? clojure.core.protocols/CollReduce 5)   ;=> true
```

`5` is not a collection, and `satisfies?` says yes anyway.
Reducing over a few things that are not obviously collections works fine, because the `Object` implementation seqs them:

| Call | Result |
|---|---|
| `(p/coll-reduce [1 2 3] + 0)` | `6` |
| `(p/coll-reduce nil + 0)` | `0` — `nil` has its own implementation |
| `(p/coll-reduce "abc" str "")` | `"abc"` — a string seqs into characters |
| `(p/coll-reduce {:a 1 :b 2} (fn [acc [k v]] (+ acc v)) 0)` | `3` |
| `(p/coll-reduce 5 + 0)` | throws |

and the throw, when it comes, does not mention protocols at all:

```text
IllegalArgumentException: Don't know how to create ISeq from: java.lang.Long
```

Compare that with calling a protocol function on a type with *no* implementation and no `Object` fallback — our `Greet` above:

```text
IllegalArgumentException: No implementation of method: :greeting
  of protocol: #'tenfold.mini-reducers/Greet found for class: java.lang.Long
```

The second message tells you what is wrong.
The first is what you actually get from `coll-reduce`, because the `Object` fallback accepted the call and failed later, inside `seq`.
Remember this in day 2's other guise: the same `Object`-fallback design is why folding a lazy sequence silently runs serially instead of refusing.

## `reify`, on something small first

`reify` makes a single anonymous object that implements a protocol, in place:

```clojure
{{#include ../../../src/tenfold/mini_reducers.clj:reify-example}}
```

That is the whole idea: `new` for an anonymous implementation, as the book says — but worth seeing on a protocol with one trivial method before meeting it wrapped around a transformed reducing function.

It is worth being precise about what "anonymous" means here, because `polite` plainly has a name.
The **class** is the anonymous part.
`reify` generates one, gives it a machine-made name you did not choose and cannot usefully refer to, and hands you a single instance of it:

```clojure
(class polite)   ;=> tenfold.mini_reducers$reify__182
```

Every `reify` form generates its own class, so two of them are two different types even when the bodies are identical.
`polite` is then an ordinary var naming one ordinary object — the anonymity is one level down, in the type rather than the value.

And yes, it is an `Object`: everything on the JVM is.

```clojure
(instance? Object polite)          ;=> true
(map #(.getName %) (supers (class polite)))
;=> ("clojure.lang.IObj" "tenfold.mini_reducers.Greet"
;    "clojure.lang.IMeta" "java.lang.Object")
```

That list is the whole truth about it: it carries metadata, it implements the interface the `Greet` protocol generated behind the scenes, and it is an `Object`.
Note what is *absent* — `polite` is not a map, not a record, and not a collection.
It implements exactly what you listed and nothing else.

Its being an `Object` is not an idle detail, either.
It is why a reified object gets picked up by an `Object` implementation of some *other* protocol — the fallback discussed above — rather than being rejected as an unknown type.
It also means `Object`'s own methods can be implemented in the same `reify`, alongside the protocol:

```clojure
{{#include ../../../src/tenfold/mini_reducers.clj:this-example}}
```

This is the case where the first parameter earns its keep.
`greeting` answers from a literal and ignores it, as before.
`toString` cannot: it has nothing of its own to say, so it asks the object for its greeting — and `this` is the only way one method can reach another on the same object.
Each method sees its own arguments and whatever the enclosing scope closed over, and nothing else.

Keep that contrast in view through the rest of the chapter.
The objects we build from here are **decorators**: they wrap something, they answer out of what they closed over, and they have no questions to ask themselves.
That is why their methods are full of `_`.

## Functions that return objects

`polite` is a `def`: one object, made once, closing over nothing.
`make-reducer` is a step beyond that — a *function* that builds and returns a reified object each time it is called — so it is worth taking that step on `Greet` first.

```clojure
{{#include ../../../src/tenfold/mini_reducers.clj:make-greeter}}
```

`make-greeter` closes over its argument: the object it returns remembers `n` because the `reify` body refers to it, exactly as any other closure would.
Call it twice and you get two objects that behave differently.
(They are still the same class — the class belongs to the `reify` *form*, and there is only one of those here.
Two different `reify` forms would give two classes.)

`decorate` is the shape that matters.
It takes an existing `Greet` and a function, and returns a new `Greet` that delegates to the first one and transforms what comes back:

```clojure
(greeting (decorate polite clojure.string/upper-case))   ;=> "GOOD EVENING"
```

The new object holds two things it was given, implements one method, and does its real work by calling the object it wraps.
That is `make-reducer`, entire — bar one twist.

## Now the book's version

The twist is *what* gets transformed.
`decorate` transforms the **result** coming back from the object it wraps.
`make-reducer` transforms the **function going in** — it takes the reducing function it is handed, passes it through `transformf`, and gives the *transformed* function to the thing it wraps.
That inversion is the entire idea of reducers, and it is why the transformation costs nothing: it happens once, on a function, rather than once per element on a result.

With that in hand, `my-reduce` is what it appeared to be — two arities forwarding to the protocol function, and doing one small job on the way: putting the arguments in the other order.
`reduce` takes its collection last, as Clojure's sequence functions conventionally do; a protocol must take the value it dispatches on first.
`my-reduce` is the shim between those two conventions, which is why both of its arities are a single line.

```clojure
{{#include ../../../src/tenfold/mini_reducers.clj:my-reduce}}
```

and `make-reducer` is `reify` doing the same job as `polite`, on a protocol that happens to matter:

```clojure
{{#include ../../../src/tenfold/mini_reducers.clj:make-reducer}}
```

<div class="callout callout-gap">
<p class="callout-title">The book doesn't say</p>

`coll-reduce` appears twice on that line, spelled two ways, and they are not the same thing.

In the **method position** — `(coll-reduce [_ f] ...)` — the name is a declaration.
It says which protocol method this body implements, and it is matched by name against the protocol's methods rather than resolved as a var.
That is why it needs no `p/` prefix even though this namespace never referred it: as a bare expression, `coll-reduce` does not resolve here at all.
Misspell it and the error says so plainly — `Can't define method not in interfaces: coll_reduc`.

In the **body** — `(p/coll-reduce reducible ...)` — it is an ordinary function call to the var in `clojure.core.protocols`, dispatching on whatever `reducible` turns out to be.

The same distinction holds for `greeting` in the `polite` example above, where it is easier to see because there is no recursion to distract from it.

The `_` is the method's first parameter, which is the object itself — Java's `this`.
It has to be declared, because the arity must match the protocol, but our implementations never use it: they answer out of the values they closed over rather than out of themselves.
`_` is not special syntax, just the conventional name for a parameter you intend to ignore; write `this` instead and it works identically.

The thing worth holding on to is *what* it refers to: `this` is the wrapper — the reified object itself — and the wrapper is not where the interesting state lives.
What we actually want is `reducible`, the object being wrapped, and that arrives from the enclosing function's parameters, through the closure.
So the parameter naming the object is the one we throw away.

`this` earns its name when a method needs the object it is a method of.
The `PageCorpus` type later in this chapter has exactly one such line, where the two-argument arity supplies a seed and hands the work to its own three-argument arity:

```clojure
(coll-reduce [this f] (p/coll-reduce this f (f)))
```

Clojure's own reducers library does the same thing in the same place, so it is idiomatic rather than a trick.

</div>

The object it returns is not a collection.
It holds a reducible and a transformation, and the only thing it knows how to do is be reduced — applying the transformation to whatever reducing function it is handed.
`(my-map inc [1 2 3 4])` has not mapped anything; it is the *recipe*, and nothing runs until `my-reduce` supplies a reducing function.

<div class="callout callout-repl">
<p class="callout-title">Try it in the REPL</p>

```clojure
(my-reduce + [1 2 3 4])                                   ;=> 10
(my-reduce + 10 [1 2 3 4])                                ;=> 20
(my-reduce + 0 (my-map inc [1 2 3 4]))                    ;=> 14
(my-reduce + 0 (my-filter even? (my-map inc [1 2 3 4])))  ;=> 6
```

The last one is the point of the exercise: two transformations composed, one pass over the vector, and no intermediate sequence between them.

</div>

<div class="callout callout-exercise">
<p class="callout-title">Exercise: what have you actually got?</p>

`(my-map inc [1 2 3 4])` returns the reified object.
Predict what each of these does to it, then try them:

```clojure
(reduce + 0 (my-map inc [1 2 3 4]))
(into [] (my-map inc [1 2 3 4]))
(count (my-map inc [1 2 3 4]))
(first (my-map inc [1 2 3 4]))
```

<details>
<summary>Show answer</summary>

```clojure
14                                 ; reduce works
[2 3 4 5]                          ; into works — it is implemented with reduce
UnsupportedOperationException       ; count does not
IllegalArgumentException            ; nor first, nor seq
```

Clojure's own `reduce` falls back to the `CollReduce` protocol for types that do not implement the `IReduceInit` interface, so our reified object is reducible by the standard library, with no registration and no inheritance — this is the extensibility the protocol buys.

Everything else fails, and should: we implemented one protocol with one function.
A reducer is not a collection.
It cannot tell you its length, or give you its first element, because it has not computed anything — it is a recipe waiting for a reducing function, and `count` would mean running it.

</details>

</div>

## Making a collection of your own foldable

The book builds a reducers library to show how reducers work.
The other way to understand the same machinery is to sit on the other side of it: write a collection that `r/fold` can parallelise, and see what it demands of you.

Two protocols do all the work, and each has exactly one function:

```clojure
(defprotocol CollReduce                         ; clojure.core.protocols
  (coll-reduce [coll f] [coll f val]))

(defprotocol CollFold                           ; clojure.core.reducers
  (coll-fold [coll n combinef reducef]))
```

`reduce` goes through the first, `r/fold` through the second.

<div class="callout callout-gap">
<p class="callout-title">The book doesn't say</p>

Neither protocol has to be implemented for `reduce` or `fold` to *work* on your type.
Both have a baseline extended to `Object`: `CollReduce`'s walks the sequence, and `CollFold`'s is three lines whose own comment in Clojure's source reads `;;can't fold, single reduce`.

So the question is never "does my collection support folding" — everything does.
The question is whether it supports folding *in parallel*, and the answer arrives as a running time rather than as an error.

</div>

### A foldable corpus

Our word count reads 5,000 Wikipedia articles from a directory.
That is a collection: 5,000 elements, each a file to be read.
Here it is as a type:

```clojure
{{#include ../../../src/tenfold/corpus.clj:corpus-type}}
```

`coll-reduce` is the easy half — delegate to `reduce` over a lazy `map slurp`.

`coll-fold` is the interesting half, and it is the binary chop from the previous chapter written out by hand: below the chunk size, read this chunk's files and reduce them on this thread; above it, split the file vector in two, fork one half, recurse into the other, and combine the two results.
It is the same shape as `foldvec` in Clojure's own source, over files instead of tree nodes.

Writing it settles the `combinef` question from the other direction.
You seed each chunk yourself, with `(combinef)` and no arguments — so the zero-arity is not a quirk of the API, it is the only way a chunk that has not started yet can have a value.

<div class="callout callout-gap">
<p class="callout-title">The book doesn't say</p>

The extension point is public; the plumbing to implement it is not.
`clojure.core.reducers` marks `fjinvoke`, `fjfork` and `fjjoin` private — only `fjtask` is public — so a type that wants to fork its own tasks re-implements those three lines over `ForkJoinPool` and `ForkJoinTask`:

```clojure
{{#include ../../../src/tenfold/corpus.clj:fj}}
```

Not difficult, but a strange omission in a library whose whole purpose is extensibility.

</div>

### You may not need the type at all

There is a cheaper trick, and it is worth reaching for first.
A **vector of filenames** is already foldable, because it is a vector.
Fold that, and do the reading inside `reducef`:

```clojure
{{#include ../../../src/tenfold/corpus.clj:vector-of-files}}
```

No protocols, no fork/join, no `deftype` — twelve lines, and the pages are still read one at a time rather than all held in memory.
The collection being folded is a vector of `File` objects, a few hundred kilobytes; the 250MB of text never exists all at once.

### Four ways to count the same words

Same 5,000 articles, same answer every time — 41,046,437 words, 984,873 distinct:

| Approach | Time | Parallel? |
|---|---|---|
| sequential `reduce` over a lazy sequence | 22.8s | no |
| `r/fold` over the same lazy sequence | 22.6s | **no**, silently |
| `r/fold` over a vector of filenames | **5.0s** | yes |
| `r/fold` over our `PageCorpus` type | **5.0s** | yes |

Medians of three runs on an Apple M1 Pro (10 cores), Temurin JDK 17, Clojure 1.12.1, over a corpus built by `make corpus`.
A 4.5× speedup on ten cores, which is the honest shape of these things — [Effortless parallelism?](../day1/07-effortless-parallelism.md) is about why it is not ten.

Three things fall out of that table.

**Row two is the whole problem.** Identical code to row three, identical result, and no parallelism whatsoever — the `Object` fallback ran a plain `reduce` and said nothing.

**Rows three and four are the same speed.** The difference between them is within run-to-run noise, and the plain vector of filenames is a tenth of the code.
That is not a disappointment — it is the useful result.
`PersistentVector`'s own `coll-fold` does the same binary chop ours does, so there is nothing to win by writing it again.

**So when is the type worth writing?** When you cannot produce the vector.
Files in a directory enumerate cheaply, so row three works.
Pages inside a 24GB XML dump do not: you do not know where page 3,000,000 starts until you have parsed everything before it, so there is no vector to hand `r/fold` — and that is precisely the case day 2 leaves the reader stuck with.

<div class="callout callout-exercise">
<p class="callout-title">Exercise</p>

Our `coll-fold` ignores `n` when deciding *how* to split — it always halves.
Clojure's `foldvec` does the same.
Why is halving the right answer, rather than cutting the collection into exactly `(/ count n)` equal chunks up front?

<details>
<summary>Show answer</summary>

Because halving builds a *tree* of tasks, and the tree is what fork/join needs.

Cutting into N chunks up front gives one flat layer of tasks and one thread to combine all N results.
Halving recursively means each combine happens on the thread that forked the two halves, so the combining is parallel too — it mirrors the shape of the split.

It also means work-stealing has something to steal.
A forked half is a task any idle worker can pick up; a flat partition assigns everything before any timing information exists.
If one chunk turns out to be slow — one enormous article — the tree can still rebalance around it.

</details>

</div>

{{#quiz ../quizzes/mini-reducers.toml}}
