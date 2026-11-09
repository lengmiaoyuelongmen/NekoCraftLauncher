# 抓一张"稳定态"窗口截图：先点一次目标页面，等动画结束再截。
# 用于视觉基线（动效连拍太依赖时序，无法逐像素比对）。
param(
    [string]$Title = "Neko Craft Launcher",
    [string]$ClickName = "设置",
    [int]$SettleMs = 1500,
    [string]$Out = "target\settled.png"
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName UIAutomationClient
Add-Type -AssemblyName UIAutomationTypes

Add-Type -TypeDefinition @'
using System;
using System.Text;
using System.Runtime.InteropServices;
public class SettleWin {
  public delegate bool EnumProc(IntPtr h, IntPtr l);
  [DllImport("user32.dll")] public static extern bool EnumWindows(EnumProc cb, IntPtr l);
  [DllImport("user32.dll")] public static extern int GetWindowText(IntPtr h, StringBuilder s, int n);
  [DllImport("user32.dll")] public static extern bool IsWindowVisible(IntPtr h);
  [DllImport("user32.dll")] public static extern bool GetWindowRect(IntPtr h, out RECT r);
  [DllImport("user32.dll")] public static extern bool ShowWindow(IntPtr h, int c);
  [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr h);
  [DllImport("user32.dll")] public static extern IntPtr GetForegroundWindow();
  [DllImport("user32.dll")] public static extern void keybd_event(byte vk, byte scan, uint flags, IntPtr extra);
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

$hwnd = [SettleWin]::Find($Title)
if ($hwnd -eq [IntPtr]::Zero) {
    Write-Host "FAIL: 找不到窗口 '$Title'" -ForegroundColor Red
    exit 1
}
for ($try = 0; $try -lt 8; $try++) {
    [SettleWin]::ShowWindow($hwnd, 9) | Out-Null
    [SettleWin]::keybd_event(0x12, 0, 0, [IntPtr]::Zero)
    Start-Sleep -Milliseconds 40
    [SettleWin]::SetForegroundWindow($hwnd) | Out-Null
    [SettleWin]::keybd_event(0x12, 0, 2, [IntPtr]::Zero)
    Start-Sleep -Milliseconds 250
    if ([SettleWin]::GetForegroundWindow() -eq $hwnd) { break }
}

if ($ClickName) {
    $root = [System.Windows.Automation.AutomationElement]::FromHandle($hwnd)
    $cond = New-Object System.Windows.Automation.PropertyCondition(
        [System.Windows.Automation.AutomationElement]::NameProperty, $ClickName)
    $el = $root.FindFirst([System.Windows.Automation.TreeScope]::Descendants, $cond)
    if ($el) {
        $el.GetCurrentPattern([System.Windows.Automation.InvokePattern]::Pattern).Invoke()
        Write-Host ("已点击：{0}" -f $ClickName) -ForegroundColor DarkGray
    } else {
        Write-Host ("找不到可点击元素：{0}" -f $ClickName) -ForegroundColor Yellow
    }
}

# 等动画 + 数据加载稳定
Start-Sleep -Milliseconds $SettleMs

$rect = New-Object SettleWin+RECT
[SettleWin]::GetWindowRect($hwnd, [ref]$rect) | Out-Null
$w = $rect.Right - $rect.Left
$h = $rect.Bottom - $rect.Top
$dir = Split-Path -Parent $Out
if ($dir -and -not (Test-Path $dir)) { New-Item -ItemType Directory -Force -Path $dir | Out-Null }
$bmp = New-Object System.Drawing.Bitmap($w, $h)
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.CopyFromScreen($rect.Left, $rect.Top, 0, 0, (New-Object System.Drawing.Size($w, $h)))
$g.Dispose()
$bmp.Save($Out, [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()
Write-Host ("稳定态截图: {0}（{1}x{2}）" -f $Out, $w, $h) -ForegroundColor Green
exit 0
