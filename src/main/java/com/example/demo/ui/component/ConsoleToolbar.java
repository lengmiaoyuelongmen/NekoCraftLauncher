package com.example.demo.ui.component;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/**
 * 启动页控制台下的工具条：展开/收起日志 + 清空 / 复制 / 日志目录 / 快捷键 + 右下角标语。
 *
 * <p>只负责布局与样式；点击后做什么由 {@link Host} 决定。
 */
public final class ConsoleToolbar {

    public interface Host {
        void toggleLog();

        void clearLog();

        void copyLog();

        void openLogDir();

        void showShortcuts();

        /** 打开体检页（工具条上常驻入口，AI/日志布局下都能点到）。 */
        void showHealthCheck();
    }

    private final Button toggle = new Button("展开日志");
    private final HBox node;

    public ConsoleToolbar(Host host) {
        toggle.setStyle("-fx-background-color: rgba(255,255,255,0.06); -fx-text-fill: -app-text-2;"
                + " -fx-background-radius: 6; -fx-padding: 3 10; -fx-font-size: 11px;"
                + " -fx-cursor: hand;");
        toggle.getStyleClass().add("log-toggle");
        toggle.setOnAction(e -> host.toggleLog());

        Button clear = action("清空日志", host::clearLog);
        Button copy = action("复制日志", host::copyLog);
        Button logDir = action("日志目录", host::openLogDir);
        Button shortcuts = action("快捷键", host::showShortcuts);
        Button health = action("一键体检", host::showHealthCheck);

        Label promise = new Label("NEKO · 为爱发电");
        promise.setStyle("-fx-text-fill: -app-accent; -fx-font-size: 10.5px;"
                + " -fx-font-weight: bold;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        node = new HBox(8.0, toggle, clear, copy, logDir, shortcuts, health, spacer, promise);
        node.setAlignment(Pos.CENTER_LEFT);
    }

    private static Button action(String text, Runnable onClick) {
        Button button = new Button(text);
        button.setStyle("-fx-background-color: rgba(255,255,255,0.05);"
                + " -fx-text-fill: -app-text-dim; -fx-background-radius: 6; -fx-padding: 3 10;"
                + " -fx-font-size: 11px; -fx-cursor: hand;");
        button.setOnAction(e -> onClick.run());
        return button;
    }

    /** 工具条本身（放进控制台容器）。 */
    public HBox node() {
        return node;
    }

    /** 展开/收起按钮，文案随状态变化。 */
    public Button toggleButton() {
        return toggle;
    }

    /** 快捷键说明文案（也给「快捷键」按钮用）。 */
    public static String shortcutHelp() {
        return "F1  启动页\n"
                + "F2  实例选择\n"
                + "F3  实例设置（概览）\n"
                + "F4  实例设置 · 模组\n"
                + "F5  下载\n"
                + "F6  设置\n"
                + "F7  工具\n"
                + "F8  设置 · 查看日志\n"
                + "F9  收起 / 展开日志\n"
                + "Esc 关闭弹窗";
    }
}
