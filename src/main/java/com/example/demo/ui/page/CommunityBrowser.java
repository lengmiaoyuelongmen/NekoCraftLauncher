package com.example.demo.ui.page;

import com.example.demo.core.app.LauncherConfig;
import com.example.demo.core.download.ModrinthSearch;
import com.example.demo.core.download.SearchFetcher;
import com.example.demo.core.download.SearchPages;
import com.example.demo.core.download.SearchSession;
import com.example.demo.core.model.ModpackProject;
import com.example.demo.ui.component.DownloadFilterBar;
import com.example.demo.ui.component.DownloadResultView;
import com.example.demo.ui.component.ModpackCell;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.net.http.HttpClient;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 社区资源浏览：搜索框 + 结果列表 + 分页加载。
 *
 * <p>从主类搬出来的搜索流水线：分页状态在 {@link SearchPages}，网络在 {@link SearchFetcher}，
 * 翻页规则在 {@link SearchSession}；这里只负责把它们串起来并更新界面。
 */
public final class CommunityBrowser {

    /** 主类提供的能力：配置、图标缓存、下载/详情动作、预取结果。 */
    public interface Host {
        LauncherConfig config();

        Map<String, Image> iconCache();

        Image decodeIcon(byte[] bytes);

        HttpClient httpClient();

        void log(String text);

        void showDownloadTasks();

        void download(ModpackProject project);

        void openDetail(ModpackProject project);

        void prefetchIcons(List<ModpackProject> projects);

        /** 预取好的某个分类结果，没有就返回 null。 */
        List<ModpackProject> prewarmed(String projectType);
    }

    private final Host host;
    private final DownloadFilterBar filterBar;
    private final ObservableList<ModpackProject> modpackData = FXCollections.observableArrayList();
    private final SearchPages pages = new SearchPages();
    private final SearchFetcher fetcher;
    private DownloadResultView resultView;
    private PauseTransition watchdog;
    private String projectType = "整合包";

    public CommunityBrowser(Host host, DownloadFilterBar filterBar) {
        this.host = host;
        this.filterBar = filterBar;
        this.fetcher = new SearchFetcher(host.httpClient(), java.time.Duration.ofSeconds(8L));
    }

    /** 构建面板（结果列表在这里创建，因为滚动到底要回调本类）。 */
    public Node build() {
        this.resultView = new DownloadResultView(
                new ModpackCell.Host() {
                    @Override
                    public Map<String, Image> iconCache() {
                        return host.iconCache();
                    }

                    @Override
                    public Image decodeIcon(byte[] bytes) {
                        return host.decodeIcon(bytes);
                    }

                    @Override
                    public HttpClient httpClient() {
                        return host.httpClient();
                    }

                    @Override
                    public void log(String text) {
                        host.log(text);
                    }

                    @Override
                    public void download(ModpackProject project) {
                        host.download(project);
                    }

                    @Override
                    public void openDetail(ModpackProject project) {
                        host.openDetail(project);
                    }
                },
                modpackData,
                this::loadMore);

        VBox panel = new VBox(10.0);
        panel.setAlignment(javafx.geometry.Pos.TOP_LEFT);
        VBox.setVgrow(panel, Priority.ALWAYS);
        panel.getChildren().addAll(filterBar, resultView);
        return panel;
    }

    public DownloadResultView resultView() {
        return resultView;
    }

    public DownloadFilterBar filterBar() {
        return filterBar;
    }

    public ObservableList<ModpackProject> modpackData() {
        return modpackData;
    }

    /** 切换分类（模组 / 整合包 / 资源包 / 数据包 / 光影包）。 */
    public void setProjectType(String projectType) {
        this.projectType = projectType == null ? "整合包" : projectType;
    }

    public String projectType() {
        return projectType;
    }

    /** 当前分类对应的 Modrinth 类型。 */
    public String apiProjectType() {
        return switch (projectType) {
            case "模组" -> "mod";
            case "资源包" -> "resourcepack";
            case "光影包" -> "shader";
            case "数据包" -> "datapack";
            default -> "modpack";
        };
    }

    /** 开始一次新搜索：默认筛选下优先用预取结果。 */
    public void search() {
        String warmedType = apiProjectType();
        String loaderFilter = filterBar.loaderCombo().getValue();
        boolean defaultFilters = SearchSession.isDefaultFilters(
                filterBar.searchField().getText(), filterBar.gameVersionCombo().getValue(),
                loaderFilter, filterBar.sortCombo().getValue(), warmedType);
        List<ModpackProject> ready = defaultFilters ? host.prewarmed(warmedType) : null;

        if (ready != null && !ready.isEmpty()) {
            modpackData.setAll(ready);
            pages.resetForPrewarm(ready.size());
            resultView.status().hide();
            updateFooter();
            host.prefetchIcons(ready);
            return;
        }
        pages.reset();
        modpackData.clear();
        resultView.setLoadMoreHintText("");
        resultView.status().showLoading("正在从 Modrinth 获取数据…（失败自动切 MCIM 镜像）");
        loadPage(true);
    }

    /** 滚动到底时加载下一页。 */
    public void loadMore() {
        loadPage(false);
    }

    private void loadPage(boolean first) {
        if (pages.isLoading() || !pages.hasMore()) {
            return;
        }
        pages.beginLoad();
        ensureWatchdog();
        watchdog.playFromStart();

        int offset = pages.offset();
        String query = filterBar.searchField().getText() == null
                ? "" : filterBar.searchField().getText().trim();
        String gameVer = filterBar.gameVersionCombo().getValue();
        String loader = filterBar.loaderCombo().getValue();
        String sort = filterBar.sortCombo().getValue();
        String pType = ModrinthSearch.projectType(projectType);
        String pageKey = pType + "|" + query + "|" + gameVer + "|" + loader + "|" + sort + "|" + offset;

        List<ModpackProject> cachedPage = pages.cached(pageKey);
        if (cachedPage != null) {
            pages.endLoad();
            resultView.status().hide();
            List<ModpackProject> added = SearchSession.mergeUnique(modpackData, cachedPage);
            pages.setOffset(offset + cachedPage.size());
            updateFooter();
            host.prefetchIcons(added);
            return;
        }

        try {
            String url = ModrinthSearch.buildUrl(query, pType, gameVer, loader, sort, offset);
            String mirror = ModrinthSearch.mirrorUrl(url, pType);
            fetcher.alternate(url, mirror, offset)
                    .orTimeout(10L, TimeUnit.SECONDS)
                    .thenAccept(body -> Platform.runLater(() -> onBody(body, pType, pageKey, offset, first)))
                    .exceptionally(ex -> {
                        Platform.runLater(() -> {
                            stopWatchdog();
                            pages.endLoad();
                            resultView.status().showText("请求失败：" + ex.getMessage() + "（点击重试）");
                            resultView.status().setRetry(() -> {
                                pages.endLoad();
                                resultView.status().showLoading("正在重新加载...");
                                loadPage(true);
                            });
                            updateFooter();
                        });
                        return null;
                    });
        } catch (Exception e) {
            pages.endLoad();
            resultView.status().showText("请求参数构建失败: " + e.getMessage());
        }
    }

    private void onBody(String body, String pType, String pageKey, int offset, boolean first) {
        stopWatchdog();
        pages.endLoad();
        if (body == null || !body.startsWith("{")) {
            resultView.status().showText("加载失败：资源站不可用，点击这里重试");
            resultView.status().setRetry(() -> {
                resultView.status().showLoading("正在重新加载...");
                loadPage(true);
            });
            updateFooter();
            return;
        }
        resultView.status().showLoading("正在准备社区资源…");
        ModrinthSearch.Page page;
        try {
            new Gson().fromJson(body, JsonObject.class);
            Set<String> existingIds = new HashSet<>();
            for (ModpackProject known : modpackData) {
                existingIds.add(known.getId());
            }
            page = ModrinthSearch.parse(body, pType, existingIds);
        } catch (Exception parseError) {
            pages.endLoad();
            resultView.status().showText("资源数据解析失败，点击重试");
            resultView.status().setRetry(() -> loadPage(true));
            return;
        }
        if (page.totalHits() >= 0) {
            pages.setTotal(page.totalHits());
        }
        if (page.rawHits() == 0) {
            pages.endLoad();
            resultView.status().showText(first ? "未找到匹配的项目，请调整筛选条件" : "没有更多结果");
            updateFooter();
            return;
        }
        modpackData.addAll(page.projects());
        pages.cache(pageKey, page.projects());
        pages.setOffset(offset + page.rawHits());
        if (pages.total() < 0 && page.rawHits() < SearchSession.PAGE_SIZE) {
            pages.setTotal(pages.offset());
        }
        resultView.status().hide();
        stopWatchdog();
        pages.endLoad();
        updateFooter();
        host.prefetchIcons(page.projects());
    }

    private void ensureWatchdog() {
        if (watchdog != null) {
            return;
        }
        watchdog = new PauseTransition(Duration.seconds(12.0));
        watchdog.setOnFinished(e -> {
            if (!pages.isLoading()) {
                return;
            }
            pages.endLoad();
            resultView.status().showText("获取超时，请检查网络后点击重试");
            resultView.status().setRetry(() -> {
                resultView.status().showLoading("正在重新加载...");
                loadPage(true);
            });
            updateFooter();
        });
    }

    private void stopWatchdog() {
        if (watchdog != null) {
            watchdog.stop();
        }
    }

    /** 底部的「已显示 N / 共 M」提示。 */
    private void updateFooter() {
        if (resultView == null) {
            return;
        }
        if (modpackData.isEmpty()) {
            resultView.setLoadMoreHintText("");
            return;
        }
        resultView.setLoadMoreHintText(pages.hasMore()
                ? "已显示 " + modpackData.size()
                    + (pages.total() >= 0 ? " / " + pages.total() : "")
                    + " · 滚动到底自动加载"
                : "已加载全部 · 共 " + modpackData.size() + " 个");
    }
}
