param(
    [string]$InnoSetup = "",
    [string]$AppVersion = "1.0.0",
    [switch]$CopyToDesktop,
    [switch]$SkipAppImage
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root

if (-not $SkipAppImage) {
    & .\package-exe.ps1
}

if ([string]::IsNullOrWhiteSpace($InnoSetup)) {
    $candidates = @(
        "C:\Program Files\Inno Setup 7\ISCC.exe",
        "C:\Program Files (x86)\Inno Setup 6\ISCC.exe",
        "C:\Program Files\Inno Setup 6\ISCC.exe",
        "C:\Program Files (x86)\Inno Setup 5\ISCC.exe"
    )
    $InnoSetup = $candidates | Where-Object { Test-Path $_ } | Select-Object -First 1
}
if ([string]::IsNullOrWhiteSpace($InnoSetup) -or -not (Test-Path $InnoSetup)) {
    throw "Inno Setup not found. Install Inno Setup 6/7 or pass -InnoSetup <ISCC.exe>"
}

& $InnoSetup "/DMyAppVersion=$AppVersion" "installer\MCLauncher.iss"
if ($LASTEXITCODE -ne 0) { throw "ISCC failed with exit code $LASTEXITCODE" }

$setup = Join-Path $root "release\installer-inno\NekoCraftLauncher-Setup-$AppVersion-x64.exe"
if (-not (Test-Path $setup)) { throw "Installer not found: $setup" }

if ($CopyToDesktop) {
    $desktop = [Environment]::GetFolderPath("Desktop")
    Copy-Item $setup (Join-Path $desktop "NekoCraftLauncher-Setup-$AppVersion-x64.exe") -Force
    Write-Host "Copied to desktop."
}

Write-Host "Installer ready: $setup"
Write-Host "Black/white wizard artwork: installer\wizard.bmp, installer\wizard-small.bmp"
