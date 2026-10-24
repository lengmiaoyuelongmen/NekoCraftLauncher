package com.example.demo;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class MicrosoftAuth {
    // 微软公开的 Minecraft Client ID
    private static final String CLIENT_ID = "00000000402b5328";
    private static final String SCOPE = "XboxLive.signin offline_access";
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final Gson gson = new Gson();

    public static CompletableFuture<UserSession> loginAsync(Consumer<String> log) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 1. 请求设备代码
                String deviceCodeBody = "client_id=" + CLIENT_ID + "&scope=" + SCOPE.replace(" ", "%20");
                HttpRequest deviceReq = HttpRequest.newBuilder()
                        .uri(URI.create("https://login.microsoftonline.com/consumers/oauth2/v2.0/devicecode"))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(deviceCodeBody))
                        .build();
                HttpResponse<String> deviceResp = client.send(deviceReq, HttpResponse.BodyHandlers.ofString());
                JsonObject deviceJson = gson.fromJson(deviceResp.body(), JsonObject.class);

                if (!deviceJson.has("device_code")) {
                    throw new RuntimeException("获取设备码失败: " + deviceResp.body());
                }

                String deviceCode = deviceJson.get("device_code").getAsString();
                String userCode = deviceJson.get("user_code").getAsString();
                String verificationUri = deviceJson.get("verification_uri").getAsString();
                int interval = deviceJson.get("interval").getAsInt();

                log.accept("🔗 请访问: " + verificationUri);
                log.accept("🔑 输入代码: " + userCode);
                log.accept("⏳ 等待授权中...");
                try {
                    java.awt.Desktop.getDesktop().browse(URI.create(verificationUri));
                } catch (Exception ignored) {
                    // 浏览器打不开时仍可在日志里手动访问
                }

                // 2. 轮询获取 Microsoft Token
                String msAccessToken = null;
                while (msAccessToken == null) {
                    Thread.sleep(interval * 1000L);
                    String tokenBody = "client_id=" + CLIENT_ID
                            + "&device_code=" + deviceCode
                            + "&grant_type=urn:ietf:params:oauth:grant-type:device_code";
                    HttpRequest tokenReq = HttpRequest.newBuilder()
                            .uri(URI.create("https://login.microsoftonline.com/consumers/oauth2/v2.0/token"))
                            .header("Content-Type", "application/x-www-form-urlencoded")
                            .POST(HttpRequest.BodyPublishers.ofString(tokenBody))
                            .build();
                    HttpResponse<String> tokenResp = client.send(tokenReq, HttpResponse.BodyHandlers.ofString());
                    JsonObject tokenJson = gson.fromJson(tokenResp.body(), JsonObject.class);

                    if (tokenJson.has("access_token")) {
                        msAccessToken = tokenJson.get("access_token").getAsString();
                    } else if (tokenJson.has("error") && !tokenJson.get("error").getAsString().equals("authorization_pending")) {
                        throw new RuntimeException("微软授权失败: " + tokenJson.get("error_description").getAsString());
                    }
                }
                log.accept("✅ 微软账号授权成功");

                // 3. Xbox Live 认证
                String xblBody = "{\"Properties\":{\"AuthMethod\":\"RPS\",\"SiteName\":\"user.auth.xboxlive.com\",\"RpsTicket\":\"d=" + msAccessToken + "\"},\"RelyingParty\":\"http://auth.xboxlive.com\",\"TokenType\":\"JWT\"}";
                String xblResponse = postJson("https://user.auth.xboxlive.com/user/authenticate", xblBody);
                JsonObject xblJson = gson.fromJson(xblResponse, JsonObject.class);

                if (!xblJson.has("Token")) {
                    throw new RuntimeException("Xbox Live 认证失败，响应异常: " + xblResponse);
                }
                String xblToken = xblJson.get("Token").getAsString();

                // 安全提取 UHS
                String uhs = null;
                if (xblJson.has("DisplayClaims") && !xblJson.get("DisplayClaims").isJsonNull()) {
                    JsonObject displayClaims = xblJson.getAsJsonObject("DisplayClaims");
                    if (displayClaims.has("xui") && !displayClaims.get("xui").isJsonNull()) {
                        JsonArray xuiArray = displayClaims.getAsJsonArray("xui");
                        if (xuiArray != null && xuiArray.size() > 0) {
                            JsonObject firstXui = xuiArray.get(0).getAsJsonObject();
                            if (firstXui.has("uhs") && !firstXui.get("uhs").isJsonNull()) {
                                uhs = firstXui.get("uhs").getAsString();
                            }
                        }
                    }
                }

                if (uhs == null || uhs.isEmpty()) {
                    throw new RuntimeException("无法解析 Xbox Live UHS，响应: " + xblResponse);
                }
                log.accept("✅ Xbox Live 认证成功");

                // 4. XSTS 认证
                String xstsBody = "{\"Properties\":{\"SandboxId\":\"RETAIL\",\"UserTokens\":[\"" + xblToken + "\"]},\"RelyingParty\":\"rp://api.minecraftservices.com/\",\"TokenType\":\"JWT\"}";
                String xstsResponse = postJson("https://xsts.auth.xboxlive.com/xsts/authorize", xstsBody);
                JsonObject xstsJson = gson.fromJson(xstsResponse, JsonObject.class);

                if (xstsJson.has("XErr")) {
                    long xerr = xstsJson.get("XErr").getAsLong();
                    String reason;
                    // 【核心修复】使用 long 类型的 if-else 判断，避免 int 溢出
                    if (xerr == 2148916233L) {
                        reason = "该微软账号没有注册 Xbox 账户";
                    } else if (xerr == 2148916238L) {
                        reason = "该账号未成年，需要成人账号将其加入家庭组";
                    } else if (xerr == 2148916235L) {
                        reason = "该账号所在地区不支持 Xbox Live";
                    } else {
                        reason = "未知原因 (错误码: " + xerr + ")";
                    }
                    throw new RuntimeException("XSTS 授权失败 [XErr=" + xerr + "]: " + reason);
                }

                if (!xstsJson.has("Token")) {
                    throw new RuntimeException("XSTS 认证失败，响应异常: " + xstsResponse);
                }
                String xstsToken = xstsJson.get("Token").getAsString();
                log.accept("✅ XSTS 认证成功");

                // 5. Minecraft 认证
                String mcBody = "{\"identityToken\":\"XBL3.0 x=" + uhs + ";" + xstsToken + "\"}";
                String mcResponse = postJson("https://api.minecraftservices.com/authentication/login_with_xbox", mcBody);
                JsonObject mcJson = gson.fromJson(mcResponse, JsonObject.class);

                if (!mcJson.has("access_token")) {
                    throw new RuntimeException("Minecraft 认证失败: " + mcResponse);
                }
                String mcAccessToken = mcJson.get("access_token").getAsString();
                log.accept("✅ Minecraft 认证成功");

                // 6. 获取档案
                HttpRequest profileReq = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.minecraftservices.com/minecraft/profile"))
                        .header("Authorization", "Bearer " + mcAccessToken)
                        .GET()
                        .build();
                HttpResponse<String> profileResp = client.send(profileReq, HttpResponse.BodyHandlers.ofString());
                JsonObject profileJson = gson.fromJson(profileResp.body(), JsonObject.class);

                if (!profileJson.has("name")) {
                    throw new RuntimeException("获取玩家信息失败，请检查该账号是否已购买 Minecraft! 响应: " + profileResp.body());
                }

                UserSession session = new UserSession();
                session.setType(UserSession.LoginType.MICROSOFT);
                session.setUsername(profileJson.get("name").getAsString());

                // 将无符号 UUID 转换回带连字符的标准格式
                String rawUuid = profileJson.get("id").getAsString();
                String formattedUuid = rawUuid.replaceFirst(
                        "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)",
                        "$1-$2-$3-$4-$5");
                session.setUuid(UUID.fromString(formattedUuid));
                session.setAccessToken(mcAccessToken);
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
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

        // 如果 HTTP 状态码不是 200，说明请求彻底失败，直接把错误内容抛出来
        if (resp.statusCode() != 200 && resp.statusCode() != 201) {
            throw new RuntimeException("HTTP " + resp.statusCode() + " 错误: " + resp.body());
        }
        return resp.body();
    }
}