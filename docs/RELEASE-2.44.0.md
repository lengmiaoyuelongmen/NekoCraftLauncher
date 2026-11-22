# Neko Craft Launcher 2.44.0

> 「立即安装」这次是真的：静默安装 → 自动重启 → 启动后确认版本。

## 安装包

| 文件 | 说明 |
|---|---|
| `NekoCraftLauncher-2.44.0.exe` | **安装版**（jpackage / WiX，静默参数 `/quiet`） |
| `NekoCraftLauncher-2.44.0-win64.zip` | 免安装版，解压双击 `NekoCraftLauncher.exe` |
| `NekoCraftLauncher-2.44.0-Features.pdf` / `.docx` | 功能预览（图文版） |
| `SHA256SUMS.txt` / `verify.txt` / `neko-codesign.cer` | 校验与签名材料 |

## 1. 立即安装：从「假动作」变成真安装

**之前为什么是假的**：安装包是 `jpackage --type exe` 产出的，静默参数是 `/quiet`；
旧代码传的是 Inno Setup 的 `/VERYSILENT /SUPPRESSMSGBOXES /NORESTART /CLOSEAPPLICATIONS`，
jpackage 不认这些参数 → 弹窗/报错或者干脆什么都不做，启动器却已经在 1.2 秒后自己退出了。

**现在**（`core/app/UpdateInstaller.java`）：

1. 安装包复制到 `%LOCALAPPDATA%\NekoCraftLauncher\update\`（临时目录会被清理）；
2. 写 `pending.json`（记录目标版本 + 安装包路径）；
3. 生成 `apply-update.cmd`：
   ```
   timeout /t 2                ← 先等启动器自己退出，否则 exe 被占用无法覆盖
   start /wait "...\Setup.exe" /quiet
   timeout /t 2
   start "" "<已安装的启动器.exe>"   ← 装完自动重新打开
   ```
4. 启动器退出，助手在后台完成安装并重新拉起新版。

### 实测证据（就在这台机器上）

```
NekoCraftLauncher-2.44.0.exe /quiet  → exit 0
安装目录：%LOCALAPPDATA%\NekoCraftLauncher\NekoCraftLauncher.exe
NekoCraftLauncher.cfg：-Djpackage.app-version=2.44.0   ← 装上的确实是 2.44.0
开始菜单快捷方式：NekoCraftLauncher.lnk
```
验证完已用 `msiexec /X{2B107933-…} /qn` 卸载干净（卸载后目录与注册项都没了），没在你机器上留测试安装。

## 2. 重启提示是真的会弹

新版启动时读 `pending.json`：

- 当前版本已到达目标版本 → Toast「已更新到 2.44.0」并清掉标记；
- 仍在旧版本 → 弹窗「更新等待完成」，按钮 **现在重启安装**（退出进程让助手继续装）。

## 3. 其他任务

- **下载前先告知**：更新卡片现在显示 `发现新版本 2.44.0（106.4 MB）`，不再是点下去才知道要下多大；
- **诊断单加安全软件探测**：检测 360 / 火绒 / Defender / 卡巴等常见进程，
  命中就写 `安全软件：360tray（若启动无反应，先把它加进信任区）`；
  这条正是那位「点了没反应」用户的关键线索。

## 工程质量

- 编译 0 告警；单元测试 **336 项全绿**（+3：安装脚本含 `/quiet` 且不含 `/VERYSILENT`、
  pending 标记读写、安装包归档）；
- 无头冒烟 `-Dmc.smoke=1` 六页全过；文案基线 2226（还降了 1）。

## 校验

```
2caa6755b56d0a8fe70889f0ca73a69299b507347d74f3b8c20ca2d6a5cff929  NekoCraftLauncher-2.44.0.exe
2c5ba2065a27e16172cf5f8c7d8282c07e5b8000f391e2d8e454c4e9325044e7  NekoCraftLauncher-2.44.0-win64.zip
fe3cf164efc9c19c2211ddb5ebf520d77b38fb259f2c598f577a02971dd9a3cc  NekoCraftLauncher-2.44.0-Features.pdf
676eaa039861f40413ad84366a0505680a7415175bccb1cdf01a5c0ea137c885  NekoCraftLauncher-2.44.0-Features.docx
41bdc7658bd42ee52d59d81ccd541597b7deac9e600715ab23327227a73cdb89  neko-codesign.cer
531dd6785a8e07291c22c3ceeaca1be907bdfcc3b45cc1a56693d6f70a3f3f9d  verify.txt
```
