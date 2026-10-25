package com.example.demo;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;

/** 导出「启动器 + 游戏文件」到同一个便携文件夹。 */
public final class PortableExporter {

    private PortableExporter() {
    }

    public static void export(File gameDir, Instance instance, File targetDir, boolean includeSaves) throws IOException {
        export(gameDir, instance, targetDir, includeSaves, null);
    }

    public static void export(File gameDir, Instance instance, File targetDir, boolean includeSaves,
                              java.util.function.DoubleConsumer progress) throws IOException {
        report(progress, 0.02);
        Files.createDirectories(targetDir.toPath());
        File base = AppPaths.baseDir();

        boolean copiedLauncher = copyLocalLauncher(targetDir, base);
        if (!copiedLauncher) {
            copiedLauncher = downloadLauncher(targetDir);
        }
        if (!copiedLauncher) {
            File jar = locateJar();
            if (jar != null) {
                Files.copy(jar.toPath(), new File(targetDir, "NekoCraftLauncher.jar").toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
                copiedLauncher = true;
            }
        }
        // 便携模式：不读取本机账户注册表，避免把导出者的登录状态带进去。
        Files.writeString(new File(targetDir, ".portable").toPath(), "portable", java.nio.charset.StandardCharsets.UTF_8);
        report(progress, 0.38);
        File launcherExe = new File(targetDir, "NekoCraftLauncher.exe");
        if (launcherExe.isFile()) {
            Files.writeString(new File(targetDir, "启动 Neko Craft Launcher.bat").toPath(),
                    "@echo off\r\ncd /d \"%~dp0\"\r\nstart \"\" \"%~dp0NekoCraftLauncher.exe\"\r\n",
                    java.nio.charset.StandardCharsets.UTF_8);
            createShortcut(targetDir, launcherExe);
        } else if (new File(targetDir, "NekoCraftLauncher.jar").isFile()) {
            Files.writeString(new File(targetDir, "启动.bat").toPath(),
                    "@echo off\r\ncd /d \"%~dp0\"\r\nstart javaw -jar \"%~dp0NekoCraftLauncher.jar\"\r\n",
                    java.nio.charset.StandardCharsets.UTF_8);
        }

        report(progress, 0.42);
        File minecraft = new File(targetDir, ".minecraft");
        File targetVersion = new File(new File(minecraft, "versions"), instance.id);
        File source = InstanceDirs.root(gameDir, instance.id, true);
        File versionsRoot = new File(gameDir, "versions");
        if (source.getAbsoluteFile().toPath().startsWith(versionsRoot.getAbsoluteFile().toPath())) {
            copyTree(source, targetVersion, includeSaves ? null : "saves");
        } else {
            // 非隔离实例：只复制当前版本目录，绝不把其他版本 / 其他游戏存档一起带走。
            copyTree(new File(versionsRoot, instance.id), targetVersion, null);
        }

        report(progress, 0.92);
        Files.writeString(new File(targetDir, "导出说明.txt").toPath(),
                "Neko Craft Launcher 便携整合包\n"
                        + "实例: " + instance.id + "\n"
                        + "启动器: " + (copiedLauncher ? "已附带完整启动器" : "启动器下载失败，仅包含游戏文件") + "\n"
                        + "游戏目录: .minecraft\n\n"
                        + "双击 NekoCraftLauncher.exe 启动。\n"
                        + "如果使用 jar 版，请双击 启动.bat，需要本机安装 Java 21。\n",
                java.nio.charset.StandardCharsets.UTF_8);
    }

    /** 优先复制本机已安装的完整启动器。 */
    private static void report(java.util.function.DoubleConsumer progress, double value) {
        if (progress != null) progress.accept(value);
    }

    private static boolean copyLocalLauncher(File targetDir, File base) throws IOException {
        java.util.List<File> candidates = new java.util.ArrayList<>();
        candidates.add(new File(base, "NekoCraftLauncher.exe"));
        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData != null) candidates.add(new File(localAppData, "NekoCraftLauncher/NekoCraftLauncher.exe"));
        String programFiles = System.getenv("ProgramFiles");
        if (programFiles != null) candidates.add(new File(programFiles, "NekoCraftLauncher/NekoCraftLauncher.exe"));
        candidates.add(new File(System.getProperty("user.dir"), "release/exe/NekoCraftLauncher/NekoCraftLauncher.exe"));
        candidates.add(new File(System.getProperty("user.dir"), "release/NekoCraftLauncher/NekoCraftLauncher.exe"));
        for (File exe : candidates) {
            if (exe == null || !exe.isFile()) continue;
            File root = exe.getParentFile();
            Files.copy(exe.toPath(), new File(targetDir, exe.getName()).toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
            copyTree(new File(root, "app"), new File(targetDir, "app"), null);
            copyTree(new File(root, "runtime"), new File(targetDir, "runtime"), null);
            return true;
        }
        return false;
    }

    /** 没有本地启动器时，从 GitHub 下载最新便携版并解压到导出目录。 */
    private static boolean downloadLauncher(File targetDir) {
        File zip = new File(targetDir, "NekoCraftLauncher-exe.zip");
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(15))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(
                            "https://github.com/lengmiaoyuelongmen/NekoCraftLauncher/releases/latest/download/NekoCraftLauncher-exe.zip"))
                    .timeout(Duration.ofMinutes(5))
                    .header("User-Agent", "NekoCraftLauncher/" + AppVersion.CURRENT)
                    .GET().build();
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() / 100 != 2) return false;
            try (InputStream in = response.body()) {
                Files.copy(in, zip.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            try (ZipInputStream zin = new ZipInputStream(Files.newInputStream(zip.toPath()))) {
                ZipEntry entry;
                while ((entry = zin.getNextEntry()) != null) {
                    File out = new File(targetDir, entry.getName());
                    if (!out.getCanonicalPath().startsWith(targetDir.getCanonicalPath())) continue;
                    if (entry.isDirectory()) out.mkdirs();
                    else {
                        out.getParentFile().mkdirs();
                        Files.copy(zin, out.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
            Files.deleteIfExists(zip.toPath());
            return new File(targetDir, "NekoCraftLauncher.exe").isFile();
        } catch (Exception ignored) {
            return false;
        } finally {
            try { Files.deleteIfExists(zip.toPath()); } catch (Exception ignored) { }
        }
    }

    /** 在导出根目录生成显眼的桌面快捷方式。 */
    private static void createShortcut(File targetDir, File exe) {
        try {
            File lnk = new File(targetDir, "启动 Neko Craft Launcher.lnk");
            String ps = "$ws=New-Object -ComObject WScript.Shell;"
                    + "$s=$ws.CreateShortcut('" + lnk.getAbsolutePath().replace("'", "''") + "');"
                    + "$s.TargetPath='" + exe.getAbsolutePath().replace("'", "''") + "';"
                    + "$s.WorkingDirectory='" + targetDir.getAbsolutePath().replace("'", "''") + "';"
                    + "$s.IconLocation='" + exe.getAbsolutePath().replace("'", "''") + ",0';"
                    + "$s.Save()";
            new ProcessBuilder("powershell", "-NoProfile", "-ExecutionPolicy", "Bypass",
                    "-Command", ps).start().waitFor();
        } catch (Exception ignored) {
        }
    }

    private static File locateJar() {
        try {
            File location = new File(PortableExporter.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
            if (location.isFile() && location.getName().endsWith(".jar")) return location;
        } catch (Exception ignored) {
        }
        for (String candidate : new String[]{"target/mc-launcher.jar", "release/mc-launcher.jar"}) {
            File jar = new File(System.getProperty("user.dir"), candidate);
            if (jar.isFile()) return jar;
        }
        return null;
    }

    private static void copyTree(File source, File target, String skipName) throws IOException {
        if (source == null || !source.exists()) return;
        if (source.isFile()) {
            Files.createDirectories(target.getParentFile().toPath());
            Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return;
        }
        Path root = source.toPath();
        try (var stream = Files.walk(root)) {
            for (Path path : stream.sorted(Comparator.naturalOrder()).toList()) {
                Path relative = root.relativize(path);
                if (skipName != null && relative.getNameCount() > 0
                        && relative.getName(0).toString().equalsIgnoreCase(skipName)) continue;
                Path destination = target.toPath().resolve(relative.toString());
                if (Files.isDirectory(path)) {
                    Files.createDirectories(destination);
                } else {
                    Files.createDirectories(destination.getParent());
                    Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }
}
