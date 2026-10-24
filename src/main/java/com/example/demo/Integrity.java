package com.example.demo;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * 实例完整性检查：版本 JSON 引用的库文件、客户端 jar 是否都在，
 * 缺失的可以走 DownloadManager 一键补齐。
 */
public final class Integrity {

    /** 缺失文件的类型。 */
    public enum Kind {
        LIBRARY,
        CLIENT_JAR,
        ASSET_INDEX,
        ASSET_OBJECT
    }

    /** 一个缺失的文件。 */
    public record Issue(String instanceId, String path, String url, String sha1, long size, Kind kind) {
        public String label() {
            return switch (kind) {
                case CLIENT_JAR -> "客户端 jar";
                case ASSET_INDEX -> "资源索引";
                case ASSET_OBJECT -> "资源对象";
                default -> "库文件";
            };
        }
    }

    /** 检查结果：检查了多少个 + 缺失列表。 */
    public record Report(int checked, List<Issue> issues) {
    }

    private Integrity() {
    }

    /**
     * @param onlyInstance null 表示检查全部已安装实例
     * @param assetSamples 每个实例抽样多少个 assets 对象（0 = 不抽样）
     */
    public static Report check(File gameDir, String onlyInstance, int assetSamples) {
        List<Issue> issues = new ArrayList<>();
        int checked = 0;
        List<String> ids = onlyInstance == null
                ? VersionProfile.installedIds(gameDir)
                : List.of(onlyInstance);
        for (String id : ids) {
            try {
                VersionProfile profile = VersionProfile.load(gameDir, id);
                checked += collect(gameDir, id, profile, issues, assetSamples);
            } catch (Exception ignored) {
                // 解析失败的实例跳过
            }
        }
        return new Report(checked, issues);
    }

    /** 兼容旧调用：默认抽样 20 个。 */
    public static Report check(File gameDir, String onlyInstance) {
        return check(gameDir, onlyInstance, 20);
    }

    /** 直接检查一个已加载的 VersionProfile（启动前自检用）。 */
    public static Report checkProfile(File gameDir, String instanceId, VersionProfile profile) {
        return checkProfile(gameDir, instanceId, profile, 20);
    }

    public static Report checkProfile(File gameDir, String instanceId, VersionProfile profile, int assetSamples) {
        List<Issue> issues = new ArrayList<>();
        int checked = collect(gameDir, instanceId, profile, issues, assetSamples);
        return new Report(checked, issues);
    }

    private static int collect(File gameDir, String id, VersionProfile profile, List<Issue> issues,
                               int assetSamples) {
        int checked = 0;
        for (JsonObject library : profile.classpathLibraries()) {
            JsonObject artifact = artifact(library);
            if (artifact == null) continue;
            String path = artifact.has("path") ? artifact.get("path").getAsString() : null;
            if (path == null || path.isBlank()) continue;
            checked++;
            if (new File(new File(gameDir, "libraries"), path.replace('/', File.separatorChar)).isFile()) continue;
            String url = artifact.has("url") ? artifact.get("url").getAsString() : "";
            if (url == null || url.isBlank()) {
                url = defaultUrl(path);
            }
            String sha1 = artifact.has("sha1") ? artifact.get("sha1").getAsString() : null;
            long size = artifact.has("size") ? artifact.get("size").getAsLong() : -1;
            issues.add(new Issue(id, path, url, sha1, size, Kind.LIBRARY));
        }

        // 客户端 jar 与资源索引：都从根版本 JSON 里取信息
        String rootId = profile.getRootId();
        JsonObject rootJson = readRootJson(gameDir, rootId);

        File clientJar = new File(new File(new File(gameDir, "versions"), rootId), rootId + ".jar");
        checked++;
        if (!clientJar.isFile()) {
            String url = "";
            String sha1 = null;
            long size = -1;
            if (rootJson != null && rootJson.has("downloads") && rootJson.get("downloads").isJsonObject()) {
                JsonObject downloads = rootJson.getAsJsonObject("downloads");
                if (downloads.has("client") && downloads.get("client").isJsonObject()) {
                    JsonObject client = downloads.getAsJsonObject("client");
                    if (client.has("url")) url = client.get("url").getAsString();
                    if (client.has("sha1")) sha1 = client.get("sha1").getAsString();
                    if (client.has("size")) size = client.get("size").getAsLong();
                }
            }
            if (url == null || url.isBlank()) {
                url = "https://bmclapi2.bangbang93.com/version/" + rootId + "/client";
            }
            issues.add(new Issue(id, rootId + "/" + rootId + ".jar", url, sha1, size, Kind.CLIENT_JAR));
        }

        if (rootJson != null && rootJson.has("assetIndex") && rootJson.get("assetIndex").isJsonObject()) {
            JsonObject assetIndex = rootJson.getAsJsonObject("assetIndex");
            String indexId = assetIndex.has("id") ? assetIndex.get("id").getAsString() : "";
            if (!indexId.isBlank()) {
                checked++;
                File indexFile = new File(new File(new File(gameDir, "assets"), "indexes"), indexId + ".json");
                if (!indexFile.isFile()) {
                    String url = assetIndex.has("url") ? assetIndex.get("url").getAsString() : "";
                    if (url == null || url.isBlank()) {
                        url = "https://bmclapi2.bangbang93.com/assets/indexes/" + indexId + ".json";
                    }
                    String sha1 = assetIndex.has("sha1") ? assetIndex.get("sha1").getAsString() : null;
                    long size = assetIndex.has("size") ? assetIndex.get("size").getAsLong() : -1;
                    issues.add(new Issue(id, indexId + ".json", url, sha1, size, Kind.ASSET_INDEX));
                } else if (assetSamples > 0) {
                    checked += sampleAssets(gameDir, id, indexFile, issues, assetSamples);
                }
            }
        }
        return checked;
    }

    private static JsonObject readRootJson(File gameDir, String rootId) {
        File json = new File(new File(new File(gameDir, "versions"), rootId), rootId + ".json");
        if (!json.isFile()) return null;
        try {
            return JsonParser.parseString(Files.readString(json.toPath(), StandardCharsets.UTF_8))
                    .getAsJsonObject();
        } catch (Exception e) {
            return null;
        }
    }

    /** 下载缺失文件，返回成功补齐的数量（失败会用备用源重试一次）。 */
    public static int repair(File gameDir, List<Issue> issues) {
        if (issues == null || issues.isEmpty()) return 0;
        List<DownloadManager.Task> tasks = new ArrayList<>();
        for (Issue issue : issues) {
            tasks.add(new DownloadManager.Task(issue.url(), target(gameDir, issue), issue.sha1(), issue.size()));
        }
        DownloadManager.downloadAll(tasks, 6, null);

        // 失败的重试一次：换成更可能命中的源
        for (Issue issue : issues) {
            File target = target(gameDir, issue);
            if (target.isFile()) continue;
            String fallback = fallbackUrl(issue);
            if (fallback == null || fallback.equals(issue.url())) continue;
            DownloadManager.downloadAll(
                    List.of(new DownloadManager.Task(fallback, target, issue.sha1(), issue.size())), 1, null);
        }

        int fixed = 0;
        for (Issue issue : issues) {
            if (target(gameDir, issue).isFile()) fixed++;
        }
        return fixed;
    }

    private static int sampleAssets(File gameDir, String instanceId, File indexFile, List<Issue> issues,
                                    int limit) {
        int checked = 0;
        try {
            JsonObject root = JsonParser.parseString(
                    Files.readString(indexFile.toPath(), StandardCharsets.UTF_8)).getAsJsonObject();
            if (!root.has("objects") || !root.get("objects").isJsonObject()) return 0;
            JsonObject objects = root.getAsJsonObject("objects");
            // 自适应：索引越大抽样越多，但不超过配置上限，至少 5 个
            int effective = Math.min(limit, Math.max(5, objects.size() / 200));
            for (var entry : objects.entrySet()) {
                if (checked >= effective) break;
                if (!entry.getValue().isJsonObject()) continue;
                JsonObject object = entry.getValue().getAsJsonObject();
                if (!object.has("hash")) continue;
                String hash = object.get("hash").getAsString();
                if (hash.length() < 2) continue;
                checked++;
                String prefix = hash.substring(0, 2);
                File file = new File(new File(new File(gameDir, "assets"), "objects"),
                        prefix + File.separator + hash);
                if (file.isFile()) continue;
                long size = object.has("size") ? object.get("size").getAsLong() : -1;
                issues.add(new Issue(instanceId, prefix + "/" + hash,
                        "https://resources.download.minecraft.net/" + prefix + "/" + hash,
                        hash, size, Kind.ASSET_OBJECT));
            }
        } catch (Exception ignored) {
            // 索引损坏就跳过抽样
        }
        return checked;
    }

    private static File target(File gameDir, Issue issue) {
        String relative = issue.path().replace('/', File.separatorChar);
        return switch (issue.kind()) {
            case CLIENT_JAR -> new File(new File(gameDir, "versions"), relative);
            case ASSET_INDEX -> new File(new File(new File(gameDir, "assets"), "indexes"), relative);
            case ASSET_OBJECT -> new File(new File(new File(gameDir, "assets"), "objects"), relative);
            default -> new File(new File(gameDir, "libraries"), relative);
        };
    }

    /** 按库坐标猜默认 Maven 源。 */
    private static String defaultUrl(String path) {
        String lower = path.toLowerCase();
        if (lower.startsWith("net/minecraftforge/") || lower.startsWith("cpw/mods/")) {
            return "https://maven.minecraftforge.net/" + path;
        }
        if (lower.startsWith("net/neoforged/")) {
            return "https://maven.neoforged.net/releases/" + path;
        }
        return "https://libraries.minecraft.net/" + path;
    }

    /** 备用源：Mojang 库优先中央仓库，Forge/NeoForge 库回退官方 Maven，客户端 jar 回退 BMCLAPI。 */
    private static String fallbackUrl(Issue issue) {
        if (issue.kind() == Kind.ASSET_INDEX) {
            return "https://bmclapi2.bangbang93.com/assets/indexes/" + issue.path();
        }
        if (issue.kind() == Kind.ASSET_OBJECT) {
            return "https://bmclapi2.bangbang93.com/assets/" + issue.path();
        }
        if (issue.kind() == Kind.CLIENT_JAR) {
            String path = issue.path();
            int slash = path.indexOf('/');
            String id = slash > 0 ? path.substring(0, slash) : path;
            return "https://bmclapi2.bangbang93.com/version/" + id + "/client";
        }
        String lower = issue.path().toLowerCase();
        if (lower.startsWith("net/minecraftforge/") || lower.startsWith("cpw/mods/")
                || lower.startsWith("net/minecraft/")) {
            return "https://maven.minecraftforge.net/" + issue.path();
        }
        if (lower.startsWith("net/neoforged/")) {
            return "https://maven.neoforged.net/releases/" + issue.path();
        }
        return "https://repo1.maven.org/maven2/" + issue.path();
    }

    private static JsonObject artifact(JsonObject library) {
        if (library.has("downloads") && library.get("downloads").isJsonObject()) {
            JsonObject downloads = library.getAsJsonObject("downloads");
            if (downloads.has("artifact") && downloads.get("artifact").isJsonObject()) {
                return downloads.getAsJsonObject("artifact");
            }
        }
        return null;
    }
}
