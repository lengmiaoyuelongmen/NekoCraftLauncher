# Neko Craft Launcher 2.29.0

> 修「启动后一直显示运行中、游戏起不来」。根因不是游戏，是 **Java 运行时下载卡死**。

## 安装包

| 文件 | 说明 |
|---|---|
| `NekoCraftLauncher-2.29.0.exe` | **安装版**：双击安装，可选目录，开始菜单 + 桌面快捷方式（按用户安装） |
| `NekoCraftLauncher-2.29.0-win64.zip` | 免安装版，解压双击 `NekoCraftLauncher.exe` |
| `NekoCraftLauncher-2.29.0-Features.pdf` / `.docx` | 功能预览（图文版） |
| `SHA256SUMS.txt` / `verify.txt` / `neko-codesign.cer` | 校验与签名材料 |

## 你的日志说明了什么

```
☕ 版本 1.21.11 需要 Java 21，未指定路径，准备 Azul Zulu 高性能运行时…
📥 Azul Zulu：下载 zulu21.54.201-ca-jdk21.0.13.1-win_x64.zip（约 200MB，请耐心等待）…
⏳ 下载进度: 10%（20 MB）
🎮 游戏正在运行中，如需中止请点顶栏的「取消启动」   ← 之后一直重复这一行
```

下载在 10% 之后就没有进展，而这条链路**没有超时**，所以启动流程永远卡在「准备环境」，
界面就一直是「运行中」。而且每次点击启动又会再排一个下载，日志里越堆越多。

## 三个修复

### 1. 优先用本机已装的 JDK（不再为启动游戏硬下 200MB）

`JavaManager` 以前只看 `game/java/zulu<major>/`，本机装了 JDK 也不认。现在按顺序扫描：

1. 运行启动器的那个 JVM（`java.home`）
2. `JAVA_HOME`
3. `C:\Program Files\Java`、`Eclipse Adoptium`、`Zulu`、`Microsoft`、`BellSoft`、`Amazon Corretto`、`Program Files (x86)\Java`
4. `%LOCALAPPDATA%\Programs\Eclipse Adoptium`

只认 `release` 里 `JAVA_VERSION` **大版本完全匹配**的（不会拿 JDK 8 跑 1.21），CRaC 实验版跳过，
同版本取版本号最大的。实测：空的游戏目录 + 本机 `jdk-21.0.11` →

```
✅ 使用本机已安装的 Java 21：C:\Program Files\Java\jdk-21.0.11\bin\java.exe
```

### 2. 下载看门狗：60 秒没进度就换源

下载改成可观测的循环：每 2 秒看一次 zip 大小，**60 秒没有增长**就判定卡死，
删掉半截文件、抛错并自动切到下一个源（Azul → Adoptium）；两个源都不行就明确报错、
把启动状态复位，而不是永远「运行中」。

进度打印从 10% 一档改成 5% 一档，日志里能看到它到底在动还是停了。

### 3. 连点防刷屏

`🎮 游戏正在运行中…` 现在 5 秒内只写一次，不会把日志刷满。

## 你现在该做什么

1. 装 2.29.0；
2. 设置 → Java 里如果填过路径，确认那个 `java.exe` 是 21 的（1.21.11 需要 21）；
3. 再点启动 —— 正常情况下日志会直接出现 `✅ 使用本机已安装的 Java 21`，
   然后进游戏；如果确实没有 21，才会下载，并且卡住会自动换源报错。

## 测试

**330 项全绿**（+3）：大版本匹配、CRaC 跳过并选最新、缺 `release` 视为未安装。

## 校验

```
3ba568090e3226eaeb92f2cbad43894e7f4f7947c18be1e207e51e30a25c9fb5  NekoCraftLauncher-2.29.0.exe
c78b18f859f052cddeac5fc6a338600a3e725dc912213951790eb6acedadaf1f  NekoCraftLauncher-2.29.0-win64.zip
2aeea95e97f792fc808f91afb682a2af84207b92a66692fc305182bf4f6e6676  NekoCraftLauncher-2.29.0-Features.pdf
641fe14eb278b7b6ca1a3bf395372723a2feaf392a19995c6cd6a6c699d13c7a  NekoCraftLauncher-2.29.0-Features.docx
41bdc7658bd42ee52d59d81ccd541597b7deac9e600715ab23327227a73cdb89  neko-codesign.cer
7fb7c27a18da560ce423415efaa525251bc6009fbe6e52d576710ccf8d19b439  verify.txt
```
