package com.example.demo;

import com.example.demo.core.download.SearchPages;
import com.example.demo.core.model.ModpackProject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 社区搜索的分页状态：偏移、总数、加载标志、页缓存。 */
class SearchPagesTest {

    private ModpackProject project(String id) {
        return new ModpackProject(id, "t", "d", "a", "", 0L, "", "Modrinth", "模组");
    }

    @Test
    void prewarmJumpsToKnownSize() {
        SearchPages pages = new SearchPages();
        pages.resetForPrewarm(40);
        assertEquals(40, pages.offset());
        assertEquals(-1, pages.total());
        assertFalse(pages.isLoading());
        assertTrue(pages.hasMore(), "总数未知时仍认为可以继续加载");
    }

    @Test
    void resetStartsOver() {
        SearchPages pages = new SearchPages();
        pages.setOffset(80);
        pages.setTotal(120);
        pages.beginLoad();
        pages.reset();
        assertEquals(0, pages.offset());
        assertEquals(-1, pages.total());
        assertFalse(pages.isLoading());
        assertTrue(pages.hasMore());
    }

    @Test
    void loadingFlagAndHasMore() {
        SearchPages pages = new SearchPages();
        pages.setTotal(40);
        pages.setOffset(40);
        assertFalse(pages.hasMore());
        pages.beginLoad();
        assertTrue(pages.isLoading());
        pages.endLoad();
        assertFalse(pages.isLoading());

        pages.setOffset(0);
        assertTrue(pages.hasMore());
    }

    @Test
    void negativeOffsetsAreClamped() {
        SearchPages pages = new SearchPages();
        pages.setOffset(-10);
        assertEquals(0, pages.offset());
        pages.resetForPrewarm(-5);
        assertEquals(0, pages.offset());
    }

    @Test
    void cacheRemembersPagesAndDropsEmptyOnes() {
        SearchPages pages = new SearchPages();
        pages.cache("k1", List.of(project("a"), project("b")));
        assertEquals(2, pages.cached("k1").size());
        assertEquals(1, pages.cachedPageCount());

        pages.cache("k2", List.of());
        assertNull(pages.cached("k2"), "空页视为无效");
        assertEquals(1, pages.cachedPageCount(), "空页会被清出缓存");

        assertNull(pages.cached("missing"));
        pages.cache(null, List.of(project("c")));
        pages.cache("k3", null);
        assertEquals(1, pages.cachedPageCount());
    }
}
