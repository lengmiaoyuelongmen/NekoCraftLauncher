# Neko Craft Launcher 2.13.0

> 收口版：把前几轮留下的两处缺口补完 —— 插件页拿不到插件数量、CHANGELOG 断档四个月；
> 并首次跑通完整 CI 链（构建 → 单测 → 架构守卫 → 文案基线 → 冒烟 → 视觉基线）。

## 安装包

| 文件 | 说明 |
|---|---|
| `NekoCraftLauncher-2.13.0.exe` | **安装版**：双击安装，可选目录，开始菜单 + 桌面快捷方式（按用户安装） |
| `NekoCraftLauncher-2.13.0-win64.zip` | 免安装版，解压双击 `NekoCraftLauncher.exe` |
| `NekoCraftLauncher-2.13.0-Features.pdf` / `.docx` | 功能预览（图文版） |
| `SHA256SUMS.txt` | 全部产物 SHA256，`sha256sum -c` 可直接校验 |
| `verify.txt` / `neko-codesign.cer` | 签名报告与自签公钥 |

## 1. 插件页真正显示插件数量

`PluginManager.loadedPlugins()` 之前是死 API（定义了、赋值了，界面零引用）。现在「工具 → 插件」
的状态行直接读它：

```
已加载 1 个插件，2 条命令已进入 Ctrl+K 面板。
```

- 没有插件时给的是引导文案：「还没有加载任何插件：把 jar 放进下面的目录后点「重新加载」。」
- 点「重新加载」后同步刷新，并弹出 `插件加载完成：1 个插件 / 2 条命令`
- 顺手修正卡片里写错的接口全名：`com.example.demo.NekoPlugin` → `com.example.demo.core.plugin.NekoPlugin`

## 2. CHANGELOG 补齐 2.9 → 2.13

`docs/CHANGELOG.md` 之前停在 2.8.1（2026-11-03），现在补齐 2.9.0 / 2.9.1 / 2.10.0 / 2.11.0 /
2.12.0 / 2.12.1 / 2.13.0 七条，每条都指向对应的 `docs/RELEASE-*.md`。

## 3. 首次跑通完整 CI

| 步骤 | 结果 |
|---|---|
| 构建 + 单测 | PASS（构建 0 告警 + 308 单测） |
| 架构守卫 | PASS（core 125 类 / 13 包，0 处 javafx/ui/入口依赖） |
| 界面文案基线 | PASS（2185 ≤ 2185） |
| 冒烟（页面 / 安装器 / 体检 / 悬停 / 键盘） | PASS（5 页切换 · 悬停差异 2519 · 体检弹窗 · 安装页面板 · 剪贴板校验 · 四角圆角） |
| 视觉基线夜间比对 | PASS（1496 采样点，差异 0.80% < 2%） |

## 4. 已知边界（不变）

- 正文仍是中文：欠账 2190 条以内，`build/i18n-report.csv` 可逐条翻，守卫只降不升
- 代码签名是自签名：要消 SmartScreen 提示需换商业 CA 证书（`-PfxPath/-PfxPassword`）
- Xbox / Minecraft 官方只有单主机：镜像靠 `-Dmc.auth.*Url=` 自建

## 校验

```
aa6f71c48e9c934d75817f2c8027ac28db3edd845d66a0235a8ea56b9b913f5e  NekoCraftLauncher-2.13.0.exe
5923e03089f77a0aade2564f2cf3372a7aafbc0367ceabeb53e9ddfa9c495d4b  NekoCraftLauncher-2.13.0-win64.zip
37d96ca7f6759efc1370be2680afc8b53c1cf8ad318c2bb3a457e852f77871da  NekoCraftLauncher-2.13.0-Features.pdf
3cf435e579313bece2924d107b0a53ea1e5343eb4cd256fdb9632c70b4b31164  NekoCraftLauncher-2.13.0-Features.docx
41bdc7658bd42ee52d59d81ccd541597b7deac9e600715ab23327227a73cdb89  neko-codesign.cer
b2c468024fd4f3dfbecbff6f7e24e16522661dbfa9fe9dd71eea7033992bcc20  verify.txt
```
