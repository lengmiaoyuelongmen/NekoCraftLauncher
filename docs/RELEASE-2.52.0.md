# Neko Craft Launcher 2.52.0

> 首帧计时口径修正（发现之前的数字虚高）、物品可改自定义名与耐久、设置页继续入 i18n。

## 安装包

| 文件 | 说明 |
|---|---|
| `NekoCraftLauncher-2.52.0.exe` | **安装版**：jpackage/WiX，静默参数 `/quiet` |
| `NekoCraftLauncher-2.52.0-win64.zip` | 免安装版；支持就地更新、回退与备份管理 |
| `NekoCraftLauncher-2.52.0-Features.pdf` / `.docx` | 功能预览（图文版） |
| `SHA256SUMS.txt` / `verify.txt` / `neko-codesign.cer` | 校验与签名材料 |

## 1. 首帧计时口径修正（重要）

上一版用 `Platform.runLater` 打点，它只代表**事件队列排到了**，不代表已经画出来，所以数字虚高。
这版改用 `AnimationTimer` 的第一次 `handle()` —— 真正渲染完第一帧的时刻：

| 口径 | 数字 |
|---|---|
| 旧（runLater） | 5164 ms |
| 新（AnimationTimer，真首帧） | **4544 ms** |

修正后的拆解（本机实测）：

```
TIMING startEnter=1283ms rootBuilt=2285ms sceneMs=119
TIMING config=140ms scene=119ms firstPage=4401ms total=4660ms
```

- 配置 140 ms；JVM+JavaFX 到 `start()` ≈1283 ms；UI 构建 ≈1002 ms；Scene 119 ms；
- **首屏渲染 ≈2.19 秒**，仍是最大头。

我顺手排除了两个常见猜测：

- **CSS 不是瓶颈**：`app.css` 只有 16.7 KB、3 处 DropShadow、3 处渐变，解析量微不足道；
- **不是软件渲染**：`-Dprism.verbose=true` 显示 `D3DPipeline`（硬件 D3D），所以不是掉到 sw 管线。

结论：剩下的 2.19 秒来自**首屏节点树的一次性绘制**（大量文本 shaping + 卡片合成），
下一步需要 JFR / JavaFX pulse 日志定位具体节点，而不是继续盲改。

## 2. 物品编辑：自定义名 + 耐久

背包「编辑物品」弹窗新增一行：

- **自定义名称**：写入 `tag.display.Name`（1.13+ 的 JSON 文本格式，自动转义引号），留空即清除；
- **已损耗 / 上限**：写根标签 `Damage` 与 `tag.Damage`，0 表示不写（避免给不可损坏物品加错标签）；
- 仍然是**先备份再写**，其它标签（附魔、耐久外的自定义数据）原样保留。

新增测试第 5 条：写名字+耐久 → 读回一致 → 清空后物品仍在且字段归零。

## 3. i18n

设置页「Java / 管理」两组：Java 分组标题、下载源、扫描本机、模组分组标题、下载策略、完整性抽样。
基线 **2185 → 2181**（本轮 -4，累计 -46）。

## 交付

- **348 项单测全绿**；无头冒烟六页全过；编译 0 告警
- exe（安装包 + 便携壳）均已签名并带 DigiCert 时间戳

## 校验

```
03ff0df980179c5a35d3681e805c7a7cef1a2c36d338158f1b3cae9f0f7d0a44  NekoCraftLauncher-2.52.0.exe
10ddb84d973fc93da97a2fe12b1bafb09e1efc53cfbe724317107f6c515be8a7  NekoCraftLauncher-2.52.0-win64.zip
1a3808dc1cf665df210ea91c9ce52e741e51427b3b46ecf058f8e2ea3cf81089  NekoCraftLauncher-2.52.0-Features.pdf
bd282f619dca428caafebc08513e9ba184f66a7946a5b89dbbd5567275fe1e58  NekoCraftLauncher-2.52.0-Features.docx
41bdc7658bd42ee52d59d81ccd541597b7deac9e600715ab23327227a73cdb89  neko-codesign.cer
a03dbe980878782daf4bf2cee375c0e61e55877861a408960833d4f70f844e8e  verify.txt
```
