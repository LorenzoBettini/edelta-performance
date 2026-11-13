#!/usr/bin/env python3
"""Summarize independent normal/NOP post-GC retained-heap measurements."""
import csv
import statistics
import sys
from collections import defaultdict
from pathlib import Path


def summarize(input_path: Path, output_path: Path):
    groups = defaultdict(lambda: defaultdict(list))
    with input_path.open(newline="", encoding="utf-8") as f:
        for row in csv.DictReader(f):
            key = (int(row["classes"]), int(row["references"]), int(row["additions"]),
                   row.get("reference_kind", "write").lower(),
                   row.get("statement_order", "references-first").lower())
            mode = row["mode"].lower()
            if mode not in ("normal", "nop"):
                raise ValueError(f"Unknown mode {mode}")
            groups[key][mode].append(int(row["delta_bytes"]))

    output_path.parent.mkdir(parents=True, exist_ok=True)
    with output_path.open("w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(("classes", "references", "additions", "reference_kind", "statement_order", "normal_n",
                    "nop_n", "normal_median_B", "nop_median_B", "median_delta_B",
                    "median_ratio", "normal_min_B", "normal_max_B", "nop_min_B", "nop_max_B"))
        for key, by_mode in sorted(groups.items()):
            if "normal" not in by_mode or "nop" not in by_mode:
                print(f"Skipping unpaired heap workload: {key}", file=sys.stderr)
                continue
            normal = by_mode["normal"]
            nop = by_mode["nop"]
            nmed, pmed = statistics.median(normal), statistics.median(nop)
            w.writerow((*key, len(normal), len(nop), nmed, pmed, nmed - pmed,
                        nmed / pmed if pmed else "", min(normal), max(normal),
                        min(nop), max(nop)))
    print(f"Wrote {output_path}")


if __name__ == "__main__":
    if len(sys.argv) not in (2, 3):
        sys.exit("Usage: python3 scripts/summarize_retained_heap.py "
                 "results/retained-heap-matrix.csv [results/retained-heap-summary.csv]")
    inp = Path(sys.argv[1])
    out = Path(sys.argv[2]) if len(sys.argv) == 3 else inp.with_name(inp.stem + "-summary.csv")
    summarize(inp, out)
