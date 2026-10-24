param(
    [string]$JavaHome = "C:\Program Files\Java\jdk-21.0.11",
    [string]$AppVersion = "1.0.0"
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root
$env:JAVA_HOME = $JavaHome

& .\mvnw.cmd -q clean package

$stage = Join-Path $root "target\jpackage-input"
if (Test-Path $stage) { Remove-Item $stage -Recurse -Force }
New-Item -ItemType Directory -Path $stage | Out-Null
Copy-Item "target\mc-launcher.jar" $stage

$dest = Join-Path $root "release\exe"
if (Test-Path $dest) { Remove-Item $dest -Recurse -Force }
New-Item -ItemType Directory -Path $dest | Out-Null

$args = @(
    "--type", "app-image",
    "--name", "NekoCraftLauncher",
    "--app-version", $AppVersion,
    "--vendor", "Neko Craft Launcher",
    "--dest", $dest,
    "--input", $stage,
    "--main-jar", "mc-launcher.jar",
    "--main-class", "com.example.demo.Launcher",
    "--java-options", "-Dfile.encoding=UTF-8"
)
$icon = Join-Path $root "src\main\resources\icon.ico"
if (Test-Path $icon) { $args += @("--icon", $icon) }

& "$JavaHome\bin\jpackage.exe" @args
if ($LASTEXITCODE -ne 0) { throw "jpackage failed with exit code $LASTEXITCODE" }

$exe = Join-Path $dest "NekoCraftLauncher\NekoCraftLauncher.exe"
$zip = Join-Path $root "release\NekoCraftLauncher-exe.zip"
if (Test-Path $zip) { Remove-Item $zip -Force }
Compress-Archive -Path (Join-Path $dest "NekoCraftLauncher\*") -DestinationPath $zip -CompressionLevel Optimal
Write-Host "EXE ready: $exe"
Write-Host "ZIP ready: $zip"
Write-Host "game\ and config.json will be created next to NekoCraftLauncher.exe on first run."
Write-Host "For an .exe installer, install WiX Toolset 3.x and use --type exe."
