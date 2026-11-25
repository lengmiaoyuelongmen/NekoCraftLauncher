# Neko Craft Launcher 2.58.0

> 背包加了「复制」与「清空同类」；CDS 归档的尝试有实测数据但**没能落地**（原因写清楚）；GC/共享参数经多轮测试无收益。

## 安装包

| 文件 | 说明 |
|---|---|
| `NekoCraftLauncher-2.58.0.exe` | **安装版**：jpackage/WiX，静默参数 `/quiet` |
| `NekoCraftLauncher-2.58.0-win64.zip` | 免安装版；支持就地更新、回退与备份管理 |
| `NekoCraftLauncher-2.58.0-Features.pdf` / `.docx` | 功能预览（图文版） |
| `SHA256SUMS.txt` / `verify.txt` / `neko-codesign.cer` | 校验与签名材料 |

## 1. 背包：复制与清空同类

- **复制**：把物品复制到最前面的空槽位（含 NBT 标签），背包满时提示「背包已满，复制不了」；
- **清空同类**：一键删掉背包里所有同名物品，返回删除数量；
- 两者都只读写一次、只备份一次。单测覆盖：复制后槽位/数量正确、`removeAll` 计数、以及重复删除返回 0。

## 2. 首帧：三个实验，两个被判无效、一个有数据但落不了地

**先做了严谨对照**（每种配置跑 3 次取 min/avg）：

| 配置 | 3 次结果 | 最小 |
|---|---|---|
| `C1` | 3581 / 3385 / 3235 | 3235 ms |
| `C1 + UseSerialGC` | 3454 / 3462 / 3403 | 3403 ms |
| `C1 + UseSerialGC + Xshare:auto` | 3447 / 3390 / 3483 | 3390 ms |

→ **GC 与共享类参数都没收益**，波动比差异还大。维持当前的 `-XX:TieredStopAtLevel=1` 即可。

**CDS 归档（最有希望的一项）**：用 `-XX:ArchiveClassesAtExit` 生成归档后再启动：

| 配置 | 3 次结果 | 平均 |
|---|---|---|
| C1（无归档） | 3581 / 3385 / 3235 | 3400 ms |
| **C1 + CDS 归档** | 2265 / 2126 / 2221 | **2204 ms** |

**确实能省约 1.2 秒**。但落地时卡住了：jpackage 生成的运行时里**没有 `java.exe`/`javaw.exe`**
（只有 `runtime\bin` 下的 dll 与 server），无法用它生成归档；用构建 JDK 生成的归档与 app 运行时版本不匹配，
`-Xshare:auto` 下会被忽略，等于白搭。

所以这轮**把打包脚本里的 CDS 改动撤回了**，只保留一个有用的开发开关 `-Dmc.exitAfterInit=1`
（打印计时后正常退出，便于做基准与归档）。下一轮的正解是：让 jpackage 阶段用一个带 java 启动器的运行时
（`--runtime-image` 指定未裁剪的 JRE）生成归档，再做 app-image。

最新首帧实测：`TIMING config=148ms scene=99ms firstPage=2966ms total=3213ms`。

## 3. i18n

本轮新增 key 均随功能走（复制/清空/提示），没有单独的老文案迁移；基线维持 **2169**。

## 交付

- **353 项单测全绿**（新增复制/清空用例）；无头冒烟六页全过；编译 0 告警
- exe（安装包 + 便携壳）均已签名并带 DigiCert 时间戳

## 校验

```
aef438b8b6b2ef249439ec7eef51737106b9c6a1adde6846c050bca54509e426  NekoCraftLauncher-2.58.0.exe
b1722d80015ad48e35c97a522834fb989b62f21c6c91a193d0acd450734c77d0  NekoCraftLauncher-2.58.0-win64.zip
deadc60a98c42478c84b7b1ec1db2ca5029af16bbf5a66683f4635e9ccae030b  NekoCraftLauncher-2.58.0-Features.pdf
858f05fa95854d3b07865c47091a16ce88d3087a6c6742d00b8a7ad00f7d6efe  NekoCraftLauncher-2.58.0-Features.docx
41bdc7658bd42ee52d59d81ccd541597b7deac9e600715ab23327227a73cdb89  neko-codesign.cer
ba0c2c3190d9bb172f5511a3e42cf7716f905704b8b28c3bc40ca9d8ed28595f  verify.txt
```
