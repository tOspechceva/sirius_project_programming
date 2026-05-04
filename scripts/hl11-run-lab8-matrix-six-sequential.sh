#!/usr/bin/env bash
# Один запуск на hl11: шесть прогонов k6 LAB8 подряд без разветвления:
#   CPU 0.5 → ratio 0.05, 0.5, 0.95 → CPU 1.0 → те же три ratio.
# Перед каждым прогоном: hl06-seed-for-load-tests.sh (полный API clear + seed на hl06).
#
# Переменные как у hl11-run-lab8-cpu-post-get-matrix.sh; по умолчанию:
#   SEED_BETWEEN_RUNS=1, K6_NO_THRESHOLDS=1 (все шесть доходят до конца при успешном seed).
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
exec bash "$SCRIPT_DIR/hl11-run-lab8-cpu-post-get-matrix.sh"
