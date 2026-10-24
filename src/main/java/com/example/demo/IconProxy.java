package com.example.demo;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Modrinth 的图标在 cdn.modrinth.com，国内网络经常连不上。
 * 这里把 CDN 地址套一层图片代理（wsrv.nl），失败时仍可回退原地址。
 */
public final class IconProxy {

    private static final String PROXY = "https://wsrv.nl/?url=";

    private IconProxy() {
    }

    public static String resolve(String url) {
        if (url == null || url.isBlank()) return url;
        if (!url.contains("cdn.modrinth.com")) return url;
        try {
            return PROXY + URLEncoder.encode(url, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return url;
        }
    }

    /** 是否是会被代理的地址（用于缓存/回退判断）。 */
    public static boolean isProxied(String url) {
        return url != null && url.startsWith(PROXY);
    }
}
