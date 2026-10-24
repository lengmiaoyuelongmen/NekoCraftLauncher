package com.example.demo;

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
import javafx.scene.layout.VBox;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/** 日志页（对标 PCL 设置里的「查看日志」）：列出日志、导出、清理、打开目录。 */
public final class LogsPanel extends VBox {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final File gameDir;
    private final Consumer<String> log;
    private final VBox list = new VBox(6);
    private final Label summary = new Label();
    private final TextField filterField = new TextField();
    private final List<File> files = new ArrayList<>();

    public LogsPanel(File gameDir, Consumer<String> log) {
        this.gameDir = gameDir;
        this.log = log;

        setSpacing(12);
        setPadding(new Insets(22, 24, 22, 24));

        Label title = new Label("日志");
        title.setStyle("-fx-text-fill: -app-text; -fx-font-size: 22px; -fx-font-weight: bold;");
        Label subtitle = new Label("游戏日志与启动器日志，用于排查崩溃和启动失败");
        subtitle.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 12px;");

        summary.setStyle("-fx-text-fill: -app-text-dim; -fx-font-size: 12px;");

        Button openLogs = ghost("打开日志目录");
        openLogs.setOnAction(e -> openDir(new File(gameDir, "logs")));
        Button openGame = ghost("打开游戏目录");
        openGame.setOnAction(e -> openDir(gameDir));
        Button exportAll = ghost("导出全部日志");
        exportAll.setOnAction(e -> exportAll());
        Button clear = ghost("清理历史日志");
        clear.setStyle("-fx-background-color: #7f1d1d; -fx-text-fill: #fecaca; -fx-background-radius: 8;"
                + " -fx-padding: 7 14; -fx-cursor: hand; -fx-font-size: 12px;");
        clear.setOnAction(e -> clearLogs());

        filterField.setPromptText("筛选日志（文件名 / 目录）");
        filterField.setPrefWidth(220);
        filterField.textProperty().addListener((obs, old, value) -> render());
        Button refresh = ghost("刷新");
        refresh.setOnAction(e -> refresh());

        HBox actions = new HBox(10, filterField, refresh, openLogs, openGame, exportAll, clear);
        actions.setAlignment(Pos.CENTER_LEFT);

        ScrollPane scroll = new ScrollPane(list);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        getChildren().addAll(title, subtitle, summary, actions, scroll);
        refresh();
    }

    /** 重新扫描日志目录。 */
    public void refresh() {
        files.clear();
        collect(files, new File(gameDir, "logs"));
        if (gameDir.getParentFile() != null) {
            // 版本隔离时，实例日志在 versions/<id>/logs
            File versions = new File(gameDir, "versions");
            File[] children = versions.listFiles(File::isDirectory);
            if (children != null) {
                for (File child : children) collect(files, new File(child, "logs"));
            }
        }
        File launcherLog = new File("launcher.log");
        if (launcherLog.isFile()) files.add(launcherLog);

        files.sort(Comparator.comparingLong(File::lastModified).reversed());
        render();
    }

    /** 按筛选框过滤后渲染列表。 */
    private void render() {
        list.getChildren().clear();
        String query = filterField.getText() == null ? "" : filterField.getText().trim().toLowerCase();
        List<File> shown = new ArrayList<>();
        for (File file : files) {
            boolean match = query.isEmpty()
                    || file.getName().toLowerCase().contains(query)
                    || (file.getParent() != null && file.getParent().toLowerCase().contains(query));
            if (match) shown.add(file);
        }
        summary.setText(shown.size() == files.size()
                ? "共 " + files.size() + " 个日志文件"
                : "共 " + files.size() + " 个 · 匹配 " + shown.size() + " 个");

        if (shown.isEmpty()) {
            Label empty = new Label(files.isEmpty()
                    ? "还没有日志。启动一次游戏后这里会出现 latest.log 等记录。"
                    : "没有匹配「" + filterField.getText() + "」的日志。");
            empty.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 12px;");
            empty.setWrapText(true);
            list.getChildren().add(empty);
            return;
        }
        for (int i = 0; i < Math.min(shown.size(), 80); i++) {
            list.getChildren().add(row(shown.get(i)));
        }
    }

    private HBox row(File file) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(9, 12, 9, 12));
        row.getStyleClass().add("log-row");

        VBox info = new VBox(3);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label name = new Label(file.getName());
        name.setStyle("-fx-text-fill: -app-text-2; -fx-font-size: 13px;");
        Label meta = new Label(TIME.format(Instant.ofEpochMilli(file.lastModified()))
                + " · " + (file.length() / 1024) + " KB · " + file.getParent());
        meta.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11px;");
        meta.setWrapText(true);
        info.getChildren().addAll(name, meta);

        Button open = ghost("打开");
        open.setOnAction(e -> openFile(file));
        Button export = ghost("导出");
        export.setOnAction(e -> exportOne(file));

        row.getChildren().addAll(info, open, export);
        return row;
    }

    private void log(String message) {
        if (log != null) log.accept(message);
    }

    private void collect(List<File> files, File dir) {
        File[] children = dir.listFiles(File::isFile);
        if (children == null) return;
        for (File file : children) {
            String lower = file.getName().toLowerCase();
            if (lower.endsWith(".log") || lower.endsWith(".txt") || lower.endsWith(".gz")) files.add(file);
        }
    }

    private void exportAll() {
        File dir = new File(gameDir, "logs");
        if (!dir.isDirectory()) {
            log("⚠️ 没有日志目录: " + dir.getAbsolutePath());
            return;
        }
        File target = new File(dir.getParentFile(), "logs-export-" + System.currentTimeMillis() + ".zip");
        try {
            ZipUtil.zipDirectory(dir, target);
            log("📦 日志已导出: " + target.getAbsolutePath());
            refresh();
        } catch (Exception e) {
            log("❌ 导出日志失败: " + e.getMessage());
        }
    }

    private void exportOne(File file) {
        try {
            File target = new File(System.getProperty("user.home"),
                    "Desktop/" + file.getName());
            Files.copy(file.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            log("📄 已导出到桌面: " + target.getAbsolutePath());
        } catch (Exception e) {
            log("❌ 导出失败: " + e.getMessage());
        }
    }

    private void clearLogs() {
        File dir = new File(gameDir, "logs");
        File[] files = dir.listFiles(File::isFile);
        int count = 0;
        if (files != null) {
            for (File file : files) {
                if (file.delete()) count++;
            }
        }
        log("🧹 已清理 " + count + " 个日志文件");
        refresh();
    }

    private void openFile(File file) {
        try {
            java.awt.Desktop.getDesktop().open(file);
        } catch (Exception e) {
            log("❌ 打开失败: " + e.getMessage());
        }
    }

    private void openDir(File dir) {
        try {
            if (!dir.isDirectory() && !dir.mkdirs()) return;
            java.awt.Desktop.getDesktop().open(dir);
        } catch (Exception e) {
            log("❌ 打开目录失败: " + e.getMessage());
        }
    }

    private Button ghost(String text) {
        Button button = new Button(text);
        button.setStyle("-fx-background-color: -app-row3; -fx-text-fill: -app-text-2; -fx-background-radius: 8;"
                + " -fx-padding: 7 14; -fx-cursor: hand; -fx-font-size: 12px;");
        button.setMinWidth(Region.USE_PREF_SIZE);
        button.setCursor(Cursor.HAND);
        return button;
    }
}
