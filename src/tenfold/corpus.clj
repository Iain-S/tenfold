(ns tenfold.corpus
  "A collection of our own, which folds in parallel.

  `r/fold` works on vectors and maps and quietly degrades to a serial
  `reduce` on everything else. This namespace shows what the other side of
  that looks like: a type that implements the two protocols `reduce` and
  `fold` are built on, over a directory of Wikipedia pages too large to hold
  in memory."
  (:require [clojure.core.protocols :as p]
            [clojure.core.reducers :as r]
            [tenfold.wiki :as wiki])
  (:import (java.util.concurrent ForkJoinPool ForkJoinTask)))

;; ANCHOR: fj
(defn fj-invoke
  "Run `f` inside the common fork/join pool and wait for the answer.
  `clojure.core.reducers` has exactly this, but keeps it private."
  [f]
  (if (ForkJoinTask/inForkJoinPool)
    (f)
    (.invoke (ForkJoinPool/commonPool) ^ForkJoinTask (r/fjtask f))))

(defn fj-fork [^ForkJoinTask task] (.fork task))
(defn fj-join [^ForkJoinTask task] (.join task))
;; ANCHOR_END: fj

;; ANCHOR: corpus-type
(deftype PageCorpus [files]
  clojure.lang.Counted
  (count [_] (count files))

  clojure.lang.Seqable
  (seq [_] (seq (map slurp files)))

  ;; `reduce` goes through this. Without it, the Object baseline would walk
  ;; the seq above — correct, just slower.
  p/CollReduce
  (coll-reduce [this f] (p/coll-reduce this f (f)))
  (coll-reduce [_ f init] (reduce f init (map slurp files)))

  ;; `r/fold` goes through this. Without it, the Object baseline calls
  ;; `reduce` and you get no parallelism and no warning.
  r/CollFold
  (coll-fold [_ n combinef reducef]
    (let [cnt (count files)]
      (if (<= cnt n)
        ;; small enough: read this chunk's files and reduce them on this thread
        (reduce reducef (combinef) (map slurp files))
        ;; too big: split in half, fork one side, do the other here, combine
        (let [half  (quot cnt 2)
              left  (PageCorpus. (subvec files 0 half))
              right (PageCorpus. (subvec files half))]
          (fj-invoke
           (fn []
             (let [task (fj-fork (r/fjtask #(r/coll-fold right n combinef reducef)))]
               (combinef (r/coll-fold left n combinef reducef)
                         (fj-join task))))))))))

(defn corpus
  "A foldable collection over the .txt pages in `dir`."
  [dir]
  (->PageCorpus (vec (wiki/page-files dir))))
;; ANCHOR_END: corpus-type

;; ANCHOR: corpus-use
(defn count-words-folded
  "Word frequencies across the corpus, folded in parallel.

  `n` is the chunk size in *pages*, not elements — the unit that matters
  here is a file, because reading it is most of the work."
  [corpus n]
  (r/fold n
          (partial merge-with +)                       ; combinef: merge chunk results
          (fn [counts page]                            ; reducef: fold one page in
            (merge-with + counts (frequencies (wiki/words page))))
          corpus))
;; ANCHOR_END: corpus-use

;; ANCHOR: vector-of-files
(defn count-words-file-vector
  "The same parallel word count with no protocol code at all.

  A *vector of filenames* is already foldable — it is a vector — so `r/fold`
  chops it up and hands each chunk to a thread. The reading happens inside
  reducef, one page at a time, so the pages are never all in memory."
  [dir n]
  (r/fold n
          (partial merge-with +)
          (fn [counts file]
            (merge-with + counts (frequencies (wiki/words (slurp file)))))
          (vec (wiki/page-files dir))))
;; ANCHOR_END: vector-of-files
