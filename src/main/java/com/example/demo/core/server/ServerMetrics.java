package com.example.demo.core.server;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 选中服务端的运行数据采样与定时任务读写。
 *
 * <p>采样只做两件事：CPU（{@link ProcessHandle#info()} 的累计 CPU 时间差分）和内存
 * （`tasklist` 的 Mem Usage，Windows 上约 20ms，5 秒一次可以接受）。
 * 面板上画的曲线就是这两条 —— 不引第三方库，也不假装能拿到更细的数据。
 */
public final class ServerMetrics {

    private ServerMetrics() {
    }

    /** 一次采样结果；cpuPercent 可能 &gt; 100（多核）。 */
    public record Sample(double cpuPercent, long memoryMb, long atMillis) {
    }

    /** CPU 采样器：保存上一次的累计 CPU 时间，用来算百分比。 */
    public static final class CpuSampler {
        private long lastCpuNanos = -1;
        private long lastAtMillis = -1;

        /** @return CPU 百分比；第一次调用返回 0（没有基准）。 */
        public double sample(Process process) {
            if (process == null || !process.isAlive()) {
                return 0;
            }
            long now = System.currentTimeMillis();
            long cpuNanos = process.toHandle().info().totalCpuDuration().orElse(java.time.Duration.ZERO)
                    .toNanos();
            double percent = 0;
            if (lastCpuNanos >= 0 && now > lastAtMillis) {
                double cpuMs = (cpuNanos - lastCpuNanos) / 1_000_000.0;
                double wallMs = now - lastAtMillis;
                percent = wallMs > 0 ? Math.max(0, Math.min(1600, cpuMs / wallMs * 100.0)) : 0;
            }
            lastCpuNanos = cpuNanos;
            lastAtMillis = now;
            return percent;
        }
    }

    /** 读进程内存（MB）；读不到返回 -1。 */
    public static long memoryMb(long pid) {
        try {
            ProcessBuilder builder = new ProcessBuilder("tasklist", "/FI", "PID eq " + pid,
                    "/FO", "CSV", "/NH");
            builder.redirectErrorStream(true);
            Process process = builder.start();
            String output = new String(process.getInputStream().readAllBytes(),
                    java.nio.charset.Charset.defaultCharset());
            process.waitFor();
            // "java.exe","1234","Console","1","1,234,567 K"
            java.util.regex.Matcher matcher = java.util.regex.Pattern
                    .compile("\"([^\"]*)\",\"(\\d+)\",\"[^\"]*\",\"\\d+\",\"([\\d.,]+) K\"")
                    .matcher(output);
            if (matcher.find()) {
                String raw = matcher.group(3).replace(",", "").replace(".", "");
                return Long.parseLong(raw) / 1024L;
            }
        } catch (Exception ignored) {
            // 读不到就算了
        }
        return -1;
    }

    // ==================== 定时任务 ====================

    /** 定时任务配置：0 表示关闭。 */
    public record Schedule(int restartMinutes, int backupMinutes) {
        public static Schedule none() {
            return new Schedule(0, 0);
        }

        public boolean anyEnabled() {
            return restartMinutes > 0 || backupMinutes > 0;
        }
    }

    public static File scheduleFile(File serverDir) {
        return new File(serverDir, "neko-schedule.json");
    }

    public static Schedule readSchedule(File serverDir) {
        File file = scheduleFile(serverDir);
        if (!file.isFile()) {
            return Schedule.none();
        }
        try {
            String json = java.nio.file.Files.readString(file.toPath(),
                    java.nio.charset.StandardCharsets.UTF_8);
            var object = com.google.gson.JsonParser.parseString(json).getAsJsonObject();
            return new Schedule(
                    object.has("restartMinutes") ? object.get("restartMinutes").getAsInt() : 0,
                    object.has("backupMinutes") ? object.get("backupMinutes").getAsInt() : 0);
        } catch (Exception e) {
            return Schedule.none();
        }
    }

    public static void writeSchedule(File serverDir, Schedule schedule) {
        try {
            var object = new com.google.gson.JsonObject();
            object.addProperty("restartMinutes", Math.max(0, schedule.restartMinutes()));
            object.addProperty("backupMinutes", Math.max(0, schedule.backupMinutes()));
            java.nio.file.Files.writeString(scheduleFile(serverDir).toPath(),
                    new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(object),
                    java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            // 写不进去不影响开服
        }
    }

    /** 备份 world 到 backups/ 下，返回生成的 zip。 */
    public static File backupWorld(File serverDir) throws Exception {
        File world = new File(serverDir, "world");
        if (!world.isDirectory()) {
            throw new IllegalStateException("没有 world 目录（还没生成过世界？）");
        }
        File backups = new File(serverDir, "backups");
        java.nio.file.Files.createDirectories(backups.toPath());
        String stamp = new java.text.SimpleDateFormat("yyyyMMdd-HHmmss").format(new java.util.Date());
        File out = new File(backups, "world-" + stamp + ".zip");
        com.example.demo.core.download.ZipUtil.zipDirectory(world, out);
        // 只保留最近 10 份，避免把磁盘塞满
        File[] files = backups.listFiles((dir, name) -> name.startsWith("world-") && name.endsWith(".zip"));
        if (files != null && files.length > 10) {
            List<File> sorted = new ArrayList<>(List.of(files));
            sorted.sort(java.util.Comparator.comparingLong(File::lastModified));
            for (int i = 0; i < sorted.size() - 10; i++) {
                //noinspection ResultOfMethodCallIgnored
                sorted.get(i).delete();
            }
        }
        return out;
    }
}
