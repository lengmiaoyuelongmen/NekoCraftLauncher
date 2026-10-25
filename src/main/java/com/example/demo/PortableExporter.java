package com.example.demo;

import java.io.File;
import java.io.IOException;
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
        } else {
            File jar = locateJar();
            if (jar != null) {
                Files.copy(jar.toPath(), new File(targetDir, "NekoCraftLauncher.jar").toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
                Files.writeString(new File(targetDir, "启动.bat").toPath(),
                        "@echo off\r\ncd /d \"%~dp0\"\r\nstart javaw -jar \"%~dp0NekoCraftLauncher.jar\"\r\n",
                        java.nio.charset.StandardCharsets.UTF_8);
                copiedLauncher = true;
            }
        }

        File minecraft = new File(targetDir, ".minecraft");
        File source = InstanceDirs.root(gameDir, instance.id, true);
        File targetVersion = new File(new File(minecraft, "versions"), instance.id);
        copyTree(source, targetVersion, includeSaves ? null : "saves");

        Files.writeString(new File(targetDir, "导出说明.txt").toPath(),
                "Neko Craft Launcher 便携整合包\n"
                        + "实例: " + instance.id + "\n"
                        + "启动器: " + (copiedLauncher ? "已附带" : "未找到启动器文件，请手动放入") + "\n"
                        + "游戏目录: .minecraft\n\n"
                        + "双击 NekoCraftLauncher.exe 启动。\n"
                        + "如果使用 jar 版，请双击 启动.bat，需要本机安装 Java 21。\n",
                java.nio.charset.StandardCharsets.UTF_8);
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
