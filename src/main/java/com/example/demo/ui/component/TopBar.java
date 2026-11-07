package com.example.demo.ui.component;

import com.example.demo.ui.shell.Icons;
import com.example.demo.ui.shell.Motion;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.util.Duration;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * 顶栏：品牌区（时钟 + 运行状态）、导航按钮与滑动指示器、命令面板入口、窗口按钮，
 * 以及无边框窗口的拖动 / 双击复位。
 *
 * <p>从主类搬出。状态文案与按钮显隐仍由主类通过 {@link #setGameRunning} /
 * {@link #setLaunching} 控制，导航高亮通过 {@link #highlight} 更新。
 */
public final class TopBar {

    public interface Host {
        void switchPage(String key);

        void killGame();

        void stopLaunch();

        void requestClose();

        void minimize();

        /** 拖动窗口到新的屏幕坐标（主类负责边界钳制）。 */
        void moveWindow(double x, double y);

        /** 双击顶栏：回到默认尺寸并居中。 */
        void resetWindow();

        void showPalette();
    }

    private final HBox root = new HBox(14.0);
    private final HBox navBox = new HBox(2.0);
    private final Map<String, Button> navButtons = new LinkedHashMap<>();
    private final Rectangle navIndicator = new Rectangle(0.0, 2.0);
    private final Circle statusDot = new Circle(4.0, Color.web("#6b7280"));
    private final Label statusLabel = new Label("就绪");
    private final Button stopGame = new Button("结束游戏");
    private final Button cancelLaunch = new Button("取消启动");
    private FadeTransition pulse;

    public TopBar(Host host) {
        build(host);
    }

    private void build(Host host) {
        root.setAlignment(Pos.CENTER_LEFT);
        root.getStyleClass().add("app-topbar");
        root.setPadding(new Insets(0.0, 6.0, 0.0, 18.0));
        root.setPrefHeight(58.0);
        root.setMinHeight(58.0);

        Rectangle mark = new Rectangle(3.0, 18.0);
        mark.setArcWidth(3.0);
        mark.setArcHeight(3.0);
        mark.setStyle("-fx-fill: #14b8a6;");

        Label appTitle = new Label("Neko Craft Launcher");
        appTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        appTitle.setTextFill(Color.web("#e8eaee"));
        Label badge = new Label("NEKO");
        badge.setStyle("-fx-font-size: 10.5px; -fx-font-weight: bold;");
        badge.setTextFill(Color.web("#5eead4"));

        Label liveClock = new Label();
        liveClock.setStyle("-fx-text-fill: #7b818b; -fx-font-size: 10.5px;"
                + " -fx-font-family: 'Cascadia Mono', 'Consolas', monospace;");
        DateTimeFormatter clockFormat = DateTimeFormatter.ofPattern("HH:mm:ss");
        liveClock.setText(LocalTime.now().format(clockFormat));
        Timeline clockTimer = new Timeline(new KeyFrame(Duration.seconds(1.0), e -> {
            if (liveClock.isVisible()) {
                liveClock.setText(LocalTime.now().format(clockFormat));
            }
        }, new KeyValue[0]));
        clockTimer.setCycleCount(-1);
        clockTimer.play();

        statusLabel.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 10px;");
        HBox statusChip = new HBox(6.0, statusDot, statusLabel);
        statusChip.setAlignment(Pos.CENTER_LEFT);
        statusChip.setPadding(new Insets(3.0, 9.0, 3.0, 9.0));
        statusChip.setStyle("-fx-background-color: rgba(255,255,255,0.045);"
                + " -fx-background-radius: 10; -fx-border-color: rgba(255,255,255,0.06);"
                + " -fx-border-radius: 10;");

        stopGame.setStyle("-fx-background-color: rgba(248,113,113,0.16); -fx-text-fill: #fecaca;"
                + " -fx-border-color: rgba(248,113,113,0.55); -fx-border-radius: 8;"
                + " -fx-background-radius: 8; -fx-font-size: 10.5px; -fx-padding: 3 10;"
                + " -fx-cursor: hand;");
        stopGame.setVisible(false);
        stopGame.setManaged(false);
        stopGame.setOnAction(e -> host.killGame());

        cancelLaunch.setStyle("-fx-background-color: transparent; -fx-text-fill: -app-text-dim;"
                + " -fx-border-color: rgba(148,163,184,0.45); -fx-border-radius: 8;"
                + " -fx-background-radius: 8; -fx-font-size: 10.5px; -fx-padding: 3 10;"
                + " -fx-cursor: hand;");
        cancelLaunch.setVisible(false);
        cancelLaunch.setManaged(false);
        cancelLaunch.setOnAction(e -> host.stopLaunch());

        HBox brand = new HBox(8.0, mark, appTitle, Icons.node("star", 13.0, "#ffcf6a"), badge,
                liveClock, statusChip, cancelLaunch, stopGame);
        brand.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        navBox.setAlignment(Pos.CENTER);
        navIndicator.setArcWidth(2.0);
        navIndicator.setArcHeight(2.0);
        navIndicator.setStyle("-fx-fill: linear-gradient(to right, #d1d5db, #9ca3af);");
        navIndicator.setMouseTransparent(true);
        navIndicator.setVisible(false);
        navIndicator.setManaged(false);
        StackPane navPane = new StackPane(navIndicator, navBox);
        navPane.setAlignment(Pos.CENTER);
        StackPane.setAlignment(navIndicator, Pos.BOTTOM_LEFT);
        navPane.setPickOnBounds(false);

        String[][] entries = {
                {"launch", "启动", "play"},
                {"download", "下载", "download"},
                {"advisor", "顾问", "message"},
                {"settings", "设置", "gear"},
                {"tools", "工具", "tools"}
        };
        for (String[] entry : entries) {
            Button button = topButton(entry[1], entry[2]);
            button.setUserData(entry[2]);
            button.setOnAction(e -> host.switchPage(entry[0]));
            navButtons.put(entry[0], button);
            Motion.press(button);
            navBox.getChildren().add(button);
        }

        Button minimize = windowButton(WindowIcon.MINIMIZE);
        minimize.setOnAction(e -> host.minimize());
        Button close = windowButton(WindowIcon.CLOSE);
        close.setOnAction(e -> host.requestClose());
        HBox windowControls = new HBox(0.0, minimize, close);
        windowControls.setAlignment(Pos.CENTER_RIGHT);
        windowControls.getStyleClass().add("window-controls");

        root.widthProperty().addListener((obs, old, width) -> {
            boolean compact = width.doubleValue() < 1080.0;
            badge.setVisible(!compact);
            badge.setManaged(!compact);
            liveClock.setVisible(!compact);
            liveClock.setManaged(!compact);
            statusChip.setVisible(!compact);
            statusChip.setManaged(!compact);
            brand.setSpacing(compact ? 5.0 : 8.0);
            navBox.setSpacing(compact ? 0.0 : 2.0);
        });

        double[] dragOffset = new double[2];
        root.setOnMousePressed(e -> {
            dragOffset[0] = e.getSceneX();
            dragOffset[1] = e.getSceneY();
        });
        root.setOnMouseDragged(e -> host.moveWindow(e.getScreenX() - dragOffset[0],
                e.getScreenY() - dragOffset[1]));
        root.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && !(e.getTarget() instanceof Button)) {
                host.resetWindow();
            }
        });

        Button paletteBtn = new Button();
        paletteBtn.setGraphic(Icons.node("search", 14.0, "#c7ccd4"));
        paletteBtn.setPrefSize(30.0, 30.0);
        paletteBtn.setMinSize(30.0, 30.0);
        paletteBtn.setMaxSize(30.0, 30.0);
        paletteBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 0;"
                + " -fx-background-radius: 8;");
        paletteBtn.setTooltip(new Tooltip("命令面板  Ctrl+K"));
        paletteBtn.setOnMouseEntered(e -> paletteBtn.setStyle(
                "-fx-background-color: rgba(255,255,255,0.10); -fx-cursor: hand; -fx-padding: 0;"
                        + " -fx-background-radius: 8;"));
        paletteBtn.setOnMouseExited(e -> paletteBtn.setStyle(
                "-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 0;"
                        + " -fx-background-radius: 8;"));
        paletteBtn.setOnAction(e -> host.showPalette());

        root.getChildren().addAll(brand, spacer, navPane, paletteBtn, windowControls);
    }

    public HBox node() {
        return root;
    }

    public HBox navBox() {
        return navBox;
    }

    public Map<String, Button> navButtons() {
        return navButtons;
    }

    public Rectangle navIndicator() {
        return navIndicator;
    }

    public Circle statusDot() {
        return statusDot;
    }

    public Label statusLabel() {
        return statusLabel;
    }

    public Button stopGameButton() {
        return stopGame;
    }

    public Button cancelLaunchButton() {
        return cancelLaunch;
    }

    /** 导航高亮 + 指示器滑动。 */
    public void highlight(String page) {
        for (Map.Entry<String, Button> entry : navButtons.entrySet()) {
            Button button = entry.getValue();
            boolean active = entry.getKey().equals(page);
            button.getStyleClass().remove("nav-btn-active");
            if (active) {
                button.getStyleClass().add("nav-btn-active");
            }
            if (button.getUserData() instanceof String iconName) {
                button.setGraphic(Icons.node(iconName, 14.0, active ? "#5eead4" : "#b8c7d9"));
            }
        }
        Button active = navButtons.get(page);
        if (active == null) {
            return;
        }
        Platform.runLater(() -> {
            Timeline timeline = new Timeline(new KeyFrame(Duration.millis(260.0),
                    new KeyValue(navIndicator.translateXProperty(), active.getLayoutX(), Motion.SPRING),
                    new KeyValue(navIndicator.widthProperty(), active.getWidth(), Motion.SPRING)));
            timeline.play();
        });
    }

    /** 游戏运行状态：圆点颜色 + 文案 + 「结束游戏」按钮 + 呼吸动画。 */
    public void setGameRunning(boolean running) {
        stopGame.setVisible(running);
        stopGame.setManaged(running);
        statusDot.setFill(Color.web(running ? "#4CAF50" : "#6b7280"));
        statusDot.setOpacity(1.0);
        statusLabel.setText(running ? "游戏运行中" : "就绪");
        if (pulse != null) {
            pulse.stop();
            pulse = null;
        }
        if (running && Motion.isEnabled()) {
            pulse = new FadeTransition(Duration.seconds(1.1), statusDot);
            pulse.setFromValue(1.0);
            pulse.setToValue(0.35);
            pulse.setAutoReverse(true);
            pulse.setCycleCount(-1);
            pulse.play();
        }
    }

    /** 启动准备中：显示「取消启动」。 */
    public void setLaunching(boolean launching) {
        cancelLaunch.setVisible(launching);
        cancelLaunch.setManaged(launching);
    }

    private Button topButton(String text, String icon) {
        Button button = new Button(text);
        button.setGraphic(Icons.node(icon, 14.0, "#c7ccd4"));
        button.setContentDisplay(ContentDisplay.LEFT);
        button.setGraphicTextGap(6.0);
        button.getStyleClass().add("nav-btn");
        return button;
    }

    private Button windowButton(WindowIcon icon) {
        Button button = new Button();
        button.setGraphic(windowIcon(icon));
        button.setPrefSize(46.0, 32.0);
        button.setMinSize(46.0, 32.0);
        button.setMaxSize(46.0, 32.0);
        button.setStyle("-fx-background-color: transparent; -fx-text-fill: white;"
                + " -fx-cursor: hand; -fx-padding: 0; -fx-background-radius: 0;");
        return button;
    }

    private Node windowIcon(WindowIcon icon) {
        Pane pane = new Pane();
        pane.setPrefSize(14.0, 14.0);
        pane.setMinSize(14.0, 14.0);
        pane.setMaxSize(14.0, 14.0);
        Function<String, SVGPath> path = content -> {
            SVGPath svg = new SVGPath();
            svg.setContent(content);
            svg.setFill(Color.TRANSPARENT);
            svg.setStroke(Color.WHITE);
            svg.setStrokeWidth(1.1);
            svg.setStrokeLineCap(StrokeLineCap.SQUARE);
            return svg;
        };
        switch (icon) {
            case MINIMIZE -> pane.getChildren().add(path.apply("M2 7 H12"));
            case MAXIMIZE -> pane.getChildren().add(path.apply("M2.5 2.5 H11.5 V11.5 H2.5 Z"));
            case RESTORE -> {
                pane.getChildren().add(path.apply("M5 2.5 H11.5 V9"));
                pane.getChildren().add(path.apply("M2.5 5 H9 V11.5 H2.5 Z"));
            }
            case CLOSE -> pane.getChildren().add(path.apply("M2.5 2.5 L11.5 11.5 M11.5 2.5 L2.5 11.5"));
        }
        return pane;
    }
}
