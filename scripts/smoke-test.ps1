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
  [DllImport("user32.dll")] public static extern bool GetWindowRect(IntPtr h, out RECT r);
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
        $h = [SmokeWin]::Find($Title)
        if ($h -eq [IntPtr]::Zero) { return $false }
        [SmokeWin]::ShowWindow($h, 9) | Out-Null   # SW_RESTORE
        [SmokeWin]::SetForegroundWindow($h) | Out-Null
        Start-Sleep -Milliseconds 400
        return $true
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
    Get-CimInstance Win32_Process -Filter "Name='java.exe' OR Name='javaw.exe'" -ErrorAction SilentlyContinue |
        Where-Object {
            $_.CommandLine -and ($_.CommandLine -match 'mc-launcher\.jar' -or $_.CommandLine -match 'com\.example\.demo\.Launcher')
        } |
        ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }
    Start-Sleep -Seconds 1
    Start-Process -FilePath (Join-Path $JavaHome "bin\java.exe") `
        -ArgumentList '-jar', $Jar -WorkingDirectory $root `
        -RedirectStandardOutput $stdout -RedirectStandardError $stderr -WindowStyle Hidden
    Start-Sleep -Seconds $StartupWaitSec
    if (-not (Focus-Launcher)) { throw "启动器窗口没有出现（$Title）" }
    Capture-Window "01-launch"

    # ---------- 逐页 ----------
    Write-Host "[3/4] 逐页切换..." -ForegroundColor Cyan
    Add-Type -AssemblyName System.Windows.Forms
    $pages = @(
        @{ key = "{F5}"; name = "02-download" },
        @{ key = "{F6}"; name = "03-settings" },
        @{ key = "{F7}"; name = "04-tools"  },
        @{ key = "{F1}"; name = "05-back-launch" }
    )
    foreach ($p in $pages) {
        if (-not (Focus-Launcher)) { throw "切换到 $($p.name) 时找不到窗口" }
        [System.Windows.Forms.SendKeys]::SendWait($p.key)
        Start-Sleep -Milliseconds 2500
        Capture-Window $p.name
        Write-Host ("  · {0}" -f $p.name) -ForegroundColor DarkGray
    }

    # ---------- 检查 ----------
    Write-Host "[4/4] 检查日志..." -ForegroundColor Cyan
    $err = ""
    if (Test-Path $stderr) { $err = Get-Content $stderr -Raw -ErrorAction SilentlyContinue }
    $exceptions = @()
    if ($err) {
        $exceptions = [regex]::Matches($err, 'Exception in thread[^\r\n]*') | ForEach-Object { $_.Value }
    }

    $shots = Get-ChildItem $outDir -Filter *.png | Select-Object -ExpandProperty Name
    Write-Host ""
    Write-Host "截图: $($shots -join ', ')" -ForegroundColor DarkGray
    if ($exceptions.Count -gt 0) {
        Write-Host "FAIL: stderr 中发现 $($exceptions.Count) 处异常" -ForegroundColor Red
        $exceptions | Select-Object -First 10 | ForEach-Object { Write-Host "  $_" -ForegroundColor Red }
        Write-Host "完整日志: $stderr" -ForegroundColor Red
        exit 1
    }
    Write-Host "PASS: 5 个页面切换完成，stderr 无异常" -ForegroundColor Green
    if (-not $KeepRunning) {
        Get-CimInstance Win32_Process -Filter "Name='java.exe' OR Name='javaw.exe'" -ErrorAction SilentlyContinue |
            Where-Object {
                $_.CommandLine -and ($_.CommandLine -match 'mc-launcher\.jar' -or $_.CommandLine -match 'com\.example\.demo\.Launcher')
            } |
            ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }
    }
    exit 0
}
finally {
    Pop-Location
}
