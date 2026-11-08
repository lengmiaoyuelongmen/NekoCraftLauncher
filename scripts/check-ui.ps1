# 统一 UI 自检：窗口四角是否圆角 + 场景里是否还有"圆角底 + 方角边"的节点。
# 可单独跑，也可由 smoke-test.ps1 调用。
param(
    [string]$Title = "Neko Craft Launcher",
    [string]$LogPath = "",
    [switch]$SkipCorners
)

$ErrorActionPreference = "Continue"
$scriptDir = $PSScriptRoot
$fail = 0

if (-not $SkipCorners) {
    $cornerScript = Join-Path $scriptDir "check-corners.ps1"
    if (Test-Path $cornerScript) {
        $out = & powershell -ExecutionPolicy Bypass -File $cornerScript -Title $Title 2>&1
        $code = $LASTEXITCODE
        $out | Select-Object -Last 2 | ForEach-Object { Write-Host $_ }
        if ($code -eq 2) {
            Write-Host "SKIP: 四角检查被跳过（窗口无法置前）" -ForegroundColor DarkYellow
        } elseif ($code -ne 0) { $fail++ }
    } else {
        Write-Host "FAIL: 缺少 check-corners.ps1" -ForegroundColor Red
        $fail++
    }
}

if ($LogPath) {
    if (-not (Test-Path $LogPath)) {
        Write-Host "FAIL: 找不到日志 $LogPath（启动器需要把 stdout 重定向到这里）" -ForegroundColor Red
        $fail++
    } else {
        $text = Get-Content $LogPath -Raw -ErrorAction SilentlyContinue
        $pageLines = @($text -split "`r?`n" | Where-Object { $_ -match "圆角描边检查: (\d+) 个节点缺失" })
        $dialogLines = @($text -split "`r?`n" | Where-Object { $_ -match "弹窗描边检查: (\d+) 个节点缺失" })
        if ($pageLines.Count -eq 0) {
            Write-Host "FAIL: 日志里没有『圆角描边检查』（页面可能没切换过）" -ForegroundColor Red
            $fail++
        }
        $badPage = @($pageLines | Where-Object { $_ -notmatch "圆角描边检查: 0 个节点缺失" })
        if ($badPage.Count -gt 0) {
            Write-Host ("FAIL: {0} 次页面自检发现缺 border-radius 的节点" -f $badPage.Count) -ForegroundColor Red
            $badPage | Select-Object -First 3 | ForEach-Object { Write-Host "  $_" -ForegroundColor Red }
            $fail++
        }
        $badDialog = @($dialogLines | Where-Object { $_ -notmatch "弹窗描边检查: 0 个节点缺失" })
        if ($badDialog.Count -gt 0) {
            Write-Host ("FAIL: {0} 次弹窗自检发现缺 border-radius 的节点" -f $badDialog.Count) -ForegroundColor Red
            $badDialog | Select-Object -First 3 | ForEach-Object { Write-Host "  $_" -ForegroundColor Red }
            $fail++
        }
        Write-Host ("页面自检 {0} 次、弹窗自检 {1} 次，全部为 0 缺失" -f $pageLines.Count, $dialogLines.Count) -ForegroundColor DarkGray
    }
}

if ($fail -gt 0) {
    Write-Host "FAIL: UI 自检未通过（$fail 项）" -ForegroundColor Red
    exit 1
}
Write-Host "PASS: UI 自检通过" -ForegroundColor Green
exit 0
