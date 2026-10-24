package com.example.demo;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class JavaManager {
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public static CompletableFuture<String> ensureZulu25JavaAsync(File gameDir, Consumer<String> logConsumer) {
        return ensureJavaAsync(gameDir, 25, logConsumer);
    }

    /** 按版本 JSON 要求的 Java 主版本准备运行时（本地没有就下载 Azul Zulu 对应大版本）。 */
    public static CompletableFuture<String> ensureJavaAsync(File gameDir, int majorVersion, Consumer<String> logConsumer) {
        return CompletableFuture.supplyAsync(() -> {
            File javaDir = new File(gameDir, "java");
            File javaExe = new File(javaDir, "zulu" + majorVersion + "/bin/java.exe");

            if (javaExe.exists()) {
                logConsumer.accept("✅ 检测到本地 Azul Zulu " + majorVersion + "："
                        + javaExe.getAbsolutePath());
                return javaExe.getAbsolutePath();
            }

            logConsumer.accept("🌐 正在查询 Azul Zulu " + majorVersion + " 最新版本信息...");
            String apiUrl = "https://api.azul.com/metadata/v1/zulu/packages/?java_version=" + majorVersion
                    + "&os=windows&arch=x64&java_package_type=jdk&release_status=ga&latest=true&availability_types=CA";

            try {
                HttpRequest apiReq = HttpRequest.newBuilder(URI.create(apiUrl)).GET().build();
                HttpResponse<String> apiResp = client.send(apiReq, HttpResponse.BodyHandlers.ofString());
                JsonArray arr = new Gson().fromJson(apiResp.body(), JsonArray.class);
                if (arr.size() == 0) throw new RuntimeException("未获取到 Zulu " + majorVersion + " 可用版本");

                JsonObject latest = arr.get(0).getAsJsonObject();
                String downloadUrl = latest.get("download_url").getAsString();
                String fileName = downloadUrl.substring(downloadUrl.lastIndexOf("/") + 1);

                File zipFile = new File(javaDir, fileName);
                zipFile.getParentFile().mkdirs();
                logConsumer.accept("📥 开始下载 Zulu " + majorVersion + " (约 200MB，请耐心等待)...");

                java.util.concurrent.atomic.AtomicInteger lastPercent = new java.util.concurrent.atomic.AtomicInteger();
                DownloadManager.Stats dlStats = DownloadManager.downloadRanged(
                        new DownloadManager.Task(downloadUrl, zipFile, null, -1), 4,
                        (percent, total, bytes) -> {
                            if (percent >= lastPercent.get() + 10) {
                                lastPercent.set(percent);
                                logConsumer.accept("⏳ Azul Zulu 下载进度: " + percent + "% ("
                                        + (bytes / 1024 / 1024) + " MB)");
                            }
                        });
                if (dlStats.failed > 0 || !zipFile.isFile() || zipFile.length() == 0) {
                    throw new RuntimeException("Azul Zulu 下载失败");
                }
                logConsumer.accept("📦 下载完毕，正在解压运行环境...");
                try (ZipInputStream zis = new ZipInputStream(new java.io.FileInputStream(zipFile))) {
                    ZipEntry entry;
                    while ((entry = zis.getNextEntry()) != null) {
                        File outFile = new File(javaDir, entry.getName());
                        if (!outFile.toPath().normalize().startsWith(javaDir.toPath())) continue;
                        if (entry.isDirectory()) outFile.mkdirs();
                        else {
                            outFile.getParentFile().mkdirs();
                            try (FileOutputStream fos = new FileOutputStream(outFile)) {
                                byte[] buffer = new byte[8192];
                                int len;
                                while ((len = zis.read(buffer)) != -1) fos.write(buffer, 0, len);
                            }
                        }
                    }
                }

                File rootExtracted = new File(javaDir, fileName.replace(".zip", ""));
                if (rootExtracted.exists() && new File(rootExtracted, "bin").exists()) {
                    rootExtracted.renameTo(new File(javaDir, "zulu" + majorVersion));
                }
                zipFile.delete();

                if (javaExe.exists()) {
                    logConsumer.accept("🎉 Zulu " + majorVersion + " 环境部署成功！");
                    return javaExe.getAbsolutePath();
                } else {
                    throw new RuntimeException("解压后未找到 java.exe");
                }
            } catch (Exception e) {
                logConsumer.accept("❌ Java 自动补全失败: " + e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }
}