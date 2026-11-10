package com.example.demo.ui.component;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * 启动页右侧控制台容器：AI 聊天区 / 启动概览 / 日志 + 命令栏 / 工具条 / 下载条，
 * 并负责「展开日志」与「AI 模式」两种状态下谁显示、谁占满高度。
 *
 * <p>从主类的 refreshLaunchBody 搬出来；主类只传状态，不再自己算高度规则。
 */
public final class ConsoleContainer {

    private final Node aiChatArea;
    private final Node overview;
    private final VBox consoleBox;
    private final Node logPane;
    private final ConsoleToolbar toolbar;
    private final HBox actions;
    private final Node downloadBar;
    private final VBox root;

    public ConsoleContainer(Node aiChatArea, Node overview, VBox consoleBox, Node logPane,
                            ConsoleToolbar toolbar, Node downloadBar) {
        this.aiChatArea = aiChatArea;
        this.overview = overview;
        this.consoleBox = consoleBox;
        this.logPane = logPane;
        this.toolbar = toolbar;
        this.actions = toolbar.node();
        this.downloadBar = downloadBar;

        this.root = new VBox(4.0);
        this.root.getChildren().addAll(aiChatArea, overview, consoleBox,
                toolbar.node(), downloadBar);
    }

    /** 整列容器（放进启动页右栏）。 */
    public VBox node() {
        return root;
    }

    /**
     * 按「日志展开状态 + 是否 AI 模式」重排高度与可见性。
     *
     * @param expanded  用户是否展开了日志
     * @param aiEnabled 是否处于 AI 模式（此时只显示聊天区）
     */
    public void refresh(boolean expanded, boolean aiEnabled) {
        boolean showLog = !aiEnabled && expanded;
        boolean showOverview = !aiEnabled && !expanded;

        setFlexible(aiChatArea, aiEnabled);
        setFlexible(overview, showOverview);

        logPane.setVisible(showLog);
        logPane.setManaged(showLog);
        if (logPane instanceof Region region) {
            region.setMinHeight(showLog ? 180.0 : 0.0);
            region.setPrefHeight(showLog ? -1.0 : 0.0);
            region.setMaxHeight(showLog ? Double.MAX_VALUE : 0.0);
        }
        VBox.setVgrow(logPane, showLog ? Priority.ALWAYS : Priority.NEVER);

        // 只有日志展开时才让 consoleBox 撑高；AI 模式下撑高会把命令栏顶到一片空白里
        consoleBox.setMaxHeight(showLog ? Double.MAX_VALUE : -1.0);
        VBox.setVgrow(consoleBox, showLog ? Priority.ALWAYS : Priority.NEVER);

        Button toggle = toolbar.toggleButton();
        toggle.setText(expanded ? "收起日志" : "展开日志");

        HBox actions = this.actions;
        actions.setVisible(!aiEnabled);
        actions.setManaged(!aiEnabled);
        if (!aiEnabled) {
            // AI 模式下整排按钮没意义；其余按钮只在日志展开时显示，收起日志与标语始终在
            for (Node node : actions.getChildren()) {
                if (node == toggle
                        || node == actions.getChildren().get(actions.getChildren().size() - 1)
                        || node instanceof Region && !(node instanceof Button)) {
                    continue;
                }
                node.setVisible(expanded);
                node.setManaged(expanded);
            }
        }
        consoleBox.requestLayout();
    }

    private static void setFlexible(Node node, boolean on) {
        if (node == null) {
            return;
        }
        node.setVisible(on);
        node.setManaged(on);
        VBox.setVgrow(node, on ? Priority.ALWAYS : Priority.NEVER);
        if (node instanceof Region region) {
            region.setMinHeight(0.0);
            region.setPrefHeight(on ? -1.0 : 0.0);
            region.setMaxHeight(on ? Double.MAX_VALUE : 0.0);
        }
    }
}
