# Neko Craft Launcher 2.54.0

> 量化了「首屏节点树到底值多少钱」，服务器在线列表可用 /list 校准，设置页文案继续收。

## 安装包

| 文件 | 说明 |
|---|---|
| `NekoCraftLauncher-2.54.0.exe` | **安装版**：jpackage/WiX，静默参数 `/quiet` |
| `NekoCraftLauncher-2.54.0-win64.zip` | 免安装版；支持就地更新、回退与备份管理 |
| `NekoCraftLauncher-2.54.0-Features.pdf` / `.docx` | 功能预览（图文版） |
| `SHA256SUMS.txt` / `verify.txt` / `neko-codesign.cer` | 校验与签名材料 |

## 1. 首屏：节点树的真实代价

新增诊断开关 `-Dmc.minimalLaunch=1`（只放一个占位块），用来量上界：

| 场景 | 首帧 |
|---|---|
| 正常启动页 | 4798 ms |
| 极简占位（几乎无节点） | **3466 ms** |
| 差值（= 启动页内容的成本） | **≈1.33 秒** |

进一步拆分：UI 构建 2420→1921（**-0.5s**），首次渲染 1994→1143（**-0.85s**）。

结论很明确：**骨架化（先占位、首帧后再填内容）预计能拿回 0.8~1.3 秒**，这值得单独一轮做。
本轮没有把它硬塞进去 —— 它要改启动页两列的构建时序，做一半比不做更糟（会出现空白闪屏）。

## 2. 服务器：在线列表可用 /list 校准

玩家页在线行多了 **用 /list 校准** 按钮：

- 服务器在运行时向控制台发 `list`，900 ms 后读日志尾部解析
  `There are N of a max of M players online: A, B`；
- 日志推断偶尔会因日志截断而不准（比如服务器重启过），这一步用服务端权威数据纠正；
- 服务器没运行 / 发送失败 / 解析不到，都有明确提示。

解析器与尾部读取都是纯函数，配了单测（含「0 人在线」的空列表情况）。

## 3. i18n

本轮把设置页零散标签（日志保留、更新分组、更新源、备份时间）也纳入 key 体系。
基线维持 **2178**（本组命中 0 条，属于既有文案已覆盖，不再虚报数字）。

> 说明：上一轮把 2181→2178 的 3 条是「个性化/智能性能」组；本轮这几个词条在代码里写法不同，
> 没有实际命中，就不计入降幅。宁可数字不动，也不虚报。

## 交付

- **350 项单测全绿**（新增 /list 解析）；无头冒烟六页全过；编译 0 告警
- 本轮首帧实测 4590 ms（与上轮 4798 ms 属同量级波动）
- exe（安装包 + 便携壳）均已签名并带 DigiCert 时间戳

## 校验

```
4eee64b95ed76239a4dbda1477293e62f94cbcfab70365c43d4aeebff2643dd4  NekoCraftLauncher-2.54.0.exe
9ab7fc5589d8aabb7732863387dda00bdf99d0758d34148a3ded6ff7c1b8e68a  NekoCraftLauncher-2.54.0-win64.zip
289dfee38ec2c082d7aa204825e001ace8f28bfaa762d637948f9dd1cbbb219d  NekoCraftLauncher-2.54.0-Features.pdf
2d20eae690210581fd6c65b4dd0d78323634ab7bb75fd4d87b5bc88d52f07963  NekoCraftLauncher-2.54.0-Features.docx
41bdc7658bd42ee52d59d81ccd541597b7deac9e600715ab23327227a73cdb89  neko-codesign.cer
cc9e06a98933ab36f6f6c193ebae486e76178185e59f3b426a056d4cc7378c25  verify.txt
```
