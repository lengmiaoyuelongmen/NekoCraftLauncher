package com.example.demo.ui.shell;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;

/**
 * 启动器实时渲染背景：只有缓慢漂移的柔光晕，没有星星/爪印等装饰图形。
 *
 * <p>用 Canvas 每帧绘制，不依赖图片、不触发布局。
 * 省电策略：窗口失焦 / 最小化时不画；静止时约 2fps；渐变对象全部缓存。
 */
public final class AmbientBackground extends Canvas {

    private static final long FRAME_INTERVAL_NANOS = 500_000_000L; // 约 2fps，足够表现缓动

    private final AnimationTimer timer;
    private long lastFrame;
    private double time;
    private boolean wasAnimating = true;

    // 缓存的渐变（主题色 / 明暗变化时才重建）
    private RadialGradient[] blobGradients;
    private Color gradientAccent;
    private boolean gradientLight;

    public AmbientBackground() {
        setMouseTransparent(true);
        setOpacity(0.95);
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
                if (now - lastFrame < FRAME_INTERVAL_NANOS) return;
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
        ensureGradients(accent, light);

        blob(g, w * (0.24 + Math.sin(time * 0.09) * 0.05),
                h * (0.22 + Math.cos(time * 0.07) * 0.06),
                Math.max(w, h) * 0.62, blobGradients[0]);
        blob(g, w * (0.78 + Math.cos(time * 0.06) * 0.05),
                h * (0.72 + Math.sin(time * 0.08) * 0.07),
                Math.max(w, h) * 0.55, blobGradients[1]);
        blob(g, w * (0.55 + Math.sin(time * 0.05) * 0.07),
                h * (0.35 + Math.cos(time * 0.11) * 0.05),
                Math.max(w, h) * 0.36, blobGradients[2]);
    }

    /** 三个光晕渐变只随主题色/明暗变化，复用同一批对象。 */
    private void ensureGradients(Color accent, boolean light) {
        if (blobGradients != null && accent.equals(gradientAccent) && light == gradientLight) return;
        gradientAccent = accent;
        gradientLight = light;
        blobGradients = new RadialGradient[]{
                gradient(accent, light ? 0.07 : 0.15),
                gradient(accent, light ? 0.05 : 0.11),
                gradient(accent, light ? 0.04 : 0.09)
        };
    }

    private static RadialGradient gradient(Color accent, double alpha) {
        return new RadialGradient(
                0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.color(accent.getRed(), accent.getGreen(), accent.getBlue(), alpha)),
                new Stop(1, Color.color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0)));
    }

    private static void blob(GraphicsContext g, double cx, double cy, double radius, RadialGradient fill) {
        g.setFill(fill);
        g.fillOval(cx - radius / 2, cy - radius / 2, radius, radius);
    }
}
