# Clojure in twenty minutes, for Rust programmers

<span class="todo">placeholder</span>

<div class="callout callout-book">
<p class="callout-title">In the book</p>

Day 1, *A Whirlwind Tour of Clojure*, p52–53.

</div>

## What the book says

TODO — summarise, briefly, so the reader can orient.

<div class="callout callout-gap">
<p class="callout-title">The book doesn't say</p>

- What `'` (quote) actually means, and why lists need it but vectors don't.
- That `def` is not `let` — it interns a var, it isn't a local binding.
- `#()` reader macro and `%` — used before being introduced.
- Keywords are functions of maps; maps are functions of keys.

</div>

## Collections are functions

Clojure's collections implement `IFn` (Interface Functions), which is to say they are callable.

Start with a map and a key, which can be written either way round:

```clojure
{{#include ../../../src/tenfold/day1.clj:lookup-both-ways}}
```

`(:a m1)` is the familiar one: the keyword is the function, the map is its argument.
`(m1 :a)` is the same lookup with the arguments swapped — the map is the function, the key is its argument.
Neither is a shorthand for the other, and neither is special syntax.
There are simply two callable things here, and you may call whichever you have to hand.

A missing key gives `nil` rather than an error, and both forms take an optional third argument to use as a default.

<div class="callout callout-gap">
<p class="callout-title">The book doesn't say</p>

This is not a special case for maps.
Vectors are functions of their indices, and sets are functions of their members:

```clojure
([:x :y :z] 1)   ;=> :y
(#{:a :b} :a)    ;=> :a
(#{:a :b} :c)    ;=> nil
```

The two are not interchangeable at the edges, though, and the difference matters in a pipeline where a map might be absent:

```clojure
(:a nil)   ;=> nil
(nil :a)   ;=> throws — "Can't call nil, form: (nil :a)"
```

Keyword-first is the safer habit for that reason.
Strings, meanwhile, are indexable but *not* callable: `("abc" 0)` throws, where `(nth "abc" 0)` gives `\a`.

</div>

## Why this is worth knowing

Because being callable means a collection can go anywhere a function can — which is where the idiom actually earns its place:

```clojure
{{#include ../../../src/tenfold/day1.clj:lookup-as-argument}}
```

`(map m1 [:a :b])` reads as "look each of these keys up in `m1`", with the map playing the part `inc` or `str` would play.
`(mapv :a [m1 m1 m1])` does the mirror image: one key, applied across many maps.
That second form is how you pull a column out of a sequence of records, and you will see it constantly.

<div class="callout callout-repl">
<p class="callout-title">Try it in the REPL</p>

The forms above are in `src/tenfold/day1.clj`, near the top.
Evaluate them with <kbd>alt+enter</kbd> and note which give a lazy sequence and which a vector: `map` returns a `LazySeq` whatever you pass it, `mapv` returns a vector.
That distinction is dormant now and becomes the subject of [Counting words, and laziness](08-counting-words.md) — and a trap in day 2, where folding a lazy sequence quietly stops being parallel.

</div>

## A trap: maps and sets as predicates

The same property makes a collection usable as a predicate, since `filter` asks only that its function return something truthy:

```clojure
{{#include ../../../src/tenfold/day1.clj:lookup-as-predicate}}
```

The first is an ordinary predicate.
The second drops everything, because `m1` has no integer keys, so every lookup returns `nil`.

The third is the one to look at twice.
It keeps `2` and `3` — and the value stored under `2` is `0`.

<div class="callout callout-gap">
<p class="callout-title">The book doesn't say</p>

**In Clojure, `0` is truthy.** Only `false` and `nil` are falsey; every other value, `0` and `""` and `[]` included, is true.

So a map used as a predicate tests *presence of the key*, not the usefulness of the value — except when the stored value is `nil` or `false`, at which point the entry exists but the element is dropped anyway.
If you mean "is it a member of this set", say that with a set: `#{2 3}` is the idiomatic predicate, and it cannot be confused by its values because it has none.

</div>

<div class="callout callout-exercise">
<p class="callout-title">Exercise</p>

Given `(def prices {:apple 0 :pear 12 :plum nil})`, what does each of these return, and why do two of them differ?

```clojure
(filterv prices [:apple :pear :plum :fig])
(mapv prices [:apple :pear :plum :fig])
(filterv #{:apple :pear :plum} [:apple :pear :plum :fig])
```

<details>
<summary>Show answer</summary>

```clojure
[:apple :pear]                ;; filterv with the map
[0 12 nil nil]                ;; mapv with the map
[:apple :pear :plum]          ;; filterv with the set
```

`:apple` survives the first filter because its value `0` is truthy.
`:plum` does not, because its value is `nil` — the key is present, but a map-as-predicate can only report the value, and `nil` is falsey.
`:fig` is absent and so also gives `nil`.

`mapv` keeps all four because it is not testing anything; the two `nil`s are "absent" and "present but `nil`", now indistinguishable.

The set gets `:plum` right because membership is the question it answers.
That is the lesson: use a map as a predicate only when you know its values are never `nil` or `false`, and reach for a set when you mean membership.

</details>

</div>

## TODO

- [x] Collections are functions — drafted, with snippets from `src/tenfold/day1.clj` and one exercise
- [ ] Draft prose for the remaining three gaps: quote, `def` vs `let`, `#()` and `%`
- [ ] Add one or two more interleaved exercises, to reach 2–3 across the chapter
- [ ] Write quiz
