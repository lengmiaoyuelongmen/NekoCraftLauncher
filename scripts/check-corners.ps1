# 圆角窗口自检：截取启动器窗口，量四个角"多远才开始有内容"。
# 方角窗口的角点几乎立刻是窗口色（对角线 1~2px）；16px 圆角的窗口大约 4~5px 才出现内容，
# 因为弧线到外框角的距离约为 0.29 × 半径。
param(
    [string]$Title = "Neko Craft Launcher"
)

$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.Windows.Forms

Add-Type -TypeDefinition @'
using System;
using System.Text;
using System.Runtime.InteropServices;
public class CornerWin {
  public delegate bool EnumProc(IntPtr h, IntPtr l);
  [DllImport("user32.dll")] public static extern bool EnumWindows(EnumProc cb, IntPtr l);
  [DllImport("user32.dll")] public static extern int GetWindowText(IntPtr h, StringBuilder s, int n);
  [DllImport("user32.dll")] public static extern bool IsWindowVisible(IntPtr h);
  [DllImport("user32.dll")] public static extern bool GetWindowRect(IntPtr h, out RECT r);
  [DllImport("user32.dll")] public static extern bool ShowWindow(IntPtr h, int c);
  [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr h);
  [DllImport("user32.dll")] public static extern uint GetDpiForWindow(IntPtr h);
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

$hwnd = [CornerWin]::Find($Title)
if ($hwnd -eq [IntPtr]::Zero) {
    Write-Host "FAIL: 找不到窗口 '$Title'（先启动启动器）" -ForegroundColor Red
    exit 1
}
# 截图前把窗口提到前台，否则 CopyFromScreen 拍到的是压在上面的别的窗口。
# Windows 会拦住后台进程抢前台，所以先用 ALT 解锁再 SetForegroundWindow（和 smoke 一致）
$focused = $false
for ($try = 0; $try -lt 5; $try++) {
    [CornerWin]::ShowWindow($hwnd, 9) | Out-Null
    [CornerWin]::keybd_event(0x12, 0, 0, [IntPtr]::Zero)
    Start-Sleep -Milliseconds 40
    [CornerWin]::SetForegroundWindow($hwnd) | Out-Null
    [CornerWin]::keybd_event(0x12, 0, 2, [IntPtr]::Zero)
    Start-Sleep -Milliseconds 350
    if ([CornerWin]::GetForegroundWindow() -eq $hwnd) { $focused = $true; break }
}
if (-not $focused) {
    Write-Host "SKIP: 窗口无法切到前台（被别的窗口盖住），本次不判圆角" -ForegroundColor DarkYellow
    exit 2
}

$rect = New-Object CornerWin+RECT
[CornerWin]::GetWindowRect($hwnd, [ref]$rect) | Out-Null
$w = $rect.Right - $rect.Left
$h = $rect.Bottom - $rect.Top
Write-Host ("窗口: {0},{1} {2}x{3}" -f $rect.Left, $rect.Top, $w, $h)

# 多截 24px 边距，用来取"桌面参考色"（窗口内部取样会把阴影当成桌面）
# 按窗口 DPI 缩放：125% 下同一个圆角的像素跨度会变大
$dpi = 96
try { $dpi = [int][CornerWin]::GetDpiForWindow($hwnd) } catch { }
if ($dpi -le 0) { $dpi = 96 }
$scale = $dpi / 96.0
$minRun = [Math]::Max(3, [int][Math]::Round(2.2 * $scale))
Write-Host ("DPI: {0}（缩放 {1:P0}），阈值 {2}px" -f $dpi, $scale, $minRun)

$margin = 24
$capW = $w + $margin * 2
$capH = $h + $margin * 2
$bmp = New-Object System.Drawing.Bitmap($capW, $capH)
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.CopyFromScreen($rect.Left - $margin, $rect.Top - $margin, 0, 0, (New-Object System.Drawing.Size($capW, $capH)))
$g.Dispose()

function Dist($a, $b) {
    $dr = [int]$a.R - [int]$b.R
    $dg = [int]$a.G - [int]$b.G
    $db = [int]$a.B - [int]$b.B
    return [math]::Sqrt($dr * $dr + $dg * $dg + $db * $db)
}

# 桌面参考色：截图左上角（窗口外）
$deskTopLeft = $bmp.GetPixel(2, 2)

$corners = @(
    @{ name = "左上"; x0 = $margin; y0 = $margin; dx = 1; dy = 1; ox = -8; oy = -8 },
    @{ name = "右上"; x0 = $margin + $w - 1; y0 = $margin; dx = -1; dy = 1; ox = 8; oy = -8 },
    @{ name = "左下"; x0 = $margin; y0 = $margin + $h - 1; dx = 1; dy = -1; ox = -8; oy = 8 },
    @{ name = "右下"; x0 = $margin + $w - 1; y0 = $margin + $h - 1; dx = -1; dy = -1; ox = 8; oy = 8 }
)

$fail = 0
foreach ($c in $corners) {
    # 每个角用"紧贴窗口外侧"的像素当桌面参考色：壁纸在窗口上下颜色可能不同
    $deskX = [Math]::Max(0, [Math]::Min($capW - 1, $c.x0 + $c.ox))
    $deskY = [Math]::Max(0, [Math]::Min($capH - 1, $c.y0 + $c.oy))
    $desk = $bmp.GetPixel($deskX, $deskY)
    # 沿对角线往里走，找到第一个"明显不是桌面"的像素
    $run = 0
    for ($i = 0; $i -lt 40; $i++) {
        $x = $c.x0 + $c.dx * $i
        $y = $c.y0 + $c.dy * $i
        if ($x -lt 0 -or $y -lt 0 -or $x -ge $capW -or $y -ge $capH) { break }
        $px = $bmp.GetPixel($x, $y)
        if ((Dist $px $desk) -gt 26) { break }
        $run = $i
    }
    $ok = $run -ge $MinRun
    if (-not $ok) { $fail++ }
    $tag = if ($ok) { "OK  " } else { "FAIL" }
    $color = if ($ok) { "Green" } else { "Red" }
    Write-Host ("{0} {1}: 对角线 {2}px 后才出现内容（阈值 {3}px）" -f $tag, $c.name, ($run + 1), $MinRun) -ForegroundColor $color
}

$bmp.Dispose()
if ($fail -gt 0) {
    Write-Host "FAIL: $fail 个角是方的（窗口裁剪可能失效）" -ForegroundColor Red
    exit 1
}
Write-Host "PASS: 四个角都是圆角" -ForegroundColor Green
