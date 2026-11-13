#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")/.."
# Modest, configurable grid. Each cell is separately measured in fresh JVMs.
# Override with e.g. CLASSES='50' REFERENCES='10 50' RUNS=3.
CLASSES=${CLASSES:-'50 250'}
REFERENCES=${REFERENCES:-'0 10 50'}
ADDITIONS=${ADDITIONS:-5}
RUNS=${RUNS:-5}
KINDS=${KINDS:-'read write'}
mkdir -p results
output="results/retained-heap-matrix.csv"
printf '%s\n' 'timestamp,mode,classes,references,additions,reference_kind,baseline_bytes,after_bytes,delta_bytes,statement_order' > "$output"
for classes in $CLASSES; do
  for references in $REFERENCES; do
    for kind in $KINDS; do
      [ "$references" -eq 0 ] && [ "$ADDITIONS" -eq 0 ] && continue
      ./scripts/run-retained-heap.sh "$classes" "$references" "$ADDITIONS" "$RUNS" "$kind"
      file="results/retained-heap-${kind}-${classes}-${references}-${ADDITIONS}.csv"
      sed -n '2,$p' "$file" >> "$output"
    done
  done
done
printf 'Wrote %s\n' "$output"
