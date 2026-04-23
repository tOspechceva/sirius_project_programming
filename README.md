# LAB2 - Spring Data JPA + PostgreSQL

Проект переведен с in-memory хранения на полноценную работу с PostgreSQL через Spring Data JPA.

## Что изменено в LAB2

- Добавлен `spring-boot-starter-data-jpa` и драйвер `postgresql`.
- Реализованы JPA-сущности:
  - `UserEntity` -> таблица `users`
  - `LessonEntity` -> таблица `lessons`
  - `LessonProgressEntity` -> таблица `lesson_progress`
- Добавлены Spring Data репозитории:
  - `SpringDataUserEntityRepository`
  - `SpringDataLessonEntityRepository`
  - `SpringDataLessonProgressEntityRepository`
- Добавлены JPA-адаптеры, реализующие старые интерфейсы репозиториев:
  - `JpaUserRepositoryAdapter`
  - `JpaLessonRepositoryAdapter`
  - `JpaLessonProgressRepositoryAdapter`
- Конфигурация приложения обновлена на работу с PostgreSQL.
- Добавлен `docker-compose.yml` для разворачивания PostgreSQL.
- Добавлен `data.sql` с тестовыми данными.
- README обновлен под LAB2.

## Стек

- Java 25
- Spring Boot 3.5
- Spring Data JPA
- PostgreSQL 16
- Docker Compose

## Структура данных

### Таблица `users`

- `id` (PK)
- `login` (unique)
- `email` (unique)
- `registration_date`

### Таблица `lessons`

- `id` (PK)
- `topic`
- `video_duration_minutes`
- `test_name`
- `max_test_score`

### Таблица `lesson_progress`

- `id` (PK)
- `user_id` (FK -> users.id)
- `lesson_id` (FK -> lessons.id)
- `completion_date`
- `test_result`
- уникальная пара (`user_id`, `lesson_id`)

## Запуск PostgreSQL

Из корня проекта:

```bash
docker compose up -d
```

Параметры БД:

- DB: `hl_module1`
- User: `postgres`
- Password: `postgres`
- Port: `5432`

## Конфигурация приложения

`src/main/resources/application.properties`:

- `server.port=8081`
- `spring.datasource.url=jdbc:postgresql://localhost:5432/hl_module1`
- `spring.datasource.username=postgres`
- `spring.datasource.password=postgres`
- `spring.jpa.hibernate.ddl-auto=create-drop`
- `spring.sql.init.mode=always`

На старте Hibernate создает таблицы, затем `data.sql` загружает тестовые записи.

## Тестовые данные (автозагрузка)

Файл: `src/main/resources/data.sql`

Загружается:

- 2 пользователя
- 3 урока
- 4 записи прогресса

После вставки обновляются sequence для корректной генерации новых ID.

## REST API (актуально для LAB2)

Базовый URL: `http://localhost:8081`

### Users

- `POST /api/users`
- `GET /api/users`
- `GET /api/users/{id}`
- `PUT /api/users/{id}`
- `DELETE /api/users/{id}`

### Lessons

- `POST /api/lessons`
- `GET /api/lessons`
- `GET /api/lessons/{id}`
- `PUT /api/lessons/{id}`
- `DELETE /api/lessons/{id}`

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

## Запуск через IntelliJ IDEA

1. Поднять PostgreSQL:
   - `docker compose up -d`
2. Открыть проект в IntelliJ.
3. Дождаться импорта Gradle.
4. Убедиться, что выбран JDK 25.
5. Запустить `Module1Application`.
6. Проверить API на `http://localhost:8081`.

## Пример быстрой проверки

1. `GET /api/users` -> должны вернуться 2 пользователя из `data.sql`.
2. `GET /api/lessons` -> должны вернуться 3 урока.
3. `GET /api/progress/users` -> должны вернуться проценты прогресса.

## Примечание по архитектуре

Контроллеры и сервисный слой сохранены. Замена коснулась только слоя хранения:

- раньше: static in-memory коллекции;
- теперь: Spring Data JPA + PostgreSQL.

Это позволяет менять реализацию репозиториев без переписывания бизнес-логики.
