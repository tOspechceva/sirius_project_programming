# Платформа онлайн-курсов

REST API на Spring Boot для учета прохождения уроков и расчета прогресса учеников.

## Постановка задачи

Реализована система с тремя основными сущностями:

- `User`: логин, email, дата регистрации.
- `Lesson`: ID, тема, длительность видео, тест.
- `LessonProgress`: связь пользователя и урока, дата завершения, результат теста.

Дополнительно реализован расчет:

- прогресса одного ученика в процентах;
- прогресса всех учеников в процентах.

## Архитектура проекта

Проект разделен на слои:

- `model` — доменные сущности.
- `repository` — интерфейсы доступа к данным.
- `repository.inmemory` — реализации на `static`-коллекциях Java.
- `service` — бизнес-логика и расчет прогресса.
- `api` — REST-контроллеры, DTO и обработчик ошибок.
- `config` — конфигурация бинов приложения.

## Связи между сущностями

- `User` и `Lesson` связаны отношением многие-ко-многим.
- Технически связь реализована через `LessonProgress`.
- Для одной пары `userId + lessonId` хранится одна запись прогресса.

## Хранение данных

По условию задания используется in-memory хранение через `static`-коллекции:

- `StaticUserRepository`
- `StaticLessonRepository`
- `StaticLessonProgressRepository`

Важно: данные не сохраняются между перезапусками приложения.

## Гибкость реализации

Формула расчета прогресса вынесена в отдельную стратегию:

- `ProgressCalculator` — общий контракт;
- `CompletionProgressCalculator` — текущая формула (процент завершенных уроков).

Это позволяет без изменения сервиса добавить другую логику расчета (например, по среднему баллу тестов или с весами уроков).

## API

Базовый адрес: `http://localhost:8081`

### Users (полный CRUD)

- `POST /api/users` — создать пользователя
- `GET /api/users` — получить список пользователей
- `GET /api/users/{id}` — получить пользователя по ID
- `PUT /api/users/{id}` — обновить пользователя
- `DELETE /api/users/{id}` — удалить пользователя

### Lessons (полный CRUD)

- `POST /api/lessons` — создать урок
- `GET /api/lessons` — получить список уроков
- `GET /api/lessons/{id}` — получить урок по ID
- `PUT /api/lessons/{id}` — обновить урок
- `DELETE /api/lessons/{id}` — удалить урок

### Progress (нужные операции)

- `POST /api/progress` — создать/перезаписать запись прогресса
- `POST /api/progress/complete` — отметить завершение (совместимый endpoint)
- `GET /api/progress` — получить все записи прогресса
- `GET /api/progress/users/{userId}/lessons` — получить прогресс пользователя по всем урокам
- `GET /api/progress/users/{userId}/lessons/{lessonId}` — получить запись прогресса по уроку
- `PUT /api/progress/users/{userId}/lessons/{lessonId}` — обновить запись прогресса
- `DELETE /api/progress/users/{userId}/lessons/{lessonId}` — удалить запись прогресса
- `GET /api/progress/users/{userId}` — получить прогресс пользователя в %
- `GET /api/progress/users` — получить прогресс всех пользователей в %

## Postman: сценарий тестирования

Рекомендуемый порядок проверки:

1. Создать двух пользователей.
2. Создать 2-3 урока.
3. Отметить прохождение уроков.
4. Проверить прогресс одного ученика.
5. Проверить прогресс всех учеников.

### 1. Создание пользователя

`POST http://localhost:8081/api/users`

```json
{
  "login": "ivan_student",
  "email": "ivan@example.com",
  "registrationDate": "2026-03-03"
}
```

### 2. Создание второго пользователя

`POST http://localhost:8081/api/users`

```json
{
  "login": "olga_student",
  "email": "olga@example.com",
  "registrationDate": "2026-03-10"
}
```

### 3. Создание урока

`POST http://localhost:8081/api/lessons`

```json
{
  "topic": "Java Basics",
  "videoDurationMinutes": 45,
  "testName": "Тест по синтаксису",
  "maxTestScore": 10
}
```

### 4. Создание второго урока

`POST http://localhost:8081/api/lessons`

```json
{
  "topic": "ООП в Java",
  "videoDurationMinutes": 55,
  "testName": "Тест по ООП",
  "maxTestScore": 20
}
```

### 5. Отметка завершения урока

`POST http://localhost:8081/api/progress/complete`

```json
{
  "userId": 1,
  "lessonId": 1,
  "completionDate": "2026-04-01",
  "testResult": 9
}
```

### 6. Прогресс одного ученика

`GET http://localhost:8081/api/progress/users/1`

### 7. Прогресс всех учеников

`GET http://localhost:8081/api/progress/users`

### 8. Обновление пользователя (пример PUT)

`PUT http://localhost:8081/api/users/1`

```json
{
  "login": "ivan_student_updated",
  "email": "ivan.new@example.com",
  "registrationDate": "2026-03-03"
}
```

### 9. Обновление урока (пример PUT)

`PUT http://localhost:8081/api/lessons/1`

```json
{
  "topic": "Java Basics Updated",
  "videoDurationMinutes": 50,
  "testName": "Тест по синтаксису v2",
  "maxTestScore": 12
}
```

### 10. Обновление записи прогресса (пример PUT)

`PUT http://localhost:8081/api/progress/users/1/lessons/1`

```json
{
  "completionDate": "2026-04-02",
  "testResult": 10
}
```

### 11. Удаление записи прогресса (пример DELETE)

`DELETE http://localhost:8081/api/progress/users/1/lessons/1`

## Запуск в IntelliJ IDEA

1. Открыть проект `c:\ad\hl-module1`.
2. Дождаться импорта Gradle.
3. Проверить JDK: `File -> Project Structure -> Project SDK` (рекомендуется JDK 25).
4. Открыть класс `Module1Application`.
5. Запустить `main` метод (`Run 'Module1Application'`).
6. Убедиться, что приложение стартовало на `http://localhost:8081`.

## Бизнес-валидация

При вызове `POST /api/progress/complete` проверяется:

- пользователь существует;
- урок существует;
- дата завершения задана;
- дата завершения не раньше даты регистрации пользователя;
- результат теста в диапазоне `0..maxScore`.

При нарушении правила возвращается `400 Bad Request` с сообщением об ошибке.

## Как можно расширить проект

- Добавить пагинацию и сортировку в `GET` списки.
- Добавить валидацию DTO через `jakarta.validation` (`@NotBlank`, `@Email`, `@Min`).
- Реализовать альтернативные формулы в `ProgressCalculator`.
- Заменить in-memory слой на БД без изменения бизнес-сервиса.
