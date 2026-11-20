# Neko Craft Launcher 2.17.0

> 三件事一起收口：把一直没生效的环境光层删掉、把柔光强度做成设置项、顺手把浅色主题的
> 新画布组件再对一遍对比度。

## 安装包

| 文件 | 说明 |
|---|---|
| `NekoCraftLauncher-2.17.0.exe` | **安装版**：双击安装，可选目录，开始菜单 + 桌面快捷方式（按用户安装） |
| `NekoCraftLauncher-2.17.0-win64.zip` | 免安装版，解压双击 `NekoCraftLauncher.exe` |
| `NekoCraftLauncher-2.17.0-Features.pdf` / `.docx` | 功能预览（图文版） |
| `SHA256SUMS.txt` / `verify.txt` / `neko-codesign.cer` | 校验与签名材料 |

## 1. 删掉「一直没生效」的环境光层

`AmbientBackground` 画在页面**下面**，页面背景不透明 —— 鼠标从左上移到右下，画面平均差异
**0.01**（等于没动）。这轮直接下线这个类，氛围光统一交给页面之上的 `MouseSheen`：

- 删除 `AmbientBackground.java` 与全部引用（含图标化/暂停/恢复三段逻辑）；
- 游戏运行时暂停的开关改为作用于 `MouseSheen`，行为保持一致；
- 少一个 2fps 的常驻 Canvas，顺带省一点电。

## 2. 柔光强度可调（设置 → 个性化）

新增「鼠标柔光」滑杆：`0 – 1.5`，默认 `0.8`，`0` 即关闭。保存后**立即生效**
（`MouseSheen.setGlobalStrength`），并持久化到 `config.sheenStrength`。

实测三档（13015 个采样点，鼠标位移后的平均差异）：

| 强度 | 平均差异 | 峰值 |
|---|---|---|
| 0（关闭） | 0 | 0 |
| 0.8（默认） | 6.13 | 54 |
| 1.5 | **11.13** | **88** |

## 3. 浅色主题对比度复查

- `RingProgress` 上一版已改成主题感知（浅色 `#0d9488` 轨道/弧线、`#0f766e` 数字）；
- 本轮把设置页在浅色下重新截图核对：卡片、滑杆、按钮、说明文字无「浅底浅字」；
- 柔光在浅色下是 alpha 0.14 × 强度，属于氛围级，不盖内容。

## 工程质量

- 编译 0 告警；单元测试 **308 项全绿**
- 新增配置项带默认值与范围钳制（`sheenStrength` 0–1.5），老配置文件缺字段时自动取默认
- 动效仍受 `config.animations` 总开关控制；柔光另有独立强度

## 校验

```
25c238895a8419af5248d820810c646c7f577218ef708146c9bc0a6580a0fc63  NekoCraftLauncher-2.17.0.exe
c5cf0e4096991dee058cdc823b8c8cf369bc58b6b450b18a590a64a92e0fda9c  NekoCraftLauncher-2.17.0-win64.zip
593b21663d2bc74b96d727ccf860aa9c68c0d8a107f502ed02c1bcfb985aa798  NekoCraftLauncher-2.17.0-Features.pdf
d10c53b73f8c3176f357b508da5cd3aa0c65f9c6a8ba00b24a0e605895cfbea9  NekoCraftLauncher-2.17.0-Features.docx
41bdc7658bd42ee52d59d81ccd541597b7deac9e600715ab23327227a73cdb89  neko-codesign.cer
82ad561ef4d0ded60836a024e3a4ff4cfe35a1d1cffcd91b3764e0f7c7b99e44  verify.txt
```
