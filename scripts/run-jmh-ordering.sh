#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")/.."
mkdir -p results
# Same statement multiset, three orders: reference-first, addition-first,
# and uniformly interleaved. Read-only cases reveal cache invalidation costs.
# Both NORMAL and NOP are measured under identical conditions.
java -Xms512m -Xmx4g -jar target/benchmarks.jar \
  EdeltaCompilationBenchmark \
  -t 1 -f 3 -wi 8 -w 2s -i 5 -r 2s \
  -prof gc \
  -p classes=50,250 \
  -p references=10 \
  -p additions=5 \
  -p referenceKind=read,write \
  -p order=references-first,additions-first,interleaved \
  -rf json -rff results/jmh-ordering.json \
  "$@"
