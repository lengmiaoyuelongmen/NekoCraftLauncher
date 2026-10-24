package com.example.demo;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;

/** 统一矢量图标：用 24×24 SVG 路径绘制，不再用字符 / emoji 当图标。 */
public final class Icons {

    private Icons() {
    }

    public static StackPane node(String name, double size, String color) {
        StackPane wrap = new StackPane();
        wrap.setPrefSize(size, size);
        wrap.setMinSize(size, size);
        wrap.setMaxSize(size, size);
        wrap.setMouseTransparent(true);
        wrap.getChildren().add(graphic(name, size, color));
        return wrap;
    }

    private static Node graphic(String name, double size, String color) {
        if ("gear".equals(name)) return gear(size, color);

        boolean filled = "play".equals(name) || "sparkles".equals(name) || "star".equals(name)
                || "dot".equals(name);
        String content = switch (name) {
            case "play" -> "M8 5 L19 12 L8 19 Z";
            case "download" -> "M12 4 V15 M7.5 10.5 L12 15 L16.5 10.5 M5 20 H19";
            case "tools" -> "M14.5 6.5 A3.5 3.5 0 0 0 9.5 11.5 L4 17 L7 20 L12.5 14.5"
                    + " A3.5 3.5 0 0 0 17.5 9.5 L15 8 Z";
            case "sparkles" -> "M12 3 L13.6 9.4 L20 11 L13.6 12.6 L12 19 L10.4 12.6 L4 11 L10.4 9.4 Z";
            case "edit" -> "M4 20 L8.5 19 L20 7.5 L16.5 4 L5 15.5 Z";
            case "image" -> "M3 5 H21 V19 H3 Z M6 16 L10 11 L13 14 L16 10 L20 16";
            case "package" -> "M12 3 L21 8 V17 L12 22 L3 17 V8 Z M3 8 L12 13 L21 8 M12 13 V22";
            case "globe" -> "M12 3 A9 9 0 1 0 12 21 A9 9 0 1 0 12 3 M3 12 H21 M12 3 C7 8 7 16 12 21 M12 3 C17 8 17 16 12 21";
            case "server" -> "M4 5 H20 V9 H4 Z M4 15 H20 V19 H4 Z";
            case "layers" -> "M12 3 L21 8 L12 13 L3 8 Z M3 12 L12 17 L21 12 M3 16 L12 21 L21 16";
            case "terminal" -> "M3 5 H21 V19 H3 Z M7 9.5 L10 12 L7 14.5 M12 15 H17";
            case "refresh" -> "M20 12 A8 8 0 1 1 17.5 6.5 M20 4 V10 H14";
            case "star" -> "M12 3.5 L14.5 9 L20.5 9.7 L16 13.9 L17.3 20 L12 16.8"
                    + " L6.7 20 L8 13.9 L3.5 9.7 L9.5 9 Z";
            case "close" -> "M6 6 L18 18 M18 6 L6 18";
            case "chevron-down" -> "M6 9 L12 15 L18 9";
            case "chevron-right" -> "M9 6 L15 12 L9 18";
            case "chevron-up" -> "M6 15 L12 9 L18 15";
            case "back" -> "M15 5 L8 12 L15 19";
            case "search" -> "M10.5 4 A6.5 6.5 0 1 0 10.5 17 A6.5 6.5 0 1 0 10.5 4 M15.5 15.5 L21 21";
            case "folder" -> "M3 6 H10 L12 8 H21 V19 H3 Z";
            case "check" -> "M5 12 L10 17 L19 7";
            case "warning" -> "M12 3 L22 20 H2 Z M12 9 V14 M12 17 V17.5";
            case "user" -> "M12 4 A4 4 0 1 0 12 12 A4 4 0 1 0 12 4 M4 21 C4 16 7 14 12 14"
                    + " C17 14 20 16 20 21";
            case "dot" -> "M12 8 A4 4 0 1 0 12 16 A4 4 0 1 0 12 8";
            default -> "M12 4 V20 M4 12 H20";
        };

        SVGPath svg = new SVGPath();
        svg.setContent(content);
        svg.setFill(javafx.scene.paint.Color.TRANSPARENT);
        if (filled) {
            svg.setStyle("-fx-fill: " + color + "; -fx-stroke: transparent;");
        } else {
            svg.setStyle("-fx-fill: transparent; -fx-stroke: " + color + ";");
            svg.setStrokeWidth(1.7);
            svg.setStrokeLineCap(StrokeLineCap.ROUND);
        }
        double scale = size / 24.0;
        svg.setScaleX(scale);
        svg.setScaleY(scale);
        return svg;
    }

    private static Node gear(double size, String color) {
        Group group = new Group();
        double scale = size / 24.0;
        Circle inner = new Circle(12, 12, 3.2);
        Circle outer = new Circle(12, 12, 6.2);
        inner.setStyle("-fx-fill: transparent; -fx-stroke: " + color + ";");
        outer.setStyle("-fx-fill: transparent; -fx-stroke: " + color + ";");
        inner.setStrokeWidth(1.6);
        outer.setStrokeWidth(1.6);
        group.getChildren().addAll(outer, inner);
        for (int i = 0; i < 8; i++) {
            double angle = Math.toRadians(45 * i);
            Line tooth = new Line(
                    12 + Math.cos(angle) * 7.2, 12 + Math.sin(angle) * 7.2,
                    12 + Math.cos(angle) * 10, 12 + Math.sin(angle) * 10);
            tooth.setStyle("-fx-stroke: " + color + ";");
            tooth.setStrokeWidth(2.1);
            tooth.setStrokeLineCap(StrokeLineCap.ROUND);
            group.getChildren().add(tooth);
        }
        group.setScaleX(scale);
        group.setScaleY(scale);
        return group;
    }
}
