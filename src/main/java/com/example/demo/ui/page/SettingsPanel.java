package com.example.demo.ui.page;

import com.example.demo.core.i18n.I18n;

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

import com.example.demo.core.model.InstanceDirs;
import com.example.demo.core.ai.DeepSeekClient;
import com.example.demo.core.app.AppVersion;
import com.example.demo.core.app.LauncherConfig;
import com.example.demo.core.app.SupportBundle;
import com.example.demo.core.app.UpdateChecker;
import com.example.demo.core.auth.SecretStore;
import com.example.demo.core.game.JavaScanner;
import com.example.demo.core.runtime.HardwareInfo;
import com.example.demo.core.runtime.MemoryAdvisor;
import com.example.demo.core.runtime.PerformanceTuner;
import com.example.demo.core.app.ConfigHistory;
import com.example.demo.ui.component.PortableServerDialog;
import com.example.demo.ui.shell.Icons;
import com.example.demo.ui.shell.ModuleHeader;
import com.example.demo.ui.shell.Motion;
import com.example.demo.ui.shell.Theme;
import com.example.demo.ui.shell.Toast;
import com.example.demo.core.model.InstanceSettings;
/**
 * 设置页：学习 PCL-CE 的「左侧选项卡 + 右侧内容」结构。
 * 页签：启动 / Java / 管理 / 个性化 / 杂项 / 软件信息。
 */
public final class SettingsPanel extends VBox {

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
    private final CheckBox keyHintsBox;
    private final CheckBox autoRepairBox;
    private volatile boolean updateBusy;
    private final RadioButton autoMemoryRadio;
    private final RadioButton customMemoryRadio;
    private final Slider memorySlider;
    private Label memoryNowLabel;
    private HBox memoryScale;
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
    private final Slider sheenSlider;
    private final CheckBox perfOverlayBox;
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
        String savedKey = config.getAiApiKey();
        if (savedKey.isBlank()) {
            aiKeyField.setPromptText("sk-...");
        } else {
            // 已经存过 Key 就只显示尾部，留空表示不修改
            String tail = savedKey.length() <= 4 ? "****" : savedKey.substring(savedKey.length() - 4);
            aiKeyField.setPromptText("已保存（…" + tail + "），留空保持不变");
        }
        aiKeyField.setText(savedKey);
        aiModelField = new TextField(config.getAiModel());
        aiModelField.setPromptText("deepseek-chat");
        aiStatus = new Label(
                savedKey.isBlank()
                        ? "还没有 Key：当前使用本地免费助手。填好 Key 后点「测试连接」，再点「保存」。"
                        : "Key 已保存在本机注册表（不写入 config.json，也不会随设置导出）。");
        aiStatus.setWrapText(true);
        aiStatus.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11px;");

        // ---------- 左：页签 ----------
        VBox side = new VBox(2);
        javafx.beans.binding.DoubleBinding sideWidth =
                javafx.beans.binding.Bindings.when(widthProperty().lessThan(1120)).then(168.0).otherwise(190.0);
        side.prefWidthProperty().bind(sideWidth);
        side.minWidthProperty().bind(sideWidth);
        side.maxWidthProperty().bind(sideWidth);
        side.setPadding(new Insets(14, 8, 12, 8));
        side.setStyle("-fx-background-color: -app-nav;"
                + " -fx-border-color: transparent -app-border-5 transparent transparent;");

        TextField tabSearch = new TextField();
        tabSearch.setPromptText("搜索设置…");
        tabSearch.setStyle("-fx-background-color: -app-row3; -fx-text-fill: -app-text;"
                + " -fx-prompt-text-fill: -app-text-muted; -fx-background-radius: 8;"
                + " -fx-border-color: -app-border-soft; -fx-border-radius: 8; -fx-padding: 5 9;"
                + " -fx-font-size: 11px;");
        tabSearch.textProperty().addListener((o, old, text) -> filterSettings(text));

        side.getChildren().addAll(
                tabSearch,
                sectionLabel(I18n.t("settings.group.game", "游戏")),
                tabItem("launch"),
                tabItem("java"),
                tabItem("manage"),
                sectionLabel(I18n.t("settings.group.tools", "工具")),
                tabItem("lan"),
                tabItem("ai"),
                sectionLabel(I18n.t("settings.group.launcher", "启动器")),
                tabItem("ui"),
                tabItem("language"),
                tabItem("misc"),
                sectionLabel(I18n.t("settings.group.about", "关于")),
                tabItem("about"),
                tabItem("update"),
                tabItem("feedback"),
                tabItem("logs"));

        // ---------- 控件先建好，页签复用 ----------
        memorySpinner = spinner(512, 16384, config.getMemoryMb(), 512, 130);
        widthSpinner = spinner(320, 3840, config.getWindowWidth(), 10, 100);
        heightSpinner = spinner(240, 2160, config.getWindowHeight(), 10, 100);

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

        Label memoryMin = new Label("512 MB");
        memoryMin.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 10.5px;");
        Label memoryNow = new Label();
        memoryNow.setStyle("-fx-text-fill: -app-text-strong; -fx-font-size: 11.5px; -fx-font-weight: bold;");
        Label memoryMax = new Label("16384 MB");
        memoryMax.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 10.5px;");
        Region sliderSpacer = new Region();
        HBox.setHgrow(sliderSpacer, Priority.ALWAYS);
        Region sliderSpacerRight = new Region();
        HBox.setHgrow(sliderSpacerRight, Priority.ALWAYS);
        this.memoryScale = new HBox(8.0, memoryMin, sliderSpacer, memoryNow, sliderSpacerRight, memoryMax);
        this.memoryScale.setAlignment(Pos.CENTER_LEFT);
        memoryNow.setText((int) memorySlider.getValue() + " MB");
        memoryNowLabel = memoryNow;

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
            if (memoryNowLabel != null) memoryNowLabel.setText(value + " MB");
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
        languageCombo.getItems().addAll("简体中文", "English");
        languageCombo.setValue("en_US".equals(config.getUiLanguage())
                ? "English" : "简体中文");
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
        autoDepsBox = check("启动时自动补装缺失的前置依赖（从 Modrinth 下载）", config.isAutoInstallDeps());
        keyHintsBox = check("体检面板显示键盘提示（Esc/Tab/Enter）", config.isShowKeyHints());
        autoRepairBox = check("启动前自动补全缺失的库文件", config.isAutoRepairLibraries());
        hardwareAccelBox = check("启用硬件加速（显卡渲染；关闭后需重启启动器）", config.isHardwareAcceleration());
        animationsBox = check("界面动画效果（关闭 = 性能模式）", config.isAnimations());

        animationSpeedCombo = new ComboBox<>(FXCollections.observableArrayList("慢", "标准", "快"));
        animationSpeedCombo.setValue(speedLabel(config.getAnimationSpeed()));
        animationSpeedCombo.setPrefWidth(100);

        opacitySlider = new Slider(0.7, 1.0, config.getUiOpacity());
        opacitySlider.setPrefWidth(220);
        sheenSlider = new Slider(0.0, 1.5, config.getSheenStrength());
        sheenSlider.setPrefWidth(220);
        sheenSlider.setBlockIncrement(0.1);
        perfOverlayBox = new CheckBox(I18n.t("settings.perfOverlay", "显示性能信息（FPS / 内存 / 线程）"));
        perfOverlayBox.setSelected(config.isShowPerfOverlay());

        selectedJavaLabel = null;

        // ---------- 页签内容 ----------
        tabPages.put("launch", buildLaunchTab());
        tabPages.put("java", buildJavaTab());
        tabPages.put("manage", buildManageTab());
        tabPages.put("ui", buildUiTab());
        tabPages.put("misc", buildMiscTab());
        tabPages.put("about", buildAboutTab());
        tabPages.put("lan", buildLanTab());
        tabPages.put("language", buildLanguageTab());
        tabPages.put("ai", buildAiTab());
        tabPages.put("update", buildUpdateTab());
        tabPages.put("feedback", buildFeedbackTab());
        tabPages.put("logs", buildLogTab());

        content.setStyle("-fx-background-color: transparent;");
        HBox.setHgrow(content, Priority.ALWAYS);
        VBox.setVgrow(content, Priority.ALWAYS);

        // ---------- 底部保存栏 ----------
        savedHint.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 12px;");
        Button importConfig = smallButton(I18n.t("action.importSettings", "导入设置"));
        importConfig.setOnAction(e -> importConfig());
        Button exportConfig = smallButton(I18n.t("action.exportSettings", "导出设置"));
        exportConfig.setOnAction(e -> exportConfig());
        Button history = smallButton(I18n.t("action.configHistory", "配置历史"));
        history.setOnAction(e -> showConfigHistory());
        Button reset = smallButton(I18n.t("action.resetDefaults", "恢复默认"));
        reset.setOnAction(e -> resetDefaults());
        Button save = new Button(I18n.t("action.save", "保存"));
        save.getStyleClass().add("btn-primary");
        save.setStyle("-fx-padding: 9 28;");
        Motion.press(save);
        save.setOnAction(e -> saveAll());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox bottom = new HBox(10, savedHint, spacer, importConfig, exportConfig, history, reset, save);
        bottom.setAlignment(Pos.CENTER_LEFT);
        bottom.setPadding(new Insets(12, 22, 14, 22));
        bottom.setStyle("-fx-background-color: -app-nav;"
                + " -fx-border-color: -app-border-5 transparent transparent transparent;");

        ModuleHeader settingsHeader = new ModuleHeader("03 / PREFERENCES", I18n.t("nav.settings", "设置"),
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
        selectTab("launch");
    }

    // ================== 页签 ==================

    private Label sectionLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: -app-text-dim-3; -fx-font-size: 11px; -fx-font-weight: bold;");
        label.setPadding(new Insets(12, 10, 4, 12));
        return label;
    }

    /** 每个分栏的符号，和下载页侧栏保持同一套图标语言。 */
    private static String tabIcon(String key) {
        return switch (key) {
            case "launch" -> "play";
            case "java" -> "package";
            case "manage" -> "tools";
            case "lan" -> "server";
            case "ai" -> "sparkles";
            case "ui" -> "image";
            case "language" -> "globe";
            case "misc" -> "layers";
            case "about" -> "folder";
            case "update" -> "refresh";
            case "feedback" -> "edit";
            case "logs" -> "terminal";
            default -> "dot";
        };
    }

    /** 页签 key → 中文标签；英文走语言包，中文作为兜底。 */
    private static final String[][] TAB_LABELS = {
        {"launch", "启动"}, {"java", "Java"}, {"manage", "管理"}, {"lan", "联机"},
        {"ai", "AI"}, {"ui", "个性化"}, {"language", "语言"}, {"misc", "杂项"},
        {"about", "软件信息"}, {"update", "软件更新"}, {"feedback", "反馈"},
        {"logs", "查看日志"}
    };

    private static String tabLabel(String key) {
        for (String[] pair : TAB_LABELS) {
            if (pair[0].equals(key)) {
                return I18n.t("settings.tab." + key, pair[1]);
            }
        }
        return key;
    }

    /** 兼容老调用：允许传中文标签（F8 / 启动失败提示里用的就是中文）。 */
    private static String tabKey(String nameOrKey) {
        for (String[] pair : TAB_LABELS) {
            if (pair[0].equals(nameOrKey) || pair[1].equals(nameOrKey)) {
                return pair[0];
            }
        }
        return nameOrKey;
    }

    private HBox tabItem(String key) {
        HBox item = new HBox(8);
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

        javafx.scene.Node icon = com.example.demo.ui.shell.Icons.node(tabIcon(key), 13.0, "#9aa3ad");
        icon.setOpacity(0.72);
        icon.setMouseTransparent(true);

        Label text = new Label(tabLabel(key));
        text.setStyle("-fx-text-fill: -app-text-dim; -fx-font-size: 13px;");
        // 点击必须落在整行上：文字与色条不吞事件，否则只点中文字时选不中分栏
        text.setMouseTransparent(true);
        bar.setMouseTransparent(true);
        item.getChildren().addAll(bar, icon, text);
        item.setOnMouseClicked(e -> selectTab(key));
        item.setOnMouseEntered(
                e -> {
                    scaleIcon(icon, 1.18);
                    if (!key.equals(currentTab)) {
                        // 反馈：未选中的分栏悬停时给一点底色，暗示可点
                        item.setStyle("-fx-background-color: -app-hover-2;"
                                + " -fx-background-radius: 8;");
                    }
                });
        item.setOnMouseExited(
                e -> {
                    scaleIcon(icon, 1.0);
                    if (!key.equals(currentTab)) {
                        item.setStyle("");
                    }
                });
        // 记下图标，切换分栏时由 selectTab 提亮
        tabItems.put(key, item);
        return item;
    }

    private static void scaleIcon(javafx.scene.Node icon, double scale) {
        if (!com.example.demo.ui.shell.Motion.isEnabled()) {
            icon.setScaleX(scale);
            icon.setScaleY(scale);
            return;
        }
        javafx.animation.ScaleTransition transition =
                new javafx.animation.ScaleTransition(com.example.demo.ui.shell.Motion.FAST, icon);
        transition.setToX(scale);
        transition.setToY(scale);
        transition.setInterpolator(com.example.demo.ui.shell.Motion.OUT);
        transition.play();
    }

    private void filterSettings(String query) {
        String q = query == null ? "" : query.trim().toLowerCase(java.util.Locale.ROOT);
        java.util.Map<String, String> keywords = java.util.Map.ofEntries(
                java.util.Map.entry("launch", "内存 java 窗口 下载 网络 参数 游戏"),
                java.util.Map.entry("java", "java 路径 运行时 jdk"),
                java.util.Map.entry("manage", "实例 目录 隔离 版本"),
                java.util.Map.entry("lan", "服务器 联机 端口"),
                java.util.Map.entry("ai", "ai deepseek key 模型 助手"),
                java.util.Map.entry("ui", "主题 颜色 字体 背景 透明度 动画"),
                java.util.Map.entry("language", "中文 英文 语言"),
                java.util.Map.entry("misc", "缓存 日志 关闭 托盘"),
                java.util.Map.entry("about", "版本 关于"),
                java.util.Map.entry("update", "更新 镜像 gitee github"),
                java.util.Map.entry("feedback", "反馈 问题"),
                java.util.Map.entry("logs", "日志 log"));
        String first = null;
        for (java.util.Map.Entry<String, HBox> entry : tabItems.entrySet()) {
            String name = entry.getKey();
            String haystack = (tabLabel(name) + " " + keywords.getOrDefault(name, "")).toLowerCase(java.util.Locale.ROOT);
            boolean match = q.isBlank() || haystack.contains(q);
            entry.getValue().setVisible(match);
            entry.getValue().setManaged(match);
            if (match && first == null) first = name;
        }
        if (!q.isBlank() && first != null) {
            final String targetTab = first;
            selectTab(targetTab);
            javafx.application.Platform.runLater(() -> highlightMatch(targetTab, q));
        }
    }

    /** 搜索命中时把右侧页面滚到第一个匹配项，并短暂高亮，省得自己找。 */
    private void highlightMatch(String tabName, String query) {
        final javafx.scene.Node node = tabPages.get(tabName);
        final javafx.scene.control.ScrollPane page =
                node instanceof javafx.scene.control.ScrollPane scroll ? scroll : null;
        if (page == null || page.getContent() == null) {
            return;
        }
        String needle = query.toLowerCase(java.util.Locale.ROOT).trim();
        if (needle.isEmpty()) {
            return;
        }
        javafx.scene.Node hit = null;
        for (javafx.scene.Node candidate : page.getContent().lookupAll(".label")) {
            if (candidate instanceof javafx.scene.control.Label label
                    && label.getText() != null
                    && label.getText().toLowerCase(java.util.Locale.ROOT).contains(needle)) {
                hit = label;
                break;
            }
        }
        if (hit == null) {
            page.setVvalue(0);
            return;
        }
        javafx.scene.Node target = hit;
        double contentHeight = page.getContent().getBoundsInLocal().getHeight();
        double y = target.getBoundsInParent().getMinY();
        if (contentHeight > 1) {
            page.setVvalue(Math.max(0, Math.min(1, y / contentHeight)));
        }
        String original = target.getStyle();
        target.setStyle(original + "; -fx-background-color: rgba(20,184,166,0.22);"
                + " -fx-background-radius: 6; -fx-padding: 1 4;");
        javafx.animation.PauseTransition revert =
                new javafx.animation.PauseTransition(javafx.util.Duration.millis(1400));
        revert.setOnFinished(e -> target.setStyle(original));
        revert.play();
    }
    private void selectTab(String name) {
        currentTab = name;
        for (Map.Entry<String, HBox> entry : tabItems.entrySet()) {
            boolean active = entry.getKey().equals(name);
            HBox box = entry.getValue();
            box.getStyleClass().remove("app-tab-active");
            if (active) box.getStyleClass().add("app-tab-active");
            if (box.getChildren().size() >= 3) {
                box.getChildren().get(0).setStyle("-fx-fill: " + (active ? "#cbd5e1" : "transparent") + ";");
                javafx.scene.Node tabIcon = box.getChildren().get(1);
                tabIcon.setOpacity(active ? 1.0 : 0.72);
                box.getChildren().get(2).setStyle("-fx-text-fill: " + (active ? "-app-text-strong" : "-app-text-dim")
                        + "; -fx-font-size: 13px;" + (active ? " -fx-font-weight: bold;" : ""));
            }
        }
        // 选中分栏交给 CSS 的 app-tab-active，清掉悬停留下的内联样式
        Node activeItem = tabItems.get(name);
        if (activeItem != null) {
            activeItem.setStyle("");
        }
        Node page = tabPages.get(name);
        if (page == null) return;
        content.getChildren().setAll(page);
        Motion.pageIn(page, 8);
        // 卡片错落入场：切换分栏时每张卡延迟 30ms 淡入，别整页同时出现
        List<javafx.scene.Node> cards = new ArrayList<>(page.lookupAll(".settings-card"));
        if (!cards.isEmpty()) {
            Motion.stagger(cards);
        }
        if (page instanceof ScrollPane scroll) {
            Platform.runLater(() -> scroll.setVvalue(0));
        }
    }

    // ================== 各页签内容 ==================

    private ScrollPane buildLaunchTab() {
        VBox memoryBox = new VBox(6, memorySlider, memoryScale,
                new HBox(8, memorySpinner, hint("MB")));
        VBox card = card(
                section("启动设置"),
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
        return InstanceDirs.mods(game, id, InstanceSettings.effectiveIsolation(game, id, config.isIsolation()));
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
                labeled("Java 路径", new HBox(8, javaPathField, browseButton("浏览", this::browseJava),
                        smallButton(I18n.t("settings.java.detect", "自动检测"), e -> detectLocalJava()))));
        return scroll(card);
    }

    private VBox javaContainer() {
        VBox box = new VBox(2, javaListBox);
        box.setStyle("-fx-background-color: -app-panel-2; -fx-background-radius: 9;"
                + " -fx-border-color: -app-hover; -fx-border-radius: 9; -fx-padding: 6;");
        box.setMinHeight(130);
        return box;
    }

    /** 导出所有实例的启动设置到一个 zip。 */
    private void exportInstanceSettings() {
        javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
        chooser.setTitle(I18n.t("settings.instances.export", "导出实例设置"));
        chooser.setInitialFileName("neko-instance-settings.zip");
        chooser.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter("ZIP", "*.zip"));
        File target = chooser.showSaveDialog(getScene() == null ? null : getScene().getWindow());
        if (target == null) {
            return;
        }
        try {
            File gameDir = new File(config.getGameDir());
            var result = com.example.demo.core.model.InstanceSettingsBundle.exportAll(gameDir, target);
            Toast.ok(I18n.t("settings.instances.exported", "已导出 ") + result.exported()
                    + I18n.t("settings.instances.exportedTail", " 个实例的设置"));
            this.log.accept("🗂 导出实例设置：" + target.getAbsolutePath() + "（" + result.exported() + " 个）");
        } catch (Exception ex) {
            Toast.error(I18n.t("settings.instances.failed", "操作失败：") + ex.getMessage());
        }
    }

    /** 从 zip 还原实例设置（同名实例会被覆盖）。 */
    private void importInstanceSettings() {
        javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
        chooser.setTitle(I18n.t("settings.instances.import", "导入实例设置"));
        chooser.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter("ZIP", "*.zip"));
        File source = chooser.showOpenDialog(getScene() == null ? null : getScene().getWindow());
        if (source == null) {
            return;
        }
        try {
            File gameDir = new File(config.getGameDir());
            var result = com.example.demo.core.model.InstanceSettingsBundle.importAll(gameDir, source);
            Toast.ok(I18n.t("settings.instances.imported", "已还原 ") + result.exported()
                    + I18n.t("settings.instances.importedTail", " 个实例的设置"));
            if (result.skipped() > 0) {
                Toast.info(I18n.t("settings.instances.skipped", "跳过 ") + result.skipped()
                        + I18n.t("settings.instances.skippedTail", " 个（本机没有对应实例）"));
            }
            this.log.accept("🗂 导入实例设置：" + source.getName() + "（" + result.exported() + " 个）");
        } catch (Exception ex) {
            Toast.error(I18n.t("settings.instances.failed", "操作失败：") + ex.getMessage());
        }
    }
    private ScrollPane buildManageTab() {
        HBox downloadRow = new HBox(18,
                inline("最大并发", concurrentSpinner, "个连接"),
                inline("限速", speedSpinner, "KB/s，0=不限"));
        downloadRow.setAlignment(Pos.CENTER_LEFT);

        Button exportInstanceSettings = smallButton(
                I18n.t("settings.instances.export", "导出实例设置"));
        exportInstanceSettings.setOnAction(e -> exportInstanceSettings());
        Button importInstanceSettings = smallButton(
                I18n.t("settings.instances.import", "导入实例设置"));
        importInstanceSettings.setOnAction(e -> importInstanceSettings());

        VBox card = card(
                section(I18n.t("settings.instances.section", "实例设置包")),
                new HBox(10, exportInstanceSettings, importInstanceSettings),
                hint(I18n.t("settings.instances.hint",
                        "只打包各实例的 instance.json（内存/JVM/Java/游戏参数/隔离），不含模组与世界")),
                section("下载"),
                labeled("下载源", new HBox(10, sourceCombo, hint("auto=镜像优先失败回官方；official/mirror=只用其一"))),
                labeled("下载策略", downloadRow),
                section("模组"),
                autoDepsBox,
                keyHintsBox,
                autoRepairBox,
                labeled("完整性抽样", new HBox(10, assetSampleSpinner, hint("每个实例抽样校验的 assets 对象数，0 = 不抽样"))),
                hint("自动补装会从 Modrinth 按当前实例的版本与加载器挑选，缺前置时先弹窗确认"));
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
                selectTab("ui");
            });
            themeRow.getChildren().add(swatch);
        }

        Button chooseBg = smallButton("选择背景图片", e -> chooseBackground());
        Button clearBg = smallButton("清除背景", e -> {
            config.setBackgroundImage("");
            config.save();
            applyBackground();
            Toast.ok("已清除启动页背景");
            selectTab("ui");
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
                labeled(I18n.t("settings.sheen.label", "鼠标柔光"), new HBox(10, sheenSlider,
                        hint(I18n.t("settings.sheen.hint", "跟随指针的氛围光强度，0 = 关闭，保存后立即生效")))),
                perfOverlayBox,
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
        selectTab("ui");
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
    public void openTab(String nameOrKey) {
        String key = tabKey(nameOrKey);
        if (tabPages.containsKey(key)) selectTab(key);
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
        Button portable = smallButton("打开便携服务器页", e -> {
            // 旧版在这里弹窗，建完就没下文（没有日志、没有记录）；现在统一进独立页面
            com.example.demo.ui.shell.Navigation.go("server");
        });
        VBox card = card(
                section("局域网联机"),
                labeled("本机地址", new HBox(12, address, copy)),
                new HBox(10, portable, hint("生成真正能启动的服务端：进度、日志、历史记录都在那一页")),
                hint("进入游戏后按 Esc → 对局域网开放，其他玩家用上面的地址直连（同一局域网）。"),
                hint("PCL CE 的 Natayark 大厅联机需要账号与官方服务，本启动器暂未接入；这里只提供局域网直连信息。"),
                section("快速步骤"),
                hint("1. 启动游戏并进入存档\n2. 菜单 → 对局域网开放\n3. 把 本机地址 + 端口 发给朋友\n4. 朋友：多人游戏 → 直接连接"),
                hint("如果连不上，检查 Windows 防火墙是否放行 Java。"));
        return scroll(card);
    }

    private ScrollPane buildLanguageTab() {
        VBox card = card(
                section(I18n.t("settings.language.title", "界面语言")),
                labeled(I18n.t("settings.language.label", "语言"), languageCombo),
                hint(I18n.t("settings.language.hint",
                        "切换后顶栏与通用按钮会跟随语言变化，页面正文仍在逐页翻译中；保存后重启生效。")));
        return scroll(card);
    }

    private ScrollPane buildAiTab() {
        Label title = new Label("DeepSeek AI 助手");
        title.setStyle("-fx-text-fill: -app-text; -fx-font-size: 17px; -fx-font-weight: bold;");
        Label intro = new Label("在启动页底部直接提问：配置 Key 后走 DeepSeek，未配置时使用本地免费助手。");
        intro.setWrapText(true);
        intro.setStyle("-fx-text-fill: -app-text-dim; -fx-font-size: 11.5px;");
        boolean hasKey = !config.getAiApiKey().isBlank();
        Label badge = new Label(hasKey ? "在线模型" : "本地助手");
        badge.setStyle("-fx-text-fill: " + (hasKey ? "#06251f" : "-app-text-2") + ";"
                + " -fx-background-color: " + (hasKey ? "-app-accent" : "-app-row3") + ";"
                + " -fx-background-radius: 20; -fx-padding: 4 10; -fx-font-size: 10px;"
                + " -fx-font-weight: bold;");
        VBox heroText = new VBox(3, title, intro);
        HBox.setHgrow(heroText, Priority.ALWAYS);
        HBox hero = new HBox(12, new StackPane(Icons.node("sparkles", 22, "#5eead4")), heroText, badge);
        hero.setAlignment(Pos.CENTER_LEFT);
        hero.setPadding(new Insets(12));
        hero.setStyle("-fx-background-color: -app-row2; -fx-background-radius: 12;"
                + " -fx-border-color: -app-border-soft; -fx-border-radius: 12;");

        Button test = smallButton("测试连接", e -> testAi());
        Button openKeys = smallButton("获取 API Key", e -> openBrowser("https://platform.deepseek.com/api_keys"));
        Button webChat = smallButton("打开网页版", e -> openBrowser("https://chat.deepseek.com"));
        Button clearKey = smallButton("清除 Key", e -> {
            config.setAiApiKey("");
            config.save();
            aiKeyField.clear();
            aiKeyField.setPromptText("sk-...");
            aiStatus.setText("已清除本机保存的 Key，AI 回到本地免费助手模式。");
            aiStatus.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11px;");
        });
        HBox actions = new HBox(8, test, openKeys, webChat, clearKey);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox card = card(
                hero,
                section("工作模式"),
                aiEnabledBox,
                hint("关闭后启动页命令栏恢复为启动器 / 游戏命令；开启且未填 Key 时使用本地免费助手。"),
                section("模型配置"),
                labeled("API Key", aiKeyField),
                labeled("模型", aiModelField),
                section("测试与入口"),
                actions,
                aiStatus,
                hint("Key 存储位置：" + SecretStore.location() + "，不会写入 config.json 或启动器日志。"));
        return scroll(card);
    }

    private void testAi() {
        String key = aiKeyField.getText() == null ? "" : aiKeyField.getText().trim();
        if (key.isBlank()) {
            key = config.getAiApiKey();
        }
        final String testKey = key;
        if (key.isBlank()) {
            aiStatus.setText("未填写 Key：本地免费助手可用，不需要联网。");
            aiStatus.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11px;");
            return;
        }
        aiStatus.setText("正在测试 DeepSeek 连接…");
        CompletableFuture.supplyAsync(() -> {
            try {
                return DeepSeekClient.chat(testKey, config.getAiBaseUrl(), aiModelField.getText(),
                        List.of(new DeepSeekClient.Message("user", "只回复两个字：可用")));
            } catch (Exception ex) {
                throw new java.util.concurrent.CompletionException(ex);
            }
        }).thenAccept(reply -> Platform.runLater(() -> {
            // 测试通过说明 Key 有效，直接落盘，省得还要再点一次「保存」
            config.setAiApiKey(testKey);
            config.save();
            aiStatus.setText("连接成功并已保存 Key，模型回复：" + reply);
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
        // 安装版走 setup.exe（jpackage /quiet）；免安装版走 win64.zip 就地覆盖
        java.io.File launcher = com.example.demo.core.app.UpdateInstaller.currentLauncherExe();
        boolean installed = com.example.demo.core.app.UpdateInstaller.isInstalledLayout(launcher);
        String setupUrl = info.url() == null ? "" : info.url().trim();
        String zipUrl = info.portableUrl() == null ? "" : info.portableUrl().trim();
        String url = installed || zipUrl.isBlank() ? setupUrl : zipUrl;
        boolean useZip = !installed && !zipUrl.isBlank();
        if (!url.startsWith("https://") || !(url.toLowerCase(java.util.Locale.ROOT).contains(".exe")
                || url.toLowerCase(java.util.Locale.ROOT).contains(".zip"))) {
            openLink(url.isBlank() ? setupUrl : url);
            return;
        }
        String safeVersion = info.version().replaceAll("[^0-9A-Za-z._-]", "_");
        String fileName = (useZip ? "NekoCraftLauncher-" + safeVersion + "-win64.zip"
                : "NekoCraftLauncher-Setup-" + safeVersion + ".exe");
        java.nio.file.Path target = java.nio.file.Paths.get(System.getProperty("java.io.tmpdir"), fileName);
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
        }).thenAccept(v -> {
            // 装之前先校验：清单来自同一个 Release，哈希对不上就删包重下，别把半截包装进去
            String expected = null;
            String sums = com.example.demo.core.app.UpdateChecker.fetchChecksums(info.checksumsUrl());
            if (!sums.isBlank()) {
                expected = com.example.demo.core.app.UpdateChecker
                        .expectedSha256(sums, target.getFileName().toString());
            }
            if (expected != null) {
                try {
                    String actual = com.example.demo.core.app.UpdateChecker.sha256(target);
                    if (!expected.equalsIgnoreCase(actual)) {
                        java.nio.file.Files.deleteIfExists(target);
                        final String detail = I18n.t("update.checksum.mismatch", "更新包校验失败（哈希不一致），已删除，请重试")
                                + " expected=" + expected + " actual=" + actual;
                        Platform.runLater(() -> {
                            updateBusy = false;
                            button.setDisable(false);
                            status.setText(detail);
                            status.setStyle("-fx-text-fill: #f87171; -fx-font-size: 11px;");
                        });
                        return;
                    }
                } catch (Exception hashError) {
                    // 算不出来就继续，不因为校验工具问题挡住更新
                }
            }
            Platform.runLater(() -> {
            // 保持按钮禁用，避免下载完成到安装启动之间被再次点击
            status.setText("下载完成，正在自动安装 " + info.version() + "…");
            status.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 11px;");
            Toast.ok("更新已下载，正在自动安装");
            // 安装程序会被 UAC / 杀软拦，先把路径说清楚：装不上也知道去哪找
            log.accept(I18n.t("update.downloaded.log", "⬆ 更新包已下载：") + target
                    + I18n.t("update.downloaded.logTail", "（等待确认后启动安装程序）"));
            // 不静默退出：用户反馈过「等了 5 分钟什么也没提示」，所以要显式告知并让他决定
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.INFORMATION);
            alert.initOwner(owner());
            alert.setTitle(I18n.t("update.downloaded.title", "更新已下载"));
            alert.setHeaderText(I18n.t("update.downloaded.header", "Neko Craft Launcher ") + info.version() + I18n.t("update.downloaded.headerTail", " 已下载完成"));
            alert.setContentText(I18n.t("update.downloaded.body", "点「立即安装」会启动安装程序并关闭启动器。\n")
                    + I18n.t("update.downloaded.path", "安装包位置：") + target);
            javafx.scene.control.ButtonType installNow =
                    new javafx.scene.control.ButtonType(I18n.t("update.downloaded.now", "立即安装"),
                            javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
            javafx.scene.control.ButtonType later =
                    new javafx.scene.control.ButtonType(I18n.t("update.downloaded.later", "稍后自己装"));
            alert.getButtonTypes().setAll(installNow, later);
            javafx.scene.control.ButtonType choice = alert.showAndWait().orElse(later);
            if (choice != installNow) {
                updateBusy = false;
                button.setDisable(false);
                status.setText(I18n.t("update.downloaded.kept", "更新包已下载到 ") + target + I18n.t("update.downloaded.keptTail", "，随时可以手动安装。"));
                try {
                    java.awt.Desktop.getDesktop().open(target.getParent().toFile());
                } catch (Exception ignored) {
                    // 打不开目录就算了，路径已经写在状态里
                }
                return;
            }
            try {
                // 免安装版：解压覆盖 app / runtime / exe；game 与 config.json 不在 zip 里，不会被碰
                if (useZip) {
                    java.io.File root = com.example.demo.core.app.UpdateInstaller.portableRoot(launcher);
                    java.io.File relaunch = launcher;
                    com.example.demo.core.app.UpdateInstaller.writePending(info.version(),
                            target.toFile());
                    java.io.File zipScript = com.example.demo.core.app.UpdateInstaller
                            .preparePortableScript(target.toFile(), root, relaunch);
                    com.example.demo.core.app.UpdateInstaller.runPowerShellDetached(zipScript);
                    status.setText(I18n.t("update.downloaded.starting2", "安装程序已启动，启动器即将关闭…装好会自动重新打开"));
                    Thread exitThread = new Thread(() -> {
                        try {
                            Thread.sleep(800);
                        } catch (InterruptedException ignored) {
                        }
                        Platform.runLater(() -> {
                            Platform.exit();
                            System.exit(0);
                        });
                    }, "neko-update-shutdown-portable");
                    exitThread.setDaemon(true);
                    exitThread.start();
                    return;
                }
                // 安装版：jpackage 产出的 exe 静默参数是 /quiet。
                // 先摆好助手脚本（等本进程退出 → 静默安装 → 重新拉起），再退出自己。
                java.io.File stashed = com.example.demo.core.app.UpdateInstaller
                        .stashInstaller(target.toFile(), info.version());
                java.io.File relaunch = com.example.demo.core.app.UpdateInstaller.currentLauncherExe();
                com.example.demo.core.app.UpdateInstaller.writePending(info.version(), stashed);
                java.io.File script = com.example.demo.core.app.UpdateInstaller
                        .prepareApplyScript(stashed, relaunch);
                com.example.demo.core.app.UpdateInstaller.runDetached(script);
                status.setText(I18n.t("update.downloaded.starting2", "安装程序已启动，启动器即将关闭…装好会自动重新打开"));
                Thread shutdown = new Thread(() -> {
                    try {
                        Thread.sleep(800);
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
            });
        }).exceptionally(ex -> {
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

    private void showConfigHistory() {
        javafx.scene.control.Dialog<Void> dialog = new javafx.scene.control.Dialog<>();
        dialog.initOwner(owner());
        dialog.setTitle("配置历史");
        dialog.setHeaderText("选择一份历史配置恢复，重启启动器后生效");
        ListView<File> list = new ListView<>();
        list.getItems().setAll(ConfigHistory.list());
        list.setCellFactory(v -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(File file, boolean empty) {
                super.updateItem(file, empty);
                if (empty || file == null) {
                    setText(null);
                    return;
                }
                setText(file.getName() + "    "
                        + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                        .format(new java.util.Date(file.lastModified())));
            }
        });
        Button restore = new Button("恢复选中");
        restore.setOnAction(e -> {
            File selected = list.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            javafx.scene.control.Alert confirm = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.CONFIRMATION,
                    "用这份历史覆盖当前 config.json？当前配置会先自动备份。",
                    javafx.scene.control.ButtonType.OK, javafx.scene.control.ButtonType.CANCEL);
            confirm.initOwner(owner());
            if (confirm.showAndWait().orElse(javafx.scene.control.ButtonType.CANCEL)
                    != javafx.scene.control.ButtonType.OK) return;
            try {
                ConfigHistory.restore(selected, LauncherConfig.file());
                Toast.ok("配置已恢复，重启启动器后生效");
                dialog.close();
            } catch (Exception ex) {
                Toast.error("恢复失败：" + ex.getMessage());
            }
        });
        VBox content = new VBox(10, list, restore);
        VBox.setVgrow(list, Priority.ALWAYS);
        content.setPrefSize(560, 380);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(javafx.scene.control.ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private ScrollPane buildUpdateTab() {
        Label version = new Label(I18n.t("settings.version", "当前版本：Neko Craft Launcher ") + AppVersion.CURRENT);
        version.setStyle("-fx-text-fill: -app-text; -fx-font-size: 13px;");

        javafx.scene.control.TextField urlField = new javafx.scene.control.TextField(config.getUpdateUrl());
        urlField.setPromptText("https://api.github.com/repos/用户/NekoCraftLauncher/releases/latest");
        HBox.setHgrow(urlField, Priority.ALWAYS);
        javafx.scene.control.TextField mirrorField =
                new javafx.scene.control.TextField(config.getUpdateMirrorUrl());
        mirrorField.setPromptText("备用更新源 / Gitee / 自建 manifest（可选）");
        HBox.setHgrow(mirrorField, Priority.ALWAYS);

        Button saveUrl = smallButton(I18n.t("settings.update.saveSource", "保存更新源"), e -> {
            config.setUpdateUrl(urlField.getText().trim());
            config.setUpdateMirrorUrl(mirrorField.getText().trim());
            config.save();
            Toast.ok(I18n.t("settings.update.sourceSaved", "更新源已保存"));
        });
        Button download = smallButton(I18n.t("settings.update.download", "下载并安装更新"), e -> { });
        download.setVisible(false);
        download.setManaged(false);

        // 回退：只有免安装版留下过备份才出现
        java.io.File latestBackup = com.example.demo.core.app.UpdateInstaller.latestBackup();
        Button rollback = smallButton(I18n.t("update.rollback", "回退到上一版"), e -> {
            java.io.File launcher = com.example.demo.core.app.UpdateInstaller.currentLauncherExe();
            java.io.File root = com.example.demo.core.app.UpdateInstaller.portableRoot(launcher);
            java.io.File backup = com.example.demo.core.app.UpdateInstaller.latestBackup();
            if (root == null || backup == null) {
                Toast.info(I18n.t("update.rollback.none", "没有可用的备份，先更新一次才会有"));
                return;
            }
            javafx.scene.control.Alert confirm = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.CONFIRMATION);
            confirm.initOwner(owner());
            confirm.setTitle(I18n.t("update.rollback.title", "回退到上一版"));
            confirm.setHeaderText(I18n.t("update.rollback.header", "将用备份覆盖当前程序文件"));
            confirm.setContentText(I18n.t("update.rollback.body", "备份时间：")
                    + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm")
                    .format(new java.util.Date(backup.lastModified()))
                    + "\n" + I18n.t("update.rollback.body2", "存档 game\\ 与 config.json 不受影响。回退会关闭启动器并自动重新打开。"));
            if (confirm.showAndWait().orElse(javafx.scene.control.ButtonType.CANCEL)
                    != javafx.scene.control.ButtonType.OK) {
                return;
            }
            try {
                java.io.File script = com.example.demo.core.app.UpdateInstaller
                        .prepareRollbackScript(backup, root);
                com.example.demo.core.app.UpdateInstaller.runPowerShellDetached(script);
                Toast.ok(I18n.t("update.rollback.running", "正在回退，启动器即将重启"));
                javafx.application.Platform.runLater(() -> {
                    javafx.application.Platform.exit();
                    System.exit(0);
                });
            } catch (Exception ex) {
                Toast.error(I18n.t("update.rollback.failed", "回退失败：") + ex.getMessage());
            }
        });
        rollback.setVisible(latestBackup != null);
        rollback.setManaged(latestBackup != null);

        // 更新内容：可滚动，跨版本时提示先看说明
        javafx.scene.control.TextArea notesArea = new javafx.scene.control.TextArea();
        notesArea.setEditable(false);
        notesArea.setWrapText(true);
        notesArea.setPrefRowCount(6);
        notesArea.setStyle("-fx-font-family: 'Consolas','Microsoft YaHei',monospace; -fx-font-size: 11px;");
        notesArea.setVisible(false);
        notesArea.setManaged(false);

        Button manageBackups = smallButton(I18n.t("update.backup.manage", "管理备份"), e -> {
            java.util.List<java.io.File> backups = com.example.demo.core.app.UpdateInstaller.listBackups();
            if (backups.isEmpty()) {
                Toast.info(I18n.t("update.backup.none", "还没有备份：免安装版更新一次后会自动留一份"));
                return;
            }
            javafx.scene.control.ListView<java.io.File> list = new javafx.scene.control.ListView<>();
            list.getItems().setAll(backups);
            list.setPrefHeight(220);
            list.setCellFactory(view -> new javafx.scene.control.ListCell<>() {
                @Override
                protected void updateItem(java.io.File item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        return;
                    }
                    long mb = com.example.demo.core.app.UpdateInstaller.backupSize(item) / 1024 / 1024;
                    setText(item.getName() + "    " + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm")
                            .format(new java.util.Date(item.lastModified())) + "    " + mb + " MB");
                }
            });
            Label total = new Label();
            Runnable refreshTotal = () -> {
                long sum = 0;
                for (java.io.File backup : com.example.demo.core.app.UpdateInstaller.listBackups()) {
                    sum += com.example.demo.core.app.UpdateInstaller.backupSize(backup);
                }
                total.setText(I18n.t("update.backup.total", "共占用 ") + (sum / 1024 / 1024) + " MB");
                total.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11px;");
            };
            refreshTotal.run();
            javafx.scene.control.Button delete = new javafx.scene.control.Button(
                    I18n.t("update.backup.delete", "删除选中"));
            delete.setOnAction(ev -> {
                java.io.File selected = list.getSelectionModel().getSelectedItem();
                if (selected == null) {
                    Toast.info(I18n.t("update.backup.pick", "先选中一个备份"));
                    return;
                }
                if (com.example.demo.core.app.UpdateInstaller.deleteBackup(selected)) {
                    Toast.ok(I18n.t("update.backup.deleted", "备份已删除"));
                    list.getItems().setAll(com.example.demo.core.app.UpdateInstaller.listBackups());
                    refreshTotal.run();
                } else {
                    Toast.error(I18n.t("update.backup.deleteFailed", "删除失败（文件可能被占用）"));
                }
            });
            javafx.scene.layout.VBox box = new javafx.scene.layout.VBox(8, list, total,
                    new javafx.scene.layout.HBox(10, delete));
            box.setPrefWidth(520);
            com.example.demo.ui.shell.InAppDialog.customSized(
                    com.example.demo.ui.shell.InAppDialog.globalHost(),
                    I18n.t("update.backup.title", "更新备份"),
                    box,
                    java.util.List.of(com.example.demo.ui.shell.InAppDialog.Action.secondary(
                            I18n.t("common.close", "关闭"), null)),
                    null,
                    560,
                    380);
        });

        Label status = new Label(I18n.t("settings.update.hint", "更新检查使用静态 JSON：version / url / notes。"));
        status.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11px;");
        status.setWrapText(true);

        Button check = smallButton(I18n.t("settings.update.check", "检查更新"), e -> {
            String url = urlField.getText().trim();
            String mirror = mirrorField.getText().trim();
            config.setUpdateUrl(url);
            config.setUpdateMirrorUrl(mirror);
            config.save();
            if (url.isBlank()) {
                Toast.info(I18n.t("settings.update.needUrl", "请先填写更新源 JSON 地址"));
                return;
            }
            ((Button) e.getSource()).setDisable(true);
            status.setText(I18n.t("settings.update.checking", "正在检查更新…"));
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
                    // 下载前就把版本、大小、说明说清楚：用户不用点下去才知道要下多大的东西
                    boolean portableMode = !com.example.demo.core.app.UpdateInstaller
                            .isInstalledLayout(com.example.demo.core.app.UpdateInstaller.currentLauncherExe());
                    long bytes = portableMode && info.portableSize() > 0
                            ? info.portableSize()
                            : info.sizeBytes();
                    String size = bytes > 0
                            ? String.format(java.util.Locale.ROOT, "%.1f MB", bytes / 1024.0 / 1024.0)
                            : I18n.t("update.sizeUnknown", "大小未知");
                    status.setText(I18n.t("update.found", "发现新版本 ") + info.version()
                            + "（" + size + "）"
                            + (info.notes().isBlank() ? "" : "：" + info.notes()));
                    status.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 11px;");
                    if (!info.url().isBlank()) {
                        download.setVisible(true);
                        download.setManaged(true);
                        download.setOnAction(ev -> downloadAndInstall(info, status, download));
                    }
                    // 更新内容内嵌展示（原来只有一行拼接，长说明直接看不见）
                    if (!info.notes().isBlank()) {
                        notesArea.setText(info.notes());
                        notesArea.setVisible(true);
                        notesArea.setManaged(true);
                    } else {
                        notesArea.setVisible(false);
                        notesArea.setManaged(false);
                    }
                    // 跨版本提醒：相隔 3 个小版本以上就别急着点，先看说明
                    int gap = com.example.demo.core.app.UpdateChecker
                            .versionGap(info.version(), AppVersion.CURRENT);
                    if (gap >= 3) {
                        status.setText(status.getText() + "\n"
                                + I18n.t("update.gap.hint", "这次跨了好几个版本，建议先看完下面的更新说明再装。"));
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
                new HBox(10, rollback, manageBackups, hint(I18n.t("update.rollback.hint",
                        "只在免安装版更新过之后可用；卸载版由安装程序自己管理版本"))),
                new HBox(10, mirrorField),
                status,
                notesArea);
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
        // 输入框留空 = 保持已保存的 Key（避免手滑清空注册表里的 Key）
        String typedKey = aiKeyField.getText() == null ? "" : aiKeyField.getText().trim();
        if (!typedKey.isEmpty()) {
            config.setAiApiKey(typedKey);
        }
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
        config.setShowKeyHints(keyHintsBox.isSelected());
        config.setAutoRepairLibraries(autoRepairBox.isSelected());
        config.setHardwareAcceleration(hardwareAccelBox.isSelected());
        config.setAnimations(animationsBox.isSelected());
        config.setAnimationSpeed(speedValue(animationSpeedCombo.getValue()));
        config.setUiOpacity(Math.round(opacitySlider.getValue() * 100.0) / 100.0);
        config.setSheenStrength(Math.round(sheenSlider.getValue() * 100.0) / 100.0);
        config.setShowPerfOverlay(perfOverlayBox.isSelected());
        com.example.demo.ui.shell.PerfOverlay.setGlobalActive(config.isShowPerfOverlay());
        com.example.demo.ui.shell.MouseSheen.setGlobalStrength(config.getSheenStrength());
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
        keyHintsBox.setSelected(true);
        autoRepairBox.setSelected(true);
        hardwareAccelBox.setSelected(true);
        animationsBox.setSelected(true);
        animationSpeedCombo.setValue("标准");
        opacitySlider.setValue(1.0);
        sheenSlider.setValue(0.8);
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
                    ? "English" : "简体中文");
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
        config.setShowKeyHints(loaded.isShowKeyHints());
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

    /** 扫描本机已装的 JDK，点一个就填进 Java 路径 —— 比手敲路径省事，也不容易填错。 */
    private void detectLocalJava() {
        File gameDir = new File(config.getGameDir());
        java.util.concurrent.CompletableFuture
                .supplyAsync(() -> com.example.demo.core.game.JavaScanner.scan(gameDir, true))
                .thenAccept(list -> javafx.application.Platform.runLater(() -> showJavaList(list)));
    }

    private void showJavaList(java.util.List<com.example.demo.core.game.JavaScanner.JavaInfo> list) {
        javafx.scene.layout.VBox box = new javafx.scene.layout.VBox(6);
        box.setPadding(new javafx.geometry.Insets(6));
        if (list.isEmpty()) {
            Label empty = new Label(I18n.t("settings.java.none",
                    "没有在本机扫描到 Java；可以点「浏览」手动选 java.exe"));
            empty.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 12px;");
            box.getChildren().add(empty);
        }
        for (var info : list) {
            // 跳过我们自己的 CRaC 备份目录，它不是可用的运行时
            if (info.exe().getAbsolutePath().toLowerCase(java.util.Locale.ROOT).contains("crac")) {
                continue;
            }
            Label name = new Label("Java " + info.major() + "  ·  " + info.vendor());
            name.setStyle("-fx-text-fill: -app-text; -fx-font-size: 12px; -fx-font-weight: bold;");
            Label path = new Label(info.exe().getAbsolutePath());
            path.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 10.5px;");
            javafx.scene.control.Button use = new javafx.scene.control.Button(
                    I18n.t("settings.java.use", "使用这个"));
            use.getStyleClass().addAll("btn-secondary", "btn-sm");
            use.setOnAction(e -> {
                javaPathField.setText(info.exe().getAbsolutePath());
                config.setJavaPath(info.exe().getAbsolutePath());
                config.save();
                Toast.ok(I18n.t("settings.java.picked", "已选用 Java ") + info.major());
                com.example.demo.ui.shell.InAppDialog.closeAll();
            });
            javafx.scene.layout.Region gap = new javafx.scene.layout.Region();
            javafx.scene.layout.HBox.setHgrow(gap, javafx.scene.layout.Priority.ALWAYS);
            javafx.scene.layout.HBox row = new javafx.scene.layout.HBox(8,
                    new javafx.scene.layout.VBox(1, name, path), gap, use);
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            row.getStyleClass().add("neko-card");
            row.setPadding(new javafx.geometry.Insets(8, 10, 8, 10));
            box.getChildren().add(row);
        }
        com.example.demo.ui.shell.InAppDialog.customSized(
                com.example.demo.ui.shell.InAppDialog.globalHost(),
                I18n.t("settings.java.title", "本机 Java"),
                box,
                java.util.List.of(com.example.demo.ui.shell.InAppDialog.Action.secondary(
                        I18n.t("common.close", "关闭"), null)),
                null,
                620,
                Math.min(520, 160 + list.size() * 62));
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
        box.getChildren().addAll(children);
        box.getStyleClass().add("settings-card");
        // 设置卡片轻轻抬起一点，鼠标掠过时能感觉到"这张卡是活的"
        com.example.demo.ui.shell.Motion.hoverCard(box);
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
        label.setStyle("-fx-text-fill: -app-text-dim-3; -fx-font-size: 12px; -fx-font-weight: bold;");
        label.setPadding(new Insets(6, 0, 0, 0));
        return label;
    }

    private static Label hint(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11.5px;");
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
        button.getStyleClass().add("btn-secondary");
        button.setMinWidth(Region.USE_PREF_SIZE);
        Motion.press(button);
        if (handler != null) button.setOnAction(handler);
        return button;
    }
}
