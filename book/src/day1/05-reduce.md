# reduce, from first principles

<span class="todo">placeholder</span>

<div class="callout callout-book">
<p class="callout-title">In the book</p>

Day 1, *Our First Functional Program*, p55–57.

</div>

## What the book says

TODO — summarise, briefly, so the reader can orient.

<div class="callout callout-gap">
<p class="callout-title">The book doesn't say</p>

- Why the hand-written recursive `my-reduce` blows the stack: **Clojure has no TCO**, because the JVM has none. The book mentions this only in the day-1 self-study.
- `recur` as the explicit fix, and why it is a special form rather than a function.
- The argument-order trap: `reduce` takes `(f init coll)`, `coll-reduce` takes `(coll f init)`.

</div>

## TODO

- [ ] Draft prose
- [ ] Add runnable snippet from `src/tenfold/`
- [ ] Add 2–3 interleaved exercises with worked answers
- [ ] Write quiz
