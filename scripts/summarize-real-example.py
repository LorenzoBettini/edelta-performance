#!/usr/bin/env python3
"""Summarize one or several original-source, NORMAL-only JMH cases."""
import csv
import json
import sys
from pathlib import Path

if len(sys.argv) != 3:
    sys.exit('Usage: python3 scripts/summarize-real-example.py JMH_JSON OUTPUT_CSV')
measurements = json.loads(Path(sys.argv[1]).read_text(encoding='utf-8'))
filtered = [m for m in measurements if m['benchmark'].endswith(
    'RealExampleCompilationBenchmark.compileAndValidate')]
if not filtered:
    sys.exit('No real-example JMH cases in input')
rows = []
for m in filtered:
    if m['primaryMetric']['scoreUnit'] != 'ms/op':
        sys.exit('Unexpected JMH unit: ' + m['primaryMetric']['scoreUnit'])
    example_id = m.get('params', {}).get('example')
    if not example_id:
        sys.exit('Missing original-example case parameter in JMH result')
    rows.append((
        example_id, 'normal', m['primaryMetric']['score'],
        m['primaryMetric'].get('scoreError', ''),
        m.get('secondaryMetrics', {}).get('gc.alloc.rate.norm', {}).get('score', '')
    ))

p = Path(sys.argv[2])
p.parent.mkdir(parents=True, exist_ok=True)
with p.open('w', newline='', encoding='utf-8') as f:
    w = csv.writer(f)
    w.writerow(('workload', 'mode', 'time_ms', 'jmh_error_ms', 'allocated_B_per_op'))
    w.writerows(sorted(rows))
print(f'Wrote {len(rows)} case(s) to {p}')
