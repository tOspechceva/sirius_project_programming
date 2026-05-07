# LAB16 — crash Core, killer в Additional, resilience4j (Retry / CircuitBreaker)

## Что сделано в коде

### Core Service (`hl-module1`)

- **`POST /api/crash`** — через ~50 ms завершает процесс (`System.exit(1)`). Включение: **`app.crash.enabled`** (в Docker по умолчанию `true`, локально `false`).
- В k8s ConfigMap **`APP_CRASH_ENABLED: "true"`** (`k8s/hl06/common/01-app-configmap.yaml`).

### Additional Service

- Зависимость **`resilience4j-spring-boot3`** (BOM 2.3.0).
- **`Lab16ResilienceExecutor`** — все HTTP-вызовы к Core из `CrudApiClient` проходят через режим:
  - **`NONE`** — без обёртки;
  - **`RETRY`** — retry instance **`crud`**;
  - **`CIRCUIT_BREAKER`** — circuit breaker **`crud`**.
- **`CoreCrashScheduler`** — раз в **`app.lab16.killer-interval-ms`** (по умолчанию 50000 ms) выполняет **`POST {CRUD_BASE_URL}/api/crash`**. Трафик к ClusterIP балансируется между Pod Core при нескольких репликах.
- Включение killer: **`app.lab16.killer-enabled=true`** (`APP_LAB16_KILLER_ENABLED`).

### Конфигурация по умолчанию (`additional` ConfigMap)

В **`k8s/hl06/common/02-additional-configmap.yaml`**: `APP_LAB16_MODE=NONE`, **`APP_LAB16_KILLER_ENABLED=false`** — чтобы случайно не ронять Core.

Для экспериментов LAB16 переопредели переменные (или применить **`k8s/hl06/additional/02-lab16-configmap.yaml`** и добавить `envFrom` в Deployment — см. комментарии в файле).

---

## Вариант 1 — Retry

Цель: параметры retry **перекрывают** простой Core после crash (рестарт Pod).

Рекомендуемые параметры (подправь под фактическое время рестарта):

- `resilience4j.retry.instances.crud.maxAttempts` — достаточно большое число;
- `resilience4j.retry.instances.crud.waitDuration` — пауза между попытками.

Пример через `kubectl` на кластере:

```bash
kubectl set env deploy/hl06-additional -n hl06 \
  APP_LAB16_MODE=RETRY \
  APP_LAB16_KILLER_ENABLED=true \
  RESILIENCE4J_RETRY_INSTANCES_CRUD_MAXATTEMPTS=25 \
  RESILIENCE4J_RETRY_INSTANCES_CRUD_WAITDURATION=3s
kubectl rollout restart deploy/hl06-additional -n hl06
```

Убедись, что **`APP_CRASH_ENABLED=true`** у Core.

Нагрузка с **hl11** (proxy + k6), профили **95/5, 50/50, 5/95**:

```bash
export OUT_DIR=~/lab16_matrix_v1
export REPEATS=2
export PROXY_BASE_URL=http://127.0.0.1:18081
export ADDITIONAL_BASE_URL=http://<NODE_IP>:31083
export HL06_SEED_BASE_URL=http://<NODE_IP>:31082
export K6_WEB_DASHBOARD=false
export HTTP_TIMEOUT=120s

bash scripts/hl11-run-lab16-variant1-ratio-matrix.sh
```

Ожидание: в summary **`http_req_failed`** ~0, checks по GET 200.

---

## Вариант 2 — CircuitBreaker

Режим **`CIRCUIT_BREAKER`**, параметр **`waitDurationInOpenState`**: **5s**, **10s**, **20s** — три серии замеров.

Перед каждой серией выставь окружение на Additional и перезапусти деплой:

```bash
kubectl set env deploy/hl06-additional -n hl06 \
  APP_LAB16_MODE=CIRCUIT_BREAKER \
  APP_LAB16_KILLER_ENABLED=true \
  RESILIENCE4J_CIRCUITBREAKER_INSTANCES_CRUD_WAITDURATIONINOPENSTATE=10s
kubectl rollout restart deploy/hl06-additional -n hl06
kubectl rollout status deploy/hl06-additional -n hl06 --timeout=180s
```

Интерактивный матричный скрипт (после каждого `WAIT` руками выставь нужное время на кластере):

```bash
export OUT_DIR_BASE=~/lab16_matrix_cb
bash scripts/hl11-run-lab16-variant2-cb-matrix.sh
```

В отчёте для каждого **`waitDurationInOpenState`** приведи из summary:

- **`get_req_duration`** (avg / p95);
- долю неуспешных запросов: **`http_req_failed`** или **`checks`** для GET.

---

## Образы

После изменений пересобери и запушь **`hl-module1:lab16`** и **`additional-service:lab16`**, обнови теги в манифестах при необходимости.

## Графики (вариант 1)

После прогона в `~/lab16_matrix_v1` должны лежать `summary-lab16-v1-pass*-r*.json`. Три PNG (по одному на профиль 95/5, 50/50, 5/95):

```bash
cd ~/sirius_project_programming
python3 scripts/plot-lab16-v1-three-graphs.py
# или
OUT_DIR=~/lab16_matrix_v1 python3 scripts/plot-lab16-v1-three-graphs.py
```

Файлы: `lab16-v1-graph-r0p95-...`, `r0p50-...`, `r0p05-...` в том же каталоге. Нужен `matplotlib` (venv: `pip install matplotlib`).
