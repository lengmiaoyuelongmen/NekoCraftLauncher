package com.example.demo;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 冒烟用的页面标记（scripts/ui-pages.json）必须仍然存在于主源码里。
 *
 * <p>以前标记只写在 smoke 脚本里，界面文案一改，冒烟就会因为"内容没出现"莫名失败；
 * 现在改成同一份数据，并由这个测试盯住漂移。
 */
class PageMarkersTest {

    @Test
    void everyMarkerExistsInMainSources() throws IOException {
        Path root = Path.of("").toAbsolutePath();
        Path json = root.resolve("scripts/ui-pages.json");
        assertTrue(Files.exists(json), "缺少 scripts/ui-pages.json");

        JsonObject parsed =
                JsonParser.parseString(Files.readString(json, StandardCharsets.UTF_8)).getAsJsonObject();
        JsonArray pages = parsed.getAsJsonArray("pages");
        assertTrue(pages != null && pages.size() > 0, "ui-pages.json 里没有页面");

        String sources = readAllSources(root.resolve("src/main/java"));
        List<String> missing = new ArrayList<>();
        for (JsonElement element : pages) {
            JsonObject page = element.getAsJsonObject();
            String marker = page.get("marker").getAsString();
            if (!sources.contains(marker)) {
                missing.add(page.get("name").getAsString() + " -> " + marker);
            }
        }
        assertFalse(
                !missing.isEmpty(),
                "这些页面标记在主源码里找不到了（界面文案改过？同步更新 ui-pages.json）：" + missing);
    }

    private static String readAllSources(Path dir) throws IOException {
        StringBuilder all = new StringBuilder();
        try (Stream<Path> walk = Files.walk(dir)) {
            for (Path file : walk.filter(p -> p.toString().endsWith(".java")).toList()) {
                all.append(Files.readString(file, StandardCharsets.UTF_8)).append('\n');
            }
        }
        return all.toString();
    }
}
