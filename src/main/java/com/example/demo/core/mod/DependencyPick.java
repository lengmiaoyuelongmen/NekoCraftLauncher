package com.example.demo.core.mod;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;

/**
 * 从 Modrinth 的版本列表里挑出「当前实例能用」的那个文件（用于自动补前置）。
 *
 * <p>纯解析 + 选择，不做网络；{@link VersionFit} 负责适配排序，这里再取主文件。
 */
public final class DependencyPick {

    /** 选中的下载文件。 */
    public record Artifact(String versionNumber, String url, String filename, long size) {
    }

    private DependencyPick() {
    }

    /**
     * @param versionsJson Modrinth `/project/{id}/version` 的响应
     * @param target 当前实例的 MC 版本与加载器
     * @return 选中的文件；没有适配版本或没有主文件时返回 null
     */
    public static Artifact pick(String versionsJson, VersionFit.Target target) {
        if (versionsJson == null || versionsJson.isBlank()) {
            return null;
        }
        JsonArray versions;
        try {
            versions = JsonParser.parseString(versionsJson).getAsJsonArray();
        } catch (Exception e) {
            return null;
        }
        if (versions.isEmpty()) {
            return null;
        }
        List<VersionFit.Candidate> candidates = new ArrayList<>();
        for (JsonElement element : versions) {
            if (!element.isJsonObject()) {
                candidates.add(new VersionFit.Candidate("", "", List.of(), List.of()));
                continue;
            }
            JsonObject version = element.getAsJsonObject();
            candidates.add(new VersionFit.Candidate(
                    version.has("version_number") ? version.get("version_number").getAsString() : "",
                    version.has("version_type") ? version.get("version_type").getAsString() : "",
                    strings(version, "game_versions"),
                    strings(version, "loaders")));
        }
        int best = VersionFit.bestMatch(candidates, target);
        if (best < 0) {
            return null;
        }
        JsonObject chosen = versions.get(best).getAsJsonObject();
        JsonArray files = chosen.has("files") && chosen.get("files").isJsonArray()
                ? chosen.getAsJsonArray("files")
                : null;
        if (files == null || files.isEmpty()) {
            return null;
        }
        JsonObject file = primaryFile(files);
        String url = file.has("url") ? file.get("url").getAsString() : "";
        String filename = file.has("filename") ? file.get("filename").getAsString() : "";
        if (url.isBlank() || filename.isBlank()) {
            return null;
        }
        long size = file.has("size") ? file.get("size").getAsLong() : 0L;
        String number = chosen.has("version_number") ? chosen.get("version_number").getAsString() : "";
        return new Artifact(number, url, filename, size);
    }

    /** 优先 primary 标记，没有就取第一个。 */
    private static JsonObject primaryFile(JsonArray files) {
        JsonObject fallback = files.get(0).getAsJsonObject();
        for (JsonElement element : files) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject file = element.getAsJsonObject();
            if (file.has("primary") && file.get("primary").getAsBoolean()) {
                return file;
            }
        }
        return fallback;
    }

    private static List<String> strings(JsonObject object, String key) {
        if (!object.has(key) || !object.get(key).isJsonArray()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (JsonElement element : object.getAsJsonArray(key)) {
            result.add(element.getAsString());
        }
        return result;
    }

    /** 自动补前置时的接口地址（与版本页保持一致：官方优先，镜像兜底）。 */
    public static String officialUrl(String projectIdOrSlug) {
        return "https://api.modrinth.com/v2/project/" + projectIdOrSlug + "/version";
    }

    public static String mirrorUrl(String projectIdOrSlug) {
        return "https://mod.mcimirror.top/modrinth/v2/project/" + projectIdOrSlug + "/version";
    }
}
