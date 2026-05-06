#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
SET_CPU="$REPO_ROOT/scripts/hl06-docker-set-cpus.sh"
RUN_K6="$REPO_ROOT/scripts/hl11-run-lab13-k6-direct.sh"
SEED_REMOTE="$REPO_ROOT/scripts/hl06-seed-for-load-tests.sh"

CPUS=(0.5 1.0)
RATIOS=(0.05 0.5 0.95)

OUT_DIR="${OUT_DIR:-$HOME/lab13_cpu_ratio_matrix}"
TARGET_VUS="${TARGET_VUS:-30}"
SPRING_KAFKA_LISTENER_CONCURRENCY="${SPRING_KAFKA_LISTENER_CONCURRENCY:-2}"
SLEEP_AFTER_SEED_SEC="${SLEEP_AFTER_SEED_SEC:-5}"
SEED_BETWEEN_RUNS="${SEED_BETWEEN_RUNS:-1}"

export OUT_DIR TARGET_VUS SPRING_KAFKA_LISTENER_CONCURRENCY
mkdir -p "$OUT_DIR"

run_seed() {
  if [[ "$SEED_BETWEEN_RUNS" != "1" && "$SEED_BETWEEN_RUNS" != "true" ]]; then
    return 0
  fi
  if [[ ! -f "$SEED_REMOTE" ]]; then
    echo "Не найден seed-скрипт: $SEED_REMOTE" >&2
    return 1
  fi
  echo "--- hl06: clear + seed, sleep ${SLEEP_AFTER_SEED_SEC}s ---"
  bash "$SEED_REMOTE"
  sleep "$SLEEP_AFTER_SEED_SEC"
}

echo "OUT_DIR=$OUT_DIR"
echo "TARGET_VUS=$TARGET_VUS, concurrency=$SPRING_KAFKA_LISTENER_CONCURRENCY"
echo "CPUS=${CPUS[*]}, RATIOS=${RATIOS[*]}"

for cpu in "${CPUS[@]}"; do
  echo "=== CPU $cpu ==="
  bash "$SET_CPU" "$cpu"

  for ratio in "${RATIOS[@]}"; do
    run_seed
    export CPU_LABEL="$cpu"
    export POST_POOL_RATIO="$ratio"
    safe_ratio="${ratio//./p}"
    export SUMMARY_EXPORT_PATH="$OUT_DIR/summary-cpu${cpu}-r${safe_ratio}-c${SPRING_KAFKA_LISTENER_CONCURRENCY}-vus${TARGET_VUS}-$(date -u +%Y%m%dT%H%M%SZ).json"
    echo "--- run: cpu=$cpu ratio=$ratio summary=$SUMMARY_EXPORT_PATH ---"
    bash "$RUN_K6"
  done
done

echo "Done. Results in: $OUT_DIR"
