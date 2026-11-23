# Neko Craft Launcher 2.49.0

> 玩家面板显示在线状态、实例页文案入 i18n、首帧耗时继续盯着。

## 安装包

| 文件 | 说明 |
|---|---|
| `NekoCraftLauncher-2.49.0.exe` | **安装版**：jpackage/WiX，静默参数 `/quiet` |
| `NekoCraftLauncher-2.49.0-win64.zip` | 免安装版；支持就地更新、回退与备份管理 |
| `NekoCraftLauncher-2.49.0-Features.pdf` / `.docx` | 功能预览（图文版） |
| `SHA256SUMS.txt` / `verify.txt` / `neko-codesign.cer` | 校验与签名材料 |

## 1. 服务器面板：在线玩家

玩家页顶部新增一行 **在线玩家 (N)：A、B**：

- 从 `server-console.log` 的 `xxx joined the game` / `xxx left the game` 事件推断，**不联网、不占端口**；
- 有在线玩家时绿色加粗，没有时灰字提示；
- 跟随上一版的 5 秒自动刷新一起更新，退出游戏后自动消失。

## 2. i18n：实例页落地

`InstancesPanel` 的可见文案全部走 key：

- 刷新 / 返回启动页 / 重命名 / 从列表移除 / 打开实例文件夹 / 打开日志目录 / 复制实例 ID / 删除实例 / 实例设置 / 检查实例完整性 / 选择 .minecraft 目录 / 收藏失败提示；
- 中文文案基线 **2217 → 2202**（-15，本轮累计 -25）。

## 3. 首帧耗时

- 保持上一版加入的 `STARTUP first-frame X ms`；
- 本轮实测 **5279 ms**（2.48 为 5199 ms）—— 新增在线玩家解析与实例页 key 未带来可测退化；波动 ±80ms 属 JVM 启动抖动；
- 下一步优化的目标点已经明确：社区预热的启动时机（现在在首次进下载页时同步触发）。

## 工程质量

- 编译 0 告警；单元测试 **343 项全绿**（+2：日志解析、缺失日志兜底）；无头冒烟六页全过
- exe（安装包 + 便携壳）均已签名并带 DigiCert 时间戳

## 校验

```
0b327adf1ff19007b0147f17b7468046579c5b8977b2f67b0183298275520642  NekoCraftLauncher-2.49.0.exe
5e3b270f570426826c7a1a487c7775a5c24be41c7e07332f8a071bc74a225e95  NekoCraftLauncher-2.49.0-win64.zip
de44b75abbbd7844d7f3db337dbf7a013f75865245bb4abf1d5dbf6b31192859  NekoCraftLauncher-2.49.0-Features.pdf
d339a743b05c694f177f2e2b11715eda054322b7ae77138782c15934d2a4abbf  NekoCraftLauncher-2.49.0-Features.docx
41bdc7658bd42ee52d59d81ccd541597b7deac9e600715ab23327227a73cdb89  neko-codesign.cer
3ae84bb19d6c2f862bdd8f99404d346e17ca1e8fc62fa3d4c4fe63d20ce338f6  verify.txt
```
