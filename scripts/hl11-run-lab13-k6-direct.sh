#!/usr/bin/env bash
set -euo pipefail

if ! command -v k6 >/dev/null 2>&1; then
  echo "k6 не найден в PATH" >&2
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
K6_SCRIPT="${K6_LAB13_SCRIPT:-$REPO_ROOT/k6/lab13-load-test.js}"

if [[ ! -f "$K6_SCRIPT" ]]; then
  echo "Нет k6 скрипта: $K6_SCRIPT" >&2
  exit 1
fi

export PROXY_BASE_URL="${PROXY_BASE_URL:-http://127.0.0.1:18081}"
export ADDITIONAL_BASE_URL="${ADDITIONAL_BASE_URL:-http://10.60.3.36:8083}"
export TARGET_VUS="${TARGET_VUS:-30}"
export START_VUS="${START_VUS:-1}"
export POST_POOL_RATIO="${POST_POOL_RATIO:-0.5}"
export RAMP_UP="${RAMP_UP:-20s}"
export STEADY_DURATION="${STEADY_DURATION:-40s}"
export RAMP_DOWN="${RAMP_DOWN:-15s}"
export SLEEP_SECONDS="${SLEEP_SECONDS:-0.2}"
export HTTP_TIMEOUT="${HTTP_TIMEOUT:-300s}"

OUT_DIR="${OUT_DIR:-$HOME/lab13_k6_results}"
mkdir -p "$OUT_DIR"
export SUMMARY_EXPORT_PATH="${SUMMARY_EXPORT_PATH:-$OUT_DIR/summary-$(date -u +%Y%m%dT%H%M%SZ)-cpu${CPU_LABEL:-na}-c${SPRING_KAFKA_LISTENER_CONCURRENCY:-na}.json}"

k6 run "$K6_SCRIPT" \
  --summary-export "$SUMMARY_EXPORT_PATH" \
  -e PROXY_BASE_URL="$PROXY_BASE_URL" \
  -e ADDITIONAL_BASE_URL="$ADDITIONAL_BASE_URL" \
  -e TARGET_VUS="$TARGET_VUS" \
  -e START_VUS="$START_VUS" \
  -e POST_POOL_RATIO="$POST_POOL_RATIO" \
  -e RAMP_UP="$RAMP_UP" \
  -e STEADY_DURATION="$STEADY_DURATION" \
  -e RAMP_DOWN="$RAMP_DOWN" \
  -e SLEEP_SECONDS="$SLEEP_SECONDS" \
  -e HTTP_TIMEOUT="$HTTP_TIMEOUT" \
  2>&1 | tee "${SUMMARY_EXPORT_PATH%.json}.log"

echo "Summary: $SUMMARY_EXPORT_PATH"
