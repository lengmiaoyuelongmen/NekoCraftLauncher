# 刷新 docs/images 下的视觉基线图。
# 需要启动器已打开并停在启动页。
param(
    [string]$Title = "Neko Craft Launcher",
    [string]$OutFile = "docs\images\motion-contact-sheet.png"
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Push-Location $root
try {
    $outDir = "target\baseline"
    Write-Host "==> 连拍设置页切换并生成九宫格" -ForegroundColor Cyan
    & powershell -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot "capture-motion.ps1") `
        -Title $Title -ClickName "设置" -Frames 30 -IntervalMs 40 -ContactSheet -OutDir $outDir
    if ($LASTEXITCODE -ne 0) { throw "连拍失败（启动器是否已打开并停在启动页？）" }

    $sheet = Join-Path $outDir "contact-sheet.png"
    if (-not (Test-Path $sheet)) { throw "没有生成 contact-sheet.png" }

    $dest = Join-Path $root $OutFile
    $destDir = Split-Path -Parent $dest
    if (-not (Test-Path $destDir)) { New-Item -ItemType Directory -Force -Path $destDir | Out-Null }
    Copy-Item $sheet $dest -Force
    Write-Host ("基线已更新: {0}" -f $dest) -ForegroundColor Green
    Write-Host "请肉眼确认过渡帧合理后再提交（见 docs/images/README.md）" -ForegroundColor DarkGray
    exit 0
} catch {
    Write-Host ("刷新基线失败：{0}" -f $_.Exception.Message) -ForegroundColor Red
    exit 1
} finally {
    Pop-Location
}
