package com.example.demo.core.auth;

import com.example.demo.core.model.UserSession;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * 微软账号登录（设备码流程）。
 *
 * <p>六步：申请设备码 → 轮询令牌 → Xbox Live → XSTS → Minecraft → 获取档案。
 * 报文格式与解析都在 {@link MicrosoftAuthProtocol}，选线路与失败切换在
 * {@link ResilientAuthClient}，这里只负责把六步串起来。
 *
 * <p>线路策略：微软 OAuth 有三个公开主机别名，登录前并行探测一次，用手测最快的那条；
 * 任何一步的连接失败 / 5xx / 应用身份不匹配都会自动换下一条，并写进日志。
 */
public final class MicrosoftAuth {

    /** 单次请求超时（秒）。登录链路的每一步都不该拖过这个时间。 */
    private static final int TIMEOUT_SECONDS = 20;

    /** 设备码的默认有效期上限，防止用户在别处授权后进程一直轮询。 */
    private static final long DEVICE_CODE_MAX_WAIT_MS = 15 * 60_000L;

    private MicrosoftAuth() {
    }

    public static CompletableFuture<UserSession> loginAsync(Consumer<String> log) {
        return CompletableFuture.supplyAsync(() -> {
            ResilientAuthClient client = ResilientAuthClient.defaults(log);
            try {
                IssuedDeviceCode issued = requestDeviceCode(client, log);
                String microsoftToken = waitForToken(client, issued, log);

                // 3. Xbox Live
                ResilientAuthClient.Result xbl = client.postJson(AuthStage.XBOX_LIVE,
                        MicrosoftAuthProtocol.xboxLiveBody(microsoftToken), TIMEOUT_SECONDS);
                requireOk(xbl, "Xbox Live 认证");
                MicrosoftAuthProtocol.XboxLive xboxLive =
                        MicrosoftAuthProtocol.parseXboxLive(xbl.body());
                log.accept("✅ Xbox Live 认证成功（" + xbl.endpoint().label() + " · "
                        + xbl.latencyMs() + "ms）");

                // 4. XSTS
                ResilientAuthClient.Result xsts = client.postJson(AuthStage.XSTS,
                        MicrosoftAuthProtocol.xstsBody(xboxLive.token()), TIMEOUT_SECONDS);
                requireOk(xsts, "XSTS 认证");
                String xstsToken = MicrosoftAuthProtocol.parseXstsToken(xsts.body());
                log.accept("✅ XSTS 认证成功（" + xsts.latencyMs() + "ms）");

                // 5. Minecraft
                ResilientAuthClient.Result mc = client.postJson(AuthStage.MINECRAFT_LOGIN,
                        MicrosoftAuthProtocol.minecraftBody(xboxLive.uhs(), xstsToken), TIMEOUT_SECONDS);
                requireOk(mc, "Minecraft 认证");
                String minecraftToken = MicrosoftAuthProtocol.parseMinecraftToken(mc.body());
                log.accept("✅ Minecraft 认证成功（" + mc.latencyMs() + "ms）");

                // 6. 档案
                ResilientAuthClient.Result profileResult = client.get(AuthStage.PROFILE,
                        "Bearer " + minecraftToken, TIMEOUT_SECONDS);
                requireOk(profileResult, "获取玩家档案");
                MicrosoftAuthProtocol.Profile profile =
                        MicrosoftAuthProtocol.parseProfile(profileResult.body());

                UserSession session = new UserSession();
                session.setType(UserSession.LoginType.MICROSOFT);
                session.setUsername(profile.name());
                session.setUuid(profile.uuid());
                session.setAccessToken(minecraftToken);
                session.setUserType("msa");

                log.accept("🎉 登录成功: " + session.getUsername());
                return session;
            } catch (Exception e) {
                log.accept("❌ 登录失败: " + e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }

    /** 设备码 + 发码时用的 client id（轮询必须用同一个）。 */
    private record IssuedDeviceCode(MicrosoftAuthProtocol.DeviceCode code, String clientId) {
    }

    /** 第一步：申请设备码。线路由 {@link ResilientAuthClient} 选，client id 与线路成对。 */
    private static IssuedDeviceCode requestDeviceCode(ResilientAuthClient client,
                                                      Consumer<String> log) {
        ResilientAuthClient.Result result = client.postForm(AuthStage.DEVICE_CODE,
                MicrosoftAuthProtocol::deviceCodeBody, null, TIMEOUT_SECONDS);
        if (!result.ok()) {
            throw new IllegalStateException("获取设备码失败（HTTP " + result.status() + "）："
                    + MicrosoftAuthProtocol.pollError(result.body()));
        }
        MicrosoftAuthProtocol.DeviceCode device = MicrosoftAuthProtocol.parseDeviceCode(result.body());
        log.accept("🛰 登录线路：" + result.endpoint().label() + "（" + result.latencyMs() + "ms）");
        log.accept("🔗 请访问: " + device.verificationUri());
        log.accept("🔑 输入代码: " + device.userCode());
        log.accept("⏳ 等待授权中...");
        try {
            java.awt.Desktop.getDesktop().browse(URI.create(device.verificationUri()));
        } catch (Exception ignored) {
            // 浏览器打不开时仍可在日志里手动访问
        }
        return new IssuedDeviceCode(device, result.endpoint().clientId());
    }

    /**
     * 第二步：轮询令牌。
     *
     * <p>设备码是跟 client id 绑定的，所以这里锁定发码时的 client id；主机仍可在别名之间切换，
     * 因为三个别名指向同一套 OAuth 服务。
     */
    private static String waitForToken(ResilientAuthClient client,
                                       IssuedDeviceCode issued,
                                       Consumer<String> log) throws InterruptedException {
        MicrosoftAuthProtocol.DeviceCode device = issued.code();
        String pinnedClientId = issued.clientId();
        long deadline = System.currentTimeMillis() + DEVICE_CODE_MAX_WAIT_MS;
        long intervalMs = Math.max(1, device.interval()) * 1000L;
        while (System.currentTimeMillis() < deadline) {
            Thread.sleep(intervalMs);
            ResilientAuthClient.Result result = client.postForm(AuthStage.TOKEN,
                    clientId -> MicrosoftAuthProtocol.tokenBody(clientId, device.deviceCode()),
                    pinnedClientId, TIMEOUT_SECONDS);
            MicrosoftAuthProtocol.PollResult poll = MicrosoftAuthProtocol.pollResult(result.body());
            if (poll == MicrosoftAuthProtocol.PollResult.TOKEN) {
                log.accept("✅ 微软账号授权成功");
                return MicrosoftAuthProtocol.accessToken(result.body());
            }
            // 先判 slow_down：它同样带 error 字段，但含义是「问得太勤」，不是失败
            if ("slow_down".equals(errorCode(result.body()))) {
                intervalMs += 2000L;
                log.accept("⏳ 授权服务要求放慢轮询，间隔调整为 " + (intervalMs / 1000) + " 秒");
                continue;
            }
            if (poll == MicrosoftAuthProtocol.PollResult.ERROR) {
                throw new IllegalStateException(
                        "微软授权失败: " + MicrosoftAuthProtocol.pollError(result.body()));
            }
        }
        throw new IllegalStateException("设备码已过期（15 分钟），请重新发起登录");
    }

    private static String errorCode(String body) {
        if (body == null) {
            return "";
        }
        int index = body.indexOf("\"error\"");
        if (index < 0) {
            return "";
        }
        int colon = body.indexOf(':', index);
        int quote = body.indexOf('"', colon + 1);
        int end = body.indexOf('"', quote + 1);
        return quote > 0 && end > quote ? body.substring(quote + 1, end) : "";
    }

    private static void requireOk(ResilientAuthClient.Result result, String step) {
        if (!result.ok()) {
            throw new IllegalStateException(step + "失败（HTTP " + result.status() + "）：" + result.body());
        }
    }
}
