(ns tenfold.mutable-state
  "Day 1 — Why mutable state is the problem.")

;; ANCHOR: race
(defn count-with-array
  "Four threads, 100,000 increments each, sharing one mutable slot.
  The arithmetic says 400,000. The JVM disagrees, differently each run."
  []
  ;; `let` binds names for the body below, like a `let` in Rust.
  ;; `long-array` looks like ordinary Clojure, but it is interop in disguise:
  ;; it allocates a Java array as if you did`new long[1]`.
  (let [slot    (long-array 1)
        ;; `future` runs its body on another thread and returns straight away.
        ;; `for` builds a sequence, here of four running futures; `doall`
        ;; forces that sequence to exist now rather than lazily, so all four
        ;; threads actually start.
        workers (doall (for [_ (range 4)]
                         (future
                           (dotimes [_ 100000]
                             ;; `aget` reads element 0; `aset` overwrites it.
                             ;; This is destructive assignment — the array is
                             ;; changed in place and every thread is looking at
                             ;; that same array. Ordinary Clojure cannot do
                             ;; this: `conj`, `assoc` and friends return a new
                             ;; value and leave the original untouched.
                             (aset slot 0 (inc (aget slot 0)))))))]
    ;; `deref` (which `@x` is shorthand for) waits for a future to finish.
    (run! deref workers)
    (aget slot 0)))
;; ANCHOR_END: race

;; try it
(count-with-array)

;; ANCHOR: atom-version
(defn count-with-atom
  "The same work, through an atom: 400,000, every time."
  []
  ;; An atom is a mutable reference holding an immutable value.
  (let [total   (atom 0)
        workers (doall (for [_ (range 4)]
                         (future
                           (dotimes [_ 100000]
                             ;; `swap!` applies a function to whatever is in
                             ;; the atom right now, and stores the result, as
                             ;; one indivisible step. If another thread got
                             ;; there first, `inc` is simply called again.
                             (swap! total inc)))))]
    (run! deref workers)
    ;; `@total` reads the atom's current value.
    @total))
;; ANCHOR_END: atom-version

;; try it
(count-with-atom)

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

;; ANCHOR: swap-vs-reset
(defn- on-four-threads
  "Run `work` 100,000 times on each of four threads, then wait for them all."
  [work]
  (run! deref (doall (for [_ (range 4)]
                       (future (dotimes [_ 100000] (work)))))))

(defn count-by-swapping
  "One step. `swap!` reads, applies inc, and writes, indivisibly."
  []
  (let [total (atom 0)]
    (on-four-threads #(swap! total inc))
    @total))

(defn count-by-resetting
  "Two steps. `@total` reads, `reset!` writes, and another thread can change
  the atom in between — so that thread's increment is overwritten and lost.
  No arrays and no interop here: this races using nothing but core Clojure."
  []
  (let [total (atom 0)]
    (on-four-threads #(reset! total (inc @total)))
    @total))
;; ANCHOR_END: swap-vs-reset
