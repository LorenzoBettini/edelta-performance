#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")/.."
mkdir -p results
# Args: [classes] [references] [additions] [fresh-JVM repetitions] [read|write] [optional order]
classes=${1:-250}
references=${2:-20}
additions=${3:-5}
runs=${4:-5}
kind=${5:-read}
order=${6:-references-first}
case "$kind" in
  read|write) ;;
  *) echo "Expected read or write reference kind" >&2; exit 2 ;;
esac
case "$order" in
  references-first|additions-first|interleaved) ;;
  *) echo "Unknown statement order: $order" >&2; exit 2 ;;
esac
output="results/retained-heap-${kind}-${classes}-${references}-${additions}"
# Distinguish ordered workload results with a suffix.
if [ "$#" -ge 6 ]; then output="${output}-order-${order}"; fi
output="${output}.csv"
printf '%s\n' 'timestamp,mode,classes,references,additions,reference_kind,baseline_bytes,after_bytes,delta_bytes,statement_order' > "$output"
i=0
while [ "$i" -lt "$runs" ]; do
  for mode in normal nop; do
    # Fail if the JVM fails; never silently ignore a missing data row.
    lines=$(java -Xms512m -Xmx4g -cp target/benchmarks.jar \
      org.example.edeltabench.RetainedHeapMain \
      "$mode" "$classes" "$references" "$additions" "$kind" "$order")
    row=$(printf '%s\n' "$lines" | grep -E '^[0-9]{4}-[0-9]{2}-[0-9]{2}T' || true)
    [ -n "$row" ] || { echo "Missing retained heap measurement" >&2; exit 1; }
    printf '%s\n' "$row" >> "$output"
  done
  i=$((i + 1))
done
printf 'Wrote %s\n' "$output"
