package com.example.demo.core.download;

import com.example.demo.core.launch.NativeExtractor;

import com.example.demo.core.meta.VersionProfile;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 实例完整性检查：版本 JSON 引用的库文件、客户端 jar、资源是否都在、是否完整。
 *
 * <p>默认只比大小（快，够发现「没下完 / 0 字节」）；{@code verifySha1=true} 时再算 SHA1，
 * 能发现「大小恰好一样但内容坏了」的文件 —— 手动点「校验文件」走这条路。
 *
 * <p>缺失/损坏的可以交给 {@link #repair(File, List)} 一键补齐，补完同样会按大小 + SHA1 复核。
 */
public final class Integrity {

    /** 缺失文件的类型。 */
    public enum Kind {
        LIBRARY,
        CLIENT_JAR,
        NATIVE,
        ASSET_INDEX,
        ASSET_OBJECT
    }

    /** 文件为什么被判为有问题。 */
    public enum Reason {
        /** 文件不存在。 */
        MISSING,
        /** 大小对不上（0 字节 / 截断 / 换了个文件）。 */
        SIZE,
        /** 大小对但 SHA1 不符（损坏或被替换）。 */
        SHA1
    }

    /** 一个有问题的文件。 */
    public record Issue(String instanceId, String path, String url, String sha1, long size, Kind kind,
                        Reason reason) {
        public String label() {
            String base = switch (kind) {
                case CLIENT_JAR -> "客户端 jar";
                case NATIVE -> "原生库";
                case ASSET_INDEX -> "资源索引";
                case ASSET_OBJECT -> "资源对象";
                default -> "库文件";
            };
            return switch (reason) {
                case MISSING -> base;
                case SIZE -> base + "（大小不符）";
                case SHA1 -> base + "（SHA1 不符）";
            };
        }
    }

    /** 检查结果：检查了多少个 + 有问题的列表。 */
    public record Report(int checked, List<Issue> issues) {
    }

    private Integrity() {
    }

    /**
     * @param onlyInstance null 表示检查全部已安装实例
     * @param assetSamples 每个实例抽样多少个 assets 对象（0 = 不抽样）
     */
    public static Report check(File gameDir, String onlyInstance, int assetSamples) {
        return check(gameDir, onlyInstance, assetSamples, false);
    }

    /**
     * @param verifySha1 true 时对「大小相符」的文件再算一遍 SHA1（慢，适合手动「校验文件」）
     */
    public static Report check(File gameDir, String onlyInstance, int assetSamples, boolean verifySha1) {
        List<Issue> issues = new ArrayList<>();
        int checked = 0;
        Map<String, String> hashCache = new HashMap<>();
        List<String> ids = onlyInstance == null
                ? VersionProfile.installedIds(gameDir)
                : List.of(onlyInstance);
        for (String id : ids) {
            try {
                VersionProfile profile = VersionProfile.load(gameDir, id);
                checked += collect(gameDir, id, profile, issues, assetSamples, verifySha1, hashCache);
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
        return checkProfile(gameDir, instanceId, profile, 20, false);
    }

    public static Report checkProfile(File gameDir, String instanceId, VersionProfile profile, int assetSamples) {
        return checkProfile(gameDir, instanceId, profile, assetSamples, false);
    }

    public static Report checkProfile(File gameDir, String instanceId, VersionProfile profile,
                                      int assetSamples, boolean verifySha1) {
        List<Issue> issues = new ArrayList<>();
        int checked = collect(gameDir, instanceId, profile, issues, assetSamples, verifySha1, new HashMap<>());
        return new Report(checked, issues);
    }

    private static int collect(File gameDir, String id, VersionProfile profile, List<Issue> issues,
                               int assetSamples, boolean verifySha1, Map<String, String> hashCache) {
        int checked = 0;
        for (JsonObject library : profile.classpathLibraries()) {
            JsonObject artifact = artifact(library);
            if (artifact == null) {
                continue;
            }
            String path = artifact.has("path") ? artifact.get("path").getAsString() : null;
            if (path == null || path.isBlank()) {
                continue;
            }
            checked++;
            String url = artifact.has("url") ? artifact.get("url").getAsString() : "";
            if (url == null || url.isBlank()) {
                url = defaultUrl(path);
            }
            String sha1 = artifact.has("sha1") ? artifact.get("sha1").getAsString() : null;
            long size = artifact.has("size") ? artifact.get("size").getAsLong() : -1;
            File local = new File(new File(gameDir, "libraries"), path.replace('/', File.separatorChar));
            Reason reason = verify(local, sha1, size, verifySha1, hashCache);
            if (reason != null) {
                issues.add(new Issue(id, path, url, sha1, size, Kind.LIBRARY, reason));
            }
        }

        // 原生库 jar：缺了游戏会因为找不到 lwjgl/jna 的 dll 起不来
        for (JsonObject library : profile.nativeLibraries()) {
            NativeExtractor.NativeArtifact nativeLib = NativeExtractor.nativeArtifact(library);
            if (nativeLib == null) {
                continue;
            }
            checked++;
            File local = new File(new File(gameDir, "libraries"),
                    nativeLib.path().replace('/', File.separatorChar));
            String url = nativeLib.url() == null || nativeLib.url().isBlank()
                    ? defaultUrl(nativeLib.path()) : nativeLib.url();
            Reason reason = verify(local, nativeLib.sha1(), nativeLib.size(), verifySha1, hashCache);
            if (reason != null) {
                issues.add(new Issue(id, nativeLib.path(), url, nativeLib.sha1(), nativeLib.size(),
                        Kind.NATIVE, reason));
            }
        }

        // 客户端 jar 与资源索引：都从根版本 JSON 里取信息
        String rootId = profile.getRootId();
        JsonObject rootJson = readRootJson(gameDir, rootId);

        File clientJar = new File(new File(new File(gameDir, "versions"), rootId), rootId + ".jar");
        checked++;
        String clientUrl = "";
        String clientSha1 = null;
        long clientSize = -1;
        if (rootJson != null && rootJson.has("downloads") && rootJson.get("downloads").isJsonObject()) {
            JsonObject downloads = rootJson.getAsJsonObject("downloads");
            if (downloads.has("client") && downloads.get("client").isJsonObject()) {
                JsonObject client = downloads.getAsJsonObject("client");
                if (client.has("url")) {
                    clientUrl = client.get("url").getAsString();
                }
                if (client.has("sha1")) {
                    clientSha1 = client.get("sha1").getAsString();
                }
                if (client.has("size")) {
                    clientSize = client.get("size").getAsLong();
                }
            }
        }
        if (clientUrl == null || clientUrl.isBlank()) {
            clientUrl = "https://bmclapi2.bangbang93.com/version/" + rootId + "/client";
        }
        Reason clientReason = verify(clientJar, clientSha1, clientSize, verifySha1, hashCache);
        if (clientReason != null) {
            issues.add(new Issue(id, rootId + "/" + rootId + ".jar", clientUrl, clientSha1, clientSize,
                    Kind.CLIENT_JAR, clientReason));
        }

        if (rootJson != null && rootJson.has("assetIndex") && rootJson.get("assetIndex").isJsonObject()) {
            JsonObject assetIndex = rootJson.getAsJsonObject("assetIndex");
            String indexId = assetIndex.has("id") ? assetIndex.get("id").getAsString() : "";
            if (!indexId.isBlank()) {
                checked++;
                File indexFile = new File(new File(new File(gameDir, "assets"), "indexes"), indexId + ".json");
                String url = assetIndex.has("url") ? assetIndex.get("url").getAsString() : "";
                if (url == null || url.isBlank()) {
                    url = "https://bmclapi2.bangbang93.com/assets/indexes/" + indexId + ".json";
                }
                String sha1 = assetIndex.has("sha1") ? assetIndex.get("sha1").getAsString() : null;
                long size = assetIndex.has("size") ? assetIndex.get("size").getAsLong() : -1;
                Reason reason = verify(indexFile, sha1, size, verifySha1, hashCache);
                if (reason != null) {
                    issues.add(new Issue(id, indexId + ".json", url, sha1, size, Kind.ASSET_INDEX, reason));
                } else if (assetSamples > 0) {
                    checked += sampleAssets(gameDir, id, indexFile, issues, assetSamples, verifySha1, hashCache);
                }
            }
        }
        return checked;
    }

    /**
     * 判断一个文件是否完好：不存在 → MISSING；大小不符 → SIZE；SHA1 不符 → SHA1；OK 返回 null。
     *
     * @param hashCache 同一次检查里相同文件的 SHA1 只算一次（多个实例共享 libraries）
     */
    private static Reason verify(File file, String expectedSha1, long expectedSize, boolean verifySha1,
                                 Map<String, String> hashCache) {
        if (!file.isFile() || file.length() == 0) {
            return Reason.MISSING;
        }
        if (expectedSize > 0 && file.length() != expectedSize) {
            return Reason.SIZE;
        }
        if (verifySha1 && expectedSha1 != null && !expectedSha1.isBlank()) {
            String actual = hashCache.computeIfAbsent(file.getAbsolutePath(), k -> DownloadManager.sha1(file));
            if (!expectedSha1.equalsIgnoreCase(actual)) {
                return Reason.SHA1;
            }
        }
        return null;
    }

    private static JsonObject readRootJson(File gameDir, String rootId) {
        File json = new File(new File(new File(gameDir, "versions"), rootId), rootId + ".json");
        if (!json.isFile()) {
            return null;
        }
        try {
            return JsonParser.parseString(Files.readString(json.toPath(), StandardCharsets.UTF_8))
                    .getAsJsonObject();
        } catch (Exception e) {
            return null;
        }
    }

    /** 下载缺失/损坏文件，返回成功补齐的数量（失败会用备用源重试一次，补完按大小 + SHA1 复核）。 */
    public static int repair(File gameDir, List<Issue> issues) {
        if (issues == null || issues.isEmpty()) {
            return 0;
        }
        List<DownloadManager.Task> tasks = new ArrayList<>();
        for (Issue issue : issues) {
            tasks.add(new DownloadManager.Task(issue.url(), target(gameDir, issue), issue.sha1(), issue.size()));
        }
        DownloadManager.downloadAll(tasks, 6, null);

        // 失败的重试一次：换成更可能命中的源
        for (Issue issue : issues) {
            if (isHealed(target(gameDir, issue), issue, true)) {
                continue;
            }
            String fallback = fallbackUrl(issue);
            if (fallback == null || fallback.equals(issue.url())) {
                continue;
            }
            DownloadManager.downloadAll(
                    List.of(new DownloadManager.Task(fallback, target(gameDir, issue), issue.sha1(), issue.size())),
                    1, null);
        }

        int fixed = 0;
        for (Issue issue : issues) {
            if (isHealed(target(gameDir, issue), issue, true)) {
                fixed++;
            }
        }
        return fixed;
    }

    private static boolean isHealed(File file, Issue issue, boolean verifySha1) {
        return verify(file, issue.sha1(), issue.size(), verifySha1, new HashMap<>()) == null;
    }

    private static int sampleAssets(File gameDir, String instanceId, File indexFile, List<Issue> issues,
                                    int limit, boolean verifySha1, Map<String, String> hashCache) {
        int checked = 0;
        try {
            JsonObject root = JsonParser.parseString(
                    Files.readString(indexFile.toPath(), StandardCharsets.UTF_8)).getAsJsonObject();
            if (!root.has("objects") || !root.get("objects").isJsonObject()) {
                return 0;
            }
            JsonObject objects = root.getAsJsonObject("objects");
            // 自适应：索引越大抽样越多，但不超过配置上限，至少 5 个
            int effective = Math.min(limit, Math.max(5, objects.size() / 200));
            for (var entry : objects.entrySet()) {
                if (checked >= effective) {
                    break;
                }
                if (!entry.getValue().isJsonObject()) {
                    continue;
                }
                JsonObject object = entry.getValue().getAsJsonObject();
                if (!object.has("hash")) {
                    continue;
                }
                String hash = object.get("hash").getAsString();
                if (hash.length() < 2) {
                    continue;
                }
                checked++;
                String prefix = hash.substring(0, 2);
                File file = new File(new File(new File(gameDir, "assets"), "objects"),
                        prefix + File.separator + hash);
                long size = object.has("size") ? object.get("size").getAsLong() : -1;
                Reason reason = verify(file, hash, size, verifySha1, hashCache);
                if (reason != null) {
                    issues.add(new Issue(instanceId, prefix + "/" + hash,
                            "https://resources.download.minecraft.net/" + prefix + "/" + hash,
                            hash, size, Kind.ASSET_OBJECT, reason));
                }
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
