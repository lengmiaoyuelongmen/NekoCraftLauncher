# 架构与重构说明

> 这份文档记录 2026-11 那次「反编译恢复 + 结构化重构」的结果、约定和后续计划。

## 1. 背景

`HelloApplication.java` 原本在一次编码事故后由 CFR 反编译恢复，留下了大量反编译痕迹：

- `(Node[]) new Node[] {…}` 之类的数组包装
- 大量冗余强转（`(String)`、`(MinecraftVersion)`、`(Throwable)`）
- 裸类型（`ComboBox`、`ArrayList`、`Dialog`、`CompletableFuture`）
- 按子节点索引强转的脆弱写法（`((Label) ((HBox) n).getChildren().get(1))`）
- 无人调用的死方法/死字段
- 两处由反编译引入的运行时崩溃

重构目标是：**在不改变 UI 和功能的前提下**，把这些清理掉，并把巨型主类按页面拆成组件。

## 2. 目录结构

```
src/main/java/com/example/demo/
├── HelloApplication.java      # 应用外壳：窗口、导航、启动流程（~7700 行）
├── LaunchOverviewPane.java    # 启动页「启动概览」
├── LaunchAdvisorBar.java      # 启动页「启动顾问」横条
├── LaunchHeroButton.java      # 启动页猫咪启动按钮
├── AccountInfoCard.java       # 账户信息网格
├── AccountToolsBar.java       # 头像下那排小按钮
├── AvatarPane.java            # 头像框（选择/右键/拖拽）
├── LoginFormPane.java         # 登录表单
├── LoginCardDrop.java         # 登录卡片拖拽
├── LoginHeroButton.java       # 登录大按钮
├── LoginCardData.java         # 登录卡片解析结果
├── CommandBar.java            # 命令栏（AI 开关 / 补全 / 历史）
├── DownloadFilterBar.java     # 下载页搜索 + 筛选
├── DownloadNavItem.java       # 下载页左侧导航项
├── DownloadResultView.java    # 下载页结果区（列表 + 无限滚动）
├── DownloadStatusPane.java    # 加载/失败状态浮层
├── PrewarmOverlay.java        # 预渲染进度遮罩
├── ModpackCell.java           # 整合包/模组列表单元格
├── BlockIcons.java            # 版本方块图标绘制
├── WorldsNoticePanel.java     # 「世界」分类提示
├── MinecraftVersionCell.java  # 版本下拉单元格
└── （其余：SettingsPanel、ToolsPanel、ModsPanel、InstanceSettingsPanel 等原有模块）
```

## 3. 组件约定

- 组件只依赖：`LauncherConfig`、少量回调（`Runnable` / `Consumer`）、以及明确的数据对象
- 主类需要回调进组件的都用方法引用，例如 `this::chooseAvatar`
- 组件不直接引用 `HelloApplication` 的字段；需要宿主能力时用小型 `Host`/回调接口
- 主类保留的字段只剩真正跨页面共享的状态（会话、配置、当前实例、日志区等）

## 4. 构建 / 运行 / 测试

```powershell
# 构建 fat jar
.\mvnw.cmd -o -q package -DskipTests

# 运行
"%JAVA_HOME%\bin\javaw.exe" -jar target\mc-launcher.jar

# 冒烟测试（构建 + 启动 + 逐页截图 + stderr 检查）
powershell -ExecutionPolicy Bypass -File scripts\smoke-test.ps1
```

冒烟测试产物在 `target\smoke\`：5 张页面截图 + `stderr.log`。

## 5. 当前质量指标（2.8.0）

| 指标 | 重构前 | 现在 |
|---|---|---|
| `HelloApplication` 行数 | 9721 | **7733** |
| 抽出的组件/工具类 | 0 | **20** |
| `-Xlint:all` 告警 | 78 | **0** |
| 死方法 / 死字段 | 20 / 5 | 0 |
| 修复的运行时崩溃 | — | 2 |

两个已修复的崩溃（都是反编译产物）：

1. `createBlockIcon` 里 `top/left/right` 恒为 `null` → `Color.web(null)` NPE，下载页「最新版本」一渲染就崩
2. `loadMinecraftVersions` 用 `children.get(0)/get(1)` 强转 `VBox` → 点「下载 → Minecraft」必然 `ClassCastException`

### 运行时开销（2.8.0 实测）

| 指标 | 数值 | 说明 |
|---|---|---|
| 空闲 CPU（启动页） | ~1.1%（单核） | 背景动画约 2fps，窗口失焦即停；工具页采样、时钟、遥测都按可见性启停 |
| 空闲 CPU（下载页） | ~2.5%（单核） | 列表可见时有更多重绘 |
| 空闲 CPU（最小化） | ~1.25%（单核） | 背景动画停止，剩余为定时器与 JavaFX 脉冲 |
| 工作集内存 | 366 MB 启动 → 425 MB 加载 40 个图标后 → 417 MB（90 秒后 GC 回落） | 图标缓存是 LRU，上限 480 张 |
| 线程数 | 75（下载期间）→ 60（空闲） | 下载线程池会回收 |
| 日志区 | 上限 400k 字符 | 超出后从头部裁到 200k，避免长时间运行无限增长 |
| 日志写入 | 90ms 合批 | 下载刷屏时不再逐行触发 TextArea 布局 |

## 6. 后续计划

1. ~~**启动页账户卡**~~：已拆成 `LaunchAccountColumn`（布局在组件里，主类只提供 `Callbacks`）
2. **启动页右侧控制台卡**（`createLaunchPanel` 剩余部分）：`logArea` / `aiChatArea` / `launchOverviewPane` / `CommandBar` / `DownloadBar` 的装配
   - 难点：`logArea` 在主类里有上百处引用，需要先抽一个 `LogSink` 或保留字段引用
3. **下载页导航**：`createDownloadPanel` 的左侧栏可再抽一层（分类注册表）
4. **AI 对话 / 命令执行 / 启动流程**：主类里这三块各自可以独立成类

## 7. 注意事项

- 源码文件**只用 UTF-8 无 BOM** 写入；不要用 PowerShell 的 `Get-Content` / `Set-Content` 改源码（5.1 会按 ANSI 读、写 BOM，历史上造成过一次文件损坏）
- `scripts/smoke-test.ps1` 本身是 **UTF-8 with BOM**（PowerShell 5.1 需要它才能正确解析中文）
- 每次改完先跑 `scripts\smoke-test.ps1`；涉及登录/退出/启动的改动需要手动补一次回归
