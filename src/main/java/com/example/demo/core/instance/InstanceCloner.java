package com.example.demo.core.instance;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Set;

/**
 * 克隆实例：复制 {@code versions/<id>} 到 {@code versions/<id>-copy}，并把版本 JSON 里的 id 改掉。
 *
 * <p>默认只复制「版本本身」（json / jar / natives / 加载器文件），不复制存档与模组；
 * {@code copyData=true} 时连 mods / saves / config 一起复制（想保留整合包内容时用）。
 *
 * <p>对标 Prism/HMCL 的「复制实例」：新实例能独立启动、能单独改设置。
 */
public final class InstanceCloner {

    /** 只复制版本时跳过的数据目录。 */
    public static final Set<String> DATA_DIRS = Set.of(
            "mods", "config", "defaultconfigs", "saves", "screenshots",
            "resourcepacks", "shaderpacks", "logs", "crash-reports", "schematics");

    private InstanceCloner() {
    }

    /**
     * @return 新实例目录
     */
    public static File duplicate(File gameDir, String sourceId, boolean copyData) throws Exception {
        File versions = new File(gameDir, "versions");
        File source = new File(versions, sourceId);
        if (!source.isDirectory()) {
            throw new IllegalArgumentException("实例目录不存在: " + source.getAbsolutePath());
        }
        String newId = freeId(versions, sourceId + "-copy");
        File target = new File(versions, newId);

        InstanceFiles.copyTree(source, target, copyData ? Set.of() : DATA_DIRS);
        renameVersionJson(target, sourceId, newId);
        return target;
    }

    /** 找一个还没被占用的 id：{@code foo-copy}、{@code foo-copy2}… */
    static String freeId(File versions, String base) {
        String candidate = base;
        int index = 2;
        while (new File(versions, candidate).exists()) {
            candidate = base + index++;
        }
        return candidate;
    }

    /** 把 {@code <oldId>.json} 里的 id 改成新 id，并重命名文件。 */
    private static void renameVersionJson(File target, String oldId, String newId) throws Exception {
        File json = new File(target, oldId + ".json");
        if (!json.isFile()) {
            return;
        }
        JsonObject object = JsonParser.parseString(
                Files.readString(json.toPath(), StandardCharsets.UTF_8)).getAsJsonObject();
        object.addProperty("id", newId);
        File renamed = new File(target, newId + ".json");
        Files.writeString(renamed.toPath(),
                new GsonBuilder().setPrettyPrinting().create().toJson(object),
                StandardCharsets.UTF_8);
        if (!renamed.getAbsolutePath().equals(json.getAbsolutePath())) {
            Files.deleteIfExists(json.toPath());
        }
    }
}
