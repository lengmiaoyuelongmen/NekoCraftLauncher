# Neko Craft Launcher 2.48.0

> 三件事一起推进：服务器玩家面板增强、模组列表分批渲染 + 首帧耗时可见、下载页导航文案入 i18n。

## 安装包

| 文件 | 说明 |
|---|---|
| `NekoCraftLauncher-2.48.0.exe` | **安装版**：jpackage/WiX，静默参数 `/quiet` |
| `NekoCraftLauncher-2.48.0-win64.zip` | 免安装版；支持就地更新、回退与备份管理 |
| `NekoCraftLauncher-2.48.0-Features.pdf` / `.docx` | 功能预览（图文版） |
| `SHA256SUMS.txt` / `verify.txt` / `neko-codesign.cer` | 校验与签名材料 |

## 1. 服务器面板：玩家名单更好用

![玩家面板](../../docs/images/preview/58-override-badge.png)

- **批量导入白名单**：一次粘贴多个名字（逗号 / 空格 / 换行都行），自动去重并跳过已存在的，导入完成提示「新增 N 个，跳过 M 个」；
- **自动刷新**：玩家面板可见时每 5 秒重读 `ops.json / whitelist.json / banned-*`，别人改了名单不用手动点「刷新」，离开页面自动停表；
- 名字长度 > 16 的脏数据直接跳过，不会写进 json。

## 2. 性能：模组列表分批渲染 + 首帧可测

- 模组列表原来一次性把所有行建完（200+ 模组会卡住界面几百毫秒），现在**先出分组头 + 每帧 30 行**，界面立刻可用，行陆续补全；
- 新增首帧计时日志：`STARTUP first-frame 5199 ms`（从进程启动算起，含 JVM 与 JavaFX 初始化）。本机实测 5.1~5.2 秒，以后优化有据可依，不用靠感觉。

> 注意：这个数字包含 JVM 启动（约 1.5 秒），不是「启动器自身」的耗时，但作为回归指标够用。

## 3. i18n：下载页导航先落地

下载页的导航分组与七个栏目（游戏 / 社区资源 / 安装包；模组 / 整合包 / 数据包 / 资源包 / 光影包 / 世界 / 收藏夹）全部改走 `I18n.t(key, 中文)`：

- 中文文案基线从 **2227** 降到 **2217**（-10）；
- 新增中英 key 共 20 条（`download.group.*`、`download.nav.*`）；
- 后续页面按同样方式推进即可，基线只会往下走。

## 工程质量

- 编译 0 告警；单元测试 **341 项全绿**；无头冒烟六页全过（`SMOKE DONE ok`）
- exe（安装包 + 便携壳）均已签名并带 DigiCert 时间戳

## 校验

```
b3518d03369a5c66ddb04499b6bca254a3b6e1f101cc6a0d4dfd66f62ce4acb2  NekoCraftLauncher-2.48.0.exe
ed155d9013d86479010b6ed3d73207610437a3da4088980a95b0f8b9648c5f36  NekoCraftLauncher-2.48.0-win64.zip
dd49fd066b4b332239f7f6d5089b145c969f32c9ff52b0f752876112d29ca2d9  NekoCraftLauncher-2.48.0-Features.pdf
aaedfe06a2d24710887db64937d62aa416f1d224dc37a7fc4c68536142fbda4f  NekoCraftLauncher-2.48.0-Features.docx
41bdc7658bd42ee52d59d81ccd541597b7deac9e600715ab23327227a73cdb89  neko-codesign.cer
30f5c1ba54d708efd681c54cbf2960dc483fcd8e4bea56ab7774d8684aac1a17  verify.txt
```
