# Effortless parallelism?

<span class="todo">placeholder</span>

<div class="callout callout-book">
<p class="callout-title">In the book</p>

Day 1, *Effortless Parallelism*, p57–58.

</div>

## What the book says

TODO — summarise, briefly, so the reader can orient.

<div class="callout callout-gap">
<p class="callout-title">The book doesn't say</p>

- How to measure honestly: JIT warm-up, `criterium` over `time`, median of several runs.
- Why the speedup is not the core count — memory bandwidth, boxing, GC.
- That `(r/fold + coll)` on a **list** is not parallel at all. The book's example uses a vector and never says why that matters.

</div>

## TODO

- [ ] Draft prose
- [ ] Add runnable snippet from `src/tenfold/`
- [ ] Add 2–3 interleaved exercises with worked answers
- [ ] Write quiz
