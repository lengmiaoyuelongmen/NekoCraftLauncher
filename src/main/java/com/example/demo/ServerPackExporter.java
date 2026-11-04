package com.example.demo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.DoubleConsumer;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** 把客户端实例导出为服务端文件包（服务端核心需按加载器另行安装）。 */
public final class ServerPackExporter {

    private static final String[] CLIENT_MODS = {
            "sodium", "iris", "embeddium", "oculus", "rubidium", "xaero", "journeymap",
            "minimap", "shader", "optifine", "fancymenu", "entityculling", "indium",
            "controllable", "controlify", "betterf3", "modmenu", "zoomify", "presencefootsteps"
    };

    private ServerPackExporter() {
    }

    public record Result(int files, long bytes, int skippedClientMods, File output) {
    }

    public static Result export(File instanceRoot, File output, boolean excludeClientMods,
                                Consumer<String> log, DoubleConsumer progress) throws Exception {
        List<File> files = new ArrayList<>();
        collect(new File(instanceRoot, "mods"), files, true);
        for (String dir : new String[]{"config", "defaultconfigs", "scripts", "kubejs", "datapacks"}) {
            collect(new File(instanceRoot, dir), files, true);
        }
        File properties = new File(instanceRoot, "server.properties");
        if (properties.isFile()) files.add(properties);

        int skipped = 0;
        long total = 0;
        for (File file : files) total += Math.max(1, file.length());
        long written = 0;
        int count = 0;
        try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(output))) {
            for (File file : files) {
                String relative = instanceRoot.toPath().relativize(file.toPath()).toString().replace('\\', '/');
                String lower = file.getName().toLowerCase(Locale.ROOT);
                if ("mods".equals(relative.split("/")[0]) && excludeClientMods && isClientMod(lower)) {
                    skipped++;
                    if (log != null) log.accept("跳过客户端模组: " + file.getName());
                    continue;
                }
                zip.putNextEntry(new ZipEntry(relative));
                try (FileInputStream in = new FileInputStream(file)) {
                    byte[] buffer = new byte[32768];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        zip.write(buffer, 0, read);
                        written += read;
                        if (progress != null) progress.accept(Math.min(0.98, (double) written / Math.max(1, total)));
                    }
                }
                zip.closeEntry();
                count++;
            }
            byte[] readme = readme().getBytes(StandardCharsets.UTF_8);
            zip.putNextEntry(new ZipEntry("README-server.txt"));
            zip.write(readme);
            zip.closeEntry();
            byte[] start = startScript().getBytes(StandardCharsets.UTF_8);
            zip.putNextEntry(new ZipEntry("start-server.bat"));
            zip.write(start);
            zip.closeEntry();
            if (progress != null) progress.accept(1.0);
        }
        return new Result(count, written, skipped, output);
    }

    private static boolean isClientMod(String name) {
        for (String key : CLIENT_MODS) if (name.contains(key)) return true;
        return false;
    }

    private static void collect(File dir, List<File> out, boolean recursive) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isFile()) {
                if (!file.getName().startsWith(".")) out.add(file);
            } else if (recursive) {
                collect(file, out, true);
            }
        }
    }

    private static String readme() {
        return """
                Neko Craft Launcher 服务端包
                ============================
                1. 按实例使用的加载器安装服务端核心（Forge / NeoForge / Fabric / Quilt）。
                2. 将本压缩包解压到服务端目录，保留 mods、config、scripts 等。
                3. 首次启动会生成 eula.txt，阅读并同意后把 eula=false 改为 true。
                4. 编辑 start-server.bat 里的 Xmx 和 server.jar 路径。
                5. 客户端专属模组可能已被过滤，缺失模组请按日志补齐。

                注意：本包不包含 Minecraft 服务端核心 jar，需要单独安装。
                """;
    }

    private static String startScript() {
        return """
                @echo off
                chcp 65001 >nul
                rem 请根据加载器把 server.jar 替换为实际服务端核心文件名
                java -Xms2G -Xmx4G -jar server.jar nogui
                pause
                """;
    }
}
