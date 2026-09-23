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

Look closely at where `transformf` sits in `decorate`, because this is the thing that changes:

```clojure
(transformf (greeting greeter))     ; delegate first, transform the answer
```

The call to `greeting` happens, an answer comes back, and *then* it is transformed.
`make-reducer` puts `transformf` on the other side of the delegation — and with it, all of the work.

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
The `PageCorpus` type in [Folding a collection of your own](08-foldable-collection.md) has exactly one such line, where the two-argument arity supplies a seed and hands the work to its own three-argument arity:

```clojure
(coll-reduce [this f] (p/coll-reduce this f (f)))
```

Clojure's own reducers library does the same thing in the same place, so it is idiomatic rather than a trick.

</div>

The object it returns is not a collection.
It holds a reducible and a transformation, and the only thing it knows how to do is be reduced — applying the transformation to whatever reducing function it is handed.
`(my-map inc [1 2 3 4])` has not mapped anything; it is the *recipe*, and nothing runs until `my-reduce` supplies a reducing function.

### What actually triggers the work

Put the two side by side:

```clojure
(transformf (greeting greeter))                      ; decorate:     work, then transform
(p/coll-reduce reducible (transformf f) init)        ; make-reducer: transform, then work
```

In `decorate` the delegated call is *inside* `transformf`'s argument, so it has already run.
In `make-reducer` `transformf` is applied to `f` first, and the transformed function is handed *down* to the reducible — which has not been asked to do anything yet.

So when does anything happen?
Here is `my-map` again with print statements in both layers:

```clojure
{{#include ../../../src/tenfold/mini_reducers.clj:traced-map}}
```

Building the thing prints **nothing**:

```clojure
(def r (traced-map "outer" inc (traced-map "inner" inc [1 2])))
;; (silence)
```

Two reducers now exist, each holding a reducible and a transform function, and not one element has been touched.
Reducing is what sets it going:

```text
(my-reduce + 0 r)

  outer transformf runs — building a reducing function
  inner transformf runs — building a reducing function
    inner step sees 1
    outer step sees 2
    inner step sees 2
    outer step sees 3
;=> 7
```

Three things to read off that trace.

**The trigger is the outermost `coll-reduce` call, and nothing else.** `my-reduce` asks the outer reducer to reduce itself; that is the first moment any of this machinery runs.

**The transform functions run once each, outside-in, before any element is seen.** Outer transforms `+` and passes the result down; inner transforms *that* and passes it down again; the vector finally receives a single reducing function that carries both transformations.
Two transform calls in total — not two per element.

**The steps then run inside-out, once per element, in one pass.** `inner step sees 1` comes before `outer step sees 2`, because inner's transformation was applied last and therefore wraps outermost at call time.
Note also that the outer step sees `2`: the value has already been incremented by the inner layer on its way through.
There is no intermediate collection between them — the composition happened to the *function*, once, before the walk began.

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

{{#quiz ../quizzes/mini-reducers.toml}}
