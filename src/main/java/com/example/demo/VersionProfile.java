package com.example.demo;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 一个版本档案：读 versions/&lt;id&gt;/&lt;id&gt;.json，并按 inheritsFrom 与父版本合并。
 * 安装 Fabric/Forge 等加载器后，它们生成的就是这种「继承原版的子档案」。
 */
public final class VersionProfile {

    private final JsonObject json;
    private final String rootId;

    private VersionProfile(JsonObject json, String rootId) {
        this.json = json;
        this.rootId = rootId;
    }

    public static File jsonFile(File gameDir, String id) {
        return new File(new File(new File(gameDir, "versions"), id), id + ".json");
    }

    public static boolean isInstalled(File gameDir, String id) {
        return jsonFile(gameDir, id).isFile();
    }

    /** 已安装的版本 id（versions 目录下同时存在 .json 的）。 */
    public static List<String> installedIds(File gameDir) {
        List<String> ids = new ArrayList<>();
        File dir = new File(gameDir, "versions");
        File[] subs = dir.listFiles(File::isDirectory);
        if (subs == null) return ids;
        for (File sub : subs) {
            if (new File(sub, sub.getName() + ".json").isFile()) ids.add(sub.getName());
        }
        ids.sort(String.CASE_INSENSITIVE_ORDER.reversed());
        return ids;
    }

    public static VersionProfile load(File gameDir, String id) throws IOException {
        File file = jsonFile(gameDir, id);
        if (!file.isFile()) throw new IOException("版本 JSON 不存在: " + file.getAbsolutePath());
        JsonObject obj = JsonParser.parseString(Files.readString(file.toPath(), StandardCharsets.UTF_8)).getAsJsonObject();
        JsonObject merged = resolve(gameDir, id, obj, new LinkedHashSet<>());
        return new VersionProfile(merged, findRootId(gameDir, id));
    }

    /** 沿 inheritsFrom 走到最底层的原版版本。 */
    private static String findRootId(File gameDir, String id) throws IOException {
        String current = id;
        Set<String> seen = new LinkedHashSet<>();
        while (seen.add(current)) {
            File file = jsonFile(gameDir, current);
            if (!file.isFile()) return current;
            JsonObject obj = JsonParser.parseString(
                    Files.readString(file.toPath(), StandardCharsets.UTF_8)).getAsJsonObject();
            if (!obj.has("inheritsFrom")) return current;
            current = obj.get("inheritsFrom").getAsString();
        }
        return current;
    }

    private static JsonObject resolve(File gameDir, String id, JsonObject child, Set<String> chain) throws IOException {
        if (!chain.add(id)) throw new IOException("版本继承出现循环: " + chain);
        if (!child.has("inheritsFrom")) return child;

        String parentId = child.get("inheritsFrom").getAsString();
        File parentFile = jsonFile(gameDir, parentId);
        if (!parentFile.isFile()) {
            throw new IOException("缺少父版本 " + parentId + "（" + parentFile.getAbsolutePath() + "）");
        }
        JsonObject parent = JsonParser.parseString(
                Files.readString(parentFile.toPath(), StandardCharsets.UTF_8)).getAsJsonObject();
        JsonObject resolvedParent = resolve(gameDir, parentId, parent, chain);
        return merge(resolvedParent, child, id);
    }

    private static JsonObject merge(JsonObject parent, JsonObject child, String childId) {
        JsonObject out = parent.deepCopy();
        out.addProperty("id", childId);

        for (String key : new String[]{"mainClass", "assetIndex", "downloads", "javaVersion",
                "type", "releaseTime", "time", "minimumLauncherVersion", "logging"}) {
            if (child.has(key) && !child.get(key).isJsonNull()) out.add(key, child.get(key).deepCopy());
        }

        // libraries：父在前、子在后；同 name 以子为准
        Map<String, JsonObject> libs = new LinkedHashMap<>();
        collectLibraries(parent, libs);
        collectLibraries(child, libs);
        JsonArray merged = new JsonArray();
        for (JsonObject lib : libs.values()) merged.add(lib);
        out.add("libraries", merged);

        // arguments：game / jvm 分别拼接
        JsonObject args = new JsonObject();
        for (String kind : new String[]{"game", "jvm"}) {
            JsonArray arr = new JsonArray();
            JsonObject pa = parent.has("arguments") && parent.get("arguments").isJsonObject()
                    ? parent.getAsJsonObject("arguments") : null;
            JsonObject ca = child.has("arguments") && child.get("arguments").isJsonObject()
                    ? child.getAsJsonObject("arguments") : null;
            if (pa != null && pa.has(kind) && pa.get(kind).isJsonArray()) arr.addAll(pa.getAsJsonArray(kind));
            if (ca != null && ca.has(kind) && ca.get(kind).isJsonArray()) arr.addAll(ca.getAsJsonArray(kind));
            if (arr.size() > 0) args.add(kind, arr);
        }
        if (args.size() > 0) out.add("arguments", args);

        if (child.has("minecraftArguments")) out.add("minecraftArguments", child.get("minecraftArguments"));
        return out;
    }

    private static void collectLibraries(JsonObject version, Map<String, JsonObject> target) {
        if (!version.has("libraries") || !version.get("libraries").isJsonArray()) return;
        for (JsonElement el : version.getAsJsonArray("libraries")) {
            if (!el.isJsonObject()) continue;
            JsonObject lib = el.getAsJsonObject();
            String name = lib.has("name") ? lib.get("name").getAsString() : ("__" + target.size());
            target.put(name, lib);
        }
    }

    // ==================== 读取 ====================

    public String getId() {
        return json.has("id") ? json.get("id").getAsString() : "unknown";
    }

    /** 继承链最底层的原版版本 id（客户端 jar 和资源都在它下面）。 */
    public String getRootId() {
        return rootId;
    }

    public String getMainClass() {
        return json.has("mainClass") ? json.get("mainClass").getAsString() : null;
    }

    public String getType() {
        return json.has("type") ? json.get("type").getAsString() : "release";
    }

    /** 版本 JSON 声明的 Java 主版本（26.x 是 25，1.20.4 是 17）；缺省按 21 处理。 */
    public int getJavaMajorVersion() {
        try {
            if (json.has("javaVersion") && json.get("javaVersion").isJsonObject()) {
                JsonObject javaVersion = json.getAsJsonObject("javaVersion");
                if (javaVersion.has("majorVersion")) return javaVersion.get("majorVersion").getAsInt();
            }
        } catch (Exception ignored) {
        }
        // 旧版本 JSON 没有 javaVersion：按原版版本号推断，避免误用 Java 21
        java.util.regex.Matcher matcher = Pattern.compile("^1\\.(\\d+)").matcher(rootId);
        if (matcher.find()) {
            int minor = Integer.parseInt(matcher.group(1));
            if (minor >= 18) return 17;
            if (minor == 17) return 16;
            return 8;
        }
        return 21;
    }

    public String getAssetIndexId() {
        if (!json.has("assetIndex") || !json.get("assetIndex").isJsonObject()) return "legacy";
        JsonObject idx = json.getAsJsonObject("assetIndex");
        return idx.has("id") ? idx.get("id").getAsString() : "legacy";
    }

    public String getAssetIndexUrl() {
        if (!json.has("assetIndex") || !json.get("assetIndex").isJsonObject()) return null;
        JsonObject idx = json.getAsJsonObject("assetIndex");
        return idx.has("url") ? idx.get("url").getAsString() : null;
    }

    public String getClientUrl() {
        try {
            return json.getAsJsonObject("downloads").getAsJsonObject("client").get("url").getAsString();
        } catch (Exception e) {
            return null;
        }
    }

    public String getClientSha1() {
        try {
            return json.getAsJsonObject("downloads").getAsJsonObject("client").get("sha1").getAsString();
        } catch (Exception e) {
            return null;
        }
    }

    public long getClientSize() {
        try {
            return json.getAsJsonObject("downloads").getAsJsonObject("client").get("size").getAsLong();
        } catch (Exception e) {
            return -1;
        }
    }

    /** 需要放进 classpath 的库（已按 rules 过滤，排除纯 natives 库）。 */
    public List<JsonObject> classpathLibraries() {
        List<JsonObject> result = new ArrayList<>();
        if (!json.has("libraries")) return result;
        for (JsonElement el : json.getAsJsonArray("libraries")) {
            if (!el.isJsonObject()) continue;
            JsonObject lib = el.getAsJsonObject();
            if (!allowed(lib)) continue;
            if (libraryPath(lib) == null) continue;
            result.add(lib);
        }
        return result;
    }

    /** 需要解压 dll 的库（含 natives 信息）。 */
    public List<JsonObject> nativeLibraries() {
        List<JsonObject> result = new ArrayList<>();
        if (!json.has("libraries")) return result;
        for (JsonElement el : json.getAsJsonArray("libraries")) {
            if (!el.isJsonObject()) continue;
            JsonObject lib = el.getAsJsonObject();
            if (!allowed(lib)) continue;
            if (lib.has("natives") || (libraryPath(lib) != null && libraryPath(lib).endsWith("-natives-windows.jar"))) {
                result.add(lib);
            }
        }
        return result;
    }

    /** 库文件相对路径，取不到返回 null（例如只有 classifiers 的旧式 natives 条目）。 */
    public static String libraryPath(JsonObject lib) {
        try {
            JsonObject downloads = lib.getAsJsonObject("downloads");
            if (downloads != null && downloads.has("artifact") && downloads.get("artifact").isJsonObject()) {
                JsonObject artifact = downloads.getAsJsonObject("artifact");
                if (artifact.has("path")) return artifact.get("path").getAsString();
            }
        } catch (Exception ignored) {
        }
        return nameToPath(lib.has("name") ? lib.get("name").getAsString() : null);
    }

    public static String libraryUrl(JsonObject lib) {
        try {
            JsonObject downloads = lib.getAsJsonObject("downloads");
            if (downloads != null && downloads.has("artifact") && downloads.get("artifact").isJsonObject()) {
                JsonObject artifact = downloads.getAsJsonObject("artifact");
                if (artifact.has("url")) return artifact.get("url").getAsString();
            }
        } catch (Exception ignored) {
        }
        String path = libraryPath(lib);
        return path == null ? null : "https://libraries.minecraft.net/" + path;
    }

    /** 库文件的 SHA1（版本 JSON 里有就用来校验）。 */
    public static String librarySha1(JsonObject lib) {
        try {
            JsonObject downloads = lib.getAsJsonObject("downloads");
            if (downloads != null && downloads.has("artifact") && downloads.get("artifact").isJsonObject()) {
                JsonObject artifact = downloads.getAsJsonObject("artifact");
                if (artifact.has("sha1")) return artifact.get("sha1").getAsString();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public static long librarySize(JsonObject lib) {
        try {
            JsonObject downloads = lib.getAsJsonObject("downloads");
            if (downloads != null && downloads.has("artifact") && downloads.get("artifact").isJsonObject()) {
                JsonObject artifact = downloads.getAsJsonObject("artifact");
                if (artifact.has("size")) return artifact.get("size").getAsLong();
            }
        } catch (Exception ignored) {
        }
        return -1;
    }

    private static String nameToPath(String name) {
        if (name == null) return null;
        String[] parts = name.split(":");
        if (parts.length < 3) return null;
        String group = parts[0].replace('.', '/');
        String artifact = parts[1];
        String version = parts[2];
        String classifier = parts.length > 3 ? "-" + parts[3] : "";
        return group + "/" + artifact + "/" + version + "/" + artifact + "-" + version + classifier + ".jar";
    }

    // ==================== 启动参数 ====================

    public JsonArray gameArguments() {
        return arguments("game");
    }

    public JsonArray jvmArguments() {
        return arguments("jvm");
    }

    public String legacyMinecraftArguments() {
        return json.has("minecraftArguments") ? json.get("minecraftArguments").getAsString() : null;
    }

    private JsonArray arguments(String kind) {
        if (!json.has("arguments") || !json.get("arguments").isJsonObject()) return null;
        JsonObject args = json.getAsJsonObject("arguments");
        return args.has(kind) && args.get(kind).isJsonArray() ? args.getAsJsonArray(kind) : null;
    }

    // ==================== rules ====================

    public static boolean allowed(JsonObject element) {
        return allowed(element, java.util.Collections.emptyMap());
    }

    /** features 用于 is_demo_user / has_custom_resolution 这类条件参数。 */
    public static boolean allowed(JsonObject element, Map<String, Boolean> features) {
        if (!element.has("rules") || !element.get("rules").isJsonArray()) return true;
        JsonArray rules = element.getAsJsonArray("rules");
        boolean hasAllowRule = false;
        boolean allowed = false;
        for (JsonElement el : rules) {
            if (!el.isJsonObject()) continue;
            JsonObject rule = el.getAsJsonObject();
            String action = rule.has("action") ? rule.get("action").getAsString() : "allow";
            boolean matches = matches(rule, features);
            if ("allow".equals(action)) {
                hasAllowRule = true;
                if (matches) allowed = true;
            } else if ("disallow".equals(action) && matches) {
                allowed = false;
            }
        }
        return hasAllowRule ? allowed : true;
    }

    private static boolean matches(JsonObject rule, Map<String, Boolean> features) {
        if (rule.has("os") && rule.get("os").isJsonObject()) {
            JsonObject os = rule.getAsJsonObject("os");
            if (os.has("name") && !os.get("name").getAsString().equalsIgnoreCase(Platform.OS_NAME)) return false;
            if (os.has("arch") && !os.get("arch").getAsString().equalsIgnoreCase(Platform.OS_ARCH)) return false;
            if (os.has("version")) {
                try {
                    if (!Pattern.compile(os.get("version").getAsString()).matcher(Platform.OS_VERSION).find()) return false;
                } catch (Exception ignored) {
                }
            }
        }
        if (rule.has("features") && rule.get("features").isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : rule.getAsJsonObject("features").entrySet()) {
                boolean required = entry.getValue().isJsonPrimitive() && entry.getValue().getAsBoolean();
                boolean actual = features != null && Boolean.TRUE.equals(features.get(entry.getKey()));
                if (required != actual) return false;
            }
        }
        return true;
    }

    /** 当前平台信息（Minecraft 的命名方式）。 */
    static final class Platform {
        static final String OS_NAME;
        static final String OS_ARCH;
        static final String OS_VERSION = System.getProperty("os.version", "");

        static {
            String os = System.getProperty("os.name", "").toLowerCase();
            if (os.contains("win")) OS_NAME = "windows";
            else if (os.contains("mac")) OS_NAME = "osx";
            else OS_NAME = "linux";

            String arch = System.getProperty("os.arch", "").toLowerCase();
            if (arch.equals("amd64") || arch.equals("x86_64")) OS_ARCH = "x86_64";
            else if (arch.equals("aarch64") || arch.equals("arm64")) OS_ARCH = "arm64";
            else OS_ARCH = arch;
        }
    }
}
