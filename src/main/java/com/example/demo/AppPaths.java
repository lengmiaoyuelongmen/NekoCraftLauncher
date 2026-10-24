package com.example.demo;

import java.io.File;

/**
 * 启动器所在目录解析：
 * 1. jpackage 生成的 exe：用 jpackage.app-path；
 * 2. 普通 jar：CodeSource / sun.java.command / class 资源 URL；
 * 3. 开发环境：当前工作目录。
 * game 与 config.json 都以这个目录为基准，保证绿色版可移植。
 */
public final class AppPaths {

    private AppPaths() {
    }

    /** 可写数据目录：安装到 Program Files 时回退到 %APPDATA%\MCLauncher。 */
    public static File dataDir() {
        File base = baseDir();
        if (isProgramFiles(base)) {
            return appDataDir();
        }
        if (writable(base)) {
            base.mkdirs();
            return base;
        }
        return appDataDir();
    }

    private static boolean isProgramFiles(File dir) {
        String path = dir.getAbsolutePath().toLowerCase(java.util.Locale.ROOT);
        for (String key : new String[]{"ProgramFiles", "ProgramFiles(x86)"}) {
            String value = System.getenv(key);
            if (value != null && !value.isBlank()
                    && path.startsWith(value.toLowerCase(java.util.Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static File appDataDir() {
        String appData = System.getenv("APPDATA");
        if (appData == null || appData.isBlank()) appData = System.getProperty("user.home");
        File fallback = new File(appData, "MCLauncher");
        fallback.mkdirs();
        return fallback;
    }

    private static boolean writable(File dir) {
        try {
            if (!dir.isDirectory() && !dir.mkdirs()) return false;
            File probe = new File(dir, ".write-test-" + System.nanoTime());
            java.nio.file.Files.writeString(probe.toPath(), "ok");
            java.nio.file.Files.deleteIfExists(probe.toPath());
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    public static File baseDir() {
        String appPath = System.getProperty("jpackage.app-path");
        if (appPath != null && !appPath.isBlank()) {
            File exe = new File(appPath);
            if (exe.getParentFile() != null) return exe.getParentFile();
        }
        // 开发/绿色启动：当前目录已有 config.json 或 game 时优先当前目录
        File cwd = new File(System.getProperty("user.dir"));
        if (new File(cwd, "config.json").isFile() || new File(cwd, "game").isDirectory()) {
            return cwd;
        }
        try {
            File location = new File(AppPaths.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
            if (location.isFile()
                    && location.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".jar")) {
                return location.getParentFile();
            }
        } catch (Exception ignored) {
            // 继续尝试
        }
        try {
            String command = System.getProperty("sun.java.command", "").trim();
            if (!command.isEmpty()) {
                File jar = new File(command.split("\\s+")[0]);
                if (jar.isFile()
                        && jar.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".jar")) {
                    return jar.getAbsoluteFile().getParentFile();
                }
            }
        } catch (Exception ignored) {
            // 继续
        }
        try {
            java.net.URL url = AppPaths.class.getResource("AppPaths.class");
            if (url != null && "jar".equals(url.getProtocol())) {
                String path = url.getPath();
                int bang = path.indexOf("!/");
                if (bang > 0) {
                    String jarPath = path.substring(0, bang);
                    if (jarPath.startsWith("file:")) jarPath = jarPath.substring(5);
                    jarPath = java.net.URLDecoder.decode(jarPath, java.nio.charset.StandardCharsets.UTF_8);
                    File jar = new File(jarPath);
                    if (jar.isFile()) return jar.getParentFile();
                }
            }
        } catch (Exception ignored) {
            // 回退
        }
        return new File(System.getProperty("user.dir"));
    }
}
