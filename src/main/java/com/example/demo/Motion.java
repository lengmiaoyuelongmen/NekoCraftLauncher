package com.example.demo;

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

    public static final Duration FAST = Duration.millis(100);
    public static final Duration NORMAL = Duration.millis(180);
    public static final Duration SLOW = Duration.millis(420);

    /** ease-out-expo，起步快、收尾慢，比默认曲线更有“质感”。 */
    public static final Interpolator OUT = Interpolator.SPLINE(0.16, 1, 0.3, 1);
    public static final Interpolator IN_OUT = Interpolator.SPLINE(0.65, 0, 0.35, 1);
    public static final Interpolator SPRING = Interpolator.SPLINE(0.22, 1.0, 0.36, 1.0);

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
        pageIn(node, 26);
    }

    /**
     * 页面进入：淡入 + 横向滑入 + 轻微上浮 + 缩放回正。
     * fromX 为正从右侧进入，为负从左侧进入，让前进/后退有方向感。
     */
    public static void pageIn(Node node, double fromX) {
        stopPending(node);
        int seq = nextSeq(node);
        node.setOpacity(0);
        node.setTranslateX(fromX);
        node.setTranslateY(6);
        node.setScaleX(1);
        node.setScaleY(1);
        if (!enabled) {
            resetPage(node);
            return;
        }
        FadeTransition fade = new FadeTransition(scaled(NORMAL), node);
        fade.setToValue(1);
        fade.setInterpolator(OUT);
        TranslateTransition slide = new TranslateTransition(scaled(NORMAL), node);
        slide.setToX(0);
        slide.setToY(0);
        slide.setInterpolator(OUT);
        ParallelTransition transition = new ParallelTransition(fade, slide);
        transition.setOnFinished(e -> {
            if (seqOf(node) == seq) resetPage(node);
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
        FadeTransition fade = new FadeTransition(scaled(FAST), node);
        fade.setToValue(0);
        fade.setInterpolator(IN_OUT);
        TranslateTransition slide = new TranslateTransition(scaled(FAST), node);
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
            node.setTranslateY(16);
            node.setScaleX(0.986);
            node.setScaleY(0.986);
            PauseTransition delay = new PauseTransition(Duration.millis(40L * i));
            int index = i;
            delay.setOnFinished(e -> {
                FadeTransition fade = new FadeTransition(scaled(Duration.millis(260)), node);
                fade.setToValue(1);
                fade.setInterpolator(OUT);
                TranslateTransition rise = new TranslateTransition(scaled(Duration.millis(320 + index * 8)), node);
                rise.setToY(0);
                rise.setInterpolator(OUT);
                ScaleTransition scale = new ScaleTransition(scaled(Duration.millis(320)), node);
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
        node.setTranslateY(12);
        PauseTransition delay = new PauseTransition(Duration.millis(Math.max(0, delayMs)));
        delay.setOnFinished(e -> {
            FadeTransition fade = new FadeTransition(scaled(Duration.millis(240)), node);
            fade.setToValue(1);
            fade.setInterpolator(OUT);
            TranslateTransition rise = new TranslateTransition(scaled(Duration.millis(300)), node);
            rise.setToY(0);
            rise.setInterpolator(OUT);
            new ParallelTransition(fade, rise).play();
        });
        delay.play();
    }

    /** 按压反馈：按下缩到 0.96，松开弹性回弹。不改布局，只改渲染变换。 */
    public static void press(Node node) {
        if (!enabled) return;
        node.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> scaleTo(node, 0.975, FAST, OUT));
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
}
