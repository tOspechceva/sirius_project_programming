#!/usr/bin/env python3
"""
Сидер данных LAB5 для REST API (requests + Faker).

Этот скрипт автоматически генерирует тестовые данные для эндпоинтов:
- users: пользователи с уникальными логинами и почтами
- lessons: учебные материалы с параметрами теста
- progress: связи многие-ко-многим (кто какой урок прошёл и с каким результатом)
- all: специальный режим для очистки всех таблиц одной командой

Поддерживает режимы: генерация, полная очистка, точечное удаление.
"""

# Библиотеки для работы с аргументами командной строки и системными функциями
from __future__ import annotations  # Включение современной аннотации типов (например, int | None)

import argparse   # Парсинг аргументов командной строки (--count, --endpoint и т.д.)
import random     # Генерация случайных чисел для вариативности данных
import sys        # Системные функции, включая корректный выход из скрипта
from datetime import date  # Работа с датами (используется Faker, но импортируем для ясности)
from typing import Any     # Тип Any для гибкой типизации словарей и ответов API

# Сторонние зависимости
import requests   # HTTP-клиент для отправки запросов к REST API
from faker import Faker  # Генератор реалистичных фейковых данных (имена, почты, даты)


# ============================================================================
# ПАРСИНГ АРГУМЕНТОВ КОМАНДНОЙ СТРОКИ
# ============================================================================
def parse_args() -> argparse.Namespace:
    """
    Настраивает и разбирает аргументы командной строки.
    Возвращает объект с полями: count, endpoint, clear, id, base_url.
    """
    # Создаём парсер с описанием — это текст, который увидит пользователь при --help
    parser = argparse.ArgumentParser(description="Seed REST service with test data")
    
    # --count: сколько записей создать (по умолчанию 500)
    parser.add_argument(
        "--count",
        type=int,
        default=500,
        help="number of objects to create (default: 500)"
    )
    
    # --endpoint: обязательный выбор одной из трёх сущностей
    parser.add_argument(
        "--endpoint",
        choices=["users", "lessons", "progress", "all"],  # Ограничиваем допустимые значения
        required=True,                              # Если не указан — скрипт завершится с ошибкой
        help="target endpoint to fill"
    )
    
    # --clear: флаг (без значения), который активирует режим очистки перед генерацией
    parser.add_argument(
        "--clear",
        action="store_true",  # Если флаг есть — значение True, иначе False
        help="clear target data before generation (or delete by --id)"
    )
    
    # --id: опциональный числовой ID для точечного удаления (работает только с --clear)
    parser.add_argument(
        "--id",
        type=int,
        help="delete one object by id for users/lessons (used with --clear)"
    )
    
    # --base-url: адрес API, чтобы не хардкодить localhost в коде
    parser.add_argument(
        "--base-url",
        default="http://localhost:8082",
        help="base URL for API (default: http://localhost:8082)"
    )
    
    # Возвращаем распарсенные аргументы как объект с атрибутами
    return parser.parse_args()


# ============================================================================
#  УНИВЕРСАЛЬНЫЙ ОТПРАВИТЕЛЬ ЗАПРОСОВ С ОБРАБОТКОЙ ОШИБОК
# ============================================================================
def request_or_fail(session: requests.Session, method: str, url: str, **kwargs: Any) -> requests.Response:
    """
    Отправляет HTTP-запрос и гарантированно останавливает скрипт при любой ошибке.
    
    Почему так? Чтобы не «молча» пропускать сбои сети или баги сервера,
    которые могут привести к некорректным тестовым данным.
    """
    try:
        # Выполняем запрос с таймаутом 15 секунд (защита от «вечных» зависаний)
        response = session.request(method, url, timeout=15, **kwargs)
    except requests.RequestException as exc:
        # Сетевые ошибки: сервер недоступен, таймаут, неверный сертификат и т.п.
        # SystemExit — корректный способ завершить скрипт с кодом ошибки
        raise SystemExit(f"Request failed: {method} {url}: {exc}") from exc

    # Проверяем HTTP-статус: 4xx (ошибка клиента) и 5xx (ошибка сервера) — это тоже провал
    if response.status_code >= 400:
        # Выводим тело ответа — там часто бывает полезное сообщение об ошибке от сервера
        raise SystemExit(f"API error {response.status_code} for {method} {url}: {response.text}")
    
    # Если всё ок — возвращаем успешный ответ для дальнейшей обработки
    return response


# ============================================================================
# 📥 ПОЛУЧЕНИЕ СПИСКА СУЩНОСТЕЙ С ПРОВЕРКОЙ ФОРМАТА
# ============================================================================
def fetch_list(session: requests.Session, base_url: str, endpoint: str) -> list[dict[str, Any]]:
    """
    Запрашивает список всех объектов эндпоинта и валидирует, что ответ — это массив.
    
    Зачем проверка типа? Чтобы сразу отловить случаи, когда сервер вернул ошибку
    в виде JSON-объекта {"error": "..."}, а не ожидаемый список [].
    """
    # Делаем GET-запрос к /api/{endpoint}
    response = request_or_fail(session, "GET", f"{base_url}/api/{endpoint}")
    # Парсим JSON-ответ
    data = response.json()
    
    # Убеждаемся, что пришли именно данные (список), а не ошибка или другая структура
    if not isinstance(data, list):
        raise SystemExit(f"Unexpected payload from /api/{endpoint}: {data}")
    
    return data


# ============================================================================
# 🧹 ОЧИСТКА ДАННЫХ: ПОЛНАЯ ИЛИ ТОЧЕЧНАЯ
# ============================================================================
def clear_target(session: requests.Session, base_url: str, endpoint: str, object_id: int | None) -> None:
    """
    Удаляет данные перед новой генерацией.
    
    Два режима:
    1. Точечное удаление по ID (только для users/lessons)
    2. Полная очистка через специальный эндпоинт /clear
    """
    # ── Режим 1: удалить одну запись по ID ──
    if object_id is not None:
        # Защита: прогресс не поддерживает удаление по ID (сложная связь многие-ко-многим)
        if endpoint not in {"users", "lessons"}:
            raise SystemExit("--id supports only users or lessons")
        
        # Отправляем DELETE /api/{endpoint}/{id}
        request_or_fail(session, "DELETE", f"{base_url}/api/{endpoint}/{object_id}")
        print(f"Deleted /api/{endpoint}/{object_id}")
        return  # Выходим, чтобы не выполнять полную очистку ниже

    # ── Режим 2: полная очистка коллекции ──
    # Предполагается, что на сервере есть специальный эндпоинт /clear для каждого ресурса
    request_or_fail(session, "DELETE", f"{base_url}/api/{endpoint}/clear")
    print(f"Cleared /api/{endpoint}")


def clear_all_targets(session: requests.Session, base_url: str) -> None:
    """
    Полная очистка всех таблиц приложения одной функцией.

    Порядок важен:
    1) progress (таблица-связка, содержит внешние ключи),
    2) users,
    3) lessons.
    """
    request_or_fail(session, "DELETE", f"{base_url}/api/progress/clear")
    request_or_fail(session, "DELETE", f"{base_url}/api/users/clear")
    request_or_fail(session, "DELETE", f"{base_url}/api/lessons/clear")
    print("Cleared all targets: progress, users, lessons")


# ============================================================================
# 👥 ГЕНЕРАЦИЯ ПОЛЬЗОВАТЕЛЕЙ
# ============================================================================
def seed_users(session: requests.Session, base_url: str, count: int, faker: Faker) -> None:
    """
    Создаёт N пользователей с гарантированно уникальными логинами и почтами.
    
    Почему уникальность критична? Чтобы избежать ошибок 409 Conflict
    при попытке создать дубликат в базе данных.
    """
    for i in range(count):
        # Формируем реалистичный и уникальный payload
        payload = {
            # login: имя_пользователя_индекс_рандом → например, ivan_petrov_123_4567
            "login": f"{faker.user_name()}_{i}_{random.randint(1000, 9999)}",
            # email: используем faker.unique, чтобы исключить повторения даже внутри одного запуска
            "email": f"{faker.unique.email()}",
            # registrationDate: случайная дата за последние 2 года (как в реальной жизни)
            "registrationDate": str(faker.date_between(start_date="-2y", end_date="today")),
        }
        # Отправляем POST-запрос на создание
        request_or_fail(session, "POST", f"{base_url}/api/users", json=payload)
    
    # По завершении — отчёт в консоль
    print(f"Created users: {count}")


# ============================================================================
# 📚 ГЕНЕРАЦИЯ УРОКОВ
# ============================================================================
def seed_lessons(session: requests.Session, base_url: str, count: int, faker: Faker) -> None:
    """
    Создаёт N учебных материалов с правдоподобными параметрами.
    """
    for i in range(count):
        payload = {
            # topic: профессия + номер → "Инженер-программист #1"
            "topic": f"{faker.job()} #{i + 1}",
            # videoDurationMinutes: реалистичная длительность урока (10–120 минут)
            "videoDurationMinutes": random.randint(10, 120),
            # testName: стандартное название теста
            "testName": f"Quiz {i + 1}",
            # maxTestScore: максимальный балл за тест (10–100)
            "maxTestScore": random.randint(10, 100),
        }
        request_or_fail(session, "POST", f"{base_url}/api/lessons", json=payload)
    
    print(f"Created lessons: {count}")


# ============================================================================
#  ПОДГОТОВКА ЗАВИСИМОСТЕЙ ДЛЯ PROGRESS
# ============================================================================
def ensure_users_and_lessons(
    session: requests.Session, base_url: str, count: int, faker: Faker
) -> tuple[list[dict[str, Any]], list[dict[str, Any]]]:
    """
    Гарантирует, что в базе есть достаточно пользователей и уроков для генерации прогресса.
    
    Зачем это нужно? Прогресс — это связь между пользователем и уроком.
    Нельзя создать запись о прохождении, если самих сущностей не существует.
    """
    # Получаем текущие списки с сервера
    users = fetch_list(session, base_url, "users")
    lessons = fetch_list(session, base_url, "lessons")

    # В LAB5 важно не "раздувать" родительские таблицы без необходимости.
    # Поэтому users/lessons дозаполняем только если таблица полностью пустая.
    # Если записи уже есть, используем их как есть.
    # Если таблица пуста, создаём базовый объём до половины requested count.
    # Пример: progress --count 100 -> users/lessons по 50.
    bootstrap_count = max(1, count // 2)

    if len(users) == 0:
        seed_users(session, base_url, bootstrap_count, faker)
        users = fetch_list(session, base_url, "users")

    if len(lessons) == 0:
        seed_lessons(session, base_url, bootstrap_count, faker)
        lessons = fetch_list(session, base_url, "lessons")

    # Возвращаем кортеж (список пользователей, список уроков) для дальнейшего использования
    return users, lessons


# ============================================================================
# 📊 ГЕНЕРАЦИЯ ПРОГРЕССА (связи многие-ко-многим)
# ============================================================================
def seed_progress(session: requests.Session, base_url: str, count: int, faker: Faker) -> None:
    """
    Создаёт записи о прохождении уроков пользователями.
    
    Особенности:
    - Уникальные пары (userId, lessonId): один пользователь не может дважды пройти один урок
    - Валидация балла: testResult не может превышать maxTestScore конкретного урока
    - Защита от бесконечного цикла при генерации уникальных пар
    """
    # Шаг 1: получаем зависимости. Если таблицы пустые — создаём по половине count.
    users, lessons = ensure_users_and_lessons(session, base_url, count, faker)

    max_pairs = len(users) * len(lessons)
    if max_pairs == 0:
        raise SystemExit("Cannot generate progress: users or lessons are empty.")
    if count > max_pairs:
        raise SystemExit(
            f"Cannot generate {count} unique progress rows with current data. "
            f"Maximum possible pairs: {max_pairs}. "
            "Add more users/lessons or reduce --count."
        )
    
    # Множество для хранения уже созданных пар (защита от дублей)
    pairs: set[tuple[int, int]] = set()
    attempts = 0  # Счётчик попыток — защита от зацикливания, если запрошено больше пар, чем возможно

    # Шаг 2: генерируем уникальные комбинации (userId, lessonId)
    while len(pairs) < count and attempts < count * 10:
        # Случайный выбор пользователя и урока из существующих
        user = random.choice(users)
        lesson = random.choice(lessons)
        # Добавляем в множество: дубли автоматически игнорируются
        pairs.add((int(user["id"]), int(lesson["id"])))
        attempts += 1

    # Индексы по id, чтобы не искать пользователя/урок каждый раз линейным проходом.
    users_by_id = {int(user["id"]): user for user in users}
    lessons_by_id = {int(lesson["id"]): lesson for lesson in lessons}

    # Шаг 3: создаём записи прогресса для каждой уникальной пары
    created = 0
    for user_id, lesson_id in pairs:
        # Дата завершения должна быть не раньше даты регистрации пользователя.
        # Иначе API вернёт 400: "Дата завершения не может быть раньше даты регистрации пользователя".
        user = users_by_id.get(user_id)
        reg_raw = user.get("registrationDate") if user is not None else None
        reg_date = date.fromisoformat(reg_raw) if reg_raw else date.today()
        completion = faker.date_between(start_date=reg_date, end_date="today")
        
        payload = {
            "userId": user_id,
            "lessonId": lesson_id,
            "completionDate": str(completion),
            # Временное значение балла (ниже скорректируем)
            "testResult": random.randint(0, 100),
        }

        # 🔍 Важная бизнес-логика: балл не может быть больше максимального для этого урока
        lesson = lessons_by_id.get(lesson_id)
        if lesson is not None:
            # Пересчитываем testResult в допустимом диапазоне [0, maxTestScore]
            payload["testResult"] = random.randint(0, int(lesson["maxTestScore"]))

        # Отправляем запрос на создание записи прогресса
        request_or_fail(session, "POST", f"{base_url}/api/progress", json=payload)
        created += 1

    print(f"Created progress rows: {created}")


# ============================================================================
# 🚀 ТОЧКА ВХОДА: MAIN
# ============================================================================
def main() -> None:
    """
    Главная функция: координирует весь процесс сидинга.
    """
    # 1. Парсим аргументы командной строки
    args = parse_args()
    
    # Валидация: нельзя создать отрицательное количество записей
    if args.count < 0:
        raise SystemExit("--count must be >= 0")

    # 2. Инициализируем генератор фейковых данных с русской локалью
    # (имена, адреса, профессии будут на русском — реалистичнее для демо)
    faker = Faker("ru_RU")
    # Инициализируем генератор случайных чисел (для воспроизводимости можно зафиксировать seed)
    random.seed()

    # 3. Используем Session для переиспользования соединения (keep-alive, куки, заголовки)
    with requests.Session() as session:
        # Устанавливаем заголовок по умолчанию для всех запросов
        session.headers.update({"Content-Type": "application/json"})

        # 4. Опциональная очистка данных перед генерацией
        if args.clear:
            if args.endpoint == "all":
                if args.id is not None:
                    raise SystemExit("--id is not supported with --endpoint all")
                clear_all_targets(session, args.base_url)
                return
            else:
                clear_target(session, args.base_url, args.endpoint, args.id)
            # Если было точечное удаление (--id) — дальше ничего не делаем, выходим
            if args.id is not None:
                return

        # 5. Если count=0 — ничего не генерируем (удобно для режима "только очистка")
        if args.count == 0:
            print("Nothing to generate: --count 0")
            return

        # 6. Запускаем генерацию нужного типа сущностей
        if args.endpoint == "all":
            # Для endpoint=all генерация не поддерживается — это режим очистки.
            # Используйте: --endpoint all --clear
            raise SystemExit("Generation for --endpoint all is not supported. Use --endpoint all --clear")
        elif args.endpoint == "users":
            seed_users(session, args.base_url, args.count, faker)
        elif args.endpoint == "lessons":
            seed_lessons(session, args.base_url, args.count, faker)
        else:
            # Для progress сначала подготавливаем зависимости, потом генерируем связи
            seed_progress(session, args.base_url, args.count, faker)

    # 7. Финальное сообщение об успешном завершении
    print("Done.")


# ============================================================================
# 🏁 ЗАПУСК СКРИПТА
# ============================================================================
if __name__ == "__main__":
    try:
        # Запускаем главную логику
        main()
    except KeyboardInterrupt:
        # Обработка прерывания пользователем (Ctrl+C)
        print("\nInterrupted.", file=sys.stderr)
        # Код выхода 130 — стандартный код для завершения по SIGINT
        raise SystemExit(130)