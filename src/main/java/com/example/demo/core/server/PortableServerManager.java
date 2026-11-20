package com.example.demo.core.server;

import com.example.demo.core.download.DownloadManager;
import com.example.demo.core.meta.VersionProfile;
import com.example.demo.core.model.Instance;
import com.example.demo.core.model.InstanceDirs;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * 便携服务端：从实例生成真正能跑的服务端目录，并把「开过的服」记下来。
 *
 * <p>和旧实现的三点差别：
 * <ul>
 *   <li>eula 直接写成 true —— 点「创建」就等于同意 EULA，否则服务端起来立刻自杀；</li>
 *   <li>原版从版本 json 取 server.jar，Fabric 从 meta.fabricmc.net 取（真能下载到）；</li>
 *   <li>创建过程有进度回调，完成后写入 servers.json，下次可以直接再开。</li>
 * </ul>
 */
public final class PortableServerManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private PortableServerManager() {
    }

    /** 一条服务端记录；id 是目录名，用作唯一键。 */
    public record Profile(String id, String name, String dir, int port, String loader,
                          String mcVersion, int memoryMb, long lastStarted) {
        public Profile withLastStarted(long when) {
            return new Profile(id, name, dir, port, loader, mcVersion, memoryMb, when);
        }
    }

    /** 创建进度：给 UI 显示「第几步 / 百分比」。 */
    public record Progress(String step, double percent) {
    }

    // ==================== 记录 ====================

    public static File serversRoot(File gameDir) {
        return new File(gameDir, "portable-servers");
    }

    private static File indexFile(File gameDir) {
        return new File(serversRoot(gameDir), "servers.json");
    }

    public static List<Profile> load(File gameDir) {
        File file = indexFile(gameDir);
        if (!file.isFile()) {
            return new ArrayList<>();
        }
        try {
            String json = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            List<Profile> list = GSON.fromJson(json, new TypeToken<List<Profile>>() {
            }.getType());
            return list == null ? new ArrayList<>() : new ArrayList<>(list);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public static void save(File gameDir, List<Profile> profiles) {
        try {
            Files.createDirectories(serversRoot(gameDir).toPath());
            Files.writeString(indexFile(gameDir).toPath(), GSON.toJson(profiles), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            // 记录失败不影响服务端本身
        }
    }

    public static void remember(File gameDir, Profile profile) {
        List<Profile> list = load(gameDir);
        list.removeIf(p -> p.id().equals(profile.id()));
        list.add(0, profile);
        save(gameDir, list);
    }

    public static void forget(File gameDir, String id) {
        List<Profile> list = load(gameDir);
        list.removeIf(p -> p.id().equals(id));
        save(gameDir, list);
    }

    // ==================== 创建 ====================

    /**
     * 从实例创建一个能直接启动的服务端。
     *
     * @param progress 进度回调（在后台线程调用，UI 侧自行切回 FX 线程）
     */
    public static Profile create(File gameDir, String instanceId, boolean isolation, int port,
                                 int memoryMb, boolean onlineMode, String motd,
                                 Consumer<Progress> progress) throws Exception {
        Consumer<Progress> report = progress == null ? p -> {
        } : progress;
        report.accept(new Progress("准备目录", 0.05));
        String safe = instanceId.replaceAll("[^0-9A-Za-z._-]", "_");
        File dest = new File(serversRoot(gameDir), safe);
        if (dest.exists()) {
            dest = new File(serversRoot(gameDir), safe + "-" + System.currentTimeMillis() / 1000);
        }
        Files.createDirectories(dest.toPath());

        Instance instance = Instance.of(gameDir, instanceId);
        report.accept(new Progress("复制模组与配置", 0.15));
        File root = InstanceDirs.root(gameDir, instanceId, isolation);
        copyDir(new File(root, "mods"), new File(dest, "mods"));
        for (String dir : new String[]{"config", "defaultconfigs", "scripts", "kubejs", "datapacks"}) {
            copyDir(new File(root, dir), new File(dest, dir));
        }

        report.accept(new Progress("写入配置", 0.3));
        Files.writeString(new File(dest, "server.properties").toPath(),
                "server-port=" + port + "\n"
                        + "motd=" + (motd == null || motd.isBlank() ? "Neko Craft Server" : motd.trim()) + "\n"
                        + "online-mode=" + onlineMode + "\n"
                        + "max-players=20\n"
                        + "view-distance=8\n"
                        + "enable-command-block=true\n",
                StandardCharsets.UTF_8);
        // 点「创建」即视为同意 EULA；旧实现写 false 导致服务端启动后立刻退出
        Files.writeString(new File(dest, "eula.txt").toPath(),
                "# 创建时已确认同意 Minecraft EULA（https://aka.ms/MinecraftEULA）\n"
                        + "eula=true\n",
                StandardCharsets.UTF_8);

        File jar = new File(dest, "server.jar");
        String loader = instance.loader == null ? "原版" : instance.loader;
        String mcVersion = instance.mcVersion == null ? "" : instance.mcVersion;
        report.accept(new Progress("下载服务端 (" + loader + ")", 0.4));
        boolean downloaded = downloadServerJar(gameDir, instanceId, loader, mcVersion, jar,
                fraction -> report.accept(new Progress("下载服务端 (" + loader + ")", 0.4 + fraction * 0.5)));
        if (!downloaded) {
            Files.writeString(new File(dest, "README-服务端.txt").toPath(),
                    "这个加载器（" + loader + "）需要手动放置 server.jar：\r\n"
                            + "1. 到对应加载器官网下载 " + mcVersion + " 的服务端安装器\r\n"
                            + "2. 在服务端目录执行安装器，生成 server.jar / libraries\r\n"
                            + "3. 回到启动器点「启动」\r\n",
                    StandardCharsets.UTF_8);
        }
        writeStartScript(dest, memoryMb);

        report.accept(new Progress("记录到历史", 0.95));
        Profile profile = new Profile(dest.getName(), instanceId, dest.getAbsolutePath(), port, loader,
                mcVersion, memoryMb, System.currentTimeMillis());
        remember(gameDir, profile);
        report.accept(new Progress("完成", 1.0));
        return profile;
    }

    private static void writeStartScript(File dest, int memoryMb) throws IOException {
        String line = "java -Xms1G -Xmx" + Math.max(512, memoryMb) + "M -jar server.jar nogui";
        Files.writeString(new File(dest, "start.bat").toPath(),
                "@echo off\r\nchcp 65001 >nul\r\ntitle Neko Portable Server\r\n" + line + "\r\npause\r\n",
                StandardCharsets.UTF_8);
    }

    /** 原版走版本 json 的 downloads.server；Fabric 走 meta.fabricmc.net。 */
    private static boolean downloadServerJar(File gameDir, String instanceId, String loader,
                                             String mcVersion, File jar,
                                             Consumer<Double> fraction) {
        try {
            if ("原版".equals(loader) || loader.isEmpty()) {
                String root = VersionProfile.load(gameDir, instanceId).getRootId();
                var json = com.google.gson.JsonParser.parseString(Files.readString(
                        VersionProfile.jsonFile(gameDir, root).toPath(), StandardCharsets.UTF_8)).getAsJsonObject();
                var server = json.getAsJsonObject("downloads").getAsJsonObject("server");
                String url = server.get("url").getAsString();
                String sha1 = server.has("sha1") ? server.get("sha1").getAsString() : null;
                long size = server.has("size") ? server.get("size").getAsLong() : -1;
                DownloadManager.Stats stats = DownloadManager.downloadAll(
                        List.of(new DownloadManager.Task(url, jar, sha1, size)), 1,
                        (done, total, bytes) -> {
                            if (fraction != null) {
                                fraction.accept(total > 0 ? Math.min(0.99, done / (double) total) : 0.5);
                            }
                        });
                return stats.downloaded > 0 && jar.isFile() && jar.length() > 0;
            }
            if (loader.toLowerCase(Locale.ROOT).contains("fabric")) {
                String loaderVersion = latestFabricLoader(mcVersion);
                if (loaderVersion == null) {
                    return false;
                }
                String url = "https://meta.fabricmc.net/v2/versions/loader/" + mcVersion + "/"
                        + loaderVersion + "/server/jar";
                byte[] bytes = get(url);
                if (bytes == null || bytes.length < 1024) {
                    return false;
                }
                Files.write(jar.toPath(), bytes);
                if (fraction != null) {
                    fraction.accept(1.0);
                }
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private static String latestFabricLoader(String mcVersion) {
        try {
            byte[] bytes = get("https://meta.fabricmc.net/v2/versions/loader/" + mcVersion);
            if (bytes == null) {
                return null;
            }
            var array = com.google.gson.JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8))
                    .getAsJsonArray();
            for (var element : array) {
                var loader = element.getAsJsonObject().getAsJsonObject("loader");
                if (loader != null && loader.has("version")) {
                    return loader.get("version").getAsString();
                }
            }
        } catch (Exception ignored) {
            // 网络不通就交给上层提示手动放置
        }
        return null;
    }

    // ==================== 运行 ====================

    /** 启动服务端进程；输出逐行回调（后台线程）。 */
    public static Process start(Profile profile, Consumer<String> log) throws IOException {
        File dir = new File(profile.dir());
        File jar = new File(dir, "server.jar");
        if (!jar.isFile()) {
            throw new IOException("服务端目录里没有 server.jar");
        }
        String java = ProcessHandle.current().info().command().orElse("java");
        List<String> command = new ArrayList<>(List.of(java,
                "-Xms1G", "-Xmx" + Math.max(512, profile.memoryMb()) + "M",
                "-jar", "server.jar", "nogui"));
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(dir);
        builder.redirectErrorStream(true);
        Process process = builder.start();
        Thread reader = new Thread(() -> {
            try (InputStream in = process.getInputStream()) {
                byte[] buffer = new byte[4096];
                StringBuilder line = new StringBuilder();
                int read;
                while ((read = in.read(buffer)) >= 0) {
                    String chunk = new String(buffer, 0, read, StandardCharsets.UTF_8);
                    for (char c : chunk.toCharArray()) {
                        if (c == '\n') {
                            if (log != null) {
                                log.accept(line.toString());
                            }
                            line.setLength(0);
                        } else if (c != '\r') {
                            line.append(c);
                        }
                    }
                }
                if (line.length() > 0 && log != null) {
                    log.accept(line.toString());
                }
            } catch (IOException ignored) {
                // 进程结束
            }
        }, "portable-server-log");
        reader.setDaemon(true);
        reader.start();
        return process;
    }

    // ==================== 网络信息 ====================

    /** 公网 IPv4：依次尝试几个公共回显服务。 */
    public static String publicIp() {
        for (String url : new String[]{
                "https://api.ipify.org",
                "https://ipv4.icanhazip.com",
                "https://ifconfig.me/ip"}) {
            try {
                byte[] bytes = get(url);
                if (bytes == null) {
                    continue;
                }
                String text = new String(bytes, StandardCharsets.UTF_8).trim();
                if (text.matches("\\d{1,3}(\\.\\d{1,3}){3}")) {
                    return text;
                }
            } catch (Exception ignored) {
                // 换下一个
            }
        }
        return "";
    }

    public static String localIp() {
        try {
            var addresses = java.net.NetworkInterface.networkInterfaces().toList();
            for (var nic : addresses) {
                if (!nic.isUp() || nic.isLoopback()) {
                    continue;
                }
                for (var address : java.util.Collections.list(nic.getInetAddresses())) {
                    if (address instanceof java.net.Inet4Address && address.isSiteLocalAddress()) {
                        return address.getHostAddress();
                    }
                }
            }
        } catch (Exception ignored) {
            // 退回本地
        }
        return "127.0.0.1";
    }

    /** 添加 Windows 防火墙入站规则（需要管理员权限，失败返回 false 与原因）。 */
    public static String allowFirewall(int port) {
        try {
            ProcessBuilder builder = new ProcessBuilder("netsh", "advfirewall", "firewall", "add", "rule",
                    "name=Neko Portable Server " + port, "dir=in", "action=allow",
                    "protocol=TCP", "localport=" + String.valueOf(port));
            builder.redirectErrorStream(true);
            Process process = builder.start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int code = process.waitFor();
            return code == 0 ? "" : output.trim();
        } catch (Exception e) {
            return e.getMessage() == null ? "执行失败" : e.getMessage();
        }
    }

    private static byte[] get(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(8))
                    .header("User-Agent", "NekoCraftLauncher/2.x")
                    .GET()
                    .build();
            HttpResponse<byte[]> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
            return response.statusCode() == 200 ? response.body() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static void copyDir(File from, File to) throws IOException {
        if (!from.isDirectory()) {
            return;
        }
        Files.createDirectories(to.toPath());
        File[] files = from.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            File target = new File(to, file.getName());
            if (file.isDirectory()) {
                copyDir(file, target);
            } else {
                Files.copy(file.toPath(), target.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }
}
