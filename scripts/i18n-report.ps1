# 清点界面里还没走 i18n 的中文字面量，并给出可导入.csv 的清单。
#
# 用法：
#   .\scripts\i18n-report.ps1                 # 打印统计 + 写 build\i18n-report.csv
#   .\scripts\i18n-report.ps1 -Check          # 与 scripts\i18n-baseline.json 比，回升就 exit 1
#   .\scripts\i18n-report.ps1 -Top 30         # 只看前 30 个文件
param(
    [switch]$Check,
    [int]$Top = 15,
    [string]$Csv = "build\i18n-report.csv",
    [string]$Baseline = "scripts\i18n-baseline.json",
    [switch]$WriteBaseline
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$uiDir = Join-Path $root "src\main\java\com\example\demo\ui"
$utf8 = New-Object System.Text.UTF8Encoding($false)
$pattern = '"([^"\\\r\n]*[\u4e00-\u9fff][^"\\\r\n]*)"'
$rows = New-Object System.Collections.Generic.List[object]
$perFile = @{}

foreach ($file in Get-ChildItem $uiDir -Recurse -Filter *.java) {
    $lines = [System.IO.File]::ReadAllLines($file.FullName)
    for ($i = 0; $i -lt $lines.Length; $i++) {
        # 已经包进 I18n.t("key", "中文") 的不算欠账：中文只是英文缺失时的兜底
        $scanned = [regex]::Replace($lines[$i], 'I18n\.t\(\s*"[^"]*"\s*,\s*"[^"]*"\s*\)', 'I18n.t("")')
        foreach ($match in [regex]::Matches($scanned, $pattern)) {
            $text = $match.Groups[1].Value
            $rows.Add([pscustomobject]@{
                File = $file.FullName.Substring($root.Length + 1)
                Line = $i + 1
                Text = $text
            })
            $key = $file.Name
            if (-not $perFile.ContainsKey($key)) { $perFile[$key] = 0 }
            $perFile[$key] = $perFile[$key] + 1
        }
    }
}

$csvDir = Split-Path -Parent (Join-Path $root $Csv)
if (-not (Test-Path $csvDir)) { New-Item -ItemType Directory -Path $csvDir -Force | Out-Null }
$rows | Export-Csv -Path (Join-Path $root $Csv) -NoTypeInformation -Encoding UTF8

Write-Host ("界面里的中文字面量：" + $rows.Count + " 条，分布在 " + $perFile.Count + " 个文件")
Write-Host ("明细已写入：" + $Csv)
$perFile.GetEnumerator() | Sort-Object Value -Descending | Select-Object -First $Top |
    ForEach-Object { Write-Host ("  {0,-34} {1,4}" -f $_.Key, $_.Value) }

if ($WriteBaseline) {
    $payload = [pscustomobject]@{
        generated = (Get-Date -Format "yyyy-MM-dd")
        note = "只降不升：新文案请写成 I18n.t(`"key`", `"中文`")，这个数字才会往下走"
        total = $rows.Count
        files = $perFile
    }
    $payload | ConvertTo-Json -Depth 4 | Set-Content -Path (Join-Path $root $Baseline) -Encoding UTF8
    Write-Host ("基线已更新：" + $Baseline + "（total=" + $rows.Count + "）")
}

if ($Check) {
    $baselinePath = Join-Path $root $Baseline
    if (-not (Test-Path $baselinePath)) {
        throw "缺少基线文件：$Baseline（先跑一次不带 -Check 的版本，把总数写进去）"
    }
    $baselineJson = Get-Content $baselinePath -Raw -Encoding UTF8 | ConvertFrom-Json
    $allowed = [int]$baselineJson.total
    if ($rows.Count -gt $allowed) {
        Write-Host ("FAIL：中文字面量从基线 " + $allowed + " 涨到了 " + $rows.Count + "，新文案请走 I18n.t(key, 中文)") -ForegroundColor Red
        exit 1
    }
    Write-Host ("PASS：中文字面量 " + $rows.Count + " ≤ 基线 " + $allowed) -ForegroundColor Green
}
exit 0
