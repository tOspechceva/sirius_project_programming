#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
PROXY_DIR="$REPO_ROOT/kafka-proxy"

PROXY_PORT="${PROXY_PORT:-18081}"
KAFKA_BOOTSTRAP_SERVERS="${KAFKA_BOOTSTRAP_SERVERS:-hl15.zil:9094,hl14.zil:9094}"
KAFKA_TOPIC="${KAFKA_TOPIC:-hl06}"

python3 -m venv "$PROXY_DIR/.venv"
"$PROXY_DIR/.venv/bin/pip" install -r "$PROXY_DIR/requirements.txt"

export PROXY_PORT
export KAFKA_BOOTSTRAP_SERVERS
export KAFKA_TOPIC

"$PROXY_DIR/.venv/bin/python" "$PROXY_DIR/app.py"
