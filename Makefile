# tenfold — see AGENTS.md for conventions.

MDBOOK_VERSION := 0.5.4
QUIZ_VERSION   := 0.5.0

.DEFAULT_GOAL := help
.PHONY: help serve build clean repl tools

help:  ## Show this help
	@grep -hE '^[a-z-]+:.*##' $(MAKEFILE_LIST) \
		| sed -e 's/:.*##/\t/' -e 's/^/  make /'

serve:  ## Live-preview the book at http://localhost:3000
	mdbook serve book --open

build:  ## Build the book into book/book (what CI runs)
	mdbook build book

clean:  ## Remove generated output
	mdbook clean book
	rm -rf book/src/quiz

repl:  ## Start a Clojure REPL with src/ on the classpath
	clj

tools:  ## Install mdBook and mdbook-quiz at the versions CI pins
	cargo install mdbook --locked --version $(MDBOOK_VERSION)
	cargo install mdbook-quiz --locked --version $(QUIZ_VERSION)
