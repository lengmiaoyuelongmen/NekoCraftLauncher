package com.example.demo.core.download;

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

    /** 依次尝试的图片地址：镜像 → 原地址 → 通用图片代理（每个都可能被墙）。 */
    public static java.util.List<String> candidates(String url) {
        java.util.List<String> list = new java.util.ArrayList<>();
        if (url == null || url.isBlank()) {
            return list;
        }
        if (url.contains("cdn.modrinth.com")) {
            list.add(url.replace("https://cdn.modrinth.com/", "https://mod.mcimirror.top/"));
        }
        list.add(url);
        if (url.contains("cdn.modrinth.com")) {
            try {
                list.add(PROXY + URLEncoder.encode(url, StandardCharsets.UTF_8));
            } catch (Exception ignored) {
                // 编码失败就不加代理候选
            }
        }
        return list;
    }

    /** 依次尝试候选源抓取图片字节；全部失败返回 null。 */
    public static byte[] fetch(java.net.http.HttpClient client, String url) {
        if (client == null) {
            return null;
        }
        for (String candidate : candidates(url)) {
            try {
                java.net.http.HttpRequest req =
                        java.net.http.HttpRequest.newBuilder(java.net.URI.create(candidate))
                                .timeout(java.time.Duration.ofSeconds(8L))
                                .GET()
                                .build();
                java.net.http.HttpResponse<java.io.InputStream> resp =
                        client.send(req, java.net.http.HttpResponse.BodyHandlers.ofInputStream());
                if (resp.statusCode() == 200) {
                    byte[] bytes = resp.body().readAllBytes();
                    if (bytes.length > 0) {
                        return bytes;
                    }
                }
            } catch (Exception ignored) {
                // 换下一个源
            }
        }
        return null;
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
