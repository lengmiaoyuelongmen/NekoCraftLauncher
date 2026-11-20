package com.example.demo;

import com.example.demo.core.auth.AuthEndpoint;
import com.example.demo.core.auth.AuthStage;
import com.example.demo.core.auth.EndpointHealth;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 线路健康表：延迟排序、失败冷却、结果过期。 */
class EndpointHealthTest {

    private static AuthEndpoint endpoint(String host, int priority) {
        return new AuthEndpoint(AuthStage.DEVICE_CODE, "微软 / " + host,
                "https://" + host + "/consumers/oauth2/v2.0/devicecode", "client", priority);
    }

    @Test
    void unknownEndpointsKeepPriorityOrder() {
        EndpointHealth health = new EndpointHealth();
        AuthEndpoint first = endpoint("a.example", 0);
        AuthEndpoint second = endpoint("b.example", 1);

        assertEquals(List.of(first, second), health.order(List.of(first, second)));
        assertEquals(EndpointHealth.Status.UNKNOWN, health.status(first));
        assertEquals(-1, health.latencyOf(first));
    }

    @Test
    void healthyEndpointsAreOrderedByMeasuredLatency() {
        EndpointHealth health = new EndpointHealth();
        AuthEndpoint slow = endpoint("slow.example", 0);
        AuthEndpoint fast = endpoint("fast.example", 1);

        health.markHealthy(slow, 480);
        health.markHealthy(fast, 120);

        assertEquals(List.of(fast, slow), health.order(List.of(slow, fast)),
                "实测快的排前面，而不是按人工优先级");
    }

    @Test
    void failingEndpointGoesToCooldownThenBack() {
        AtomicLong now = new AtomicLong(1_000_000L);
        EndpointHealth health = new EndpointHealth(now::get);
        AuthEndpoint broken = endpoint("broken.example", 0);
        AuthEndpoint working = endpoint("working.example", 1);

        health.markHealthy(working, 200);
        health.markUnhealthy(broken, "connect timed out");

        assertEquals(EndpointHealth.Status.UNHEALTHY, health.status(broken));
        assertEquals(List.of(working, broken), health.order(List.of(broken, working)),
                "冷却中的线路排到最后，但不会被剔除");

        now.addAndGet(EndpointHealth.COOLDOWN_MS + 1);

        assertEquals(EndpointHealth.Status.UNKNOWN, health.status(broken),
                "冷却结束后重新参与探测");
    }

    @Test
    void healthyResultExpires() {
        AtomicLong now = new AtomicLong(5_000_000L);
        EndpointHealth health = new EndpointHealth(now::get);
        AuthEndpoint endpoint = endpoint("a.example", 0);

        health.markHealthy(endpoint, 90);
        assertEquals(EndpointHealth.Status.HEALTHY, health.status(endpoint));

        now.addAndGet(EndpointHealth.HEALTHY_TTL_MS + 1);
        assertEquals(EndpointHealth.Status.UNKNOWN, health.status(endpoint), "过期后需要重新探测");
    }

    @Test
    void failureCounterAccumulates() {
        EndpointHealth health = new EndpointHealth();
        AuthEndpoint endpoint = endpoint("a.example", 0);

        health.markUnhealthy(endpoint, "HTTP 500");
        health.markUnhealthy(endpoint, "HTTP 502");

        assertEquals("HTTP 502", health.failureDetail(endpoint));
        assertTrue(health.coolingDown(endpoint));
    }

    @Test
    void resetClearsEverything() {
        EndpointHealth health = new EndpointHealth();
        AuthEndpoint endpoint = endpoint("a.example", 0);
        health.markHealthy(endpoint, 100);

        health.reset();

        assertEquals(EndpointHealth.Status.UNKNOWN, health.status(endpoint));
    }
}
