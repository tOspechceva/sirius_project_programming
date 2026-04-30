#!/usr/bin/env bash
# Запускать на hl11. На hl06: clear + seed (по умолчанию как в автопрогонах: 200 users, 150 lessons, 400 progress).
# Требует на hl06: репозиторий и .venv с pip install -r scripts/requirements.txt
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# Путь на hl06 (не используй только "~/" в переменной для ssh — лучше абсолютный путь).
REPO="${HL06_REPO:-/home/hl/sirius_project_programming}"
PY="${HL06_PYTHON:-.venv/bin/python}"
BASE_URL_SEED="${HL06_SEED_BASE_URL:-http://localhost:8082}"
TIMEOUT="${HL06_SEED_TIMEOUT:-180}"
USERS="${SEED_USERS:-200}"
LESSONS="${SEED_LESSONS:-150}"
PROGRESS="${SEED_PROGRESS:-400}"

"$SCRIPT_DIR/ssh-hl06-from-hl11.sh" bash -lc "
set -euo pipefail
cd \"${REPO}\"
if [[ ! -x ${PY} ]]; then
  echo \"Нет ${PY} в ${REPO} — создай venv: python3 -m venv .venv && .venv/bin/pip install -r scripts/requirements.txt\" >&2
  exit 1
fi
${PY} scripts/lab5_seed.py --base-url ${BASE_URL_SEED} --timeout ${TIMEOUT} --endpoint users --clear --count ${USERS}
${PY} scripts/lab5_seed.py --base-url ${BASE_URL_SEED} --timeout ${TIMEOUT} --endpoint lessons --clear --count ${LESSONS}
${PY} scripts/lab5_seed.py --base-url ${BASE_URL_SEED} --timeout ${TIMEOUT} --endpoint progress --clear --count ${PROGRESS}
echo \"Seed done: users=${USERS} lessons=${LESSONS} progress=${PROGRESS}\"
"
