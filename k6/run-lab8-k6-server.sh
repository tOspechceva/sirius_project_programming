#!/usr/bin/env bash
set -euo pipefail

# Запуск k6 на сервере (без build/push).
# Скрипт использует существующие контейнеры app/additional-service из docker-compose.

K6_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RESULTS_DIR="$K6_DIR/results"
mkdir -p "$RESULTS_DIR"

CRUD_BASE_URL="${CRUD_BASE_URL:-http://app:8081}"
ADDITIONAL_BASE_URL="${ADDITIONAL_BASE_URL:-http://additional-service:8082}"

TARGET_VUS="${TARGET_VUS:-10}"
START_VUS="${START_VUS:-1}"
POST_POOL_RATIO="${POST_POOL_RATIO:-0.5}"

RAMP_UP="${RAMP_UP:-20s}"
STEADY_DURATION="${STEADY_DURATION:-40s}"
RAMP_DOWN="${RAMP_DOWN:-15s}"
SLEEP_SECONDS="${SLEEP_SECONDS:-0.2}"
HTTP_TIMEOUT="${HTTP_TIMEOUT:-120s}"
DUMP_OBSERVABILITY="${DUMP_OBSERVABILITY:-0}"
OBS_DUMP_MAX_CHARS="${OBS_DUMP_MAX_CHARS:-16000}"

# Попытаемся взять имя docker-network из уже работающего контейнера app.
APP_CONTAINER="${APP_CONTAINER:-hl-module1-app}"
NETWORK_NAME="$(docker inspect -f '{{range $name, $net := .NetworkSettings.Networks}}{{println $name}}{{end}}' "$APP_CONTAINER" 2>/dev/null | sed -n '1p' || true)"
NETWORK_NAME="${NETWORK_NAME:-${DOCKER_NETWORK:-hl-module1_default}}"

SUMMARY_FILE="${SUMMARY_FILE:-summary-vus-${TARGET_VUS}.json}"
SUMMARY_EXPORT_PATH="/scripts/results/${SUMMARY_FILE}"

echo "Using docker network: $NETWORK_NAME"
echo "Running k6 lab8 test: TARGET_VUS=$TARGET_VUS POST_POOL_RATIO=$POST_POOL_RATIO"
echo "Snapshots will be printed in console."
echo "DUMP_OBSERVABILITY=$DUMP_OBSERVABILITY (set 1 for LAB9 JSON lines at end)"

docker run --rm \
  --network "$NETWORK_NAME" \
  -v "${K6_DIR}:/scripts" \
  -e CRUD_BASE_URL="$CRUD_BASE_URL" \
  -e ADDITIONAL_BASE_URL="$ADDITIONAL_BASE_URL" \
  -e TARGET_VUS="$TARGET_VUS" \
  -e START_VUS="$START_VUS" \
  -e POST_POOL_RATIO="$POST_POOL_RATIO" \
  -e RAMP_UP="$RAMP_UP" \
  -e STEADY_DURATION="$STEADY_DURATION" \
  -e RAMP_DOWN="$RAMP_DOWN" \
  -e SLEEP_SECONDS="$SLEEP_SECONDS" \
  -e HTTP_TIMEOUT="$HTTP_TIMEOUT" \
  -e DUMP_OBSERVABILITY="$DUMP_OBSERVABILITY" \
  -e OBS_DUMP_MAX_CHARS="$OBS_DUMP_MAX_CHARS" \
  grafana/k6:0.52.0 \
  run /scripts/lab8-load-test.js \
  --summary-export "$SUMMARY_EXPORT_PATH"

echo "Done. Summary saved to: $SUMMARY_EXPORT_PATH"

