# AGENTS.md

`tenfold` is an mdBook companion to **days 1 and 2 of the Clojure chapter** of
*Seven Concurrency Models in Seven Weeks* (Paul Butcher, P1.0 — the Clojure
material is Chapter 3). It is prose first, with a small runnable Clojure
library alongside it. The design doc is
[`docs/superpowers/specs/2026-09-15-tenfold-design.md`](docs/superpowers/specs/2026-09-15-tenfold-design.md)
— read it before making structural changes.

## Layout

```text
Makefile              serve / build / clean / repl / tools
book/book.toml        mdBook config (quiz preprocessor, callout CSS, Pages URLs)
book/src/             chapters; SUMMARY.md is the table of contents
book/src/quizzes/     mdbook-quiz TOML, one per chapter
book/theme/custom.css callout and .todo styling
src/tenfold/          runnable Clojure, included into chapters
deps.edn              Clojure CLI deps
docs/                 design notes
book/book/            mdBook output — generated, gitignored, never edit
```

## Commands

```console
$ make serve                    # live preview at localhost:3000
$ make build                    # what CI runs
$ make clean                    # drop book/book and generated quiz assets
$ make repl                     # Clojure REPL with src/ on the classpath
$ make tools                    # install mdBook + mdbook-quiz at CI's versions
```

`make` on its own lists the targets. They are thin wrappers over `mdbook serve
book --open`, `mdbook build book` and `clj` — use those directly if you prefer.
The pinned versions live in one place, the `Makefile`; CI pins the same ones.

Clojure needs a JVM and the Clojure CLI. `.github/workflows/deploy.yml` builds on
push to `main` and deploys `book/book` to GitHub Pages.

## Copyright — the hard constraint

This repository is public; the source book is copyrighted and paid for.

- **Never** copy text or code from *Seven Concurrency Models in Seven Weeks*.
- Cite it by day, section and page instead (`Day 2, *Divide and Conquer*, p67`).
  Page numbers refer to the P1.0 PDF.
- Direct quotation only where it genuinely earns its place, and kept short.
- All prose here is original; all Clojure is original or derived from the
  author's own work.

Licences are split: prose is CC BY 4.0 (`LICENSE-PROSE`), code MIT
(`LICENSE-CODE`).

## Writing conventions

- **Companion, not substitute.** Assume the reader has the book open. The thesis
  is that *the things that bite you are exactly the things the book does not say
  out loud* — chapters exist to fill named gaps, not to re-teach the book.
- **Reader profile:** competent programmer, Rust-literate, new to Rust
  concurrency. Rust callouts are previews only — never load-bearing for the
  Clojure explanation.
- **Chapter order follows the book.** Material that needs a build-up the book
  does not give becomes an **Interlude** instead, which owes nothing to the
  book's sequencing.
- **British English**, em dashes, sentence-case headings. Match the voice of
  `book/src/introduction.md` and `book/src/day1/06-interlude-shape-of-a-fold.md`
  — those two are the reference for finished prose.
- **Timings are hand-curated** and must state their hardware and method. Do not
  present a number from an unstated machine.

### Callouts

Raw HTML in Markdown, with a blank line around the inner content so Markdown
still renders inside:

```html
<div class="callout callout-book">
<p class="callout-title">In the book</p>

Day 2, *Divide and Conquer*, p67–68.

</div>
```

| Class | Title | Purpose |
|---|---|---|
| `callout-book` | In the book | Anchor to day / section / page |
| `callout-gap` | The book doesn't say | The gap being filled — the point of the project |
| `callout-repl` | Try it in the REPL | Something to run in Calva |
| `callout-rust` | Coming up in Rust | Forward pointer to `rayon` / fearless concurrency |
| `callout-exercise` | (varies) | Exercise; the worked answer goes in a `<details>` |

### Code in chapters

Never paste Clojure into a chapter. Put it in `src/tenfold/`, wrap it in anchor
comments, and include it — so the prose cannot drift from code that runs:

```clojure
;; ANCHOR: my-reduce-recur
(defn my-reduce-recur [f init coll] ...)
;; ANCHOR_END: my-reduce-recur
```

```markdown
{{#include ../../../src/tenfold/day1.clj:my-reduce-recur}}
```

Verify any snippet you add by evaluating it (`clj`) before committing.

### Quizzes

One `mdbook-quiz` TOML per chapter in `book/src/quizzes/`, referenced at the end
of the chapter with `{{#quiz ../quizzes/<name>.toml}}`. Favour **conceptual**
questions ("why must `combinef` have a zero-arity?") over recall, give every
question a stable `id`, and write a `context` block explaining the answer.
`book/src/quizzes/collfold.toml` is the model.

## Chapter status

Most chapters are placeholders: a `<span class="todo">placeholder</span>` marker,
their callouts, and a TODO checklist carrying the gap-list. Written through so
far: the introduction, *Interlude: the shape of a fold*, and *CollFold and the
two-arity combinef*. When completing a placeholder, keep its gap-list — that
list is the chapter's brief — and remove the marker and checklist only once the
chapter genuinely covers it.

Adding a chapter means adding the file *and* its line in `book/src/SUMMARY.md`;
mdBook will not pick it up otherwise.

## Gotchas

- `book/book/` is build output. It is gitignored and must never be hand-edited.
- `book/src/quiz/` (singular) is generated mdbook-quiz assets and is also
  gitignored. `book/src/quizzes/` (plural) is the source — do not confuse them.
- `{{#include}}` paths are relative to the chapter file, so day chapters need
  `../../../src/tenfold/...`.
- `r/fold` over a lazy sequence silently falls back to serial `reduce`. If an
  example claims parallelism, check the collection is a vector or map.
