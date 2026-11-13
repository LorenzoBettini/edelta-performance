#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")/.."
mkdir -p results
# Representative modest-size program: variable ref count, few modifications.
# Both reference styles and both interpreter modes use the same JVM/JMH options.
java -Xms512m -Xmx4g -jar target/benchmarks.jar \
  EdeltaCompilationBenchmark \
  -t 1 -f 3 -wi 8 -w 2s -i 5 -r 2s \
  -prof gc \
  -p classes=20,50 \
  -p references=0,5,10 \
  -p additions=5 \
  -p order=references-first \
  -p referenceKind=read,write \
  -rf json -rff results/jmh-realistic.json \
  "$@"
