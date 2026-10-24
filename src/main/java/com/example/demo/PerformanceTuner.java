package com.example.demo;

import java.util.ArrayList;
import java.util.List;

/**
 * 智能性能分配：参考 Pavise 一类游戏加速器的思路（进程优先级、GC 调优、
 * 工作集/计时器类优化），这里只做安全、可回退的部分：
 * - 按堆大小生成 G1GC 参数
 * - 游戏进程优先级（启动后由 PowerShell 设置）
 * - 不写注册表、不改系统服务、不清理 standby list
 */
public final class PerformanceTuner {

    public record Plan(int memoryMb, String jvmArgs, String summary) {
    }

    private PerformanceTuner() {
    }

    public static Plan plan(int memoryMb) {
        int heap = Math.max(1024, Math.min(16384, memoryMb));
        String region = regionSize(heap);
        List<String> flags = new ArrayList<>();
        flags.add("-XX:+UseG1GC");
        flags.add("-XX:+ParallelRefProcEnabled");
        flags.add("-XX:MaxGCPauseMillis=50");
        flags.add("-XX:+UnlockExperimentalVMOptions");
        flags.add("-XX:G1NewSizePercent=20");
        flags.add("-XX:G1MaxNewSizePercent=40");
        flags.add("-XX:G1HeapRegionSize=" + region);
        flags.add("-XX:G1ReservePercent=20");
        flags.add("-XX:InitiatingHeapOccupancyPercent=15");
        flags.add("-XX:+DisableExplicitGC");
        if (heap <= 8192) {
            flags.add("-XX:+AlwaysPreTouch");
        }
        String jvm = String.join(" ", flags);
        String summary = "堆 " + heap + " MB · G1 区域 " + region + " · "
                + (heap <= 8192 ? "预触碰堆" : "不预触碰堆") + " · GC 目标 50ms";
        return new Plan(heap, jvm, summary);
    }

    /** G1 区域大小通常取堆的 1/2048，限制在 1–32 MB 且为 2 的幂。 */
    private static String regionSize(int heapMb) {
        int raw = Math.max(1, heapMb / 2048);
        int size = 1;
        while (size * 2 <= raw && size < 32) {
            size *= 2;
        }
        return size + "M";
    }

    /** 把智能参数合并进已有 JVM 参数（已包含同项则不重复）。 */
    public static String merge(String existing, String smart) {
        String base = existing == null ? "" : existing.trim();
        List<String> merged = new ArrayList<>();
        if (!base.isBlank()) {
            for (String part : base.split("\\s+")) {
                if (!part.isBlank()) merged.add(part);
            }
        }
        if (smart != null && !smart.isBlank()) {
            for (String part : smart.split("\\s+")) {
                if (part.isBlank()) continue;
                String key = part.split("=")[0];
                boolean present = merged.stream().anyMatch(item -> item.split("=")[0].equals(key));
                if (!present) merged.add(part);
            }
        }
        return String.join(" ", merged);
    }
}
