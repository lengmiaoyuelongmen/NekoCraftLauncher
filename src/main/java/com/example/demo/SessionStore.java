package com.example.demo;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

/**
 * 登录状态保存到本机注册表（HKCU），不写入 config.json / 游戏目录：
 * - 同一台机器重开启动器自动恢复，无需重复登录；
 * - 换机器或把启动器目录拷走不会带走账号。
 *
 * 数据以 Base64(UTF-8 JSON) 存在单个 REG_SZ 值里，避开 reg.exe 的中文编码问题。
 */
public final class SessionStore {

    private static final String KEY = "HKCU\\Software\\MCLauncher\\Session";
    private static final String VALUE = "data";

    public record Saved(String loginType, String username, String uuid, String accessToken,
                        String server, String skinUrl, String userType) {
    }

    private SessionStore() {
    }

    public static void save(UserSession session, String loginType, String server) {
        if (session == null || session.getUsername() == null) return;
        JsonObject json = new JsonObject();
        json.addProperty("loginType", loginType == null ? "" : loginType);
        json.addProperty("username", session.getUsername());
        json.addProperty("uuid", session.getUuid() == null ? "" : session.getUuid().toString());
        json.addProperty("accessToken", session.getAccessToken() == null ? "" : session.getAccessToken());
        json.addProperty("server", server == null ? "" : server);
        json.addProperty("skinUrl", session.getSkinUrl() == null ? "" : session.getSkinUrl());
        json.addProperty("userType", session.getUserType() == null ? loginType : session.getUserType());
        String payload = Base64.getEncoder().encodeToString(
                json.toString().getBytes(StandardCharsets.UTF_8));
        run("reg", "add", KEY, "/v", VALUE, "/t", "REG_SZ", "/d", payload, "/f");
    }

    public static Saved load() {
        String payload = readValue();
        if (payload == null || payload.isBlank()) return null;
        try {
            String json = new String(Base64.getDecoder().decode(payload.trim()), StandardCharsets.UTF_8);
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            String username = get(obj, "username");
            if (username.isBlank()) return null;
            return new Saved(get(obj, "loginType"), username, get(obj, "uuid"),
                    get(obj, "accessToken"), get(obj, "server"), get(obj, "skinUrl"),
                    get(obj, "userType"));
        } catch (Exception e) {
            return null;
        }
    }

    public static void clear() {
        run("reg", "delete", KEY, "/f");
    }

    private static String readValue() {
        try {
            Process process = new ProcessBuilder("reg", "query", KEY, "/v", VALUE)
                    .redirectErrorStream(true)
                    .start();
            String result = null;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    process.getInputStream(), Charset.defaultCharset()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (!trimmed.startsWith(VALUE)) continue;
                    String[] parts = trimmed.split("\\s{2,}");
                    if (parts.length >= 3) result = parts[2].trim();
                }
            }
            process.waitFor(3, TimeUnit.SECONDS);
            return result;
        } catch (Exception e) {
            return null;
        }
    }

    private static String get(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : "";
    }

    private static void run(String... command) {
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            process.waitFor(3, TimeUnit.SECONDS);
        } catch (Exception ignored) {
            // 注册表不可用时静默失败
        }
    }
}
