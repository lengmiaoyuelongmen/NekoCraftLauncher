package com.example.demo;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class AuthManager {
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public static CompletableFuture<UserSession> loginAsync(String type, String username, String password, String authServerUrl, Consumer<String> log) {
        if (type.equals("正版登录 (微软)")) {
            return MicrosoftAuth.loginAsync(log);
        }

        return CompletableFuture.supplyAsync(() -> {
            UserSession session = new UserSession();
            if (type.equals("离线登录")) {
                session.setType(UserSession.LoginType.OFFLINE);
                session.setUsername(username);
                session.setUuid(UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes()));
                session.setAccessToken("0");
                session.setUserType("legacy");
                log.accept("✅ 离线登录成功");
            } else if (type.equals("第三方外置登录")) {
                String normalizedUrl = normalizeAuthServerUrl(authServerUrl);
                log.accept("自动校准验证服务器 API 为: " + normalizedUrl);

                try {
                    JsonObject payload = new JsonObject();
                    payload.addProperty("username", username);
                    payload.addProperty("password", password);
                    payload.addProperty("clientToken", UUID.randomUUID().toString());
                    JsonObject agent = new JsonObject();
                    agent.addProperty("name", "Minecraft");
                    agent.addProperty("version", 1);
                    payload.add("agent", agent);

                    HttpRequest req = HttpRequest.newBuilder()
                            .uri(URI.create(normalizedUrl + "/authserver/authenticate"))
                            .header("Content-Type", "application/json")
                            .timeout(Duration.ofSeconds(10))
                            .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                            .build();

                    int maxRetries = 5;
                    HttpResponse<String> resp = null;
                    for (int i = 1; i <= maxRetries; i++) {
                        try {
                            resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                            break;
                        } catch (Exception e) {
                            if (i == maxRetries) {
                                throw new RuntimeException("连接验证服务器失败 (已重试5次): " + e.getMessage());
                            }
                            log.accept("⚠️ 连接超时，正在重试 (" + i + "/5)...");
                            Thread.sleep(1500);
                        }
                    }

                    if (resp != null && resp.statusCode() == 200) {
                        JsonObject respJson = new Gson().fromJson(resp.body(), JsonObject.class);
                        String accessToken = respJson.get("accessToken").getAsString();
                        JsonObject profile = respJson.getAsJsonObject("selectedProfile");

                        session.setType(UserSession.LoginType.THIRD_PARTY);
                        session.setUsername(profile.get("name").getAsString());

                        String rawUuid = profile.get("id").getAsString();
                        String formattedUuid = rawUuid.replaceFirst(
                                "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)",
                                "$1-$2-$3-$4-$5");
                        session.setUuid(UUID.fromString(formattedUuid));
                        session.setAccessToken(accessToken);
                        session.setUserType("mojang");
                        session.setAuthServerUrl(normalizedUrl);
                        log.accept("✅ 外置登录成功: " + session.getUsername());

                        // ==================== 二次请求：获取完整档案（皮肤） ====================
                        log.accept("🔍 正在获取完整角色档案以同步皮肤...");
                        try {
                            String sessionServerUrl = normalizedUrl + "/sessionserver/session/minecraft/profile/" + rawUuid;
                            HttpRequest profileReq = HttpRequest.newBuilder()
                                    .uri(URI.create(sessionServerUrl))
                                    .GET()
                                    .build();

                            HttpResponse<String> profileResp = client.send(profileReq, HttpResponse.BodyHandlers.ofString());
                            if (profileResp.statusCode() == 200) {
                                JsonObject fullProfile = new Gson().fromJson(profileResp.body(), JsonObject.class);
                                if (fullProfile.has("properties") && fullProfile.get("properties").isJsonArray()) {
                                    JsonArray props = fullProfile.getAsJsonArray("properties");
                                    for (int i = 0; i < props.size(); i++) {
                                        JsonObject prop = props.get(i).getAsJsonObject();
                                        if (prop.has("name") && "textures".equals(prop.get("name").getAsString())) {
                                            String base64Value = prop.get("value").getAsString();
                                            String decoded = new String(Base64.getDecoder().decode(base64Value));
                                            log.accept("📄 获取到皮肤数据: " + decoded);

                                            JsonObject texturesJson = new Gson().fromJson(decoded, JsonObject.class);
                                            if (texturesJson.has("textures")) {
                                                JsonObject textures = texturesJson.getAsJsonObject("textures");
                                                if (textures.has("SKIN")) {
                                                    JsonObject skin = textures.getAsJsonObject("SKIN");
                                                    if (skin.has("url")) {
                                                        session.setSkinUrl(skin.get("url").getAsString());
                                                        log.accept("🎨 成功提取皮肤 URL: " + session.getSkinUrl());
                                                    }
                                                }
                                            }
                                            break;
                                        }
                                    }
                                } else {
                                    log.accept("⚠️ 档案中未包含 properties，账号可能未绑定皮肤");
                                }
                            } else {
                                log.accept("⚠️ 获取角色档案失败，HTTP 状态码: " + profileResp.statusCode());
                            }
                        } catch (Exception e) {
                            log.accept("❌ 获取角色档案异常: " + e.getMessage());
                        }
                        // ========================================================================

                    } else if (resp != null) {
                        throw new RuntimeException("服务器拒绝了认证 (" + resp.statusCode() + "): " + resp.body());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("登录被中断");
                } catch (Exception e) {
                    throw new RuntimeException("第三方认证失败: " + e.getMessage());
                }
            }
            return session;
        });
    }

    private static String normalizeAuthServerUrl(String inputUrl) {
        if (inputUrl == null || inputUrl.trim().isEmpty()) return "";
        String url = inputUrl.trim();
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }
        if (url.endsWith("/")) url = url.substring(0, url.length() - 1);
        if (url.contains("littleskin.cn")) return "https://littleskin.cn/api/yggdrasil";
        if (!url.contains("/api/yggdrasil")) {
            int userIndex = url.indexOf("/user");
            if (userIndex != -1) url = url.substring(0, userIndex);
            try {
                java.net.URI uri = new java.net.URI(url);
                String scheme = uri.getScheme();
                String host = uri.getHost();
                if (host != null) url = scheme + "://" + host + "/api/yggdrasil";
                else url = url + "/api/yggdrasil";
            } catch (Exception e) {
                url = url + "/api/yggdrasil";
            }
        }
        return url;
    }
}