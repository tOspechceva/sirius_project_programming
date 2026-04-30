# LAB9 — Observability (scheduled), тайминги, логи и нагрузка

## Что реализовано в коде

### 1. `ObservabilityService` (основной сервис и `additional-service`)

- **Scheduled** пересчёт снимков: `@Scheduled(fixedDelayString = "${observability.tick-ms:1000}")`, метод `refresh()`.
- **Скользящие окна** настраиваются строкой `observability.windows` (по умолчанию `10s,30s,1m`). Поддерживаются суффиксы `ms`, `s`, `m`.
- Для каждой операции (имя строки, задаётся в `timed` / `timedDb` / `timedS2s` / `timedCalc`) в каждом окне считаются: `count`, `errors`, `avgMs`, `minMs`, `maxMs`, `rps`.
- **Логи приложения**: после каждого `refresh()` на уровне `INFO` пишется строка  
  `[<spring.application.name>] observability refresh: ...`  
  с компактным представлением всех окон (если не отключено свойствами ниже).

### 2. Свойства (оба сервиса)

| Свойство | Назначение |
|----------|------------|
| `spring.application.name` | префикс в логе observability |
| `observability.windows` | окна, например `10s,30s,1m` |
| `observability.tick-ms` | период `refresh`, мс |
| `observability.log-on-refresh` | включить/выключить лог после `refresh` |
| `observability.log-empty-snapshots` | если `false`, не логировать тик, когда во всех окнах нет событий (`count=0`) |

Переменные окружения (пример): `OBSERVABILITY_WINDOWS`, `OBSERVABILITY_TICK_MS`, `OBSERVABILITY_LOG_ON_REFRESH`, `OBSERVABILITY_LOG_EMPTY_SNAPSHOTS`, `SPRING_APPLICATION_NAME`.

### 3. Тайминги (имена операций в статистике)

**Основной сервис (`hl-module1`):**

- Контроллеры: `UserController`, `LessonController`, `ProgressController` — префикс `controller:...`.
- Контроллер `ObservabilityController` — `controller:observability:get`, `controller:observability:getAllWindows`.
- БД: `CourseProgressService` — `timedDb("db:...", ...)`.
- Расчёт прогресса в основном сервисе: `timedCalc("calc:main:user-progress")`, `timedCalc("calc:main:all-users-progress")`.

**Additional service:**

- `AdditionalProgressController` — `controller:getUserProgress`, `controller:getAllUsersProgress`.
- `ObservabilityController` — как у основного сервиса.
- Вызовы CRUD по HTTP: `CrudApiClient` — `s2s:crud:get-users`, `get-lessons`, `get-progress`.
- Расчёт «дополнительной» статистики: `AdditionalProgressService` — `calc:additional:user-progress`, `calc:additional:all-users-progress`.

### 4. HTTP API статистики

- `GET /api/observability` — все окна.
- `GET /api/observability?window=10s` (или `30s`, `1m`) — одно окно.
- `GET /api/observability/windows` — то же, что все окна (удобно для скриптов).

---

## Нагрузка LAB8/LAB9: CPU 0.5 и 1.0, графики и сохранение статистики

### На `hl06` (где крутятся контейнеры)

1. Задать лимит CPU у контейнеров приложений (пример для `hl-module1-app`):

```bash
docker update --cpus 0.5 hl-module1-app
docker update --cpus 0.5 hl-module1-additional-service
```

Повторить эксперимент с `1.0` вместо `0.5`.

2. Убедиться, что сервисы подняты и порты опубликованы (`8082` CRUD, `8083` additional).

### На `hl11` (или другой машине нагрузки)

1. Очистка и сид (при необходимости) — как в LAB5/LAB8.

2. Прогон k6 с дампом observability в консоль (удобно сохранить в файл через `tee`):

```bash
export CRUD_BASE_URL="http://10.60.3.36:8082"
export ADDITIONAL_BASE_URL="http://10.60.3.36:8083"
export DUMP_OBSERVABILITY=1
export TARGET_VUS=30
export POST_POOL_RATIO=0.5
export HTTP_TIMEOUT="300s"
export SUMMARY_EXPORT_PATH="/home/hl/lab9_k6_results/run-cpu05-ratio050.json"

./run-lab8-k6-server.sh 2>&1 | tee "/home/hl/lab9_k6_results/run-cpu05-ratio050.log"
```

В логе будут строки `SNAPSHOT_*`, итоговые метрики k6, а также `OBSERVABILITY_CRUD` и `OBSERVABILITY_ADDITIONAL` с JSON телом ответа `/api/observability`.

3. **Параллельно** на `hl06` в логах контейнеров (`docker compose logs -f app`, `... additional-service`) будут периодические строки  
   `[hl-module1] observability refresh: ...` / `[additional-service] observability refresh: ...`  
   во время нагрузки.

4. **Графики** по времени ответа POST/GET: как в LAB6/LAB8 — из `summary-export` JSON собрать CSV и построить PNG (см. `k6/results` и скрипты агрегации в репозитории). Для каждого CPU и каждого ratio сохранить отдельный `summary` и отдельный `.log` с `tee`.

### Отдельное сохранение JSON observability без k6

```bash
export CRUD_BASE_URL="http://10.60.3.36:8082"
export ADDITIONAL_BASE_URL="http://10.60.3.36:8083"
export OUT_DIR="/home/hl/lab9_k6_results"
bash k6/collect-observability-after-k6.sh
```

---

## Краткий чеклист сдачи LAB9

- [ ] Включены `@EnableScheduling` и свойства `observability.*`.
- [ ] Под нагрузкой в логах видны `observability refresh` с окнами `10s`, `30s`, `1m`.
- [ ] В статистике различаются `controller:*`, `db:*`, `s2s:crud:*`, `calc:main:*`, `calc:additional:*`.
- [ ] Для CPU `0.5` и `1.0` сохранены: k6 summary JSON, лог прогона с `SNAPSHOT_*` и `OBSERVABILITY_*`, графики POST/GET.
