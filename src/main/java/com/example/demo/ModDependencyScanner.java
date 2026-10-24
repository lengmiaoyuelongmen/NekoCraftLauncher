package com.example.demo;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * 读取 mods 目录里每个 jar 的元数据（Fabric / Quilt / Forge / NeoForge），
 * 用来做「前置依赖检查」和「缺前置一键补齐」。
 *
 * <p>只读 jar 里的描述文件，不加载任何模组代码：</p>
 * <ul>
 *     <li>Fabric / Quilt：{@code fabric.mod.json} / {@code quilt.mod.json}</li>
 *     <li>Forge：{@code META-INF/mods.toml}</li>
 *     <li>NeoForge：{@code META-INF/neoforge.mods.toml}</li>
 * </ul>
 */
public final class ModDependencyScanner {

    /** 一条前置依赖。versionRange 只用于展示，不参与解析。 */
    public record Dep(String modId, String versionRange, boolean mandatory) {
    }

    /** 一个模组 jar 的元数据；解析不出来时 modId 为 null（不影响列表展示）。 */
    public record ModInfo(File file, String modId, String name, String version, String loader,
                          List<Dep> deps, List<String> embeddedModIds) {

        public boolean isEnabled() {
            return !file.getName().toLowerCase(Locale.ROOT).endsWith(".disabled");
        }
    }

    /** 游戏 / 加载器本身不是「前置模组」，不参与缺失检查。 */
    private static final Set<String> PLATFORM_IDS = Set.of(
            "minecraft", "java", "forge", "neoforge", "fabricloader", "fabric_loader",
            "quilt_loader", "quilt", "liteloader", "optifine", "fabric-language-kotlin",
            "kotlin", "kotlin-stdlib");

    /** 同一个前置在不同加载器里 id 不一样，检查时归一化。 */
    private static final Map<String, String> ALIASES = Map.of(
            "cloth-config2", "cloth-config",
            "cloth_config2", "cloth-config",
            "architectury", "architectury-api",
            "geckolib3", "geckolib",
            "fabric_api", "fabric-api");

    private ModDependencyScanner() {
    }

    /**
     * 扫描整个 mods 目录（含 .disabled），解析失败的文件也会返回，只是 modId 为 null。
     */
    public static List<ModInfo> scan(File modsDir) {
        File[] files = modsDir == null ? null : modsDir.listFiles((dir, name) -> {
            String lower = name.toLowerCase(Locale.ROOT);
            return lower.endsWith(".jar") || lower.endsWith(".jar.disabled");
        });
        if (files == null || files.length == 0) return List.of();
        List<ModInfo> result = new ArrayList<>(files.length);
        for (File file : files) {
            try {
                result.add(scanJar(file));
            } catch (Exception e) {
                result.add(new ModInfo(file, null, baseName(file.getName()), "", "未知", List.of(), List.of()));
            }
        }
        result.sort(Comparator.comparing(info -> info.file().getName()));
        return result;
    }

    /** 读取单个 jar 的元数据（含 jar-in-jar 内嵌的前置，例如 Forge 的 JarJar）。 */
    public static ModInfo scanJar(File file) throws IOException {
        try (ZipFile zip = new ZipFile(file)) {
            ModInfo info = parseFromZip(file, zip);
            List<String> embedded = readEmbeddedModIds(zip);
            return new ModInfo(file, info.modId(), info.name(), info.version(), info.loader(),
                    info.deps(), embedded);
        }
    }

    private static ModInfo parseFromZip(File file, ZipFile zip) throws IOException {
        String fabric = read(zip, "fabric.mod.json");
        if (fabric != null) return parseFabric(file, fabric);

        String quilt = read(zip, "quilt.mod.json");
        if (quilt != null) return parseQuilt(file, quilt);

        String neoForge = read(zip, "META-INF/neoforge.mods.toml");
        if (neoForge != null) return parseForgeToml(file, neoForge, "NeoForge");

        String forge = read(zip, "META-INF/mods.toml");
        if (forge != null) return parseForgeToml(file, forge, "Forge");

        return new ModInfo(file, null, baseName(file.getName()), "", "未知", List.of(), List.of());
    }

    /**
     * 计算缺失的必装前置。
     *
     * @return 缺失的 modId -> 需要它的模组显示名（去重、保持顺序）
     */
    public static Map<String, List<String>> findMissingDependencies(List<ModInfo> mods) {
        Set<String> installed = installedIds(mods);

        Map<String, List<String>> missing = new LinkedHashMap<>();
        for (ModInfo info : mods) {
            if (!info.isEnabled()) continue;
            for (Dep dep : info.deps()) {
                if (!dep.mandatory()) continue;
                String id = normalize(dep.modId());
                if (id.isBlank() || PLATFORM_IDS.contains(id)) continue;
                String canonical = ALIASES.getOrDefault(id, id);
                if (installed.contains(id) || installed.contains(canonical)) continue;
                List<String> requesters = missing.computeIfAbsent(canonical, key -> new ArrayList<>());
                String display = displayName(info);
                if (!requesters.contains(display)) requesters.add(display);
            }
        }
        return missing;
    }

    /** 某个 jar 依赖了、但目录里没有的前置 id。 */
    public static List<String> missingFor(ModInfo info, List<ModInfo> all) {
        Set<String> installed = installedIds(all);
        List<String> result = new ArrayList<>();
        for (Dep dep : info.deps()) {
            if (!dep.mandatory()) continue;
            String id = normalize(dep.modId());
            if (id.isBlank() || PLATFORM_IDS.contains(id)) continue;
            String canonical = ALIASES.getOrDefault(id, id);
            if (installed.contains(id) || installed.contains(canonical)) continue;
            if (!result.contains(canonical)) result.add(canonical);
        }
        return result;
    }

    public static String displayName(ModInfo info) {
        if (info.name() != null && !info.name().isBlank()) return info.name();
        if (info.modId() != null && !info.modId().isBlank()) return info.modId();
        return baseName(info.file().getName());
    }

    /** 已安装的 modId 集合（含 jar 内嵌的模组）。 */
    private static Set<String> installedIds(List<ModInfo> mods) {
        Set<String> installed = new LinkedHashSet<>();
        for (ModInfo info : mods) {
            if (info.modId() != null && !info.modId().isBlank()) installed.add(normalize(info.modId()));
            for (String embedded : info.embeddedModIds()) {
                if (embedded != null && !embedded.isBlank()) installed.add(normalize(embedded));
            }
        }
        return installed;
    }

    // ================== 各加载器的解析 ==================

    static ModInfo parseFabric(File file, String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        String id = str(root, "id");
        String name = str(root, "name");
        String version = str(root, "version");
        List<Dep> deps = parseDepContainer(root.get("depends"));
        return new ModInfo(file, id, name.isBlank() ? id : name, version, "Fabric", deps, List.of());
    }

    static ModInfo parseQuilt(File file, String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonObject loader = root.has("quilt_loader") && root.get("quilt_loader").isJsonObject()
                ? root.getAsJsonObject("quilt_loader") : root;
        String id = str(loader, "id");
        String version = str(loader, "version");
        String name = "";
        if (loader.has("metadata") && loader.get("metadata").isJsonObject()) {
            name = str(loader.getAsJsonObject("metadata"), "name");
        }
        List<Dep> deps = parseDepContainer(loader.get("depends"));
        return new ModInfo(file, id, name.isBlank() ? id : name, version, "Quilt", deps, List.of());
    }

    /**
     * Forge / NeoForge 的 mods.toml。只取第一个 [[mods]] 块和全部 [[dependencies.*]] 块，
     * 够用来判断前置；完整 TOML 语法（多行数组、转义）这里不需要。
     */
    static ModInfo parseForgeToml(File file, String toml, String loaderName) {
        String modId = "";
        String displayName = "";
        String version = "";
        List<Dep> deps = new ArrayList<>();

        boolean inFirstModsBlock = false;
        boolean modsBlockSeen = false;
        String depBlockOwner = null;
        String depModId = "";
        String depRange = "*";
        boolean depMandatory = true;

        for (String rawLine : toml.split("\\R")) {
            String line = stripComment(rawLine).trim();
            if (line.isEmpty()) continue;

            if (line.startsWith("[[dependencies.") && line.endsWith("]]")) {
                // 上一个依赖块收尾
                if (depBlockOwner != null && !depModId.isBlank()) {
                    deps.add(new Dep(depModId, depRange, depMandatory));
                }
                depBlockOwner = line.substring("[[dependencies.".length(), line.length() - 2).trim();
                depModId = "";
                depRange = "*";
                depMandatory = true;
                inFirstModsBlock = false;
                continue;
            }
            if (line.startsWith("[[")) {
                if (depBlockOwner != null && !depModId.isBlank()) {
                    deps.add(new Dep(depModId, depRange, depMandatory));
                }
                depBlockOwner = null;
                depModId = "";
                boolean isModsBlock = line.startsWith("[[mods]]");
                inFirstModsBlock = isModsBlock && !modsBlockSeen;
                if (isModsBlock) modsBlockSeen = true;
                continue;
            }
            if (line.startsWith("[")) {
                if (depBlockOwner != null && !depModId.isBlank()) {
                    deps.add(new Dep(depModId, depRange, depMandatory));
                }
                depBlockOwner = null;
                depModId = "";
                inFirstModsBlock = false;
                continue;
            }

            int eq = line.indexOf('=');
            if (eq <= 0) continue;
            String key = line.substring(0, eq).trim();
            String value = unquote(line.substring(eq + 1).trim());

            if (inFirstModsBlock) {
                switch (key) {
                    case "modId" -> { if (modId.isBlank()) modId = value; }
                    case "displayName" -> { if (displayName.isBlank()) displayName = value; }
                    case "version" -> { if (version.isBlank()) version = value; }
                    default -> { }
                }
            } else if (depBlockOwner != null) {
                switch (key) {
                    case "modId" -> depModId = value;
                    case "versionRange" -> depRange = value.isBlank() ? "*" : value;
                    case "mandatory" -> depMandatory = !"false".equalsIgnoreCase(value);
                    default -> { }
                }
            }
        }
        if (depBlockOwner != null && !depModId.isBlank()) {
            deps.add(new Dep(depModId, depRange, depMandatory));
        }

        return new ModInfo(file, modId, displayName.isBlank() ? modId : displayName, version, loaderName, deps,
                List.of());
    }

    // ================== jar-in-jar（内嵌前置） ==================

    /**
     * 读取 jar 里内嵌的模组 id（Forge JarJar 的 META-INF/jarjar/*.jar、
     * Fabric 的 META-INF/jars/*.jar）。内嵌前置已经随主模组装好，不能再报缺失。
     */
    private static List<String> readEmbeddedModIds(ZipFile zip) {
        List<String> ids = new ArrayList<>();
        java.util.Enumeration<? extends ZipEntry> entries = zip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (entry.isDirectory()) continue;
            String name = entry.getName();
            if (!name.endsWith(".jar")) continue;
            if (!name.startsWith("META-INF/jars/") && !name.startsWith("META-INF/jarjar/")) continue;
            String id = readNestedModId(zip, entry);
            if (id != null && !id.isBlank() && !ids.contains(id)) ids.add(id);
        }
        return ids;
    }

    private static String readNestedModId(ZipFile zip, ZipEntry entry) {
        try (InputStream in = zip.getInputStream(entry);
             ZipInputStream nested = new ZipInputStream(in)) {
            ZipEntry inner;
            while ((inner = nested.getNextEntry()) != null) {
                if (inner.isDirectory()) continue;
                switch (inner.getName()) {
                    case "fabric.mod.json" -> {
                        String json = new String(nested.readAllBytes(), StandardCharsets.UTF_8);
                        return str(JsonParser.parseString(json).getAsJsonObject(), "id");
                    }
                    case "quilt.mod.json" -> {
                        String json = new String(nested.readAllBytes(), StandardCharsets.UTF_8);
                        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
                        JsonObject loader = root.has("quilt_loader") && root.get("quilt_loader").isJsonObject()
                                ? root.getAsJsonObject("quilt_loader") : root;
                        return str(loader, "id");
                    }
                    case "META-INF/mods.toml", "META-INF/neoforge.mods.toml" -> {
                        String toml = new String(nested.readAllBytes(), StandardCharsets.UTF_8);
                        return firstForgeModId(toml);
                    }
                    default -> { }
                }
            }
        } catch (Exception ignored) {
            // 内嵌 jar 损坏不影响主模组扫描
        }
        return null;
    }

    private static String firstForgeModId(String toml) {
        for (String rawLine : toml.split("\\R")) {
            String line = stripComment(rawLine).trim();
            if (line.startsWith("modId")) {
                int eq = line.indexOf('=');
                if (eq > 0) return unquote(line.substring(eq + 1).trim());
            }
        }
        return "";
    }

    // ================== 工具 ==================

    /** depends 既可能是 {id: range} 对象，也可能是 [{id, versions}] 数组。 */
    private static List<Dep> parseDepContainer(JsonElement element) {
        List<Dep> result = new ArrayList<>();
        if (element == null || element.isJsonNull()) return result;

        if (element.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                result.add(new Dep(entry.getKey(), rangeOf(entry.getValue()), true));
            }
            return result;
        }
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (JsonElement item : array) {
                if (item.isJsonPrimitive()) {
                    result.add(new Dep(item.getAsString(), "*", true));
                } else if (item.isJsonObject()) {
                    JsonObject object = item.getAsJsonObject();
                    String id = str(object, "id");
                    if (id.isBlank()) id = str(object, "modId");
                    boolean optional = object.has("optional") && object.get("optional").isJsonPrimitive()
                            && object.get("optional").getAsBoolean();
                    result.add(new Dep(id, rangeOf(object), !optional));
                }
            }
        }
        return result;
    }

    private static String rangeOf(JsonElement element) {
        if (element == null || element.isJsonNull()) return "*";
        if (element.isJsonPrimitive()) return element.getAsString();
        if (element.isJsonArray()) {
            List<String> parts = new ArrayList<>();
            for (JsonElement item : element.getAsJsonArray()) {
                if (item.isJsonPrimitive()) parts.add(item.getAsString());
            }
            return String.join(" || ", parts);
        }
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            String range = str(object, "versions");
            if (range.isBlank()) range = str(object, "versionRange");
            return range.isBlank() ? "*" : range;
        }
        return "*";
    }

    private static String read(ZipFile zip, String entryName) throws IOException {
        ZipEntry entry = zip.getEntry(entryName);
        if (entry == null || entry.isDirectory()) return null;
        try (InputStream in = zip.getInputStream(entry)) {
            byte[] bytes = in.readAllBytes();
            return bytes.length == 0 ? null : new String(bytes, StandardCharsets.UTF_8);
        }
    }

    private static String str(JsonObject object, String key) {
        if (object == null || !object.has(key)) return "";
        JsonElement element = object.get(key);
        return element.isJsonPrimitive() ? element.getAsString() : "";
    }

    private static String normalize(String id) {
        return id == null ? "" : id.trim().toLowerCase(Locale.ROOT).replace(' ', '-');
    }

    private static String baseName(String fileName) {
        String name = fileName;
        if (name.toLowerCase(Locale.ROOT).endsWith(".disabled")) {
            name = name.substring(0, name.length() - ".disabled".length());
        }
        if (name.toLowerCase(Locale.ROOT).endsWith(".jar")) {
            name = name.substring(0, name.length() - ".jar".length());
        }
        return name;
    }

    /** 去掉 TOML 行尾注释（引号里的 # 不算）。 */
    private static String stripComment(String line) {
        boolean inSingle = false;
        boolean inDouble = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\'' && !inDouble) inSingle = !inSingle;
            else if (c == '"' && !inSingle) inDouble = !inDouble;
            else if (c == '#' && !inSingle && !inDouble) return line.substring(0, i);
        }
        return line;
    }

    private static String unquote(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        if (value.length() >= 2 && value.startsWith("'") && value.endsWith("'")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
}
