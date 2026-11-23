# Neko Craft Launcher 2.50.0

> 服务器玩家管理可以编辑背包了（真读真写 NBT），顺带预热延后与设置页 i18n。

## 安装包

| 文件 | 说明 |
|---|---|
| `NekoCraftLauncher-2.50.0.exe` | **安装版**：jpackage/WiX，静默参数 `/quiet` |
| `NekoCraftLauncher-2.50.0-win64.zip` | 免安装版；支持就地更新、回退与备份管理 |
| `NekoCraftLauncher-2.50.0-Features.pdf` / `.docx` | 功能预览（图文版） |
| `SHA256SUMS.txt` / `verify.txt` / `neko-codesign.cer` | 校验与签名材料 |

## 1. 编辑玩家背包（新功能）

玩家页每个 OP / 白名单成员后面多了 **背包** 按钮：

- 读 `world\playerdata\<uuid>.dat`（gzip NBT）里的 `Inventory` 列表，按槽位列出 `#槽位 物品 数量`；
- **改数量**：0 = 删除该槽位，1~64 直接写回；
- **新增物品**：填 `minecraft:bread`（不写命名空间会自动补 `minecraft:`）+ 数量，自动放进最小的空槽位；
- **写回前自动备份**：`server-backups\<uuid>.dat-<时间戳>.bak`；
- **其余数据不动**：生命、经验、末影箱等标签原样保留（整棵树读进来再整体写回）。

技术上新增了两个类：

| 类 | 作用 |
|---|---|
| `core.server.Nbt` | 最小 NBT 读写（Compound/List/标量/ByteArray/IntArray，gzip 包装），不引第三方库 |
| `core.server.PlayerInventory` | 定位 dat（uuid 大小写/连字符都认）、读取槽位、按编辑写回、备份、空槽位分配 |

如果玩家还没进过服（没有 dat），会提示「先进一次服务器」，不会凭空建文件。

## 2. 社区预热延后到首帧之后

原来的预热在切到下载页时同步触发，会抢首帧时间。现在延后 600ms：

```java
PauseTransition prewarmDelay = new PauseTransition(Duration.millis(600));
prewarmDelay.setOnFinished(ev -> this.ensureCommunityPrewarm());
```

## 3. i18n：设置页更新区落地

版本号、保存更新源、下载并安装、检查更新、更新源提示等 8 条改走 key；基线 **2202 → 2194**（本轮 -8）。

## 实测与测试

- 新增 `PlayerInventoryNbtTest`（3 条）：NBT gzip 往返、读背包 → 改数量 + 删除 + 新增 → 写回且**生命/经验等标签完好**、uuid 带不带连字符都能定位；
- **346 项单测全绿**；无头冒烟六页全过；基线 2194；
- 首帧本轮实测 **5537 ms**（2.49 为 5279 ms）——这一轮多了背包编辑器与 NBT 类，波动仍在 JVM 启动抖动区间，预热延后要观察下一轮数据。

## 校验

```
d83b4ee852cbad3f8306a280e02bb842951f6bfc9ebfba66e6e081a22f251015  NekoCraftLauncher-2.50.0.exe
fd2d67af6ee7e28de1ebffdc365955861db280ee8a6e11e797dc57b0cbbe3396  NekoCraftLauncher-2.50.0-win64.zip
1d3ec7eaaf6462554cf5c03688241cf92e090b8657aa30dd5df41461661d2f27  NekoCraftLauncher-2.50.0-Features.pdf
a0a08625d8f03f7af3712231202dfc3912e8b60c77ddd65144d0909baf8f6d45  NekoCraftLauncher-2.50.0-Features.docx
41bdc7658bd42ee52d59d81ccd541597b7deac9e600715ab23327227a73cdb89  neko-codesign.cer
3b59d10cc670d3e8669d5a226e4a740ffe4216d6505f9c7f569766983e4c1b06  verify.txt
```
