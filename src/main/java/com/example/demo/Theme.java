package com.example.demo;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 主题：强调色 / 浅色模式 / 字体缩放。
 * 界面大量使用内联样式，所以通过遍历节点修改 style 字符串来实现。
 */
public final class Theme {

    public record Palette(String name, String accent, String dark, String light, String soft) {
    }

    public static final Palette[] PRESETS = {
            new Palette("共产红", "#e11d2e", "#a30f1b", "#ff6b6b", "#ffd1d1"),
            new Palette("为爱发电", "#ff4d3a", "#b3261e", "#ffb26b", "#ffe1a8"),
            new Palette("青绿", "#9ca3af", "#6b7280", "#d1d5db", "#e5e7eb"),
            new Palette("天蓝", "#38bdf8", "#0284c7", "#7dd3fc", "#bae6fd"),
            new Palette("紫罗兰", "#8b5cf6", "#7c3aed", "#a78bfa", "#ddd6fe"),
            new Palette("琥珀", "#f59e0b", "#d97706", "#fbbf24", "#fde68a"),
            new Palette("玫红", "#f43f5e", "#e11d48", "#fb7185", "#fecdd3"),
            new Palette("中性灰", "#94a3b8", "#64748b", "#cbd5e1", "#e2e8f0")
    };

    private static final Pattern HEX = Pattern.compile("#([0-9a-fA-F]{6}|[0-9a-fA-F]{3})\\b");
    private static final Pattern RGBA = Pattern.compile("rgba\\((\\d+),\\s*(\\d+),\\s*(\\d+),\\s*([0-9.]+)\\)");
    private static final Pattern FONT_SIZE = Pattern.compile("-fx-font-size:\\s*([0-9.]+)px");

    private static Palette current = PRESETS[0];
    private static boolean light = false;
    private static javafx.scene.Scene boundScene;

    public static void bindScene(javafx.scene.Scene scene) {
        boundScene = scene;
        syncLightStylesheet();
        syncAccentVariable();
    }

    private static void syncLightStylesheet() {
        if (boundScene == null) return;
        var url = Theme.class.getResource("/com/example/demo/app-light.css");
        if (url == null) return;
        String path = url.toExternalForm();
        // 必须放在 app.css 之后，否则变量会被基础样式覆盖
        boundScene.getStylesheets().remove(path);
        if (light) {
            boundScene.getStylesheets().add(path);
        }
    }

    private Theme() {
    }

    public static Palette current() {
        return current;
    }

    public static Palette byName(String name) {
        if (name != null) {
            for (Palette palette : PRESETS) {
                if (palette.name().equals(name)) return palette;
            }
        }
        return PRESETS[0];
    }

    public static void setCurrent(Palette palette) {
        current = palette == null ? PRESETS[0] : palette;
        syncAccentVariable();
    }

    /** 让 CSS 里的 -app-accent（滑块、下拉选中、进度条等）也跟着主题色变。 */
    private static void syncAccentVariable() {
        if (boundScene == null || !(boundScene.getRoot() instanceof javafx.scene.layout.Region root)) return;
        String style = root.getStyle() == null ? "" : root.getStyle();
        style = style.replaceAll("-app-accent:\\s*#[0-9a-fA-F]+;?", "");
        root.setStyle(style + " -app-accent: " + (light ? current.dark() : current.accent()) + ";");
    }

    public static void setLight(boolean value) {
        light = value;
        syncLightStylesheet();
    }

    public static boolean isLight() {
        return light;
    }

    public static void apply(Node root) {
        if (root == null) return;
        recolor(root);
        if (root instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                apply(child);
            }
        }
    }

    /** 缩放整棵界面树的内联字号。 */
    public static void scaleFonts(Node root, double scale) {
        if (root == null || scale <= 0 || Math.abs(scale - 1.0) < 0.001) return;
        applyFontScale(root, scale);
    }

    private static void applyFontScale(Node node, double scale) {
        if (Boolean.TRUE.equals(node.getProperties().get("font-scaled"))) return;
        String style = node.getStyle();
        if (style != null && style.contains("-fx-font-size")) {
            Matcher matcher = FONT_SIZE.matcher(style);
            StringBuilder out = new StringBuilder();
            while (matcher.find()) {
                double value = Double.parseDouble(matcher.group(1)) * scale;
                matcher.appendReplacement(out, "-fx-font-size: "
                        + String.format(Locale.ROOT, "%.1f", value) + "px");
            }
            matcher.appendTail(out);
            String next = out.toString();
            if (!next.equals(style)) node.setStyle(next);
        }
        node.getProperties().put("font-scaled", Boolean.TRUE);
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                applyFontScale(child, scale);
            }
        }
    }

    private static void recolor(Node node) {
        // 主题色选择器的色块本身不能被换色
        if ("theme-swatch".equals(node.getUserData())
                || "keep-dark".equals(node.getUserData())) return;

        String style = node.getStyle();
        if (style != null && !style.isBlank()) {
            // 浅色模式由 CSS 变量（-app-*）负责，这里只同步强调色
            String next = applyAccent(style);
            if (!next.equals(style)) node.setStyle(next);
        }

        if (node instanceof Rectangle rectangle && rectangle.getFill() instanceof Color color) {
            String hex = toHex(color);
            if (isAccent(hex)) {
                rectangle.setFill(Color.web(current.accent()));
            } else if (light && luminance(color) < 0.35 && color.getOpacity() > 0.3) {
                rectangle.setFill(Color.web("#c9d2de"));
            }
        }
    }

    private static String applyAccent(String css) {
        String next = css;
        for (Palette palette : PRESETS) {
            next = next.replace(palette.accent(), current.accent())
                    .replace(palette.dark(), current.dark())
                    .replace(palette.light(), current.light())
                    .replace(palette.soft(), current.soft());
        }
        return next;
    }

    /** 把深色界面样式转成浅色（按亮度反转，保留强调色）。 */
    private static String toLight(String css) {
        Matcher hexMatcher = HEX.matcher(css);
        StringBuilder out = new StringBuilder();
        while (hexMatcher.find()) {
            String raw = hexMatcher.group(1);
            String hex = raw.length() == 3
                    ? "#" + raw.charAt(0) + raw.charAt(0) + raw.charAt(1) + raw.charAt(1)
                    + raw.charAt(2) + raw.charAt(2)
                    : "#" + raw;
            String replacement = isAccent(hex) ? hex : lightColorFor(hex);
            hexMatcher.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        hexMatcher.appendTail(out);

        Matcher rgbaMatcher = RGBA.matcher(out.toString());
        StringBuilder out2 = new StringBuilder();
        while (rgbaMatcher.find()) {
            int r = Integer.parseInt(rgbaMatcher.group(1));
            int g = Integer.parseInt(rgbaMatcher.group(2));
            int b = Integer.parseInt(rgbaMatcher.group(3));
            double alpha = Double.parseDouble(rgbaMatcher.group(4));
            Color color = Color.rgb(r, g, b);
            String replacement;
            double l = luminance(color);
            if (l > 0.75) {
                replacement = "rgba(15,23,42," + String.format(Locale.ROOT, "%.2f",
                        Math.min(0.22, Math.max(0.05, alpha * 1.6))) + ")";
            } else if (l < 0.30) {
                replacement = "rgba(255,255,255," + String.format(Locale.ROOT, "%.2f",
                        Math.min(0.85, Math.max(0.25, alpha * 1.8))) + ")";
            } else {
                replacement = rgbaMatcher.group(0);
            }
            rgbaMatcher.appendReplacement(out2, Matcher.quoteReplacement(replacement));
        }
        rgbaMatcher.appendTail(out2);
        return out2.toString();
    }

    private static String lightColorFor(String hex) {
        Color color = Color.web(hex);
        double l = luminance(color);
        if (l > 0.62) {
            if (l > 0.86) return "#0f172a";
            if (l > 0.72) return "#1f2937";
            return "#374151";
        }
        if (l < 0.06) return "#ffffff";
        if (l < 0.12) return "#f6f8fb";
        if (l < 0.20) return "#eef2f7";
        if (l < 0.32) return "#e3e9f1";
        return hex;
    }

    private static boolean isAccent(String hex) {
        for (Palette palette : PRESETS) {
            if (palette.accent().equalsIgnoreCase(hex)
                    || palette.dark().equalsIgnoreCase(hex)
                    || palette.light().equalsIgnoreCase(hex)
                    || palette.soft().equalsIgnoreCase(hex)) return true;
        }
        return false;
    }

    private static double luminance(Color color) {
        return 0.2126 * color.getRed() + 0.7152 * color.getGreen() + 0.0722 * color.getBlue();
    }

    private static String toHex(Color color) {
        return String.format("#%02x%02x%02x",
                Math.round(color.getRed() * 255),
                Math.round(color.getGreen() * 255),
                Math.round(color.getBlue() * 255));
    }
}
