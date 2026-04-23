const fs = require("fs");
const path = require("path");

const resultsDir = path.join(__dirname, "results");
const outCsv = path.join(resultsDir, "avg-response-vs-vus.csv");
const outHtml = path.join(resultsDir, "avg-response-vs-vus.html");
const outMd = path.join(resultsDir, "avg-response-vs-vus.md");

if (!fs.existsSync(resultsDir)) {
    throw new Error("results directory does not exist");
}

const files = fs
    .readdirSync(resultsDir)
    .filter((name) => /^summary-vus-\d+\.json$/.test(name));

if (files.length === 0) {
    throw new Error("No summary-vus-*.json files found in k6/results");
}

const points = files
    .map((file) => {
        const vus = Number(file.match(/^summary-vus-(\d+)\.json$/)[1]);
        const raw = fs.readFileSync(path.join(resultsDir, file), "utf8");
        const json = JSON.parse(raw);
        const avgMs = json?.metrics?.http_req_duration?.values?.avg;
        if (typeof avgMs !== "number") {
            throw new Error(`http_req_duration avg not found in ${file}`);
        }
        return { vus, avgMs: Number(avgMs.toFixed(2)) };
    })
    .sort((a, b) => a.vus - b.vus);

const csvLines = ["vus,avg_response_ms", ...points.map((p) => `${p.vus},${p.avgMs}`)];
fs.writeFileSync(outCsv, csvLines.join("\n"), "utf8");

const mdLines = [
    "| VUs | Avg response (ms) |",
    "| --- | --- |",
    ...points.map((p) => `| ${p.vus} | ${p.avgMs} |`)
];
fs.writeFileSync(outMd, mdLines.join("\n"), "utf8");

const maxX = Math.max(...points.map((p) => p.vus));
const maxY = Math.max(...points.map((p) => p.avgMs));
const width = 900;
const height = 520;
const chartX = 70;
const chartY = 40;
const chartW = width - 120;
const chartH = height - 120;

const scaled = points.map((p) => {
    const x = chartX + (p.vus / maxX) * chartW;
    const y = chartY + chartH - (p.avgMs / maxY) * chartH;
    return { ...p, x, y };
});

const polyline = scaled.map((p) => `${p.x},${p.y}`).join(" ");

const dots = scaled
    .map(
        (p) => `<circle cx="${p.x}" cy="${p.y}" r="4" fill="#2563eb">
  <title>VUs=${p.vus}, avg=${p.avgMs}ms</title>
</circle>`
    )
    .join("\n");

const labels = scaled
    .map(
        (p) =>
            `<text x="${p.x - 16}" y="${p.y - 10}" font-size="12" fill="#111827">${p.avgMs}ms</text>`
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

const html = `<!doctype html>
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
    <h1>K6: Avg response time vs VUs</h1>
    <p>Scenario: POST /api/users and GET /api/progress/users, ratio 50/50 (configurable).</p>
    <svg width="${width}" height="${height}" viewBox="0 0 ${width} ${height}">
      <rect x="${chartX}" y="${chartY}" width="${chartW}" height="${chartH}" fill="#ffffff" stroke="#d1d5db" />
      ${yGrid}
      ${xTicks}
      <polyline fill="none" stroke="#2563eb" stroke-width="2.5" points="${polyline}" />
      ${dots}
      ${labels}
      <text x="${width / 2 - 50}" y="${height - 20}" font-size="14" fill="#111827">Load (VUs)</text>
      <text x="14" y="${height / 2}" transform="rotate(-90 14,${height / 2})" font-size="14" fill="#111827">
        Avg response (ms)
      </text>
    </svg>
  </div>
</body>
</html>`;

fs.writeFileSync(outHtml, html, "utf8");

console.log(`Generated:\n- ${outCsv}\n- ${outMd}\n- ${outHtml}`);
