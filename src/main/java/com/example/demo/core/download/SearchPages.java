package com.example.demo.core.download;

import com.example.demo.core.model.ModpackProject;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 社区搜索的分页状态：当前偏移、总数、是否正在加载、已取过的页缓存。
 *
 * <p>从主类的四个散落字段收拢而来；规则与 {@link SearchSession} 保持一致
 * （总数未知按「可能有下一页」处理）。
 */
public final class SearchPages {

    private int offset;
    private int total = -1;
    private boolean loading;
    private final Map<String, List<ModpackProject>> cache = new LinkedHashMap<>();

    /** 预取结果直接当成第一页：偏移跳到已有个数，总数仍未知。 */
    public void resetForPrewarm(int size) {
        this.offset = Math.max(0, size);
        this.total = -1;
        this.loading = false;
    }

    /** 新一次搜索：从头开始，缓存保留（同一批筛选条件再次搜索仍然命中）。 */
    public void reset() {
        this.offset = 0;
        this.total = -1;
        this.loading = false;
    }

    public int offset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = Math.max(0, offset);
    }

    public int total() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public boolean isLoading() {
        return loading;
    }

    public void beginLoad() {
        this.loading = true;
    }

    public void endLoad() {
        this.loading = false;
    }

    /** 还有没有下一页。 */
    public boolean hasMore() {
        return SearchSession.hasMore(offset, total);
    }

    /** 取缓存页；空页视为无效并清掉（避免一直命中一个空列表）。 */
    public List<ModpackProject> cached(String key) {
        List<ModpackProject> page = cache.get(key);
        if (page != null && page.isEmpty()) {
            cache.remove(key);
            return null;
        }
        return page;
    }

    public void cache(String key, List<ModpackProject> page) {
        if (key == null || page == null) {
            return;
        }
        cache.put(key, page);
    }

    /** 缓存的页数（测试与诊断用）。 */
    public int cachedPageCount() {
        return cache.size();
    }
}
