package com.example.demo.ui.component;

import com.example.demo.core.app.RingSegments;
import com.example.demo.ui.shell.Motion;
import com.example.demo.ui.shell.Theme;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

/**
 * 环形进度：安装页用它替掉一根只会变长的进度条。
 *
 * <p>两点设计取舍：
 * <ul>
 *   <li><b>平滑过渡</b>：外部频繁调用 {@link #setProgress(double)} 时不会一格一格跳，
 *       内部用一个 160ms 的 Timeline 把弧线补到目标值；</li>
 *   <li><b>阶段感</b>：环中间显示百分比，阶段文案换行时由调用方做交叉淡入，
 *       让「正在装库 → 正在解包」这种切换看得出来。</li>
 * </ul>
 *
 * <p>用 Canvas 直接画弧，避免 Arc + 多层 StackPane 带来的布局开销。
 */
public final class RingProgress extends StackPane {

    private static final double SIZE = 92.0;
    private static final double STROKE = 8.0;

    private final Canvas canvas = new Canvas(SIZE, SIZE);
    // 深浅主题两套色：浅色主题下原来的淡青几乎看不见
    private Color track() {
        return Theme.isLight() ? Color.web("#0d9488", 0.18) : Color.web("#5eead4", 0.16);
    }

    private Color accent() {
        return Theme.isLight() ? Color.web("#0d9488") : Color.web("#5eead4");
    }
    private int segments = 0;
    private int stage = -1;
    private double target = 0;
    private double shown = 0;
    private Timeline animation;

    public RingProgress() {
        setPrefSize(SIZE, SIZE);
        setMinSize(SIZE, SIZE);
        setMaxSize(SIZE, SIZE);
        getChildren().add(canvas);
        redraw();
    }

    /** 分段数（安装阶段数）；0 或 1 时退回整圈连续弧。 */
    public void setSegments(int count) {
        this.segments = Math.max(0, count);
        redraw();
    }

    /** 当前阶段（0 基）：这一段画得更亮，用来指示「正在做第几步」。 */
    public void setStage(int index) {
        this.stage = index;
        redraw();
    }

    /** 设置进度（0..1），内部补间到目标值。 */
    public void setProgress(double value) {
        target = Math.max(0, Math.min(1, value));
        if (!Motion.isEnabled()) {
            shown = target;
            redraw();
            return;
        }
        if (animation != null) {
            animation.stop();
        }
        animation = new Timeline(new KeyFrame(Duration.millis(180),
                new KeyValue(progressProxy(), target, Interpolator.SPLINE(0.16, 1, 0.3, 1))));
        animation.play();
    }

    /** 当前显示值（测试/断言用）。 */
    public double progress() {
        return shown;
    }

    private javafx.beans.property.DoubleProperty proxy;

    private javafx.beans.property.DoubleProperty progressProxy() {
        if (proxy == null) {
            proxy = new javafx.beans.property.SimpleDoubleProperty(shown);
            proxy.addListener((o, old, value) -> {
                shown = value.doubleValue();
                redraw();
            });
        }
        return proxy;
    }

    private void redraw() {
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.clearRect(0, 0, SIZE, SIZE);
        double pad = STROKE / 2 + 1;
        double arcSize = SIZE - pad * 2;
        g.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        g.setTextAlign(TextAlignment.CENTER);
        g.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 17));

        if (segments <= 1) {
            g.setLineWidth(STROKE);
            g.setStroke(track());
            g.strokeArc(pad, pad, arcSize, arcSize, 90, -360, javafx.scene.shape.ArcType.OPEN);
            if (shown > 0.001) {
                g.setStroke(accent());
                g.strokeArc(pad, pad, arcSize, arcSize, 90, -360 * shown, javafx.scene.shape.ArcType.OPEN);
            }
        } else {
            // 分段：每段先画底，再按进度覆盖；当前阶段加粗提亮
            double slot = RingSegments.slotDegrees(segments);
            double sweep = RingSegments.sweepDegrees(segments, 5.0);
            double filledUpTo = shown * segments;
            for (int i = 0; i < segments; i++) {
                double start = RingSegments.startAngle(segments, i, 5.0);
                boolean done = i + 1 <= filledUpTo + 0.001;
                boolean current = i == stage;
                g.setLineWidth(current ? STROKE + 2.0 : STROKE);
                g.setStroke(done ? accent() : track());
                if (current && !done) {
                    // 正在做的那段用半亮，和「已完成」「未开始」区分开
                    g.setStroke(accent().deriveColor(0, 1.0, 1.0, 0.55));
                }
                g.strokeArc(pad, pad, arcSize, arcSize, start, -sweep, javafx.scene.shape.ArcType.OPEN);
            }
            // 段间空隙留白：靠 sweep 与 slot 的差额实现，不需要额外擦除
            if (slot <= 0) {
                g.setStroke(track());
            }
        }

        g.setFill(Theme.isLight() ? Color.web("#0f766e") : Color.web("#e5fbf6"));
        g.fillText(Math.round(shown * 100) + "%", SIZE / 2, SIZE / 2 + 6);
    }
}
