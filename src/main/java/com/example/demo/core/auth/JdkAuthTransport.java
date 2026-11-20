package com.example.demo.core.auth;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * 真实的 HTTP 传输层（JDK HttpClient）。
 *
 * <p>两处超时都必须设：连接超时管「这个主机连不上」，请求超时管「连上了但不回话」。
 * 没有它们，一条被墙的线路会把整个登录卡死，切换也就无从谈起。
 */
public final class JdkAuthTransport implements AuthTransport {

    private static final int DEFAULT_TIMEOUT_SECONDS = 15;

    private final HttpClient client;

    public JdkAuthTransport() {
        this(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(6))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build());
    }

    public JdkAuthTransport(HttpClient client) {
        this.client = client;
    }

    @Override
    public Response send(Request request) throws Exception {
        int timeout = request.timeoutSeconds() > 0 ? request.timeoutSeconds() : DEFAULT_TIMEOUT_SECONDS;
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(request.url()))
                .timeout(Duration.ofSeconds(timeout));
        for (Map.Entry<String, String> header : request.headers().entrySet()) {
            builder.header(header.getKey(), header.getValue());
        }
        if (request.body() == null) {
            builder.GET();
        } else {
            builder.method(request.method(), HttpRequest.BodyPublishers.ofString(request.body()));
        }
        long start = System.nanoTime();
        HttpResponse<String> response =
                client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        long latencyMs = Math.round((System.nanoTime() - start) / 1_000_000.0);
        return new Response(response.statusCode(), response.body(), latencyMs);
    }
}
