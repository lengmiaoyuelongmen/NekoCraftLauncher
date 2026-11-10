package com.example.demo.ui.page;

import com.example.demo.core.game.LoaderInstaller;
import com.example.demo.ui.shell.Icons;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.example.demo.core.model.MinecraftVersion;
/**
 * 安装器页：选游戏版本 / 填实例名 / 手风琴选加载器版本 / 开始下载。
 *
 * <p>从主类拆出来的第三块页面级代码。这里只负责界面与选择状态；
 * 「开始下载」要做什么（官网走浏览器、原版/加载器走安装流程）仍由 {@link Host#startInstall} 决定。
 */
public final class InstallerPage {

    public interface Host {
        /** 返回版本列表。 */
        void backToList();

        /** 点了「开始下载」；业务逻辑由宿主读取本页状态后执行。 */
        void startInstall(Button trigger);
    }

    /** 手风琴里一行加载器需要记住的节点。 */
    private static final class LoaderRow {
        final HBox header;
        final VBox body;
        final Label subtitle;
        final Label arrow;

        LoaderRow(HBox header, VBox body, Label subtitle, Label arrow) {
            this.header = header;
            this.body = body;
            this.subtitle = subtitle;
            this.arrow = arrow;
        }
    }

    private final Host host;
    private final Label title = new Label("安装新实例");
    private final ComboBox<MinecraftVersion> versionCombo = new ComboBox<>();
    private final TextField nameField = new TextField();
    private final VBox accordion = new VBox(6.0);
    private final Map<String, LoaderRow> rows = new LinkedHashMap<>();
    private final Map<String, String> selectedVersions = new LinkedHashMap<>();
    private final Label status = new Label("先选游戏版本，再展开加载器选择具体版本。");
    private final ProgressBar progress = new ProgressBar();
    private final Button download = new Button("开始下载");
    private String expandedLoader;

    public InstallerPage(Host host) {
        this.host = host;
    }

    public VBox build() {
        VBox panel = new VBox(10.0);
        panel.setAlignment(Pos.TOP_LEFT);
        panel.setPadding(new Insets(0.0));

        Button back = new Button("返回列表");
        back.setGraphic(Icons.node("back", 13.0, "#9ca3af"));
        back.setGraphicTextGap(4.0);
        back.setStyle("-fx-background-color: transparent; -fx-text-fill: -app-text-dim;"
                + " -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 2 4;");
        back.setOnAction(e -> host.backToList());

        title.setStyle("-fx-text-fill: -app-text; -fx-font-size: 18px; -fx-font-weight: bold;");
        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);
        HBox top = new HBox(10.0, back, title, topSpacer);
        top.setAlignment(Pos.CENTER_LEFT);

        versionCombo.setPrefWidth(150.0);
        versionCombo.setPromptText("Minecraft 版本");
        versionCombo.setStyle("-fx-background-radius: 8;");
        versionCombo.setConverter(new StringConverter<MinecraftVersion>() {
            @Override
            public String toString(MinecraftVersion value) {
                return value == null ? "" : value.getId();
            }

            @Override
            public MinecraftVersion fromString(String string) {
                return null;
            }
        });
        versionCombo.valueProperty().addListener((o, ov, nv) -> onGameVersionChanged());

        nameField.setPromptText("实例名（留空使用版本号）");
        nameField.setPrefWidth(200.0);
        HBox.setHgrow(nameField, Priority.ALWAYS);
        HBox versionRow = new HBox(10.0, label("游戏版本"), versionCombo, label("实例名"), nameField);
        versionRow.setAlignment(Pos.CENTER_LEFT);

        for (String loader : new String[]{"原版", "Fabric", "Forge", "NeoForge", "Quilt",
                "OptiFine", "LabyMod"}) {
            accordion.getChildren().add(createLoaderRow(loader));
        }

        download.setStyle("-fx-background-color: linear-gradient(to bottom right, #d1d5db, #9ca3af);"
                + " -fx-text-fill: #111827; -fx-font-weight: bold; -fx-background-radius: 9;"
                + " -fx-padding: 9 26; -fx-cursor: hand; -fx-font-size: 13px;");
        download.setMinWidth(Double.NEGATIVE_INFINITY);
        download.setOnAction(e -> host.startInstall(download));

        progress.setMaxWidth(Double.MAX_VALUE);
        progress.setVisible(false);
        progress.setManaged(false);
        progress.setStyle("-fx-accent: -app-accent;");

        status.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11.5px;");
        status.setWrapText(true);
        HBox.setHgrow(status, Priority.ALWAYS);
        HBox bottom = new HBox(12.0, download, status);
        bottom.setAlignment(Pos.CENTER_LEFT);
        VBox bottomBox = new VBox(6.0, bottom, progress);

        ScrollPane accordionScroll = new ScrollPane(accordion);
        accordionScroll.setFitToWidth(true);
        accordionScroll.setPrefViewportHeight(330.0);
        accordionScroll.setPrefHeight(360.0);
        accordionScroll.setMinHeight(250.0);
        accordionScroll.setMaxHeight(360.0);
        accordionScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;"
                + " -fx-border-color: transparent;");

        VBox card = new VBox(10.0, versionRow, accordionScroll, bottomBox);
        card.setPadding(new Insets(14.0));
        card.setMaxWidth(780.0);
        card.setMaxHeight(Double.MAX_VALUE);
        card.getStyleClass().add("neko-card");

        // 默认展开 Fabric，但必须等「用户显式点了某个分类」之后再判断：
        // 否则先同步展开用户点的那个、再被这里的 runLater 覆盖回 Fabric
        // —— 这就是「点 Forge 却展开 Fabric」的元凶。
        Platform.runLater(() -> {
            if (expandedLoader == null) {
                expand("Fabric");
            }
        });
        panel.getChildren().addAll(top, card);
        return panel;
    }

    // ==================== 对外状态 ====================

    public MinecraftVersion selectedGame() {
        return versionCombo.getValue();
    }

    public String instanceName() {
        String text = nameField.getText();
        return text == null || text.isBlank() ? null : text.trim();
    }

    public void setInstanceName(String name) {
        nameField.setText(name == null ? "" : name);
    }

    public String expandedLoader() {
        return expandedLoader;
    }

    public String selectedLoaderVersion(String loader) {
        return selectedVersions.get(loader);
    }

    public void setStatus(String text) {
        status.setText(text);
    }

    /** 安装进行中：显示不确定进度条。 */
    public void setBusy(boolean running) {
        progress.setVisible(running);
        progress.setManaged(running);
        progress.setProgress(running ? -1.0 : 0.0);
    }

    /** 填充可选游戏版本（默认只列正式版；当前选中项即使不是正式版也要保留）。 */
    public void setGameVersions(List<MinecraftVersion> versions, MinecraftVersion selected) {
        List<MinecraftVersion> items = new java.util.ArrayList<>(versions);
        if (selected != null && items.stream().noneMatch(v -> v.getId().equals(selected.getId()))) {
            items.add(0, selected);
        }
        versionCombo.getItems().setAll(items);
        if (selected != null) {
            versionCombo.setValue(selected);
        }
    }

    public boolean hasGameVersions() {
        return !versionCombo.getItems().isEmpty();
    }

    public void selectFirstGameVersion() {
        if (!versionCombo.getItems().isEmpty()) {
            versionCombo.setValue(versionCombo.getItems().get(0));
        }
    }

    /** 打开安装器页并展开指定加载器（找不到就保持当前展开项）。 */
    public void showFor(String loader) {
        updateTitle();
        if (loader != null && rows.containsKey(loader)) {
            expand(loader);
        } else if (expandedLoader == null) {
            expand("Fabric");
        }
    }

    public void updateTitle() {
        MinecraftVersion game = versionCombo.getValue();
        title.setText(game == null ? "安装新实例" : "安装 Minecraft " + game.getId());
    }

    /** 切换游戏版本：之前选的加载器版本作废，并重新预检各加载器是否适配。 */
    public void onGameVersionChanged() {
        selectedVersions.clear();
        updateTitle();
        checkLoaderSupport();
        if (expandedLoader != null) {
            loadVersions(expandedLoader);
        }
    }

    /**
     * 预检：哪些加载器根本没有这个 MC 版本的构建。
     *
     * <p>不点开也能看出来——没构建的加载器置灰并在副标题写明，省得用户点进去等一次
     * 注定失败的请求。
     */
    private void checkLoaderSupport() {
        MinecraftVersion game = versionCombo.getValue();
        if (game == null) {
            return;
        }
        String mc = game.getId();
        for (String loader : new String[]{"Fabric", "Forge", "NeoForge", "Quilt"}) {
            LoaderRow row = rows.get(loader);
            if (row == null) {
                continue;
            }
            row.subtitle.setText("正在检查 " + loader + " 是否支持 " + mc + "…");
            LoaderInstaller.listLoaderVersions(loader, mc)
                    .thenAccept(versions -> Platform.runLater(() -> {
                        MinecraftVersion current = versionCombo.getValue();
                        if (current == null || !mc.equals(current.getId())) {
                            return;
                        }
                        boolean supported = versions != null && !versions.isEmpty();
                        applySupport(loader, supported, mc);
                    }));
        }
    }

    /** 把预检结果画到加载器行上。 */
    private void applySupport(String loader, boolean supported, String mc) {
        LoaderRow row = rows.get(loader);
        if (row == null) {
            return;
        }
        if (supported) {
            row.subtitle.setText("");
            row.header.setOpacity(1.0);
            row.header.setDisable(false);
        } else {
            row.subtitle.setText("没有支持 " + mc + " 的 " + loader + " 构建");
            row.header.setOpacity(0.55);
            row.header.setDisable(false);
        }
    }

    // ==================== 手风琴 ====================

    private void expand(String name) {
        LoaderRow target = rows.get(name);
        if (target == null) {
            return;
        }
        expandedLoader = name;
        for (Map.Entry<String, LoaderRow> entry : rows.entrySet()) {
            boolean open = entry.getKey().equals(name);
            LoaderRow row = entry.getValue();
            row.body.setVisible(open);
            row.body.setManaged(open);
            row.header.setStyle(open
                    ? "-fx-background-color: -app-hover-2; -fx-background-radius: 10 10 0 0;"
                    : "-fx-background-color: transparent; -fx-background-radius: 10;");
            row.arrow.setText(open ? "▴" : "▾");
        }
        loadVersions(name);
    }

    private void loadVersions(String name) {
        LoaderRow row = rows.get(name);
        if (row == null) {
            return;
        }
        row.body.getChildren().clear();
        MinecraftVersion game = versionCombo.getValue();
        if (game == null) {
            row.body.getChildren().setAll(message("先选择 Minecraft 版本"));
            row.subtitle.setText("等待游戏版本");
            return;
        }
        if ("原版".equals(name)) {
            row.body.getChildren().setAll(message("纯原版不需要加载器，直接开始下载即可。"));
            row.subtitle.setText("纯原版");
            return;
        }
        if ("OptiFine".equals(name) || "LabyMod".equals(name)) {
            row.body.getChildren().setAll(message("官网安装器，点「开始下载」会打开官网。"));
            row.subtitle.setText("官网安装器");
            return;
        }
        row.body.getChildren().setAll(message("正在读取 " + name + " 版本…"));
        String mc = game.getId();
        LoaderInstaller.listLoaderVersions(name, mc)
                .thenAccept(versions -> Platform.runLater(() -> {
                    if (!name.equals(expandedLoader)) {
                        return;
                    }
                    if (versionCombo.getValue() != game) {
                        return;
                    }
                    String selected = selectedVersions.get(name);
                    VBox list = new VBox(4.0);
                    list.getChildren().add(versionRow(name, null, "自动（最新）", selected == null));
                    for (String version : versions) {
                        list.getChildren().add(
                                versionRow(name, version, version, version.equals(selected)));
                    }
                    row.body.getChildren().setAll(bodyScroll(list));
                    row.subtitle.setText(selected == null ? "自动（最新）" : selected);
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        if (!name.equals(expandedLoader)) {
                            return;
                        }
                        row.body.getChildren().setAll(message("读取版本失败：" + ex.getMessage()));
                    });
                    return null;
                });
    }

    private HBox versionRow(String loader, String version, String text, boolean selected) {
        HBox row = new HBox(8.0);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(7.0, 10.0, 7.0, 10.0));
        row.setCursor(Cursor.HAND);
        row.setUserData(version);
        Label mark = new Label(selected ? "●" : "○");
        mark.setStyle("-fx-text-fill: " + (selected ? "-app-accent" : "-app-text-dim-3")
                + "; -fx-font-size: 11px;");
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: -app-text-2; -fx-font-size: 12px;");
        row.getChildren().addAll(mark, label);
        row.setStyle(rowStyle(selected));
        row.setOnMouseClicked(e -> {
            selectedVersions.put(loader, version);
            LoaderRow loaderRow = rows.get(loader);
            if (loaderRow != null) {
                loaderRow.subtitle.setText(version == null ? "自动（最新）" : version);
            }
            Parent parent = row.getParent();
            if (parent instanceof VBox box) {
                for (Node node : box.getChildren()) {
                    if (!(node instanceof HBox other)) {
                        continue;
                    }
                    boolean isSelected = Objects.equals(other.getUserData(), version);
                    other.setStyle(rowStyle(isSelected));
                    Node first = other.getChildren().isEmpty() ? null : other.getChildren().get(0);
                    if (first instanceof Label dot) {
                        dot.setText(isSelected ? "●" : "○");
                        dot.setStyle("-fx-text-fill: "
                                + (isSelected ? "-app-accent" : "-app-text-dim-3")
                                + "; -fx-font-size: 11px;");
                    }
                }
            }
        });
        return row;
    }

    private VBox createLoaderRow(String name) {
        VBox holder = new VBox(0.0);
        holder.setStyle("-fx-background-color: -app-row; -fx-background-radius: 10;"
                + " -fx-border-color: -app-border-soft; -fx-border-radius: 10;");
        HBox header = new HBox(10.0);
        header.setPadding(new Insets(8.0, 12.0, 8.0, 12.0));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setCursor(Cursor.HAND);
        Label rowTitle = new Label(name);
        rowTitle.setStyle("-fx-text-fill: -app-text-2; -fx-font-size: 13px; -fx-font-weight: bold;");
        Label subtitle = new Label("选择版本");
        subtitle.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11px;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label arrow = new Label("▾");
        arrow.setStyle("-fx-text-fill: -app-text-dim; -fx-font-size: 12px;");
        header.getChildren().addAll(rowTitle, subtitle, spacer, arrow);
        header.setOnMouseClicked(e -> expand(name));
        VBox body = new VBox(4.0);
        body.setPadding(new Insets(2.0, 10.0, 10.0, 10.0));
        body.setVisible(false);
        body.setManaged(false);
        holder.getChildren().addAll(header, body);
        rows.put(name, new LoaderRow(header, body, subtitle, arrow));
        return holder;
    }

    private ScrollPane bodyScroll(Node content) {
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(190.0);
        scroll.setMinHeight(190.0);
        scroll.setMaxHeight(190.0);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;"
                + " -fx-border-color: transparent;");
        return scroll;
    }

    private static String rowStyle(boolean selected) {
        return "-fx-background-color: " + (selected ? "-app-hover-2" : "transparent")
                + "; -fx-background-radius: 7; -fx-border-color: transparent;";
    }

    private static Label message(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11.5px; -fx-padding: 8 10;");
        label.setWrapText(true);
        return label;
    }

    private static Label label(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 12px;");
        return label;
    }
}
