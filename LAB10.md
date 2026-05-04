# LAB10 — кеш пользователей в Additional service

## Что сделано в коде (репозиторий `additional-service`)

- Класс `UserCacheService`: хранилище `HashMap<Long, User>`, синхронизация одним замком, методы `getUserById`, `warmAll`.
- Периодический лог: `@Scheduled(fixedDelayString = "${user-cache.stats-log-ms:60000}")` → строка вида  
  `[user-cache] size=… hits=… misses=…`.
- Подключение в `AdditionalProgressService`:
  - расчёт по одному пользователю — без загрузки всего списка `/api/users`, данные через кеш / `GET /api/users/{id}`;
  - расчёт по всем пользователям — после `GET /api/users` кеш обновляется (`warmAll`).

Параметры (env / `application.properties`):

- `USER_CACHE_STATS_LOG_MS` — интервал лога статистики кеша (мс), по умолчанию `60000`.
- `USER_CACHE_STATS_LOG_ENABLED` — включить/выключить этот лог (`true` / `false`).

В `docker-compose.yml` для сервиса `additional-service` добавлены переменные `USER_CACHE_STATS_LOG_MS` и `USER_CACHE_STATS_LOG_ENABLED`.

## Снять графики CPU 0.5 и 1.0 и сохранить логи кеша

Предполагается стек на **hl06**, k6 с **hl11**, лимит CPU задаётся скриптом (как в LAB6/LAB9).

### 1. Подготовка

- Образ `additional-service` с изменениями LAB10 собран и запущен в compose.
- Для более частых строк `[user-cache]` на время эксперимента можно задать, например:  
  `export USER_CACHE_STATS_LOG_MS=30000` перед `docker compose up -d` (или в `.env`).

### 2. Прогон при CPU = 0.5

На **hl11** (из каталога с репозиторием `sirius_project_programming`):

```bash
./scripts/hl06-docker-set-cpus.sh 0.5
```

Запусти нагрузку (например lab8 с hl11, см. `scripts/hl11-run-lab8-k6-direct.sh` и переменные `CRUD_BASE_URL` / `ADDITIONAL_BASE_URL` на IP hl06).

Сохрани логи additional-service на **hl06**:

```bash
docker logs hl-module1-additional-service 2>&1 | tee ~/lab10_cpu0.5_additional_logs.txt
```

(или снимай фрагмент после прогона: `docker logs hl-module1-additional-service 2>&1 | grep '\[user-cache\]' | tee ~/lab10_cpu0.5_user_cache.txt`).

**График CPU:** во время прогона в отдельном SSH на hl06:

```bash
docker stats hl-module1-app hl-module1-additional-service --no-stream
```

или непрерывно:

```bash
docker stats hl-module1-app hl-module1-additional-service
```

Сделай скриншот / экспорт (или сохрани вывод в файл `lab10_cpu0.5_docker_stats.txt`). Дополнительно можно приложить summary k6 (`summary-*.json`).

### 3. Прогон при CPU = 1.0

```bash
./scripts/hl06-docker-set-cpus.sh 1.0
```

Повтори k6 с **теми же** параметрами, снова сохрани:

- `~/lab10_cpu1.0_additional_logs.txt` (или только строки `[user-cache]`);
- вывод `docker stats` / скрин для графика нагрузки на CPU.

### 4. Отчёт

Сопоставь для 0.5 и 1.0:

- графики / числа по CPU (из `docker stats` или мониторинга);
- строки `[user-cache] size=… hits=… misses=…` после прогона — по ним видно наполнение кеша и долю попаданий (например при повторных запросах прогресса по одному пользователю).
