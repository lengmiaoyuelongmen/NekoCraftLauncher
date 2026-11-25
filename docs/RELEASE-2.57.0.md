# Neko Craft Launcher 2.57.0

> **首帧 3.7s → 3.06s（-0.64 秒）**：找到并落地了真正有效的开关；背包支持拖拽换位；i18n 这轮真的降了 9 条。

## 安装包

| 文件 | 说明 |
|---|---|
| `NekoCraftLauncher-2.57.0.exe` | **安装版**：jpackage/WiX，静默参数 `/quiet` |
| `NekoCraftLauncher-2.57.0-win64.zip` | 免安装版；支持就地更新、回退与备份管理 |
| `NekoCraftLauncher-2.57.0-Features.pdf` / `.docx` | 功能预览（图文版） |
| `SHA256SUMS.txt` / `verify.txt` / `neko-codesign.cer` | 校验与签名材料 |

## 1. 首帧：C1 编译（实测有效）

先排除了 ES2 管线（本机 `-Dprism.order=es2` 直接 `no suitable pipeline found`，只有 D3D 可用），
然后对比 JIT 策略：

| 启动参数 | 首帧 |
|---|---|
| 默认（C1+C2 分层） | 3723 ms |
| **`-XX:TieredStopAtLevel=1`（只用 C1）** | **3082 ms** |

同一台机器、相邻两次运行，**-641 ms**。原因是启动器生命周期短，C2 的分层编译在它跑完前根本来不及收益，
反而抢占启动阶段的 CPU。

落地方式：`package-exe.ps1` 给 jpackage 增加 `--java-options -XX:TieredStopAtLevel=1`，
安装版与免安装版的启动壳都带上。**取舍已写在脚本注释里**：启动器以 UI 与 IO 为主，C2 的长期优化收益很小。

加上骨架化后的最新实测：

```
TIMING config=132ms scene=92ms firstPage=2831ms total=3055ms
```

## 2. 背包：拖拽换位

物品行支持**拖拽到另一行交换槽位**（目标为空则移动过去）：

- 拖起时显示行快照作为拖动影像；
- 放下后写回 NBT（一次读写 + 一次备份），弹提示 `已交换槽位 A ↔ B`；
- 空槽位拖拽不改文件。

核心是 `PlayerInventory.moveSlot(from, to)`，配了单测（移动 / 交换 / 空槽位返回 false）。

## 3. i18n：这次真的降了

上一轮说过「先 grep 实际字符串再改」。这轮照做，命中 9 条：

启用 DeepSeek AI、搜索设置提示、反馈附加系统信息、反馈输入提示、优先独显、Java 路径提示、
JVM 参数提示、自动检测 Java、版本隔离。

基线 **2178 → 2169**（本轮 -9，累计 -58）。

## 交付

- **352 项单测全绿**（新增槽位移动用例）；无头冒烟六页全过（C1 模式下同样通过）；编译 0 告警
- exe（安装包 + 便携壳）均已签名并带 DigiCert 时间戳

## 校验

```
e0f71e95f2005ad83f8d9a68577b439afeb461fee4d921a2938b065bdcfae78c  NekoCraftLauncher-2.57.0.exe
ecb89a11d72e82eaf6700b71da91baf4fbeb8f3717398828d0c2c191f594fd2a  NekoCraftLauncher-2.57.0-win64.zip
8b92f7dff1804a7bb458fdb28bfba37231665d2f79a10db94cfd265bd019244a  NekoCraftLauncher-2.57.0-Features.pdf
fdd36afccf8ff8e280bfef573bf364eb4af83d44ea041933c4c3e74344bd01d2  NekoCraftLauncher-2.57.0-Features.docx
41bdc7658bd42ee52d59d81ccd541597b7deac9e600715ab23327227a73cdb89  neko-codesign.cer
63f7dc646f01ac549242c55bee81822a5e55ff77ed591491633d25f2e1bebdc5  verify.txt
```
