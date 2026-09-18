# Introduction

This is a companion to **days 1 and 2 of the Clojure chapter** of *Seven Concurrency Models in Seven Weeks* by Paul Butcher.

## Why this exists

*Seven Concurrency Models* is a good book that moves rather too fast in some places.

This companion slows down at those points.

## Why "tenfold"

Because there are 10 cores on an Apple M1 chip and one way to do work on all of them in parallel is to use `r/fold`/

## How to read this

Chapters follow the book's own running order, so you can read the two side by side.
Where an idea needs a proper build-up that the book does not give it, there is an **Interlude** instead — a standalone chapter that owes nothing to the book's sequencing.

Four kinds of callout appear throughout:

<div class="callout callout-book">
<p class="callout-title">In the book</p>

Where you are: day, section, and page.
Page numbers refer to the P1.0 PDF.

</div>

<div class="callout callout-gap">
<p class="callout-title">The book doesn't say</p>

The gap being filled.
If you only read one kind of callout, read these.

</div>

<div class="callout callout-repl">
<p class="callout-title">Try it in the REPL</p>

Something to run.
All the code is in `src/` in this repository, so you can clone it and evaluate as you read.

</div>

<div class="callout callout-rust">
<p class="callout-title">Coming up in Rust</p>

A forward pointer.
This companion assumes you know Rust the language but have not yet met its concurrency story — no `rayon`, no `Send`/`Sync`.
These callouts flag ideas you will meet again there, so they are *previews*, never prerequisites.
Skip every one of them and the Clojure still makes sense.

</div>

## Running the code

You will need a JVM and the [Clojure CLI](https://clojure.org/guides/install_clojure).

```console
$ git clone https://github.com/Iain-S/tenfold
$ cd tenfold
...
```

The book's day-2 examples run over a Wikipedia dump.
Getting hold of one is covered in [One page at a time](day2/01-one-page-at-a-time.md).

## What this is not

This companion contains no text or code from *Seven Concurrency Models in Seven Weeks*.
It cites the book by day, section and page so you can read along,.
