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
                String official = "https://maven.minecraftforge.net/net/minecraftforge/forge/" + full
                        + "/forge-" + full + "-installer.jar";
                String mirrored = mavenMirror == null ? null
                        : mavenMirror + "net/minecraftforge/forge/" + full
                                + "/forge-" + full + "-installer.jar";
                File installer = new File(gameDir, "downloads/forge-installer-" + full + ".jar");
                log.accept("📥 下载 Forge 安装器...");
                if (mirrored != null) {
                    downloadAny(installer, log, official, mirrored);
                } else {
                    downloadAny(installer, log, official);
                }

                prefetchInstallerLibraries(installer, gameDir, log);
                log.accept("⚙️ 正在静默运行 Forge 安装器（需要几分钟）...");
                runInstaller(javaPath, installer, gameDir, log, mirrorEnabled());

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
                String officialMeta =
                        "https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml";
                String mirroredMeta = mavenMirror == null ? null
                        : mavenMirror + "net/neoforged/neoforge/maven-metadata.xml";
                String metadata = mirroredMeta != null
                        ? getAny(log, officialMeta, mirroredMeta)
                        : getAny(log, officialMeta);
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

                String official = "https://maven.neoforged.net/releases/net/neoforged/neoforge/"
                        + version + "/neoforge-" + version + "-installer.jar";
                String mirrored = mavenMirror == null ? null
                        : mavenMirror + "net/neoforged/neoforge/" + version
                                + "/neoforge-" + version + "-installer.jar";
                File installer = new File(gameDir, "downloads/neoforge-installer-" + version + ".jar");
                log.accept("📥 下载 NeoForge 安装器...");
                if (mirrored != null) {
                    downloadAny(installer, log, official, mirrored);
                } else {
                    downloadAny(installer, log, official);
                }

                prefetchInstallerLibraries(installer, gameDir, log);
                log.accept("⚙️ 正在静默运行 NeoForge 安装器（需要几分钟）...");
                runInstaller(javaPath, installer, gameDir, log, mirrorEnabled());

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

    /** 可选的 Maven 镜像（BMCLAPI）；为 null 时只走官方源。 */
    private static volatile String mavenMirror = "https://bmclapi2.bangbang93.com/maven/";

    /** 主类按「下载源」设置调用：official → null，其它 → BMCLAPI 镜像。 */
    public static void setMavenMirror(String mirror) {
        mavenMirror = mirror;
    }

    private static boolean mirrorEnabled() {
        return mavenMirror != null && !mavenMirror.isBlank();
    }

    private static String get(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("User-Agent", "MCLauncher/1.0")
                .GET().build();
        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("HTTP " + response.statusCode() + " -> " + url);
        }
        return response.body();
    }

    /** 依次尝试多个源，返回第一个成功的响应；都失败时抛出带源清单的异常。 */
    private static String getAny(Consumer<String> log, String... urls) {
        StringBuilder errors = new StringBuilder();
        for (String url : urls) {
            try {
                return get(url);
            } catch (Exception e) {
                String reason = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                errors.append("\n  - ").append(url).append(" → ").append(reason);
                if (log != null) log.accept("   ↳ 源不可用: " + reason);
            }
        }
        throw new RuntimeException("所有下载源都连接失败:" + errors);
    }

    private static void download(String url, File target) throws Exception {
        Files.deleteIfExists(target.toPath());
        Files.createDirectories(target.getParentFile().toPath());
        File part = new File(target.getAbsolutePath() + ".part");
        Files.deleteIfExists(part.toPath());
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofMinutes(10))
                .header("User-Agent", "MCLauncher/1.0")
                .GET().build();
        HttpResponse<java.nio.file.Path> response = CLIENT.send(request,
                HttpResponse.BodyHandlers.ofFile(part.toPath(),
                        java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.WRITE));
        if (response.statusCode() != 200) {
            part.delete();
            throw new RuntimeException("下载失败 HTTP " + response.statusCode());
        }
        long expected = response.headers().firstValueAsLong("Content-Length").orElse(-1);
        if (expected > 0 && part.length() != expected) {
            part.delete();
            throw new RuntimeException("安装器下载不完整: " + part.length() + "/" + expected);
        }
        if (!isValidJar(part)) {
            part.delete();
            throw new RuntimeException("安装器不是有效 JAR");
        }
        Files.move(part.toPath(), target.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * 预取安装器依赖的库文件。
     *
     * <p>Forge/NeoForge 的安装器会直接连 maven.neoforged.net 下载库（"--mirror" 只影响部分下载），
     * 国内网络连不上就会 "HTTP connect timed out"。这里先按安装器里的 install_profile.json
     * 从 BMCLAPI 镜像把库下好，安装器执行时就能直接在本地命中。
     */
    private static void prefetchInstallerLibraries(File installer, File gameDir, Consumer<String> log) {
        if (mavenMirror == null || mavenMirror.isBlank()) {
            return;
        }
        List<String> paths = new ArrayList<>();
        List<String> hashes = new ArrayList<>();
        try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(installer)) {
            // 安装器会同时处理 install_profile 与 version.json 里的库，两边都要预取
            collectLibraries(zip, "install_profile.json", paths, hashes);
            collectLibraries(zip, "version.json", paths, hashes);
        } catch (Exception e) {
            return;
        }
        if (paths.isEmpty()) {
            return;
        }

        int downloaded = 0;
        int skipped = 0;
        int failed = 0;
        File libRoot = new File(gameDir, "libraries");
        for (int i = 0; i < paths.size(); i++) {
            String path = paths.get(i);
            File out = new File(libRoot, path.replace('/', File.separatorChar));
            String sha1 = hashes.get(i);
            if (out.isFile() && out.length() > 0
                    && (sha1.isBlank() || sha1.equalsIgnoreCase(sha1Of(out)))) {
                skipped++;
                continue;
            }
            try {
                downloadRaw(mavenMirror + path, out, sha1);
                downloaded++;
            } catch (Exception e) {
                failed++;
            }
        }
        log.accept("   \uD83D\uDCE6 预取安装库: 下载 " + downloaded + " / 已存在 " + skipped
                + (failed > 0 ? " / 失败 " + failed : ""));
    }

    /** 从安装器 jar 里的某个 JSON 收集需要预取的库（libraries + processors.classpath）。 */
    private static void collectLibraries(
            java.util.zip.ZipFile zip, String entryName, List<String> paths, List<String> hashes) {
        try {
            java.util.zip.ZipEntry entry = zip.getEntry(entryName);
            if (entry == null) {
                return;
            }
            JsonObject root = JsonParser.parseString(
                    new String(zip.getInputStream(entry).readAllBytes(), StandardCharsets.UTF_8))
                    .getAsJsonObject();
            if (root.has("libraries") && root.get("libraries").isJsonArray()) {
                for (JsonElement element : root.getAsJsonArray("libraries")) {
                    if (!element.isJsonObject()) continue;
                    JsonObject library = element.getAsJsonObject();
                    String path = null;
                    String sha1 = "";
                    if (library.has("downloads") && library.get("downloads").isJsonObject()) {
                        JsonObject downloads = library.getAsJsonObject("downloads");
                        if (downloads.has("artifact") && downloads.get("artifact").isJsonObject()) {
                            JsonObject artifact = downloads.getAsJsonObject("artifact");
                            if (artifact.has("path")) {
                                path = artifact.get("path").getAsString();
                            }
                            if (artifact.has("sha1") && !artifact.get("sha1").isJsonNull()) {
                                sha1 = artifact.get("sha1").getAsString();
                            }
                        }
                    }
                    if (path == null && library.has("name")) {
                        path = mavenPath(library.get("name").getAsString());
                    }
                    if (path == null || path.isBlank()) continue;
                    if (!paths.contains(path)) {
                        paths.add(path);
                        hashes.add(sha1);
                    }
                }
            }
            if (root.has("processors") && root.get("processors").isJsonArray()) {
                for (JsonElement element : root.getAsJsonArray("processors")) {
                    if (!element.isJsonObject()) continue;
                    JsonObject processor = element.getAsJsonObject();
                    if (!processor.has("classpath") || !processor.get("classpath").isJsonArray()) continue;
                    for (JsonElement cp : processor.getAsJsonArray("classpath")) {
                        String path = mavenPath(cp.getAsString());
                        if (path != null && !paths.contains(path)) {
                            paths.add(path);
                            hashes.add("");
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // 某个 JSON 解析失败不影响另一个
        }
    }

    /** group:artifact:version@ext → maven 相对路径。 */
    private static String mavenPath(String coordinate) {
        if (coordinate == null || coordinate.isBlank()) {
            return null;
        }
        try {
            String ext = "jar";
            String name = coordinate;
            int at = name.indexOf('@');
            if (at > 0) {
                ext = name.substring(at + 1);
                name = name.substring(0, at);
            }
            String[] parts = name.split(":");
            if (parts.length < 3) {
                return null;
            }
            String group = parts[0].replace('.', '/');
            String artifact = parts[1];
            String version = parts[2];
            return group + "/" + artifact + "/" + version + "/" + artifact + "-" + version + "." + ext;
        } catch (Exception e) {
            return null;
        }
    }

    private static String sha1Of(File file) {
        try (java.io.InputStream in = Files.newInputStream(file.toPath())) {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-1");
            byte[] buffer = new byte[65536];
            int read;
            while ((read = in.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            StringBuilder sb = new StringBuilder();
            for (byte b : digest.digest()) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    /** 依次尝试多个源下载安装器，成功即返回；都失败时给出每个源的原因。 */
    private static void downloadAny(File target, Consumer<String> log, String... urls) {
        StringBuilder errors = new StringBuilder();
        for (String url : urls) {
            try {
                download(url, target);
                log.accept("   ✔ 已从 " + hostOf(url) + " 下载安装器");
                return;
            } catch (Exception e) {
                String reason = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                errors.append("\n  - ").append(url).append(" → ").append(reason);
                if (log != null) log.accept("   ↳ 下载失败(" + hostOf(url) + "): " + reason);
            }
        }
        throw new RuntimeException("所有下载源都失败:" + errors);
    }

    private static String hostOf(String url) {
        try {
            String host = java.net.URI.create(url).getHost();
            return host == null ? url : host;
        } catch (Exception e) {
            return url;
        }
    }

    /** 下载任意类型的文件（库/压缩包），只校验长度与 sha1，不做 JAR 结构检查。 */
    private static void downloadRaw(String url, File target, String sha1) throws Exception {
        Files.deleteIfExists(target.toPath());
        Files.createDirectories(target.getParentFile().toPath());
        File part = new File(target.getAbsolutePath() + ".part");
        Files.deleteIfExists(part.toPath());
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofMinutes(10))
                .header("User-Agent", "MCLauncher/1.0")
                .GET().build();
        HttpResponse<java.nio.file.Path> response = CLIENT.send(request,
                HttpResponse.BodyHandlers.ofFile(part.toPath(),
                        java.nio.file.StandardOpenOption.CREATE,
                        java.nio.file.StandardOpenOption.WRITE));
        if (response.statusCode() != 200) {
            part.delete();
            throw new RuntimeException("下载失败 HTTP " + response.statusCode());
        }
        long expected = response.headers().firstValueAsLong("Content-Length").orElse(-1);
        if (expected > 0 && part.length() != expected) {
            part.delete();
            throw new RuntimeException("文件不完整: " + part.length() + "/" + expected);
        }
        if (sha1 != null && !sha1.isBlank() && !sha1.equalsIgnoreCase(sha1Of(part))) {
            part.delete();
            throw new RuntimeException("SHA1 校验失败");
        }
        Files.move(part.toPath(), target.toPath(),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    private static boolean isValidJar(File file) {
        try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(file)) {
            boolean forge = zip.getEntry("install_profile.json") != null;
            boolean manifest = zip.getEntry("META-INF/MANIFEST.MF") != null;
            if (!forge && !manifest) return false;
            java.util.Enumeration<? extends java.util.zip.ZipEntry> entries = zip.entries();
            byte[] buffer = new byte[8192];
            while (entries.hasMoreElements()) {
                java.util.zip.ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) continue;
                try (java.io.InputStream in = zip.getInputStream(entry)) {
                    while (in.read(buffer) != -1) {
                        // 读完整条目，提前发现截断的 ZIP
                    }
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static void runInstaller(String javaPath, File installer, File gameDir, Consumer<String> log)
            throws Exception {
        runInstaller(javaPath, installer, gameDir, log, false);
    }

    private static void runInstaller(String javaPath, File installer, File gameDir, Consumer<String> log,
                                     boolean useMirror) throws Exception {
        // Forge/NeoForge 安装器要求目录里已经存在 launcher_profiles.json，否则直接报
        // "There is no Minecraft launcher profile ... you need to run the launcher first!"
        ensureLauncherProfiles(gameDir);

        String java = resolveJava(javaPath);
        if (java == null) {
            throw new RuntimeException("没有可用的 Java：实例里没有内置运行时，设置里也没填 Java 路径");
        }
        List<String> command = new ArrayList<>();
        command.add(java);
        command.add("-jar");
        command.add(installer.getAbsolutePath());
        command.add("--installClient");
        command.add(gameDir.getAbsolutePath());
        if (useMirror && mavenMirror != null) {
            // 安装器自己下载库文件也走镜像，否则国内会卡在 maven.neoforged.net
            command.add("--mirror");
            command.add(mavenMirror);
        }
        ProcessBuilder pb = new ProcessBuilder(command);
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

    /**
     * 挑一个真的能跑的 java.exe：传进来的路径可能不存在（内置运行时下载失败时会回退成不存在的路径），
     * 也可能只是 "java"（PATH 里没有 java 时 CreateProcess error=2）。
     */
    private static String resolveJava(String javaPath) {
        if (javaPath != null) {
            String trimmed = javaPath.trim();
            if (!trimmed.isEmpty() && !"java".equalsIgnoreCase(trimmed)) {
                File file = new File(trimmed);
                if (file.isFile()) return file.getAbsolutePath();
            }
        }
        File home = new File(System.getProperty("java.home", ""));
        File exe = new File(home, "bin/java.exe");
        if (exe.isFile()) return exe.getAbsolutePath();
        File bin = new File(home, "bin/java");
        if (bin.isFile()) return bin.getAbsolutePath();
        return null;
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
