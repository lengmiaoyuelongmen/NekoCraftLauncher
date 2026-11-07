package com.example.demo.core.download;

import com.example.demo.core.model.ModpackProject;

import java.util.ArrayList;
import java.util.List;

/**
 * 社区搜索的分页状态与去重规则。
 *
 * <p>搜索请求本身（URL、镜像、解析）已经在 {@link ModrinthSearch} 里；这里补上主类里
 * 散落的那几段判断：缓存键怎么拼、什么时候还有下一页、翻页时怎么去重。
 */
public final class SearchSession {

    /** 每页条数（Modrinth 允许 1..100，这里沿用 40）。 */
    public static final int PAGE_SIZE = 40;

    private SearchSession() {
    }

    /** 一次搜索的标识：分类 + 关键词 + 游戏版本 + 加载器 + 排序。 */
    public record Key(String projectType, String query, String gameVersion, String loader,
                      String sort, int offset) {

        /** 缓存键（同一组条件 + 同一偏移量才算同一页）。 */
        public String cacheKey() {
            return projectType + "|" + nullToEmpty(query) + "|" + nullToEmpty(gameVersion) + "|"
                    + nullToEmpty(loader) + "|" + nullToEmpty(sort) + "|" + offset;
        }

        public Key nextPage() {
            return new Key(projectType, query, gameVersion, loader, sort, offset + PAGE_SIZE);
        }

        private static String nullToEmpty(String value) {
            return value == null ? "" : value;
        }
    }

    /** 官方源与镜像的请求顺序：按页码奇偶交替。 */
    public record Pair(String primary, String backup) {
    }

    public static Pair primaryBackup(String officialUrl, String mirrorUrl, int offset) {
        boolean evenPage = Math.max(0, offset) / PAGE_SIZE % 2 == 0;
        return evenPage ? new Pair(officialUrl, mirrorUrl) : new Pair(mirrorUrl, officialUrl);
    }

    /** 还有没有下一页：总数未知（-1）时按“可能有”处理。 */
    public static boolean hasMore(int offset, int total) {
        return total < 0 || offset < total;
    }

    public static int nextOffset(int offset, int pageSize) {
        return offset + Math.max(1, pageSize);
    }

    /** 把新一页并入已有列表，按项目 id 去重；返回真正新增的部分（用于预取图标）。 */
    public static List<ModpackProject> mergeUnique(List<ModpackProject> target,
                                                   List<ModpackProject> incoming) {
        List<ModpackProject> added = new ArrayList<>();
        if (incoming == null) {
            return added;
        }
        for (ModpackProject project : incoming) {
            if (project == null || project.getId() == null) {
                continue;
            }
            boolean exists = target.stream()
                    .anyMatch(existing -> project.getId().equals(existing.getId()));
            if (exists) {
                continue;
            }
            target.add(project);
            added.add(project);
        }
        return added;
    }

    /** 是否还是「默认筛选」：没有关键词、不限版本/加载器、默认排序。 */
    public static boolean isDefaultFilters(String query, String gameVersion, String loader,
                                           String sort, String projectType) {
        boolean defaultLoader = "任意".equals(loader)
                || ("datapack".equals(projectType) && "DataPack".equals(loader));
        return (query == null || query.trim().isEmpty())
                && "任意".equals(gameVersion)
                && defaultLoader
                && "默认".equals(sort);
    }
}
