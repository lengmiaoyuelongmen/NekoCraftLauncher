package com.example.demo.core.app;

/**
 * 骨架屏的显示时长计算。
 *
 * <p>为什么需要它：本地加载常常只要 30–80ms，骨架屏一闪而过，人眼看不到，
 * 自动化的截图也抓不到 —— 「看起来没实现」和「实现了但太快」在这里是同一件事。
 * 所以给骨架屏一个最短可见时间：短于它就把隐藏动作推迟到满 {@link #MIN_VISIBLE_MS}，
 * 顺便把真实可见时长写进日志（{@code [UI] 骨架屏 312ms}），可被冒烟脚本和单测断言。
 *
 * <p>纯计算、不依赖 JavaFX，UI 只负责按返回值排一个 PauseTransition。
 */
public final class SkeletonTiming {

    /** 最短可见时间（毫秒）。低于这个值的加载会被「撑」到这个长度。 */
    public static final long MIN_VISIBLE_MS = 300L;

    private SkeletonTiming() {
    }

    /** 还要等多久才允许隐藏；已经够久返回 0。 */
    public static long hideDelayMs(long shownAtMs, long nowMs) {
        long elapsed = Math.max(0L, nowMs - shownAtMs);
        return Math.max(0L, MIN_VISIBLE_MS - elapsed);
    }

    /** 应用延迟后骨架屏真正可见的时长。 */
    public static long visibleMs(long shownAtMs, long nowMs) {
        long elapsed = Math.max(0L, nowMs - shownAtMs);
        return Math.max(MIN_VISIBLE_MS, elapsed);
    }

    /** 给冒烟/日志用的一行。 */
    public static String report(long visibleMs) {
        return "[UI] 骨架屏 " + visibleMs + "ms";
    }
}
