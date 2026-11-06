package com.example.demo.core.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * 单个实例的设置：内存 / JVM 参数 / Java 路径 / 是否隔离。
 *
 * <p>存在 <b>实例自己的目录里</b>（{@code versions/&lt;id&gt;/instance.json}），而不是全局大表 ——
 * 这样复制、导出、删除实例时设置跟着走（Prism / HMCL 的做法）。
 *
 * <p>兼容旧版：老版本把这三项存在 {@code game/instance-settings.json}，第一次读到时会自动
 * 迁移到新位置并把旧条目删掉。
 */
public final class InstanceSettings {

    public static final Settings EMPTY = new Settings(null, null, null, null, null);

    /**
     * @param isolation {@code null} 表示跟随全局设置；{@code true/false} 表示该实例强制隔离/不隔离
     */
    public record Settings(Integer memoryMb, String jvmArgs, String javaPath, Boolean isolation,
                           String gameArgs) {

        public boolean isEmpty() {
            return memoryMb == null
                    && (jvmArgs == null || jvmArgs.isBlank())
                    && (javaPath == null || javaPath.isBlank())
                    && isolation == null
                    && (gameArgs == null || gameArgs.isBlank());
        }

        public boolean hasIsolationOverride() {
            return isolation != null;
        }
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private InstanceSettings() {
    }

    /** 实例设置文件：versions/&lt;id&gt;/instance.json。 */
    public static File file(File gameDir, String instanceId) {
        return new File(InstanceDirs.versionDir(gameDir, instanceId), "instance.json");
    }

    public static Settings get(File gameDir, String instanceId) {
        if (gameDir == null || instanceId == null || instanceId.isBlank()) {
            return EMPTY;
        }
        File own = file(gameDir, instanceId);
        if (own.isFile()) {
            Settings parsed = parse(own);
            if (parsed != null) {
                return parsed;
            }
        }
        return migrateLegacy(gameDir, instanceId);
    }

    /** 保存启动相关三项；{@code isolation} 覆盖项保持不变。 */
    public static void save(File gameDir, String instanceId, Integer memoryMb, String jvmArgs,
                            String javaPath) {
        if (gameDir == null || instanceId == null || instanceId.isBlank()) {
            return;
        }
        save(gameDir, instanceId, memoryMb, jvmArgs, javaPath, null);
    }

    /** 带游戏参数的重载；{@code gameArgs} 为 null 表示保留原值。 */
    public static void save(File gameDir, String instanceId, Integer memoryMb, String jvmArgs,
                            String javaPath, String gameArgs) {
        if (gameDir == null || instanceId == null || instanceId.isBlank()) {
            return;
        }
        Settings current = get(gameDir, instanceId);
        write(gameDir, instanceId, new Settings(memoryMb, trimToNull(jvmArgs), trimToNull(javaPath),
                current.isolation(),
                gameArgs == null ? current.gameArgs() : trimToNull(gameArgs)));
    }

    /** 单独保存「是否隔离」；{@code null} 表示恢复跟随全局。 */
    public static void saveIsolation(File gameDir, String instanceId, Boolean isolation) {
        if (gameDir == null || instanceId == null || instanceId.isBlank()) {
            return;
        }
        Settings current = get(gameDir, instanceId);
        write(gameDir, instanceId, new Settings(current.memoryMb(), current.jvmArgs(),
                current.javaPath(), isolation, current.gameArgs()));
    }

    /** 该实例实际生效的隔离状态：实例有覆盖用实例的，否则用全局。 */
    public static boolean effectiveIsolation(File gameDir, String instanceId, boolean global) {
        Boolean override = get(gameDir, instanceId).isolation();
        return override == null ? global : override;
    }

    /** 删掉实例时一并清掉设置文件（实例目录被删时通常已经跟着没了，这里兜底）。 */
    public static void delete(File gameDir, String instanceId) {
        if (gameDir == null || instanceId == null) {
            return;
        }
        try {
            Files.deleteIfExists(file(gameDir, instanceId).toPath());
        } catch (Exception ignored) {
            // 删不掉也不影响：实例目录没了，设置文件也没人读
        }
    }

    private static void write(File gameDir, String instanceId, Settings settings) {
        File own = file(gameDir, instanceId);
        try {
            if (settings == null || settings.isEmpty()) {
                Files.deleteIfExists(own.toPath());
                clearLegacy(gameDir, instanceId);
                return;
            }
            File parent = own.getParentFile();
            if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
                return;
            }
            JsonObject json = new JsonObject();
            if (settings.memoryMb() != null && settings.memoryMb() > 0) {
                json.addProperty("memoryMb", settings.memoryMb());
            }
            if (settings.jvmArgs() != null) {
                json.addProperty("jvmArgs", settings.jvmArgs());
            }
            if (settings.javaPath() != null) {
                json.addProperty("javaPath", settings.javaPath());
            }
            if (settings.isolation() != null) {
                json.addProperty("isolation", settings.isolation());
            }
            if (settings.gameArgs() != null) {
                json.addProperty("gameArgs", settings.gameArgs());
            }
            Files.writeString(own.toPath(), GSON.toJson(json), StandardCharsets.UTF_8);
            clearLegacy(gameDir, instanceId);
        } catch (Exception ignored) {
            // 写失败不影响启动，下次保存再试
        }
    }

    private static Settings parse(File file) {
        try {
            String text = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            JsonObject json = JsonParser.parseString(text).getAsJsonObject();
            if (json == null) {
                return null;
            }
            Integer memory = json.has("memoryMb") && !json.get("memoryMb").isJsonNull()
                    ? json.get("memoryMb").getAsInt() : null;
            String jvm = json.has("jvmArgs") && !json.get("jvmArgs").isJsonNull()
                    ? trimToNull(json.get("jvmArgs").getAsString()) : null;
            String javaPath = json.has("javaPath") && !json.get("javaPath").isJsonNull()
                    ? trimToNull(json.get("javaPath").getAsString()) : null;
            Boolean isolation = json.has("isolation") && !json.get("isolation").isJsonNull()
                    ? json.get("isolation").getAsBoolean() : null;
            String gameArgs = json.has("gameArgs") && !json.get("gameArgs").isJsonNull()
                    ? trimToNull(json.get("gameArgs").getAsString()) : null;
            return new Settings(memory, jvm, javaPath, isolation, gameArgs);
        } catch (Exception broken) {
            return null;
        }
    }

    // ---------- 旧版 game/instance-settings.json 的读取与迁移 ----------

    private static File legacyFile(File gameDir) {
        return new File(gameDir, "instance-settings.json");
    }

    private static JsonObject loadLegacy(File gameDir) {
        File file = legacyFile(gameDir);
        if (!file.isFile()) {
            return new JsonObject();
        }
        try {
            JsonObject all = JsonParser.parseString(
                    Files.readString(file.toPath(), StandardCharsets.UTF_8)).getAsJsonObject();
            return all == null ? new JsonObject() : all;
        } catch (Exception e) {
            return new JsonObject();
        }
    }

    private static Settings migrateLegacy(File gameDir, String instanceId) {
        JsonObject all = loadLegacy(gameDir);
        if (!all.has(instanceId) || !all.get(instanceId).isJsonObject()) {
            return EMPTY;
        }
        JsonObject entry = all.getAsJsonObject(instanceId);
        Integer memory = entry.has("memoryMb") && !entry.get("memoryMb").isJsonNull()
                ? entry.get("memoryMb").getAsInt() : null;
        String jvm = entry.has("jvmArgs") ? trimToNull(entry.get("jvmArgs").getAsString()) : null;
        String javaPath = entry.has("javaPath") ? trimToNull(entry.get("javaPath").getAsString()) : null;
        Settings migrated = new Settings(memory, jvm, javaPath, null, null);
        if (!migrated.isEmpty()) {
            // 迁移落地：写进实例目录，并从旧表里摘掉
            write(gameDir, instanceId, migrated);
        }
        return migrated;
    }

    private static void clearLegacy(File gameDir, String instanceId) {
        File file = legacyFile(gameDir);
        if (!file.isFile()) {
            return;
        }
        JsonObject all = loadLegacy(gameDir);
        if (!all.has(instanceId)) {
            return;
        }
        all.remove(instanceId);
        try {
            if (all.isEmpty()) {
                Files.deleteIfExists(file.toPath());
            } else {
                Files.writeString(file.toPath(), GSON.toJson(all), StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {
            // 清不掉就留着，下次再清
        }
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
