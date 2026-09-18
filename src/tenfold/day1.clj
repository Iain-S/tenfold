(ns tenfold.day1
  "Day 1 — Programming Without Mutable State."
  (:require [clojure.core.reducers :as r]))


;; maps are functions of keys, keys are functions of maps

;; map definitions
(def m1 {:a 7 :b 8 :c 9})

;; lazy sequence
(map m1 [:a :b])

;; vector
(mapv m1 [:a :b])

;; mapping keywords
(mapv :a [m1 m1 m1])

;; filtering with a normal function
(filterv (fn [x] (< x 7)) [2 3 4 5 6 7 8 9 10])

;; filtering with a map as a filter func
;; note that our map contains 2 as a key with 0 as the value
(filterv {:a 0 "a" 0 2 0 3 1} [1 2 3 4 5])

;; ANCHOR: my-reduce
(defn my-reduce
  "reduce, written out longhand. Blows the stack on long inputs:
  Clojure has no tail-call optimisation, because the JVM has none."
  [f init coll]
  (if (empty? coll)
    init
    (my-reduce f (f init (first coll)) (rest coll))))
;; ANCHOR_END: my-reduce

;; ANCHOR: my-reduce-recur
(defn my-reduce-recur
  "The same function, with the recursion made explicit. `recur` reuses the
  current stack frame, so this one is safe on any input."
  [f init coll]
  (if (empty? coll)
    init
    (recur f (f init (first coll)) (rest coll))))
;; ANCHOR_END: my-reduce-recur

;; ANCHOR: word-frequencies
(defn word-frequencies
  "Count occurrences of each word, without mutating anything."
  [words]
  (reduce (fn [counts word] (update counts word (fnil inc 0)))
          {}
          words))
;; ANCHOR_END: word-frequencies

(comment
  (my-reduce + 0 [1 2 3 4])
  (my-reduce-recur + 0 (range 1000000))
  (word-frequencies ["a" "a" "b" "a" "c"])
  (r/fold + (vec (range 10000000))))
