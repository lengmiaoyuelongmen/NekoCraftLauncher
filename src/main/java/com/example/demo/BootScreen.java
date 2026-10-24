package com.example.demo;

import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.util.Duration;

/**
 * MOSS 风格的启动预加载页：红色光学核心、同心扫描环、启动自检日志。
 * 纯 JavaFX Canvas 实时绘制，不依赖图片资源。
 */
public final class BootScreen extends StackPane {

    private static final long DURATION_MS = 3200;
    private static final String[] STEPS = {
            "猫爪自检", "依赖校验", "资源同步", "欢迎回来，主人"
    };

    private final Canvas canvas = new Canvas();
    private final ProgressBar progressBar = new ProgressBar(0);
    private final Label percentLabel = new Label("0%");
    private final Label statusLabel = new Label("正在建立连接…");
    private final Label[] stepLabels = new Label[STEPS.length];
    private final Runnable onFinished;
    private final long startedAt = System.nanoTime();
    private final AnimationTimer timer;
    private boolean finishing;
    private double mouseX = -1;
    private double mouseY = -1;
    private Label alertLabel;

    public BootScreen(Runnable onFinished) {
        this.onFinished = onFinished;
        setStyle("-fx-background-color: #071212;");
        setPickOnBounds(true);
        setFocusTraversable(true);

        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());
        getChildren().add(canvas);

        Label brand = new Label("NEKO CORE");
        brand.setStyle("-fx-text-fill: #5eead4; -fx-font-size: 15px; -fx-font-weight: bold;"
                + " -fx-font-family: 'Cascadia Mono', 'Consolas', monospace;");
        Label sub = new Label("BOOT SEQUENCE / LOCAL SYSTEM");
        sub.setStyle("-fx-text-fill: rgba(94,234,212,0.60); -fx-font-size: 9px;"
                + " -fx-font-family: 'Cascadia Mono', 'Consolas', monospace;");
        VBox brandBox = new VBox(2, brand, sub);

        alertLabel = new Label("● NEKO ASSISTANT // ONLINE  v" + AppVersion.CURRENT);
        alertLabel.setStyle("-fx-text-fill: rgba(94,234,212,0.90); -fx-font-size: 9px;"
                + " -fx-font-family: 'Cascadia Mono', 'Consolas', monospace;");
        Label right = alertLabel;
        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);
        HBox top = new HBox(16, brandBox, topSpacer, right);
        top.setAlignment(Pos.TOP_LEFT);
        top.setPadding(new Insets(24, 28, 0, 28));

        Label title = new Label("N E K O   C O R E");
        title.setStyle("-fx-text-fill: #f4f4f4; -fx-font-size: 27px; -fx-font-weight: bold;"
                + " -fx-font-family: 'Cascadia Mono', 'Consolas', monospace;");
        Label tagline = new Label("猫娘助手 · 为爱发电 · 今天也要一起冒险");
        tagline.setStyle("-fx-text-fill: rgba(255,255,255,0.55); -fx-font-size: 11px;");
        VBox centerText = new VBox(7, title, tagline);
        centerText.setAlignment(Pos.CENTER);

        VBox logs = new VBox(3);
        logs.setAlignment(Pos.CENTER_LEFT);
        logs.setMaxWidth(320);
        for (int i = 0; i < STEPS.length; i++) {
            Label line = new Label("  " + STEPS[i]);
            line.setStyle("-fx-text-fill: rgba(255,255,255,0.35); -fx-font-size: 10px;"
                    + " -fx-font-family: 'Cascadia Mono', 'Consolas', monospace;");
            line.setOpacity(0);
            stepLabels[i] = line;
            logs.getChildren().add(line);
        }

        VBox centerGroup = new VBox(9, centerText, logs);
        centerGroup.setAlignment(Pos.CENTER);
        centerGroup.setTranslateY(88);

        progressBar.setPrefWidth(360);
        progressBar.setPrefHeight(3);
        progressBar.setStyle("-fx-accent: #2dd4bf;");
        percentLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.72); -fx-font-size: 10px;"
                + " -fx-font-family: 'Cascadia Mono', 'Consolas', monospace;");
        percentLabel.setMinWidth(36);
        statusLabel.setStyle("-fx-text-fill: rgba(94,234,212,0.80); -fx-font-size: 10px;"
                + " -fx-font-family: 'Cascadia Mono', 'Consolas', monospace;");
        HBox progressRow = new HBox(10, progressBar, percentLabel);
        progressRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(progressBar, Priority.ALWAYS);
        Label terminal = new Label("neko@home:~$ ./nyan --boot --for-the-people");
        terminal.setStyle("-fx-text-fill: rgba(255,205,110,0.55); -fx-font-size: 8.5px;"
                + " -fx-font-family: 'Cascadia Mono', 'Consolas', monospace;");
        VBox bottom = new VBox(6, terminal, statusLabel, progressRow,
                new Label("猫娘助手 · 为爱发电  //  Esc 跳过") {{
                    setStyle("-fx-text-fill: rgba(255,255,255,0.25); -fx-font-size: 8.5px;"
                            + " -fx-font-family: 'Cascadia Mono', 'Consolas', monospace;");
                }});
        bottom.setPadding(new Insets(0, 28, 22, 28));
        bottom.setMaxWidth(Double.MAX_VALUE);

        VBox leftTips = new VBox(6,
                threatLabel("NEKO ASSISTANT", "rgba(94,234,212,0.95)"),
                threatLabel("猫娘正在准备启动", "rgba(153,246,228,0.82)"),
                threatLabel("猫爪已就位", "rgba(125,211,252,0.68)"),
                threatLabel("今天也要一起冒险", "rgba(125,211,252,0.45)"));
        leftTips.setPadding(new Insets(0, 0, 0, 28));
        leftTips.setMaxWidth(190);
        leftTips.setTranslateY(126);
        VBox rightTips = new VBox(6,
                threatLabel("NYA~", "rgba(153,246,228,0.85)"),
                threatLabel("COZY MODE", "rgba(125,211,252,0.65)"),
                threatLabel("PAW POWERED", "rgba(125,211,252,0.55)"),
                threatLabel("CUTE & OPEN", "rgba(94,234,212,0.60)"));
        rightTips.setPadding(new Insets(0, 28, 0, 0));
        rightTips.setMaxWidth(190);
        rightTips.setAlignment(Pos.CENTER_RIGHT);
        rightTips.getChildren().forEach(node -> ((Label) node).setAlignment(Pos.CENTER_RIGHT));

        BorderPane overlay = new BorderPane();
        overlay.setTop(top);
        overlay.setLeft(leftTips);
        overlay.setRight(rightTips);
        BorderPane.setAlignment(leftTips, Pos.CENTER_LEFT);
        BorderPane.setAlignment(rightTips, Pos.CENTER_RIGHT);
        overlay.setCenter(centerGroup);
        overlay.setBottom(bottom);
        overlay.setPickOnBounds(false);
        BorderPane.setAlignment(centerGroup, Pos.CENTER);

        javafx.scene.image.ImageView chibi = new javafx.scene.image.ImageView();
        try (java.io.InputStream in = BootScreen.class.getResourceAsStream("/neko-chibi.png")) {
            if (in != null) chibi.setImage(new javafx.scene.image.Image(in));
        } catch (Exception ignored) {
        }
        chibi.setFitHeight(190);
        chibi.setFitWidth(134);
        chibi.setPreserveRatio(true);
        StackPane chibiCard = new StackPane(chibi);
        chibiCard.setMaxSize(170, 210);
        chibiCard.setStyle("-fx-background-color: rgba(220,244,212,0.96); -fx-background-radius: 24;"
                + " -fx-border-color: rgba(94,234,212,0.65); -fx-border-radius: 24; -fx-padding: 8;");
        StackPane.setAlignment(chibiCard, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(chibiCard, new Insets(0, 34, 88, 0));
        getChildren().add(chibiCard);
        getChildren().add(overlay);

        setOnMouseMoved(e -> {
            mouseX = e.getX();
            mouseY = e.getY();
        });
        setOnMouseClicked(e -> finish());
        setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE || e.getCode() == KeyCode.SPACE || e.getCode() == KeyCode.ENTER) {
                finish();
            }
        });

        timer = new AnimationTimer() {
            private long last;
            private double time;

            @Override
            public void handle(long now) {
                if (now - last < 16_000_000L) return;
                last = now;
                time += 0.016;
                double elapsed = (System.nanoTime() - startedAt) / 1_000_000.0;
                double progress = Math.min(1.0, elapsed / DURATION_MS);
                progressBar.setProgress(Math.min(1.0, progress * 1.03));
                percentLabel.setText((int) (progress * 100) + "%");
                int activeStep = Math.min(STEPS.length - 1, (int) (progress * STEPS.length));
                for (int i = 0; i < stepLabels.length; i++) {
                    if (i < activeStep) {
                        stepLabels[i].setText("  ✓ " + STEPS[i]);
                        stepLabels[i].setStyle("-fx-text-fill: rgba(255,205,110,0.72); -fx-font-size: 10px;"
                                + " -fx-font-family: 'Cascadia Mono', 'Consolas', monospace;");
                        stepLabels[i].setOpacity(1);
                    } else if (i == activeStep) {
                        stepLabels[i].setText("  › " + STEPS[i]);
                        stepLabels[i].setStyle("-fx-text-fill: rgba(255,120,100,0.9); -fx-font-size: 10px;"
                                + " -fx-font-family: 'Cascadia Mono', 'Consolas', monospace;");
                        stepLabels[i].setOpacity(1);
                    }
                }
                statusLabel.setText(progress < 0.32 ? "猫爪准备中…" : progress < 0.62 ? "伸个懒腰，校验依赖…" : progress < 0.9 ? "同步猫猫资源…" : "欢迎回来，主人~");
                if (alertLabel != null) {
                    alertLabel.setOpacity(0.55 + 0.35 * (0.5 + 0.5 * Math.sin(time * 3.2)));
                }
                draw(time, progress);
                if (progress >= 1) finish();
            }
        };
        timer.start();
        Platform.runLater(this::requestFocus);
    }

    private double[] starPoints(double cx, double cy, double outer, double inner) {
        double[] points = new double[20];
        for (int i = 0; i < 10; i++) {
            double angle = Math.toRadians(-90 + i * 36);
            double radius = i % 2 == 0 ? outer : inner;
            points[i * 2] = cx + Math.cos(angle) * radius;
            points[i * 2 + 1] = cy + Math.sin(angle) * radius;
        }
        return points;
    }

    private void drawFlag(GraphicsContext g, double x, double y, double w, double h, double time) {
        g.setStroke(Color.web("#2dd4bf", 0.65));
        g.setLineWidth(1);
        g.strokeLine(x, y, x, y + h + 6);
        g.setFill(Color.web("#14b8a6", 0.82));
        g.beginPath();
        g.moveTo(x + 3, y + 5);
        g.bezierCurveTo(x + w * 0.3, y + 1 + Math.sin(time * 1.7) * 3,
                x + w * 0.65, y + 9 + Math.cos(time * 1.4) * 3, x + w, y + 4);
        g.lineTo(x + w, y + h - 4);
        g.bezierCurveTo(x + w * 0.65, y + h + 2 + Math.cos(time * 1.5) * 3,
                x + w * 0.3, y + h - 6 + Math.sin(time * 1.2) * 3, x + 3, y + h - 1);
        g.closePath();
        g.fill();
        double[] star = starPoints(x + w * 0.5, y + h * 0.48, h * 0.2, h * 0.08);
        double[] sx = new double[10];
        double[] sy = new double[10];
        for (int i = 0; i < 10; i++) {
            sx[i] = star[i * 2];
            sy[i] = star[i * 2 + 1];
        }
        g.setFill(Color.web("#2dd4bf", 0.9));
        g.fillPolygon(sx, sy, 10);
    }

    private Label threatLabel(String text, String color) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 9px;"
                + " -fx-font-family: 'Cascadia Mono', 'Consolas', monospace;");
        return label;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private void draw(double time, double progress) {
        double w = getWidth();
        double h = getHeight();
        if (w < 4 || h < 4) return;
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.clearRect(0, 0, w, h);
        g.setFill(Color.web("#050608"));
        g.fillRect(0, 0, w, h);

        g.setLineWidth(1);
        g.setStroke(Color.web("#2dd4bf", 0.035));
        for (double x = 0; x < w; x += 64) g.strokeLine(x, 0, x, h);
        for (double y = 0; y < h; y += 64) g.strokeLine(0, y, w, y);

        double scanY = (time * 130) % (h + 120) - 60;
        g.setFill(Color.web("#14b8a6", 0.035));
        g.fillRect(0, scanY, w, 54);
        g.setStroke(Color.web("#5eead4", 0.13));
        g.strokeLine(0, scanY, w, scanY);

        if (((int) (time * 3)) % 7 < 2) {
            for (int i = 0; i < 4; i++) {
                double gy = (time * 190 + i * 137) % h;
                g.setFill(Color.web("#2dd4bf", 0.055));
                g.fillRect(0, gy, w, i % 2 == 0 ? 1 : 3);
            }
        }

        RadialGradient vignette = new RadialGradient(0, 0, 0.5, 0.5, 0.72, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.TRANSPARENT), new Stop(0.62, Color.TRANSPARENT),
                new Stop(1, Color.web("#031716", 0.55)));
        g.setFill(vignette);
        g.fillRect(0, 0, w, h);

        double cx = w / 2;
        double cy = h * 0.40;
        RadialGradient glow = new RadialGradient(0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#14b8a6", 0.16)),
                new Stop(0.45, Color.web("#14b8a6", 0.05)),
                new Stop(1, Color.TRANSPARENT));
        g.setFill(glow);
        double glowSize = Math.max(w, h) * 0.55;
        g.fillOval(cx - glowSize / 2, cy - glowSize / 2, glowSize, glowSize);

        double[] watermark = starPoints(cx, cy, 176, 68);
        double[] wmY = new double[10];
        double[] wmX = new double[10];
        for (int i = 0; i < 10; i++) {
            wmX[i] = watermark[i * 2];
            wmY[i] = watermark[i * 2 + 1];
        }
        g.setFill(Color.web("#14b8a6", 0.04));
        g.fillPolygon(wmX, wmY, 10);
        g.setStroke(Color.web("#2dd4bf", 0.06));
        g.strokePolygon(wmX, wmY, 10);


        for (int i = 0; i < 4; i++) {
            double radius = 86 + i * 32 + Math.sin(time * 0.6 + i) * 2;
            g.setStroke(Color.web("#2dd4bf", 0.18 - i * 0.03));
            g.setLineWidth(1);
            g.strokeOval(cx - radius, cy - radius, radius * 2, radius * 2);
        }
        g.setStroke(Color.web("#5eead4", 0.45));
        g.setLineDashes(10, 18);
        g.setLineDashOffset(time * 26);
        g.strokeArc(cx - 154, cy - 154, 308, 308, time * 18 % 360, 110, javafx.scene.shape.ArcType.OPEN);
        g.strokeArc(cx - 118, cy - 118, 236, 236, -time * 24 % 360, 70, javafx.scene.shape.ArcType.OPEN);
        g.setLineDashes(null);

        int ticks = 32;
        for (int i = 0; i < ticks; i++) {
            double angle = Math.toRadians(i * (360.0 / ticks));
            double inner = 184;
            double outer = i % 4 == 0 ? 196 : 190;
            g.setStroke(Color.web("#2dd4bf", i % 4 == 0 ? 0.5 : 0.18));
            g.strokeLine(cx + Math.cos(angle) * inner, cy + Math.sin(angle) * inner,
                    cx + Math.cos(angle) * outer, cy + Math.sin(angle) * outer);
        }

        for (int i = 0; i < 12; i++) {
            double a1 = Math.toRadians(i * 30 + time * 4);
            double a2 = Math.toRadians(i * 30 + 22 + time * 4);
            double r1 = 60;
            double r2 = 76;
            g.setFill(Color.web(i % 2 == 0 ? "#041f1d" : "#062a27"));
            g.fillPolygon(new double[]{cx + Math.cos(a1) * r1, cx + Math.cos(a1) * r2,
                            cx + Math.cos(a2) * r2, cx + Math.cos(a2) * r1},
                    new double[]{cy + Math.sin(a1) * r1, cy + Math.sin(a1) * r2,
                            cy + Math.sin(a2) * r2, cy + Math.sin(a2) * r1}, 4);
            g.setStroke(Color.web("#2dd4bf", 0.16));
            g.strokeLine(cx + Math.cos(a1) * r1, cy + Math.sin(a1) * r1,
                    cx + Math.cos(a1) * r2, cy + Math.sin(a1) * r2);
        }

        double lookX = 0;
        double lookY = 0;
        if (mouseX >= 0 && mouseY >= 0) {
            lookX = clamp((mouseX - cx) * 0.018, -9, 9);
            lookY = clamp((mouseY - cy) * 0.018, -7, 7);
        }
        double ex = cx + lookX;
        double ey = cy + lookY;

        g.setFill(Color.web("#052e2b"));
        g.fillOval(cx - 58, cy - 58, 116, 116);
        g.setStroke(Color.web("#5eead4", 0.75));
        g.setLineWidth(1.5);
        g.strokeOval(cx - 58, cy - 58, 116, 116);
        RadialGradient iris = new RadialGradient(0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#ccfbf1")), new Stop(0.55, Color.web("#2dd4bf")),
                new Stop(1, Color.web("#042f2e")));
        g.setFill(iris);
        g.fillOval(ex - 30, ey - 30, 60, 60);
        for (int i = 0; i < 28; i++) {
            double a = Math.toRadians(i * (360.0 / 28) + time * 8);
            g.setStroke(Color.web("#99f6e4", 0.16));
            g.strokeLine(ex + Math.cos(a) * 14, ey + Math.sin(a) * 14,
                    ex + Math.cos(a) * 29, ey + Math.sin(a) * 29);
        }
        g.setFill(Color.web("#021716"));
        g.fillOval(ex - 8, ey - 23, 16, 46);
        g.setStroke(Color.web("#7dd3fc", 0.35));
        g.strokeOval(ex - 8, ey - 23, 16, 46);
        g.setFill(Color.web("#2dd4bf", 0.8));
        g.fillOval(ex - 16, ey - 18, 8, 8);
        g.setStroke(Color.web("#7dd3fc", 0.55));
        g.strokeLine(cx - 78, cy, cx + 78, cy);
        double sweepY = cy - 70 + (time * 36) % 140;
        g.setStroke(Color.web("#7dd3fc", 0.18));
        g.strokeLine(cx - 70, sweepY, cx + 70, sweepY);

        g.setStroke(Color.web("#2dd4bf", 0.34));
        g.setLineWidth(1);
        double inset = 22;
        double len = 18;
        g.strokeLine(inset, inset, inset + len, inset);
        g.strokeLine(inset, inset, inset, inset + len);
        g.strokeLine(w - inset, inset, w - inset - len, inset);
        g.strokeLine(w - inset, inset, w - inset, inset + len);
        g.strokeLine(inset, h - inset, inset + len, h - inset);
        g.strokeLine(inset, h - inset, inset, h - inset - len);
        g.strokeLine(w - inset, h - inset, w - inset - len, h - inset);
        g.strokeLine(w - inset, h - inset, w - inset, h - inset - len);

        g.setStroke(Color.web("#14b8a6", 0.18 + 0.12 * (0.5 + 0.5 * Math.sin(time * 4.4))));
        g.setLineWidth(1);
        // Neko：猫耳 + 胡须 + 奔跑小猫加载动画
        g.setFill(Color.web("#052e2b", 0.92));
        g.fillPolygon(new double[]{cx - 62, cx - 38, cx - 20}, new double[]{cy - 52, cy - 96, cy - 52}, 3);
        g.fillPolygon(new double[]{cx + 62, cx + 38, cx + 20}, new double[]{cy - 52, cy - 96, cy - 52}, 3);
        g.setStroke(Color.web("#5eead4", 0.65));
        g.setLineWidth(1.4);
        g.strokePolygon(new double[]{cx - 62, cx - 38, cx - 20}, new double[]{cy - 52, cy - 96, cy - 52}, 3);
        g.strokePolygon(new double[]{cx + 62, cx + 38, cx + 20}, new double[]{cy - 52, cy - 96, cy - 52}, 3);
        g.setStroke(Color.web("#99f6e4", 0.35));
        for (int i = -1; i <= 1; i++) {
            g.strokeLine(cx - 34, cy + 8 + i * 6, cx - 72, cy + 2 + i * 9);
            g.strokeLine(cx + 34, cy + 8 + i * 6, cx + 72, cy + 2 + i * 9);
        }
        double catX = 34 + Math.max(0, Math.min(1, progress)) * Math.max(0, w - 150);
        drawRunningCat(g, catX, h - 78, 20, time);
        g.strokeRect(10, 10, w - 20, h - 20);
    }

    private void drawRunningCat(GraphicsContext g, double x, double y, double size, double time) {
        g.setFill(Color.web("#5eead4", 0.85));
        g.fillOval(x, y, size * 1.5, size * 0.75);
        g.fillOval(x + size * 1.15, y - size * 0.45, size * 0.72, size * 0.62);
        g.setFill(Color.web("#042f2e"));
        g.fillPolygon(new double[]{x + size * 1.22, x + size * 1.34, x + size * 1.42},
                new double[]{y - size * 0.34, y - size * 0.83, y - size * 0.42}, 3);
        g.fillPolygon(new double[]{x + size * 1.55, x + size * 1.68, x + size * 1.76},
                new double[]{y - size * 0.34, y - size * 0.83, y - size * 0.42}, 3);
        g.setStroke(Color.web("#99f6e4", 0.85));
        g.setLineWidth(2);
        double phase = Math.sin(time * 14);
        g.strokeLine(x + size * 0.30, y + size * 0.55, x + size * 0.18 + phase * 8, y + size * 0.95);
        g.strokeLine(x + size * 0.70, y + size * 0.55, x + size * 0.82 - phase * 8, y + size * 0.95);
        g.strokeLine(x + size * 1.25, y + size * 0.35, x + size * 1.42 + phase * 7, y + size * 0.72);
        g.strokeLine(x + size * 1.48, y + size * 0.35, x + size * 1.66 - phase * 7, y + size * 0.72);
        g.strokeLine(x - size * 0.02, y + size * 0.12, x - size * 0.38, y - size * 0.30 + phase * 6);
    }

    private void finish() {
        if (finishing) return;
        finishing = true;
        timer.stop();
        FadeTransition fade = new FadeTransition(
                Duration.millis(Motion.isEnabled() ? 420 : 120), this);
        fade.setToValue(0);
        fade.setOnFinished(e -> {
            if (getParent() instanceof StackPane parent) parent.getChildren().remove(this);
            if (onFinished != null) onFinished.run();
        });
        fade.play();
    }
}
