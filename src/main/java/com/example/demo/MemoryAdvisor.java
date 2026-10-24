package com.example.demo;

import java.io.File;
import java.lang.management.ManagementFactory;

/**
 * 智能内存分配：按本机内存给基础值，再按当前实例的模组数量加一点余量，
 * 最后限制在 1–12 GB 且按 256 MB 取整。
 */
public final class MemoryAdvisor {

    public record Suggestion(int memoryMb, String reason) {
    }

    private static final long MB = 1024L * 1024L;

    private MemoryAdvisor() {
    }

    /**
     * @param modsDir 当前实例的 mods 目录（null 或不存在时按 0 个模组算）
     */
    public static Suggestion suggest(File modsDir) {
        long totalMb = totalMemoryMb();
        int base;
        String baseReason;
        if (totalMb <= 0) {
            base = 4096;
            baseReason = "读不到本机内存，按 4 GB 起步";
        } else {
            long totalGb = totalMb / 1024;
            double ratio = totalGb <= 8 ? 0.50 : totalGb <= 16 ? 0.45 : 0.35;
            base = (int) (totalMb * ratio);
            baseReason = "本机 " + totalGb + " GB × " + (int) Math.round(ratio * 100) + "%";
        }

        int mods = countMods(modsDir);
        int extra = Math.min(2048, (mods / 50) * 512);
        int value = base + extra;
        value = Math.max(1024, Math.min(12288, value));
        value = (value + 255) / 256 * 256;

        StringBuilder reason = new StringBuilder(baseReason).append(" = ").append(base).append(" MB");
        if (mods > 0) {
            reason.append("；模组 ").append(mods).append(" 个");
            if (extra > 0) {
                reason.append("，加 ").append(extra).append(" MB");
            } else {
                reason.append("（不足 50 个，不额外加）");
            }
        }
        reason.append(" → 建议 ").append(value).append(" MB");
        return new Suggestion(value, reason.toString());
    }

    public static int countMods(File modsDir) {
        if (modsDir == null || !modsDir.isDirectory()) return 0;
        File[] files = modsDir.listFiles((dir, name) -> {
            String lower = name.toLowerCase();
            return lower.endsWith(".jar") && !lower.endsWith(".disabled");
        });
        return files == null ? 0 : files.length;
    }

    public static long totalMemoryMb() {
        try {
            java.lang.management.OperatingSystemMXBean bean =
                    ManagementFactory.getOperatingSystemMXBean();
            if (bean instanceof com.sun.management.OperatingSystemMXBean os) {
                return os.getTotalMemorySize() / MB;
            }
        } catch (Exception ignored) {
            // 读不到按未知处理
        }
        return -1;
    }
}
