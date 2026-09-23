# AGENTS.md

`tenfold` is an mdBook companion to **days 1 and 2 of the Clojure chapter** of *Seven Concurrency Models in Seven Weeks* (Paul Butcher, P1.0 — the Clojure material is Chapter 3).
It is prose first, with a small runnable Clojure library alongside it.
The design doc is [`docs/superpowers/specs/2026-09-15-tenfold-design.md`](docs/superpowers/specs/2026-09-15-tenfold-design.md) — read it before making structural changes.

## Layout

```text
Makefile              serve / build / clean / repl / fmt / hooks / tools
scripts/mdfmt.py      the Markdown one-sentence-per-line formatter
.pre-commit-config.yaml  the hooks git runs before each commit
book/book.toml        mdBook config (quiz preprocessor, callout CSS, Pages URLs)
book/src/             chapters; SUMMARY.md is the table of contents
book/src/quizzes/     mdbook-quiz TOML, one per chapter
book/theme/custom.css callout and .todo styling
book/theme/highlight.js highlight.js bundle — generated, includes Clojure
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
$ make fmt                      # reflow Markdown to one sentence per line
$ make fmt-check                # fail if any Markdown is unformatted
$ make hooks                    # install the pre-commit hooks (once per clone)
$ make tools                    # install mdBook + mdbook-quiz at CI's versions
$ make highlight-js             # regenerate book/theme/highlight.js
$ make corpus                   # fetch a dump shard, extract 5000 pages to wiki5000/
```

`make` on its own lists the targets.
They are thin wrappers over `mdbook serve book --open`, `mdbook build book` and `clj` — use those directly if you prefer.
The pinned versions live in one place, the `Makefile`; CI pins the same ones.

Clojure needs a JVM and the Clojure CLI.
`.github/workflows/deploy.yml` builds on push to `main` and deploys `book/book` to GitHub Pages.

## Copyright — the hard constraint

This repository is public; the source book is copyrighted and paid for.

- **Never** copy text or code from *Seven Concurrency Models in Seven Weeks*.
- Cite it by day, section and page instead (`Day 2, *Divide and Conquer*, p67`).
  Page numbers refer to the P1.0 PDF.
- Direct quotation only where it genuinely earns its place, and kept short.
- All prose here is original; all Clojure is original or derived from the author's own work.

Licences are split: prose is CC BY 4.0 (`LICENSE-PROSE`), code MIT (`LICENSE-CODE`).

### The local PDF

A copy of the book's PDF sits in the working directory, so citations can be checked rather than guessed.
**It must never be committed**, and neither must any text extracted from it.
`.gitignore` covers the PDF itself and the `pdf_*.txt` extracts; put anything else you extract outside the repository.

Check a citation with `pdftotext -layout <pdf> -` and search the result.
**Never convert PDF page to printed page with a fixed offset** — it drifts through the book (14 at the start, then 13 from p50, 12 from p190, 11 from p224, 9 from p272) because of unnumbered part and blank pages.
Read the printed number off each page's own running header instead: `pdftotext` puts it on the page's first line, as `Chapter 3. Functional Programming • 50`.
Extracts are a research aid only — nothing from them belongs in a chapter beyond the day, section and page.

### Other sources worth reading

Two good, freely available sources of Clojure lore, useful when a chapter needs background the book does not give:

- [*Clojure for the Brave and True*](https://www.braveclojure.com/) — Daniel Higginbotham's introduction, strong on the reader's-first-encounter explanations this project is trying to write.
- The [official Clojure blog](https://clojure.org/news/news) and the reference pages under [clojure.org](https://clojure.org/reference/documentation) — in particular Rich Hickey's original reducers posts, which are the primary source for day 2.

Treat these exactly as the book is treated: **read them for inspiration and to check your understanding, never to copy from**.
Their wording, their examples and their diagrams are all someone else's copyright.
If one of them shapes a chapter, link to it and say what it gave you.

## Writing conventions

- **Companion, not substitute.** Assume the reader has the book open.
  The thesis is that *the things that bite you are exactly the things the book does not say out loud* — chapters exist to fill named gaps, not to re-teach the book.
- **Reader profile:** competent programmer, Rust-literate, new to Rust concurrency.
  Rust callouts are previews only — never load-bearing for the Clojure explanation.
- **Chapter order follows the book.** Material that needs a build-up the book does not give becomes an **Interlude** instead, which owes nothing to the book's sequencing.
- **British English**, em dashes, sentence-case headings.
  Match the voice of `book/src/introduction.md` and `book/src/day1/06-interlude-shape-of-a-fold.md` — those two are the reference for finished prose.
- **Timings are hand-curated** and must state their hardware and method.
  Do not present a number from an unstated machine.

### One sentence per line

Every Markdown file in this repository is written **one sentence per line, with no hard wrap** — lines run as long as the sentence does, and the editor soft-wraps them.
Sentences are the unit of the line, so a diff shows the sentence that changed rather than a reflowed paragraph.
The same rule applies inside list items: the first sentence follows the marker, and each later sentence goes on its own line, indented to line up with the text above it.

Run `make fmt` after writing prose; `scripts/mdfmt.py` does the reflow, and leaves code fences, tables, raw HTML, headings and `{{#include}}` directives untouched.
`make hooks` installs the [pre-commit](https://pre-commit.com/) hooks, which run the same check over staged Markdown and refuse the commit if anything is unformatted — worth running once per clone.
`.pre-commit-config.yaml` is the only hook mechanism here; it also covers whitespace, YAML and TOML syntax, spelling, markdownlint, and a guard against committing the book PDF.
Install the runner first, with `brew install pre-commit` or `pipx install pre-commit`.
The transformation only changes where lines break, so the rendered HTML is identical either way.

### Callouts

Raw HTML in Markdown, with a blank line around the inner content so Markdown still renders inside:

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

Never paste Clojure into a chapter.
Put it in `src/tenfold/`, wrap it in anchor comments, and include it — so the prose cannot drift from code that runs:

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

One `mdbook-quiz` TOML per chapter in `book/src/quizzes/`, referenced at the end of the chapter with `{{#quiz ../quizzes/<name>.toml}}`.
Favour **conceptual** questions ("why must `combinef` have a zero-arity?") over recall, give every question a stable `id`, and write a `context` block explaining the answer.
`book/src/quizzes/collfold.toml` is the model.

## Chapter status

Most chapters are placeholders: a `<span class="todo">placeholder</span>` marker, their callouts, and a TODO checklist carrying the gap-list.
Written through so far: the introduction, *Interlude: the shape of a fold*, and *CollFold and the two-arity combinef*.
When completing a placeholder, keep its gap-list — that list is the chapter's brief — and remove the marker and checklist only once the chapter genuinely covers it.

Adding a chapter means adding the file *and* its line in `book/src/SUMMARY.md`; mdBook will not pick it up otherwise.

## Gotchas

- `book/book/` is build output.
  It is gitignored and must never be hand-edited.
- `book/src/quiz/` (singular) is generated mdbook-quiz assets and is also gitignored.
  `book/src/quizzes/` (plural) is the source — do not confuse them.
- `{{#include}}` paths are relative to the chapter file, so day chapters need `../../../src/tenfold/...`.
- `r/fold` over a lazy sequence silently falls back to serial `reduce`.
  If an example claims parallelism, check the collection is a vector or map.
- A real Wikipedia dump declares an XML namespace, so `data.xml` tags come back as `:xmlns.…export-0.11%2F/page`, never `:page`.
  Match tags by `name`, as `tenfold.wiki/tag=` does; matching the keyword silently finds nothing.
- `make corpus` builds `wiki5000/` from a dump shard.
  The dump, the corpus and anything derived from them are gitignored — Wikipedia text is CC BY-SA, and the corpus is reproducible from one command, so neither belongs in the repository.
- mdBook's bundled highlight.js has **no Clojure grammar**, so ```` ```clojure ```` blocks render unstyled with the stock theme. `book/theme/highlight.js` overrides it with the highlight.js common bundle plus Clojure, and `make highlight-js` regenerates that file — do not hand-edit it. Highlighting is applied in the browser, so a rendered page's HTML shows no `hljs-` spans; check the bundle itself if it looks wrong.
