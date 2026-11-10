#!/usr/bin/env python3
"""Run from the repo root before committing Kotlin edits.
Re-anchor ktlint baseline entries after edits: map each recorded line through the HEAD->worktree
diff; entries whose line changed or vanished are dropped (the new code there must be clean)."""
import difflib, re, subprocess, sys, os
BASE = 'app/config/ktlint/baseline.xml'
s = open(BASE).read()
def remap(rel, body):
    path = 'app/' + rel
    try:
        old = subprocess.check_output(['git', 'show', 'HEAD:' + path], stderr=subprocess.DEVNULL).decode().splitlines()
    except subprocess.CalledProcessError:
        return body
    if not os.path.exists(path):
        return None
    new = open(path).read().splitlines()
    if old == new:
        return body
    m = {}
    for tag, i1, i2, j1, j2 in difflib.SequenceMatcher(None, old, new, autojunk=False).get_opcodes():
        if tag == 'equal':
            for k in range(i2 - i1):
                m[i1 + k + 1] = j1 + k + 1
    out, dropped = [], 0
    for line in body.splitlines(keepends=True):
        mm = re.search(r'<error line="(\d+)"', line)
        if not mm:
            out.append(line); continue
        o = int(mm.group(1))
        if o in m:
            out.append(line.replace(f'line="{o}"', f'line="{m[o]}"', 1))
        else:
            dropped += 1
    print(f'{rel}: dropped {dropped}', file=sys.stderr)
    return ''.join(out)
def sub(mt):
    rel, body = mt.group(1), mt.group(2)
    nb = remap(rel, body)
    if nb is None:
        print(f'{rel}: file removed', file=sys.stderr); return ''
    return f'<file name="{rel}">{nb}</file>'
s = re.sub(r'<file name="([^"]+)">(.*?)</file>', sub, s, flags=re.S)
s = re.sub(r'\n\s*\n(\s*<file|\s*</baseline)', r'\n\1', s)
open(BASE, 'w').write(s)
