# LAB12 — потребитель Kafka в основном приложении

Реализовано в модуле **`hl-module1`** (не `additional-service`).

## Что сделано в коде

- Зависимость **`spring-boot-starter-kafka`**, **`@EnableKafka`** в `Module1Application`.
- Контракт сообщения: `KafkaCommandEnvelope` — поля **`entity`**, **`operation`**, **`payload`** (JSON-объект).
  - **`entity`**: `USER` | `LESSON` | `PROGRESS`
  - **`operation`**: `POST` | `DEL`
- **`KafkaCommandProcessor`** — маршрутизация на репозитории / `CourseProgressService` (создание пользователя, урока, прогресса; удаление по `id` или паре `userId`/`lessonId`).
- **`CourseKafkaListener`** — `@KafkaListener` на топик из **`app.kafka.topic.commands`** (по умолчанию **`hl06`**), **`concurrency`** из **`spring.kafka.listener.concurrency`** (по умолчанию **2**). Чтобы потоки не простаивали, у топика на кластере должно быть **≥ партиций**, чем `concurrency` (у топика `hl06` на стенде уже **2** партиции).

## Настройки (`application.properties` / `application-docker.properties`)

| Свойство | Смысл |
|----------|--------|
| `spring.kafka.bootstrap-servers` | Брокеры Kafka |
| `spring.kafka.consumer.group-id` | Группа консьюмера |
| `spring.kafka.listener.concurrency` | Параллелизм `@KafkaListener` |
| `spring.kafka.listener.auto-startup` | **`false`** по умолчанию — приложение поднимается без брокера; для работы consumer выставьте **`true`** и корректный bootstrap |
| `app.kafka.topic.commands` | Имя топика команд (по умолчанию `hl06`) |

На стенде с Kafka: задайте переменные окружения, например:

```text
SPRING_KAFKA_BOOTSTRAP_SERVERS=hl15.zil:9094,hl14.zil:9094
SPRING_KAFKA_LISTENER_AUTO_STARTUP=true
APP_KAFKA_TOPIC_COMMANDS=hl06
SPRING_KAFKA_LISTENER_CONCURRENCY=2
```

(При туннеле с ПК подставьте `localhost:19094,localhost:19095` или аналог из LAB11.)

## Примеры JSON

**Создать пользователя**

```json
{"entity":"USER","operation":"POST","payload":{"login":"kafkaUser","email":"kafkaUser@ex.com","registrationDate":"2024-06-01"}}
```

**Удалить пользователя**

```json
{"entity":"USER","operation":"DEL","payload":{"id": 123}}
```

**Создать урок**

```json
{"entity":"LESSON","operation":"POST","payload":{"topic":"Kafka intro","videoDurationMinutes":10,"testName":"quiz","maxTestScore":100}}
```

**Удалить урок**

```json
{"entity":"LESSON","operation":"DEL","payload":{"id": 5}}
```

**Прогресс (отметить урок)**

```json
{"entity":"PROGRESS","operation":"POST","payload":{"userId":1,"lessonId":1,"completionDate":"2024-06-15","testResult":80}}
```

**Удалить запись прогресса**

```json
{"entity":"PROGRESS","operation":"DEL","payload":{"userId":1,"lessonId":1}}
```

## Отправка сообщений

**Console producer** (одна строка JSON на сообщение):

```bash
printf '%s\n' '{"entity":"USER","operation":"POST","payload":{"login":"t1","email":"t1@e.com","registrationDate":"2024-01-01"}}' \
  | kafka-console-producer.sh --topic hl06 --bootstrap-server localhost:9094
```

**Python** (из корня репо, после `pip install -r scripts/requirements.txt`):

```bash
python scripts/lab12_kafka_send.py --bootstrap localhost:9094 --topic hl06 \
  --json '{"entity":"USER","operation":"POST","payload":{"login":"p1","email":"p1@e.com","registrationDate":"2024-02-01"}}'
```

## Локальный Kafka без туннеля

См. `docker-compose` и подсказки в задании LAB12 (hl-module2): заменить advertised hosts на `localhost`, разнести брокеры по портам хоста, выставить `SPRING_KAFKA_BOOTSTRAP_SERVERS` и `SPRING_KAFKA_LISTENER_AUTO_STARTUP=true`.
