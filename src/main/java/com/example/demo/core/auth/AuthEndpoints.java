package com.example.demo.core.auth;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 公开登录接口清单：每个阶段给出多条线路，按「先检测、再使用、失败就换」的方式使用。
 *
 * <p>这些地址都是微软 / Xbox / Mojang 官方对外的接口，不含任何第三方中转。
 * 同一套 OAuth 服务有多个主机别名（{@code login.microsoftonline.com}、
 * {@code login.microsoft.com}、{@code login.windows.net}），
 * 各自独立解析、独立限流，所以一条被墙或被限速时可以换另一条。
 *
 * <p>client id 说明：{@code 00000000402b5328}（微软账号旧版应用）现在已经不被
 * {@code /consumers/oauth2/v2.0} 接受，实测返回 AADSTS700016；这里改用开源启动器
 * 公开的公共 client id，并支持用系统属性追加：
 * <pre>
 *   -Dmc.auth.extraClientIds=id1,id2
 *   -Dmc.auth.extraHosts=login.partner.microsoftonline.cn
 * </pre>
 */
public final class AuthEndpoints {

    /** 默认公共 client id（公开、无需注册，XboxLive.signin 范围可用）。 */
    public static final String DEFAULT_CLIENT_ID = "88650e7e-efee-4857-b9a9-cf580a00ef43";

    /** 实测可用的微软 OAuth 主机别名，顺序即默认优先级。 */
    public static final List<String> MICROSOFT_HOSTS = List.of(
            "login.microsoftonline.com",
            "login.microsoft.com",
            "login.windows.net");

    public static final String XBOX_LIVE_URL = "https://user.auth.xboxlive.com/user/authenticate";
    public static final String XSTS_URL = "https://xsts.auth.xboxlive.com/xsts/authorize";
    public static final String MINECRAFT_LOGIN_URL =
            "https://api.minecraftservices.com/authentication/login_with_xbox";
    public static final String PROFILE_URL = "https://api.minecraftservices.com/minecraft/profile";

    private AuthEndpoints() {
    }

    /** 当前生效的 client id 列表：默认值 + 系统属性追加（去重、保序）。 */
    public static List<String> clientIds() {
        Set<String> ids = new LinkedHashSet<>();
        ids.add(DEFAULT_CLIENT_ID);
        for (String extra : splitProperty("mc.auth.extraClientIds")) {
            if (extra.matches("[0-9a-fA-F-]{16,64}")) {
                ids.add(extra);
            }
        }
        return List.copyOf(ids);
    }

    /** 当前生效的微软主机列表：内置别名 + 系统属性追加。 */
    public static List<String> microsoftHosts() {
        Set<String> hosts = new LinkedHashSet<>(MICROSOFT_HOSTS);
        for (String extra : splitProperty("mc.auth.extraHosts")) {
            if (extra.matches("[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}")) {
                hosts.add(extra);
            }
        }
        return List.copyOf(hosts);
    }

    /** 某个阶段的全部候选线路（未排序，排序交给 {@link EndpointHealth}）。 */
    public static List<AuthEndpoint> candidates(AuthStage stage) {
        return candidates(stage, clientIds(), microsoftHosts());
    }

    /** 便于测试：指定 client id 与主机生成候选。 */
    public static List<AuthEndpoint> candidates(AuthStage stage, List<String> clientIds, List<String> hosts) {
        List<AuthEndpoint> list = new ArrayList<>();
        switch (stage) {
            case DEVICE_CODE -> {
                int priority = 0;
                for (String clientId : clientIds) {
                    for (String host : hosts) {
                        list.add(new AuthEndpoint(stage, "微软 / " + host,
                                "https://" + host + "/consumers/oauth2/v2.0/devicecode",
                                clientId, priority++));
                    }
                }
            }
            case TOKEN -> {
                int priority = 0;
                for (String clientId : clientIds) {
                    for (String host : hosts) {
                        list.add(new AuthEndpoint(stage, "微软 / " + host,
                                "https://" + host + "/consumers/oauth2/v2.0/token",
                                clientId, priority++));
                    }
                }
            }
            case XBOX_LIVE -> list.add(new AuthEndpoint(stage, "Xbox Live", XBOX_LIVE_URL, null, 0));
            case XSTS -> list.add(new AuthEndpoint(stage, "XSTS", XSTS_URL, null, 0));
            case MINECRAFT_LOGIN -> list.add(new AuthEndpoint(stage, "Minecraft 服务",
                    MINECRAFT_LOGIN_URL, null, 0));
            case PROFILE -> list.add(new AuthEndpoint(stage, "玩家档案", PROFILE_URL, null, 0));
        }
        return list;
    }

    /** 只保留指定 client id 的线路（设备码与轮询必须成对，不能中途换 id）。 */
    public static List<AuthEndpoint> forClientId(List<AuthEndpoint> endpoints, String clientId) {
        if (clientId == null || clientId.isBlank()) {
            return endpoints;
        }
        List<AuthEndpoint> filtered = new ArrayList<>();
        for (AuthEndpoint endpoint : endpoints) {
            if (clientId.equals(endpoint.clientId())) {
                filtered.add(endpoint);
            }
        }
        return filtered.isEmpty() ? endpoints : filtered;
    }

    private static List<String> splitProperty(String key) {
        String raw = System.getProperty(key, "");
        if (raw.isBlank()) {
            return List.of();
        }
        List<String> parts = new ArrayList<>();
        for (String piece : raw.split("[,;\\s]+")) {
            if (!piece.isBlank()) {
                parts.add(piece.trim());
            }
        }
        return parts;
    }
}
