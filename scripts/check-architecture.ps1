# Architecture guard: core must stay free of JavaFX and UI classes.
# Usage (from project root): powershell -ExecutionPolicy Bypass -File scripts\check-architecture.ps1
# Exits 1 when a rule is broken.

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$demo = Join-Path $root 'src\main\java\com\example\demo'
$coreDir = Join-Path $demo 'core'
$uiDir = Join-Path $demo 'ui'

if (-not (Test-Path $coreDir)) {
    Write-Host 'FAIL: core package not found at src\main\java\com\example\demo\core'
    exit 1
}

$rootClasses = @(Get-ChildItem $demo -Filter *.java | ForEach-Object { $_.BaseName })
$coreFiles = @(Get-ChildItem $coreDir -Recurse -Filter *.java)
$uiFiles = @()
if (Test-Path $uiDir) { $uiFiles = @(Get-ChildItem $uiDir -Recurse -Filter *.java) }
$errors = New-Object System.Collections.Generic.List[string]

foreach ($f in $coreFiles) {
    $text = Get-Content $f.FullName -Raw
    if ($text -match 'javafx\.') {
        $errors.Add("javafx reference: $($f.FullName)")
    }
    foreach ($rc in $rootClasses) {
        if ($text -match "(?m)^\s*import\s+com\.example\.demo\.$rc\s*;") {
            $errors.Add("entry dependency '$rc' imported by $($f.Name)")
        }
    }
    if ($text -match '(?m)^\s*import\s+com\.example\.demo\.ui\.') {
        $errors.Add("UI dependency imported by $($f.FullName)")
    }
}

Write-Host ("core classes: {0} in {1} packages" -f $coreFiles.Count, (@($coreFiles | Group-Object { $_.Directory.Name }).Count))
foreach ($g in ($coreFiles | Group-Object { $_.Directory.Name } | Sort-Object Name)) {
    Write-Host ("  {0,-10} {1}" -f $g.Name, $g.Count)
}
Write-Host ("ui classes:   {0}" -f $uiFiles.Count)
foreach ($g in ($uiFiles | Group-Object { $_.Directory.Name } | Sort-Object Name)) {
    Write-Host ("  {0,-10} {1}" -f $g.Name, $g.Count)
}
Write-Host ("entry classes: {0}" -f $rootClasses.Count)

if ($errors.Count -gt 0) {
    Write-Host ''
    Write-Host 'FAIL: architecture rules violated'
    foreach ($e in $errors) { Write-Host "  - $e" }
    exit 1
}

Write-Host ''
Write-Host 'PASS: core has no javafx, no ui.* and no entry imports'
exit 0
