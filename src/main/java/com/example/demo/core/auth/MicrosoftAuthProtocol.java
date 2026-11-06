package com.example.demo.core.auth;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.UUID;

/**
 * 微软登录协议的「纯协议层」：请求体拼装 + 响应解析。
 *
 * <p>登录是最容易坏、又最难查的链路（设备码 / 轮询 / XBL / XSTS / Minecraft / profile 六步），
 * 把每一步的报文格式单独拎出来，就能用固定 JSON 单测，而不必真的登录一次微软账号。
 *
 * <p>HTTP 仍然在 {@link MicrosoftAuth} 里，这里不发起任何请求。
 */
public final class MicrosoftAuthProtocol {

    /** 微软公开的 Minecraft Client ID。 */
    public static final String CLIENT_ID = "00000000402b5328";
    public static final String SCOPE = "XboxLive.signin offline_access";

    /** 设备码响应。 */
    public record DeviceCode(String deviceCode, String userCode, String verificationUri, int interval) {
    }

    /** Xbox Live 认证结果：令牌 + 用户哈希（UHS）。 */
    public record XboxLive(String token, String uhs) {
    }

    /** Minecraft 档案：名字 + 带连字符的 UUID。 */
    public record Profile(String name, UUID uuid) {
    }

    private static final Gson GSON = new Gson();

    private static final String DEVICE_CODE_URL =
            "https://login.microsoftonline.com/consumers/oauth2/v2.0/devicecode";
    private static final String TOKEN_URL =
            "https://login.microsoftonline.com/consumers/oauth2/v2.0/token";

    private MicrosoftAuthProtocol() {
    }

    public static String deviceCodeUrl() {
        return DEVICE_CODE_URL;
    }

    public static String tokenUrl() {
        return TOKEN_URL;
    }

    /** 申请设备码的 form body。 */
    public static String deviceCodeBody() {
        return "client_id=" + CLIENT_ID + "&scope=" + SCOPE.replace(" ", "%20");
    }

    /** 轮询令牌的 form body。 */
    public static String tokenBody(String deviceCode) {
        return "client_id=" + CLIENT_ID
                + "&device_code=" + deviceCode
                + "&grant_type=urn:ietf:params:oauth:grant-type:device_code";
    }

    /** 解析设备码响应；缺字段时抛出可读异常。 */
    public static DeviceCode parseDeviceCode(String body) {
        JsonObject json = parseObject(body);
        if (json == null || !json.has("device_code")) {
            throw new IllegalStateException("获取设备码失败: " + body);
        }
        return new DeviceCode(
                json.get("device_code").getAsString(),
                json.has("user_code") ? json.get("user_code").getAsString() : "",
                json.has("verification_uri") ? json.get("verification_uri").getAsString() : "",
                json.has("interval") ? json.get("interval").getAsInt() : 5);
    }

    /** 轮询结果：要么拿到 token，要么还在等用户授权，要么失败。 */
    public enum PollResult {
        /** 已拿到 access_token。 */
        TOKEN,
        /** 用户还没授权（authorization_pending）。 */
        PENDING,
        /** 授权被拒绝/过期。 */
        ERROR
    }

    public static PollResult pollResult(String tokenBody) {
        JsonObject json = parseObject(tokenBody);
        if (json == null) {
            return PollResult.ERROR;
        }
        if (json.has("access_token") && !json.get("access_token").isJsonNull()) {
            return PollResult.TOKEN;
        }
        if (json.has("error") && !json.get("error").isJsonNull()) {
            return "authorization_pending".equals(json.get("error").getAsString())
                    ? PollResult.PENDING
                    : PollResult.ERROR;
        }
        return PollResult.ERROR;
    }

    public static String accessToken(String tokenBody) {
        JsonObject json = parseObject(tokenBody);
        return json != null && json.has("access_token") ? json.get("access_token").getAsString() : null;
    }

    /** 授权失败时给用户看的原因。 */
    public static String pollError(String tokenBody) {
        JsonObject json = parseObject(tokenBody);
        if (json == null) {
            return tokenBody;
        }
        if (json.has("error_description")) {
            return json.get("error_description").getAsString();
        }
        return json.has("error") ? json.get("error").getAsString() : tokenBody;
    }

    public static String xboxLiveBody(String microsoftAccessToken) {
        return "{\"Properties\":{\"AuthMethod\":\"RPS\",\"SiteName\":\"user.auth.xboxlive.com\","
                + "\"RpsTicket\":\"d=" + microsoftAccessToken + "\"},"
                + "\"RelyingParty\":\"http://auth.xboxlive.com\",\"TokenType\":\"JWT\"}";
    }

    /** 解析 XBL 响应：令牌 + DisplayClaims.xui[0].uhs。 */
    public static XboxLive parseXboxLive(String body) {
        JsonObject json = parseObject(body);
        if (json == null || !json.has("Token")) {
            throw new IllegalStateException("Xbox Live 认证失败，响应异常: " + body);
        }
        String token = json.get("Token").getAsString();
        String uhs = null;
        if (json.has("DisplayClaims") && json.get("DisplayClaims").isJsonObject()) {
            JsonObject claims = json.getAsJsonObject("DisplayClaims");
            if (claims.has("xui") && claims.get("xui").isJsonArray()) {
                JsonArray xui = claims.getAsJsonArray("xui");
                if (!xui.isEmpty() && xui.get(0).isJsonObject()
                        && xui.get(0).getAsJsonObject().has("uhs")) {
                    uhs = xui.get(0).getAsJsonObject().get("uhs").getAsString();
                }
            }
        }
        if (uhs == null || uhs.isBlank()) {
            throw new IllegalStateException("无法解析 Xbox Live UHS，响应: " + body);
        }
        return new XboxLive(token, uhs);
    }

    public static String xstsBody(String xblToken) {
        return "{\"Properties\":{\"SandboxId\":\"RETAIL\",\"UserTokens\":[\"" + xblToken + "\"]},"
                + "\"RelyingParty\":\"rp://api.minecraftservices.com/\",\"TokenType\":\"JWT\"}";
    }

    /**
     * 解析 XSTS 响应。
     *
     * @return XSTS token
     * @throws IllegalStateException 带 XErr 的常见原因说明
     */
    public static String parseXstsToken(String body) {
        JsonObject json = parseObject(body);
        if (json == null) {
            throw new IllegalStateException("XSTS 认证失败，响应异常: " + body);
        }
        if (json.has("XErr")) {
            long xerr = json.get("XErr").getAsLong();
            throw new IllegalStateException("XSTS 授权失败 [XErr=" + xerr + "]: " + xstsReason(xerr));
        }
        if (!json.has("Token")) {
            throw new IllegalStateException("XSTS 认证失败，响应异常: " + body);
        }
        return json.get("Token").getAsString();
    }

    /** Xbox 错误码 → 人话。 */
    public static String xstsReason(long xerr) {
        if (xerr == 2148916233L) {
            return "该微软账号没有注册 Xbox 账户";
        }
        if (xerr == 2148916238L) {
            return "该账号未成年，需要成人账号将其加入家庭组";
        }
        if (xerr == 2148916235L) {
            return "该账号所在地区不支持 Xbox Live";
        }
        return "未知原因 (错误码: " + xerr + ")";
    }

    public static String minecraftBody(String uhs, String xstsToken) {
        return "{\"identityToken\":\"XBL3.0 x=" + uhs + ";" + xstsToken + "\"}";
    }

    public static String parseMinecraftToken(String body) {
        JsonObject json = parseObject(body);
        if (json == null || !json.has("access_token")) {
            throw new IllegalStateException("Minecraft 认证失败: " + body);
        }
        return json.get("access_token").getAsString();
    }

    /** 解析档案；没买游戏时微软会返回空对象。 */
    public static Profile parseProfile(String body) {
        JsonObject json = parseObject(body);
        if (json == null || !json.has("name") || !json.has("id")) {
            throw new IllegalStateException("获取玩家信息失败，请检查该账号是否已购买 Minecraft! 响应: " + body);
        }
        return new Profile(json.get("name").getAsString(), formatUuid(json.get("id").getAsString()));
    }

    /** 无符号 UUID（32 位 hex）→ 带连字符的标准格式。 */
    public static UUID formatUuid(String raw) {
        if (raw == null) {
            throw new IllegalStateException("UUID 为空");
        }
        String formatted = raw.replaceFirst(
                "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)",
                "$1-$2-$3-$4-$5");
        return UUID.fromString(formatted);
    }

    private static JsonObject parseObject(String body) {
        try {
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            return json;
        } catch (Exception e) {
            return null;
        }
    }
}
