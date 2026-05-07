#!/usr/bin/env python3
"""
LAB16 вариант 1 (retry + killer): три графика по POST/GET профилям.
По оси X — Pass (1, 2), линии — GET avg и POST avg из k6 summary.

  OUT_DIR=~/lab16_matrix_v1 python3 scripts/plot-lab16-v1-three-graphs.py

Имя файлов: summary-lab16-v1-pass1-r0p95-vus30-....json
Переопределить префикс: LAB16_VARIANT=lab16-v1
"""
from __future__ import annotations

import json
import os
import re
from pathlib import Path

import matplotlib.pyplot as plt

OUT_DIR = Path(os.environ.get("OUT_DIR", Path.home() / "lab16_matrix_v1"))
VARIANT = os.environ.get("LAB16_VARIANT", "lab16-v1")


def build_rx(variant: str) -> re.Pattern[str]:
    escaped = re.escape(variant)
    return re.compile(rf"summary-{escaped}-pass(?P<pass>\d+)-r(?P<ratio>\d+p\d+)-")


RX = build_rx(VARIANT)


def metric_avg(metrics: dict, key: str) -> float | None:
    v = metrics.get(key, {})
    if isinstance(v, dict):
        if "avg" in v:
            return float(v["avg"])
        if "values" in v and isinstance(v["values"], dict):
            x = v["values"].get("avg")
            if x is not None:
                return float(x)
    return None


def load_by_ratio() -> dict[str, dict[int, tuple[float, float]]]:
    out: dict[str, dict[int, tuple[float, float]]] = {}
    pattern = f"summary-{VARIANT}-pass*-r*.json"
    for f in sorted(OUT_DIR.glob(pattern)):
        m = RX.search(f.name)
        if not m:
            continue
        pass_num = int(m.group("pass"))
        ratio = m.group("ratio").replace("p", ".")
        raw = json.loads(f.read_text(encoding="utf-8"))
        metrics = raw.get("metrics") or {}
        g = metric_avg(metrics, "get_req_duration")
        p = metric_avg(metrics, "post_req_duration")
        if g is None or p is None:
            print(f"skip (no avg): {f.name}")
            continue
        out.setdefault(ratio, {})[pass_num] = (g, p)
    return out


def main() -> None:
    data = load_by_ratio()
    if not data:
        raise SystemExit(
            f"Нет summary-{VARIANT}-pass*-r*.json в {OUT_DIR} (см. OUT_DIR, LAB16_VARIANT)."
        )

    order = ["0.95", "0.50", "0.05"]
    titles = {"0.95": "95/5", "0.50": "50/50", "0.05": "5/95"}

    for ratio in order:
        if ratio not in data:
            print(f"Нет данных для ratio={ratio}, пропуск")
            continue
        passes = data[ratio]
        xs = sorted(passes.keys())
        y_get = [passes[x][0] for x in xs]
        y_post = [passes[x][1] for x in xs]

        plt.figure(figsize=(9, 5))
        plt.plot(xs, y_get, marker="o", linewidth=2, label="GET avg (ms)")
        plt.plot(xs, y_post, marker="o", linewidth=2, label="POST avg (ms)")
        plt.title(
            f"LAB16 Variant 1 retry+killer ({titles[ratio]} POST/GET): avg duration"
        )
        plt.xlabel("Pass")
        plt.ylabel("Average request duration (ms)")
        plt.xticks(xs)
        plt.grid(alpha=0.3)
        plt.legend()
        plt.tight_layout()
        safe = ratio.replace(".", "p")
        out_png = OUT_DIR / f"lab16-v1-graph-r{safe}-pass-get-vs-post-avg.png"
        plt.savefig(out_png, dpi=160)
        plt.close()
        print(f"Saved: {out_png}")

    print("Done.")


if __name__ == "__main__":
    main()
