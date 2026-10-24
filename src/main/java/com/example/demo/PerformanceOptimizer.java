package com.example.demo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 可逆的 Windows 游戏优化（参考 BoosterX 的思路，只保留低风险项）：
 * 高性能电源计划、关闭 Game DVR 后台录制、开启游戏模式、关闭窗口透明效果。
 * 修改前会记录原值到 game/optimization-backup.json，工具页可一键恢复。
 */
public final class PerformanceOptimizer {

    public record Step(String name, boolean ok, String detail) {
    }

    public record Result(List<Step> steps) {
        public int applied() {
            return (int) steps.stream().filter(Step::ok).count();
        }

        public boolean anyFailed() {
            return steps.stream().anyMatch(step -> !step.ok());
        }
    }

    public static final class Entry {
        String key;
        String name;
        String type;
        String value;
        boolean existed;

        Entry() {
        }

        Entry(String key, String name, String type, String value, boolean existed) {
            this.key = key;
            this.name = name;
            this.type = type;
            this.value = value;
            this.existed = existed;
        }
    }

    public static final class Backup {
        String powerScheme;
        List<Entry> values = new ArrayList<>();
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type BACKUP_TYPE = new TypeToken<Backup>() {
    }.getType();

    private static final String GAME_DVR_KEY = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\GameDVR";
    private static final String GAME_CONFIG_KEY = "HKCU\\System\\GameConfigStore";
    private static final String GAME_BAR_KEY = "HKCU\\Software\\Microsoft\\GameBar";
    private static final String PERSONALIZE_KEY =
            "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize";

    private PerformanceOptimizer() {
    }

    public static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("windows");
    }

    public static File backupFile(File gameDir) {
        return new File(gameDir, "optimization-backup.json");
    }

    public static boolean hasBackup(File gameDir) {
        File file = backupFile(gameDir);
        return file.isFile() && file.length() > 10;
    }

    public static Result apply(File gameDir, boolean powerPlan, boolean gameDvr,
                               boolean gameMode, boolean transparency) {
        List<Step> steps = new ArrayList<>();
        if (!isWindows()) {
            steps.add(new Step("系统检查", false, "仅支持 Windows"));
            return new Result(steps);
        }
        Backup backup = load(gameDir);
        if (backup == null) backup = new Backup();

        if (powerPlan && backup.powerScheme == null) {
            backup.powerScheme = activeScheme();
        }
        if (gameDvr) {
            remember(backup, GAME_DVR_KEY, "AppCaptureEnabled");
            remember(backup, GAME_CONFIG_KEY, "GameDVR_Enabled");
        }
        if (gameMode) {
            remember(backup, GAME_BAR_KEY, "AllowAutoGameMode");
            remember(backup, GAME_BAR_KEY, "AutoGameModeEnabled");
        }
        if (transparency) {
            remember(backup, PERSONALIZE_KEY, "EnableTransparency");
        }
        save(gameDir, backup);

        if (powerPlan) {
            steps.add(setPowerScheme("SCHEME_MIN", "高性能电源计划"));
        }
        if (gameDvr) {
            steps.add(setDword(GAME_DVR_KEY, "AppCaptureEnabled", 0, "关闭 Game DVR 录制"));
            steps.add(setDword(GAME_CONFIG_KEY, "GameDVR_Enabled", 0, "关闭后台游戏录制"));
        }
        if (gameMode) {
            steps.add(setDword(GAME_BAR_KEY, "AllowAutoGameMode", 1, "允许游戏模式"));
            steps.add(setDword(GAME_BAR_KEY, "AutoGameModeEnabled", 1, "开启游戏模式"));
        }
        if (transparency) {
            steps.add(setDword(PERSONALIZE_KEY, "EnableTransparency", 0, "关闭窗口透明效果"));
        }
        return new Result(steps);
    }

    public static Result restore(File gameDir) {
        List<Step> steps = new ArrayList<>();
        if (!isWindows()) {
            steps.add(new Step("系统检查", false, "仅支持 Windows"));
            return new Result(steps);
        }
        Backup backup = load(gameDir);
        if (backup == null) {
            steps.add(new Step("恢复优化", false, "没有找到优化备份"));
            return new Result(steps);
        }
        if (backup.powerScheme != null && !backup.powerScheme.isBlank()) {
            steps.add(setPowerScheme(backup.powerScheme, "恢复原电源计划"));
        }
        for (Entry entry : backup.values) {
            if (entry.existed) {
                steps.add(restoreValue(entry));
            } else {
                steps.add(deleteValue(entry));
            }
        }
        return new Result(steps);
    }

    private static void remember(Backup backup, String key, String name) {
        for (Entry entry : backup.values) {
            if (entry.key.equalsIgnoreCase(key) && entry.name.equalsIgnoreCase(name)) return;
        }
        RegValue current = query(key, name);
        backup.values.add(new Entry(key, name, current.type, current.value, current.existed));
    }

    private static String activeScheme() {
        ProcessResult result = run(List.of("powercfg", "/getactivescheme"));
        if (!result.ok) return null;
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})")
                .matcher(result.output);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static Step setPowerScheme(String scheme, String label) {
        ProcessResult result = run(List.of("powercfg", "/setactive", scheme));
        return new Step(label, result.ok, result.ok ? "已切换" : shorten(result.output));
    }

    private static Step setDword(String key, String name, int value, String label) {
        ProcessResult result = run(List.of("reg", "add", key, "/v", name,
                "/t", "REG_DWORD", "/d", String.valueOf(value), "/f"));
        return new Step(label, result.ok, result.ok ? "已写入" : shorten(result.output));
    }

    private static Step restoreValue(Entry entry) {
        String type = entry.type == null || entry.type.isBlank() ? "REG_DWORD" : entry.type;
        String value = entry.value == null ? "" : entry.value;
        if ("REG_DWORD".equalsIgnoreCase(type) && value.startsWith("0x")) {
            try {
                value = String.valueOf(Long.parseLong(value.substring(2), 16));
            } catch (NumberFormatException ignored) {
                // 保留原字符串
            }
        }
        ProcessResult result = run(List.of("reg", "add", entry.key, "/v", entry.name,
                "/t", type, "/d", value, "/f"));
        return new Step("恢复 " + entry.name, result.ok, result.ok ? "已恢复" : shorten(result.output));
    }

    private static Step deleteValue(Entry entry) {
        ProcessResult result = run(List.of("reg", "delete", entry.key, "/v", entry.name, "/f"));
        return new Step("移除 " + entry.name, result.ok, result.ok ? "已移除" : shorten(result.output));
    }

    private record RegValue(boolean existed, String type, String value) {
    }

    private static RegValue query(String key, String name) {
        ProcessResult result = run(List.of("reg", "query", key, "/v", name));
        if (!result.ok) return new RegValue(false, "", "");
        for (String line : result.output.split("\\R")) {
            String trimmed = line.trim();
            if (!trimmed.toLowerCase(Locale.ROOT).startsWith(name.toLowerCase(Locale.ROOT))) continue;
            String[] tokens = trimmed.split("\\s+");
            if (tokens.length >= 3) {
                return new RegValue(true, tokens[tokens.length - 2], tokens[tokens.length - 1]);
            }
        }
        return new RegValue(false, "", "");
    }

    private record ProcessResult(boolean ok, String output) {
    }

    private static ProcessResult run(List<String> command) {
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int code = process.waitFor();
            return new ProcessResult(code == 0, output);
        } catch (Exception e) {
            return new ProcessResult(false, e.getMessage());
        }
    }

    private static String shorten(String text) {
        if (text == null) return "";
        String oneLine = text.strip().replaceAll("\\s+", " ");
        return oneLine.length() > 120 ? oneLine.substring(0, 120) + "…" : oneLine;
    }

    private static Backup load(File gameDir) {
        File file = backupFile(gameDir);
        if (!file.isFile()) return null;
        try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            Backup backup = GSON.fromJson(reader, BACKUP_TYPE);
            if (backup != null && backup.values == null) backup.values = new ArrayList<>();
            return backup;
        } catch (Exception e) {
            return null;
        }
    }

    private static void save(File gameDir, Backup backup) {
        try (Writer writer = Files.newBufferedWriter(backupFile(gameDir).toPath(), StandardCharsets.UTF_8)) {
            GSON.toJson(backup, writer);
        } catch (Exception ignored) {
            // 备份写失败时仍尝试应用，但恢复能力受限
        }
    }
}
