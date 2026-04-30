#!/usr/bin/env bash
# Запускать на hl11. Задаёт лимит CPU контейнерам приложения на hl06.
# Пример: ./hl06-docker-set-cpus.sh 0.5
# Пример: ./hl06-docker-set-cpus.sh 1.0
set -euo pipefail

CPU="${1:?укажи CPU, например 0.5 или 1.0}"

SSH_KEY="${HL06_SSH_KEY:-$HOME/.ssh/id_ed25519_hl11_to_hl06}"
HL06="${HL06_TARGET:-hl@10.60.3.36}"
if [[ ! -f "$SSH_KEY" ]]; then
  echo "Нет файла ключа: $SSH_KEY" >&2
  exit 1
fi

CPU_Q=$(printf '%q' "$CPU")

ssh -i "$SSH_KEY" \
  -o StrictHostKeyChecking=accept-new \
  -o BatchMode=yes \
  -o ConnectTimeout=20 \
  "$HL06" bash -s <<ENDSSH
set -euo pipefail
for c in hl-module1-app hl-module1-additional-service; do
  if docker inspect "\$c" >/dev/null 2>&1; then
    echo "docker update --cpus $CPU_Q \$c"
    docker update --cpus $CPU_Q "\$c"
  else
    echo "контейнер не найден (пропуск): \$c" >&2
  fi
done
docker ps --filter 'name=hl-module1-' --format 'table {{.Names}}\t{{.Status}}'
ENDSSH
