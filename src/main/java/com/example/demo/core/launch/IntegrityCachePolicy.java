package com.example.demo.core.launch;

/**
 * 启动前完整性检查结果的复用规则：同一个实例、5 分钟内查过就直接复用，
 * 避免每次启动都把几百个文件重新扫一遍。
 */
public final class IntegrityCachePolicy {

    /** 复用窗口：5 分钟。 */
    public static final long TTL_MS = 5 * 60 * 1000L;

    private IntegrityCachePolicy() {
    }

    public static boolean reusable(boolean hasReport, String cachedProfileId, long cachedAt,
                                   String profileId, long now) {
        return reusable(hasReport, cachedProfileId, cachedAt, profileId, now, TTL_MS);
    }

    public static boolean reusable(boolean hasReport, String cachedProfileId, long cachedAt,
                                   String profileId, long now, long ttlMs) {
        if (!hasReport || profileId == null || cachedProfileId == null) {
            return false;
        }
        if (!profileId.equals(cachedProfileId)) {
            return false;
        }
        long age = now - cachedAt;
        return age >= 0 && age < ttlMs;
    }

    /** 复用时给日志用的“多少秒前”文案。 */
    public static long ageSeconds(long cachedAt, long now) {
        return Math.max(1L, (now - cachedAt) / 1000L);
    }
}
