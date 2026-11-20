# Neko Craft Launcher 2.12.1

> 2.12.0 的收尾：把「空状态」和设置页主按钮也接进语言包。到这一版，英文界面从顶栏、页面标题、
> 三套侧栏页签，到「还没有实例 / 还没有启动记录 / 还没有日志」这类空状态，全部是英文。

## 安装包

| 文件 | 说明 |
|---|---|
| `NekoCraftLauncher-2.12.1.exe` | **安装版**：双击安装，可选目录，开始菜单 + 桌面快捷方式（按用户安装） |
| `NekoCraftLauncher-2.12.1-win64.zip` | 免安装版，解压双击 `NekoCraftLauncher.exe` |
| `NekoCraftLauncher-2.12.1-Features.pdf` / `.docx` | 功能预览（图文版） |
| `SHA256SUMS.txt` | 全部产物 SHA256，`sha256sum -c` 可直接校验 |
| `verify.txt` / `neko-codesign.cer` | 签名报告与自签公钥 |

exe 与 jar 均已签名（自签名 + DigiCert 时间戳）。

## 1. 空状态双语

| key | 中文 | English |
|---|---|---|
| `empty.instances` | 还没有实例。去「下载」页装一个版本… | No instances yet. Install a version from Downloads… |
| `empty.launchHistory` | 还没有启动记录，启动一次后会显示在这里 | No launches yet - your first launch will show up here. |
| `empty.advisor` | 还没有启动记录，启动一次后会生成稳定性画像 | No launch history yet - a stability profile appears after the first launch. |
| `empty.favorites` | 还没有收藏的资源… | No favourites yet. Click the star on a card in Downloads… |
| `empty.logs` | 还没有日志… | No logs yet. latest.log shows up after the first game launch. |
| `empty.accounts` | 还没有保存的账户，登录后会自动保存。 | No saved accounts yet - signing in saves them automatically. |
| `empty.downloadTasks` | 还没有已完成的下载。 | No finished downloads yet. |
| `empty.downloadInfo` | 暂无下载任务 | No download tasks |
| `empty.servers` | 还没有 servers.dat… | No servers.dat yet. Open Multiplayer, then Add server… |

设置页主按钮：`保存 / 导入设置 / 导出设置 / 配置历史 / 恢复默认` 一并进语言包。

## 2. 文案欠账

| 版本 | 中文字面量 |
|---|---|
| 2.11.0 | 2269 |
| 2.12.0 | 2205 |
| **2.12.1** | **2190** |

守卫不变：`I18nCoverageTest` + `ci.ps1` 的「界面文案基线」，只降不升；清单在
`build/i18n-report.csv`。

## 工程质量

- 编译 0 告警；单元测试 **308 项全绿**
- 架构守卫 PASS（core 125 类 / 13 包）
- 文案基线 `2190 ≤ 2190` PASS
- 语言包条目：中英各 74 条，`everyChineseKeyHasAnEnglishTranslation` 覆盖

## 校验

```
45b00ee73ecf505d1411b27e01f49794763e948a841bf73be6e69792824b6ea1  NekoCraftLauncher-2.12.1.exe
e7bda84b2e270adf3c3fff243dde6c8fce4e469fb24bea12aa8d05d61afa9296  NekoCraftLauncher-2.12.1-win64.zip
cbfed4b0adbf610bae91d769ac9af8575b19c9beaeadb768c0e86b5384beda46  NekoCraftLauncher-2.12.1-Features.pdf
08e0fe13109a101348105246329b2084d2a90a1daf01aec6e9717e9b55a132e6  NekoCraftLauncher-2.12.1-Features.docx
41bdc7658bd42ee52d59d81ccd541597b7deac9e600715ab23327227a73cdb89  neko-codesign.cer
7b0822069cd0d9e88a60ecc2d6fa750bf455ba8466d3fcf39e6bd0f4b6a07e06  verify.txt
```
