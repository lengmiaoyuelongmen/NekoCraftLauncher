package com.example.demo;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.DoubleConsumer;
import java.time.Duration;

/**
 * 更新检查：读取一个静态 JSON（可放 GitHub Releases / 自己的 CDN）：
 * {
 *   "version": "1.1.0",
 *   "url": "https://example.com/MCLauncher-Setup.exe",
 *   "notes": "更新内容..."
 * }
 */
public final class UpdateChecker {

    public record Info(String version, String url, String notes) {
    }

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /** VPN / 代理证书未进 Java cacerts 时的兜底客户端；只用于更新源。 */
    private static final HttpClient FALLBACK_CLIENT = buildFallbackClient();

    private static HttpClient buildFallbackClient() {
        try {
            javax.net.ssl.TrustManager[] trustAll = {new javax.net.ssl.X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() { return new java.security.cert.X509Certificate[0]; }
                public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) { }
                public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) { }
            }};
            javax.net.ssl.SSLContext context = javax.net.ssl.SSLContext.getInstance("TLS");
            context.init(null, trustAll, new java.security.SecureRandom());
            return HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(12))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .sslContext(context)
                    .build();
        } catch (Exception ignored) {
            return CLIENT;
        }
    }

    private UpdateChecker() {
    }

    private static final String FALLBACK_URL = LauncherConfig.DEFAULT_UPDATE_URL;

    public static Info fetch(String url) throws Exception {
        return fetch(url, "");
    }

    /** 主更新源失败后依次尝试备用镜像和内置 GitHub 源。 */
    public static Info fetch(String url, String mirrorUrl) throws Exception {
        try {
            return fetchOnce(url);
        } catch (Exception first) {
            if (mirrorUrl != null && !mirrorUrl.isBlank()) {
                try {
                    return fetchOnce(mirrorUrl);
                } catch (Exception ignored) {
                }
            }
            if (url != null && url.contains("lengmiaoyuelongmen/NekoCraftLauncher")) throw first;
            try {
                return fetchOnce(FALLBACK_URL);
            } catch (Exception ignored) {
                throw first;
            }
        }
    }

    private static Info fetchOnce(String url) throws Exception {
        try {
            return fetchOnce(url, CLIENT);
        } catch (Exception first) {
            if (!isTlsError(first)) throw first;
            return fetchOnce(url, FALLBACK_CLIENT);
        }
    }

    private static boolean isTlsError(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof javax.net.ssl.SSLException
                    || current instanceof java.security.cert.CertificateException
                    || (current.getMessage() != null
                    && (current.getMessage().contains("certification path")
                    || current.getMessage().contains("PKIX")))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static Info fetchOnce(String url, HttpClient client) throws Exception {
        if (url == null || url.isBlank()) throw new IllegalArgumentException("请先填写更新地址");
        String requestUrl = normalize(url.trim());
        HttpRequest request = HttpRequest.newBuilder(URI.create(requestUrl))
                .timeout(Duration.ofSeconds(15))
                .header("User-Agent", "NekoCraftLauncher/" + AppVersion.CURRENT)
                .header("Accept", "application/vnd.github+json, application/json")
                .GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("HTTP " + response.statusCode()
                    + (response.statusCode() == 404 ? "（仓库或 Release 不存在）" : ""));
        }
        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();

        // GitHub Releases API: /repos/{owner}/{repo}/releases/latest
        if (json.has("tag_name")) {
            String version = json.get("tag_name").getAsString();
            String notes = json.has("body") && !json.get("body").isJsonNull()
                    ? json.get("body").getAsString() : "";
            String download = "";
            if (json.has("assets") && json.get("assets").isJsonArray()) {
                String fallback = "";
                for (var element : json.getAsJsonArray("assets")) {
                    JsonObject asset = element.getAsJsonObject();
                    String name = asset.has("name") ? asset.get("name").getAsString() : "";
                    String assetUrl = asset.has("browser_download_url")
                            ? asset.get("browser_download_url").getAsString() : "";
                    if (assetUrl.isBlank()) continue;
                    if (name.toLowerCase(java.util.Locale.ROOT).endsWith(".exe")) {
                        if (name.toLowerCase(java.util.Locale.ROOT).contains("setup")) {
                            download = assetUrl;
                            break;
                        }
                        if (fallback.isBlank()) fallback = assetUrl;
                    }
                }
                if (download.isBlank()) download = fallback;
            }
            if (download.isBlank() && json.has("html_url")) {
                download = json.get("html_url").getAsString();
            }
            if (version.isBlank()) throw new RuntimeException("GitHub Release 缺少 tag_name");
            return new Info(version.trim(), download.trim(), notes);
        }

        // 静态 JSON: {"version":"1.1.0","url":"...","notes":"..."}
        String version = json.has("version") ? json.get("version").getAsString() : "";
        String down = json.has("url") ? json.get("url").getAsString() : "";
        String notes = json.has("notes") ? json.get("notes").getAsString() : "";
        if (version.isBlank()) throw new RuntimeException("更新 JSON 里没有 version");
        return new Info(version.trim(), down.trim(), notes);
    }

    /** 允许直接粘贴 GitHub 仓库地址或 Release 页面，自动转成 Releases API。 */
    private static String normalize(String url) {
        String clean = url.trim();
        if (clean.startsWith("https://api.github.com/")) return clean;
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("https?://github\\.com/([^/]+)/([^/]+)(?:/.*)?")
                .matcher(clean);
        if (m.matches()) {
            String repo = m.group(2).replaceAll("\\.git$", "");
            return "https://api.github.com/repos/" + m.group(1) + "/" + repo + "/releases/latest";
        }
        return clean;
    }

    /** 下载安装包到本地，progress 为 0~1，未知总大小时为 -1。 */
    public static void download(String url, Path target, DoubleConsumer progress) throws Exception {
        try {
            downloadInternal(url, target, progress, CLIENT);
        } catch (Exception first) {
            if (!isTlsError(first)) throw first;
            downloadInternal(url, target, progress, FALLBACK_CLIENT);
        }
    }

    private static void downloadInternal(String url, Path target, DoubleConsumer progress,
                                         HttpClient client) throws Exception {
        boolean localHttp = url != null && (url.startsWith("http://127.0.0.1")
                || url.startsWith("http://localhost"));
        if (url == null || (!url.startsWith("https://") && !localHttp)) {
            throw new IllegalArgumentException("只允许 HTTPS 或本机 HTTP 下载地址");
        }
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofMinutes(10))
                .header("User-Agent", "NekoCraftLauncher/" + AppVersion.CURRENT)
                .GET().build();
        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() / 100 != 2) throw new RuntimeException("HTTP " + response.statusCode());
        long total = response.headers().firstValueAsLong("Content-Length").orElse(-1);
        if (progress != null) progress.accept(total <= 0 ? -1 : 0);
        Files.createDirectories(target.getParent());
        try (InputStream in = response.body();
             java.io.OutputStream out = Files.newOutputStream(target)) {
            byte[] buffer = new byte[32768];
            long readTotal = 0;
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
                readTotal += read;
                if (progress != null && total > 0) progress.accept((double) readTotal / total);
            }
        }
    }

    /** 简单的语义化版本比较：返回 true 表示 remote 比 current 新。 */
    public static boolean isNewer(String remote, String current) {
        int[] a = parse(remote);
        int[] b = parse(current);
        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            int left = i < a.length ? a[i] : 0;
            int right = i < b.length ? b[i] : 0;
            if (left != right) return left > right;
        }
        return false;
    }

    private static int[] parse(String version) {
        String[] parts = version.replaceAll("[^0-9.]", "").split("\\.");
        int[] numbers = new int[Math.max(1, parts.length)];
        for (int i = 0; i < parts.length; i++) {
            try {
                numbers[i] = parts[i].isEmpty() ? 0 : Integer.parseInt(parts[i]);
            } catch (NumberFormatException e) {
                numbers[i] = 0;
            }
        }
        return numbers;
    }
}
