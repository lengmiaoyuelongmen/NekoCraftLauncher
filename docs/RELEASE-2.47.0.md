# Neko Craft Launcher 2.47.0

> 更新界面收尾：更新说明可滚动看全、备份能管理、跨版本会提醒。

## 安装包

| 文件 | 说明 |
|---|---|
| `NekoCraftLauncher-2.47.0.exe` | **安装版**：jpackage/WiX，静默参数 `/quiet` |
| `NekoCraftLauncher-2.47.0-win64.zip` | 免安装版；支持就地更新、回退与备份管理 |
| `NekoCraftLauncher-2.47.0-Features.pdf` / `.docx` | 功能预览（图文版） |
| `SHA256SUMS.txt` / `verify.txt` / `neko-codesign.cer` | 校验与签名材料 |

## 1. 更新说明内嵌展示

检查到新版本后，原来只把 `notes` 拼在状态行里（长说明直接被挤没）。现在多了一块
**只读、可滚动、自动换行**的说明区，Release notes 有多少写多少；没有说明就自动隐藏。

## 2. 备份管理

设置 → 软件更新 新增 **管理备份**：

- 列出 `update\backup\` 下的所有备份，显示时间与占用；
- 底部显示「共占用 X MB」；
- 选中可 **删除**（文件被占用时给明确失败提示，不会静默吞掉）。

配合上一版的「回退到上一版」，免安装版现在有完整的「更新—回退—清理」闭环。

## 3. 跨版本提醒

版本差 ≥ 3 个小版本时，状态行会追加：

> 这次跨了好几个版本，建议先看完下面的更新说明再装。

大版本跳跃（如 2.x → 3.0）按 100 计，一定触发提醒。

## 实现与测试

- `UpdateChecker.versionGap(remote, current)`：同大版本按 minor 差，跨大版本按 ×100；
- `UpdateInstaller.listBackups / backupSize / deleteBackup`：新到旧排序、递归求和、递归删除（失败返回 false）；
- 新增 `UpdateGapAndBackupTest`（2 条）：版本差计算（0/1/5/100）与备份列举、占用统计、删除。

## 工程质量

- 编译 0 告警；单元测试 **341 项全绿**（+2）；无头冒烟六页全过；文案基线 2227
- exe（安装包 + 便携壳）均已签名并带 DigiCert 时间戳

## 校验

```
a1fbf5e07f0baca14e506766f3ebd9f6f331520231df2e129dee82f3aa729d66  NekoCraftLauncher-2.47.0.exe
e11550d3605f2164b5ab515dc8656c9bb764cddfb3f2baa9ff97f77d5bff35bc  NekoCraftLauncher-2.47.0-win64.zip
f5b432f7c36c9869f257530f9e38b005fda1e8daa1e402ca47fc5f676f0dafb5  NekoCraftLauncher-2.47.0-Features.pdf
981a3209cd7caf947b7fa3a3f13169f304fb9b54cdfd6894f7bb4d2f4e51493e  NekoCraftLauncher-2.47.0-Features.docx
41bdc7658bd42ee52d59d81ccd541597b7deac9e600715ab23327227a73cdb89  neko-codesign.cer
1702eb5a31cfbb3c74141766af723c5ace0f7bb6aec7abd03969710bb12f0129  verify.txt
```
