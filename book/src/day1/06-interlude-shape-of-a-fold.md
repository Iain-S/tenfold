# Interlude: the shape of a fold

<div class="callout callout-book">
<p class="callout-title">In the book</p>

Nowhere.
This chapter exists because the book uses these properties throughout days 1 and 2 without ever naming them.

</div>

Interludes do not follow the book.
This one is here because everything in day 2 depends on two properties that day 1 relies on silently.

## Why order stops mattering

`reduce` is a loop with an accumulator.
It walks left to right, and that order is part of its meaning:

```clojure
{{#include ../../../src/tenfold/day1.clj:my-reduce-recur}}
```

`fold` throws that order away.
It cuts the collection into pieces, reduces each piece independently and in parallel, then combines the pieces.
Nothing coordinates which piece finishes first, or which two get combined together.

So for `fold` to give the same answer as `reduce` — reliably, not just on the run you happened to try — the combining operation must not care about order or grouping.
That is two separate requirements.

## Associativity

**Associativity** means the grouping does not matter:

```text
(a ⊕ b) ⊕ c  ==  a ⊕ (b ⊕ c)
```

`+` is associative.
`-` is not: `(1 - 2) - 3` is `-4`, but `1 - (2 - 3)` is `2`.

This is precisely the property `fold` needs, because the shape of the combining tree is not something you control.
It depends on the collection's size, the chunk boundaries, and which fork/join worker finished first.

<div class="callout callout-gap">
<p class="callout-title">The book doesn't say</p>

Associativity is a **correctness requirement**, not a performance consideration.
Fold a non-associative function and you do not get a crash — you get an answer, sometimes the right one.
Change the input size, move to a machine with a different core count, and the answer changes.

Note that associativity is *not* commutativity.
`fold` does not require commutativity: the pieces are combined in collection order, so `str` folds correctly even though `(str "a" "b")` differs from `(str "b" "a")`.
What it requires is that the *bracketing* be free.

</div>

## Identity

**Identity** means there is a value that changes nothing:

```text
e ⊕ a  ==  a  ==  a ⊕ e
```

`0` for `+`.
`1` for `*`.
`""` for `str`.
`[]` for merging sorted sequences.

`fold` needs one because each chunk's reduction has to start somewhere.
Rather than take a starting value as an argument, `fold` asks the combining function for one, by calling it with no arguments — which is the whole subject of [CollFold and the two-arity combinef](../day2/07-collfold.md).

An operation with both properties is a **monoid**.
You do not need the word, but you do need the two properties, and now you can recognise them: *does the bracketing matter, and is there a do-nothing value?*

<div class="callout callout-rust">
<p class="callout-title">Coming up in Rust</p>

The same two requirements turn up in Rust's `rayon`, and for the same reason — they are facts about parallel reduction, not about either language.
What differs is how you are told: `rayon` makes you pass the identity explicitly, so forgetting it is caught at compile time.

Neither language can check associativity for you.
That one is on you in both.

</div>

## Exercises

<div class="callout callout-exercise">
<p class="callout-title">Exercise 1 — which of these can be folded?</p>

For each, decide whether it is associative, and whether it has an identity: subtraction, `max`, string concatenation, "average of two numbers", set union, function composition.

<details>
<summary>Show answer</summary>

| Operation | Associative? | Identity | Foldable? |
|---|---|---|---|
| subtraction | no | — | No |
| `max` | yes | none in `Long` (needs `Long/MIN_VALUE`) | Yes, with a hand-written combinef |
| string concat | yes | `""` | Yes |
| average of two | **no** | — | No |
| set union | yes | `#{}` | Yes |
| composition | yes | `identity` | Yes |

"Average" is the trap, and it is a trap people fall into in production.
The average of the averages of two chunks is not the average of the whole, unless the chunks are the same size — which fold does not guarantee.
Fold a *sum* and a *count* together, then divide once at the end.

</details>

</div>

<div class="callout callout-exercise">
<p class="callout-title">Exercise 2 — break it on purpose</p>

Fold subtraction over `(vec (range 1000))` and compare to `reduce`.
Then try `(vec (range 100))`.
Do both disagree?
Does the disagreement change between runs?

<details>
<summary>Show answer</summary>

`(vec (range 100))` is a single chunk, so `fold` degenerates to `reduce` and the two agree — the bug is invisible.
`(vec (range 1000))` is two chunks and they disagree.

The disagreement is usually stable between runs on the same machine, because the chunking is deterministic given the size.
This is what makes it dangerous: it looks reproducible, so it looks like it must be correct, right up until the input size or the hardware changes.

</details>

</div>

{{#quiz ../quizzes/shape-of-a-fold.toml}}
