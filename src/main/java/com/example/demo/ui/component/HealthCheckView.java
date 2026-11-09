package com.example.demo.ui.component;

import com.example.demo.core.launch.HealthAdvice;
import com.example.demo.ui.shell.Icons;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * 一键体检的集成式面板：顶部一条总览，下面逐项符号 + 结论。
 *
 * <p>不再用系统样式的 {@code Dialog}，而是直接作为 {@code InAppDialog} 的内容，
 * 和启动器其余界面共用同一套配色、圆角和字体。
 */
public final class HealthCheckView extends VBox {

    /**
     * @param instanceLabel 实例名（为空时显示"未选择实例"）
     * @param lines 体检结论
     * @param onAction 点击告警行时的跳转动作（null 表示不可点）
     */
    public HealthCheckView(
            String instanceLabel, List<HealthAdvice.Line> lines,
            java.util.function.Consumer<HealthAdvice.Line> onAction) {
        super(0);
        setPrefWidth(510);
        setMaxWidth(510);
        setSpacing(10);

        long warnings = lines.stream().filter(line -> !line.ok()).count();
        String summary =
                warnings == 0
                        ? "全部 " + lines.size() + " 项正常"
                        : "共 " + lines.size() + " 项，" + warnings + " 项需要注意";

        HBox header = new HBox(10.0);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10, 12, 10, 12));
        header.setStyle(
                "-fx-background-color: rgba(255,255,255,0.045); -fx-background-radius: 12;"
                        + " -fx-border-color: rgba(255,255,255,0.06); -fx-border-radius: 12;");
        Node badge = Icons.node(warnings == 0 ? "check" : "warning", 18.0,
                warnings == 0 ? "#4ade80" : "#fbbf24");
        Label title = new Label(summary);
        title.setStyle(
                "-fx-text-fill: -app-text; -fx-font-size: 13.5px; -fx-font-weight: bold;");
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        Label who = new Label(instanceLabel == null || instanceLabel.isBlank()
                ? "未选择实例"
                : "实例 " + instanceLabel);
        who.setStyle("-fx-text-fill: -app-text-dim-3; -fx-font-size: 11px;");
        header.getChildren().addAll(badge, title, gap, who);

        getChildren().add(header);
        for (HealthAdvice.Line line : lines) {
            getChildren().add(row(line, onAction));
        }
        Label keys = new Label("Esc 关闭  ·  Tab 切换按钮  ·  Enter 确认");
        keys.setStyle("-fx-text-fill: -app-text-dim-3; -fx-font-size: 10.5px;");
        HBox footer = new HBox(keys);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(2, 4, 0, 4));
        getChildren().add(footer);
    }

    /** 单行：状态符号 + 级别 + 说明；告警行用暖色描边且可点击跳转。 */
    private static Node row(
            HealthAdvice.Line line,
            java.util.function.Consumer<HealthAdvice.Line> onAction) {
        HBox row = new HBox(10.0);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 12, 8, 12));
        row.setStyle(
                "-fx-background-color: "
                        + (line.ok() ? "rgba(255,255,255,0.02)" : "rgba(251,191,36,0.08)")
                        + "; -fx-background-radius: 10; -fx-border-radius: 10; -fx-border-color: "
                        + (line.ok() ? "rgba(255,255,255,0.04)" : "rgba(251,191,36,0.28)")
                        + ";");
        Node dot = Icons.node(line.ok() ? "check" : "warning", 13.0,
                line.ok() ? "#4ade80" : "#fbbf24");
        Label level = new Label(line.level());
        level.setMinWidth(64);
        level.setStyle(
                "-fx-text-fill: " + (line.ok() ? "-app-text-dim" : "#fbbf24")
                        + "; -fx-font-size: 11.5px; -fx-font-weight: bold;");
        Label text = new Label(line.message());
        text.setWrapText(true);
        text.setStyle("-fx-text-fill: -app-text-2; -fx-font-size: 12px;");
        HBox.setHgrow(text, Priority.ALWAYS);
        row.getChildren().addAll(dot, level, text);
        if (!line.ok() && onAction != null) {
            // 告警行可点：崩溃记录 → 打开目录、模组不匹配 → 打开 mods…由调用方决定
            row.setCursor(javafx.scene.Cursor.HAND);
            row.getStyleClass().add("health-row-actionable");
            row.setOnMouseClicked(
                    e -> {
                        onAction.accept(line);
                    });
            Label go = new Label("›");
            go.setStyle("-fx-text-fill: #fbbf24; -fx-font-size: 14px;");
            row.getChildren().add(go);
        }
        return row;
    }
}
