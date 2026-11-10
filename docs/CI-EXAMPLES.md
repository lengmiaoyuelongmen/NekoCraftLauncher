# 把 CI 跑起来

仓库里的验证链是纯 PowerShell 脚本，任何能跑 Windows 的环境都能用：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/ci.ps1            # 构建 + 单测 + 守卫 + 冒烟 + UI 自检
powershell -ExecutionPolicy Bypass -File scripts/ci.ps1 -SkipBuild # 复用已有 jar
powershell -ExecutionPolicy Bypass -File scripts/ci.ps1 -Nightly   # 额外跑视觉基线比对（自己起停启动器）
```

## GitHub Actions（自托管 Windows runner）

UI 测试需要真实桌面（UIA + 截图），GitHub 托管 runner 没有交互桌面，必须用自托管 Windows 机器。

```yaml
name: ui-checks
on:
  push:
    branches: [main]
  schedule:
    - cron: "0 18 * * *"   # 每天 UTC 18:00 = 北京时间 02:00
jobs:
  nightly:
    runs-on: [self-hosted, windows]
    steps:
      - uses: actions/checkout@v4
      - name: JDK 21
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "21"
      - name: 关闭锁屏并保持桌面可用
        shell: powershell
        run: |
          powercfg /change standby-timeout-ac 0
          powercfg /change monitor-timeout-ac 0
      - name: 一键验证（含夜间基线）
        shell: powershell
        run: powershell -ExecutionPolicy Bypass -File scripts/ci.ps1 -Nightly
      - name: 失败时收集产物
        if: failure()
        uses: actions/upload-artifact@v4
        with:
          name: ui-artifacts
          path: |
            target/smoke/*.png
            target/smoke/*.log
            target/baseline-nightly/*.png
```

注意事项：

- runner 必须**已登录且桌面未锁屏**，否则 UIA 找不到窗口、截图全黑；
- 自托管 runner 上只保留一个启动器实例，`ci.ps1` 会自己起停进程；
- 冒烟和夜间比对都会占用前台，别在同一台机器上并行跑多个 job。

## Windows 计划任务

适合"每台开发机每晚跑一次"：

```powershell
$action = New-ScheduledTaskAction -Execute "powershell.exe" `
    -Argument "-ExecutionPolicy Bypass -File C:\path\to\demo\scripts\ci.ps1 -Nightly"
$trigger = New-ScheduledTaskTrigger -Daily -At 2am
$principal = New-ScheduledTaskPrincipal -UserId $env:USERNAME -LogonType Interactive
Register-ScheduledTask -TaskName "NekoCraftLauncher-Nightly" -Action $action -Trigger $trigger -Principal $principal
```

`-LogonType Interactive` 很重要：任务必须以"已登录用户"身份运行，才有交互桌面。

## 失败时看什么

| 产物 | 位置 | 说明 |
|---|---|---|
| 冒烟截图 | `target/smoke/*.png` | 每页一张，命名 `01-launch`…`05-back-launch` |
| 启动器输出 | `target/smoke/stdout.log` / `stderr.log` | UI 自检与描边修复日志都在这里 |
| 基线差异截图 | `target/baseline-nightly/diff-<时间>.png` | 夜间比对失败时自动保存 |
| 差异热力图 | `target/baseline-nightly/heatmap-<时间>.png` | 红块标出与基线不同的区域 |

看到热力图后判断：

- 红块集中在设置卡片/按钮 → 真的改了 UI，跑 `scripts/refresh-baseline.ps1` 更新基线；
- 红块只在时间、内存数字附近 → 动态内容造成，检查阈值是否过严；
- 整屏飘红 → 窗口没在前台/没截到启动器，先修环境再谈基线。
