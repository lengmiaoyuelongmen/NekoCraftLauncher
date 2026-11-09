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

# 额外对每页导航按钮做悬停动效断言（需要窗口在前台，默认关闭）
powershell -ExecutionPolicy Bypass -File scripts/smoke-test.ps1 -SkipBuild -HoverCheck
```

做四件事：

1. 启动真实进程（带 `-Dmc.uiSelfCheck=1`，会自动开关一个弹窗）；
2. 用 UIA 逐页点击导航，页面清单与标记来自 `scripts/ui-pages.json`；
3. 每页截图并检查不是空白页、标题确实切换、**页面标记文案在 UIA 树里出现**；
4. 打开体检面板点「复制」，断言系统剪贴板里有报告且**至少 9 行**；
5. 调用 `scripts/check-ui.ps1` 做 UI 自检（含「页面过渡完成」计数与耗时断言 `-MinTransitions`，
   过渡耗时 0ms 或 >3s 都算失败）。

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

## 8. 一键 CI

```powershell
powershell -ExecutionPolicy Bypass -File scripts/ci.ps1
powershell -ExecutionPolicy Bypass -File scripts/ci.ps1 -SkipBuild   # 已有 jar 时只跑守卫+冒烟
powershell -ExecutionPolicy Bypass -File scripts/ci.ps1 -Nightly      # 额外跑视觉基线比对（自动起停启动器）
```

依次执行：构建+单测 → 架构守卫 → 冒烟（含 UI 自检）。任一步失败立即退出并打印 `CI FAIL：<原因>`，
成功输出 `CI PASS：…`；退出码可直接被 CI 使用。
## 9. 动效中间帧连拍与基线刷新

```powershell
powershell -ExecutionPolicy Bypass -File scripts/refresh-baseline.ps1
```

连拍 → 生成九宫格 → 覆盖 `docs/images/motion-contact-sheet.png`，跑完请先肉眼确认过渡帧合理再提交。


```powershell
# 切换动画 + 断言差异单调收敛 + 生成九宫格对比图
powershell -ExecutionPolicy Bypass -File scripts/capture-motion.ps1 -ClickName "设置" -Frames 30 -IntervalMs 40 `
    -ExpectDecreasing -ContactSheet -OutDir target/motion

# 悬停动效（把鼠标移到指定元素上再连拍）
powershell -ExecutionPolicy Bypass -File scripts/capture-motion.ps1 -HoverName "设置" -Frames 24 -OutDir target/motion-hover
```

参数说明：

| 参数 | 作用 |
|---|---|
| `-ClickName` | 第 4 帧用 UIA 触发该元素的 Invoke |
| `-HoverName` | 第 4 帧把鼠标移到该元素中心（UIA 矩形） |
| `-ExpectDecreasing` | 断言点击后的差异序列单调收敛（允许 ±5 抖动），否则 exit 1 |
| `-ContactSheet` | 把关键帧拼成 3×3 对比图 `contact-sheet.png`，可直接贴文档 |
| `-MinHoverDiff <n>` | 悬停差异峰值低于 n 时判定动效失效并 exit 1 |
| `-RequireForeground` | 窗口切不到前台时直接返回 4（冒烟据此记为 SKIP，不算失败） |

单张截图延迟 >300ms，抓不到 200ms 的动画；这个脚本改为**连拍**：每 40ms 抓一帧，第 4 帧时用 UIA
触发一次点击，最后把每帧与"稳定帧"逐点比较，输出差异序列。实测一次设置页切换：

```
帧 4: 122   帧 6: 98   帧 11: 79   帧 17: 52   帧 22: 24   帧 28: 0
```

差异单调收敛到 0，说明确实拍到了过渡过程；帧图保存在 `target/motion*/frame-XX.png`。
注意：差异也可能来自内容加载（预渲染遮罩、列表填充），需要用眼睛复核具体帧。
## 7. 已知限制

- 冒烟里的四角检查常被 **SKIP**：冒烟自己占着前台，`SetForegroundWindow` 常被系统拒绝。
  需要真实四角结论时单独跑 `check-corners.ps1`。
- 动效（悬停、错落入场）只有代码级证据，静态截图看不出来。
- `check-ui.ps1` 的弹窗部分依赖 stdout 日志；手工启动时记得重定向 stdout。
