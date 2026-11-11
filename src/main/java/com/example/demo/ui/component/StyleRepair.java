package com.example.demo.ui.component;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Region;

/**
 * 圆角描边修复：JavaFX 的 {@code -fx-border-color} 默认是直角，一旦节点写了
 * {@code -fx-background-radius} 却没写 {@code -fx-border-radius}，就会看到
 * “圆角底 + 方角边框”的毛刺。
 *
 * <p>这里遍历场景树，把缺少 {@code -fx-border-radius} 的节点补成和背景一致的半径。
 * 只在样式里已经有 {@code -fx-border-color} 时动手，其它节点不碰。
 */
public final class StyleRepair {

    private StyleRepair() {
    }

    /**
     * 修复 root 及其所有子节点。
     *
     * @return 本次修好的节点数量
     */
    public static int fixRoundedBorders(Parent root) {
        if (root == null) {
            return 0;
        }
        // 只修当前可见的节点：隐藏页面的节点不需要描边，也避免一次性"修 260 个"的假象
        if (!root.isVisible()) {
            return 0;
        }
        int fixed = fixOne(root) ? 1 : 0;
        for (Node child : root.getChildrenUnmodifiable()) {
            if (!child.isVisible()) {
                continue;
            }
            if (child instanceof Parent parent) {
                fixed += fixRoundedBorders(parent);
            } else {
                fixed += fixOne(child) ? 1 : 0;
            }
        }
        return fixed;
    }

    /** 只判断、不修改：用于自检。只有"圆角底 + 方角边"才算缺陷，直线分隔线不算。 */
    public static boolean needsFix(Node node) {
        if (!(node instanceof Region region)) {
            return false;
        }
        String style = region.getStyle();
        if (style == null
                || style.isEmpty()
                || !style.contains("-fx-border-color")
                || style.contains("-fx-border-radius")) {
            return false;
        }
        String radii = radiiFromStyle(style);
        if (radii == null) {
            radii = radiiFromBackground(region);
        }
        return radii != null;
    }

    private static boolean fixOne(Node node) {
        if (!(node instanceof Region region)) {
            return false;
        }
        String style = region.getStyle();
        if (style == null || style.isEmpty()) {
            return false;
        }
        if (!style.contains("-fx-border-color") || style.contains("-fx-border-radius")) {
            return false;
        }
        String radii = radiiFromStyle(style);
        if (radii == null) {
            radii = radiiFromBackground(region);
        }
        if (radii == null) {
            return false;
        }
        String suffix = style.endsWith(";") ? " " : "; ";
        region.setStyle(style + suffix + "-fx-border-radius: " + radii + ";");
        return true;
    }

    /** 从内联样式的 {@code -fx-background-radius} 里取半径。 */
    private static String radiiFromStyle(String style) {
        int at = style.indexOf("-fx-background-radius:");
        if (at < 0) {
            return null;
        }
        int start = at + "-fx-background-radius:".length();
        int end = style.indexOf(';', start);
        if (end < 0) {
            end = style.length();
        }
        String value = style.substring(start, end).trim();
        return value.isEmpty() ? null : value;
    }

    /** 内联样式里没有半径时，从实际背景填充里读（CSS 主题设置的也能拿到）。 */
    private static String radiiFromBackground(Region region) {
        Background background = region.getBackground();
        if (background == null || background.getFills().isEmpty()) {
            return null;
        }
        BackgroundFill fill = background.getFills().get(0);
        CornerRadii radii = fill.getRadii();
        double topLeft = radii.getTopLeftHorizontalRadius();
        double topRight = radii.getTopRightHorizontalRadius();
        double bottomRight = radii.getBottomRightHorizontalRadius();
        double bottomLeft = radii.getBottomLeftHorizontalRadius();
        if (topLeft <= 0 && topRight <= 0 && bottomRight <= 0 && bottomLeft <= 0) {
            return null;
        }
        if (topLeft == topRight && topRight == bottomRight && bottomRight == bottomLeft) {
            return format(topLeft);
        }
        return format(topLeft) + " " + format(topRight) + " " + format(bottomRight) + " " + format(bottomLeft);
    }

    private static String format(double value) {
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }
    /** 统计还有多少节点写了 border-color 却没有 border-radius（0 才算干净）。 */
    public static int countMissing(Parent root) {
        if (root == null) {
            return 0;
        }
        // 与 fixRoundedBorders 保持一致：隐藏节点既不计入也不修
        if (!root.isVisible()) {
            return 0;
        }
        int missing = needsFix(root) ? 1 : 0;
        for (Node child : root.getChildrenUnmodifiable()) {
            if (!child.isVisible()) {
                continue;
            }
            if (child instanceof Parent parent) {
                missing += countMissing(parent);
            } else {
                missing += needsFix(child) ? 1 : 0;
            }
        }
        return missing;
    }}
