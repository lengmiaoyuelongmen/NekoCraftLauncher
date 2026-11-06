package com.example.demo;

import com.example.demo.core.download.ModrinthSearch;
import com.example.demo.core.model.ModpackProject;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Modrinth 搜索协议：类型映射、facets 拼装、镜像切换、响应解析。 */
class ModrinthSearchTest {

    @Test
    void mapsUiTypesToProjectTypes() {
        assertEquals("mod", ModrinthSearch.projectType("模组"));
        assertEquals("resourcepack", ModrinthSearch.projectType("资源包"));
        assertEquals("shader", ModrinthSearch.projectType("光影包"));
        assertEquals("datapack", ModrinthSearch.projectType("数据包"));
        assertEquals("modpack", ModrinthSearch.projectType("整合包"));
        assertEquals("modpack", ModrinthSearch.projectType(null));
    }

    @Test
    void buildsFacetsAndEncodesQuery() {
        String url = ModrinthSearch.buildUrl("sodium", "mod", "1.20.1", "Fabric", "下载量", 40);

        assertTrue(url.startsWith("https://api.modrinth.com/v2/search?query=sodium"), url);
        assertTrue(url.contains("project_type%3Amod"), "project_type 必须在 facets 里: " + url);
        assertTrue(url.contains("versions%3A1.20.1"), "游戏版本进 facets: " + url);
        assertTrue(url.contains("categories%3Afabric"), "loader 要转小写: " + url);
        assertTrue(url.contains("index=downloads"), "排序映射: " + url);
        assertTrue(url.contains("offset=40"), url);
    }

    @Test
    void omitsOptionalFacetsWhenAny() {
        String url = ModrinthSearch.buildUrl("", "modpack", "任意", "任意", "默认", 0);

        assertTrue(url.contains("project_type%3Amodpack"), url);
        assertTrue(!url.contains("versions"), "「任意」不应带版本条件: " + url);
        assertTrue(!url.contains("categories"), "「任意」不应带 loader 条件: " + url);
        assertTrue(url.contains("index=relevance"), url);
    }

    @Test
    void datapackIgnoresLoaderFilter() {
        String url = ModrinthSearch.buildUrl("", "datapack", "任意", "Fabric", "默认", 0);
        assertTrue(!url.contains("categories"), url);
    }

    @Test
    void usesMirrorOnlyForModsAndModpacks() {
        String modUrl = "https://api.modrinth.com/v2/search?query=x";
        assertTrue(ModrinthSearch.mirrorUrl(modUrl, "mod").startsWith("https://mod.mcimirror.top/modrinth/v2"));
        assertTrue(ModrinthSearch.mirrorUrl(modUrl, "modpack").contains("mcimirror"));
        assertEquals(modUrl, ModrinthSearch.mirrorUrl(modUrl, "resourcepack"),
                "资源包/光影仍走官方源");
    }

    @Test
    void parsesHitsAndDeduplicates() {
        String json = """
                {
                  "total_hits": 123,
                  "hits": [
                    {"project_id": "A", "title": "Sodium", "description": "fps", "author": "jelly",
                     "icon_url": "https://cdn/x.png", "downloads": 100, "date_modified": "2024-05-01T00:00:00Z"},
                    {"project_id": "B", "title": "Known", "description": "already", "author": "x",
                     "downloads": 5, "date_modified": "2024-01-01T00:00:00Z"}
                  ]
                }
                """;

        ModrinthSearch.Page page = ModrinthSearch.parse(json, "mod", Set.of("B"));

        assertEquals(123, page.totalHits());
        assertEquals(2, page.rawHits(), "原始 hits 数用于分页");
        assertEquals(1, page.projects().size(), "已在列表里的项目要跳过");
        ModpackProject project = page.projects().get(0);
        assertEquals("A", project.getId());
        assertEquals("Sodium", project.getTitle());
        assertEquals("jelly", project.getAuthor());
        assertEquals("2024-05-01", project.getUpdated());
        assertEquals("mod", project.getProjectType());
    }

    @Test
    void handlesEmptyResult() {
        ModrinthSearch.Page page = ModrinthSearch.parse("{\"hits\": []}", "mod", Set.of());
        assertEquals(0, page.rawHits());
        assertEquals(-1, page.totalHits());
        assertTrue(page.projects().isEmpty());
    }

    @Test
    void rejectsBrokenJson() {
        assertThrows(IllegalArgumentException.class,
                () -> ModrinthSearch.parse("not-json", "mod", Set.of()));
    }
}
