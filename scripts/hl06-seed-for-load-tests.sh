#!/usr/bin/env bash
# Запускать на hl11. На hl06: полная очистка БД через API + seed (по умолчанию: 200 users, 150 lessons, 400 progress).
# Очистка: один вызов lab5_seed.py --endpoint all --clear (порядок progress → users → lessons внутри Python),
# с отдельным длинным таймаутом HL06_CLEAR_TIMEOUT — иначе на большой БД таймаутится только users/clear.
# Требует на hl06: репозиторий и .venv с pip install -r scripts/requirements.txt
# Передача на удалёнку через bash -s (устойчиво к CRLF в файле, в отличие от bash -lc "…").
set -euo pipefail

REPO="${HL06_REPO:-/home/hl/sirius_project_programming}"
PY="${HL06_PYTHON:-.venv/bin/python}"
BASE_URL_SEED="${HL06_SEED_BASE_URL:-http://localhost:8082}"
CLEAR_TIMEOUT="${HL06_CLEAR_TIMEOUT:-900}"
SEED_TIMEOUT="${HL06_SEED_TIMEOUT:-300}"
USERS="${SEED_USERS:-200}"
LESSONS="${SEED_LESSONS:-150}"
PROGRESS="${SEED_PROGRESS:-400}"

SSH_KEY="${HL06_SSH_KEY:-$HOME/.ssh/id_ed25519_hl11_to_hl06}"
HL06="${HL06_TARGET:-hl@10.60.3.36}"
if [[ ! -f "$SSH_KEY" ]]; then
  echo "Нет файла ключа: $SSH_KEY" >&2
  exit 1
fi

REPO_Q=$(printf '%q' "$REPO")
PY_Q=$(printf '%q' "$PY")
BASE_Q=$(printf '%q' "$BASE_URL_SEED")
CLEAR_Q=$(printf '%q' "$CLEAR_TIMEOUT")
SEED_Q=$(printf '%q' "$SEED_TIMEOUT")
USERS_Q=$(printf '%q' "$USERS")
LESSONS_Q=$(printf '%q' "$LESSONS")
PROGRESS_Q=$(printf '%q' "$PROGRESS")

ssh -i "$SSH_KEY" \
  -o StrictHostKeyChecking=accept-new \
  -o BatchMode=yes \
  -o ConnectTimeout=20 \
  "$HL06" bash -s <<ENDSSH
set -euo pipefail
cd $REPO_Q
if [[ ! -x $PY_Q ]]; then
  echo "Нет $PY в $REPO — создай venv: python3 -m venv .venv && .venv/bin/pip install -r scripts/requirements.txt" >&2
  exit 1
fi
echo "Full clear (timeout ${CLEAR_TIMEOUT}s)..."
$PY_Q scripts/lab5_seed.py --base-url $BASE_Q --timeout $CLEAR_Q --endpoint all --clear
echo "Seeding users/lessons/progress (timeout ${SEED_TIMEOUT}s each)..."
$PY_Q scripts/lab5_seed.py --base-url $BASE_Q --timeout $SEED_Q --endpoint users --count $USERS_Q
$PY_Q scripts/lab5_seed.py --base-url $BASE_Q --timeout $SEED_Q --endpoint lessons --count $LESSONS_Q
$PY_Q scripts/lab5_seed.py --base-url $BASE_Q --timeout $SEED_Q --endpoint progress --count $PROGRESS_Q
echo "Seed done: users=$USERS lessons=$LESSONS progress=$PROGRESS"
ENDSSH
