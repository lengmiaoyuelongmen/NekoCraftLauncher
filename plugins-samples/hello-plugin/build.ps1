# 编译并打包示例插件：产出 plugins/hello-plugin.jar
#
# 用法：
#   .\plugins-samples\hello-plugin\build.ps1
#   .\plugins-samples\hello-plugin\build.ps1 -LauncherJar path\to\mc-launcher.jar
param(
    [string]$JavaHome = "C:\Program Files\Java\jdk-21.0.11",
    [string]$LauncherJar = ""
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent (Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path))
if ([string]::IsNullOrWhiteSpace($LauncherJar)) {
    $LauncherJar = Join-Path $root "target\mc-launcher.jar"
}
if (-not (Test-Path $LauncherJar)) {
    throw "找不到启动器 jar：$LauncherJar（先跑 mvnw package）"
}

$src = Join-Path $PSScriptRoot "src"
$classes = Join-Path $PSScriptRoot "build\classes"
if (Test-Path $classes) { Remove-Item $classes -Recurse -Force }
New-Item -ItemType Directory -Path $classes -Force | Out-Null

$javac = Join-Path $JavaHome "bin\javac.exe"
$jar = Join-Path $JavaHome "bin\jar.exe"
if (-not (Test-Path $javac)) { $javac = "javac" }
if (-not (Test-Path $jar)) { $jar = "jar" }

$sources = @(Get-ChildItem $src -Recurse -Filter *.java | Select-Object -ExpandProperty FullName)
if ($sources.Count -eq 0) { throw "没有找到插件源码：$src" }
$args = @("-encoding", "UTF-8", "-cp", $LauncherJar, "-d", $classes) + $sources
& $javac $args
if ($LASTEXITCODE -ne 0) { throw "javac 编译失败" }

# 资源（ServiceLoader 的 META-INF/services）
$resources = Join-Path $src "META-INF"
if (Test-Path $resources) {
    Copy-Item $resources (Join-Path $classes "META-INF") -Recurse -Force
}

$pluginsDir = Join-Path $root "plugins"
if (-not (Test-Path $pluginsDir)) { New-Item -ItemType Directory -Path $pluginsDir | Out-Null }
$outJar = Join-Path $pluginsDir "hello-plugin.jar"
if (Test-Path $outJar) { Remove-Item $outJar -Force }
& $jar --create --file $outJar -C $classes .
if ($LASTEXITCODE -ne 0) { throw "jar 打包失败" }

Write-Host "插件已生成：$outJar"
Write-Host "启动启动器后，命令面板（Ctrl+K）里会出现「插件示例：打个招呼」。"
