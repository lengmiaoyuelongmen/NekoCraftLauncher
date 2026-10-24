package com.example.demo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;

/** 实例的显示名 / 描述（存在 game/instances.json，和 PCL 的实例元数据类似）。 */
public final class InstanceMeta {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static File file(File gameDir) {
        return new File(gameDir, "instances.json");
    }

    private static JsonObject loadAll(File gameDir) {
        File f = file(gameDir);
        if (!f.isFile()) return new JsonObject();
        try (Reader reader = new FileReader(f)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            return root == null ? new JsonObject() : root;
        } catch (Exception e) {
            return new JsonObject();
        }
    }

    private static void saveAll(File gameDir, JsonObject root) {
        try (Writer writer = new FileWriter(file(gameDir))) {
            GSON.toJson(root, writer);
        } catch (Exception ignored) {
        }
    }

    public static String name(File gameDir, String id) {
        JsonObject all = loadAll(gameDir);
        if (all.has(id) && all.get(id).isJsonObject()) {
            JsonObject meta = all.getAsJsonObject(id);
            if (meta.has("name") && !meta.get("name").getAsString().isBlank()) {
                return meta.get("name").getAsString();
            }
        }
        return id;
    }

    public static String description(File gameDir, String id) {
        JsonObject all = loadAll(gameDir);
        if (all.has(id) && all.get(id).isJsonObject()) {
            JsonObject meta = all.getAsJsonObject(id);
            if (meta.has("desc")) return meta.get("desc").getAsString();
        }
        return "";
    }

    public static boolean isFavorite(File gameDir, String id) {
        JsonObject all = loadAll(gameDir);
        if (all.has(id) && all.get(id).isJsonObject()) {
            JsonObject meta = all.getAsJsonObject(id);
            return meta.has("fav") && meta.get("fav").getAsBoolean();
        }
        return false;
    }

    public static void setFavorite(File gameDir, String id, boolean favorite) {
        JsonObject all = loadAll(gameDir);
        JsonObject meta = all.has(id) && all.get(id).isJsonObject()
                ? all.getAsJsonObject(id) : new JsonObject();
        meta.addProperty("fav", favorite);
        all.add(id, meta);
        saveAll(gameDir, all);
    }

    public static void set(File gameDir, String id, String name, String description) {
        JsonObject all = loadAll(gameDir);
        JsonObject meta = all.has(id) && all.get(id).isJsonObject()
                ? all.getAsJsonObject(id) : new JsonObject();
        if (name != null) meta.addProperty("name", name);
        if (description != null) meta.addProperty("desc", description);
        all.add(id, meta);
        saveAll(gameDir, all);
    }
}
