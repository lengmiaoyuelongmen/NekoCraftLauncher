package com.example.demo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 每个实例的累计游戏时长（game/playtime.json），单位秒。 */
public final class Playtime {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, Map<String, Long>> CACHE = new ConcurrentHashMap<>();

    private Playtime() {
    }

    private static File file(File gameDir) {
        return new File(gameDir, "playtime.json");
    }

    public static synchronized long seconds(File gameDir, String instanceId) {
        if (gameDir == null || instanceId == null) return 0;
        return load(gameDir).getOrDefault(instanceId, 0L);
    }

    public static synchronized void add(File gameDir, String instanceId, long seconds) {
        if (gameDir == null || instanceId == null || seconds <= 0) return;
        Map<String, Long> data = load(gameDir);
        data.merge(instanceId, seconds, Long::sum);
        try (Writer writer = Files.newBufferedWriter(file(gameDir).toPath(), StandardCharsets.UTF_8)) {
            GSON.toJson(data, writer);
        } catch (Exception ignored) {
            // 写失败不影响游戏
        }
    }

    public static String format(long seconds) {
        if (seconds <= 0) return "0 分钟";
        long minutes = seconds / 60;
        if (minutes < 60) return Math.max(1, minutes) + " 分钟";
        long hours = minutes / 60;
        long rest = minutes % 60;
        return rest == 0 ? hours + " 小时" : hours + " 小时 " + rest + " 分";
    }

    private static Map<String, Long> load(File gameDir) {
        String key = gameDir.getAbsolutePath();
        return CACHE.computeIfAbsent(key, k -> read(gameDir));
    }

    private static Map<String, Long> read(File gameDir) {
        Map<String, Long> data = new java.util.LinkedHashMap<>();
        File file = file(gameDir);
        if (!file.isFile()) return data;
        try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            for (String id : json.keySet()) {
                try {
                    data.put(id, json.get(id).getAsLong());
                } catch (Exception ignored) {
                    // 单个条目坏了就跳过
                }
            }
        } catch (Exception ignored) {
            // 文件损坏当作空
        }
        return data;
    }
}
