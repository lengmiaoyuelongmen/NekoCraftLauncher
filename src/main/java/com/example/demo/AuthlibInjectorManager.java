package com.example.demo;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class AuthlibInjectorManager {
    private static final String API_URL = "https://authlib-injector.yushi.moe/artifact/latest.json";
    private static final HttpClient client = HttpClient.newHttpClient();

    public static CompletableFuture<File> ensureInjectorAsync(File gameDir, Consumer<String> log) {
        return CompletableFuture.supplyAsync(() -> {
            File injectorFile = new File(gameDir, "authlib-injector.jar");
            if (injectorFile.exists() && injectorFile.length() > 1024) {
                return injectorFile;
            }

            try {
                log.accept("📥 正在获取 authlib-injector 最新版本信息...");
                HttpRequest apiReq = HttpRequest.newBuilder(URI.create(API_URL)).GET().build();
                HttpResponse<String> apiResp = client.send(apiReq, HttpResponse.BodyHandlers.ofString());

                JsonObject json = new Gson().fromJson(apiResp.body(), JsonObject.class);
                String downloadUrl = json.get("download_url").getAsString();

                log.accept("📥 开始下载 authlib-injector.jar...");
                HttpRequest dlReq = HttpRequest.newBuilder(URI.create(downloadUrl)).GET().build();
                HttpResponse<java.io.InputStream> dlResp = client.send(dlReq, HttpResponse.BodyHandlers.ofInputStream());

                if (!gameDir.exists()) gameDir.mkdirs();
                try (java.io.InputStream in = dlResp.body(); java.io.FileOutputStream out = new java.io.FileOutputStream(injectorFile)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                }
                log.accept("✅ authlib-injector 下载完成");
                return injectorFile;
            } catch (Exception e) {
                log.accept("❌ authlib-injector 下载失败: " + e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }
}