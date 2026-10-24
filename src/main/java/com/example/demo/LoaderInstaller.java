package com.example.demo;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 模组加载器安装。
 * Fabric / Quilt 走官方 meta 接口直接生成版本档案；Forge / NeoForge 下载官方 installer 并在本地静默执行。
 */
public final class LoaderInstaller {

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private LoaderInstaller() {
    }

    // ==================== Fabric ====================

    public static CompletableFuture<String> installFabric(File gameDir, String gameVersion, Consumer<String> log) {
        return installFabric(gameDir, gameVersion, null, log);
    }

    public static CompletableFuture<String> installFabric(File gameDir, String gameVersion, String requestedLoader,
                                                          Consumer<String> log) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.accept("🧵 查询 Fabric 加载器版本...");
                String listJson = get("https://meta.fabricmc.net/v2/versions/loader/" + gameVersion);
                JsonArray list = JsonParser.parseString(listJson).getAsJsonArray();
                if (list.size() == 0) throw new RuntimeException("Fabric 没有适配 " + gameVersion + " 的加载器");

                String loaderVersion = requestedLoader;
                if (loaderVersion == null || loaderVersion.isBlank()) {
                    loaderVersion = list.get(0).getAsJsonObject()
                            .getAsJsonObject("loader").get("version").getAsString();
                }
                log.accept("   使用 Fabric Loader " + loaderVersion);

                String profileJson = get("https://meta.fabricmc.net/v2/versions/loader/"
                        + gameVersion + "/" + loaderVersion + "/profile/json");
                JsonObject profile = JsonParser.parseString(profileJson).getAsJsonObject();
                String id = profile.get("id").getAsString();

                saveProfile(gameDir, id, profile);
                log.accept("✅ Fabric 安装完成: " + id);
                return id;
            } catch (Exception e) {
                throw new RuntimeException("Fabric 安装失败: " + e.getMessage(), e);
            }
        });
    }

    // ==================== Quilt ====================

    public static CompletableFuture<String> installQuilt(File gameDir, String gameVersion, Consumer<String> log) {
        return installQuilt(gameDir, gameVersion, null, log);
    }

    public static CompletableFuture<String> installQuilt(File gameDir, String gameVersion, String requestedLoader,
                                                          Consumer<String> log) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.accept("🧶 查询 Quilt 加载器版本...");
                String listJson = get("https://meta.quiltmc.org/v3/versions/loader/" + gameVersion);
                JsonArray list = JsonParser.parseString(listJson).getAsJsonArray();
                if (list.size() == 0) throw new RuntimeException("Quilt 没有适配 " + gameVersion + " 的加载器");

                String loaderVersion = requestedLoader;
                if (loaderVersion == null || loaderVersion.isBlank()) {
                    loaderVersion = list.get(0).getAsJsonObject()
                            .getAsJsonObject("loader").get("version").getAsString();
                }
                log.accept("   使用 Quilt Loader " + loaderVersion);

                String profileJson = get("https://meta.quiltmc.org/v3/versions/loader/"
                        + gameVersion + "/" + loaderVersion + "/profile/json");
                JsonObject profile = JsonParser.parseString(profileJson).getAsJsonObject();
                String id = profile.get("id").getAsString();

                saveProfile(gameDir, id, profile);
                log.accept("✅ Quilt 安装完成: " + id);
                return id;
            } catch (Exception e) {
                throw new RuntimeException("Quilt 安装失败: " + e.getMessage(), e);
            }
        });
    }

    // ==================== Forge ====================

    public static CompletableFuture<String> installForge(File gameDir, String gameVersion, String javaPath,
                                                         Consumer<String> log) {
        return installForge(gameDir, gameVersion, javaPath, null, log);
    }

    public static CompletableFuture<String> installForge(File gameDir, String gameVersion, String javaPath,
                                                         String requestedVersion, Consumer<String> log) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.accept("🔨 查询 Forge 版本...");
                String promos = get("https://files.minecraftforge.net/net/minecraftforge/forge/promotions_slim.json");
                JsonObject promosJson = JsonParser.parseString(promos).getAsJsonObject()
                        .getAsJsonObject("promos");
                String version = requestedVersion;
                if (version == null || version.isBlank()) {
                    for (String key : new String[]{gameVersion + "-recommended", gameVersion + "-latest"}) {
                        if (promosJson.has(key)) {
                            version = promosJson.get(key).getAsString();
                            break;
                        }
                    }
                }
                if (version == null) throw new RuntimeException("Forge 没有适配 " + gameVersion + " 的版本");
                log.accept("   使用 Forge " + version);

                String full = gameVersion + "-" + version;
                String url = "https://maven.minecraftforge.net/net/minecraftforge/forge/" + full
                        + "/forge-" + full + "-installer.jar";
                File installer = new File(gameDir, "downloads/forge-installer-" + full + ".jar");
                log.accept("📥 下载 Forge 安装器...");
                download(url, installer);

                log.accept("⚙️ 正在静默运行 Forge 安装器（需要几分钟）...");
                runInstaller(javaPath, installer, gameDir, log);

                String id = findNewVersion(gameDir, gameVersion, "-forge-");
                if (id == null) throw new RuntimeException("安装器执行完毕但没找到 Forge 版本目录");
                log.accept("✅ Forge 安装完成: " + id);
                return id;
            } catch (Exception e) {
                throw new RuntimeException("Forge 安装失败: " + e.getMessage(), e);
            }
        });
    }

    // ==================== NeoForge ====================

    public static CompletableFuture<String> installNeoForge(File gameDir, String gameVersion, String javaPath,
                                                            Consumer<String> log) {
        return installNeoForge(gameDir, gameVersion, javaPath, null, log);
    }

    public static CompletableFuture<String> installNeoForge(File gameDir, String gameVersion, String javaPath,
                                                            String requestedVersion, Consumer<String> log) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.accept("🦊 查询 NeoForge 版本...");
                String metadata = get("https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml");
                String prefix = neoforgePrefix(gameVersion);
                List<String> versions = new ArrayList<>();
                Matcher m = Pattern.compile("<version>([^<]+)</version>").matcher(metadata);
                while (m.find()) {
                    if (m.group(1).startsWith(prefix)) versions.add(m.group(1));
                }
                if (versions.isEmpty()) throw new RuntimeException("NeoForge 没有适配 " + gameVersion + " 的版本");
                String version = requestedVersion == null || requestedVersion.isBlank()
                        ? versions.get(versions.size() - 1) : requestedVersion;
                log.accept("   使用 NeoForge " + version);

                String url = "https://maven.neoforged.net/releases/net/neoforged/neoforge/" + version
                        + "/neoforge-" + version + "-installer.jar";
                File installer = new File(gameDir, "downloads/neoforge-installer-" + version + ".jar");
                log.accept("📥 下载 NeoForge 安装器...");
                download(url, installer);

                log.accept("⚙️ 正在静默运行 NeoForge 安装器（需要几分钟）...");
                runInstaller(javaPath, installer, gameDir, log);

                String id = findNewVersion(gameDir, gameVersion, "-neoforge-");
                if (id == null) throw new RuntimeException("安装器执行完毕但没找到 NeoForge 版本目录");
                log.accept("✅ NeoForge 安装完成: " + id);
                return id;
            } catch (Exception e) {
                throw new RuntimeException("NeoForge 安装失败: " + e.getMessage(), e);
            }
        });
    }

    /** 列出某个加载器对指定 Minecraft 版本的可用版本（新 → 旧）。 */
    public static CompletableFuture<List<String>> listLoaderVersions(String loader, String gameVersion) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                switch (loader) {
                    case "Fabric": {
                        JsonArray list = JsonParser.parseString(get(
                                "https://meta.fabricmc.net/v2/versions/loader/" + gameVersion)).getAsJsonArray();
                        List<String> out = new ArrayList<>();
                        for (var element : list) {
                            out.add(element.getAsJsonObject().getAsJsonObject("loader")
                                    .get("version").getAsString());
                        }
                        return out;
                    }
                    case "Quilt": {
                        JsonArray list = JsonParser.parseString(get(
                                "https://meta.quiltmc.org/v3/versions/loader/" + gameVersion)).getAsJsonArray();
                        List<String> out = new ArrayList<>();
                        for (var element : list) {
                            out.add(element.getAsJsonObject().getAsJsonObject("loader")
                                    .get("version").getAsString());
                        }
                        return out;
                    }
                    case "Forge": {
                        String metadata = get("https://maven.minecraftforge.net/net/minecraftforge/forge/maven-metadata.xml");
                        String prefix = gameVersion + "-";
                        List<String> out = new ArrayList<>();
                        Matcher matcher = Pattern.compile("<version>([^<]+)</version>").matcher(metadata);
                        while (matcher.find()) {
                            String value = matcher.group(1);
                            if (value.startsWith(prefix)) out.add(value.substring(prefix.length()));
                        }
                        java.util.Collections.reverse(out);
                        return out;
                    }
                    case "NeoForge": {
                        String metadata = get("https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml");
                        String prefix = neoforgePrefix(gameVersion);
                        List<String> out = new ArrayList<>();
                        Matcher matcher = Pattern.compile("<version>([^<]+)</version>").matcher(metadata);
                        while (matcher.find()) {
                            if (matcher.group(1).startsWith(prefix)) out.add(matcher.group(1));
                        }
                        java.util.Collections.reverse(out);
                        return out;
                    }
                    default:
                        return List.of();
                }
            } catch (Exception e) {
                return List.of();
            }
        });
    }

    /** 1.20.4 → "20.4."；1.21.1 → "21.1." */
    private static String neoforgePrefix(String gameVersion) {
        String[] parts = gameVersion.split("\\.");
        if (parts.length >= 3) return parts[1] + "." + parts[2] + ".";
        return gameVersion + ".";
    }

    // ==================== 公共部分 ====================

    private static void saveProfile(File gameDir, String id, JsonObject profile) throws Exception {
        File dir = new File(new File(gameDir, "versions"), id);
        Files.createDirectories(dir.toPath());
        Files.writeString(new File(dir, id + ".json").toPath(), profile.toString(),
                StandardCharsets.UTF_8);
    }

    private static String get(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(60))
                .header("User-Agent", "MCLauncher/1.0")
                .GET().build();
        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("HTTP " + response.statusCode() + " -> " + url);
        }
        return response.body();
    }

    private static void download(String url, File target) throws Exception {
        if (target.isFile() && target.length() > 1024) return;
        Files.createDirectories(target.getParentFile().toPath());
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofMinutes(10))
                .header("User-Agent", "MCLauncher/1.0")
                .GET().build();
        HttpResponse<java.nio.file.Path> response = CLIENT.send(request,
                HttpResponse.BodyHandlers.ofFile(target.toPath(),
                        java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.WRITE));
        if (response.statusCode() != 200) throw new RuntimeException("下载失败 HTTP " + response.statusCode());
    }

    private static void runInstaller(String javaPath, File installer, File gameDir, Consumer<String> log) throws Exception {
        // Forge/NeoForge 安装器要求目录里已经存在 launcher_profiles.json，否则直接报
        // "There is no Minecraft launcher profile ... you need to run the launcher first!"
        ensureLauncherProfiles(gameDir);

        ProcessBuilder pb = new ProcessBuilder(javaPath, "-jar", installer.getAbsolutePath(),
                "--installClient", gameDir.getAbsolutePath());
        pb.directory(gameDir);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        List<String> recent = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;
                recent.add(trimmed);
                if (recent.size() > 40) recent.remove(0);
                // 安装器输出很啰嗦，只挑关键行回显
                if (trimmed.contains("Successfully") || trimmed.contains("error") || trimmed.contains("Error")
                        || trimmed.contains("Exception") || trimmed.contains("Downloading")
                        || trimmed.contains("There was an error")) {
                    log.accept("   " + trimmed);
                }
            }
        }
        int code = process.waitFor();
        if (code != 0) {
            for (String line : recent) log.accept("   | " + line);
            throw new RuntimeException("安装器退出码 " + code);
        }
    }

    /** 某些安装器会把这里当成"官方启动器已运行"的标记。 */
    private static void ensureLauncherProfiles(File gameDir) throws Exception {
        File profiles = new File(gameDir, "launcher_profiles.json");
        if (profiles.isFile()) return;
        String content = "{\n"
                + "  \"profiles\": {},\n"
                + "  \"selectedProfile\": \"\",\n"
                + "  \"clientToken\": \"\",\n"
                + "  \"authenticationDatabase\": {},\n"
                + "  \"launcherVersion\": { \"name\": \"1.0\", \"format\": 21 }\n"
                + "}\n";
        Files.createDirectories(gameDir.toPath());
        Files.writeString(profiles.toPath(), content, StandardCharsets.UTF_8);
    }

    private static String findNewVersion(File gameDir, String gameVersion, String marker) {
        Set<String> ids = new LinkedHashSet<>(VersionProfile.installedIds(gameDir));
        for (String id : ids) {
            if (id.startsWith(gameVersion) && id.contains(marker)) return id;
        }
        return null;
    }
}
