package com.example.demo;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 扫描本机已安装的 Java（对标 PCL 的 Java 自动选择）。
 * 优先读 JDK 目录里的 release 文件，读不到才去执行 java -version。
 */
public final class JavaScanner {

    public record JavaInfo(File exe, int major, String vendor, String version) {
        public String label() {
            String v = version == null || version.isBlank() ? String.valueOf(major) : version;
            return "Java " + major + "（" + v + "）· " + (vendor == null || vendor.isBlank() ? "未知厂商" : vendor);
        }
    }

    private JavaScanner() {
    }

    /** 扫描结果缓存（2 分钟）：设置页 / 启动流程会重复调用，没必要每次都扫盘。 */
    private static List<JavaInfo> cached;
    private static long cachedAt;
    private static String cachedDir;

    public static List<JavaInfo> scan(File gameDir) {
        return scan(gameDir, false);
    }

    /**
     * @param force true 时忽略缓存（设置页「重新检测」按钮用）
     */
    public static List<JavaInfo> scan(File gameDir, boolean force) {
        String key = gameDir == null ? "" : gameDir.getAbsolutePath();
        long now = System.currentTimeMillis();
        if (!force && cached != null && key.equals(cachedDir) && now - cachedAt < 120_000) {
            return cached;
        }
        List<JavaInfo> list = scanNow(gameDir);
        cached = list;
        cachedAt = now;
        cachedDir = key;
        return list;
    }

    private static List<JavaInfo> scanNow(File gameDir) {
        Map<String, JavaInfo> found = new LinkedHashMap<>();
        List<File> homes = new ArrayList<>();

        addHome(homes, System.getenv("JAVA_HOME"));
        String pf = System.getenv("ProgramFiles");
        if (pf != null) {
            File p = new File(pf);
            for (String vendor : new String[]{"Java", "Zulu", "Eclipse Adoptium", "Microsoft", "BellSoft",
                    "Amazon Corretto", "Semeru", "Temurin"}) {
                scanChildren(homes, new File(p, vendor));
            }
        }
        String pf86 = System.getenv("ProgramFiles(x86)");
        if (pf86 != null) scanChildren(homes, new File(pf86, "Java"));
        if (gameDir != null) scanChildren(homes, new File(gameDir, "java"));

        for (File home : homes) {
            File exe = new File(home, "bin/java.exe");
            if (!exe.isFile()) continue;
            JavaInfo info = read(home, exe);
            if (info == null) continue;
            found.putIfAbsent(exe.getAbsolutePath().toLowerCase(), info);
        }

        List<JavaInfo> list = new ArrayList<>(found.values());
        list.sort(Comparator.comparingInt(JavaInfo::major).reversed()
                .thenComparing(JavaInfo::label));
        return list;
    }

    /** 选最合适的 Java：优先同大版本，其次取略高版本里最低的。 */
    public static JavaInfo bestMatch(File gameDir, int requiredMajor) {
        List<JavaInfo> all = scan(gameDir);
        JavaInfo exact = null;
        JavaInfo higher = null;
        for (JavaInfo info : all) {
            if (info.major() == requiredMajor) {
                if (exact == null) exact = info;
            } else if (info.major() > requiredMajor) {
                if (higher == null || info.major() < higher.major()) higher = info;
            }
        }
        if (exact != null) return exact;
        if (higher != null) return higher;
        // 没有完全匹配的，挑一个不比要求低太多但可用的（>= 8 且 >= required-4）
        for (JavaInfo info : all) {
            if (info.major() >= requiredMajor) return info;
            if (info.major() >= requiredMajor - 4 && info.major() >= 8) return info;
        }
        return all.isEmpty() ? null : all.get(0);
    }

    private static void addHome(List<File> homes, String path) {
        if (path == null || path.isBlank()) return;
        File dir = new File(path);
        if (new File(dir, "bin/java.exe").isFile()) homes.add(dir);
    }

    private static void scanChildren(List<File> homes, File parent) {
        File[] children = parent.listFiles(File::isDirectory);
        if (children == null) return;
        for (File child : children) {
            if (new File(child, "bin/java.exe").isFile()) homes.add(child);
        }
    }

    private static JavaInfo read(File home, File exe) {
        String version = null;
        String implementor = null;
        File release = new File(home, "release");
        if (release.isFile()) {
            try {
                for (String line : Files.readAllLines(release.toPath(), StandardCharsets.UTF_8)) {
                    if (line.startsWith("JAVA_VERSION=")) {
                        version = line.substring("JAVA_VERSION=".length()).replace("\"", "").trim();
                    } else if (line.startsWith("IMPLEMENTOR=")) {
                        implementor = line.substring("IMPLEMENTOR=".length()).replace("\"", "").trim();
                    }
                }
            } catch (Exception ignored) {
            }
        }
        if (version == null) {
            version = home.getName().replaceAll("^[a-zA-Z_-]+", "");
        }
        int major = majorOf(version);
        if (major <= 0) {
            major = majorOf(home.getName());
        }
        if (major <= 0) return null;
        if (implementor == null || implementor.isBlank()) implementor = vendorOf(home);
        return new JavaInfo(exe, major, implementor, version);
    }

    private static int majorOf(String version) {
        if (version == null) return -1;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)(?:\\.(\\d+))?").matcher(version);
        if (!m.find()) return -1;
        int first = Integer.parseInt(m.group(1));
        if (first == 1 && m.group(2) != null) return Integer.parseInt(m.group(2));
        return first;
    }

    private static String vendorOf(File home) {
        String name = home.getName().toLowerCase();
        String parent = home.getParentFile() == null ? "" : home.getParentFile().getName().toLowerCase();
        if (name.contains("zulu") || parent.contains("zulu")) return "Azul Zulu";
        if (parent.contains("adoptium") || name.contains("temurin") || parent.contains("temurin")) return "Eclipse Temurin";
        if (parent.contains("corretto") || name.contains("corretto")) return "Amazon Corretto";
        if (parent.contains("microsoft")) return "Microsoft";
        if (parent.contains("bellsoft") || name.contains("liberica")) return "BellSoft Liberica";
        if (parent.contains("semeru")) return "IBM Semeru";
        if (parent.contains("java")) return "Oracle / OpenJDK";
        return "未知厂商";
    }
}
