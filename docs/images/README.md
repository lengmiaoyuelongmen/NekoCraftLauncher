# 视觉基线图

这里存放用于"肉眼回归"的基线截图，目前只有一张：

| 文件 | 内容 | 来源 |
|---|---|---|
| `motion-contact-sheet.png` | 页面切换动效的 3×3 关键帧拼图（前四格启动页、中间三格过渡中、最后两格稳定） | `scripts/capture-motion.ps1 -ClickName 设置 -ContactSheet` |

## 更新方式

```powershell
# 1. 启动器已打开，切到启动页
powershell -ExecutionPolicy Bypass -File scripts/capture-motion.ps1 `
    -ClickName "设置" -Frames 30 -IntervalMs 40 -ContactSheet -OutDir target/motion

# 2. 用新图替换基线（先肉眼确认过渡帧合理）
Copy-Item target/motion/contact-sheet.png docs/images/motion-contact-sheet.png -Force
```

## 什么时候该更新

- 主动调整了动画时长/缓动（`Motion` 里的 `STAGGER_*`、`pageIn` 参数）；
- 页面布局大改，关键帧构图明显变化；
- 换了主题配色。

**不要**因为"截图颜色有一点差异"就更新——先确认是不是真的改了设计，避免把回归图变成走过场。
