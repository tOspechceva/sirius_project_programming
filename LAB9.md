# LAB9 — простыми словами

## В чем суть лабы

В этой лабе мы не пишем новый бизнес-функционал. Мы проверяем, как сервисы ведут себя под нагрузкой, и собираем наблюдаемость (observability):

- сколько запросов проходит;
- сколько ошибок;
- какие задержки (avg/min/max, p95 в k6 summary);
- как это меняется при разных лимитах CPU и разном соотношении чтения/записи.

Главная идея: показать цифрами и графиками, как система работает при разных режимах нагрузки.

---

## Что обязательно сделать

Нужно сделать **6 прогонов**:

- CPU: `0.5` и `1.0`;
- ratio read/write: `50/50`, `95/5`, `5/95`.

Для **каждого** прогона обязательно:

1. очистить и заново заполнить данные (clear + seed);
2. выставить CPU лимит;
3. запустить нагрузку;
4. сохранить summary и лог;
5. сохранить observability JSON.

---

## Что считаем read/write

В текущем k6-сценарии:

- write = POST в CRUD;
- read = GET в additional (который внутри ходит в CRUD).

Пока тестируем старые запросы (как в LAB8/LAB9 сценарии), без переключения на новые endpoint'ы.

---

## Подробно: что именно мы написали в коде для LAB9

Ниже не общая теория, а конкретно наши изменения по классам и методам.

### 1) Общая observability-модель в двух сервисах

Мы добавили одинаковую идею и в `hl-module1`, и в `additional-service`:

- есть сервис `ObservabilityService`;
- каждый важный участок кода оборачивается в `timed(...)` / `timedDb(...)` / `timedS2s(...)` / `timedCalc(...)`;
- при успехе вызывается `recordSuccess(operation, durationNanos)`;
- при ошибке вызывается `recordFailure(operation, durationNanos)`.

В результате каждое действие получает имя операции (например `controller:getAllUsers`, `db:getAllLessons`, `s2s:crud:get-progress`), и дальше мы видим статистику по этим именам.

---

### 2) `ObservabilityService` (в обоих сервисах)

Что делает этот класс:

1. Хранит поток событий (`TimedEvent`) в памяти:
   - время события;
   - имя операции;
   - была ошибка или нет;
   - длительность.
2. По расписанию (`@Scheduled(fixedDelayString = "${observability.tick-ms:1000}")`) вызывает `refresh()`.
3. В `refresh()`:
   - удаляет слишком старые события;
   - пересчитывает статистику для окон `10s`, `30s`, `1m` (или из `observability.windows`);
   - сохраняет агрегат в `snapshots`.
4. Для каждой операции в каждом окне считает:
   - `count`, `errors`, `avgMs`, `minMs`, `maxMs`, `rps`.
5. Логирует компактную строку:
   - `[application-name] observability refresh: ...`
   - лог можно включать/выключать через `observability.log-on-refresh` и `observability.log-empty-snapshots`.

Почему это важно для лабы:

- мы видим не только итог k6, но и внутреннюю структуру времени (контроллер, БД, S2S, вычисления);
- можем сравнить поведение при CPU 0.5 vs 1.0 и разных ratio.

---

### 3) `ObservabilityController` (в обоих сервисах)

Добавлены endpoint'ы:

- `GET /api/observability` — все окна сразу;
- `GET /api/observability?window=10s` — одно окно;
- `GET /api/observability/windows` — тот же полный снимок, удобный для скриптов.

Также сам контроллер тоже обернут в `timed(...)`, поэтому обращения к observability API тоже попадают в статистику.

---

### 4) Где именно добавлены тайминги в основном сервисе (`hl-module1`)

В `hl-module1` метрики размечены по слоям:

- `controller:*` — в REST-контроллерах (`UserController`, `LessonController`, `ProgressController`, `ObservabilityController`);
- `db:*` — операции доступа к БД в `CourseProgressService`;
- `calc:main:*` — вычисления прогресса на стороне main.

Итог: по main-сервису видно, где теряется время — в HTTP-слое, в БД или в расчётах.

---

### 5) Где именно добавлены тайминги в `additional-service`

В `additional-service` размечены:

- `controller:*` — контроллеры additional;
- `s2s:crud:*` — сетевые вызовы в CRUD в `CrudApiClient`;
- `calc:additional:*` — вычисления в `AdditionalProgressService`.

Особенно важно, что `s2s:crud:*` позволяет явно видеть сетевую цену походов из additional в main.

---

### 6) Что мы сделали для демонстрации «плохой логики» (N+1)

Это отдельная часть для лабораторного эксперимента.

Добавили новый endpoint:

- `GET /api/progress/n-plus-one` в `AdditionalProgressController`.

Его бизнес-логика в `AdditionalProgressService.calculateProgressWithNPlusOneLookups()`:

1. Берем список прогресса (`GET /api/progress`).
2. Для **каждой** записи:
   - отдельно вызываем `GET /api/users/{id}`;
   - отдельно вызываем `GET /api/lessons/{id}`.
3. Формируем ответ через `NPlusOneProgressResponse`.

Это намеренно неэффективно: при росте количества записей количество S2S-вызовов растет линейно и быстро становится дорогим.

Чтобы это работало, в `CrudApiClient` добавлены методы:

- `getUserByIdBody(long userId)` -> `GET /api/users/{id}`;
- `getLessonByIdBody(long lessonId)` -> `GET /api/lessons/{id}`.

И добавлен DTO:

- `NPlusOneProgressResponse` (данные пользователя + урока + прогресса в одной строке ответа).

---

### 7) Что делают скрипты, которые мы написали для LAB9

- `scripts/hl06-seed-for-load-tests.sh`  
  Удаленно на hl06 делает clear + seed (`users`, `lessons`, `progress`) перед каждым прогоном.

- `scripts/hl06-docker-set-cpus.sh`  
  Удаленно на hl06 выставляет лимиты CPU контейнерам `hl-module1-app` и `hl-module1-additional-service`.

- `scripts/hl06-wait-app-http.sh`  
  Ждет, пока CRUD endpoint станет доступен (`200`), чтобы не запускать тест в момент старта контейнера.

- `scripts/hl11-run-lab8-k6-direct.sh`  
  Запускает k6 на hl11 без Docker, пишет `summary-export` и `.log`.

- `scripts/hl11-run-lab8-k6-one.sh`  
  Альтернативный запуск через runner-скрипт (если используем Docker-вариант k6).

- `k6/collect-observability-after-k6.sh`  
  Сохраняет `/api/observability` обоих сервисов в JSON-файлы для артефактов отчета.

---

## Пошагово: как запускать

## 1) Подготовка (hl11)

```bash
cd ~/sirius_project_programming
git pull
chmod +x scripts/*.sh k6/collect-observability-after-k6.sh

export HL06_REPO=/home/hl/sirius_project_programming
export CRUD_BASE_URL=http://10.60.3.36:8082
export ADDITIONAL_BASE_URL=http://10.60.3.36:8083
export K6_DIR="$HOME/lab6_k6_results_tospe"
export OUT_DIR="$HOME/lab9_k6_results"
mkdir -p "$OUT_DIR"
```

## 2) Один прогон (шаблон)

```bash
# 1. clear + seed
./scripts/hl06-seed-for-load-tests.sh

# 2. cpu limit на hl06
./scripts/hl06-docker-set-cpus.sh 0.5

# 3. нагрузка
export TARGET_VUS=30
export POST_POOL_RATIO=0.50
export DUMP_OBSERVABILITY=1
export SUMMARY_EXPORT_PATH="$OUT_DIR/summary-cpu0.5-ratio0.50.json"
./scripts/hl11-run-lab8-k6-direct.sh

# 4. observability JSON
STAMP="cpu0.5-ratio0.50-$(date -u +%Y%m%dT%H%M%SZ)"
OUT_DIR="$OUT_DIR" STAMP="$STAMP" bash k6/collect-observability-after-k6.sh
```

## 3) Повторить для всех комбинаций

- CPU `0.5`, ratio `0.50`
- CPU `0.5`, ratio `0.95`
- CPU `0.5`, ratio `0.05`
- CPU `1.0`, ratio `0.50`
- CPU `1.0`, ratio `0.95`
- CPU `1.0`, ratio `0.05`

---

## Что сдаем в итоге

Для каждого из 6 запусков сохраняем:

- `summary-*.json` (k6 summary);
- `summary-*.log` (полный лог прогона);
- `observability-crud-*.json`;
- `observability-additional-*.json`.

Также строим графики (как в LAB6/LAB8):

- отдельно для read/write;
- сравнение CPU `0.5` vs `1.0`;
- сравнение ratio `50/50`, `95/5`, `5/95`.

---

## Мини-чеклист перед отправкой

- [ ] Есть 6 прогонов (2 CPU × 3 ratio)
- [ ] Перед каждым прогоном был clear + seed
- [ ] Есть summary/log для каждого запуска
- [ ] Есть observability JSON для каждого запуска
- [ ] Есть графики по задержкам и/или throughput

