#!/usr/bin/env python3
"""LAB5 data seeder for REST API using requests + Faker."""

from __future__ import annotations

import argparse
import random
import sys
from datetime import date
from typing import Any

import requests
from faker import Faker


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Seed REST service with test data")
    parser.add_argument("--count", type=int, default=500, help="number of objects to create (default: 500)")
    parser.add_argument(
        "--endpoint",
        choices=["users", "lessons", "progress"],
        required=True,
        help="target endpoint to fill",
    )
    parser.add_argument(
        "--clear",
        action="store_true",
        help="clear target data before generation (or delete by --id)",
    )
    parser.add_argument(
        "--id",
        type=int,
        help="delete one object by id for users/lessons (used with --clear)",
    )
    parser.add_argument(
        "--base-url",
        default="http://localhost:8082",
        help="base URL for API (default: http://localhost:8082)",
    )
    return parser.parse_args()


def request_or_fail(session: requests.Session, method: str, url: str, **kwargs: Any) -> requests.Response:
    try:
        response = session.request(method, url, timeout=15, **kwargs)
    except requests.RequestException as exc:
        raise SystemExit(f"Request failed: {method} {url}: {exc}") from exc

    if response.status_code >= 400:
        raise SystemExit(f"API error {response.status_code} for {method} {url}: {response.text}")
    return response


def fetch_list(session: requests.Session, base_url: str, endpoint: str) -> list[dict[str, Any]]:
    response = request_or_fail(session, "GET", f"{base_url}/api/{endpoint}")
    data = response.json()
    if not isinstance(data, list):
        raise SystemExit(f"Unexpected payload from /api/{endpoint}: {data}")
    return data


def clear_target(session: requests.Session, base_url: str, endpoint: str, object_id: int | None) -> None:
    if object_id is not None:
        if endpoint not in {"users", "lessons"}:
            raise SystemExit("--id supports only users or lessons")
        request_or_fail(session, "DELETE", f"{base_url}/api/{endpoint}/{object_id}")
        print(f"Deleted /api/{endpoint}/{object_id}")
        return

    request_or_fail(session, "DELETE", f"{base_url}/api/{endpoint}/clear")
    print(f"Cleared /api/{endpoint}")


def seed_users(session: requests.Session, base_url: str, count: int, faker: Faker) -> None:
    for i in range(count):
        payload = {
            "login": f"{faker.user_name()}_{i}_{random.randint(1000, 9999)}",
            "email": f"{faker.unique.email()}",
            "registrationDate": str(faker.date_between(start_date="-2y", end_date="today")),
        }
        request_or_fail(session, "POST", f"{base_url}/api/users", json=payload)
    print(f"Created users: {count}")


def seed_lessons(session: requests.Session, base_url: str, count: int, faker: Faker) -> None:
    for i in range(count):
        payload = {
            "topic": f"{faker.job()} #{i + 1}",
            "videoDurationMinutes": random.randint(10, 120),
            "testName": f"Quiz {i + 1}",
            "maxTestScore": random.randint(10, 100),
        }
        request_or_fail(session, "POST", f"{base_url}/api/lessons", json=payload)
    print(f"Created lessons: {count}")


def ensure_users_and_lessons(session: requests.Session, base_url: str, count: int, faker: Faker) -> tuple[list[dict[str, Any]], list[dict[str, Any]]]:
    users = fetch_list(session, base_url, "users")
    lessons = fetch_list(session, base_url, "lessons")

    if len(users) < count:
        seed_users(session, base_url, count - len(users), faker)
        users = fetch_list(session, base_url, "users")
    if len(lessons) < count:
        seed_lessons(session, base_url, count - len(lessons), faker)
        lessons = fetch_list(session, base_url, "lessons")

    return users, lessons


def seed_progress(session: requests.Session, base_url: str, count: int, faker: Faker) -> None:
    users, lessons = ensure_users_and_lessons(session, base_url, max(1, min(count, 200)), faker)
    pairs: set[tuple[int, int]] = set()
    attempts = 0

    while len(pairs) < count and attempts < count * 10:
        user = random.choice(users)
        lesson = random.choice(lessons)
        pairs.add((int(user["id"]), int(lesson["id"])))
        attempts += 1

    created = 0
    for user_id, lesson_id in pairs:
        completion = faker.date_between(start_date="-1y", end_date="today")
        payload = {
            "userId": user_id,
            "lessonId": lesson_id,
            "completionDate": str(completion),
            "testResult": random.randint(0, 100),
        }

        # Clamp result to lesson max score.
        lesson = next((item for item in lessons if int(item["id"]) == lesson_id), None)
        if lesson is not None:
            payload["testResult"] = random.randint(0, int(lesson["maxTestScore"]))

        request_or_fail(session, "POST", f"{base_url}/api/progress", json=payload)
        created += 1

    print(f"Created progress rows: {created}")


def main() -> None:
    args = parse_args()
    if args.count < 0:
        raise SystemExit("--count must be >= 0")

    faker = Faker("ru_RU")
    random.seed()

    with requests.Session() as session:
        session.headers.update({"Content-Type": "application/json"})

        if args.clear:
            clear_target(session, args.base_url, args.endpoint, args.id)
            if args.id is not None:
                return

        if args.count == 0:
            print("Nothing to generate: --count 0")
            return

        if args.endpoint == "users":
            seed_users(session, args.base_url, args.count, faker)
        elif args.endpoint == "lessons":
            seed_lessons(session, args.base_url, args.count, faker)
        else:
            # Ensure valid dates for generated progress if users were just created.
            seed_progress(session, args.base_url, args.count, faker)

    print("Done.")


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\nInterrupted.", file=sys.stderr)
        raise SystemExit(130)
