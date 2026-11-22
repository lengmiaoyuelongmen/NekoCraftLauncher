# Neko Craft Launcher 2.43.0

> 更新流程不再「静默消失」：卡住会报错，下载完会先问你再装。

## 安装包

| 文件 | 说明 |
|---|---|
| `NekoCraftLauncher-2.43.0.exe` | **安装版**：双击安装，可选目录，开始菜单 + 桌面快捷方式（按用户安装） |
| `NekoCraftLauncher-2.43.0-win64.zip` | 免安装版，解压双击 `NekoCraftLauncher.exe` |
| `NekoCraftLauncher-2.43.0-Features.pdf` / `.docx` | 功能预览（图文版） |
| `SHA256SUMS.txt` / `verify.txt` / `neko-codesign.cer` | 校验与签名材料 |

## 反馈的问题

> 我点击那个更新完成过以后，它会自动更新，等了 5 分钟，什么也没提示。

之前的行为：下载完成后**直接**拉起安装程序 `/VERYSILENT` 并 1.2 秒后关闭启动器。
一旦下载中途卡住（连接挂着不传数据），进度条不动、日志没有、界面也没有 —— 就是「5 分钟什么也没提示」。

## 这次改了什么

1. **下载停顿保护**：45 秒没有收到新字节就判定失败，报错写明停顿多久、已下载多少，
   而不是无限等待（`UpdateChecker.download` 现在按 32KB 分块读取并带看门狗）。
2. **下载完先问你**：弹出「更新已下载」对话框，两个按钮：
   - **立即安装** → 启动安装程序并关闭启动器；
   - **稍后自己装** → 保留安装包、打开所在目录，状态栏提示路径。
3. **日志留痕**：`⬆ 更新包已下载：<路径>（等待确认后启动安装程序）`，
   装不上时也知道去哪个文件双击。

## 与上一版的关系

2.42.0 修的是「启动卡在运行中」（准备阶段 150 秒看门狗 + 运行时索引 20 秒超时）；
这一版把同类的「静默卡死」问题在更新链路上补齐，两条线现在都不会再无声挂起。

## 工程质量

- 编译 0 告警；单元测试 **333 项全绿**；文案基线 2227
- 新增中英文案 `update.downloaded.*` 11 条

## 校验

```
8be91cc1966b0418eac451c84e1721ee6af91887d269ccd104fae988bb46596b  NekoCraftLauncher-2.43.0.exe
80efa3705adec728f04214483dbb9d4905549be8689ab16942a1d3ac20c093fd  NekoCraftLauncher-2.43.0-win64.zip
a4080c68bfa4e90e2dc428c2cc39287975956d64437c6cb1c4054d02d1aeb152  NekoCraftLauncher-2.43.0-Features.pdf
4031cbfeb8c448391e66c70158926450358faac66a321752018f252086fa594e  NekoCraftLauncher-2.43.0-Features.docx
41bdc7658bd42ee52d59d81ccd541597b7deac9e600715ab23327227a73cdb89  neko-codesign.cer
bee0c36516ad8682f3d459bd0d16466a407799dcf09a7d244c3fcc60ff7066a7  verify.txt
```
