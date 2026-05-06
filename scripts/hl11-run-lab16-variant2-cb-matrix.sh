#!/usr/bin/env bash
set -euo pipefail

# LAB16 вариант 2: CircuitBreaker — три прогона с разным waitDurationInOpenState (5s / 10s / 20s).
# Перед КАЖДЫМ прогоном на кластере выставь переменную окружения на Additional и перезапусти деплой:
#   kubectl set env deploy/hl06-additional -n hl06 \
#     APP_LAB16_MODE=CIRCUIT_BREAKER \
#     RESILIENCE4J_CIRCUITBREAKER_INSTANCES_CRUD_WAITDURATIONINOPENSTATE=10s
#   kubectl rollout restart deploy/hl06-additional -n hl06 && kubectl rollout status deploy/hl06-additional -n hl06 --timeout=180s
#
# На hl11: proxy + этот скрипт; при запросе нажми Enter между шагами или выставь WAIT_SECS заранее и закомментируй read.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
RUN_K6="$REPO_ROOT/scripts/hl11-run-lab13-k6-direct.sh"
SEED_REMOTE="$REPO_ROOT/scripts/hl06-seed-for-load-tests.sh"

WAIT_LIST="${WAIT_LIST:-5 10 20}"
RATIOS=(0.95 0.50 0.05)
TARGET_VUS="${TARGET_VUS:-30}"
REPEATS="${REPEATS:-2}"
SEED_BETWEEN_RUNS="${SEED_BETWEEN_RUNS:-1}"
SLEEP_AFTER_SEED_SEC="${SLEEP_AFTER_SEED_SEC:-5}"

export TARGET_VUS REPEATS

run_seed() {
  if [[ "$SEED_BETWEEN_RUNS" != "1" && "$SEED_BETWEEN_RUNS" != "true" ]]; then
    return 0
  fi
  echo "--- clear + seed, sleep ${SLEEP_AFTER_SEED_SEC}s ---"
  bash "$SEED_REMOTE"
  sleep "$SLEEP_AFTER_SEED_SEC"
}

for w in $WAIT_LIST; do
  OUT_DIR="${OUT_DIR_BASE:-$HOME/lab16_matrix_cb}/wait-${w}s"
  mkdir -p "$OUT_DIR"
  export OUT_DIR
  echo ""
  echo "========== CB waitDurationInOpenState=${w}s -> OUT_DIR=$OUT_DIR =========="
  echo "Убедись, что на additional выставлено RESILIENCE4J_CIRCUITBREAKER_INSTANCES_CRUD_WAITDURATIONINOPENSTATE=${w}s и APP_LAB16_MODE=CIRCUIT_BREAKER"
  read -r -p "Нажми Enter когда кластер обновлён..."

  for ((pass = 1; pass <= REPEATS; pass++)); do
    for ratio in "${RATIOS[@]}"; do
      run_seed
      export POST_POOL_RATIO="$ratio"
      safe_ratio="${ratio//./p}"
      export SUMMARY_EXPORT_PATH="$OUT_DIR/summary-lab16-v2-cb${w}s-pass${pass}-r${safe_ratio}-vus${TARGET_VUS}-$(date -u +%Y%m%dT%H%M%SZ).json"
      echo "--- run: wait=${w}s pass=$pass ratio=$ratio ---"
      bash "$RUN_K6"
    done
  done
done

echo "Done."
