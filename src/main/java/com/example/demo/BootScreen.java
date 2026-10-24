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
            "开源许可校验", "集体算力唤醒", "同志档案同步", "同志，欢迎回来"
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
        setStyle("-fx-background-color: #050608;");
        setPickOnBounds(true);
        setFocusTraversable(true);

        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());
        getChildren().add(canvas);

        Label brand = new Label("NEKO CORE");
        brand.setStyle("-fx-text-fill: #ff5a4a; -fx-font-size: 15px; -fx-font-weight: bold;"
                + " -fx-font-family: 'Cascadia Mono', 'Consolas', monospace;");
        Label sub = new Label("BOOT SEQUENCE / LOCAL SYSTEM");
        sub.setStyle("-fx-text-fill: rgba(255,90,74,0.55); -fx-font-size: 9px;"
                + " -fx-font-family: 'Cascadia Mono', 'Consolas', monospace;");
        VBox brandBox = new VBox(2, brand, sub);

        alertLabel = new Label("● COLLECTIVE CORE // ACTIVE  v" + AppVersion.CURRENT);
        alertLabel.setStyle("-fx-text-fill: rgba(255,74,58,0.85); -fx-font-size: 9px;"
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
        Label tagline = new Label("开放协作 · 为人民服务 · 代码属于人民");
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
        progressBar.setStyle("-fx-accent: #ff453a;");
        percentLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.72); -fx-font-size: 10px;"
                + " -fx-font-family: 'Cascadia Mono', 'Consolas', monospace;");
        percentLabel.setMinWidth(36);
        statusLabel.setStyle("-fx-text-fill: rgba(255,90,74,0.72); -fx-font-size: 10px;"
                + " -fx-font-family: 'Cascadia Mono', 'Consolas', monospace;");
        HBox progressRow = new HBox(10, progressBar, percentLabel);
        progressRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(progressBar, Priority.ALWAYS);
        Label terminal = new Label("comrade@neko:~$ sudo ./boot --for-the-people");
        terminal.setStyle("-fx-text-fill: rgba(255,205,110,0.55); -fx-font-size: 8.5px;"
                + " -fx-font-family: 'Cascadia Mono', 'Consolas', monospace;");
        VBox bottom = new VBox(6, terminal, statusLabel, progressRow,
                new Label("为爱发电 · 永远免费更新  //  Esc 跳过") {{
                    setStyle("-fx-text-fill: rgba(255,255,255,0.25); -fx-font-size: 8.5px;"
                            + " -fx-font-family: 'Cascadia Mono', 'Consolas', monospace;");
                }});
        bottom.setPadding(new Insets(0, 28, 22, 28));
        bottom.setMaxWidth(Double.MAX_VALUE);

        VBox leftThreat = new VBox(6,
                threatLabel("★ 全世界无产者，联合起来！", "rgba(255,92,70,0.95)"),
                threatLabel("为人民服务", "rgba(255,205,110,0.72)"),
                threatLabel("各尽所能，按需分配", "rgba(255,150,120,0.55)"),
                threatLabel("代码属于人民 · 开源共享", "rgba(255,150,120,0.38)"));
        leftThreat.setPadding(new Insets(0, 0, 0, 28));
        leftThreat.setMaxWidth(180);
        leftThreat.setTranslateY(126);
        VBox rightThreat = new VBox(6,
                threatLabel("FREE AS IN FREEDOM", "rgba(255,205,110,0.8)"),
                threatLabel("OPEN SOURCE / GPL", "rgba(255,150,120,0.55)"),
                threatLabel("SHARED COMPUTE", "rgba(255,150,120,0.5)"),
                threatLabel("POWER TO THE PEOPLE", "rgba(255,92,70,0.55)"));
        rightThreat.setPadding(new Insets(0, 28, 0, 0));
        rightThreat.setMaxWidth(180);
        rightThreat.setAlignment(Pos.CENTER_RIGHT);
        rightThreat.getChildren().forEach(node -> ((Label) node).setAlignment(Pos.CENTER_RIGHT));

        BorderPane overlay = new BorderPane();
        overlay.setTop(top);
        overlay.setLeft(leftThreat);
        overlay.setRight(rightThreat);
        BorderPane.setAlignment(leftThreat, Pos.CENTER_LEFT);
        BorderPane.setAlignment(rightThreat, Pos.CENTER_RIGHT);
        overlay.setCenter(centerGroup);
        overlay.setBottom(bottom);
        overlay.setPickOnBounds(false);
        BorderPane.setAlignment(centerGroup, Pos.CENTER);
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
                statusLabel.setText(progress < 0.32 ? "正在唤醒集体算力…"
                        : progress < 0.62 ? "开源协议与依赖校验…"
                        : progress < 0.9 ? "正在同步同志档案…" : "同志，欢迎回来");
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
        g.setStroke(Color.web("#ffcf6a", 0.65));
        g.setLineWidth(1);
        g.strokeLine(x, y, x, y + h + 6);
        g.setFill(Color.web("#d81e1e", 0.82));
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
        g.setFill(Color.web("#ffcf6a", 0.9));
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
        g.setStroke(Color.web("#ff3b30", 0.035));
        for (double x = 0; x < w; x += 64) g.strokeLine(x, 0, x, h);
        for (double y = 0; y < h; y += 64) g.strokeLine(0, y, w, y);

        double scanY = (time * 130) % (h + 120) - 60;
        g.setFill(Color.web("#ff2d20", 0.035));
        g.fillRect(0, scanY, w, 54);
        g.setStroke(Color.web("#ff5a48", 0.13));
        g.strokeLine(0, scanY, w, scanY);

        if (((int) (time * 3)) % 7 < 2) {
            for (int i = 0; i < 4; i++) {
                double gy = (time * 190 + i * 137) % h;
                g.setFill(Color.web("#ff3b30", 0.055));
                g.fillRect(0, gy, w, i % 2 == 0 ? 1 : 3);
            }
        }

        RadialGradient vignette = new RadialGradient(0, 0, 0.5, 0.5, 0.72, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.TRANSPARENT), new Stop(0.62, Color.TRANSPARENT),
                new Stop(1, Color.web("#2a0300", 0.55)));
        g.setFill(vignette);
        g.fillRect(0, 0, w, h);

        double cx = w / 2;
        double cy = h * 0.40;
        RadialGradient glow = new RadialGradient(0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#ff2d20", 0.16)),
                new Stop(0.45, Color.web("#ff2d20", 0.05)),
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
        g.setFill(Color.web("#ff2d20", 0.04));
        g.fillPolygon(wmX, wmY, 10);
        g.setStroke(Color.web("#ffcf6a", 0.06));
        g.strokePolygon(wmX, wmY, 10);

        drawFlag(g, 30, 104, 88, 48, time);

        for (int i = 0; i < 4; i++) {
            double radius = 86 + i * 32 + Math.sin(time * 0.6 + i) * 2;
            g.setStroke(Color.web("#ff453a", 0.18 - i * 0.03));
            g.setLineWidth(1);
            g.strokeOval(cx - radius, cy - radius, radius * 2, radius * 2);
        }
        g.setStroke(Color.web("#ff5a48", 0.45));
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
            g.setStroke(Color.web("#ff453a", i % 4 == 0 ? 0.5 : 0.18));
            g.strokeLine(cx + Math.cos(angle) * inner, cy + Math.sin(angle) * inner,
                    cx + Math.cos(angle) * outer, cy + Math.sin(angle) * outer);
        }

        for (int i = 0; i < 12; i++) {
            double a1 = Math.toRadians(i * 30 + time * 4);
            double a2 = Math.toRadians(i * 30 + 22 + time * 4);
            double r1 = 60;
            double r2 = 76;
            g.setFill(Color.web(i % 2 == 0 ? "#0d0303" : "#170505"));
            g.fillPolygon(new double[]{cx + Math.cos(a1) * r1, cx + Math.cos(a1) * r2,
                            cx + Math.cos(a2) * r2, cx + Math.cos(a2) * r1},
                    new double[]{cy + Math.sin(a1) * r1, cy + Math.sin(a1) * r2,
                            cy + Math.sin(a2) * r2, cy + Math.sin(a2) * r1}, 4);
            g.setStroke(Color.web("#ff453a", 0.16));
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

        g.setFill(Color.web("#120405"));
        g.fillOval(cx - 58, cy - 58, 116, 116);
        g.setStroke(Color.web("#ff5a48", 0.75));
        g.setLineWidth(1.5);
        g.strokeOval(cx - 58, cy - 58, 116, 116);
        RadialGradient iris = new RadialGradient(0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#ff8a72")), new Stop(0.55, Color.web("#ff3020")),
                new Stop(1, Color.web("#4a0905")));
        g.setFill(iris);
        g.fillOval(ex - 30, ey - 30, 60, 60);
        for (int i = 0; i < 28; i++) {
            double a = Math.toRadians(i * (360.0 / 28) + time * 8);
            g.setStroke(Color.web("#ffb0a0", 0.16));
            g.strokeLine(ex + Math.cos(a) * 14, ey + Math.sin(a) * 14,
                    ex + Math.cos(a) * 29, ey + Math.sin(a) * 29);
        }
        g.setFill(Color.web("#070203"));
        g.fillOval(ex - 8, ey - 23, 16, 46);
        g.setStroke(Color.web("#ff6b57", 0.35));
        g.strokeOval(ex - 8, ey - 23, 16, 46);
        g.setFill(Color.web("#ffd8d0", 0.8));
        g.fillOval(ex - 16, ey - 18, 8, 8);
        g.setStroke(Color.web("#ff6b57", 0.55));
        g.strokeLine(cx - 78, cy, cx + 78, cy);
        double sweepY = cy - 70 + (time * 36) % 140;
        g.setStroke(Color.web("#ff6b57", 0.18));
        g.strokeLine(cx - 70, sweepY, cx + 70, sweepY);

        g.setStroke(Color.web("#ff453a", 0.34));
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

        g.setStroke(Color.web("#ff2d20", 0.18 + 0.12 * (0.5 + 0.5 * Math.sin(time * 4.4))));
        g.setLineWidth(1);
        g.strokeRect(10, 10, w - 20, h - 20);
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
