#!/usr/bin/env bash
# Запускать на hl11. Задаёт лимит CPU контейнерам приложения на hl06.
# Пример: ./hl06-docker-set-cpus.sh 0.5
# Пример: ./hl06-docker-set-cpus.sh 1.0
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CPU="${1:?укажи CPU, например 0.5 или 1.0}"

"$SCRIPT_DIR/ssh-hl06-from-hl11.sh" bash -lc "
set -euo pipefail
for c in hl-module1-app hl-module1-additional-service; do
  if docker inspect \"\$c\" >/dev/null 2>&1; then
    echo \"docker update --cpus $CPU \$c\"
    docker update --cpus \"$CPU\" \"\$c\"
  else
    echo \"контейнер не найден (пропуск): \$c\" >&2
  fi
done
docker ps --format 'table {{.Names}}\t{{.CPUs}}' | head -20
"
