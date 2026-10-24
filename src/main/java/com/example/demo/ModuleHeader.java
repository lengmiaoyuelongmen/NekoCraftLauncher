package com.example.demo;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;

/**
 * 每个模块的标识头：独立图形徽标 + 模块编号 + 标题 + 副标题 + 右侧半透明水印。
 * 用几何形状和文案区分模块，不依赖主题色。
 */
public final class ModuleHeader extends HBox {

    public enum Badge {CIRCLE, SQUARE, HEX, DIAMOND, STACK}

    private Label metaLabel;

    public ModuleHeader(String code, String title, String subtitle, String glyph, Badge badge) {
        this(code, title, subtitle, glyph, badge, "");
    }

    public ModuleHeader(String code, String title, String subtitle, String glyph, Badge badge, String meta) {
        super(9);
        setAlignment(Pos.CENTER_LEFT);
        setPadding(new javafx.geometry.Insets(0, 0, 0, 0));
        setMinHeight(32);
        setPrefHeight(32);

        StackPane badgePane = new StackPane(badgeShape(badge), Icons.node(glyph, 13, "#9ca3af"));
        badgePane.setMinSize(30, 30);
        badgePane.setPrefSize(30, 30);
        badgePane.setMaxSize(30, 30);

        Label codeLabel = new Label(code);
        codeLabel.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 8.5px; -fx-font-weight: bold;");
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill: -app-text; -fx-font-size: 16px; -fx-font-weight: bold;");
        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.setStyle("-fx-text-fill: -app-text-dim-2; -fx-font-size: 10.5px;");
        HBox.setMargin(codeLabel, new javafx.geometry.Insets(0, 0, 0, 2));
        HBox.setMargin(titleLabel, new javafx.geometry.Insets(0, 0, 0, 2));

        Region rule = new Region();
        rule.setMinHeight(1);
        rule.setPrefHeight(1);
        rule.setMaxHeight(1);
        rule.setStyle("-fx-background-color: linear-gradient(to right, transparent, -app-border-2 22%, -app-border-2 68%, transparent 96%);");
        HBox.setHgrow(rule, Priority.ALWAYS);
        HBox.setMargin(rule, new javafx.geometry.Insets(0, 10, 0, 14));

        metaLabel = new Label(meta);
        metaLabel.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 10px;");
        metaLabel.setVisible(meta != null && !meta.isBlank());
        metaLabel.setManaged(metaLabel.isVisible());

        StackPane watermark = Icons.node(glyph, 20, "#9ca3af");
        watermark.setOpacity(0.05);
        watermark.setMouseTransparent(true);
        HBox.setMargin(watermark, new javafx.geometry.Insets(0, 0, 0, 10));

        getChildren().addAll(badgePane, codeLabel, titleLabel, subtitleLabel, rule, metaLabel, watermark);
    }

    public void setMeta(String meta) {
        if (metaLabel == null) return;
        metaLabel.setText(meta == null ? "" : meta);
        metaLabel.setVisible(meta != null && !meta.isBlank());
        metaLabel.setManaged(metaLabel.isVisible());
    }

    private Label glyph(String text, double size) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: -app-text; -fx-font-size: " + size + "px; -fx-font-weight: bold;");
        return label;
    }

    private Shape badgeShape(Badge badge) {
        Shape shape;
        switch (badge) {
            case CIRCLE -> shape = new Circle(16);
            case HEX -> {
                Polygon hex = new Polygon();
                for (int i = 0; i < 6; i++) {
                    double angle = Math.toRadians(60 * i - 30);
                    hex.getPoints().addAll(Math.cos(angle) * 18, Math.sin(angle) * 18);
                }
                shape = hex;
            }
            case DIAMOND -> {
                Rectangle diamond = new Rectangle(23, 23);
                diamond.setArcWidth(6);
                diamond.setArcHeight(6);
                diamond.setRotate(45);
                shape = diamond;
            }
            case STACK -> {
                javafx.scene.shape.SVGPath stacked = new javafx.scene.shape.SVGPath();
                stacked.setContent("M2 8 L20 8 L20 26 L2 26 Z M10 2 L30 2 L30 22 L10 22 Z");
                shape = stacked;
            }
            default -> {
                Rectangle square = new Rectangle(30, 30);
                square.setArcWidth(10);
                square.setArcHeight(10);
                shape = square;
            }
        }
        shape.setFill(javafx.scene.paint.Color.TRANSPARENT);
        shape.setStyle("-fx-stroke: -app-text-muted;");
        shape.setStrokeWidth(1.2);
        return shape;
    }
}
