package com.example.demo;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 一键反馈包：反馈文本 + 系统信息 + 配置（脱敏）+ 最近日志，打包成 zip。
 */
public final class SupportBundle {

    private SupportBundle() {
    }

    public static File create(File gameDir, LauncherConfig config, String feedback, String systemInfo)
            throws Exception {
        File dir = new File(gameDir, "logs");
        if (!dir.isDirectory() && !dir.mkdirs()) {
            throw new Exception("无法创建日志目录: " + dir.getAbsolutePath());
        }
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        File zip = new File(dir, "feedback-" + stamp + ".zip");
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip))) {
            putText(out, "feedback.txt", feedback == null ? "" : feedback);
            putText(out, "system-info.txt", systemInfo == null ? "" : systemInfo);
            putText(out, "config-redacted.json", redactedConfig(gameDir));

            File launcherLog = new File(dir, "launcher.log");
            if (launcherLog.isFile()) putFile(out, "logs/launcher.log", launcherLog);
            File rotated = new File(dir, "launcher.1.log");
            if (rotated.isFile()) putFile(out, "logs/launcher.1.log", rotated);
            File gameLog = new File(dir, "latest.log");
            if (gameLog.isFile()) putFile(out, "logs/latest.log", gameLog);
        }
        return zip;
    }

    private static String redactedConfig(File gameDir) {
        try {
            File configFile = gameDir.getParentFile() == null
                    ? new File("config.json") : new File(gameDir.getParentFile(), "config.json");
            if (!configFile.isFile()) configFile = new File("config.json");
            if (!configFile.isFile()) return "{}";
            JsonObject json = JsonParser.parseString(
                    Files.readString(configFile.toPath(), StandardCharsets.UTF_8)).getAsJsonObject();
            json.remove("javaPath");
            json.remove("updateUrl");
            return json.toString();
        } catch (Exception e) {
            return "{\"error\":\"" + e.getMessage() + "\"}";
        }
    }

    private static void putText(ZipOutputStream out, String name, String text) throws Exception {
        out.putNextEntry(new ZipEntry(name));
        out.write(text.getBytes(StandardCharsets.UTF_8));
        out.closeEntry();
    }

    private static void putFile(ZipOutputStream out, String name, File file) throws Exception {
        out.putNextEntry(new ZipEntry(name));
        try (FileInputStream in = new FileInputStream(file)) {
            in.transferTo(out);
        }
        out.closeEntry();
    }
}
