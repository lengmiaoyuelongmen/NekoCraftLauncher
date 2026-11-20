package com.example.demo.ui.shell;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;

/**
 * 跟随鼠标的柔光高光层。
 *
 * <p>为什么要单独一层：页面自己的背景是不透明的，{@link AmbientBackground} 那些光晕被盖住了，
 * 只调它的参数等于对空气上色。这一层画在页面之上、鼠标穿透，所以不管当前是哪个页面都看得到。
 *
 * <p>性能：只有指针在动的时候才按 20fps 追，静止 900ms 后降到 2fps；
 * 渐变对象只在主题颜色变化时重建。
 */
public final class MouseSheen extends Canvas {

    private static final long ACTIVE_INTERVAL = 50_000_000L;
    private static final long IDLE_INTERVAL = 500_000_000L;

    private final AnimationTimer timer;
    private double x = -1000;
    private double y = -1000;
    private double targetX = -1000;
    private double targetY = -1000;
    private long lastMove;
    private long lastFrame;
    private RadialGradient gradient;
    private Color gradientColor;
    private boolean gradientLight;

    public MouseSheen() {
        setMouseTransparent(true);
        setOpacity(1.0);
        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (!Motion.isEnabled()) {
                    return;
                }
                long interval = (now - lastMove) < 900_000_000L ? ACTIVE_INTERVAL : IDLE_INTERVAL;
                if (now - lastFrame < interval) {
                    return;
                }
                lastFrame = now;
                step();
            }
        };
        timer.start();
    }

    /** 挂到场景后开始接收鼠标位置。 */
    public void bindTo(javafx.scene.Scene scene) {
        scene.addEventFilter(MouseEvent.MOUSE_MOVED, event -> {
            targetX = event.getX();
            targetY = event.getY();
            lastMove = System.nanoTime();
        });
    }

    private void step() {
        if (getScene() == null || getWidth() < 4 || getHeight() < 4) {
            return;
        }
        // 0.18 的跟随系数：比背景视差跟得紧一些，光斑才有「跟着手走」的感觉
        x += (targetX - x) * 0.18;
        y += (targetY - y) * 0.18;
        draw();
    }

    private void draw() {
        GraphicsContext g = getGraphicsContext2D();
        g.clearRect(0, 0, getWidth(), getHeight());
        if (x < -500 || y < -500) {
            return;
        }
        boolean light = Theme.isLight();
        Color accent = Color.web(Theme.current().accent());
        if (gradient == null || !accent.equals(gradientColor) || light != gradientLight) {
            gradientColor = accent;
            gradientLight = light;
            double alpha = light ? 0.14 : 0.18;
            gradient = new RadialGradient(0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.color(accent.getRed(), accent.getGreen(), accent.getBlue(), alpha)),
                    new Stop(0.55, Color.color(accent.getRed(), accent.getGreen(), accent.getBlue(), alpha * 0.45)),
                    new Stop(1, Color.color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0)));
        }
        double radius = Math.max(420, Math.min(getWidth(), getHeight()) * 0.72);
        g.setFill(gradient);
        g.fillOval(x - radius / 2, y - radius / 2, radius, radius);
    }

    public void stop() {
        timer.stop();
    }

    public void start() {
        lastFrame = 0;
        timer.start();
    }
}
