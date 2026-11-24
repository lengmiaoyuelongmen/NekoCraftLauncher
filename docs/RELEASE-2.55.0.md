# Neko Craft Launcher 2.55.0

> 骨架化落地：首帧 **4590 ms → 3583 ms（-1.0 秒）**；服务器玩家行可踢出 / 封禁。

## 安装包

| 文件 | 说明 |
|---|---|
| `NekoCraftLauncher-2.55.0.exe` | **安装版**：jpackage/WiX，静默参数 `/quiet` |
| `NekoCraftLauncher-2.55.0-win64.zip` | 免安装版；支持就地更新、回退与备份管理 |
| `NekoCraftLauncher-2.55.0-Features.pdf` / `.docx` | 功能预览（图文版） |
| `SHA256SUMS.txt` / `verify.txt` / `neko-codesign.cer` | 校验与签名材料 |

## 1. 骨架化：首帧实测 -1.0 秒

做法（可用 `-Dmc.noSkeleton=1` 回退对比）：

1. 启动页先用两个占位块（300×520 / 520×520）搭出左右两列；
2. 首帧画完后（一次性 `AnimationTimer` 的第一次 `handle`），再构建真实的账户卡与控制台区块；
3. 构建完成后替换占位块内容。

| 指标 | 骨架化前 | 骨架化后 |
|---|---|---|
| 首帧（真渲染） | 4590 ms | **3583 ms** |
| UI 构建段（rootBuilt） | 2235 ms | 1975 ms |
| Scene | 121 ms | 128 ms |

收益与上一轮量化预测（0.8~1.3 秒）吻合。

### 过程中踩的两个坑（都已修）

- **`logArea` NPE**：准备阶段会往日志写东西，但它在右列里构建 → 骨架分支现在先单独建好日志区；
- **`versionComboBox` NPE**：版本列表异步加载完成时会填充下拉框，此刻它还没建 → 5 处使用点全部加了空值保护，构建完成后再触发刷新。

截图确认：骨架填充后左右两列内容与之前完全一致（账户卡、启动概览、历史、当前实例、快捷操作、控制台）。

## 2. 服务器玩家行：踢出 / 封禁

白名单 / OP 每一行新增 **踢出**、**封禁** 两个按钮：

- 直接往运行中的服务端控制台发 `kick <名字>` / `ban <名字>`；
- 服务器没在运行、发送失败都有明确提示；
- 按实例 id 反查配置目录匹配到正确的服务器实例；只有一个在跑时直接用它（路径写法差异也能命中）。

## 3. 关于 i18n

本轮没有新增 key 落地（精力在骨架化与功能上），基线维持 **2178**，不虚报。

## 交付

- **350 项单测全绿**；无头冒烟六页全过（含骨架路径）；编译 0 告警
- exe（安装包 + 便携壳）均已签名并带 DigiCert 时间戳

## 校验

```
a92335ea1967e7301885e987d5f67a906671fc9006627221f59a7feb8b243d04  NekoCraftLauncher-2.55.0.exe
e4e38745429adfe94e2dc16ee837ad1d1ece509dbb86fe0f29b6435cd30387a3  NekoCraftLauncher-2.55.0-win64.zip
fd9d77966757db23455a85566283ecf12feab9e0e1890cea32376d5343cd46b3  NekoCraftLauncher-2.55.0-Features.pdf
9cf211cf3f101bb02b66d347b3e39ead9a6ec92e26a404ad86430e13c5fe281c  NekoCraftLauncher-2.55.0-Features.docx
41bdc7658bd42ee52d59d81ccd541597b7deac9e600715ab23327227a73cdb89  neko-codesign.cer
742a7c22f25695ae7e6504165c844bf2f7609a1885ef0a12a8c9aa57ab7ee6ce  verify.txt
```
