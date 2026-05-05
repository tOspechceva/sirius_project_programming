#!/usr/bin/env python3
from __future__ import annotations

import json
import os
from typing import Any

from flask import Flask, jsonify, request
from kafka import KafkaProducer

app = Flask(__name__)

KAFKA_BOOTSTRAP = os.getenv("KAFKA_BOOTSTRAP_SERVERS", "hl15.zil:9094,hl14.zil:9094")
KAFKA_TOPIC = os.getenv("KAFKA_TOPIC", "hl06")
PROXY_PORT = int(os.getenv("PROXY_PORT", "18081"))

producer = KafkaProducer(
    bootstrap_servers=[s.strip() for s in KAFKA_BOOTSTRAP.split(",") if s.strip()],
    value_serializer=lambda v: json.dumps(v, ensure_ascii=False).encode("utf-8"),
    acks="all",
)


@app.get("/health")
def health() -> Any:
    return jsonify({"status": "ok", "topic": KAFKA_TOPIC})


@app.post("/publish")
def publish() -> Any:
    data = request.get_json(silent=True)
    if not isinstance(data, dict):
        return jsonify({"error": "JSON object is required"}), 400

    future = producer.send(KAFKA_TOPIC, value=data)
    meta = future.get(timeout=10)
    return jsonify(
        {
            "status": "sent",
            "topic": meta.topic,
            "partition": meta.partition,
            "offset": meta.offset,
        }
    )


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=PROXY_PORT)
