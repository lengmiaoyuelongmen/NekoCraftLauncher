# Neko Craft Launcher 2.9.1

> 这一版是 2.9.0 的收口修复：把「发布出来的安装包自称 2.8.5」这条版本号漂移连根拔掉，
> 并把功能预览整理成带真实截图的图文版（Markdown / Word / PDF 三份）。

## 安装包

| 文件 | 说明 |
|---|---|
| `NekoCraftLauncher-2.9.1.exe` | **安装版**：双击安装，可选目录，自动创建开始菜单与桌面快捷方式（按用户安装，无需管理员） |
| `NekoCraftLauncher-2.9.1-win64.zip` | 免安装版，解压后双击 `NekoCraftLauncher.exe` |
| `NekoCraftLauncher-2.9.1-Features.pdf` | 功能预览（图文版，20 页） |
| `NekoCraftLauncher-2.9.1-Features.docx` | 功能预览（Word 版，可直接改） |

- 系统要求：Windows 10/11 x64，内置 Java 运行时，不需要另外装 Java
- 首次运行若出现 SmartScreen 提示，选「更多信息 → 仍要运行」（构建产物未做代码签名）

## 修复

| 问题 | 根因 | 处理 |
|---|---|---|
| 2.9.0 安装包在「启动器」栏显示 **2.8.5** | `.github/workflows/release.yml` 打补丁的路径写成了 `src/main/java/com/example/demo/AppVersion.java`，真实文件在 `core/app/` 下，替换静默失败 | 路径改正，并加校验：替换后必须能在源码里找到 `CURRENT = "<tag 版本>"`，否则构建直接失败 |
| 版本号只能靠 CI 猜 | 本地打包脚本的 `-AppVersion` 只影响安装包文件名，不影响程序内显示 | `AppVersion.CURRENT` 同步升到 2.9.1，CI 与本地打包都从同一处读取 |

## 文档

- `docs/FEATURES.md` 改成**图文版**：15 张实机截图（浅色 10 张、深色 3 张、悬停前后对照 2 张），
  每个模块一节，表格保留状态标注。
- 新增 `scripts/gen-feature-doc.mjs`：从 `docs/FEATURES.md` 一键生成 Word（最小 OOXML 包，
  不依赖 Office/Python）与打印用 HTML，再由 Edge 无头模式导出 PDF。
- 「已知限制」表里 **深色主题悬停** 从 🧪 改为 ✅，依据是本轮实测：

| 采样点 | 窗口内坐标 | 悬停差异像素 |
|---|---|---|
| 设置页卡片 | 600, 220 | 2654 |
| 设置页左侧导航项 | 60, 397 | 1682 |
| 顶栏「工具」按钮 | 947, 27 | 427 |

## 工程质量

- 编译 0 告警；单元测试 **255 项全绿**；架构守卫通过（core 113 类 / 12 包，0 处 javafx、ui、入口依赖）
- 发布产物由 `package-exe.ps1`（app image + zip）与 `package-installer.ps1`（jpackage → WiX）生成，
  与 2.9.0 完全同一条链路
- 安装包与免安装包都在本机装过一遍并启动确认版本号显示 **2.9.1**

## 校验

```
SHA256  NekoCraftLauncher-2.9.1.exe
D6B05D804FF73F9BD9255E612BEF95866C08BDB2A85E2DC2B04EF9D101D06E89

SHA256  NekoCraftLauncher-2.9.1-win64.zip
F1A650FFAD60271D013A411E730713B22E302ECEF4BB03BBD6A386E601313985

SHA256  NekoCraftLauncher-2.9.1-Features.pdf
A5FFF69AE85091F530C8268314B4115FA24A50102E75302A6DB46831E4748A86

SHA256  NekoCraftLauncher-2.9.1-Features.docx
E950FF3CCB7DD721DC985BB314C64B46C43C3D0A6D019AB0D147551B13BD325E
```
