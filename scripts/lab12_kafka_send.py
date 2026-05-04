#!/usr/bin/env python3
"""
LAB12: отправка одного JSON-сообщения в Kafka (как ждёт CourseKafkaListener).

Пример:
  pip install kafka-python
  python scripts/lab12_kafka_send.py --bootstrap localhost:9092 --topic hl06 \\
    --json '{"entity":"USER","operation":"POST","payload":{"login":"k1","email":"k1@ex.com","registrationDate":"2024-06-01"}}'
"""
from __future__ import annotations

import argparse
import json
import sys

try:
    from kafka import KafkaProducer
except ImportError as exc:
    raise SystemExit("Установите: pip install kafka-python") from exc


def main() -> None:
    parser = argparse.ArgumentParser(description="LAB12: send one JSON command to Kafka")
    parser.add_argument(
        "--bootstrap",
        default="localhost:9092",
        help="bootstrap servers, через запятую",
    )
    parser.add_argument("--topic", default="hl06", help="имя топика")
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument("--json", help="тело сообщения одной строкой JSON")
    group.add_argument("--json-file", type=argparse.FileType("r", encoding="utf-8"), help="файл с JSON")
    args = parser.parse_args()

    raw = args.json if args.json is not None else args.json_file.read()
    try:
        parsed = json.loads(raw)
    except json.JSONDecodeError as exc:
        raise SystemExit(f"Невалидный JSON: {exc}") from exc

    servers = [s.strip() for s in args.bootstrap.split(",") if s.strip()]
    producer = KafkaProducer(
        bootstrap_servers=servers,
        value_serializer=lambda v: json.dumps(v, ensure_ascii=False).encode("utf-8"),
    )
    producer.send(args.topic, value=parsed)
    producer.flush(timeout=30)
    producer.close()
    print("sent to", args.topic)


if __name__ == "__main__":
    main()
