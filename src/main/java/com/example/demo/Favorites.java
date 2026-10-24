package com.example.demo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/**
 * 收藏的社区资源（Modrinth 项目）：保存在工作目录的 favorites.json。
 * 和「实例收藏」（InstancesMeta）分开，互不影响。
 */
public final class Favorites {

    /** 一条资源收藏。 */
    public record Fav(String id, String title, String type, String iconUrl, long downloads, long time) {
    }

    private static final File FILE = new File("favorites.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private Favorites() {
    }

    public static List<Fav> list() {
        List<Fav> result = new ArrayList<>();
        JsonArray array = loadArray();
        for (JsonElement element : array) {
            if (!element.isJsonObject()) continue;
            JsonObject object = element.getAsJsonObject();
            if (!object.has("id")) continue;
            result.add(new Fav(
                    object.get("id").getAsString(),
                    object.has("title") ? object.get("title").getAsString() : "",
                    object.has("type") ? object.get("type").getAsString() : "modpack",
                    object.has("iconUrl") && !object.get("iconUrl").isJsonNull()
                            ? object.get("iconUrl").getAsString() : null,
                    object.has("downloads") ? object.get("downloads").getAsLong() : 0,
                    object.has("time") ? object.get("time").getAsLong() : 0));
        }
        return result;
    }

    public static boolean contains(String id) {
        if (id == null) return false;
        for (Fav fav : list()) {
            if (id.equals(fav.id())) return true;
        }
        return false;
    }

    /** 有则取消，没有则添加。 */
    public static void toggle(Fav fav) {
        JsonArray array = loadArray();
        boolean removed = false;
        for (int i = 0; i < array.size(); i++) {
            JsonElement element = array.get(i);
            if (element.isJsonObject() && element.getAsJsonObject().has("id")
                    && fav.id().equals(element.getAsJsonObject().get("id").getAsString())) {
                array.remove(i);
                removed = true;
                break;
            }
        }
        if (!removed) {
            JsonObject object = new JsonObject();
            object.addProperty("id", fav.id());
            object.addProperty("title", fav.title());
            object.addProperty("type", fav.type());
            if (fav.iconUrl() != null) object.addProperty("iconUrl", fav.iconUrl());
            object.addProperty("downloads", fav.downloads());
            object.addProperty("time", fav.time() > 0 ? fav.time() : System.currentTimeMillis());
            array.add(object);
        }
        save(array);
    }

    public static void remove(String id) {
        JsonArray array = loadArray();
        for (int i = 0; i < array.size(); i++) {
            JsonElement element = array.get(i);
            if (element.isJsonObject() && element.getAsJsonObject().has("id")
                    && id.equals(element.getAsJsonObject().get("id").getAsString())) {
                array.remove(i);
                break;
            }
        }
        save(array);
    }

    private static JsonArray loadArray() {
        if (!FILE.isFile()) return new JsonArray();
        try (Reader reader = new FileReader(FILE)) {
            JsonElement element = JsonParser.parseReader(reader);
            return element != null && element.isJsonArray() ? element.getAsJsonArray() : new JsonArray();
        } catch (Exception e) {
            return new JsonArray();
        }
    }

    private static void save(JsonArray array) {
        try (Writer writer = new FileWriter(FILE)) {
            GSON.toJson(array, writer);
        } catch (Exception ignored) {
            // 写失败不影响本次使用
        }
    }
}
