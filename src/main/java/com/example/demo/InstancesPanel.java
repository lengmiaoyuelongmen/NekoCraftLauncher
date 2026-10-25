package com.example.demo;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 实例选择页（对标 PCL）：左侧文件夹列表，右侧搜索 + 按加载器分组的实例卡片。
 * 单击实例即选中并返回启动页。
 */
public final class InstancesPanel extends HBox {

    private File gameDir;
    private final Consumer<Instance> onSelect;
    private final Consumer<String> log;
    private final LauncherConfig config;
    private final Consumer<File> onSwitchDir;
    private final Consumer<Instance> onDelete;
    private final java.util.function.BiConsumer<Instance, Boolean> onClone;
    private final Consumer<Instance> onCheck;
    private final Consumer<Instance> onSettings;
    private final Runnable onImport;
    private final java.util.function.Consumer<Instance> onExport;
    private final Runnable onBack;
    private final VBox side = new VBox(0);

    private final VBox groupBox = new VBox(10);
    private final TextField searchField = new TextField();
    private final Label emptyHint = new Label();
    private String selectedId;
    private final Map<String, Boolean> expanded = new LinkedHashMap<>();
    private boolean favoritesOnly;

    public InstancesPanel(File gameDir, String selectedId, Consumer<Instance> onSelect, Consumer<String> log,
                          LauncherConfig config, Consumer<File> onSwitchDir,
                          Consumer<Instance> onDelete,
                          java.util.function.BiConsumer<Instance, Boolean> onClone,
                          Consumer<Instance> onCheck, Consumer<Instance> onSettings,
                          Runnable onImport, java.util.function.Consumer<Instance> onExport, Runnable onBack) {
        this.gameDir = gameDir;
        this.selectedId = selectedId;
        this.onSelect = onSelect;
        this.log = log;
        this.config = config;
        this.onSwitchDir = onSwitchDir;
        this.onDelete = onDelete;
        this.onClone = onClone;
        this.onCheck = onCheck;
        this.onSettings = onSettings;
        this.onImport = onImport;
        this.onExport = onExport;
        this.onBack = onBack;

        setSpacing(0);
        setStyle("-fx-background-color: transparent;");

        // ---------- 左侧 ----------
        side.setPrefWidth(196);
        side.setMinWidth(196);
        side.setStyle("-fx-background-color: -app-nav-2; -fx-border-color: transparent -app-row3 transparent transparent;");
        side.setPadding(new Insets(16, 0, 0, 0));
        renderSide();

        // ---------- 右侧 ----------
        VBox main = new VBox(12);
        main.setPadding(new Insets(16, 20, 16, 20));
        HBox.setHgrow(main, Priority.ALWAYS);

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button favorites = new Button("★ 只看收藏");
        favorites.setStyle("-fx-background-color: -app-row3; -fx-text-fill: -app-text-2; -fx-background-radius: 8;"
                + " -fx-padding: 6 14; -fx-cursor: hand; -fx-font-size: 12px;");
        favorites.setOnAction(e -> {
            favoritesOnly = !favoritesOnly;
            favorites.setStyle("-fx-background-color: " + (favoritesOnly ? "#9ca3af" : "-app-row3")
                    + "; -fx-text-fill: #111827; -fx-background-radius: 8; -fx-padding: 6 14;"
                    + " -fx-cursor: hand; -fx-font-size: 12px;");
            render();
        });
        Button refresh = new Button("↻ 刷新");
        refresh.setStyle("-fx-background-color: -app-row3; -fx-text-fill: -app-text-2; -fx-background-radius: 8;"
                + " -fx-padding: 6 14; -fx-cursor: hand; -fx-font-size: 12px;");
        refresh.setOnAction(e -> reload());

        searchField.setPromptText("搜索游戏实例");
        searchField.setStyle("-fx-background-color: -app-input; -fx-text-fill: -app-text;"
                + " -fx-prompt-text-fill: -app-text-muted; -fx-background-radius: 9;");
        searchField.setPrefHeight(38);
        HBox.setHgrow(searchField, Priority.ALWAYS);
        searchField.textProperty().addListener((obs, old, value) -> render());
        header.getChildren().addAll(searchField, favorites, refresh);

        emptyHint.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 13px;");
        emptyHint.setWrapText(true);

        ScrollPane scroll = new ScrollPane(groupBox);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        groupBox.setFillWidth(true);

        ModuleHeader instancesHeader = new ModuleHeader("05 / INSTANCES", "实例选择",
                "每个实例独立的 mods / config / saves", "layers", ModuleHeader.Badge.STACK, "版本隔离");
        main.getChildren().addAll(instancesHeader, header, emptyHint, scroll);
        getChildren().addAll(side, main);

        reload();
    }

    public void setSelectedId(String id) {
        this.selectedId = id;
        render();
    }

    private void renderSide() {
        side.getChildren().clear();
        Label back = new Label("‹ 返回启动页");
        back.setStyle("-fx-text-fill: -app-text-dim; -fx-font-size: 12px; -fx-cursor: hand;"
                + " -fx-padding: 2 18 10 18;");
        back.setOnMouseEntered(e -> back.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 12px;"
                + " -fx-cursor: hand; -fx-padding: 2 18 10 18;"));
        back.setOnMouseExited(e -> back.setStyle("-fx-text-fill: -app-text-dim; -fx-font-size: 12px;"
                + " -fx-cursor: hand; -fx-padding: 2 18 10 18;"));
        back.setOnMouseClicked(e -> {
            if (onBack != null) onBack.run();
        });
        side.getChildren().add(back);
        side.getChildren().add(sectionLabel("文件夹列表"));
        File active = gameDir.getAbsoluteFile();
        List<File> all = new ArrayList<>();
        java.util.Set<String> seen = new java.util.LinkedHashSet<>();
        for (String path : config.getGameDirs()) {
            File dir = new File(path).getAbsoluteFile();
            if (seen.add(dir.getPath())) all.add(dir);
        }
        if (seen.add(active.getPath())) all.add(0, active);
        for (File dir : all) {
            boolean isActive = dir.equals(active);
            HBox item = sideItem(displayName(dir), "▣", isActive, () -> switchTo(dir));
            attachDirMenu(item, dir, isActive);
            side.getChildren().add(item);
            if (isActive) {
                Label pathLabel = new Label(dir.getAbsolutePath());
                pathLabel.setStyle("-fx-text-fill: -app-text-dim-3; -fx-font-size: 10px;");
                pathLabel.setWrapText(true);
                pathLabel.setPadding(new Insets(2, 18, 8, 30));
                side.getChildren().add(pathLabel);
            }
        }
        side.getChildren().add(sectionLabel("添加或导入"));
        side.getChildren().add(sideItem("添加已有文件夹", "＋", false, this::browseFolder));
        side.getChildren().add(sideItem("导入整合包", "⇩", false, () -> {
            if (onImport != null) onImport.run();
        }));
        side.getChildren().add(sideItem("导出整合包", "⇧", false, this::exportSelected));
    }

    private void exportSelected() {
        if (selectedId == null || selectedId.isBlank()) {
            Toast.info("先选择一个实例，再导出整合包");
            return;
        }
        if (onExport != null) onExport.accept(Instance.of(gameDir, selectedId));
    }

    /** 目录右键菜单：重命名 / 移除。 */
    private void attachDirMenu(HBox item, File dir, boolean active) {
        javafx.scene.control.ContextMenu menu = new javafx.scene.control.ContextMenu();
        javafx.scene.control.MenuItem rename = new javafx.scene.control.MenuItem("重命名");
        rename.setOnAction(e -> renameDir(dir));
        javafx.scene.control.MenuItem remove = new javafx.scene.control.MenuItem("从列表移除");
        remove.setOnAction(e -> removeDir(dir, active));
        menu.getItems().addAll(rename, remove);
        item.setOnContextMenuRequested(e -> menu.show(item, e.getScreenX(), e.getScreenY()));
    }

    private void renameDir(File dir) {
        javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog(
                config.gameDirName(dir.getAbsolutePath()));
        dialog.setTitle("重命名目录");
        dialog.setHeaderText("给「" + displayName(dir) + "」起一个显示名");
        dialog.setContentText("显示名");
        dialog.showAndWait().ifPresent(name -> {
            config.setGameDirName(dir.getAbsolutePath(), name);
            config.save();
            renderSide();
            log.accept("✏️ 已重命名目录: " + name);
        });
    }

    private void removeDir(File dir, boolean active) {
        List<String> others = config.getGameDirs().stream()
                .filter(path -> !new File(path).getAbsoluteFile().equals(dir.getAbsoluteFile()))
                .toList();
        if (others.isEmpty()) {
            log.accept("ℹ 至少要保留一个游戏目录");
            return;
        }
        if (active) {
            switchTo(new File(others.get(0)));
        }
        String stored = config.getGameDirs().stream()
                .filter(path -> new File(path).getAbsoluteFile().equals(dir.getAbsoluteFile()))
                .findFirst().orElse(dir.getAbsolutePath());
        config.removeGameDir(stored);
        config.save();
        renderSide();
        log.accept("🗑️ 已从列表移除目录: " + dir.getAbsolutePath());
    }

    private String displayName(File dir) {
        String alias = config.gameDirName(dir.getAbsolutePath());
        if (alias != null && !alias.isBlank()) return alias;
        String name = dir.getName();
        if (name == null || name.isBlank()) return dir.getAbsolutePath();
        return name;
    }

    private void switchTo(File dir) {
        if (dir == null || dir.getAbsoluteFile().equals(gameDir.getAbsoluteFile())) return;
        if (!dir.isDirectory() && !dir.mkdirs()) {
            log.accept("❌ 无法使用目录: " + dir.getAbsolutePath());
            return;
        }
        gameDir = dir;
        if (onSwitchDir != null) onSwitchDir.accept(dir);
        expanded.clear();
        renderSide();
        reload();
        log.accept("📁 已切换游戏目录: " + dir.getAbsolutePath());
    }

    private void browseFolder() {
        javafx.stage.DirectoryChooser chooser = new javafx.stage.DirectoryChooser();
        chooser.setTitle("选择 .minecraft 目录");
        File chosen = chooser.showDialog(getScene() == null ? null : getScene().getWindow());
        if (chosen != null) switchTo(chosen);
    }

    private void reload() {
        expanded.clear();
        for (Instance instance : Instance.list(gameDir)) {
            expanded.putIfAbsent(instance.loader, true);
        }
        render();
    }

    private void render() {
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        List<Instance> all = Instance.list(gameDir);

        Map<String, List<Instance>> groups = new LinkedHashMap<>();
        for (Instance instance : all) {
            if (favoritesOnly && !InstanceMeta.isFavorite(gameDir, instance.id)) continue;
            if (!query.isEmpty()
                    && !instance.id.toLowerCase().contains(query)
                    && !instance.mcVersion.toLowerCase().contains(query)
                    && !instance.loader.toLowerCase().contains(query)) {
                continue;
            }
            groups.computeIfAbsent(instance.loader, k -> new ArrayList<>()).add(instance);
        }

        groupBox.getChildren().clear();
        if (groups.isEmpty()) {
            emptyHint.setText(all.isEmpty()
                    ? "还没有实例。去「下载」页装一个版本，或用 Forge/Fabric 安装一个加载器。"
                    : "没有匹配的实例。");
            return;
        }
        emptyHint.setText("共 " + all.size() + " 个实例");

        for (Map.Entry<String, List<Instance>> entry : groups.entrySet()) {
            groupBox.getChildren().add(groupCard(entry.getKey(), entry.getValue()));
        }
    }

    private VBox groupCard(String loader, List<Instance> instances) {
        VBox card = new VBox(0);
        card.setStyle("-fx-background-color: -app-card; -fx-background-radius: 10; -fx-border-color: -app-border; -fx-border-radius: 10;");

        boolean isOpen = expanded.getOrDefault(loader, true);
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10, 14, 10, 14));
        header.setCursor(Cursor.HAND);

        Label name = new Label(loader + " 实例 (" + instances.size() + ")");
        name.setStyle("-fx-text-fill: -app-text-2; -fx-font-size: 13px; -fx-font-weight: bold;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        javafx.scene.layout.StackPane arrow = Icons.node(isOpen ? "chevron-down" : "chevron-right", 12, "#7b818b");
        header.getChildren().addAll(name, spacer, arrow);

        VBox body = new VBox(2);
        body.setPadding(new Insets(0, 8, 8, 8));
        body.setVisible(isOpen);
        body.setManaged(isOpen);
        for (Instance instance : instances) {
            body.getChildren().add(row(instance));
        }

        header.setOnMouseClicked(e -> {
            expanded.put(loader, !isOpen);
            render();
        });

        card.getChildren().addAll(header, body);
        return card;
    }

    private HBox row(Instance instance) {
        boolean selected = instance.id.equals(selectedId);

        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(9, 12, 9, 12));
        row.setCursor(Cursor.HAND);
        row.setStyle("-fx-background-color: " + (selected ? "-app-hover-3" : "transparent") + "; -fx-background-radius: 8;");

        Rectangle indicator = new Rectangle(3, 22, Color.web(selected ? "#9ca3af" : "transparent"));
        indicator.setArcWidth(3);
        indicator.setArcHeight(3);

        StackPane icon = new StackPane();
        icon.setPrefSize(34, 34);
        icon.setMinSize(34, 34);
        File instanceIcon = new File(new File(new File(gameDir, "versions"), instance.id), "icon.png");
        if (instanceIcon.isFile()) {
            javafx.scene.image.Image image = new javafx.scene.image.Image(
                    instanceIcon.toURI().toString(), 34, 34, true, true);
            javafx.scene.image.ImageView iconView = new javafx.scene.image.ImageView(image);
            iconView.setFitWidth(34);
            iconView.setFitHeight(34);
            Rectangle iconClip = new Rectangle(34, 34);
            iconClip.setArcWidth(14);
            iconClip.setArcHeight(14);
            iconView.setClip(iconClip);
            icon.getChildren().add(iconView);
        } else {
            icon.setStyle("-fx-background-color: " + loaderColor(instance.loader) + "; -fx-background-radius: 7;");
            Label iconText = new Label(instance.loader.substring(0, 1));
            iconText.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
            icon.getChildren().add(iconText);
        }

        VBox info = new VBox(2);
        HBox.setHgrow(info, Priority.ALWAYS);
        boolean favorite = InstanceMeta.isFavorite(gameDir, instance.id);
        javafx.scene.layout.StackPane star = Icons.node("star", 14, favorite ? "#eab308" : "#4b5563");
        star.setMouseTransparent(false);
        star.setCursor(javafx.scene.Cursor.HAND);
        star.setOnMouseClicked(e -> {
            InstanceMeta.setFavorite(gameDir, instance.id, !InstanceMeta.isFavorite(gameDir, instance.id));
            render();
            e.consume();
        });

        Label title = new Label(InstanceMeta.name(gameDir, instance.id));
        title.setStyle("-fx-text-fill: " + (selected ? "-app-text" : "-app-text-2") + "; -fx-font-size: 13px;");
        long played = Playtime.seconds(gameDir, instance.id);
        Label sub = new Label(instance.subtitle()
                + (played > 0 ? " · 已玩 " + Playtime.format(played) : ""));
        sub.setStyle("-fx-text-fill: -app-text-dim-3; -fx-font-size: 11px;");
        info.getChildren().addAll(title, sub);

        Label tag = new Label(selected ? "当前" : "");
        tag.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 11px;");

        row.getChildren().addAll(indicator, icon, info, star, tag);
        Motion.hoverLift(row);
        row.setOnMouseEntered(e -> {
            if (!instance.id.equals(selectedId)) {
                row.setStyle("-fx-background-color: -app-hover; -fx-background-radius: 8;");
            }
        });
        row.setOnMouseExited(e -> {
            if (!instance.id.equals(selectedId)) {
                row.setStyle("-fx-background-color: transparent; -fx-background-radius: 8;");
            }
        });
        row.setOnMouseClicked(e -> {
            if (e.getButton() != javafx.scene.input.MouseButton.PRIMARY) return;
            if (e.getClickCount() == 2 && onSettings != null) {
                onSettings.accept(instance);
                return;
            }
            selectedId = instance.id;
            render();
            if (onSelect != null) onSelect.accept(instance);
        });
        attachInstanceMenu(row, instance);
        return row;
    }

    /** 实例右键菜单：打开目录 / 复制 ID / 打开日志 / 删除。 */
    private void attachInstanceMenu(HBox row, Instance instance) {
        javafx.scene.control.ContextMenu menu = new javafx.scene.control.ContextMenu();

        javafx.scene.control.MenuItem openFolder = new javafx.scene.control.MenuItem("打开实例文件夹");
        openFolder.setOnAction(e -> openInstanceFolder(instance, false));
        javafx.scene.control.MenuItem openLogs = new javafx.scene.control.MenuItem("打开日志目录");
        openLogs.setOnAction(e -> openInstanceFolder(instance, true));
        javafx.scene.control.MenuItem copyId = new javafx.scene.control.MenuItem("复制实例 ID");
        copyId.setOnAction(e -> {
            javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
            content.putString(instance.id);
            javafx.scene.input.Clipboard.getSystemClipboard().setContent(content);
            Toast.ok("已复制实例 ID: " + instance.id);
        });
        javafx.scene.control.MenuItem delete = new javafx.scene.control.MenuItem("删除实例");
        delete.setOnAction(e -> confirmDelete(instance));

        javafx.scene.control.MenuItem settings = new javafx.scene.control.MenuItem("实例设置");
        settings.setOnAction(e -> {
            if (onSettings != null) onSettings.accept(instance);
        });

        javafx.scene.control.MenuItem check = new javafx.scene.control.MenuItem("检查实例完整性");
        check.setOnAction(e -> {
            if (onCheck != null) onCheck.accept(instance);
        });

        javafx.scene.control.MenuItem cloneLight = new javafx.scene.control.MenuItem("克隆实例（仅版本）");
        cloneLight.setOnAction(e -> {
            if (onClone != null) onClone.accept(instance, false);
        });
        javafx.scene.control.MenuItem cloneFull = new javafx.scene.control.MenuItem("克隆实例（含 mods / 配置）");
        cloneFull.setOnAction(e -> {
            if (onClone != null) onClone.accept(instance, true);
        });

        menu.getItems().addAll(settings, openFolder, openLogs, copyId, check, cloneLight, cloneFull,
                new javafx.scene.control.SeparatorMenuItem(), delete);
        row.setOnContextMenuRequested(e -> menu.show(row, e.getScreenX(), e.getScreenY()));
    }

    private void openInstanceFolder(Instance instance, boolean logs) {
        File root = InstanceDirs.root(gameDir, instance.id, config.isIsolation());
        File dir = logs ? new File(root, "logs") : root;
        try {
            if (!dir.isDirectory()) dir.mkdirs();
            java.awt.Desktop.getDesktop().open(dir);
        } catch (Exception e) {
            log.accept("❌ 打开目录失败: " + e.getMessage());
        }
    }

    private void confirmDelete(Instance instance) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.CONFIRMATION);
        alert.setTitle("删除实例");
        alert.setHeaderText("确定删除实例「" + InstanceMeta.name(gameDir, instance.id) + "」？");
        alert.setContentText("会删除 versions/" + instance.id + " 整个目录，不可恢复。");
        if (alert.showAndWait().orElse(javafx.scene.control.ButtonType.CANCEL)
                == javafx.scene.control.ButtonType.OK) {
            if (onDelete != null) onDelete.accept(instance);
            Toast.info("正在删除实例 " + instance.id);
        }
    }

    private String loaderColor(String loader) {
        return switch (loader) {
            case "Fabric" -> "#c8a06a";
            case "Forge" -> "#5b7fa6";
            case "NeoForge" -> "#b06a5b";
            case "Quilt" -> "#9a6ab0";
            default -> "-app-text-dim-2";
        };
    }

    private Label sectionLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11px; -fx-font-weight: bold;");
        label.setPadding(new Insets(4, 18, 6, 18));
        return label;
    }

    private HBox sideItem(String text, String iconText, boolean active, Runnable action) {
        HBox item = new HBox(8);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(8, 18, 8, 22));
        item.setCursor(Cursor.HAND);
        item.setStyle("-fx-background-color: " + (active ? "-app-card" : "transparent") + ";");

        Rectangle bar = new Rectangle(2, 14, Color.web(active ? "#9ca3af" : "transparent"));
        Label icon = new Label(iconText);
        icon.setStyle("-fx-text-fill: " + (active ? "#d1d5db" : "-app-text-muted") + "; -fx-font-size: 11px;");
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: " + (active ? "-app-text-2" : "-app-text-dim") + "; -fx-font-size: 12px;");
        item.getChildren().addAll(bar, icon, label);

        if (!active) {
            item.setOnMouseEntered(e -> item.setStyle("-fx-background-color: -app-hover-2;"));
            item.setOnMouseExited(e -> item.setStyle("-fx-background-color: transparent;"));
            item.setOnMouseClicked(e -> action.run());
        }
        return item;
    }
}
