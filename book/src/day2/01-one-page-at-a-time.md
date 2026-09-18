# One page at a time

<div class="callout callout-book">
<p class="callout-title">In the book</p>

Day 2, *One Page at a Time*, p61–62.

</div>

## What the book says

Day 2 opens by parallelising the word count from day 1.
`pmap` is `map` with the function applied in parallel; `#(frequencies (get-words %))` is the `#()` reader macro, shorthand for a one-argument anonymous function; and `(partial merge-with +)` reduces the resulting per-page maps into one total.
Three ideas, two pages, and then it moves on to reducers.

What it does not tell you is where `pages` comes from.

<div class="callout callout-gap">
<p class="callout-title">The book doesn't say</p>

Day 1 counted words across a Wikipedia dump, and day 2 parallelises that count, but neither shows the code that turns a 24GB compressed file into a sequence you can `map` over.
The `pages` in every example is a two-element vector of short strings, or a variable that arrived from off-stage.

That missing code is the whole trick, and it is not in the book.

</div>

## Where the pages come from

A Wikipedia dump is one XML document containing every article.
Reading it means never having the document in memory, and the mechanism that allows it is `clojure.data.xml`:

```clojure
{{#include ../../../src/tenfold/wiki.clj:page-texts}}
```

The load-bearing line is `(:content (xml/parse reader))`.
`data.xml` is a **pull parser** — it reads from the stream only when asked — so that `:content` is a lazy sequence of child elements rather than a parsed document.
`filter` and `map` are lazy too, so the whole pipeline pulls one `<page>` from the file, hands it on, and forgets it.

This is why the book can wave at the problem: the laziness comes from the library, and the code that uses it looks like ordinary sequence code.
Nothing in `page-texts` says "stream" anywhere.

<div class="callout callout-gap">
<p class="callout-title">The book doesn't say</p>

Three things that will bite you here, none of which are visible in the code above.

**The file is compressed, and the compression is unusual.** Wikipedia dumps are *multistream* bzip2 — many bzip2 streams concatenated into one file.
`BZip2CompressorInputStream` stops at the end of the first stream unless you pass `true` for `decompressConcatenated`, and when it does, you get a partial read rather than an error.

**You cannot wrap the reader in `with-open` the obvious way.** Return a lazy sequence from inside `with-open` and the file is closed before anything has read from it:

```text
XMLStreamException: ParseError at [row,col]:[4,15]
Message: Stream closed
```

The `with-open` has to wrap the *consumption*, which is why the helper takes the function to apply rather than returning the sequence.

**The tags are not the keywords you expect.** A real dump declares a default XML namespace, so `data.xml` hands back tags like `:xmlns.http%3A%2F%2Fwww.mediawiki.org%2Fxml%2Fexport-0.11%2F/page`, not `:page`.
Match on `(= "page" (name (:tag node)))` rather than on the keyword, or `filter` finds nothing and you get an empty sequence with no error to explain it.
The version number in that namespace moves with the dump format, too, so matching the whole keyword breaks the next time Wikipedia bumps it.

**The filtering idiom is used before it is explained.** `(filter #(= :page (:tag %)) content)` appears whenever you walk a `data.xml` tree, because `:content` holds every child element — whitespace text nodes included — and you want the ones with a particular tag.
It is worth naming once: an element is a map of `:tag`, `:attrs` and `:content`, and walking the tree is `filter` on `:tag` all the way down.

</div>

## The version you will actually develop against

Parsing 24GB of XML on every REPL evaluation is not a development loop.
Extract a few thousand pages once, into one file each, and read the directory instead:

```clojure
{{#include ../../../src/tenfold/wiki.clj:page-files}}
```

`map slurp` is lazy in exactly the same way, so the laziness lesson survives the shortcut — and `slurp` closes each file as it goes, which sidesteps the `with-open` problem entirely.

Building that directory is one command:

```console
$ make corpus
```

which fetches one shard of the English Wikipedia export (285MB compressed, rather than the full 24GB) and streams 5,000 articles out of it into `wiki5000/`, skipping the redirect stubs that make up much of any dump.
It took 22 seconds here.
`make corpus CORPUS_SIZE=200` if you want a smaller one.

<div class="callout callout-repl">
<p class="callout-title">Try it in the REPL</p>

`write-sample-dump!` in `tenfold.wiki` writes a file in the same shape as a real dump — plain or `.bz2` — so you can exercise all of this before downloading anything:

```clojure
(require '[tenfold.wiki :as w])
(w/write-sample-dump! "sample.xml.bz2" 50)
(w/with-pages "sample.xml.bz2" 10 w/count-words-sequential)
```

For real data, Simple English Wikipedia is 340MB compressed and English Wikipedia is 23.9GB, both at `dumps.wikimedia.org`.
Develop against the first; save the second for the timing chapter.

</div>

## Laziness is not a style preference here

With 5,000 real articles — 195MB of text, median article 20KB — the difference between consuming the sequence and keeping it is the difference between working and not:

| | 256MB heap |
|---|---|
| `(count-words-sequential (dir-page-texts "wiki5000"))` | 27.6s, **completes** |
| `(vec (dir-page-texts "wiki5000"))` | **OutOfMemoryError** |

Same data, same heap, same JVM: 31,157,950 words and 804,126 distinct ones out of the first, and a heap dump out of the second.
Measured on an Apple M1 Pro, Temurin JDK 17, Clojure 1.12.1, `-Xmx256m`.

The lesson generalises past this example.
A lazy sequence is only as lazy as its least patient consumer, and holding a reference to the head of one is how you turn a streaming program into a loading program without changing a line of the pipeline.

## Back to `pmap`

With pages arriving lazily, the book's parallel version works as advertised — but `pmap`'s own laziness has a shape worth knowing.
It is *semi-lazy*: it runs ahead of consumption by a fixed margin, `(+ 2 (.. Runtime getRuntime availableProcessors))` futures, which is 12 on a 10-core machine.
Not "as parallel as your cores", and not tunable.

<div class="callout callout-gap">
<p class="callout-title">The book doesn't say</p>

`pmap` parallelises at whatever granularity you hand it, and one Wikipedia article is a *small* unit of work.
Each page becomes its own future: a task submitted, a thread coordinated, a result map allocated and later merged.
For work this size the coordination can cost more than the counting — which is exactly the problem the next section of the book solves with batching, without first saying that this is the problem it is solving.

Clojure's own docstring is blunt about it: `pmap` is "only useful for computationally intensive functions where the time of f dominates the coordination overhead".

</div>

<div class="callout callout-exercise">
<p class="callout-title">Exercise</p>

You want the ten most common words across the first 1,000 articles.
Which of these runs in constant memory, and which loads a gigabyte?

```clojure
(take 10 (sort-by (comp - val) (w/with-pages dump 1000 w/count-words-sequential)))

(let [pages (w/with-pages dump 1000 vec)]
  (take 10 (sort-by (comp - val) (w/count-words-sequential pages))))
```

<details>
<summary>Show answer</summary>

The first streams: each page is read, counted and discarded.
The second holds all 1,000 pages at once — `vec` forces the sequence inside `with-open`, so it does not throw, it simply loads the lot into memory and then does the same work.

The subtler point is that *neither* is constant memory overall.
`frequencies` builds a map of every distinct word, and that map grows with the vocabulary, not with the number of pages: 804,126 entries for 5,000 articles.
Streaming the input does not make the output small.

This is the distinction to hold on to when day 2 starts folding: the input can be streamed, the accumulator cannot.
A fold's combining step has to hold every chunk's partial result at once.

</details>

</div>

{{#quiz ../quizzes/one-page-at-a-time.toml}}
