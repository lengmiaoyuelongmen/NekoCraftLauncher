package com.example.demo.core.auth;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 带「先检测、后使用、失败自动切换」的登录请求器。
 *
 * <p>一次登录会经过六个阶段（设备码 → 轮询 → XBL → XSTS → Minecraft → 档案），
 * 每个阶段都可能有多条公开线路。这里的策略是：
 * <ol>
 *   <li><b>先检测</b>：对没测过的线路并行发一次极短的探测请求（4 秒超时），
 *       只要对方回了 HTTP（哪怕是 400），就算这条路活着，并记下真实延迟；</li>
 *   <li><b>再使用</b>：按 {@link EndpointHealth} 排出的顺序发真实请求——
 *       先用手测最快且健康的线路，其次才是没测过的；</li>
 *   <li><b>失败切换</b>：连接失败 / 超时 / 5xx / 429 / 明确的应用级拒绝（AADSTS700016 等）
 *       都算这条线路不可用，标记冷却后立刻换下一条，业务错误（如用户还没授权）不换线。</li>
 * </ol>
 *
 * <p>「全部线路都失败」时会抛出带每条线路原因的异常，而不是只报最后一句话。
 */
public final class ResilientAuthClient {

    /** 探测超时：够短，不至于让登录卡住；够长，跨境也能完成 TLS 握手。 */
    private static final int PROBE_TIMEOUT_SECONDS = 4;

    /** 单线路阶段的额外重试次数（Xbox Live / XSTS / Minecraft 都只有一条官方地址）。 */
    private static final int SINGLE_ENDPOINT_ATTEMPTS = 3;

    /** 一条线路的最终结果。 */
    public record Result(AuthEndpoint endpoint, int status, String body, long latencyMs, int switches) {

        public boolean ok() {
            return status >= 200 && status < 300;
        }
    }

    /** 探测结果，给「重新检测线路」这类界面用。 */
    public record Probe(AuthEndpoint endpoint, boolean healthy, long latencyMs, String detail) {
    }

    private final AuthTransport transport;
    private final EndpointHealth health;
    private final Consumer<String> log;

    public ResilientAuthClient(AuthTransport transport, EndpointHealth health, Consumer<String> log) {
        this.transport = transport;
        this.health = health;
        this.log = log == null ? message -> { } : log;
    }

    /** 生产用：真实传输层 + 新健康表。 */
    public static ResilientAuthClient defaults(Consumer<String> log) {
        return new ResilientAuthClient(new JdkAuthTransport(), new EndpointHealth(), log);
    }

    public EndpointHealth health() {
        return health;
    }

    /**
     * 发一个表单请求（设备码 / 令牌轮询用）。
     *
     * @param bodyForClientId 用该线路的 client id 拼请求体（client id 与设备码必须成对）
     * @param pinnedClientId  锁定 client id（轮询时必须与申请设备码时一致）；为 null 表示不锁定
     */
    public Result postForm(AuthStage stage, Function<String, String> bodyForClientId,
                           String pinnedClientId, int timeoutSeconds) {
        return execute(stage, pinnedClientId, timeoutSeconds,
                (endpoint) -> AuthTransport.Request.postForm(endpoint.url(),
                        bodyForClientId.apply(endpoint.clientId()), timeoutSeconds));
    }

    /** 发一个 JSON 请求（XBL / XSTS / Minecraft 用，没有 client id 之分）。 */
    public Result postJson(AuthStage stage, String body, int timeoutSeconds) {
        return execute(stage, null, timeoutSeconds,
                (endpoint) -> AuthTransport.Request.postJson(endpoint.url(), body, timeoutSeconds));
    }

    /** 发一个带鉴权头的 GET（读取档案用）。 */
    public Result get(AuthStage stage, String authorizationHeader, int timeoutSeconds) {
        return execute(stage, null, timeoutSeconds,
                (endpoint) -> new AuthTransport.Request("GET", endpoint.url(),
                        java.util.Map.of("Accept", "application/json",
                                "Authorization", authorizationHeader), null, timeoutSeconds));
    }

    private Result execute(AuthStage stage, String pinnedClientId, int timeoutSeconds,
                           Function<AuthEndpoint, AuthTransport.Request> requestFor) {
        List<AuthEndpoint> candidates = AuthEndpoints.forClientId(AuthEndpoints.candidates(stage), pinnedClientId);
        if (candidates.isEmpty()) {
            throw new IllegalStateException("没有可用的 " + stage.label() + " 线路");
        }
        probe(candidates);
        List<AuthEndpoint> ordered = health.order(candidates);

        List<String> failures = new ArrayList<>();
        int attempts = ordered.size() == 1 ? SINGLE_ENDPOINT_ATTEMPTS : ordered.size();
        int switches = 0;
        for (int attempt = 0; attempt < attempts; attempt++) {
            AuthEndpoint endpoint = ordered.get(attempt % ordered.size());
            if (attempt >= ordered.size()) {
                sleep(400L * (attempt - ordered.size() + 1));
            }
            AuthTransport.Request request = requestFor.apply(endpoint);
            long start = System.nanoTime();
            AuthTransport.Response response;
            try {
                response = transport.send(request);
            } catch (Exception e) {
                long waited = Math.round((System.nanoTime() - start) / 1_000_000.0);
                String reason = e.getClass().getSimpleName()
                        + (e.getMessage() == null ? "" : ": " + e.getMessage());
                health.markUnhealthy(endpoint, reason);
                failures.add(endpoint.label() + " → " + reason + "（" + waited + "ms）");
                switches++;
                announceSwitch(stage, endpoint, reason, request.url());
                continue;
            }
            if (switchable(response)) {
                health.markUnhealthy(endpoint, "HTTP " + response.status());
                failures.add(endpoint.label() + " → HTTP " + response.status()
                        + " " + response.shortBody());
                switches++;
                announceSwitch(stage, endpoint, "HTTP " + response.status(), request.url());
                continue;
            }
            health.markHealthy(endpoint, response.latencyMs());
            if (switches > 0) {
                log.accept("✅ " + stage.label() + " 已切到 " + endpoint.label()
                        + "（" + response.latencyMs() + "ms）");
            }
            return new Result(endpoint, response.status(), response.body(), response.latencyMs(), switches);
        }
        throw new IllegalStateException(stage.label() + " 全部 " + ordered.size()
                + " 条线路都失败：\n  " + String.join("\n  ", failures));
    }

    /**
     * 并行探测没测过的线路，把结果写进健康表。
     *
     * <p>探测请求故意用一个「合法但不完整」的 GET：接口会回 400/405，这正是我们想看到的
     * 「服务器活着」。5xx、超时、DNS 失败才算这条线路挂了。
     */
    public List<Probe> probe(List<AuthEndpoint> endpoints) {
        List<AuthEndpoint> unknown = new ArrayList<>();
        for (AuthEndpoint endpoint : endpoints) {
            if (health.status(endpoint) == EndpointHealth.Status.UNKNOWN) {
                unknown.add(endpoint);
            }
        }
        if (unknown.isEmpty()) {
            return List.of();
        }
        ExecutorService pool = Executors.newFixedThreadPool(Math.min(6, unknown.size()), runnable -> {
            Thread thread = new Thread(runnable, "auth-probe");
            thread.setDaemon(true);
            return thread;
        });
        try {
            List<CompletableFuture<Probe>> futures = new ArrayList<>();
            for (AuthEndpoint endpoint : unknown) {
                futures.add(CompletableFuture.supplyAsync(() -> probeOne(endpoint), pool));
            }
            List<Probe> probes = new ArrayList<>();
            long fastest = Long.MAX_VALUE;
            int alive = 0;
            for (CompletableFuture<Probe> future : futures) {
                Probe probe = future.join();
                probes.add(probe);
                if (probe.healthy()) {
                    alive++;
                    fastest = Math.min(fastest, probe.latencyMs());
                }
            }
            if (alive == 0) {
                log.accept("⚠️ 线路检测：本次探测的 " + probes.size() + " 条线路都没回应，仍按优先级尝试");
            } else {
                log.accept("🔎 线路检测：" + alive + "/" + probes.size() + " 条可用，最快 "
                        + fastest + "ms");
            }
            return probes;
        } finally {
            pool.shutdownNow();
        }
    }

    private Probe probeOne(AuthEndpoint endpoint) {
        long start = System.nanoTime();
        try {
            AuthTransport.Response response = transport.send(
                    AuthTransport.Request.get(endpoint.url(), PROBE_TIMEOUT_SECONDS));
            long latency = response.latencyMs() > 0
                    ? response.latencyMs()
                    : Math.round((System.nanoTime() - start) / 1_000_000.0);
            if (response.status() >= 500) {
                health.markUnhealthy(endpoint, "探测 HTTP " + response.status());
                return new Probe(endpoint, false, latency, "HTTP " + response.status());
            }
            health.markHealthy(endpoint, latency);
            return new Probe(endpoint, true, latency, "HTTP " + response.status());
        } catch (Exception e) {
            long latency = Math.round((System.nanoTime() - start) / 1_000_000.0);
            String detail = e.getClass().getSimpleName()
                    + (e.getMessage() == null ? "" : ": " + e.getMessage());
            health.markUnhealthy(endpoint, detail);
            return new Probe(endpoint, false, latency, detail);
        }
    }

    /**
     * 这个响应算不算「线路问题」。
     *
     * <p>关键点：用户还没授权（authorization_pending）、账号没买游戏这类是<b>业务结果</b>，
     * 换线路也一样，必须原样交给上层；只有网络/服务端/应用身份不匹配才换。
     */
    static boolean switchable(AuthTransport.Response response) {
        if (response.serverSide()) {
            return true;
        }
        String body = response.body() == null ? "" : response.body();
        if (body.contains("authorization_pending") || body.contains("slow_down")) {
            return false;
        }
        return body.contains("AADSTS700016")      // client id 在这个租户不存在
                || body.contains("AADSTS50059")   // 缺少租户信息
                || body.contains("unauthorized_client")
                || body.contains("\"invalid_client\"")
                || body.contains("temporarily_unavailable");
    }

    private void announceSwitch(AuthStage stage, AuthEndpoint failed, String reason, String url) {
        log.accept("🔀 " + stage.label() + " 线路不可用：" + failed.label() + "（" + reason + "），切换下一条");
        if (url != null) {
            log.accept("   ↳ 失败地址：" + url);
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
