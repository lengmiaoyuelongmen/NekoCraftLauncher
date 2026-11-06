package com.example.demo.ui.page;

import com.example.demo.core.meta.ReleaseTime;
import com.example.demo.ui.component.BlockIcons;
import com.example.demo.ui.component.McVersionCell;
import com.example.demo.ui.shell.Icons;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.collections.FXCollections;

import java.util.List;

import com.example.demo.core.model.MinecraftVersion;
/**
 * 下载中心的 Minecraft 版本页：类型筛选 + 最新版本 + 正式版/快照/远古版三个可折叠分组。
 *
 * <p>数据来自主类持有的 {@code allMinecraftVersions}（版本清单），这里只负责展示与筛选；
 * 点「详情」交给 {@link Host#openDetail}。
 */
public final class MinecraftVersionsPage {

    public interface Host {
        void openDetail(MinecraftVersion version);
    }

    private static final String[][] FILTERS = {
            {"all", "全部"}, {"release", "正式版"}, {"snapshot", "快照"}, {"old", "旧版"}
    };
    private static final String CHIP_IDLE = "-fx-background-color: -app-card;"
            + " -fx-text-fill: -app-text-dim; -fx-background-radius: 16; -fx-padding: 5 14;"
            + " -fx-cursor: hand; -fx-border-color: transparent;";
    private static final String CHIP_ACTIVE = "-fx-background-color: rgba(156, 163, 175,0.16);"
            + " -fx-text-fill: #d1d5db; -fx-background-radius: 16; -fx-padding: 5 14;"
            + " -fx-cursor: hand; -fx-border-color: #9ca3af; -fx-border-radius: 16;";

    private final ObservableList<MinecraftVersion> versions;
    private final Host host;
    private final String gameDir;
    private final VBox latestContainer = new VBox(10.0);
    private final VBox categoriesContainer = new VBox(10.0);
    private String filter = "all";

    public MinecraftVersionsPage(ObservableList<MinecraftVersion> versions, String gameDir,
                                 Host host) {
        this.versions = versions;
        this.gameDir = gameDir;
        this.host = host;
    }

    public VBox build() {
        VBox panel = new VBox(15.0);
        panel.setAlignment(Pos.TOP_LEFT);
        VBox.setVgrow(panel, Priority.ALWAYS);

        HBox filterRow = new HBox(8.0);
        filterRow.setAlignment(Pos.CENTER_LEFT);
        ToggleGroup typeGroup = new ToggleGroup();
        for (String[] entry : FILTERS) {
            ToggleButton chip = new ToggleButton(entry[1]);
            chip.setToggleGroup(typeGroup);
            chip.setSelected(entry[0].equals(filter));
            chip.setStyle(chip.isSelected() ? CHIP_ACTIVE : CHIP_IDLE);
            chip.selectedProperty().addListener(
                    (o, ov, nv) -> chip.setStyle(nv != false ? CHIP_ACTIVE : CHIP_IDLE));
            chip.setOnAction(e -> {
                if (!chip.isSelected()) {
                    chip.setSelected(true);
                    return;
                }
                filter = entry[0];
                populateCategories();
            });
            filterRow.getChildren().add(chip);
        }

        VBox latestCard = new VBox(10.0);
        latestCard.setStyle("-fx-background-color: linear-gradient(to bottom, -app-card-top,"
                + " -app-card-alt2); -fx-background-radius: 18; -fx-padding: 16 18;"
                + " -fx-border-color: -app-border-soft; -fx-border-radius: 18;"
                + " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.24), 20, 0.1, 0, 6);");
        Label latestTitle = new Label("最新版本");
        latestTitle.setStyle("-fx-text-fill: -app-text-strong; -fx-font-size: 16px;"
                + " -fx-font-weight: bold;");
        latestCard.getChildren().addAll(latestTitle, latestContainer);

        categoriesContainer.setStyle("-fx-background-color: transparent;");
        ScrollPane scrollPane = new ScrollPane(categoriesContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;"
                + " -fx-border-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        HBox.setHgrow(scrollPane, Priority.ALWAYS);

        panel.getChildren().addAll(filterRow, latestCard, scrollPane);

        versions.addListener((javafx.collections.ListChangeListener<MinecraftVersion>) c -> {
            if (!versions.isEmpty()) {
                populateLatest();
                populateCategories();
            }
        });
        if (!versions.isEmpty()) {
            populateLatest();
            populateCategories();
        }
        return panel;
    }

    /** 版本清单刷新后重建内容。 */
    public void refresh() {
        populateLatest();
        populateCategories();
    }

    private void populateLatest() {
        latestContainer.getChildren().clear();
        MinecraftVersion latestRelease = versions.stream()
                .filter(v -> "release".equals(v.getType())).findFirst().orElse(null);
        MinecraftVersion latestSnapshot = versions.stream()
                .filter(v -> "snapshot".equals(v.getType())).findFirst().orElse(null);
        if (latestRelease != null) {
            latestContainer.getChildren().add(latestRow(latestRelease));
        }
        if (latestSnapshot != null) {
            latestContainer.getChildren().add(latestRow(latestSnapshot));
        }
    }

    private HBox latestRow(MinecraftVersion version) {
        HBox row = new HBox(12.0);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(7.0, 10.0, 7.0, 10.0));
        row.setMinHeight(54.0);
        row.setStyle("-fx-background-color: -app-hover-2; -fx-background-radius: 8; -fx-cursor: hand;");
        row.setOnMouseEntered(e -> row.setStyle(
                "-fx-background-color: #333; -fx-background-radius: 8; -fx-cursor: hand;"));
        row.setOnMouseExited(e -> row.setStyle(
                "-fx-background-color: -app-hover-2; -fx-background-radius: 8; -fx-cursor: hand;"));

        StackPane icon = BlockIcons.create(version.getType());
        icon.setPrefSize(40.0, 40.0);
        icon.setMinSize(40.0, 40.0);
        icon.setMaxSize(40.0, 40.0);

        VBox info = new VBox(2.0);
        info.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label(version.getId());
        title.setStyle("-fx-text-fill: -app-text-strong; -fx-font-size: 16px;"
                + " -fx-font-weight: bold;");
        String typeStr = "release".equals(version.getType()) ? "最新正式版" : "最新预览版";
        String time = ReleaseTime.format(version.getReleaseTime());
        Label subtitle = new Label(typeStr + (time.isEmpty() ? "" : " · 发布于 " + time));
        subtitle.setStyle("-fx-text-fill: -app-text-muted-3; -fx-font-size: 11.5px;");
        info.getChildren().addAll(title, subtitle);
        HBox.setHgrow(info, Priority.ALWAYS);

        Button detail = new Button("详情");
        detail.setStyle("-fx-background-color: #9ca3af; -fx-text-fill: #111827;"
                + " -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand;"
                + " -fx-padding: 6 16;");
        detail.setMinWidth(Double.NEGATIVE_INFINITY);
        detail.setOnAction(e -> host.openDetail(version));
        row.setOnMouseClicked(e -> host.openDetail(version));
        row.getChildren().addAll(icon, info, detail);
        return row;
    }

    private void populateCategories() {
        categoriesContainer.getChildren().clear();
        List<MinecraftVersion> releases =
                versions.stream().filter(v -> "release".equals(v.getType())).toList();
        List<MinecraftVersion> snapshots =
                versions.stream().filter(v -> "snapshot".equals(v.getType())).toList();
        List<MinecraftVersion> old = versions.stream()
                .filter(v -> v.getType() != null && v.getType().startsWith("old")).toList();
        if (!("snapshot".equals(filter) || "old".equals(filter) || releases.isEmpty())) {
            categoriesContainer.getChildren().add(categoryPanel("正式版", releases));
        }
        if (!("release".equals(filter) || "old".equals(filter) || snapshots.isEmpty())) {
            categoriesContainer.getChildren().add(categoryPanel("快照 / 预览版", snapshots));
        }
        if (!("release".equals(filter) || "snapshot".equals(filter) || old.isEmpty())) {
            categoriesContainer.getChildren().add(categoryPanel("远古版（Alpha / Beta）", old));
        }
        if (categoriesContainer.getChildren().isEmpty()) {
            Label none = new Label("没有符合筛选的版本。");
            none.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 12px; -fx-padding: 10;");
            categoriesContainer.getChildren().add(none);
        }
    }

    private VBox categoryPanel(String name, List<MinecraftVersion> items) {
        VBox panel = new VBox();
        panel.setStyle("-fx-background-color: linear-gradient(to bottom, -app-card-top,"
                + " -app-card-alt2); -fx-background-radius: 16; -fx-border-color: -app-border-soft;"
                + " -fx-border-radius: 16; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.20),"
                + " 16, 0.08, 0, 4);");

        HBox header = new HBox(10.0);
        header.setPadding(new Insets(15.0, 20.0, 15.0, 20.0));
        header.setStyle("-fx-cursor: hand; -fx-background-radius: 10;");
        header.setOnMouseEntered(e -> header.setStyle("-fx-background-color: -app-hover-2;"
                + " -fx-cursor: hand; -fx-background-radius: 10;"));
        header.setOnMouseExited(e -> header.setStyle("-fx-background-color: transparent;"
                + " -fx-cursor: hand; -fx-background-radius: 10;"));

        Label title = new Label(name + " (" + items.size() + ")");
        title.setStyle("-fx-text-fill: -app-text-strong; -fx-font-size: 16px;"
                + " -fx-font-weight: bold;");
        StackPane arrow = Icons.node("chevron-down", 12.0, "#9ca3af");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(title, spacer, arrow);

        VBox content = new VBox(10.0);
        content.setPadding(new Insets(0.0, 10.0, 10.0, 10.0));
        content.setVisible(false);
        content.setManaged(false);
        content.setStyle("-fx-background-color: -app-card-alt; -fx-background-radius: 0 0 10 10;");

        ListView<MinecraftVersion> listView = new ListView<>();
        listView.setItems(FXCollections.observableArrayList(items));
        listView.setPrefHeight(300.0);
        listView.setStyle("-fx-background-color: transparent;"
                + " -fx-control-inner-background: transparent; -fx-border-color: transparent;");
        listView.setCellFactory(param -> new McVersionCell(gameDir, host::openDetail));
        listView.addEventFilter(ScrollEvent.SCROLL, (EventHandler<ScrollEvent>) event -> {
            event.consume();
            Node found = listView.lookup(".scroll-bar:vertical");
            if (found instanceof ScrollBar bar && bar.getMax() > bar.getMin() && bar.isVisible()) {
                double range = bar.getMax() - bar.getMin();
                double next = bar.getValue() - event.getDeltaY() / 1000.0 * range;
                bar.setValue(Math.max(bar.getMin(), Math.min(bar.getMax(), next)));
            }
        });
        content.getChildren().add(listView);

        header.setOnMouseClicked(e -> {
            boolean show = !content.isVisible();
            content.setVisible(show);
            content.setManaged(show);
            arrow.getChildren().setAll(Icons.node(show ? "chevron-up" : "chevron-down", 12.0, "#9ca3af"));
        });
        panel.getChildren().addAll(header, content);
        return panel;
    }
}
