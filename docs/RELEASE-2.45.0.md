# Neko Craft Launcher 2.45.0

> 免安装版（zip）也能「立即安装」了：就地覆盖 app / runtime / exe，存档与配置不动。

## 安装包

| 文件 | 说明 |
|---|---|
| `NekoCraftLauncher-2.45.0.exe` | **安装版**：jpackage/WiX，静默参数 `/quiet` |
| `NekoCraftLauncher-2.45.0-win64.zip` | 免安装版；解压后**也能自动更新**（本版新增） |
| `NekoCraftLauncher-2.45.0-Features.pdf` / `.docx` | 功能预览（图文版） |
| `SHA256SUMS.txt` / `verify.txt` / `neko-codesign.cer` | 校验与签名材料 |

## 1. 免安装版就地更新

之前只有安装版能自动更新，解压即用的 zip 版点「立即安装」会装到别处去。现在按运行位置自动分流：

| 运行位置 | 更新方式 |
|---|---|
| `%LOCALAPPDATA%\NekoCraftLauncher\…`（安装版） | 下载 `Setup.exe` → `/quiet` 静默安装 |
| 其他目录（解压即用） | 下载 `*-win64.zip` → 解压到临时目录 → 覆盖 `app\`、`runtime\`、`NekoCraftLauncher.exe` |

两种方式都会：等启动器退出 → 安装/覆盖 → 自动重新打开 → 新版启动时确认版本。

**覆盖范围**：只覆盖 zip 里有的文件；`game\`（存档、模组）和 `config.json` 不在 zip 里，
所以更新不会动你的存档和配置。

### 实测证据（真机）

1. 从 `release\NekoCraftLauncher-2.45.0-win64.zip` 解出完整免安装目录；
2. 把 `app\NekoCraftLauncher.cfg` 的版本号改成 `0.0.1-OLD`，并放一个 `game` 占位文件；
3. 跑助手脚本（与正式流程同一套 `Expand-Archive` + `Copy-Item` + `Start-Process`）：
   ```
   APPLY-OK
   app-version 回到 2.44.0   ← 文件确实被覆盖
   game 仍存在               ← 存档目录没被碰
   ```

单测也补了 2 条（脚本内容、安装版/免安装版路径判定），共 **338 项全绿**。

## 2. 下载前显示对应包大小

更新卡片现在按当前形态显示：安装版显示 `Setup.exe` 大小，免安装版显示 `zip` 大小，
不再是「点下去才知道要下多大」。

## 3. 顺带

- `UpdateChecker.Info` 增加 `portableUrl / portableSize`，从 GitHub Release 资产里挑
  `*-win64.zip`；找不到就退回原来的 setup.exe 逻辑；
- 安装助手与免安装助手共用 `pending.json`，所以「重启提示」对两种形态都成立。

## 工程质量

- 编译 0 告警；单元测试 **338 项全绿**（+2）；无头冒烟六页全过；文案基线 2226
- exe（安装包 + 便携壳）均已签名并带 DigiCert 时间戳

## 校验

```
90fe55984320b34d5af735e693718e013dbf670aad2993dfad7b0035f1257892  NekoCraftLauncher-2.45.0.exe
f31a694989d61b0238b047b4a552c8fd628515aa32318f960c3a183fd211c82d  NekoCraftLauncher-2.45.0-win64.zip
b8a3015c0619ee81677d8b9642a6c12bea5f96094369dafc94f44a4b5d4f5f1e  NekoCraftLauncher-2.45.0-Features.pdf
4d92f767c18e7937dfa12d955932cf3b8585233ce6a036989370fe3b73653e78  NekoCraftLauncher-2.45.0-Features.docx
41bdc7658bd42ee52d59d81ccd541597b7deac9e600715ab23327227a73cdb89  neko-codesign.cer
ad370fe091f7f5b5b6d3948c068292de3612c0ff575e5bce14de0053b91d5b86  verify.txt
```
