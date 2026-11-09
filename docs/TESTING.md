# 测试与自检手册

启动器的验证分三层：单测、冒烟（真实进程 + UIA）、UI 自检。这里记录常用命令、测试钩子和已知限制。

## 1. 构建与单测

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.11'
.\mvnw.cmd -o -q clean package          # 编译 + 单测 + 打 fat jar
.\mvnw.cmd -o -q test                    # 只跑单测
.\mvnw.cmd -o -q test -Dtest=VersionFitTest   # 单个测试类
```

当前基线：**255 个单测**，0 编译告警（`-Xlint:all,-processing`）。

## 2. 冒烟测试

```powershell
powershell -ExecutionPolicy Bypass -File scripts/smoke-test.ps1 -SkipBuild
```

做四件事：

1. 启动真实进程（带 `-Dmc.uiSelfCheck=1`，会自动开关一个弹窗）；
2. 用 UIA 逐页点击导航，页面清单与标记来自 `scripts/ui-pages.json`；
3. 每页截图并检查不是空白页、标题确实切换、**页面标记文案在 UIA 树里出现**；
4. 调用 `scripts/check-ui.ps1` 做 UI 自检。

通过时输出类似：

```
页面自检 5 次（累计修复 5 个）、弹窗自检 1 次
PASS: UI 自检通过
PASS: 5 个页面切换完成，stderr 无异常，UI 自检通过
```

## 3. UI 自检

```powershell
# 四角圆角 + 场景描边（需要启动器正在运行）
powershell -ExecutionPolicy Bypass -File scripts/check-ui.ps1 -LogPath target/smoke/stdout.log

# 只查四角
powershell -ExecutionPolicy Bypass -File scripts/check-corners.ps1

# 只查描边（要求至少一次弹窗自检）
powershell -ExecutionPolicy Bypass -File scripts/check-ui.ps1 -LogPath <日志> -SkipCorners -RequireDialog
```

- `check-corners.ps1` 沿对角线量「多远才出现非桌面像素」：方角 1~2px，16px 圆角 4~5px；
  阈值按 DPI 缩放（`max(3, round(2.2×缩放))`）。窗口无法置前时返回 **2**，视为 SKIP。
- 描边检查读启动器 stdout 里的 `圆角描边修复: N 个，剩余缺失: M 个` 和
  `弹窗描边修复: …`，要求所有行的「剩余缺失」为 0。

## 4. 架构守卫

```powershell
powershell -ExecutionPolicy Bypass -File scripts/check-architecture.ps1
```

断言 `core` 不依赖 `javafx.*`、`ui.*` 和入口类。

## 5. 测试钩子（系统属性）

| 属性 | 作用 |
|---|---|
| `-Dmc.uiSelfCheck=1` | 启动 2.5 秒后自动弹一个 `InAppDialog`，700ms 后自动关闭；让冒烟每次都有弹窗描边数据 |
| `-Dmc.failPage=<页面名>` | 让 `createPage(<页面名>)` 抛异常，用于验证「页面构造失败 → 弹错误 + 回退启动页」 |
| `-Dmc.import=<文件>` | 启动后自动导入指定整合包（冒烟/回归用） |

示例：

```powershell
Start-Process javaw -ArgumentList '-Dmc.failPage=tools','-jar','target\mc-launcher.jar'
```

## 6. 手工验证配方

- **缺前置依赖**：往 `game/versions/<实例>/mods` 放一个带
  `fabric.mod.json`（`"depends":{"jei":"*"}`）的假模组，启动即触发；
- **加载器不适配**：造一个 `.mrpack`，`dependencies` 写 `fabric-loader` + `minecraft 1.12.2`
  （Fabric 元数据对 1.12.2 返回 400），可复现「加载器不可用」的安全失败；
- **跨加载器**：把带 `fabric.mod.json` 的 jar 放进整合包 `overrides/mods` 再导入原版实例。

## 7. 已知限制

- 冒烟里的四角检查常被 **SKIP**：冒烟自己占着前台，`SetForegroundWindow` 常被系统拒绝。
  需要真实四角结论时单独跑 `check-corners.ps1`。
- 动效（悬停、错落入场）只有代码级证据，静态截图看不出来。
- `check-ui.ps1` 的弹窗部分依赖 stdout 日志；手工启动时记得重定向 stdout。
