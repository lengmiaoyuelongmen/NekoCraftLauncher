package com.example.demo.ui.page;

import com.example.demo.ui.shell.ModuleHeader;
import com.example.demo.ui.shell.Motion;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;

import java.util.List;

/**
 * 启动页的骨架：左边账户卡（可滚动，按黄金比例 38.2% 分栏）+ 右边概览/控制台 + 顶部模块标题。
 *
 * <p>只做布局，不认识登录、启动、下载这些业务；两列内容由调用方构建好传进来。
 */
public final class LaunchPage {

    private LaunchPage() {
    }

    public static VBox build(Node userArea, Node rightColumn) {
        VBox panel = new VBox(21.0);
        panel.setPadding(new Insets(21.0));

        ScrollPane userScroll = new ScrollPane(userArea);
        userScroll.setFitToWidth(true);
        userScroll.setFitToHeight(true);
        userScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        userScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        userScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;"
                + " -fx-border-color: transparent;");

        // 黄金比例分栏：账户卡占可用宽度的 38.2%（1/φ²），再夹在 300~420 之间
        DoubleBinding leftColumnWidth = Bindings.createDoubleBinding(
                () -> {
                    double available = panel.getWidth() - 42.0;
                    return Math.min(420.0, Math.max(300.0, available * 0.382));
                },
                panel.widthProperty());
        userScroll.prefWidthProperty().bind(leftColumnWidth);
        userScroll.minWidthProperty().bind(leftColumnWidth);
        userScroll.maxWidthProperty().bind(leftColumnWidth);

        HBox mainContent = new HBox(21.0, userScroll, rightColumn);
        VBox.setVgrow(mainContent, Priority.ALWAYS);
        mainContent.setMinWidth(0.0);

        ModuleHeader header = new ModuleHeader(
                "01 / NEKO",
                "启动",
                "猫娘助手 · 今天也要一起冒险",
                "sparkles",
                ModuleHeader.Badge.CIRCLE,
                "为爱发电 · 永远免费更新");
        panel.getChildren().addAll(header, mainContent);

        Motion.stagger(List.of(userArea, rightColumn));
        return panel;
    }
}
