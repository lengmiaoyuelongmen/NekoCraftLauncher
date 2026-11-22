# Neko Craft Launcher 2.41.0

> 修「点了启动一直没反应」：现在会明确告诉你缺什么 Java、找过哪些目录，并给一键去设置。

## 安装包

| 文件 | 说明 |
|---|---|
| `NekoCraftLauncher-2.41.0.exe` | **安装版**：双击安装，可选目录，开始菜单 + 桌面快捷方式（按用户安装） |
| `NekoCraftLauncher-2.41.0-win64.zip` | 免安装版，解压双击 `NekoCraftLauncher.exe` |
| `NekoCraftLauncher-2.41.0-Features.pdf` / `.docx` | 功能预览（图文版） |
| `SHA256SUMS.txt` / `verify.txt` / `neko-codesign.cer` | 校验与签名材料 |

## 问题现场

用户日志停在：

```
☕ 版本 1.21.11 需要 Java 21，未指定路径，准备 Azul Zulu 高性能运行时…
🔎 查询 Java 21 运行时下载源…
📥 Azul Zulu：下载 zulu21.…-win_x64.zip（约 200MB，请耐心等待）…
（之后没有任何输出，按钮一直是「运行中」）
```

也就是说：准备运行时这一步既没成功、也没报错，界面就一直卡着。

## 这次改了什么

1. **复用本机 JDK 的路径更早生效**，并写清楚用了哪个：
   `✅ 使用本机已安装的 Java 21：C:\Program Files\Java\jdk-21.0.11\bin\java.exe`
   ——本机实测就是这条路径，命中后**一次网络请求都不会发**。
2. **找不到时把检查过的位置全列出来**（新增）：
   ```
   🔎 本机没有 Java 21，已检查 6 个位置
      · C:\Program Files\Java\jdk-21.0.11（Java 21）
      · C:\Program Files\Java\jre1.8.0_231（Java 8）
      · C:\Program Files\Java\latest（没有 bin/java.exe）
   ```
   下次贴日志就能直接判断是「没装」还是「版本对不上」。
3. **下载失败不再只有一行红字**：弹窗写明两条路，并带 **打开 Java 设置** 按钮直接跳过去。
4. 失败提示补上可操作建议：`可以到 设置 → Java 手动指定 java.exe，或安装 JDK 21`。

## 立即能用的绕过办法（旧版本也适用）

设置 → Java → 手动选择：`C:\Program Files\Java\jdk-21.0.11\bin\java.exe`，
然后重新点启动即可，不必等 200 MB 运行时下载。

## 回归测试

新增 `JavaEnsureLocalProbeTest`：本机有 JDK 21 时，`ensureJavaAsync` 必须直接返回它，
且日志里**不能出现任何「下载」字样**。本机实测：

```
PROBE resolved=C:\Program Files\Java\jdk-21.0.11\bin\java.exe
PROBE log ✅ 使用本机已安装的 Java 21：C:\Program Files\Java\jdk-21.0.11\bin\java.exe
```

## 工程质量

- 编译 0 告警；单元测试 **333 项全绿**；文案基线 2226
- 新增中英文案 `java.missing.*` 4 条

## 顺带说明：Actions 现在能自己发版了

`release.yml` 手动触发实测成功（run 45045713426），产物已发布为 `v2.40.0`
（exe + docx + SHA256SUMS）。这次 2.41.0 仍按本地管线发布，包含完整 7 个资产。

## 校验

```
307d8d8a5d5a4a1d20f26f100f22972c661984de6fdce251cf2a3dd7b4374913  NekoCraftLauncher-2.41.0.exe
c6ab9395a6cd39ca7f1b3912d6dbf4780141751033dbcc82929c8a1feb580a5c  NekoCraftLauncher-2.41.0-win64.zip
005392d60ca1afab130afdbac8ed1ca6e505c90e95d492b831b7f4489306f0bc  NekoCraftLauncher-2.41.0-Features.pdf
c4e4e971684551e76e6a3a50641202be0b94e3bacf4a712a8d8e47935e1c7f39  NekoCraftLauncher-2.41.0-Features.docx
41bdc7658bd42ee52d59d81ccd541597b7deac9e600715ab23327227a73cdb89  neko-codesign.cer
840db6a1399f9aa6ba510291d89bbde4b280466918274b9560cfcfe78eea29d6  verify.txt
```
