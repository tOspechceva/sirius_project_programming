#!/usr/bin/env bash
# Запускать на hl11. Любая команда выполняется на hl06 по SSH с ключом.
# Пример: ./ssh-hl06-from-hl11.sh 'hostname'
# Пример: ./ssh-hl06-from-hl11.sh bash -lc "cd ~/sirius_project_programming && docker compose ps"
set -euo pipefail

SSH_KEY="${HL06_SSH_KEY:-$HOME/.ssh/id_ed25519_hl11_to_hl06}"
HL06="${HL06_TARGET:-hl@10.60.3.36}"

if [[ ! -f "$SSH_KEY" ]]; then
  echo "Нет файла ключа: $SSH_KEY" >&2
  echo "Задай HL06_SSH_KEY=... или создай ключ на hl11." >&2
  exit 1
fi

exec ssh -i "$SSH_KEY" \
  -o StrictHostKeyChecking=accept-new \
  -o BatchMode=yes \
  -o ConnectTimeout=20 \
  "$HL06" "$@"
