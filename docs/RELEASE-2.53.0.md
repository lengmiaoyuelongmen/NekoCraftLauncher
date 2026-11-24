# Neko Craft Launcher 2.53.0

> 用 JFR 找到了首屏渲染的热点；物品可编辑描述行（Lore）；设置页文案继续入 i18n。

## 安装包

| 文件 | 说明 |
|---|---|
| `NekoCraftLauncher-2.53.0.exe` | **安装版**：jpackage/WiX，静默参数 `/quiet` |
| `NekoCraftLauncher-2.53.0-win64.zip` | 免安装版；支持就地更新、回退与备份管理 |
| `NekoCraftLauncher-2.53.0-Features.pdf` / `.docx` | 功能预览（图文版） |
| `SHA256SUMS.txt` / `verify.txt` / `neko-codesign.cer` | 校验与签名材料 |

## 1. 首屏渲染热点：JFR 实测结论

用 `-XX:StartFlightRecording=settings=profile` 录了 14 秒启动过程（238 个 ExecutionSample），
按自身耗时排序的热点帧：

| 样本数 | 帧 |
|---|---|
| 16 + 14 + 12 | `javafx.scene.Node.doProcessCSS()` / `Node$1.doProcessCSS` / `NodeHelper.processCSSImpl` |
| 12 | `sun.security.ec.ECOperations…double4`（登录态校验的椭圆曲线运算） |
| 8 | `MessageDigest/SHA2.implCompress`（哈希，JWT/签名相关） |
| 8 | `Prism ShapeUtil.rasterizeShape` + Marlin（矢量图标光栅化） |

结论：**热点是 CSS 处理（Node 树逐节点走 CSS 引擎）**，其次是登录态校验的密码学运算，再是矢量图标光栅化。

同时用一个直接实验排除了「概览面板太重」的猜测：`createOverviewPane()` 实测只占 **18 ms**，
所以不是它。首屏 ≈2.2 秒是**整棵节点树的 CSS 处理 + 光栅化**造成的，要真正压下来需要减少首屏节点数
（例如卡片内列表延后构建），这属于结构性改动，值得单独一轮做，而不是顺手改两行就宣称优化。

本轮实测数据（保持可对照）：`TIMING config=142ms scene=124ms total=4798ms`。

## 2. 物品编辑：描述行（Lore）

背包「编辑物品」弹窗新增描述行编辑：

- 每行一条，保存为 `tag.display.Lore` 的 JSON 文本（灰色、非斜体，避免客户端渲染差异）；
- 留空即清除；清掉后 `display` 若为空会一并移除，不留空 Compound；
- 写入前照旧备份 `server-backups\`，与名字/耐久/附魔共用同一个弹窗。

新增测试第 6 条：写入两行 → 读回（含样式字段）→ 清空后物品仍在。

## 3. i18n

设置页「个性化 / 智能性能」组：智能性能、智能优化、一键分配、内存优化、浅色模式、启用智能性能等 7 条。
基线 **2181 → 2178**（本轮 -3，累计 -49）。

## 交付

- **349 项单测全绿**（新增 Lore 往返）；无头冒烟六页全过；编译 0 告警
- exe（安装包 + 便携壳）均已签名并带 DigiCert 时间戳

## 校验

```
b0e40c5073095dcaf84581b583c6af777ba3ed060c8bd40d91727228171ed726  NekoCraftLauncher-2.53.0.exe
f2c4686f9141f06a662fa8368e64373c638b3ac0055c6a66e06e79b72b02ca2d  NekoCraftLauncher-2.53.0-win64.zip
485723b18a7cd021fbe44f917e57d5999a97393a680fdf74ab5a2cb8228ed46d  NekoCraftLauncher-2.53.0-Features.pdf
3e9453ef586c1d5db1e8d7ddcca0d4b505d235c0c333a91525f1f5ebbab92a03  NekoCraftLauncher-2.53.0-Features.docx
41bdc7658bd42ee52d59d81ccd541597b7deac9e600715ab23327227a73cdb89  neko-codesign.cer
3fc874318a72b35be82b75f57b6a3f26dae6fe9056c26679016c59c5d3b5e839  verify.txt
```
