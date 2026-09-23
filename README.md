# tenfold

A companion to **days 1 and 2 of the Clojure chapter** of *Seven Concurrency Models in Seven Weeks* by Paul Butcher.

I found the book moved a bit too quickly and used functions without thoroughly introducing them first.
This companion book tries to rectify that.

Read it at **<https://iain-s.github.io/tenfold>** (with the book open beside you).

## Building

```console
$ make tools     # cargo install mdbook + mdbook-quiz, at the versions CI uses
$ make serve     # live preview at http://localhost:3000
```

`make` with no target lists the rest (`build`, `clean`, `repl`, `fmt`).

Prose is written one sentence per line, with no hard wrap.
`make fmt` enforces that, and `make hooks` installs a pre-commit check — see [AGENTS.md](AGENTS.md).

## Running the Clojure

Needs a JVM and the [Clojure CLI](https://clojure.org/guides/install_clojure).

```console
$ clj
```

Setting up VS Code and Calva is covered in [the setup chapter](book/src/day1/02-setup-calva.md).

## Layout

```text
book/           mdBook source
  src/          chapters, and quizzes/ for mdbook-quiz TOML
  theme/        callout styling
src/tenfold/    runnable Clojure, included into chapters via {{#include}}
docs/           design notes
```

Chapters include code from `src/tenfold/` rather than copying it, so the prose cannot drift from code that actually runs.

## Status

Scaffold.
Three chapters are written through — the introduction, *Interlude: the shape of a fold*, and *CollFold and the two-arity combinef* — and the rest are placeholders carrying their gap-lists.
See [the design doc](docs/superpowers/specs/2026-09-15-tenfold-design.md).

## Licence

Dual-licensed, the usual split for a book with code in it:

- **Prose** — [CC BY 4.0](LICENSE-PROSE).
  Share it, adapt it, use it in your teaching; just credit it.
- **Code** — [MIT](LICENSE-CODE).
  Everything under `src/`, and every code snippet in the chapters.

Copyright © 2026 Iain Stenson.

This repository contains no text or code from *Seven Concurrency Models in Seven Weeks*.
It cites the book by day, section and page so you can read along.
Buy the book; this is useless without it.
