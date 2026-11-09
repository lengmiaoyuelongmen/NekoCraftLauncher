# 用连拍抓动效中间帧：截图工具单张延迟太大（>300ms），这里连续抓 N 帧，
# 中途可选触发一次 UIA 点击，然后和最后一帧（稳定态）比较，找出"动画进行中"的帧。
param(
    [string]$Title = "Neko Craft Launcher",
    [string]$ClickName = "",
    [string]$HoverName = "",
    [int]$WarmupMs = 300,
    [int]$Frames = 30,
    [int]$IntervalMs = 40,
    [string]$OutDir = "target\motion",
    [switch]$ExpectDecreasing,
    [switch]$ContactSheet,
    [int]$MinHoverDiff = 0
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName UIAutomationClient
Add-Type -AssemblyName UIAutomationTypes

Add-Type -TypeDefinition @'
using System;
using System.Text;
using System.Runtime.InteropServices;
public class MotionWin {
  public delegate bool EnumProc(IntPtr h, IntPtr l);
  [DllImport("user32.dll")] public static extern bool EnumWindows(EnumProc cb, IntPtr l);
  [DllImport("user32.dll")] public static extern int GetWindowText(IntPtr h, StringBuilder s, int n);
  [DllImport("user32.dll")] public static extern bool IsWindowVisible(IntPtr h);
  [DllImport("user32.dll")] public static extern bool GetWindowRect(IntPtr h, out RECT r);
  [DllImport("user32.dll")] public static extern bool ShowWindow(IntPtr h, int c);
  [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr h);
  [DllImport("user32.dll")] public static extern IntPtr GetForegroundWindow();
  [DllImport("user32.dll")] public static extern void keybd_event(byte vk, byte scan, uint flags, IntPtr extra);
  [DllImport("user32.dll")] public static extern bool SetCursorPos(int x, int y);
  [StructLayout(LayoutKind.Sequential)] public struct RECT { public int Left, Top, Right, Bottom; }
  public static IntPtr Find(string title) {
    IntPtr found = IntPtr.Zero;
    EnumWindows((h, l) => {
      if (!IsWindowVisible(h)) return true;
      var sb = new StringBuilder(512);
      GetWindowText(h, sb, 512);
      if (sb.ToString().StartsWith(title)) { found = h; return false; }
      return true;
    }, IntPtr.Zero);
    return found;
  }
}
'@ -ErrorAction SilentlyContinue

$hwnd = [MotionWin]::Find($Title)
if ($hwnd -eq [IntPtr]::Zero) {
    Write-Host "FAIL: 找不到窗口 '$Title'" -ForegroundColor Red
    exit 1
}
for ($try = 0; $try -lt 5; $try++) {
    [MotionWin]::ShowWindow($hwnd, 9) | Out-Null
    [MotionWin]::keybd_event(0x12, 0, 0, [IntPtr]::Zero)
    Start-Sleep -Milliseconds 40
    [MotionWin]::SetForegroundWindow($hwnd) | Out-Null
    [MotionWin]::keybd_event(0x12, 0, 2, [IntPtr]::Zero)
    Start-Sleep -Milliseconds 250
    if ([MotionWin]::GetForegroundWindow() -eq $hwnd) { break }
}

$rect = New-Object MotionWin+RECT
[MotionWin]::GetWindowRect($hwnd, [ref]$rect) | Out-Null
$w = $rect.Right - $rect.Left
$h = $rect.Bottom - $rect.Top
if ($w -le 0 -or $h -le 0) {
    Write-Host "FAIL: 窗口尺寸无效" -ForegroundColor Red
    exit 1
}

if (-not (Test-Path $OutDir)) { New-Item -ItemType Directory -Force -Path $OutDir | Out-Null }
Get-ChildItem $OutDir -Filter "frame-*.png" -ErrorAction SilentlyContinue | Remove-Item -Force

Start-Sleep -Milliseconds $WarmupMs
Write-Host ("连拍 {0} 帧，窗口 {1}x{2}" -f $Frames, $w, $h)

$clicked = $false
$paths = @()
for ($i = 0; $i -lt $Frames; $i++) {
    if (-not $clicked -and $HoverName -and $i -eq 4) {
        $root = [System.Windows.Automation.AutomationElement]::FromHandle($hwnd)
        $cond = New-Object System.Windows.Automation.PropertyCondition(
            [System.Windows.Automation.AutomationElement]::NameProperty, $HoverName)
        $el = $root.FindFirst([System.Windows.Automation.TreeScope]::Descendants, $cond)
        if ($el) {
            $r = $el.Current.BoundingRectangle
            $cx = [int]($r.X + $r.Width / 2)
            $cy = [int]($r.Y + $r.Height / 2)
            [MotionWin]::SetCursorPos($cx, $cy) | Out-Null
            Write-Host ("第 {0} 帧把鼠标移到：{1}" -f $i, $HoverName) -ForegroundColor DarkGray
        } else {
            Write-Host ("找不到可悬停元素：{0}" -f $HoverName) -ForegroundColor Yellow
        }
        $clicked = $true
    }
    if (-not $clicked -and $ClickName -and $i -eq 4) {
        $root = [System.Windows.Automation.AutomationElement]::FromHandle($hwnd)
        $cond = New-Object System.Windows.Automation.PropertyCondition(
            [System.Windows.Automation.AutomationElement]::NameProperty, $ClickName)
        $el = $root.FindFirst([System.Windows.Automation.TreeScope]::Descendants, $cond)
        if ($el) {
            $el.GetCurrentPattern([System.Windows.Automation.InvokePattern]::Pattern).Invoke()
            Write-Host ("第 {0} 帧触发点击：{1}" -f $i, $ClickName) -ForegroundColor DarkGray
        } else {
            Write-Host ("找不到可点击元素：{0}" -f $ClickName) -ForegroundColor Yellow
        }
        $clicked = $true
    }
    $bmp = New-Object System.Drawing.Bitmap($w, $h)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.CopyFromScreen($rect.Left, $rect.Top, 0, 0, (New-Object System.Drawing.Size($w, $h)))
    $g.Dispose()
    $path = Join-Path $OutDir ("frame-{0:D2}.png" -f $i)
    $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    $paths += $path
    Start-Sleep -Milliseconds $IntervalMs
}

# 和最后一帧（稳定态）比较，找出动画进行中的帧
$final = New-Object System.Drawing.Bitmap($paths[-1])
$diffCounts = @()
for ($i = 0; $i -lt $paths.Count - 1; $i++) {
    $frame = New-Object System.Drawing.Bitmap($paths[$i])
    $diff = 0
    for ($y = 0; $y -lt $h; $y += 6) {
        for ($x = 0; $x -lt $w; $x += 6) {
            if ($frame.GetPixel($x, $y).ToArgb() -ne $final.GetPixel($x, $y).ToArgb()) { $diff++ }
        }
    }
    $frame.Dispose()
    $diffCounts += [pscustomobject]@{ Frame = $i; Diff = $diff }
}
$final.Dispose()

# 点击之后的帧才算"动画进行中"：点击前的帧是旧页面，差异天然很大
$after = @($diffCounts | Where-Object { $_.Frame -ge 4 })
Write-Host "逐帧差异（点击后）：" -ForegroundColor Cyan
$after | ForEach-Object { Write-Host ("  帧 {0}: {1}" -f $_.Frame, $_.Diff) -ForegroundColor DarkGray }
$moving = @($after | Where-Object { $_.Diff -gt 10 })
if ($moving.Count -gt 0) {
    $top = $after | Sort-Object Diff -Descending | Select-Object -First 1
    Write-Host ("抓到动画中间帧 {0} 帧（差异 {1}→0），示例: {2}" -f $moving.Count, $top.Diff, $paths[$top.Frame]) -ForegroundColor Green
    # 悬停断言：差异太小说明悬停动效没生效
    if ($HoverName -and $MinHoverDiff -gt 0) {
        $peak = ($after | Measure-Object -Property Diff -Maximum).Maximum
        if ($peak -lt $MinHoverDiff) {
            Write-Host ("FAIL: 悬停差异峰值只有 {0}，低于阈值 {1}（悬停动效可能失效）" -f $peak, $MinHoverDiff) -ForegroundColor Red
            exit 1
        }
        Write-Host ("PASS: 悬停差异峰值 {0} ≥ 阈值 {1}" -f $peak, $MinHoverDiff) -ForegroundColor Green
    }
    # 可选：断言差异序列单调递减（允许少量抖动）
    if ($ExpectDecreasing) {
        $prev = [int]::MaxValue
        $violations = @()
        foreach ($point in $after) {
            if ($point.Diff -gt $prev + 5) { $violations += $point }
            $prev = [math]::Min($prev, $point.Diff)
        }
        if ($violations.Count -gt 0) {
            Write-Host ("FAIL: 差异序列没有单调收敛（{0} 处反弹）" -f $violations.Count) -ForegroundColor Red
            exit 1
        }
        Write-Host "PASS: 差异序列单调收敛" -ForegroundColor Green
    }
    # 可选：把关键帧拼成一张九宫格，方便直接贴文档
    if ($ContactSheet) {
        $pick = @()
        $afterSorted = $after | Sort-Object Frame
        $hit = 0
        foreach ($point in $afterSorted) {
            if ($point.Diff -gt 0 -and $hit -lt 8) { $pick += $point.Frame; $hit++ }
        }
        $pick += ($paths.Count - 1)
        $cellW = [int]($w / 3)
        $cellH = [int]($h / 3)
        $sheet = New-Object System.Drawing.Bitmap(($cellW * 3), ($cellH * 3))
        $g = [System.Drawing.Graphics]::FromImage($sheet)
        $g.Clear([System.Drawing.Color]::Black)
        for ($k = 0; $k -lt [Math]::Min(9, $pick.Count); $k++) {
            $frame = New-Object System.Drawing.Bitmap($paths[$pick[$k]])
            $dst = New-Object System.Drawing.Rectangle((($k % 3) * $cellW), ([math]::Floor($k / 3) * $cellH), $cellW, $cellH)
            $g.DrawImage($frame, $dst)
            $frame.Dispose()
        }
        $g.Dispose()
        $sheetPath = Join-Path $OutDir "contact-sheet.png"
        $sheet.Save($sheetPath, [System.Drawing.Imaging.ImageFormat]::Png)
        $sheet.Dispose()
        Write-Host ("九宫格对比图: " + $sheetPath) -ForegroundColor Green
    }
    exit 0
}
Write-Host "没有抓到中间帧（可能动画太快或未触发）" -ForegroundColor Yellow
exit 2
