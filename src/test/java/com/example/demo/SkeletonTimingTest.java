package com.example.demo;

import com.example.demo.core.app.SkeletonTiming;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 骨架屏最短可见时间：太快也撑到 300ms，够久就不额外等。 */
class SkeletonTimingTest {

    @Test
    void fastLoadsAreHeldToTheMinimumVisibleTime() {
        assertEquals(300, SkeletonTiming.hideDelayMs(1_000, 1_000), "刚显示就结束时补满 300ms");
        assertEquals(220, SkeletonTiming.hideDelayMs(1_000, 1_080), "80ms 结束 → 再等 220ms");
        assertEquals(1, SkeletonTiming.hideDelayMs(1_000, 1_299));
    }

    @Test
    void slowLoadsHideImmediately() {
        assertEquals(0, SkeletonTiming.hideDelayMs(1_000, 1_300));
        assertEquals(0, SkeletonTiming.hideDelayMs(1_000, 4_500));
    }

    @Test
    void visibleTimeIsNeverShorterThanTheMinimum() {
        assertEquals(SkeletonTiming.MIN_VISIBLE_MS, SkeletonTiming.visibleMs(1_000, 1_020),
                "实际只加载了 20ms，但骨架屏真的显示了 300ms");
        assertEquals(1_500, SkeletonTiming.visibleMs(1_000, 2_500));
    }

    @Test
    void clockGoingBackwardsDoesNotProduceNegativeTimes() {
        assertEquals(SkeletonTiming.MIN_VISIBLE_MS, SkeletonTiming.hideDelayMs(2_000, 1_000));
        assertEquals(SkeletonTiming.MIN_VISIBLE_MS, SkeletonTiming.visibleMs(2_000, 1_000));
    }

    @Test
    void reportLineIsParseable() {
        String line = SkeletonTiming.report(312);
        assertTrue(line.startsWith("[UI] 骨架屏 "), line);
        assertTrue(line.endsWith("ms"), line);
        assertTrue(line.contains("312"), line);
    }
}
