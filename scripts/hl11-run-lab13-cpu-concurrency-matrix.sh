#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
SET_CPU="$REPO_ROOT/scripts/hl06-docker-set-cpus.sh"
RUN_K6="$REPO_ROOT/scripts/hl11-run-lab13-k6-direct.sh"

CPUS=(0.5 1.0)
CONCURRENCIES=(1 2)

OUT_DIR="${OUT_DIR:-$HOME/lab13_cpu_concurrency_matrix}"
TARGET_VUS="${TARGET_VUS:-30}"
POST_POOL_RATIO="${POST_POOL_RATIO:-0.5}"
export OUT_DIR TARGET_VUS POST_POOL_RATIO

mkdir -p "$OUT_DIR"

for cpu in "${CPUS[@]}"; do
  echo "=== CPU $cpu ==="
  bash "$SET_CPU" "$cpu"

  for c in "${CONCURRENCIES[@]}"; do
    echo "--- concurrency=$c ---"
    export SPRING_KAFKA_LISTENER_CONCURRENCY="$c"
    export CPU_LABEL="$cpu"
    export SUMMARY_EXPORT_PATH="$OUT_DIR/summary-cpu${cpu}-c${c}-vus${TARGET_VUS}-$(date -u +%Y%m%dT%H%M%SZ).json"
    bash "$RUN_K6"
  done
done

echo "Done. Results in: $OUT_DIR"
