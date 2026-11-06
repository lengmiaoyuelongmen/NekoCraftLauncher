package com.example.demo;

import com.example.demo.core.download.CurseForgeClient;
import com.google.gson.JsonParser;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** CurseForge / MCIM 镜像协议：搜索解析、文件解析与排序、按版本和加载器筛选。 */
class CurseForgeClientTest {

    private JsonObject json(String text) {
        return JsonParser.parseString(text).getAsJsonObject();
    }

    @Test
    void parsesSearchPage() {
        JsonObject root = json("""
                {"data":[
                  {"id": 1, "name": "Sodium", "slug": "sodium", "summary": "fps",
                   "downloadCount": 123, "authors": [{"name": "jelly"}],
                   "logo": {"thumbnailUrl": "https://cf/thumb.png"}},
                  {"id": 2, "name": "NoLogo", "slug": "nologo",
                   "logo": {"url": "https://cf/logo.png"}}
                ], "pagination": {"totalCount": 999}}
                """);

        CurseForgeClient.MirrorPage page = CurseForgeClient.parseMirrorPage(root);

        assertEquals(999, page.total());
        assertEquals(2, page.hits().size());
        CurseForgeClient.Hit first = page.hits().get(0);
        assertEquals("Sodium", first.name());
        assertEquals("jelly", first.author());
        assertEquals("https://cf/thumb.png", first.iconUrl());
        assertEquals(123, first.downloads());
        assertEquals("https://cf/logo.png", page.hits().get(1).iconUrl(), "没有 thumbnail 时回退 url");
    }

    @Test
    void emptyOrBrokenSearchReturnsEmptyPage() {
        assertEquals(0, CurseForgeClient.parseMirrorPage(null).hits().size());
        assertEquals(0, CurseForgeClient.parseMirrorPage(json("{}")).hits().size());
    }

    @Test
    void parsesFilesIgnoringNonJarsAndSortingReleaseFirst() {
        JsonObject root = json("""
                {"data":[
                  {"id": 10, "fileName": "alpha.jar", "releaseType": 3, "fileLength": 100},
                  {"id": 11, "fileName": "readme.txt", "releaseType": 1},
                  {"id": 12, "fileName": "beta.jar", "releaseType": 2},
                  {"id": 13, "fileName": "release-old.jar", "releaseType": 1},
                  {"id": 14, "fileName": "release-new.jar", "releaseType": 1}
                ]}
                """);

        List<CurseForgeClient.MirrorFile> files = CurseForgeClient.parseMirrorFiles(root);

        assertEquals(4, files.size(), "非 jar 文件要过滤掉");
        assertEquals("release-new.jar", files.get(0).fileName());
        assertEquals("release-old.jar", files.get(1).fileName());
        assertEquals("beta.jar", files.get(2).fileName());
        assertEquals("alpha.jar", files.get(3).fileName());
    }

    @Test
    void buildsForgeCdnUrlsFromFileId() {
        CurseForgeClient.MirrorFile file = new CurseForgeClient.MirrorFile(
                1234567, "Sodium", "sodium 1.0.jar", "", 1, 1024);
        List<String> urls = file.urls();

        assertTrue(urls.get(0).contains("/1234/567/"), "id 1234567 应拆成 /1234/567/: " + urls.get(0));
        assertTrue(urls.get(0).contains("sodium%201.0.jar"), "文件名要 URL 编码: " + urls.get(0));
        assertTrue(urls.stream().anyMatch(u -> u.contains("mcimirror")));
    }

    @Test
    void filtersFilesByGameVersionAndLoader() {
        List<CurseForgeClient.FileEntry> files = List.of(
                new CurseForgeClient.FileEntry(1, "mod-1.20.1-fabric.jar", "1.20.1", "release"),
                new CurseForgeClient.FileEntry(2, "mod-1.20.1-forge.jar", "1.20.1", "release"),
                new CurseForgeClient.FileEntry(3, "mod-1.20.1-neoforge.jar", "1.20.1", "release"),
                new CurseForgeClient.FileEntry(4, "mod-1.20.1-beta.jar", "1.20.1", "beta"),
                new CurseForgeClient.FileEntry(5, "mod-1.19.2-fabric.jar", "1.19.2", "release"));
        CurseForgeClient.Project project =
                new CurseForgeClient.Project(1, "Test", "test", 0, "", files);

        List<CurseForgeClient.FileEntry> fabric = CurseForgeClient.matching(project, "1.20.1", "fabric");
        // 当前策略：指定加载器时文件名必须带该加载器；没写加载器的通用包会被排除
        assertEquals(1, fabric.size(), "只留文件名带 fabric 的 1.20.1 包: " + fabric);
        assertEquals(1, fabric.get(0).id(), "release 优先");

        List<CurseForgeClient.FileEntry> forge = CurseForgeClient.matching(project, "1.20.1", "forge");
        assertEquals(1, forge.size(), "forge 不能把 neoforge 算进来: " + forge);
        assertEquals(2, forge.get(0).id());

        List<CurseForgeClient.FileEntry> neoforge = CurseForgeClient.matching(project, "1.20.1", "neoforge");
        assertEquals(3, neoforge.get(0).id());

        List<CurseForgeClient.FileEntry> all = CurseForgeClient.matching(project, null, "原版");
        assertEquals(5, all.size(), "不筛选时全部返回（含非 jar 名命中的）");
    }
}
