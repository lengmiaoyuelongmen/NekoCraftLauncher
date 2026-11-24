# Neko Craft Launcher 2.51.0

> 首帧耗时有真实拆分了，背包编辑器支持附魔与物品色块，设置页文案继续入 i18n。

## 安装包

| 文件 | 说明 |
|---|---|
| `NekoCraftLauncher-2.51.0.exe` | **安装版**：jpackage/WiX，静默参数 `/quiet` |
| `NekoCraftLauncher-2.51.0-win64.zip` | 免安装版；支持就地更新、回退与备份管理 |
| `NekoCraftLauncher-2.51.0-Features.pdf` / `.docx` | 功能预览（图文版） |
| `SHA256SUMS.txt` / `verify.txt` / `neko-codesign.cer` | 校验与签名材料 |

## 1. 首帧三段计时（终于知道慢在哪了）

新增日志两行，本机实测：

```
TIMING startEnter=1258ms rootBuilt=2345ms sceneMs=121
TIMING config=142ms scene=121ms firstPage=4901ms total=5164ms
STARTUP first-frame 5164 ms
```

拆解：

| 阶段 | 耗时 | 说明 |
|---|---|---|
| 配置加载 | 142 ms | `LauncherConfig.load()` |
| JVM + JavaFX 启动 | ≈1258 ms | 到 `Application.start()` 入口，属于 JVM 与 toolkit 启动，进程内改不动 |
| UI 构建 | ≈1087 ms | `start()` 入口 → `rootWrap` 建完（2324-1258） |
| Scene 装配 | 121 ms | `createScene`（CSS 应用、加速器注册） |
| **首帧渲染** | **≈2700 ms** | `stage.show()` 到真正画出来 |

结论：真正的大头是**首帧渲染（约 2.7 秒）**，不是配置也不是 UI 构建 —— 之前的猜测被数据否掉了。

### 顺带做的一个改动

启动页概览的历史 / 崩溃报告读取原本在构造时同步执行，现在延后到首帧之后：

- 改动前 first-frame 5252 ms → 改动后 5155 ms（**-97 ms**）；
- 幅度不大，但方向对：磁盘 IO 不该挡第一屏。

下一步该拿工具看 CSS 与特效（`app.css` 里大量 shadow/渐变 + 首屏节点数量），而不是继续凭感觉改代码。

## 2. 背包编辑器增强

- **物品色块**：没有材质包也能分辨 —— 按物品 id 的哈希生成稳定颜色 + 首字母，24px 方块；
- **附魔编辑**：每个槽位多了「附魔」按钮，列出已有附魔（等级 0 = 移除），可新增
  `minecraft:unbreaking` 这类条目（不写命名空间自动补 `minecraft:`）；
- 附魔写回同样**先备份**，且只动 `tag.Enchantments`，物品其它标签（耐久、自定义名）不受影响；
- 清空附魔会把空的 `tag` 一并去掉，不留下无意义的空 Compound。

## 3. i18n

设置页「启动」组落地：窗口尺寸、游戏内存、最大/最小内存、自动配置/自定义、游戏目录、Java 路径、浏览按钮等 9 条。

基线 **2194 → 2185**（本轮 -9，累计 -42）。

## 测试

`PlayerInventoryNbtTest` 增到 **4 条**：新增「附魔写入 → 读回 → 清空 → 其它槽位不受影响」。
全套 **347 项单测全绿**，无头冒烟六页全过。

## 校验

```
b56de2ddc16a9cf47c945f7adc74073fc5a8dab7b5ca6d3c94aee945d6ec2da4  NekoCraftLauncher-2.51.0.exe
53b03eef2cc0cb7a872d667491035d3af276fca8a43d2b5d63691815b92aa82a  NekoCraftLauncher-2.51.0-win64.zip
87870405b1df89bf54e5d44ef8cbe23685cfbc529274624461e79de86ef98ea1  NekoCraftLauncher-2.51.0-Features.pdf
63cd1999842cd387985ea5ace043c078e70ba48ad2f1720a4c7cd308a7c168ca  NekoCraftLauncher-2.51.0-Features.docx
41bdc7658bd42ee52d59d81ccd541597b7deac9e600715ab23327227a73cdb89  neko-codesign.cer
a5985b4ea816753043a9a74ce57f93e2838d3148eb83a21ab437216d07cbac20  verify.txt
```
