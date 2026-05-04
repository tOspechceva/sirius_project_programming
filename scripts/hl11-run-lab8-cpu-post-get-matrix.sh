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
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

SET_CPU="${REPO_ROOT}/scripts/hl06-docker-set-cpus.sh"
K6_DIRECT="${REPO_ROOT}/scripts/hl11-run-lab8-k6-direct.sh"

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

OUT_DIR="${OUT_DIR:-$HOME/lab8_cpu_post_get_matrix}"
mkdir -p "$OUT_DIR"

CPUS=(0.5 1.0)
RATIOS=(0.05 0.5 0.95)

echo "OUT_DIR=$OUT_DIR"
echo "TARGET_VUS=$TARGET_VUS POST ratios: ${RATIOS[*]} CPUs: ${CPUS[*]}"

for cpu in "${CPUS[@]}"; do
  echo "======== CPU $cpu (hl06 docker update) ========"
  bash "$SET_CPU" "$cpu"

  for ratio in "${RATIOS[@]}"; do
    ts="$(date -u +%Y%m%dT%H%M%SZ)"
    safe_ratio="${ratio//./p}"
    export POST_POOL_RATIO="$ratio"
    export SUMMARY_EXPORT_PATH="${OUT_DIR}/summary-cpu${cpu}-r${safe_ratio}-vus${TARGET_VUS}-${ts}.json"
    echo ">>> POST_POOL_RATIO=$ratio -> $SUMMARY_EXPORT_PATH"
    bash "$K6_DIRECT"
  done
done

echo "Готово. Артефакты в: $OUT_DIR"
