package com.example.demo.ui.page;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

/**
 * 整合包/资源「可安装版本」的加载：先出加载页，再并行问官方与镜像，拿到列表后交给
 * {@link ModpackVersionPage}；失败或没有版本时在加载页上给「重新获取」。
 *
 * <p>从主类的 fetchVersionsAndDownload + showVersionLoadingPage 搬出。版本列表按项目缓存
 * 10 分钟。
 */
public final class ModpackVersionLoader {

    public interface Host {
        StackPane contentArea();

        void showPage(Node page);

        void backToCommunity();

        /** 展示版本选择页。 */
        void openVersions(String title, JsonArray versions, IntConsumer onChosen);

        /** 用户选定版本后开始下载。 */
        void downloadChosen(String title, String projectType, JsonArray versions, int index);

        void log(String text);
    }

    private static final long CACHE_TTL_MS = 10 * 60 * 1000L;
    private static final HttpClient HTTP =
            HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(15L)).build();

    private final Host host;
    private final Map<String, JsonArray> cache = new ConcurrentHashMap<>();
    private final Map<String, Long> cacheAt = new ConcurrentHashMap<>();
    private Label loadingLabel;
    private Button retryButton;
    private String lastId;
    private String lastTitle;
    private String lastType;

    public ModpackVersionLoader(Host host) {
        this.host = host;
    }

    /** 打开某个项目的版本列表（有缓存直接开，否则先出加载页再请求）。 */
    public void open(String projectId, String projectTitle, String projectType) {
        this.lastId = projectId;
        this.lastTitle = projectTitle;
        this.lastType = projectType;
        showLoading(projectTitle);

        JsonArray cached = cache.get(projectId);
        Long cachedAt = cacheAt.get(projectId);
        if (cached != null && cachedAt != null
                && System.currentTimeMillis() - cachedAt < CACHE_TTL_MS) {
            Platform.runLater(() -> openPage(projectTitle, projectType, cached));
            return;
        }

        String officialUrl = "https://api.modrinth.com/v2/project/" + projectId + "/version";
        String mirrorUrl = officialUrl.replace(
                "https://api.modrinth.com/v2", "https://mod.mcimirror.top/modrinth/v2");
        Supplier<String> fetchOne = () -> {
            CompletableFuture<String> winner = new CompletableFuture<>();
            for (String url : new String[] {mirrorUrl, officialUrl}) {
                CompletableFuture.runAsync(() -> {
                    try {
                        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                                .header("User-Agent", "PCL-CE-Launcher/1.0")
                                .GET()
                                .timeout(java.time.Duration.ofSeconds(15L))
                                .build();
                        HttpResponse<String> response =
                                HTTP.send(request, HttpResponse.BodyHandlers.ofString());
                        if (response.statusCode() == 200
                                && response.body() != null
                                && response.body().startsWith("[")) {
                            winner.complete(response.body());
                        }
                    } catch (Exception ignored) {
                        // 另一个源还有机会
                    }
                });
            }
            try {
                return winner.get(20L, TimeUnit.SECONDS);
            } catch (Exception e) {
                return null;
            }
        };

        CompletableFuture.supplyAsync(fetchOne).thenAccept(body -> Platform.runLater(() -> {
            if (body == null || !body.startsWith("[")) {
                host.log("❌ 获取 " + projectTitle + " 版本列表失败。\n");
                showRetry("读取版本列表失败，检查网络后点「重新获取」");
                return;
            }
            JsonArray versions = new Gson().fromJson(body, JsonArray.class);
            if (versions.size() > 0) {
                cache.put(projectId, versions);
                cacheAt.put(projectId, System.currentTimeMillis());
            }
            if (versions.size() == 0) {
                host.log("❌ " + projectTitle + " 暂无可用版本。\n");
                showRetry("这个项目暂时没有可下载的版本");
                return;
            }
            openPage(projectTitle, projectType, versions);
        }));
    }

    private void openPage(String projectTitle, String projectType, JsonArray versions) {
        host.openVersions(projectTitle, versions, chosen -> {
            if (chosen >= 0) {
                host.downloadChosen(projectTitle, projectType, versions, chosen);
            }
        });
    }

    private void showLoading(String title) {
        Button back = new Button("‹  返回");
        back.getStyleClass().addAll("btn-secondary", "btn-sm");
        Label heading = new Label(title);
        heading.setStyle("-fx-text-fill: -app-text; -fx-font-size: 16px; -fx-font-weight: bold;");
        Label subtitle = new Label("正在读取可安装的版本…");
        subtitle.setStyle("-fx-text-fill: -app-text-dim; -fx-font-size: 11px;");
        VBox headingBox = new VBox(2, heading, subtitle);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(12, back, headingBox, spacer);
        header.setAlignment(Pos.CENTER_LEFT);

        loadingLabel = new Label("正在读取版本列表…");
        loadingLabel.setStyle("-fx-text-fill: -app-text-2; -fx-font-size: 13px;");
        Button retry = new Button("重新获取");
        retry.getStyleClass().addAll("btn-secondary", "btn-sm");
        retry.setVisible(false);
        retry.setManaged(false);
        retry.setOnAction(e -> {
            if (lastId == null) {
                host.backToCommunity();
                return;
            }
            cache.remove(lastId);
            cacheAt.remove(lastId);
            open(lastId, lastTitle, lastType);
        });
        retryButton = retry;

        VBox center = new VBox(12, loadingLabel, retry);
        center.setAlignment(Pos.CENTER);
        VBox.setVgrow(center, Priority.ALWAYS);

        VBox page = new VBox(10, header, center);
        page.setPadding(new Insets(6, 10, 6, 6));
        page.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        page.getStyleClass().add("version-page");
        host.contentArea().getChildren().removeIf(n -> n.getStyleClass().contains("version-page"));
        back.setOnAction(e -> host.backToCommunity());
        host.showPage(page);
    }

    private void showRetry(String message) {
        if (loadingLabel != null) {
            loadingLabel.setText(message);
        }
        if (retryButton != null) {
            retryButton.setVisible(true);
            retryButton.setManaged(true);
        }
    }

    /** 清掉某个项目的版本缓存（测试与诊断用）。 */
    public void invalidate(String projectId) {
        cache.remove(projectId);
        cacheAt.remove(projectId);
    }

    /** 加载页上正在显示的错误文案（测试与诊断用）。 */
    public String loadingMessage() {
        return loadingLabel == null ? null : loadingLabel.getText();
    }

    /** 供外部（如对话框）重试当前项目。 */
    public void retry() {
        if (lastId != null) {
            cache.remove(lastId);
            cacheAt.remove(lastId);
            open(lastId, lastTitle, lastType);
        }
    }
}
