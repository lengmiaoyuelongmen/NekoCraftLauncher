# Architecture guard: the core package must stay free of JavaFX and UI classes.
# Usage (from project root): powershell -ExecutionPolicy Bypass -File scripts\check-architecture.ps1
# Exits 1 when a rule is broken.

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$demo = Join-Path $root 'src\main\java\com\example\demo'
$coreDir = Join-Path $demo 'core'

if (-not (Test-Path $coreDir)) {
    Write-Host 'FAIL: core package not found at src\main\java\com\example\demo\core'
    exit 1
}

$rootClasses = @(Get-ChildItem $demo -Filter *.java | ForEach-Object { $_.BaseName })
$coreFiles = @(Get-ChildItem $coreDir -Recurse -Filter *.java)
$errors = New-Object System.Collections.Generic.List[string]

foreach ($f in $coreFiles) {
    $text = Get-Content $f.FullName -Raw
    if ($text -match 'javafx\.') {
        $errors.Add("javafx reference: $($f.FullName)")
    }
    foreach ($rc in $rootClasses) {
        if ($text -match "(?m)^\s*import\s+com\.example\.demo\.$rc\s*;") {
            $errors.Add("UI dependency '$rc' imported by $($f.Name)")
        }
    }
}

$groups = $coreFiles | Group-Object { $_.Directory.Name } | Sort-Object Name
Write-Host ("core classes: {0} in {1} packages" -f $coreFiles.Count, $groups.Count)
foreach ($g in $groups) {
    Write-Host ("  {0,-10} {1}" -f $g.Name, $g.Count)
}
Write-Host ("root classes (UI + entry): {0}" -f $rootClasses.Count)

if ($errors.Count -gt 0) {
    Write-Host ''
    Write-Host 'FAIL: architecture rules violated'
    foreach ($e in $errors) { Write-Host "  - $e" }
    exit 1
}

Write-Host ''
Write-Host 'PASS: core has no javafx or UI imports'
exit 0
