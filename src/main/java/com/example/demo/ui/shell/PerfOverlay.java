package com.example.demo.ui.shell;

import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

/**
 * 启动器内置的性能叠加层：FPS / 堆占用 / 线程数。
 *
 * <p>为什么放在启动器里而不是只在 IDE 里看：打包后的版本跑在用户机器上，
 * 卡顿、掉帧只有现场能复现。这里用 {@link AnimationTimer} 统计真实帧间隔，
 * 每 500ms 更新一次；关掉时计时器停止，不产生任何开销。
 *
 * <p>叠在窗口右上角，鼠标穿透，不参与布局。
 */
public final class PerfOverlay extends StackPane {

    private static PerfOverlay latestInstance;

    private final Label label = new Label();
    private final AnimationTimer timer;
    private long lastFrameAt;
    private long lastUpdateAt;
    private double fps;
    private boolean active;

    public PerfOverlay() {
        latestInstance = this;
        setMouseTransparent(true);
        setPickOnBounds(false);
        setAlignment(Pos.TOP_RIGHT);
        setPadding(new Insets(6, 10, 0, 0));
        label.setStyle("-fx-text-fill: #7dd3fc; -fx-font-family: 'Consolas', monospace;"
                + " -fx-font-size: 10px; -fx-background-color: rgba(6,20,24,0.55);"
                + " -fx-background-radius: 6; -fx-padding: 2 6;");
        getChildren().add(label);

        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (!active) {
                    return;
                }
                if (lastFrameAt > 0) {
                    double deltaMs = (now - lastFrameAt) / 1_000_000.0;
                    if (deltaMs > 0.1) {
                        double instant = 1000.0 / deltaMs;
                        // 指数平滑，读数不抖
                        fps = fps <= 0 ? instant : fps * 0.85 + instant * 0.15;
                    }
                }
                lastFrameAt = now;
                if (now - lastUpdateAt > 500_000_000L) {
                    lastUpdateAt = now;
                    refresh();
                }
            }
        };
    }

    /** 开关（设置页切换时调用）。 */
    public void setActive(boolean value) {
        active = value;
        setVisible(value);
        setManaged(value);
        if (value) {
            lastFrameAt = 0;
            lastUpdateAt = 0;
            timer.start();
            refresh();
        } else {
            timer.stop();
        }
    }

    public boolean isActive() {
        return active;
    }

    /** 全局开关：设置页保存后立即生效。 */
    public static void setGlobalActive(boolean value) {
        PerfOverlay instance = latestInstance;
        if (instance != null) {
            instance.setActive(value);
        }
    }

    private void refresh() {
        Runtime runtime = Runtime.getRuntime();
        long heap = (runtime.totalMemory() - runtime.freeMemory()) / 1024L / 1024L;
        label.setText(String.format(java.util.Locale.ROOT,
                "FPS %.0f · HEAP %dM · THR %d", fps, heap, Thread.activeCount()));
    }
}
