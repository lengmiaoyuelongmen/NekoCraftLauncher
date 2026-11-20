# 正版登录线路：多接口、先检测、失败自动切换

> 适用范围：Neko Craft Launcher 2.9.1+（`core/auth`）
> 最后实测：2026-11-20，本机（Windows 11 / 中国电信网络）

## 1. 原来的问题和它的根因

启动器一直在用的设备码 client id 是微软账号旧版应用：

```
00000000402b5328
```

实测（直接把请求打到 `/consumers/oauth2/v2.0/devicecode`）：

```json
{"error":"unauthorized_client",
 "error_description":"AADSTS700016: Application with identifier '00000000402b5328'
 was not found in the directory '9188040d-6c67-4c5b-b112-36a304b66dad'."}
```

也就是说：**这条登录链路今天已经发不出设备码**，用户看到的是「获取设备码失败」，
和网络好坏无关。同类公共 client id 里，`000000004C12AE6F`、`62242856-…` 也都被拒。

## 2. 现在的做法

```
设备码 ─┐
        ├─ 微软 OAuth 三条主机别名（同一套服务，独立解析/限流）
令牌轮询 ┘       login.microsoftonline.com · login.microsoft.com · login.windows.net
Xbox Live ──── user.auth.xboxlive.com（官方唯一）
XSTS ───────── xsts.auth.xboxlive.com（官方唯一）
Minecraft ──── api.minecraftservices.com（官方唯一）
档案 ───────── api.minecraftservices.com/minecraft/profile
```

每次登录按三步走：

1. **先检测**：对没测过的线路并行发一次 4 秒超时的 GET 探测。只要对方回了 HTTP（哪怕是 400/405）
   就算这条线路活着，并记下真实延迟；
2. **再使用**：健康表按「实测最快 → 没测过（人工优先级）→ 冷却中」排序，用排第一的线路发真实请求；
3. **失败切换**：连接失败 / 超时 / 5xx / 429 / `AADSTS700016` / `unauthorized_client`
   都算线路不可用 —— 标记冷却 60 秒并立刻换下一条；`authorization_pending`
   （用户还没授权）、`XErr`（账号没买游戏/未成年）这类**业务结果不换线**，原样交给上层解释。

设备码与轮询会**锁定同一个 client id**（同一份设备码不能换个 app 去问），
但主机仍可在三个别名之间切换 —— 这才是「一个失效自动切换另一个」的正确粒度。

## 3. 实测数据（本机，2026-11-20 04:11）

命令栏输入 `线路`（别名 `auth` / `line`）得到的真实探测结果：

| 阶段 | 线路 | 延迟 |
|---|---|---|
| 设备码 | login.microsoftonline.com | 361 ms |
| 设备码 | login.microsoft.com | 343 ms |
| 设备码 | login.windows.net | **273 ms** |
| 令牌轮询 | login.microsoftonline.com | 176 ms |
| 令牌轮询 | login.microsoft.com | 217 ms |
| 令牌轮询 | login.windows.net | **149 ms** |
| Xbox Live | user.auth.xboxlive.com | 262 ms |
| XSTS | xsts.auth.xboxlive.com | 582 ms |
| Minecraft 登录 | api.minecraftservices.com | 314 ms |
| 玩家档案 | api.minecraftservices.com | 291 ms |

合计 **10/10 条可用**，登录时首选 `login.windows.net`（当次最快）。

顺带把「发得出设备码」也验了：真实请求返回 HTTP 200，拿到 `user_code` 与
`verification_uri=https://www.microsoft.com/link`（未进行用户授权，属预期）。

被探到不可用的历史候选：`user.auth.xboxlive.cn` / `xsts.auth.xboxlive.cn`
（DNS 直接失败，未纳入清单）。

## 4. 怎么扩展线路

不改代码就能加：

```bat
rem 追加公共 client id（逗号或分号分隔）
set JAVA_TOOL_OPTIONS=-Dmc.auth.extraClientIds=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx

rem 追加微软 OAuth 主机别名
set JAVA_TOOL_OPTIONS=-Dmc.auth.extraHosts=login.example-mirror.net
```

代码入口在 `core/auth/AuthEndpoints.java`：

```java
public static final String DEFAULT_CLIENT_ID = "88650e7e-efee-4857-b9a9-cf580a00ef43";
public static final List<String> MICROSOFT_HOSTS = List.of(
        "login.microsoftonline.com", "login.microsoft.com", "login.windows.net");
```

## 5. 结构

| 类 | 职责 |
|---|---|
| `AuthStage` / `AuthEndpoint` / `AuthEndpoints` | 阶段、线路、候选清单 |
| `EndpointHealth` | 健康表：10 分钟有效期、60 秒冷却、按实测延迟排序（时间是注入的，可单测） |
| `AuthTransport` / `JdkAuthTransport` | HTTP 抽象（连接 6s / 请求 20s 超时），可换成假传输层 |
| `ResilientAuthClient` | 探测 → 排序 → 请求 → 失败换线 → 汇总失败原因 |
| `MicrosoftAuth` | 六步流程串联，日志里写明用了哪条线路、切了几次 |
| `LauncherCommands` | 命令面板的 `线路` / `auth` / `line` |

## 6. 测试

新增 22 项单测（全部不联网）：

- `AuthEndpointsTest`：候选生成、client id × 主机组合、系统属性追加、按 client id 过滤、
  以及「默认 client id 不能是被拒绝的那个」；
- `EndpointHealthTest`：延迟排序、失败冷却、冷却结束复活、结果过期、失败计数；
- `ResilientAuthClientTest`：探测后用最快、连接异常换线、5xx 换线、AADSTS700016 换线、
  `authorization_pending` 不换线、`XErr` 不换线、单线路重试三次、全失败时列出每条线路原因、
  轮询锁定 client id。

```
mvnw test  →  Tests run: 278, Failures: 0, Errors: 0
```

## 7. 已知限制

- **Xbox / Minecraft 只有官方单一主机**，没有可切换的公开等价接口：这两步靠「同址重试 3 次」，
  真被墙时只能走系统代理，不是换 URL 能解决的；
- 默认 client id 是开源社区公开的公共应用，微软理论上可以回收 —— 所以做成了可配置，
  失效时用 `-Dmc.auth.extraClientIds=` 追加即可，不用改代码；
- 健康表只存在内存里，重启后重新探测（≈0.6 秒），不做持久化以免留下过期判断。
