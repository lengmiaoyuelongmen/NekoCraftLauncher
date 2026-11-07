package com.example.demo.ui.component;

import com.example.demo.ui.shell.Icons;
import com.example.demo.ui.shell.Motion;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.application.Platform;

import java.util.ArrayList;
import java.util.List;

/**
 * 命令面板（Ctrl+K）：搜索命令、↑↓ 选择、Enter 执行、Esc 或点背景关闭。
 *
 * <p>从主类搬出；命令清单由调用方提供，这里只管渲染、过滤、键盘导航与进场动画。
 */
public final class CommandPalette {

    /** 一条命令：图标、标题、说明、动作。 */
    public record Action(String icon, String title, String hint, Runnable run) {
    }

    private static final int MAX_ROWS = 9;

    private CommandPalette() {
    }

    /**
     * 构建并返回遮罩层（调用方负责挂到场景上）。
     *
     * @param actions 全部命令
     * @param close   关闭动作（Esc、点背景、执行命令前都调用）
     */
    public static StackPane create(List<Action> actions, Runnable close) {
        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(4,7,12,0.62);");
        StackPane.setAlignment(overlay, Pos.CENTER);

        VBox card = new VBox(10.0);
        card.setPrefWidth(560.0);
        card.setMaxWidth(560.0);
        card.setMaxHeight(Double.NEGATIVE_INFINITY);
        card.setPadding(new Insets(14.0));
        card.setStyle("-fx-background-color: linear-gradient(to bottom, -app-card-top, -app-card-alt2);"
                + " -fx-background-radius: 18; -fx-border-color: -app-border-soft;"
                + " -fx-border-radius: 18;"
                + " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.45), 40, 0.2, 0, 12);");

        TextField search = new TextField();
        search.setPromptText("搜索命令、页面、操作…");
        search.setPrefHeight(40.0);
        search.setStyle("-fx-background-color: -app-input; -fx-text-fill: -app-text;"
                + " -fx-prompt-text-fill: -app-text-muted; -fx-background-radius: 12;"
                + " -fx-border-color: -app-border-3; -fx-border-radius: 12;"
                + " -fx-padding: 8 12; -fx-font-size: 13px;");

        VBox list = new VBox(4.0);
        Label hint = new Label("↑↓ 选择 · Enter 执行 · Esc 关闭");
        hint.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 10.5px;");

        int[] selected = {0};
        ArrayList<HBox> rows = new ArrayList<>();
        Runnable paintSelection = () -> {
            for (int i = 0; i < rows.size(); i++) {
                boolean active = i == selected[0];
                rows.get(i).setStyle("-fx-background-color: "
                        + (active ? "-app-hover-2" : "transparent")
                        + "; -fx-background-radius: 9;");
            }
        };
        Runnable runSelected = () -> {
            if (rows.isEmpty()) {
                return;
            }
            int index = Math.max(0, Math.min(selected[0], rows.size() - 1));
            Action action = (Action) rows.get(index).getUserData();
            close.run();
            if (action != null && action.run() != null) {
                action.run().run();
            }
        };
        Runnable refresh = () -> {
            String query = search.getText() == null ? "" : search.getText().trim().toLowerCase();
            list.getChildren().clear();
            rows.clear();
            for (Action action : actions) {
                String haystack = (action.title() + " " + action.hint()).toLowerCase();
                if (!query.isEmpty() && !haystack.contains(query)) {
                    continue;
                }
                HBox row = new HBox(10.0);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(8.0, 10.0, 8.0, 10.0));
                row.setUserData(action);
                row.setCursor(Cursor.HAND);
                StackPane icon = Icons.node(action.icon(), 15.0, "#c7ccd4");
                Label title = new Label(action.title());
                title.setStyle("-fx-text-fill: -app-text-2; -fx-font-size: 12.5px;"
                        + " -fx-font-weight: bold;");
                Label sub = new Label(action.hint());
                sub.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 10.5px;");
                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);
                row.getChildren().addAll(icon, title, sub, spacer);
                row.setOnMouseEntered(e -> {
                    selected[0] = rows.indexOf(row);
                    paintSelection.run();
                });
                row.setOnMouseClicked(e -> {
                    close.run();
                    action.run().run();
                });
                rows.add(row);
                list.getChildren().add(row);
                if (rows.size() >= MAX_ROWS) {
                    break;
                }
            }
            selected[0] = 0;
            paintSelection.run();
        };

        search.textProperty().addListener((o, a, b) -> refresh.run());
        search.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DOWN) {
                if (!rows.isEmpty()) {
                    selected[0] = (selected[0] + 1) % rows.size();
                }
                paintSelection.run();
                e.consume();
            } else if (e.getCode() == KeyCode.UP) {
                if (!rows.isEmpty()) {
                    selected[0] = (selected[0] - 1 + rows.size()) % rows.size();
                }
                paintSelection.run();
                e.consume();
            } else if (e.getCode() == KeyCode.ENTER) {
                runSelected.run();
                e.consume();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                close.run();
                e.consume();
            }
        });

        card.getChildren().addAll(search, list, hint);
        overlay.getChildren().add(card);
        card.setOnMouseClicked(e -> e.consume());
        overlay.setOnMouseClicked(e -> {
            if (e.getTarget() == overlay) {
                close.run();
            }
        });

        refresh.run();
        overlay.setOpacity(0.0);
        card.setScaleX(0.97);
        card.setScaleY(0.97);
        FadeTransition fade = new FadeTransition(Duration.millis(160.0), overlay);
        fade.setToValue(1.0);
        ScaleTransition scale = new ScaleTransition(Duration.millis(180.0), card);
        scale.setToX(1.0);
        scale.setToY(1.0);
        scale.setInterpolator(Motion.SPRING);
        fade.play();
        scale.play();
        Platform.runLater(search::requestFocus);
        return overlay;
    }

    /** 从 Parent 里摘掉遮罩（供关闭动画使用）。 */
    public static void detach(Parent overlayParent, StackPane overlay) {
        if (overlayParent instanceof StackPane pane) {
            pane.getChildren().remove(overlay);
        }
    }
}
