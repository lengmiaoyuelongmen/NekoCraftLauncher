package com.example.demo;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 可选的外部下载引擎：复用 Motrix 自带的 aria2c.exe 做多连接下载。
 *
 * <p>只在检测到 aria2c 时启用；失败会自动回退到内置 HTTP 下载器，所以关掉 Motrix 也不影响使用。
 */
public final class Aria2Downloader {

    /** aria2 每秒打印的进度行，例如 [#1 SIZE:1.5MiB/29.2MiB(5%) CN:16 DL:587KiB ETA:48s]。 */
    private static final Pattern PROGRESS =
            Pattern.compile(
                    "SIZE:([0-9.]+)([KMGT]?i?B)/([0-9.]+)([KMGT]?i?B)\\(([0-9]+)%\\).*?DL:([0-9.]+)([KMGT]?i?B)");

    private static volatile File cached;
    private static volatile boolean looked;

    private Aria2Downloader() {
    }

    /** 找 aria2c：系统属性 → PATH → Motrix / 常见安装位置。 */
    public static synchronized File locate() {
        if (looked) {
            return cached;
        }
        looked = true;
        List<String> candidates = new ArrayList<>();
        String override = System.getProperty("neko.aria2");
        if (override != null && !override.isBlank()) {
            candidates.add(override);
        }
        // 内置引擎优先：从 jar 里解出 aria2c.exe，装没装 Motrix 都能用
        File bundled = ensureBundled();
        if (bundled != null) {
            cached = bundled;
            return cached;
        }
        String env = System.getenv("ARIA2C");
        if (env != null && !env.isBlank()) {
            candidates.add(env);
        }
        String programFiles = System.getenv("ProgramFiles");
        String programFilesX86 = System.getenv("ProgramFiles(x86)");
        String localAppData = System.getenv("LOCALAPPDATA");
        if (programFiles != null) {
            candidates.add(programFiles + "\\Motrix\\resources\\extra\\win32\\x64\\aria2c.exe");
            candidates.add(programFiles + "\\Motrix\\resources\\extra\\win32\\ia32\\aria2c.exe");
            candidates.add(programFiles + "\\Motrix\\aria2c.exe");
            candidates.add(programFiles + "\\aria2\\aria2c.exe");
        }
        if (programFilesX86 != null) {
            candidates.add(programFilesX86 + "\\Motrix\\resources\\extra\\win32\\x64\\aria2c.exe");
        }
        if (localAppData != null) {
            candidates.add(localAppData + "\\Programs\\Motrix\\resources\\extra\\win32\\x64\\aria2c.exe");
            candidates.add(localAppData + "\\Motrix\\aria2c.exe");
        }
        candidates.add("aria2c.exe");
        candidates.add("aria2c");
        for (String path : candidates) {
            if (path == null || path.isBlank()) {
                continue;
            }
            File file = new File(path);
            if (file.isFile()) {
                cached = file;
                return cached;
            }
            if (!path.contains("\\") && !path.contains("/")) {
                File onPath = findOnPath(path);
                if (onPath != null) {
                    cached = onPath;
                    return cached;
                }
            }
        }
        return null;
    }

    public static boolean available() {
        return locate() != null;
    }

    public static String describe() {
        File file = locate();
        if (file == null) {
            return "内置 HTTP 下载器";
        }
        boolean bundled = file.getAbsolutePath().contains("engine" + File.separator + "aria2c.exe");
        return (bundled ? "内置 aria2（已随启动器打包） · " : "aria2 · ") + file.getAbsolutePath();
    }

    /**
     * 把打包在 jar 里的 aria2c.exe 解到可写目录并复用。
     *
     * <p>资源路径 {@code /engine/aria2c.exe}（来自 Motrix 的 1.37.0 构建）。
     */
    private static File ensureBundled() {
        try (java.io.InputStream in =
                Aria2Downloader.class.getResourceAsStream("/engine/aria2c.exe")) {
            if (in == null) {
                return null;
            }
            byte[] bytes = in.readAllBytes();
            if (bytes.length == 0) {
                return null;
            }
            File dir = new File(AppPaths.dataDir(), "engine");
            if (!dir.isDirectory() && !dir.mkdirs()) {
                dir = new File(System.getProperty("java.io.tmpdir"), "neko-engine");
                dir.mkdirs();
            }
            File exe = new File(dir, "aria2c.exe");
            if (exe.isFile() && exe.length() == bytes.length) {
                return exe;
            }
            File temp = new File(dir, "aria2c.exe.tmp");
            try (java.io.OutputStream out = new java.io.FileOutputStream(temp)) {
                out.write(bytes);
            }
            if (exe.exists() && !exe.delete()) {
                return temp.isFile() ? temp : null;
            }
            return temp.renameTo(exe) ? exe : (temp.isFile() ? temp : null);
        } catch (Exception e) {
            return null;
        }
    }

    private static File findOnPath(String name) {
        String pathEnv = System.getenv("PATH");
        if (pathEnv == null) {
            return null;
        }
        for (String dir : pathEnv.split(File.pathSeparator)) {
            if (dir.isBlank()) {
                continue;
            }
            File file = new File(dir, name);
            if (file.isFile()) {
                return file;
            }
        }
        return null;
    }

    private record Units(double value, String unit) {
        long bytes() {
            double factor =
                    switch (unit.toUpperCase(Locale.ROOT).replace("IB", "B")) {
                        case "KB" -> 1024.0;
                        case "MB" -> 1024.0 * 1024;
                        case "GB" -> 1024.0 * 1024 * 1024;
                        case "TB" -> 1024.0 * 1024 * 1024 * 1024;
                        default -> 1.0;
                    };
            return (long) (value * factor);
        }
    }

    private static Units parse(String number, String unit) {
        double v;
        try {
            v = Double.parseDouble(number);
        } catch (Exception e) {
            v = 0;
        }
        return new Units(v, unit);
    }

    /**
     * 用 aria2c 下载单个文件（支持断点续传与 SHA1 校验）。
     *
     * @param onProgress 每收到一行进度回调 (已下载字节, 总字节, 速度字节/秒)
     * @return 成功返回 true；aria2 不可用、失败或被取消返回 false（调用方回退到内置下载器）
     */
    public static boolean download(
            File target,
            String url,
            String sha1,
            long size,
            Progress onProgress,
            BooleanSupplier cancelled) {
        File aria2 = locate();
        if (aria2 == null || target == null || url == null) {
            return false;
        }
        File parent = target.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            return false;
        }
        List<String> command = new ArrayList<>();
        command.add(aria2.getAbsolutePath());
        command.add("--no-conf=true");
        command.add("--continue=true");
        command.add("--auto-file-renaming=false");
        command.add("--allow-overwrite=true");
        command.add("--file-allocation=none");
        command.add("--max-connection-per-server=16");
        command.add("--split=16");
        command.add("--min-split-size=1M");
        command.add("--summary-interval=1");
        command.add("--console-log-level=notice");
        command.add("--download-result=hide");
        command.add("--retry-wait=2");
        command.add("--max-tries=3");
        if (sha1 != null && !sha1.isBlank()) {
            command.add("--checksum=sha-1=" + sha1);
        }
        command.add("-d");
        command.add(parent == null ? "." : parent.getAbsolutePath());
        command.add("-o");
        command.add(target.getName());
        command.add(url);

        Process process = null;
        try {
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.redirectErrorStream(true);
            process = builder.start();
            final Process running = process;
            try (BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(running.getInputStream(), Charset.defaultCharset()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (cancelled != null && cancelled.getAsBoolean()) {
                        running.destroyForcibly();
                        return false;
                    }
                    Matcher m = PROGRESS.matcher(line);
                    if (m.find()) {
                        Units done = parse(m.group(1), m.group(2));
                        Units total = parse(m.group(3), m.group(4));
                        Units speed = parse(m.group(6), m.group(7));
                        if (onProgress != null) {
                            onProgress.update(done.bytes(), total.bytes(), speed.bytes());
                        }
                    }
                }
            }
            int code = running.waitFor();
            if (code != 0) {
                return false;
            }
            if (size > 0 && target.length() != size) {
                return false;
            }
            return target.isFile() && target.length() > 0;
        } catch (Exception e) {
            return false;
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    /** 进度回调。 */
    public interface Progress {
        void update(long downloaded, long total, long speedBps);
    }
}
