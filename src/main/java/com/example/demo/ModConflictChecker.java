package com.example.demo;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** 轻量模组 ID 冲突检测：重复 modId / 重复文件名。 */
public final class ModConflictChecker {

    private ModConflictChecker() {
    }

    public static List<String> check(File modsDir) {
        List<String> issues = new ArrayList<>();
        if (modsDir == null || !modsDir.isDirectory()) return issues;
        File[] files = modsDir.listFiles((d, n) -> n.toLowerCase(Locale.ROOT).endsWith(".jar"));
        if (files == null) return issues;
        Map<String, String> ids = new HashMap<>();
        for (File file : files) {
            String id = readModId(file);
            if (id == null || id.isBlank()) continue;
            String previous = ids.putIfAbsent(id.toLowerCase(Locale.ROOT), file.getName());
            if (previous != null) {
                issues.add("重复 modId " + id + "：" + previous + " / " + file.getName());
            }
        }
        return issues;
    }

    private static String readModId(File jar) {
        try (ZipFile zip = new ZipFile(jar)) {
            ZipEntry fabric = zip.getEntry("fabric.mod.json");
            if (fabric != null) {
                try (InputStream in = zip.getInputStream(fabric)) {
                    JsonObject json = JsonParser.parseString(new String(
                            in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
                    if (json.has("id")) return json.get("id").getAsString();
                }
            }
            for (String name : new String[]{"META-INF/neoforge.mods.toml", "META-INF/mods.toml"}) {
                ZipEntry toml = zip.getEntry(name);
                if (toml == null) continue;
                try (InputStream in = zip.getInputStream(toml)) {
                    String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                    java.util.regex.Matcher m = java.util.regex.Pattern
                            .compile("(?m)^\\s*modId\\s*=\\s*[\"']([^\"']+)[\"']")
                            .matcher(text);
                    if (m.find()) return m.group(1);
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
