# Persistent data structures

<span class="todo">placeholder</span>

<div class="callout callout-book">
<p class="callout-title">In the book</p>

Day 1, *A Whirlwind Tour of Clojure*, p54 (asserted in passing).

</div>

## What the book says

TODO — summarise, briefly, so the reader can orient.

<div class="callout callout-gap">
<p class="callout-title">The book doesn't say</p>

- **Structural sharing.** The book says updates are cheap without showing why.
- The 32-way branching trie behind vectors, and where the `O(log32 n)` comes from.
- Why this is what makes parallel `fold` safe: no chunk can observe another's write.

</div>

## TODO

- [ ] Draft prose
- [ ] Add runnable snippet from `src/tenfold/`
- [ ] Add 2–3 interleaved exercises with worked answers
- [ ] Write quiz
