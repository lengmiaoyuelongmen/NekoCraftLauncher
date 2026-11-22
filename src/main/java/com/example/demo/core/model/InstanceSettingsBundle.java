package com.example.demo.core.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * 实例启动设置的打包与还原。
 *
 * <p>只搬 {@code instance.json}（内存 / JVM / Java / 游戏参数 / 隔离），不搬模组和世界 ——
 * 那属于实例本身的导出功能。所以这个包很小，适合丢进网盘或发给朋友对齐配置。
 *
 * <p>包结构：
 * <pre>
 * manifest.json          {"exportedAt":"...","count":3}
 * instances/&lt;id&gt;/instance.json
 * </pre>
 */
public final class InstanceSettingsBundle {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private InstanceSettingsBundle() {
    }

    /** versions/ 下所有目录（不依赖版本 JSON 是否完整，配置能导就导）。 */
    private static List<File> versionDirs(File gameDir) {
        List<File> dirs = new ArrayList<>();
        File[] files = new File(gameDir, "versions").listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    dirs.add(file);
                }
            }
        }
        return dirs;
    }
    /** 导出的结果：写了几份、跳过几个没覆盖的实例。 */
    public record Result(int exported, int skipped, File file) {
    }

    /**
     * 导出所有「有实例级覆盖」的实例设置。
     *
     * @param zip 目标 zip（父目录不存在会自动创建）
     */
    public static Result exportAll(File gameDir, File zip) throws IOException {
        List<File> versionDirs = versionDirs(gameDir);
        List<String> exportedIds = new ArrayList<>();
        File parent = zip.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("无法创建目录：" + parent);
        }
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip))) {
            for (File dir : versionDirs) {
                InstanceSettings.Settings settings = InstanceSettings.get(gameDir, dir.getName());
                if (settings.isEmpty()) {
                    continue;
                }
                File source = new File(dir, "instance.json");
                if (!source.isFile()) {
                    continue;
                }
                String entryName = "instances/" + dir.getName() + "/instance.json";
                out.putNextEntry(new ZipEntry(entryName));
                out.write(java.nio.file.Files.readAllBytes(source.toPath()));
                out.closeEntry();
                exportedIds.add(dir.getName());
            }
            String manifest = "{\n"
                    + "  \"exportedAt\": \"" + LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\",\n"
                    + "  \"count\": " + exportedIds.size() + ",\n"
                    + "  \"instances\": " + GSON.toJson(exportedIds) + "\n"
                    + "}\n";
            out.putNextEntry(new ZipEntry("manifest.json"));
            out.write(manifest.getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }
        return new Result(exportedIds.size(), versionDirs.size() - exportedIds.size(), zip);
    }

    /** 从包里还原实例设置；同名实例会被覆盖，找不到对应实例的条目计入 skipped。 */
    public static Result importAll(File gameDir, File zip) throws IOException {
        if (zip == null || !zip.isFile()) {
            throw new IOException("找不到设置包");
        }
        List<String> known = new ArrayList<>();
        for (File dir : versionDirs(gameDir)) {
            known.add(dir.getName());
        }
        int applied = 0;
        int skipped = 0;
        try (ZipFile archive = new ZipFile(zip)) {
            Enumeration<? extends ZipEntry> entries = archive.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (!name.startsWith("instances/") || !name.endsWith("/instance.json")) {
                    continue;
                }
                String id = name.substring("instances/".length(), name.length() - "/instance.json".length());
                if (!known.contains(id)) {
                    skipped++;
                    continue;
                }
                File target = InstanceSettings.file(gameDir, id);
                File parent = target.getParentFile();
                if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
                    skipped++;
                    continue;
                }
                try (InputStream in = archive.getInputStream(entry)) {
                    java.nio.file.Files.copy(in, target.toPath(),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
                applied++;
            }
        }
        return new Result(applied, skipped, zip);
    }
}
