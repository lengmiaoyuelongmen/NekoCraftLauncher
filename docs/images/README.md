# 视觉基线图

| 文件 | 内容 | 来源 |
|---|---|---|
| `settings-settled.png` | 设置页的"稳定态"整窗截图（动画结束后） | `scripts/refresh-baseline.ps1` |
| `motion-contact-sheet.png` | 页面切换动效的 3×3 关键帧拼图（仅作文档说明，不参与比对） | `scripts/capture-motion.ps1 -ContactSheet` |

> 逐像素比对只用在 `settings-settled.png` 上。动效拼图每次帧选择都可能不同，**不能**当基线。

## 更新方式

```powershell
powershell -ExecutionPolicy Bypass -File scripts/refresh-baseline.ps1
```

脚本会点击「设置」、等动画结束、截图并覆盖 `docs/images/settings-settled.png`，跑完请肉眼确认再提交。

## 夜间比对

```powershell
powershell -ExecutionPolicy Bypass -File scripts/check-baseline.ps1 -Launch
```

自己启动启动器 → 抓稳定态 → 与基线做 24×24 区块平均色比较，差异超过 2% 判失败。
动态文本（时钟、内存数值）只影响少数区块，不会误报。

## 什么时候该更新基线

- 主动改了设置页布局/配色；
- 换了主题或字号；
- 新增了设置项导致页面结构变化。

**不要**因为"内存数字不一样"就更新基线。