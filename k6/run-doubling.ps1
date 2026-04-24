$ErrorActionPreference = "Stop"

$vusPoints = @(10, 20, 40, 80, 160)
$resultsDir = Join-Path $PSScriptRoot "results"

if (-not (Test-Path $resultsDir)) {
    New-Item -ItemType Directory -Path $resultsDir | Out-Null
}

foreach ($vus in $vusPoints) {
    $summaryFile = "/scripts/results/summary-vus-$vus.json"
    Write-Host "Running k6 test for total VUs=$vus (POST/GET split by pools)"

    docker compose run --rm k6 run /scripts/load-test.js `
        -e BASE_URL=http://app:8081 `
        -e TARGET_VUS=$vus `
        -e POST_POOL_RATIO=0.5 `
        -e RAMP_UP=20s `
        -e STEADY_DURATION=40s `
        -e RAMP_DOWN=15s `
        --summary-export $summaryFile
}

Write-Host "Generating chart artifacts..."
node "$PSScriptRoot/generate-chart.js"
Write-Host "Done. See k6/results."
