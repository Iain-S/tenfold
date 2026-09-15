# Colophon

Built with [mdBook](https://rust-lang.github.io/mdBook/) and
[mdbook-quiz](https://github.com/cognitive-engineering-lab/mdbook-quiz), the
quiz preprocessor written by the Cognitive Engineering Lab at Brown for their
[experimental edition of the Rust book](https://rust-book.cs.brown.edu/).

The quizzes here follow that lab's own finding that conceptual questions — *why*
something behaves as it does — discriminate between understanding and
recognition far better than recall questions do.

## Sources

- Paul Butcher, *Seven Concurrency Models in Seven Weeks*, Pragmatic Bookshelf.
  Page references are to the P1.0 PDF.
- Rich Hickey, [Reducers — A Library and Model for Collection
  Processing](https://clojure.org/news/2012/05/08/reducers) (2012).
- Daniel Higginbotham, [Clojure for the Brave and
  True](https://www.braveclojure.com/).
- The `clojure.core.reducers` source.

Measurements were taken on an Apple Silicon Mac with 10 cores, Clojure 1.12.1,
Temurin JDK. Any timing in this book states its hardware; none should be
believed about yours.
