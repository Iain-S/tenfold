#!/usr/bin/env python3
"""Reflow Markdown to one sentence per line, with no hard wrapping.

Sentences are the unit of a line: paragraphs and list items are unwrapped, then
split at sentence boundaries. Nothing else is touched — code fences, tables, raw
HTML, headings and mdBook {{#include}} / {{#quiz}} directives pass through
verbatim, and the rendered HTML is unchanged.

    $ python3 scripts/mdfmt.py --check FILE...   # non-zero exit if unformatted
    $ python3 scripts/mdfmt.py FILE...           # rewrite in place

Usually run via `make fmt` / `make fmt-check`, or the pre-commit hook in
.githooks/. See AGENTS.md.
"""
import re, sys

ABBREV = r'(?<!\be\.g)(?<!\bi\.e)(?<!\betc)(?<!\bcf)(?<!\bvs)(?<!\bMr)(?<!\bMrs)(?<!\bDr)(?<!\bSt)(?<!\bNo)(?<!\bFig)(?<!\bapprox)'
# split after . ? ! (plus optional closing quote/bracket), before an opening-ish char
SPLIT = re.compile(ABBREV + r'(?<=[.?!])(["\'’”\)\]]*)\s+(?=["\'“‘\*_\[(]*(?:`|<[a-z]|[A-Z0-9]))')

FENCE = re.compile(r'^\s*(```|~~~)')
LIST = re.compile(r'^(\s*)([-*+]|\d+[.)])(\s+)(.*)$')
# lines that are never merged into a paragraph
ATOMIC = re.compile(r'^\s*(#|\||>|<|\{\{#|---\s*$|\*\*\*\s*$|___\s*$|\[\^|\[[^\]]+\]:)')
# ...except when the '<' opens an inline tag: `<kbd>ctrl+enter</kbd> evaluates`
# is prose, not a raw-HTML block, so it reflows like any other sentence.
INLINE_TAG = re.compile(r'^\s*</?(kbd|code|em|strong|a|b|i|u|sup|sub|abbr|var|samp)\b', re.I)


def atomic(line):
    return bool(ATOMIC.match(line)) and not INLINE_TAG.match(line)

def split_sentences(text):
    parts, last = [], 0
    for m in SPLIT.finditer(text):
        # never break inside an inline code span: an odd number of backticks
        # before this point means we are inside one
        if text.count('`', 0, m.start()) % 2:
            continue
        end = m.end(1)
        parts.append(text[last:end])
        last = m.end()
    parts.append(text[last:])
    return [p for p in (p.strip() for p in parts) if p]

def flush_para(buf, out, indent='', first_prefix=None):
    if not buf:
        return
    joined = ' '.join(l.strip() for l in buf)
    sentences = split_sentences(joined)
    for i, s in enumerate(sentences):
        if i == 0 and first_prefix is not None:
            out.append(first_prefix + s)
        else:
            out.append(indent + s)
    buf.clear()

def process(text):
    lines = text.split('\n')
    out, i, in_fence, fence_tok = [], 0, False, None
    para, item = [], None   # item = (indent, first_prefix)

    def flush():
        nonlocal item
        if item is not None:
            flush_para(para, out, item[0], item[1])
            item = None
        else:
            flush_para(para, out)

    while i < len(lines):
        line = lines[i]
        if in_fence:
            out.append(line)
            if FENCE.match(line) and line.strip().startswith(fence_tok):
                in_fence = False
            i += 1
            continue
        m = FENCE.match(line)
        if m:
            flush()
            in_fence, fence_tok = True, m.group(1)
            out.append(line)
            i += 1
            continue
        if not line.strip():
            flush()
            out.append('')
            i += 1
            continue
        if atomic(line):
            flush()
            out.append(line)
            i += 1
            continue
        lm = LIST.match(line)
        if lm:
            flush()
            lead, marker, gap, rest = lm.groups()
            item = (lead + ' ' * (len(marker) + len(gap)), lead + marker + gap)
            para = [rest]
            i += 1
            continue
        # continuation line
        para.append(line)
        i += 1
    flush()
    return '\n'.join(out)


def main(argv):
    check = '--check' in argv
    paths = [a for a in argv if a != '--check']
    offenders = []
    for path in paths:
        with open(path) as f:
            src = f.read()
        new = process(src)
        if new == src:
            continue
        if check:
            offenders.append(path)
        else:
            with open(path, 'w') as f:
                f.write(new)
            print('reflowed', path)
    if offenders:
        print('Not one sentence per line:', file=sys.stderr)
        for p in offenders:
            print('  ' + p, file=sys.stderr)
        print('\nRun `make fmt` to fix.', file=sys.stderr)
        return 1
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
