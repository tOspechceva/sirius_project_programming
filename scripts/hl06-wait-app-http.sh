#!/usr/bin/env bash
# Запускать на hl11. Ждёт ответ CRUD на hl06 (с hl11 по сети).
set -euo pipefail

BASE_URL="${CRUD_BASE_URL:-http://10.60.3.36:8082}"
PATH_CHECK="${HEALTH_PATH:-/api/users}"
MAX_SEC="${WAIT_MAX_SEC:-120}"
SLEEP="${WAIT_SLEEP_SEC:-2}"

echo "Ожидание ${BASE_URL}${PATH_CHECK} (до ${MAX_SEC}s)..."
for ((i = 0; i < MAX_SEC; i += SLEEP)); do
  if curl -sf "${BASE_URL}${PATH_CHECK}" >/dev/null; then
    echo "OK"
    exit 0
  fi
  echo "… ждём (${i}s)"
  sleep "$SLEEP"
done
echo "Таймаут" >&2
exit 1
