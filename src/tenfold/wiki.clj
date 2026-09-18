(ns tenfold.wiki
  "Reading a Wikipedia dump lazily.

  Day 1's `Putting It All Together` counts words across a Wikipedia dump
  without ever holding the dump in memory, and the book shows the counting
  but not the reading. This namespace is the missing half: a lazy sequence
  of page texts, from a file far larger than your heap."
  (:require [clojure.data.xml :as xml]
            [clojure.java.io :as io]
            [clojure.string :as str])
  (:import (org.apache.commons.compress.compressors.bzip2 BZip2CompressorInputStream
                                                      BZip2CompressorOutputStream)))

;; ANCHOR: dump-reader
(defn dump-reader
  "A Reader over a Wikipedia dump, decompressing .bz2 on the fly.

  The dumps are `multistream` bzip2 — many concatenated streams — so
  decompressConcatenated must be true or you silently get only the first
  chunk of the file."
  ^java.io.Reader [filename]
  (let [in (io/input-stream filename)]
    (io/reader (if (str/ends-with? filename ".bz2")
                 (BZip2CompressorInputStream. in true)
                 in)
               :encoding "UTF-8")))
;; ANCHOR_END: dump-reader

;; ANCHOR: page-texts
(defn- tag=
  "Does this node have the given tag?

  Compared by *name*, not by keyword identity. A real dump declares a default
  XML namespace, so `data.xml` hands back tags like
  :xmlns.http%3A%2F%2Fwww.mediawiki.org%2Fxml%2Fexport-0.10%2F/page
  rather than :page. Matching on the name works either way."
  [tag node]
  (and (map? node)
       (some? (:tag node))
       (= (name tag) (name (:tag node)))))

(defn- child
  "The first child element of `element` with the given tag."
  [element tag]
  (first (filter #(tag= tag %) (:content element))))

(defn- page-text
  "The article text buried at <page><revision><text>...</text></revision></page>."
  [page]
  (first (:content (child (child page :revision) :text))))

(defn page-texts
  "A lazy sequence of article texts from an open dump reader.

  `clojure.data.xml/parse` is a pull parser: the `:content` of the root
  element is a lazy sequence, and each <page> is parsed only as you ask for
  it. Nothing here holds on to the head, so a 40GiB dump costs one page of
  memory at a time — as long as the caller does not hold on to it either."
  [reader]
  (->> (:content (xml/parse reader))
       (filter #(tag= :page %))
       (map page-text)
       (remove nil?)))
;; ANCHOR_END: page-texts

;; ANCHOR: page-files
(defn page-files
  "The page files in `dir`, in numeric order: 0.txt, 1.txt, 2.txt, ...

  A directory of already-extracted pages is the practical way to work while
  writing this book — no XML parsing, no decompression, and each file is one
  article. The laziness question does not go away, though: 5,000 articles is
  200MB of text, so the sequence still has to be consumed as it is produced."
  [dir]
  (->> (file-seq (io/file dir))
       (filter #(.isFile ^java.io.File %))
       (filter #(str/ends-with? (.getName ^java.io.File %) ".txt"))
       (sort-by #(parse-long (str/replace (.getName ^java.io.File %) ".txt" "")))))

(defn dir-page-texts
  "A lazy sequence of page texts from a directory of .txt files.

  `map slurp` is lazy, so each file is read — and closed — only as the
  sequence is walked."
  [dir]
  (map slurp (page-files dir)))
;; ANCHOR_END: page-files

;; ANCHOR: count-words
(defn words
  "The words of a page, lowercased."
  [text]
  (map str/lower-case (re-seq #"\w+" text)))

(defn count-words-sequential
  "Word frequencies across a sequence of page texts, on one thread."
  [pages]
  (frequencies (mapcat words pages)))
;; ANCHOR_END: count-words

;; ANCHOR: with-pages
(defn with-pages
  "Call `f` with a lazy sequence of at most `n` page texts from `filename`,
  closing the dump afterwards.

  The `with-open` has to wrap the *consumption*, not the construction of the
  sequence: return a lazy sequence from inside `with-open` and the reader is
  closed before anything reads from it."
  [filename n f]
  (with-open [r (dump-reader filename)]
    (f (cond->> (page-texts r)
         n (take n)))))
;; ANCHOR_END: with-pages

(comment
  ;; Count words in the first 100 articles of a dump:
  (with-pages "enwiki-latest-pages-articles.xml.bz2" 100 count-words-sequential)

  ;; How many pages are in there at all? (Streams the lot; takes a while.)
  (with-pages "enwiki-latest-pages-articles.xml.bz2" nil count))

;; ANCHOR: sample-dump
(defn write-sample-dump!
  "Write a small file in the same shape as a Wikipedia dump, so the reader
  above can be exercised without downloading 20GiB first.

  Pass a filename ending in .bz2 to get a compressed one. The pages are
  written one at a time rather than built up in memory, so this can generate
  a file much larger than the heap."
  [filename n-pages]
  (let [out (io/output-stream filename)]
    (with-open [w (io/writer
                   (if (str/ends-with? filename ".bz2")
                     (BZip2CompressorOutputStream. out)
                     out)
                   :encoding "UTF-8")]
      (.write w "<mediawiki>\n")
      (doseq [i (range n-pages)]
        (.write w (str "  <page>\n"
                       "    <title>Article " i "</title>\n"
                       "    <revision>\n"
                       "      <text xml:space=\"preserve\">"
                       (str/join " " (repeat 20 (str "word" (mod i 7))))
                       " common</text>\n"
                       "    </revision>\n"
                       "  </page>\n")))
      (.write w "</mediawiki>\n"))
    filename))
;; ANCHOR_END: sample-dump

;; ANCHOR: extract-pages
(defn extract-pages!
  "Write the first `n` page texts from `dump` into `dir`, one file each:
  0.txt, 1.txt, ... — the corpus the rest of this book develops against.

  Streams: the dump is never held in memory, so this works on a full 24GB
  dump as happily as on a 300MB shard. Skips redirect stubs, which make up a
  large fraction of any dump and contain no prose worth counting."
  [dump dir n]
  (let [out (io/file dir)]
    (.mkdirs out)
    (with-open [r (dump-reader dump)]
      (let [texts (->> (page-texts r)
                       (remove #(str/starts-with? (str/triml %) "#REDIRECT"))
                       (take n))]
        (doseq [[i text] (map-indexed vector texts)]
          (spit (io/file out (str i ".txt")) text))
        (count (.listFiles out))))))
;; ANCHOR_END: extract-pages

(defn -main
  "Entry point for `make corpus`: extract-pages! from the command line."
  [dump dir n]
  (println (format "Extracting %s pages from %s into %s/ ..." n dump dir))
  (println (format "Wrote %d files." (extract-pages! dump dir (parse-long n))))
  (shutdown-agents))
