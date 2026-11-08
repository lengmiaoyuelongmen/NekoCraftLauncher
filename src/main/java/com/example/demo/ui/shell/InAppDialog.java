package com.example.demo.ui.shell;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * 集成式弹窗：不再开系统窗口，而是盖在主界面上的遮罩 + 卡片。
 *
 * <p>用法：主窗口场景根节点放一个空的 {@link StackPane} 作为宿主层，
 * 所有确认框 / 输入框 / 选择框都渲染到这一层里，风格跟着应用主题走。
 */
public final class InAppDialog {

    private static StackPane globalHost;

    private InAppDialog() {
    }

    /** 由主窗口启动时注册，其他面板不用自己持有宿主层引用。 */
    public static void setGlobalHost(StackPane host) {
        globalHost = host;
    }

    public static StackPane globalHost() {
        return globalHost;
    }

    /** 一个按钮：文字 + 样式类（btn-primary / btn-secondary / btn-danger / btn-cyan）+ 动作。 */
    public record Action(String text, String styleClass, Runnable run) {
        public static Action primary(String text, Runnable run) {
            return new Action(text, "btn-primary", run);
        }

        public static Action secondary(String text, Runnable run) {
            return new Action(text, "btn-secondary", run);
        }

        public static Action danger(String text, Runnable run) {
            return new Action(text, "btn-danger", run);
        }

        public static Action cyan(String text, Runnable run) {
            return new Action(text, "btn-cyan", run);
        }
    }

    /** 单按钮提示框（替代 Alert 的信息/错误弹窗）。 */
    public static void alert(String title, String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        Label text = new Label(message);
        text.setWrapText(true);
        text.setStyle("-fx-text-fill: -app-text-2; -fx-font-size: 12.5px;");
        show(null, title == null ? "提示" : title, text, List.of(Action.primary("知道了", null)), null);
    }

    /** 错误提示：标题统一成「出错了」，正文保留原始信息。 */
    public static void error(String message) {
        alert("出错了", message);
    }

    /** 普通提示。 */
    public static void info(String message) {
        alert("提示", message);
    }

    /** 简单确认框。 */
    public static void confirm(
            StackPane host, String title, String message, String okText, Runnable onOk) {
        Label text = new Label(message);
        text.setWrapText(true);
        text.setStyle("-fx-text-fill: -app-text-2; -fx-font-size: 12.5px;");
        show(
                host,
                title,
                text,
                List.of(
                        Action.secondary("取消", null),
                        Action.primary(okText == null ? "确定" : okText, onOk)),
                null);
    }

    /** 单行输入框。 */
    public static void input(
            StackPane host,
            String title,
            String subtitle,
            String initial,
            Consumer<String> onOk) {
        Label hint = new Label(subtitle == null ? "" : subtitle);
        hint.setWrapText(true);
        hint.setStyle("-fx-text-fill: -app-text-dim; -fx-font-size: 11.5px;");
        TextField field = new TextField(initial == null ? "" : initial);
        field.setStyle(
                "-fx-background-color: -app-row3; -fx-text-fill: -app-text;"
                        + " -fx-prompt-text-fill: -app-text-muted; -fx-background-radius: 9;"
                        + " -fx-border-color: -app-border-soft; -fx-border-radius: 9; -fx-padding: 8 10;");
        VBox content = new VBox(8, hint, field);
        javafx.application.Platform.runLater(
                () -> {
                    field.requestFocus();
                    field.selectAll();
                });
        show(
                host,
                title,
                content,
                List.of(
                        Action.secondary("取消", null),
                        Action.primary(
                                "确定",
                                () -> {
                                    if (onOk != null) {
                                        onOk.accept(field.getText());
                                    }
                                })),
                null);
    }

    /** 单选列表框：返回值即选中的字符串。 */
    public static void choose(
            StackPane host,
            String title,
            String header,
            List<String> options,
            Consumer<String> onPick) {
        if (options == null || options.isEmpty()) {
            return;
        }
        javafx.scene.control.ListView<String> list = new javafx.scene.control.ListView<>();
        list.getItems().addAll(options);
        list.getSelectionModel().selectFirst();
        list.setPrefHeight(Math.min(260, 34 + options.size() * 26));
        Label hint = new Label(header == null ? "" : header);
        hint.setWrapText(true);
        hint.setStyle("-fx-text-fill: -app-text-dim; -fx-font-size: 11.5px;");
        VBox content = new VBox(8, hint, list);
        show(
                host,
                title,
                content,
                List.of(
                        Action.secondary("取消", null),
                        Action.primary(
                                "确定",
                                () -> {
                                    String pick = list.getSelectionModel().getSelectedItem();
                                    if (pick != null && onPick != null) {
                                        onPick.accept(pick);
                                    }
                                })),
                null);
    }

    /** 完全自定义内容，并指定卡片最大尺寸（文件管理器这类大面板用）。 */
    public static void customSized(
            StackPane host,
            String title,
            Node content,
            List<Action> actions,
            Runnable onCancel,
            double maxWidth,
            double maxHeight) {
        show(host, title, content, actions, onCancel, maxWidth, maxHeight);
    }

    /** 完全自定义内容。 */
    public static void custom(
            StackPane host, String title, Node content, List<Action> actions, Runnable onCancel) {
        show(host, title, content, actions, onCancel);
    }

    private static void show(
            StackPane host, String title, Node content, List<Action> actions, Runnable onCancel) {
        show(host, title, content, actions, onCancel, 520, -1);
    }

    private static void show(
            StackPane host,
            String title,
            Node content,
            List<Action> actions,
            Runnable onCancel,
            double maxWidth,
            double maxHeight) {
        StackPane layer = host != null ? host : globalHost;
        if (layer == null) {
            return;
        }
        StackPane backdrop = new StackPane();
        backdrop.setStyle("-fx-background-color: rgba(6,10,14,0.42);");
        backdrop.setPickOnBounds(true);

        Label titleLabel = new Label(title == null ? "" : title);
        titleLabel.setStyle("-fx-text-fill: -app-text; -fx-font-size: 15px; -fx-font-weight: bold;");

        VBox card = new VBox(14, titleLabel, content);
        card.setPadding(new Insets(20));
        card.setMaxWidth(maxWidth);
        card.setMaxHeight(maxHeight > 0 ? maxHeight : Region.USE_PREF_SIZE);
        if (maxHeight > 0) {
            card.setPrefHeight(maxHeight);
        }
        card.setStyle(
                "-fx-background-color: -app-card; -fx-background-radius: 16;"
                        + " -fx-border-color: -app-border-soft; -fx-border-radius: 16;"
                        + " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.30), 26, 0.14, 0, 10);");
        VBox.setVgrow(content, Priority.ALWAYS);

        HBox buttons = new HBox(8);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        List<Button> created = new ArrayList<>();
        if (actions != null) {
            for (Action action : actions) {
                Button button = new Button(action.text());
                button.getStyleClass().add(action.styleClass());
                button.getStyleClass().add("btn-sm");
                button.setOnAction(
                        e -> {
                            close(layer, backdrop);
                            if (action.run() != null) {
                                action.run().run();
                            }
                        });
                created.add(button);
                buttons.getChildren().add(button);
            }
        }
        card.getChildren().add(buttons);

        StackPane.setAlignment(card, Pos.CENTER);
        backdrop.getChildren().add(card);
        backdrop.setOnMouseClicked(
                e -> {
                    if (e.getTarget() == backdrop) {
                        close(layer, backdrop);
                        if (onCancel != null) {
                            onCancel.run();
                        }
                    }
                });
        backdrop.setOnKeyPressed(
                e -> {
                    if (e.getCode() == KeyCode.ESCAPE) {
                        e.consume();
                        close(layer, backdrop);
                        if (onCancel != null) {
                            onCancel.run();
                        }
                    }
                });
        backdrop.setFocusTraversable(true);

        layer.getChildren().add(backdrop);
        // 弹窗里的按钮也是"圆角底 + 方角描边"的高发区，创建后补一次
        com.example.demo.ui.component.StyleRepair.fixRoundedBorders(backdrop);
        // 入场动画：淡入 + 卡片轻微放大，和应用的 Motion 风格一致
        backdrop.setOpacity(0);
        card.setScaleX(0.97);
        card.setScaleY(0.97);
        ParallelTransition in =
                new ParallelTransition(
                        new FadeTransition(Duration.millis(140), backdrop),
                        new ScaleTransition(Duration.millis(160), card));
        ((FadeTransition) in.getChildren().get(0)).setToValue(1);
        ((ScaleTransition) in.getChildren().get(1)).setToX(1);
        ((ScaleTransition) in.getChildren().get(1)).setToY(1);
        in.play();
        javafx.application.Platform.runLater(backdrop::requestFocus);
        if (!created.isEmpty()) {
            created.get(created.size() - 1).setDefaultButton(true);
        }
    }

    private static void close(StackPane host, StackPane backdrop) {
        FadeTransition out = new FadeTransition(Duration.millis(110), backdrop);
        out.setToValue(0);
        out.setOnFinished(e -> host.getChildren().remove(backdrop));
        out.play();
    }

    /** 宿主层里当前是否有弹窗。 */
    public static boolean isOpen(StackPane host) {
        if (host == null || host.getChildren().isEmpty()) {
            return false;
        }
        for (Node node : host.getChildren()) {
            if (node instanceof StackPane) {
                return true;
            }
        }
        return false;
    }

    /** 取第一个按钮（供默认按钮用），避免调用方重复写样板代码。 */
    public static Optional<Button> firstButton(Node card) {
        if (card instanceof VBox box) {
            for (Node child : box.getChildren()) {
                if (child instanceof HBox row) {
                    for (Node node : row.getChildren()) {
                        if (node instanceof Button button) {
                            return Optional.of(button);
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }
}
