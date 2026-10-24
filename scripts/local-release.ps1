param(
    [string]$Version = "1.0.3",
    [int]$Port = 8787,
    [switch]$NoServe,
    [string]$PythonExe = "C:\Users\Administrator\.dsh\dsh-runtimes\dsh-primary-runtime\dependencies\python\python.exe"
)

$ErrorActionPreference = "Continue"
$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $root
$env:Path = "$env:Path;C:\Program Files\Git\cmd"

$base = Join-Path $root "local-github"
$repo = Join-Path $base "NekoCraftLauncher.git"
$site = Join-Path $base "site"
$rel = Join-Path $site "releases\v$Version"
New-Item -ItemType Directory -Path $rel -Force | Out-Null

if (-not (Test-Path $repo)) {
    git init --bare $repo | Out-Host
}
git remote remove local 2>$null
git remote add local $repo
git push local main --tags 2>$null | Out-Host

$setup = Join-Path $root "release\installer-inno\NekoCraftLauncher-Setup-$Version-x64.exe"
$zip = Join-Path $root "release\NekoCraftLauncher-exe.zip"
if (-not (Test-Path $setup)) { throw "Installer not found: $setup" }
Copy-Item $setup (Join-Path $rel "NekoCraftLauncher-Setup-$Version-x64.exe") -Force
if (Test-Path $zip) { Copy-Item $zip (Join-Path $rel "NekoCraftLauncher-exe.zip") -Force }

$setupUrl = "http://127.0.0.1:$Port/releases/v$Version/NekoCraftLauncher-Setup-$Version-x64.exe"
$update = [ordered]@{
    version = $Version
    url = $setupUrl
    notes = "本地 Release v$Version：分段并发下载、Azul Zulu 自动准备、中文安装程序、托盘守护。"
}
$utf8 = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText((Join-Path $site "update.json"), ($update | ConvertTo-Json -Depth 4), $utf8)

$commits = (git log --oneline -8 | ForEach-Object { "<li><code>$_</code></li>" }) -join "`n"
$html = @'
<!doctype html>
<html lang="zh-CN">
<head>
<meta charset="utf-8">
<title>Neko Craft Launcher · Local GitHub</title>
<style>
:root { --bg:#12080a; --panel:#1d0d10; --line:#4a2024; --red:#e11d2e; --gold:#ffcf6a; --text:#f5e8e6; --dim:#b99a98; }
* { box-sizing:border-box; }
body { margin:0; background:radial-gradient(circle at 20% 0%, #2a1014, var(--bg) 55%); color:var(--text); font-family:"Cascadia Mono","Consolas","Microsoft YaHei UI",monospace; }
header { padding:24px 40px; border-bottom:1px solid var(--line); display:flex; gap:14px; align-items:center; }
.star { color:var(--gold); font-size:24px; }
h1 { font-size:20px; margin:0; }
h1 span { color:var(--red); }
main { max-width:900px; margin:36px auto; padding:0 24px; }
.card { background:linear-gradient(180deg,#241014,#180a0d); border:1px solid var(--line); border-radius:12px; padding:22px; margin-bottom:20px; }
h2 { font-size:15px; color:var(--gold); margin:0 0 14px; }
code, pre { background:#0e0507; border:1px solid var(--line); border-radius:6px; color:#ffb0a0; }
code { padding:2px 6px; }
pre { padding:14px; overflow:auto; }
a { color:var(--gold); }
ul { padding-left:20px; }
li { margin:6px 0; color:var(--dim); }
.btn { display:inline-block; padding:9px 16px; background:var(--red); color:#fff; text-decoration:none; border-radius:8px; font-weight:bold; margin:6px 8px 6px 0; }
small { color:var(--dim); }
</style>
</head>
<body>
<header><div class="star">★</div><h1>Neko <span>Craft</span> Launcher <small>/ Local GitHub</small></h1></header>
<main>
  <div class="card">
    <h2>仓库 / Repository</h2>
    <p><code>{{REPO}}</code></p>
    <pre>git clone "{{REPO}}"</pre>
    <p><small>本地裸仓库，包含 main 与全部 tag；无需 GitHub 账号。</small></p>
  </div>
  <div class="card">
    <h2>最新 Release · v{{VERSION}}</h2>
    <a class="btn" href="releases/v{{VERSION}}/NekoCraftLauncher-Setup-{{VERSION}}-x64.exe">下载安装包</a>
    <a class="btn" href="releases/v{{VERSION}}/NekoCraftLauncher-exe.zip">下载便携版</a>
    <p><small>更新源：<code>http://127.0.0.1:{{PORT}}/update.json</code></small></p>
  </div>
  <div class="card">
    <h2>最近提交</h2>
    <ul>{{COMMITS}}</ul>
  </div>
  <div class="card">
    <h2>启动本地服务</h2>
    <pre>powershell -ExecutionPolicy Bypass -File .\scripts\local-release.ps1 -Version {{VERSION}}</pre>
    <p><small>为爱发电 · 永远免费更新 · 代码属于人民</small></p>
  </div>
</main>
</body>
</html>
'@
$html = $html.Replace("{{VERSION}}", $Version).Replace("{{REPO}}", $repo).Replace("{{PORT}}", "$Port").Replace("{{COMMITS}}", $commits)
[System.IO.File]::WriteAllText((Join-Path $site "index.html"), $html, $utf8)

Write-Host "Local repo: $repo"
Write-Host "Site:       $site"
Write-Host "Update:     http://127.0.0.1:$Port/update.json"
Write-Host "Browser:    http://127.0.0.1:$Port/"

if (-not $NoServe) {
    $existing = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
    if ($existing) {
        Write-Host "Server already listening on $Port"
    } else {
        if (-not (Test-Path $PythonExe)) { $PythonExe = "python" }
        $proc = Start-Process -FilePath $PythonExe -ArgumentList @("-m", "http.server", "$Port", "--bind", "127.0.0.1", "--directory", $site) -WindowStyle Hidden -PassThru
        $proc.Id | Set-Content (Join-Path $base "server.pid")
        Start-Sleep -Seconds 1
        Write-Host "Started local server PID $($proc.Id)"
    }
}
