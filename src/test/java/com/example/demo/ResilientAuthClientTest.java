package com.example.demo;

import com.example.demo.core.auth.AuthEndpoints;
import com.example.demo.core.auth.AuthStage;
import com.example.demo.core.auth.AuthTransport;
import com.example.demo.core.auth.EndpointHealth;
import com.example.demo.core.auth.ResilientAuthClient;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.http.HttpTimeoutException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 「先检测、失败切换」：用假传输层覆盖探测、换线、业务错误不换线、全部失败。 */
class ResilientAuthClientTest {

    private static final String HOST_PRIMARY = "login.microsoftonline.com";
    private static final String HOST_ALIAS = "login.microsoft.com";
    private static final String HOST_LEGACY = "login.windows.net";

    @FunctionalInterface
    interface Handler {
        AuthTransport.Response handle(AuthTransport.Request request) throws Exception;
    }

    static final class FakeTransport implements AuthTransport {
        private final Map<String, Handler> handlers = new LinkedHashMap<>();
        final List<AuthTransport.Request> requests = new CopyOnWriteArrayList<>();

        FakeTransport on(String host, Handler handler) {
            handlers.put(host, handler);
            return this;
        }

        @Override
        public Response send(Request request) throws Exception {
            requests.add(request);
            Handler handler = handlers.get(hostOf(request.url()));
            if (handler == null) {
                throw new IOException("测试里没有为 " + hostOf(request.url()) + " 声明响应");
            }
            return handler.handle(request);
        }

        List<AuthTransport.Request> posts() {
            List<AuthTransport.Request> posts = new ArrayList<>();
            for (AuthTransport.Request request : requests) {
                if ("POST".equals(request.method())) {
                    posts.add(request);
                }
            }
            return posts;
        }

        static String hostOf(String url) {
            int start = url.indexOf("://") + 3;
            int slash = url.indexOf('/', start);
            return slash > 0 ? url.substring(start, slash) : url.substring(start);
        }
    }

    private static AuthTransport.Response probe(long latency) {
        return new AuthTransport.Response(400, "{\"error\":\"probe\"}", latency);
    }

    private static AuthTransport.Response ok(String body, long latency) {
        return new AuthTransport.Response(200, body, latency);
    }

    /** 三个主机的常见组合：探测回 400（活着），POST 由调用方决定。 */
    private static FakeTransport base() {
        FakeTransport transport = new FakeTransport();
        transport.on(HOST_PRIMARY, request -> probe(10));
        transport.on(HOST_ALIAS, request -> probe(20));
        transport.on(HOST_LEGACY, request -> probe(30));
        return transport;
    }

    @Test
    void probesUnknownEndpointsAndPicksTheFastest() {
        FakeTransport transport = new FakeTransport()
                .on(HOST_PRIMARY, request -> "GET".equals(request.method()) ? probe(210) : ok("{\"a\":1}", 210))
                .on(HOST_ALIAS, request -> "GET".equals(request.method()) ? probe(30) : ok("{\"a\":1}", 30))
                .on(HOST_LEGACY, request -> "GET".equals(request.method()) ? probe(120) : ok("{\"a\":1}", 120));
        EndpointHealth health = new EndpointHealth();
        ResilientAuthClient client = new ResilientAuthClient(transport, health, message -> { });

        ResilientAuthClient.Result result = client.postForm(AuthStage.DEVICE_CODE,
                id -> "client_id=" + id, null, 5);

        assertEquals(HOST_ALIAS, result.endpoint().host(), "探测后应选实测最快的线路");
        assertEquals(200, result.status());
        assertEquals(4, transport.requests.size(), "3 次探测 + 1 次真实请求");
        assertEquals(30, client.health().latencyOf(result.endpoint()));
    }

    @Test
    void switchesWhenTheChosenEndpointThrows() {
        AtomicInteger primaryPosts = new AtomicInteger();
        FakeTransport transport = base()
                .on(HOST_PRIMARY, request -> {
                    if ("GET".equals(request.method())) {
                        return probe(10);
                    }
                    primaryPosts.incrementAndGet();
                    throw new HttpTimeoutException("request timed out");
                })
                .on(HOST_ALIAS, request -> "GET".equals(request.method())
                        ? probe(20)
                        : ok("{\"device_code\":\"dev\"}", 60));
        ResilientAuthClient client = new ResilientAuthClient(transport, new EndpointHealth(), message -> { });

        ResilientAuthClient.Result result = client.postForm(AuthStage.DEVICE_CODE,
                id -> "client_id=" + id, null, 5);

        assertEquals(HOST_ALIAS, result.endpoint().host());
        assertEquals(1, result.switches(), "换过一次线");
        assertEquals(1, primaryPosts.get(), "失败的主线路不重复尝试");
        assertEquals(EndpointHealth.Status.UNHEALTHY,
                client.health().status(AuthEndpoints.candidates(AuthStage.DEVICE_CODE).get(0)),
                "失败的主线路进入冷却");
    }

    @Test
    void switchesOnServerError() {
        FakeTransport transport = base()
                .on(HOST_PRIMARY, request -> "GET".equals(request.method())
                        ? probe(10)
                        : new AuthTransport.Response(503, "busy", 12))
                .on(HOST_ALIAS, request -> "GET".equals(request.method())
                        ? probe(20)
                        : ok("{\"device_code\":\"dev\"}", 25));
        ResilientAuthClient client = new ResilientAuthClient(transport, new EndpointHealth(), message -> { });

        ResilientAuthClient.Result result = client.postForm(AuthStage.DEVICE_CODE,
                id -> "client_id=" + id, null, 5);

        assertEquals(HOST_ALIAS, result.endpoint().host(), "5xx 要换线");
        assertEquals(200, result.status());
    }

    @Test
    void switchesWhenApplicationIsUnknownToTheTenant() {
        FakeTransport transport = base()
                .on(HOST_PRIMARY, request -> "GET".equals(request.method())
                        ? probe(10)
                        : new AuthTransport.Response(400,
                        "{\"error\":\"unauthorized_client\","
                                + "\"error_description\":\"AADSTS700016: Application not found\"}", 18))
                .on(HOST_ALIAS, request -> "GET".equals(request.method())
                        ? probe(20)
                        : ok("{\"device_code\":\"dev\"}", 30));
        ResilientAuthClient client = new ResilientAuthClient(transport, new EndpointHealth(), message -> { });

        ResilientAuthClient.Result result = client.postForm(AuthStage.DEVICE_CODE,
                id -> "client_id=" + id, null, 5);

        assertEquals(HOST_ALIAS, result.endpoint().host(), "client id 不被接受时要换线");
    }

    @Test
    void keepsWaitingWhenUserHasNotAuthorizedYet() {
        String pending = "{\"error\":\"authorization_pending\"}";
        FakeTransport transport = base()
                .on(HOST_PRIMARY, request -> "GET".equals(request.method())
                        ? probe(10)
                        : new AuthTransport.Response(400, pending, 15));
        ResilientAuthClient client = new ResilientAuthClient(transport, new EndpointHealth(), message -> { });

        ResilientAuthClient.Result result = client.postForm(AuthStage.TOKEN,
                id -> "client_id=" + id, null, 5);

        assertEquals(HOST_PRIMARY, result.endpoint().host(), "等待用户授权不是线路故障，不换线");
        assertEquals(400, result.status());
        assertEquals(0, result.switches());
        assertEquals(1, transport.posts().size(), "只发了 1 次真实请求");
    }

    @Test
    void businessErrorsAreNotHidden() {
        FakeTransport transport = new FakeTransport()
                .on("xsts.auth.xboxlive.com", request -> "GET".equals(request.method())
                        ? new AuthTransport.Response(405, "", 12)
                        : new AuthTransport.Response(401,
                        "{\"XErr\":2148916233,\"Message\":\"no xbox account\"}", 15));
        ResilientAuthClient client = new ResilientAuthClient(transport, new EndpointHealth(), message -> { });

        ResilientAuthClient.Result result = client.postJson(AuthStage.XSTS, "{}", 5);

        assertEquals(401, result.status());
        assertTrue(result.body().contains("XErr"), "账号问题要原样交给上层解释");
        assertEquals(0, result.switches());
    }

    @Test
    void singleEndpointStageRetriesBeforeGivingUp() {
        AtomicInteger posts = new AtomicInteger();
        FakeTransport transport = new FakeTransport()
                .on("xsts.auth.xboxlive.com", request -> {
                    if ("GET".equals(request.method())) {
                        return new AuthTransport.Response(405, "", 12);
                    }
                    if (posts.incrementAndGet() < 3) {
                        throw new IOException("connection reset");
                    }
                    return ok("{\"Token\":\"xsts\"}", 40);
                });
        ResilientAuthClient client = new ResilientAuthClient(transport, new EndpointHealth(), message -> { });

        ResilientAuthClient.Result result = client.postJson(AuthStage.XSTS, "{}", 5);

        assertTrue(result.ok(), "同一条线路重试后成功");
        assertEquals(3, posts.get());
    }

    @Test
    void reportsEveryFailedLineWhenNothingWorks() {
        FakeTransport transport = new FakeTransport()
                .on(HOST_PRIMARY, request -> {
                    throw new IOException("dns failure");
                })
                .on(HOST_ALIAS, request -> {
                    throw new HttpTimeoutException("timed out");
                })
                .on(HOST_LEGACY, request -> new AuthTransport.Response(502, "bad gateway", 9));
        ResilientAuthClient client = new ResilientAuthClient(transport, new EndpointHealth(), message -> { });

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> client.postForm(AuthStage.DEVICE_CODE, id -> "client_id=" + id, null, 5));

        String message = error.getMessage();
        assertTrue(message.contains("3 条线路都失败"), message);
        assertTrue(message.contains(HOST_PRIMARY), message);
        assertTrue(message.contains(HOST_ALIAS), message);
        assertTrue(message.contains(HOST_LEGACY), message);
    }

    @Test
    void pinnedClientIdKeepsDeviceCodeAndPollingInPairs() {
        String extra = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
        String previous = System.getProperty("mc.auth.extraClientIds");
        try {
            System.setProperty("mc.auth.extraClientIds", extra);
            FakeTransport transport = base()
                    .on(HOST_PRIMARY, request -> "GET".equals(request.method())
                            ? probe(10)
                            : ok("{\"access_token\":\"t\"}", 20));
            ResilientAuthClient client = new ResilientAuthClient(transport, new EndpointHealth(), message -> { });

            client.postForm(AuthStage.TOKEN, id -> "client_id=" + id, extra, 5);

            assertFalse(transport.posts().isEmpty());
            for (AuthTransport.Request request : transport.posts()) {
                assertTrue(request.body().contains("client_id=" + extra),
                        "轮询只能用在发码时那个 client id：" + request.body());
            }
        } finally {
            if (previous == null) {
                System.clearProperty("mc.auth.extraClientIds");
            } else {
                System.setProperty("mc.auth.extraClientIds", previous);
            }
        }
    }
}
