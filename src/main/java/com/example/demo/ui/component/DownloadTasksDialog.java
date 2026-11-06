package com.example.demo.ui.component;

import com.example.demo.core.download.DownloadManager;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.File;
import java.util.function.Consumer;

/**
 * 「下载任务」对话框：当前进行中的文件 + 最近完成的一个文件 + 打开所在文件夹。
 *
 * <p>从主类搬出来；主类只传状态、最近文件和「打开目录」的回调。
 */
public final class DownloadTasksDialog {

    private DownloadTasksDialog() {
    }

    public static void show(Stage owner, DownloadManager.Status status, File lastDownloaded,
                            Consumer<File> openDir) {
        boolean busy = status.total() > 0 && status.done() < status.total();
        Dialog<ButtonType> dialog = new Dialog<>();
        if (owner != null) {
            dialog.initOwner(owner);
        }
        dialog.setTitle("下载任务");
        dialog.setHeaderText(busy ? "正在下载" : "下载任务与最近文件");

        Label current = new Label(busy
                ? (status.file() == null || status.file().isBlank() ? "正在准备下载…" : status.file())
                : "当前没有进行中的下载任务。");
        current.setStyle("-fx-text-fill: -app-text-2; -fx-font-size: 12px;");

        Label last = new Label(lastDownloaded == null
                ? "还没有已完成的下载。"
                : "最近完成：\n" + lastDownloaded.getAbsolutePath());
        last.setWrapText(true);
        last.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11px;");

        Button open = new Button("打开所在文件夹");
        open.setStyle("-fx-background-color: -app-btn-2; -fx-text-fill: -app-text-2;"
                + " -fx-background-radius: 8; -fx-padding: 7 14; -fx-cursor: hand;");
        open.setDisable(lastDownloaded == null);
        open.setOnAction(e -> {
            if (lastDownloaded != null && lastDownloaded.getParentFile() != null && openDir != null) {
                openDir.accept(lastDownloaded.getParentFile());
            }
        });

        dialog.getDialogPane().setContent(new VBox(10.0, current, last, open));
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }
}
