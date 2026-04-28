import glob
import json
import re


def read_avg_metric(metrics: dict, name: str) -> float:
    """
    Support both k6 summary formats:
    - metrics[name]["avg"]
    - metrics[name]["values"]["avg"]
    """
    metric = metrics.get(name, {})
    if "avg" in metric and metric["avg"] is not None:
        return float(metric["avg"])
    return float(metric.get("values", {}).get("avg", 0.0))


def main() -> None:
    rows = ["cpu,ratio,post_avg_ms,get_avg_ms,http_avg_ms,failed_pct,requests"]

    for path in sorted(glob.glob("k6/results/lab6_cpu_local/summary-cpu-*-ratio-*.json")):
        match = re.search(r"summary-cpu-(.*)-ratio-(.*)\.json$", path)
        if not match:
            continue

        cpu, ratio = match.group(1), match.group(2)

        with open(path, "r", encoding="utf-8") as file:
            data = json.load(file)

        metrics = data.get("metrics", {})
        post_avg_ms = read_avg_metric(metrics, "post_req_duration")
        get_avg_ms = read_avg_metric(metrics, "get_req_duration")
        http_avg_ms = read_avg_metric(metrics, "http_req_duration")
        failed_pct = metrics.get("http_req_failed", {}).get("value", 0) * 100
        requests = metrics.get("http_reqs", {}).get("count", 0)

        rows.append(
            f"{cpu},{ratio},{post_avg_ms},{get_avg_ms},{http_avg_ms},{failed_pct},{requests}"
        )

    output_path = "k6/results/lab6_cpu_local/lab6_cpu_local_results.csv"
    with open(output_path, "w", encoding="utf-8") as file:
        file.write("\n".join(rows) + "\n")

    print(f"saved: {output_path}")


if __name__ == "__main__":
    main()
