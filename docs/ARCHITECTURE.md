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

## 2. 当前结构（Phase 1 完成）

```
src/main/java/com/example/demo/
├── Launcher.java            # 进程入口
├── HelloApplication.java    # 8620 行 → 后续拆成 ui.shell + ui.page（Phase 5）
├── ...（58 个 UI 类，仍是默认包）
└── core/                    # 60 个类，0 处 import javafx
    ├── app/        7   AppPaths, AppVersion, LauncherConfig, SingleInstance, ConfigHistory, UpdateChecker, SupportBundle
    ├── auth/       6   AuthManager, AccountStore, MicrosoftAuth, AuthlibInjectorManager, SessionStore, SecretStore
    ├── download/   8   DownloadManager, Downloader, Aria2Downloader, Integrity, SourcePinger, CurseForgeClient, IconProxy, ZipUtil
    ├── game/      10   JavaManager, JavaScanner, LoaderInstaller, ModpackImport/Exporter, PortableExporter, ServerPackExporter, ModDependencyScanner, ModConflictChecker, CrashAnalyzer
    ├── launch/     3   LaunchCommand, LaunchCancelledException, GameCommandSender
    ├── meta/       2   VersionProfile, VersionManager
    ├── model/     16   Instance, InstanceDirs, InstanceMeta, InstanceOverrides, MinecraftVersion, VersionDetails, VersionManifest, AssetIndex, Library, Modpack, ModpackProject, UserSession, LoginCardData, Favorites, Playtime, LaunchHistory
    ├── runtime/    6   HardwareInfo, SystemMetrics, PerformanceOptimizer, PerformanceTuner, MemoryAdvisor, NetworkBoost
    └── ai/         2   DeepSeekClient, LocalAiAssistant
```

### 分层规则（`scripts/check-architecture.ps1` 会强制检查）

1. `core` 下任何类 **不许 import javafx.\***，也不许 import 默认包里的 UI 类。
2. UI 通过 `core` 的公开 API 和回调（`Consumer<String>` 日志、`Runnable` 进度）与核心通信。
3. 网络、磁盘、进程调用集中在 core，便于替换与测试。

## 3. 为什么这样切

- **app**：只回答「程序装在哪、配置存哪、是否单实例」。
- **model**：纯数据 + 目录解析（`InstanceDirs` 就是 Prism 的 instance 目录约定）。
- **meta**：版本清单、版本 JSON、继承链、rules —— 启动器的地基。
- **launch**：把 VersionProfile + 账号 + 配置翻译成一条 java 命令。
- **download**：所有 HTTP 下载都从这里走，天然带镜像/重试/校验。
- **auth**：账号类型（离线 / 微软 / 外置登录）只在这里出现。
- **game**：加载器安装、整合包导入导出、依赖扫描、崩溃分析。
- **runtime**：硬件探测、性能建议、系统指标，纯计算。

## 4. Phase 1 实测（2026-11-06）

| 指标 | 重构前 | 现在 |
|---|---|---|
| `com.example.demo` 直接持有的类 | 118 | 58（全是 UI + 入口） |
| `core` 包类数 | 0 | **60** |
| core 中的 JavaFX 引用 | — | **0** |
| `-Xlint:all` 告警 | 0 | **0** |
| 单元测试 | 13 通过 | **13 通过** |
| 冒烟测试（5 页切换 + stderr） | PASS | **PASS** |
| jar 体积 | 51.8 MB | 51.8 MB |
| 行为变化 | — | **无**（纯包迁移 + 1 处可见性） |

## 5. 迁移手法

1. 脚本按「域 → 新包」映射移动文件，改 `package` 行。
2. 反向补 import：给被移动类补上「仍在默认包的类」的 import。
3. 全量符号表修 import：删掉指向已不存在路径的 import，按简单名唯一性补缺失 import。
4. 编译 → 测试 → 冒烟，三步都过才算一个阶段完成。

> 这类机械迁移必须一次跑完再编译，不要边移边改逻辑。

## 6. 后续阶段

| 阶段 | 内容 | 验收 |
|---|---|---|
| **Phase 2** | 启动管线任务化：`LaunchPlan`（不可变值对象）+ `LaunchTask` 链（检查 Java → 校验文件 → 解压 natives → 拼命令 → 起进程），从 `HelloApplication.startGame` 抽离 | 新增 6+ 单测；启动页实测能进游戏 |
| **Phase 3** | 实例一等公民：`InstanceSettings`（内存 / JVM 参数 / Java 路径 / 是否隔离 / 游戏目录）落到 `versions/<id>/instance.json`；实例设置页读写 | 两个实例用不同内存启动成功 |
| **Phase 4** | 下载引擎统一：`DownloadEngine` 统一入口，所有下载带 SHA1 校验、并发上限、失败重试与镜像回退；`DownloadManager` 退化为其实现 | 强制断网/坏源测试通过 |
| **Phase 5** | UI 拆分：`HelloApplication` → `ui.shell`（窗口/导航/主题）+ `ui.page.*`（启动/下载/实例/设置/工具）+ `ui.component.*` | 主类 ≤ 800 行；页面可单独实例化 |
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
