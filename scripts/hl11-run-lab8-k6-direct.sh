#!/usr/bin/env bash
# Запускать на hl11, если k6 установлен локально (без Docker).
# Нужны lab8-load-test.js и каталог для summary.
set -euo pipefail

if ! command -v k6 >/dev/null 2>&1; then
  echo "k6 не найден в PATH. Установи k6 или используй hl11-run-lab8-k6-one.sh с Docker." >&2
  exit 1
fi

export CRUD_BASE_URL="${CRUD_BASE_URL:-http://10.60.3.36:8082}"
export ADDITIONAL_BASE_URL="${ADDITIONAL_BASE_URL:-http://10.60.3.36:8083}"
export TARGET_VUS="${TARGET_VUS:-30}"
export POST_POOL_RATIO="${POST_POOL_RATIO:-0.5}"
export HTTP_TIMEOUT="${HTTP_TIMEOUT:-300s}"
export DUMP_OBSERVABILITY="${DUMP_OBSERVABILITY:-1}"

K6_DIR="${K6_DIR:-$HOME/lab6_k6_results_tospe}"
SCRIPT="${K6_LAB8_SCRIPT:-$K6_DIR/lab8-load-test.js}"

export SUMMARY_EXPORT_PATH="${SUMMARY_EXPORT_PATH:-$HOME/lab8_k6_results_tospe/summary-$(date -u +%Y%m%dT%H%M%SZ)-vus${TARGET_VUS}-r${POST_POOL_RATIO}.json}"
mkdir -p "$(dirname "$SUMMARY_EXPORT_PATH")"

if [[ ! -f "$SCRIPT" ]]; then
  echo "Нет $SCRIPT. Задай K6_DIR / K6_LAB8_SCRIPT." >&2
  exit 1
fi

cd "$K6_DIR"
k6 run "$SCRIPT" \
  --summary-export "$SUMMARY_EXPORT_PATH" \
  -e CRUD_BASE_URL="$CRUD_BASE_URL" \
  -e ADDITIONAL_BASE_URL="$ADDITIONAL_BASE_URL" \
  -e TARGET_VUS="$TARGET_VUS" \
  -e START_VUS="${START_VUS:-1}" \
  -e POST_POOL_RATIO="$POST_POOL_RATIO" \
  -e RAMP_UP="${RAMP_UP:-20s}" \
  -e STEADY_DURATION="${STEADY_DURATION:-40s}" \
  -e RAMP_DOWN="${RAMP_DOWN:-15s}" \
  -e SLEEP_SECONDS="${SLEEP_SECONDS:-0.2}" \
  -e HTTP_TIMEOUT="$HTTP_TIMEOUT" \
  -e DUMP_OBSERVABILITY="$DUMP_OBSERVABILITY" \
  -e OBS_DUMP_MAX_CHARS="${OBS_DUMP_MAX_CHARS:-16000}" \
  2>&1 | tee "${SUMMARY_EXPORT_PATH%.json}.log"

echo "Summary: $SUMMARY_EXPORT_PATH"
