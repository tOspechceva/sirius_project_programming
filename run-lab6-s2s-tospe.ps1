$ErrorActionPreference = "Stop"

# === Config ===
$Hl11User = "hl"
$Hl11Host = "hlssh.zil.digital"
$Hl11Port = 2311

$Hl06UserAtHost = "hl@10.60.3.36"
$BaseUrl = "http://10.60.3.36:8082"
$Vus = 10
$Ratios = @("0.05", "0.50", "0.95")
$Cpus = @("0.5", "1.0", "1.5", "2.0")

$K6Dir = "~/lab6_k6_tospe"
$ResDir = "~/lab6_k6_results_tospe"

$LocalOutDir = "C:\Users\tospe\OneDrive\Рисунки\laba_rov"
New-Item -ItemType Directory -Force -Path $LocalOutDir | Out-Null

Write-Host "Step 1/3: Run server-to-server k6 on hl11"
Write-Host "You may be asked for hl11 password once."

$remoteScript = @'
set -euo pipefail

K6_DIR=~/lab6_k6_tospe
RES_DIR=~/lab6_k6_results_tospe
BASE_URL="http://10.60.3.36:8082"
VUS=10
RATIOS=("0.05" "0.50" "0.95")
CPUS=("0.5" "1.0" "1.5" "2.0")
HL06="hl@10.60.3.36"
HL06_PASSWORD='highload++course'

if ! command -v sshpass >/dev/null 2>&1; then
  echo "[hl11] sshpass is required for password-based hl06 access."
  echo "Install once on hl11: sudo apt update && sudo apt install -y sshpass"
  exit 1
fi

SCP_HL06="sshpass -p \"$HL06_PASSWORD\" scp -o StrictHostKeyChecking=accept-new"
SSH_HL06="sshpass -p \"$HL06_PASSWORD\" ssh -o StrictHostKeyChecking=accept-new"

echo "[hl11] Prepare directories"
rm -rf "$K6_DIR"
mkdir -p "$K6_DIR" "$RES_DIR"

echo "[hl11] Copy k6 folder from hl06"
eval "$SCP_HL06 -r $HL06:~/sirius_project_programming/k6/* \"$K6_DIR\"/"
ls -lah "$K6_DIR"

for cpu in "${CPUS[@]}"; do
  echo "=== CPU $cpu ==="
  eval "$SSH_HL06 $HL06 \"docker update --cpus $cpu hl-module1-app\""

  until curl -sf "$BASE_URL/api/users" >/dev/null; do
    echo "waiting app..."
    sleep 2
  done

  for ratio in "${RATIOS[@]}"; do
    out="$RES_DIR/tospe-summary-cpu-${cpu}-ratio-${ratio}.json"
    echo "RUN cpu=$cpu ratio=$ratio -> $out"

    k6 run "$K6_DIR/load-test.js" \
      -e BASE_URL="$BASE_URL" \
      -e TARGET_VUS="$VUS" \
      -e POST_POOL_RATIO="$ratio" \
      -e RAMP_UP=15s \
      -e STEADY_DURATION=40s \
      -e RAMP_DOWN=15s \
      --summary-export "$out"
  done
done

echo "[hl11] Build CSV and 3 charts (2 lines each: POST/GET)"
python3 - <<'PY'
import csv, glob, json, os, re
from collections import defaultdict
import matplotlib.pyplot as plt

res_dir = os.path.expanduser("~/lab6_k6_results_tospe")
json_paths = sorted(glob.glob(os.path.join(res_dir, "tospe-summary-cpu-*-ratio-*.json")))

rows = ["cpu,ratio,post_avg_ms,get_avg_ms,http_avg_ms,failed_pct,requests"]
grouped = defaultdict(list)

def avg(metrics, name):
    m = metrics.get(name, {})
    if "avg" in m:
        return float(m["avg"])
    return float(m.get("values", {}).get("avg", 0.0))

for p in json_paths:
    m = re.search(r"tospe-summary-cpu-(.*)-ratio-(.*)\.json$", p)
    if not m:
        continue
    cpu = float(m.group(1))
    ratio = m.group(2)
    data = json.load(open(p, encoding="utf-8"))
    metrics = data.get("metrics", {})
    post_avg = avg(metrics, "post_req_duration")
    get_avg = avg(metrics, "get_req_duration")
    http_avg = avg(metrics, "http_req_duration")
    failed_pct = float(metrics.get("http_req_failed", {}).get("value", 0.0)) * 100.0
    reqs = int(metrics.get("http_reqs", {}).get("count", 0))
    rows.append(f"{cpu},{ratio},{post_avg},{get_avg},{http_avg},{failed_pct},{reqs}")
    grouped[ratio].append((cpu, post_avg, get_avg))

csv_path = os.path.join(res_dir, "tospe_lab6_s2s_results.csv")
open(csv_path, "w", encoding="utf-8").write("\n".join(rows) + "\n")
print("saved:", csv_path)

labels = {"0.05": "5/95", "0.50": "50/50", "0.95": "95/5"}
suffix = {"0.05": "5_95", "0.50": "50_50", "0.95": "95_5"}

for ratio in ("0.05", "0.50", "0.95"):
    pts = sorted(grouped.get(ratio, []), key=lambda x: x[0])
    if not pts:
        continue
    xs = [x for x, _, _ in pts]
    ys_post = [y for _, y, _ in pts]
    ys_get = [y for _, _, y in pts]
    plt.figure(figsize=(9, 5))
    plt.plot(xs, ys_post, marker="o", label="POST avg (ms)")
    plt.plot(xs, ys_get, marker="s", label="GET avg (ms)")
    plt.title(f"LAB6 server->server | VUS=const | write/read={labels[ratio]}")
    plt.xlabel("CPU cores (step 0.5)")
    plt.ylabel("Average response time (ms)")
    plt.grid(True, alpha=0.3)
    plt.legend()
    plt.tight_layout()
    out = os.path.join(res_dir, f"tospe_lab6_s2s_ratio_{suffix[ratio]}_2lines.png")
    plt.savefig(out, dpi=150)
    plt.close()
    print("saved:", out)
PY

ls -lah "$RES_DIR"
'@

# Write script to a temporary local file, upload, then execute on hl11.
$tmpScript = Join-Path $env:TEMP "tospe_lab6_s2s_remote.sh"
Set-Content -Path $tmpScript -Value $remoteScript -Encoding UTF8

scp -P $Hl11Port $tmpScript "$Hl11User@$Hl11Host`:/tmp/tospe_lab6_s2s_remote.sh"
ssh -p $Hl11Port "$Hl11User@$Hl11Host" "bash /tmp/tospe_lab6_s2s_remote.sh"
ssh -p $Hl11Port "$Hl11User@$Hl11Host" "rm -f /tmp/tospe_lab6_s2s_remote.sh"
Remove-Item -Force $tmpScript

Write-Host "Step 2/3: Download result CSV + PNG files"
scp -P $Hl11Port "$Hl11User@$Hl11Host`:~/lab6_k6_results_tospe/tospe_lab6_s2s_results.csv" "$LocalOutDir\"
scp -P $Hl11Port "$Hl11User@$Hl11Host`:~/lab6_k6_results_tospe/tospe_lab6_s2s_ratio_*_2lines.png" "$LocalOutDir\"

Write-Host "Step 3/3: Done."
Write-Host "Results saved to: $LocalOutDir"
