package com.example.demo.core.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 服务端目录里的可读可写文件：server.properties 与玩家名单。
 *
 * <p>面板化的关键一步：把「只能开服」变成「能看能改」。
 * properties 用有序 Map 保存，写回时只改值、不动顺序和注释掉的旧键，避免把别人的配置搅乱。
 */
public final class ServerFiles {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private ServerFiles() {
    }

    // ==================== server.properties ====================

    /** 读取为「键 → 值」的有序映射；注释与空行统一丢弃（写回时按标准格式重写）。 */
    public static Map<String, String> readProperties(File dir) {
        Map<String, String> map = new LinkedHashMap<>();
        File file = new File(dir, "server.properties");
        if (!file.isFile()) {
            return map;
        }
        try {
            for (String line : Files.readAllLines(file.toPath(), StandardCharsets.UTF_8)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                int eq = trimmed.indexOf('=');
                if (eq <= 0) {
                    continue;
                }
                map.put(trimmed.substring(0, eq).trim(), trimmed.substring(eq + 1).trim());
            }
        } catch (IOException ignored) {
            // 读失败就当空配置
        }
        return map;
    }

    /** 写回：先把原文件读成 Map，覆盖给定键，再整体写回（保持已有键顺序）。 */
    public static void writeProperties(File dir, Map<String, String> updates) throws IOException {
        Map<String, String> map = readProperties(dir);
        for (Map.Entry<String, String> entry : updates.entrySet()) {
            map.put(entry.getKey(), entry.getValue());
        }
        StringBuilder text = new StringBuilder();
        text.append("#Minecraft server properties\r\n");
        text.append("#由 Neko Craft Launcher 写回\r\n");
        for (Map.Entry<String, String> entry : map.entrySet()) {
            text.append(entry.getKey()).append('=').append(entry.getValue()).append("\r\n");
        }
        Files.writeString(new File(dir, "server.properties").toPath(), text.toString(),
                StandardCharsets.UTF_8);
    }

    // ==================== 玩家名单 ====================

    /** 一个玩家名单条目（ops / whitelist / banned 共用一个结构）。 */
    public record PlayerEntry(String name, String uuid, String level, String reason) {
    }

    public static List<PlayerEntry> readOps(File dir) {
        return readPlayerFile(new File(dir, "ops.json"), true);
    }

    public static List<PlayerEntry> readWhitelist(File dir) {
        return readPlayerFile(new File(dir, "whitelist.json"), false);
    }

    public static List<PlayerEntry> readBanned(File dir) {
        return readPlayerFile(new File(dir, "banned-players.json"), false);
    }

    private static List<PlayerEntry> readPlayerFile(File file, boolean withLevel) {
        List<PlayerEntry> list = new ArrayList<>();
        if (!file.isFile()) {
            return list;
        }
        try {
            JsonArray array = JsonParser.parseString(Files.readString(file.toPath(), StandardCharsets.UTF_8))
                    .getAsJsonArray();
            for (var element : array) {
                JsonObject object = element.getAsJsonObject();
                list.add(new PlayerEntry(
                        get(object, "name"),
                        get(object, "uuid"),
                        withLevel ? get(object, "level") : "",
                        get(object, "reason")));
            }
        } catch (Exception ignored) {
            // 文件坏了就当没有
        }
        return list;
    }

    /** 追加一条 ops.json（需要服务端重启或 /reload 才生效）。 */
    public static void addOp(File dir, String name, String uuid) throws IOException {
        File file = new File(dir, "ops.json");
        JsonArray array = file.isFile()
                ? JsonParser.parseString(Files.readString(file.toPath(), StandardCharsets.UTF_8)).getAsJsonArray()
                : new JsonArray();
        for (var element : array) {
            if (name.equalsIgnoreCase(get(element.getAsJsonObject(), "name"))) {
                return;
            }
        }
        JsonObject entry = new JsonObject();
        entry.addProperty("uuid", uuid == null ? "" : uuid);
        entry.addProperty("name", name);
        entry.addProperty("level", 4);
        entry.addProperty("bypassesPlayerLimit", false);
        array.add(entry);
        Files.writeString(file.toPath(), GSON.toJson(array), StandardCharsets.UTF_8);
    }

    /** 追加一条 whitelist.json。 */
    /**
     * 从 server-console.log 里推断当前在线玩家（原版日志的 joined / left the game 事件）。
     *
     * <p>只读日志、不连服务器，所以离线也能用；日志被截断时最多不准，不会报错。
     */
    /** 解析 /list 的输出：「There are 2 of a max of 20 players online: Steve, Alex」。 */
    public static java.util.List<String> parseListResponse(String consoleTail) {
        java.util.List<String> players = new java.util.ArrayList<>();
        if (consoleTail == null) {
            return players;
        }
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("players online:\\s*(.*)$");
        for (String line : consoleTail.split("\\R")) {
            java.util.regex.Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                String names = matcher.group(1).trim();
                if (names.isEmpty()) {
                    continue;
                }
                for (String name : names.split(",")) {
                    String trimmed = name.trim();
                    if (!trimmed.isEmpty()) {
                        players.add(trimmed);
                    }
                }
            }
        }
        return players;
    }

    /** 读日志尾部若干行（校准 /list 用）。 */
    public static String tail(File dir, int lines) {
        File log = new File(dir, "server-console.log");
        if (!log.isFile()) {
            return "";
        }
        try {
            java.util.List<String> all = java.nio.file.Files.readAllLines(log.toPath());
            int from = Math.max(0, all.size() - Math.max(1, lines));
            return String.join("\n", all.subList(from, all.size()));
        } catch (Exception e) {
            return "";
        }
    }
    public static java.util.List<String> onlinePlayers(File dir) {
        File log = new File(dir, "server-console.log");
        java.util.LinkedHashSet<String> online = new java.util.LinkedHashSet<>();
        if (!log.isFile()) {
            return new java.util.ArrayList<>(online);
        }
        try {
            java.util.List<String> lines = java.nio.file.Files.readAllLines(log.toPath());
            java.util.regex.Pattern join = java.util.regex.Pattern
                    .compile("([A-Za-z0-9_]{1,16}) joined the game");
            java.util.regex.Pattern leave = java.util.regex.Pattern
                    .compile("([A-Za-z0-9_]{1,16}) left the game");
            for (String line : lines) {
                java.util.regex.Matcher left = leave.matcher(line);
                if (left.find()) {
                    online.remove(left.group(1));
                    continue;
                }
                java.util.regex.Matcher joined = join.matcher(line);
                if (joined.find()) {
                    online.add(joined.group(1));
                }
            }
        } catch (Exception ignored) {
            // 日志读不到就当作没有在线玩家
        }
        return new java.util.ArrayList<>(online);
    }
    /** 白名单里没有就追加；返回 true 表示这次真的加了。 */
    public static boolean addWhitelistIfMissing(File dir, String name) throws IOException {
        if (name == null || name.isBlank()) {
            return false;
        }
        for (PlayerEntry entry : readWhitelist(dir)) {
            if (entry.name() != null && entry.name().equalsIgnoreCase(name.trim())) {
                return false;
            }
        }
        addWhitelist(dir, name.trim(), "");
        return true;
    }
    public static void addWhitelist(File dir, String name, String uuid) throws IOException {
        File file = new File(dir, "whitelist.json");
        JsonArray array = file.isFile()
                ? JsonParser.parseString(Files.readString(file.toPath(), StandardCharsets.UTF_8)).getAsJsonArray()
                : new JsonArray();
        for (var element : array) {
            if (name.equalsIgnoreCase(get(element.getAsJsonObject(), "name"))) {
                return;
            }
        }
        JsonObject entry = new JsonObject();
        entry.addProperty("uuid", uuid == null ? "" : uuid);
        entry.addProperty("name", name);
        array.add(entry);
        Files.writeString(file.toPath(), GSON.toJson(array), StandardCharsets.UTF_8);
    }

    private static String get(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : "";
    }

    /** 从 ops.json 里移除某个玩家（按名字，大小写不敏感）。 */
    public static void removeOp(File dir, String name) throws IOException {
        removePlayer(new File(dir, "ops.json"), name);
    }

    /** 从 whitelist.json 里移除某个玩家。 */
    public static void removeWhitelist(File dir, String name) throws IOException {
        removePlayer(new File(dir, "whitelist.json"), name);
    }

    private static void removePlayer(File file, String name) throws IOException {
        if (!file.isFile() || name == null || name.isBlank()) {
            return;
        }
        JsonArray array = JsonParser.parseString(Files.readString(file.toPath(), StandardCharsets.UTF_8))
                .getAsJsonArray();
        JsonArray kept = new JsonArray();
        for (var element : array) {
            JsonObject object = element.getAsJsonObject();
            if (!name.equalsIgnoreCase(get(object, "name"))) {
                kept.add(object);
            }
        }
        Files.writeString(file.toPath(), GSON.toJson(kept), StandardCharsets.UTF_8);
    }
}
