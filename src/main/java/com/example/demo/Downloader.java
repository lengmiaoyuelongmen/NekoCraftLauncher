package com.example.demo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class Downloader {
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public static long downloadFileSync(String officialUrl, File targetFile) {
        // 大于 1KB 视为已完成；但空文件/极小的残留文件会重新下载
        if (targetFile.exists() && targetFile.length() > 1024) return targetFile.length();
        if (!targetFile.getParentFile().exists()) targetFile.getParentFile().mkdirs();

        String mirrorUrl = officialUrl
                .replace("libraries.minecraft.net", "bmclapi2.bangbang93.com/maven")
                .replace("piston-data.mojang.com", "bmclapi2.bangbang93.com")
                .replace("launchermeta.mojang.com", "bmclapi2.bangbang93.com")
                .replace("resources.download.minecraft.net", "bmclapi2.bangbang93.com/assets");

        List<String> candidateUrls = new ArrayList<>();
        if (!mirrorUrl.equals(officialUrl)) candidateUrls.add(mirrorUrl);
        candidateUrls.add(officialUrl);

        File partFile = new File(targetFile.getAbsolutePath() + ".part");

        for (String url : candidateUrls) {
            for (int attempt = 1; attempt <= 2; attempt++) {
                try {
                    HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofSeconds(20)).GET().build();
                    HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
                    if (response.statusCode() == 200) {
                        String contentType = response.headers().firstValue("Content-Type").orElse("");
                        if (contentType.contains("text/html") && !officialUrl.endsWith(".json")) continue;

                        // 先写 .part，成功后再改名，避免中断留下"看起来已下载"的半截文件
                        try (InputStream in = response.body(); FileOutputStream out = new FileOutputStream(partFile)) {
                            byte[] buffer = new byte[16384];
                            int bytesRead;
                            while ((bytesRead = in.read(buffer)) != -1) out.write(buffer, 0, bytesRead);
                        }
                        if (partFile.length() > 0) {
                            java.nio.file.Files.move(partFile.toPath(), targetFile.toPath(),
                                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                            return targetFile.length();
                        }
                    }
                } catch (Exception ignored) {
                } finally {
                    if (partFile.exists()) partFile.delete();
                }
            }
        }
        return 0L;
    }
}