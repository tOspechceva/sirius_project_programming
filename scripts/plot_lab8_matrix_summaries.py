#!/usr/bin/env python3
"""
Строит PNG-графики из k6 summary после матрицы CPU × POST_POOL_RATIO.
Имена файлов как у hl11-run-lab8-cpu-post-get-matrix.sh:
  summary-cpu0.5-r0p05-vus30-....json
  (r0p05 = ratio 0.05: «p» вместо точки)

Пример (на локалке, в каталоге с json):
  python scripts/plot_lab8_matrix_summaries.py .
  python scripts/plot_lab8_matrix_summaries.py ~/Downloads/lab8_matrix
"""
from __future__ import annotations

import argparse
import json
import re
from collections import defaultdict
from pathlib import Path

import matplotlib.pyplot as plt

# summary-cpu0.5-r0p05-vus30-20260504T084229Z.json
FNAME_RE = re.compile(
    r"summary-cpu(?P<cpu>[\d.]+)-r(?P<ratio_enc>[\dp]+)-vus(?P<vus>\d+)-",
    re.IGNORECASE,
)


def decode_ratio(ratio_enc: str) -> float:
    return float(ratio_enc.replace("p", ".", 1) if "p" in ratio_enc else ratio_enc)


def metric_avg(data: dict, key: str) -> float | None:
    m = data.get("metrics", {}).get(key)
    if not m:
        return None
    v = m.get("avg")
    return float(v) if isinstance(v, (int, float)) else None


def load_rows(directory: Path) -> list[tuple[float, float, dict]]:
    rows: list[tuple[float, float, dict]] = []
    for path in sorted(directory.glob("summary-cpu*-r*-vus*.json")):
        m = FNAME_RE.match(path.name)
        if not m:
            continue
        cpu = float(m.group("cpu"))
        ratio = decode_ratio(m.group("ratio_enc"))
        with path.open(encoding="utf-8") as f:
            data = json.load(f)
        rows.append((cpu, ratio, data))
    return rows


def main() -> None:
    ap = argparse.ArgumentParser(description="LAB8 matrix: charts from k6 summary JSON files")
    ap.add_argument(
        "directory",
        type=Path,
        default=Path("."),
        nargs="?",
        help="Каталог с summary-cpu*.json (по умолчанию текущий)",
    )
    ap.add_argument(
        "-o",
        "--output-dir",
        type=Path,
        default=None,
        help="Куда сохранить PNG (по умолчанию — тот же каталог, что и json)",
    )
    args = ap.parse_args()
    src = args.directory.resolve()
    out_dir = (args.output_dir or src).resolve()
    out_dir.mkdir(parents=True, exist_ok=True)

    rows = load_rows(src)
    if not rows:
        raise SystemExit(f"Не найдено summary-cpu*-r*-vus*.json в {src}")

    # series[ratio_label] -> list of (cpu, http_avg, post_avg, get_avg) sorted by cpu
    series: dict[str, list[tuple[float, float | None, float | None, float | None]]] = defaultdict(list)
    ratio_labels = {
        "0.05": "5% POST / 95% GET",
        "0.50": "50% POST / 50% GET",
        "0.95": "95% POST / 5% GET",
    }

    for cpu, ratio, data in rows:
        rk = f"{ratio:.2f}"
        http_avg = metric_avg(data, "http_req_duration")
        post_avg = metric_avg(data, "post_req_duration")
        get_avg = metric_avg(data, "get_req_duration")
        series[rk].append((cpu, http_avg, post_avg, get_avg))

    for key, points in series.items():
        points.sort(key=lambda x: x[0])
        xs = [p[0] for p in points]
        label = ratio_labels.get(key, f"ratio {key}")

        plt.figure(figsize=(9, 5))
        if all(p[1] is not None for p in points):
            plt.plot(xs, [p[1] for p in points], marker="o", label="http_req_duration avg (ms)")
        if all(p[2] is not None for p in points):
            plt.plot(xs, [p[2] for p in points], marker="s", label="post_req_duration avg (ms)")
        if all(p[3] is not None for p in points):
            plt.plot(xs, [p[3] for p in points], marker="^", label="get_req_duration avg (ms)")
        plt.title(f"LAB8 matrix: latency vs CPU\n{label}")
        plt.xlabel("CPU limit (docker)")
        plt.ylabel("Average (ms)")
        plt.grid(True, alpha=0.3)
        plt.legend()
        plt.tight_layout()
        safe = key.replace(".", "_")
        dest = out_dir / f"lab8_matrix_ratio_{safe}.png"
        plt.savefig(dest, dpi=150)
        plt.close()
        print("saved:", dest)

    print("done, files:", len(rows))


if __name__ == "__main__":
    main()
