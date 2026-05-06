#!/usr/bin/env bash
set -euo pipefail

# LAB15: те же профили, что LAB14 (k6 + clear/seed), итоги складываются в OUT_DIR с меткой VARIANT.
# VARIANT=v1 — без HPA (или HPA удалён). VARIANT=v2 — после применения HPA.
#
# Пример:
#   export VARIANT=v1 OUT_DIR=~/lab15_matrix_v1 REPEATS=2
#   bash scripts/hl11-run-lab15-ratio-matrix.sh
#
#   kubectl apply -f k8s/hl06/app/11-hpa.yaml -f k8s/hl06/additional/11-hpa.yaml
#   export VARIANT=v2 OUT_DIR=~/lab15_matrix_v2 REPEATS=2
#   bash scripts/hl11-run-lab15-ratio-matrix.sh

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
RUN_K6="$REPO_ROOT/scripts/hl11-run-lab13-k6-direct.sh"
SEED_REMOTE="$REPO_ROOT/scripts/hl06-seed-for-load-tests.sh"

RATIOS=(0.95 0.50 0.05)
VARIANT="${VARIANT:-v1}"
OUT_DIR="${OUT_DIR:-$HOME/lab15_matrix_${VARIANT}}"
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
  if [[ ! -f "$SEED_REMOTE" ]]; then
    echo "Seed script not found: $SEED_REMOTE" >&2
    return 1
  fi
  echo "--- clear + seed, sleep ${SLEEP_AFTER_SEED_SEC}s ---"
  bash "$SEED_REMOTE"
  sleep "$SLEEP_AFTER_SEED_SEC"
}

echo "OUT_DIR=$OUT_DIR"
echo "VARIANT=$VARIANT TARGET_VUS=$TARGET_VUS, REPEATS=$REPEATS"
echo "RATIOS=${RATIOS[*]}"

for ((pass = 1; pass <= REPEATS; pass++)); do
  for ratio in "${RATIOS[@]}"; do
    run_seed
    export POST_POOL_RATIO="$ratio"
    safe_ratio="${ratio//./p}"
    export SUMMARY_EXPORT_PATH="$OUT_DIR/summary-lab15-${VARIANT}-pass${pass}-r${safe_ratio}-vus${TARGET_VUS}-$(date -u +%Y%m%dT%H%M%SZ).json"
    echo "--- run: variant=$VARIANT pass=$pass ratio=$ratio summary=$SUMMARY_EXPORT_PATH ---"
    bash "$RUN_K6"
  done
done

echo "Done. Results in: $OUT_DIR"
