# Edelta performance benchmarks

Standalone Java/Maven benchmarks for **Edelta 3.13.0**, using the released
Maven Central artifacts (no Edelta source checkout required).

The project measures headless Edelta resource loading, interpretation, linking,
and validation, comparing the standard interpreter (`normal`) with a configuration
that skips interpretation (`nop`). The latter is a cost baseline, **not** a
semantically equivalent compiler. It also measures allocated memory (JMH GC
profiler) and approximate post-GC retained heap (separate JVMs).

Synthetic workloads vary Ecore class count, `ecoreref` occurrences, class additions,
read-only versus modifying references, and statement order. The 16 bundled Edelta
programs and their Ecore inputs are benchmarked with the normal interpreter only.
These inputs are in `src/main/resources/real-examples/`; the `cases.tsv` manifest
lists their IDs and model files. The input sources are kept unchanged.

Results represent **warm, headless processing cycles**, not Eclipse editor
response times. JMH reports milliseconds and allocated bytes per operation;
retained-heap measurements report an approximate used-heap difference after GC.

## Requirements

- Java 21+ and network access for the initial Maven dependency download.
- Maven wrapper (`./mvnw`; no separate Maven installation required).
- Python 3 (standard library only) for CSV summaries and source checks.
- macOS or Linux with POSIX shell scripts.

The run scripts use JMH 1.37, **8 x 2-second warmup iterations**, **5 x 2-second
measurement iterations**, 3 JVM forks, 1 thread, the GC allocation profiler,
and `-Xms512m -Xmx4g`.

## Build and verify

```sh
./mvnw clean verify
java -cp target/benchmarks.jar org.example.edeltabench.SmokeMain
java -cp target/benchmarks.jar org.example.edeltabench.RealExampleSmokeMain --all
python3 scripts/verify-real-examples.py
./scripts/record-environment.sh
```

## Run all benchmark suites

Each launcher writes raw results to `results/`, and the corresponding Python
command writes a summary CSV. The JMH suites can take considerable time.

```sh
# 1. Small synthetic workloads
./scripts/run-jmh.sh
python3 scripts/summarize_jmh.py results/jmh-realistic.json results/summary-realistic.csv

# 2. Synthetic scaling (classes and reference count)
./scripts/run-jmh-scaling.sh
python3 scripts/summarize_jmh.py results/jmh-scaling.json results/summary-scaling.csv

# 3. Number of added classes
./scripts/run-jmh-additions.sh
python3 scripts/summarize_jmh.py results/jmh-additions.json results/summary-additions.csv

# 4. Statement order (references-first, additions-first, interleaved)
./scripts/run-jmh-ordering.sh
python3 scripts/summarize_jmh.py results/jmh-ordering.json results/summary-ordering.csv

# 5. All 16 bundled Edelta programs, normal interpreter
./scripts/run-jmh-all-real-examples.sh
python3 scripts/summarize-real-example.py results/jmh-all-real-examples.json results/summary-all-real-examples.csv

# 6. Retained-heap matrix (fresh JVM for each measurement)
./scripts/run-retained-heap-matrix.sh
python3 scripts/summarize_retained_heap.py results/retained-heap-matrix.csv results/retained-heap-summary.csv

# 7. Retained-heap statement ordering
./scripts/run-retained-heap-ordering.sh
python3 scripts/summarize_retained_heap.py results/retained-heap-ordering.csv results/retained-heap-ordering-summary.csv
```

The retained-heap launchers also save individual CSV files per workload.
The environment report is generated at `results/environment.txt`.
Scripts overwrite their corresponding output files when rerun.

To inspect the bundled source cases, run:

```sh
java -cp target/benchmarks.jar org.example.edeltabench.RealExampleSmokeMain --list
```

Benchmark source code and supplementary inputs are included here; measured
results are not bundled with the project archive.
