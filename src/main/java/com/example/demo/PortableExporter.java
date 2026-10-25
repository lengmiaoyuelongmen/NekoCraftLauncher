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
        Files.createDirectories(targetDir.toPath());
        File base = AppPaths.baseDir();

        File exe = new File(base, "NekoCraftLauncher.exe");
        boolean copiedLauncher = false;
        if (exe.isFile()) {
            Files.copy(exe.toPath(), new File(targetDir, exe.getName()).toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
            copyTree(new File(base, "app"), new File(targetDir, "app"), null);
            copyTree(new File(base, "runtime"), new File(targetDir, "runtime"), null);
            copiedLauncher = true;
        }
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

        File minecraft = new File(targetDir, ".minecraft");
        File source = InstanceDirs.root(gameDir, instance.id, true);
        File targetVersion = new File(new File(minecraft, "versions"), instance.id);
        copyTree(source, targetVersion, includeSaves ? null : "saves");

        Files.writeString(new File(targetDir, "导出说明.txt").toPath(),
                "Neko Craft Launcher 便携整合包\n"
                        + "实例: " + instance.id + "\n"
                        + "启动器: " + (copiedLauncher ? "已附带完整启动器" : "启动器下载失败，仅包含游戏文件") + "\n"
                        + "游戏目录: .minecraft\n\n"
                        + "双击 NekoCraftLauncher.exe 启动。\n"
                        + "如果使用 jar 版，请双击 启动.bat，需要本机安装 Java 21。\n",
                java.nio.charset.StandardCharsets.UTF_8);
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
            return location.isFile() && location.getName().endsWith(".jar") ? location : null;
        } catch (Exception e) {
            return null;
        }
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
