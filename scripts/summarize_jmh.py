#!/usr/bin/env python3
"""Summarize JMH timing and allocation measurements into CSV."""
import csv
import json
import sys
from pathlib import Path


def summarize(input_path: Path, output_path: Path):
    benchmarks = json.loads(input_path.read_text(encoding="utf-8"))
    by_case = {}
    for measurement in benchmarks:
        if not measurement["benchmark"].endswith("EdeltaCompilationBenchmark.compileAndValidate"):
            continue
        parameters = measurement["params"]
        key = (int(parameters["classes"]), int(parameters["references"]),
               int(parameters["additions"]), parameters.get("referenceKind", "write").lower(),
               parameters.get("order", "references-first").lower())
        mode = parameters["mode"].lower()
        score = measurement["primaryMetric"]["score"]
        unit = measurement["primaryMetric"]["scoreUnit"]
        if unit != "ms/op":
            raise ValueError(f"Unexpected time unit {unit}: adjust parser")
        secondary = measurement.get("secondaryMetrics", {})
        allocated = secondary.get("gc.alloc.rate.norm", {}).get("score")
        if mode in by_case.setdefault(key, {}):
            raise ValueError(f"Duplicate JMH result: {key}, {mode}")
        by_case[key][mode] = (score, allocated)

    output_path.parent.mkdir(parents=True, exist_ok=True)
    with output_path.open("w", newline="", encoding="utf-8") as f:
        writer = csv.writer(f)
        writer.writerow(("classes", "references", "additions", "reference_kind", "statement_order",
                         "normal_ms", "nop_ms", "time_ratio", "time_delta_ms",
                         "normal_alloc_B_per_op", "nop_alloc_B_per_op",
                         "alloc_ratio", "alloc_delta_B_per_op"))
        for key, modes in sorted(by_case.items()):
            if "normal" not in modes or "nop" not in modes:
                print(f"Skipping unpaired workload: {key}", file=sys.stderr)
                continue
            normal_t, normal_a = modes["normal"]
            nop_t, nop_a = modes["nop"]
            time_ratio = normal_t / nop_t if nop_t else ""
            alloc_ratio = (normal_a / nop_a) if normal_a is not None and nop_a else ""
            alloc_delta = ((normal_a - nop_a)
                           if normal_a is not None and nop_a is not None else "")
            writer.writerow((*key, normal_t, nop_t, time_ratio, normal_t - nop_t,
                             normal_a if normal_a is not None else "",
                             nop_a if nop_a is not None else "", alloc_ratio, alloc_delta))
    print(f"Wrote {output_path}")


if __name__ == "__main__":
    if len(sys.argv) not in (2, 3):
        sys.exit("Usage: python3 scripts/summarize_jmh.py results/jmh-extended.json "
                 "[results/summary-extended.csv]")
    source = Path(sys.argv[1])
    if len(sys.argv) > 2:
        target = Path(sys.argv[2])
    elif source.name == "jmh.json":
                target = source.with_name("summary-original-v2.csv")
    else:
        target = source.with_name(source.stem.replace("jmh", "summary", 1) + ".csv")
    summarize(source, target)
