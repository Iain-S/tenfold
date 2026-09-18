(ns tenfold.mutable-state
  "Day 1 — Why mutable state is the problem.")

;; ANCHOR: race
(defn count-with-array
  "Four threads, 100,000 increments each, sharing one mutable slot.
  The arithmetic says 400,000. The JVM disagrees, differently each run."
  []
  (let [slot    (long-array 1)
        workers (doall (for [_ (range 4)]
                         (future (dotimes [_ 100000]
                                   (aset slot 0 (inc (aget slot 0)))))))]
    (run! deref workers)
    (aget slot 0)))
;; ANCHOR_END: race

;; ANCHOR: atom-version
(defn count-with-atom
  "The same work, through an atom: 400,000, every time."
  []
  (let [total   (atom 0)
        workers (doall (for [_ (range 4)]
                         (future (dotimes [_ 100000]
                                   (swap! total inc)))))]
    (run! deref workers)
    @total))
;; ANCHOR_END: atom-version

;; ANCHOR: interop
;; A Clojure collection is immutable: conj returns a new vector and leaves
;; the old one alone.
(def v1 [1 2 3])
(def v2 (conj v1 4))
v1                        ;=> [1 2 3]
v2                        ;=> [1 2 3 4]

;; A Java object reached through interop is not. `(StringBuilder. "ab")`
;; constructs one; `(.append sb "c")` calls a method on it.
(def sb (StringBuilder. "ab"))
(.append sb "c")          ;; mutates sb in place
(str sb)                  ;=> "abc"
;; ANCHOR_END: interop

;; ANCHOR: transient
(defn build-vector
  "A transient is a mutable draft of a persistent collection: cheap to add to,
  and converted back with persistent! when the building is finished."
  [n]
  (persistent!
   (reduce conj! (transient []) (range n))))
;; ANCHOR_END: transient
