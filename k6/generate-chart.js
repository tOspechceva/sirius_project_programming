const fs = require("fs");
const path = require("path");

const resultsDir = path.join(__dirname, "results");
const outCsv = path.join(resultsDir, "avg-response-vs-vus.csv");
const outMd = path.join(resultsDir, "avg-response-vs-vus.md");
const outHtml = path.join(resultsDir, "avg-response-vs-vus.html");

if (!fs.existsSync(resultsDir)) {
    throw new Error("results directory does not exist");
}

const files = fs
    .readdirSync(resultsDir)
    .filter((name) => /^summary-vus-\d+\.json$/.test(name));

if (files.length === 0) {
    throw new Error("No summary-vus-*.json files found in k6/results");
}

function extractAvg(metric, json, file) {
    const durationMetric = json?.metrics?.[metric];
    const avgMs = durationMetric?.avg ?? durationMetric?.values?.avg;
    if (typeof avgMs !== "number") {
        throw new Error(`${metric} avg not found in ${file}`);
    }
    return Number(avgMs.toFixed(2));
}

function buildPoints(metric) {
    return files
        .map((file) => {
            const vus = Number(file.match(/^summary-vus-(\d+)\.json$/)[1]);
            const raw = fs.readFileSync(path.join(resultsDir, file), "utf8");
            const json = JSON.parse(raw);
            return { vus, avgMs: extractAvg(metric, json, file) };
        })
        .sort((a, b) => a.vus - b.vus);
}

function buildHtml(postPoints, getPoints) {
    const points = postPoints;
    const maxX = Math.max(...points.map((p) => p.vus));
    const maxY = Math.max(
        ...postPoints.map((p) => p.avgMs),
        ...getPoints.map((p) => p.avgMs)
    );
    const width = 900;
    const height = 520;
    const chartX = 70;
    const chartY = 40;
    const chartW = width - 120;
    const chartH = height - 120;

    const scale = (series) => series.map((p) => {
        const x = chartX + (p.vus / maxX) * chartW;
        const y = chartY + chartH - (p.avgMs / maxY) * chartH;
        return { ...p, x, y };
    });
    const postScaled = scale(postPoints);
    const getScaled = scale(getPoints);

    const postPolyline = postScaled.map((p) => `${p.x},${p.y}`).join(" ");
    const getPolyline = getScaled.map((p) => `${p.x},${p.y}`).join(" ");

    const postDots = postScaled
        .map(
            (p) => `<circle cx="${p.x}" cy="${p.y}" r="4" fill="#2563eb">
  <title>POST: VUs=${p.vus}, avg=${p.avgMs}ms</title>
</circle>`
        )
        .join("\n");
    const getDots = getScaled
        .map(
            (p) => `<circle cx="${p.x}" cy="${p.y}" r="4" fill="#dc2626">
  <title>GET: VUs=${p.vus}, avg=${p.avgMs}ms</title>
</circle>`
        )
        .join("\n");

    const postLabels = postScaled
        .map(
            (p) => `<text x="${p.x - 16}" y="${p.y - 10}" font-size="11" fill="#1e3a8a">${p.avgMs}</text>`
        )
        .join("\n");
    const getLabels = getScaled
        .map(
            (p) => `<text x="${p.x - 16}" y="${p.y + 18}" font-size="11" fill="#7f1d1d">${p.avgMs}</text>`
        )
        .join("\n");

    const xTicks = points
        .map((p) => {
            const x = chartX + (p.vus / maxX) * chartW;
            return `<line x1="${x}" y1="${chartY + chartH}" x2="${x}" y2="${chartY + chartH + 6}" stroke="#374151"/>
<text x="${x - 10}" y="${chartY + chartH + 24}" font-size="12" fill="#111827">${p.vus}</text>`;
        })
        .join("\n");

    const yTicks = 5;
    const yGrid = Array.from({ length: yTicks + 1 }, (_, i) => {
        const ratio = i / yTicks;
        const y = chartY + chartH - ratio * chartH;
        const value = (ratio * maxY).toFixed(0);
        return `<line x1="${chartX}" y1="${y}" x2="${chartX + chartW}" y2="${y}" stroke="#e5e7eb"/>
<text x="${chartX - 45}" y="${y + 4}" font-size="12" fill="#111827">${value}</text>`;
    }).join("\n");

    return `<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8" />
  <title>K6 Avg Response vs VUs</title>
  <style>
    body { font-family: Arial, sans-serif; margin: 24px; color: #111827; }
    h1 { margin-bottom: 8px; }
    p { margin-top: 0; color: #374151; }
    .card { border: 1px solid #e5e7eb; border-radius: 8px; padding: 16px; max-width: 960px; }
  </style>
</head>
<body>
  <div class="card">
    <h1>K6: Avg response vs VUs (2 pools)</h1>
    <p>Blue line: POST /api/users, red line: GET /api/progress/users.</p>
    <svg width="${width}" height="${height}" viewBox="0 0 ${width} ${height}">
      <rect x="${chartX}" y="${chartY}" width="${chartW}" height="${chartH}" fill="#ffffff" stroke="#d1d5db" />
      ${yGrid}
      ${xTicks}
      <polyline fill="none" stroke="#2563eb" stroke-width="2.5" points="${postPolyline}" />
      <polyline fill="none" stroke="#dc2626" stroke-width="2.5" points="${getPolyline}" />
      ${postDots}
      ${getDots}
      ${postLabels}
      ${getLabels}
      <rect x="${chartX + 10}" y="${chartY + 10}" width="14" height="4" fill="#2563eb" />
      <text x="${chartX + 30}" y="${chartY + 16}" font-size="12" fill="#111827">POST pool</text>
      <rect x="${chartX + 120}" y="${chartY + 10}" width="14" height="4" fill="#dc2626" />
      <text x="${chartX + 140}" y="${chartY + 16}" font-size="12" fill="#111827">GET pool</text>
      <text x="${width / 2 - 50}" y="${height - 20}" font-size="14" fill="#111827">Load (VUs)</text>
      <text x="14" y="${height / 2}" transform="rotate(-90 14,${height / 2})" font-size="14" fill="#111827">
        Avg response (ms)
      </text>
    </svg>
  </div>
</body>
</html>`;
}

const postPoints = buildPoints("post_req_duration");
const getPoints = buildPoints("get_req_duration");

const byVus = new Map();
for (const p of postPoints) {
    byVus.set(p.vus, { vus: p.vus, postAvgMs: p.avgMs, getAvgMs: null });
}
for (const p of getPoints) {
    const existing = byVus.get(p.vus) || { vus: p.vus, postAvgMs: null, getAvgMs: null };
    existing.getAvgMs = p.avgMs;
    byVus.set(p.vus, existing);
}
const merged = Array.from(byVus.values()).sort((a, b) => a.vus - b.vus);

const csvLines = [
    "vus,post_avg_response_ms,get_avg_response_ms",
    ...merged.map((p) => `${p.vus},${p.postAvgMs ?? ""},${p.getAvgMs ?? ""}`)
];
fs.writeFileSync(outCsv, csvLines.join("\n"), "utf8");

const mdLines = [
    "| VUs | POST avg (ms) | GET avg (ms) |",
    "| --- | --- | --- |",
    ...merged.map((p) => `| ${p.vus} | ${p.postAvgMs ?? "-"} | ${p.getAvgMs ?? "-"} |`)
];
fs.writeFileSync(outMd, mdLines.join("\n"), "utf8");

const html = buildHtml(postPoints, getPoints);
fs.writeFileSync(outHtml, html, "utf8");

console.log(`Generated:
- ${outCsv}
- ${outMd}
- ${outHtml}`);
