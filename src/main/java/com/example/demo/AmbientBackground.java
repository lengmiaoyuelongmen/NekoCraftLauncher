package com.example.demo;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;

import java.util.Random;

/**
 * 启动器实时渲染背景：缓慢漂移的灰阶光晕 + 细微粒子。
 * 用 Canvas 每帧绘制，不依赖图片、不触发布局。
 */
public final class AmbientBackground extends Canvas {

    private final AnimationTimer timer;
    private long lastFrame;
    private double time;
    private boolean pointerActive;
    private double pointerX;
    private double pointerY;
    private double smoothX;
    private double smoothY;
    private boolean wasAnimating = true;
    private final double[] starX = new double[24];
    private final double[] starY = new double[24];
    private final double[] starSize = new double[24];
    private final double[] starPhase = new double[24];

    public AmbientBackground() {
        setMouseTransparent(true);
        setOpacity(0.95);
        Random random = new Random(20261024L);
        for (int i = 0; i < starX.length; i++) {
            starX[i] = random.nextDouble();
            starY[i] = random.nextDouble();
            starSize[i] = 0.8 + random.nextDouble() * 1.6;
            starPhase[i] = random.nextDouble() * Math.PI * 2;
        }
        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (!Motion.isEnabled()) return;
                if (getScene() == null
                        || !(getScene().getWindow() instanceof javafx.stage.Stage stage)
                        || stage.isIconified() || !stage.isShowing() || !stage.isFocused()) {
                    lastFrame = 0;
                    return;
                }
                // 24fps 足够表现缓动背景，显著降低空闲 CPU / GPU 占用
                if (now - lastFrame < 500_000_000L) return;
                lastFrame = now;
                time += 0.500;
                draw();
            }
        };
        timer.start();
    }

    public void stop() {
        timer.stop();
    }

    public void start() {
        lastFrame = 0;
        timer.start();
    }

    public void setPointer(double x, double y) {
        if (!pointerActive) {
            smoothX = x;
            smoothY = y;
        }
        pointerX = x;
        pointerY = y;
        pointerActive = true;
    }

    public void clearPointer() {
        pointerActive = false;
    }

    private void draw() {
        double w = getWidth();
        double h = getHeight();
        if (w < 4 || h < 4) return;
        GraphicsContext g = getGraphicsContext2D();
        if (!Motion.isEnabled()) {
            if (wasAnimating) {
                g.clearRect(0, 0, w, h);
                wasAnimating = false;
            }
            return;
        }
        wasAnimating = true;
        g.clearRect(0, 0, w, h);
        boolean light = Theme.isLight();
        Color accent = Color.web(Theme.current().accent());
        Color accentSoft = Color.color(accent.getRed(), accent.getGreen(), accent.getBlue(), light ? 0.06 : 0.10);

        blob(g, w * (0.24 + Math.sin(time * 0.09) * 0.05),
                h * (0.22 + Math.cos(time * 0.07) * 0.06),
                Math.max(w, h) * 0.62,
                Color.color(accent.getRed(), accent.getGreen(), accent.getBlue(), light ? 0.07 : 0.15));
        blob(g, w * (0.78 + Math.cos(time * 0.06) * 0.05),
                h * (0.72 + Math.sin(time * 0.08) * 0.07),
                Math.max(w, h) * 0.55,
                Color.color(accent.getRed(), accent.getGreen(), accent.getBlue(), light ? 0.05 : 0.08));
        blob(g, w * (0.55 + Math.sin(time * 0.05) * 0.07),
                h * (0.35 + Math.cos(time * 0.11) * 0.05),
                Math.max(w, h) * 0.36,
                Color.color(accent.getRed(), accent.getGreen(), accent.getBlue(), light ? 0.04 : 0.06));

        double scx = w * 0.5;
        double scy = h * 0.46;
        double[] star = new double[20];
        for (int i = 0; i < 10; i++) {
            double angle = Math.toRadians(-90 + i * 36);
            double radius = i % 2 == 0 ? Math.min(w, h) * 0.32 : Math.min(w, h) * 0.125;
            star[i * 2] = scx + Math.cos(angle) * radius;
            star[i * 2 + 1] = scy + Math.sin(angle) * radius;
        }
        double[] sx = new double[10];
        double[] sy = new double[10];
        for (int i = 0; i < 10; i++) { sx[i] = star[i * 2]; sy[i] = star[i * 2 + 1]; }
        g.setFill(light ? Color.rgb(120, 10, 20, 0.022) : Color.rgb(225, 29, 46, 0.028));
        g.fillPolygon(sx, sy, 10);
        g.setStroke(light ? Color.rgb(120, 10, 20, 0.035) : Color.rgb(255, 205, 110, 0.04));
        g.strokePolygon(sx, sy, 10);

        // Neko 爪印：低对比度装饰，保持可读性
        paw(g, w * 0.84, h * 0.20, Math.min(w, h) * 0.035, accentSoft);
        paw(g, w * 0.13, h * 0.78, Math.min(w, h) * 0.026, accentSoft);

        if (pointerActive) {
            smoothX += (pointerX - smoothX) * 0.14;
            smoothY += (pointerY - smoothY) * 0.14;
            blob(g, smoothX, smoothY, 210,
                    light ? Color.rgb(15, 23, 42, 0.055) : Color.rgb(255, 255, 255, 0.065));
        }

        for (int i = 0; i < starX.length; i++) {
            double x = (starX[i] + time * 0.004 * (0.3 + (i % 5) * 0.16)) % 1.0;
            double y = starY[i] + Math.sin(time * 0.6 + starPhase[i]) * 0.004;
            double alpha = 0.10 + 0.10 * (0.5 + 0.5 * Math.sin(time * 1.3 + starPhase[i]));
            g.setFill(light ? Color.rgb(90, 10, 20, alpha * 0.5) : Color.rgb(255, 120, 100, alpha));
            g.fillOval(x * w, y * h, starSize[i], starSize[i]);
        }
    }

    private void paw(GraphicsContext g, double cx, double cy, double r, Color color) {
        g.setFill(color);
        g.fillOval(cx - r * 0.42, cy + r * 0.08, r * 0.84, r * 0.70);
        g.fillOval(cx - r * 0.72, cy - r * 0.42, r * 0.34, r * 0.42);
        g.fillOval(cx - r * 0.16, cy - r * 0.60, r * 0.34, r * 0.42);
        g.fillOval(cx + r * 0.38, cy - r * 0.42, r * 0.34, r * 0.42);
    }

    private void blob(GraphicsContext g, double cx, double cy, double radius, Color color) {
        RadialGradient gradient = new RadialGradient(0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
                new Stop(0, color), new Stop(0.55, color.deriveColor(0, 1, 1, 0.45)),
                new Stop(1, Color.TRANSPARENT));
        g.setFill(gradient);
        g.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);
    }
}
