package com.example.demo.core.auth;

import com.example.demo.core.model.LoginCardData;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 解析「登录卡片」文本：从别的启动器导出的账号 JSON、或纯文本 key=value 都能认。
 *
 * <p>支持两类输入：
 * <ul>
 *   <li>JSON：{@code {"server": "...", "username": "...", "password": "..."}}，也认
 *       {@code auth:{...}} 嵌套、{@code apiRoot/api_root/yggdrasil/url/host} 等别名
 *   <li>文本：{@code 服务器: ...}、{@code 邮箱: ...}、{@code 密码: ...} 一行一个
 * </ul>
 *
 * <p>纯解析，不碰网络与 UI。
 */
public final class LoginCardParser {

    private static final Pattern SERVER_LINE = Pattern.compile(
            "(?im)^\\s*(?:服务器|地址|server|apiRoot|api_root|url)\\s*[:=]\\s*(\\S+)");
    private static final Pattern USER_LINE = Pattern.compile(
            "(?im)^\\s*(?:邮箱|账号|用户名|username|email|account)\\s*[:=]\\s*(\\S+)");
    private static final Pattern PASSWORD_LINE = Pattern.compile(
            "(?im)^\\s*(?:密码|password|passwd|pwd)\\s*[:=]\\s*(\\S+)");

    private LoginCardParser() {
    }

    /** @return 解析结果；用户名和密码都为空时返回 {@code null}（不是一张有效卡片） */
    public static LoginCardData parse(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String server = "";
        String username = "";
        String password = "";
        try {
            JsonObject json = JsonParser.parseString(text).getAsJsonObject();
            server = firstString(json,
                    "server", "apiRoot", "api_root", "authlibInjector", "authlib_injector",
                    "yggdrasil", "url", "host");
            username = firstString(json, "username", "email", "account", "name");
            password = firstString(json, "password", "passwd", "pwd");
            if (json.has("auth") && json.get("auth").isJsonObject()) {
                JsonObject auth = json.getAsJsonObject("auth");
                if (server.isBlank()) {
                    server = firstString(auth, "server", "apiRoot", "url");
                }
                if (username.isBlank()) {
                    username = firstString(auth, "username", "email");
                }
                if (password.isBlank()) {
                    password = firstString(auth, "password", "passwd");
                }
            }
        } catch (Exception ignored) {
            // 不是 JSON，按文本格式继续解析
        }
        if (server.isBlank()) {
            server = find(SERVER_LINE, text);
        }
        if (username.isBlank()) {
            username = find(USER_LINE, text);
        }
        if (password.isBlank()) {
            password = find(PASSWORD_LINE, text);
        }
        if (username.isBlank() && password.isBlank()) {
            return null;
        }
        return new LoginCardData(server, username, password);
    }

    /** 外置登录服务器地址归一化：补 https、去尾部斜杠、LittleSkin 补 /api/yggdrasil。 */
    public static String normalizeServer(String server) {
        if (server == null || server.isBlank()) {
            return "";
        }
        String value = server.trim();
        if (!value.startsWith("http://") && !value.startsWith("https://")) {
            value = "https://" + value;
        }
        value = value.replaceAll("/+$", "");
        if (value.contains("littleskin.cn") && !value.contains("/api/yggdrasil")) {
            value = value + "/api/yggdrasil";
        }
        return value;
    }

    private static String find(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1).trim() : "";
    }

    private static String firstString(JsonObject object, String... keys) {
        for (String key : keys) {
            if (!object.has(key) || object.get(key).isJsonNull() || !object.get(key).isJsonPrimitive()) {
                continue;
            }
            String value = object.get(key).getAsString();
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }
}
