# LAB3 - Containerization + Flyway Migrations

В этой лабораторной проект упакован в Docker-контейнер, схема БД перенесена в Flyway-миграции, и весь стенд (`app + db`) поднимается одной командой через Docker Compose.

## Что изменили по сравнению с LAB2

- Добавили `Dockerfile` для контейнеризации Spring Boot приложения.
- Расширили `docker-compose.yml`: теперь поднимается не только PostgreSQL, но и само приложение.
- Перешли с `data.sql`/`ddl-auto=create-drop` на Flyway-миграции:
  - DDL в `src/main/resources/db/migration/V1__init_schema.sql`
  - DML в `src/main/resources/db/migration/V2__seed_data.sql`
- Удалили `src/main/resources/data.sql`.
- Изменили настройки JPA:
  - `spring.jpa.hibernate.ddl-auto=validate`
  - `spring.sql.init.mode=never`
- Добавили зависимость `org.flywaydb:flyway-core` в `build.gradle`.

## Стек

- Java 25
- Spring Boot 3.5
- Spring Data JPA
- Flyway
- PostgreSQL 16
- Docker / Docker Compose

## Структура миграций

Папка миграций:

- `src/main/resources/db/migration/V1__init_schema.sql` - создание таблиц.
- `src/main/resources/db/migration/V2__seed_data.sql` - начальные данные и фиксация sequence.

### Таблицы

- `users`:
  - `id` PK
  - `login` UNIQUE
  - `email` UNIQUE
  - `registration_date`
- `lessons`:
  - `id` PK
  - `topic`
  - `video_duration_minutes`
  - `test_name`
  - `max_test_score`
- `lesson_progress`:
  - `id` PK
  - `user_id` FK -> `users.id`
  - `lesson_id` FK -> `lessons.id`
  - `completion_date`
  - `test_result`
  - уникальная пара (`user_id`, `lesson_id`)

## Запуск стенда через Docker Compose

Из корня проекта:

```bash
docker compose up -d --build
```

Поднимутся два контейнера:

- `hl-module1-postgres`
- `hl-module1-app`

Проверка:

```bash
docker ps
```

Ожидаемо:

- PostgreSQL слушает `5432`
- Приложение доступно с хоста на `8082`

## Переменные окружения приложения в compose

Сервис `app` получает:

- `DB_URL=jdbc:postgresql://postgres:5432/hl_module1`
- `DB_USERNAME=postgres`
- `DB_PASSWORD=postgres`

## Проверка после старта (демонстрация преподавателю)

1. Поднять стенд:
   - `docker compose up -d --build`
2. Показать контейнеры:
   - `docker ps`
3. Показать логи приложения:
   - `docker logs hl-module1-app`
   - в логах должно быть `Started Module1Application`
4. Проверить API:
   - `GET http://localhost:8082/api/users`
   - `GET http://localhost:8082/api/lessons`
   - `GET http://localhost:8082/api/progress/users`

Если ответы приходят, значит стенд развернут корректно: миграции применились, БД и приложение связаны, API доступно.

## Локальный запуск без Docker (опционально)

Можно запускать приложение из IDE, если PostgreSQL уже доступен на `localhost:5432` с параметрами:

- DB: `hl_module1`
- user: `postgres`
- password: `postgres`

Файл `application.properties` уже настроен с дефолтами и поддержкой env-переменных.

## Команды остановки/очистки

Остановить стенд:

```bash
docker compose down
```

Остановить и удалить volume БД:

```bash
docker compose down -v
```
