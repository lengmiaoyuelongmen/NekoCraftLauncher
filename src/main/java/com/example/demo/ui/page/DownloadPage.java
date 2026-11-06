package com.example.demo.ui.page;

import com.example.demo.ui.component.DownloadNavItem;
import com.example.demo.ui.shell.ModuleHeader;
import com.example.demo.ui.shell.Motion;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * 下载中心的页面外壳：左侧分类导航（含滑动指示器）+ 右侧内容区。
 *
 * <p>从主类拆出来的第一块「页面级」代码。这里只负责导航结构与内容切换动画；
 * 选中某个分类之后要做什么（打开安装器 / 拉版本列表 / 搜索社区资源）由 {@link Host#onSelect} 决定，
 * 主类不需要再关心指示器怎么动、面板怎么换。
 */
public final class DownloadPage {

    /** 主类提供的能力：四个内容面板 + 分类选中后的业务分发。 */
    public interface Host {
        Node communityPanel();

        Node minecraftPanel();

        Node worldsNoticePanel();

        Node favoritesPanel();

        /** 用户点了左侧某个分类（文字与导航项一致）。 */
        void onSelect(String text);
    }

    private static final Interpolator SMOOTH = Interpolator.SPLINE(0.25, 0.1, 0.25, 1.0);

    private final Host host;
    private final StackPane contentArea = new StackPane();
    private final VBox navBox = new VBox(0.0);
    private final Rectangle indicator = new Rectangle(4.0, 42.0);
    private final List<DownloadNavItem> navItems = new ArrayList<>();
    private final List<DownloadNavItem> installerItems = new ArrayList<>();

    public DownloadPage(Host host) {
        this.host = host;
    }

    /** 构建整个下载中心（左侧导航 + 右侧内容区）。 */
    public HBox build() {
        HBox panel = new HBox(0.0);
        panel.setStyle("-fx-background-color: -app-bg;");

        VBox sidebar = new VBox(0.0);
        sidebar.setPrefWidth(230.0);
        sidebar.setMinWidth(230.0);
        sidebar.setMaxWidth(230.0);
        sidebar.setStyle(
                "-fx-background-color: linear-gradient(to bottom, -app-nav, -app-surface-2); -fx-padding:"
                        + " 20 0 0 0; -fx-border-color: transparent -app-border-2 transparent transparent;");

        indicator.setFill(javafx.scene.paint.Color.web("#9ca3af"));
        indicator.setArcWidth(4.0);
        indicator.setArcHeight(4.0);
        indicator.setMouseTransparent(true);

        StackPane navContainer = new StackPane(navBox, indicator);
        StackPane.setAlignment(indicator, Pos.TOP_LEFT);
        navContainer.setAlignment(Pos.TOP_LEFT);
        ScrollPane navScroll = new ScrollPane(navContainer);
        navScroll.setFitToWidth(true);
        navScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        navScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        navScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        navBox.getChildren().add(group("游戏", false));
        navBox.getChildren().add(navItem("Minecraft"));
        navBox.getChildren().add(group("社区资源", true));
        for (String item : new String[]{"模组", "整合包", "数据包", "资源包", "光影包", "世界", "收藏夹"}) {
            DownloadNavItem node = navItem(item);
            if ("整合包".equals(item)) {
                node.setActive(true);
            }
            navBox.getChildren().add(node);
        }
        navBox.getChildren().add(group("安装包", true));
        for (String loader : new String[]{"OptiFine", "Fabric", "Quilt", "Forge", "NeoForge"}) {
            DownloadNavItem node = navItem(loader);
            installerItems.add(node);
            navBox.getChildren().add(node);
        }
        sidebar.getChildren().add(navScroll);
        VBox.setVgrow(navScroll, Priority.ALWAYS);

        contentArea.setPadding(new Insets(18.0));
        contentArea.setStyle("-fx-background-color: transparent;");
        HBox.setHgrow(contentArea, Priority.ALWAYS);
        VBox.setVgrow(contentArea, Priority.ALWAYS);
        contentArea.setMinWidth(0);

        Node community = host.communityPanel();
        Node minecraft = host.minecraftPanel();
        Node worlds = host.worldsNoticePanel();
        Node favorites = host.favoritesPanel();
        minecraft.setVisible(false);
        worlds.setVisible(false);
        favorites.setVisible(false);
        contentArea.getChildren().addAll(community, minecraft, worlds, favorites);

        Region header = new ModuleHeader(
                "02 / LIBRARY",
                "下载中心",
                "Minecraft · 模组 · 整合包 · 资源",
                "download",
                ModuleHeader.Badge.SQUARE,
                "Minecraft Java Edition");
        VBox.setMargin(header, new Insets(12.0, 18.0, 0.0, 18.0));
        VBox right = new VBox(14.0, header, contentArea);
        right.setMinWidth(0);
        right.setPrefWidth(0);
        VBox.setVgrow(contentArea, Priority.ALWAYS);
        HBox.setHgrow(right, Priority.ALWAYS);

        panel.getChildren().addAll(sidebar, right);
        return panel;
    }

    public StackPane contentArea() {
        return contentArea;
    }

    /** 切换右侧内容（同一时间只留一个子节点，避免 StackPane 被最宽面板撑开）。 */
    public void show(Node target) {
        if (target == null) {
            return;
        }
        if (contentArea.getChildren().size() != 1 || contentArea.getChildren().get(0) != target) {
            contentArea.getChildren().setAll(target);
        }
        target.setVisible(true);
        Motion.pageIn(target);
    }

    /** 页面重新可见时把指示器对齐到当前选中项（尺寸可能刚算出来）。 */
    public void alignIndicator() {
        for (DownloadNavItem item : navItems) {
            if (item.isActive()) {
                indicator.setHeight(Math.max(24.0, item.getHeight() - 8.0));
                indicator.setTranslateY(centeredY(item));
                return;
            }
        }
        javafx.application.Platform.runLater(this::alignIndicator);
    }

    private double centeredY(Region item) {
        return item.getLayoutY() + (item.getHeight() - indicator.getHeight()) / 2.0;
    }

    private DownloadNavItem navItem(String text) {
        DownloadNavItem node = new DownloadNavItem(text);
        navItems.add(node);
        node.setOnMouseClicked(e -> select(text, node));
        return node;
    }

    /** 选中某个分类：更新指示器与高亮，然后把业务交给宿主。 */
    public void select(String text, DownloadNavItem node) {
        indicator.setHeight(Math.max(24.0, node.getHeight() - 8.0));
        double targetY = centeredY(node);
        Timeline timeline = new Timeline(new KeyFrame(Duration.millis(250.0),
                new KeyValue(indicator.translateYProperty(), targetY, SMOOTH)));
        timeline.play();
        for (DownloadNavItem item : navItems) {
            item.setActive(false);
        }
        node.setActive(true);
        host.onSelect(text);
    }

    private Label group(String text, boolean indented) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: -app-text-muted-2; -fx-font-size: 13px; -fx-font-weight: bold;"
                + " -fx-padding: " + (indented ? "25 20 5 25;" : "10 20 5 25;"));
        return label;
    }
}
