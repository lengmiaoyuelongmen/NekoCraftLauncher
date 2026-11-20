package com.example.demo.core.auth;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * 线路健康表：记录每条登录线路的探测结果，决定「先试哪条、跳过哪条」。
 *
 * <p>规则很简单，但每条都有理由：
 * <ul>
 *   <li>探测结果 10 分钟内有效（{@link #HEALTHY_TTL_MS}）——太短会每次登录都重新探测，太长会把恢复的线路当死线；</li>
 *   <li>失败的线路冷却 60 秒（{@link #COOLDOWN_MS}），冷却期内排到最后但不永久拉黑——
 *       网络抖动是常态，一小时前失败不代表现在不行；</li>
 *   <li>排在前面的永远是「实测最快且健康」的线路，其次才是没测过的（按人工优先级）。</li>
 * </ul>
 *
 * <p>时间是注入的（{@link LongSupplier}），单测里可以不睡觉直接验证过期与冷却。
 */
public final class EndpointHealth {

    /** 线路状态。 */
    public enum Status {
        /** 没测过，或上次结果已过期。 */
        UNKNOWN,
        /** 最近一次探测/请求成功。 */
        HEALTHY,
        /** 最近一次失败，正在冷却。 */
        UNHEALTHY
    }

    /** 一条线路的最近一次结果。 */
    public record Snapshot(Status status, long latencyMs, long checkedAtMillis, int failures, String detail) {
    }

    /** 健康结果的有效期。 */
    public static final long HEALTHY_TTL_MS = 10 * 60_000L;
    /** 失败后的冷却期。 */
    public static final long COOLDOWN_MS = 60_000L;

    private final Map<String, Snapshot> states = new ConcurrentHashMap<>();
    private final LongSupplier clock;

    public EndpointHealth() {
        this(System::currentTimeMillis);
    }

    public EndpointHealth(LongSupplier clock) {
        this.clock = clock;
    }

    /** 请求成功：记下延迟，清空失败计数。 */
    public void markHealthy(AuthEndpoint endpoint, long latencyMs) {
        states.put(endpoint.key(),
                new Snapshot(Status.HEALTHY, Math.max(0, latencyMs), clock.getAsLong(), 0, ""));
    }

    /** 请求失败：累加失败次数并进入冷却。 */
    public void markUnhealthy(AuthEndpoint endpoint, String reason) {
        Snapshot old = states.get(endpoint.key());
        int failures = old == null ? 1 : old.failures() + 1;
        states.put(endpoint.key(), new Snapshot(Status.UNHEALTHY, 0, clock.getAsLong(), failures,
                reason == null ? "" : reason));
    }

    /** 当前状态（会把过期的 HEALTHY 视为 UNKNOWN、把冷却结束的 UNHEALTHY 视为 UNKNOWN）。 */
    public Status status(AuthEndpoint endpoint) {
        Snapshot snapshot = states.get(endpoint.key());
        if (snapshot == null) {
            return Status.UNKNOWN;
        }
        long age = clock.getAsLong() - snapshot.checkedAtMillis();
        if (snapshot.status() == Status.HEALTHY) {
            return age <= HEALTHY_TTL_MS ? Status.HEALTHY : Status.UNKNOWN;
        }
        if (snapshot.status() == Status.UNHEALTHY) {
            return age < COOLDOWN_MS ? Status.UNHEALTHY : Status.UNKNOWN;
        }
        return Status.UNKNOWN;
    }

    /** 实测延迟；没测过返回 -1。 */
    public long latencyOf(AuthEndpoint endpoint) {
        Snapshot snapshot = states.get(endpoint.key());
        return snapshot == null || snapshot.status() != Status.HEALTHY ? -1 : snapshot.latencyMs();
    }

    /** 上次失败原因，给日志用。 */
    public String failureDetail(AuthEndpoint endpoint) {
        Snapshot snapshot = states.get(endpoint.key());
        return snapshot == null ? "" : snapshot.detail();
    }

    /** 是否正在冷却（冷却中的线路仍然会排在最后被尝试，不作永久淘汰）。 */
    public boolean coolingDown(AuthEndpoint endpoint) {
        return status(endpoint) == Status.UNHEALTHY;
    }

    /**
     * 排序：健康的按实测延迟升序 → 没测过的按人工优先级 → 冷却中的按人工优先级垫底。
     *
     * <p>这样「先检测后使用」不需要额外步骤：只要探测结果写进表，下一次排序就自动生效。
     */
    public List<AuthEndpoint> order(List<AuthEndpoint> candidates) {
        List<AuthEndpoint> healthy = new ArrayList<>();
        List<AuthEndpoint> unknown = new ArrayList<>();
        List<AuthEndpoint> cooling = new ArrayList<>();
        for (AuthEndpoint endpoint : candidates) {
            switch (status(endpoint)) {
                case HEALTHY -> healthy.add(endpoint);
                case UNHEALTHY -> cooling.add(endpoint);
                default -> unknown.add(endpoint);
            }
        }
        healthy.sort(Comparator.comparingLong(this::latencyOf)
                .thenComparingInt(AuthEndpoint::priority));
        unknown.sort(Comparator.comparingInt(AuthEndpoint::priority));
        cooling.sort(Comparator.comparingInt(AuthEndpoint::priority));

        List<AuthEndpoint> ordered = new ArrayList<>(candidates.size());
        ordered.addAll(healthy);
        ordered.addAll(unknown);
        ordered.addAll(cooling);
        return ordered;
    }

    /** 清空（单测或用户在设置里点「重新检测」时用）。 */
    public void reset() {
        states.clear();
    }
}
