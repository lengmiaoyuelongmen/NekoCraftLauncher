package com.example.demo;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 设置页：学习 PCL-CE 的「左侧选项卡 + 右侧内容」结构。
 * 页签：启动 / Java / 管理 / 个性化 / 杂项 / 软件信息。
 */
public final class SettingsPanel extends VBox {

    private javafx.scene.layout.HBox windowRatioRow;

    private final LauncherConfig config;
    private final Consumer<String> log;

    private final Map<String, Node> tabPages = new LinkedHashMap<>();
    private final Map<String, HBox> tabItems = new LinkedHashMap<>();
    private final StackPane content = new StackPane();
    private String currentTab = "启动";

    // 控件
    private final Spinner<Integer> memorySpinner;
    private final Spinner<Integer> widthSpinner;
    private final Spinner<Integer> heightSpinner;
    private final Spinner<Integer> concurrentSpinner;
    private final Spinner<Integer> speedSpinner;
    private final Spinner<Integer> assetSampleSpinner;
    private final TextField javaPathField;
    private final TextField gameDirField;
    private final CheckBox aiEnabledBox;
    private final PasswordField aiKeyField;
    private final TextField aiModelField;
    private final Label aiStatus;
    private final TextField jvmArgsField;
    private final ComboBox<String> sourceCombo;
    private final CheckBox autoJava;
    private final CheckBox isolationBox;
    private final CheckBox autoDepsBox;
    private final CheckBox autoRepairBox;
    private volatile boolean updateBusy;
    private final RadioButton autoMemoryRadio;
    private final RadioButton customMemoryRadio;
    private final Slider memorySlider;
    private final ProgressBar systemMemoryBar;
    private final ProgressBar gameMemoryBar;
    private final Label systemMemoryLabel;
    private final Label gameMemoryLabel;
    private final ComboBox<String> languageCombo;
    private final ComboBox<String> fontScaleCombo;
    private final TextArea feedbackArea;
    private final CheckBox feedbackAttach;
    private final CheckBox lightModeBox;
    private final CheckBox smartPerformanceBox;
    private final CheckBox preferGpuBox;
    private final ComboBox<String> gamePriorityCombo;
    private final CheckBox hardwareAccelBox;
    private final CheckBox animationsBox;
    private final ComboBox<String> animationSpeedCombo;
    private final Slider opacitySlider;
    private final VBox javaListBox = new VBox(2);
    private final Map<String, JavaScanner.JavaInfo> javaByLabel = new LinkedHashMap<>();
    private String selectedJavaLabel;
    private final Label savedHint = new Label("");

    public SettingsPanel(LauncherConfig config, Consumer<String> log) {
        this.config = config;
        this.log = log;
        setSpacing(0);

        aiEnabledBox = new CheckBox("启用 DeepSeek AI");
        aiEnabledBox.setSelected(config.isAiEnabled());
        aiEnabledBox.setStyle("-fx-text-fill: -app-text-2;");
        aiKeyField = new PasswordField();
        aiKeyField.setPromptText("sk-...");
        aiKeyField.setText(config.getAiApiKey());
        aiModelField = new TextField(config.getAiModel());
        aiModelField.setPromptText("deepseek-chat");
        aiStatus = new Label("Key 保存在本机 config.json，不会上传到除 DeepSeek 之外的服务。");
        aiStatus.setWrapText(true);
        aiStatus.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11px;");

        // ---------- 左：页签 ----------
        VBox side = new VBox(2);
        side.setPrefWidth(190);
        side.setMinWidth(190);
        side.setMaxWidth(190);
        side.setPadding(new Insets(14, 8, 12, 8));
        side.setStyle("-fx-background-color: -app-nav;"
                + " -fx-border-color: transparent -app-border-5 transparent transparent;");

        side.getChildren().addAll(
                sectionLabel("游戏"),
                tabItem("启动"),
                tabItem("Java"),
                tabItem("管理"),
                sectionLabel("工具"),
                tabItem("联机"),
                tabItem("AI"),
                sectionLabel("启动器"),
                tabItem("个性化"),
                tabItem("语言"),
                tabItem("杂项"),
                sectionLabel("关于"),
                tabItem("软件信息"),
                tabItem("软件更新"),
                tabItem("反馈"),
                tabItem("查看日志"));

        // ---------- 控件先建好，页签复用 ----------
        memorySpinner = spinner(512, 16384, config.getMemoryMb(), 512, 130);
        widthSpinner = spinner(320, 3840, config.getWindowWidth(), 10, 100);
        heightSpinner = spinner(240, 2160, config.getWindowHeight(), 10, 100);

        javafx.scene.control.ToggleGroup ratioGroup = new javafx.scene.control.ToggleGroup();
        javafx.scene.layout.HBox ratioRow = new javafx.scene.layout.HBox(6);
        String[][] ratios = {{"16:9", "16", "9"}, {"16:10", "16", "10"},
                {"4:3", "4", "3"}, {"21:9", "21", "9"}};
        double currentRatio = config.getWindowHeight() <= 0 ? 0
                : (double) config.getWindowWidth() / config.getWindowHeight();
        for (String[] ratio : ratios) {
            double value = Double.parseDouble(ratio[1]) / Double.parseDouble(ratio[2]);
            javafx.scene.control.ToggleButton chip = new javafx.scene.control.ToggleButton(ratio[0]);
            chip.setToggleGroup(ratioGroup);
            chip.setUserData(value);
            chip.setStyle("-fx-background-radius: 8; -fx-border-radius: 8; -fx-padding: 4 11;"
                    + " -fx-background-color: -app-row2; -fx-text-fill: -app-text-muted; -fx-cursor: hand;");
            chip.setOnAction(e -> {
                if (!chip.isSelected()) {
                    chip.setSelected(true);
                    return;
                }
                int width = widthSpinner.getValue();
                int height = (int) Math.round(width / value);
                height = Math.max(240, Math.min(2160, height));
                heightSpinner.getValueFactory().setValue(height);
            });
            ratioGroup.selectedToggleProperty().addListener((obs, old, selected) -> {
                boolean on = selected == chip;
                chip.setStyle("-fx-background-radius: 8; -fx-border-radius: 8; -fx-padding: 4 11;"
                        + " -fx-cursor: hand; -fx-background-color: " + (on ? "-app-accent" : "-app-row2") + ";"
                        + " -fx-text-fill: " + (on ? "#06251f" : "-app-text-muted") + ";");
            });
            if (Math.abs(currentRatio - value) < 0.02) chip.setSelected(true);
            ratioRow.getChildren().add(chip);
        }
        windowRatioRow = ratioRow;

        ToggleGroup memoryGroup = new ToggleGroup();
        autoMemoryRadio = new RadioButton("自动配置");
        customMemoryRadio = new RadioButton("自定义");
        autoMemoryRadio.setToggleGroup(memoryGroup);
        customMemoryRadio.setToggleGroup(memoryGroup);
        autoMemoryRadio.setStyle("-fx-text-fill: -app-text-2;");
        customMemoryRadio.setStyle("-fx-text-fill: -app-text-2;");
        autoMemoryRadio.setSelected(config.isAutoMemory());
        customMemoryRadio.setSelected(!config.isAutoMemory());

        memorySlider = new Slider(512, 16384, Math.max(512, Math.min(16384, config.getMemoryMb())));
        memorySlider.setBlockIncrement(512);
        memorySlider.setMajorTickUnit(4096);
        memorySlider.setShowTickMarks(false);
        memorySlider.setMaxWidth(Double.MAX_VALUE);

        systemMemoryBar = new ProgressBar(0);
        systemMemoryBar.setMaxWidth(Double.MAX_VALUE);
        systemMemoryBar.setPrefHeight(6);
        gameMemoryBar = new ProgressBar(0);
        gameMemoryBar.setMaxWidth(Double.MAX_VALUE);
        gameMemoryBar.setPrefHeight(6);
        systemMemoryLabel = new Label();
        systemMemoryLabel.setStyle("-fx-text-fill: -app-text-dim-2; -fx-font-size: 11px;");
        gameMemoryLabel = new Label();
        gameMemoryLabel.setStyle("-fx-text-fill: -app-text-dim-2; -fx-font-size: 11px;");

        memorySlider.valueProperty().addListener((o, ov, nv) -> {
            int value = (int) (Math.round(nv.doubleValue() / 512.0) * 512);
            value = Math.max(512, Math.min(16384, value));
            if (memorySpinner.getValue() != value) memorySpinner.getValueFactory().setValue(value);
            updateMemoryBars();
        });
        memorySpinner.valueProperty().addListener((o, ov, nv) -> {
            if (Math.abs(memorySlider.getValue() - nv) >= 256) memorySlider.setValue(nv);
            updateMemoryBars();
        });
        autoMemoryRadio.selectedProperty().addListener((o, ov, nv) -> {
            memorySlider.setDisable(nv);
            memorySpinner.setDisable(nv);
            if (nv) {
                MemoryAdvisor.Suggestion suggestion = MemoryAdvisor.suggest(currentModsDir());
                memorySlider.setValue(suggestion.memoryMb());
                memorySpinner.getValueFactory().setValue(suggestion.memoryMb());
            }
        });
        customMemoryRadio.selectedProperty().addListener((o, ov, nv) -> {
            if (nv) {
                memorySlider.setDisable(false);
                memorySpinner.setDisable(false);
            }
        });
        memorySlider.setDisable(config.isAutoMemory());
        memorySpinner.setDisable(config.isAutoMemory());
        if (config.isAutoMemory()) {
            MemoryAdvisor.Suggestion suggestion = MemoryAdvisor.suggest(currentModsDir());
            memorySlider.setValue(suggestion.memoryMb());
            memorySpinner.getValueFactory().setValue(suggestion.memoryMb());
        }
        updateMemoryBars();

        languageCombo = new ComboBox<>();
        languageCombo.getItems().addAll("简体中文", "English（预览，未完成）");
        languageCombo.setValue("en_US".equals(config.getUiLanguage())
                ? "English（预览，未完成）" : "简体中文");
        languageCombo.setMaxWidth(320);
        languageCombo.setStyle("-fx-padding: 4;");

        fontScaleCombo = new ComboBox<>();
        fontScaleCombo.getItems().addAll("小", "标准", "大", "特大");
        fontScaleCombo.setValue(fontScaleLabel(config.getUiFontScale()));
        fontScaleCombo.setMaxWidth(200);

        feedbackArea = new TextArea();
        feedbackArea.setPromptText("描述你遇到的问题 / 想要的功能…（可以附上复现步骤）");
        feedbackArea.setPrefRowCount(6);
        feedbackArea.setWrapText(true);
        feedbackAttach = new CheckBox("附加系统信息（启动器版本 / Java / 系统）");
        feedbackAttach.setSelected(true);
        feedbackAttach.setStyle("-fx-text-fill: -app-text-2;");

        lightModeBox = new CheckBox("浅色模式（顶栏保持深色）");
        lightModeBox.setSelected(config.isLightTheme());
        lightModeBox.setStyle("-fx-text-fill: -app-text-2;");
        lightModeBox.selectedProperty().addListener((o, ov, nv) -> {
            config.setLightTheme(nv);
            Theme.setLight(nv);
            config.save();
            if (getScene() != null) Theme.apply(getScene().getRoot());
        });

        smartPerformanceBox = new CheckBox("启用智能性能（G1GC 参数优化）");
        smartPerformanceBox.setSelected(config.isSmartPerformance());
        smartPerformanceBox.setStyle("-fx-text-fill: -app-text-2;");
        gamePriorityCombo = new ComboBox<>();
        gamePriorityCombo.getItems().addAll("普通", "高于正常", "高");
        gamePriorityCombo.setValue("above".equals(config.getGamePriority()) ? "高于正常"
                : "high".equals(config.getGamePriority()) ? "高" : "普通");
        gamePriorityCombo.setStyle("-fx-padding: 2;");
        preferGpuBox = new CheckBox("优先高性能独显（写入 HKCU 显卡首选项）");
        preferGpuBox.setSelected(config.isPreferDiscreteGpu());
        preferGpuBox.setStyle("-fx-text-fill: -app-text-2;");

        concurrentSpinner = spinner(1, 64, config.getMaxConcurrent(), 4, 100);
        speedSpinner = spinner(0, 102400, config.getSpeedLimitKb(), 256, 120);
        assetSampleSpinner = spinner(0, 200, config.getAssetSampleCount(), 10, 100);

        javaPathField = new TextField(config.getJavaPath());
        javaPathField.setPromptText("留空 = 自动检测，检测不到再自动下载 Zulu");
        HBox.setHgrow(javaPathField, Priority.ALWAYS);

        gameDirField = new TextField(config.getGameDir());
        gameDirField.setPromptText("例如 .minecraft 或 D:\\Minecraft");
        HBox.setHgrow(gameDirField, Priority.ALWAYS);

        jvmArgsField = new TextField(config.getJvmArgs());
        jvmArgsField.setPromptText("可选，例如 -XX:+UseG1GC");

        sourceCombo = new ComboBox<>(FXCollections.observableArrayList("auto", "official", "mirror"));
        sourceCombo.setValue(config.getDownloadSource());
        sourceCombo.setPrefWidth(150);

        autoJava = check("自动检测并选择最合适的 Java（推荐）", config.isAutoSelectJava());
        isolationBox = check("开启版本隔离（每个实例独立 mods / config / saves）", config.isIsolation());
        autoDepsBox = check("安装模组时自动补装缺失的前置依赖", config.isAutoInstallDeps());
        autoRepairBox = check("启动前自动补全缺失的库文件", config.isAutoRepairLibraries());
        hardwareAccelBox = check("启用硬件加速（显卡渲染；关闭后需重启启动器）", config.isHardwareAcceleration());
        animationsBox = check("界面动画效果（关闭 = 性能模式）", config.isAnimations());

        animationSpeedCombo = new ComboBox<>(FXCollections.observableArrayList("慢", "标准", "快"));
        animationSpeedCombo.setValue(speedLabel(config.getAnimationSpeed()));
        animationSpeedCombo.setPrefWidth(100);

        opacitySlider = new Slider(0.7, 1.0, config.getUiOpacity());
        opacitySlider.setPrefWidth(220);

        selectedJavaLabel = null;

        // ---------- 页签内容 ----------
        tabPages.put("启动", buildLaunchTab());
        tabPages.put("Java", buildJavaTab());
        tabPages.put("管理", buildManageTab());
        tabPages.put("个性化", buildUiTab());
        tabPages.put("杂项", buildMiscTab());
        tabPages.put("软件信息", buildAboutTab());
        tabPages.put("联机", buildLanTab());
        tabPages.put("语言", buildLanguageTab());
        tabPages.put("AI", buildAiTab());
        tabPages.put("软件更新", buildUpdateTab());
        tabPages.put("反馈", buildFeedbackTab());
        tabPages.put("查看日志", buildLogTab());

        content.setStyle("-fx-background-color: transparent;");
        HBox.setHgrow(content, Priority.ALWAYS);
        VBox.setVgrow(content, Priority.ALWAYS);

        // ---------- 底部保存栏 ----------
        savedHint.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 12px;");
        Button importConfig = smallButton("导入设置");
        importConfig.setOnAction(e -> importConfig());
        Button exportConfig = smallButton("导出设置");
        exportConfig.setOnAction(e -> exportConfig());
        Button reset = smallButton("恢复默认");
        reset.setOnAction(e -> resetDefaults());
        Button save = new Button("保存");
        save.setStyle("-fx-background-color: linear-gradient(to bottom right, #9ca3af, #6b7280);"
                + " -fx-text-fill: #111827; -fx-font-weight: bold; -fx-background-radius: 9;"
                + " -fx-padding: 9 28; -fx-cursor: hand;");
        Motion.press(save);
        save.setOnAction(e -> saveAll());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox bottom = new HBox(10, savedHint, spacer, importConfig, exportConfig, reset, save);
        bottom.setAlignment(Pos.CENTER_LEFT);
        bottom.setPadding(new Insets(12, 22, 14, 22));
        bottom.setStyle("-fx-background-color: -app-nav;"
                + " -fx-border-color: -app-border-5 transparent transparent transparent;");

        ModuleHeader settingsHeader = new ModuleHeader("03 / PREFERENCES", "设置",
                "启动参数 · Java · 外观", "gear", ModuleHeader.Badge.HEX, "本地配置");
        VBox.setMargin(settingsHeader, new Insets(12, 22, 2, 22));
        VBox right = new VBox(settingsHeader, content, bottom);
        VBox.setVgrow(content, Priority.ALWAYS);
        HBox.setHgrow(right, Priority.ALWAYS);

        HBox body = new HBox(side, right);
        HBox.setHgrow(right, Priority.ALWAYS);
        VBox.setVgrow(body, Priority.ALWAYS);
        getChildren().add(body);

        reloadJavaList();
        selectTab("启动");
    }

    // ================== 页签 ==================

    private Label sectionLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: -app-text-dim-3; -fx-font-size: 11px; -fx-font-weight: bold;");
        label.setPadding(new Insets(12, 10, 4, 12));
        return label;
    }

    private HBox tabItem(String name) {
        HBox item = new HBox(9);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(8, 10, 8, 10));
        item.setCursor(Cursor.HAND);
        item.setMaxWidth(Double.MAX_VALUE);
        item.setPickOnBounds(true);
        item.getStyleClass().add("app-tab");

        javafx.scene.shape.Rectangle bar = new javafx.scene.shape.Rectangle(3, 14);
        bar.setArcWidth(2);
        bar.setArcHeight(2);
        bar.setStyle("-fx-fill: transparent;");

        Label text = new Label(name);
        text.setStyle("-fx-text-fill: -app-text-dim; -fx-font-size: 13px;");
        item.getChildren().addAll(bar, text);
        item.setOnMouseClicked(e -> selectTab(name));
        tabItems.put(name, item);
        return item;
    }

    private void selectTab(String name) {
        currentTab = name;
        for (Map.Entry<String, HBox> entry : tabItems.entrySet()) {
            boolean active = entry.getKey().equals(name);
            HBox box = entry.getValue();
            box.getStyleClass().remove("app-tab-active");
            if (active) box.getStyleClass().add("app-tab-active");
            if (box.getChildren().size() >= 2) {
                box.getChildren().get(0).setStyle("-fx-fill: " + (active ? "#d1d5db" : "transparent") + ";");
                box.getChildren().get(1).setStyle("-fx-text-fill: " + (active ? "-app-text" : "-app-text-dim")
                        + "; -fx-font-size: 13px;");
            }
        }
        Node page = tabPages.get(name);
        if (page == null) return;
        content.getChildren().setAll(page);
        Motion.pageIn(page, 8);
        if (page instanceof ScrollPane scroll) {
            Platform.runLater(() -> scroll.setVvalue(0));
        }
    }

    // ================== 各页签内容 ==================

    private ScrollPane buildLaunchTab() {
        VBox memoryBox = new VBox(6, memorySlider,
                new HBox(8, memorySpinner, hint("MB")));
        VBox card = card(
                section("启动设置"),
                labeled("窗口比例", windowRatioRow),
                labeled("窗口尺寸", new HBox(8, widthSpinner, new Label("×"), heightSpinner, hint("像素"))),
                section("游戏内存"),
                new HBox(18, autoMemoryRadio, customMemoryRadio),
                labeled("最大内存 (-Xmx)", memoryBox),
                labeled("系统内存占用", new VBox(4, systemMemoryBar, systemMemoryLabel)),
                labeled("游戏分配", new VBox(4, gameMemoryBar, gameMemoryLabel)),
                labeled("内存优化", memoryOptimizeRow()),
                section("智能性能"),
                labeled("智能优化", new HBox(10, smartPerformanceBox, gamePriorityCombo,
                        hint("G1GC 参数优化；进程优先级右侧独立设置"))),
                labeled("一键分配", new HBox(10,
                        smallButton("智能分配性能", e -> applySmartPerformance()),
                        hint("按本机内存 + 当前实例模组数推荐"))),
                hint("参考 Pavise 的思路，只做 JVM 调优与进程优先级；不写注册表、不改系统服务、不清理 standby 内存"),
                section("实例"),
                isolationBox,
                labeled("游戏目录", new HBox(8, gameDirField, browseButton("浏览", this::browseGameDir))),
                labeled("额外 JVM 参数", jvmArgsField));
        return scroll(card);
    }

    /** 内存优化：显示本机内存，一键推荐最大内存 / 填充 G1GC 参数。 */
    private HBox memoryOptimizeRow() {
        long totalMb = totalMemoryMb();
        Label info = hint(totalMb > 0 ? "本机约 " + (totalMb / 1024) + " GB" : "无法读取本机内存");
        Button recommend = smallButton("智能分配", e -> {
            MemoryAdvisor.Suggestion suggestion = MemoryAdvisor.suggest(currentModsDir());
            memorySlider.setValue(suggestion.memoryMb());
            memorySpinner.getValueFactory().setValue(suggestion.memoryMb());
            Toast.ok("建议 " + suggestion.memoryMb() + " MB：" + suggestion.reason());
        });
        Button jvm = smallButton("填充优化 JVM 参数", e -> {
            String preset = "-XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=50 "
                    + "-XX:+UnlockExperimentalVMOptions -XX:G1NewSizePercent=20 "
                    + "-XX:G1MaxNewSizePercent=40 -XX:G1HeapRegionSize=8M "
                    + "-XX:G1ReservePercent=20 -XX:InitiatingHeapOccupancyPercent=15";
            String current = jvmArgsField.getText() == null ? "" : jvmArgsField.getText().trim();
            if (current.contains("UseG1GC")) {
                Toast.info("已经填过 G1GC 参数了");
                return;
            }
            jvmArgsField.setText(current.isBlank() ? preset : current + " " + preset);
            Toast.ok("已填充 G1GC 优化参数");
        });
        HBox row = new HBox(10, info, recommend, jvm);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private void applySmartPerformance() {
        MemoryAdvisor.Suggestion suggestion = MemoryAdvisor.suggest(currentModsDir());
        memorySlider.setValue(suggestion.memoryMb());
        memorySpinner.getValueFactory().setValue(suggestion.memoryMb());
        PerformanceTuner.Plan plan = PerformanceTuner.plan(suggestion.memoryMb());
        smartPerformanceBox.setSelected(true);
        gamePriorityCombo.setValue("高");
        log.accept("⚡ 智能性能：" + plan.summary() + "；" + suggestion.reason());
        Toast.ok("已分配 " + suggestion.memoryMb() + " MB，并启用 G1GC + 高优先级");
    }

    private File currentModsDir() {
        File game = new File(config.getGameDir());
        String id = config.getLastInstance();
        if (id == null || id.isBlank()) return new File(game, "mods");
        return InstanceDirs.mods(game, id, config.isIsolation());
    }

    private long totalMemoryMb() {
        try {
            java.lang.management.OperatingSystemMXBean bean =
                    java.lang.management.ManagementFactory.getOperatingSystemMXBean();
            if (bean instanceof com.sun.management.OperatingSystemMXBean os) {
                return os.getTotalMemorySize() / 1024 / 1024;
            }
        } catch (Exception ignored) {
            // 读取失败就按未知处理
        }
        return -1;
    }

    private long recommendMemoryMb(long totalMb) {
        if (totalMb <= 0) return 4096;
        long capped = Math.min(8192, Math.max(1024, totalMb / 2));
        return Math.round(capped / 512.0) * 512;
    }

    private ScrollPane buildJavaTab() {
        VBox card = card(
                section("Java 运行时"),
                autoJava,
                javaContainer(),
                new HBox(8, smallButton("重新检测", e -> reloadJavaList()), hint("选中某一项会填进下面的路径")),
                labeled("Java 路径", new HBox(8, javaPathField, browseButton("浏览", this::browseJava))));
        return scroll(card);
    }

    private VBox javaContainer() {
        VBox box = new VBox(2, javaListBox);
        box.setStyle("-fx-background-color: -app-panel-2; -fx-background-radius: 9;"
                + " -fx-border-color: -app-hover; -fx-border-radius: 9; -fx-padding: 6;");
        box.setMinHeight(130);
        return box;
    }

    private ScrollPane buildManageTab() {
        HBox downloadRow = new HBox(18,
                inline("最大并发", concurrentSpinner, "个连接"),
                inline("限速", speedSpinner, "KB/s，0=不限"));
        downloadRow.setAlignment(Pos.CENTER_LEFT);

        VBox card = card(
                section("下载"),
                labeled("下载源", new HBox(10, sourceCombo, hint("auto=镜像优先失败回官方；official/mirror=只用其一"))),
                labeled("下载策略", downloadRow),
                section("模组"),
                autoDepsBox,
                autoRepairBox,
                labeled("完整性抽样", new HBox(10, assetSampleSpinner, hint("每个实例抽样校验的 assets 对象数，0 = 不抽样"))),
                hint("自动补装前置依赖：最多递归两层，找不到会写进日志"));
        return scroll(card);
    }

    private ScrollPane buildUiTab() {
        HBox themeRow = new HBox(10);
        themeRow.setAlignment(Pos.CENTER_LEFT);
        for (Theme.Palette palette : Theme.PRESETS) {
            Button swatch = new Button();
            swatch.setUserData("theme-swatch");
            boolean active = palette.name().equals(Theme.current().name());
            swatch.setStyle("-fx-background-color: " + palette.accent() + ";"
                    + " -fx-background-radius: 18; -fx-min-width: 30; -fx-min-height: 30;"
                    + " -fx-max-width: 30; -fx-max-height: 30; -fx-cursor: hand;"
                    + " -fx-border-color: " + (active ? "#ffffff" : "transparent") + ";"
                    + " -fx-border-radius: 18; -fx-border-width: 2;");
            javafx.scene.control.Tooltip.install(swatch,
                    new javafx.scene.control.Tooltip(palette.name()));
            swatch.setOnAction(e -> {
                Theme.setCurrent(palette);
                config.setAccentColor(palette.name());
                config.save();
                if (getScene() != null) Theme.apply(getScene().getRoot());
                Toast.ok("主题色已切换：" + palette.name());
                selectTab("个性化");
            });
            themeRow.getChildren().add(swatch);
        }

        Button chooseBg = smallButton("选择背景图片", e -> chooseBackground());
        Button clearBg = smallButton("清除背景", e -> {
            config.setBackgroundImage("");
            config.save();
            applyBackground();
            Toast.ok("已清除启动页背景");
            selectTab("个性化");
        });
        String bg = config.getBackgroundImage();
        Label bgLabel = hint(bg == null || bg.isBlank() ? "未设置" : new File(bg).getName());

        VBox card = card(
                section("主题"),
                labeled("强调色", themeRow),
                hint("默认青绿，刻意和 PCL-CE 的蓝色区分开；切换后立即生效并保存"),
                section("背景"),
                labeled("启动页背景", new HBox(10, chooseBg, clearBg, bgLabel)),
                section("外观"),
                labeled("字体大小", new HBox(10, fontScaleCombo, hint("重启启动器后生效"))),
                labeled("浅色模式", lightModeBox),
                labeled("不透明度", new HBox(10, opacitySlider, hint("窗口不透明度，保存后立即生效"))),
                section("动画"),
                labeled("动画效果", new HBox(10, animationsBox, animationSpeedCombo, hint("低配机器建议关掉动画"))),
                hint("动画关闭后，页面切换 / 卡片入场 / 按钮反馈都会变成瞬时完成"));
        return scroll(card);
    }

    private void chooseBackground() {
        javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
        chooser.setTitle("选择启动页背景图片");
        chooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter(
                "图片", "*.png", "*.jpg", "*.jpeg", "*.webp", "*.gif"));
        File file = chooser.showOpenDialog(getScene() == null ? null : getScene().getWindow());
        if (file == null) return;
        config.setBackgroundImage(file.getAbsolutePath());
        config.save();
        applyBackground();
        Toast.ok("背景已设置：" + file.getName());
        selectTab("个性化");
    }

    /** 把背景图直接贴到启动页（通过 id 查找），并重新套一遍主题色。 */
    private void applyBackground() {
        if (getScene() == null) return;
        javafx.scene.Node node = getScene().lookup("#launch-root");
        if (node == null) return;
        String path = config.getBackgroundImage();
        File file = path == null || path.isBlank() ? null : new File(path);
        if (file != null && file.isFile()) {
            node.setStyle("-fx-background-image: url('" + file.toURI() + "');"
                    + " -fx-background-size: cover; -fx-background-position: center center;"
                    + " -fx-background-color: transparent;");
        } else {
            node.setStyle("");
        }
        Theme.apply(getScene().getRoot());
    }

    private ScrollPane buildMiscTab() {
        Button restart = smallButton("重启启动器", e -> restartLauncher());
        HBox restartRow = new HBox(10, restart, hint("切换硬件加速 / 导入设置后，用它让改动生效"));
        restartRow.setAlignment(Pos.CENTER_LEFT);

        VBox card = card(
                section("渲染"),
                hardwareAccelBox,
                restartRow,
                section("数据"),
                new HBox(10,
                        smallButton("打开配置目录", e -> openDir(new File(System.getProperty("user.dir")))),
                        smallButton("打开游戏目录", e -> openDir(new File(config.getGameDir()))),
                        smallButton("打开缓存目录", e -> openDir(new File("cache")))),
                hint("config.json / cache 都保存在启动器工作目录"));
        return scroll(card);
    }

    private ScrollPane buildAboutTab() {
        Label name = new Label("Neko Craft Launcher");
        name.setStyle("-fx-text-fill: -app-text; -fx-font-size: 20px; -fx-font-weight: bold;");
        Label version = new Label("版本 " + AppVersion.CURRENT + " · JavaFX 21 · 参考 PCL-CE 的选项卡结构");
        version.setStyle("-fx-text-fill: -app-text-dim-2; -fx-font-size: 12px;");
        HardwareInfo.Detect hardware = HardwareInfo.cached() != null
                ? HardwareInfo.cached() : HardwareInfo.detect(new File(config.getGameDir()));

        VBox card = card(
                name,
                version,
                new Region() {{ setPrefHeight(4); }},
                section("硬件"),
                infoRow("CPU", hardware.cpu() + "（" + hardware.logicalCores() + " 线程）"),
                infoRow("内存", String.format(java.util.Locale.ROOT, "%.1f GB", hardware.ramMb() / 1024.0)),
                infoRow("显卡", hardware.gpu().label()),
                infoRow("游戏盘剩余", hardware.gameDiskFreeMb() > 0
                        ? (hardware.gameDiskFreeMb() / 1024) + " GB" : "未知"),
                section("环境"),
                infoRow("工作目录", new File(System.getProperty("user.dir")).getAbsolutePath()),
                infoRow("游戏目录", new File(config.getGameDir()).getAbsolutePath()),
                infoRow("Java", System.getProperty("java.version") + " / " + System.getProperty("java.vendor")),
                infoRow("系统", System.getProperty("os.name") + " " + System.getProperty("os.arch")),
                section("链接"),
                new HBox(10,
                        smallButton("PCL-CE 仓库", e -> openBrowser("https://github.com/PCL-Community/PCL-CE")),
                        smallButton("Modrinth", e -> openBrowser("https://modrinth.com")),
                        smallButton("打开日志页", e -> Toast.info("按 F8 打开日志页"))));
        return scroll(card);
    }

    /** 外部入口：按名字打开某个页签（F8 / 启动失败提示用）。 */
    public void openTab(String name) {
        if (tabPages.containsKey(name)) selectTab(name);
    }

    private VBox buildLogTab() {
        LogsPanel panel = new LogsPanel(new File(config.getGameDir()), log);
        VBox.setVgrow(panel, Priority.ALWAYS);
        return panel;
    }

    private ScrollPane buildLanTab() {
        String ip = localIp();
        Label address = new Label(ip + ":25565");
        address.setStyle("-fx-text-fill: -app-text; -fx-font-size: 14px; -fx-font-weight: bold;");
        Button copy = smallButton("复制地址", e -> {
            copyToClipboard(ip + ":25565");
            Toast.ok("已复制局域网地址");
        });
        VBox card = card(
                section("局域网联机"),
                labeled("本机地址", new HBox(12, address, copy)),
                hint("进入游戏后按 Esc → 对局域网开放，其他玩家用上面的地址直连（同一局域网）。"),
                hint("PCL CE 的 Natayark 大厅联机需要账号与官方服务，本启动器暂未接入；这里只提供局域网直连信息。"),
                section("快速步骤"),
                hint("1. 启动游戏并进入存档\n2. 菜单 → 对局域网开放\n3. 把 本机地址 + 端口 发给朋友\n4. 朋友：多人游戏 → 直接连接"),
                hint("如果连不上，检查 Windows 防火墙是否放行 Java。"));
        return scroll(card);
    }

    private ScrollPane buildLanguageTab() {
        VBox card = card(
                section("界面语言"),
                labeled("语言", languageCombo),
                hint("English 界面仍在开发中，切换后只有部分文本会变化，保存后重启生效。"));
        return scroll(card);
    }

    private ScrollPane buildAiTab() {
        Label title = new Label("DeepSeek AI");
        title.setStyle("-fx-text-fill: -app-text; -fx-font-size: 15px; -fx-font-weight: bold;");
        Label intro = new Label("开启后，启动页命令栏里输入的文字会直接发给 DeepSeek；关闭后恢复为启动器命令 / 游戏命令。"
                + "未填写 API Key 时自动使用本地免费助手（不联网，只回答启动器 / 常见 MC 问题）。");
        intro.setWrapText(true);
        intro.setStyle("-fx-text-fill: -app-text-dim; -fx-font-size: 12px;");

        Button test = smallButton("测试连接", e -> testAi());
        Button openKeys = smallButton("打开 DeepSeek 开放平台", e -> openBrowser("https://platform.deepseek.com/api_keys"));
        Button webChat = smallButton("DeepSeek 网页版", e -> openBrowser("https://chat.deepseek.com"));

        VBox card = card(
                title,
                intro,
                aiEnabledBox,
                labeled("API Key", aiKeyField),
                labeled("模型", aiModelField),
                new HBox(10, test, openKeys, webChat),
                aiStatus,
                hint("DeepSeek 官方 API 需要 Key；未填时使用本地免费助手，不等同于在线模型。"
                        + " 计费由 DeepSeek 账户承担；不需要 AI 时取消勾选即可。"));
        return scroll(card);
    }

    private void testAi() {
        String key = aiKeyField.getText() == null ? "" : aiKeyField.getText().trim();
        if (key.isBlank()) {
            aiStatus.setText("未填写 Key：本地免费助手可用，不需要联网。");
            aiStatus.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11px;");
            return;
        }
        aiStatus.setText("正在测试 DeepSeek 连接…");
        CompletableFuture.supplyAsync(() -> {
            try {
                return DeepSeekClient.chat(key, config.getAiBaseUrl(), aiModelField.getText(),
                        List.of(new DeepSeekClient.Message("user", "只回复两个字：可用")));
            } catch (Exception ex) {
                throw new java.util.concurrent.CompletionException(ex);
            }
        }).thenAccept(reply -> Platform.runLater(() -> {
            aiStatus.setText("连接成功，模型回复：" + reply);
            aiStatus.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 11px;");
        })).exceptionally(ex -> {
            Platform.runLater(() -> {
                aiStatus.setText("连接失败：" + rootMessage(ex));
                aiStatus.setStyle("-fx-text-fill: #f87171; -fx-font-size: 11px;");
            });
            return null;
        });
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) current = current.getCause();
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }

    private void downloadAndInstall(UpdateChecker.Info info, Label status, Button button) {
        if (updateBusy) {
            status.setText("更新正在下载或安装中，请稍候…");
            return;
        }
        String url = info.url() == null ? "" : info.url().trim();
        if (!url.startsWith("https://") || !url.toLowerCase(java.util.Locale.ROOT).contains(".exe")) {
            openLink(url);
            return;
        }
        String safeVersion = info.version().replaceAll("[^0-9A-Za-z._-]", "_");
        java.nio.file.Path target = java.nio.file.Paths.get(System.getProperty("java.io.tmpdir"),
                "NekoCraftLauncher-Setup-" + safeVersion + ".exe");
        updateBusy = true;
        button.setDisable(true);
        status.setText("正在下载更新 " + info.version() + "…");
        status.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11px;");
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                UpdateChecker.download(url, target, progress -> Platform.runLater(() -> {
                    if (progress >= 0) {
                        status.setText("正在下载更新 " + info.version() + "… "
                                + (int) Math.round(progress * 100) + "%");
                    }
                }));
            } catch (Exception ex) {
                throw new java.util.concurrent.CompletionException(ex);
            }
        }).thenAccept(v -> Platform.runLater(() -> {
            // 保持按钮禁用，避免下载完成到安装启动之间被再次点击
            status.setText("下载完成，正在自动安装 " + info.version() + "…");
            status.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 11px;");
            Toast.ok("更新已下载，正在自动安装");
            try {
                ProcessBuilder installer = new ProcessBuilder(target.toString(),
                        "/VERYSILENT", "/SUPPRESSMSGBOXES", "/NORESTART",
                        "/CLOSEAPPLICATIONS");
                installer.directory(target.getParent().toFile());
                installer.start();
                // 给安装程序一点时间接管，然后退出启动器以便覆盖 exe
                Thread shutdown = new Thread(() -> {
                    try {
                        Thread.sleep(1200);
                    } catch (InterruptedException ignored) {
                    }
                    Platform.runLater(() -> {
                        Platform.exit();
                        System.exit(0);
                    });
                }, "neko-update-shutdown");
                shutdown.setDaemon(true);
                shutdown.start();
            } catch (Exception ex) {
                updateBusy = false;
                button.setDisable(false);
                try {
                    java.awt.Desktop.getDesktop().open(target.toFile());
                    status.setText("安装程序已打开，请按提示完成安装。");
                } catch (Exception openError) {
                    status.setText("安装包已下载到 " + target + "，请手动运行。");
                    status.setStyle("-fx-text-fill: #f87171; -fx-font-size: 11px;");
                }
            }
        })).exceptionally(ex -> {
            Throwable cause = ex;
            while (cause.getCause() != null) cause = cause.getCause();
            final Throwable root = cause;
            Platform.runLater(() -> {
                updateBusy = false;
                button.setDisable(false);
                status.setText("下载失败：" + (root.getMessage() == null
                        ? root.getClass().getSimpleName() : root.getMessage()));
                status.setStyle("-fx-text-fill: #f87171; -fx-font-size: 11px;");
            });
            return null;
        });
    }

    private ScrollPane buildUpdateTab() {
        Label version = new Label("当前版本：Neko Craft Launcher " + AppVersion.CURRENT);
        version.setStyle("-fx-text-fill: -app-text; -fx-font-size: 13px;");

        javafx.scene.control.TextField urlField = new javafx.scene.control.TextField(config.getUpdateUrl());
        urlField.setPromptText("https://api.github.com/repos/用户/NekoCraftLauncher/releases/latest");
        HBox.setHgrow(urlField, Priority.ALWAYS);
        javafx.scene.control.TextField mirrorField =
                new javafx.scene.control.TextField(config.getUpdateMirrorUrl());
        mirrorField.setPromptText("备用更新源 / Gitee / 自建 manifest（可选）");
        HBox.setHgrow(mirrorField, Priority.ALWAYS);

        Button saveUrl = smallButton("保存更新源", e -> {
            config.setUpdateUrl(urlField.getText().trim());
            config.setUpdateMirrorUrl(mirrorField.getText().trim());
            config.save();
            Toast.ok("更新源已保存");
        });
        Button download = smallButton("下载并安装更新", e -> { });
        download.setVisible(false);
        download.setManaged(false);

        Label status = new Label("更新检查使用静态 JSON：version / url / notes。");
        status.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11px;");
        status.setWrapText(true);

        Button check = smallButton("检查更新", e -> {
            String url = urlField.getText().trim();
            String mirror = mirrorField.getText().trim();
            config.setUpdateUrl(url);
            config.setUpdateMirrorUrl(mirror);
            config.save();
            if (url.isBlank()) {
                Toast.info("请先填写更新源 JSON 地址");
                return;
            }
            ((Button) e.getSource()).setDisable(true);
            status.setText("正在检查更新…");
            status.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11px;");
            java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                try {
                    return UpdateChecker.fetch(url, mirror);
                } catch (Exception ex) {
                    throw new java.util.concurrent.CompletionException(ex);
                }
            }).thenAccept(info -> Platform.runLater(() -> {
                ((Button) e.getSource()).setDisable(false);
                if (UpdateChecker.isNewer(info.version(), AppVersion.CURRENT)) {
                    status.setText("发现新版本 " + info.version()
                            + (info.notes().isBlank() ? "" : "：" + info.notes()));
                    status.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 11px;");
                    if (!info.url().isBlank()) {
                        download.setVisible(true);
                        download.setManaged(true);
                        download.setOnAction(ev -> downloadAndInstall(info, status, download));
                    }
                } else {
                    status.setText("已是最新版本（远端 " + info.version() + "）");
                    status.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11px;");
                    download.setVisible(false);
                    download.setManaged(false);
                }
            })).exceptionally(ex -> {
                Throwable cause = ex;
                while (cause.getCause() != null) cause = cause.getCause();
                final Throwable root = cause;
                Platform.runLater(() -> {
                    ((Button) e.getSource()).setDisable(false);
                    status.setText("检查失败：" + (root.getMessage() == null
                            ? root.getClass().getSimpleName() : root.getMessage()));
                    status.setStyle("-fx-text-fill: #f87171; -fx-font-size: 11px;");
                });
                return null;
            });
        });

        Label promiseTitle = new Label("为爱发电 · 永远免费更新");
        promiseTitle.setStyle("-fx-text-fill: #ffcf6a; -fx-font-size: 14px; -fx-font-weight: bold;");
        VBox card = card(
                section("为爱发电"),
                promiseTitle,
                hint("无广告、无付费墙、无强制登录；更新只为把新功能送到你手里。"),
                section("版本信息"),
                version,
                hint("支持 GitHub Releases API、仓库地址，或静态 JSON：version / url / notes。"),
                hint("GitHub 仓库地址会自动转换为 releases/latest；留空则不检查。"),
                new HBox(10, urlField, saveUrl, check, download),
                new HBox(10, mirrorField),
                status);
        return scroll(card);
    }

    private void openLink(String url) {
        try {
            java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
        } catch (Exception e) {
            Toast.error("打开链接失败：" + e.getMessage());
        }
    }





    private ScrollPane buildFeedbackTab() {
        Button copy = smallButton("复制反馈内容", e -> {
            StringBuilder text = new StringBuilder();
            text.append("【反馈】\n")
                    .append(feedbackArea.getText() == null ? "" : feedbackArea.getText())
                    .append("\n\n");
            if (feedbackAttach.isSelected()) text.append(systemInfo());
            copyToClipboard(text.toString());
            Toast.ok(feedbackArea.getText() == null || feedbackArea.getText().isBlank()
                    ? "已复制系统信息" : "反馈内容已复制");
        });
        Button bundle = smallButton("导出反馈包", e -> {
            try {
                File zip = SupportBundle.create(new File(config.getGameDir()), config,
                        feedbackArea.getText(), systemInfo());
                Toast.ok("反馈包已生成：" + zip.getName());
                openDir(zip.getParentFile());
            } catch (Exception ex) {
                Toast.error("导出失败：" + ex.getMessage());
            }
        });
        Button logs = smallButton("打开日志目录", e -> openDir(new File(config.getGameDir(), "logs")));
        Button clear = smallButton("清空", e -> feedbackArea.clear());
        VBox card = card(
                section("反馈"),
                hint("描述你遇到的问题 / 想要的功能；需要的话把日志一起附上。"),
                feedbackArea,
                feedbackAttach,
                new HBox(10, copy, bundle, logs, clear),
                hint("反馈会复制到剪贴板，可直接粘贴到群聊或 Issues。"));
        return scroll(card);
    }

    private String fontScaleLabel(double scale) {
        if (scale < 0.97) return "小";
        if (scale > 1.16) return "特大";
        if (scale > 1.04) return "大";
        return "标准";
    }

    private double fontScaleValue(String label) {
        if (label == null) return 1.0;
        return switch (label) {
            case "小" -> 0.9;
            case "大" -> 1.1;
            case "特大" -> 1.2;
            default -> 1.0;
        };
    }

    private String systemInfo() {
        return "启动器: Neko Craft Launcher 1.1.0\n"
                + "Java: " + System.getProperty("java.version")
                + " (" + System.getProperty("java.vendor") + ")\n"
                + "系统: " + System.getProperty("os.name") + " "
                + System.getProperty("os.version") + " " + System.getProperty("os.arch") + "\n"
                + "内存: 最大 " + config.getMemoryMb() + " MB"
                + (config.isAutoMemory() ? "（自动配置）" : "（自定义）") + "\n"
                + "游戏目录: " + new File(config.getGameDir()).getAbsolutePath() + "\n";
    }

    private void updateMemoryBars() {
        if (systemMemoryBar == null) return;
        long total = totalMemoryMb();
        if (total <= 0) return;
        long used = totalMemoryUsedMb();
        systemMemoryBar.setProgress(Math.min(1.0, used / (double) total));
        systemMemoryLabel.setText(String.format("已使用 %.1f GB / 共 %.1f GB",
                used / 1024.0, total / 1024.0));
        int memory = memorySpinner.getValue();
        gameMemoryBar.setProgress(Math.min(1.0, memory / (double) total));
        gameMemoryLabel.setText("游戏分配 " + memory + " MB");
    }

    private long totalMemoryUsedMb() {
        try {
            java.lang.management.OperatingSystemMXBean bean =
                    java.lang.management.ManagementFactory.getOperatingSystemMXBean();
            if (bean instanceof com.sun.management.OperatingSystemMXBean os) {
                return (os.getTotalMemorySize() - os.getFreeMemorySize()) / 1024 / 1024;
            }
        } catch (Exception ignored) {
            // 读不到就返回 0
        }
        return 0;
    }

    private String localIp() {
        try {
            java.util.Enumeration<java.net.NetworkInterface> interfaces =
                    java.net.NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                java.net.NetworkInterface ni = interfaces.nextElement();
                if (!ni.isUp() || ni.isLoopback()) continue;
                java.util.Enumeration<java.net.InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    java.net.InetAddress address = addresses.nextElement();
                    if (address instanceof java.net.Inet4Address && !address.isLoopbackAddress()) {
                        return address.getHostAddress();
                    }
                }
            }
        } catch (Exception ignored) {
            // 没有网络接口
        }
        return "127.0.0.1";
    }

    private void copyToClipboard(String text) {
        try {
            java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new java.awt.datatransfer.StringSelection(text), null);
        } catch (Exception e) {
            javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
            content.putString(text);
            javafx.scene.input.Clipboard.getSystemClipboard().setContent(content);
        }
    }

    // ================== 保存 / 恢复 ==================

    private void saveAll() {
        commitSpinner(memorySpinner);
        commitSpinner(widthSpinner);
        commitSpinner(heightSpinner);
        commitSpinner(concurrentSpinner);
        commitSpinner(speedSpinner);
        commitSpinner(assetSampleSpinner);

        config.setAiEnabled(aiEnabledBox.isSelected());
        config.setAiApiKey(aiKeyField.getText() == null ? "" : aiKeyField.getText().trim());
        config.setAiModel(aiModelField.getText() == null ? "" : aiModelField.getText().trim());
        config.setMemoryMb(memorySpinner.getValue());
        config.setAutoMemory(autoMemoryRadio.isSelected());
        config.setAccentColor(Theme.current().name());
        config.setUiFontScale(fontScaleValue(fontScaleCombo.getValue()));
        config.setLightTheme(lightModeBox.isSelected());
        config.setSmartPerformance(smartPerformanceBox.isSelected());
        config.setPreferDiscreteGpu(preferGpuBox.isSelected());
        config.setGamePriority(switch (gamePriorityCombo.getValue()) {
            case "高于正常" -> "above";
            case "高" -> "high";
            default -> "normal";
        });
        config.setUiLanguage(languageCombo.getValue() != null && languageCombo.getValue().startsWith("English")
                ? "en_US" : "zh_CN");
        config.setWindowWidth(widthSpinner.getValue());
        config.setWindowHeight(heightSpinner.getValue());
        config.setJavaPath(javaPathField.getText().trim());
        config.setAutoSelectJava(autoJava.isSelected());
        config.setGameDir(gameDirField.getText().trim());
        config.setJvmArgs(jvmArgsField.getText().trim());
        config.setDownloadSource(sourceCombo.getValue());
        config.setMaxConcurrent(concurrentSpinner.getValue());
        config.setSpeedLimitKb(speedSpinner.getValue());
        config.setAssetSampleCount(assetSampleSpinner.getValue());
        config.setIsolation(isolationBox.isSelected());
        config.setAutoInstallDeps(autoDepsBox.isSelected());
        config.setAutoRepairLibraries(autoRepairBox.isSelected());
        config.setHardwareAcceleration(hardwareAccelBox.isSelected());
        config.setAnimations(animationsBox.isSelected());
        config.setAnimationSpeed(speedValue(animationSpeedCombo.getValue()));
        config.setUiOpacity(Math.round(opacitySlider.getValue() * 100.0) / 100.0);
        Motion.configure(config.isAnimations(), config.getAnimationSpeed());

        if (getScene() != null && getScene().getWindow() != null) {
            getScene().getWindow().setOpacity(config.getUiOpacity());
        }
        config.save();
        savedHint.setText("已保存 ✓");
        Toast.ok("设置已保存");
        if (log != null) {
            log.accept("⚙ 设置已保存：内存 " + config.getMemoryMb() + "MB，下载源 "
                    + config.getDownloadSource() + "，并发 " + config.getMaxConcurrent());
        }
    }

    private void resetDefaults() {
        aiEnabledBox.setSelected(false);
        aiKeyField.clear();
        aiModelField.setText("deepseek-chat");
        memorySpinner.getValueFactory().setValue(2048);
        widthSpinner.getValueFactory().setValue(854);
        heightSpinner.getValueFactory().setValue(480);
        javaPathField.setText("");
        autoJava.setSelected(true);
        gameDirField.setText(".minecraft");
        jvmArgsField.setText("");
        sourceCombo.setValue("auto");
        concurrentSpinner.getValueFactory().setValue(16);
        speedSpinner.getValueFactory().setValue(0);
        assetSampleSpinner.getValueFactory().setValue(20);
        isolationBox.setSelected(true);
        autoDepsBox.setSelected(true);
        autoRepairBox.setSelected(true);
        hardwareAccelBox.setSelected(true);
        animationsBox.setSelected(true);
        animationSpeedCombo.setValue("标准");
        opacitySlider.setValue(1.0);
        autoMemoryRadio.setSelected(false);
        customMemoryRadio.setSelected(true);
        languageCombo.setValue("简体中文");
        fontScaleCombo.setValue("标准");
        lightModeBox.setSelected(true);
        Theme.setLight(true);
        if (getScene() != null) Theme.apply(getScene().getRoot());
        Theme.setCurrent(Theme.PRESETS[0]);
        config.setAccentColor("Neko 白");
        config.setBackgroundImage("");
        config.save();
        applyBackground();
        savedHint.setText("已恢复默认值，点保存生效");
    }

    private void exportConfig() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("导出启动器设置");
        chooser.setInitialFileName("launcher-config.json");
        File target = chooser.showSaveDialog(owner());
        if (target == null) return;
        try {
            config.save();
            Files.copy(new File("config.json").toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            savedHint.setText("已导出: " + target.getName());
            Toast.ok("设置已导出");
        } catch (Exception ex) {
            savedHint.setText("导出失败: " + ex.getMessage());
        }
    }

    private void importConfig() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("导入启动器设置");
        File source = chooser.showOpenDialog(owner());
        if (source == null) return;
        try {
            Files.copy(source.toPath(), new File("config.json").toPath(), StandardCopyOption.REPLACE_EXISTING);
            LauncherConfig loaded = LauncherConfig.load();
            config.setMemoryMb(loaded.getMemoryMb());
            Theme.setCurrent(Theme.byName(loaded.getAccentColor()));
            config.setAccentColor(Theme.current().name());
            config.setUiFontScale(loaded.getUiFontScale());
            config.setBackgroundImage(loaded.getBackgroundImage());
            fontScaleCombo.setValue(fontScaleLabel(loaded.getUiFontScale()));
            aiEnabledBox.setSelected(loaded.isAiEnabled());
            aiKeyField.setText(loaded.getAiApiKey());
            aiModelField.setText(loaded.getAiModel());
            lightModeBox.setSelected(loaded.isLightTheme());
            Theme.setLight(loaded.isLightTheme());
            applyBackground();
            autoMemoryRadio.setSelected(loaded.isAutoMemory());
            customMemoryRadio.setSelected(!loaded.isAutoMemory());
            languageCombo.setValue("en_US".equals(loaded.getUiLanguage())
                    ? "English（预览，未完成）" : "简体中文");
            config.setWindowWidth(loaded.getWindowWidth());
            config.setWindowHeight(loaded.getWindowHeight());
            config.setJavaPath(loaded.getJavaPath());
            config.setAutoSelectJava(loaded.isAutoSelectJava());
            config.setGameDir(loaded.getGameDir());
            config.setJvmArgs(loaded.getJvmArgs());
            config.setDownloadSource(loaded.getDownloadSource());
            config.setMaxConcurrent(loaded.getMaxConcurrent());
            config.setSpeedLimitKb(loaded.getSpeedLimitKb());
            config.setAssetSampleCount(loaded.getAssetSampleCount());
            config.setIsolation(loaded.isIsolation());
            config.setAutoInstallDeps(loaded.isAutoInstallDeps());
            config.setAutoRepairLibraries(loaded.isAutoRepairLibraries());
            config.setHardwareAcceleration(loaded.isHardwareAcceleration());
            config.setAnimations(loaded.isAnimations());
            config.setAnimationSpeed(loaded.getAnimationSpeed());
            config.save();
            savedHint.setText("已导入，重新打开设置可看到新值");
            Toast.ok("设置已导入");
        } catch (Exception ex) {
            savedHint.setText("导入失败: " + ex.getMessage());
        }
    }

    // ================== 小工具 ==================

    private void reloadJavaList() {
        javaByLabel.clear();
        javaListBox.getChildren().clear();
        String dir = gameDirField.getText() == null || gameDirField.getText().isBlank()
                ? config.getGameDir() : gameDirField.getText().trim();
        List<JavaScanner.JavaInfo> list = JavaScanner.scan(new File(dir), true);
        for (JavaScanner.JavaInfo info : list) {
            String label = info.label();
            javaByLabel.put(label, info);
            javaListBox.getChildren().add(javaRow(label, info));
        }
        if (list.isEmpty()) {
            Label empty = new Label("（没检测到已安装的 Java，将自动下载 Zulu）");
            empty.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 12px;");
            empty.setPadding(new Insets(6, 8, 6, 8));
            javaListBox.getChildren().add(empty);
        }
    }

    /** 一行 Java：点击即选中（不嵌套 ListView，避免抢走页面滚轮）。 */
    private HBox javaRow(String label, JavaScanner.JavaInfo info) {
        boolean selected = label.equals(selectedJavaLabel);
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(6, 8, 6, 8));
        row.setCursor(Cursor.HAND);
        row.setStyle("-fx-background-radius: 7; -fx-background-color: " + (selected ? "-app-panel-2" : "transparent") + ";");
        Label text = new Label(label);
        text.setStyle("-fx-text-fill: " + (selected ? "#ffffff" : "-app-text-2") + "; -fx-font-size: 12px;");
        row.getChildren().add(text);
        row.setOnMouseEntered(e -> {
            if (!label.equals(selectedJavaLabel)) {
                row.setStyle("-fx-background-radius: 7; -fx-background-color: rgba(255,255,255,0.04);");
            }
        });
        row.setOnMouseExited(e -> {
            if (!label.equals(selectedJavaLabel)) {
                row.setStyle("-fx-background-radius: 7; -fx-background-color: transparent;");
            }
        });
        row.setOnMouseClicked(e -> {
            selectedJavaLabel = label;
            javaPathField.setText(info.exe().getAbsolutePath());
            autoJava.setSelected(false);
            for (Node node : javaListBox.getChildren()) {
                if (!(node instanceof HBox other) || other.getChildren().isEmpty()) continue;
                boolean isSelected = other == row;
                other.setStyle("-fx-background-radius: 7; -fx-background-color: "
                        + (isSelected ? "-app-panel-2" : "transparent") + ";");
                other.getChildren().get(0).setStyle("-fx-text-fill: "
                        + (isSelected ? "#ffffff" : "-app-text-2") + "; -fx-font-size: 12px;");
            }
        });
        return row;
    }

    private void browseJava() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("选择 java.exe");
        File current = new File(javaPathField.getText());
        if (current.getParentFile() != null && current.getParentFile().isDirectory()) {
            chooser.setInitialDirectory(current.getParentFile());
        }
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Java", "java.exe", "javaw.exe", "*.*"));
        File chosen = chooser.showOpenDialog(owner());
        if (chosen != null) {
            javaPathField.setText(chosen.getAbsolutePath());
            autoJava.setSelected(false);
        }
    }

    private void browseGameDir() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("选择游戏目录");
        File current = new File(gameDirField.getText());
        if (current.isDirectory()) chooser.setInitialDirectory(current);
        File chosen = chooser.showDialog(owner());
        if (chosen != null) gameDirField.setText(chosen.getAbsolutePath());
    }

    private Button browseButton(String text, Runnable action) {
        Button button = smallButton(text);
        button.setOnAction(e -> action.run());
        return button;
    }

    private void openDir(File dir) {
        try {
            if (!dir.isDirectory() && !dir.mkdirs()) return;
            java.awt.Desktop.getDesktop().open(dir);
        } catch (Exception e) {
            Toast.warn("打开目录失败: " + e.getMessage());
        }
    }

    private void openBrowser(String url) {
        try {
            java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
        } catch (Exception e) {
            Toast.warn("打开浏览器失败");
        }
    }

    /** 重新拉起自己：硬件加速是启动期属性，改完必须重启进程才生效。 */
    private void restartLauncher() {
        File restartLog = new File(System.getProperty("java.io.tmpdir"), "mc-launcher-restart.log");
        try {
            String java = ProcessHandle.current().info().command().orElse("");
            if (java.isBlank()) {
                java = new File(System.getProperty("java.home"), "bin/javaw.exe").getAbsolutePath();
            }
            File javaw = new File(new File(java).getParentFile(), "javaw.exe");
            if (javaw.isFile()) java = javaw.getAbsolutePath();

            String classPath = System.getProperty("java.class.path");
            List<String> command = new ArrayList<>();
            command.add(java);
            if (classPath != null && classPath.toLowerCase().endsWith(".jar") && !classPath.contains(";")) {
                command.add("-jar");
                command.add(new File(classPath).getAbsolutePath());
            } else {
                command.add("-cp");
                command.add(classPath);
                command.add("com.example.demo.Launcher");
            }
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.directory(new File(System.getProperty("user.dir")));
            builder.redirectOutput(ProcessBuilder.Redirect.appendTo(restartLog));
            builder.redirectErrorStream(true);
            builder.start();
            Platform.exit();
            System.exit(0);
        } catch (Exception e) {
            savedHint.setText("重启失败: " + e.getMessage());
        }
    }

    private javafx.stage.Window owner() {
        return getScene() == null ? null : getScene().getWindow();
    }

    private VBox card(Node... children) {
        VBox box = new VBox(13);
        box.setPadding(new Insets(20));
        box.setStyle("-fx-background-color: linear-gradient(to bottom, -app-card-top, -app-card-alt2);"
                + " -fx-background-radius: 18; -fx-border-color: -app-border-soft; -fx-border-radius: 18;"
                + " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.28), 22, 0.12, 0, 7);");
        box.getChildren().addAll(children);
        box.setMaxWidth(780);
        return box;
    }

    private ScrollPane scroll(Node contentNode) {
        VBox wrap = new VBox(14, contentNode);
        wrap.setPadding(new Insets(18, 22, 18, 22));
        wrap.setAlignment(Pos.TOP_CENTER);
        ScrollPane scroll = new ScrollPane(wrap);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        return scroll;
    }

    private Spinner<Integer> spinner(int min, int max, int value, int step, int width) {
        Spinner<Integer> spinner = new Spinner<>();
        spinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(min, max, value, step));
        spinner.setEditable(true);
        spinner.setPrefWidth(width);
        return spinner;
    }

    private static CheckBox check(String text, boolean value) {
        CheckBox box = new CheckBox(text);
        box.setSelected(value);
        box.setStyle("-fx-text-fill: -app-text-2; -fx-font-size: 13px;");
        return box;
    }

    private static Label section(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 12px; -fx-font-weight: bold;");
        label.setPadding(new Insets(6, 0, 0, 0));
        return label;
    }

    private static Label hint(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11px;");
        return label;
    }

    private static VBox labeled(String labelText, Node control) {
        Label label = new Label(labelText);
        label.setStyle("-fx-text-fill: -app-text-dim; -fx-font-size: 13px;");
        return new VBox(6, label, control);
    }

    private static HBox inline(String labelText, Node control, String hintText) {
        Label label = new Label(labelText);
        label.setStyle("-fx-text-fill: -app-text-dim; -fx-font-size: 13px;");
        HBox row = new HBox(8, label, control, hint(hintText));
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private static HBox infoRow(String key, String value) {
        Label k = new Label(key);
        k.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 12px;");
        k.setMinWidth(80);
        Label v = new Label(value == null ? "" : value);
        v.setStyle("-fx-text-fill: -app-text-2; -fx-font-size: 12px;");
        v.setWrapText(true);
        HBox row = new HBox(12, k, v);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private static void commitSpinner(Spinner<Integer> spinner) {
        try {
            int typed = Integer.parseInt(spinner.getEditor().getText().trim());
            spinner.getValueFactory().setValue(typed);
        } catch (Exception ignored) {
        }
    }

    private static String speedLabel(double speed) {
        if (speed < 0.85) return "慢";
        if (speed > 1.2) return "快";
        return "标准";
    }

    private static double speedValue(String label) {
        if ("慢".equals(label)) return 0.7;
        if ("快".equals(label)) return 1.5;
        return 1.0;
    }

    private Button smallButton(String text) {
        return smallButton(text, null);
    }

    private Button smallButton(String text, javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        Button button = new Button(text);
        button.setStyle("-fx-background-color: -app-border-4; -fx-text-fill: -app-text-2; -fx-cursor: hand;"
                + " -fx-background-radius: 8; -fx-padding: 8 16; -fx-font-size: 12px;");
        button.setMinWidth(Region.USE_PREF_SIZE);
        Motion.press(button);
        if (handler != null) button.setOnAction(handler);
        return button;
    }
}
