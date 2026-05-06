#!/usr/bin/env python3
"""
LAB15: график «6 точек» — для каждого POST/GET профиля (0.95 / 0.50 / 0.05)
сравниваются вариант 1 и вариант 2 (по p95 post прокси → Kafka).
Берёт среднее по pass для каждого ratio, если REPEATS > 1.
"""
from __future__ import annotations

import json
import os
import re
import statistics
from pathlib import Path

import matplotlib.pyplot as plt

# Директории с summary-lab15-v1-*.json и summary-lab15-v2-*.json
DIR_V1 = Path(os.environ.get("LAB15_DIR_V1", Path.home() / "lab15_matrix_v1"))
DIR_V2 = Path(os.environ.get("LAB15_DIR_V2", Path.home() / "lab15_matrix_v2"))
OUT_PNG = Path(
    os.environ.get(
        "LAB15_SIX_POINTS_PNG",
        Path.home() / "lab15_six_points_post_p95.png",
    )
)

RX = re.compile(
    r"summary-lab15-v(?P<v>[12])-pass(?P<pass>\d+)-r(?P<ratio>\d+p\d+)-"
)


def p95_from_metrics(metrics: dict, key: str) -> float | None:
    block = metrics.get(key)
    if not isinstance(block, dict):
        return None
    if "values" in block and isinstance(block["values"], dict):
        v = block["values"].get("p(95)")
        if isinstance(v, (int, float)):
            return float(v)
    s = block.get("p(95)")
    if isinstance(s, str) and s.replace(".", "", 1).isdigit():
        return float(s)
    return None


def load_averages_per_ratio(result_dir: Path, only_variant: str) -> dict[str, list[float]]:
    """ratio '0.95' -> [p95_pass1, p95_pass2, ...]"""
    by_ratio: dict[str, list[float]] = {}
    for f in sorted(result_dir.glob(f"summary-lab15-v{only_variant}-pass*-r*.json")):
        m = RX.search(f.name)
        if not m or m.group("v") != only_variant:
            continue
        ratio_token = m.group("ratio").replace("p", ".")
        raw = json.loads(f.read_text(encoding="utf-8"))
        metrics = raw.get("metrics") or {}
        p95 = p95_from_metrics(metrics, "post_req_duration")
        if p95 is None:
            continue
        by_ratio.setdefault(ratio_token, []).append(p95)
    return by_ratio


def mean_or_single(vals: list[float]) -> float:
    return statistics.mean(vals) if vals else 0.0


def main() -> None:
    if not DIR_V1.is_dir() or not DIR_V2.is_dir():
        raise SystemExit(f"Нужны каталоги: {DIR_V1} и {DIR_V2}")

    avg_v1 = {k: mean_or_single(v) for k, v in load_averages_per_ratio(DIR_V1, "1").items()}
    avg_v2 = {k: mean_or_single(v) for k, v in load_averages_per_ratio(DIR_V2, "2").items()}

    order = ["0.95", "0.50", "0.05"]
    x = list(range(len(order)))
    y1 = [avg_v1.get(r) for r in order]
    y2 = [avg_v2.get(r) for r in order]
    if any(v is None for v in y1 + y2):
        raise SystemExit(
            "Не хватает точек данных. Проверь имена summary-lab15-v1/v2-pass*-r*.json"
        )

    width = 0.35
    plt.figure(figsize=(10, 5.5))
    plt.bar([i - width / 2 for i in x], y1, width=width, label="Вариант 1 (1 pod / сервис)")
    plt.bar([i + width / 2 for i in x], y2, width=width, label="Вариант 2 (HPA до 3 pod)")
    plt.ylabel("post_req_duration p95 (ms)")
    plt.xlabel("POST pool ratio (доля POST в смеси)")
    plt.title("LAB15: сравнение вариантов по p95 POST (Kafka proxy)")
    plt.xticks(x, [f"{float(r)*100:.0f}/{100 - float(r)*100:.0f}" for r in order])
    plt.grid(axis="y", alpha=0.3)
    plt.legend()
    plt.tight_layout()
    OUT_PNG.parent.mkdir(parents=True, exist_ok=True)
    plt.savefig(OUT_PNG, dpi=160)
    plt.close()
    print(f"Saved: {OUT_PNG}")


if __name__ == "__main__":
    main()
