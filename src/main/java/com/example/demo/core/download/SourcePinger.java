package com.example.demo.core.download;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.example.demo.core.app.AppVersion;
/** 下载源测速：Modrinth 官方 / MCIM / CurseForge 镜像。 */
public final class SourcePinger {

    public record Result(String name, String url, long millis, boolean ok, String note) {}

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private SourcePinger() {
    }

    public static List<Result> test() {
        List<Result> results = new ArrayList<>();
        results.add(probe("Modrinth 官方",
                "https://api.modrinth.com/v2/tag/game_version"));
        results.add(probe("MCIM 镜像",
                "https://mod.mcimirror.top/modrinth/v2/tag/game_version"));
        results.add(probe("CurseForge 镜像",
                "https://mod.mcimirror.top/curseforge/v1/mods/search?gameId=432&pageSize=1"));
        results.sort(Comparator.comparing((Result r) -> !r.ok()).thenComparingLong(Result::millis));
        return results;
    }

    private static Result probe(String name, String url) {
        long start = System.currentTimeMillis();
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(6))
                    .header("User-Agent", "NekoCraftLauncher/" + AppVersion.CURRENT)
                    .GET().build();
            HttpResponse<Void> response = CLIENT.send(request, HttpResponse.BodyHandlers.discarding());
            long ms = System.currentTimeMillis() - start;
            return new Result(name, url, ms, response.statusCode() >= 200 && response.statusCode() < 400,
                    "HTTP " + response.statusCode());
        } catch (Exception e) {
            return new Result(name, url, System.currentTimeMillis() - start, false,
                    e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        }
    }
}
