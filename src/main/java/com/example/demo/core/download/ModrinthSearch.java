package com.example.demo.core.download;

import com.example.demo.core.model.ModpackProject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Modrinth 搜索协议：把 UI 里的筛选条件翻译成 URL，再把返回的 JSON 解析成 {@link ModpackProject}。
 *
 * <p>从主类的 {@code loadSearchPage} 里搬出来 —— 这段是整个下载页最关键、也最容易写错的部分
 * （facets 编码、镜像回退、字段缺失），现在可以脱离 UI 单测。
 */
public final class ModrinthSearch {

    public static final String OFFICIAL = "https://api.modrinth.com/v2";
    public static final String MIRROR = "https://mod.mcimirror.top/modrinth/v2";

    /** 一页结果：项目列表 + 总数（未知时 -1）。 */
    public record Page(List<ModpackProject> projects, int totalHits, int rawHits) {
    }

    private ModrinthSearch() {
    }

    /** UI 里的中文类型 → Modrinth 的 project_type。 */
    public static String projectType(String uiType) {
        if (uiType == null) {
            return "modpack";
        }
        return switch (uiType) {
            case "模组" -> "mod";
            case "资源包" -> "resourcepack";
            case "光影包" -> "shader";
            case "数据包" -> "datapack";
            default -> "modpack";
        };
    }

    /** 排序下拉 → Modrinth 的 index 参数。 */
    public static String indexFor(String sortLabel) {
        if ("下载量".equals(sortLabel)) {
            return "downloads";
        }
        if ("更新时间".equals(sortLabel)) {
            return "updated";
        }
        return "relevance";
    }

    /**
     * 拼搜索 URL（官方源）。
     *
     * @param gameVersion 选择「任意」时传 null 或空
     * @param loader      选择「任意」时传 null 或空；数据包不加 loader 条件
     */
    public static String buildUrl(String query, String projectType, String gameVersion,
                                  String loader, String sortLabel, int offset) {
        JsonArray facets = new JsonArray();
        facets.add(arrayOf("project_type:" + projectType));
        if (gameVersion != null && !gameVersion.isBlank() && !"任意".equals(gameVersion)) {
            facets.add(arrayOf("versions:" + gameVersion));
        }
        if (!"datapack".equals(projectType) && loader != null && !loader.isBlank()
                && !"任意".equals(loader)) {
            facets.add(arrayOf("categories:" + loader.toLowerCase(java.util.Locale.ROOT)));
        }
        String encodedFacets = URLEncoder.encode(facets.toString(), StandardCharsets.UTF_8);
        String encodedQuery = URLEncoder.encode(query == null ? "" : query, StandardCharsets.UTF_8);
        return OFFICIAL + "/search?query=" + encodedQuery
                + "&facets=" + encodedFacets
                + "&index=" + indexFor(sortLabel)
                + "&limit=40&offset=" + offset;
    }

    /**
     * 国内镜像地址。
     *
     * <p>实测只有模组 / 整合包的搜索接口在镜像上有缓存，资源包/光影/数据包仍走官方源。
     */
    public static String mirrorUrl(String officialUrl, String projectType) {
        if (!"mod".equals(projectType) && !"modpack".equals(projectType)) {
            return officialUrl;
        }
        return officialUrl.replace(OFFICIAL, MIRROR);
    }

    /**
     * 解析搜索响应。
     *
     * @param existingIds 已经在列表里的项目 id，用来去重
     * @throws IllegalArgumentException JSON 结构不对
     */
    public static Page parse(String body, String projectType, Set<String> existingIds) {
        JsonObject root;
        try {
            root = JsonParser.parseString(body).getAsJsonObject();
        } catch (Exception e) {
            throw new IllegalArgumentException("资源数据解析失败", e);
        }
        int total = root.has("total_hits") && !root.get("total_hits").isJsonNull()
                ? root.get("total_hits").getAsInt() : -1;
        JsonArray hits = root.getAsJsonArray("hits");
        List<ModpackProject> projects = new ArrayList<>();
        if (hits == null) {
            return new Page(projects, total, 0);
        }
        for (JsonElement element : hits) {
            if (element == null || !element.isJsonObject()) {
                continue;
            }
            JsonObject hit = element.getAsJsonObject();
            String id = string(hit, "project_id");
            if (id == null || (existingIds != null && existingIds.contains(id))) {
                continue;
            }
            String title = string(hit, "title");
            String description = string(hit, "description");
            String author = string(hit, "author");
            String iconUrl = string(hit, "icon_url");
            long downloads = hit.has("downloads") ? hit.get("downloads").getAsLong() : 0L;
            String modified = string(hit, "date_modified");
            String updated = modified != null && modified.length() >= 10
                    ? modified.substring(0, 10) : "";
            projects.add(new ModpackProject(id, title == null ? "" : title,
                    description == null ? "" : description, author == null ? "" : author,
                    iconUrl, downloads, updated, "Modrinth", projectType));
        }
        return new Page(projects, total, hits.size());
    }

    private static JsonArray arrayOf(String value) {
        JsonArray array = new JsonArray();
        array.add(value);
        return array;
    }

    private static String string(JsonObject object, String key) {
        if (!object.has(key) || object.get(key).isJsonNull() || !object.get(key).isJsonPrimitive()) {
            return null;
        }
        return object.get(key).getAsString();
    }
}
