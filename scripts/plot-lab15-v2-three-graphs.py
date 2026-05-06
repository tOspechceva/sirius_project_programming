#!/usr/bin/env python3
"""
LAB15 variant 2: три графика (по одному на каждый POST pool ratio).
На каждом: по оси X — Pass (1, 2), линии — GET и POST (avg из k6 summary).

Использование:
  python3 scripts/plot-lab15-v2-three-graphs.py
  OUT_DIR=~/lab15_matrix_v2 python3 scripts/plot-lab15-v2-three-graphs.py
"""
from __future__ import annotations

import json
import os
import re
from pathlib import Path

import matplotlib.pyplot as plt

OUT_DIR = Path(os.environ.get("OUT_DIR", Path.home() / "lab15_matrix_v2"))

# summary-lab15-v2-pass1-r0p95-vus30-....json
RX = re.compile(r"summary-lab15-v2-pass(?P<pass>\d+)-r(?P<ratio>\d+p\d+)-")


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


def load_by_ratio() -> dict[str, dict[int, tuple[float, float, str]]]:
    """
    ratio_key '0.95' -> pass -> (get_avg, post_avg, filename)
    """
    out: dict[str, dict[int, tuple[float, float, str]]] = {}
    for f in sorted(OUT_DIR.glob("summary-lab15-v2-pass*-r*.json")):
        m = RX.search(f.name)
        if not m:
            continue
        pass_num = int(m.group("pass"))
        ratio_token = m.group("ratio")
        ratio = ratio_token.replace("p", ".")
        raw = json.loads(f.read_text(encoding="utf-8"))
        metrics = raw.get("metrics") or {}
        g = metric_avg(metrics, "get_req_duration")
        p = metric_avg(metrics, "post_req_duration")
        if g is None or p is None:
            print(f"skip (no avg): {f.name}")
            continue
        out.setdefault(ratio, {})[pass_num] = (g, p, f.name)
    return out


def main() -> None:
    data = load_by_ratio()
    if not data:
        raise SystemExit(f"Нет summary-lab15-v2-pass*-r*.json в {OUT_DIR}")

    # порядок профилей 95/5, 50/50, 5/95
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
        plt.title(f"LAB15 Variant 2 ({titles[ratio]} POST/GET): avg duration")
        plt.xlabel("Pass")
        plt.ylabel("Average request duration (ms)")
        plt.xticks(xs)
        plt.grid(alpha=0.3)
        plt.legend()
        plt.tight_layout()
        safe = ratio.replace(".", "p")
        out_png = OUT_DIR / f"lab15-v2-graph-r{safe}-pass-get-vs-post-avg.png"
        plt.savefig(out_png, dpi=160)
        plt.close()
        print(f"Saved: {out_png}")

    print("Done.")


if __name__ == "__main__":
    main()
