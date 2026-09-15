# tenfold

A companion to **days 1 and 2 of the Clojure chapter** of *Seven Concurrency
Models in Seven Weeks* by Paul Butcher.

That book is good and it moves fast — functions and terms arrive already in
use. This fills the gaps, with one organising claim:

> The things that bite you are exactly the things the book does not say out loud.

Read it at **https://iain-s.github.io/tenfold** (with the book open beside you).

## Building

```console
$ cargo install mdbook mdbook-quiz --locked
$ mdbook serve book --open
```

## Running the Clojure

Needs a JVM and the [Clojure CLI](https://clojure.org/guides/install_clojure).

```console
$ clj
```

Setting up VS Code and Calva is covered in
[the setup chapter](book/src/day1/02-setup-calva.md).

## Layout

```text
book/           mdBook source
  src/          chapters, and quizzes/ for mdbook-quiz TOML
  theme/        callout styling
src/tenfold/    runnable Clojure, included into chapters via {{#include}}
docs/           design notes
```

Chapters include code from `src/tenfold/` rather than copying it, so the prose
cannot drift from code that actually runs.

## Status

Scaffold. Three chapters are written through — the introduction, *Interlude: the
shape of a fold*, and *CollFold and the two-arity combinef* — and the rest are
placeholders carrying their gap-lists. See
[the design doc](docs/superpowers/specs/2026-09-15-tenfold-design.md).

## Licence

TODO — not yet chosen. Likely CC BY 4.0 for the prose and MIT for the code.

This repository contains no text or code from *Seven Concurrency Models in Seven
Weeks*. It cites the book by day, section and page so you can read along. Buy
the book; this is useless without it.
