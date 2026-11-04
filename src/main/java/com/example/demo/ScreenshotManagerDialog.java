package com.example.demo;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Window;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/** 实例截图管理：预览、打开、删除。 */
public final class ScreenshotManagerDialog {
    private ScreenshotManagerDialog() {}

    /** 旧入口：有集成式宿主就渲染在主窗口里。 */
    public static void show(Window owner, File dir) {
        StackPane host = InAppDialog.globalHost();
        if (host != null) {
            showInApp(host, dir);
            return;
        }
        Dialog<Void> dialog = new Dialog<>();
        if (owner != null) dialog.initOwner(owner);
        dialog.setTitle("截图管理");
        dialog.getDialogPane().setContent(buildContent(dir));
        UiFit.dialog(dialog, 920, 480);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        if (owner != null && owner.getScene() != null) {
            dialog.getDialogPane().getStylesheets().addAll(owner.getScene().getStylesheets());
        }
        dialog.showAndWait();
    }

    public static void showInApp(StackPane host, File dir) {
        if (host == null) host = InAppDialog.globalHost();
        if (host == null) return;
        Node content = buildContent(dir);
        ((Region) content).setPrefHeight(420);
        InAppDialog.customSized(
                host,
                "截图管理",
                content,
                List.of(InAppDialog.Action.secondary("关闭", null)),
                null,
                900,
                540);
    }

    private static Node buildContent(File dir) {
        ListView<File> list = new ListView<>();
        ImageView preview = new ImageView();
        preview.setFitWidth(460);
        preview.setFitHeight(260);
        preview.setPreserveRatio(true);
        StackPane previewPane = new StackPane(preview);
        previewPane.setPrefSize(470, 270);
        previewPane.setStyle("-fx-background-color: -app-card-alt; -fx-background-radius: 12;");
        fileList(list, dir);
        list.getSelectionModel().selectedItemProperty().addListener((o, a, file) -> {
            if (file != null) {
                Image image = new Image(file.toURI().toString(), 1040, 600, true, true);
                preview.setImage(image.isError() ? null : image);
            }
        });
        Button open = button("打开截图");
        open.setOnAction(e -> {
            File file = list.getSelectionModel().getSelectedItem();
            if (file != null) {
                try { new ProcessBuilder("explorer.exe", file.getAbsolutePath()).start(); }
                catch (Exception ignored) {}
            }
        });
        Button folder = button("打开截图目录");
        folder.setOnAction(e -> {
            try { new ProcessBuilder("explorer.exe", dir.getAbsolutePath()).start(); }
            catch (Exception ignored) {}
        });
        Button delete = button("删除截图");
        delete.setOnAction(e -> {
            File file = list.getSelectionModel().getSelectedItem();
            if (file == null) return;
            InAppDialog.confirm(
                    null,
                    "删除截图",
                    "删除 " + file.getName() + "？",
                    "删除",
                    () -> {
                        if (file.delete()) {
                            fileList(list, dir);
                            preview.setImage(null);
                        }
                    });
        });
        HBox tools = new HBox(8, open, folder, delete);
        tools.setAlignment(Pos.CENTER_LEFT);
        HBox body = new HBox(12, list, new VBox(8, previewPane, tools));
        HBox.setHgrow(list, Priority.ALWAYS);
        Label path = new Label("截图目录：" + dir.getAbsolutePath());
        path.setStyle("-fx-text-fill: -app-text-dim; -fx-font-size: 11px;");
        VBox content = new VBox(10, path, body);
        content.setPadding(new Insets(4));
        VBox.setVgrow(body, Priority.ALWAYS);
        return content;
    }

    private static void fileList(ListView<File> list, File dir) {
        File[] files = dir.listFiles((d, n) -> n.toLowerCase(java.util.Locale.ROOT).endsWith(".png"));
        if (files == null) files = new File[0];
        Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
        list.getItems().setAll(files);
    }

    private static Button button(String text) {
        Button b = new Button(text);
        b.getStyleClass().addAll("btn-secondary", "btn-sm");
        return b;
    }
}
