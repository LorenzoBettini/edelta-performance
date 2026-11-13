#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")/.."
mkdir -p results
# Compare read-only and modifying references under identical settings.
java -Xms512m -Xmx4g -jar target/benchmarks.jar \
  EdeltaCompilationBenchmark \
  -t 1 -f 3 -wi 8 -w 2s -i 5 -r 2s \
  -prof gc \
  -p classes=50,250 \
  -p references=0,10,50 \
  -p additions=20 \
  -p order=references-first \
  -p referenceKind=read,write \
  -rf json -rff results/jmh-scaling.json \
  "$@"
