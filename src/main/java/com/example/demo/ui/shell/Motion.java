package com.example.demo.ui.shell;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;

import java.util.List;

/**
 * 统一动效：时长、缓动、页面切换、交错入场、按压反馈。
 * 设置里可以整体关闭（省电 / 老机器 / 远程桌面）。
 * 只改 opacity / translate / scale，不触发布局，保证动画开销低。
 */
public final class Motion {

    /** 交错入场的步进与时长：统一在这里调参，避免各处散落魔数。 */
    private static final long STAGGER_STEP_MS = 40L;
    private static final long STAGGER_FADE_MS = 260L;
    private static final long STAGGER_RISE_MS = 320L;
    private static final long REVEAL_FADE_MS = 240L;
    private static final long REVEAL_RISE_MS = 300L;
    private static final double REVEAL_OFFSET_Y = 12.0;
    private static final double STAGGER_OFFSET_Y = 16.0;

    public static final Duration FAST = Duration.millis(120);
    public static final Duration NORMAL = Duration.millis(210);
    public static final Duration SLOW = Duration.millis(420);

    /**
     * 缓动曲线。之前为了「线性动画」把三条都设成 LINEAR，结果所有动效一个味道。
     * 现在按用途分开：进场/位移用 ease-out-expo，往返用 ease-in-out，
     * 需要回弹的地方（按压、卡片悬停）用带过冲的曲线，观感才有层次。
     */
    public static final Interpolator OUT = Interpolator.SPLINE(0.16, 1.0, 0.30, 1.0);
    public static final Interpolator IN_OUT = Interpolator.SPLINE(0.65, 0.0, 0.35, 1.0);
    public static final Interpolator SPRING = Interpolator.SPLINE(0.34, 1.0, 0.64, 1.0);
    /** 页面级过渡专用：起手更快、收尾更稳。 */
    public static final Interpolator EMPHASIS = Interpolator.SPLINE(0.20, 0.0, 0.0, 1.0);

    private static boolean enabled = true;
    private static double speed = 1.0;

    private Motion() {
    }

    public static void configure(boolean animationsEnabled, double animationSpeed) {
        enabled = animationsEnabled;
        speed = animationSpeed <= 0 ? 1.0 : Math.min(3.0, animationSpeed);
    }

    public static boolean isEnabled() {
        return enabled;
    }

    private static Duration scaled(Duration duration) {
        return Duration.millis(duration.toMillis() / speed);
    }

    public static void pageIn(Node node) {
        pageIn(node, 18);
    }

    /**
     * 页面进入：淡入 + 横向滑入 + 轻微上浮 + 缩放回正。
     * fromX 为正从右侧进入，为负从左侧进入，让前进/后退有方向感。
     */
    public static void pageIn(Node node, double fromX) {
        pageIn(node, fromX, null);
    }

    /**
     * 带完成回调的页面进入：动画结束后回调一次（关闭动画时立即回调）。
     *
     * <p>冒烟用它在日志里确认「每个页面都真的跑过过渡」，动画被删掉或提前中断时能发现。
     */
    public static void pageIn(Node node, double fromX, Runnable onFinished) {
        stopPending(node);
        int seq = nextSeq(node);
        node.setOpacity(0);
        node.setTranslateX(fromX);
        node.setTranslateY(4);
        node.setScaleX(1);
        node.setScaleY(1);
        if (!enabled) {
            resetPage(node);
            if (onFinished != null) {
                onFinished.run();
            }
            return;
        }
        FadeTransition fade = new FadeTransition(scaled(NORMAL), node);
        fade.setToValue(1);
        fade.setInterpolator(OUT);
        TranslateTransition slide = new TranslateTransition(scaled(NORMAL), node);
        slide.setToX(0);
        slide.setToY(0);
        slide.setInterpolator(OUT);
        ScaleTransition grow = new ScaleTransition(scaled(Duration.millis(240)), node);
        grow.setFromX(0.985);
        grow.setFromY(0.985);
        grow.setToX(1);
        grow.setToY(1);
        grow.setInterpolator(EMPHASIS);
        ParallelTransition transition = new ParallelTransition(fade, slide, grow);
        transition.setOnFinished(e -> {
            if (seqOf(node) == seq) {
                resetPage(node);
            }
            if (onFinished != null) {
                onFinished.run();
            }
        });
        node.getProperties().put(ANIM_KEY, transition);
        transition.play();
    }

    private static void resetPage(Node node) {
        node.setOpacity(1);
        node.setTranslateX(0);
        node.setTranslateY(0);
        node.setScaleX(1);
        node.setScaleY(1);
    }

    /** 页面退出：淡出 + 向反方向轻滑 + 微微缩小，结束隐藏。 */
    public static void pageOut(Node node, Runnable onFinished) {
        stopPending(node);
        int seq = nextSeq(node);
        if (!enabled) {
            node.setOpacity(0);
            if (onFinished != null) onFinished.run();
            return;
        }
        Duration outDuration = scaled(Duration.millis(140));
        FadeTransition fade = new FadeTransition(outDuration, node);
        fade.setToValue(0);
        fade.setInterpolator(IN_OUT);
        TranslateTransition slide = new TranslateTransition(outDuration, node);
        double currentX = node.getTranslateX();
        slide.setToX(currentX > 0 ? -18 : 18);
        slide.setInterpolator(IN_OUT);
        ParallelTransition out = new ParallelTransition(fade, slide);
        out.setOnFinished(e -> {
            if (seqOf(node) != seq) return;
            node.setVisible(false);
            node.setTranslateX(0);
            node.setTranslateY(0);
            node.setScaleX(1);
            node.setScaleY(1);
            node.setOpacity(1);
            if (onFinished != null) onFinished.run();
        });
        node.getProperties().put(ANIM_KEY, out);
        out.play();
    }

    private static final String SEQ_KEY = "mc.motion.seq";
    private static final String ANIM_KEY = "mc.motion.anim";

    private static void stopPending(Node node) {
        Object previous = node.getProperties().get(ANIM_KEY);
        if (previous instanceof javafx.animation.Animation animation) {
            animation.stop();
        }
    }

    private static int nextSeq(Node node) {
        int seq = seqOf(node) + 1;
        node.getProperties().put(SEQ_KEY, seq);
        return seq;
    }

    private static int seqOf(Node node) {
        Object value = node.getProperties().get(SEQ_KEY);
        return value instanceof Number number ? number.intValue() : 0;
    }

    /** 首屏卡片交错入场（每个节点延迟 40ms，淡入 + 上浮 + 回正）。 */
    public static void stagger(List<? extends Node> nodes) {
        if (!enabled) return;
        for (int i = 0; i < nodes.size(); i++) {
            Node node = nodes.get(i);
            if (Boolean.TRUE.equals(node.getProperties().get("mc.staggered"))) continue;
            node.getProperties().put("mc.staggered", true);
            node.setOpacity(0);
            node.setTranslateY(STAGGER_OFFSET_Y);
            node.setScaleX(0.986);
            node.setScaleY(0.986);
            PauseTransition delay = new PauseTransition(Duration.millis(STAGGER_STEP_MS * i));
            int index = i;
            delay.setOnFinished(e -> {
                FadeTransition fade = new FadeTransition(scaled(Duration.millis(STAGGER_FADE_MS)), node);
                fade.setToValue(1);
                fade.setInterpolator(OUT);
                TranslateTransition rise =
                        new TranslateTransition(
                                scaled(Duration.millis(STAGGER_RISE_MS + index * 8L)), node);
                rise.setToY(0);
                rise.setInterpolator(OUT);
                ScaleTransition scale =
                        new ScaleTransition(scaled(Duration.millis(STAGGER_RISE_MS)), node);
                scale.setToX(1);
                scale.setToY(1);
                scale.setInterpolator(SPRING);
                new ParallelTransition(fade, rise, scale).play();
            });
            delay.play();
        }
    }

    /** 单个节点延迟入场（卡片/空状态/日志行）。 */
    public static void reveal(Node node, long delayMs) {
        if (!enabled) return;
        node.setOpacity(0);
        node.setTranslateY(REVEAL_OFFSET_Y);
        PauseTransition delay = new PauseTransition(Duration.millis(Math.max(0, delayMs)));
        delay.setOnFinished(e -> {
            FadeTransition fade = new FadeTransition(scaled(Duration.millis(REVEAL_FADE_MS)), node);
            fade.setToValue(1);
            fade.setInterpolator(OUT);
            TranslateTransition rise =
                    new TranslateTransition(scaled(Duration.millis(REVEAL_RISE_MS)), node);
            rise.setToY(0);
            rise.setInterpolator(OUT);
            new ParallelTransition(fade, rise).play();
        });
        delay.play();
    }

    /** 按压反馈：按下缩到 0.96，松开弹性回弹。不改布局，只改渲染变换。 */
    public static void press(Node node) {
        if (!enabled) return;
        ripple(node);   // 按压同时带水波，统一所有按钮的点击反馈
        node.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> scaleTo(node, 0.986, FAST, OUT));
        node.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> scaleTo(node, 1.0, FAST, SPRING));
        node.addEventHandler(MouseEvent.MOUSE_EXITED, e -> scaleTo(node, 1.0, FAST, OUT));
    }

    /** 悬停微放大（卡片 / 主按钮用）。 */
    /** PCL-CE 风格卡片悬停：轻微上浮 + 阴影扩散，不做整行 CSS 重算。 */
    public static void hoverCard(javafx.scene.Node node) {
        if (!enabled) return;
        if (node.getProperties().put("mc.hover-card", Boolean.TRUE) != null) return;
        javafx.scene.effect.DropShadow shadow = new javafx.scene.effect.DropShadow(
                0, javafx.scene.paint.Color.web("#14b8a6", 0.20));
        shadow.setOffsetY(2.5);
        node.setEffect(shadow);
        node.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> animateCard(node, 1.006, -2.0, shadow, 10));
        node.addEventHandler(MouseEvent.MOUSE_EXITED, e -> animateCard(node, 1.0, 0.0, shadow, 0));
    }

    private static void animateCard(javafx.scene.Node node, double scale, double translateY,
                                     javafx.scene.effect.DropShadow shadow, double radius) {
        ScaleTransition scaleTransition = new ScaleTransition(scaled(Duration.millis(135)), node);
        scaleTransition.setToX(scale);
        scaleTransition.setToY(scale);
        scaleTransition.setInterpolator(scale > 1 ? OUT : IN_OUT);
        TranslateTransition lift = new TranslateTransition(scaled(Duration.millis(135)), node);
        lift.setToY(translateY);
        lift.setInterpolator(scale > 1 ? OUT : IN_OUT);
        javafx.animation.Timeline glow = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(scaled(Duration.millis(135)),
                        new javafx.animation.KeyValue(shadow.radiusProperty(), radius, OUT)));
        new ParallelTransition(scaleTransition, lift, glow).play();
    }

    public static void hoverScale(Node node, double scale) {
        if (!enabled) return;
        node.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> scaleTo(node, scale, NORMAL, SPRING));
        node.addEventHandler(MouseEvent.MOUSE_EXITED, e -> scaleTo(node, 1.0, NORMAL, OUT));
    }

    /** 悬停轻抬：放大 1.012 + 上移 2.5px，列表卡片用，比纯换色更明显。 */
    public static void hoverLift(Node node) {
        if (!enabled) return;
        node.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> animateLift(node, 1.012, -2.5, NORMAL));
        node.addEventHandler(MouseEvent.MOUSE_EXITED, e -> animateLift(node, 1.0, 0, NORMAL));
    }

    private static void animateLift(Node node, double scale, double translateY, Duration duration) {
        ScaleTransition scaleTransition = new ScaleTransition(scaled(duration), node);
        scaleTransition.setToX(scale);
        scaleTransition.setToY(scale);
        scaleTransition.setInterpolator(scale > 1 ? SPRING : OUT);
        TranslateTransition lift = new TranslateTransition(scaled(duration), node);
        lift.setToY(translateY);
        lift.setInterpolator(scale > 1 ? SPRING : OUT);
        new ParallelTransition(scaleTransition, lift).play();
    }

    private static void scaleTo(Node node, double target, Duration duration, Interpolator interpolator) {
        ScaleTransition scale = new ScaleTransition(scaled(duration), node);
        scale.setToX(target);
        scale.setToY(target);
        scale.setInterpolator(interpolator);
        scale.play();
    }

    /** 数值脉冲：强调某个 Label 时用。 */
    public static void pulse(Node node) {
        if (!enabled) return;
        ScaleTransition scale = new ScaleTransition(scaled(Duration.millis(180)), node);
        scale.setFromX(1);
        scale.setFromY(1);
        scale.setToX(1.08);
        scale.setToY(1.08);
        scale.setAutoReverse(true);
        scale.setCycleCount(2);
        scale.setInterpolator(IN_OUT);
        scale.play();
    }

    /**
     * 点击水波：从鼠标落点扩散一圈半透明圆，300ms 内放大并淡出。
     *
     * <p>圆用节点的 clip 裁掉溢出部分，所以圆角按钮里也不会糊到外面；
     * 只改渲染，不触发布局。同一节点重复点击会叠加多层，符合直觉。
     */
    public static void ripple(Node node) {
        if (!enabled) {
            return;
        }
        if (Boolean.TRUE.equals(node.getProperties().get("mc.ripple"))) {
            return;
        }
        node.getProperties().put("mc.ripple", Boolean.TRUE);
        // 用场景坐标：事件的 x/y 是相对「事件源」的，点中文字/图标时会落在子节点坐标系里，
        // 换算到父容器就会跑到左上角（之前水波总出现在左上角就是这个原因）
        node.addEventHandler(MouseEvent.MOUSE_PRESSED,
                event -> playRipple(node, event.getSceneX(), event.getSceneY()));
    }

    private static void playRipple(Node node, double sceneX, double sceneY) {
        if (!enabled) {
            return;
        }
        javafx.scene.Scene scene = node.getScene();
        if (scene == null || !(scene.getRoot() instanceof javafx.scene.layout.Pane root)) {
            return;
        }
        // 把按钮内的落点换算到窗口坐标系，水波画在窗口最上层，任何控件（Button / HBox / 卡片）都能用
        javafx.geometry.Point2D inRoot = root.sceneToLocal(sceneX, sceneY);
        double extent = 0;
        if (node instanceof javafx.scene.layout.Region region) {
            extent = Math.max(region.getWidth(), region.getHeight());
        }
        double radius = Math.max(14, extent * 0.55);
        javafx.scene.shape.Circle circle = new javafx.scene.shape.Circle(Math.max(4, radius * 0.18));
        circle.setCenterX(inRoot.getX());
        circle.setCenterY(inRoot.getY());
        circle.setMouseTransparent(true);
        // 关键：让容器跳过布局。之前这个圆是 managed 的，StackPane 会把它重新摆到左上角，
        // 于是「点哪都在左上角冒一下水波」
        circle.setManaged(false);
        circle.setFill(javafx.scene.paint.Color.web("#5eead4", 0.26));
        root.getChildren().add(circle);
        ScaleTransition grow = new ScaleTransition(scaled(Duration.millis(320)), circle);
        grow.setToX(radius / Math.max(1, circle.getRadius()));
        grow.setToY(radius / Math.max(1, circle.getRadius()));
        grow.setInterpolator(OUT);
        FadeTransition fade = new FadeTransition(scaled(Duration.millis(320)), circle);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        fade.setInterpolator(OUT);
        ParallelTransition ripple = new ParallelTransition(grow, fade);
        ripple.setOnFinished(e -> root.getChildren().remove(circle));
        ripple.play();
    }

    /** 弹窗入场：0.94 → 1 的缩放 + 淡入，和页面过渡同一套缓动。 */
    public static void dialogIn(Node node) {
        if (!enabled) {
            node.setOpacity(1);
            node.setScaleX(1);
            node.setScaleY(1);
            return;
        }
        node.setOpacity(0);
        node.setScaleX(0.94);
        node.setScaleY(0.94);
        FadeTransition fade = new FadeTransition(scaled(Duration.millis(180)), node);
        fade.setToValue(1);
        fade.setInterpolator(EMPHASIS);
        ScaleTransition scale = new ScaleTransition(scaled(Duration.millis(220)), node);
        scale.setToX(1);
        scale.setToY(1);
        scale.setInterpolator(SPRING);
        new ParallelTransition(fade, scale).play();
    }

    /** 弹窗关闭：快速缩小淡出，220ms 内完成。 */
    public static void dialogOut(Node node, Runnable after) {
        if (!enabled) {
            if (after != null) after.run();
            return;
        }
        FadeTransition fade = new FadeTransition(scaled(Duration.millis(140)), node);
        fade.setToValue(0);
        fade.setInterpolator(IN_OUT);
        ScaleTransition scale = new ScaleTransition(scaled(Duration.millis(140)), node);
        scale.setToX(0.96);
        scale.setToY(0.96);
        scale.setInterpolator(IN_OUT);
        ParallelTransition out = new ParallelTransition(fade, scale);
        out.setOnFinished(e -> {
            if (after != null) after.run();
        });
        out.play();
    }

    /** 吐司入场：从上方 12px 滑下 + 淡入。 */
    public static void toastIn(Node node) {
        if (!enabled) return;
        node.setOpacity(0);
        node.setTranslateY(-12);
        FadeTransition fade = new FadeTransition(scaled(Duration.millis(180)), node);
        fade.setToValue(1);
        fade.setInterpolator(OUT);
        TranslateTransition slide = new TranslateTransition(scaled(Duration.millis(240)), node);
        slide.setToY(0);
        slide.setInterpolator(SPRING);
        new ParallelTransition(fade, slide).play();
    }

    /**
     * 列表逐行入场：前 visibleLimit 行按 stepMs 错开淡入上浮，后面的直接显示。
     *
     * <p>为什么要限流：一次渲染 200 行模组时，逐行延迟会让最后一行等 5 秒才出现，
     * 看上去像卡住。只让「第一屏看得见的行」参与动画，剩下的立即出现。
     */
    public static void staggerRows(List<? extends Node> rows, long stepMs, int visibleLimit) {
        if (!enabled || rows == null) {
            return;
        }
        int limit = Math.min(Math.max(0, visibleLimit), rows.size());
        for (int i = 0; i < rows.size(); i++) {
            Node row = rows.get(i);
            if (Boolean.TRUE.equals(row.getProperties().get("mc.rowStaggered"))) {
                continue;
            }
            row.getProperties().put("mc.rowStaggered", Boolean.TRUE);
            if (i >= limit) {
                continue;
            }
            if (row.getOpacity() == 1.0 && row.getTranslateY() == 0.0) {
                reveal(row, i * stepMs);
            } else {
                reveal(row, 0);
            }
        }
    }

    /** 换图时做一次交叉淡入：先淡到 0，换图，再淡回 1。 */
    public static void crossFade(Node node, Runnable swap) {
        if (!enabled) {
            if (swap != null) swap.run();
            return;
        }
        FadeTransition out = new FadeTransition(scaled(Duration.millis(90)), node);
        out.setToValue(0.25);
        out.setInterpolator(IN_OUT);
        out.setOnFinished(e -> {
            if (swap != null) swap.run();
            FadeTransition in = new FadeTransition(scaled(Duration.millis(180)), node);
            in.setToValue(1);
            in.setInterpolator(OUT);
            in.play();
        });
        out.play();
    }
    /** 拖拽提示层：呼吸式缩放 + 边框亮度，提示「这里可以松手」。 */
    public static javafx.animation.Timeline dropPulse(Node node) {
        if (!enabled) return null;
        node.setScaleX(0.985);
        node.setScaleY(0.985);
        javafx.animation.Timeline timeline = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(Duration.ZERO,
                        new javafx.animation.KeyValue(node.scaleXProperty(), 0.985),
                        new javafx.animation.KeyValue(node.scaleYProperty(), 0.985)),
                new javafx.animation.KeyFrame(scaled(Duration.millis(760)),
                        new javafx.animation.KeyValue(node.scaleXProperty(), 1.012, Interpolator.EASE_BOTH),
                        new javafx.animation.KeyValue(node.scaleYProperty(), 1.012, Interpolator.EASE_BOTH)));
        timeline.setAutoReverse(true);
        timeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
        timeline.play();
        return timeline;
    }

    /** 松手确认：提示层向上一顶再淡出，和随后的安装进度衔接。 */
    public static void dropConfirm(Node node, Runnable after) {
        if (!enabled) {
            if (after != null) after.run();
            return;
        }
        node.setScaleX(1.0);
        node.setScaleY(1.0);
        ScaleTransition scale = new ScaleTransition(scaled(Duration.millis(260)), node);
        scale.setToX(1.06);
        scale.setToY(1.06);
        scale.setInterpolator(SPRING);
        FadeTransition fade = new FadeTransition(scaled(Duration.millis(260)), node);
        fade.setToValue(0);
        fade.setInterpolator(IN_OUT);
        ParallelTransition out = new ParallelTransition(scale, fade);
        out.setOnFinished(e -> {
            node.setVisible(false);
            node.setScaleX(1.0);
            node.setScaleY(1.0);
            node.setOpacity(1.0);
            if (after != null) after.run();
        });
        out.play();
    }

    /** 图标弹一下：拖拽层里的下载图标用，避免整层只有静态文字。 */
    public static void iconBounce(Node icon) {
        if (!enabled) return;
        TranslateTransition up = new TranslateTransition(scaled(Duration.millis(220)), icon);
        up.setByY(-8);
        up.setAutoReverse(true);
        up.setCycleCount(4);
        up.setInterpolator(SPRING);
        up.play();
    }
}
