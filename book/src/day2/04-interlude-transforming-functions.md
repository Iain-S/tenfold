# Interlude: functions that transform functions

<div class="callout callout-book">
<p class="callout-title">In the book</p>

Day 2, *Reducers' Internals*, p65–66 — the idea is used, never named.

</div>

Interludes do not follow the book.
This one exists because one idea carries the whole reducers library, and the book performs it rather than stating it.

## The move

`map` and `filter` look like functions from a collection to a collection.
Reducers make them something else: functions from a **reducing function** to a reducing function.

Nothing else changes.
The source is the same, the driver is the same — `reduce` still walks the collection and threads an accumulator through a step function.
What a reducer changes is the step.

That is worth saying in those words, because it reframes what `map` *is*.
`(r/map inc coll)` does not produce incremented data.
It produces a reduction that will increment each value as it goes past.
`reduce` is the only thing here that consumes anything; everything else adjusts how the consuming happens.

## What the composition actually looks like

Say you want three transformations over a vector:

```clojure
(my-reduce + 0 (my-map sq (my-map dbl (my-map inc v))))
```

At **build time**, that is three objects, each holding the next one and a transform function:

```text
my-map sq  ───►  my-map dbl  ───►  my-map inc  ───►  [0 1 2 …]
 (reify)          (reify)           (reify)           vector

 reducible ┘      reducible ┘       reducible ┘
 transformf       transformf        transformf
```

No data has moved.
Nothing has been counted, incremented or allocated beyond the three small objects themselves.

At **reduce time** the chain collapses.
Each layer transforms the reducing function it is given and passes the result inward, so the vector ends up holding a single function:

```text
   +  ──sq's transformf──►  rf₁  ──dbl's──►  rf₂  ──inc's──►  rf₃
                                                               │
                                             the vector walks itself once,
                                                calling rf₃ per element
```

The nesting survives only inside `rf₃`'s closures.
There is no intermediate collection to eliminate, because none is ever built — which is the claim the book makes on p65 without showing it.

<div class="callout callout-gap">
<p class="callout-title">The book doesn't say</p>

The transformations happen **once per reduction**, not once per element.

Three layers over a million elements means three calls to `transformf` — three, not three million — and then one function called a million times.
That is the whole efficiency argument, and it is invisible in the code: the cost you might expect to pay per element was paid once, before the walk began.

The composition also runs in two directions at two different times, which is worth watching for once:

- transform functions apply **outside-in**, when the reduction is set up
- the resulting steps then run **inside-out**, once per element

The traced example in [Building a mini reducers library](05-mini-reducers.md) prints both orders.

</div>

## A name for it: fusion

*Clojure for the Brave and True* has a good name for what the diagram above shows.
It calls the function applied to each element an **elemental function**, and calls collapsing several of them into one **fusion** — see [Know Your Reducers](https://www.braveclojure.com/quests/reducers/know-your-reducers/).

The distinction it draws is the useful part.
You can always fuse by hand: write `(map (comp sq dbl inc) coll)` rather than three nested `map`s, and you have one elemental function and no intermediate sequences.
That works, and it is a refactoring you have to remember to do — rewriting a pipeline that was perfectly clear as three steps.

Reducers fuse **automatically**.
Chain three `r/map`s and the transformations compose into a single reducing function before the walk begins, exactly as the diagram shows, with no rewriting on your part.
Written as three steps, executed as one.

That also sharpens the claim about intermediate collections.
It is not that reducers build them more cleverly; it is that fusing the elemental functions leaves nothing to build.

## Does it actually pay?

It is tempting to assume the intermediate-collection argument makes reducers dramatically faster.
Measured, on 1,000,000 elements, with three transformations — `inc`, then double, then square:

| Approach | ms |
|---|---|
| lazy seqs, `(map sq (map dbl (map inc v)))` | 35–45 |
| lazy seqs, one `map` with `(comp sq dbl inc)` | 25–30 |
| reducers, `my-map` nested three deep | 29–40 |
| reducers, one `my-map` with `(comp sq dbl inc)` | 25–35 |
| `r/fold`, `r/map` nested three deep | 6.1–6.4 |
| `r/fold`, one `r/map` with `comp` | 5.0–5.7 |
| `r/fold`, no transformation at all | 2.0 |

Apple M1 Pro (10 cores), Temurin JDK 17, Clojure 1.12.1, criterium `quick-benchmark`, three separate JVM sessions with the variants interleaved.
The ranges are the spread across those sessions, and they are wide: run-to-run variation is ±25%, which is larger than several of the differences in the table.

Three things survive that caveat.

**Stacking reducers costs little** — which is the fusion, visible in the numbers.
Nesting three `my-map`s came out 0–15% slower than hand-fusing the three functions with `comp` and using one `my-map`: one extra closure call per element per layer, and no allocation.
Chaining costs about what hand-fusing costs, which is what "fuses automatically" amounts to when measured rather than asserted.

**Stacking lazy sequences costs a lot.** The same choice is worth 35–50% there, consistently.
That gap is the intermediate sequences: two of them, built and discarded.
Lazy sequences do not fuse, so there the hand-fusing is not optional if you care about it.

**The serial win is modest.** Reducers are not dramatically faster than a lazy pipeline that composes its functions; on a vector, chunked sequences are already efficient.
Anyone selling reducers on serial speed alone is overselling.

<div class="callout callout-gap">
<p class="callout-title">The book doesn't say</p>

Where the payoff actually is: the last three rows.
`r/fold` is **5–8× faster** than any serial variant here, because it uses ten cores rather than one.

Reducers' serial machinery — the protocol, the reified objects, the transformed step functions — is not really an optimisation in itself.
It is the *precondition* for that parallelism: a chain of reducers can be folded because the transformation lives in a function rather than in a sequence of intermediate collections, and functions can be applied independently in every chunk at once.
Day 2 spends its pages on the machinery and its punchline on the speedup, without connecting them.

</div>

<div class="callout callout-exercise">
<p class="callout-title">Exercise</p>

`(comp sq dbl inc)` composes functions of a value: each takes one value and returns one value.
`transformf` composes functions of a *reducing function*: each takes an `rf` and returns an `rf`.

Given that both compose, why can `filter` be written as a `transformf` but not as an ordinary `comp`-able function of a value?

<details>
<summary>Show answer</summary>

Because a value function must return a value, and filtering sometimes needs to return *nothing at all*.

A reducing function takes `(acc, v)` and returns the new accumulator, so a filtering step has an obvious way to say "nothing happened here" — it returns `acc` unchanged:

```clojure
(fn [reducef]
  (fn [acc v] (if (pred v) (reducef acc v) acc)))
```

There is no equivalent at the value level: `(comp sq keep-if-even inc)` has nowhere to put "no value" without inventing a sentinel, which is why sequence `filter` returns a *collection* rather than a value.

This is the general reason the reducing-function level is the more expressive place to work — it carries the accumulator, so a step can choose to contribute nothing, contribute once, or contribute many times.
`mapcat` is the last of those.

</details>

</div>
