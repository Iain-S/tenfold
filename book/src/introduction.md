# Introduction

This is a companion to **days 1 and 2 of the Clojure chapter** of *Seven
Concurrency Models in Seven Weeks* by Paul Butcher.

## Why this exists

*Seven Concurrency Models* is a good book that moves rather too fast in some places.

This companion slows down at those points.

Two examples, both of which cost real debugging time before a word of this was
written.

**The zero-arity seed.** On page 66 the book builds its own reducers library,
and writes this:

```clojure
(coll-reduce reducible (transformf f1) (f1))
```

Look at `(f1)` — the reducing function, called with *no arguments*, to produce
the seed value. The book prints it and moves on. It is never mentioned. And it
is a hard requirement: hand `r/fold` a combining function that has no zero-arity
case and you get an `ArityException` from somewhere deep inside the reducers
machinery, with nothing in the message to connect it back to that unremarked
`(f1)` on page 66.

**The silent serial fallback.** The book never tells you which collections can
actually be folded in parallel. Vectors and maps can. Lazy sequences cannot —
they quietly fall back to an ordinary serial `reduce`. So this:

```clojure
(def nums (take 10000 (repeatedly #(rand-int 100))))
(r/fold + nums)
```

is not parallel. It gives the right answer, at single-threaded speed, with no
warning of any kind. `take` returns a lazy sequence.

Neither of these is a flaw in the reducers library. Both are load-bearing facts
that the book leaves implicit.

## Why "tenfold"

Because that is the book's implicit promise, and the question worth asking of
it: what does it actually take to get a tenfold speedup out of a fold? Some of
the answer is "use `r/fold`." Rather more of it is knowing when `r/fold` will
do nothing at all.

## How to read this

Chapters follow the book's own running order, so you can read the two side by
side. Where an idea needs a proper build-up that the book does not give it,
there is an **Interlude** instead — a standalone chapter that owes nothing to
the book's sequencing.

Four kinds of callout appear throughout:

<div class="callout callout-book">
<p class="callout-title">In the book</p>

Where you are: day, section, and page. Page numbers refer to the P1.0 PDF.

</div>

<div class="callout callout-gap">
<p class="callout-title">The book doesn't say</p>

The gap being filled. If you only read one kind of callout, read these.

</div>

<div class="callout callout-repl">
<p class="callout-title">Try it in the REPL</p>

Something to run. All the code is in `src/` in this repository, so you can
clone it and evaluate as you read.

</div>

<div class="callout callout-rust">
<p class="callout-title">Coming up in Rust</p>

A forward pointer. This companion assumes you know Rust the language but have
not yet met its concurrency story — no `rayon`, no `Send`/`Sync`. These
callouts flag ideas you will meet again there, so they are *previews*, never
prerequisites. Skip every one of them and the Clojure still makes sense.

</div>

## Running the code

You will need a JVM and the [Clojure CLI](https://clojure.org/guides/install_clojure).

```console
$ git clone https://github.com/Iain-S/tenfold
$ cd tenfold
$ clj
```

The book's day-2 examples run over a Wikipedia dump. Getting hold of one is
covered in [One page at a time](day2/01-one-page-at-a-time.md).

## What this is not

This companion contains no text or code from *Seven Concurrency Models in Seven
Weeks*. It cites the book by day, section and page so you can read along, and
quotes it only where a direct quotation earns its place. Buy the book; it is
worth it, and this is useless without it.
