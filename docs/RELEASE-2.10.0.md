# Neko Craft Launcher 2.10.0

> 这一版把之前挂着的「未完成」清完了：登录线路容错、代码签名、插件示例、骨架屏可观测、
> 界面中英切换（壳层）。共 5 项，都有可复现的验证方式。

## 安装包

| 文件 | 说明 |
|---|---|
| `NekoCraftLauncher-2.10.0.exe` | **安装版**：双击安装，可选目录，自动创建开始菜单与桌面快捷方式（按用户安装，无需管理员） |
| `NekoCraftLauncher-2.10.0-win64.zip` | 免安装版，解压后双击 `NekoCraftLauncher.exe` |
| `NekoCraftLauncher-2.10.0-Features.pdf` | 功能预览（图文版，17 张实机截图） |
| `NekoCraftLauncher-2.10.0-Features.docx` | 功能预览（Word 版） |
| `neko-codesign.cer` | 自签名代码签名证书公钥（想消掉「未知发布者」可导入到「受信任的根证书颁发机构」） |
| `verify.txt` | 本次发布产物的签名校验报告 |

- 系统要求：Windows 10/11 x64，内置 Java 运行时
- exe 与 jar 都已签名（自签名，见下文「代码签名」）

## 1. 正版登录：多接口 + 先探测 + 失败自动切换

原来的设备码 client id `00000000402b5328` 已被微软拒绝（`AADSTS700016`），登录根本发不出设备码。

现在：

- 微软 OAuth 三条公开主机别名（`login.microsoftonline.com` / `login.microsoft.com` /
  `login.windows.net`）并行探测，按实测延迟选最快；
- 连接失败 / 超时 / 5xx / 429 / 应用被拒 → 标记冷却 60 秒并立刻换下一条；
  `authorization_pending`、`XErr` 等业务结果不换线；
- 设备码与轮询锁定同一 client id，主机仍可切换；
- 命令栏 `线路`（别名 `auth` / `line`）随时体检，实测 **10/10 条可用**。

详见 [`docs/AUTH-ENDPOINTS.md`](../docs/AUTH-ENDPOINTS.md)。

## 2. 代码签名

`scripts/sign-release.ps1` 一条命令：生成自签名证书 → `signtool` 签 exe（带 DigiCert 时间戳）
→ `jarsigner` 签 jar → 重打 zip → 校验并输出 `verify.txt`。

- 签名存在、带时间戳、内容改动可被发现；
- `Status` 显示 `UnknownError` 是**信任链问题**（自签根证书不在系统信任库），不是签名损坏；
- 换成商业 CA 证书只需 `-PfxPath / -PfxPassword`，流程不变。

详见 [`docs/SIGNING.md`](../docs/SIGNING.md)。

## 3. 插件：示例可跑通

- 扩展点：`plugins/*.jar` + `ServiceLoader`（`NekoPlugin`）；
- 仓库自带 `plugins-samples/hello-plugin`，`build.ps1` 一条命令产出 `plugins/hello-plugin.jar`；
- 实机验证：日志出现 `🔌 已加载插件: Hello 插件`，命令面板首条即插件命令。

详见 [`docs/PLUGINS.md`](../docs/PLUGINS.md)。

## 4. 骨架屏可观测

加载态骨架行有 **300ms 最短可见时间**（本地 30–80ms 的加载不再一闪而过），收起时打印
`[UI] 骨架屏 <ms>`。实测下载页预加载记录为 `[UI] 骨架屏 1906ms`，冒烟/脚本可直接断言。

## 5. 界面中英切换（壳层）

设置 → 语言选 English，重启后顶栏导航、运行状态、结束/取消按钮变英文；英文更长时自动去掉
导航图标并放宽按钮，避免 `Downloa…` 截断。页面正文仍在逐页翻译（`docs/FEATURES.md` 已标注）。

## 工程质量

- 编译 0 告警；单元测试 **289 项全绿**（新增 22 项登录线路、6 项文案、5 项骨架时序）
- 架构守卫通过：core 128 类，0 处 javafx / ui / 入口依赖
- 发布链路：`mvnw package` → `jarsigner` 签 jar → `package-exe.ps1 -SkipBuild` →
  `package-installer.ps1 -SkipBuild` → `sign-release.ps1 -SkipJar`

## 校验

```
SHA256  NekoCraftLauncher-2.10.0.exe
8C403EF1E9460554AE0326E56B34C5C348F5AC62831FF0D7A663AC4ABB247FE1

SHA256  NekoCraftLauncher-2.10.0-win64.zip
E4CBB8282D760BB8283BAE958D2EB9F34476B9209C3F7E2A48B4C9B1B4BC0344

SHA256  NekoCraftLauncher-2.10.0-Features.pdf
7C133E45D88D6B01E5DA61A4FE828A7509F423503B3AF499EDD6FE58D81ED29E

SHA256  NekoCraftLauncher-2.10.0-Features.docx
5EAB24C242BFF8CEAE1F77140A8C80CB05B1DB51AB4BC73D756E6C9C2901F04B
```
