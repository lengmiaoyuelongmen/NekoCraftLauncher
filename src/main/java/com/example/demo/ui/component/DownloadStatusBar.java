package com.example.demo.ui.component;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * 启动页底部的下载状态条：任务名 + 进度 + 统计 + 极客遥测 + 「取消导入」按钮。
 *
 * <p>从主类搬出来；各控件仍由主类取用（改文字、改进度），取消动作通过回调交回主类。
 */
public final class DownloadStatusBar {

    public interface Host {
        /** 用户点了「取消导入」。 */
        void cancelImport();
    }

    private final Label fileLabel = new Label("空闲");
    private final Label statsLabel = new Label("—");
    private final ProgressBar progress = new ProgressBar(0.0);
    private final Label telemetry = new Label("[SYS] INIT");
    private final Button cancelButton = new Button("取消导入");
    private final VBox node;

    public DownloadStatusBar(Host host) {
        Label title = new Label("下载任务");
        title.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11px;");

        fileLabel.setStyle("-fx-text-fill: -app-text-2; -fx-font-size: 12px;");
        statsLabel.setStyle("-fx-text-fill: -app-text-dim-2; -fx-font-size: 11px;");

        progress.setMaxWidth(Double.MAX_VALUE);
        progress.setPrefHeight(7.0);
        progress.setMinHeight(7.0);
        progress.setStyle("-fx-accent: #14b8a6; -fx-background-color: #cfe8dc;");
        progress.setVisible(false);
        progress.setManaged(false);
        statsLabel.setVisible(false);
        statsLabel.setManaged(false);

        telemetry.setStyle("-fx-text-fill: #ff8a72; -fx-font-size: 8.5px;"
                + " -fx-font-family: 'Cascadia Mono', 'Consolas', monospace;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        cancelButton.setStyle("-fx-background-color: rgba(248,113,113,0.16);"
                + " -fx-text-fill: #fecaca; -fx-border-color: rgba(248,113,113,0.5);"
                + " -fx-background-radius: 7; -fx-border-radius: 7; -fx-font-size: 10px;"
                + " -fx-padding: 2 8; -fx-cursor: hand;");
        cancelButton.setVisible(false);
        cancelButton.setManaged(false);
        cancelButton.setOnAction(e -> {
            if (host != null) {
                host.cancelImport();
            }
        });

        HBox header = new HBox(8.0, title, fileLabel, spacer, cancelButton, telemetry);
        header.setAlignment(Pos.CENTER_LEFT);

        node = new VBox(4.0, header, progress, statsLabel);
        node.setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-background-radius: 10;"
                + " -fx-border-color: rgba(255,255,255,0.06); -fx-border-radius: 10;"
                + " -fx-padding: 5 8;");
    }

    public VBox node() {
        return node;
    }

    public Label fileLabel() {
        return fileLabel;
    }

    public Label statsLabel() {
        return statsLabel;
    }

    public ProgressBar progress() {
        return progress;
    }

    public Label telemetry() {
        return telemetry;
    }

    public Button cancelButton() {
        return cancelButton;
    }
}
