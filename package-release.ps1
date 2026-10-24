param(
    [string]$JavaHome = "C:\Program Files\Java\jdk-21.0.11"
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root
$env:JAVA_HOME = $JavaHome

& .\mvnw.cmd -q clean package

$out = Join-Path $root "release\NekoCraftLauncher"
if (Test-Path $out) { Remove-Item $out -Recurse -Force }
New-Item -ItemType Directory -Path $out | Out-Null

Copy-Item "target\mc-launcher.jar" $out
Copy-Item "README.md" $out

$bat = @"
@echo off
cd /d "%~dp0"
start "" javaw -jar "%~dp0mc-launcher.jar"
"@
Set-Content -Path (Join-Path $out "start.bat") -Value $bat -Encoding ASCII

Write-Host "Release ready: $out"
Write-Host "Included: mc-launcher.jar, README.md, start.bat"
Write-Host "Not included: game\, config.json (created on first launch)"
