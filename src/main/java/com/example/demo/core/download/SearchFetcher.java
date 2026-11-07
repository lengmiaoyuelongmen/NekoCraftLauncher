package com.example.demo.core.download;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * 社区搜索的网络层：官方源与镜像交替、失败自动换源。
 *
 * <p>只负责「发请求、拿 JSON 文本」；解析与分页规则分别在 {@link ModrinthSearch} 和
 * {@link SearchSession}。请求失败一律返回 {@code null}，由调用方决定提示文案。
 */
public final class SearchFetcher {

    private static final String USER_AGENT = "PCL-CE-Launcher/1.0";

    private final HttpClient client;
    private final Duration timeout;

    public SearchFetcher(HttpClient client, Duration timeout) {
        this.client = client;
        this.timeout = timeout == null ? Duration.ofSeconds(8L) : timeout;
    }

    /** 取一个 URL 的响应体；非 200 或异常都返回 null。 */
    public CompletableFuture<String> fetch(String url) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                        .header("User-Agent", USER_AGENT)
                        .GET()
                        .timeout(timeout)
                        .build();
                HttpResponse<String> response =
                        client.send(request, HttpResponse.BodyHandlers.ofString());
                return response.statusCode() == 200 ? response.body() : null;
            } catch (Exception e) {
                return null;
            }
        });
    }

    /**
     * 官方源与镜像交替：按页码奇偶决定先问谁，第一个返回 JSON 的就用。
     *
     * <p>这样单数页和双数页会打到不同来源，既分担压力，又能在某一源挂掉时自动切换。
     */
    public CompletableFuture<String> alternate(String officialUrl, String mirrorUrl, int offset) {
        SearchSession.Pair order = SearchSession.primaryBackup(officialUrl, mirrorUrl, offset);
        return fetch(order.primary()).thenCompose(body -> {
            if (body != null && body.startsWith("{")) {
                return CompletableFuture.completedFuture(body);
            }
            return fetch(order.backup());
        });
    }
}
