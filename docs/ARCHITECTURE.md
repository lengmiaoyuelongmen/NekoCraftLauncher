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
├── core/                    # 87 个类，0 处 import javafx / 0 处 import ui.*
│   ├── ai/         2   DeepSeekClient, LocalAiAssistant
│   ├── app/        8   AppPaths, AppVersion, LauncherConfig, SingleInstance, ConfigHistory, UpdateChecker, SupportBundle, Bootstrap
│   ├── auth/       7   AuthManager, AccountStore, MicrosoftAuth, AuthlibInjectorManager, SessionStore, SecretStore, LoginCardParser
│   ├── download/  10   DownloadManager, GameFiles, ModrinthSearch, Downloader, Aria2Downloader, Integrity, SourcePinger, CurseForgeClient, IconProxy, ZipUtil
│   ├── game/       9   JavaManager, JavaRuntimeIndex, JavaRuntimeInstaller, JavaVersionProbe, JavaScanner, LoaderInstaller, PortableExporter, CrashAnalyzer, ImportErrors
│   ├── instance/   2   InstanceFiles, InstanceCloner
│   ├── launch/    14   LaunchPlan, LaunchPlanner, LaunchContext, LaunchTask, LaunchPipeline, ClientJarTask, LibrariesTask, AssetsTask, NativesTask, NativeExtractor, LaunchScriptExporter, LaunchPreflight, LaunchCancelledException, GameCommandSender
│   ├── meta/       2   VersionProfile, VersionManager
│   ├── mod/        5   ModDependencyScanner, ModConflictChecker, ModpackImport, ModpackExporter, ServerPackExporter
│   ├── model/     18   Instance, InstanceDirs, InstanceMeta, InstanceSettings, MinecraftVersion, VersionDetails, VersionManifest, AssetIndex, Library, Modpack, ModpackProject, UserSession, LoginCardData, Favorites, Playtime, LaunchHistory, LanguageConfig, GameDirDetector
│   ├── plugin/     3   PluginManager, PluginContext, NekoPlugin
│   └── runtime/    6   HardwareInfo, SystemMetrics, PerformanceOptimizer, PerformanceTuner, MemoryAdvisor, NetworkBoost
└── ui/                      # 55 个类，只能单向依赖 core
    ├── shell/     17   HelloApplication（应用外壳）, WindowChrome, DownloadBarView, Theme, Motion, Dialogs, InAppDialog, Icons, Toast, UiFit, ModuleHeader, BootScreen, PrewarmOverlay, AmbientBackground, LauncherLog, CommandCompleter, HelloController
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

### Phase 5b-3：窗口 / 托盘生命周期独立

关闭行为、系统托盘、托盘通知从主类搬到 `ui.shell.WindowChrome`，主类通过一个 6 方法的 `Host`
接口提供窗口、配置、对话框层、游戏状态、日志和「真正退出」：

```java
new WindowChrome(new WindowChrome.Host() {
    LauncherConfig config(); Stage stage(); StackPane dialogLayer();
    boolean gameRunning(); void log(String message); void quit();
});
```

主类只保留 `quitLauncher()`（杀游戏进程、移除托盘、释放单例锁、`Platform.exit`），
窗口代码里不再混着业务逻辑。顺带删掉 4 个已不用的 AWT import。

主类 **8,609 → 8,520 行**；面板/守卫/冒烟全绿；新增 `WindowChromeTest` 2 个
（`closeAction=exit` 直接退出、没有窗口时也直接退出而不是留僵尸进程）。

### Java 运行时管理：双下载源 + 校验

原来只会从 Azul 的接口拿包，在国内网络下 Azul 一慢就整条启动链卡死；解压逻辑也依赖「解压出来的
目录名恰好等于 zip 文件名」。

| 之前 | 现在 |
|---|---|
| 只查 Azul | Azul → **Adoptium Temurin 自动回退**（两个源都查，一个失败换另一个） |
| 选包只看「不是 CRaC」 | 还要求是 **.zip**，并**优先非 JavaFX 构建**（体积小一半） |
| 下载后直接解压 | Adoptium 提供 **SHA-256 校验**，不符就删包重来 |
| 依赖解压目录名 | 自动剥掉 JDK 压缩包的顶层目录，并做 zip-slip 防护；解压后必须找到 `bin/java.exe` |

代码拆成三个可测单元：`JavaRuntimeIndex`（查询 + 选包）、`JavaRuntimeInstaller`（解压 + 规范化 +
sha256）、`JavaManager`（编排：本地检测 → 查询 → 下载 → 校验 → 安装 → 失败换源）。

验证：

- 单元测试 53 → **62 通过**（选包规则 5 个、安装/校验 4 个）
- 真实 API 实跑（只查询不下载）：Java 8 / 21 两个大版本都能在 Azul 与 Adoptium 拿到正确的 zip 与 sha256

### Phase 5b-4：启动前的小逻辑全部外迁

主类里还有几段「纯逻辑但埋在 UI 里」的代码，这轮抽出来并补测试：

| 新类 | 内容 | 测试 |
|---|---|---|
| `core.model.LanguageConfig` | 首次启动写 `options.txt` 的 `lang:zh_cn`，已有其它设置不动 | 3 个 |
| `core.model.GameDirDetector` | 从任选目录识别 Minecraft 根（自身 / `.minecraft` / 子目录） | 2 个 |
| `core.game.JavaVersionProbe` | 解析 `java -version`（1.8.0_401 → 8，17.0.2 → 17） | 1 个 |
| `core.launch.LaunchPreflight` | 启动前自检：目录可写、磁盘 ≥300MB，异常信息直接给用户看 | 2 个 |

主类 **8,520 → 8,428 行**；单测 **62 → 70 通过**。实机启动 1.12.2 验证新自检链路：

```
🧪 自检通过：目录可写，磁盘剩余 57 GB，需要 Java 8
✅ 游戏进程已启动
[Game] Created: 1024x512 textures-atlas / OpenAL initialized
```

### Phase 5b-5：启动状态机

主类原来用 4 个布尔量描述启动状态（`launching` / `gameRunning` / `launchCancelled` /
`gameStopRequested`），组合起来有十几种非法状态，而且准备线程和 FX 线程都在读写。

现在换成 `core.launch.LaunchState`：

```
IDLE --beginPrepare()--> PREPARING --markRunning()--> RUNNING --finish()--> IDLE
  ^                          |                           |
  +---------- finish() ------+------------ finish() ------+
```

- `beginPrepare()` 返回 false 表示已经在启动/运行中（重复点击天然被拦）
- `cancel()` 只在 PREPARING 生效（进程起来后「取消」按钮本来就会隐藏）
- `requestStop()` 与阶段解耦，`quitLauncher` / 结束游戏都用它
- 字段 `volatile` + 迁移方法 `synchronized`，后台准备线程与 FX 线程可安全读写

主类里的 24 处布尔量读写全部换成语义清晰的 `launchState.*()` 调用。

**实机验证（完整状态回环）**：

1. 点「启动游戏」→ 1.18.2 启动成功（`游戏进程已启动`）
2. 强杀游戏进程 → `⚠ 游戏异常退出（退出码 -1）` → 崩溃诊断 → 按钮恢复「★ 启动游戏」
3. 再点「启动游戏」→ 新进程起来（重进 1.12.2，688MB），证明状态确实回到 IDLE

新增 `LaunchStateTest` 5 个（单次启动、取消只在准备阶段、运行态、finish 复位、停止请求）；
单测 70 → **75 通过**。主类维持 8,428 行（布尔量换状态机，行数几乎不变，但状态不再散落）。

### Phase 5b-6：下载条组件化 + 原生库纳入校验

| 改动 | 说明 |
|---|---|
| `ui.shell.DownloadBarView` | 文件名 / 进度 / 速度 / 停滞提示的渲染逻辑（原 `updateDownloadBar` 65 行 + 3 个状态字段）收进组件；主类只剩两行委托 |
| `Integrity` 增加原生库检查 | 之前只查 classpath 库、客户端 jar、资源索引；现在 `downloads.classifiers` 里的 natives jar 也查（缺了游戏起不来），失败提示显示「原生库」 |

主类 **8,428 → 8,359 行**；单测 75 → **76 通过**（新增「缺失原生库必须报出来」）。
### Phase 5b-7：搜索协议进 core

下载页的搜索原来把「筛选条件 → URL」和「响应 JSON → 项目列表」写在 UI 方法里（约 90 行），
现在抽成 `core.download.ModrinthSearch`：

| API | 职责 |
|---|---|
| `projectType(uiType)` | 中文类型 → `mod / resourcepack / shader / datapack / modpack` |
| `indexFor(sortLabel)` | 排序下拉 → `relevance / downloads / updated` |
| `buildUrl(...)` | facets 拼装（类型 / 游戏版本 / 加载器）+ URL 编码 |
| `mirrorUrl(url, type)` | 只有模组/整合包走 MCIM 镜像，其余保持官方源 |
| `parse(body, type, existingIds)` | 解析 hits、跳过已存在项目、保留原始条数用于分页 |

验证：

- 单测 76 → **84 通过**（类型映射、facets、镜像规则、去重解析、空结果、坏 JSON）
- **真实 API 实跑**：query=sodium + 1.20.1 + Fabric + 按下载量 → HTTP 200、40 条、解析 40 个项目，
  Top3 是 Sodium / Sodium Extra / Reese's Sodium Options

### Phase 5b-8：列表显示逻辑进 core

三个纯函数从主类搬进 core（都带测试）：

| 新 API | 职责 |
|---|---|
| `core.model.InstanceId.mcVersion(id)` | 从 `1.20.1-forge-47.2.0` / `fabric-loader-0.20.1-26.3` / `quilt-loader-…` / `26.4-snapshot-5` 里还原原版版本号（顺带补上 Quilt，原来不认） |
| `core.mod.ProjectFormat.typeLabel(type)` | `mod / resourcepack / shader / datapack / world` → 中文 |
| `core.mod.ProjectFormat.count(n)` | 下载量显示「万 / 亿」 |

主类 **8,359 → 8,265 行**；单测 84 → **87 通过**。

### Phase 3c：实例级「游戏参数」

之前只能给每个实例配 JVM 参数，游戏参数（`--width`、`--height`、模组用的启动开关）只能全局拼。

| 层 | 改动 |
|---|---|
| `LauncherConfig` | 新增 `gameArgs`（全局默认），`copyForLaunch` 增加带 gameArgs 的重载 |
| `InstanceSettings` | `Settings` 增加 `gameArgs`，写进 `versions/<id>/instance.json`，旧数据缺失时为 null |
| `LaunchPlanner` | 版本参数之后、`--lang` 之前追加自定义游戏参数 |
| 实例设置页 | 「启动设置（本实例）」新增「游戏参数」输入框，支持「恢复全局」一并清空 |

验证：

- 单测 87 → **89 通过**（planner 追加参数、实例设置读写不丢内存）
- **真实数据实跑**：给 `versions/1.12.2/instance.json` 写 `--width 1920 --height 1080`，
  走 `InstanceSettings → copyForLaunch → LaunchPlanner`，命令行尾
  `… --versionType release --width 1920 --height 1080 --lang zh_CN`，同时保住 `-Xmx2048M`；
  验证后已删除测试设置

主类 8,265 → **8,270 行**（多了几行转发，功能在 core）。

### Phase 5c：CurseForge 协议可测化

第二条资源来源（CurseForge / MCIM 镜像）之前完全没有测试，解析逻辑和 HTTP 混在一起。
这轮把纯解析拆出来：

| 新 API | 职责 |
|---|---|
| `CurseForgeClient.parseMirrorPage(root)` | 搜索响应 → Hit 列表 + 总数（logo 缩略图回退、authors 取第一个） |
| `CurseForgeClient.parseMirrorFiles(root)` | 文件列表 → 只要 `.jar`，release > beta > alpha，同类型新文件在前 |
| `matching(project, version, loader)` | 已有方法，补上测试（版本过滤、forge 不吞 neoforge、release 优先） |
| `FileEntry.urls()` / `MirrorFile.urls()` | 补上测试：文件 id 拆 `/1234/567/`、文件名 URL 编码、镜像兜底 |

单测 89 → **94 通过**。

### Phase 4b：下载失败可解释

以前下载失败只会列出 URL，用户看不出是资源没了、被限流还是硬盘满。现在：

| 改动 | 说明 |
|---|---|
| `core.download.DownloadFailure` | HTTP 状态 + 异常类型 → `NOT_FOUND / FORBIDDEN / SERVER / TIMEOUT / NETWORK / CHECKSUM / DISK / UNKNOWN`，并给中文说明 |
| `DownloadManager.Task.failureReason` | 每个任务记录最近一次失败原因（HTTP 码、IO 异常、校验不符） |
| `DownloadManager.Stats.failureReasons` | URL → 原因，随结果一起返回 |
| 日志 | 依赖库补齐失败时输出 `✗ <url>（资源不存在（404）…）` |

验证：

- 单测 94 → **97 通过**（HTTP 404/403/5xx、超时异常、磁盘异常、说明文案）
- **真实网络验证**：正常文件下载成功（version_manifest_v2.json，278 KB）；
  不存在的文件返回 `failed=1`，明细为
  `…neko.json -> 资源不存在（404），可能是版本/文件已被删除`

> 控制服务仍不可用，这轮继续只做不依赖界面的可验证改动。

### Phase 4c：微软登录协议可测化

登录是最容易坏、又最难查的链路（设备码 → 轮询 → Xbox Live → XSTS → Minecraft → 档案），
之前全挤在一个方法里、零测试。现在拆成：

| 层 | 内容 |
|---|---|
| `core.auth.MicrosoftAuthProtocol` | 纯协议：表单体拼装、设备码解析、轮询结果判定、XBL/XSTS/Minecraft 报文、档案与 UUID 格式化、XErr 错误码翻译 |
| `core.auth.MicrosoftAuth` | 只负责 HTTP 与串流程，行为与原来一致 |

单测 97 → **106 通过**（9 个用例：设备码缺字段、`authorization_pending` 与真失败、
XBL 缺 UHS、三个 XErr 码、未购买游戏、UUID 格式化）。

> 没有真实微软账号，整条登录成功无法自动化验证；但每一步的报文与错误分支现在都有测试。

### Phase 4d：zip 解包安全统一

zip-slip（压缩包里带 `../` 条目把文件写到目标目录外）在两处各写了一遍。现在统一成：

- `ZipUtil.unzip(zip, targetDir)` / `unzip(zip, targetDir, filter)`：规范化后必须仍在目标目录内，
  否则跳过；返回实际写出的文件数
- `JavaRuntimeInstaller` 删除私有 `unzip`，改用共享实现
- `ZipUtil.zipDirectory` 改成流式拷贝

单测 106 → **111 通过**：嵌套目录、`../`、`..\\`、`dir/../../` 三种穿越写法、过滤只解 jar、
缺失压缩包报错、打包→解包往返。

### Phase 4e：rules 求值修正

版本 JSON 的 `rules` 是启动器地基：决定一个库/参数在当前平台上是否生效。旧实现有一个真 bug：

```java
// 旧：只有 disallow 规则时，无论是否命中都返回 true
return hasAllowRule ? allowed : true;
```

也就是说 `{"rules":[{"action":"disallow","os":{"name":"windows"}}]}` 这种「明确禁止 Windows」的条目
反而会被**保留**。现在按「最后一条命中的规则生效；一条都没命中时，只有 disallow 规则才保留」：

| 场景 | 旧 | 新 |
|---|---|---|
| 无 rules | 保留 | 保留 |
| allow 命中 | 保留 | 保留 |
| allow 未命中 | 排除 | 排除 |
| disallow 命中 | **保留（bug）** | 排除 |
| 只有 disallow 且未命中 | 保留 | 保留 |

单测 111 → **118 通过**（新增：allow 必须命中、disallow 命中排除、只有 disallow 且未命中保留、
最后命中者优先、库路径解析含 classifier、classpath 与 natives 分流）。

回归验证：用真实安装的 **14 个实例**重跑启动计划，全部 `ok=14 failed=0`，
classpath 长度与修改前一致（例如 1.20.1 仍是 8597 字符），说明没有误删库。

### Phase 4f：修复「同尺寸损坏文件」修不动的问题

回归时用启动器自己的修复逻辑去修之前发现的 1.20.1 损坏资源索引，结果 `repaired=0/1` —— 修了个寂寞。

根因在 `DownloadManager.isUpToDate`：

```java
if (task.size > 0 && file.length() != task.size) return false;
...
return task.size > 0;   // 大小一致就当完整，SHA1 根本没看
```

同尺寸损坏的文件永远被判定为「已存在」，修复流程直接跳过。修复：

- `Task` 新增 `verifySha1` 标记；为 true 时「已存在」判定改为比对 SHA1
- `Integrity.repair` 创建任务时置 true（只有修文件时需要，日常下载仍只比大小，避免几千个资源逐个哈希）
- 单测覆盖：同尺寸但 SHA1 不符 → 默认跳过、`verifySha1=true` 时重新下载

**真实修复验证**：

```
修复前: 1.20.1 家族 4 个实例 issues=1（资源索引 SHA1 不符）
repaired=1/1
修复后: 全部 14 个实例 issues=0
```

单测 118 → **119 通过**；0 告警；冒烟 PASS。


> 顺带记录一个已知局限（用测试固定下来）：指定加载器时，文件名里没带 `fabric/forge/neoforge`
  的通用包会被过滤掉——这是现有启发式策略，改成“未知加载器也放行”需要更细的元数据，留待后续。

> 这一轮没有动 UI 结构：桌面控制服务不可用（`CONTROL_UNAVAILABLE`），无法在真实界面里验证
> 大块 UI 抽取，因此只做可单测的 core 迁移，避免把没验证过的 UI 改动交出去。


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
| ~~**Phase 5b-3**~~ ✅ | 窗口/托盘生命周期外迁：`WindowChrome` + 6 方法 Host | 2 个新单测；主类 -89 行 |
| ~~**Java 运行时**~~ ✅ | 双下载源（Zulu → Adoptium）+ SHA-256 校验 + 健壮解压 | 9 个新单测；真实 API 实跑验证 |
| ~~**Phase 5b-4**~~ ✅ | 启动前逻辑外迁：`LanguageConfig` / `GameDirDetector` / `JavaVersionProbe` / `LaunchPreflight` | 8 个新单测；实机启动验证 |
| ~~**Phase 5b-5**~~ ✅ | 启动状态机 `LaunchState` 取代 4 个布尔量 | 5 个新单测；实机跑通「启动→崩溃→再启动」 |
| ~~**Phase 5b-6**~~ ✅ | `DownloadBarView` 组件化；`Integrity` 纳入原生库 | 1 个新单测；主类 -69 行 |
| **Phase 2b** | 把 `startGame` 的编排（检查 Java → 校验 → 下载 → 解压 → 起进程）抽成 `LaunchTask` 链 + `LaunchPipeline`，UI 只订阅进度 | 主类再减 ~300 行；断点续跑可测 |
| **Phase 3** | 实例一等公民：`InstanceSettings`（内存 / JVM 参数 / Java 路径 / 是否隔离 / 游戏目录）落到 `versions/<id>/instance.json`；实例设置页读写 | 两个实例用不同内存启动成功 |
| **Phase 4** | 下载引擎统一：`DownloadEngine` 统一入口，所有下载带 SHA1 校验、并发上限、失败重试与镜像回退；`DownloadManager` 退化为其实现 | 强制断网/坏源测试通过 |
| **Phase 5b** | 把 8.5k 行的 `HelloApplication` 拆成 shell（窗口/导航/主题）+ 各页面类，页面可单独实例化 | 主类 ≤ 800 行 |
| **Phase 6** | 可观测性：启动耗时分解、下载统计、崩溃报告归档 | 日志里能看出每一步耗时 |

## 附：界面验证（无 MCP 控制时）

MCP 的桌面控制可能不可用（`CONTROL_UNAVAILABLE`），那时 UI 改动不能只靠编译 + 冒烟。
临时脚本 `C:\Users\Administrator\.dsh\tmp\neko-verify\ui.ps1`（未进仓库）用 Win32 直接操作窗口：

```powershell
powershell -File ui.ps1 launch                 # 启动 jar 并等窗口
powershell -File ui.ps1 click 626 30           # 窗口内相对坐标点击（自动置顶）
powershell -File ui.ps1 wheel 520 500 6        # 滚轮
powershell -File ui.ps1 shot out.png           # PrintWindow 截图
powershell -File ui.ps1 stop
```

这一轮用它补验了此前只有单测/冒烟覆盖的功能：

- **关闭对话框**：点 ✕ → 弹出「终止启动器？」（退出启动器 / 最小化到托盘 / 取消 + 记住选择）→ 点取消正常返回
- **实例设置页**：滚到底部确认「启动设置（本实例）」包含 内存覆盖 / JVM 参数 / Java 路径 / **游戏参数** / 版本隔离下拉
- **下载页**：搜索与列表正常（默认整合包分类，显示 40 条 / 共 19187）

并顺手把「加入服务器」的地址解析（域名 / 端口 / IPv6 方括号）抽成
`core.launch.ServerAddress`，加了 5 个用例（单测 119 → **124 通过**）。

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

## 14. 主类拆分现状（2026-11-06 收尾）

### 已搬出主类的页面 / 组件

| 类 | 内容 | 真机验证方式 |
|---|---|---|
| `ui.page.DownloadPage` | 下载中心外壳：分类导航 + 滑动指示器 + 内容区切换 | UIA 点下载/Minecraft/模组/安装包 |
| `ui.page.MinecraftVersionsPage` | Minecraft 版本页：筛选 chip / 最新版本 / 三个折叠分组 | UIA 展开「正式版 (103)」 |
| `ui.page.InstallerPage` | 安装器页：版本下拉 / 实例名 / 7 个加载器手风琴 / 状态与进度 | UIA 点 Forge、NeoForge、Fabric |
| `ui.page.VersionDetailPage` | 版本详情页 + 5 个加载器 chip | UIA 详情 → 返回列表 → Fabric chip |
| `ui.page.ModDetailPage` | 社区项目详情：简介 + 按版本/加载器折叠的文件列表 | UIA 点 Sodium 卡片 |
| `ui.component.AccountManagerDialog` | 账户管理：切换 / 删除 / 添加 | 全屏截图（对话框是独立窗口） |
| `ui.component.DownloadTasksDialog` | 下载任务对话框 | 编译 + 冒烟 |
| `ui.component.McVersionCell` | 版本行单元格（不再反向引用主类） | UIA 展开版本列表 |

主类：**8,970 → 7,292 行**（-1,678，-19%）。

### 还没拆的（按优先级）

1. **启动页装配**（`createLaunchPanel` 及启动概览/控制台）——和 `LaunchState` 状态机耦合最深，拆的时候必须逐项验证「启动 → 取消 → 崩溃 → 重开」。
2. **设置页 / 工具页 / 世界页的装配**——面板组件已在 `ui.component`，主类只剩布局与回调，收益中等。
3. **会话业务**：`handleLogin` / `logout` / `restoreSessionFromRegistry` / `switchAccount`——都是「网络 + 存储 + 界面刷新」混合体，建议先抽一个 `SessionController` 再挪界面。
4. **社区搜索流水线**（`performSearch` / `loadSearchPage` / 预取）——纯业务，适合进 core，但需要网络桩才能测。

### 冒烟脚本这次的两个坑（已修）

- 用 `java.exe` 启动会带一个控制台窗口，抢焦点还挡截图 → 改用 `javaw.exe`。
- 脚本报错时启动的游戏进程仍在跑，外层工具会一直等这个子进程，**看起来像卡死** → 在 `finally` 里无条件收掉启动器，并给抢前台加重试。

## 13. 账户管理对话框独立（Phase 5h）

| 归属 | 内容 |
|---|---|
| `ui.component.AccountManagerDialog` | 已保存账户列表（名字 / 登录类型 / 验证服务器 / 皮肤披风标记）+ 切换 / 删除 / 添加账户 |
| `HelloApplication` | 只传三个回调：切换账户、删除账户、添加账户（退出登录） |

主类 **7,405 → 7,355 行**。删除账户后对话框自己重建列表，不再由调用方递归重开。

**实机验证**（全屏截图，因为对话框是独立窗口，PrintWindow 拍不到）：
点「账户管理」弹窗正常，显示 `Aurora_From_the / 第三方外置登录 · https://littleskin.cn/api/yggdrasil`
与「切换 / 删除 / 添加账户 / 关闭」。

## 12. Minecraft 版本页独立（Phase 5g）

| 归属 | 内容 |
|---|---|
| `ui.page.MinecraftVersionsPage` | 类型筛选 chip、最新版本卡片、正式版/快照/远古版三个可折叠分组（含 ListView 滚轮处理） |
| `ui.component.McVersionCell` | 版本行单元格：图标 + 版本号（含「已安装」）+ 类型徽标 + 详情按钮 |
| `HelloApplication` | 只保留「版本清单」这一份数据与 `openVersionDetail` 回调 |

主类 **7,710 → 7,405 行**。

**实机验证**：下载 → Minecraft：筛选 chip / 最新版本（26.3、26.4-snapshot-7 带发布时间）/
分组数量（正式版 103、快照 758、远古版 61）都正常；点「正式版 (103)」展开后列表渲染正确
（26.3 显示「· 已安装」、26.2、26.1.2 …），说明新的单元格组件与已安装检测都工作。

## 11. 安装器页独立（Phase 5f）

| 归属 | 内容 |
|---|---|
| `ui.page.InstallerPage` | 安装器整页：版本下拉 / 实例名 / 7 个加载器手风琴 / 加载器版本列表 / 状态与进度条 |
| `HelloApplication` | 只剩 `startInstallerFromPanel`（官网分流、启动安装流程）与几个转发方法 |

主类 **8,062 → 7,710 行**（重构起点 8,970）。

### 顺带修掉一个真实 bug：点 Forge 却展开 Fabric

拆完后用 UIA 点「Forge」分类，页面展开的却是 Fabric。原因：

```java
// build() 末尾
Platform.runLater(() -> expand("Fabric"));   // 默认展开
// 用户点分类 → 同步 expand("Forge")
```

同步的 `expand("Forge")` 先执行，随后排队中的 runLater 把它覆盖回 Fabric —— **点任何加载器都会显示 Fabric**。
修复：默认展开也要先判断有没有已经展开的：

```java
Platform.runLater(() -> { if (expandedLoader == null) expand("Fabric"); });
```

**实机验证**：点 Forge → 展开 Forge（66.1.5 / 66.1.4 / 66.1.3 … 真实版本号）；
点 NeoForge → 展开 NeoForge 并显示「正在读取 NeoForge 版本…」；点 Fabric → 展开 Fabric。

## 10. 版本详情页独立（Phase 5e）

| 归属 | 内容 |
|---|---|
| `ui.page.VersionDetailPage` | 版本详情页：图标 / 版本号 / 徽标 / 已安装状态 / 元信息表 / 「直接安装加载器」五个 chip |
| `core.meta.ReleaseTime` | 发布时间格式化（ISO → 本地 `yyyy/MM/dd HH:mm`），列表与详情共用、可单测 |
| `HelloApplication` | 只保留「创建一个 `versionDetailPanel` 容器 + 提供三个回调」 |

拆完主类 **8,152 → 8,062 行**。

**实机验证**（UIA 按名字点击 + 截图）：

- 下载 → Minecraft → 点「详情」：版本详情页正常（26.3 / 正式版 / 发布于 2026/09/15 19:23 / 已安装 / 实例目录 / 5 个加载器 chip）
- 点「返回列表」：回到版本列表，列表里的发布时间格式化正常
- 点详情页的「Fabric」chip：直接进入该版本的安装器页且 Fabric 行展开

## 9. 下载页外壳独立（Phase 5d）

主类的第一块「页面级」代码搬进了 `ui.page.DownloadPage`：

| 归属 | 内容 |
|---|---|
| `DownloadPage` | 左侧分类导航（分组标题 / 滑动指示器 / 高亮）、内容区、切换动画、`alignIndicator()` |
| `HelloApplication` | 只提供四个内容面板（懒创建）与 `onDownloadSelect(text)` 的业务分发 |

拆完后主类 **8,251 → 8,180 行**，下载页的导航结构可以独立阅读。

**实机验证**（ui.ps1 点击 + 截图）：

- 进入下载页：侧栏 / 标题 / 筛选 / 列表与重构前一致
- 点「Minecraft」：版本页正常（最新 26.3 / 正式版 103 / 快照 758 / 远古版 61）
- 点「模组」：搜索框提示变为「搜索 模组…」，列表为 Fabric API / Sodium / Iris Shaders…
- 点安装包分类：安装器页正常打开（加载器版本列表可见）

## 16. 启动页拆分完成（2026-11-07 01:50）

### 启动页现在的组成

| 类 | 职责 |
|---|---|
| `ui.page.LaunchPage` | 两列布局：黄金比例分栏（38.2%，300~420）、滚动包装、模块标题、入场动画 |
| `ui.component.LaunchAccountColumn` | 账户卡 / 登录表单 / 启动按钮（早前拆出） |
| `ui.component.LaunchOverviewPane` | 启动概览：统计、最近一次、记录列表、当前实例、快捷操作 |
| `ui.component.ConsoleContainer` | 右栏容器 + 展开日志 / AI 模式的高度与可见性规则 |
| `ui.component.ConsoleToolbar` | 展开收起、清空、复制、日志目录、快捷键、标语 |
| `ui.component.DownloadStatusBar` | 下载任务行：进度、统计、遥测、取消导入 |
| `ui.component.CommandBar` | 命令输入与 AI 开关（早前拆出） |

主类里启动页只剩「组装 + 业务回调」。

### 启动业务进 core 的部分（都带测试）

| 类 | 内容 |
|---|---|
| `core.launch.LaunchGuards` | 启动拦截原因（游戏在跑优先于正在启动）与文案 |
| `core.launch.IntegrityCachePolicy` | 完整性检查 5 分钟同实例复用规则（含时钟倒退保护） |
| `core.launch.AutoRepair` | 自动补全的结果核算与两条日志文案 |
| `core.launch.GameExit` | 退出判定（主动结束 / 正常 / 崩溃）、两套文案、会话时长 |
| `core.model.LaunchFormat` | 耗时与会话时长格式化 |
| `core.model.LaunchHistory` | 启动记录（新增 `stopped` 标记与 `outcomeLabel`） |

### 这一轮修掉的两个真 bug

1. **启动概览退出后不刷新**：`onExit` 写了记录却没刷新概览面板，数字停留在上一局。
2. **主动结束被当成正常退出**：现在记录里带 `stopped`，概览显示「主动结束」、记录行显示灰色「主动」。

### 为什么 `LaunchPage` 前三次都失败

前三次抽取后页面空白，根因是 `createLaunchPanel` 最后仍然 `return panel;`
（那个旧的空 `VBox`），新页面从未被挂到场景上。窥探日志显示
`launchPanel.getScene() == null`、两列 `opacity=0`（节点不在场景里，动画自然不跑）。
教训：搬迁返回值这类改动，编译通过不代表接上了，必须截图确认。

### 最终回归（2026-11-07 01:50）

```
clean package     0 告警
单测              164 通过，0 失败
冒烟              PASS（5 页截图，逐页等到真实绘制后比较哈希）
架构守卫          PASS（含编码守卫）
14 实例启动计划    ok=14 failed=0
14 实例深度校验    issues=0
主类              8,970 → 7,154 行（-20.3%）
```

### 还没拆的（按收益排序）

1. **社区搜索流水线**（`performSearch` / `loadSearchPage` 系列约 300 行）：网络 + 分页状态 + UI 回调交织，
   分页规则已进 `core.download.SearchSession`，剩下的多是回调编排，拆起来收益中等、风险偏高。
2. **实例管理相关操作**（导入/导出/克隆/删除）：多数已委托给 `core.instance`，主类只做对话框与刷新。
3. **设置/工具/世界页**：已经是「一行 `new` 组件」，没有可拆的装配代码。

## 15. 暂停点（2026-11-06 22:30）

### 状态：可运行、可验证

| 项 | 值 |
|---|---|
| 构建 | `mvnw -o -q clean package` 通过，0 告警 |
| 单测 | **150 通过** |
| 冒烟 | PASS（5 页截图，逐页等到真实绘制） |
| 架构守卫 | PASS（含编码守卫） |
| 主类 | `HelloApplication` 7,240 行（起点 8,970） |

### 本轮新拆的东西

| 类 | 内容 |
|---|---|
| `core.download.SearchSession` | 搜索缓存键 / 翻页 / 跨页去重 / 默认筛选判定 |
| `core.model.LaunchFormat` | 启动概览的耗时与会话时长格式化 |
| `core.auth.SessionState` | 登录类型映射、UUID 解析、展示文案（会话业务收口） |
| `ui.component.AccountManagerDialog` | 账户管理对话框 |

新增测试：`SearchSessionTest`(6)、`LaunchHistoryTest`(7)、`LaunchFormatTest`(3)、`SessionStateTest`(7)。

### 未完成的阻塞项：启动页布局搬迁

尝试过三次（提交 `0c21494` 尝试、`8a4987a` 回滚、后续两次重试），每次都回滚。已知事实：

- `LaunchPage.build(userArea, logContainer)` **不抛异常**；
- 抽取后 `launchPanel` 有 2 个子节点、账户列 11 个、控制台 5 个，但两列 `opacity=0`；
- 去掉 `LaunchPage` 里重复的 `Motion.stagger` 后，内容区**依然空白**；
- 用「等到画面有内容再截图」的方式复验，抓到的是顶栏正常 + 内容区空（299 种颜色），
  排除「没画完」的可能。

下一步建议：把 `createLaunchPanel` 整体复制进 `LaunchPage`（不做删改），逐句比对
`setVgrow / setMaxSize / setPadding / Motion` 的差异；或先用软件渲染
（`-Dprism.order=sw`）排除渲染层干扰。

### 环境注意

- 这台机器冷启动 + 首帧现在很慢（实测窗口出现 30~40 秒），冒烟已按 90 秒等待并做"画出来再截图"；
- 跑 UI 验证前先关掉占 CPU 的程序，结果会可靠得多。

## 9. 新增代码放哪里

| 想做的事 | 放哪 | 注意 |
|---|---|---|
| 解析版本清单 / 版本 JSON / 继承链 / rules | `core/meta` | 只处理 JSON，不碰文件系统 |
| 拼启动命令、加一步启动准备 | `core/launch` | 新步骤实现 `LaunchTask`，注册进 `LaunchPipeline` |
| 下载、校验、镜像、失败分类 | `core/download` | 下载用 `DownloadManager.Task`（带 sha1/size），修复要 `verifySha1=true` |
| 账号 / 微软登录 / 外置认证 | `core/auth` | 协议解析与 HTTP 分开写，便于单测 |
| 模组 / 整合包 / 资源类型 | `core/mod` | 解压走 `ZipUtil`，路径走 `InstanceFiles` |
| 实例目录、实例设置 | `core/model` | 设置写 `versions/<id>/instance.json` |
| Java 运行时 | `core/game` | 新下载源实现「查询 → 选包」两段，参考 `JavaRuntimeIndex` |
| 页面 / 大块面板 | `ui/page` | 只能依赖 core |
| 控件 / 对话框 / 单元格 | `ui/component` | 通过 Host 接口拿宿主能力，别直接引用主类 |
| 窗口、主题、动画、全局对话框 | `ui/shell` | 主类只做装配，具体能力下沉 |

**测试**：放 `src/test/java/com/example/demo/`，命名 `<被测类>Test`。纯解析 / 规则 / 路径计算优先补测试。

**守卫**：`scripts/check-architecture.ps1` 会拦下 `core` 里出现 JavaFX、`ui.*`、入口类的引用。

**每次改动的最低门槛**：`mvnw -o -q package`（0 告警）→ 单测全绿 → `smoke-test.ps1` PASS。

## 17. 收尾阶段：主类只做装配（2026-11-07 17:30）

这一阶段把「主类里还剩的大块」清完，判定标准从「按页面拆」变成「按方法拆」：
凡是能独立成方法、并且有明确入参的，都从 `HelloApplication` 里挪出来。

### 新拆出的类

| 类 | 职责 | 验证方式 |
|---|---|---|
| `ui.component.TopBar` | 品牌区（时钟 / 就绪状态 / 结束游戏 / 取消启动）、导航按钮与滑动指示器、命令面板入口、窗口按钮、拖动与双击复位 | 冒烟 5 页截图，顶栏与重构前一致 |
| `ui.component.CommandPalette` | 命令面板 UI：搜索框、过滤列表、↑↓/Enter/Esc、进场动画、点背景关闭 | Ctrl+K 打开（11 条命令）、Esc 关闭 |
| `ui.component.FavoritesRenderer` | 收藏夹列表：分组折叠、三种排序、搜索、行布局、取消收藏 | 收藏夹空状态实测（有收藏时的行渲染未覆盖） |
| `ui.component.ExportInstanceDialog` | 导出整合包对话框与三条导出路径（mrpack / 便携 / 服务端包） | 实例选择 → 导出整合包，弹窗与取消 |
| `ui.page.ModpackVersionLoader` | 版本列表缓存（10 分钟）、官方/镜像并行竞速、加载页与「重新获取」 | 点开 Fabulously Optimized，列表正常返回 |
| `ui.component.WindowIcon` | 窗口按钮图标枚举（原为主类私有内部枚举） | 随 TopBar |
| `core.app.LauncherCommands` | 命令解析（空 / 斜杠转发 / 各命令 / 内存区间）与帮助文案 | 7 个单测 + 命令栏输入 `help` 实测 |

### 主类内部的方法拆分

| 原方法 | 行数 | 拆后 |
|---|---|---|
| `createLaunchPanel` | 305 | 187（`createAccountColumn` + `createVersionSelector`） |
| `importModpack` | 277 | 5（`createImportProgressPage` + `runImportAsync`） |
| `fetchVersionsAndDownload` | 97 | 1 行委托（`ModpackVersionLoader`） |
| `showVersionLoadingPage` | 44 | 删除（并入 loader） |
| `executeCommand` | 135 | 21 + 4 个小方法（解析在 core） |
| `renderFavoriteResources` | 116 | 6（`FavoritesRenderer`） |
| `start(Stage)` | 409 | 约 275（`installCrtOverlay` / `startBackgroundServices` / `installDropOverlay`） |
| `startProcess` | 164 | 约 45（`handleGameExit` + `pumpGameOutput`） |
| `startGame` | 129 | 约 45（`launchWithEnv`） |
| `downloadChosenVersion` | 117 | 约 105（`pickPrimaryFile`） |

### 结果

- 主类 **8,970 → 5,993 行**（-33%）。
- ≥50 行的方法合计 **3,525 → 约 2,400 行**。
- 单测 **13 → 177**（新增 `LauncherCommandsTest`、`SearchFetcher`/`SearchPages`/`SearchSession`、`LaunchGuards`、`AutoRepair`、`GameExit`、`LaunchHistory`、`IntegrityCachePolicy`、`SessionState` 等）。
- 每个阶段都保持：0 告警、`clean package` 通过、单测全绿、冒烟 PASS、架构守卫 PASS。

### 装配阶段的第二刀（2026-11-07 20:00）

| 原方法 | 行数 | 拆后 |
|---|---|---|
| `createLaunchPanel` | 187 | 约 45（`createAccountColumn` / `createVersionSelector` / `createConsoleSection`） |
| `start(Stage)` | 296 | 138（`buildLayout` / `installCrtOverlay` / `installDropOverlay` / `installDragAndDrop` / `installStageBounds` / `installKeyBindings` / `startBackgroundServices`） |
| `runImportAsync` | 266 | 约 70（`runImportAttempt` + `installPackInto`，状态放 `ImportState`） |
| `startProcess` | 164 | 约 45（`handleGameExit` / `pumpGameOutput`） |
| `startGame` | 129 | 约 45（`launchWithEnv`） |

主类 **6,050 → 5,999 行**；类里已经没有超过 150 行的方法；≥50 行方法合计 **2,074 行**
（开工时 3,525）。

搬 `start(Stage)` 的快捷键处理器后，特意用 Ctrl+K 复验了一次命令面板仍能打开——
这类「搬完看不出问题」的代码，只有真按一次键才知道没断。

### 导入的第二个 bug：复用实例时改掉了名字

导入时会复用「同 MC 版本 + 同加载器」的已有实例（合理，省一次下载），但成功分支仍然
调用 `InstanceMeta.set(gameDir, instanceId, pack.name(), ...)`，把已有实例的显示名
覆盖成整合包名（实测 1.12.2「Multi File Pack」被改成「Neko Import Test」）。

修复：只有新建实例才写名字，复用实例只更新描述。配 `InstanceMetaTest` 5 个用例，
其中一条专门守住「null 名字不覆盖」。

### 快捷键实测（2026-11-07 21:00）

搬运 `installKeyBindings` 后按了一遍能按的快捷键：

| 快捷键 | 结果 |
|---|---|
| Ctrl+K | ✅ 命令面板打开（11 条命令），Esc 关闭 |
| Ctrl+Shift+0 | ✅ 窗口从 1060×830 变为 1060×792 并居中 |
| F11 | ❌ **代码里没有 F11 处理**——`installKeyBindings` 只有 F1–F9 分支，之前汇报里写的「F11 全屏」是我记错了 |
| F5 / F1–F9 | ⚠️ 合成按键（keybd_event）送不到 JavaFX 场景，实测按 F5 页面没切换；这些分支与 Ctrl+K 在同一个 `switch` 里，但没拿到运行时证据 |
| 拖放安装模组 | ⚠️ 无法用脚本模拟 OLE 拖放，未验证 |

结论：`installKeyBindings` 的搬运至少 Ctrl+K、Ctrl+Shift+0 两条路径确认可用；F 键分支需要真实键盘验证。

### 收尾：控制台列再拆一层

`createConsoleSection` 143 → 约 60 行：`createLogPane()`（只读日志框 + 刷新按钮）和
`createOverviewPane()`（概览面板 + 透明滚动容器）。类里最长的方法现在是
`start(Stage)` 138 行。

### 业务判断：按「能不能用」来选，而不是「最新就行」

这一阶段按 Prism / HMCL / PCL 的通行做法补了三类判断，全部放在 core 里配单测。

**1. 版本适配（`core.mod.VersionFit`）**

```java
Loader detectLoader(mainClass, libraryNames)   // NeoForge/Quilt 先判，避免被 Forge/Fabric 库误判
Target(gameVersion, loader)
boolean compatible(candidate, target)
int bestMatch(candidates, target)              // 版本页默认选中它
String fitLabel(candidate, target)             // 「适配当前实例」/「需要 MC 26.3」
```

版本页据此：默认**只列适配当前实例的版本**，标题栏给「显示全部版本」开关；
没有适配版本时全列并在副标题说明「没有适配 1.12.2 · 原版 的版本」。

真机证据：1.12.2 下打开 VeinMiner（无 1.12.2 构建）→ 全列 + 提示；
用离线 harness 喂四个构造版本（26.3-neoforge / 1.20.1-forge / 1.12.2-release /
1.12.2-beta）→ 列表只剩两条 1.12.2（release + beta）。

**2. 启动前兼容性（`core.launch.LaunchCompatibility`）**

| 检查 | 级别 | 处理 |
|---|---|---|
| Java 版本低于要求 | BLOCK | 弹「启动失败」并取消启动 |
| 没有可用 Java / 客户端 jar 缺失 / 版本 JSON 缺失 | BLOCK | 同上 |
| 缺库文件（classpath 统计） | BLOCK | 提示去「工具 → 校验库文件」 |
| Java 版本识别不出 | WARN | 记日志后照常启动 |

真机证据：把 1.12.2 的需求改成 Java 21、同时固定用 Java 8 → 启动被拦，
弹窗文案「启动已取消：该版本需要 Java 21，当前是 Java 8…」。
另有一条正向证据：不指定 Java 路径时启动器会自动拉取所需运行时（日志
`准备 Azul Zulu 高性能运行时… ☕ Java 21（需要 21）`）再启动。

**缺库那条没能真机复现**：把 `patchy-1.3.9.jar` 改名后启动，前置完整性检查已经把文件
补回来了，所以拦截分支只有单测覆盖——这本身是比「拦住」更好的结果。

### 业务判断（续）：加载器版本与模组依赖

**加载器与 MC 版本匹配（`core.meta.LoaderCompatibility`，9 个单测）**

Forge / NeoForge 的版本号里带 MC 前缀（`1.20.1-47.4.10`、`26.3-66.1.0`），装错必然起不来；
Fabric / Quilt 的 loader 与 MC 解耦。安装流程在真正调用 `LoaderInstaller` 之前先校验：
不匹配就弹「加载器版本不匹配」并复位按钮，不再白等一次失败安装。

真机证据：用启动器的安装页/命令触发不匹配的 Forge 版本时会被拦下（本轮以单测覆盖为主，
安装页的真实交互依赖网络拉取加载器列表，网络抖动时未走通）。

**模组依赖预检（`ModDependencyScanner.describeMissing`，新增 2 个单测）**

启动前扫描当前实例 `mods/`，缺前置只在日志里提示、不阻断（与 PCL 一致）：

```
⚠ 模组依赖缺失: cloth-config ← 由 Neko Test Mod 需要
   请在「下载」页装好对应前置再启动
```

真机证据：往 `versions/1.12.2/mods` 放一个声明依赖 `cloth-config` 的假模组后启动，
日志区出现上面两行（`r71-deps.png`）；验证完已删除该假模组。

### 业务判断（续二）：一键补前置

`core.mod.DependencyPick` + `core.mod.DependencyInstaller`：

- `pick(versionsJson, target)` 用 `VersionFit` 的适配排序挑版本，再取 primary 文件
  （没有 primary 标记就取第一个）；返回 `(versionNumber, url, filename, size)`。
- `installOne(modId, modsDir, target, http, log)` 先问官方源、失败换镜像，拿不到适配版本
  就返回「没有适配 X · Y 的版本」，成功则写临时文件再改名，避免半截文件。

界面侧：启动前扫描发现缺前置 → 弹「缺少 N 个前置模组 / id ← 由 谁需要」→
点「自动安装」在后台逐个装，日志逐条输出结果。

真机证据（`r72-deps-dialog.png`、`r73-deps-install.png`）：放一个声明依赖 `jei` 的假模组后启动，
弹出对话框；点「自动安装」后下载了 `jei_1.12.2-4.22.0.1034.jar`（715 KB）到实例 mods 目录，
日志出现「✅ 已安装前置 jei → …」「🧩 前置安装完成：1/1」。验证后假模组与下载的 jar 均已删除。

### 业务判断（续三）：递归补前置与两个小修

`core.mod.DependencyResolver`（6 个单测）把「装完再扫、扫完再装」收敛成一个可测的循环：

```java
Summary resolve(initialIds, fetcher, scanner, maxInstalls, log)
// Summary(installed, failed, skipped, passes, complete) + message()
```

- 新装的前置如果自己还缺前置，下一轮继续装；
- 同一个 id 只尝试一次（失败的不会反复重试成死循环）；
- 单次最多 `maxInstalls`（界面侧传 24），超出只提示不继续；
- 每轮结束重新扫描，用扫描结果判断是否真的补齐。

顺手修的两个缺陷：

1. `DependencyInstaller` 下载失败时临时 `.part` 文件原先会残留，现在 catch 里清掉。
2. 旧的「一键补前置」只装一层，装完也不复核；现在用 resolver，日志会给出
   「安装 N 个，共 M 轮；依赖已补齐 / 仍有缺失」。

真机：用一个依赖 `jei` 的假模组触发，点「自动安装」后 JEI 1.12.2 落盘
（`jei_1.12.2-4.22.0.1034.jar`），行为与上一版一致；递归路径的多轮行为由单测覆盖
（测试里构造了 a→b 的两级链、失败重试、上限与去重）。

### 可靠性扫尾：失败必须看得见（2026-11-08）

这一轮专门清「操作失败却报成功」的路径，都是实际会误导用户的：

| 位置 | 原来 | 现在 |
|---|---|---|
| 批量删模组 / 启用禁用 | 无论删掉几个都弹成功 | `core.mod.BulkDelete` 统计成功/失败，失败用 `Toast.error` 并写明个数 |
| 删除实例 | 目录删了，`instances.json` 条目还在 | 成功后 `InstanceMeta.remove()` 一并清掉 |
| 实例重命名/描述保存 | 写盘失败仍显示「已保存 ✓」 | `InstanceMeta.set` 返回布尔，界面显示「保存失败：磁盘只读或被占用」 |
| 收藏实例 | 写盘失败无提示 | 失败弹错误提示，不重绘成"已收藏" |
| 导入取消/失败 | 会删掉**复用**的实例目录（mods/saves 全丢） | `core.instance.ImportRollback` 只删本次新建的东西（真机复现见下） |
| 导入成功 | 清单文件没下全也说「安装完成」 | 追加「⚠ 整合包声明 N 个文件，实际下载成功 M 个」 |
| 克隆实例 | 中途失败留下半成品目录 | `finally` 里清理，源实例不动 |
| 加载器安装 | 版本与 MC 不匹配也照装 | `core.meta.LoaderCompatibility` 先校验，不匹配直接拦住 |
| 补前置 | 半截 jar 被当成已安装 | `reuseExisting(实际大小, 期望大小)` 不符就重下 |
| 补前置 | 下载失败残留 `.part` | catch 里清理 |

对应单测从 227 增至 **241**（BulkDelete、ImportRollback、InstanceMeta 写入失败、
InstanceCloner 回滚、DependencyInstaller 复用判断等）。

**真机复现（2026-11-08 17:30）**：造了一个声明 `minecraft 1.12.2 + fabric-loader 0.15.11`
的整合包，Fabric 元数据接口对 1.12.2 返回 HTTP 400，两次重试都失败：

```
❌ 第 1 次安装失败: HTTP 400 -> https://meta.fabricmc.net/v2/versions/loader/1.12.2
↩ 复用的实例，保留原目录：1.12.2
❌ 导入失败（已重试 2 次）…
```

修复后 `versions/1.12.2` 仍是 56 个文件、版本目录仍是 14 个；修复前这条路径会把整个
实例目录删掉。这也顺带验证了「导入失败 → 回滚」的日志文案。
### 加载器支持预检（2026-11-08 18:30）

导入/安装整合包前先问一次加载器元数据，确认它到底有没有适配这个 MC 版本：

- 拿不到任何版本 → 直接判定失败，不再让安装器发一遍注定 HTTP 400 的请求；
- 这类失败重试也不会变好，用 `ImportAbortException` 标记为「终态失败」，跳过自动重试。

真机对比（同一个声明 `fabric-loader 0.15.11 + minecraft 1.12.2` 的包）：

| 版本 | 结果 |
|---|---|
| 改动前 | 重试两次，报原始错误 `HTTP 400 → https://meta.fabricmc.net/v2/versions/loader/1.12.2` |
| 改动后 | 一次即失败：`导入失败：Fabric 没有可用于 Minecraft 1.12.2 的版本（该加载器尚未适配这个游戏版本，或元数据服务不可达）` |

两次都验证了同一件事：`versions/1.12.2` 保留 56 个文件、版本目录 14 个不变。

### 安装页的加载器可用性预检（2026-11-08 18:45）

切换「游戏版本」时，安装页会对 Fabric / Forge / NeoForge / Quilt 各查一次元数据：

- 有构建：副标题清空，正常显示；
- 没有构建：副标题直接写「没有支持 26.3 的 NeoForge 构建」并把整行降到 55% 不透明度，
  用户不用点进去等一次注定失败的请求。

真机（Minecraft 26.3）：NeoForge 行显示「没有支持 26.3 的 NeoForge 构建」且置灰，Fabric 行
照常列出 0.20.1 / 0.20.0 / 0.19.5…，Forge 行无异常。

### 一键体检：模组与加载器是否匹配（2026-11-08 19:30）

新增 `core.mod.ModLoaderCheck`：按 jar 里的元数据判断模组家族（`fabric.mod.json` → Fabric、
`quilt.mod.json` → Quilt、`META-INF/neoforge.mods.toml` → NeoForge、`META-INF/mods.toml` /
`mcmod.info` → Forge），再和实例实际加载器比对：

| 实例加载器 | 允许的模组 |
|---|---|
| Fabric | Fabric |
| Quilt | Fabric / Quilt |
| Forge | Forge |
| NeoForge | NeoForge |
| 原版 | 无（装了就是错） |

`.jar.disabled` 不参与判断（用户已经关掉了）。认不出家族的 jar 一律不报，交给完整性检查。

真机：往原版 1.12.2 的 mods 里丢一个 `nekofake-1.0.0.jar`（带 `fabric.mod.json`），体检面板多出一行
「不匹配 · 实例是原版，却装了 1 个模组（原版加载不了模组）：nekofake-1.0.0.jar」。

### 体检三连（2026-11-08 20:25）

1. **Java 行不再是死文案**：读版本 JSON 的 `javaMajorVersion`，再用 `JavaScanner.bestMatch` 找本机最合适的
   Java，结论交给 `core.launch.HealthAdvice.javaLine(要求, 实际)`——没要求 / 缺 Java / 版本过低 / 正常，四种说法。
2. **内存行按建议值判断**：`MemoryAdvisor.suggest(mods)` 给建议，自动分配时直接用建议值，手动设置时用配置值；
   低于建议 75% 才提示「偏低」，差一点不制造焦虑。
3. **导入后跨加载器提示**：整合包落盘后用 `ModLoaderCheck.scan(target/mods, 实例加载器)` 检查，
   不匹配就写日志（同时进安装进度与主日志）：

```
⚠ 整合包里有模组与当前加载器不匹配：实例是原版，却装了 1 个模组（原版加载不了模组）：nekocross-1.0.0.jar
   可在「一键体检」里复查，或换一个对应加载器的整合包
```

真机：体检面板显示「正常 · Java 8（该版本要求 8）」与「正常 · 自动分配 8704 MB（建议 8704 MB，本机 23 GB × 35% = 8522 MB…）」；
把一个带 `fabric.mod.json` 的 jar 打进整合包导入原版 1.12.2，日志出现上面那条警告（重试时会重复，
因为每次尝试都会重新扫描，属于预期）。

### 体检三连之二（2026-11-08 21:00）

1. **内存上限行**：`HealthAdvice.heapLine(最大堆, 本机物理内存)`——超过物理内存判「超出物理内存」，
   超过 80% 判「偏高」，都给出安全上限建议；读不到物理内存时跳过。面板显示
   「正常 · -Xmx 8704 MB（本机 24349 MB）」。
2. **跨加载器检查前移**：扫描从「合并到实例之后」挪到「解包完成、合并之前」，提示语改成
   「仍然可以继续安装；装完可在「一键体检」复查」。日志顺序验证：警告出现在「导入完成」之前。
3. **体检报告导出**：多了「导出体检报告」按钮，写 `game/logs/health-check-<时间>.txt` 并复制到剪贴板，
   Toast 报文件名。报告由 `HealthAdvice.render` 生成，界面和文件是同一份数据。

真机导出的报告：

```
Neko Craft Launcher 体检报告  2026-11-08 20:50:01  实例 1.12.2
────────────────────
[正常] 版本文件存在
[正常] Java 8（该版本要求 8）
[正常] 自动分配 8704 MB（建议 8704 MB，本机 23 GB × 35% = 8522 MB…）
[正常] -Xmx 8704 MB（本机 24349 MB）
[正常] 3 个模组文件
[注意] 有记录  发现 1 条崩溃记录
[正常] 未发现重复 modId
[注意] 不匹配  实例是原版，却装了 1 个模组（原版加载不了模组）：nekocross-1.0.0.jar
```

### 圆角与体检三连之三（2026-11-08 21:20）

**圆角缺陷**：窗口是 `StageStyle.TRANSPARENT`，外壳 `VBox` 用 `-fx-background-radius: 16` 画圆角，但**没有裁剪子节点**——
页面背景图、列表底色会画到圆角外面，四个角出现方角毛刺。修法：给外壳挂一个跟随宽高的
`Rectangle` 裁剪（arc 32），并给拖放遮罩补上 `-fx-background-radius: 16`。

**体检新增磁盘行**：`HealthAdvice.diskLine(可用, 总容量)`，< 512 MB「空间不足」、< 2 GB「偏低」，
面板显示「正常 · 可用 40.3 GB / 共 199.2 GB」。

**报告附启动日志**：导出的 `health-check-*.txt` 末尾追加「最近 N 行启动日志」，真机验证附上了 7 行。

**启动前预检**：`preflight()` 里用 `HealthAdvice.heapLine` 检查最大堆与物理内存的关系，超出时在日志里
先警告一句，再继续启动（不拦，因为多数情况玩家只想知道为什么卡）。

### 圆角自检脚本 + 启动前一次性提示（2026-11-08 22:05）

1. **`scripts/check-corners.ps1`**：截取启动器窗口（含 24px 边距取桌面参考色），沿四条对角线量
   「多远才出现非桌面像素」。方角约 1~2px，16px 圆角约 4~5px（弧线到外框角约 0.29×半径）。
   阈值 3px，四个角全部实测 4px → PASS。这个脚本能真正抓住"裁剪失效"的回归。
2. **导出提示**：体检面板按钮旁显示「导出时附带最近 N 行启动日志」，N = min(50, 日志行数)，
   真机显示 7 行。
3. **启动前一次性提示**：首次点启动时汇总 Java / 内存 / 磁盘三类硬问题，弹「启动前检查」，
   列出每条原因并提供「仍然启动 / 取消」，会话内只弹一次。真机把 1.12.2 的 `javaVersion`
   临时改成 99 复现：弹窗显示「• 只找到 Java 25，该版本需要 Java 99 或更高」，取消后正常停留。

### 圆角续：按键方角描边 + 自检进冒烟（2026-11-08 22:50）

**按键圆角缺陷**：JavaFX 的 `-fx-border-color` 默认画直角，一批内联样式写了
`-fx-background-radius` 却没写 `-fx-border-radius`（全仓 22 处），于是"圆角底 + 方角边框"。
新增 `ui.component.StyleRepair`：遍历场景树，对**有 border-color、没有 border-radius** 的 Region，
按它自己的背景半径补齐 `-fx-border-radius`；每次切页后跑一遍，新创建的按钮也能覆盖。

**圆角自检进冒烟**：`smoke-test.ps1` 现在会调用 `check-corners.ps1`，四角不是圆角就 FAIL。
输出：`PASS: 5 个页面切换完成，stderr 无异常，四角为圆角`。

**启动前检查可关掉**：弹窗加了「以后不再提示」复选框，勾选并点「仍然启动」会写
`config.skipLaunchAdvice`，之后不再弹。

**体检报告日志行数可配置**：`config.healthReportLogLines`（默认 50，0 = 全部），导出时按它截取。

### 圆角收尾：弹窗/Toast 也修、冒烟断言 0 缺失（2026-11-08 23:20）

- `StyleRepair` 现在挂到三处：切页之后、`InAppDialog.show()` 建完弹窗之后、Toast 的 pill 创建之后，
  弹窗和提示条里的按钮也享受同样的描边修复。
- 自检口径收紧：只有「背景是圆角（内联 `-fx-background-radius` 或实际背景半径 > 0）+ 有 border-color
  + 没有 border-radius」才算缺陷；纯直线分隔线不算，避免误报。
- 每次切页后 `System.out.println("圆角描边检查: N 个节点缺失")`，冒烟读 stdout 断言 N 恒为 0：
  `PASS: 5 个页面切换完成，stderr 无异常，四角为圆角，描边自检 0 缺失`。
- 视觉证据：用 UIA 找到「保存」按钮的真实矩形（1385,860 82x35），裁 30×30 放大 8 倍——
  左上是平滑的抗锯齿圆弧，没有方角描边。

### UI 自检统一入口（2026-11-08 23:55）

- 新增 `scripts/check-ui.ps1`：一次跑「窗口四角」+「场景描边」两类检查，可单独调用
  （`-SkipCorners` 只查描边，`-LogPath` 指定启动器 stdout 日志）。
- 四角阈值改成**按 DPI 缩放**：`GetDpiForWindow` 取 DPI，阈值 = max(3, round(2.2×缩放))；
  125% 缩放下 16px 圆角的跨度会变大，固定 3px 会误判。
- 描边自检多了弹窗一行：`InAppDialog` 建完弹窗后输出「弹窗描边检查: N 个节点缺失」。
  真机手测（启动前检查弹窗）确认输出为 0。
- 冒烟改为调用 `check-ui.ps1`，输出：`PASS: 5 个页面切换完成，stderr 无异常，UI 自检通过`。
  若窗口无法置前（被别的窗口盖住），四角检查返回 2 被标记为 SKIP，不让 CI 假失败。

### UI 自检闭环（2026-11-09 00:30）

- 启动器支持 `-Dmc.uiSelfCheck=1`：启动 2.5 秒后自动弹一个 `InAppDialog`，700ms 后自动关闭；
  冒烟带这个参数启动，于是「弹窗描边」每次都有数据（实测 1 次）。
- 描边日志改成同时报修复数和剩余数：`圆角描边修复: N 个，剩余缺失: M 个`；
  冒烟 5 次切页累计修复 5 个、剩余 0。
- `check-ui.ps1` 新增 `-RequireDialog`：要求至少出现一次弹窗自检，冒烟默认开启。
- 四角检查在窗口无法置前时返回 2，冒烟记 SKIP（今天多次出现：冒烟自己占着前台）。

冒烟输出：

```
SKIP: 窗口无法切到前台（被别的窗口盖住），本次不判圆角
页面自检 5 次（累计修复 5 个）、弹窗自检 1 次
PASS: UI 自检通过
PASS: 5 个页面切换完成，stderr 无异常，UI 自检通过
```

### 符号化与集成式顶栏（2026-11-09 01:05）

- **顶栏集成**：徽标 `NEKO`、时钟、运行状态收进同一个胶囊「状态舱」，用 `·` 分隔，
  默认 1060 宽度下也保留（收窄阈值从 1080 调到 980）；状态舱带 `hoverScale(1.02)`。
- **导航符号化**：五个导航按钮保留图标 + 文字（UIA/无障碍仍按名字可找），补了 tooltip 与
  `hoverScale(1.05)`，按下时仍有 `Motion.press`。
- **账户区符号化**：皮肤/账户/体检三个按钮各加图标（image / user / check）、tooltip、hover 放大，
  并给了 `account-tool-*` 的 id 方便自动化定位。
- 冒烟与 UI 自检全绿：页面自检 5 次（累计修复 5 个）、弹窗自检 1 次。

### 侧边栏符号化 + 筛选带集成（2026-11-09 07:10）

- **侧边栏每一项都有自己的图标**（不再是统一的 `▸`）：
  Minecraft=play、模组=layers、整合包=package、数据包=folder、资源包=image、
  光影包=sparkles、世界=globe、收藏夹=star、安装包=tools。
- **交互**：悬停时图标放大到 1.18、文字右移 4px；选中时图标提亮 + 白色加粗文案；
  导航项按序号错落入场（`Motion.reveal`，每项延迟 18ms）。
- **筛选带集成**：原来「游戏版本/加载器」和「来源/标签/排序」分两行，现在是同一个 FlowPane
  的一条筛选带，五个下拉都带符号标签（●/⚙/🌐/✦/⌄），后面紧跟测速、刷新、下载任务三个图标按钮，
  宽度不足自动换行。
- 保留"图标 + 文字"而非纯符号：UIA/无障碍仍按名字可找，类目一眼可读。

### 设置分栏符号化 + 时钟字形修复（2026-11-09 07:20）

- **时钟字形**：顶栏时钟原来优先 `Cascadia Mono`，缺字时回退成奇怪字形（截图里显示成
  `dgy;'u|`）；改为 `Consolas` 优先并加 `-fx-font-smoothing-type: gray`，真机显示正常
  `07:14:57`。
- **设置分栏符号化**：启动=play、Java=package、管理=tools、联机=server、AI=sparkles、
  个性化=image、语言=globe、杂项=layers、软件信息=folder、软件更新=refresh、反馈=edit、
  查看日志=terminal；悬停图标放大 1.18，选中时图标提亮 + 文案加粗。
  （注意：`selectTab` 里原来按 `getChildren().get(1)` 取文字，加了图标后索引要改成 2。）
- **卡片悬停**：设置页每张卡片挂上 `Motion.hoverCard`（轻微抬升），与下载列表卡片一致。

真机：设置页 12 个分栏全部带图标；冒烟 5 页 + 弹窗自检全绿。

### 冒烟补内容断言 + 页面兜底 + 卡片错落（2026-11-09 21:10）

- **冒烟盲区**（上一轮真机崩溃暴露）：标题变了不代表页面建出来了。现在每个页面带一个
  `marker` 文案（下载=下载中心、设置=启动设置、工具=百宝箱、启动=启动游戏），切页后等它出现在
  UIA 树里，12 秒内没出现直接 FAIL。第一次跑就抓到了"启动概览"在日志展开时不存在的误设。
- **switchPage 兜底**：`createPage` 抛异常时记日志、弹 `InAppDialog.error`、退回启动页，
  不再白屏。`createPage` 返回 null 也会记一行。
- **设置卡片错落**：`card()` 打上 `settings-card` 样式类，切换分栏时对页内卡片执行
  `Motion.stagger`（每张延迟 30ms 淡入）。

### 页面标记共享 + 兜底真机验证 + 列表入场（2026-11-09 21:25）

- **标记共享**：页面清单搬到 `scripts/ui-pages.json`，冒烟按 JSON 驱动；新增
  `PageMarkersTest`（单测 255）把每个 marker 拿去主源码里搜，界面文案一改就会失败，避免
  冒烟与界面各写一份导致漂移。
- **兜底真机验证**：加了测试钩子 `-Dmc.failPage=tools`（`createPage` 里主动抛异常）。真机点击
  工具页得到：日志 `❌ 页面「tools」创建失败: java.lang.IllegalStateException: 测试注入的页面创建失败: tools`、
  弹窗「页面「tools」创建失败…」、并自动退回启动页——不再是白屏。
- **下载列表入场**：`ModpackCell` 首次渲染时 `Motion.reveal`（淡入 + 上移），ListView 复用 cell
  后不再重复播放（`revealed` 标记）。

### 模组列表入场 + 测试手册（2026-11-09 22:00）

- **模组列表**：`ModsPanel.refreshLocal` 重建列表后对每行执行 `Motion.stagger`，
  刷新时逐行错落淡入；刷新被判为"内容没变"时直接 return，不会重复播。
- **测试手册**：新增 `docs/TESTING.md`，把构建/单测、冒烟、UI 自检、架构守卫、测试钩子
  （`-Dmc.uiSelfCheck`、`-Dmc.failPage`、`-Dmc.import`）、手工验证配方与已知限制集中记录。
- 动效（入场/悬停）仍是代码级证据：截图延迟约 300ms，比动画时长（约 200ms）还长，
  抓不到中间帧；这一点在 TESTING.md 的「已知限制」里写明了。

### 实例分组入场 + 一键 CI（2026-11-09 22:15）

- **实例列表**：`InstancesPanel.render()` 建完加载器分组后执行 `Motion.stagger`，分组逐组淡入；
  收藏筛选、搜索、展开/收起都会重新入场。
- **一键 CI**：新增 `scripts/ci.ps1`，串起「构建+单测 → 架构守卫 → 冒烟（含 UI 自检）」，
  失败立即退出并打印 `CI FAIL：<原因>`，成功打印 `CI PASS：…`。实测输出：

```
PASS: 5 个页面切换完成，stderr 无异常，UI 自检通过
CI PASS：构建、单测、架构守卫、冒烟、UI 自检全部通过
```

- 动效证据第三次尝试失败：把 `animationSpeed` 调到 0.5（动画延长一倍）后仍拍不到中间帧——
  UIA 点击到截图之间本来就超过 400ms。结论写进 `docs/TESTING.md`：动效要么录屏，要么不做截图断言。

### 动效连拍脚本 + CI 快查（2026-11-09 22:30）

- **`scripts/capture-motion.ps1`**：每 40ms 连拍窗口，第 4 帧用 UIA 触发一次点击，然后逐帧与稳定帧比较。
  实测设置页切换的差异序列 `122 → 98 → 79 → 52 → 24 → 0`（单调收敛），证明过渡过程被拍到了；
  帧图在 `target/motion*/`。局限：差异也可能来自内容加载，需要人工复核。
- **`Motion` 常数化**：交错入场步进/淡入/上浮、单节点 reveal 的偏移与时长都提成常量
  （`STAGGER_STEP_MS` 等），调参不用再满文件找魔数。
- **`scripts/ci.ps1 -SkipSmoke`**：只跑构建+单测+架构守卫，提交前快速自查用。

### 动效证据链闭环（2026-11-09 22:40）

`scripts/capture-motion.ps1` 现在能出三类证据：

| 能力 | 参数 | 实测 |
|---|---|---|
| 切换动画 + 单调性断言 | `-ClickName 设置 -ExpectDecreasing` | 差异序列 20575→0，`PASS: 差异序列单调收敛` |
| 悬停动效 | `-HoverName 设置` | 16 帧有差异（最大 520），帧图可见按钮高亮 |
| 九宫格对比图 | `-ContactSheet` | `target/motion3/contact-sheet.png`（3×3，看出启动页→设置页的过渡） |

九宫格第 5~7 格是过渡中间态，第 8~9 格已稳定——这就是"动画确实存在"的视觉证据。

### 动效回归进 CI + 视觉基线（2026-11-09 22:50）

- **应用内打点**：`Motion.pageIn(node, fromX, onFinished)` 新增完成回调，`switchPage` 借它在日志里写
  「页面过渡完成: <页面>」。冒烟通过 `check-ui.ps1 -MinTransitions <页数>` 断言每个页面都真的跑过过渡，
  动画被删掉或提前中断时 CI 会失败。
- **视觉基线**：动效九宫格已落盘到 `docs/images/motion-contact-sheet.png`（3×3，前四格启动页、
  中间三格过渡、最后两格稳定）：

![动效九宫格](../images/motion-contact-sheet.png)

- **悬停断言**：`capture-motion.ps1 -MinHoverDiff <峰值>`，峰值低于阈值判定悬停动效失效。
  真机两条路径都验证过：`PASS: 悬停差异峰值 683 ≥ 阈值 200`；把阈值提到 900 时
  `FAIL: 悬停差异峰值只有 775，低于阈值 900`（exit 1）。

### 集成式体检面板 + 动效断言收口（2026-11-10 23:15）

- **一键体检改成集成式面板**：不再用系统 `Dialog`，改为 `InAppDialog` + 新的
  `ui.component.HealthCheckView`——顶部一条总览（`共 N 项，M 项需要注意` + 实例名），
  下面每项「符号 + 级别 + 说明」，告警行用暖色描边；底部四个动作
  「关闭 / 打开崩溃目录 / 运行完整性检查 / 导出报告（附 N 行日志）」。
- **过渡耗时进日志**：`页面过渡完成: settings 303ms`，`check-ui.ps1` 断言耗时 0ms 或 >3s 都算失败。
- **每页悬停断言**：`smoke-test.ps1 -HoverCheck` 对四个导航按钮逐个跑
  `capture-motion -HoverName <导航> -MinHoverDiff 150`，失败计入冒烟结果（默认关闭，因为
  需要窗口在前台）。
- **基线图维护说明**：新增 `docs/images/README.md`，写清基线图怎么生成、什么时候该更新。

### 体检面板收尾 + 悬停断言进 CI（2026-11-10 23:30）

- **复制按钮**：体检面板新增「复制」动作，只把报告写进剪贴板、不落盘（日志记一行
  `🩺 体检报告已复制到剪贴板（N 项）`）。
- **告警行可点**：`HealthCheckView` 的告警行带 `›` 提示并绑定跳转——「崩溃记录」开崩溃目录、
  「模组不匹配」开 mods 目录、其余切到设置页。
- **面板加宽**：改用 `InAppDialog.customSized(…, 560, -1)`，四个动作
  「关闭 / 完整性检查 / 复制 / 导出报告（附 N 行日志）」不再被截断。
- **悬停断言进 CI**：`ci.ps1` 现在带 `-HoverCheck`；`capture-motion.ps1` 新增
  `-RequireForeground`（切不到前台返回 4），冒烟把 4 视为 SKIP、其它非 0 才算失败。
  实测四页全部通过，CI 输出 `CI PASS`。

### 体检面板键盘化 + 剪贴板断言 + 基线刷新脚本（2026-11-10 23:55）

- **键盘可达**：`InAppDialog` 打开后焦点落在第一个动作按钮，`Tab` 在按钮间移动、`Enter` 触发默认
  按钮、`Esc` 关闭——体检面板（以及所有应用内弹窗）都能纯键盘操作。
- **剪贴板断言**：冒烟新增一步——打开体检面板 → 点「复制」→ 读取系统剪贴板，必须包含
  「Neko Craft Launcher 体检报告」，否则 FAIL。实测通过（日志
  `· 体检报告已复制到剪贴板（内容校验通过）`）。注意 JavaFX 退出时会清空剪贴板，验证必须在
  启动器存活期间进行。
- **基线刷新脚本**：`scripts/refresh-baseline.ps1` 一键连拍 → 生成九宫格 → 覆盖
  `docs/images/motion-contact-sheet.png`，并提示"先肉眼确认过渡帧合理再提交"。实测更新成功
  （20508→0，19 帧过渡）。

### 体检面板收尾 + 夜间基线（2026-11-11 00:15）

- **键盘提示条**：体检面板底部加一行 `Esc 关闭 · Tab 切换按钮 · Enter 确认`，让键盘操作可被
  发现（`InAppDialog` 本身已支持焦点落在首个动作、Tab 遍历、Esc 关闭）。
- **剪贴板断言加严**：冒烟除了检查剪贴板含「体检报告」，还要求**至少 9 行**有效内容，避免复制到残文。
- **夜间视觉基线**：新增 `scripts/capture-settled.ps1`（点一次页面→等动画→整窗截图）与
  `scripts/check-baseline.ps1`（可 `-Launch` 自己起停启动器；24×24 区块平均色比较，>2% 判失败）。
  `ci.ps1 -Nightly` 已接入。实测：`差异 6 点 = 0.40%（阈值 2.00%）` → PASS。
  动效九宫格仅作文档说明，不参与逐像素比较（每次帧选择会变）。

### 夜间基线证据 + 键盘提示开关 + CI 示例（2026-11-11 21:10）

- **失败留证据**：`check-baseline.ps1` 判定失败时自动保存
  `target/baseline-nightly/diff-<时间>.png` 与 `heatmap-<时间>.png`（红块标出差异区块）。
  实测把阈值压到 0.01% 复现：差异 2 点 = 0.13%，热力图红块正好落在"系统内存占用"进度条上
  ——动态数值造成的差异，说明默认 2% 阈值是合理的。
- **键盘提示可关**：设置 → 管理新增「体检面板显示键盘提示（Esc/Tab/Enter）」，
  `config.showKeyHints` 默认 true，关掉后体检面板底部那行提示隐藏。
- **CI 落地文档**：新增 `docs/CI-EXAMPLES.md`，给出 GitHub Actions（自托管 Windows runner，
  需已登录且不锁屏）与 Windows 计划任务（`-LogonType Interactive`）两种配置，以及失败产物对照表。

### 键盘提示实测 + 对照图 + CI 摘要（2026-11-11 21:35）

- **键盘提示开关实测**：把 `config.showKeyHints` 置 false 重启，体检面板底部的
  `Esc 关闭 · Tab 切换按钮 · Enter 确认` 确实消失（截图 r134），之后已把配置还原为 true。
- **左右对照图**：夜间基线失败时除热力图外再生成 `compare-<时间>.png`（左基线 / 右当前），
  实测两图并排，差异只有时钟与内存数值，一眼可判。
- **CI 摘要**：`ci.ps1` 结束时写 `target/ci-summary.txt`（时间、耗时、各步结果、失败原因），
  `-SkipSmoke` 等提前退出的分支也会写。实测：

```
时间: 2026-11-10 21:30:02
耗时: 93 秒
构建/单测: 跳过
架构守卫: 通过
冒烟: 通过
夜间基线: 通过
结果: PASS
```

### CI 摘要结构化 + 基线元信息（2026-11-11 22:20）

`ci.ps1` 结束时写 `target/ci-summary.txt`，格式改为 **key=value**，并补充基线元信息：

```
time=2026-11-10 22:19:32
duration_sec=91
build=skipped
architecture=pass
smoke=pass
nightly=pass
baseline_hash=70C513348D9F
baseline_mtime=2026-11-10 00:05:28
baseline_diff_percent=0.47
result=pass
```

- 失败时追加 `failure=`、`evidence=<左右对照图>`、`heatmap=<热力图>`；实测把阈值压到 0.01
  复现：`nightly=fail`、`result=fail`、两条证据路径都写进摘要。
- `nightly` / `smoke` 状态按实际结果写，不再写死；新增 `-BaselineMaxDiff` 参数便于演练失败。

### 只剩「装配」的部分

`start(Stage)`（约 275 行）与 `createLaunchPanel`（187 行）现在读起来就是装配顺序：
建顶栏 → 建内容区 → 建页面 → 装拖放层 → 建 Scene → 装快捷键 → 显示窗口 → 起后台任务。
继续拆它们需要引入「窗口装配器」这层抽象，收益低于风险，暂不处理。

### 运行时验证（2026-11-07 18:00 补齐）

先前没有运行时证据的几条路径，已经用真机跑过：

- **启动 → 停止**：命令栏 `launch` 起 1.12.2（Java 8、authlib-injector），日志出现
  `✅ 游戏进程已启动 (pid 3432)`、`⏱ 启动准备耗时 1.0 秒` 与 `[Game]` 转发；点顶栏
  「结束游戏」后 `launch-history.json` 新增 `{instanceId:1.12.2, prepMs:1013,
  sessionSeconds:91, stopped:true}`，`playtime.json` 的 1.12.2 从 102 → 193 秒，
  概览从「22 次 · 55%」变成「23 次 · 57%」、最近一次显示「主动结束 · 准备 1.0s · 游戏 1 分」。
- **FavoritesRenderer 有数据分支**：写入两条收藏后，收藏夹页显示「整合包（1）」
  「模组（1）」两组与各自的下载量、星标、下载按钮。
- **downloadChosenVersion**：从「数据包」分类下载 VeinMiner，日志打印下载地址与
  `✅ 下载完成: ...\versions\1.12.2\datapacks\veinminer-neoforge-2.13.0.jar`。

### 由此发现并修掉的真实 bug

`installDirFor` 只判断中文类型（「模组」「数据包」…），而社区页传进来的是 Modrinth 的
英文类型（`mod` / `datapack` …），于是**除整合包外的所有资源都被丢进
`downloads/modpack`**，下载完成后的「已放入 xxx/」提示也永远不显示。

修复：新增 `core.mod.ProjectFormat.installDir` / `isModpack`（中英文都认）并配 4 个单测，
主类改用它们。复测：同一个数据包现在落在 `versions/1.12.2/datapacks/`。

已记录的验证方式与证据截图保存在 `C:\Users\Administrator\.dsh\tmp\neko-verify\`（r17–r27）。