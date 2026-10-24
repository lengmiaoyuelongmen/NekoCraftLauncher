# Neko Craft Launcher

> 为爱发电 · 永远免费更新 · 代码属于人民

一个 Minecraft Java 版第三方启动器，使用 JavaFX 21 构建。界面采用共产国际 / MOSS 风格：红旗、五角星、红色光学核心、极客终端与集体主义文案。

## 功能

- 离线登录 / 微软正版登录 / 第三方外置登录（LittleSkin Yggdrasil）
- 版本下载、加载器安装（Fabric / Quilt / Forge / NeoForge / OptiFine）
- 实例管理、实例级内存 / JVM / Java 覆盖
- Modrinth 模组、整合包、资源包、光影搜索与下载
- 模组前置依赖检查与一键补齐
- 截图图库 + 应用内大图预览
- 启动历史、成功率、启动耗时与实例健康评分
- 游戏进程优先级、启动后最小化、游戏时暂停背景动画
- 命令面板 `Ctrl+K`
- MOSS 风格启动预加载页
- 共产红 / 为爱发电主题
- GitHub Releases 更新检查与安装包下载

## 环境

- Windows 10 / 11 x64
- JDK 21（开发）
- Maven Wrapper 已内置

## 运行

```powershell
.\mvnw.cmd clean package
java -jar target\mc-launcher.jar
```

或直接运行：

```powershell
.\run.cmd
```

## 打包

```powershell
# 生成自带 Java 运行时的 app-image 和便携 zip
powershell -ExecutionPolicy Bypass -File .\package-exe.ps1

# 生成 Inno Setup 安装包
powershell -ExecutionPolicy Bypass -File .\package-inno.ps1
```

产物：

- `release/exe/NekoCraftLauncher/NekoCraftLauncher.exe`
- `release/NekoCraftLauncher-exe.zip`
- `release/installer-inno/NekoCraftLauncher-Setup-<version>-x64.exe`

## GitHub Releases 自动更新

### 1. 创建仓库

本机若没有 Git / GitHub CLI，先安装并登录：

```powershell
winget install --id Git.Git -e
winget install --id GitHub.cli -e
gh auth login
```

然后初始化并推送：

```powershell
git init
git add .
git commit -m "Neko Craft Launcher"
gh repo create NekoCraftLauncher --public --source=. --remote=origin --push
```

也可以直接运行仓库准备脚本（需要已登录 `gh`）：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\create-github-repo.ps1
```

也可以直接在 GitHub 网页新建仓库，再按页面提示 `git remote add origin ...` 并推送。

### 2. 配置更新源

启动器「设置 → 软件更新」支持三种地址：

```text
https://api.github.com/repos/lengmiaoyuelongmen/NekoCraftLauncher/releases/latest
https://github.com/lengmiaoyuelongmen/NekoCraftLauncher
```

直接粘贴仓库地址会自动转换成 GitHub Releases API。也可以使用静态 JSON：

```json
{
  "version": "1.0.1",
  "url": "https://example.com/NekoCraftLauncher-Setup-1.0.1-x64.exe",
  "notes": "更新内容"
}
```

### 3. 发布新版本

```powershell
git tag v1.0.1
git push origin v1.0.1
```

`.github/workflows/release.yml` 会自动：

1. 从 tag 提取版本号并写入 `AppVersion.java`
2. 运行测试并构建 app-image
3. 编译 Inno Setup 安装包
4. 创建 GitHub Release 并上传安装包与便携版

启动器检测到更高版本后，可以直接在设置页「下载并安装更新」。

> 更新检查使用 GitHub 公共 API，未登录时受 60 次/小时限流。正式发布建议把仓库设为 Public。

## 目录

```text
src/main/java/com/example/demo/   JavaFX 启动器源码
src/main/resources/               样式、图标、主题资源
installer/                        Inno Setup 配置与向导图
.github/workflows/                自动构建与 Release 工作流
release/                          本地打包产物（不提交）
game/                             本地游戏数据（不提交）
config.json                       本地配置（不提交）
```

## 免责声明

本项目与 Mojang Studios、Microsoft、LittleSkin 无隶属关系。Minecraft 是 Mojang Studios 的商标。请使用正版游戏与服务。

## 许可证

GNU General Public License v3.0，见 [LICENSE](LICENSE)。

为爱发电，永远免费更新。
