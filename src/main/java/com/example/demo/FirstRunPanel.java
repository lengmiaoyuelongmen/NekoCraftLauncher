package com.example.demo;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * 首次启动的全屏引导页（不是弹窗）：左侧说明，右侧四项可逆优化，底部应用/稍后。
 * 应用过程就在页面内显示结果，完成后淡出回到启动器。
 */
public final class FirstRunPanel extends StackPane {

    private final LauncherConfig config;
    private final Consumer<String> log;
    private final Runnable onFinished;
    private final Label status = new Label("修改前会自动备份原值，可随时在 工具 → 性能优化 恢复。");

    private Option power;
    private Option dvr;
    private Option gameMode;
    private Option transparency;

    public FirstRunPanel(LauncherConfig config, Consumer<String> log, Runnable onFinished) {
        this.config = config;
        this.log = log;
        this.onFinished = onFinished;
        setAlignment(Pos.CENTER);
        setPadding(new Insets(34, 48, 34, 48));
        setStyle("-fx-background-color: linear-gradient(to bottom right, -app-root-1 0%, -app-root-2 48%, -app-root-3 100%);"
                + " -fx-background-radius: 12;");
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        getChildren().add(buildContent());
        playIn();
    }

    private VBox buildContent() {
        HBox brand = new HBox(10);
        brand.setAlignment(Pos.CENTER_LEFT);
        Region mark = new Region();
        mark.setPrefSize(4, 20);
        mark.setStyle("-fx-background-color: -app-accent; -fx-background-radius: 2;");
        Label name = new Label("Neko Craft Launcher");
        name.setStyle("-fx-text-fill: -app-text; -fx-font-size: 15px; -fx-font-weight: bold;");
        Label version = new Label("1.0");
        version.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11px;");
        brand.getChildren().addAll(mark, name, version);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label step = new Label("首次启动 · SETUP");
        step.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11px;");
        HBox top = new HBox(10, brand, spacer, step);
        top.setAlignment(Pos.CENTER_LEFT);

        Label eyebrow = new Label("00 / FIRST RUN");
        eyebrow.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 10px; -fx-font-weight: bold;");
        Label title = new Label("先给游戏\n腾出性能。");
        title.setStyle("-fx-text-fill: -app-text; -fx-font-size: 36px; -fx-font-weight: bold; -fx-line-spacing: 2;");
        Label intro = new Label("首次启动可以顺手完成几项可逆的系统优化，"
                + "让 Minecraft 更稳定、帧率更稳。只改用户级设置，不会关闭安全软件或系统服务。");
        intro.setWrapText(true);
        intro.setMaxWidth(400);
        intro.setStyle("-fx-text-fill: -app-text-dim; -fx-font-size: 13px; -fx-line-spacing: 3;");

        VBox bullets = new VBox(8,
                bullet("4 项优化均可一键恢复"),
                bullet("修改前记录原电源方案与注册表值"),
                bullet("跳过也不影响启动器使用"));
        bullets.setPadding(new Insets(10, 0, 0, 0));

        VBox left = new VBox(14, eyebrow, title, intro, bullets);
        left.setMaxWidth(420);
        left.setAlignment(Pos.CENTER_LEFT);

        power = option("高性能电源计划", "减少 CPU 降频，游戏时更稳");
        dvr = option("关闭 Game DVR", "停止后台录制占用 GPU / 磁盘");
        gameMode = option("开启游戏模式", "让 Windows 优先分配游戏资源");
        transparency = option("关闭透明效果", "减少桌面合成带来的额外开销");

        VBox options = new VBox(9, power.row(), dvr.row(), gameMode.row(), transparency.row());
        options.setPadding(new Insets(4, 0, 0, 0));

        Label cardTitle = new Label("本次会应用");
        cardTitle.setStyle("-fx-text-fill: -app-text-2; -fx-font-size: 12px; -fx-font-weight: bold;");
        VBox card = new VBox(10, cardTitle, options);
        card.setPadding(new Insets(16));
        card.setMaxWidth(480);
        card.setStyle("-fx-background-color: -app-card; -fx-background-radius: 14;"
                + " -fx-border-color: -app-border-soft; -fx-border-radius: 14;");
        HBox.setHgrow(card, Priority.ALWAYS);

        HBox body = new HBox(38, left, card);
        body.setAlignment(Pos.CENTER_LEFT);
        body.setFillHeight(false);
        VBox.setVgrow(body, Priority.ALWAYS);

        status.setWrapText(true);
        status.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11.5px;");
        HBox.setHgrow(status, Priority.ALWAYS);

        Button skip = new Button("稍后再说");
        skip.setStyle("-fx-background-color: transparent; -fx-text-fill: -app-text-dim;"
                + " -fx-border-color: -app-border-3; -fx-border-radius: 9; -fx-background-radius: 9;"
                + " -fx-padding: 9 22; -fx-cursor: hand;");
        skip.setOnAction(e -> finish());

        Button apply = new Button("应用优化并进入");
        apply.setStyle("-fx-background-color: linear-gradient(to bottom right, #d1d5db, #9ca3af);"
                + " -fx-text-fill: #111827; -fx-font-weight: bold; -fx-font-size: 13px;"
                + " -fx-background-radius: 9; -fx-padding: 9 26; -fx-cursor: hand;");
        apply.setOnAction(e -> apply(apply, skip));
        Motion.press(apply);

        HBox actions = new HBox(12, status, skip, apply);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox content = new VBox(20, top, body, actions);
        content.setMaxWidth(980);
        VBox.setVgrow(body, Priority.ALWAYS);
        return content;
    }

    private Option option(String title, String subtitle) {
        CheckBox box = new CheckBox();
        box.setSelected(true);
        box.setMouseTransparent(true);
        box.setFocusTraversable(false);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill: -app-text-2; -fx-font-size: 13px; -fx-font-weight: bold;");
        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11px;");
        VBox text = new VBox(2, titleLabel, subtitleLabel);
        HBox.setHgrow(text, Priority.ALWAYS);

        HBox row = new HBox(12, box, text);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(11, 14, 11, 14));
        row.setStyle(rowStyle(false));
        row.setCursor(javafx.scene.Cursor.HAND);
        row.setMaxWidth(Double.MAX_VALUE);
        row.setOnMouseEntered(e -> row.setStyle(rowStyle(true)));
        row.setOnMouseExited(e -> row.setStyle(rowStyle(false)));
        row.setOnMouseClicked(e -> box.setSelected(!box.isSelected()));
        return new Option(row, box);
    }

    private String rowStyle(boolean hover) {
        return "-fx-background-color: " + (hover ? "-app-hover-2" : "-app-row") + ";"
                + " -fx-background-radius: 10; -fx-border-color: -app-border-soft;"
                + " -fx-border-radius: 10;";
    }

    private record Option(HBox row, CheckBox box) {
    }

    private Label bullet(String text) {
        Label label = new Label("·  " + text);
        label.setStyle("-fx-text-fill: -app-text-dim-3; -fx-font-size: 12px;");
        return label;
    }

    private void apply(Button apply, Button skip) {
        apply.setDisable(true);
        skip.setDisable(true);
        status.setText("正在应用优化…");
        status.setStyle("-fx-text-fill: -app-text-2; -fx-font-size: 11.5px;");

        CompletableFuture.supplyAsync(() -> PerformanceOptimizer.apply(new File(config.getGameDir()),
                        power.box().isSelected(), dvr.box().isSelected(),
                        gameMode.box().isSelected(), transparency.box().isSelected()))
                .thenAccept(result -> javafx.application.Platform.runLater(() -> {
                    for (PerformanceOptimizer.Step step : result.steps()) {
                        log.accept((step.ok() ? "✅ " : "⚠️ ") + step.name() + "：" + step.detail());
                    }
                    if (result.applied() > 0) {
                        config.setPerformanceApplied(true);
                    }
                    config.setPerformanceAsked(true);
                    config.save();
                    status.setText(result.anyFailed()
                            ? "完成 " + result.applied() + " 项，部分项目失败，可在工具页重试或恢复。"
                            : "已应用 " + result.applied() + " 项优化，正在进入启动器…");
                    status.setStyle("-fx-text-fill: -app-text-2; -fx-font-size: 11.5px;");
                    PauseTransition pause = new PauseTransition(Duration.millis(900));
                    pause.setOnFinished(e -> finish());
                    pause.play();
                }))
                .exceptionally(ex -> {
                    javafx.application.Platform.runLater(() -> {
                        apply.setDisable(false);
                        skip.setDisable(false);
                        status.setText("优化失败：" + ex.getMessage() + "，可稍后重试。");
                        status.setStyle("-fx-text-fill: #f87171; -fx-font-size: 11.5px;");
                    });
                    return null;
                });
    }

    private void finish() {
        config.setPerformanceAsked(true);
        config.save();
        FadeTransition fade = new FadeTransition(Duration.millis(220), this);
        fade.setToValue(0);
        TranslateTransition move = new TranslateTransition(Duration.millis(220), this);
        move.setByY(10);
        fade.setOnFinished(e -> onFinished.run());
        fade.play();
        move.play();
    }

    private void playIn() {
        setOpacity(0);
        setTranslateY(14);
        FadeTransition fade = new FadeTransition(Duration.millis(320), this);
        fade.setToValue(1);
        TranslateTransition move = new TranslateTransition(Duration.millis(320), this);
        move.setToY(0);
        fade.play();
        move.play();
    }
}
