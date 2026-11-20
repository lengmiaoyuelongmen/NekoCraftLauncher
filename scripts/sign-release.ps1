# 给发布产物做代码签名（自签名证书，可验证、可替换成 CA 证书）。
#
# 用法：
#   .\scripts\sign-release.ps1 -Version 2.10.0              # 首次会生成自签名证书
#   .\scripts\sign-release.ps1 -Version 2.10.0 -PfxPath D:\codesign.pfx -PfxPassword ***
#
# 做的事：
#   1. 证书：没有就用 New-SelfSignedCertificate 生成一张 5 年期的代码签名证书并导出 .pfx/.cer
#   2. exe：signtool 签安装包与免安装版的启动 exe（SHA256 + 时间戳）
#   3. jar：jarsigner 签 app-image 里的启动器 jar
#   4. zip：签名后重新打包，保证 zip 里的是签名版
#   5. 校验：signtool verify / jarsigner -verify，结果写到 release\signing\verify.txt
param(
    [Parameter(Mandatory = $true)][string]$Version,
    [string]$ReleaseDir = "release",
    [string]$PfxPath = "release\signing\neko-codesign.pfx",
    [string]$PfxPassword = "neko-local",
    [string]$JavaHome = "C:\Program Files\Java\jdk-21.0.11",
    [string]$TimestampUrl = "http://timestamp.digicert.com",
    [switch]$SkipTimestamp,
    [switch]$SkipJar
)

$ErrorActionPreference = "Continue"  # 原生工具（keytool/jarsigner/signtool）会把正常输出写到 stderr
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

function Resolve-SignTool {
    $kitsRoot = "${env:ProgramFiles(x86)}\Windows Kits\10\bin"
    $candidates = @(
        "$kitsRoot\10.0.26100.0\x64\signtool.exe",
        "$kitsRoot\10.0.22621.0\x64\signtool.exe"
    )
    foreach ($path in $candidates) {
        if (Test-Path $path) { return $path }
    }
    $found = Get-ChildItem "$env:ProgramFiles(x86)\Windows Kits\10\bin" -Recurse -Filter signtool.exe -ErrorAction SilentlyContinue |
        Where-Object { $_.FullName -match '\\x64\\' } | Select-Object -First 1
    if ($found) { return $found.FullName }
    throw "找不到 signtool.exe（安装 Windows SDK）"
}

$signTool = Resolve-SignTool
$jarsigner = Join-Path $JavaHome "bin\jarsigner.exe"
$keytool = Join-Path $JavaHome "bin\keytool.exe"
$signingDir = Join-Path $root "release\signing"
if (-not (Test-Path $signingDir)) { New-Item -ItemType Directory -Path $signingDir -Force | Out-Null }

# ---------- 1. 证书 ----------
$pfxFull = Join-Path $root $PfxPath
if (-not (Test-Path $pfxFull)) {
    Write-Host "生成自签名代码签名证书（5 年）..."
    $cert = New-SelfSignedCertificate `
        -Type CodeSigningCert `
        -Subject "CN=Neko Craft Launcher (self-signed)" `
        -FriendlyName "Neko Craft Launcher self-signed" `
        -KeyUsage DigitalSignature `
        -KeyExportPolicy Exportable `
        -KeyLength 2048 `
        -HashAlgorithm SHA256 `
        -NotAfter (Get-Date).AddYears(5) `
        -CertStoreLocation "Cert:\CurrentUser\My"
    $secure = ConvertTo-SecureString -String $PfxPassword -Force -AsPlainText
    Export-PfxCertificate -Cert $cert -FilePath $pfxFull -Password $secure | Out-Null
    Export-Certificate -Cert $cert -FilePath (Join-Path $signingDir "neko-codesign.cer") | Out-Null
    Write-Host "证书已导出：$pfxFull（公钥 .cer 在 release\signing\）"
} else {
    Write-Host "复用已有证书：$pfxFull"
}

$jarKeystore = Join-Path $signingDir "neko-jar.jks"
if (-not (Test-Path $jarKeystore)) {
    Write-Host "生成 jar 签名用的 keystore..."
    & $keytool -genkeypair -alias neko -keyalg RSA -keysize 2048 -validity 1825 `
        -dname "CN=Neko Craft Launcher (self-signed)" `
        -keystore $jarKeystore -storepass $PfxPassword -keypass $PfxPassword 2>&1 |
        Select-Object -Last 3
}

# ---------- 2. 签 exe ----------
function Sign-Exe([string]$file) {
    if (-not (Test-Path $file)) { return $false }
    # jpackage 生成的 app-image 文件带只读属性，签名前必须先放开
    Set-ItemProperty -Path $file -Name IsReadOnly -Value $false
    $args = @("sign", "/f", $pfxFull, "/p", $PfxPassword, "/fd", "SHA256")
    if (-not $SkipTimestamp) {
        $args += @("/tr", $TimestampUrl, "/td", "SHA256")
    }
    $args += $file
    & $signTool @args | Out-Null
    if ($LASTEXITCODE -ne 0) {
        Write-Host "  签名失败（可能需要联网时间戳）：$file，改为不带时间戳重试"
        & $signTool sign /f $pfxFull /p $PfxPassword /fd SHA256 $file | Out-Null
        if ($LASTEXITCODE -ne 0) { throw "signtool 签名失败：$file" }
    }
    Write-Host "  已签名 exe: $file"
    return $true
}

$exeTargets = @()
$exeTargets += Get-ChildItem (Join-Path $root "$ReleaseDir\installer") -Filter "NekoCraftLauncher-$Version.exe" -ErrorAction SilentlyContinue
$exeTargets += Get-ChildItem (Join-Path $root "$ReleaseDir\exe\NekoCraftLauncher") -Filter "NekoCraftLauncher.exe" -ErrorAction SilentlyContinue
foreach ($exe in $exeTargets) { Sign-Exe $exe.FullName | Out-Null }

# ---------- 3. 签 jar（app image 内） ----------
$appJars = Get-ChildItem (Join-Path $root "$ReleaseDir\exe\NekoCraftLauncher\app") -Filter "*.jar" -ErrorAction SilentlyContinue
foreach ($jar in $appJars) {
    if ($SkipJar) {
        Write-Host "  跳过 jar 签名（打包前已签）：$($jar.Name)"
        continue
    }
    & $jarsigner -keystore $jarKeystore -storepass $PfxPassword -keypass $PfxPassword `
        -sigalg SHA256withRSA -digestalg SHA-256 $jar.FullName neko 2>&1 | Select-Object -Last 2
    if ($LASTEXITCODE -ne 0) { throw "jarsigner 签名失败：$($jar.FullName)" }
    Write-Host "  已签名 jar: $($jar.Name)"
}

# ---------- 4. 重新打包 zip（里面必须是签名后的 exe/jar） ----------
$appDir = Join-Path $root "$ReleaseDir\exe\NekoCraftLauncher"
$zip = Join-Path $root "$ReleaseDir\NekoCraftLauncher-$Version-win64.zip"
if (Test-Path $zip) { Remove-Item $zip -Force }
Compress-Archive -Path (Join-Path $appDir "*") -DestinationPath $zip -CompressionLevel Optimal
Write-Host "已重新打包（签名版）：$zip"

# ---------- 5. 校验 ----------
$report = New-Object System.Collections.Generic.List[string]
$report.Add("Neko Craft Launcher $Version 代码签名校验")
$report.Add("时间：$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')")
$report.Add("证书：CN=Neko Craft Launcher (self-signed)（自签名，未购买 CA）")
$report.Add("")

foreach ($exe in $exeTargets) {
    $sig = Get-AuthenticodeSignature $exe.FullName
    $report.Add("EXE  $($exe.Name)")
    $report.Add("     Status      : $($sig.Status)")
    $report.Add("     Signer      : $($sig.SignerCertificate.Subject)")
    $report.Add("     Thumbprint  : $($sig.SignerCertificate.Thumbprint)")
    $report.Add("     TimeStamper : $($sig.TimeStamperCertificate.Subject)")
    $verify = & $signTool verify /pa $exe.FullName 2>&1 | Out-String
    $report.Add("     signtool    : " + ($verify.Trim() -replace "`r?`n", " | "))
    $report.Add("")
}

foreach ($jar in $appJars) {
    $jarVerify = & $jarsigner -verify $jar.FullName 2>&1 | Out-String
    $report.Add("JAR  $($jar.Name)")
    $report.Add("     jarsigner   : " + ($jarVerify.Trim() -replace "`r?`n", " | "))
    $report.Add("")
}

$reportPath = Join-Path $signingDir "verify.txt"
$report | Set-Content -Path $reportPath -Encoding UTF8
Write-Host ""
$report | ForEach-Object { Write-Host $_ }
Write-Host "校验报告：$reportPath"
