package com.example.demo;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.io.File;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.function.Consumer;

/** 世界（存档）页：列出当前实例的存档，支持打开目录。 */
public final class WorldsPanel extends VBox {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")
            .withZone(ZoneId.systemDefault());

    private final File savesDir;
    private final Consumer<String> log;
    private final VBox list = new VBox(6);
    private final Label summary = new Label();

    public WorldsPanel(File savesDir, Consumer<String> log) {
        this.savesDir = savesDir;
        this.log = log;

        setSpacing(12);
        setPadding(new Insets(22, 24, 22, 24));

        Label title = new Label("世界");
        title.setStyle("-fx-text-fill: -app-text; -fx-font-size: 22px; -fx-font-weight: bold;");
        Label subtitle = new Label("当前实例的存档（版本隔离时每个实例独立）");
        subtitle.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 12px;");

        summary.setStyle("-fx-text-fill: -app-text-dim; -fx-font-size: 12px;");

        Button open = new Button("打开存档目录");
        open.setStyle("-fx-background-color: -app-row3; -fx-text-fill: -app-text-2; -fx-background-radius: 8;"
                + " -fx-padding: 7 14; -fx-cursor: hand; -fx-font-size: 12px;");
        open.setOnAction(e -> openDir());

        Button refresh = new Button("刷新");
        refresh.setStyle("-fx-background-color: -app-row3; -fx-text-fill: -app-text-2; -fx-background-radius: 8;"
                + " -fx-padding: 7 14; -fx-cursor: hand; -fx-font-size: 12px;");
        refresh.setOnAction(e -> refresh());

        HBox actions = new HBox(10, open, refresh);
        actions.setAlignment(Pos.CENTER_LEFT);

        ScrollPane scroll = new ScrollPane(list);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        getChildren().addAll(title, subtitle, summary, actions, scroll);
        refresh();
    }

    public void refresh() {
        list.getChildren().clear();
        File[] worlds = savesDir.listFiles(File::isDirectory);
        if (worlds == null) worlds = new File[0];
        java.util.Arrays.sort(worlds, Comparator.comparingLong(File::lastModified).reversed());
        summary.setText("共 " + worlds.length + " 个存档 · " + savesDir.getAbsolutePath());
        if (worlds.length == 0) {
            Label empty = new Label("还没有存档。进游戏创建一个世界后就会出现在这里。");
            empty.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 12px;");
            empty.setWrapText(true);
            list.getChildren().add(empty);
            return;
        }
        for (File world : worlds) {
            list.getChildren().add(row(world));
        }
    }

    private HBox row(File world) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 12, 10, 12));
        row.setStyle("-fx-background-color: -app-card; -fx-background-radius: 8;");

        VBox info = new VBox(3);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label name = new Label(world.getName());
        name.setStyle("-fx-text-fill: -app-text-2; -fx-font-size: 13px;");
        Label meta = new Label("最后修改 " + TIME.format(Instant.ofEpochMilli(world.lastModified()))
                + " · " + folderSizeMb(world) + " MB");
        meta.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11px;");
        info.getChildren().addAll(name, meta);

        Button open = new Button("打开");
        open.setStyle("-fx-background-color: #333; -fx-text-fill: #ccc; -fx-cursor: hand;"
                + " -fx-background-radius: 6; -fx-padding: 4 12; -fx-font-size: 12px;");
        open.setOnAction(e -> {
            try {
                java.awt.Desktop.getDesktop().open(world);
            } catch (Exception ex) {
                log("❌ 打开失败: " + ex.getMessage());
            }
        });

        row.getChildren().addAll(info, open);
        return row;
    }

    private String folderSizeMb(File dir) {
        long total = 0;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) total += file.length();
            }
        }
        return String.valueOf(total / 1024 / 1024);
    }

    private void openDir() {
        try {
            if (!savesDir.isDirectory() && !savesDir.mkdirs()) return;
            java.awt.Desktop.getDesktop().open(savesDir);
        } catch (Exception e) {
            log("❌ 打开目录失败: " + e.getMessage());
        }
    }

    private void log(String message) {
        if (log != null) log.accept(message);
    }
}
