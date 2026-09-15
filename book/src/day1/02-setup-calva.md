# Setting up: VS Code and Calva

<div class="callout callout-book">
<p class="callout-title">In the book</p>

Not in the book. Butcher assumes you have a working Clojure environment and
leaves you to it.

</div>

You can read the next chapter without any of this. You cannot *do* anything
without it, and Clojure is a language you learn by evaluating things, so spend
the ten minutes.

Bindings below are macOS. Calva's docs list Windows and Linux equivalents where
they differ; the big divergence is slurp and barf.

## 1. Install

You need a JVM (any recent one), the Clojure CLI, and VS Code.

```console
$ brew install temurin clojure/tools/clojure
$ clojure --version
```

Then install the **Calva** extension — search the VS Code marketplace for
"Calva: Clojure & ClojureScript Interactive Programming". Nothing else is
required; Calva bundles its own language server and nREPL middleware.

## 2. How a deps.edn project is laid out

Calva expects a normal `deps.edn` project, which is what this repository is:

```text
tenfold/
  deps.edn              # dependencies and source paths
  src/
    tenfold/
      day1.clj          # namespace tenfold.day1
      day2.clj          # namespace tenfold.day2
```

The rule that catches everyone once: **the file path must match the namespace**,
and underscores in filenames become hyphens in namespace names. A namespace
`tenfold.mini-reducers` lives in `src/tenfold/mini_reducers.clj`. Get this wrong
and you get a "namespace not found" that looks nothing like a filename problem.

## 3. Jack in

Open the `tenfold` folder in VS Code, then:

<div class="callout callout-repl">
<p class="callout-title">Try it in the REPL</p>

Press <kbd>ctrl+alt+c</kbd> <kbd>ctrl+alt+j</kbd> — that is two chords, one after
the other. The command is **"Start a Project REPL and Connect"**, which everyone
calls *jack-in*.

Calva will ask which project type (choose **deps.edn**) and which aliases to
include (none, for now). A REPL window opens. You are connected when the status
bar shows the connection and the REPL prompt appears.

</div>

Jack-in starts the REPL *for* you with the nREPL dependencies Calva needs
already injected. The alternative — "Connect to a running REPL" — attaches to a
REPL you started yourself, which means getting that middleware right by hand.
Use jack-in unless you have a specific reason not to.

## 4. Evaluating things

This is the whole point. Put the cursor inside a form and:

| Keys | What it evaluates |
|---|---|
| <kbd>ctrl+enter</kbd> | the **current form** — the innermost one containing the cursor |
| <kbd>alt+enter</kbd> | the **current top-level form** — the whole `(defn ...)` you are inside |
| <kbd>ctrl+shift+enter</kbd> | the **enclosing form** |

`alt+enter` is the one you will use most: it redefines the function you are
editing, in the running REPL, without restarting anything. That is the
interactive-programming loop the book is implicitly assuming throughout.

Results appear inline next to the form and in the REPL window.
<kbd>ctrl+alt+c</kbd> <kbd>ctrl+c</kbd> copies the last result to the clipboard.

## 5. Four Paredit commands worth learning

Calva edits *structure*, not text. Fighting it with ordinary text editing is the
single most common reason people bounce off Clojure tooling. Learn these four
and the rest can wait.

| Keys (macOS) | Command | What it does |
|---|---|---|
| <kbd>ctrl+alt+.</kbd> | **Slurp forward** | pull the next form *into* the current one |
| <kbd>ctrl+alt+,</kbd> | **Barf forward** | push the last form *out* of the current one |
| <kbd>ctrl+alt+s</kbd> | **Splice** | remove the brackets around the current form |
| <kbd>ctrl+alt+p</kbd> <kbd>ctrl+alt+r</kbd> | **Raise** | replace the enclosing form with the current one |

On Windows and Linux, slurp and barf are <kbd>ctrl+alt+right</kbd> and
<kbd>ctrl+alt+left</kbd>.

Slurp and barf are the two that change how you write code. Say you have:

```clojure
(+ 1 2) 3
```

Cursor inside the `(+ 1 2)`, slurp forward, and the parenthesis reaches right to
swallow the `3`:

```clojure
(+ 1 2 3)
```

Barf forward spits it back out. Growing and shrinking a form is a
single keystroke rather than a hunt for the closing bracket.

<div class="callout callout-exercise">
<p class="callout-title">Exercise</p>

Type `(map inc [1 2 3]) (println "done")` on one line. Using only slurp, barf,
splice and raise — no typing of brackets — turn it into:

```clojure
(println (map inc [1 2 3]))
```

<details>
<summary>Show answer</summary>

Put the cursor inside `(println "done")` and delete `"done"`, leaving
`(map inc [1 2 3]) (println)`. Move the cursor into `(println)` and **slurp
backward** (<kbd>ctrl+alt+shift+left</kbd>) to pull the map form in.

If you started the other way round — cursor in the `(map ...)` form — **raise**
is the shortcut: it replaces an enclosing form with the current one, which is
how you throw away a wrapper you no longer want.

The general lesson: when you catch yourself about to hand-balance brackets,
there is a Paredit command that does it without risk.

</details>

</div>

## 6. Instrumenting a function

Calva has a real step debugger, and it is worth knowing about before you need
it — `println` debugging in a lazy language will mislead you, for reasons the
laziness chapter gets into.

<div class="callout callout-repl">
<p class="callout-title">Try it in the REPL</p>

Put the cursor in a `defn` and press <kbd>ctrl+alt+c</kbd> <kbd>i</kbd> to
**instrument** it. Calva places invisible breakpoints at each logical step
inside the function. Call the function and execution pauses at the first one,
with VS Code's debugger controls (continue, step over, step in, step out)
driving it from there.

Equivalently, write `#dbg` immediately before the form and evaluate it with
<kbd>alt+enter</kbd>. For a single breakpoint rather than the whole function,
write `#break` before the form you care about.

</div>

Conditional breakpoints work by attaching metadata, for example
`^{:break/when (= i 7)}` before a `#break` — handy when the tenth iteration is
the one misbehaving.

## Check it works

<div class="callout callout-exercise">
<p class="callout-title">Exercise</p>

Open `src/tenfold/day1.clj`, jack in, and evaluate the whole file. Then, in the
REPL, confirm you get `10`:

```clojure
(reduce + [1 2 3 4])
```

Now break it on purpose: evaluate `(reduce + )`. Read the error. Get used to
reading these now, while you know what caused them — Clojure's stack traces are
a genuine obstacle, and they do not get friendlier later.

<details>
<summary>Show answer</summary>

`(reduce + )` gives an `ArityException`: `Wrong number of args (1) passed to:
clojure.core/reduce`. The useful habit is reading the *last* part of the
message — the thing being called and the arity it got — rather than the stack
trace above it.

Hold on to that habit. An `ArityException` from a function you did not call
directly is exactly the error waiting for you in
[CollFold and the two-arity combinef](../day2/07-collfold.md), and there the
call is buried inside the reducers library.

</details>

</div>
