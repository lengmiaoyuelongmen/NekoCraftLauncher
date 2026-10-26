package com.example.demo;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CurseForge 第二来源（无需 API Key）：
 * - 元数据/文件列表走 cfwidget 公共接口；
 * - 下载走 ForgeCDN（mediafilez），URL 由文件 id + 文件名还原；
 * - 只读，不写入任何 CurseForge 账号信息。
 */
public final class CurseForgeClient {

    public record FileEntry(long id, String name, String version, String type) {
        public java.util.List<String> urls() {
            long dir = id / 1000;
            long rest = id % 1000;
            String encoded = URLEncoder.encode(name, StandardCharsets.UTF_8).replace("+", "%20");
            return java.util.List.of(
                    "https://edge.forgecdn.net/files/" + dir + "/"
                            + String.format("%03d", rest) + "/" + encoded,
                    "https://mediafilez.forgecdn.net/files/" + dir + "/" + rest + "/" + encoded,
                    "https://mediafilez.forgecdn.net/files/" + dir + "/"
                            + String.format("%03d", rest) + "/" + encoded,
                    "https://mod.mcimirror.top/files/" + dir + "/" + rest + "/" + encoded);
        }

        public String directUrl() {
            return urls().get(0);
        }

        public String label() {
            return name + (version == null || version.isBlank() ? "" : "  ·  " + version)
                    + (type == null || type.isBlank() ? "" : "  ·  " + type);
        }
    }

    public record Project(long id, String title, String slug, long downloads,
                          String summary, List<FileEntry> files) {
    }

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static final Map<String, Optional<Project>> CACHE = new ConcurrentHashMap<>();

    private CurseForgeClient() {
    }

    public static Project lookup(String slugOrId) {
        if (slugOrId == null || slugOrId.isBlank()) return null;
        String key = slugOrId.toLowerCase(Locale.ROOT);
        return CACHE.computeIfAbsent(key, k -> Optional.ofNullable(fetch(k))).orElse(null);
    }

    // ================== MCIM 镜像（无需 Key，可搜索） ==================

    private static final String MIRROR = "https://mod.mcimirror.top/curseforge/v1";

    public record Hit(long id, String name, String slug, String summary,
                      long downloads, String author, String iconUrl) {
    }

    public record MirrorPage(List<Hit> hits, int total) {
    }

    public record MirrorFile(long id, String displayName, String fileName,
                             String downloadUrl, int releaseType, long size) {
        public String label() {
            String type = switch (releaseType) {
                case 1 -> "release";
                case 2 -> "beta";
                case 3 -> "alpha";
                default -> "file";
            };
            return (displayName == null || displayName.isBlank() ? fileName : displayName)
                    + "  ·  " + type + (size > 0 ? "  ·  " + (size / 1024 / 1024.0) + " MB" : "");
        }

        public java.util.List<String> urls() {
            long dir = id / 1000;
            long rest = id % 1000;
            String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
            java.util.List<String> urls = new ArrayList<>();
            if (downloadUrl != null && !downloadUrl.isBlank()) urls.add(downloadUrl);
            urls.add("https://edge.forgecdn.net/files/" + dir + "/"
                    + String.format("%03d", rest) + "/" + encoded);
            urls.add("https://mediafilez.forgecdn.net/files/" + dir + "/" + rest + "/" + encoded);
            urls.add("https://mod.mcimirror.top/files/" + dir + "/" + rest + "/" + encoded);
            return urls;
        }
    }

    private static int loaderType(String loader) {
        if (loader == null) return 0;
        return switch (loader.toLowerCase(Locale.ROOT)) {
            case "forge" -> 1;
            case "fabric" -> 4;
            case "quilt" -> 5;
            case "neoforge" -> 6;
            default -> 0;
        };
    }

    public static MirrorPage searchMirror(String query, String gameVersion, String loader,
                                          int index, int pageSize) throws Exception {
        StringBuilder url = new StringBuilder(MIRROR)
                .append("/mods/search?gameId=432&classId=6&sortField=6&sortOrder=desc")
                .append("&index=").append(Math.max(0, index))
                .append("&pageSize=").append(Math.max(1, Math.min(50, pageSize)));
        if (query != null && !query.isBlank()) {
            url.append("&searchFilter=").append(URLEncoder.encode(query, StandardCharsets.UTF_8));
        }
        if (gameVersion != null && !gameVersion.isBlank()) {
            url.append("&gameVersion=").append(URLEncoder.encode(gameVersion, StandardCharsets.UTF_8));
        }
        int type = loaderType(loader);
        if (type > 0) url.append("&modLoaderType=").append(type);

        JsonObject root = getJson(url.toString());
        if (root == null || !root.has("data")) return new MirrorPage(List.of(), 0);
        List<Hit> hits = new ArrayList<>();
        for (JsonElement element : root.getAsJsonArray("data")) {
            JsonObject obj = element.getAsJsonObject();
            String icon = "";
            if (obj.has("logo") && obj.get("logo").isJsonObject()) {
                JsonObject logo = obj.getAsJsonObject("logo");
                if (logo.has("thumbnailUrl")) icon = logo.get("thumbnailUrl").getAsString();
                else if (logo.has("url")) icon = logo.get("url").getAsString();
            }
            String author = "";
            if (obj.has("authors") && obj.get("authors").isJsonArray()
                    && !obj.getAsJsonArray("authors").isEmpty()) {
                JsonObject first = obj.getAsJsonArray("authors").get(0).getAsJsonObject();
                if (first.has("name")) author = first.get("name").getAsString();
            }
            hits.add(new Hit(
                    obj.has("id") ? obj.get("id").getAsLong() : 0,
                    obj.has("name") ? obj.get("name").getAsString() : "",
                    obj.has("slug") ? obj.get("slug").getAsString() : "",
                    obj.has("summary") ? obj.get("summary").getAsString() : "",
                    obj.has("downloadCount") ? obj.get("downloadCount").getAsLong() : 0,
                    author, icon));
        }
        int total = 0;
        if (root.has("pagination") && root.get("pagination").isJsonObject()) {
            JsonObject pagination = root.getAsJsonObject("pagination");
            if (pagination.has("totalCount")) total = pagination.get("totalCount").getAsInt();
        }
        return new MirrorPage(hits, total);
    }

    public static List<MirrorFile> mirrorFiles(long modId, String gameVersion, String loader) throws Exception {
        StringBuilder url = new StringBuilder(MIRROR)
                .append("/mods/").append(modId).append("/files?pageSize=50");
        if (gameVersion != null && !gameVersion.isBlank()) {
            url.append("&gameVersion=").append(URLEncoder.encode(gameVersion, StandardCharsets.UTF_8));
        }
        int type = loaderType(loader);
        if (type > 0) url.append("&modLoaderType=").append(type);

        JsonObject root = getJson(url.toString());
        if (root == null || !root.has("data")) return List.of();
        List<MirrorFile> files = new ArrayList<>();
        for (JsonElement element : root.getAsJsonArray("data")) {
            JsonObject obj = element.getAsJsonObject();
            String fileName = obj.has("fileName") ? obj.get("fileName").getAsString() : "";
            if (!fileName.toLowerCase(Locale.ROOT).endsWith(".jar")) continue;
            files.add(new MirrorFile(
                    obj.has("id") ? obj.get("id").getAsLong() : 0,
                    obj.has("displayName") ? obj.get("displayName").getAsString() : fileName,
                    fileName,
                    obj.has("downloadUrl") && !obj.get("downloadUrl").isJsonNull()
                            ? obj.get("downloadUrl").getAsString() : "",
                    obj.has("releaseType") ? obj.get("releaseType").getAsInt() : 0,
                    obj.has("fileLength") ? obj.get("fileLength").getAsLong() : 0));
        }
        files.sort(Comparator.comparingInt((MirrorFile f) -> f.releaseType() == 1 ? 0
                        : f.releaseType() == 2 ? 1 : 2)
                .thenComparing(Comparator.comparingLong(MirrorFile::id).reversed()));
        return files;
    }

    /** 按整合包 manifest 中的 projectID/fileID 精确取文件。 */
    public static MirrorFile fileById(long modId, long fileId) {
        JsonObject root = getJson(MIRROR + "/mods/" + modId + "/files/" + fileId);
        if (root == null || !root.has("data") || !root.get("data").isJsonObject()) return null;
        JsonObject obj = root.getAsJsonObject("data");
        String fileName = obj.has("fileName") ? obj.get("fileName").getAsString() : "";
        if (fileName.isBlank()) return null;
        String downloadUrl = obj.has("downloadUrl") && !obj.get("downloadUrl").isJsonNull()
                ? obj.get("downloadUrl").getAsString() : "";
        if (downloadUrl.isBlank()) {
            FileEntry fallback = new FileEntry(fileId, fileName, "", "");
            downloadUrl = fallback.directUrl();
        }
        return new MirrorFile(fileId,
                obj.has("displayName") ? obj.get("displayName").getAsString() : fileName,
                fileName, downloadUrl,
                obj.has("releaseType") ? obj.get("releaseType").getAsInt() : 0,
                obj.has("fileLength") ? obj.get("fileLength").getAsLong() : -1);
    }

    private static JsonObject getJson(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(25))
                    .header("User-Agent", "MCLauncher/1.0")
                    .header("Accept", "application/json")
                    .GET().build();
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return null;
            JsonElement element = JsonParser.parseString(response.body());
            return element.isJsonObject() ? element.getAsJsonObject() : null;
        } catch (Exception e) {
            return null;
        }
    }
    private static Project fetch(String slug) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(
                            "https://api.cfwidget.com/minecraft/mc-mods/"
                                    + URLEncoder.encode(slug, StandardCharsets.UTF_8)))
                    .timeout(Duration.ofSeconds(20))
                    .header("User-Agent", "MCLauncher/1.0")
                    .GET().build();
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return null;
            JsonObject obj = JsonParser.parseString(response.body()).getAsJsonObject();
            List<FileEntry> files = new ArrayList<>();
            if (obj.has("files") && obj.get("files").isJsonArray()) {
                for (JsonElement element : obj.getAsJsonArray("files")) {
                    JsonObject file = element.getAsJsonObject();
                    if (!file.has("id") || !file.has("name")) continue;
                    files.add(new FileEntry(
                            file.get("id").getAsLong(),
                            file.get("name").getAsString(),
                            file.has("version") ? file.get("version").getAsString() : "",
                            file.has("type") ? file.get("type").getAsString() : ""));
                }
            }
            return new Project(
                    obj.has("id") ? obj.get("id").getAsLong() : 0,
                    obj.has("title") ? obj.get("title").getAsString() : slug,
                    obj.has("slug") ? obj.get("slug").getAsString() : slug,
                    obj.has("downloads") && obj.get("downloads").isJsonObject()
                            && obj.getAsJsonObject("downloads").has("total")
                            ? obj.getAsJsonObject("downloads").get("total").getAsLong() : 0,
                    obj.has("summary") ? obj.get("summary").getAsString() : "",
                    files);
        } catch (Exception e) {
            return null;
        }
    }

    /** 按当前游戏版本 + 加载器筛选，release 优先、新文件优先。 */
    public static List<FileEntry> matching(Project project, String gameVersion, String loader) {
        if (project == null) return List.of();
        List<FileEntry> result = new ArrayList<>();
        String loaderKey = loader == null ? "" : loader.toLowerCase(Locale.ROOT);
        for (FileEntry file : project.files()) {
            String name = file.name().toLowerCase(Locale.ROOT);
            if (!name.endsWith(".jar")) continue;
            if (gameVersion != null && !gameVersion.isBlank() && !file.version().equals(gameVersion)) continue;
            if (!loaderKey.isBlank() && !"原版".equals(loaderKey)) {
                boolean fabric = name.contains("fabric");
                boolean neoforge = name.contains("neoforge");
                boolean forge = name.contains("forge") && !neoforge;
                boolean quilt = name.contains("quilt");
                switch (loaderKey) {
                    case "fabric" -> {
                        if (!fabric) continue;
                    }
                    case "forge" -> {
                        if (!forge) continue;
                    }
                    case "neoforge" -> {
                        if (!neoforge) continue;
                    }
                    case "quilt" -> {
                        if (!quilt) continue;
                    }
                    default -> {
                        // 未知加载器不过滤文件名
                    }
                }
            }
            result.add(file);
        }
        result.sort(Comparator
                .comparingInt((FileEntry f) -> "release".equals(f.type()) ? 0 : "beta".equals(f.type()) ? 1 : 2)
                .thenComparing(Comparator.comparingLong(FileEntry::id).reversed()));
        return result;
    }
}
