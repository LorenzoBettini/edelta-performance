#!/usr/bin/env python3
"""Check packaged original Edelta/Ecore fixtures and their SHA-256 inventory."""
import csv
import hashlib
from pathlib import Path

root = Path(__file__).resolve().parents[1] / 'src/main/resources/real-examples'
corpus = root / 'source-archive'
manifest = root / 'cases.tsv'
checksums = root / 'source-files.sha256'
ecore_support = root / 'support/EcoreForTests.ecore'

files = {p.relative_to(corpus).as_posix(): p for p in corpus.rglob('*') if p.is_file()}
assert sum(n.endswith('.edelta') for n in files) == 16, 'Expected 16 original Edelta sources'
assert sum(n.endswith('.ecore') for n in files) == 98, 'Expected 98 original Ecore sources'

seen = set()
for line in checksums.read_text(encoding='utf-8').splitlines():
    if not line.strip():
        continue
    digest, path = line.split('  ', 1)
    assert path in files, f'Missing original file: {path}'
    assert hashlib.sha256(files[path].read_bytes()).hexdigest() == digest, f'Modified: {path}'
    seen.add(path)
assert seen == set(files), 'Unaccounted source-archive fixtures'

cases = {}
for line in manifest.read_text(encoding='utf-8').splitlines():
    if not line or line.startswith('#'):
        continue
    case_id, program, models, expected_package, category = line.split('\t')
    assert case_id not in cases, f'Duplicate case ID: {case_id}'
    assert program in files, f'Missing Edelta file for {case_id}'
    assert all(model in files for model in models.split(';')), case_id
    assert expected_package and category in ('migration', 'evolution')
    cases[case_id] = (program, models)
assert len(cases) == 16
assert ecore_support.is_file(), 'Missing the Ecore metamodel for Edelta imports'
assert b'name="ecore"' in ecore_support.read_bytes(), 'Unexpected Ecore support resource'
assert b'name="EString"' in ecore_support.read_bytes(), 'EString is missing from Ecore support'
assert b'name="EInt"' in ecore_support.read_bytes(), 'EInt is missing from Ecore support'
assert {program for program, models in cases.values()} == {
    path for path in files if path.endswith('.edelta')
}
print(f'OK: {len(files)} original source files unchanged; {len(cases)} benchmark cases; Ecore metamodel available')
