(ns tenfold.day2
  "Day 2 — Functional Parallelism.

  The running example is parallel merge sort, which is a good foil for the
  book's `+`: it forces you to confront the combinef contract that `+`
  satisfies by accident."
  (:require [clojure.core.reducers :as r]))

;; ANCHOR: merge-sorted
(defn merge-sorted
  "Merge two already-sorted sequences into one sorted sequence."
  [acc a b]
  (cond
    (empty? b) (concat acc a)
    (empty? a) (concat acc b)
    (< (first a) (first b)) (recur (conj (vec acc) (first a)) (rest a) b)
    :else                   (recur (conj (vec acc) (first b)) a (rest b))))
;; ANCHOR_END: merge-sorted

;; ANCHOR: broken-combinef
(defn merge-binary-broken
  "Looks right. Blows up inside r/fold with an ArityException."
  [a b]
  (merge-sorted [] a b))
;; ANCHOR_END: broken-combinef

;; ANCHOR: fixed-combinef
(defn merge-binary
  "A combining function for r/fold needs BOTH arities:
    - 0 args: produce the seed for a chunk's reduction
    - 2 args: merge two finished chunk results"
  ([] [])
  ([a b] (merge-sorted [] a b)))
;; ANCHOR_END: fixed-combinef

;; ANCHOR: insert-sorted
(defn insert-sorted
  "Insert val into the already-sorted sequence `ending`."
  [beginning val ending]
  (cond
    (empty? ending)        (concat beginning [val])
    (< val (first ending)) (concat beginning [val] ending)
    :else (recur (conj (vec beginning) (first ending)) val (rest ending))))

(defn sort-binary
  "Reducing function: grow a sorted accumulator one element at a time."
  [acc val]
  (insert-sorted [] val acc))
;; ANCHOR_END: insert-sorted

;; ANCHOR: parallel-sort
(defn parallel-sort
  "Sort by folding: sort each chunk serially, merge chunks pairwise."
  [coll]
  (r/fold merge-binary sort-binary coll))
;; ANCHOR_END: parallel-sort

(comment
  ;; Vector: really folded, in parallel.
  (parallel-sort (vec (repeatedly 1000 #(rand-int 100))))

  ;; Lazy seq: silently falls back to a serial reduce.
  (parallel-sort (repeatedly 1000 #(rand-int 100)))

  ;; The arity error, on demand:
  (r/fold merge-binary-broken sort-binary (vec (range 10))))
