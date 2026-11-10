# Nightly 视觉基线比对：连拍当前界面，和 docs/images 里提交的基线逐点比较。
# 差异超过阈值（默认 2%）就失败，用来发现"没人注意到的 UI 漂移"。
param(
    [string]$Title = "Neko Craft Launcher",
    [string]$Baseline = "docs\images\settings-settled.png",
    [double]$MaxDiffPercent = 2.0,
    [int]$SampleStep = 8,
    [switch]$Launch
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Push-Location $root
try {
    $launcher = $null
    if ($Launch) {
        $jar = Join-Path $root "target\mc-launcher.jar"
        if (-not (Test-Path $jar)) { throw "找不到 $jar，先跑构建" }
        $java = Join-Path "C:\Program Files\Java\jdk-21.0.11\bin" "javaw.exe"
        $launcher = Start-Process -FilePath $java -ArgumentList "-jar", $jar -WorkingDirectory $root -PassThru
        Write-Host "==> 已启动启动器，等待窗口" -ForegroundColor Cyan
        $deadline = (Get-Date).AddSeconds(120)
        while ((Get-Date) -lt $deadline) {
            Start-Sleep -Milliseconds 800
            $h = & powershell -ExecutionPolicy Bypass -Command "Add-Type -AssemblyName System.Windows.Forms; 1" 2>$null
            $proc = Get-Process -Id $launcher.Id -ErrorAction SilentlyContinue
            if ($proc -and $proc.MainWindowTitle -like "*Neko Craft Launcher*") { break }
        }
        Start-Sleep -Seconds 3
    }

    if (-not (Test-Path $Baseline)) {
        Write-Host ("FAIL: 找不到基线图 {0}（先跑 scripts/refresh-baseline.ps1）" -f $Baseline) -ForegroundColor Red
        exit 1
    }
    $outDir = "target\baseline-nightly"
    $current = Join-Path $outDir "settings.png"
    Write-Host "==> 抓当前设置页的稳定态截图" -ForegroundColor Cyan
    & powershell -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot "capture-settled.ps1") `
        -Title $Title -ClickName "设置" -Out $current
    if ($LASTEXITCODE -ne 0) { throw "截图失败（启动器是否已打开并停在启动页？）" }
    if (-not (Test-Path $current)) { throw "没有生成稳定态截图" }

    Add-Type -AssemblyName System.Drawing
    $a = New-Object System.Drawing.Bitmap((Resolve-Path $Baseline).Path)
    $b = New-Object System.Drawing.Bitmap((Resolve-Path $current).Path)
    $aw = $a.Width; $ah = $a.Height; $bw = $b.Width; $bh = $b.Height
    if ($aw -ne $bw -or $ah -ne $bh) {
        $a.Dispose(); $b.Dispose()
        Write-Host ("FAIL: 尺寸不一致（基线 {0}x{1}，当前 {2}x{3}）" -f $aw, $ah, $bw, $bh) -ForegroundColor Red
        exit 1
    }

    # 粗粒度比较：每 24x24 区块取平均色，只有色差明显的区块才算差异。
    # 动态文本（时间、内存数值）只影响少数区块，不会把整张图判成不一致。
    $block = 24
    $total = 0
    $diff = 0
    $hotBlocks = New-Object System.Collections.Generic.List[object]
    for ($y = 0; $y -lt $a.Height - $block; $y += $block) {
        for ($x = 0; $x -lt $a.Width - $block; $x += $block) {
            $total++
            $ar = 0; $ag = 0; $ab = 0; $br = 0; $bg = 0; $bb = 0
            for ($dy = 0; $dy -lt $block; $dy += 6) {
                for ($dx = 0; $dx -lt $block; $dx += 6) {
                    $pa = $a.GetPixel($x + $dx, $y + $dy)
                    $pb = $b.GetPixel($x + $dx, $y + $dy)
                    $ar += $pa.R; $ag += $pa.G; $ab += $pa.B
                    $br += $pb.R; $bg += $pb.G; $bb += $pb.B
                }
            }
            $n = [math]::Pow([math]::Ceiling($block / 6.0), 2)
            $dr = ($ar - $br) / $n; $dg = ($ag - $bg) / $n; $db = ($ab - $bb) / $n
            $dist = [math]::Sqrt($dr * $dr + $dg * $dg + $db * $db)
            if ($dist -gt 22) {
                $diff++
                $hotBlocks.Add([pscustomobject]@{ X = $x; Y = $y })
            }
        }
    }
    $width = $a.Width
    $height = $a.Height
    $a.Dispose(); $b.Dispose()

    $percent = if ($total -gt 0) { 100.0 * $diff / $total } else { 0 }
    Write-Host ("采样 {0} 点（{1}x{2}），差异 {3} 点 = {4:N2}%（阈值 {5:N2}%）" -f `
        $total, $width, $height, $diff, $percent, $MaxDiffPercent) -ForegroundColor Cyan

    if ($percent -gt $MaxDiffPercent) {
        # 留证据：当前截图 + 差异热力图，方便直接看哪里变了
        $stamp = Get-Date -Format "yyyyMMdd-HHmmss"
        $shotCopy = Join-Path $outDir ("diff-" + $stamp + ".png")
        Copy-Item $current $shotCopy -Force
        $heatPath = Join-Path $outDir ("heatmap-" + $stamp + ".png")
        try {
            $baseBmp = New-Object System.Drawing.Bitmap((Resolve-Path $current).Path)
            $heat = New-Object System.Drawing.Bitmap($baseBmp.Width, $baseBmp.Height)
            $g = [System.Drawing.Graphics]::FromImage($heat)
            $g.DrawImage($baseBmp, 0, 0)
            $red = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(120, 255, 0, 0))
            foreach ($hot in $hotBlocks) {
                $g.FillRectangle($red, $hot.X, $hot.Y, $block, $block)
            }
            $red.Dispose()
            $g.Dispose()
            $baseBmp.Dispose()
            $heat.Save($heatPath, [System.Drawing.Imaging.ImageFormat]::Png)
            $heat.Dispose()
            # 左右对照图：左边基线、右边当前，一眼看出哪里变了
            $comparePath = Join-Path $outDir ("compare-" + $stamp + ".png")
            try {
                $baseImg = New-Object System.Drawing.Bitmap((Resolve-Path $Baseline).Path)
                $curImg = New-Object System.Drawing.Bitmap((Resolve-Path $current).Path)
                $gap = 12
                $cmp = New-Object System.Drawing.Bitmap(($baseImg.Width * 2 + $gap), $baseImg.Height)
                $g2 = [System.Drawing.Graphics]::FromImage($cmp)
                $g2.Clear([System.Drawing.Color]::Black)
                $g2.DrawImage($baseImg, 0, 0)
                $g2.DrawImage($curImg, $baseImg.Width + $gap, 0)
                $g2.Dispose()
                $baseImg.Dispose(); $curImg.Dispose()
                $cmp.Save($comparePath, [System.Drawing.Imaging.ImageFormat]::Png)
                $cmp.Dispose()
            } catch {
                Write-Host ("对照图生成失败: {0}" -f $_) -ForegroundColor DarkYellow
            }
            Write-Host ("当前截图: " + $shotCopy) -ForegroundColor DarkYellow
            Write-Host ("差异热力图: " + $heatPath) -ForegroundColor DarkYellow
            if (Test-Path $comparePath) {
                Write-Host ("左右对照: " + $comparePath) -ForegroundColor DarkYellow
            }
        } catch {
            Write-Host ("热力图生成失败: {0}" -f $_) -ForegroundColor DarkYellow
        }
        Write-Host "FAIL: 界面与基线差异过大，确认是设计变更后跑 scripts/refresh-baseline.ps1 更新基线" -ForegroundColor Red
        exit 1
    }
    Write-Host "PASS: 界面与基线一致" -ForegroundColor Green
    exit 0
} catch {
    Write-Host ("夜间基线比对失败：{0}" -f $_.Exception.Message) -ForegroundColor Red
    exit 1
} finally {
    if ($launcher) {
        try { $launcher | Stop-Process -Force } catch { }
    }
    Pop-Location
}
