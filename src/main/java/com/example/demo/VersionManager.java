package com.example.demo;

import com.google.gson.Gson;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class VersionManager {
    private static final String OFFICIAL_MANIFEST_URL = "https://piston-meta.mojang.com/mc/game/version_manifest.json";
    private static final String MIRROR_MANIFEST_URL = "https://bmclapi2.bangbang93.com/mc/game/version_manifest.json";

    /** 版本列表落盘缓存：下次启动先秒开列表，再后台更新。 */
    private static final File CACHE_FILE = new File("cache/version_manifest.json");

    private static final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private static final Gson gson = new Gson();

    private static String fetchWithFallback(String officialUrl, String mirrorUrl) {
        List<String> urls = new ArrayList<>();
        if (mirrorUrl != null && !mirrorUrl.isEmpty()) urls.add(mirrorUrl);
        urls.add(officialUrl);

        for (String url : urls) {
            for (int i = 0; i < 3; i++) {
                try {
                    HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofSeconds(15)).GET().build();
                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() == 200 && response.body() != null && response.body().startsWith("{")) {
                        return response.body();
                    }
                } catch (Exception ignored) {}
            }
        }
        return null;
    }

    public static CompletableFuture<List<MinecraftVersion>> fetchVersionListAsync() {
        return CompletableFuture.supplyAsync(() -> {
            String body = fetchWithFallback(OFFICIAL_MANIFEST_URL, MIRROR_MANIFEST_URL);
            if (body == null) {
                // 网络不可用时退回磁盘缓存（离线也能看到版本列表）
                List<MinecraftVersion> cached = loadCachedList();
                if (cached != null && !cached.isEmpty()) return cached;
                throw new RuntimeException("获取版本列表失败");
            }
            writeCache(body);
            VersionManifest manifest = gson.fromJson(body, VersionManifest.class);
            return manifest.getVersions();
        });
    }

    /** 读本地缓存的版本列表；没有或损坏返回 null。 */
    public static List<MinecraftVersion> loadCachedList() {
        if (!CACHE_FILE.isFile()) return null;
        try (Reader reader = new FileReader(CACHE_FILE)) {
            VersionManifest manifest = gson.fromJson(reader, VersionManifest.class);
            if (manifest == null || manifest.getVersions() == null || manifest.getVersions().isEmpty()) {
                return null;
            }
            return manifest.getVersions();
        } catch (Exception e) {
            return null;
        }
    }

    private static void writeCache(String body) {
        try {
            File parent = CACHE_FILE.getParentFile();
            if (parent != null && !parent.isDirectory()) parent.mkdirs();
            try (Writer writer = new FileWriter(CACHE_FILE)) {
                writer.write(body);
            }
        } catch (Exception ignored) {
            // 缓存写失败不影响启动
        }
    }

    public static CompletableFuture<String> fetchVersionDetailsAsync(String officialUrl) {
        return CompletableFuture.supplyAsync(() -> {
            String mirrorUrl = officialUrl.replace("piston-meta.mojang.com", "bmclapi2.bangbang93.com");
            String body = fetchWithFallback(officialUrl, mirrorUrl);
            if (body == null) throw new RuntimeException("获取版本详情失败");
            return body;
        });
    }
}