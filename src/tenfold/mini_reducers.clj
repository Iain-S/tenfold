(ns tenfold.mini-reducers
  "A miniature reducers library, built from scratch — and, first, the
  protocol machinery the book uses a page before explaining it."
  (:require [clojure.core.protocols :as p]))

;; ANCHOR: own-protocol
;; A protocol is a named set of functions, with no implementation.
(defprotocol Greet
  "Anything that can greet."
  (greeting [this] "Return a greeting for this thing."))

;; Implementations are attached to types afterwards, from anywhere —
;; including to types you do not own, like String.
(extend-protocol Greet
  String
  (greeting [s] (str "hello, " s))

  clojure.lang.IPersistentVector
  (greeting [v] (str "hello, all " (count v) " of you")))

(comment
  (greeting "world")                    ;=> "hello, world"
  (greeting [1 2 3])                    ;=> "hello, all 3 of you"
  (satisfies? Greet 5)                  ;=> false
  (greeting 5))
;; IllegalArgumentException: No implementation of method: :greeting
;;   of protocol: #'tenfold.mini-reducers/Greet found for class: java.lang.Long
;; ANCHOR_END: own-protocol

;; ANCHOR: reify-example
;; `reify` makes one anonymous object that implements a protocol, in place.
;; No deftype, no name, no file of its own.
(def polite
  (reify Greet
    ;; `greeting` here is a *declaration*, not a call: it names the protocol
    ;; method being implemented. `[_]` is that method's parameter list, and
    ;; its first parameter is the object itself — Java's `this`. We have no
    ;; use for it, so it is named `_`, the conventional name for "ignored".
    ;; It cannot be left out: the arity has to match the protocol.
    (greeting [_] "good evening")))

(comment
  (greeting polite)                     ;=> "good evening"
  (type polite)                         ;=> tenfold.mini_reducers$reify__1234
  (satisfies? Greet polite))            ;=> true
;; ANCHOR_END: reify-example
;; ANCHOR: this-example
;; One reify can implement several things at once — here the Greet protocol
;; and Object's toString. Now `this` earns its keep: toString has nothing of
;; its own to say, so it asks the object for its greeting. That call is the
;; only way one method can reach another on the same object.
(def named
  (reify Greet
    (greeting [_] "good evening")

    Object
    (toString [this] (str "#<Greeter " (pr-str (greeting this)) ">"))))

(comment
  (str named)                           ;=> "#<Greeter \"good evening\">"
  (greeting named))                     ;=> "good evening"
;; ANCHOR_END: this-example


;; ANCHOR: make-greeter
;; A function that *returns* a reified object. The object closes over `n`,
;; so each call to make-greeter hands back a different greeter.
(defn make-greeter [n]
  (reify Greet
    (greeting [_] (clojure.string/join " " (repeat n "hello")))))

;; And one that wraps an existing greeter, transforming what it says. It
;; holds another Greet and delegates to it — the shape make-reducer uses.
(defn decorate [greeter transformf]
  (reify Greet
    (greeting [_] (transformf (greeting greeter)))))

(comment
  (greeting (make-greeter 3))                                  ;=> "hello hello hello"
  (greeting (decorate polite clojure.string/upper-case))       ;=> "GOOD EVENING"
  (greeting (decorate (make-greeter 2) #(str % "!"))))         ;=> "hello hello!"
;; ANCHOR_END: make-greeter

;; ANCHOR: my-reduce
;; `coll-reduce` is not special syntax and not built into the language: it is
;; one function in clojure.core.protocols, which this namespace requires as
;; `p`. Nothing refers it for you.
;;
;; Note what this function actually does: it swaps the argument order, and
;; nothing else. Clojure's sequence functions take the collection LAST, by
;; long convention — (reduce f coll). A protocol dispatches on the type of
;; its FIRST argument, so coll-reduce must take the collection first. This
;; is the shim between the two, which is why both arities are one-liners.
(defn my-reduce
  ([f coll]      (p/coll-reduce coll f))
  ([f init coll] (p/coll-reduce coll f init)))
;; ANCHOR_END: my-reduce

;; ANCHOR: make-reducer
(defn make-reducer
  "Wrap a reducible so that a reducing function handed to it is put through
  `transformf` before the wrapped reducible ever sees it. The result is not a
  collection — it is a thing that knows how to be reduced.

  It holds no state of its own, so it can be reduced more than once, with a
  different reducing function each time."
  [reducible transformf]
  (reify p/CollReduce
    ;; Two spellings of the same name on one line, doing different jobs:
    ;;
    ;;   coll-reduce   — bare, in the method position: the name of the
    ;;                   protocol method we are implementing. Not a var
    ;;                   reference at all, which is why it needs no `p/`
    ;;                   even though nothing referred it into this namespace.
    ;;   p/coll-reduce — qualified, in the body: an ordinary call to the
    ;;                   function in clojure.core.protocols, which dispatches
    ;;                   on whatever `reducible` turns out to be.
    ;;
    ;; `_` is again `this`, ignored: this object answers out of the
    ;; `reducible` and `transformf` it closed over, not out of itself.
    (coll-reduce [_ f] (p/coll-reduce reducible (transformf f) (f)))
    (coll-reduce [_ f init] (p/coll-reduce reducible (transformf f) init))))

(defn my-map [mapf reducible]
  (make-reducer reducible
                (fn [reducef]
                  (fn [acc v] (reducef acc (mapf v))))))

(defn my-filter [predf reducible]
  (make-reducer reducible
                (fn [reducef]
                  (fn [acc v] (if (predf v) (reducef acc v) acc)))))
;; ANCHOR_END: make-reducer

(comment
  (my-reduce + [1 2 3 4])                                  ;=> 10
  (my-reduce + 10 [1 2 3 4])                               ;=> 20
  (my-reduce + 0 (my-map inc [1 2 3 4]))                   ;=> 14
  (my-reduce + 0 (my-filter even? (my-map inc [1 2 3 4]))) ;=> 6
  )

;; ANCHOR: traced-map
(defn traced-map
  "my-map, with print statements, to show when each part actually runs."
  [label f reducible]
  (make-reducer reducible
                (fn [rf]
                  (println " " label "transformf runs — building a reducing function")
                  (fn [acc v]
                    (println "   " label "step sees" v)
                    (rf acc (f v))))))

(comment
  ;; Building prints nothing at all: no work has happened.
  (def r (traced-map "outer" inc (traced-map "inner" inc [1 2])))

  ;; Reducing is what triggers everything.
  (my-reduce + 0 r))
;; ANCHOR_END: traced-map
