# tenfold — design

**Date:** 2026-09-15 **Author:** Iain Stenson (with Claude) **Status:** approved in chat; scaffold in progress

## What this is

`tenfold` is a small web book that expands on **days 1 and 2 of the Clojure chapter** of *Seven Concurrency Models in Seven Weeks* (Paul Butcher, Pragmatic Bookshelf, P1.0).
In that edition the Clojure material is Chapter 3, *Functional Programming*: Day 1 *Programming Without Mutable State* (p50) and Day 2 *Functional Parallelism* (p61).

The book introduces functions and terms with very little introduction.
`tenfold` fills those gaps.

### Thesis

The things that bite you are exactly the things the book does not say out loud.
Two concrete cases, both encountered before writing a line of this book:

1. **The zero-arity seed.** On p66 the book writes `(coll-reduce reducible (transformf f1) (f1))` — calling `f1` with no arguments to produce the seed — and never remarks on it.
   A `combinef` without a 0-arity throws `ArityException` from `r/fold`.
2. **Silent serial fallback.** The book never says which collections implement `CollFold`.
   Vectors and maps do; lazy sequences do not, and fall back to a serial `reduce` with no warning.
   `(take n (repeatedly ...))` is a lazy seq, so `r/fold` over it is not parallel at all.

The name is the book's question: what does it actually take to get a tenfold speedup out of a fold?

## Decisions

| Decision | Choice | Why |
|---|---|---|
| Generator | mdBook | Same tooling as the Brown Rust book; Markdown others can edit |
| Quizzes | `mdbook-quiz` | The Brown lab's own preprocessor; TOML quizzes referenced from Markdown |
| Hosting | GitHub Pages | Free on public repos; static output from mdBook |
| Repo | `Iain-S/tenfold`, public | Pages is free on public repos; no org governance overhead |
| Mode | Companion, not standalone | Reader has the PDF open at days 1–2 |
| Reader | Competent programmer, Rust-literate, **new to Rust concurrency** | Matches the author and intended colleagues |
| Rust content | Forward pointers only | Reader has not met `rayon` yet; signposts, never prerequisites |
| Code | Ported, cleaned up, runnable | Own `deps.edn` + `src/`; chapters `{{#include}}` from real files |
| Structure | Book order + interludes | Companion navigation without inheriting confusing sequencing |

### Rejected alternatives

- **Quarto (via clojupyter or Clay).** Viable, and would bake real evaluated output into the page.
  Rejected because the prize is small here: timings depend on core count, so a number rendered on a 2-core CI runner is *less* truthful than a curated number labelled with its hardware.
  Quizzes would also have to be hand-rolled.
- **Mirror the book section-for-section.** Inherits the running order the book got wrong.
- **Concept-first reordering.** Better pedagogy, but unusable as a companion — the reader cannot find their place.

## Copyright stance

The source book is copyrighted and paid-for.
`tenfold` is public, so:

- All prose is original.
  All code is original or derived from the reader's own work in `calva_project`.
- The book is cited by day, section, and page — never reproduced.
- Short quotations only where a direct quote genuinely earns its place.
- `tenfold` is a companion: it is not useful as a substitute for the book, by design.

## Structure

```
tenfold/
  book/
    book.toml
    src/
      SUMMARY.md
      introduction.md
      day1/*.md
      day2/*.md
      afterword/*.md
      quizzes/*.toml
  src/                    # runnable Clojure, included into chapters
    tenfold/*.clj
  deps.edn
  .github/workflows/deploy.yml
```

### Chapter outline

**Front matter** — how to use this book: edition and page anchors, callout conventions, how to run the code.

**Part 1 — Day 1: Programming Without Mutable State**

1. Why mutable state is the problem
2. Clojure in twenty minutes, for Rust programmers
3. *Gap:* persistent data structures — why "copying" is cheap (structural sharing; the book asserts this without showing it)
4. `reduce` from first principles — `my-reduce`, absence of TCO, `recur`
5. **Interlude: the shape of a fold** — identity and associativity; a monoid, without using the word
6. Effortless parallelism? — first `r/fold`, and honest measurement
7. Counting words; laziness, and where it bites

**Part 2 — Day 2: Functional Parallelism**

1. One page at a time — lazy XML parsing
2. Batching, and why it wins
3. What a *reducible* actually is
4. **Interlude: functions that transform functions** — the conceptual heart the book never names; the road to transducers
5. Building a mini reducers library — protocols, `reify`, `CollReduce`
6. The binary chop — fork/join, work-stealing, and that 512
7. **`CollFold` and the two-arity `combinef`** — the arity error as a worked debugging story
8. When `fold` doesn't help — lazy-seq fallback, non-associative combine, Amdahl

**Afterword**

- What reducers became: transducers (the book predates them)
- Consolidated Rust forward-pointers

## Conventions

Four callout styles, used consistently:

| Callout | Purpose |
|---|---|
| **In the book** | Anchor to Day / section / page |
| **The book doesn't say** | Our expansion — the gap being filled |
| **Coming up in Rust** | Forward pointer to fearless concurrency / `rayon` |
| **Try it in the REPL** | Something to run in Calva |

Rust callouts are *previews*.
They must make sense to a reader who has never seen `rayon`, and must never be load-bearing for the Clojure explanation.

### Quizzes

One `mdbook-quiz` TOML per chapter, weighted toward **conceptual** questions ("why must `combinef` have a zero-arity?") over recall ("what does `r/fold` return?").
This follows the Brown lab's own finding that conceptual questions discriminate better between readers who understood and readers who did not.

### Code fidelity

Chapters include snippets from real files under `src/` via mdBook's `{{#include}}` with anchor comments, so prose cannot drift from code that actually runs.
Timings are hand-curated, and every timing states its hardware and method.

## Scope of the first pass

Scaffold, not finished book:

- Every chapter file exists, with its callouts and section headings stubbed and marked `TODO`.
- A few chapters carry real content end to end, so the rendering can be previewed and the conventions judged.
- At least one working quiz.
- Working local build (`mdbook serve`) and a Pages deploy workflow.

## Success criteria

1. `mdbook serve` renders the book locally with working navigation.
2. At least one `mdbook-quiz` quiz works in the browser.
3. All four callout styles appear in the rendered output, visually distinct.
4. Included Clojure snippets come from files that actually run under `clojure -M`.
5. A colleague with the PDF open can read a completed chapter and be less confused than by the book alone.
