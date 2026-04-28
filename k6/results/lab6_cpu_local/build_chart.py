import csv
from collections import defaultdict

import matplotlib.pyplot as plt


def main() -> None:
    csv_path = "k6/results/lab6_cpu_local/lab6_cpu_local_results.csv"
    output_dir = "k6/results/lab6_cpu_local"

    # Group by ratio, each point holds (cpu, post_avg_ms, get_avg_ms, failed_pct).
    series = defaultdict(list)
    with open(csv_path, newline="", encoding="utf-8") as file:
        reader = csv.DictReader(file)
        for row in reader:
            series[row["ratio"]].append(
                (
                    float(row["cpu"]),
                    float(row["post_avg_ms"]),
                    float(row["get_avg_ms"]),
                    float(row.get("failed_pct", 0)),
                )
            )

    labels = {"0.05": "5/95", "0.50": "50/50", "0.95": "95/5"}
    file_suffixes = {"0.05": "5_95", "0.50": "50_50", "0.95": "95_5"}

    for ratio in ("0.05", "0.50", "0.95"):
        points = sorted(series.get(ratio, []), key=lambda item: item[0])
        if not points:
            print(f"skip ratio {ratio}: no data")
            continue

        xs = [item[0] for item in points]
        post_ys = [item[1] for item in points]
        get_ys = [item[2] for item in points]
        has_failed = any(item[3] > 0 for item in points)

        plt.figure(figsize=(9, 5))
        plt.plot(xs, post_ys, marker="o", label="POST avg (ms)")
        plt.plot(xs, get_ys, marker="s", label="GET avg (ms)")
        plt.title(
            "LAB6: Response time vs CPU\n"
            f"VUS = const, write/read = {labels.get(ratio, ratio)}"
            + (" (contains failed requests)" if has_failed else "")
        )
        plt.xlabel("CPU cores (step 0.5)")
        plt.ylabel("Average response time (ms)")
        plt.grid(True, alpha=0.3)
        plt.legend()
        plt.tight_layout()

        output_path = f"{output_dir}/lab6_cpu_local_ratio_{file_suffixes.get(ratio, ratio)}.png"
        plt.savefig(output_path, dpi=150)
        plt.close()
        print(f"saved: {output_path}")


if __name__ == "__main__":
    main()
