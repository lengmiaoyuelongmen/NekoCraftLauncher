param(
    [string]$RepoName = "NekoCraftLauncher",
    [ValidateSet("public", "private")]
    [string]$Visibility = "public"
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $root

if (-not (Get-Command git -ErrorAction SilentlyContinue)) {
    throw "未找到 git。请先运行：winget install --id Git.Git -e"
}
if (-not (Get-Command gh -ErrorAction SilentlyContinue)) {
    throw "未找到 GitHub CLI。请先运行：winget install --id GitHub.cli -e"
}

gh auth status | Out-Host
if ($LASTEXITCODE -ne 0) {
    throw "GitHub CLI 尚未登录。请先运行：gh auth login"
}

if (-not (Test-Path ".git")) {
    git init
}

git add .
git commit -m "Neko Craft Launcher" 2>$null
if ($LASTEXITCODE -ne 0) {
    Write-Host "没有新的改动需要提交，继续创建远端仓库。"
}

$existing = gh repo view $RepoName --json name 2>$null
if ($LASTEXITCODE -eq 0) {
    Write-Host "仓库已存在：$RepoName"
} else {
    gh repo create $RepoName --$Visibility --source=. --remote=origin --push
}

gh repo view --web
Write-Host ""
Write-Host "更新源可填写："
gh repo view --json owner,name --jq '"https://api.github.com/repos/\(.owner.login)/\(.name)/releases/latest"'
Write-Host ""
Write-Host "发布新版本："
Write-Host "  git tag v1.0.1"
Write-Host "  git push origin v1.0.1"
