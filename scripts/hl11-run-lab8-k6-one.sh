#!/usr/bin/env bash
# Запускать на hl11. Один прогон k6 lab8 (POST CRUD + GET additional) с сохранением summary и опциональным дампом observability.
# Перед запуском: положи lab8-load-test.js и run-lab8-k6-server.sh в K6_DIR (или экспортируй K6_DIR на каталог с этими файлами).
set -euo pipefail

export CRUD_BASE_URL="${CRUD_BASE_URL:-http://10.60.3.36:8082}"
export ADDITIONAL_BASE_URL="${ADDITIONAL_BASE_URL:-http://10.60.3.36:8083}"
export TARGET_VUS="${TARGET_VUS:-30}"
export POST_POOL_RATIO="${POST_POOL_RATIO:-0.5}"
export HTTP_TIMEOUT="${HTTP_TIMEOUT:-300s}"
export DUMP_OBSERVABILITY="${DUMP_OBSERVABILITY:-1}"

K6_DIR="${K6_DIR:-$HOME/lab6_k6_results_tospe}"
RUNNER="${LAB8_RUNNER:-$K6_DIR/run-lab8-k6-server.sh}"

if [[ ! -f "$RUNNER" ]]; then
  echo "Нет $RUNNER. Задай K6_DIR или скопируй run-lab8-k6-server.sh и lab8-load-test.js из репозитория." >&2
  exit 1
fi

export SUMMARY_EXPORT_PATH="${SUMMARY_EXPORT_PATH:-$HOME/lab8_k6_results_tospe/summary-$(date -u +%Y%m%dT%H%M%SZ)-vus${TARGET_VUS}-r${POST_POOL_RATIO}.json}"
mkdir -p "$(dirname "$SUMMARY_EXPORT_PATH")"

echo "K6_DIR=$K6_DIR"
echo "SUMMARY_EXPORT_PATH=$SUMMARY_EXPORT_PATH"
echo "CRUD_BASE_URL=$CRUD_BASE_URL ADDITIONAL_BASE_URL=$ADDITIONAL_BASE_URL"

cd "$K6_DIR"
bash "$RUNNER" 2>&1 | tee "${SUMMARY_EXPORT_PATH%.json}.log"
echo "Summary: $SUMMARY_EXPORT_PATH"
