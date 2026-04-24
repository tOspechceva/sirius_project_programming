# HL Module 1 (LAB2 + LAB3 + LAB4 + LAB5)

REST-сервис "Платформа онлайн-курсов" на Spring Boot.

Проект включает:
- Spring Data JPA + PostgreSQL.
- Flyway-миграции схемы БД.
- Docker Compose (app + db + k6).
- Нагрузочное тестирование k6 с 2 пулами (POST и GET).
- Python-скрипт для автогенерации данных перед нагрузкой (LAB5).

## Структура и ключевые компоненты

- `src/main/java/.../api` — контроллеры REST API.
- `src/main/java/.../service` — бизнес-логика прогресса.
- `src/main/java/.../repository` — Spring Data репозитории.
- `src/main/java/.../entity` — JPA-сущности (`UserEntity`, `LessonEntity`, `LessonProgressEntity`).
- `src/main/resources/db/migration` — Flyway-миграции.
- `k6/` — профили и скрипты нагрузочного тестирования.
- `scripts/lab5_seed.py` — генератор тестовых данных.

## Требования

- Docker + Docker Compose.
- (Опционально для локального сидера) Python 3.10+.
- (Опционально для генерации графика локально) Node.js.

## Запуск проекта

Поднять стенд:

```bash
docker compose up -d --build
```

Проверить:

```bash
docker ps
docker compose logs app --tail=100
```

Порты:
- API с хоста: `http://localhost:8082`
- PostgreSQL: `localhost:5432`
- Внутри docker-сети API: `http://app:8081`

## Конфигурация приложения

Файл: `src/main/resources/application.properties`

- `server.port=8081`
- datasource через `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- `spring.sql.init.mode=never` (initdb-логика отключена)
- схема поднимается Flyway-миграцией `V1__init_schema.sql`

## REST API

### Users
- `POST /api/users`
- `GET /api/users`
- `GET /api/users/{id}`
- `PUT /api/users/{id}`
- `DELETE /api/users/{id}`
- `DELETE /api/users/clear` — удалить всех пользователей (с очищением связанного прогресса)

### Lessons
- `POST /api/lessons`
- `GET /api/lessons`
- `GET /api/lessons/{id}`
- `PUT /api/lessons/{id}`
- `DELETE /api/lessons/{id}`
- `DELETE /api/lessons/clear` — удалить все уроки (с очищением связанного прогресса)

### Progress
- `POST /api/progress`
- `POST /api/progress/complete`
- `GET /api/progress`
- `GET /api/progress/users/{userId}/lessons`
- `GET /api/progress/users/{userId}/lessons/{lessonId}`
- `PUT /api/progress/users/{userId}/lessons/{lessonId}`
- `DELETE /api/progress/users/{userId}/lessons/{lessonId}`
- `GET /api/progress/users/{userId}`
- `GET /api/progress/users`
- `DELETE /api/progress/clear` — удалить весь прогресс

## LAB5: Python-сидер тестовых данных

### Установка зависимостей

```bash
pip install -r scripts/requirements.txt
```

### Поддерживаемые аргументы

- `--count` — количество создаваемых объектов (по умолчанию `500`)
- `--endpoint` — целевой endpoint: `users`, `lessons`, `progress`
- `--clear` — очистить endpoint перед генерацией (или удалить конкретный объект вместе со связями)
- `--id` — удалить конкретный объект по id (поддержка: `users`, `lessons`)
- `--base-url` — базовый URL API (по умолчанию `http://localhost:8082`)

### Примеры

Очистить и создать 500 пользователей:

```bash
python scripts/lab5_seed.py --endpoint users --clear --count 500
```

Очистить и создать 300 уроков:

```bash
python scripts/lab5_seed.py --endpoint lessons --clear --count 300
```

Очистить и создать 1000 записей прогресса:

```bash
python scripts/lab5_seed.py --endpoint progress --clear --count 1000
```

Удалить конкретного пользователя:

```bash
python scripts/lab5_seed.py --endpoint users --clear --id 10
```

## LAB4: Нагрузочное тестирование k6

### Что тестируется

- POST пул: `POST /api/users`
- GET пул: `GET /api/progress/users`

Сценарии разделены на 2 независимых пула VU:
- `post_pool`
- `get_pool`

### Основные параметры

Файл: `k6/profile.js`

- `BASE_URL` (default: `http://localhost:8082`)
- `TARGET_VUS` (default: `10`)
- `START_VUS` (default: `1`)
- `POST_POOL_RATIO` (default: `0.5`) — доля VU для POST-пула
- `RAMP_UP` (default: `20s`)
- `STEADY_DURATION` (default: `40s`)
- `RAMP_DOWN` (default: `15s`)
- `SLEEP_SECONDS` (default: `0.2`)

### Быстрый запуск серии "удвоение нагрузки"

```powershell
.\k6\run-doubling.ps1
```

Скрипт запускает VUs: `10, 20, 40, 80, 160`.

### Ручной запуск одного теста

```bash
docker compose run --rm k6 run /scripts/load-test.js \
  -e BASE_URL=http://app:8081 \
  -e TARGET_VUS=40 \
  -e POST_POOL_RATIO=0.5 \
  -e RAMP_UP=20s \
  -e STEADY_DURATION=40s \
  -e RAMP_DOWN=15s \
  --summary-export /scripts/results/summary-vus-40.json
```

### Построение графика

```bash
node k6/generate-chart.js
```

Результаты:
- `k6/results/avg-response-vs-vus.csv`
- `k6/results/avg-response-vs-vus.md`
- `k6/results/avg-response-vs-vus.html`

На графике 2 линии:
- синяя: среднее время ответа POST-пула
- красная: среднее время ответа GET-пула

## Типовой сценарий демонстрации (преподавателю)

1. Поднять стенд `docker compose up -d --build`.
2. Очистить и заполнить данные через `scripts/lab5_seed.py`.
3. Показать CRUD и `/clear` endpoint-ы в Postman.
4. Запустить `.\k6\run-doubling.ps1`.
5. Открыть `k6/results/avg-response-vs-vus.html`.
6. Пояснить метрики `post_req_duration` и `get_req_duration` (две линии на графике).
