# When fold doesn't help

<span class="todo">placeholder</span>

<div class="callout callout-book">
<p class="callout-title">In the book</p>

Day 2, wrap-up — mostly absent from the book.

</div>

## What the book says

TODO — summarise, briefly, so the reader can orient.

<div class="callout callout-gap">
<p class="callout-title">The book doesn't say</p>

- **Lazy sequences silently fall back to serial `reduce`.** `(take n (repeatedly f))` is a lazy seq.
- Non-associative `combinef` gives answers that are wrong *intermittently*, which is the worst kind of wrong.
- Amdahl's law: the serial fraction sets the ceiling.
- Work too small per element: the fork/join overhead dominates.

</div>

## TODO

- [ ] Draft prose
- [ ] Add runnable snippet from `src/tenfold/`
- [ ] Add 2–3 interleaved exercises with worked answers
- [ ] Write quiz
