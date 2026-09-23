# Folding a collection of your own

<div class="callout callout-book">
<p class="callout-title">In the book</p>

Nowhere.
The book shows `fold` from the caller's side throughout day 2; which collections can be folded, and what it takes to make one, is never discussed.

</div>

By now the two halves of the story have both been told: [Building a mini reducers library](05-mini-reducers.md) showed the protocol that `reduce` goes through, [The binary chop](06-binary-chop.md) showed how `fold` splits work up, and [CollFold and the two-arity combinef](07-collfold.md) showed what it demands of your combining function.

This chapter puts them together from the other side.
Instead of calling `fold`, we write the thing being folded.

## The two protocols

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

## A foldable corpus

Our word count reads 5,000 Wikipedia articles from a directory.
That is a collection: 5,000 elements, each a file to be read.
Here it is as a type:

```clojure
{{#include ../../../src/tenfold/corpus.clj:corpus-type}}
```

`coll-reduce` is the easy half — delegate to `reduce` over a lazy `map slurp`.

`coll-fold` is the interesting half, and it is [the binary chop](06-binary-chop.md) written out by hand: below the chunk size, read this chunk's files and reduce them on this thread; above it, split the file vector in two, fork one half, recurse into the other, and combine the two results.
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

## You may not need the type at all

There is a cheaper trick, and it is worth reaching for first.
A **vector of filenames** is already foldable, because it is a vector.
Fold that, and do the reading inside `reducef`:

```clojure
{{#include ../../../src/tenfold/corpus.clj:vector-of-files}}
```

No protocols, no fork/join, no `deftype` — twelve lines, and the pages are still read one at a time rather than all held in memory.
The collection being folded is a vector of `File` objects, a few hundred kilobytes; the 250MB of text never exists all at once.

## Four ways to count the same words

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
