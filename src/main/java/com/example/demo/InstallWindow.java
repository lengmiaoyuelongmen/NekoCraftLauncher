package com.example.demo;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.util.ArrayList;
import java.util.List;

/**
 * 独立安装进度窗口：像 PCL 那样把「准备 → 游戏版本 → 加载器 → 文件」的每一步摆出来。
 *
 * <p>不是模态的，用户仍然可以操作主界面；安装结束自动关闭。
 */
public final class InstallWindow {

    private final Stage stage = new Stage(StageStyle.DECORATED);
    private final VBox stepBox = new VBox(8);
    private final Label titleLabel = new Label();
    private final Label statusLabel = new Label("准备中…");
    private final ProgressBar progress = new ProgressBar(0);
    private final List<Label> stepIcons = new ArrayList<>();
    private final List<Label> stepTexts = new ArrayList<>();
    private Runnable onCancel;
    private boolean finished;

    public InstallWindow(Window owner, String title, List<String> steps) {
        stage.setTitle("安装 - " + title);
        if (owner != null) {
            stage.initOwner(owner);
        }
        stage.initModality(Modality.NONE);
        stage.setResizable(true);
        stage.setWidth(460);
        stage.setMinWidth(420);

        titleLabel.setText(title);
        titleLabel.setStyle("-fx-text-fill: -app-text; -fx-font-size: 15px; -fx-font-weight: bold;");
        Label subtitle = new Label("安装过程会逐步显示在这里");
        subtitle.setStyle("-fx-text-fill: -app-text-dim; -fx-font-size: 11.5px;");

        for (String step : steps) {
            Label icon = new Label("○");
            icon.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 12px;");
            icon.setMinWidth(18);
            Label text = new Label(step);
            text.setStyle("-fx-text-fill: -app-text-dim; -fx-font-size: 12px;");
            HBox row = new HBox(8, icon, text);
            row.setAlignment(Pos.CENTER_LEFT);
            stepBox.getChildren().add(row);
            stepIcons.add(icon);
            stepTexts.add(text);
        }

        statusLabel.setStyle("-fx-text-fill: -app-text-2; -fx-font-size: 11.5px;");
        statusLabel.setWrapText(true);
        progress.setMaxWidth(Double.MAX_VALUE);
        progress.setPrefHeight(6);
        progress.setStyle("-fx-accent: -app-accent;");

        Button cancel = new Button("取消");
        cancel.getStyleClass().addAll("btn-secondary", "btn-sm");
        cancel.setOnAction(
                e -> {
                    statusLabel.setText("正在取消…");
                    cancel.setDisable(true);
                    if (onCancel != null) {
                        onCancel.run();
                    }
                });
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox footer = new HBox(8, statusLabel, spacer, cancel);
        footer.setAlignment(Pos.CENTER_LEFT);

        VBox root = new VBox(14, titleLabel, subtitle, stepBox, progress, footer);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: -app-bg-2;");
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Scene scene = new Scene(root, 440, 320);
        String css = HelloApplication.THEME_URL;
        if (css != null) {
            scene.getStylesheets().add(css);
        }
        String light = InstallWindow.class.getResource("/com/example/demo/app-light.css") != null
                ? InstallWindow.class.getResource("/com/example/demo/app-light.css").toExternalForm()
                : null;
        if (light != null && Theme.isLight()) {
            scene.getStylesheets().add(light);
        }
        stage.setScene(scene);
        stage.setOnCloseRequest(
                e -> {
                    if (!finished) {
                        e.consume();
                        statusLabel.setText("安装进行中，先点「取消」再关闭");
                    }
                });
    }

    public void show() {
        stage.show();
        stage.toFront();
    }

    public void setOnCancel(Runnable action) {
        this.onCancel = action;
    }

    /** 标记第 index 步的状态：run / ok / fail。 */
    public void setStep(int index, String state) {
        if (index < 0 || index >= stepIcons.size()) {
            return;
        }
        Label icon = stepIcons.get(index);
        Label text = stepTexts.get(index);
        switch (state) {
            case "run" -> {
                icon.setText("●");
                icon.setStyle("-fx-text-fill: -app-accent; -fx-font-size: 12px;");
                text.setStyle("-fx-text-fill: -app-text; -fx-font-size: 12px; -fx-font-weight: bold;");
            }
            case "ok" -> {
                icon.setText("✔");
                icon.setStyle("-fx-text-fill: #22c55e; -fx-font-size: 12px;");
                text.setStyle("-fx-text-fill: -app-text-2; -fx-font-size: 12px;");
            }
            case "fail" -> {
                icon.setText("✘");
                icon.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12px;");
                text.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12px; -fx-font-weight: bold;");
            }
            case "skip" -> {
                icon.setText("—");
                icon.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 12px;");
                text.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 12px;");
            }
            default -> {
                icon.setText("○");
                icon.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 12px;");
                text.setStyle("-fx-text-fill: -app-text-dim; -fx-font-size: 12px;");
            }
        }
    }

    /** 0..1，负值表示不确定进度。 */
    public void setProgress(double value) {
        if (value < 0) {
            progress.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        } else {
            progress.setProgress(Math.max(0, Math.min(1, value)));
        }
    }

    public void setStatus(String text) {
        statusLabel.setText(text);
    }

    /** 安装结束：全部步骤收尾，2 秒后自动关窗。 */
    public void finish(boolean ok, String message) {
        finished = true;
        setProgress(ok ? 1 : 0);
        statusLabel.setText(message);
        statusLabel.setStyle(
                ok
                        ? "-fx-text-fill: #22c55e; -fx-font-size: 11.5px;"
                        : "-fx-text-fill: #ef4444; -fx-font-size: 11.5px;");
        stage.setOnCloseRequest(null);
        Platform.runLater(
                () -> {
                    javafx.animation.PauseTransition delay =
                            new javafx.animation.PauseTransition(javafx.util.Duration.seconds(2));
                    delay.setOnFinished(e -> stage.close());
                    delay.play();
                });
    }

    public void close() {
        finished = true;
        stage.close();
    }

    public Stage stage() {
        return stage;
    }
}
