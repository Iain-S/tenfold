# Setting up: VS Code and Calva

<div class="callout callout-book">
<p class="callout-title">In the book</p>

Not in the book.
Paul Butcher assumes you have a working Clojure environment and leaves you to it.

</div>

Bindings below are macOS.
Calva's docs list Windows and Linux equivalents where they differ; the big divergence is slurp and barf.

## 1. Install

You need a JVM (any recent one), the Clojure CLI, and VS Code.

```console
$ brew install temurin clojure/tools/clojure
$ clojure --version
```

Then install the **Calva** VSCode extension — search the VS Code marketplace for "Calva: Clojure & ClojureScript Interactive Programming".
Nothing else is required; Calva bundles its own language server and nREPL middleware.

## 2. How a deps.edn project is laid out

Calva expects a normal `deps.edn` project, which is what this repository is:

```text
tenfold/
  deps.edn              # dependencies and source paths
  src/
    tenfold/
      day1.clj                  # namespace tenfold.day1
      day2.clj                  # namespace tenfold.day2
      calva_and_paraedit.clj    # the practice file for this chapter
```

The rule that catches everyone once: **the file path must match the namespace**, and underscores in filenames become hyphens in namespace names.
A namespace `tenfold.mini-reducers` lives in `src/tenfold/mini_reducers.clj`, and the practice file above declares `tenfold.calva-and-paraedit`.
Get this wrong and you get a "namespace not found" that looks nothing like a filename problem.

## 3. Jack in

Open the `tenfold` folder in VS Code, then:

<div class="callout callout-repl">
<p class="callout-title">Try it in the REPL</p>

Open the command palette wit <kbd>cmd+shift+p</kbd> and choose **"Start a Project REPL and Connect"**, which everyone calls *jack-in*.

Calva will ask which project type (choose **deps.edn**) and which aliases to include (none, for now).
A REPL window opens.
You are connected when the status bar shows the connection and the REPL prompt appears.

</div>

Jack-in starts the REPL *for* you with the nREPL dependencies Calva needs already injected.
The alternative — "Connect to a running REPL" — attaches to a REPL you started yourself, which means getting that middleware right by hand.
Use jack-in unless you have a specific reason not to.

With the REPL connected, open `src/tenfold/calva_and_paraedit.clj`.
The rest of this chapter is practised in that file: every command below has a form there waiting for it, in the order they are introduced.

Do not load the file as a whole — evaluate the forms one at a time, as instructed.
Several of them are deliberately broken (`(+ 1 (2) 3)` is not valid Clojure) because fixing them *is* the exercise, so requiring the namespace just throws.

## 4. Evaluating things

This is the whole point.
Put the cursor inside a form and:

| Keys | What it evaluates |
|---|---|
| <kbd>ctrl+enter</kbd> | the **current form** — the innermost one containing the cursor |
| <kbd>alt+enter</kbd> | the **current top-level form** — the whole `(defn ...)` you are inside |
| <kbd>ctrl+shift+enter</kbd> | the **enclosing form** |

`alt+enter` is the one you will use most: it redefines the function you are editing, in the running REPL, without restarting anything.
That is the interactive-programming loop the book is implicitly assuming throughout.

The practice file opens with `(+ (- 1 50) 500)` for exactly this.
Put the cursor on the `(- 1 50)` and press <kbd>ctrl+enter</kbd> to get `-49`, then <kbd>alt+enter</kbd> from the same spot to evaluate the whole top-level form and get `451`.
That is the distinction between the two bindings, in one expression.

Results appear inline next to the form and in the REPL window.
<kbd>ctrl+alt+c</kbd> <kbd>ctrl+c</kbd> copies the last result to the clipboard.

## 5. Four Paredit commands worth learning

Calva edits *structure*, not text.
Fighting it with ordinary text editing is the single most common reason people bounce off Clojure tooling.
Learn these four and the rest can wait.

| Keys (macOS) | Command | What it does |
|---|---|---|
| <kbd>ctrl+alt+.</kbd> | **Slurp forward** | pull the next form *into* the current one |
| <kbd>ctrl+alt+,</kbd> | **Barf forward** | push the last form *out* of the current one |
| <kbd>ctrl+alt+s</kbd> | **Splice** | remove the brackets around the current form |
| <kbd>ctrl+alt+p</kbd> <kbd>ctrl+alt+r</kbd> | **Raise** | replace the enclosing form with the current one |

On Windows and Linux, slurp and barf are <kbd>ctrl+alt+right</kbd> and <kbd>ctrl+alt+left</kbd>.

The practice file has a labelled form for each of the four — `;; slurp forward`, `;; barf foreward`, `;; splice`, `;; raise` — so you can try each command on a form that is already in the wrong shape.

Slurp and barf are the two that change how you write code.
Say you have:

```clojure
(+ 1 2) 3
```

Cursor inside the `(+ 1 2)`, slurp forward, and the parenthesis reaches right to swallow the `3`:

```clojure
(+ 1 2 3)
```

Barf forward spits it back out.
Growing and shrinking a form is a single keystroke rather than a hunt for the closing bracket.

<div class="callout callout-exercise">
<p class="callout-title">Exercise</p>

The last exercise in the practice file is the line `(map inc [1 2 3]) (println "done")`.
Using only slurp, barf, splice and raise — no typing of brackets — turn it into:

```clojure
(println (map inc [1 2 3]))
```

<details>
<summary>Show answer</summary>

Put the cursor inside `(println "done")` and delete `"done"`, leaving `(map inc [1 2 3]) (println)`.
Move the cursor into `(println)` and **slurp backward** (<kbd>ctrl+alt+shift+left</kbd>) to pull the map form in.

If you started the other way round — cursor in the `(map ...)` form — **raise** is the shortcut: it replaces an enclosing form with the current one, which is how you throw away a wrapper you no longer want.

The general lesson: when you catch yourself about to hand-balance brackets, there is a Paredit command that does it without risk.

</details>

</div>

## 6. Instrumenting a function

Calva has a real step debugger, and it is worth knowing about before you need it — `println` debugging in a lazy language will mislead you, for reasons the laziness chapter gets into.

<div class="callout callout-repl">
<p class="callout-title">Try it in the REPL</p>

The practice file has `badfunc`, which divides by zero somewhere inside a four-line arithmetic expression.
Put the cursor in it, press <kbd>ctrl+alt+c</kbd> <kbd>i</kbd> to **instrument** it, then evaluate `(badfunc)`.
Calva places invisible breakpoints at each logical step inside the function, so execution pauses part-way through — with VS Code's debugger controls (continue, step over, step in, step out) driving it from there — and you can watch which sub-expression is the one that throws.

</div>

### `#dbg` and `#break` are not the same thing

Both tags go immediately before a form, and both take effect when you evaluate that form with <kbd>alt+enter</kbd>.
What differs is how much they instrument.

| What you write | What it instruments | Where execution stops |
|---|---|---|
| <kbd>ctrl+alt+c</kbd> <kbd>i</kbd> on a `defn` | the whole function | at every logical step inside it |
| `#dbg` before a form | that whole form | at every logical step inside it |
| `#break` before a form | nothing — it is a single marker | at that one form, once |

`#dbg` is the instrument command in reader-tag form; Calva's documentation says it "has the same effect as using the instrument command".
`#break` is one breakpoint, of the sort you would set by clicking in the gutter in any other debugger.

The reason the two appear to behave identically is the *first* pause.
If you put `#dbg` on the form you were interested in anyway, execution stops there — which is exactly where `#break` would have stopped too.
The difference only shows when you press continue.
With `#dbg` you stop again at the next step inside the instrumented form, and again after that; with `#break` nothing else is marked, so the call runs to completion.

The practice file's `goodfunc` carries `#dbg` on its outer `(+ ...)`.
Continue through it and you pause in turn at `(* 1E1 1E2)`, then `(- 1 2)`, then `(/ 7 1)`.
Move the tag onto just `(* 1E1 1E2)` and make it `#break`, and you get one pause and then the answer.

Reach for `#dbg`, or the instrument command, when you do not yet know *where* a function goes wrong.
Reach for `#break` when you already know, and want the state at that point without stepping through everything leading up to it.

Conditional breakpoints work by attaching metadata, for example `^{:break/when (= i 7)}` before a `#break` — handy when the tenth iteration is the one misbehaving.
