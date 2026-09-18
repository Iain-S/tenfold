(ns tenfold.calva-and-paraedit
  "Day 1 — Setup Calva.")

;; Use ctrl+enter and alt+enter on different parts of this expression.
(+ (- 1 50) 500)

;; slurp forward
(+ 1)5

;; barf foreward
(+ 1 2 3)

;; splice
(+ 1 (2) 3)

;; raise
(+ 1 (2) 3)

;; raise again
(+ (+ 1 1) 0)

;; change to 
;; (println (map inc [1 2 3]))
;; using slurp, barf, splice and/or raise
(map inc [1 2 3]) (println "done")

;; instrumenting a function
(defn badfunc []
  (+ 2
     (* 1E7 1E7)
     (- 1 2)
     (/ 7 0)))

;; running the instrumented function
(badfunc)

;; #dbg
(defn goodfunc []
  #dbg(+ 2
     (* 1E1 1E2)
     (- 1 2)
     (/ 7 1)))

(goodfunc)

