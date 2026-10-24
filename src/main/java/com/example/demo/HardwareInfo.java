package com.example.demo;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * 启动时识别电脑配置，显卡部分会：
 * 1. 优先用 nvidia-smi 取真实 N 卡型号 + 驱动 + 显存；
 * 2. 否则读 Win32_VideoController，按型号/厂商过滤虚拟显卡并排优先级；
 * 3. 输出厂商（NVIDIA / AMD / Intel）、是否独显、驱动版本；
 * 4. 可选把 java.exe 的 Windows 显卡首选项设为“高性能”（HKCU，可回滚）。
 */
public final class HardwareInfo {

    public record Gpu(String name, String vendor, boolean discrete, String driver, long memoryMb) {
        public String label() {
            String prefix = name.toUpperCase(Locale.ROOT).startsWith(vendor.toUpperCase(Locale.ROOT))
                    ? "" : vendor + " ";
            return prefix + name + (discrete ? "（独显）" : "（核显/基础显示）")
                    + (driver == null || driver.isBlank() ? "" : " · 驱动 " + driver)
                    + (memoryMb > 0 ? " · " + (memoryMb / 1024) + " GB 显存" : "");
        }
    }

    public record Detect(String cpu, int logicalCores, long ramMb, Gpu gpu,
                         String os, String arch, long gameDiskFreeMb) {
        public String summary() {
            StringBuilder text = new StringBuilder();
            text.append("CPU ").append(cpu).append("（").append(logicalCores).append(" 线程）");
            text.append(" · 内存 ").append(String.format(Locale.ROOT, "%.1f GB", ramMb / 1024.0));
            text.append(" · ").append(gpu.label());
            text.append(" · ").append(os).append(" ").append(arch);
            if (gameDiskFreeMb > 0) {
                text.append(" · 游戏盘剩余 ").append(gameDiskFreeMb / 1024).append(" GB");
            }
            return text.toString();
        }
    }

    private static volatile Detect cached;

    private HardwareInfo() {
    }

    public static Detect detect() {
        return detect(null);
    }

    public static synchronized Detect detect(File gameDir) {
        if (cached != null && gameDir == null) {
            return cached;
        }
        String cpu = cpuName();
        int cores = Runtime.getRuntime().availableProcessors();
        long ram = totalMemoryMb();
        Gpu gpu = bestGpu();
        String os = System.getProperty("os.name", "Windows") + " " + System.getProperty("os.version", "");
        String arch = System.getProperty("os.arch", "");
        long free = 0;
        try {
            if (gameDir != null && gameDir.exists()) {
                free = gameDir.getUsableSpace() / (1024 * 1024);
            }
        } catch (Exception ignored) {
            // 读不到盘空间就不显示
        }
        Detect result = new Detect(cpu, cores, ram, gpu, os.trim(), arch, free);
        if (gameDir == null || cached == null) {
            cached = result;
        }
        return result;
    }

    public static Detect cached() {
        return cached;
    }

    // ================== 显卡识别 ==================

    private static Gpu bestGpu() {
        Gpu nvidia = nvidiaSmi();
        if (nvidia != null) {
            return nvidia;
        }
        List<Gpu> adapters = wmiGpus();
        if (adapters.isEmpty()) {
            return new Gpu("未知显卡", "未知", false, "", 0);
        }
        Gpu best = adapters.get(0);
        int bestScore = score(best);
        for (Gpu gpu : adapters) {
            int value = score(gpu);
            if (value > bestScore) {
                best = gpu;
                bestScore = value;
            }
        }
        return best;
    }

    /** 独显优先；虚拟显卡/远程桌面/基础显示排最后。 */
    private static int score(Gpu gpu) {
        String name = gpu.name().toLowerCase(Locale.ROOT);
        int score = 0;
        if (gpu.discrete()) score += 5;
        switch (gpu.vendor()) {
            case "NVIDIA" -> score += 4;
            case "AMD" -> score += 3;
            case "Intel" -> score += 1;
            default -> score += 0;
        }
        if (name.contains("gameviewer") || name.contains("remote") || name.contains("virtual")
                || name.contains("parsec") || name.contains("spacedesk") || name.contains("moonlight")
                || name.contains("basic display") || name.contains("vmware") || name.contains("virtualbox")
                || name.contains("hyper-v")) {
            score -= 20;
        }
        if (name.contains("uhd graphics") || name.contains("iris") || name.contains("vega")
                || name.contains("radeon graphics")) {
            score -= 2;
        }
        return score;
    }

    private static Gpu nvidiaSmi() {
        try {
            Process process = new ProcessBuilder("nvidia-smi",
                    "--query-gpu=name,driver_version,memory.total",
                    "--format=csv,noheader,nounits")
                    .redirectErrorStream(true)
                    .start();
            String line = null;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    process.getInputStream(), Charset.defaultCharset()))) {
                String value;
                while ((value = reader.readLine()) != null) {
                    if (!value.isBlank()) {
                        line = value.trim();
                        break;
                    }
                }
            }
            process.waitFor(4, TimeUnit.SECONDS);
            if (line == null) return null;
            String[] parts = line.split(",");
            if (parts.length < 2) return null;
            String name = parts[0].trim();
            String driver = parts[1].trim();
            long memory = 0;
            if (parts.length >= 3) {
                try {
                    memory = Long.parseLong(parts[2].trim());
                } catch (NumberFormatException ignored) {
                    // 显存解析失败不影响型号
                }
            }
            return new Gpu(name, "NVIDIA", true, driver, memory);
        } catch (Exception e) {
            return null;
        }
    }

    private static List<Gpu> wmiGpus() {
        List<Gpu> result = new ArrayList<>();
        try {
            Process process = new ProcessBuilder("powershell", "-NoProfile", "-NonInteractive",
                    "-WindowStyle", "Hidden", "-Command",
                    "Get-CimInstance Win32_VideoController | ForEach-Object { "
                            + "\"$($_.Name)|$($_.DriverVersion)|$($_.AdapterCompatibility)|$($_.AdapterRAM)\" }")
                    .redirectErrorStream(true)
                    .start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    process.getInputStream(), Charset.defaultCharset()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isBlank() || !line.contains("|")) continue;
                    String[] parts = line.split("\\|");
                    String name = parts.length > 0 ? parts[0].trim() : "";
                    if (name.isBlank()) continue;
                    String driver = parts.length > 1 ? parts[1].trim() : "";
                    String compat = parts.length > 2 ? parts[2].trim() : "";
                    long ram = 0;
                    if (parts.length > 3) {
                        try {
                            long bytes = Long.parseLong(parts[3].trim());
                            if (bytes > 0) ram = bytes / (1024 * 1024);
                        } catch (NumberFormatException ignored) {
                            // AdapterRAM 可能是负数或空
                        }
                    }
                    result.add(new Gpu(name, vendorOf(name, compat), discreteOf(name), driver, ram));
                }
            }
            process.waitFor(5, TimeUnit.SECONDS);
        } catch (Exception ignored) {
            // PowerShell 不可用时返回空
        }
        return result;
    }

    private static String vendorOf(String name, String compat) {
        String text = (name + " " + compat).toLowerCase(Locale.ROOT);
        if (text.contains("nvidia") || text.contains("geforce") || text.contains("quadro")
                || text.contains("rtx") || text.contains("gtx")) {
            return "NVIDIA";
        }
        if (text.contains("amd") || text.contains("ati") || text.contains("radeon")) {
            return "AMD";
        }
        if (text.contains("intel")) {
            return "Intel";
        }
        if (text.contains("microsoft") || text.contains("vmware") || text.contains("virtual")) {
            return "虚拟/基础显示";
        }
        return "未知";
    }

    private static boolean discreteOf(String name) {
        String text = name.toLowerCase(Locale.ROOT);
        return text.contains("geforce") || text.contains("rtx") || text.contains("gtx")
                || text.contains("quadro") || text.contains("radeon rx") || text.contains("radeon pro")
                || text.contains("arc a") || text.contains("arc b");
    }

    /**
     * 把指定 exe 的 Windows 显卡首选项设为“高性能”（仅写 HKCU，可删）。
     * 失败返回 false，不影响启动。
     */
    public static boolean preferHighPerformanceGpu(String exePath) {
        if (exePath == null || exePath.isBlank()) return false;
        String key = "HKCU\\Software\\Microsoft\\DirectX\\UserGpuPreferences";
        try {
            Process process = new ProcessBuilder("reg", "add", key, "/v", exePath,
                    "/t", "REG_SZ", "/d", "GpuPreference=2;", "/f")
                    .redirectErrorStream(true)
                    .start();
            boolean finished = process.waitFor(3, TimeUnit.SECONDS);
            return finished && process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    // ================== CPU / 内存 ==================

    private static String cpuName() {
        String fromReg = regQuery("HKLM\\HARDWARE\\DESCRIPTION\\System\\CentralProcessor\\0",
                "ProcessorNameString");
        if (fromReg != null && !fromReg.isBlank()) return fromReg.trim();
        String env = System.getenv("PROCESSOR_IDENTIFIER");
        if (env != null && !env.isBlank()) return env.trim();
        return "未知 CPU";
    }

    private static String regQuery(String key, String value) {
        try {
            Process process = new ProcessBuilder("reg", "query", key, "/v", value)
                    .redirectErrorStream(true)
                    .start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    process.getInputStream(), Charset.defaultCharset()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (trimmed.startsWith(value)) {
                        String[] parts = trimmed.split("\\s{2,}");
                        if (parts.length >= 3) return parts[2].trim();
                    }
                }
            }
            process.waitFor(2, TimeUnit.SECONDS);
        } catch (Exception ignored) {
            // 读取失败返回 null
        }
        return null;
    }

    public static long totalMemoryMb() {
        try {
            java.lang.management.OperatingSystemMXBean bean =
                    ManagementFactory.getOperatingSystemMXBean();
            if (bean instanceof com.sun.management.OperatingSystemMXBean os) {
                return os.getTotalMemorySize() / (1024 * 1024);
            }
        } catch (Exception ignored) {
            // 未知
        }
        return 0;
    }
}
