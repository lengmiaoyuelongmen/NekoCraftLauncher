package com.example.demo.core.game;

import com.example.demo.core.download.DownloadManager;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Java 运行时管理：本地有就用，没有就按需下载（先 Azul Zulu，失败再 Adoptium Temurin）。
 *
 * <p>运行时统一放在 {@code game/java/zulu<major>/}（历史命名，配置文件与导出脚本都按这个路径找），
 * 下载源的选择与解压在 {@link JavaRuntimeIndex} / {@link JavaRuntimeInstaller}，这里只做编排。
 */
public final class JavaManager {

    private JavaManager() {
    }

    public static CompletableFuture<String> ensureZulu25JavaAsync(File gameDir, Consumer<String> logConsumer) {
        return ensureJavaAsync(gameDir, 25, logConsumer);
    }

    /** 按版本 JSON 要求的 Java 主版本准备运行时（本地没有就下载对应大版本）。 */
    public static CompletableFuture<String> ensureJavaAsync(File gameDir, int majorVersion,
                                                            Consumer<String> logConsumer) {
        return CompletableFuture.supplyAsync(() -> {
            Consumer<String> log = logConsumer == null ? message -> { } : logConsumer;
            File javaDir = new File(gameDir, "java");
            File runtimeDir = runtimeDir(gameDir, majorVersion);
            File javaExe = new File(new File(runtimeDir, "bin"), "java.exe");

            if (javaExe.isFile() && !isCracBuild(runtimeDir)) {
                log.accept("✅ 检测到本地 Azul Zulu " + majorVersion + "：" + javaExe.getAbsolutePath());
                return javaExe.getAbsolutePath();
            }

            // 本机已经装了对应大版本的 JDK 就直接用，避免为了启动游戏硬下 200MB
            File localJava = findLocalJava(majorVersion);
            if (localJava != null) {
                log.accept("✅ 使用本机已安装的 Java " + majorVersion + "：" + localJava.getAbsolutePath());
                return localJava.getAbsolutePath();
            }
            // 没找到就把「找过哪些目录、为什么跳过」写进日志：
            // 用户贴日志时能直接看出是没装、还是版本对不上，不用再猜。
            List<File> candidates = localJavaCandidates();
            log.accept("🔎 本机没有 Java " + majorVersion + "，已检查 " + candidates.size() + " 个位置");
            for (File dir : candidates) {
                File exe = new File(new File(dir, "bin"), "java.exe");
                int major = releaseMajor(dir);
                if (!exe.isFile()) {
                    log.accept("   · " + dir.getAbsolutePath() + "（没有 bin/java.exe）");
                } else if (major != majorVersion) {
                    log.accept("   · " + dir.getAbsolutePath() + "（Java " + (major <= 0 ? "未知" : major) + "）");
                }
            }
            if (javaExe.isFile()) {
                File backup = new File(javaDir, "zulu" + majorVersion + "-crac-backup-" + System.currentTimeMillis());
                log.accept("⚠ 检测到 Azul CRaC 实验版，正在替换为标准运行时…");
                if (!runtimeDir.renameTo(backup)) {
                    log.accept("⚠ 无法移动旧运行时，将继续使用 " + runtimeDir);
                }
            }

            if (!javaDir.isDirectory() && !javaDir.mkdirs()) {
                throw new IllegalStateException("无法创建 Java 目录: " + javaDir.getAbsolutePath());
            }

            log.accept("🔎 查询 Java " + majorVersion + " 运行时下载源…");
            List<JavaRuntimeIndex.Package> sources = new ArrayList<>();
            JavaRuntimeIndex.Package zulu = JavaRuntimeIndex.findZulu(majorVersion);
            if (zulu != null) {
                sources.add(zulu);
            }
            JavaRuntimeIndex.Package adoptium = JavaRuntimeIndex.findAdoptium(majorVersion);
            if (adoptium != null) {
                sources.add(adoptium);
            }
            if (sources.isEmpty()) {
                throw new IllegalStateException("没有可用的 Java " + majorVersion
                        + " 下载源（Azul / Adoptium 都查询失败），请手动在设置里指定 java.exe");
            }

            Exception lastError = null;
            for (JavaRuntimeIndex.Package candidate : sources) {
                try {
                    File installed = downloadAndInstall(javaDir, runtimeDir, candidate, log);
                    return installed.getAbsolutePath();
                } catch (Exception e) {
                    lastError = e;
                    log.accept("⚠ " + candidate.source() + " 安装失败: " + e.getMessage() + "，尝试下一个源");
                }
            }
            throw new IllegalStateException("Java " + majorVersion + " 自动补全失败: "
                    + (lastError == null ? "没有可用下载源" : lastError.getMessage())
                    + "；可以到 设置 → Java 手动指定 java.exe，或安装 JDK " + majorVersion, lastError);
        });
    }

    private static File downloadAndInstall(File javaDir, File runtimeDir,
                                           JavaRuntimeIndex.Package candidate, Consumer<String> log)
            throws Exception {
        File zip = new File(javaDir, candidate.fileName());
        log.accept("📥 " + candidate.source() + "：下载 " + candidate.fileName()
                + "（约 200MB；60 秒没有任何进度会自动换源，不会一直卡住）…");
        AtomicInteger lastPercent = new AtomicInteger();
        CompletableFuture<DownloadManager.Stats> future = CompletableFuture.supplyAsync(() ->
                DownloadManager.downloadRanged(
                        new DownloadManager.Task(candidate.url(), zip, null, -1), 4,
                        (done, total, bytes) -> {
                            if (done >= lastPercent.get() + 5) {
                                lastPercent.set(done);
                                log.accept("⏳ 下载进度: " + done + "%（" + (bytes / 1024 / 1024) + " MB）");
                            }
                        }));
        // 看门狗：60 秒没有任何字节增长就判定这次下载卡死，交给下一个源。
        // 没有它的话界面会一直停在「运行中」——之前的现场就是这样卡住的。
        long lastSize = -1;
        long lastChange = System.currentTimeMillis();
        while (!future.isDone()) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            long size = zip.length();
            if (size != lastSize) {
                lastSize = size;
                lastChange = System.currentTimeMillis();
            } else if (System.currentTimeMillis() - lastChange > 60_000L) {
                future.cancel(true);
                Files.deleteIfExists(zip.toPath());
                throw new IllegalStateException("下载停滞超过 60 秒（已下载 "
                        + (size / 1024 / 1024) + " MB），换下一个源");
            }
        }
        DownloadManager.Stats stats;
        try {
            stats = future.get(15, java.util.concurrent.TimeUnit.MINUTES);
        } catch (Exception e) {
            Files.deleteIfExists(zip.toPath());
            throw new IllegalStateException("下载未完成: " + e.getMessage(), e);
        }
        if (stats.failed > 0 || !zip.isFile() || zip.length() == 0) {
            throw new IllegalStateException("下载失败");
        }
        String expected = candidate.sha256();
        if (expected != null && !expected.isBlank()) {
            String actual = JavaRuntimeInstaller.sha256(zip);
            if (!expected.equalsIgnoreCase(actual)) {
                Files.deleteIfExists(zip.toPath());
                throw new IllegalStateException("SHA-256 校验失败（期望 " + expected + "，实际 " + actual + "）");
            }
            log.accept("🔒 SHA-256 校验通过");
        }
        log.accept("📦 正在解压 " + candidate.source() + "…");
        File javaExe = JavaRuntimeInstaller.install(zip, runtimeDir);
        Files.deleteIfExists(zip.toPath());
        log.accept("🎉 " + candidate.source() + " " + candidate.fileName() + " 部署成功");
        return javaExe;
    }

    /**
     * 在本机找一个大版本匹配的 JDK。
     *
     * <p>顺序：运行本启动器的 java → JAVA_HOME → 常见安装目录。只认 release 文件里
     * {@code JAVA_VERSION} 大版本完全匹配的，避免拿 JDK 8 去跑 1.21。
     */
    static File findLocalJava(int majorVersion) {
        return findLocalJavaIn(localJavaCandidates(), majorVersion);
    }

    /** 从候选目录里挑大版本匹配的 java.exe；抽出来便于单测用临时目录验证。 */
    static File findLocalJavaIn(List<File> candidates, int majorVersion) {
        File best = null;
        int bestScore = -1;
        for (File home : new ArrayList<>(candidates)) {
            File exe = new File(new File(home, "bin"), "java.exe");
            if (!exe.isFile() || releaseMajor(home) != majorVersion) {
                continue;
            }
            if (home.getName().toLowerCase(java.util.Locale.ROOT).contains("crac")) {
                continue;
            }
            int score = versionNumber(home);
            if (score > bestScore) {
                bestScore = score;
                best = exe;
            }
        }
        return best;
    }

    /** 常见安装位置：当前 JVM、JAVA_HOME、Program Files 下的各大发行版。 */
    private static List<File> localJavaCandidates() {
        List<File> candidates = new ArrayList<>();
        String selfHome = System.getProperty("java.home");
        if (selfHome != null) {
            candidates.add(new File(selfHome));
        }
        String javaHome = System.getenv("JAVA_HOME");
        if (javaHome != null && !javaHome.isBlank()) {
            candidates.add(new File(javaHome));
        }
        String[] roots = {
                "C:\\Program Files\\Java",
                "C:\\Program Files\\Eclipse Adoptium",
                "C:\\Program Files\\Zulu",
                "C:\\Program Files\\Microsoft",
                "C:\\Program Files\\BellSoft",
                "C:\\Program Files\\Amazon Corretto",
                "C:\\Program Files (x86)\\Java",
        };
        for (String root : roots) {
            File[] children = new File(root).listFiles();
            if (children != null) {
                candidates.addAll(List.of(children));
            }
        }
        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData != null) {
            File[] children = new File(localAppData, "Programs\\Eclipse Adoptium").listFiles();
            if (children != null) {
                candidates.addAll(List.of(children));
            }
        }
        return candidates;
    }
    private static int releaseMajor(File javaHome) {
        try {
            File release = new File(javaHome, "release");
            if (!release.isFile()) {
                return -1;
            }
            for (String line : Files.readAllLines(release.toPath())) {
                if (line.startsWith("JAVA_VERSION=")) {
                    String value = line.substring("JAVA_VERSION=".length()).replace("\"", "").trim();
                    String[] parts = value.split("\\.");
                    if (parts.length > 0) {
                        if ("1".equals(parts[0]) && parts.length > 1) {
                            return Integer.parseInt(parts[1]);
                        }
                        return Integer.parseInt(parts[0]);
                    }
                }
            }
        } catch (Exception ignored) {
            // 读不到就跳过
        }
        return -1;
    }

    private static int versionNumber(File javaHome) {
        String name = javaHome.getName().replaceAll("[^0-9.]", ".");
        String[] parts = name.split("\\.");
        StringBuilder digits = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            digits.append(part);
            if (digits.length() >= 6) {
                break;
            }
        }
        try {
            return digits.length() == 0 ? 0 : Integer.parseInt(digits.substring(0, Math.min(6, digits.length())));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    /** 运行时目录：{@code game/java/zulu<major>}。 */
    public static File runtimeDir(File gameDir, int majorVersion) {
        return new File(new File(gameDir, "java"), "zulu" + majorVersion);
    }
    /** CRaC 实验版运行时容易与旧 LWJGL/Forge 冲突，优先使用标准构建。 */
    private static boolean isCracBuild(File javaHome) {
        File release = new File(javaHome, "release");
        if (!release.isFile()) {
            return false;
        }
        try {
            String text = Files.readString(release.toPath());
            return text.contains("CRaC") || text.contains("jdk.crac");
        } catch (Exception e) {
            return false;
        }
    }
}
