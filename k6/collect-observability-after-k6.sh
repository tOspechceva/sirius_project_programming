#!/usr/bin/env bash
set -euo pipefail

# Сохраняет JSON с /api/observability для CRUD и additional-service (LAB9).
# Запускать с машины, откуда доступны базовые URL (например hl11 после прогона k6).

CRUD_BASE_URL="${CRUD_BASE_URL:?set CRUD_BASE_URL e.g. http://10.60.3.36:8082}"
ADDITIONAL_BASE_URL="${ADDITIONAL_BASE_URL:?set ADDITIONAL_BASE_URL e.g. http://10.60.3.36:8083}"
OUT_DIR="${OUT_DIR:-.}"
STAMP="${STAMP:-$(date -u +%Y%m%dT%H%M%SZ)}"

mkdir -p "$OUT_DIR"

curl -sS "${CRUD_BASE_URL}/api/observability" -o "${OUT_DIR}/observability-crud-${STAMP}.json"
curl -sS "${ADDITIONAL_BASE_URL}/api/observability" -o "${OUT_DIR}/observability-additional-${STAMP}.json"

echo "Wrote:"
echo "  ${OUT_DIR}/observability-crud-${STAMP}.json"
echo "  ${OUT_DIR}/observability-additional-${STAMP}.json"
