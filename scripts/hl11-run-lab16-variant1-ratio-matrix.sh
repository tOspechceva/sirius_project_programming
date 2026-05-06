#!/usr/bin/env bash
set -euo pipefail

# LAB16 вариант 1: retry на вызовах к Core; killer на стороне Additional должен быть включён в кластере.
# Перед запуском на hl06 (или через ConfigMap): APP_LAB16_MODE=RETRY, APP_LAB16_KILLER_ENABLED=true

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
RUN_K6="$REPO_ROOT/scripts/hl11-run-lab13-k6-direct.sh"
SEED_REMOTE="$REPO_ROOT/scripts/hl06-seed-for-load-tests.sh"

RATIOS=(0.95 0.50 0.05)
VARIANT="${VARIANT:-lab16-v1}"
OUT_DIR="${OUT_DIR:-$HOME/lab16_matrix_${VARIANT}}"
TARGET_VUS="${TARGET_VUS:-30}"
REPEATS="${REPEATS:-2}"
SEED_BETWEEN_RUNS="${SEED_BETWEEN_RUNS:-1}"
SLEEP_AFTER_SEED_SEC="${SLEEP_AFTER_SEED_SEC:-5}"

export OUT_DIR TARGET_VUS REPEATS VARIANT
mkdir -p "$OUT_DIR"

run_seed() {
  if [[ "$SEED_BETWEEN_RUNS" != "1" && "$SEED_BETWEEN_RUNS" != "true" ]]; then
    return 0
  fi
  echo "--- clear + seed, sleep ${SLEEP_AFTER_SEED_SEC}s ---"
  bash "$SEED_REMOTE"
  sleep "$SLEEP_AFTER_SEED_SEC"
}

echo "OUT_DIR=$OUT_DIR VARIANT=$VARIANT TARGET_VUS=$TARGET_VUS REPEATS=$REPEATS"

for ((pass = 1; pass <= REPEATS; pass++)); do
  for ratio in "${RATIOS[@]}"; do
    run_seed
    export POST_POOL_RATIO="$ratio"
    safe_ratio="${ratio//./p}"
    export SUMMARY_EXPORT_PATH="$OUT_DIR/summary-${VARIANT}-pass${pass}-r${safe_ratio}-vus${TARGET_VUS}-$(date -u +%Y%m%dT%H%M%SZ).json"
    echo "--- run: pass=$pass ratio=$ratio summary=$SUMMARY_EXPORT_PATH ---"
    bash "$RUN_K6"
  done
done

echo "Done. Results in: $OUT_DIR"
