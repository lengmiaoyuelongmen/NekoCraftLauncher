package com.example.demo.core.app;

/**
 * 环形进度的分段几何。
 *
 * <p>安装有 9 个阶段，环上如果只有一根连续弧，看不出「现在走到第几步」。
 * 这里把 360° 等分成 N 段并留出固定间隙，纯计算、可单测；绘制交给 UI。
 */
public final class RingSegments {

    private RingSegments() {
    }

    /** 每段占的角度（含间隙）。 */
    public static double slotDegrees(int segments) {
        if (segments <= 0) {
            return 360;
        }
        return 360.0 / segments;
    }

    /** 一段实际绘制的弧长（扣掉两侧间隙）；间隙过大时收敛到 1° 以内，避免画反。 */
    public static double sweepDegrees(int segments, double gapDegrees) {
        double slot = slotDegrees(segments);
        double gap = Math.max(0, Math.min(gapDegrees, slot * 0.6));
        return Math.max(0.5, slot - gap);
    }

    /**
     * 第 index 段的起始角（JavaFX 的 90° 在正上方，顺时针为负）。
     *
     * @param segments 总段数
     * @param index    从 0 开始
     * @param gap      段间间隙（度）
     */
    public static double startAngle(int segments, int index, double gap) {
        if (segments <= 0) {
            return 90;
        }
        double slot = slotDegrees(segments);
        double halfGap = Math.max(0, Math.min(gap, slot * 0.6)) / 2;
        return 90 - index * slot - halfGap;
    }

    /** 进度落在第几段（0 基）；完成度为 1 时返回最后一段。 */
    public static int stageOfProgress(int segments, double progress) {
        if (segments <= 0) {
            return 0;
        }
        double clamped = Math.max(0, Math.min(1, progress));
        int index = (int) Math.floor(clamped * segments);
        return Math.min(segments - 1, Math.max(0, index));
    }
}
