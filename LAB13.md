# LAB13 — нагрузка LAB12 через Kafka

## Что добавлено

- `k6/lab13-load-test.js` — запись идёт не в CRUD API, а в REST-proxy (`POST /publish`), который публикует в Kafka.
- `kafka-proxy/app.py` — минимальный Flask-прокси: принимает JSON и отправляет в Kafka-топик.
- `kafka-proxy/requirements.txt` — зависимости proxy (`flask`, `kafka-python`).
- `scripts/hl11-run-lab13-proxy.sh` — запуск proxy рядом с k6 (на hl11).
- `scripts/hl11-run-lab13-k6-direct.sh` — прямой запуск k6 для LAB13.
- `scripts/hl11-run-lab13-cpu-concurrency-matrix.sh` — матрица CPU `0.5/1.0` × `concurrency 1/2`.

## Batch listener в основном приложении

- `CourseKafkaListener` переведён на batch: метод принимает `List<String>`.
- `application.properties` и `application-docker.properties`:
  - `spring.kafka.listener.type=batch`
  - `spring.kafka.listener.concurrency=${SPRING_KAFKA_LISTENER_CONCURRENCY:2}`
  - `spring.kafka.consumer.max-poll-records=${SPRING_KAFKA_CONSUMER_MAX_POLL_RECORDS:100}`

## Где запускать

- **hl06**: `hl-module1-app` и `additional-service`.
- **hl11**: `kafka-proxy` и `k6`.
- **hl14/hl15**: Kafka кластер и топик `hl06` (2 партиции).

## Базовый сценарий запуска

### 1) На hl06: задать режим listener и CPU

```bash
cd ~/sirius_project_programming
export SPRING_KAFKA_LISTENER_AUTO_STARTUP=true
export SPRING_KAFKA_LISTENER_CONCURRENCY=1
export SPRING_KAFKA_CONSUMER_MAX_POLL_RECORDS=100
docker compose up -d --force-recreate app additional-service
```

CPU выставляется скриптом `hl06-docker-set-cpus.sh` (одинаково на оба контейнера).

### 2) На hl11: запустить proxy

```bash
cd ~/sirius_project_programming
export KAFKA_BOOTSTRAP_SERVERS="hl15.zil:9094,hl14.zil:9094"
export KAFKA_TOPIC="hl06"
export PROXY_PORT=18081
bash scripts/hl11-run-lab13-proxy.sh
```

### 3) На hl11 в другом окне: k6

```bash
cd ~/sirius_project_programming
export PROXY_BASE_URL="http://127.0.0.1:18081"
export ADDITIONAL_BASE_URL="http://10.60.3.36:8083"
bash scripts/hl11-run-lab13-k6-direct.sh
```

### 4) Полная матрица (CPU × concurrency)

```bash
cd ~/sirius_project_programming
export PROXY_BASE_URL="http://127.0.0.1:18081"
export ADDITIONAL_BASE_URL="http://10.60.3.36:8083"
bash scripts/hl11-run-lab13-cpu-concurrency-matrix.sh
```

## Что снимать в отчёт

- summary-файлы k6 по 4 прогонам (2 CPU × 2 concurrency),
- графики latency/throughput по этим 4 точкам,
- подтверждение, что топик имеет 2 партиции, а listener работает batch-режимом.
