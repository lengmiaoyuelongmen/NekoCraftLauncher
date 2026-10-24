package com.example.demo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/** 本地启动历史：记录每次启动的准备耗时、会话时长、退出码与异常标记。 */
public final class LaunchHistory {

    public record Entry(long time, String instanceId, long prepMs, long sessionSeconds,
                        int exitCode, boolean crash) {
    }

    public record Summary(int total, int crashes, long avgPrepMs) {
        public int successRate() {
            return total == 0 ? 100 : Math.round((total - crashes) * 100f / total);
        }
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int LIMIT = 60;

    private LaunchHistory() {
    }

    private static File file(File gameDir) {
        return new File(gameDir, "launch-history.json");
    }

    public static synchronized void record(File gameDir, Entry entry) {
        List<Entry> entries = load(gameDir);
        entries.add(0, entry);
        if (entries.size() > LIMIT) {
            entries = new ArrayList<>(entries.subList(0, LIMIT));
        }
        save(gameDir, entries);
    }

    public static synchronized List<Entry> recent(File gameDir) {
        return load(gameDir);
    }

    public static synchronized Summary summary(File gameDir) {
        List<Entry> entries = load(gameDir);
        int crashes = 0;
        long prepTotal = 0;
        int prepCount = 0;
        for (Entry entry : entries) {
            if (entry.crash()) crashes++;
            if (entry.prepMs() > 0) {
                prepTotal += entry.prepMs();
                prepCount++;
            }
        }
        return new Summary(entries.size(), crashes, prepCount == 0 ? 0 : prepTotal / prepCount);
    }

    public static synchronized void clear(File gameDir) {
        save(gameDir, new ArrayList<>());
    }

    private static List<Entry> load(File gameDir) {
        File file = file(gameDir);
        if (!file.isFile()) return new ArrayList<>();
        try {
            String text = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            List<Entry> entries = GSON.fromJson(text, new TypeToken<List<Entry>>() { }.getType());
            return entries == null ? new ArrayList<>() : entries;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private static void save(File gameDir, List<Entry> entries) {
        try {
            Files.writeString(file(gameDir).toPath(), GSON.toJson(entries), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            // 历史记录写失败不影响启动
        }
    }
}
