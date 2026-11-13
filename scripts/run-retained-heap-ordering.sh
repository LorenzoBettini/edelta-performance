#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")/.."
# Dedicated ordering matrix, one fresh JVM per NORMAL/NOP measurement.
CLASSES=${CLASSES:-50}
REFERENCES=${REFERENCES:-10}
ADDITIONS=${ADDITIONS:-5}
RUNS=${RUNS:-5}
KINDS=${KINDS:-'read write'}
ORDERS=${ORDERS:-'references-first additions-first interleaved'}
mkdir -p results
output="results/retained-heap-ordering.csv"
printf '%s\n' 'timestamp,mode,classes,references,additions,reference_kind,baseline_bytes,after_bytes,delta_bytes,statement_order' > "$output"
for kind in $KINDS; do
  for order in $ORDERS; do
    ./scripts/run-retained-heap.sh "$CLASSES" "$REFERENCES" "$ADDITIONS" "$RUNS" "$kind" "$order"
    file="results/retained-heap-${kind}-${CLASSES}-${REFERENCES}-${ADDITIONS}-order-${order}.csv"
    sed -n '2,$p' "$file" >> "$output"
  done
done
printf 'Wrote %s\n' "$output"
