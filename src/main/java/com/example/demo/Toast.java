package com.example.demo;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * 轻量吐司提示：不打断操作、自动消失，替代一部分弹窗。
 * 挂在主窗口最上层，鼠标穿透。
 */
public final class Toast {

    public static final String OK = "ok";
    public static final String INFO = "info";
    public static final String WARN = "warn";
    public static final String ERROR = "error";

    private static VBox stack;
    private static StackPane host;

    private Toast() {
    }

    public static void install(StackPane sceneRoot) {
        host = sceneRoot;
        stack = new VBox(8);
        stack.setAlignment(Pos.TOP_CENTER);
        stack.setMouseTransparent(true);
        stack.setPickOnBounds(false);
        StackPane.setAlignment(stack, Pos.TOP_CENTER);
        StackPane.setMargin(stack, new Insets(60, 0, 0, 0));
        sceneRoot.getChildren().add(stack);
    }

    public static void ok(String message) {
        show(message, OK);
    }

    public static void info(String message) {
        show(message, INFO);
    }

    public static void warn(String message) {
        show(message, WARN);
    }

    public static void error(String message) {
        show(message, ERROR);
    }

    public static void show(String message, String kind) {
        if (message == null || message.isBlank()) return;
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> show(message, kind));
            return;
        }
        if (stack == null || host == null) return;
        if (stack.getChildren().size() >= 4) {
            stack.getChildren().remove(0);
        }

        String accent = switch (kind == null ? INFO : kind) {
            case OK -> "#e5e7eb";
            case WARN -> "#fbbf24";
            case ERROR -> "#f87171";
            default -> "#d1d5db";
        };
        String iconText = switch (kind == null ? INFO : kind) {
            case OK -> "✓";
            case WARN -> "!";
            case ERROR -> "✕";
            default -> "i";
        };

        Label icon = new Label(iconText);
        icon.setStyle("-fx-text-fill: " + accent + "; -fx-font-size: 13px; -fx-font-weight: bold;");

        Label text = new Label(message);
        text.setStyle("-fx-text-fill: " + (Theme.isLight() ? "#0f172a" : "#e5e7eb")
                + "; -fx-font-size: 12px;");
        text.setWrapText(true);
        text.setMaxWidth(460);

        HBox pill = new HBox(10, icon, text);
        pill.setAlignment(Pos.CENTER_LEFT);
        pill.setMaxWidth(Region.USE_PREF_SIZE);
        pill.setPadding(new Insets(10, 16, 10, 16));
        boolean light = Theme.isLight();
        pill.setStyle("-fx-background-color: " + (light ? "rgba(255,255,255,0.98)" : "rgba(28,31,39,0.96)") + ";"
                + " -fx-background-radius: 10;"
                + " -fx-border-color: " + (light ? "rgba(15,23,42,0.14)" : "rgba(255,255,255,0.10)") + ";"
                + " -fx-border-radius: 10;"
                + " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.20), 12, 0.08, 0, 4);");

        pill.setOpacity(0);
        pill.setTranslateY(-8);
        stack.getChildren().add(pill);

        FadeTransition in = new FadeTransition(Duration.millis(160), pill);
        in.setToValue(1);
        TranslateTransition drop = new TranslateTransition(Duration.millis(200), pill);
        drop.setToY(0);
        drop.setInterpolator(Motion.OUT);
        in.play();
        drop.play();

        PauseTransition wait = new PauseTransition(Duration.millis(2600));
        wait.setOnFinished(e -> {
            FadeTransition out = new FadeTransition(Duration.millis(180), pill);
            out.setToValue(0);
            TranslateTransition lift = new TranslateTransition(Duration.millis(180), pill);
            lift.setToY(-6);
            out.setOnFinished(done -> stack.getChildren().remove(pill));
            out.play();
            lift.play();
        });
        wait.play();
    }
}
