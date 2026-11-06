# Neko Craft Launcher 冒烟测试
#
# 用法（在项目根目录）：
#   powershell -ExecutionPolicy Bypass -File scripts\smoke-test.ps1
#   powershell -ExecutionPolicy Bypass -File scripts\smoke-test.ps1 -SkipBuild
#
# 流程：构建（可选）→ 启动 jar → 逐页切换（F1/F2/F5/F6/F7）→ 每页截图 →
#       检查 stderr 是否有异常 → 输出 PASS/FAIL 汇总。
# 截图输出在 target\smoke\*.png。

param(
    [switch]$SkipBuild,
    [string]$Jar = "target\mc-launcher.jar",
    [string]$JavaHome = "C:\Program Files\Java\jdk-21.0.11",
    [string]$Title = "Neko Craft Launcher",
    [int]$StartupWaitSec = 12,
    [switch]$KeepRunning
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Push-Location $root
try {
    $outDir = Join-Path $root "target\smoke"
    New-Item -ItemType Directory -Force -Path $outDir | Out-Null
    $stdout = Join-Path $outDir "stdout.log"
    $stderr = Join-Path $outDir "stderr.log"
    Remove-Item $stdout, $stderr -ErrorAction SilentlyContinue

    # ---------- Win32 ----------
    Add-Type -TypeDefinition @'
using System;
using System.Text;
using System.Runtime.InteropServices;
public class SmokeWin {
  public delegate bool EnumProc(IntPtr h, IntPtr l);
  [DllImport("user32.dll")] public static extern bool EnumWindows(EnumProc cb, IntPtr l);
  [DllImport("user32.dll")] public static extern int GetWindowText(IntPtr h, StringBuilder s, int n);
  [DllImport("user32.dll")] public static extern bool IsWindowVisible(IntPtr h);
  [DllImport("user32.dll")] public static extern bool ShowWindow(IntPtr h, int c);
  [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr h);
  [DllImport("user32.dll")] public static extern IntPtr GetForegroundWindow();
  [DllImport("user32.dll")] public static extern void keybd_event(byte vk, byte scan, uint flags, IntPtr extra);
  [DllImport("user32.dll")] public static extern bool GetWindowRect(IntPtr h, out RECT r);
  [DllImport("user32.dll")] public static extern bool PostMessage(IntPtr h, uint msg, IntPtr wParam, IntPtr lParam);
  public static IntPtr Find(string title) {
    IntPtr found = IntPtr.Zero;
    EnumWindows((h, l) => {
      if (!IsWindowVisible(h)) return true;
      var sb = new StringBuilder(512);
      GetWindowText(h, sb, 512);
      if (sb.ToString() == title) { found = h; return false; }
      return true;
    }, IntPtr.Zero);
    return found;
  }
  [StructLayout(LayoutKind.Sequential)] public struct RECT { public int Left, Top, Right, Bottom; }
}
'@ -ErrorAction SilentlyContinue

    function Focus-Launcher {
        for ($attempt = 1; $attempt -le 5; $attempt++) {
            $h = [SmokeWin]::Find($Title)
            if ($h -eq [IntPtr]::Zero) { return $false }
            [SmokeWin]::ShowWindow($h, 9) | Out-Null   # SW_RESTORE
            # Windows 会拦住后台进程抢前台：先点一下 ALT 解锁，否则 SendKeys 会打到别的窗口上
            [SmokeWin]::keybd_event(0x12, 0, 0, [IntPtr]::Zero)
            Start-Sleep -Milliseconds 40
            [SmokeWin]::SetForegroundWindow($h) | Out-Null
            [SmokeWin]::keybd_event(0x12, 0, 2, [IntPtr]::Zero)
            Start-Sleep -Milliseconds 400
            if ([SmokeWin]::GetForegroundWindow() -eq $h) { return $true }
        }
        return $false
    }

    function Wait-Launcher([int]$timeoutSec) {
        $deadline = (Get-Date).AddSeconds($timeoutSec)
        while ((Get-Date) -lt $deadline) {
            if ([SmokeWin]::Find($Title) -ne [IntPtr]::Zero) { return $true }
            Start-Sleep -Milliseconds 500
        }
        return $false
    }

    function Capture-Window([string]$name) {
        $h = [SmokeWin]::Find($Title)
        if ($h -eq [IntPtr]::Zero) { return }
        $r = New-Object SmokeWin+RECT
        [SmokeWin]::GetWindowRect($h, [ref]$r) | Out-Null
        $w = $r.Right - $r.Left
        $ht = $r.Bottom - $r.Top
        if ($w -le 0 -or $ht -le 0) { return }
        Add-Type -AssemblyName System.Drawing
        $bmp = New-Object System.Drawing.Bitmap($w, $ht)
        $g = [System.Drawing.Graphics]::FromImage($bmp)
        $g.CopyFromScreen($r.Left, $r.Top, 0, 0, (New-Object System.Drawing.Size($w, $ht)))
        $bmp.Save((Join-Path $outDir "$name.png"), [System.Drawing.Imaging.ImageFormat]::Png)
        $g.Dispose(); $bmp.Dispose()
    }

    # ---------- 构建 ----------
    if (-not $SkipBuild) {
        Write-Host "[1/4] 构建..." -ForegroundColor Cyan
        $env:JAVA_HOME = $JavaHome
        & .\mvnw.cmd -o -q package -DskipTests
        if ($LASTEXITCODE -ne 0) { throw "构建失败 (exit $LASTEXITCODE)" }
    } else {
        Write-Host "[1/4] 跳过构建" -ForegroundColor DarkGray
    }

    if (-not (Test-Path $Jar)) { throw "找不到 $Jar" }

    # ---------- 启动 ----------
    Write-Host "[2/4] 启动启动器..." -ForegroundColor Cyan
    # 结束所有启动器实例（包括 IDEA 里跑的那份），否则单实例锁会让新进程只弹「已有实例在运行」
    function Stop-ExistingLauncher {
        Get-Process javaw, java -ErrorAction SilentlyContinue |
            Where-Object { $_.MainWindowTitle -like "*$Title*" } |
            ForEach-Object { try { $_.Kill() } catch { } }
    }
    Stop-ExistingLauncher
    Start-Sleep -Seconds 1
    # 用 javaw 启动：java.exe 会带一个控制台窗口，抢焦点还会把截图挡掉
    $javaExe = Join-Path $JavaHome "bin\javaw.exe"
    if (-not (Test-Path $javaExe)) { $javaExe = Join-Path $JavaHome "bin\java.exe" }
    $launcher = Start-Process -FilePath $javaExe `
        -ArgumentList '-jar', $Jar -WorkingDirectory $root `
        -RedirectStandardOutput $stdout -RedirectStandardError $stderr -WindowStyle Hidden -PassThru
    # 轮询等窗口出现，不要用固定 sleep（冷启动/杀毒扫描会拖慢）
    if (-not (Wait-Launcher ([Math]::Max(90, $StartupWaitSec + 20)))) {
        throw "启动器窗口没有出现（$Title，等待 $([Math]::Max(90, $StartupWaitSec + 20)) 秒）"
    }
    # 窗口出现 ≠ 界面搭好：预热遮罩期间顶部导航还不存在，等到「下载」按钮可点为止
    Add-Type -AssemblyName UIAutomationClient
    Add-Type -AssemblyName UIAutomationTypes
    function Wait-Ready([int]$timeoutSec) {
        $deadline = (Get-Date).AddSeconds($timeoutSec)
        while ((Get-Date) -lt $deadline) {
            $h = [SmokeWin]::Find($Title)
            if ($h -ne [IntPtr]::Zero) {
                try {
                    $root = [System.Windows.Automation.AutomationElement]::FromHandle($h)
                    $cond = New-Object System.Windows.Automation.AndCondition(
                        (New-Object System.Windows.Automation.PropertyCondition(
                            [System.Windows.Automation.AutomationElement]::NameProperty, "下载")),
                        (New-Object System.Windows.Automation.PropertyCondition(
                            [System.Windows.Automation.AutomationElement]::ControlTypeProperty,
                            [System.Windows.Automation.ControlType]::Button)))
                    if ($root.FindFirst([System.Windows.Automation.TreeScope]::Descendants, $cond)) { return $true }
                } catch { }
            }
            Start-Sleep -Milliseconds 500
        }
        return $false
    }
    if (-not (Wait-Ready 90)) { throw "界面在 90 秒内没有准备好（找不到顶部导航按钮）" }
    if (-not (Focus-Launcher)) { Write-Host "  （抢不到前台，不影响后续 UIA 操作）" -ForegroundColor DarkGray }
    # 顶部导航可点 ≠ 启动页画完：等一帧稳定再截，否则会拍到只有底色的空页
    Start-Sleep -Seconds 2
    Capture-Window "01-launch"

    # ---------- 逐页 ----------
    Write-Host "[3/4] 逐页切换..." -ForegroundColor Cyan
    Add-Type -AssemblyName System.Windows.Forms
    $pages = @(
        @{ nav = "下载"; name = "02-download" },
        @{ nav = "设置"; name = "03-settings" },
        @{ nav = "工具"; name = "04-tools" },
        @{ nav = "启动"; name = "05-back-launch" }
    )
    Add-Type -AssemblyName UIAutomationClient
    Add-Type -AssemblyName UIAutomationTypes
    foreach ($p in $pages) {
        # 用 UI Automation 直接触发顶部导航按钮：不依赖前台焦点，也不会打到别的窗口
        $h = [SmokeWin]::Find($Title)
        if ($h -eq [IntPtr]::Zero) { throw "切换到 $($p.name) 时找不到窗口" }
        $root = [System.Windows.Automation.AutomationElement]::FromHandle($h)
        $cond = New-Object System.Windows.Automation.AndCondition(
            (New-Object System.Windows.Automation.PropertyCondition(
                [System.Windows.Automation.AutomationElement]::NameProperty, $p.nav)),
            (New-Object System.Windows.Automation.PropertyCondition(
                [System.Windows.Automation.AutomationElement]::ControlTypeProperty,
                [System.Windows.Automation.ControlType]::Button)))
        $el = $root.FindFirst([System.Windows.Automation.TreeScope]::Descendants, $cond)
        if (-not $el) { throw "找不到导航按钮：$($p.nav)" }
        $el.GetCurrentPattern([System.Windows.Automation.InvokePattern]::Pattern).Invoke()
        Start-Sleep -Milliseconds 2500
        Capture-Window $p.name
        Write-Host ("  · {0}（{1}）" -f $p.name, $p.nav) -ForegroundColor DarkGray
    }

    # ---------- 检查 ----------
    Write-Host "[4/4] 检查日志..." -ForegroundColor Cyan
    $err = ""
    if (Test-Path $stderr) { $err = Get-Content $stderr -Raw -ErrorAction SilentlyContinue }
    $exceptions = @()
    if ($err) {
        $exceptions = [regex]::Matches($err, 'Exception in thread[^\r\n]*') | ForEach-Object { $_.Value }
    }

    # 空白页检测：整页几乎同色说明渲染失败（截图哈希「不同」并不代表页面有内容）
    Add-Type -AssemblyName System.Drawing
    function Get-DistinctColors([string]$path) {
        $bmp = [System.Drawing.Bitmap]::FromFile($path)
        try {
            $colors = New-Object System.Collections.Generic.HashSet[int]
            $stepX = [Math]::Max(1, [int]($bmp.Width / 40))
            $stepY = [Math]::Max(1, [int]($bmp.Height / 40))
            for ($y = 0; $y -lt $bmp.Height; $y += $stepY) {
                for ($x = 0; $x -lt $bmp.Width; $x += $stepX) {
                    [void]$colors.Add($bmp.GetPixel($x, $y).ToArgb())
                }
            }
            return $colors.Count
        } finally { $bmp.Dispose() }
    }
    $blank = @()
    foreach ($shot in (Get-ChildItem $outDir -Filter *.png)) {
        $count = Get-DistinctColors $shot.FullName
        if ($count -lt 60) { $blank += ("{0}（只有 {1} 种颜色）" -f $shot.Name, $count) }
    }
    if ($blank.Count -gt 0) {
        Write-Host ("FAIL: 页面几乎是空白: {0}" -f ($blank -join '; ')) -ForegroundColor Red
        exit 1
    }

    $shots = Get-ChildItem $outDir -Filter *.png | Select-Object -ExpandProperty Name
    $hashes = @(Get-ChildItem $outDir -Filter *.png | ForEach-Object { (Get-FileHash $_.FullName -Algorithm SHA256).Hash })
    $unique = @($hashes | Sort-Object -Unique).Count
    # 01 与 05 都是启动页，允许相同；但 01→02→03→04 必须四张都不一样，否则就是某次点击没生效
    $pageHashes = @($hashes[0], $hashes[1], $hashes[2], $hashes[3])
    $pageUnique = @($pageHashes | Sort-Object -Unique).Count
    if ($hashes.Count -ne 5 -or $pageUnique -ne 4) {
        Write-Host ("FAIL: 页面没有真正切换（前四张里只有 {0} 张不同）" -f $pageUnique) -ForegroundColor Red
        exit 1
    }
    Write-Host ""
    Write-Host "截图: $($shots -join ', ')" -ForegroundColor DarkGray
    if ($exceptions.Count -gt 0) {
        Write-Host "FAIL: stderr 中发现 $($exceptions.Count) 处异常" -ForegroundColor Red
        $exceptions | Select-Object -First 10 | ForEach-Object { Write-Host "  $_" -ForegroundColor Red }
        Write-Host "完整日志: $stderr" -ForegroundColor Red
        exit 1
    }
    Write-Host "PASS: 5 个页面切换完成，stderr 无异常" -ForegroundColor Green
    exit 0
}
finally {
    if (-not $KeepRunning -and $launcher) {
        try { $launcher.Kill() } catch { }
    }
    Pop-Location
}
