param(
    [string]$JavaHome = "C:\Program Files\Java\jdk-21.0.11",
    [string]$WixDir = "$env:LOCALAPPDATA\Programs\wix314",
    [string]$AppVersion = "1.0.0"
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root
$env:JAVA_HOME = $JavaHome

if (-not (Test-Path (Join-Path $WixDir "candle.exe"))) {
    Write-Host "WiX 3.x not found at $WixDir"
    Write-Host "Install it with: winget install WiXToolset.WiXToolset"
    Write-Host "or download wix314-binaries.zip and extract to that folder."
    exit 1
}
$env:PATH = "$WixDir;$env:PATH"

& .\mvnw.cmd -q clean package

$stage = Join-Path $root "target\jpackage-input"
if (Test-Path $stage) { Remove-Item $stage -Recurse -Force }
New-Item -ItemType Directory -Path $stage | Out-Null
Copy-Item "target\mc-launcher.jar" $stage

$dest = Join-Path $root "release\installer"
if (Test-Path $dest) { Remove-Item $dest -Recurse -Force }
New-Item -ItemType Directory -Path $dest | Out-Null

$jpackageArgs = @(
    "--type", "exe",
    "--name", "NekoCraftLauncher",
    "--app-version", $AppVersion,
    "--vendor", "Neko Craft Launcher",
    "--dest", $dest,
    "--input", $stage,
    "--main-jar", "mc-launcher.jar",
    "--main-class", "com.example.demo.Launcher",
    "--java-options", "-Dfile.encoding=UTF-8",
    "--win-dir-chooser",
    "--win-menu",
    "--win-menu-group", "Neko Craft Launcher",
    "--win-shortcut",
    "--win-per-user-install",
    "--win-upgrade-uuid", "3f2c9a1e-7b4d-4c8e-9f10-6a5b2d3c4e5f"
)
$icon = Join-Path $root "src\main\resources\icon.ico"
if (Test-Path $icon) { $jpackageArgs += @("--icon", $icon) }

& "$JavaHome\bin\jpackage.exe" @jpackageArgs
if ($LASTEXITCODE -ne 0) { throw "jpackage failed with exit code $LASTEXITCODE" }

$installer = Join-Path $dest "NekoCraftLauncher-$AppVersion.exe"
Write-Host "Installer ready: $installer"
Write-Host "Includes Java runtime, Start Menu entry and desktop shortcut."
Write-Host "game\ and config.json are created next to the installed exe on first run."
