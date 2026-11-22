package com.example.demo.core.game;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;

/**
 * Java 运行时下载源：先 Azul Zulu（国内 CDN 快），失败再 Adoptium Temurin。
 *
 * <p>选包规则和 HTTP 分开，{@code parse*} 方法可以直接用固定 JSON 单测；{@code find*} 才发请求。
 *
 * <p>Zulu 的接口会同时返回 zip/msi、fx/非 fx、CRaC 实验版，这里只取「非 CRaC 的 zip」，
 * 并优先不带 JavaFX 的包（体积小一半）。
 */
public final class JavaRuntimeIndex {

    /** 一个可下载的 JDK。 */
    public record Package(String source, String url, String fileName, String sha256) {
    }

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private JavaRuntimeIndex() {
    }

    /** 查询 Azul Zulu；网络失败或没有合适包时返回 null。 */
    public static Package findZulu(int majorVersion) {
        String url = "https://api.azul.com/metadata/v1/zulu/packages/?java_version=" + majorVersion
                + "&os=windows&arch=x64&java_package_type=jdk&release_status=ga&latest=true&availability_types=CA";
        try {
            String body = get(url);
            return parseZulu(JsonParser.parseString(body).getAsJsonArray());
        } catch (Exception e) {
            return null;
        }
    }

    /** 查询 Adoptium Temurin；网络失败或没有合适包时返回 null。 */
    public static Package findAdoptium(int majorVersion) {
        String url = "https://api.adoptium.net/v3/assets/latest/" + majorVersion
                + "/hotspot?os=windows&architecture=x64&image_type=jdk";
        try {
            String body = get(url);
            return parseAdoptium(JsonParser.parseString(body).getAsJsonArray());
        } catch (Exception e) {
            return null;
        }
    }

    /** 从 Zulu 的 packages 数组里挑一个 zip（跳过 CRaC / msi，优先非 fx）。 */
    public static Package parseZulu(JsonArray packages) {
        Package fallback = null;
        for (JsonElement element : packages) {
            if (element == null || !element.isJsonObject()) {
                continue;
            }
            JsonObject item = element.getAsJsonObject();
            if (!item.has("download_url")) {
                continue;
            }
            String downloadUrl = item.get("download_url").getAsString();
            String lower = downloadUrl.toLowerCase(Locale.ROOT);
            if (!lower.endsWith(".zip") || lower.contains("crac")) {
                continue;
            }
            String fileName = downloadUrl.substring(downloadUrl.lastIndexOf('/') + 1);
            Package candidate = new Package("Azul Zulu", downloadUrl, fileName, null);
            if (!lower.contains("-fx-")) {
                return candidate;
            }
            if (fallback == null) {
                fallback = candidate;
            }
        }
        return fallback;
    }

    /** 从 Adoptium 的 assets 数组里取第一个带 package.link 的 JDK（附 sha256）。 */
    public static Package parseAdoptium(JsonArray assets) {
        for (JsonElement element : assets) {
            if (element == null || !element.isJsonObject()) {
                continue;
            }
            JsonObject binary = element.getAsJsonObject().has("binary")
                    ? element.getAsJsonObject().getAsJsonObject("binary")
                    : null;
            if (binary == null || !binary.has("package") || !binary.get("package").isJsonObject()) {
                continue;
            }
            JsonObject pkg = binary.getAsJsonObject("package");
            if (!pkg.has("link")) {
                continue;
            }
            String link = pkg.get("link").getAsString();
            String name = pkg.has("name") ? pkg.get("name").getAsString()
                    : link.substring(link.lastIndexOf('/') + 1);
            String checksum = pkg.has("checksum") ? pkg.get("checksum").getAsString() : null;
            return new Package("Adoptium Temurin", link, name, checksum);
        }
        return null;
    }

    private static String get(String url) throws Exception {
        // 必须给整体请求超时：只设 connectTimeout 时，服务端「连上了但不回」会一直挂着，
        // 界面就永远停在「运行中」（用户的现场就是这样）。
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .GET()
                .build();
        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("HTTP " + response.statusCode());
        }
        return response.body();
    }
}
