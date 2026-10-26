package com.example.demo;

import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

/** Neko Core 启动页：背景只保留插画，文字和进度条全部由 JavaFX 实时绘制。 */
public final class BootScreen extends StackPane {

    private static final long DURATION_MS = 3200;

    private final StackPane progressTrack = new StackPane();
    private final Region progressFill = new Region();
    private final Label percentLabel = new Label("0%");
    private final Label statusLabel = new Label("猫猫团正在准备启动…");
    private final Runnable onFinished;
    private final java.util.function.BooleanSupplier extraReady;
    private final long startedAt = System.nanoTime();
    private final AnimationTimer timer;
    private double lastProgress;
    private boolean finishing;

    public BootScreen(Runnable onFinished) {
        this(onFinished, () -> true);
    }

    public BootScreen(Runnable onFinished, java.util.function.BooleanSupplier extraReady) {
        this.onFinished = onFinished;
        this.extraReady = extraReady;
        setStyle("-fx-background-color: #f7fcf5; -fx-background-radius: 16;");
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        javafx.scene.shape.Rectangle bootClip = new javafx.scene.shape.Rectangle();
        bootClip.setArcWidth(32);
        bootClip.setArcHeight(32);
        bootClip.widthProperty().bind(widthProperty());
        bootClip.heightProperty().bind(heightProperty());
        setClip(bootClip);
        setPickOnBounds(true);
        setFocusTraversable(true);

        Image image = null;
        try (java.io.InputStream in = BootScreen.class.getResourceAsStream("/boot-nyan.png")) {
            if (in != null) image = new Image(in);
        } catch (Exception ignored) {
        }
        ImageView background = new ImageView(image);
        background.setPreserveRatio(true);
        background.setSmooth(false);
        background.setMouseTransparent(true);
        background.setManaged(false);
        getChildren().add(background);
        final Image bgImage = image;
        Runnable placeBackground = () -> {
            if (bgImage == null) return;
            double scale = Math.min(getWidth() / bgImage.getWidth(), getHeight() / bgImage.getHeight());
            double w = bgImage.getWidth() * scale;
            double h = bgImage.getHeight() * scale;
            background.setFitWidth(w);
            background.setFitHeight(h);
            background.setLayoutX((getWidth() - w) / 2);
            background.setLayoutY((getHeight() - h) / 2);
        };
        widthProperty().addListener((obs, old, value) -> placeBackground.run());
        heightProperty().addListener((obs, old, value) -> placeBackground.run());

        Label brand = new Label("✦  NEKO CORE");
        brand.setStyle("-fx-text-fill: #6fa86a; -fx-font-size: 21px; -fx-font-weight: bold;"
                + " -fx-font-family: 'Microsoft YaHei UI', 'Segoe UI'; -fx-font-smoothing-type: lcd;");
        Label sub = new Label("BOOT SEQUENCE / LOCAL SYSTEM");
        sub.setStyle("-fx-text-fill: rgba(111,168,106,0.70); -fx-font-size: 9px;"
                + " -fx-font-family: 'Cascadia Mono', 'Consolas', monospace; -fx-font-smoothing-type: lcd;");
        VBox topLeft = new VBox(5, brand, sub);
        AnchorPane.setLeftAnchor(topLeft, 46.0);
        AnchorPane.setTopAnchor(topLeft, 24.0);

        Label online = new Label("● NEKO ASSISTANT // ONLINE  v" + AppVersion.CURRENT);
        online.setStyle("-fx-text-fill: #6f8f6a; -fx-font-size: 9.5px;"
                + " -fx-font-weight: bold;"
                + " -fx-font-family: 'Cascadia Mono', 'Consolas', monospace; -fx-font-smoothing-type: lcd;");
        AnchorPane.setRightAnchor(online, 30.0);
        AnchorPane.setTopAnchor(online, 32.0);

        VBox leftTips = new VBox(7,
                tip("NEKO ASSISTANT", "#5f8a59"),
                tip("猫娘正在准备启动", "#4f7d4a"),
                tip("请稍后...", "#6f9869"),
                tip("今天也要一起冒险", "#6f9869"));
        AnchorPane.setLeftAnchor(leftTips, 46.0);
        AnchorPane.setTopAnchor(leftTips, 154.0);

        Label terminal = new Label("neko@home~$ ./nyan --boot --for-the-people");
        terminal.setStyle("-fx-text-fill: #5f7d5a; -fx-font-size: 10.5px;"
                + " -fx-font-family: 'Cascadia Mono', 'Consolas', monospace; -fx-font-smoothing-type: lcd;");
        statusLabel.setStyle("-fx-text-fill: #4f724b; -fx-font-size: 11.5px;"
                + " -fx-font-family: 'Microsoft YaHei UI', 'Segoe UI'; -fx-font-smoothing-type: lcd;");

        progressTrack.setPrefHeight(9);
        progressTrack.setMinHeight(9);
        progressTrack.setMaxHeight(9);
        progressTrack.setMaxWidth(Double.MAX_VALUE);
        progressTrack.setSnapToPixel(true);
        progressTrack.setStyle("-fx-background-color: linear-gradient(to right,"
                + " rgba(198,226,190,0.55), rgba(238,248,233,0.45));"
                + " -fx-background-radius: 4; -fx-border-color: rgba(122,167,107,0.20);"
                + " -fx-border-radius: 4;");
        progressFill.setPrefHeight(9);
        progressFill.setMaxHeight(9);
        progressFill.setSnapToPixel(true);
        progressFill.setStyle("-fx-background-color: linear-gradient(to right, #8cc984, #b3dda9);"
                + " -fx-background-radius: 4;");
        progressTrack.getChildren().add(progressFill);
        StackPane.setAlignment(progressFill, Pos.CENTER_LEFT);

        javafx.scene.canvas.Canvas catMarker = new javafx.scene.canvas.Canvas(30, 28);
        drawCatMarker(catMarker);
        catMarker.setMouseTransparent(true);
        StackPane progressStack = new StackPane(progressTrack, catMarker);
        progressStack.setPrefHeight(28);
        progressStack.setMinHeight(28);
        progressStack.setMaxHeight(28);
        StackPane.setAlignment(progressTrack, Pos.CENTER_LEFT);
        StackPane.setAlignment(catMarker, Pos.CENTER_LEFT);
        progressStack.widthProperty().addListener((obs, old, value) -> {
            progressFill.setPrefWidth(Math.max(0, value.doubleValue() * lastProgress - 2));
            catMarker.setTranslateX(Math.max(-12, value.doubleValue() * lastProgress - 20));
        });

        percentLabel.setStyle("-fx-text-fill: #5f7d5a; -fx-font-size: 10.5px;"
                + " -fx-font-family: 'Cascadia Mono', 'Consolas', monospace; -fx-font-smoothing-type: lcd;");
        percentLabel.setMinWidth(40);
        HBox.setHgrow(progressStack, Priority.ALWAYS);
        HBox progressRow = new HBox(10, progressStack, percentLabel);
        progressRow.setAlignment(Pos.CENTER_LEFT);

        Label footer = new Label("猫娘助手：为爱发电  //  Esc 取消");
        footer.setStyle("-fx-text-fill: #7c9a77; -fx-font-size: 9.5px;"
                + " -fx-font-family: 'Microsoft YaHei UI', 'Segoe UI'; -fx-font-smoothing-type: lcd;");
        VBox bottom = new VBox(7, terminal, statusLabel, progressRow, footer);
        bottom.setMaxWidth(790);
        AnchorPane.setLeftAnchor(bottom, 46.0);
        AnchorPane.setRightAnchor(bottom, 80.0);
        AnchorPane.setBottomAnchor(bottom, 24.0);

        AnchorPane overlay = new AnchorPane(topLeft, online, leftTips, bottom);
        overlay.setPickOnBounds(false);
        getChildren().add(overlay);
        setOnMouseClicked(e -> finish());
        setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE || e.getCode() == KeyCode.SPACE || e.getCode() == KeyCode.ENTER) {
                finish();
            }
        });

        timer = new AnimationTimer() {
            private long last;

            @Override
            public void handle(long now) {
                if (now - last < 33_000_000L) return;
                last = now;
                double elapsed = (System.nanoTime() - startedAt) / 1_000_000.0;
                double progress = Math.min(1.0, elapsed / DURATION_MS);
                boolean ready = extraReady == null || extraReady.getAsBoolean();
                double display = progress;
                if (progress >= 1 && !ready) {
                    display = 0.99;
                    statusLabel.setText("正在预渲染社区资源…");
                } else if (progress < 0.32) {
                    statusLabel.setText("猫猫团正在准备启动…");
                } else if (progress < 0.68) {
                    statusLabel.setText("正在校验本地资源…");
                } else if (progress < 0.92) {
                    statusLabel.setText("正在同步猫猫资源…");
                } else {
                    statusLabel.setText("猫猫团已就绪，正在加载中…");
                }
                lastProgress = Math.min(1.0, display);
                progressFill.setPrefWidth(Math.max(0, progressStack.getWidth() * lastProgress - 2));
                catMarker.setTranslateX(Math.max(-12, progressStack.getWidth() * lastProgress - 20));
                percentLabel.setText((int) (display * 100) + "%");
                if (progress >= 1 && ready) finish();
            }
        };
        timer.start();
        Platform.runLater(() -> {
            placeBackground.run();
            requestFocus();
        });
    }

    private Label tip(String text, String color) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 10.5px;"
                + " -fx-font-family: 'Microsoft YaHei UI', 'Segoe UI'; -fx-font-smoothing-type: lcd;");
        return label;
    }

    private void drawCatMarker(javafx.scene.canvas.Canvas canvas) {
        var g = canvas.getGraphicsContext2D();
        g.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        g.setFill(Color.web("#ffffff", 0.98));
        g.setStroke(Color.web("#7fad76", 0.9));
        g.setLineWidth(1);
        g.fillPolygon(new double[]{5, 9, 12}, new double[]{10, 2, 10}, 3);
        g.strokePolygon(new double[]{5, 9, 12}, new double[]{10, 2, 10}, 3);
        g.fillPolygon(new double[]{18, 21, 25}, new double[]{10, 2, 10}, 3);
        g.strokePolygon(new double[]{18, 21, 25}, new double[]{10, 2, 10}, 3);
        g.fillOval(4, 7, 22, 18);
        g.strokeOval(4, 7, 22, 18);
        g.setFill(Color.web("#5d7d58"));
        g.fillOval(10, 14, 2.2, 2.6);
        g.fillOval(18, 14, 2.2, 2.6);
        g.setFill(Color.web("#f3b8b2", 0.9));
        g.fillOval(7, 17, 4, 2);
        g.fillOval(20, 17, 4, 2);
        g.setStroke(Color.web("#8caf83", 0.9));
        g.strokeLine(15, 17, 15, 19);
        g.strokeLine(5, 18, 0, 17);
        g.strokeLine(5, 20, 0, 21);
        g.strokeLine(25, 18, 30, 17);
        g.strokeLine(25, 20, 30, 21);
    }

    private void finish() {
        if (finishing) return;
        finishing = true;
        timer.stop();
        FadeTransition fade = new FadeTransition(Duration.millis(260), this);
        fade.setToValue(0);
        fade.setOnFinished(e -> {
            if (getParent() instanceof StackPane parent) parent.getChildren().remove(this);
            if (onFinished != null) onFinished.run();
        });
        fade.play();
    }
}
