package com.example.demo;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * 整合包导入：支持 Modrinth (.mrpack)、CurseForge (manifest.json)、
 * PCL-CE 导出（PCL/Setup.ini，可能带 PCL/Game/）和 HMCL (mmc-pack.json)。
 * 只解析元数据 + 解包覆盖文件，游戏/加载器安装由调用方完成。
 */
public final class ModpackImport {

    public record Pack(String name, String gameVersion, String loader, String loaderVersion,
                       int declaredFiles, String format) {
        public boolean hasLoader() {
            return loader != null && !loader.isBlank() && !"vanilla".equals(loader);
        }
    }

    private ModpackImport() {
    }

    public static Pack inspect(File archive) throws Exception {
        try (ZipFile zip = new ZipFile(archive)) {
            ZipEntry index = zip.getEntry("modrinth.index.json");
            if (index != null) {
                JsonObject json = JsonParser.parseString(new String(
                        zip.getInputStream(index).readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
                String name = json.has("name") ? json.get("name").getAsString()
                        : stripExt(archive.getName());
                String game = "";
                String loader = "vanilla";
                String loaderVersion = "";
                if (json.has("dependencies") && json.get("dependencies").isJsonObject()) {
                    JsonObject deps = json.getAsJsonObject("dependencies");
                    if (deps.has("minecraft")) game = deps.get("minecraft").getAsString();
                    for (String key : new String[]{"forge", "neoforge", "fabric", "quilt"}) {
                        if (deps.has(key)) {
                            loader = key;
                            loaderVersion = deps.get(key).getAsString();
                            break;
                        }
                    }
                }
                int files = json.has("files") && json.get("files").isJsonArray()
                        ? json.getAsJsonArray("files").size() : 0;
                return new Pack(name, game, loader, loaderVersion, files, "Modrinth");
            }

            ZipEntry manifest = zip.getEntry("manifest.json");
            if (manifest != null) {
                JsonObject json = JsonParser.parseString(new String(
                        zip.getInputStream(manifest).readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
                String name = json.has("name") ? json.get("name").getAsString() : stripExt(archive.getName());
                String game = "";
                if (json.has("minecraft") && json.getAsJsonObject("minecraft").has("version")) {
                    game = json.getAsJsonObject("minecraft").get("version").getAsString();
                }
                String loader = "vanilla";
                String loaderVersion = "";
                if (json.has("minecraft") && json.getAsJsonObject("minecraft").has("modLoaders")) {
                    JsonArray loaders = json.getAsJsonObject("minecraft").getAsJsonArray("modLoaders");
                    for (JsonElement element : loaders) {
                        JsonObject obj = element.getAsJsonObject();
                        if (obj.has("primary") && obj.get("primary").getAsBoolean() && obj.has("id")) {
                            String id = obj.get("id").getAsString();
                            int dash = id.indexOf('-');
                            if (dash > 0) {
                                loader = id.substring(0, dash).toLowerCase(Locale.ROOT);
                                loaderVersion = id.substring(dash + 1);
                            }
                            break;
                        }
                    }
                }
                int files = json.has("files") && json.get("files").isJsonArray()
                        ? json.getAsJsonArray("files").size() : 0;
                return new Pack(name, game, loader, loaderVersion, files, "CurseForge");
            }

            ZipEntry setup = zip.getEntry("PCL/Setup.ini");
            if (setup != null) {
                String text = new String(zip.getInputStream(setup).readAllBytes(), StandardCharsets.UTF_8);
                String game = value(text, "VersionVanillaName");
                String loader = "vanilla";
                String loaderVersion = "";
                String[][] loaders = {
                        {"forge", "VersionForge"}, {"neoforge", "VersionNeoForge"},
                        {"fabric", "VersionFabric"}, {"quilt", "VersionQuilt"}
                };
                for (String[] candidate : loaders) {
                    String value = value(text, candidate[1]);
                    if (value != null && !value.isBlank() && !"False".equalsIgnoreCase(value)) {
                        loader = candidate[0];
                        loaderVersion = value;
                        break;
                    }
                }
                String name = value(text, "Info");
                if (name == null || name.isBlank()) name = stripExt(archive.getName());
                return new Pack(name, game, loader, loaderVersion, 0, "PCL-CE");
            }

            ZipEntry mmc = zip.getEntry("mmc-pack.json");
            if (mmc != null) {
                JsonObject json = JsonParser.parseString(new String(
                        zip.getInputStream(mmc).readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
                String game = "";
                String loader = "vanilla";
                String loaderVersion = "";
                if (json.has("components") && json.get("components").isJsonArray()) {
                    for (JsonElement element : json.getAsJsonArray("components")) {
                        JsonObject obj = element.getAsJsonObject();
                        String uid = obj.has("uid") ? obj.get("uid").getAsString() : "";
                        String version = obj.has("version") ? obj.get("version").getAsString() : "";
                        if ("net.minecraft".equals(uid)) game = version;
                        else if (uid.contains("forge") && !uid.contains("neoforge")) {
                            loader = "forge";
                            loaderVersion = version;
                        } else if (uid.contains("neoforge")) {
                            loader = "neoforge";
                            loaderVersion = version;
                        } else if (uid.contains("fabric")) {
                            loader = "fabric";
                            loaderVersion = version;
                        }
                    }
                }
                String name = stripExt(archive.getName());
                return new Pack(name, game, loader, loaderVersion, 0, "HMCL");
            }

            return new Pack(stripExt(archive.getName()), "", "vanilla", "", 0, "未知");
        }
    }

    /**
     * 解出整合包的游戏文件到 targetDir：
     * overrides/、client-overrides/ 会去掉前缀；PCL/Game/ 会去掉前缀；PCL/ 元数据跳过。
     */
    public static int extract(File archive, File targetDir, Consumer<String> log) throws Exception {
        int count = 0;
        try (ZipFile zip = new ZipFile(archive)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String raw = entry.getName().replace('\\', '/');
                if (entry.isDirectory()) continue;
                String path = mapPath(raw);
                if (path == null || path.isBlank()) continue;
                File out = new File(targetDir, path);
                if (!out.toPath().normalize().startsWith(targetDir.toPath())) continue;
                File parent = out.getParentFile();
                if (parent != null && !parent.exists()) parent.mkdirs();
                try (InputStream in = zip.getInputStream(entry);
                     FileOutputStream output = new FileOutputStream(out)) {
                    in.transferTo(output);
                }
                count++;
            }
        }
        if (log != null) log.accept("解出 " + count + " 个文件到 " + targetDir.getAbsolutePath());
        return count;
    }

    /** 按 Modrinth 索引里的 files 列表补齐实际模组文件。overrides 只包含配置，不含大部分模组。 */
    public static int installIndexedFiles(File archive, File targetDir, Consumer<String> log) throws Exception {
        java.util.List<DownloadManager.Task> tasks = new java.util.ArrayList<>();
        try (ZipFile zip = new ZipFile(archive)) {
            ZipEntry index = zip.getEntry("modrinth.index.json");
            if (index == null) return 0;
            JsonObject json = JsonParser.parseString(new String(
                    zip.getInputStream(index).readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
            if (!json.has("files") || !json.get("files").isJsonArray()) return 0;
            for (JsonElement element : json.getAsJsonArray("files")) {
                if (!element.isJsonObject()) continue;
                JsonObject file = element.getAsJsonObject();
                String path = file.has("path") ? file.get("path").getAsString() : "";
                if (path.isBlank()) continue;
                File out = new File(targetDir, path);
                if (!out.toPath().normalize().startsWith(targetDir.toPath())) continue;
                String url = "";
                if (file.has("downloads") && file.get("downloads").isJsonArray()
                        && !file.getAsJsonArray("downloads").isEmpty()) {
                    url = file.getAsJsonArray("downloads").get(0).getAsString();
                }
                if (url.isBlank()) continue;
                String sha1 = null;
                if (file.has("hashes") && file.get("hashes").isJsonObject()) {
                    JsonObject hashes = file.getAsJsonObject("hashes");
                    if (hashes.has("sha1")) sha1 = hashes.get("sha1").getAsString();
                }
                long size = file.has("fileSize") ? file.get("fileSize").getAsLong() : -1;
                tasks.add(new DownloadManager.Task(url, out, sha1, size));
            }
        }
        if (tasks.isEmpty()) return 0;
        if (log != null) log.accept("正在补齐索引中的 " + tasks.size() + " 个模组文件…");
        DownloadManager.Stats stats = DownloadManager.downloadAll(tasks, 8, null, "auto", 0);
        int installed = tasks.size() - stats.failed;
        if (log != null) log.accept("索引文件完成: " + installed + "/" + tasks.size()
                + (stats.failed > 0 ? "，失败 " + stats.failed : ""));
        return installed;
    }

    private static String mapPath(String raw) {
        if (raw.equals("modrinth.index.json") || raw.equals("manifest.json")
                || raw.equals("modlist.html") || raw.equals("PCL/Setup.ini")
                || raw.equals("mmc-pack.json") || raw.equals("instance.cfg")) {
            return null;
        }
        String path = raw;
        if (path.startsWith("overrides/")) path = path.substring("overrides/".length());
        else if (path.startsWith("client-overrides/")) path = path.substring("client-overrides/".length());
        else if (path.startsWith("PCL/Game/")) path = path.substring("PCL/Game/".length());
        if (path.startsWith("PCL/") || path.startsWith("META-INF/")) return null;
        return path;
    }

    private static String value(String ini, String key) {
        for (String line : ini.split("\\R")) {
            int colon = line.indexOf(':');
            if (colon <= 0) continue;
            if (line.substring(0, colon).trim().equalsIgnoreCase(key)) {
                return line.substring(colon + 1).trim();
            }
        }
        return null;
    }

    private static String stripExt(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }
}
