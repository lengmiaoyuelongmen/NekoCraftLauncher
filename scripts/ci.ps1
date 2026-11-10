# 一键跑完整验证链：构建 + 单测 → 架构守卫 → 冒烟（含 UI 自检）。
# 任何一步失败都立即退出，非 0 退出码可直接给 CI 用。
param(
    [switch]$SkipBuild,
    [switch]$SkipSmoke,
    [switch]$Nightly
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Push-Location $root
try {
    $script:started = Get-Date
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
        if ($SkipBuild) { $buildText2 = "跳过" } else { $buildText2 = "通过" }
        $lines = @(
            ("时间: " + (Get-Date -Format "yyyy-MM-dd HH:mm:ss")),
            ("耗时: " + [int]((Get-Date) - $script:started).TotalSeconds + " 秒"),
            ("构建/单测: " + $buildText2),
            "架构守卫: 通过",
            "冒烟: 跳过",
            "结果: PASS"
        )
        Set-Content -Path "target\ci-summary.txt" -Value $lines -Encoding UTF8
        Write-Host ""
        Write-Host "CI PASS：构建、单测、架构守卫通过（未跑冒烟）" -ForegroundColor Green
        exit 0
    }
    if ($Nightly) {
        Write-Host "==> 3/3 冒烟 + UI 自检 + 夜间基线比对" -ForegroundColor Cyan
    } else {
        Write-Host "==> 3/3 冒烟 + UI 自检" -ForegroundColor Cyan
    }
    & powershell -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot "smoke-test.ps1") -SkipBuild -HoverCheck
    if ($LASTEXITCODE -ne 0) { throw "冒烟失败" }

    if ($Nightly) {
        Write-Host "==> 夜间基线比对" -ForegroundColor Cyan
        & powershell -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot "check-baseline.ps1") -Launch
        if ($LASTEXITCODE -ne 0) { throw "夜间基线比对失败" }
    }
    $summary = "target\ci-summary.txt"
    if ($SkipBuild) { $buildText = "跳过" } else { $buildText = "通过" }
    if ($SkipSmoke) { $smokeText = "跳过" } else { $smokeText = "通过" }
    if ($Nightly) { $nightlyText = "通过" } else { $nightlyText = "未运行" }
    $lines = @(
        ("时间: " + (Get-Date -Format "yyyy-MM-dd HH:mm:ss")),
        ("耗时: " + [int]((Get-Date) - $script:started).TotalSeconds + " 秒"),
        ("构建/单测: " + $buildText),
        "架构守卫: 通过",
        ("冒烟: " + $smokeText),
        ("夜间基线: " + $nightlyText),
        "结果: PASS"
    )
    Set-Content -Path $summary -Value $lines -Encoding UTF8
    Write-Host ""
    Write-Host "CI PASS：构建、单测、架构守卫、冒烟、UI 自检全部通过" -ForegroundColor Green
    Write-Host ("摘要: " + (Resolve-Path $summary).Path) -ForegroundColor DarkGray
    exit 0
} catch {
    Write-Host ""
    Write-Host ("CI FAIL：{0}" -f $_.Exception.Message) -ForegroundColor Red
    $summary = Join-Path (Get-Location) "target\ci-summary.txt"
    $lines = @(
        ("时间: " + (Get-Date -Format "yyyy-MM-dd HH:mm:ss")),
        ("耗时: " + [int]((Get-Date) - $script:started).TotalSeconds + " 秒"),
        ("失败原因: " + $_.Exception.Message),
        "结果: FAIL"
    )
    try { Set-Content -Path $summary -Value $lines -Encoding UTF8 } catch { }
    exit 1
} finally {
    Pop-Location
}
