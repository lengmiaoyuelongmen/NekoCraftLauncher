# 一键跑完整验证链：构建 + 单测 → 架构守卫 → 冒烟（含 UI 自检）→（可选）夜间基线比对。
# 任何一步失败都立即退出，非 0 退出码可直接给 CI 用。
# 结束时写 target\ci-summary.txt，格式为 key=value，便于 CI 直接解析。
param(
    [switch]$SkipBuild,
    [switch]$SkipSmoke,
    [switch]$Nightly,
    [double]$BaselineMaxDiff = 2.0
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Push-Location $root

$script:started = Get-Date
$script:evidence = ""
$script:evidenceHeat = ""
$script:baselinePercent = ""
$script:smokeResult = "fail"
$script:nightlyResult = "fail"

function Write-CiSummary {
    param([string]$result, [string]$failure = "")

    $baselineFile = "docs\images\settings-settled.png"
    $baselineHash = ""
    $baselineTime = ""
    if (Test-Path $baselineFile) {
        $baselineHash = (Get-FileHash $baselineFile -Algorithm SHA256).Hash.Substring(0, 12)
        $baselineTime = (Get-Item $baselineFile).LastWriteTime.ToString("yyyy-MM-dd HH:mm:ss")
    }
    $lines = @(
        ("time=" + (Get-Date -Format "yyyy-MM-dd HH:mm:ss")),
        ("duration_sec=" + [int]((Get-Date) - $script:started).TotalSeconds),
        ("build=" + $(if ($SkipBuild) { "skipped" } else { "pass" })),
        ("architecture=pass"),
        ("smoke=" + $(if ($SkipSmoke) { "skipped" } elseif ($script:smokeResult -eq "pass") { "pass" } else { "fail" })),
        ("nightly=" + $(if (-not $Nightly) { "not-run" } elseif ($script:nightlyResult -eq "pass") { "pass" } else { "fail" })),
        ("baseline_hash=" + $baselineHash),
        ("baseline_mtime=" + $baselineTime),
        ("baseline_diff_percent=" + $script:baselinePercent),
        ("result=" + $result)
    )
    if ($failure) { $lines += ("failure=" + $failure) }
    if ($script:evidence) { $lines += ("evidence=" + $script:evidence) }
    if ($script:evidenceHeat) { $lines += ("heatmap=" + $script:evidenceHeat) }
    try { Set-Content -Path "target\ci-summary.txt" -Value $lines -Encoding UTF8 } catch { }
    return "target\ci-summary.txt"
}

try {
    if (-not $SkipBuild) {
        Write-Host "==> 1/3 构建与单测" -ForegroundColor Cyan
        $env:JAVA_HOME = if ($env:JAVA_HOME) { $env:JAVA_HOME } else { "C:\Program Files\Java\jdk-21.0.11" }
        & .\mvnw.cmd -o -q clean package
        if ($LASTEXITCODE -ne 0) { throw "构建失败（exit $LASTEXITCODE）" }
    } else {
        Write-Host "==> 1/3 构建已跳过（-SkipBuild）" -ForegroundColor DarkGray
    }

    Write-Host "==> 2/3 架构守卫" -ForegroundColor Cyan
    & powershell -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot "check-architecture.ps1")
    if ($LASTEXITCODE -ne 0) { throw "架构守卫失败" }

    if ($SkipSmoke) {
        Write-Host "==> 3/3 冒烟已跳过（-SkipSmoke）" -ForegroundColor DarkGray
        $summaryPath = Write-CiSummary -result "pass"
        Write-Host ""
        Write-Host "CI PASS：构建、单测、架构守卫通过（未跑冒烟）" -ForegroundColor Green
        Write-Host ("摘要: " + (Resolve-Path $summaryPath).Path) -ForegroundColor DarkGray
        exit 0
    }

    if ($Nightly) {
        Write-Host "==> 3/3 冒烟 + UI 自检 + 夜间基线比对" -ForegroundColor Cyan
    } else {
        Write-Host "==> 3/3 冒烟 + UI 自检" -ForegroundColor Cyan
    }
    & powershell -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot "smoke-test.ps1") -SkipBuild -HoverCheck
    if ($LASTEXITCODE -ne 0) { throw "冒烟失败" }
    $script:smokeResult = "pass"

    if ($Nightly) {
        Write-Host "==> 夜间基线比对" -ForegroundColor Cyan
        $baselineOut = & powershell -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot "check-baseline.ps1") -Launch -MaxDiffPercent $BaselineMaxDiff 2>&1
        $baselineOut | ForEach-Object { Write-Host $_ }
        $pctLine = $baselineOut | Select-String '差异 \d+ 点 = ([\d.]+)%'
        if ($pctLine) { $script:baselinePercent = $pctLine.Matches[0].Groups[1].Value }
        $cmp = $baselineOut | Select-String '左右对照: (.+)$'
        if ($cmp) { $script:evidence = $cmp.Matches[0].Groups[1].Value.Trim() }
        $heat = $baselineOut | Select-String '差异热力图: (.+)$'
        if ($heat) { $script:evidenceHeat = $heat.Matches[0].Groups[1].Value.Trim() }
        if ($LASTEXITCODE -ne 0) { throw "夜间基线比对失败" }
        $script:nightlyResult = "pass"
    }

    $summaryPath = Write-CiSummary -result "pass"
    Write-Host ""
    Write-Host "CI PASS：构建、单测、架构守卫、冒烟、UI 自检全部通过" -ForegroundColor Green
    Write-Host ("摘要: " + (Resolve-Path $summaryPath).Path) -ForegroundColor DarkGray
    exit 0
} catch {
    Write-Host ""
    Write-Host ("CI FAIL：{0}" -f $_.Exception.Message) -ForegroundColor Red
    $summaryPath = Write-CiSummary -result "fail" -failure $_.Exception.Message
    if (Test-Path $summaryPath) {
        Write-Host ("摘要: " + (Resolve-Path $summaryPath).Path) -ForegroundColor DarkGray
    }
    exit 1
} finally {
    Pop-Location
}
