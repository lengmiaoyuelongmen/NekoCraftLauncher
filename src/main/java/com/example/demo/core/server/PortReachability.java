package com.example.demo.core.server;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 端口对外可达性自检。
 *
 * <p>UPnP 映射成功不等于「别人真能连进来」：还可能被运营商 NAT、被防火墙拦、
 * 或者路由器映射到了错误的 IP。这里借 check-host.net 的公开 TCP 探测接口，
 * 从境外节点实际连一次 {@code 公网IP:端口}，结果是「真的连上了」才算数。
 *
 * <p>全程只读：只发起探测请求，不改变本机任何配置。
 */
public final class PortReachability {

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(6))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private PortReachability() {
    }

    /** 探测结果：reachable=能连上，reason 说明依据。 */
    public record Result(boolean reachable, boolean conclusive, String reason) {
    }

    /**
     * 从外部节点探测 host:port（阻塞，调用方放后台线程）。
     *
     * @param host 公网 IP 或域名
     * @param port 端口
     */
    public static Result check(String host, int port) {
        if (host == null || host.isBlank() || port <= 0) {
            return new Result(false, false, "缺少公网地址或端口");
        }
        // 先做本机 TCP 直连：立刻就有结论，且不依赖任何第三方服务
        Result local = connectTest(host, port, 3000);
        String requestId = startCheck(host, port);
        if (requestId != null) {
            long deadline = System.currentTimeMillis() + 12_000L;
            while (System.currentTimeMillis() < deadline) {
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                Boolean verdict = parseResult(get("https://check-host.net/check-result/" + requestId));
                if (verdict != null) {
                    return verdict
                            ? new Result(true, true, "外部节点已成功建立 TCP 连接")
                            : new Result(false, true, "外部节点连不上：映射未生效，或被防火墙/运营商拦住");
                }
            }
        }
        // 外部探测不可用（限流/超时）时，退回本机结论并说明它证明不了什么
        return local.reachable()
                ? new Result(true, false, "本机直连公网地址成功；外部探测本轮不可用，最终以朋友能否连上为准")
                : new Result(false, false, "本机直连也没成功，且外部探测不可用（映射大概率没生效）");
    }

/** 直接对 host:port 做一次 TCP 连接（毫秒级超时），用于 hairpin / 本机连通性判断。 */
    public static Result connectTest(String host, int port, int timeoutMs) {
        try (java.net.Socket socket = new java.net.Socket()) {
            socket.connect(new java.net.InetSocketAddress(host, port), Math.max(500, timeoutMs));
            return new Result(true, true, "TCP 连接成功");
        } catch (Exception e) {
            return new Result(false, true, "TCP 连接失败：" + (e instanceof java.net.SocketTimeoutException
                    ? "超时" : (e.getMessage() == null ? e.toString() : e.getMessage())));
        }
    }

    private static String startCheck(String host, int port) {
        try {
            HttpRequest request = HttpRequest.newBuilder(
                            URI.create("https://check-host.net/check-tcp?host="
                                    + java.net.URLEncoder.encode(host + ":" + port, StandardCharsets.UTF_8)
                                    + "&max_nodes=3"))
                    .timeout(Duration.ofSeconds(10))
                    .header("Accept", "application/json")
                    .header("User-Agent", "NekoCraftLauncher/2.x")
                    .GET()
                    .build();
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return null;
            }
            Matcher matcher = Pattern.compile("\"request_id\"\\s*:\\s*\"([^\"]+)\"").matcher(response.body());
            return matcher.find() ? matcher.group(1) : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 解析 check-result 响应。
     *
     * <p>形状大致是 {@code {"node":{"1.2.3.4":[[{"time":0.12,"address":"x"}]]}}}，
     * 成功节点是数组里带 time 的对象，失败是 {@code [null]} 或 {@code [{"error":"..."}]}。
     *
     * @return true=有节点连上，false=所有节点都失败，null=还没出结果
     */
    static Boolean parseResult(String body) {
        if (body == null || body.isBlank() || !body.contains("{")) {
            return null;
        }
        // 只要出现 "time" 且不是全部 null，就认为有人连上了
        boolean hasTime = body.contains("\"time\"");
        boolean allNull = !body.contains(":[[");
        if (allNull) {
            return null;
        }
        if (hasTime) {
            // 排除 {"error": ...} 里也可能带 time 的极端情况：error 优先
            if (body.contains("\"error\"") && !body.contains("\"address\"")) {
                return false;
            }
            return true;
        }
        return false;
    }

    private static String get(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("Accept", "application/json")
                    .header("User-Agent", "NekoCraftLauncher/2.x")
                    .GET()
                    .build();
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200 ? response.body() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
