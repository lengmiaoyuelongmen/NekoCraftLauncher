# Neko Craft Launcher 2.42.0

> 专治「点了启动，按钮一直显示运行中，什么也没发生」：现在最多等 150 秒就收场并告诉你怎么查。

## 安装包

| 文件 | 说明 |
|---|---|
| `NekoCraftLauncher-2.42.0.exe` | **安装版**：双击安装，可选目录，开始菜单 + 桌面快捷方式（按用户安装） |
| `NekoCraftLauncher-2.42.0-win64.zip` | 免安装版，解压双击 `NekoCraftLauncher.exe` |
| `NekoCraftLauncher-2.42.0-Features.pdf` / `.docx` | 功能预览（图文版） |
| `SHA256SUMS.txt` / `verify.txt` / `neko-codesign.cer` | 校验与签名材料 |

## 反馈的现场

- 群里反馈：`点击启动以后，它就一直显示运行中，什么也没发生`（还贴了 44 秒录屏）；
- 录屏里任务管理器 CPU 6%、内存 19%，**没有任何 java 进程起来** —— 说明卡在「准备」阶段，进程根本没启动；
- 日志停在：`📥 Azul Zulu：下载 zulu21.…（约 200MB，请耐心等待）…` 之后再无输出。

## 根因（两处，都修了）

1. **运行时索引查询没有「整体超时」**：`JavaRuntimeIndex` 只设了 `connectTimeout`，
   服务端「连上了但不回」时会无限等待，界面就永远停在运行中。
   → 现在每个请求都带 20 秒整体超时。
2. **准备阶段没有兜底**：任何一步卡住（下载、解压、杀软拦截）都表现为「永远运行中」。
   → 新增 150 秒看门狗：超时后**自动收掉运行状态**，弹出「启动卡住了」，
   写明卡在哪个阶段，并给「复制日志」/「日志」两个按钮。

## 顺带加固（2.41 已有，这里合并说明）

- 本机已装对应版本 JDK 时**直接复用，不下载**（本机实测命中 `C:\Program Files\Java\jdk-21.0.11\bin\java.exe`，零网络请求）；
- 找不到时把检查过的目录全列进日志（`· …（Java 8）`、`· …（没有 bin/java.exe）`），一眼看出是没装还是版本不对；
- 失败弹窗带「打开 Java 设置」按钮，不再只写一行红字。

## 用户现在可以怎么做

1. **升级到 2.42.0**（旧版 2.25.0 没有本地 JDK 复用，也没有看门狗）；
2. 若仍卡住：设置 → Java 手动填 `C:\Program Files\Java\jdk-21.0.11\bin\java.exe`；
3. 群里那位机器上有 **360 安全卫士**在跑，把启动器目录与 `game\java` 加进信任区，
   避免 java.exe 被拦（这也符合「一点就卡、进程根本没起来」的现象）；
4. 卡住时点弹窗里的「复制日志」，把日志发来就能直接定位。

## 回归测试

- `JavaEnsureLocalProbeTest`：本机有 JDK 21 时必须复用且日志里不出现「下载」；
- 无头冒烟 `-Dmc.smoke=1` 六页全过（SMOKE DONE ok）；
- 单元测试 **333 项全绿**；文案基线 2227。

## 校验

```
9d9d200d824de69f4ac4601094af0eee6f50aecad0f9db7e3ebe5efcd876860a  NekoCraftLauncher-2.42.0.exe
72593c98af87aa3834cbc0c539de1c85f055508cec663f09889ecb230122894a  NekoCraftLauncher-2.42.0-win64.zip
d6bd4b00e5e1ed9e5599123556ee80a281a5cc34775c86cca22d5655e93db43a  NekoCraftLauncher-2.42.0-Features.pdf
d92071a88e20d1622f0393e5c070d5d23d5ac6e084fdbc78c861408adaaa7def  NekoCraftLauncher-2.42.0-Features.docx
41bdc7658bd42ee52d59d81ccd541597b7deac9e600715ab23327227a73cdb89  neko-codesign.cer
5f5a93803c064301cc467cbc826dea96019f584a000d9102a3ced4eaeef14840  verify.txt
```
