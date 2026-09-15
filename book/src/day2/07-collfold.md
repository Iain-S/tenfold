# CollFold and the two-arity combinef

<div class="callout callout-book">
<p class="callout-title">In the book</p>

Day 2, *Supporting Fold*, p68–69.

</div>

This chapter is about one unremarked pair of parentheses, and the afternoon it
can cost you.

## The error

Here is a combining function for a parallel merge sort. It merges two
already-sorted sequences. It is correct, and it is obviously correct:

```clojure
{{#include ../../../src/tenfold/day2.clj:broken-combinef}}
```

Hand it to `r/fold` and you get this:

```text
Execution error (ArityException)
Wrong number of args (0) passed to: tenfold.day2/merge-binary-broken
```

Zero arguments. Nothing in your code calls `merge-binary-broken` with zero
arguments. You did not write a call with zero arguments. Something inside the
reducers library did.

## Where the zero-argument call comes from

Back on page 66, when the book builds its own reducers library, it writes a
`coll-reduce` implementation whose seed value is `(f1)` — the reducing function,
invoked with no arguments. That line is printed and never discussed.

That is the call. `fold` splits your collection into chunks and reduces each one
separately, and each of those reductions needs a starting value. Rather than
take an `init` argument, `fold` asks the combining function to produce one, by
calling it with no arguments.

<div class="callout callout-gap">
<p class="callout-title">The book doesn't say</p>

`combinef` must be a function of **two arities**:

- `(combinef)` — zero arguments, returning the seed for a chunk's reduction
- `(combinef a b)` — two arguments, merging two finished chunk results

The book's examples hide this because they use `+`, and `+` already has a
zero-arity: `(+)` is `0`. It satisfies the contract by accident, so the contract
never has to be explained.

</div>

The fix is to say out loud what `+` was doing silently:

```clojure
{{#include ../../../src/tenfold/day2.clj:fixed-combinef}}
```

## Why it is not just a seed

The zero-arity is not an implementation detail you can satisfy with any old
value. `(combinef)` must be an **identity** for `combinef`, and `combinef`
itself must be **associative** — for the reasons set out in
[Interlude: the shape of a fold](../day1/06-interlude-shape-of-a-fold.md).

`[]` works above because merging an empty sequence with anything gives back that
thing unchanged. Had we returned, say, `[0]`, every chunk would have quietly
contributed a spurious zero to the result — and how many spurious zeros you got
would depend on how many chunks the collection happened to be split into, which
depends on its size and your machine. That is a bug that changes its answer when
you move it to a different laptop.

<div class="callout callout-rust">
<p class="callout-title">Coming up in Rust</p>

Rust's data-parallelism library, `rayon`, has the same split — but it makes the
seed an explicit argument rather than a hidden arity:

```rust
// roughly: reduce each chunk from an identity, then combine the chunks
collection.par_iter()
    .fold(|| identity(), |acc, x| reduce(acc, x))
    .reduce(|| identity(), |a, b| combine(a, b));
```

Because the identity is a parameter you must pass, the error you just debugged
cannot happen: leaving it out is a compile error, not a runtime `ArityException`
from inside a library. Same contract, enforced at a different time.

You will not meet `rayon` for a while yet. Note only the shape: *reduce within
chunks, combine across chunks, and the combine step needs an identity.*

</div>

## The other half of the trap

Fix the arity and this still is not parallel:

```clojure
(def nums (take 10000 (repeatedly #(rand-int 100))))
(parallel-sort nums)
```

It returns the right answer, at single-threaded speed. `take` returns a **lazy
sequence**, and lazy sequences do not implement `CollFold`. When `fold` is given
something it cannot fold, it falls back to a plain serial `reduce`. No warning,
no error — just none of the parallelism you asked for.

<div class="callout callout-gap">
<p class="callout-title">The book doesn't say</p>

Which collections actually fold in parallel. Measured, rather than assumed:

| Collection | Folds in parallel? |
|---|---|
| vector | **yes** |
| hash map (`{...}`, `zipmap`, `into {}`) | **yes** |
| sorted map, array map | no |
| hash set | no |
| list, lazy sequence, `range` | no |

Everything in the "no" rows degrades silently to a serial `reduce`. That
includes anything built by `take`, `map`, `filter`, `repeatedly` or `range` —
which is to say, most of the collections you produce without thinking about it.

Note that `satisfies?` will not tell you which is which. `CollFold` has a
default implementation extended to `Object` — the serial fallback — so
everything "satisfies" the protocol. The only reliable test is to measure.

If you want `fold` to do anything at all, make it a vector. `(vec ...)` is cheap
insurance.

</div>

```clojure
{{#include ../../../src/tenfold/day2.clj:parallel-sort}}
```

<div class="callout callout-repl">
<p class="callout-title">Try it in the REPL</p>

```clojure
(require '[tenfold.day2 :refer [parallel-sort]])

;; really parallel
(time (parallel-sort (vec (repeatedly 200000 #(rand-int 1000)))))

;; silently serial — same answer, and you would never know
(time (parallel-sort (repeatedly 200000 #(rand-int 1000))))
```

</div>

## Exercises

<div class="callout callout-exercise">
<p class="callout-title">Exercise 1 — find the other accidental identities</p>

`+` satisfies the combinef contract by accident. Which of these do too, and what
does each return with no arguments?

`*`, `max`, `str`, `conj`, `into`, `concat`, `comp`

<details>
<summary>Show answer</summary>

| Function | `(f)` | Valid combinef? |
|---|---|---|
| `*` | `1` | Yes — identity for multiplication, associative |
| `str` | `""` | Yes |
| `concat` | `()` | Yes |
| `comp` | `identity` | Yes |
| `conj` | `[]` | Two-arity is fine, but `conj` is **not associative** over collections — `(conj (conj [] 1) 2)` and `(conj [] (conj [1] 2))` are different things |
| `into` | throws | No zero-arity |
| `max` | throws | No zero-arity — and there is no identity to give it short of negative infinity |

`max` is the instructive one. It is perfectly associative, so it *feels* like it
should fold, but it has no identity element, so there is nothing sensible for
`(max)` to return. You fold a maximum by supplying your own combinef:
`(fn ([] Long/MIN_VALUE) ([a b] (max a b)))`.

</details>

</div>

<div class="callout callout-exercise">
<p class="callout-title">Exercise 2 — make the wrong answer appear</p>

Write a combinef whose zero-arity returns something that is *not* an identity —
for instance `(fn ([] [0]) ([a b] (merge-sorted [] a b)))`. Fold a vector of 100
elements with it, then the same contents as a vector of 100 000 elements. How
many spurious zeros does each result contain, and why is it not the same number?

<details>
<summary>Show answer</summary>

One spurious `0` per chunk: **1** for n=100, and **256** for n=100 000.

If you predicted 196 for the second one — n/512, rounded up — that is the
natural reading of the book's description, and it is wrong. `fold` does not cut
the collection into 512-element pieces. It *halves* it, halves the halves, and
keeps going until the pieces are smaller than 512. So the number of chunks is
always a power of two:

| n | chunks | `ceil(n/512)` |
|---|---|---|
| 100 | 1 | 1 |
| 1 000 | 2 | 2 |
| 10 000 | 32 | 20 |
| 100 000 | 256 | 196 |
| 1 000 000 | 2 048 | 1 954 |

Real chunks are therefore between 256 and 512 elements, not 512.

The point of the exercise is the failure *mode*, not the arithmetic. A broken
identity produces a bug whose severity scales with input size and whose exact
answer depends on chunking and the machine. It will pass your unit tests, which
use small inputs, and corrupt your production run.

</details>

</div>

<div class="callout callout-exercise">
<p class="callout-title">Exercise 3 — fold a set and a map</p>

Try `r/fold` over a set, and over a map. Before you run them, predict which will
actually run in parallel.

```clojure
(r/fold + (set (range 10000)))
(r/fold (fn ([] 0) ([a b] (+ a b)))
        (fn [acc [_ v]] (+ acc v))
        (zipmap (range 10000) (range 10000)))
```

<details>
<summary>Show answer</summary>

The map folds in parallel; the set does not.

`PersistentHashMap` implements `CollFold`. `PersistentHashSet` does not — despite
being built on the same trie machinery, and being exactly as unordered and as
splittable. It is an omission, not a principle. Sorted maps and array maps do not
fold either.

If you ran the map example as written, you also hit a second surprise:

```text
ArityException  Wrong number of args (3) passed to: ...
```

When `fold` reduces a map it calls `reducef` with **three** arguments —
`(acc k v)`, the key and value separately — not with a two-element pair the way
`reduce` over a map does. So a reducing function written for `reduce` will not
work with `fold`, and fails with an `ArityException` that looks exactly like the
one at the top of this chapter but has a completely different cause.

Two different arity errors, same library, neither mentioned in the book.

The general lesson: you cannot reason about which collections fold from first
principles, because the answer is a fact about what somebody implemented. Measure,
or convert to a vector.

</details>

</div>

{{#quiz ../quizzes/collfold.toml}}
