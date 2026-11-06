package com.example.demo;

import com.example.demo.core.launch.IntegrityCachePolicy;
import com.example.demo.core.launch.LaunchGuards;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 启动前置拦截与完整性检查缓存策略。 */
class LaunchGuardsTest {

    @Test
    void gameProcessWinsOverLaunching() {
        assertEquals(LaunchGuards.Block.GAME_RUNNING, LaunchGuards.blockReason(true, false));
        assertEquals(LaunchGuards.Block.GAME_RUNNING, LaunchGuards.blockReason(true, true));
        assertEquals(LaunchGuards.Block.LAUNCHING, LaunchGuards.blockReason(false, true));
        assertEquals(LaunchGuards.Block.NONE, LaunchGuards.blockReason(false, false));
    }

    @Test
    void messagesMatchTheReason() {
        assertTrue(LaunchGuards.logMessage(LaunchGuards.Block.GAME_RUNNING).contains("已有游戏在运行"));
        assertTrue(LaunchGuards.logMessage(LaunchGuards.Block.LAUNCHING).contains("取消启动"));
        assertEquals("", LaunchGuards.logMessage(LaunchGuards.Block.NONE));
        assertTrue(LaunchGuards.toastMessage(LaunchGuards.Block.GAME_RUNNING).contains("结束当前游戏"));
        assertEquals("", LaunchGuards.toastMessage(LaunchGuards.Block.LAUNCHING));
    }

    @Test
    void cacheReusableOnlyForSameProfileWithinTtl() {
        long now = 10_000_000L;
        assertTrue(IntegrityCachePolicy.reusable(true, "1.12.2", now - 60_000L, "1.12.2", now));
        assertFalse(IntegrityCachePolicy.reusable(true, "1.12.2", now - 60_000L, "1.18.2", now),
                "实例不同不能复用");
        assertFalse(IntegrityCachePolicy.reusable(false, "1.12.2", now - 60_000L, "1.12.2", now),
                "没有缓存结果");
        assertFalse(IntegrityCachePolicy.reusable(true, "1.12.2",
                now - IntegrityCachePolicy.TTL_MS - 1, "1.12.2", now), "超过 5 分钟");
        assertFalse(IntegrityCachePolicy.reusable(true, null, now, "1.12.2", now));
        assertFalse(IntegrityCachePolicy.reusable(true, "1.12.2", now + 5_000L, "1.12.2", now),
                "时间倒退（改过系统时间）也当失效");
    }

    @Test
    void ageSecondsNeverZero() {
        long now = 1_000_000L;
        assertEquals(1L, IntegrityCachePolicy.ageSeconds(now, now));
        assertEquals(90L, IntegrityCachePolicy.ageSeconds(now - 90_000L, now));
    }
}
