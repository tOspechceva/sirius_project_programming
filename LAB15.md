# LAB15 — ResourceQuota, нагрузка v1/v2, HPA, health + probes

## Какой сервер для чего

| Задача | Куда заходить (SSH) |
|--------|----------------------|
| `kubectl apply`, HPA, удалить HPA, смотреть поды/HPA/квоту | **hl16** (master k3s), *или* **hl06**, если на нём настроен рабочий `kubectl` на API кластера (как в инструкции k3s: `server: https://10.60.3.14:6443` в `~/.kube/config`) |
| k6, матрица нагрузки, Kafka REST proxy (`18081`), скрипты из `scripts/` | **hl11** |
| Проверить, что поды тянут образы / логи приложения | **hl06** или **hl16** (где удобнее `kubectl`) |

Вариант 2 = сначала команды **kubectl на hl16 (или hl06)**, затем тот же нагрузочный прогон **на hl11**.

## 1. Resource Quota (6 CPU / 6 Gi)

Манифест: `k8s/hl06/common/04-resourcequota.yaml` (`requests` и `limits` для CPU и памяти).

Проверка:

```bash
kubectl describe resourcequota hl06-quota -n hl06
```

## 2. Health endpoints (Core + Additional)

Оба сервиса используют Spring Boot Actuator:

- `/actuator/health/liveness` — liveness (группа: `livenessState`, `db`, `kafka`)
- `/actuator/health/readiness` — readiness (группа: `readinessState`, `db`, `kafka`)

**Core (`hl-module1`):** PostgreSQL через стандартный индикатор `db`, Kafka — кастомный `KafkaClusterHealthIndicator` (AdminClient → `clusterId`).

**Additional:** то же для своего datasource и Kafka bootstrap (только проверка доступности брокеров; consumer не обязателен).

После сборки образов `lab15` probes настроены в:

- `k8s/hl06/app/10-deployment.yaml`
- `k8s/hl06/additional/10-deployment.yaml`

Дополнительно для Additional в k8s добавлены переменные БД и ConfigMap `hl06-additional-lab-kafka-config`, Secret `hl06-db-secret`.

## 3. Вариант 1 — нагрузка без HPA

Убедитесь, что HPA не применён (или удалён):

```bash
kubectl delete hpa -n hl06 hl06-app-hpa hl06-additional-hpa 2>/dev/null || true
kubectl get deploy -n hl06 hl06-app hl06-additional -o jsonpath='{.items[*].spec.replicas}'; echo
```

На машине с k6 (например `hl11`), с запущенным Kafka proxy:

```bash
export VARIANT=v1
export OUT_DIR=~/lab15_matrix_v1
export REPEATS=2
export PROXY_BASE_URL=http://127.0.0.1:18081
export ADDITIONAL_BASE_URL=http://<NODE_IP>:31083
export HL06_SEED_BASE_URL=http://<NODE_IP>:31082

bash scripts/hl11-run-lab15-ratio-matrix.sh
```

## 4. Вариант 2 — HPA до 3 реплик по CPU

### Шаг A — только kubectl: **hl16** (или **hl06** с рабочим kubeconfig)

На кластере должен быть **metrics-server** (стандарт для HPA по CPU):

```bash
kubectl get apiservice v1beta1.metrics.k8s.io
```

Применить HPA (из каталога репозитория):

```bash
cd ~/sirius_project_programming/hl-module1   # путь поправь под себя

kubectl apply -f k8s/hl06/app/11-hpa.yaml
kubectl apply -f k8s/hl06/additional/11-hpa.yaml
kubectl get hpa -n hl06
kubectl describe hpa -n hl06 hl06-app-hpa
```

В HPA задано `stabilizationWindowSeconds: 20` при scale up (устойчивость перед добавлением реплик), `maxReplicas: 3`, цель CPU ~50% (при необходимости подстройте `averageUtilization`).

### Шаг B — нагрузка: **hl11**

На **hl11** в отдельном терминале держи Kafka proxy (`scripts/hl11-run-lab13-proxy.sh`), затем матрицу варианта 2:

```bash
cd ~/sirius_project_programming/hl-module1

export VARIANT=v2
export OUT_DIR=~/lab15_matrix_v2
export REPEATS=2
export PROXY_BASE_URL=http://127.0.0.1:18081
export ADDITIONAL_BASE_URL=http://<IP_ноды_с_NodePort>:31083
export HL06_SEED_BASE_URL=http://<IP_ноды_с_NodePort>:31082

bash scripts/hl11-run-lab15-ratio-matrix.sh
```

`<IP_ноды_с_NodePort>` — IP ноды, где доступны `31082`/`31083` (как в LAB14; уточни через `kubectl get pods -n hl06 -o wide`).

Опционально во время теста с **hl16** или **hl06**:

```bash
kubectl get pods -n hl06 -w
kubectl get hpa -n hl06
```

## 5. График «6 точек» (3 профиля × 2 варианта)

После того как в `~/lab15_matrix_v1` лежат `summary-lab15-v1-*.json`, а в `~/lab15_matrix_v2` — `summary-lab15-v2-*.json`:

```bash
python3 scripts/plot-lab15-six-points.py
# или
LAB15_DIR_V1=~/lab15_matrix_v1 LAB15_DIR_V2=~/lab15_matrix_v2 \
  LAB15_SIX_POINTS_PNG=~/lab15_six_points_post_p95.png \
  python3 scripts/plot-lab15-six-points.py
```

Столбчатый график: по оси X три профиля POST/GET (95/5, 50/50, 5/95), два столбца на профиль — вариант 1 и вариант 2, метрика — **p95 `post_req_duration`** (среднее по passes, если `REPEATS>1`).

## 6. Образы

В манифестах указан тег `lab15`. Соберите и запушьте образы `hl-module1:lab15` и `additional-service:lab15` в свой registry перед `kubectl apply`.

## 7. Probes: если под `Running`, но `0/1`, рестарты, `503` в событиях

- **Liveness** проверяет только `livenessState` (лёгкий эндпоинт), **readiness** — БД + Kafka. Так kubelet не перезапускает контейнер из‑за долгого ответа health.
- **Startup** использует `/actuator/health/liveness`, таймауты probe увеличены в манифестах.

Если **readiness** всё ещё **503** на одной ноде (например под на `hl06`, а на `hl17` было бы ОК) — проверь доступ **с этой ноды** до `DBHOST` и Kafka bootstrap (маршрутизация/firewall). Вариант: оставить поды на ноде, откуда есть доступ, или исправить сеть.
