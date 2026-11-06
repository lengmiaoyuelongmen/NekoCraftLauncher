package com.example.demo;

import com.example.demo.core.download.SearchSession;
import com.example.demo.core.model.ModpackProject;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 社区搜索分页：缓存键、翻页判断、跨页去重、默认筛选。 */
class SearchSessionTest {

    private ModpackProject project(String id) {
        return new ModpackProject(id, "t-" + id, "desc", "author", "", 0L, "", "Modrinth", "整合包");
    }

    @Test
    void cacheKeyIncludesEveryFilterAndOffset() {
        SearchSession.Key key = new SearchSession.Key("modpack", "sodium", "1.20.1", "Fabric", "默认", 40);
        assertEquals("modpack|sodium|1.20.1|Fabric|默认|40", key.cacheKey());

        SearchSession.Key blank = new SearchSession.Key("modpack", null, null, null, null, 0);
        assertEquals("modpack|||||0", blank.cacheKey());
    }

    @Test
    void nextPageAdvancesByPageSize() {
        SearchSession.Key key = new SearchSession.Key("modpack", "q", "任意", "任意", "默认", 0);
        assertEquals(SearchSession.PAGE_SIZE, key.nextPage().offset());
        assertEquals(80, key.nextPage().nextPage().offset());
    }

    @Test
    void hasMoreHandlesUnknownTotal() {
        assertTrue(SearchSession.hasMore(0, -1), "总数未知时按可能有下一页处理");
        assertTrue(SearchSession.hasMore(40, 100));
        assertFalse(SearchSession.hasMore(100, 100));
        assertFalse(SearchSession.hasMore(120, 100));
    }

    @Test
    void mergeUniqueSkipsDuplicatesAndReturnsAdded() {
        List<ModpackProject> target = new ArrayList<>(List.of(project("a"), project("b")));
        List<ModpackProject> added = SearchSession.mergeUnique(target,
                List.of(project("b"), project("c"), project("a"), project("d")));

        assertEquals(4, target.size());
        assertEquals(2, added.size(), "只有 c 和 d 是新的");
        assertEquals("c", added.get(0).getId());
        assertEquals("d", added.get(1).getId());
    }

    @Test
    void mergeUniqueHandlesNullInput() {
        List<ModpackProject> target = new ArrayList<>();
        assertTrue(SearchSession.mergeUnique(target, null).isEmpty());
        assertTrue(target.isEmpty());
    }

    @Test
    void defaultFiltersDetection() {
        assertTrue(SearchSession.isDefaultFilters("", "任意", "任意", "默认", "modpack"));
        assertTrue(SearchSession.isDefaultFilters(null, "任意", "任意", "默认", "modpack"));
        assertTrue(SearchSession.isDefaultFilters("", "任意", "DataPack", "默认", "datapack"),
                "数据包分类下 DataPack 也算默认加载器");

        assertFalse(SearchSession.isDefaultFilters("sodium", "任意", "任意", "默认", "modpack"));
        assertFalse(SearchSession.isDefaultFilters("", "1.20.1", "任意", "默认", "modpack"));
        assertFalse(SearchSession.isDefaultFilters("", "任意", "Fabric", "默认", "modpack"));
        assertFalse(SearchSession.isDefaultFilters("", "任意", "任意", "下载量", "modpack"));
    }
}
