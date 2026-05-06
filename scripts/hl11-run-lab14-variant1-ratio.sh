#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
RUN_K6="$REPO_ROOT/scripts/hl11-run-lab13-k6-direct.sh"
SEED_REMOTE="$REPO_ROOT/scripts/hl06-seed-for-load-tests.sh"

# Variant 1: both services are deployed with 1 CPU / 1Gi in k8s manifests.
# Ratios: 95/5, 50/50, 5/95 (POST/GET)
RATIOS=(0.95 0.50 0.05)

OUT_DIR="${OUT_DIR:-$HOME/lab14_variant1_ratio_matrix}"
TARGET_VUS="${TARGET_VUS:-30}"
REPEATS="${REPEATS:-2}"  # 3 ratios x 2 repeats = 6 runs
SEED_BETWEEN_RUNS="${SEED_BETWEEN_RUNS:-1}"
SLEEP_AFTER_SEED_SEC="${SLEEP_AFTER_SEED_SEC:-5}"

export OUT_DIR TARGET_VUS REPEATS
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
echo "TARGET_VUS=$TARGET_VUS, REPEATS=$REPEATS"
echo "RATIOS=${RATIOS[*]}"

for (( pass=1; pass<=REPEATS; pass++ )); do
  for ratio in "${RATIOS[@]}"; do
    run_seed
    export POST_POOL_RATIO="$ratio"
    safe_ratio="${ratio//./p}"
    export SUMMARY_EXPORT_PATH="$OUT_DIR/summary-v1-pass${pass}-r${safe_ratio}-vus${TARGET_VUS}-$(date -u +%Y%m%dT%H%M%SZ).json"
    echo "--- run: pass=$pass ratio=$ratio summary=$SUMMARY_EXPORT_PATH ---"
    bash "$RUN_K6"
  done
done

echo "Done. Results in: $OUT_DIR"
