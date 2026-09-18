(ns tenfold.day1
  "Day 1 — Programming Without Mutable State."
  (:require [clojure.core.reducers :as r]))


;; Maps are functions of keys; keywords are functions of maps.

;; ANCHOR: lookup-both-ways
(def m1 {:a 7 :b 8 :c 9})

(m1 :a)             ;=> 7    the map is the function, the key is the argument
(:a m1)             ;=> 7    the keyword is the function, the map is the argument

(m1 :zz)            ;=> nil  a missing key is not an error
(m1 :zz :fallback)  ;=> :fallback
(:zz m1 :fallback)  ;=> :fallback
;; ANCHOR_END: lookup-both-ways

;; ANCHOR: lookup-as-argument
(map m1 [:a :b])       ;=> (7 8)    a lazy sequence
(mapv m1 [:a :b])      ;=> [7 8]    a vector

(mapv :a [m1 m1 m1])   ;=> [7 7 7]  the keyword does the looking up instead
;; ANCHOR_END: lookup-as-argument

;; ANCHOR: lookup-as-predicate
(filterv (fn [x] (< x 7)) [2 3 4 5 6 7 8 9 10])  ;=> [2 3 4 5 6]

;; m1 has no integer keys, so every lookup is nil and everything is dropped
(filterv m1 [1 2 3 4 5])                         ;=> []

;; this map does have 2 as a key — and its value, 0, is truthy in Clojure
(filterv {:a 0 "a" 0 2 0 3 1} [1 2 3 4 5])       ;=> [2 3]

;; a set is the idiomatic membership predicate
(filterv #{2 3} [1 2 3 4 5])                     ;=> [2 3]
;; ANCHOR_END: lookup-as-predicate

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
