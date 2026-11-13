#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")/.."
mkdir -p results
# Isolate the effect of independent class-creation operations.
# The 0-addition cases remain valid because references=10.
java -Xms512m -Xmx4g -jar target/benchmarks.jar \
  EdeltaCompilationBenchmark \
  -t 1 -f 3 -wi 8 -w 2s -i 5 -r 2s \
  -prof gc \
  -p classes=50 \
  -p references=10 \
  -p additions=0,5,20,50 \
  -p order=references-first \
  -p referenceKind=read,write \
  -rf json -rff results/jmh-additions.json \
  "$@"
