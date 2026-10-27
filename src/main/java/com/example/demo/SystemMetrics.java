package com.example.demo;

import java.lang.management.ManagementFactory;

/** 启动器实时性能指标（CPU / 内存）。 */
public final class SystemMetrics {

    public record Snapshot(double cpuLoad, double processCpuLoad, long usedMemory,
                           long totalMemory, int threads) {
    }

    private SystemMetrics() {
    }

    public static Snapshot snapshot() {
        double cpu = -1, process = -1;
        long used = 0, total = 0;
        try {
            java.lang.management.OperatingSystemMXBean bean = ManagementFactory.getOperatingSystemMXBean();
            if (bean instanceof com.sun.management.OperatingSystemMXBean os) {
                cpu = os.getCpuLoad();
                process = os.getProcessCpuLoad();
                used = os.getTotalMemorySize() - os.getFreeMemorySize();
                total = os.getTotalMemorySize();
            }
        } catch (Exception ignored) {
        }
        long heapUsed = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        if (used <= 0) used = heapUsed;
        return new Snapshot(Math.max(0, cpu), Math.max(0, process), used, total,
                Thread.activeCount());
    }

    public static String bytes(long value) {
        if (value <= 0) return "0 MB";
        double gb = value / 1024.0 / 1024.0 / 1024.0;
        if (gb >= 1) return String.format(java.util.Locale.ROOT, "%.1f GB", gb);
        return String.format(java.util.Locale.ROOT, "%.0f MB", value / 1024.0 / 1024.0);
    }

    public static String percent(double value) {
        return value < 0 ? "—" : Math.round(value * 100) + "%";
    }
}
