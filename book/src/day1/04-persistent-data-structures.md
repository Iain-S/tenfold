# Persistent data structures

<span class="todo">placeholder</span>

<div class="callout callout-book">
<p class="callout-title">In the book</p>

Nowhere in days 1 and 2.
The tour on p52–53 uses immutable collections throughout without ever saying what makes updating one cheap, and the term *persistent data structure* does not appear until Chapter 4, *The Clojure Way* (p86).
The words "structural sharing" appear nowhere in the book at all.

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
