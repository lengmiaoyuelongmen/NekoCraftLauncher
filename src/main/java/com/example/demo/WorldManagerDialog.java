package com.example.demo;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.example.demo.core.runtime.SystemMetrics;
/** 世界 / 存档管理：查看大小、备份、删除、打开目录。 */
public final class WorldManagerDialog {

    private WorldManagerDialog() {
    }

    /** 旧入口：有集成式宿主就渲染在主窗口里，没有才退回独立窗口。 */
    public static void show(Window owner, File savesDir, Consumer<String> log) {
        StackPane host = InAppDialog.globalHost();
        if (host != null) {
            showInApp(host, savesDir, log);
            return;
        }
        Dialog<Void> dialog = new Dialog<>();
        if (owner != null) dialog.initOwner(owner);
        dialog.setTitle("世界管理");
        dialog.getDialogPane().setContent(buildContent(owner, savesDir, log));
        UiFit.dialog(dialog, 760, 520);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        if (owner != null && owner.getScene() != null) {
            dialog.getDialogPane().getStylesheets().addAll(owner.getScene().getStylesheets());
        }
        dialog.showAndWait();
    }

    public static void showInApp(StackPane host, File savesDir, Consumer<String> log) {
        if (host == null) host = InAppDialog.globalHost();
        if (host == null) return;
        Window owner = host.getScene() == null ? null : host.getScene().getWindow();
        Node content = buildContent(owner, savesDir, log);
        ((Region) content).setPrefHeight(420);
        InAppDialog.customSized(
                host,
                "世界管理",
                content,
                List.of(InAppDialog.Action.secondary("关闭", null)),
                null,
                820,
                560);
    }

    private static Node buildContent(Window owner, File savesDir, Consumer<String> log) {
        ListView<File> list = new ListView<>();
        list.setCellFactory(v -> new ListCell<>() {
            @Override
            protected void updateItem(File world, boolean empty) {
                super.updateItem(world, empty);
                if (empty || world == null) {
                    setText(null);
                    return;
                }
                long bytes = size(world);
                setText("🌍  " + world.getName() + "   ·   " + SystemMetrics.bytes(bytes)
                        + "   ·   " + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm")
                        .format(new java.util.Date(world.lastModified())));
            }
        });
        Runnable refresh = () -> {
            File[] dirs = savesDir.listFiles(File::isDirectory);
            if (dirs == null) dirs = new File[0];
            Arrays.sort(dirs, Comparator.comparing(File::lastModified).reversed());
            list.getItems().setAll(dirs);
        };
        refresh.run();

        Button open = action("打开世界文件夹");
        open.setOnAction(e -> {
            File world = list.getSelectionModel().getSelectedItem();
            if (world != null) openDir(world);
            else openDir(savesDir);
        });
        Button backup = action("备份为 ZIP");
        backup.setOnAction(e -> {
            File world = list.getSelectionModel().getSelectedItem();
            if (world == null) {
                InAppDialog.confirm(null, "没有选中世界", "先在列表里选一个世界再备份。", "知道了", null);
                return;
            }
            FileChooser chooser = new FileChooser();
            chooser.setTitle("备份世界");
            chooser.setInitialFileName(world.getName() + "-backup.zip");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("ZIP", "*.zip"));
            File target = chooser.showSaveDialog(owner);
            if (target == null) return;
            try {
                zip(world, target);
                if (log != null) log.accept("💾 已备份世界: " + target.getAbsolutePath());
                InAppDialog.confirm(
                        null, "备份完成", "已保存到：\n" + target.getAbsolutePath(), "知道了", null);
            } catch (Exception ex) {
                InAppDialog.confirm(null, "备份失败", String.valueOf(ex.getMessage()), "知道了", null);
            }
        });
        Button delete = action("删除世界");
        delete.setOnAction(e -> {
            File world = list.getSelectionModel().getSelectedItem();
            if (world == null) return;
            InAppDialog.confirm(
                    null,
                    "删除世界",
                    "确定删除世界 " + world.getName() + "？此操作不可恢复。",
                    "删除",
                    () -> {
                        deleteRecursive(world.toPath());
                        refresh.run();
                    });
        });
        Button refreshBtn = action("刷新");
        refreshBtn.setOnAction(e -> refresh.run());
        HBox tools = new HBox(8, open, backup, delete, refreshBtn);
        tools.setAlignment(Pos.CENTER_LEFT);

        Label path = new Label("存档目录：" + savesDir.getAbsolutePath());
        path.setStyle("-fx-text-fill: -app-text-dim; -fx-font-size: 11px;");
        VBox content = new VBox(10, path, list, tools);
        content.setPadding(new Insets(6));
        VBox.setVgrow(list, Priority.ALWAYS);
        return content;
    }

    private static Button action(String text) {
        Button button = new Button(text);
        button.getStyleClass().addAll("btn-secondary", "btn-sm");
        return button;
    }

    private static long size(File file) {
        if (file.isFile()) return file.length();
        File[] files = file.listFiles();
        if (files == null) return 0;
        long total = 0;
        for (File child : files) total += size(child);
        return total;
    }

    private static void zip(File source, File target) throws Exception {
        try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(target))) {
            zipDir(source, source.getName(), zip);
        }
    }

    private static void zipDir(File dir, String base, ZipOutputStream zip) throws Exception {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File file : files) {
            String name = base + "/" + file.getName();
            if (file.isDirectory()) {
                zipDir(file, name, zip);
            } else {
                zip.putNextEntry(new ZipEntry(name));
                try (FileInputStream in = new FileInputStream(file)) {
                    in.transferTo(zip);
                }
                zip.closeEntry();
            }
        }
    }

    private static void openDir(File dir) {
        try {
            new ProcessBuilder("explorer.exe", dir.getAbsolutePath()).start();
        } catch (Exception ignored) {
        }
    }

    private static void deleteRecursive(java.nio.file.Path path) {
        try {
            if (Files.isDirectory(path)) {
                try (var stream = Files.list(path)) {
                    for (var child : stream.toList()) deleteRecursive(child);
                }
            }
            Files.deleteIfExists(path);
        } catch (Exception ignored) {
        }
    }
}
