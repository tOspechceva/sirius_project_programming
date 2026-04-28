$ErrorActionPreference = "Stop"

$VmHost = "hl@hlssh.zil.digital"
$VmPort = 2306
$VmRepo = "~/sirius_project_programming"

$VUS = 30
$Ratios = @("0.05", "0.50", "0.95")
$Cpus = @("0.5", "1.0", "1.5", "2.0")
$SeedUsers = 200
$SeedLessons = 150
$SeedProgress = 400
$SeedBaseUrl = "http://localhost:18080"

$LocalResults = "k6/results/lab6_cpu_local"
New-Item -ItemType Directory -Force -Path $LocalResults | Out-Null

function Reset-And-SeedData {
  param(
    [string]$BaseUrl,
    [int]$UsersCount,
    [int]$LessonsCount,
    [int]$ProgressCount
  )

  Write-Host "Reset DB via API..."
  python "scripts/lab5_seed.py" --endpoint all --clear --base-url $BaseUrl
  if ($LASTEXITCODE -ne 0) {
    throw "Failed to clear data."
  }

  Write-Host "Seed users: $UsersCount"
  python "scripts/lab5_seed.py" --endpoint users --count $UsersCount --base-url $BaseUrl
  if ($LASTEXITCODE -ne 0) {
    throw "Failed to seed users."
  }

  Write-Host "Seed lessons: $LessonsCount"
  python "scripts/lab5_seed.py" --endpoint lessons --count $LessonsCount --base-url $BaseUrl
  if ($LASTEXITCODE -ne 0) {
    throw "Failed to seed lessons."
  }

  Write-Host "Seed progress: $ProgressCount"
  python "scripts/lab5_seed.py" --endpoint progress --count $ProgressCount --base-url $BaseUrl
  if ($LASTEXITCODE -ne 0) {
    throw "Failed to seed progress."
  }
}

foreach ($cpu in $Cpus) {
  Write-Host "=== CPU $cpu on VM ==="

  $remoteCmd = "cd $VmRepo && sed -Ei 's/(cpus: `").*(`")/\1$cpu\2/' docker-compose.yml && docker compose --compatibility up -d --build app"
  ssh -p $VmPort $VmHost $remoteCmd

  do {
    Start-Sleep -Seconds 2
    try {
      $r = Invoke-WebRequest -Uri "http://localhost:18080/v3/api-docs" -Method GET -TimeoutSec 20
      $ok = ($r.StatusCode -eq 200)
    } catch {
      $ok = $false
    }
    if (-not $ok) {
      Write-Host "waiting app for cpu=$cpu..."
    }
  } until ($ok)

  foreach ($ratio in $Ratios) {
    $summary = "/scripts/results/lab6_cpu_local/summary-cpu-$cpu-ratio-$ratio.json"
    Write-Host "RUN local->server cpu=$cpu ratio=$ratio"
    Reset-And-SeedData -BaseUrl $SeedBaseUrl -UsersCount $SeedUsers -LessonsCount $SeedLessons -ProgressCount $SeedProgress

    docker run --rm `
      -v "${PWD}\k6:/scripts" `
      grafana/k6:0.52.0 run /scripts/load-test.js `
      -e BASE_URL=http://host.docker.internal:18080 `
      -e TARGET_VUS=$VUS `
      -e POST_POOL_RATIO=$ratio `
      -e RAMP_UP=15s `
      -e STEADY_DURATION=40s `
      -e RAMP_DOWN=15s `
      --summary-export $summary
  }
}

Write-Host "Done: k6/results/lab6_cpu_local"
