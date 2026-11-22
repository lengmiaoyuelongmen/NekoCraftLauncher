# Neko Craft Launcher 2.36.0

> 收掉最后两件：实例设置能打包带走、实例指定的 Java 不匹配时给一键修复。

## 安装包

| 文件 | 说明 |
|---|---|
| `NekoCraftLauncher-2.36.0.exe` | **安装版**：双击安装，可选目录，开始菜单 + 桌面快捷方式（按用户安装） |
| `NekoCraftLauncher-2.36.0-win64.zip` | 免安装版，解压双击 `NekoCraftLauncher.exe` |
| `NekoCraftLauncher-2.36.0-Features.pdf` / `.docx` | 功能预览（图文版） |
| `SHA256SUMS.txt` / `verify.txt` / `neko-codesign.cer` | 校验与签名材料 |

## 1. 实例设置包（导出 / 导入）

设置 → 管理 新增「实例设置包」：

- **导出实例设置**：把所有「有实例级覆盖」的实例打成一个 zip
  （`instances/<id>/instance.json` + `manifest.json`，带导出时间和清单）；
- **导入实例设置**：还原到对应实例；本机没有的实例会被跳过并提示数量；
- 只搬 `instance.json`（内存 / JVM / Java / 游戏参数 / 隔离），**不含模组与世界** ——
  所以包很小，适合丢网盘或发给朋友对齐配置。

实现里刻意按 `versions/` 目录扫描，而不是按「版本列表」判断：配置能导就导，
不要求该实例的版本 JSON 完整。

### 测试

新增 2 项单测：

- 导出只包含有覆盖的实例（1 个有、1 个没有 → `exported=1, skipped=1`），删原文件后能原样还原；
- 导入到另一台机器（没有同名实例）→ `exported=0, skipped=1`，不会乱写。

## 2. 实例 Java 不匹配 → 一键改用全局

以前实例单独指定了 Java、版本又不够时，只会弹一句「启动已取消：…」。
现在这种情况单独处理：

> 实例「1.12.2」单独指定了 Java：
> `C:\Program Files\Java\jdk-21.0.11\bin\java.exe`
> 需要 Java 8，实际 21 …
> 可以清除这条覆盖，改用全局 Java 后重试。

按钮：**改用全局 Java 并重试**（清掉该实例的覆盖并直接重新启动）/ 取消。

## 工程质量

- 编译 0 告警；单元测试 **332 项全绿**（+2）；文案基线 2223
- 新增 `core.model.InstanceSettingsBundle`，无新依赖

## 校验

```
77680f30ff87091ca26dda5558345e7d2f180976982c608b7c1612fcd207e55c  NekoCraftLauncher-2.36.0.exe
9f7621b06984ec16941c70e3028017d530cdf7f2347b495b6e8386cbc193e56c  NekoCraftLauncher-2.36.0-win64.zip
3efb9fea6d727d39d29603e26a1d1cc548cd3165320ce285fa86009de987b409  NekoCraftLauncher-2.36.0-Features.pdf
863aca505a9fbf4c3bd5c89b1d3495844c35caaecf0de96475778c077c2b2e8a  NekoCraftLauncher-2.36.0-Features.docx
41bdc7658bd42ee52d59d81ccd541597b7deac9e600715ab23327227a73cdb89  neko-codesign.cer
a6aeefef38c1c1ce1ba71163db93b9f69a442a7ca0699876cd2ab3297fc75210  verify.txt
```
