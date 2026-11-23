# Neko Craft Launcher 2.46.0

> 更新这条链路收口：装前校验哈希、装前自动备份、装坏了能一键回退。

## 安装包

| 文件 | 说明 |
|---|---|
| `NekoCraftLauncher-2.46.0.exe` | **安装版**：jpackage/WiX，静默参数 `/quiet` |
| `NekoCraftLauncher-2.46.0-win64.zip` | 免安装版；支持就地更新与回退 |
| `NekoCraftLauncher-2.46.0-Features.pdf` / `.docx` | 功能预览（图文版） |
| `SHA256SUMS.txt` / `verify.txt` / `neko-codesign.cer` | 校验与签名材料 |

## 1. 装之前先校验 SHA-256

- 从同一个 Release 取 `SHA256SUMS.txt`，按文件名找到期望哈希；
- 下载完成后立刻算实际哈希比对，**不一致就删包并提示「更新包校验失败，请重试」**；
- 拿不到清单（离线/资产缺失）时跳过校验，不会因为校验工具问题挡住更新。

## 2. 覆盖前自动备份，失败自动还原

免安装版更新脚本现在是四步（`apply-update.ps1`）：

```
等 2 秒（让启动器退出）
Save-Old     → 备份 app\ runtime\ NekoCraftLauncher.exe 到 update\backup\app-<时间戳>\
Expand + Copy → 解压新包并覆盖
出错         → Restore-Old 自动换回备份，并往 update-error.log 写原因
```

## 3. 一键回退到上一版

设置 → 软件更新 新增 **回退到上一版**（只在有备份时出现）：

- 显示备份时间，确认后关闭启动器 → 用备份覆盖 → 自动重新打开；
- `game\` 与 `config.json` 不在覆盖范围内，存档与配置不受影响；
- 安装版不显示这个按钮 —— 它由安装程序自己管理版本。

## 踩到并修掉的真 bug

`Copy-Item $src $dst -Recurse` 在 `$dst` 已存在时会把整个目录塞进去（变成 `app\app`），
脚本"执行成功"但文件根本没被替换。回滚脚本和备份恢复都改成
`Copy-Item (Join-Path $src '*') $dst -Recurse -Force`，并给重新启动加了 `try/catch`
（拉不起来不应该让整个脚本判失败）。

新增 `UpdateInstallerRollbackTest`：**真的执行生成的脚本**，断言文件被换回、且没有 `app\app` 嵌套。

## 工程质量

- 编译 0 告警；单元测试 **339 项全绿**（+1，且是真跑脚本）；文案基线 2227
- exe（安装包 + 便携壳）均已签名并带 DigiCert 时间戳

## 校验

```
4911b92e13ea8fadd6c37d743fdf43fccc915f3ca32117588250b9a676363df4  NekoCraftLauncher-2.46.0.exe
891166a46bd264d82808e8bf3817fb58ebe26f2a7cfaddc972707dc7b464e1d3  NekoCraftLauncher-2.46.0-win64.zip
6805f89913a7c500741cbd6faa81005eed664c260eb60d48e3f40fb65cc7e7a9  NekoCraftLauncher-2.46.0-Features.pdf
70c134b7be93120b894c6f39292e6c2754c5dc05b4feb494bf9418144a6b1dce  NekoCraftLauncher-2.46.0-Features.docx
41bdc7658bd42ee52d59d81ccd541597b7deac9e600715ab23327227a73cdb89  neko-codesign.cer
341ef5951d839b13d65140cdc857a4c2cd1c0db4fe9af9ac408a4bd3572851d4  verify.txt
```
