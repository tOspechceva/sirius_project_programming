#!/usr/bin/env bash
# Запускать на hl11. Матрица нагрузки LAB8:
#   CPU контейнеров на hl06: 0.5 и 1.0 (через hl06-docker-set-cpus.sh)
#   POST_POOL_RATIO: 0.05 (≈5% POST / 95% GET), 0.5, 0.95 (≈95% POST / 5% GET)
#
# Перед запуском: k6 в PATH; ключ SSH на hl06; на hl06 запущен compose.
#
# Переопределения (пример):
#   export TARGET_VUS=40
#   export STEADY_DURATION=60s
#   export OUT_DIR="$HOME/lab8_matrix_results"
#   export DUMP_OBSERVABILITY=0
#   export K6_NO_THRESHOLDS=0   # по умолчанию 1 — иначе k6 выходит с ошибкой на пороге p95 и матрица обрывается
#   export SEED_BETWEEN_RUNS=0   # по умолчанию 1 — перед каждым k6: hl06-seed-for-load-tests.sh (полный clear + seed на hl06)
#   export HL06_CLEAR_TIMEOUT=900   # DELETE .../clear на большой БД (см. hl06-seed-for-load-tests.sh)
#   export HL06_SEED_TIMEOUT=300    # таймаут POST при создании сущностей
#   export SLEEP_AFTER_SEED_SEC=5
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

SET_CPU="${REPO_ROOT}/scripts/hl06-docker-set-cpus.sh"
K6_DIRECT="${REPO_ROOT}/scripts/hl11-run-lab8-k6-direct.sh"
SEED_REMOTE="${REPO_ROOT}/scripts/hl06-seed-for-load-tests.sh"

if [[ ! -f "$SET_CPU" ]]; then
  echo "Не найден $SET_CPU" >&2
  exit 1
fi
if [[ ! -f "$K6_DIRECT" ]]; then
  echo "Не найден $K6_DIRECT" >&2
  exit 1
fi
if ! command -v k6 >/dev/null 2>&1; then
  echo "k6 не найден в PATH" >&2
  exit 1
fi

export K6_DIR="${K6_DIR:-$REPO_ROOT/k6}"
export CRUD_BASE_URL="${CRUD_BASE_URL:-http://10.60.3.36:8082}"
export ADDITIONAL_BASE_URL="${ADDITIONAL_BASE_URL:-http://10.60.3.36:8083}"
export TARGET_VUS="${TARGET_VUS:-30}"
export START_VUS="${START_VUS:-1}"
export RAMP_UP="${RAMP_UP:-20s}"
export STEADY_DURATION="${STEADY_DURATION:-40s}"
export RAMP_DOWN="${RAMP_DOWN:-15s}"
export SLEEP_SECONDS="${SLEEP_SECONDS:-0.2}"
export HTTP_TIMEOUT="${HTTP_TIMEOUT:-300s}"
export DUMP_OBSERVABILITY="${DUMP_OBSERVABILITY:-0}"
# Иначе первый же прогон при медленном p(95) завершает k6 с кодом 99 и цикл матрицы обрывается (set -e + pipefail в direct).
export K6_NO_THRESHOLDS="${K6_NO_THRESHOLDS:-1}"

SEED_BETWEEN_RUNS="${SEED_BETWEEN_RUNS:-1}"
SLEEP_AFTER_SEED_SEC="${SLEEP_AFTER_SEED_SEC:-5}"

OUT_DIR="${OUT_DIR:-$HOME/lab8_cpu_post_get_matrix}"
mkdir -p "$OUT_DIR"

CPUS=(0.5 1.0)
RATIOS=(0.05 0.5 0.95)

echo "OUT_DIR=$OUT_DIR"
echo "TARGET_VUS=$TARGET_VUS POST ratios: ${RATIOS[*]} CPUs: ${CPUS[*]}"
echo "K6_NO_THRESHOLDS=$K6_NO_THRESHOLDS SEED_BETWEEN_RUNS=$SEED_BETWEEN_RUNS"

run_seed() {
  if [[ "$SEED_BETWEEN_RUNS" != "1" && "$SEED_BETWEEN_RUNS" != "true" ]]; then
    return 0
  fi
  if [[ ! -f "$SEED_REMOTE" ]]; then
    echo "Пропуск seed: нет $SEED_REMOTE" >&2
    return 0
  fi
  echo "--- hl06: clear + seed (CRUD / hl-module1-app), затем пауза ${SLEEP_AFTER_SEED_SEC}s ---"
  bash "$SEED_REMOTE"
  sleep "$SLEEP_AFTER_SEED_SEC"
}

for cpu in "${CPUS[@]}"; do
  echo "======== CPU $cpu (hl06 docker update) ========"
  bash "$SET_CPU" "$cpu"

  for ratio in "${RATIOS[@]}"; do
    ts="$(date -u +%Y%m%dT%H%M%SZ)"
    safe_ratio="${ratio//./p}"
    export POST_POOL_RATIO="$ratio"
    export SUMMARY_EXPORT_PATH="${OUT_DIR}/summary-cpu${cpu}-r${safe_ratio}-vus${TARGET_VUS}-${ts}.json"
    echo ">>> POST_POOL_RATIO=$ratio -> $SUMMARY_EXPORT_PATH"
    run_seed
    bash "$K6_DIRECT"
  done
done

echo "Готово. Артефакты в: $OUT_DIR"
