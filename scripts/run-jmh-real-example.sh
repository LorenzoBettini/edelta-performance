#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")/.."
mkdir -p results
# Original-source example cases (NORMAL interpreter only).
# First run: java -cp target/benchmarks.jar org.example.edeltabench.RealExampleSmokeMain CASE-ID
# Set REAL_EXAMPLE_RESULT_FILE to keep results from separate selected runs distinct.
OUT="${REAL_EXAMPLE_RESULT_FILE:-results/jmh-real-example.json}"
java -Xms512m -Xmx4g -jar target/benchmarks.jar \
  RealExampleCompilationBenchmark \
  -t 1 -f 3 -wi 8 -w 2s -i 5 -r 2s \
  -prof gc \
  -rf json -rff "$OUT" \
  "$@"
