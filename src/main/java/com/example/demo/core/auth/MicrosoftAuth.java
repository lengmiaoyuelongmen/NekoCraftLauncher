package com.example.demo.core.auth;

import com.example.demo.core.model.UserSession;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * 微软账号登录（设备码流程）。
 *
 * <p>六步：申请设备码 → 轮询令牌 → Xbox Live → XSTS → Minecraft → 获取档案。
 * 报文格式与解析都在 {@link MicrosoftAuthProtocol}，这里只负责发请求和串流程。
 */
public final class MicrosoftAuth {

    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    private MicrosoftAuth() {
    }

    public static CompletableFuture<UserSession> loginAsync(Consumer<String> log) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 1. 设备码
                HttpRequest deviceReq = HttpRequest.newBuilder()
                        .uri(URI.create(MicrosoftAuthProtocol.deviceCodeUrl()))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(MicrosoftAuthProtocol.deviceCodeBody()))
                        .build();
                HttpResponse<String> deviceResp =
                        CLIENT.send(deviceReq, HttpResponse.BodyHandlers.ofString());
                MicrosoftAuthProtocol.DeviceCode device =
                        MicrosoftAuthProtocol.parseDeviceCode(deviceResp.body());

                log.accept("🔗 请访问: " + device.verificationUri());
                log.accept("🔑 输入代码: " + device.userCode());
                log.accept("⏳ 等待授权中...");
                try {
                    java.awt.Desktop.getDesktop().browse(URI.create(device.verificationUri()));
                } catch (Exception ignored) {
                    // 浏览器打不开时仍可在日志里手动访问
                }

                // 2. 轮询令牌
                String microsoftToken = null;
                while (microsoftToken == null) {
                    Thread.sleep(Math.max(1, device.interval()) * 1000L);
                    HttpRequest tokenReq = HttpRequest.newBuilder()
                            .uri(URI.create(MicrosoftAuthProtocol.tokenUrl()))
                            .header("Content-Type", "application/x-www-form-urlencoded")
                            .POST(HttpRequest.BodyPublishers.ofString(
                                    MicrosoftAuthProtocol.tokenBody(device.deviceCode())))
                            .build();
                    HttpResponse<String> tokenResp =
                            CLIENT.send(tokenReq, HttpResponse.BodyHandlers.ofString());
                    MicrosoftAuthProtocol.PollResult result =
                            MicrosoftAuthProtocol.pollResult(tokenResp.body());
                    if (result == MicrosoftAuthProtocol.PollResult.TOKEN) {
                        microsoftToken = MicrosoftAuthProtocol.accessToken(tokenResp.body());
                    } else if (result == MicrosoftAuthProtocol.PollResult.ERROR) {
                        throw new IllegalStateException(
                                "微软授权失败: " + MicrosoftAuthProtocol.pollError(tokenResp.body()));
                    }
                }
                log.accept("✅ 微软账号授权成功");

                // 3. Xbox Live
                String xblResponse = postJson("https://user.auth.xboxlive.com/user/authenticate",
                        MicrosoftAuthProtocol.xboxLiveBody(microsoftToken));
                MicrosoftAuthProtocol.XboxLive xboxLive =
                        MicrosoftAuthProtocol.parseXboxLive(xblResponse);
                log.accept("✅ Xbox Live 认证成功");

                // 4. XSTS
                String xstsResponse = postJson("https://xsts.auth.xboxlive.com/xsts/authorize",
                        MicrosoftAuthProtocol.xstsBody(xboxLive.token()));
                String xstsToken = MicrosoftAuthProtocol.parseXstsToken(xstsResponse);
                log.accept("✅ XSTS 认证成功");

                // 5. Minecraft
                String mcResponse = postJson(
                        "https://api.minecraftservices.com/authentication/login_with_xbox",
                        MicrosoftAuthProtocol.minecraftBody(xboxLive.uhs(), xstsToken));
                String minecraftToken = MicrosoftAuthProtocol.parseMinecraftToken(mcResponse);
                log.accept("✅ Minecraft 认证成功");

                // 6. 档案
                HttpRequest profileReq = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.minecraftservices.com/minecraft/profile"))
                        .header("Authorization", "Bearer " + minecraftToken)
                        .GET()
                        .build();
                HttpResponse<String> profileResp =
                        CLIENT.send(profileReq, HttpResponse.BodyHandlers.ofString());
                MicrosoftAuthProtocol.Profile profile =
                        MicrosoftAuthProtocol.parseProfile(profileResp.body());

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

    private static String postJson(String url, String body) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200 && resp.statusCode() != 201) {
            throw new IllegalStateException("HTTP " + resp.statusCode() + " 错误: " + resp.body());
        }
        return resp.body();
    }
}
