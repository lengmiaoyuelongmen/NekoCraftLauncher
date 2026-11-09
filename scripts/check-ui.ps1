# 统一 UI 自检：窗口四角是否圆角 + 场景里是否还有"圆角底 + 方角边"的节点。
# 可单独跑，也可由 smoke-test.ps1 调用。
param(
    [string]$Title = "Neko Craft Launcher",
    [string]$LogPath = "",
    [switch]$SkipCorners,
    [switch]$RequireDialog,
    [int]$MinTransitions = 0
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
        # 形如：圆角描边修复: 3 个，剩余缺失: 0 个
        $pageLines = @($text -split "`r?`n" | Where-Object { $_ -match "圆角描边修复: \d+ 个，剩余缺失: \d+ 个" })
        $dialogLines = @($text -split "`r?`n" | Where-Object { $_ -match "弹窗描边修复: \d+ 个，剩余缺失: \d+ 个" })
        if ($pageLines.Count -eq 0) {
            Write-Host "FAIL: 日志里没有『圆角描边修复』（页面可能没切换过）" -ForegroundColor Red
            $fail++
        }
        $badPage = @($pageLines | Where-Object { $_ -notmatch "剩余缺失: 0 个" })
        if ($badPage.Count -gt 0) {
            Write-Host ("FAIL: {0} 次页面自检发现缺 border-radius 的节点" -f $badPage.Count) -ForegroundColor Red
            $badPage | Select-Object -First 3 | ForEach-Object { Write-Host "  $_" -ForegroundColor Red }
            $fail++
        }
        $badDialog = @($dialogLines | Where-Object { $_ -notmatch "剩余缺失: 0 个" })
        if ($badDialog.Count -gt 0) {
            Write-Host ("FAIL: {0} 次弹窗自检发现缺 border-radius 的节点" -f $badDialog.Count) -ForegroundColor Red
            $badDialog | Select-Object -First 3 | ForEach-Object { Write-Host "  $_" -ForegroundColor Red }
            $fail++
        }
        if ($RequireDialog -and $dialogLines.Count -eq 0) {
            Write-Host "FAIL: 要求至少打开过一次弹窗（弹窗自检 0 次）" -ForegroundColor Red
            $fail++
        }
        # 页面过渡打点：每切一次页应该有且只有一条「页面过渡完成」
        $transitionLines = @($text -split "`r?`n" | Where-Object { $_ -match "页面过渡完成: " })
        if ($MinTransitions -gt 0 -and $transitionLines.Count -lt $MinTransitions) {
            Write-Host ("FAIL: 页面过渡只完成了 {0} 次，期望至少 {1} 次（动画可能被跳过或中断）" -f $transitionLines.Count, $MinTransitions) -ForegroundColor Red
            $fail++
        }
        Write-Host ("页面过渡完成 {0} 次" -f $transitionLines.Count) -ForegroundColor DarkGray
        $pageFixed = 0
        foreach ($line in $pageLines) {
            if ($line -match "圆角描边修复: (\d+) 个") { $pageFixed += [int]$Matches[1] }
        }
        Write-Host ("页面自检 {0} 次（累计修复 {1} 个）、弹窗自检 {2} 次" -f $pageLines.Count, $pageFixed, $dialogLines.Count) -ForegroundColor DarkGray
    }
}

if ($fail -gt 0) {
    Write-Host "FAIL: UI 自检未通过（$fail 项）" -ForegroundColor Red
    exit 1
}
Write-Host "PASS: UI 自检通过" -ForegroundColor Green
exit 0
