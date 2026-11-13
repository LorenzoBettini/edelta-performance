#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")/.."
mkdir -p results
out="results/environment.txt"
{
  echo "Edelta performance experiment environment"
  echo "UTC date: $(date -u '+%Y-%m-%dT%H:%M:%SZ')"
  echo "System: $(uname -a)"
  echo
  echo "Java version:"
  java -version 2>&1
  echo
  echo "Java VM properties:"
  java -XshowSettings:properties -version 2>&1 | grep -E '^ *(java\.home|java\.version|java\.vm\.name|os\.arch|os\.name|os\.version) *=' || true
  echo
  echo "Processor information:"
  if command -v lscpu >/dev/null 2>&1; then
    lscpu
  elif command -v sysctl >/dev/null 2>&1; then
    sysctl -n machdep.cpu.brand_string 2>/dev/null || true
    sysctl -n hw.ncpu 2>/dev/null || true
  fi
  echo
  echo "Memory information:"
  if command -v free >/dev/null 2>&1; then
    free -h
  elif command -v sysctl >/dev/null 2>&1; then
    sysctl -n hw.memsize 2>/dev/null || true
  fi
  echo
  echo "JMH options read from the launch scripts (CLI overrides remain possible):"
  for script in scripts/run-jmh.sh scripts/run-jmh-scaling.sh scripts/run-jmh-additions.sh scripts/run-jmh-ordering.sh scripts/run-jmh-real-example.sh; do
    [ -f "$script" ] || continue
    printf '%s: ' "$script"
    # The launcher options, not the Java annotation defaults, govern these runs.
    awk '/-t 1 -f / { gsub(/^[[:space:]]+|[[:space:]]*\\$/, ""); print; found=1; exit } END { if (!found) print "(not found)" }' "$script"
  done
  echo "Configured Java heap: -Xms512m -Xmx4g"
} > "$out"
printf 'Wrote %s\n' "$out"
