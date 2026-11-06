# 架构蓝图：对标 PCL2 / HMCL / Prism 的分层重构

> 目标：把「功能能用」的启动器变成「结构站得住」的启动器 —— 核心可单测、UI 可替换、下载与启动可追踪。
> 状态：Phase 1 已完成（2026-11-06）。后续阶段见第 6 节。

## 1. 对标：市面最好启动器的共同做法

| 能力 | PCL2 / PCL-CE | HMCL | Prism Launcher | Neko 之前 | Neko 目标 |
|---|---|---|---|---|---|
| 代码分层 | C# 单程序集，但 Task 链清晰 | **HMCLCore（纯 Java）+ HMCL（JavaFX）** | 核心库 + Qt 前端 | 118 个类同包，无边界 | core / ui 两层 |
| 启动流程 | `LoaderTask` 任务链 | `launch` 包 + 逐步 Task | `LaunchTask` 流水线 | 8620 行主类里 400 行过程式 | 任务链 + `LaunchPlan` |
| 实例模型 | 版本隔离目录 + 实例配置 | 实例级设置文件 | **实例是一等公民**，`instance.cfg` | 全局 config + 一个隔离开关 | 每实例独立配置 |
| 下载 | 多线程 + 镜像切换 + 自动修复 | 多源 + SHA1 校验 | 多源 + 校验 | DownloadManager + aria2 + 部分校验 | 统一引擎，全量校验 |
| 认证 | 统一账号层 | 统一账号层（MS/OAuth/authlib） | 统一账号层 | AuthManager 等 6 个类 | core.auth 独立域 |
| Java 运行时 | 自动匹配/下载 | **自动下载 Java** | 自动匹配 | 扫描 + 手动 | 探测 + 按版本要求匹配 |
| 可测性 | 低 | 中（core 有单测） | 高（C++ 单测） | 4 个测试，仅覆盖工具类 | core 每域单测 |

结论：**「最好的方案」不是换技术栈，而是 HMCL 的分层 + Prism 的实例化 + PCL2 的任务化**。UI 仍是 JavaFX（现有 53 个界面的资产不能丢）。

## 2. 当前结构（Phase 1 + 5a 完成）

```
src/main/java/com/example/demo/
├── Launcher.java            # 进程入口（唯一留在默认包的类）
├── core/                    # 79 个类，0 处 import javafx / 0 处 import ui.*
│   ├── ai/         2   DeepSeekClient, LocalAiAssistant
│   ├── app/        8   AppPaths, AppVersion, LauncherConfig, SingleInstance, ConfigHistory, UpdateChecker, SupportBundle, Bootstrap
│   ├── auth/       7   AuthManager, AccountStore, MicrosoftAuth, AuthlibInjectorManager, SessionStore, SecretStore, LoginCardParser
│   ├── download/   9   DownloadManager, GameFiles, Downloader, Aria2Downloader, Integrity, SourcePinger, CurseForgeClient, IconProxy, ZipUtil
│   ├── game/       6   JavaManager, JavaScanner, LoaderInstaller, PortableExporter, CrashAnalyzer, ImportErrors
│   ├── instance/   2   InstanceFiles, InstanceCloner
│   ├── launch/    13   LaunchPlan, LaunchPlanner, LaunchContext, LaunchTask, LaunchPipeline, ClientJarTask, LibrariesTask, AssetsTask, NativesTask, NativeExtractor, LaunchScriptExporter, LaunchCancelledException, GameCommandSender
│   ├── meta/       2   VersionProfile, VersionManager
│   ├── mod/        5   ModDependencyScanner, ModConflictChecker, ModpackImport, ModpackExporter, ServerPackExporter
│   ├── model/     16   Instance, InstanceDirs, InstanceMeta, InstanceSettings, MinecraftVersion, VersionDetails, VersionManifest, AssetIndex, Library, Modpack, ModpackProject, UserSession, LoginCardData, Favorites, Playtime, LaunchHistory
│   ├── plugin/     3   PluginManager, PluginContext, NekoPlugin
│   └── runtime/    6   HardwareInfo, SystemMetrics, PerformanceOptimizer, PerformanceTuner, MemoryAdvisor, NetworkBoost
└── ui/                      # 53 个类，只能单向依赖 core
    ├── shell/     15   HelloApplication（应用外壳）, Theme, Motion, Dialogs, InAppDialog, Icons, Toast, UiFit, ModuleHeader, BootScreen, PrewarmOverlay, AmbientBackground, LauncherLog, CommandCompleter, HelloController
    ├── page/      11   InstancesPanel, InstanceSettingsPanel, SettingsPanel, ToolsPanel, LogsPanel, WorldsPanel, FirstRunPanel, InstallWindow, InstallProgressPage, ModsPanel, InstanceFileManager
    └── component/ 27   启动页/登录/下载/皮肤/世界等可复用控件与对话框
```

依赖方向只有一条：`ui → core`，`Launcher → ui`。`core` 不知道 UI 的存在。

### 分层规则（`scripts/check-architecture.ps1` 会强制检查）

1. `core` 下任何类 **不许 import javafx.\***、`ui.*` 或入口类。
2. UI 通过 `core` 的公开 API 和回调（`Consumer<String>` 日志、`Runnable` 进度）与核心通信。
3. 网络、磁盘、进程调用集中在 core，便于替换与测试。

## 3. 为什么这样切

- **app**：只回答「程序装在哪、配置存哪、是否单实例」。
- **model**：纯数据 + 目录解析（`InstanceDirs` 就是 Prism 的 instance 目录约定）。
- **meta**：版本清单、版本 JSON、继承链、rules —— 启动器的地基。
- **launch**：把 VersionProfile + 账号 + 配置翻译成一条 java 命令。
- **download**：所有 HTTP 下载都从这里走，天然带镜像/重试/校验。
- **auth**：账号类型（离线 / 微软 / 外置登录）只在这里出现。
- **mod**：模组/整合包的导入导出与依赖检查（下一步把模组扫描也并进来）。
- **game**：Java 运行时、加载器安装、实例打包、崩溃分析。
- **plugin**：插件 ABI。
- **runtime**：硬件探测、性能建议、系统指标，纯计算。


## 4. 进度记录（2026-11-06）

### Phase 1：core / UI 分层

| 指标 | 重构前 | Phase 1 后 |
|---|---|---|
| `com.example.demo` 直接持有的类 | 118 | 58（全是 UI + 入口） |
| `core` 包类数 | 0 | **60** |
| core 中的 JavaFX 引用 | — | **0** |
| `-Xlint:all` 告警 | 0 | **0** |
| 单元测试 | 13 通过 | **13 通过** |
| 冒烟测试（5 页切换 + stderr） | PASS | **PASS** |
| 行为变化 | — | **无**（纯包迁移 + 1 处可见性） |

### Phase 2：启动管线对象化

`HelloApplication` 里 400 行的「拼命令 + 解压 natives + 起进程」拆成三个可测对象：

| 新类 | 职责 | 测试 |
|---|---|---|
| `LaunchPlan` | 不可变的「工作目录 + 参数表」；`append()` 返回新实例、`maskedCommand()` 脱敏、`start()` 起进程 | 5 个 |
| `LaunchPlanner` | 版本 JSON（含 inheritsFrom / rules / 占位符）→ `LaunchPlan` | 2 个 |
| `NativeExtractor` | 原生库解压：只取 dll、按 lwjgl/jna/netty 分目录、已存在跳过 | 3 个 |

验证：

- **真实实例全量**：用 14 个已装实例（1.7.10 → 26.3，含 Forge / Fabric）跑 `LaunchPlanner`，全部生成合法命令（mainClass 在、classpath 非空、无 `${}` 残留）
- **真实启动**：实机点「启动游戏」→ `✅ 游戏进程已启动 (pid …)` → 游戏日志出现 `Setting user`、`Created: 1024x512 textures-atlas`（1.18.2 进到标题界面）
- 单测 13 → **21 通过**；`-Xlint:all` 仍 **0 告警**；冒烟 PASS

### Phase 5a：ui/* 落地（包结构闭环）

目标结构里的 `ui/*` 与 `core/mod` 补齐，`com.example.demo` 只剩入口类：

| 指标 | Phase 2 后 | Phase 5a 后 |
|---|---|---|
| 默认包里的类 | 58 | **1**（只剩 `Launcher`） |
| `core` | 62 类 / 9 包 | **66 类 / 11 包**（新增 `mod`、`plugin`） |
| `ui` | 0 | **53 类 / 3 包**（shell 15、page 11、component 27） |
| 架构守卫 | core 不碰 javafx | 额外禁止 `core → ui.*`、`core → 入口类` |
| 编译 / 单测 / 冒烟 | 0 告警 / 21 / PASS | **0 告警 / 21 / PASS** |

迁移注意点（都已经踩过）：

- 资源路径全部是绝对路径（`/icon.png`、`/com/example/demo/app.css`、`/engine/aria2c.exe`），换包不受影响
- `hello-view.fxml` 里的 `fx:controller` 已同步改成 `com.example.demo.ui.shell.HelloController`
- 插件 ABI（`NekoPlugin`）跟着挪进 `core.plugin`，`plugins/` 目录当前为空，无兼容负担

### Phase 2b：启动准备管线

`ensureInstalled` 里「下客户端 jar → 补依赖库 → 补资源 → 解压 natives」四段过程式代码变成可组合的任务链：

```java
new LaunchPipeline()
    .add(new ClientJarTask(config.getDownloadSource()))
    .add(new LibrariesTask())
    .add(new AssetsTask())
    .add(new NativesTask(config.isIsolation()))
    .run(context);          // 每步前查取消，每步后打「⏱ 名字 xx ms」
```

| 新类 | 职责 |
|---|---|
| `GameFiles`（core.download） | 客户端 jar / 依赖库 / 资源 / 原生库 的增量补齐，纯逻辑、可单测 |
| `LaunchContext` | 版本档案 + 目录 + 日志 + 进度 + 取消信号，UI 与 core 的唯一接缝 |
| `LaunchTask` / `LaunchPipeline` | 任务链：顺序执行、快速失败、逐步计时 |
| 四个 Task | 把上面四件事各包一层，名字直接进日志 |

验证：

- 真实启动 1.12.2，日志出现 `⏱ 客户端 jar 1 ms`、`依赖库就绪：新下载 0，已存在 34`、`资源索引未变化，跳过逐文件校验`、`⏱ 原生库 9 ms`，随后 `✅ 游戏进程已启动 (pid …)`
- 单测 21 → **26 通过**（新增 `LaunchPipelineTest` 5 个：顺序、计时、失败即停、取消）
- `-Xlint:all` 0 告警；冒烟 PASS

### Phase 3a：实例设置跟着实例走

原来三项覆盖（内存 / JVM 参数 / Java 路径）存在全局 `game/instance-settings.json` 大表里，
现在改成 **`versions/<id>/instance.json`**，复制/导出/删除实例时设置跟着走（Prism 的做法）：

```json
{ "memoryMb": 2048, "jvmArgs": "-Dneko.test.instance=1", "isolation": true }
```

- `InstanceSettings` 负责读写与**旧表自动迁移**（第一次读到旧条目就搬到新位置并删掉旧条目）
- 新增 `isolation` 覆盖项：`null` 跟随全局，`true/false` 强制开/关；启动时按 `effectiveIsolation()` 决定实例目录与 natives 目录
- 实例设置页的「启动设置（本实例）」多了「版本隔离：跟随全局 / 开启 / 关闭」下拉
- 实例面板里的目录、模组数、存档数都改按该实例的生效隔离状态计算

验证：

- 单元测试 26 → **31 通过**（新增 `InstanceSettingsTest` 5 个：落盘、清空、旧表迁移、隔离覆盖优先、改内存不丢隔离）
- 真机写入 `{"memoryMb":2048,"jvmArgs":"-Dneko.test.instance=1"}` 后启动 1.12.2，实际命令行确认包含 `-Xmx2048M` 与 `-Dneko.test.instance=1`
- 0 告警；冒烟 PASS

### Phase 4a：校验和（SHA1）落地

`Integrity` 以前只比大小：截断能发现，但「大小恰好一样、内容坏了」永远查不出来。现在：

- `Issue` 带 `reason`：`MISSING / SIZE / SHA1`，明细里直接显示「库文件（SHA1 不符）」
- `Integrity.check(..., verifySha1=true)` 会对「大小相符」的文件再算一遍 SHA1（同一次检查里相同文件只算一次，多个实例共享 libraries 不重复哈希）
- 覆盖范围从「库文件」扩到 客户端 jar、资源索引、抽样资源对象
- `repair()` 补完后按 **大小 + SHA1** 复核，不再出现「补了但没修好还报已补齐」
- 工具页「校验文件」默认走深度模式

真实数据验证（14 个已装实例，全量深度校验 8.4 秒）：

```
1.12.2 / 1.8.9 / 1.7.10 / 1.16.5 / 1.18.2 / 26.3 / fabric / forge…  issues=0
1.20.1 家族 4 个实例                                               issues=1（资源索引 SHA1 不符）
```

抓到一个真问题：`game/assets/indexes/5.json` 大小 413067 与官方完全一致，但 SHA1 是
`8a45c6be…`，官方是 `ada52cc6…`，逐字节比对有 5920 字节不同（从 `minecraft/lang/af_za.json`
的 hash 开始）—— 旧逻辑只比大小，永远发现不了；现在点「校验文件 → 补齐」会重新下载并校验。

单测 31 → **36 通过**（新增 `IntegrityTest` 5 个：完好 / 缺失 / 大小不符 / 同尺寸不同内容
只有深度模式能发现 / 0 字节算缺失）。

### Phase 5b-1：先把主类里的纯逻辑搬走（带测试）

拆页面前，先把 `HelloApplication` 里不依赖 JavaFX 的逻辑抽成可单测的 core 类：

| 新类 | 从主类搬走的内容 | 测试 |
|---|---|---|
| `core.auth.LoginCardParser` | 登录卡片解析（JSON / 文本 / 嵌套 auth）+ 外置登录地址归一化 | 5 个 |
| `core.launch.LaunchScriptExporter` | 导出 `launch-<id>.bat`（复用 `LaunchPlanner`，命令与真实启动完全一致） | 1 个 |
| `core.game.ImportErrors` | 异常链取最内层原因 + 按关键字给可操作建议 | 3 个 |

主类行数（同一口径实测）：**2.8.5 = 8,970 行 → 现在 8,667 行**（-303）。剩下的 8.6k 行里
大头是下载页（社区搜索/版本/安装器）、启动编排、实例管理这三块，需要按页面拆，见下一阶段。

单测 36 → **45 通过**；0 告警；冒烟 PASS。

### Phase 5b-2：实例管理里的文件操作外迁

「克隆实例 / 删除实例」是主类里最容易写错的两段文件操作（递归删除、带过滤复制、改版本 JSON 的 id），
先搬到 core 并补测试：

| 新类 | 职责 | 测试 |
|---|---|---|
| `core.instance.InstanceFiles` | `deleteTree`（递归删、失败返回 false）、`copyTree`（可跳过顶层目录） | 3 个 |
| `core.instance.InstanceCloner` | 克隆实例：自动选 `-copy`/`-copy2`、复制版本、按需带 mods/saves、改写 JSON 里的 id | 3 个 |

主类对应方法缩短为 20 行左右的 UI 壳（只负责刷列表 / Toast / 日志）。主类 **8,667 → 8,609 行**，
单测 **45 → 51 通过**。

## 5. 迁移手法

1. 脚本按「域 → 新包」映射移动文件，改 `package` 行。
2. 反向补 import：给被移动类补上「仍在默认包的类」的 import。
3. 全量符号表修 import：删掉指向已不存在路径的 import，按简单名唯一性补缺失 import。
4. 编译 → 测试 → 冒烟，三步都过才算一个阶段完成。

> 这类机械迁移必须一次跑完再编译，不要边移边改逻辑。

## 6. 后续阶段

| 阶段 | 内容 | 验收 |
|---|---|---|
| ~~**Phase 2**~~ ✅ | 启动管线对象化：`LaunchPlan` + `LaunchPlanner` + `NativeExtractor` 已落地 | 8 个新单测；14 实例全量生成命令；实机进游戏 |
| ~~**Phase 5a**~~ ✅ | `ui/*` + `core/mod` 包结构闭环，入口只剩 `Launcher` | 守卫 PASS；编译/单测/冒烟全绿 |
| ~~**Phase 2b**~~ ✅ | 启动准备管线：`LaunchPipeline` + `LaunchContext` + 4 个 Task，`GameFiles` 承接下载/校验 | 5 个管线单测；实机启动成功 |
| ~~**Phase 3a**~~ ✅ | `InstanceSettings`：`versions/<id>/instance.json` + 旧表迁移 + 隔离覆盖 | 5 个单测；实机命令行带实例内存/JVM 参数 |
| ~~**Phase 4a**~~ ✅ | `Integrity` 支持 SHA1 深度校验 + 修复后复核 | 5 个单测；14 实例全量深度校验 |
| ~~**Phase 5b-1**~~ ✅ | 主类纯逻辑外迁：`LoginCardParser` / `LaunchScriptExporter` / `ImportErrors` | 9 个新单测；主类 -303 行 |
| ~~**Phase 5b-2**~~ ✅ | 实例文件操作外迁：`InstanceFiles` / `InstanceCloner` | 6 个新单测；主类 -58 行 |
| **Phase 2b** | 把 `startGame` 的编排（检查 Java → 校验 → 下载 → 解压 → 起进程）抽成 `LaunchTask` 链 + `LaunchPipeline`，UI 只订阅进度 | 主类再减 ~300 行；断点续跑可测 |
| **Phase 3** | 实例一等公民：`InstanceSettings`（内存 / JVM 参数 / Java 路径 / 是否隔离 / 游戏目录）落到 `versions/<id>/instance.json`；实例设置页读写 | 两个实例用不同内存启动成功 |
| **Phase 4** | 下载引擎统一：`DownloadEngine` 统一入口，所有下载带 SHA1 校验、并发上限、失败重试与镜像回退；`DownloadManager` 退化为其实现 | 强制断网/坏源测试通过 |
| **Phase 5b** | 把 8.5k 行的 `HelloApplication` 拆成 shell（窗口/导航/主题）+ 各页面类，页面可单独实例化 | 主类 ≤ 800 行 |
| **Phase 6** | 可观测性：启动耗时分解、下载统计、崩溃报告归档 | 日志里能看出每一步耗时 |

## 7. 常用命令

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-21.0.11'
.\mvnw.cmd -o -q package                 # 编译 + 单测 + fat jar
powershell -ExecutionPolicy Bypass -File scripts\check-architecture.ps1
powershell -ExecutionPolicy Bypass -File scripts\smoke-test.ps1 -SkipBuild
```

## 8. 铁律（沿用）

- 源码 UTF-8 **无 BOM**；只有 `scripts/smoke-test.ps1` 带 BOM。
- 不用 PowerShell `Get-Content`/`Set-Content` 改源码。
- 每次改动后：0 告警 + 单测 + 冒烟，缺一不可。
