package com.example.demo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

/**
 * 单个实例的启动覆盖项（内存 / JVM 参数）。
 * 存在 game/instance-settings.json，启动该实例时覆盖全局设置。
 */
public final class InstanceOverrides {

    public record Override(Integer memoryMb, String jvmArgs, String javaPath) {
        public boolean isEmpty() {
            return memoryMb == null
                    && (jvmArgs == null || jvmArgs.isBlank())
                    && (javaPath == null || javaPath.isBlank());
        }
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private InstanceOverrides() {
    }

    private static File file(File gameDir) {
        return new File(gameDir, "instance-settings.json");
    }

    public static Override get(File gameDir, String instanceId) {
        if (instanceId == null) return new Override(null, null, null);
        JsonObject all = loadAll(gameDir);
        if (!all.has(instanceId) || !all.get(instanceId).isJsonObject()) {
            return new Override(null, null, null);
        }
        JsonObject entry = all.getAsJsonObject(instanceId);
        Integer memory = entry.has("memoryMb") && !entry.get("memoryMb").isJsonNull()
                ? entry.get("memoryMb").getAsInt() : null;
        String jvm = entry.has("jvmArgs") ? entry.get("jvmArgs").getAsString() : "";
        String javaPath = entry.has("javaPath") ? entry.get("javaPath").getAsString() : "";
        return new Override(memory, jvm, javaPath);
    }

    public static void save(File gameDir, String instanceId, Integer memoryMb, String jvmArgs, String javaPath) {
        if (instanceId == null) return;
        JsonObject all = loadAll(gameDir);
        if ((memoryMb == null || memoryMb <= 0)
                && (jvmArgs == null || jvmArgs.isBlank())
                && (javaPath == null || javaPath.isBlank())) {
            all.remove(instanceId);
        } else {
            JsonObject entry = new JsonObject();
            if (memoryMb != null && memoryMb > 0) {
                entry.addProperty("memoryMb", memoryMb);
            }
            if (jvmArgs != null && !jvmArgs.isBlank()) {
                entry.addProperty("jvmArgs", jvmArgs.trim());
            }
            if (javaPath != null && !javaPath.isBlank()) {
                entry.addProperty("javaPath", javaPath.trim());
            }
            all.add(instanceId, entry);
        }
        try {
            Files.writeString(file(gameDir).toPath(), GSON.toJson(all), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            // 写失败不影响启动，下次再存
        }
    }

    private static JsonObject loadAll(File gameDir) {
        File file = file(gameDir);
        if (!file.isFile()) return new JsonObject();
        try {
            String text = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            JsonObject all = JsonParser.parseString(text).getAsJsonObject();
            return all == null ? new JsonObject() : all;
        } catch (Exception e) {
            return new JsonObject();
        }
    }

    /** 调试用：当前覆盖数量。 */
    public static int size(File gameDir) {
        return loadAll(gameDir).size();
    }
}
