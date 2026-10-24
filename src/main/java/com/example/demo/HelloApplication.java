package com.example.demo;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import javafx.animation.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.io.*;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class HelloApplication extends Application {

    /** 克隆实例时，属于「数据」而不是「版本」的目录。 */
    private static final java.util.Set<String> CLONE_DATA_DIRS = java.util.Set.of(
            "mods", "config", "defaultconfigs", "saves", "screenshots", "resourcepacks",
            "shaderpacks", "logs", "crash-reports", "schematics");

    private UserSession currentSession;
    private Button maximizeWindowButton;
    private StackPane contentStack;
    private javafx.scene.layout.VBox rootPane;
    private VBox launchPanel;
    private HBox downloadPanel;

    /** 页面缓存：按需创建，切换时做淡入淡出。 */
    private final Map<String, javafx.scene.Node> pages = new LinkedHashMap<>();
    private final Map<String, Button> navButtons = new LinkedHashMap<>();
    private javafx.stage.Stage primaryStage;
    private String currentPage;
    private boolean downloadInitialized;
    private InstancesPanel instancesPanel;
    private InstanceSettingsPanel instanceSettingsPanel;
    private ToolsPanel toolsPanel;
    private SettingsPanel settingsPanel;
    private String navHighlight;
    /** 测试启动：true 时启动 45 秒后自动结束并报告结果 */
    private boolean testMode;
    private javafx.stage.Stage stageRef;
    private volatile boolean testSuccess;
    private Rectangle navIndicator;
    private HBox navBox;

    /** 程序内部切换版本下拉框时不要覆盖「上次使用的实例」 */
    private boolean suppressInstancePersist;
    /** 用户手动选过实例（刷新列表时不要抢回来） */
    private boolean userPickedInstance;

    private Label usernameLabel;
    private Label userTypeLabel;
    private Label insightTip;
    private Label insightMetrics;
    private Label uuidLabel;
    private Button launchBtn;
    private ComboBox<MinecraftVersion> versionComboBox;
    private TextArea logArea;
    private Label launchMainLabel;
    private VBox downloadBarPanel;
    private int downloadBarSeq;
    private int lastLaunchLogStart;
    private long gameStartAt;
    private long launchPrepareStart;
    private long lastLaunchPrepMs;
    private boolean minimizedForGame;
    private boolean gameRunning;
    private boolean trayInstalled;
    private boolean quitting;
    private java.awt.TrayIcon trayIcon;
    private Label geekTelemetry;
    private final long appStartedAt = System.currentTimeMillis();
    private AmbientBackground ambientBackground;
    private String gameInstanceId;
    private ComboBox<String> loginTypeComboRef;
    private ComboBox<String> serverFieldRef;
    private VBox loginCard;
    private ComboBox<String> loginTypeCombo;
    private ComboBox<String> serverField;
    private TextField usernameField;
    private PasswordField passwordField;
    private Button loginBtn;
    private Label loginBtnLabel;
    private Button logoutButton;
    private javafx.scene.shape.Circle gameStatusDot;
    private Label gameStatusLabel;
    private javafx.animation.FadeTransition gameStatusPulse;
    private VBox playerInfoBox;
    private Label playerInfoType;
    private Label playerInfoUuid;
    private Label playerInfoServer;
    private Label playerInfoInstance;
    private Label playerInfoPlaytime;
    private final java.util.List<String> commandHistory = new java.util.ArrayList<>();
    private int commandHistoryIndex = 0;
    private TextField commandField;
    private StackPane commandPrefix;
    private Button commandAiToggle;
    private boolean aiBusy;
    private final java.util.List<DeepSeekClient.Message> aiHistory = new java.util.ArrayList<>();
    private Process gameProcess;
    private ProgressBar downloadProgress;
    private Label downloadFileLabel;
    private Label downloadStatsLabel;
    private Label launchVersionLabel;
    private ImageView avatarImageView;

    private Rectangle downloadNavIndicator;
    private VBox downloadNavBox;
    private String minecraftFilter = "all";
    private VBox favoritesPanel;
    private VBox worldsNoticePanel;
    private VBox favoritesList;
    private VBox favoritesResourceList;
    private TextField favoritesSearch;
    private ComboBox<String> favoritesSortCombo;
    private String favoritesQuery = "";

    private StackPane downloadContentArea;
    private VBox communityPanel;
    private VBox minecraftPanel;
    private VBox installerPanel;
    private VBox versionDetailPanel;
    private VBox modDetailPanel;
    private WorldsPanel worldsPanel;
    private ComboBox<MinecraftVersion> installerVersionCombo;
    private final java.util.Map<String, ComboBox<String>> installerLoaderCombos = new java.util.LinkedHashMap<>();
    private ToggleGroup installerLoaderGroup;
    private Label installerTitle;
    private final java.util.Map<String, VBox> installerLoaderBodies = new java.util.LinkedHashMap<>();
    private final java.util.Map<String, HBox> installerLoaderHeaders = new java.util.LinkedHashMap<>();
    private final java.util.Map<String, Label> installerLoaderSubtitles = new java.util.LinkedHashMap<>();
    private final java.util.Map<String, Label> installerLoaderArrows = new java.util.LinkedHashMap<>();
    private final java.util.Map<String, String> installerSelectedVersions = new java.util.LinkedHashMap<>();
    private String installerExpandedLoader;
    private TextField installerNameField;
    private Label installerStatus;
    private javafx.scene.control.ProgressBar installerProgress;
    private VBox installerAccordion;
    private String currentLoader = "Fabric";
    private String currentProjectType = "整合包";

    private TextField searchField;
    private VBox filterBox;
    private StackPane listPane;
    private VBox statusBox;
    private ProgressIndicator loadingIndicator;
    private Label statusLabel;
    private ComboBox<String> sourceCombo;
    private ComboBox<String> tagCombo;
    private ComboBox<String> sortCombo;
    private ComboBox<String> gameVersionCombo;
    private ComboBox<String> loaderCombo;

    private ListView<ModpackProject> modpackListView;
    private static final java.util.Map<String, Integer> PAGE_ORDER =
            java.util.Map.of("launch", 0, "download", 1, "settings", 2, "tools", 3);
    private volatile String lastSearchSource = "Modrinth";
    private long lastMirrorNoticeAt;
    private ObservableList<ModpackProject> modpackData = FXCollections.observableArrayList();

    /** 社区资源分页状态：滚动到底自动续拉，不再只显示固定的 20 个。 */
    private int searchOffset;
    private int searchTotal = -1;
    private boolean loadingMoreResults;
    private boolean suppressFilterSearch;
    private Label loadMoreHint;

    private ObservableList<MinecraftVersion> allMinecraftVersions = FXCollections.observableArrayList();
    private VBox minecraftCategoriesContainer;

    private Timeline searchDebounceTimer;
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(15))
            .build();
    private boolean isSearching = false;
    private static final Duration FAST_ANIM = Duration.millis(150);
    private static final Interpolator SMOOTH_INTERPOLATOR = Interpolator.SPLINE(0.25, 0.1, 0.25, 1);

    /** 启动器配置（内存 / Java 路径 / 游戏目录），启动时从 config.json 读取 */
    private final LauncherConfig config = LauncherConfig.load();

    /** 全局样式表 URL：弹窗是独立的 Scene，需要自己再挂一次才能保持深色主题 */
    public static String THEME_URL = null;

    /** 解析游戏目录：相对路径以启动器 jar 所在目录为基准，保证双击启动时 game 落在启动器旁边。 */
    private File resolveGameDir() {
        File configured = new File(config.getGameDir());
        if (configured.isAbsolute()) return configured;
        File resolved = new File(AppPaths.dataDir(), config.getGameDir()).getAbsoluteFile();
        config.setGameDir(resolved.getAbsolutePath());
        return resolved;
    }

    /** 依次尝试 jpackage / CodeSource / sun.java.command / class 资源 URL，最后回退到当前工作目录。 */
    private File launcherBaseDir() {
        return AppPaths.baseDir();
    }

    @Override
    public void start(Stage stage) {
        stageRef = stage;
        File bootstrapGameDir = resolveGameDir();
        Bootstrap.ensureGameDir(bootstrapGameDir, null);
        if (!SingleInstance.acquire(bootstrapGameDir)) {
            javafx.scene.control.Alert running = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.WARNING,
                    "启动器已经在运行，请先关闭已打开的窗口。", javafx.scene.control.ButtonType.OK);
            running.setTitle("Neko Craft Launcher");
            running.setHeaderText("已有实例在运行");
            running.showAndWait();
            Platform.exit();
            return;
        }
        stage.setOnCloseRequest(e -> {
            e.consume();
            requestClose();
        });
        try (java.io.InputStream iconStream = HelloApplication.class.getResourceAsStream("/icon.png")) {
            if (iconStream != null) stage.getIcons().add(new Image(iconStream));
        } catch (Exception ignored) {
            // 没图标也能启动
        }
        Theme.setCurrent(Theme.byName(config.getAccentColor()));
        Theme.setLight(config.isLightTheme());
        Motion.configure(config.isAnimations(), config.getAnimationSpeed());
        HBox topBar = createTopBar();

        contentStack = new StackPane();
        contentStack.setAlignment(javafx.geometry.Pos.TOP_LEFT);
        launchPanel = createLaunchPanel();
        downloadPanel = createDownloadPanel();
        downloadPanel.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        pages.put("launch", launchPanel);
        pages.put("download", downloadPanel);
        launchPanel.setId("launch-root");
        applyLaunchBackground();
        launchPanel.setMaxWidth(Double.MAX_VALUE);
        launchPanel.setMaxHeight(Double.MAX_VALUE);
        downloadPanel.setMaxWidth(Double.MAX_VALUE);
        downloadPanel.setMaxHeight(Double.MAX_VALUE);
        downloadPanel.setVisible(false);
        downloadPanel.setOpacity(0);
        AmbientBackground ambient = new AmbientBackground();
        ambientBackground = ambient;
        ambient.widthProperty().bind(contentStack.widthProperty());
        ambient.heightProperty().bind(contentStack.heightProperty());
        contentStack.getChildren().add(ambient);
        contentStack.getChildren().addAll(launchPanel, downloadPanel);
        contentStack.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_MOVED,
                e -> ambient.setPointer(e.getX(), e.getY()));
        contentStack.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_EXITED,
                e -> ambient.clearPointer());

        primaryStage = stage;
        primaryStage.maximizedProperty().addListener((obs, oldMax, max) -> {
            if (maximizeWindowButton != null) {
                maximizeWindowButton.setGraphic(windowIcon(max ? WindowIcon.RESTORE : WindowIcon.MAXIMIZE));
            }
        });
        VBox root = new VBox(topBar, contentStack);
        rootPane = root;
        VBox.setVgrow(contentStack, Priority.ALWAYS);
        contentStack.setMinHeight(0);
        root.setStyle("-fx-background-color: linear-gradient(to bottom, -app-root-1 0%, -app-root-2 32%, -app-root-3 100%);"
                + " -fx-background-radius: 12;");
        root.setMinSize(0, 0);
        root.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        javafx.scene.shape.Rectangle resizeGrip = new javafx.scene.shape.Rectangle(18, 18, Color.web("#ffffff", 0.10));
        resizeGrip.setArcWidth(6);
        resizeGrip.setArcHeight(6);
        resizeGrip.setCursor(Cursor.SE_RESIZE);
        StackPane rootWrap = new StackPane(root, resizeGrip);
        rootWrap.setAlignment(Pos.TOP_LEFT);
        StackPane.setAlignment(resizeGrip, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(resizeGrip, new Insets(0, 6, 6, 0));
        final double[] gripStart = new double[4];
        resizeGrip.setOnMousePressed(e -> {
            gripStart[0] = e.getScreenX();
            gripStart[1] = e.getScreenY();
            gripStart[2] = stage.getWidth();
            gripStart[3] = stage.getHeight();
        });
        resizeGrip.setOnMouseDragged(e -> {
            stage.setWidth(Math.max(980, gripStart[2] + e.getScreenX() - gripStart[0]));
            stage.setHeight(Math.max(640, gripStart[3] + e.getScreenY() - gripStart[1]));
        });
        rootWrap.setStyle("-fx-background-color: transparent;");

        javafx.scene.canvas.Canvas crt = new javafx.scene.canvas.Canvas();
        crt.setMouseTransparent(true);
        crt.widthProperty().bind(rootWrap.widthProperty());
        crt.heightProperty().bind(rootWrap.heightProperty());
        Runnable paintCrt = () -> {
            double w = crt.getWidth();
            double h = crt.getHeight();
            if (w < 4 || h < 4) return;
            javafx.scene.canvas.GraphicsContext g = crt.getGraphicsContext2D();
            g.clearRect(0, 0, w, h);
            g.setFill(Color.web("#ff3b30", 0.018));
            for (double y = 0; y < h; y += 3) g.fillRect(0, y, w, 1);
            g.setStroke(Color.web("#ff5a48", 0.22));
            g.setLineWidth(1);
            double inset = 12;
            double len = 18;
            g.strokeLine(inset, inset, inset + len, inset);
            g.strokeLine(inset, inset, inset, inset + len);
            g.strokeLine(w - inset, inset, w - inset - len, inset);
            g.strokeLine(w - inset, inset, w - inset, inset + len);
            g.strokeLine(inset, h - inset, inset + len, h - inset);
            g.strokeLine(inset, h - inset, inset, h - inset - len);
            g.strokeLine(w - inset, h - inset, w - inset - len, h - inset);
            g.strokeLine(w - inset, h - inset, w - inset, h - inset - len);
        };
        crt.widthProperty().addListener((o, a, b) -> paintCrt.run());
        crt.heightProperty().addListener((o, a, b) -> paintCrt.run());
        rootWrap.getChildren().add(1, crt);

        Scene scene = new Scene(rootWrap, 1060, 700);
        scene.setFill(Color.TRANSPARENT);
        stage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
        stage.setMinWidth(1060);
        stage.setMinHeight(700);

        scene.setOnKeyPressed(e -> {
            if (e.isControlDown() && e.getCode() == javafx.scene.input.KeyCode.K) {
                showCommandPalette();
                e.consume();
                return;
            }
            if (e.isControlDown() && e.getCode() == javafx.scene.input.KeyCode.Q) {
                requestClose();
                e.consume();
                return;
            }
            switch (e.getCode()) {
                case F1 -> switchPage("launch");
                case F2 -> switchPage("instances");
                case F3 -> openInstanceSettings("overview");
                case F4 -> openInstanceSettings("mods");
                case F5 -> switchPage("download");
                case F6 -> switchPage("settings");
                case F7 -> switchPage("tools");
                case F8 -> {
                    switchPage("settings");
                    if (settingsPanel != null) settingsPanel.openTab("查看日志");
                }
                default -> { }
            }
        });

        // 全局样式来自资源文件（app.css），不再每次启动拼字符串 + 写临时文件；
        // 控件配色集中在 .root 的 -app-* 变量里，改主题只改一处。
        try {
            java.net.URL cssUrl = HelloApplication.class.getResource("/com/example/demo/app.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
                THEME_URL = cssUrl.toExternalForm();
            }
        } catch (Exception e) {
            System.err.println("[theme] 全局样式加载失败，界面会回退到默认样式");
        }

        // app.css 之后再绑定，浅色样式表才能排在它后面
        Theme.bindScene(scene);

        Toast.install(rootWrap);

        javafx.application.Platform.setImplicitExit(false);
        stage.setOpacity(Math.max(0.7, Math.min(1.0, config.getUiOpacity())));
        stage.setTitle("Neko Craft Launcher");
        stage.iconifiedProperty().addListener((obs, was, iconified) -> {
            if (iconified) ambient.stop();
            else if (!gameRunning || !config.isPauseVisualsInGame()) ambient.start();
        });
        BootScreen boot = new BootScreen(() -> Platform.runLater(this::maybeOfferPerformanceOptimization));
        rootWrap.getChildren().add(boot);
        stage.setScene(scene);
        stage.show();
        stage.centerOnScreen();

        switchPage("launch");
        Bootstrap.ensureGameDir(new File(config.getGameDir()),
                message -> Platform.runLater(() -> logArea.appendText(message + "\n")));
        LauncherLog.attach(new File(config.getGameDir()), logArea);
        restoreSessionFromRegistry();
        File detectGameDir = new File(config.getGameDir());
        CompletableFuture.runAsync(() -> {
            HardwareInfo.Detect hardware = HardwareInfo.detect(detectGameDir);
            MemoryAdvisor.Suggestion suggestion = MemoryAdvisor.suggest(
                    InstanceDirs.mods(detectGameDir, config.getLastInstance(), config.isIsolation()));
            Platform.runLater(() -> {
                logArea.appendText("🖥 硬件识别：" + hardware.summary() + "\n");
                logArea.appendText("💡 智能推荐内存：" + suggestion.memoryMb() + " MB（"
                        + suggestion.reason() + "）\n");
            });
        });
        String autoImport = System.getProperty("mc.import");
        if (autoImport != null && !autoImport.isBlank()) {
            Platform.runLater(() -> {
                logArea.appendText("🧪 自动导入测试: " + autoImport + "\n");
                importModpack(new File(autoImport));
            });
        }
        DownloadManager.addStatusListener(status -> Platform.runLater(() -> updateDownloadBar(status)));
        updateGeekTelemetry();
        javafx.animation.Timeline telemetryTimer = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), e -> updateGeekTelemetry()));
        telemetryTimer.setCycleCount(javafx.animation.Animation.INDEFINITE);
        telemetryTimer.play();
        refreshInstalledVersions();
        if (!config.isHardwareAcceleration()) {
            logArea.appendText("🖥 已按设置禁用硬件加速（软件渲染）。如果界面明显变卡，"
                    + "可在 设置 → 启动器 里重新开启。\n");
        }

        // 先用本地缓存的版本列表把界面填满（0 网络等待），再后台拉最新
        List<MinecraftVersion> cachedVersions = VersionManager.loadCachedList();
        if (cachedVersions != null && !cachedVersions.isEmpty()) {
            applyVersionList(cachedVersions, false);
        }

        VersionManager.fetchVersionListAsync().thenAccept(versionList ->
                Platform.runLater(() -> applyVersionList(versionList, true))
        ).exceptionally(ex -> {
            Platform.runLater(() -> {
                if (versionComboBox.getItems().isEmpty()) {
                    versionComboBox.setPromptText("版本列表加载失败");
                }
                logArea.appendText("❌ 版本列表加载失败: " + ex.getMessage() + "\n");
                logArea.appendText("   已安装的版本仍然可以直接启动。\n");
            });
            return null;
        });
    }

    /** 把一份版本列表灌进下拉框（缓存和在线共用；缓存不刷日志）。 */
    private void applyVersionList(List<MinecraftVersion> versionList, boolean announce) {
        if (versionList == null || versionList.isEmpty()) return;
        allMinecraftVersions.setAll(versionList);
        List<MinecraftVersion> releases = versionList.stream()
                .filter(v -> "release".equals(v.getType())).toList();
        if (releases.isEmpty()) {
            if (announce) logArea.appendText("⚠️ 版本列表里没有正式版，只拿到快照。\n");
            return;
        }
        versionComboBox.getItems().setAll(releases);
        suppressInstancePersist = true;
        try {
            versionComboBox.getSelectionModel().selectFirst();
            refreshInstalledVersions();
        } finally {
            suppressInstancePersist = false;
        }
        if (announce) {
            logArea.appendText("✅ 版本列表加载完成（共 " + versionList.size() + " 个版本，"
                    + releases.size() + " 个正式版）\n");
        }
    }

    // ================== 顶部与切换逻辑 ==================
    // ================== 顶部导航与页面切换 ==================
    private HBox createTopBar() {
        HBox topBar = new HBox(14);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.getStyleClass().add("app-topbar");
        topBar.setPadding(new Insets(0, 6, 0, 18));
        topBar.setPrefHeight(58);
        topBar.setMinHeight(58);

        javafx.scene.shape.Rectangle mark = new javafx.scene.shape.Rectangle(3, 18);
        mark.setArcWidth(3);
        mark.setArcHeight(3);
        mark.setStyle("-fx-fill: #14b8a6;");
        Label appTitle = new Label("Neko Craft Launcher");
        appTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        appTitle.setTextFill(javafx.scene.paint.Color.web("#e8eaee"));
        Label badge = new Label("NEKO");
        badge.setStyle("-fx-font-size: 10px; -fx-font-weight: bold;");
        badge.setTextFill(javafx.scene.paint.Color.web("#5eead4"));
        Label liveClock = new Label();
        liveClock.setStyle("-fx-text-fill: #7b818b; -fx-font-size: 10.5px; -fx-font-family: 'Cascadia Mono', 'Consolas', monospace;");
        java.time.format.DateTimeFormatter clockFormat = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss");
        liveClock.setText(java.time.LocalTime.now().format(clockFormat));
        javafx.animation.Timeline clockTimer = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1),
                        e -> liveClock.setText(java.time.LocalTime.now().format(clockFormat))));
        clockTimer.setCycleCount(javafx.animation.Animation.INDEFINITE);
        clockTimer.play();
        gameStatusDot = new javafx.scene.shape.Circle(4, Color.web("#6b7280"));
        gameStatusLabel = new Label("就绪");
        gameStatusLabel.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 10px;");
        HBox statusChip = new HBox(6, gameStatusDot, gameStatusLabel);
        statusChip.setAlignment(Pos.CENTER_LEFT);
        statusChip.setPadding(new Insets(3, 9, 3, 9));
        statusChip.setStyle("-fx-background-color: rgba(255,255,255,0.045); -fx-background-radius: 10;"
                + " -fx-border-color: rgba(255,255,255,0.06); -fx-border-radius: 10;");
        HBox brand = new HBox(8, mark, appTitle, Icons.node("star", 13, "#ffcf6a"), badge, liveClock, statusChip);
        brand.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        navBox = new HBox(2);
        navBox.setAlignment(Pos.CENTER);
        navIndicator = new Rectangle(0, 2);
        navIndicator.setArcWidth(2);
        navIndicator.setArcHeight(2);
        navIndicator.setStyle("-fx-fill: linear-gradient(to right, #d1d5db, #9ca3af);");
        navIndicator.setMouseTransparent(true);
        // 顶栏选中态改用白色胶囊描边，旧的蓝色下划线不再显示
        navIndicator.setVisible(false);
        navIndicator.setManaged(false);

        StackPane navPane = new StackPane(navIndicator, navBox);
        navPane.setAlignment(Pos.CENTER);
        StackPane.setAlignment(navIndicator, Pos.BOTTOM_LEFT);
        navPane.setPickOnBounds(false);

        String[][] entries = {
                {"launch", "启动", "play"},
                {"download", "下载", "download"},
                {"settings", "设置", "gear"},
                {"tools", "工具", "tools"}
        };
        for (String[] entry : entries) {
            Button button = createTopButton(entry[1], entry[2]);
            button.setUserData(entry[2]);
            button.setOnAction(e -> switchPage(entry[0]));
            navButtons.put(entry[0], button);
            Motion.press(button);
            navBox.getChildren().add(button);
        }

        // 窗口控制按钮（无边框窗口需要自绘，固定 46x32，图标严格居中）
        Button minimize = windowButton(WindowIcon.MINIMIZE);
        minimize.setOnAction(e -> primaryStage.setIconified(true));
        maximizeWindowButton = windowButton(WindowIcon.MAXIMIZE);
        maximizeWindowButton.setOnAction(e -> primaryStage.setMaximized(!primaryStage.isMaximized()));
        Button close = windowButton(WindowIcon.CLOSE);
        close.setOnAction(e -> requestClose());
        HBox windowControls = new HBox(0, minimize, maximizeWindowButton, close);
        windowControls.setAlignment(Pos.CENTER_RIGHT);
        windowControls.getStyleClass().add("window-controls");

        // 拖动窗口 / 双击最大化
        final double[] dragOffset = new double[2];
        topBar.setOnMousePressed(e -> {
            dragOffset[0] = e.getSceneX();
            dragOffset[1] = e.getSceneY();
        });
        topBar.setOnMouseDragged(e -> {
            if (primaryStage.isMaximized()) return;
            primaryStage.setX(e.getScreenX() - dragOffset[0]);
            primaryStage.setY(e.getScreenY() - dragOffset[1]);
        });
        topBar.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) primaryStage.setMaximized(!primaryStage.isMaximized());
        });

        Button paletteBtn = new Button();
        paletteBtn.setGraphic(Icons.node("search", 14, "#c7ccd4"));
        paletteBtn.setPrefSize(30, 30);
        paletteBtn.setMinSize(30, 30);
        paletteBtn.setMaxSize(30, 30);
        paletteBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 0; -fx-background-radius: 8;");
        paletteBtn.setTooltip(new javafx.scene.control.Tooltip("命令面板  Ctrl+K"));
        paletteBtn.setOnMouseEntered(e -> paletteBtn.setStyle(
                "-fx-background-color: rgba(255,255,255,0.10); -fx-cursor: hand; -fx-padding: 0; -fx-background-radius: 8;"));
        paletteBtn.setOnMouseExited(e -> paletteBtn.setStyle(
                "-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 0; -fx-background-radius: 8;"));
        paletteBtn.setOnAction(e -> showCommandPalette());

        topBar.getChildren().addAll(brand, spacer, navPane, paletteBtn, windowControls);
        return topBar;
    }

    private enum WindowIcon {MINIMIZE, MAXIMIZE, RESTORE, CLOSE}

    private String windowButtonStyle() {
        return "-fx-background-color: transparent; -fx-text-fill: white;"
                + " -fx-cursor: hand; -fx-padding: 0; -fx-background-radius: 0;";
    }

    private Button windowButton(WindowIcon icon) {
        Button button = new Button();
        button.setGraphic(windowIcon(icon));
        button.setPrefSize(46, 32);
        button.setMinSize(46, 32);
        button.setMaxSize(46, 32);
        button.setFocusTraversable(false);
        button.setStyle(windowButtonStyle());
        button.getStyleClass().add("window-control");
        button.setOnMouseEntered(e -> button.setStyle(
                "-fx-background-color: " + (icon == WindowIcon.CLOSE ? "#e81123" : "rgba(255,255,255,0.12)") + ";"
                        + " -fx-cursor: hand; -fx-padding: 0; -fx-background-radius: 0;"));
        button.setOnMouseExited(e -> button.setStyle(windowButtonStyle()));
        return button;
    }

    private javafx.scene.Node windowIcon(WindowIcon icon) {
        javafx.scene.layout.Pane pane = new javafx.scene.layout.Pane();
        pane.setPrefSize(14, 14);
        pane.setMinSize(14, 14);
        pane.setMaxSize(14, 14);

        java.util.function.Function<String, javafx.scene.shape.SVGPath> path = content -> {
            javafx.scene.shape.SVGPath svg = new javafx.scene.shape.SVGPath();
            svg.setContent(content);
            svg.setFill(javafx.scene.paint.Color.TRANSPARENT);
            svg.setStroke(javafx.scene.paint.Color.WHITE);
            svg.setStrokeWidth(1.1);
            svg.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.SQUARE);
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
    private Button createTopButton(String text, String icon) {
        // 颜色 / 悬停都由 app.css 里的 .nav-btn 控制，图标用矢量路径
        Button button = new Button(text);
        button.setGraphic(Icons.node(icon, 14, "#c7ccd4"));
        button.setContentDisplay(javafx.scene.control.ContentDisplay.LEFT);
        button.setGraphicTextGap(6);
        button.getStyleClass().add("nav-btn");
        return button;
    }

    private String inactiveButtonStyle() {
        return "-fx-background-color: transparent; -fx-text-fill: #aaa; -fx-font-size: 14px;"
                + " -fx-cursor: hand; -fx-padding: 8 18; -fx-background-radius: 10;";
    }

    private String hoverButtonStyle() {
        return "-fx-background-color: rgba(255,255,255,0.08); -fx-text-fill: white; -fx-font-size: 14px;"
                + " -fx-cursor: hand; -fx-padding: 8 18; -fx-background-radius: 10;";
    }

    private String activeButtonStyle() {
        return "-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 14px;"
                + " -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8 18; -fx-background-radius: 10;";
    }

    private boolean isActiveButton(Button button) {
        return currentPage != null && navButtons.get(currentPage) == button;
    }

    /** 页面懒加载：第一次打开才创建，减少启动开销。 */
    private Node createPage(String name) {
        File gameDir = new File(config.getGameDir());
        return switch (name) {
            case "launch" -> launchPanel;
            case "download" -> downloadPanel;
            case "instances" -> {
                instancesPanel = new InstancesPanel(gameDir, currentInstanceId(), this::selectInstance,
                        this::logMessage, config, this::switchGameDir,
                        instance -> deleteInstance(instance.id),
                        (instance, copyData) -> cloneInstance(instance.id, copyData),
                        this::checkInstance,
                        instance -> {
                            selectInstance(instance);
                            openInstanceSettings("overview");
                        },
                        this::importModpackDialog,
                        () -> switchPage("launch"));
                yield instancesPanel;
            }
            case "settings" -> {
                settingsPanel = new SettingsPanel(config, this::logMessage);
                yield settingsPanel;
            }
            case "tools" -> {
                toolsPanel = new ToolsPanel(config, this::logMessage, this::refreshInstalledVersions);
                yield toolsPanel;
            }
            case "logs" -> new LogsPanel(new File(config.getGameDir()), this::logMessage);
            default -> launchPanel;
        };
    }

    private void switchPage(String pageName) {
        Node target = pages.computeIfAbsent(pageName, this::createPage);
        if (!contentStack.getChildren().contains(target)) {
            target.setVisible(false);
            target.setOpacity(0);
            contentStack.getChildren().add(target);
        }
        String highlight = navHighlight != null ? navHighlight : pageName;
        navHighlight = null;
        applyNavHighlight(highlight);

        if (currentPage != null && currentPage.equals(pageName) && target.isVisible()) {
            return;
        }

        Node current = contentStack.getChildren().stream()
                .filter(Node::isVisible).findFirst().orElse(null);
        currentPage = pageName;

        if (current == target) {
            target.setVisible(true);
            target.setOpacity(1);
            return;
        }

        if (current != null) {
            Motion.pageOut(current, null);
        }

        if ("download".equals(pageName) && !downloadInitialized) {
            downloadInitialized = true;
            Platform.runLater(() -> {
                performSearch();
                alignDownloadNavIndicator();
            });
        }

        target.setVisible(true);
        Motion.pageIn(target);
        applyLaunchBackground();
        if ("launch".equals(pageName)) { applyAiMode(); refreshLaunchInsight(); }
        Platform.runLater(() -> {
            if (stageRef != null && stageRef.getScene() != null) {
                Theme.apply(stageRef.getScene().getRoot());
                Theme.scaleFonts(stageRef.getScene().getRoot(), config.getUiFontScale());
            }
        });
    }

    /** 启动页背景图片（个性化里设置）。 */
    private void applyLaunchBackground() {
        String path = config.getBackgroundImage();
        File file = path == null || path.isBlank() ? null : new File(path);
        applyPanelBackground(launchPanel, file);
        applyPanelBackground(downloadPanel, file);
        applyPanelBackground(settingsPanel, file);
        applyPanelBackground(toolsPanel, file);
        applyPanelBackground(instancesPanel, file);
        applyPanelBackground(instanceSettingsPanel, file);
        applyPanelBackground(communityPanel, file);
        applyPanelBackground(minecraftPanel, file);
    }

    private void applyPanelBackground(javafx.scene.Node panel, File file) {
        if (panel == null) return;
        String style = panel.getStyle() == null ? "" : panel.getStyle();
        style = style.replaceAll("(?i)-fx-background-image\\s*:[^;]*;?", "")
                .replaceAll("(?i)-fx-background-size\\s*:[^;]*;?", "")
                .replaceAll("(?i)-fx-background-position\\s*:[^;]*;?", "");
        if (file != null && file.isFile()) {
            style += " -fx-background-image: url('" + file.toURI() + "');"
                    + " -fx-background-size: cover; -fx-background-position: center center;";
        }
        panel.setStyle(style.trim());
    }

    /** 高亮当前所在的导航项（实例设置页对应「模组」或「设置」）。 */
    private void applyNavHighlight(String highlight) {
        for (Map.Entry<String, Button> entry : navButtons.entrySet()) {
            Button button = entry.getValue();
            boolean active = entry.getKey().equals(highlight);
            button.getStyleClass().remove("nav-btn-active");
            if (active) button.getStyleClass().add("nav-btn-active");
            if (button.getUserData() instanceof String iconName) {
                button.setGraphic(Icons.node(iconName, 14, active ? "#5eead4" : "#b8c7d9"));
            }
        }
        moveNavIndicator(highlight);
    }

    /** 顶部导航的蓝色滑块，跟着当前按钮平滑移动。 */
    private void moveNavIndicator(String key) {
        if (navIndicator == null || navBox == null) return;
        Button active = navButtons.get(key);
        if (active == null) return;
        Platform.runLater(() -> {
            double targetX = active.getLayoutX();
            double targetWidth = active.getWidth();
            Timeline timeline = new Timeline(
                    new KeyFrame(Duration.millis(260),
                            new KeyValue(navIndicator.translateXProperty(), targetX, Motion.SPRING),
                            new KeyValue(navIndicator.widthProperty(), targetWidth, Motion.SPRING)));
            timeline.play();
        });
    }

    /** 下载页左侧竖条的初始位置对齐到「整合包」。 */
    private void alignDownloadNavIndicator() {
        if (downloadNavIndicator == null || downloadNavBox == null) return;
        for (javafx.scene.Node node : downloadNavBox.getChildren()) {
            if (!(node instanceof HBox item)) continue;
            if (item.getChildren().size() < 2) continue;
            if (item.getChildren().get(1) instanceof Label label && "整合包".equals(label.getText())) {
                if (item.getHeight() <= 0) {
                    // 布局还没完成，下一帧再对齐一次
                    Platform.runLater(this::alignDownloadNavIndicator);
                    return;
                }
                downloadNavIndicator.setHeight(Math.max(24, item.getHeight() - 8));
                downloadNavIndicator.setTranslateY(centeredIndicatorY(item));
                return;
            }
        }
    }

    /** 竖条在导航项里垂直居中的 Y，避免压到相邻选项。 */
    private double centeredIndicatorY(HBox item) {
        return item.getLayoutY() + (item.getHeight() - downloadNavIndicator.getHeight()) / 2.0;
    }

    // ================== 启动面板 ==================
    private VBox createLaunchPanel() {
        VBox panel = new VBox(20);
        panel.setPadding(new Insets(25));

        VBox userArea = new VBox(12);
        userArea.setPrefWidth(346);
        userArea.setAlignment(Pos.TOP_CENTER);
        userArea.setStyle("-fx-background-color: -app-card;"
                + " -fx-background-radius: 20; -fx-padding: 20;"
                + " -fx-border-color: -app-border-2; -fx-border-width: 1; -fx-border-radius: 20;"
                + " -fx-effect: dropshadow(gaussian, rgba(15,23,42,0.12), 24, 0.12, 0, 8);");

        avatarImageView = new ImageView();
        avatarImageView.setFitWidth(104);
        avatarImageView.setFitHeight(104);
        avatarImageView.setSmooth(false);
        Rectangle clip = new Rectangle(104, 104);
        clip.setArcWidth(14); clip.setArcHeight(14);
        avatarImageView.setClip(clip);
        StackPane avatarPane = new StackPane(Icons.node("user", 42, "#6b7280"), avatarImageView);
        avatarPane.setStyle("-fx-background-color: linear-gradient(to bottom right, rgba(255,255,255,0.10), rgba(255,255,255,0.03));"
                + " -fx-background-radius: 16; -fx-border-color: rgba(156,163,175,0.55); -fx-border-radius: 16;"
                + " -fx-border-width: 1.5; -fx-padding: 4;"
                + " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 18, 0.15, 0, 6);");
        avatarPane.setPrefSize(112, 112);
        avatarPane.setMinSize(112, 112);
        avatarPane.setMaxSize(112, 112);
        javafx.scene.control.Tooltip skinTip = new javafx.scene.control.Tooltip("把皮肤 PNG 拖到这里预览");
        javafx.scene.control.Tooltip.install(avatarPane, skinTip);
        avatarPane.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles() || event.getDragboard().hasUrl()
                    || event.getDragboard().hasString()) {
                event.acceptTransferModes(javafx.scene.input.TransferMode.COPY);
            }
            event.consume();
        });
        avatarPane.setOnDragDropped(event -> {
            javafx.scene.input.Dragboard board = event.getDragboard();
            try {
                javafx.scene.image.Image skin = null;
                if (board.hasFiles()) {
                    for (java.io.File file : board.getFiles()) {
                        String name = file.getName().toLowerCase(java.util.Locale.ROOT);
                        if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg")) {
                            skin = new javafx.scene.image.Image(file.toURI().toString(), true);
                            break;
                        }
                    }
                } else if (board.hasUrl()) {
                    skin = new javafx.scene.image.Image(board.getUrl(), true);
                }
                if (skin != null && !skin.isError()) {
                    avatarImageView.setImage(skin);
                    Toast.ok("皮肤已加载预览");
                    logArea.appendText("🧵 已载入皮肤预览\n");
                }
            } catch (Exception ex) {
                Toast.error("皮肤图片读取失败");
            }
            event.setDropCompleted(true);
            event.consume();
        });

        usernameLabel = new Label("未登录");
        usernameLabel.setStyle("-fx-font-size: 27px; -fx-font-weight: bold; -fx-text-fill: -app-text;");
        userTypeLabel = new Label("请在下方登录");
        userTypeLabel.setStyle("-fx-font-size: 12.5px; -fx-text-fill: -app-text-dim-2;");
        uuidLabel = new Label("");
        uuidLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #5c6572;");

        loginCard = new VBox(8);
        loginCard.setPadding(new Insets(12));
        loginCard.setStyle("-fx-background-color: rgba(255,255,255,0.035); -fx-background-radius: 12;"
                + " -fx-border-color: rgba(255,255,255,0.05); -fx-border-radius: 12;");

        loginTypeCombo = new ComboBox<>();
        loginTypeCombo.getItems().addAll("离线登录", "第三方外置登录", "正版登录 (微软)");
        loginTypeCombo.setValue("离线登录");
        loginTypeComboRef = loginTypeCombo;
        loginTypeCombo.setMaxWidth(Double.MAX_VALUE);
        loginTypeCombo.setButtonCell(createDarkListCell());

        serverField = new ComboBox<>();
        serverField.setEditable(true);
        serverField.setPromptText("验证服务器地址");
        serverField.setMaxWidth(Double.MAX_VALUE);
        serverField.setPrefHeight(34);
        serverField.setVisible(false);
        serverField.setManaged(false);
        serverField.getItems().addAll("https://littleskin.cn/api/yggdrasil", "https://littleskin.cn");
        serverFieldRef = serverField;
        serverField.setButtonCell(createDarkListCell());

        usernameField = new TextField();
        usernameField.setPromptText("玩家名/邮箱");
        usernameField.setPrefHeight(34);

        passwordField = new PasswordField();
        passwordField.setPromptText("密码");
        passwordField.setPrefHeight(34);
        passwordField.setVisible(false);
        passwordField.setManaged(false);

        loginBtn = new Button();
        loginBtnLabel = new Label("登录");
        loginBtnLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-font-size: 12.5px;");
        loginBtn.setGraphic(loginBtnLabel);
        loginBtn.setDefaultButton(true);
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        loginBtn.setStyle("-fx-background-color: linear-gradient(to bottom, #e62937, #9c101b);"
                + " -fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-cursor: hand;"
                + " -fx-background-radius: 9; -fx-padding: 8;");
        Motion.press(loginBtn);
        loginBtn.setOnMouseEntered(e -> loginBtn.setStyle("-fx-background-color: linear-gradient(to bottom, #ff4b58, #b51422);"
                + " -fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-cursor: hand;"
                + " -fx-background-radius: 9; -fx-padding: 8;"));
        loginBtn.setOnMouseExited(e -> loginBtn.setStyle("-fx-background-color: linear-gradient(to bottom, #e62937, #9c101b);"
                + " -fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-cursor: hand;"
                + " -fx-background-radius: 9; -fx-padding: 8;"));

        Label msHint = new Label("将打开浏览器并显示设备代码；需要已购买 Minecraft。");
        msHint.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 10.5px;");
        msHint.setWrapText(true);
        msHint.setVisible(false);
        msHint.setManaged(false);

        loginTypeCombo.setOnAction(e -> {
            String selected = loginTypeCombo.getValue();
            boolean is3rd = "第三方外置登录".equals(selected);
            boolean isMs = "正版登录 (微软)".equals(selected);
            serverField.setVisible(is3rd); serverField.setManaged(is3rd);
            passwordField.setVisible(is3rd); passwordField.setManaged(is3rd);
            usernameField.setVisible(!isMs); usernameField.setManaged(!isMs);
            msHint.setVisible(isMs); msHint.setManaged(isMs);
            loginBtnLabel.setText(isMs ? "开始微软授权" : "登录");
        });

        HBox credentialsRow = new HBox(8, usernameField, passwordField);
        HBox.setHgrow(usernameField, Priority.ALWAYS);
        HBox.setHgrow(passwordField, Priority.ALWAYS);
        usernameField.setMaxWidth(Double.MAX_VALUE);
        passwordField.setMaxWidth(Double.MAX_VALUE);
        loginCard.getChildren().addAll(loginTypeCombo, serverField, credentialsRow, msHint, loginBtn);

        logoutButton = new Button("退出登录");
        logoutButton.setStyle("-fx-background-color: transparent; -fx-text-fill: -app-text-muted;"
                + " -fx-font-size: 11px; -fx-cursor: hand; -fx-padding: 0;");
        logoutButton.setVisible(false);
        logoutButton.setManaged(false);
        logoutButton.setOnAction(e -> logout());

        playerInfoBox = new VBox(7);
        playerInfoBox.setPadding(new Insets(12));
        playerInfoBox.setStyle("-fx-background-color: rgba(255,255,255,0.028); -fx-background-radius: 10;"
                + " -fx-border-color: rgba(255,255,255,0.05); -fx-border-radius: 10;");
        playerInfoType = new Label("—");
        playerInfoUuid = new Label("—");
        playerInfoServer = new Label("—");
        playerInfoInstance = new Label("—");
        playerInfoPlaytime = new Label("—");
        playerInfoBox.getChildren().addAll(
                infoCaption("账号信息"),
                infoLine("类型", playerInfoType),
                infoLine("UUID", playerInfoUuid),
                infoLine("验证服务器", playerInfoServer),
                infoLine("当前实例", playerInfoInstance),
                infoLine("游戏时长", playerInfoPlaytime));
        playerInfoBox.setVisible(false);
        playerInfoBox.setManaged(false);

        versionComboBox = new ComboBox<>();
        versionComboBox.setPromptText("加载版本中...");
        versionComboBox.setMaxWidth(Double.MAX_VALUE);
        versionComboBox.setPrefHeight(36);
        versionComboBox.setMinHeight(36);
        versionComboBox.setStyle("-fx-padding: 5;");
        versionComboBox.setConverter(new StringConverter<MinecraftVersion>() {
            public String toString(MinecraftVersion o) { return o == null ? "" : o.getId(); }
            public MinecraftVersion fromString(String s) { return null; }
        });
        versionComboBox.setButtonCell(new ListCell<MinecraftVersion>() {
            protected void updateItem(MinecraftVersion item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else { setText(item.getId()); setTextFill(Color.web(Theme.isLight() ? "#1f2937" : "#E0E0E0")); }
            }
        });

        launchBtn = new Button();
        launchMainLabel = new Label("★ 启动游戏");
        launchMainLabel.setStyle("-fx-text-fill: -app-text; -fx-font-size: 18px; -fx-font-weight: bold;");
        launchVersionLabel = new Label("");
        launchVersionLabel.setStyle("-fx-text-fill: -app-launch-text-dim; -fx-font-size: 11px;");
        VBox launchBox = new VBox(1, launchMainLabel, launchVersionLabel);
        launchBox.setAlignment(Pos.CENTER);
        launchBtn.setGraphic(launchBox);
        launchBtn.setMaxWidth(Double.MAX_VALUE);
        launchBtn.setPrefHeight(56);
        launchBtn.setMinHeight(56);
        launchMainLabel.setStyle("-fx-text-fill: -app-launch-text; -fx-font-size: 19px; -fx-font-weight: bold;");
        launchVersionLabel.setStyle("-fx-text-fill: -app-launch-text-dim; -fx-font-size: 11px;");
        String launchNormal = "-fx-background-color: linear-gradient(to bottom, -app-launch-top, -app-launch-bottom);"
                + " -fx-text-fill: -app-text; -fx-font-size: 19px; -fx-font-weight: bold;"
                + " -fx-background-radius: 13; -fx-border-radius: 13;"
                + " -fx-border-color: -app-launch-border; -fx-border-width: 1.2; -fx-cursor: hand;";
        String launchHover = "-fx-background-color: linear-gradient(to bottom, -app-launch-top-hover, -app-launch-bottom-hover);"
                + " -fx-text-fill: -app-text; -fx-font-size: 19px; -fx-font-weight: bold;"
                + " -fx-background-radius: 13; -fx-border-radius: 13;"
                + " -fx-border-color: -app-launch-border-hover; -fx-border-width: 1.2; -fx-cursor: hand;";
        launchBtn.setStyle(launchNormal);
        javafx.scene.effect.DropShadow launchGlow = new javafx.scene.effect.DropShadow(22, Color.web("#9ca3af", 0.28));
        launchBtn.setEffect(launchGlow);
        javafx.animation.Timeline glowPulse = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.ZERO,
                        new javafx.animation.KeyValue(launchGlow.radiusProperty(), 16)),
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(2.8),
                        new javafx.animation.KeyValue(launchGlow.radiusProperty(), 30)));
        glowPulse.setAutoReverse(true);
        glowPulse.setCycleCount(javafx.animation.Animation.INDEFINITE);
        if (Motion.isEnabled()) glowPulse.play();
        Motion.press(launchBtn);
        Motion.hoverScale(launchBtn, 1.014);
        launchBtn.setOnMouseEntered(e -> launchBtn.setStyle(launchHover));
        launchBtn.setOnMouseExited(e -> launchBtn.setStyle(launchNormal));

        // 顶部资料/登录区可滚动，启动按钮固定在底部，切换登录方式时整体比例不变。

        // 与 PCL 启动页一致：启动按钮下方是「实例选择 / 实例设置」两个描边按钮
        Button instSelect = new Button("实例选择");
        Button instSettings = new Button("实例设置");
        for (Button b : new Button[]{instSelect, instSettings}) {
            b.setMaxWidth(Double.MAX_VALUE);
            b.setStyle("-fx-background-color: transparent; -fx-text-fill: -app-text-2; -fx-font-size: 13px;"
                    + " -fx-border-color: #4b5563; -fx-border-radius: 4; -fx-background-radius: 4;"
                    + " -fx-padding: 9; -fx-cursor: hand;");
            HBox.setHgrow(b, Priority.ALWAYS);
            Motion.press(b);
            b.setOnMouseEntered(e -> b.setStyle("-fx-background-color: rgba(255,255,255,0.05);"
                    + " -fx-text-fill: white; -fx-font-size: 13px;"
                    + " -fx-border-color: -app-text-muted; -fx-border-radius: 4; -fx-background-radius: 4;"
                    + " -fx-padding: 9; -fx-cursor: hand;"));
            b.setOnMouseExited(e -> b.setStyle("-fx-background-color: transparent;"
                    + " -fx-text-fill: -app-text-2; -fx-font-size: 13px;"
                    + " -fx-border-color: #4b5563; -fx-border-radius: 4; -fx-background-radius: 4;"
                    + " -fx-padding: 9; -fx-cursor: hand;"));
        }
        instSelect.setOnAction(e -> switchPage("instances"));
        instSettings.setOnAction(e -> openInstanceSettings("overview"));
        HBox bottomRow = new HBox(10, instSelect, instSettings);
        bottomRow.setAlignment(Pos.CENTER);

        // 版本选择交给「实例选择」页，启动页只显示当前版本（与 PCL 一致）
        versionComboBox.setVisible(false);
        versionComboBox.setManaged(false);

        Region profileSpacer = new Region();
        VBox.setVgrow(profileSpacer, Priority.ALWAYS);
        userArea.getChildren().addAll(avatarPane, usernameLabel, userTypeLabel, uuidLabel,
                loginCard, logoutButton, playerInfoBox, profileSpacer, launchBtn, bottomRow);

        VBox logContainer = new VBox(4);
        logContainer.setStyle("-fx-background-color: -app-card;"
                + " -fx-background-radius: 20; -fx-padding: 6;"
                + " -fx-border-color: -app-border-2; -fx-border-width: 1; -fx-border-radius: 20;"
                + " -fx-effect: dropshadow(gaussian, rgba(15,23,42,0.12), 24, 0.12, 0, 8);");
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setWrapText(true);
        logArea.setStyle("-fx-control-inner-background: -app-bg-2; -fx-text-fill: -app-text-dim;"
                + " -fx-font-family: 'Cascadia Mono', 'Consolas', monospace; -fx-background-radius: 11;"
                + " -fx-border-color: rgba(255,255,255,0.05); -fx-border-radius: 11; -fx-font-size: 12.5px;");
        VBox.setVgrow(logArea, Priority.ALWAYS);

        Label downloadTitle = new Label("下载任务");
        downloadTitle.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11px;");
        downloadFileLabel = new Label("空闲");
        downloadFileLabel.setStyle("-fx-text-fill: -app-text-2; -fx-font-size: 12px;");
        downloadStatsLabel = new Label("—");
        downloadStatsLabel.setStyle("-fx-text-fill: -app-text-dim-2; -fx-font-size: 11px;");

        downloadProgress = new ProgressBar(0);
        downloadProgress.setMaxWidth(Double.MAX_VALUE);
        downloadProgress.setPrefHeight(6);

        downloadProgress.setVisible(false);
        downloadProgress.setManaged(false);
        downloadStatsLabel.setVisible(false);
        downloadStatsLabel.setManaged(false);

        geekTelemetry = new Label("[SYS] INIT");
        geekTelemetry.setStyle("-fx-text-fill: #ff8a72; -fx-font-size: 8.5px;"
                + " -fx-font-family: 'Cascadia Mono', 'Consolas', monospace;");
        Region telemetrySpacer = new Region();
        HBox.setHgrow(telemetrySpacer, Priority.ALWAYS);
        HBox downloadHeader = new HBox(8, downloadTitle, downloadFileLabel, telemetrySpacer, geekTelemetry);
        downloadHeader.setAlignment(Pos.CENTER_LEFT);
        VBox downloadBar = new VBox(4, downloadHeader, downloadProgress, downloadStatsLabel);
        downloadBarPanel = downloadBar;
        downloadBar.setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-background-radius: 10;"
                + " -fx-border-color: rgba(255,255,255,0.06); -fx-border-radius: 10; -fx-padding: 5 8;");

        Button refreshFab = new Button();
        refreshFab.setGraphic(Icons.node("refresh", 16, "#9ca3af"));
        refreshFab.setStyle("-fx-background-color: linear-gradient(to bottom right, #d1d5db, #6b7280);"
                + " -fx-text-fill: #111827; -fx-font-size: 17px; -fx-background-radius: 20;"
                + " -fx-min-width: 40; -fx-min-height: 40; -fx-cursor: hand;");
        Motion.press(refreshFab);
        refreshFab.setOnMouseEntered(e -> refreshFab.setStyle("-fx-background-color: linear-gradient(to bottom right, #e5e7eb, #9ca3af);"
                + " -fx-text-fill: #111827; -fx-font-size: 17px; -fx-background-radius: 20;"
                + " -fx-min-width: 40; -fx-min-height: 40; -fx-cursor: hand;"));
        refreshFab.setOnMouseExited(e -> refreshFab.setStyle("-fx-background-color: linear-gradient(to bottom right, #d1d5db, #6b7280);"
                + " -fx-text-fill: #111827; -fx-font-size: 17px; -fx-background-radius: 20;"
                + " -fx-min-width: 40; -fx-min-height: 40; -fx-cursor: hand;"));
        refreshFab.setOnAction(e -> reloadVersionsAsync());

        StackPane logPane = new StackPane(logArea, refreshFab);
        StackPane.setAlignment(refreshFab, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(refreshFab, new Insets(0, 12, 12, 0));
        VBox.setVgrow(logPane, Priority.ALWAYS);
        logPane.setMinHeight(0);
        logArea.setMinHeight(0);
        Button clearLog = new Button("清空日志");
        clearLog.setOnAction(e -> logArea.clear());
        Button copyLog = new Button("复制日志");
        copyLog.setOnAction(e -> {
            javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
            content.putString(logArea.getText());
            javafx.scene.input.Clipboard.getSystemClipboard().setContent(content);
            Toast.ok("日志已复制");
        });
        Button openLogDir = new Button("日志目录");
        openLogDir.setOnAction(e -> openDir(new File(config.getGameDir(), "logs")));
        Button shortcuts = new Button("快捷键");
        shortcuts.setOnAction(e -> {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.INFORMATION);
            alert.setTitle("快捷键");
            alert.setHeaderText("Neko Craft Launcher 快捷键");
            alert.setContentText("F1  启动页\nF2  实例选择\nF3  实例设置（概览）\nF4  实例设置 · 模组\n"
                    + "F5  下载\nF6  设置\nF7  工具\nF8  设置 · 查看日志\nEsc 关闭弹窗");
            alert.showAndWait();
        });
        Label promise = new Label("NEKO · 为爱发电");
        promise.setStyle("-fx-text-fill: -app-accent; -fx-font-size: 9.5px; -fx-font-weight: bold;");
        Region promiseSpacer = new Region();
        HBox.setHgrow(promiseSpacer, Priority.ALWAYS);
        HBox logActions = new HBox(8, clearLog, copyLog, openLogDir, shortcuts, promiseSpacer, promise);
        logActions.setAlignment(Pos.CENTER_LEFT);
        for (Button action : new Button[]{clearLog, copyLog, openLogDir}) {
            action.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-text-fill: -app-text-dim;"
                    + " -fx-background-radius: 6; -fx-padding: 3 10; -fx-font-size: 11px; -fx-cursor: hand;");
        }
        HBox commandBar = createCommandBar();
        HBox insightBar = buildLaunchInsightBar();
        logContainer.getChildren().addAll(insightBar, logPane, commandBar, logActions, downloadBar);
        HBox.setHgrow(logContainer, Priority.ALWAYS);

        HBox mainContent = new HBox(25, userArea, logContainer);
        VBox.setVgrow(mainContent, Priority.ALWAYS);
        ModuleHeader launchHeader = new ModuleHeader("01 / NEKO", "启动",
                "猫娘助手 · 今天也要一起冒险", "play", ModuleHeader.Badge.CIRCLE, "为爱发电 · 永远免费更新");
        panel.getChildren().addAll(launchHeader, mainContent);

        loginBtn.setOnAction(e -> handleLogin(loginTypeCombo, serverField, usernameField, passwordField, loginBtn));
        versionComboBox.valueProperty().addListener((obs, old, value) -> {
            launchVersionLabel.setText(value == null ? "" : value.getId());
            if (playerInfoInstance != null) playerInfoInstance.setText(value == null ? "未选择" : value.getId());
            if (value != null && !suppressInstancePersist) {
                userPickedInstance = true;
                config.setLastInstance(value.getId());
                config.save();
            }
        });
        launchBtn.setOnAction(e -> handleLaunch());

        // 首屏两张卡片轻微错峰入场
        Motion.stagger(List.of(userArea, logContainer));

        return panel;
    }

    /** 日志下方的命令栏：启动器命令 + 以 / 开头发送到游戏。 */
    /** 启动页顶部顾问条：用本地启动历史给出稳定性判断。 */
    private HBox buildLaunchInsightBar() {
        StackPane icon = Icons.node("sparkles", 14, "#9ca3af");
        Label title = new Label("启动顾问");
        title.setStyle("-fx-text-fill: -app-text-2; -fx-font-size: 11px; -fx-font-weight: bold;");
        insightTip = new Label();
        insightTip.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 10.5px;");
        insightTip.setWrapText(false);
        insightMetrics = new Label();
        insightMetrics.setStyle("-fx-text-fill: -app-text-dim-3; -fx-font-size: 10px;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button history = new Button("记录");
        history.setStyle("-fx-background-color: rgba(255,255,255,0.06); -fx-text-fill: -app-text-dim;"
                + " -fx-background-radius: 7; -fx-padding: 3 10; -fx-font-size: 10.5px; -fx-cursor: hand;");
        history.setOnAction(e -> {
            switchPage("tools");
            if (toolsPanel != null) toolsPanel.openHistoryTab();
        });
        HBox bar = new HBox(8, icon, title, insightTip, spacer, insightMetrics, history);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(5, 12, 5, 12));
        bar.setStyle("-fx-background-color: linear-gradient(to right, rgba(255,255,255,0.055), rgba(255,255,255,0.018));"
                + " -fx-background-radius: 11; -fx-border-color: rgba(255,255,255,0.07); -fx-border-radius: 11;");
        refreshLaunchInsight();
        return bar;
    }

    private void refreshLaunchInsight() {
        if (insightTip == null || insightMetrics == null) return;
        File dir = new File(config.getGameDir());
        java.util.List<LaunchHistory.Entry> entries = LaunchHistory.recent(dir);
        if (entries.isEmpty()) {
            insightTip.setText("还没有启动记录，启动一次后会生成稳定性画像");
            insightMetrics.setText("");
            return;
        }
        int total = entries.size();
        int crashes = 0;
        long prepSum = 0;
        int prepCount = 0;
        for (LaunchHistory.Entry entry : entries) {
            if (entry.crash()) crashes++;
            if (entry.prepMs() > 0) {
                prepSum += entry.prepMs();
                prepCount++;
            }
        }
        int rate = Math.round((total - crashes) * 100f / total);
        long avgPrep = prepCount == 0 ? 0 : prepSum / prepCount;
        long lastPrep = entries.get(0).prepMs();
        insightMetrics.setText("上次准备 " + (lastPrep <= 0 ? "—"
                        : String.format(java.util.Locale.ROOT, "%.1fs", lastPrep / 1000.0))
                + " · 成功率 " + rate + "%");
        if (rate < 80) {
            insightTip.setText("最近异常率偏高，建议先跑一次崩溃诊断");
        } else if (lastPrep > 5000) {
            insightTip.setText("启动准备偏慢，可检查 Java 与磁盘占用");
        } else if (avgPrep > 0) {
            insightTip.setText("运行稳定，平均准备 " + String.format(java.util.Locale.ROOT, "%.1fs", avgPrep / 1000.0));
        } else {
            insightTip.setText("运行稳定，继续游戏吧");
        }
    }

    private HBox createCommandBar() {
        commandPrefix = new StackPane(Icons.node("terminal", 14, "#9ca3af"));
        commandPrefix.setPrefSize(18, 18);
        commandPrefix.setMinSize(18, 18);
        commandPrefix.setMaxSize(18, 18);
        StackPane prefix = commandPrefix;
        commandField = new TextField();
        TextField field = commandField;
        field.setPromptText("命令：help 查看启动器命令；/give @s diamond 64 发送到游戏");
        field.setPrefHeight(32);
        field.setStyle("-fx-background-color: -app-input; -fx-text-fill: -app-text;"
                + " -fx-prompt-text-fill: -app-text-muted; -fx-background-radius: 8;"
                + " -fx-border-color: -app-border-3; -fx-border-radius: 8;"
                + " -fx-padding: 6 10; -fx-font-size: 12px; -fx-font-family: 'Cascadia Mono', 'Consolas', monospace;");
        HBox.setHgrow(field, Priority.ALWAYS);

        Button send = new Button("发送");
        send.setStyle("-fx-background-color: -app-row3; -fx-text-fill: -app-text-2; -fx-background-radius: 8;"
                + " -fx-padding: 5 14; -fx-font-size: 12px; -fx-cursor: hand;");
        Motion.press(send);

        Runnable submit = () -> {
            String text = field.getText();
            if (text == null || text.isBlank()) return;
            field.clear();
            if (config.isAiEnabled()) {
                sendAiMessage(text);
            } else {
                executeCommand(text);
            }
        };
        Popup completePopup = new Popup();
        completePopup.setAutoHide(true);
        completePopup.setAutoFix(true);
        ListView<CommandCompleter.Completion> completeList = new ListView<>();
        completeList.setMaxHeight(190);
        completeList.setFocusTraversable(false);
        completeList.setStyle("-fx-background-color: #1b1e24; -fx-control-inner-background: #1b1e24;"
                + " -fx-background-radius: 10; -fx-border-color: #333a46; -fx-border-radius: 10;");
        completeList.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(CommandCompleter.Completion item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                Label value = new Label(item.value());
                value.setStyle("-fx-text-fill: #e8eaee; -fx-font-size: 12px;"
                        + " -fx-font-family: 'Cascadia Mono', 'Consolas', monospace;");
                Label desc = new Label(item.description());
                desc.setStyle("-fx-text-fill: #7b8494; -fx-font-size: 10px;");
                HBox box = new HBox(10, value, desc);
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box);
            }
        });
        completePopup.getContent().add(completeList);

        java.util.function.Consumer<CommandCompleter.Completion> applyCompletion = completion -> {
            field.setText(CommandCompleter.apply(field.getText(), completion.value()));
            field.positionCaret(field.getText().length());
            completePopup.hide();
        };
        java.util.function.Consumer<Integer> moveSelection = step -> {
            int size = completeList.getItems().size();
            if (size == 0) return;
            int index = completeList.getSelectionModel().getSelectedIndex();
            index = Math.floorMod(index + step, size);
            completeList.getSelectionModel().select(index);
            completeList.scrollTo(index);
        };

        field.setOnAction(e -> submit.run());
        send.setOnAction(e -> submit.run());
        field.textProperty().addListener((o, a, b) -> {
            if (completePopup.isShowing()) completePopup.hide();
        });
        completeList.setOnMouseClicked(e -> {
            CommandCompleter.Completion item = completeList.getSelectionModel().getSelectedItem();
            if (item != null) applyCompletion.accept(item);
        });
        field.setOnKeyPressed(e -> {
            if (config.isAiEnabled()) return;
            javafx.scene.input.KeyCode code = e.getCode();
            if (code == javafx.scene.input.KeyCode.TAB) {
                if (completePopup.isShowing()) {
                    moveSelection.accept(1);
                    e.consume();
                    return;
                }
                List<CommandCompleter.Completion> items = CommandCompleter.complete(
                        field.getText(), new File(config.getGameDir()));
                if (items.isEmpty()) return;
                if (items.size() == 1) {
                    applyCompletion.accept(items.get(0));
                    e.consume();
                    return;
                }
                completeList.getItems().setAll(items);
                completeList.setPrefWidth(Math.max(340, field.getWidth()));
                completeList.setPrefHeight(Math.min(190, items.size() * 28 + 8));
                completeList.getSelectionModel().select(0);
                javafx.geometry.Bounds bounds = field.localToScreen(field.getBoundsInLocal());
                if (bounds != null) {
                    completePopup.show(field, bounds.getMinX(), bounds.getMaxY() + 4);
                }
                e.consume();
                return;
            }
            if (code == javafx.scene.input.KeyCode.ESCAPE && completePopup.isShowing()) {
                completePopup.hide();
                e.consume();
                return;
            }
            if (completePopup.isShowing()
                    && (code == javafx.scene.input.KeyCode.UP || code == javafx.scene.input.KeyCode.DOWN)) {
                moveSelection.accept(code == javafx.scene.input.KeyCode.DOWN ? 1 : -1);
                e.consume();
                return;
            }
            if (completePopup.isShowing() && code == javafx.scene.input.KeyCode.ENTER) {
                CommandCompleter.Completion item = completeList.getSelectionModel().getSelectedItem();
                if (item != null) applyCompletion.accept(item);
                e.consume();
                return;
            }
            if (commandHistory.isEmpty()) return;
            if (code == javafx.scene.input.KeyCode.UP) {
                commandHistoryIndex = Math.max(0, commandHistoryIndex - 1);
                field.setText(commandHistory.get(commandHistoryIndex));
                field.positionCaret(field.getText().length());
                e.consume();
            } else if (code == javafx.scene.input.KeyCode.DOWN) {
                commandHistoryIndex = Math.min(commandHistory.size(), commandHistoryIndex + 1);
                field.setText(commandHistoryIndex >= commandHistory.size()
                        ? "" : commandHistory.get(commandHistoryIndex));
                field.positionCaret(field.getText().length());
                e.consume();
            }
        });

        commandAiToggle = new Button();
        commandAiToggle.setMinWidth(Region.USE_PREF_SIZE);
        commandAiToggle.setTooltip(new javafx.scene.control.Tooltip(
                "开启后命令栏内容发给 DeepSeek；关闭后作为启动器 / 游戏命令"));
        commandAiToggle.setOnAction(e -> {
            config.setAiEnabled(!config.isAiEnabled());
            config.save();
            applyAiMode();
            logArea.appendText(config.isAiEnabled()
                    ? "🧠 DeepSeek AI 已开启：命令栏输入将发给模型\n"
                    : "⌘ DeepSeek AI 已关闭：命令栏恢复为启动器 / 游戏命令\n");
        });

        HBox bar = new HBox(8, commandAiToggle, prefix, field, send);
        bar.setAlignment(Pos.CENTER_LEFT);
        applyAiMode();
        return bar;
    }

    /** 根据配置刷新命令栏的 AI 状态。 */
    private void applyAiMode() {
        if (commandAiToggle == null || commandField == null) return;
        boolean on = config.isAiEnabled();
        commandAiToggle.setText(on ? "AI 开" : "AI 关");
        commandAiToggle.setGraphic(Icons.node(on ? "sparkles" : "terminal", 12,
                on ? "#111827" : "#9ca3af"));
        commandAiToggle.setContentDisplay(javafx.scene.control.ContentDisplay.LEFT);
        commandAiToggle.setGraphicTextGap(5);
        commandAiToggle.setStyle(on
                ? "-fx-background-color: #9ca3af; -fx-text-fill: #111827; -fx-font-weight: bold;"
                + " -fx-background-radius: 8; -fx-padding: 5 10; -fx-font-size: 11px; -fx-cursor: hand;"
                : "-fx-background-color: transparent; -fx-text-fill: -app-text-2;"
                + " -fx-border-color: -app-border-3; -fx-border-radius: 8; -fx-background-radius: 8;"
                + " -fx-padding: 5 10; -fx-font-size: 11px; -fx-cursor: hand;");
        if (commandPrefix != null) {
            commandPrefix.getChildren().setAll(Icons.node(on ? "sparkles" : "terminal", 14,
                    on ? "#d1d5db" : "#9ca3af"));
        }
        commandField.setPromptText(on
                ? (config.getAiApiKey().isBlank()
                    ? "本地免费助手：问启动器 / MC 问题；设置 → AI 可填 Key"
                    : "问 DeepSeek：例如「这个报错怎么解决？」")
                : "命令：help 查看启动器命令；/give @s diamond 64 发送到游戏");
    }

    private void sendAiMessage(String text) {
        if (aiBusy) {
            logArea.appendText("🧠 上一条还在回答，请稍候。\n");
            return;
        }
        if (config.getAiApiKey().isBlank()) {
            logArea.appendText("🧠 你：" + text + "\n");
            aiHistory.add(new DeepSeekClient.Message("user", text));
            trimAiHistory();
            String reply = LocalAiAssistant.answer(text, new File(config.getGameDir()));
            aiHistory.add(new DeepSeekClient.Message("assistant", reply));
            trimAiHistory();
            logArea.appendText("🆓 本地免费助手（未配置 Key）：\n" + reply + "\n");
            logArea.setScrollTop(Double.MAX_VALUE);
            return;
        }

        aiBusy = true;
        logArea.appendText("🧠 你：" + text + "\n");
        logArea.appendText("🤖 DeepSeek 正在思考…\n");
        aiHistory.add(new DeepSeekClient.Message("user", text));
        trimAiHistory();

        java.util.List<DeepSeekClient.Message> messages = new java.util.ArrayList<>();
        messages.add(new DeepSeekClient.Message("system", aiSystemPrompt()));
        messages.addAll(aiHistory);
        String key = config.getAiApiKey();
        String base = config.getAiBaseUrl();
        String model = config.getAiModel();

        java.util.concurrent.CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return DeepSeekClient.chat(key, base, model, messages);
                    } catch (Exception ex) {
                        throw new java.util.concurrent.CompletionException(ex);
                    }
                })
                .thenAccept(reply -> Platform.runLater(() -> {
                    aiBusy = false;
                    aiHistory.add(new DeepSeekClient.Message("assistant", reply));
                    trimAiHistory();
                    logArea.appendText("🤖 DeepSeek：\n" + reply + "\n");
                    logArea.setScrollTop(Double.MAX_VALUE);
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        aiBusy = false;
                        Throwable cause = ex;
                        while (cause.getCause() != null) cause = cause.getCause();
                        logArea.appendText("❌ DeepSeek 请求失败：" + cause.getMessage() + "\n");
                        logArea.setScrollTop(Double.MAX_VALUE);
                    });
                    return null;
                });
    }

    private String aiSystemPrompt() {
        int versions = 0;
        try {
            versions = VersionProfile.installedIds(new File(config.getGameDir())).size();
        } catch (Exception ignored) {
        }
        return "你是「Neko Craft Launcher」内置的 AI 助手，用中文简洁回答，优先给出可操作步骤。"
                + "当前游戏目录：" + config.getGameDir()
                + "；已安装实例数：" + versions
                + "；当前实例：" + currentInstanceId() + "。"
                + "可以回答 Minecraft 模组、报错、启动器使用等问题；不确定时明确说明。";
    }

    private void trimAiHistory() {
        while (aiHistory.size() > 10) aiHistory.remove(0);
    }

    private StackPane commandPaletteOverlay;

    private record PaletteAction(String icon, String title, String hint, Runnable action) {
    }

    /** 命令面板：Ctrl+K 打开，输入过滤，上下选择，回车执行。 */
    private void showCommandPalette() {
        if (commandPaletteOverlay != null || stageRef == null || stageRef.getScene() == null) return;
        if (!(stageRef.getScene().getRoot() instanceof StackPane layer)) return;

        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(4,7,12,0.62);");
        StackPane.setAlignment(overlay, Pos.CENTER);

        VBox card = new VBox(10);
        card.setPrefWidth(560);
        card.setMaxWidth(560);
        card.setMaxHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        card.setPadding(new Insets(14));
        card.setStyle("-fx-background-color: linear-gradient(to bottom, -app-card-top, -app-card-alt2);"
                + " -fx-background-radius: 18; -fx-border-color: -app-border-soft; -fx-border-radius: 18;"
                + " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.45), 40, 0.2, 0, 12);");

        TextField search = new TextField();
        search.setPromptText("搜索命令、页面、操作…");
        search.setPrefHeight(40);
        search.setStyle("-fx-background-color: -app-input; -fx-text-fill: -app-text; -fx-prompt-text-fill: -app-text-muted;"
                + " -fx-background-radius: 12; -fx-border-color: -app-border-3; -fx-border-radius: 12;"
                + " -fx-padding: 8 12; -fx-font-size: 13px;");

        VBox list = new VBox(4);
        Label hint = new Label("↑↓ 选择 · Enter 执行 · Esc 关闭");
        hint.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 10.5px;");

        List<PaletteAction> all = new ArrayList<>();
        all.add(new PaletteAction("play", "启动游戏", "立即启动当前实例", this::handleLaunch));
        all.add(new PaletteAction("layers", "实例选择", "切换、收藏或导入实例", () -> switchPage("instances")));
        all.add(new PaletteAction("gear", "实例设置", "打开当前实例的概览", () -> openInstanceSettings("overview")));
        all.add(new PaletteAction("download", "下载中心", "模组 / 整合包 / 资源", () -> switchPage("download")));
        all.add(new PaletteAction("download", "版本列表", "Minecraft 版本下载与安装",
                () -> { switchPage("download"); showDownloadPanel(minecraftPanel); loadMinecraftVersions(); }));
        all.add(new PaletteAction("gear", "启动器设置", "Java、内存、外观", () -> switchPage("settings")));
        all.add(new PaletteAction("tools", "工具", "环境、缓存、诊断", () -> switchPage("tools")));
        all.add(new PaletteAction("tools", "启动记录", "启动耗时、异常率、会话历史",
                () -> { switchPage("tools"); if (toolsPanel != null) toolsPanel.openHistoryTab(); }));
        all.add(new PaletteAction("sparkles", "性能优化", "打开可逆优化工具页",
                () -> { switchPage("tools"); if (toolsPanel != null) toolsPanel.openPerformanceTab(); }));
        all.add(new PaletteAction("folder", "打开游戏目录", config.getGameDir(), () -> openDir(new File(config.getGameDir()))));
        all.add(new PaletteAction("terminal", "打开日志目录", "查看 latest.log / launcher.log",
                () -> openDir(new File(config.getGameDir(), "logs"))));
        all.add(new PaletteAction("sparkles", "切换 AI 模式", "命令栏切换 DeepSeek / 本地助手", () -> {
            config.setAiEnabled(!config.isAiEnabled());
            config.save();
            applyAiMode();
            Toast.info(config.isAiEnabled() ? "AI 模式已开启" : "AI 模式已关闭");
        }));
        all.add(new PaletteAction("warning", "崩溃诊断", "分析最近一次崩溃", () -> {
            CrashAnalyzer.Report report = CrashAnalyzer.analyze(new File(config.getGameDir()));
            logArea.appendText(CrashAnalyzer.toText(report));
            CrashReportDialog.show(stageRef, report);
        }));

        final int[] selected = {0};
        final java.util.List<HBox> rows = new ArrayList<>();

        Runnable paintSelection = () -> {
            for (int i = 0; i < rows.size(); i++) {
                boolean active = i == selected[0];
                rows.get(i).setStyle("-fx-background-color: " + (active ? "-app-hover-2" : "transparent") + ";"
                        + " -fx-background-radius: 9;");
            }
        };

        Runnable runSelected = () -> {
            if (rows.isEmpty()) return;
            int index = Math.max(0, Math.min(selected[0], rows.size() - 1));
            PaletteAction action = (PaletteAction) rows.get(index).getUserData();
            closeCommandPalette();
            if (action != null && action.action() != null) action.action().run();
        };

        Runnable refresh = () -> {
            String query = search.getText() == null ? "" : search.getText().trim().toLowerCase();
            list.getChildren().clear();
            rows.clear();
            for (PaletteAction action : all) {
                String haystack = (action.title() + " " + action.hint()).toLowerCase();
                if (!query.isEmpty() && !haystack.contains(query)) continue;
                HBox row = new HBox(10);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(8, 10, 8, 10));
                row.setUserData(action);
                row.setCursor(Cursor.HAND);
                StackPane icon = Icons.node(action.icon(), 15, "#c7ccd4");
                Label title = new Label(action.title());
                title.setStyle("-fx-text-fill: -app-text-2; -fx-font-size: 12.5px; -fx-font-weight: bold;");
                Label sub = new Label(action.hint());
                sub.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 10.5px;");
                Region sp = new Region();
                HBox.setHgrow(sp, Priority.ALWAYS);
                row.getChildren().addAll(icon, title, sub, sp);
                row.setOnMouseEntered(e -> {
                    selected[0] = rows.indexOf(row);
                    paintSelection.run();
                });
                row.setOnMouseClicked(e -> {
                    closeCommandPalette();
                    action.action().run();
                });
                rows.add(row);
                list.getChildren().add(row);
                if (rows.size() >= 9) break;
            }
            selected[0] = 0;
            paintSelection.run();
        };

        search.textProperty().addListener((o, a, b) -> refresh.run());
        search.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.DOWN) {
                if (!rows.isEmpty()) selected[0] = (selected[0] + 1) % rows.size();
                paintSelection.run();
                e.consume();
            } else if (e.getCode() == javafx.scene.input.KeyCode.UP) {
                if (!rows.isEmpty()) selected[0] = (selected[0] - 1 + rows.size()) % rows.size();
                paintSelection.run();
                e.consume();
            } else if (e.getCode() == javafx.scene.input.KeyCode.ENTER) {
                runSelected.run();
                e.consume();
            } else if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                closeCommandPalette();
                e.consume();
            }
        });

        card.getChildren().addAll(search, list, hint);
        overlay.getChildren().add(card);
        card.setOnMouseClicked(e -> e.consume());
        overlay.setOnMouseClicked(e -> {
            if (e.getTarget() == overlay) closeCommandPalette();
        });

        layer.getChildren().add(overlay);
        commandPaletteOverlay = overlay;
        refresh.run();

        overlay.setOpacity(0);
        card.setScaleX(0.97);
        card.setScaleY(0.97);
        javafx.animation.FadeTransition fade = new javafx.animation.FadeTransition(javafx.util.Duration.millis(160), overlay);
        fade.setToValue(1);
        javafx.animation.ScaleTransition scale = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(180), card);
        scale.setToX(1);
        scale.setToY(1);
        scale.setInterpolator(Motion.SPRING);
        fade.play();
        scale.play();
        Platform.runLater(search::requestFocus);
    }

    private void closeCommandPalette() {
        StackPane overlay = commandPaletteOverlay;
        commandPaletteOverlay = null;
        if (overlay == null) return;
        javafx.animation.FadeTransition fade = new javafx.animation.FadeTransition(javafx.util.Duration.millis(120), overlay);
        fade.setToValue(0);
        fade.setOnFinished(e -> {
            if (overlay.getParent() instanceof StackPane parent) parent.getChildren().remove(overlay);
        });
        fade.play();
    }

    private void executeCommand(String raw) {
        String text = raw.trim();
        if (text.isEmpty()) return;
        if (commandHistory.isEmpty() || !commandHistory.get(commandHistory.size() - 1).equals(text)) {
            commandHistory.add(text);
        }
        commandHistoryIndex = commandHistory.size();
        logArea.appendText("⌘ " + text + "\n");

        if (text.startsWith("/")) {
            sendCommandToGame(text.substring(1).trim());
            return;
        }

        String[] parts = text.split("\\s+", 2);
        String command = parts[0].toLowerCase(java.util.Locale.ROOT);
        String arg = parts.length > 1 ? parts[1].trim() : "";
        switch (command) {
            case "help", "?" -> logArea.appendText("""
                    ⌘ 启动器命令：
                      help                显示本帮助
                      launch              启动当前实例
                      kill                结束当前游戏进程
                      versions            列出已安装版本
                      java                显示 Java 路径
                      memory <MB>         修改最大内存（下次启动生效）
                      open [子目录]        打开游戏目录 / 指定子目录
                      crash               诊断最近一次崩溃
                      import <文件>        导入 .zip / .mrpack 整合包
                      clear               清空日志
                      /<命令>             发送到游戏聊天（需游戏窗口，单人需开作弊）
                    """);
            case "launch" -> {
                if (launchBtn != null) launchBtn.fire();
            }
            case "kill" -> killGameProcess();
            case "versions" -> {
                File dir = new File(config.getGameDir(), "versions");
                File[] dirs = dir.listFiles(File::isDirectory);
                if (dirs == null || dirs.length == 0) {
                    logArea.appendText("没有已安装的版本\n");
                } else {
                    Arrays.sort(dirs, Comparator.comparing(File::getName));
                    StringBuilder list = new StringBuilder("已安装 ").append(dirs.length).append(" 个版本：");
                    for (File d : dirs) list.append("\n  · ").append(d.getName());
                    logArea.appendText(list.append("\n").toString());
                }
            }
            case "java" -> logArea.appendText("Java 路径：" + config.getJavaPath()
                    + "\n启动器 Java：" + System.getProperty("java.version")
                    + "（" + System.getProperty("java.vendor") + "）\n");
            case "memory" -> {
                try {
                    int mb = Integer.parseInt(arg);
                    if (mb < 512 || mb > 32768) throw new NumberFormatException();
                    config.setMemoryMb(mb);
                    config.save();
                    logArea.appendText("✅ 最大内存已设为 " + mb + " MB（下次启动生效）\n");
                } catch (NumberFormatException ex) {
                    logArea.appendText("用法：memory 4096（512–32768 MB）\n");
                }
            }
            case "import" -> {
                File file = new File(arg);
                if (!file.isFile()) {
                    logArea.appendText("找不到整合包：" + arg + "\n");
                } else {
                    importModpack(file);
                }
            }
            case "crash" -> {
                CrashAnalyzer.Report report = CrashAnalyzer.analyze(new File(config.getGameDir()));
                logArea.appendText(CrashAnalyzer.toText(report));
                CrashReportDialog.show(stageRef, report);
            }
            case "open" -> {
                File target = arg.isEmpty() ? new File(config.getGameDir())
                        : new File(config.getGameDir(), arg);
                try {
                    if (!target.exists() && !target.mkdirs()) {
                        logArea.appendText("目录不存在：" + target.getAbsolutePath() + "\n");
                        return;
                    }
                    java.awt.Desktop.getDesktop().open(target);
                    logArea.appendText("📂 已打开 " + target.getAbsolutePath() + "\n");
                } catch (Exception ex) {
                    logArea.appendText("打开失败：" + ex.getMessage() + "\n");
                }
            }
            case "clear" -> logArea.clear();
            default -> logArea.appendText("未知命令：" + command + "（输入 help 查看）\n");
        }
    }

    /** 以 / 开头的命令：聚焦游戏窗口，按 T 打开聊天后输入并回车。 */
    private void sendCommandToGame(String command) {
        if (command == null || command.isBlank()) {
            logArea.appendText("用法：/give @s diamond 64\n");
            return;
        }
        if (gameProcess == null || !gameProcess.isAlive()) {
            logArea.appendText("⚠ 没有正在运行的游戏进程，先启动游戏\n");
            return;
        }
        logArea.appendText("📨 发送到游戏：/" + command + "\n");
        CompletableFuture.runAsync(() -> {
            boolean ok = GameCommandSender.send(gameProcess, command);
            Platform.runLater(() -> logArea.appendText(ok
                    ? "✅ 已发送（单人需开启作弊，服务器需要 OP）\n"
                    : "⚠ 发送失败：没找到游戏窗口或窗口未响应\n"));
        });
    }

    private void killGameProcess() {
        if (gameProcess == null || !gameProcess.isAlive()) {
            logArea.appendText("没有正在运行的游戏进程\n");
            return;
        }
        long pid = gameProcess.pid();
        gameProcess.destroyForcibly();
        logArea.appendText("🛑 已结束游戏进程 (pid " + pid + ")\n");
    }

    private <T> ListCell<T> createDarkListCell() {
        return new ListCell<T>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.toString());
                    setTextFill(Color.web(Theme.isLight() ? "#1f2937" : "#E0E0E0"));
                    setStyle("-fx-background-color: transparent;");
                }
            }
        };
    }

    // ================== 下载面板 ==================
    private HBox createDownloadPanel() {
        HBox panel = new HBox(0);
        panel.setStyle("-fx-background-color: transparent;");

        VBox sidebar = new VBox(0);
        sidebar.setPrefWidth(230);
        sidebar.setMinWidth(230);
        sidebar.setMaxWidth(230);
        sidebar.setStyle("-fx-background-color: linear-gradient(to bottom, -app-nav, -app-surface-2);"
                + " -fx-padding: 20 0 0 0; -fx-border-color: transparent -app-border-2 transparent transparent;");

        downloadNavIndicator = new Rectangle(4, 42);
        downloadNavIndicator.setFill(Color.web("#9ca3af"));
        downloadNavIndicator.setArcWidth(4); downloadNavIndicator.setArcHeight(4);
        downloadNavIndicator.setMouseTransparent(true);

        downloadNavBox = new VBox(0);
        // 蓝条要画在导航项之上，否则会被选中行的背景盖住（看起来像被截断/错位）
        StackPane navContainer = new StackPane(downloadNavBox, downloadNavIndicator);
        StackPane.setAlignment(downloadNavIndicator, Pos.TOP_LEFT);
        navContainer.setAlignment(Pos.TOP_LEFT);

        // 左侧导航本身要能滚动，否则加载器分类多了会被窗口底部裁掉
        ScrollPane navScroll = new ScrollPane(navContainer);
        navScroll.setFitToWidth(true);
        navScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        navScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        navScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        Label group1 = new Label("游戏");
        group1.setStyle("-fx-text-fill: -app-text-muted-2; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 10 20 5 25;");
        downloadNavBox.getChildren().add(group1);

        downloadNavBox.getChildren().add(createDownloadNavItem("Minecraft"));

        Label group2 = new Label("社区资源");
        group2.setStyle("-fx-text-fill: -app-text-muted-2; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 25 20 5 25;");
        downloadNavBox.getChildren().add(group2);

        String[] items2 = {"模组", "整合包", "数据包", "资源包", "光影包", "世界", "收藏夹"};
        for (String item : items2) {
            HBox navItem = createDownloadNavItem(item);
            if (item.equals("整合包")) {
                ((Label)navItem.getChildren().get(1)).setStyle("-fx-text-fill: "
                        + (Theme.isLight() ? "#111827" : "#ffffff") + "; -fx-font-size: 15px; -fx-font-weight: bold;");
                ((Label)navItem.getChildren().get(0)).setStyle("-fx-text-fill: -app-text-2; -fx-font-size: 10px;");
                navItem.setStyle("-fx-background-color: -app-hover-2; -fx-background-radius: 0 8 8 0;"
                        + " -fx-background-insets: 2 8 2 0;");
            }
            downloadNavBox.getChildren().add(navItem);
        }

        Label group3 = new Label("安装包");
        group3.setStyle("-fx-text-fill: -app-text-muted-2; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 25 20 5 25;");
        downloadNavBox.getChildren().add(group3);
        for (String loader : new String[]{"OptiFine", "Fabric", "Quilt", "Forge", "NeoForge"}) {
            downloadNavBox.getChildren().add(createDownloadNavItem(loader));
        }

        sidebar.getChildren().add(navScroll);
        VBox.setVgrow(navScroll, Priority.ALWAYS);

        downloadContentArea = new StackPane();
        downloadContentArea.setPadding(new Insets(18));
        downloadContentArea.setStyle("-fx-background-color: transparent;");
        HBox.setHgrow(downloadContentArea, Priority.ALWAYS);
        VBox.setVgrow(downloadContentArea, Priority.ALWAYS);

        communityPanel = createCommunityPanel();
        minecraftPanel = createMinecraftPanel();
        minecraftPanel.setVisible(false);
        worldsNoticePanel = createWorldsNoticePanel();
        worldsNoticePanel.setVisible(false);
        favoritesPanel = createFavoritesPanel();
        favoritesPanel.setVisible(false);

        downloadContentArea.getChildren().addAll(communityPanel, minecraftPanel, worldsNoticePanel, favoritesPanel);

        ModuleHeader downloadHeader = new ModuleHeader("02 / LIBRARY", "下载中心",
                "Minecraft · 模组 · 整合包 · 资源", "download", ModuleHeader.Badge.SQUARE, "Minecraft Java Edition");
        VBox.setMargin(downloadHeader, new Insets(12, 18, 0, 18));
        VBox downloadRight = new VBox(14, downloadHeader, downloadContentArea);
        VBox.setVgrow(downloadContentArea, Priority.ALWAYS);
        HBox.setHgrow(downloadRight, Priority.ALWAYS);
        panel.getChildren().addAll(sidebar, downloadRight);
        return panel;
    }

    /** 下载页内容切换：只显示目标面板并做一次进入动画。 */
    private void showDownloadPanel(javafx.scene.Node target) {
        for (javafx.scene.Node node : downloadContentArea.getChildren()) {
            node.setVisible(node == target);
        }
        if (target != null) Motion.pageIn(target);
    }

    /** 侧栏 hover 时文字轻微右移，保留过渡动画。 */
    private void slideNavItem(Label label, double dx) {
        Timeline timeline = new Timeline(new KeyFrame(Duration.millis(140),
                new KeyValue(label.translateXProperty(), dx, Motion.OUT)));
        timeline.play();
    }

    private HBox createDownloadNavItem(String text) {
        HBox navItem = new HBox(12);
        navItem.setAlignment(Pos.CENTER_LEFT);
        navItem.setPadding(new Insets(12, 25, 12, 25));
        navItem.setCursor(Cursor.HAND);
        navItem.setMaxWidth(Double.MAX_VALUE);
        navItem.setStyle("-fx-background-color: transparent;");

        Label icon = new Label("■");
        icon.setStyle("-fx-text-fill: #444; -fx-font-size: 10px;");
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #999; -fx-font-size: 15px;");
        label.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(label, Priority.ALWAYS);

        navItem.getChildren().addAll(icon, label);

        navItem.setOnMouseEntered(e -> {
            if (!label.getStyle().contains("white")) {
                navItem.setStyle("-fx-background-color: -app-hover-2;");
                slideNavItem(label, 4);
            }
        });
        navItem.setOnMouseExited(e -> {
            if (!label.getStyle().contains("white")) {
                navItem.setStyle("-fx-background-color: transparent;");
                slideNavItem(label, 0);
            }
        });

        Motion.press(navItem);
        navItem.setOnMouseClicked(e -> {
            downloadNavIndicator.setHeight(Math.max(24, navItem.getHeight() - 8));
            double targetY = centeredIndicatorY(navItem);
            Timeline timeline = new Timeline(
                    new KeyFrame(Duration.millis(250),
                            new KeyValue(downloadNavIndicator.translateYProperty(), targetY, SMOOTH_INTERPOLATOR)
                    )
            );
            timeline.play();

            for (Node n : downloadNavBox.getChildren()) {
                if (n instanceof HBox) {
                    ((Label)((HBox)n).getChildren().get(1)).setStyle("-fx-text-fill: #999; -fx-font-size: 15px;");
                    ((Label)((HBox)n).getChildren().get(0)).setStyle("-fx-text-fill: #444; -fx-font-size: 10px;");
                    n.setStyle("-fx-background-color: transparent;");
                }
            }
            label.setStyle("-fx-text-fill: " + (Theme.isLight() ? "#111827" : "#ffffff")
                    + "; -fx-font-size: 15px; -fx-font-weight: bold;");
            icon.setStyle("-fx-text-fill: -app-text-2; -fx-font-size: 10px;");
            navItem.setStyle("-fx-background-color: -app-hover-2; -fx-background-radius: 0 8 8 0;"
                    + " -fx-background-insets: 2 8 2 0;");

            boolean installer = text.equals("Fabric") || text.equals("Quilt") || text.equals("Forge")
                    || text.equals("NeoForge") || text.equals("OptiFine");

            if (installer) {
                if (text.equals("OptiFine")) {
                    openBrowser("https://optifine.net/downloads");
                    Toast.info("OptiFine 官网安装器需要手动运行");
                    return;
                }
                if (installerPanel == null) {
                    installerPanel = createInstallerPanel();
                    downloadContentArea.getChildren().add(installerPanel);
                }
                showDownloadPanel(installerPanel);
                showInstaller(text);
                return;
            }

            if (text.equals("Minecraft")) {
                showDownloadPanel(minecraftPanel);
                loadMinecraftVersions();
            } else if (text.equals("世界")) {
                showDownloadPanel(worldsNoticePanel);
            } else if (text.equals("收藏夹")) {
                refreshFavoritesPanel();
                showDownloadPanel(favoritesPanel);
            } else {
                currentProjectType = text;
                showDownloadPanel(communityPanel);
                searchField.setPromptText("搜索 " + text + "...");
                if (text.equals("数据包")) {
                    loaderCombo.setValue("DataPack");
                } else {
                    loaderCombo.setValue("任意");
                    // 打开下载页不自动按实例筛选，用户自己选择版本和加载器
                }
                performSearch();
            }
        });

        return navItem;
    }

    /** 下载页的模组分类默认按当前实例的游戏版本 + 加载器筛选，减少不兼容安装。 */
    private void applyCurrentInstanceFilter() {
        String id = currentInstanceId();
        if (id == null) return;
        try {
            suppressFilterSearch = true;
            Instance instance = Instance.of(new File(config.getGameDir()), id);
            if (gameVersionCombo != null && gameVersionCombo.getItems().contains(instance.mcVersion)) {
                gameVersionCombo.setValue(instance.mcVersion);
            }
            if (loaderCombo != null && !"原版".equals(instance.loader)
                    && loaderCombo.getItems().contains(instance.loader)) {
                loaderCombo.setValue(instance.loader);
            }
        } catch (Exception ignored) {
        } finally {
            suppressFilterSearch = false;
        }
        performSearch();
    }

    /** 下载页优先按当前实例的 MC 版本 + 加载器筛选。 */
    private void configureVersionLoaderFilters() {
        if (gameVersionCombo == null || loaderCombo == null) return;
        java.util.LinkedHashSet<String> versions = new java.util.LinkedHashSet<>(gameVersionCombo.getItems());
        for (String id : VersionProfile.installedIds(new File(config.getGameDir()))) {
            String mc = extractMcVersion(id);
            if (mc != null && !mc.isBlank()) versions.add(mc);
        }
        gameVersionCombo.getItems().setAll(versions);
        // 默认保持“任意”，不自动套用当前实例条件
        gameVersionCombo.setValue("任意");
        loaderCombo.setValue("任意");
    }

    private String extractMcVersion(String id) {
        if (id == null) return null;
        String value = id;
        int forge = value.indexOf("-forge-");
        if (forge > 0) value = value.substring(0, forge);
        if (value.startsWith("fabric-loader-")) value = value.substring(value.lastIndexOf('-') + 1);
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("^(1\\.\\d+(?:\\.\\d+)?|\\d+\\.\\d+(?:\\.\\d+)?)").matcher(value);
        return matcher.find() ? matcher.group(1) : null;
    }

    private ComboBox<String> createFilterCombo(String defaultVal, String... items) {
        ComboBox<String> combo = new ComboBox<>();
        combo.getItems().add(defaultVal);
        combo.getItems().addAll(items);
        combo.setValue(defaultVal);
        combo.setStyle("-fx-background-radius: 6; -fx-font-size: 13px;");
        combo.setButtonCell(createDarkListCell());
        combo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (!suppressFilterSearch && communityPanel != null) {
                performSearch();
            }
        });
        return combo;
    }

    // ================== 社区资源面板 ==================
    private VBox createCommunityPanel() {
        VBox panel = new VBox(10);
        panel.setAlignment(Pos.TOP_LEFT);
        VBox.setVgrow(panel, Priority.ALWAYS);

        HBox searchBar = new HBox(15);
        searchBar.setAlignment(Pos.CENTER_LEFT);
        searchField = new TextField();
        searchField.setPromptText("搜索 整合包...");
        searchField.setPrefHeight(40);
        searchField.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(searchField, Priority.ALWAYS);
        searchField.setStyle("-fx-font-size: 14px; -fx-padding: 0 15;");

        searchDebounceTimer = new Timeline(new KeyFrame(Duration.millis(500), e -> performSearch()));
        searchDebounceTimer.setCycleCount(1);
        searchField.textProperty().addListener((obs, old, newText) -> {
            if (searchDebounceTimer != null) searchDebounceTimer.playFromStart();
        });

        Button searchBtn = new Button("搜索");
        searchBtn.setStyle("-fx-background-color: #9ca3af; -fx-text-fill: #111827; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 10 25; -fx-font-weight: bold;");
        searchBtn.setOnAction(e -> {
            if (searchDebounceTimer != null) searchDebounceTimer.stop();
            performSearch();
        });
        searchField.setOnAction(e -> {
            if (searchDebounceTimer != null) searchDebounceTimer.stop();
            performSearch();
        });

        searchBtn.setMinWidth(Region.USE_PREF_SIZE);
        searchBar.getChildren().addAll(searchField, searchBtn);

        filterBox = new VBox(8);
        filterBox.setStyle("-fx-background-color: linear-gradient(to bottom, -app-card-top, -app-card-alt2);"
                + " -fx-padding: 12 14; -fx-background-radius: 16; -fx-border-color: -app-border-soft;"
                + " -fx-border-radius: 16; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.22), 18, 0.1, 0, 5);");

        HBox row1 = new HBox(10);
        row1.setAlignment(Pos.CENTER_LEFT);
        HBox sourceBox = new HBox(6, new Label("来源"), sourceCombo = createFilterCombo("全部", "Modrinth", "CurseForge"));
        sourceBox.setAlignment(Pos.CENTER_LEFT);
        sourceBox.getChildren().get(0).setStyle("-fx-text-fill: -app-text-muted-3; -fx-font-size: 12px;");

        HBox tagBox = new HBox(6, new Label("标签"), tagCombo = createFilterCombo("全部", "轻量", "冒险", "硬核"));
        tagBox.setAlignment(Pos.CENTER_LEFT);
        tagBox.getChildren().get(0).setStyle("-fx-text-fill: -app-text-muted-3; -fx-font-size: 12px;");

        HBox verBox = new HBox(6, new Label("游戏版本"), gameVersionCombo = createFilterCombo("任意", "1.21", "1.20.1", "1.19.2", "1.16.5", "1.12.2", "1.7.10"));
        verBox.setAlignment(Pos.CENTER_LEFT);
        verBox.getChildren().get(0).setStyle("-fx-text-fill: -app-text-muted-3; -fx-font-size: 12px;");

        HBox loaderBox = new HBox(6, new Label("加载器"), loaderCombo = createFilterCombo("任意", "Fabric", "Forge", "Quilt", "NeoForge", "DataPack"));
        loaderBox.setAlignment(Pos.CENTER_LEFT);
        loaderBox.getChildren().get(0).setStyle("-fx-text-fill: -app-text-muted-3; -fx-font-size: 12px;");

        HBox sortBox = new HBox(6, new Label("排序"), sortCombo = createFilterCombo("默认", "下载量", "更新时间"));
        sortBox.setAlignment(Pos.CENTER_LEFT);
        sortBox.getChildren().get(0).setStyle("-fx-text-fill: -app-text-muted-3; -fx-font-size: 12px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refreshBtn = new Button();
        refreshBtn.setGraphic(Icons.node("refresh", 15, "#9ca3af"));
        refreshBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #aaa; -fx-font-size: 16px; -fx-cursor: hand;");
        refreshBtn.setOnAction(e -> performSearch());

        Button taskBtn = new Button();
        taskBtn.setGraphic(Icons.node("download", 15, "#9ca3af"));
        taskBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #aaa; -fx-font-size: 16px; -fx-cursor: hand;");

        sourceCombo.setPrefWidth(125); sourceCombo.setMinWidth(110);
        tagCombo.setPrefWidth(125); tagCombo.setMinWidth(110);
        gameVersionCombo.setPrefWidth(155); gameVersionCombo.setMinWidth(135);
        loaderCombo.setPrefWidth(165); loaderCombo.setMinWidth(145);
        sortCombo.setPrefWidth(135); sortCombo.setMinWidth(115);
        HBox row2 = new HBox(12, sourceBox, tagBox, sortBox);
        row2.setAlignment(Pos.CENTER_LEFT);
        row1.getChildren().addAll(verBox, loaderBox, spacer, refreshBtn, taskBtn);
        filterBox.getChildren().addAll(row1, row2);
        configureVersionLoaderFilters();

        modpackListView = new ListView<>(modpackData);
        modpackListView.setStyle("-fx-background-color: transparent; -fx-control-inner-background: transparent; -fx-border-color: transparent;");
        modpackListView.setCellFactory(param -> new ModpackCell());
        modpackListView.setMaxHeight(Double.MAX_VALUE);

        loadingIndicator = new ProgressIndicator();
        loadingIndicator.setMaxSize(60, 60);
        statusLabel = new Label("正在加载...");
        statusLabel.setStyle("-fx-text-fill: " + (Theme.isLight() ? "#0f172a" : "#e5e7eb")
                + "; -fx-font-size: 14px; -fx-font-weight: bold;");

        statusBox = new VBox(15, loadingIndicator, statusLabel);
        statusBox.setAlignment(Pos.CENTER);
        statusBox.setVisible(false);

        StackPane overlay = new StackPane(statusBox);
        overlay.setStyle("-fx-background-color: " + (Theme.isLight() ? "rgba(255,255,255,0.88)" : "rgba(18,18,18,0.72)")
                + "; -fx-background-radius: 16;");
        overlay.setVisible(false);
        statusBox.visibleProperty().addListener((obs, old, isVisible) -> overlay.setVisible(isVisible));

        listPane = new StackPane(modpackListView, overlay);
        VBox.setVgrow(listPane, Priority.ALWAYS);
        HBox.setHgrow(listPane, Priority.ALWAYS);

        loadMoreHint = new Label();
        loadMoreHint.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 12px;");
        Button backToTop = new Button("回到顶部");
        backToTop.setStyle("-fx-background-color: -app-btn-2; -fx-text-fill: -app-text-2; -fx-background-radius: 8;"
                + " -fx-padding: 5 12; -fx-font-size: 11px; -fx-cursor: hand;");
        backToTop.setOnAction(e -> modpackListView.scrollTo(0));
        HBox moreBar = new HBox(10, backToTop, loadMoreHint);
        moreBar.setAlignment(Pos.CENTER);

        // 列表滚动到接近底部时自动加载下一页
        modpackListView.skinProperty().addListener((obs, old, skin) -> Platform.runLater(this::attachResultScroll));

        panel.getChildren().addAll(searchBar, filterBox, listPane, moreBar);

        return panel;
    }

    /** 挂滚动监听：ListView 的竖向滚动条到底部就续拉一页。 */
    private void attachResultScroll() {
        if (modpackListView == null) return;
        if (modpackListView.lookup(".scroll-bar:vertical") instanceof javafx.scene.control.ScrollBar scrollBar) {
            if (scrollBar.getProperties().put("mc.hooked", Boolean.TRUE) != null) return;
            scrollBar.valueProperty().addListener((obs, old, value) -> {
                if (value.doubleValue() > 0.9) loadSearchPage(false);
            });
        }
    }

    // ================== 原版 Minecraft 专属面板 ==================
    private VBox createMinecraftPanel() {
        VBox panel = new VBox(15);
        panel.setAlignment(Pos.TOP_LEFT);
        VBox.setVgrow(panel, Priority.ALWAYS);

        HBox filterRow = new HBox(8);
        filterRow.setAlignment(Pos.CENTER_LEFT);
        ToggleGroup typeGroup = new ToggleGroup();
        String[][] filters = {{"all", "全部"}, {"release", "正式版"}, {"snapshot", "快照"}, {"old", "旧版"}};
        for (String[] filter : filters) {
            ToggleButton chip = new ToggleButton(filter[1]);
            chip.setToggleGroup(typeGroup);
            chip.setSelected(filter[0].equals(minecraftFilter));
            String idle = "-fx-background-color: -app-card; -fx-text-fill: -app-text-dim; -fx-background-radius: 16;"
                    + " -fx-padding: 5 14; -fx-cursor: hand; -fx-border-color: transparent;";
            String active = "-fx-background-color: rgba(156, 163, 175,0.16); -fx-text-fill: #d1d5db;"
                    + " -fx-background-radius: 16; -fx-padding: 5 14; -fx-cursor: hand;"
                    + " -fx-border-color: #9ca3af; -fx-border-radius: 16;";
            chip.setStyle(chip.isSelected() ? active : idle);
            chip.selectedProperty().addListener((o, ov, nv) -> chip.setStyle(nv ? active : idle));
            chip.setOnAction(e -> {
                if (!chip.isSelected()) {
                    chip.setSelected(true);
                    return;
                }
                minecraftFilter = filter[0];
                populateMinecraftCategories();
            });
            filterRow.getChildren().add(chip);
        }

        VBox latestVersionCard = new VBox(10);
        latestVersionCard.setStyle("-fx-background-color: linear-gradient(to bottom, -app-card-top, -app-card-alt2);"
                + " -fx-background-radius: 18; -fx-padding: 16 18; -fx-border-color: -app-border-soft;"
                + " -fx-border-radius: 18; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.24), 20, 0.1, 0, 6);");

        Label latestTitle = new Label("最新版本");
        latestTitle.setStyle("-fx-text-fill: -app-text-strong; -fx-font-size: 16px; -fx-font-weight: bold;");
        latestVersionCard.getChildren().add(latestTitle);

        VBox latestVersionsContainer = new VBox(10);
        latestVersionCard.getChildren().add(latestVersionsContainer);

        minecraftCategoriesContainer = new VBox(10);
        minecraftCategoriesContainer.setStyle("-fx-background-color: transparent;");

        ScrollPane scrollPane = new ScrollPane(minecraftCategoriesContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        HBox.setHgrow(scrollPane, Priority.ALWAYS);

        panel.getChildren().addAll(filterRow, latestVersionCard, scrollPane);

        allMinecraftVersions.addListener((javafx.collections.ListChangeListener<MinecraftVersion>) c -> {
            if (!allMinecraftVersions.isEmpty()) {
                populateLatestVersions(latestVersionsContainer);
                populateMinecraftCategories();
            }
        });

        return panel;
    }

    private void populateLatestVersions(VBox container) {
        container.getChildren().clear();
        MinecraftVersion latestRelease = allMinecraftVersions.stream().filter(v -> "release".equals(v.getType())).findFirst().orElse(null);
        MinecraftVersion latestSnapshot = allMinecraftVersions.stream().filter(v -> "snapshot".equals(v.getType())).findFirst().orElse(null);

        if (latestRelease != null) container.getChildren().add(createLatestVersionRow(latestRelease));
        if (latestSnapshot != null) container.getChildren().add(createLatestVersionRow(latestSnapshot));
    }

    private HBox createLatestVersionRow(MinecraftVersion version) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(7, 10, 7, 10));
        row.setMinHeight(54);
        row.setStyle("-fx-background-color: -app-hover-2; -fx-background-radius: 8; -fx-cursor: hand;");
        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color: #333; -fx-background-radius: 8; -fx-cursor: hand;"));
        row.setOnMouseExited(e -> row.setStyle("-fx-background-color: -app-hover-2; -fx-background-radius: 8; -fx-cursor: hand;"));

        StackPane icon = createBlockIcon(version.getType());
        icon.setPrefSize(40, 40);
        icon.setMinSize(40, 40);
        icon.setMaxSize(40, 40);

        VBox info = new VBox(2);
        info.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label(version.getId());
        title.setStyle("-fx-text-fill: -app-text-strong; -fx-font-size: 16px; -fx-font-weight: bold;");
        String typeStr = "release".equals(version.getType()) ? "最新正式版" : "最新预览版";
        String time = formatReleaseTime(version.getReleaseTime());
        Label subtitle = new Label(typeStr + (time.isEmpty() ? "" : " · 发布于 " + time));
        subtitle.setStyle("-fx-text-fill: -app-text-muted-3; -fx-font-size: 11.5px;");
        info.getChildren().addAll(title, subtitle);
        HBox.setHgrow(info, Priority.ALWAYS);

        Button installBtn = new Button("详情");
        installBtn.setStyle("-fx-background-color: #9ca3af; -fx-text-fill: #111827; -fx-font-weight: bold;"
                + " -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 6 16;");
        installBtn.setMinWidth(Region.USE_PREF_SIZE);
        installBtn.setOnAction(e -> openVersionDetail(version));
        row.setOnMouseClicked(e -> openVersionDetail(version));

        row.getChildren().addAll(icon, info, installBtn);
        return row;
    }

    private String formatReleaseTime(String iso) {
        if (iso == null || iso.isBlank()) return "";
        try {
            return java.time.Instant.parse(iso)
                    .atZone(java.time.ZoneId.systemDefault())
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"));
        } catch (Exception e) {
            return iso.length() >= 10 ? iso.substring(0, 10).replace('-', '/') : iso;
        }
    }

    /** 安装菜单：点开直接选加载器，不再弹对话框。 */
    private javafx.scene.control.MenuButton createInstallMenu(MinecraftVersion version) {
        javafx.scene.control.MenuButton menu = new javafx.scene.control.MenuButton("安装");
        menu.setStyle("-fx-background-color: #9ca3af; -fx-text-fill: #111827; -fx-font-weight: bold;"
                + " -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 6 14;");
        menu.setMinWidth(Region.USE_PREF_SIZE);
        String[] loaders = {"原版 (Vanilla)", "Fabric", "Quilt", "Forge", "NeoForge"};
        for (String loader : loaders) {
            javafx.scene.control.MenuItem item = new javafx.scene.control.MenuItem(loader);
            item.setOnAction(e -> startInstall(version, loader, null, menu));
            menu.getItems().add(item);
        }
        return menu;
    }

    private void setInstallerProgress(boolean running) {
        if (installerProgress == null) return;
        installerProgress.setVisible(running);
        installerProgress.setManaged(running);
        if (running) installerProgress.setProgress(javafx.scene.control.ProgressIndicator.INDETERMINATE_PROGRESS);
        else installerProgress.setProgress(0);
    }

    private void startInstall(MinecraftVersion version, String loader, String loaderVersion,
                              javafx.scene.control.ButtonBase trigger) {
        trigger.setDisable(true);
        trigger.setText("安装中...");
        setInstallerProgress(true);
        logMessage("📥 开始安装 Minecraft " + version.getId() + "（" + loader
                + (loaderVersion == null ? " · 最新加载器" : " · " + loaderVersion) + "）");
        executeInstallProcess(version, loader, loaderVersion, trigger);
    }

    private void populateMinecraftCategories() {
        minecraftCategoriesContainer.getChildren().clear();

        List<MinecraftVersion> pool = allMinecraftVersions;
        List<MinecraftVersion> releases = pool.stream().filter(v -> "release".equals(v.getType())).toList();
        List<MinecraftVersion> snapshots = pool.stream().filter(v -> "snapshot".equals(v.getType())).toList();
        List<MinecraftVersion> old = pool.stream()
                .filter(v -> v.getType() != null && v.getType().startsWith("old")).toList();

        if (!"snapshot".equals(minecraftFilter) && !"old".equals(minecraftFilter) && !releases.isEmpty()) {
            minecraftCategoriesContainer.getChildren().add(createCategoryPanel("正式版", releases));
        }
        if (!"release".equals(minecraftFilter) && !"old".equals(minecraftFilter) && !snapshots.isEmpty()) {
            minecraftCategoriesContainer.getChildren().add(createCategoryPanel("快照 / 预览版", snapshots));
        }
        if (!"release".equals(minecraftFilter) && !"snapshot".equals(minecraftFilter) && !old.isEmpty()) {
            minecraftCategoriesContainer.getChildren().add(createCategoryPanel("远古版（Alpha / Beta）", old));
        }
        if (minecraftCategoriesContainer.getChildren().isEmpty()) {
            Label none = new Label("没有符合筛选的版本。");
            none.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 12px; -fx-padding: 10;");
            minecraftCategoriesContainer.getChildren().add(none);
        }
    }

    private VBox createCategoryPanel(String categoryName, List<MinecraftVersion> versions) {
        VBox panel = new VBox();
        panel.setStyle("-fx-background-color: linear-gradient(to bottom, -app-card-top, -app-card-alt2);"
                + " -fx-background-radius: 14; -fx-border-color: -app-border-soft; -fx-border-radius: 14;"
                + " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.20), 16, 0.08, 0, 4);");

        HBox header = new HBox(10);
        header.setPadding(new Insets(15, 20, 15, 20));
        header.setStyle("-fx-cursor: hand; -fx-background-radius: 10;");
        header.setOnMouseEntered(e -> header.setStyle("-fx-background-color: -app-hover-2; -fx-cursor: hand; -fx-background-radius: 10;"));
        header.setOnMouseExited(e -> header.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-background-radius: 10;"));

        Label title = new Label(categoryName + " (" + versions.size() + ")");
        title.setStyle("-fx-text-fill: -app-text-strong; -fx-font-size: 16px; -fx-font-weight: bold;");
        StackPane arrow = Icons.node("chevron-down", 12, "#9ca3af");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().addAll(title, spacer, arrow);

        VBox content = new VBox(10);
        content.setPadding(new Insets(0, 10, 10, 10));
        content.setVisible(false);
        content.setManaged(false);
        content.setStyle("-fx-background-color: -app-card-alt; -fx-background-radius: 0 0 10 10;");

        ListView<MinecraftVersion> listView = new ListView<>();
        listView.setItems(FXCollections.observableArrayList(versions));
        listView.setPrefHeight(300);
        listView.setStyle("-fx-background-color: transparent; -fx-control-inner-background: transparent; -fx-border-color: transparent;");
        listView.setCellFactory(param -> new McVersionCell());

        content.getChildren().add(listView);

        header.setOnMouseClicked(e -> {
            boolean show = !content.isVisible();
            content.setVisible(show);
            content.setManaged(show);
            arrow.getChildren().setAll(Icons.node(show ? "chevron-up" : "chevron-down", 12, "#9ca3af"));
        });

        panel.getChildren().addAll(header, content);
        return panel;
    }

    private StackPane createBlockIcon(String type) {
        double size = 40;
        javafx.scene.canvas.Canvas canvas = new javafx.scene.canvas.Canvas(size, size);
        javafx.scene.canvas.GraphicsContext g = canvas.getGraphicsContext2D();

        boolean old = type != null && type.startsWith("old");
        boolean release = "release".equals(type);

        String top = release ? "#7CB342" : old ? "#9E9E9E" : "#FF9800";
        String left = release ? "#795548" : old ? "#757575" : "#E65100";
        String right = release ? "#5D4037" : old ? "#616161" : "#BF360C";
        String edge = release ? "#558B2F" : old ? "#757575" : "#F57C00";

        double[] topX = {20, 38, 20, 2};
        double[] topY = {2, 12, 22, 12};
        double[] leftX = {2, 20, 20, 2};
        double[] leftY = {12, 22, 40, 30};
        double[] rightX = {38, 20, 20, 38};
        double[] rightY = {12, 22, 40, 30};

        g.setFill(javafx.scene.paint.Color.web(top));
        g.fillPolygon(topX, topY, 4);
        g.setFill(javafx.scene.paint.Color.web(left));
        g.fillPolygon(leftX, leftY, 4);
        g.setFill(javafx.scene.paint.Color.web(right));
        g.fillPolygon(rightX, rightY, 4);

        if (release) {
            g.setFill(javafx.scene.paint.Color.web("#689F38"));
            g.fillPolygon(new double[]{2, 20, 20, 2}, new double[]{12, 22, 25, 15}, 4);
            g.fillPolygon(new double[]{38, 20, 20, 38}, new double[]{12, 22, 25, 15}, 4);
            g.setFill(javafx.scene.paint.Color.web("#8BC34A"));
            g.fillRect(12, 7, 6, 6);
            g.fillRect(26, 12, 5, 5);
            g.setFill(javafx.scene.paint.Color.web("#6D4C41"));
            g.fillRect(8, 27, 4, 4);
            g.fillRect(12, 33, 5, 4);
            g.fillRect(26, 29, 4, 5);
            g.setFill(javafx.scene.paint.Color.web("#8D6E63"));
            g.fillRect(30, 26, 4, 4);
        } else if (!old) {
            g.setFill(javafx.scene.paint.Color.web("#FFB74D"));
            g.fillRect(14, 8, 12, 7);
            g.setFill(javafx.scene.paint.Color.web("#FFCC80"));
            g.fillRect(17, 10, 6, 3);
            g.setFill(javafx.scene.paint.Color.web("#BF360C"));
            g.fillRect(14, 27, 5, 5);
            g.fillRect(25, 27, 5, 5);
            g.setFill(javafx.scene.paint.Color.web("#FFE0B2"));
            g.fillRect(16, 29, 2, 2);
            g.fillRect(27, 29, 2, 2);
        } else {
            g.setFill(javafx.scene.paint.Color.web("#BDBDBD"));
            g.fillRect(8, 18, 5, 4);
            g.fillRect(15, 24, 4, 4);
            g.fillRect(26, 18, 4, 4);
            g.setFill(javafx.scene.paint.Color.web("#424242"));
            g.fillRect(11, 27, 4, 4);
            g.fillRect(28, 25, 4, 4);
        }

        g.setStroke(javafx.scene.paint.Color.web(edge));
        g.setLineWidth(1);
        g.strokePolygon(topX, topY, 4);
        g.strokeLine(20, 22, 20, 40);

        StackPane block = new StackPane(canvas);
        block.setPrefSize(size, size);
        block.setMinSize(size, size);
        block.setMaxSize(size, size);
        block.setMouseTransparent(true);
        return block;
    }

    // ================== 安装包分类（Fabric / Quilt / Forge / NeoForge） ==================

    private VBox createInstallerPanel() {
        VBox panel = new VBox(10);
        panel.setAlignment(Pos.TOP_LEFT);
        panel.setPadding(new Insets(0));

        Button back = new Button("返回列表");
        back.setGraphic(Icons.node("back", 13, "#9ca3af"));
        back.setGraphicTextGap(4);
        back.setStyle("-fx-background-color: transparent; -fx-text-fill: -app-text-dim;"
                + " -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 2 4;");
        back.setOnAction(e -> showDownloadPanel(minecraftPanel));

        installerTitle = new Label("安装新实例");
        installerTitle.setStyle("-fx-text-fill: -app-text; -fx-font-size: 18px; -fx-font-weight: bold;");
        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);
        HBox top = new HBox(10, back, installerTitle, topSpacer);
        top.setAlignment(Pos.CENTER_LEFT);

        installerVersionCombo = new ComboBox<>();
        installerVersionCombo.setPrefWidth(150);
        installerVersionCombo.setPromptText("Minecraft 版本");
        installerVersionCombo.setStyle("-fx-background-radius: 8;");
        installerVersionCombo.setConverter(new javafx.util.StringConverter<MinecraftVersion>() {
            @Override
            public String toString(MinecraftVersion value) {
                return value == null ? "" : value.getId();
            }

            @Override
            public MinecraftVersion fromString(String string) {
                return null;
            }
        });
        installerVersionCombo.valueProperty().addListener((o, ov, nv) -> refreshInstallerLoaderVersions());

        installerNameField = new TextField();
        installerNameField.setPromptText("实例名（留空使用版本号）");
        installerNameField.setPrefWidth(200);
        HBox.setHgrow(installerNameField, Priority.ALWAYS);

        HBox versionRow = new HBox(10,
                installerLabel("游戏版本"), installerVersionCombo,
                installerLabel("实例名"), installerNameField);
        versionRow.setAlignment(Pos.CENTER_LEFT);

        installerAccordion = new VBox(6);
        String[] loaders = {"原版", "Fabric", "Forge", "NeoForge", "Quilt", "OptiFine", "LabyMod"};
        for (String loader : loaders) {
            installerAccordion.getChildren().add(createInstallerLoaderRow(loader));
        }

        Button download = new Button("开始下载");
        download.setStyle("-fx-background-color: linear-gradient(to bottom right, #d1d5db, #9ca3af);"
                + " -fx-text-fill: #111827; -fx-font-weight: bold; -fx-background-radius: 9;"
                + " -fx-padding: 9 26; -fx-cursor: hand; -fx-font-size: 13px;");
        download.setMinWidth(Region.USE_PREF_SIZE);
        download.setOnAction(e -> startInstallerFromPanel(download));

        installerProgress = new javafx.scene.control.ProgressBar();
        installerProgress.setMaxWidth(Double.MAX_VALUE);
        installerProgress.setVisible(false);
        installerProgress.setManaged(false);
        installerProgress.setStyle("-fx-accent: -app-accent;");
        installerStatus = new Label("先选游戏版本，再展开加载器选择具体版本。");
        installerStatus.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11.5px;");
        installerStatus.setWrapText(true);
        HBox.setHgrow(installerStatus, Priority.ALWAYS);

        HBox bottom = new HBox(12, download, installerStatus);
        bottom.setAlignment(Pos.CENTER_LEFT);
        VBox bottomBox = new VBox(6, bottom, installerProgress);

        ScrollPane accordionScroll = new ScrollPane(installerAccordion);
        accordionScroll.setFitToWidth(true);
        accordionScroll.setPrefViewportHeight(330);
        accordionScroll.setPrefHeight(360);
        accordionScroll.setMinHeight(250);
        accordionScroll.setMaxHeight(360);
        accordionScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;"
                + " -fx-border-color: transparent;");

        VBox card = new VBox(10, versionRow, accordionScroll, bottomBox);
        card.setPadding(new Insets(14));
        card.setMaxWidth(780);
        card.setMaxHeight(Double.MAX_VALUE);
        card.setStyle("-fx-background-color: -app-card; -fx-background-radius: 12;"
                + " -fx-border-color: -app-border-2; -fx-border-radius: 12;");

        panel.getChildren().addAll(top, card);
        javafx.application.Platform.runLater(() -> expandInstallerLoader("Fabric"));
        return panel;
    }

    private Label installerLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 12px;");
        return label;
    }

    /** 加载器折叠行：点标题展开版本列表，同一时间只展开一个。 */
    private VBox createInstallerLoaderRow(String name) {
        VBox holder = new VBox(0);
        holder.setStyle("-fx-background-color: -app-row; -fx-background-radius: 10;"
                + " -fx-border-color: -app-border-soft; -fx-border-radius: 10;");

        HBox header = new HBox(10);
        header.setPadding(new Insets(8, 12, 8, 12));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setCursor(Cursor.HAND);

        Label title = new Label(name);
        title.setStyle("-fx-text-fill: -app-text-2; -fx-font-size: 13px; -fx-font-weight: bold;");
        Label subtitle = new Label("选择版本");
        subtitle.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11px;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label arrow = new Label("▾");
        arrow.setStyle("-fx-text-fill: -app-text-dim; -fx-font-size: 12px;");

        header.getChildren().addAll(title, subtitle, spacer, arrow);
        header.setOnMouseClicked(e -> expandInstallerLoader(name));

        VBox body = new VBox(4);
        body.setPadding(new Insets(2, 10, 10, 10));
        body.setVisible(false);
        body.setManaged(false);

        holder.getChildren().addAll(header, body);
        installerLoaderHeaders.put(name, header);
        installerLoaderSubtitles.put(name, subtitle);
        installerLoaderArrows.put(name, arrow);
        installerLoaderBodies.put(name, body);
        return holder;
    }

    private void expandInstallerLoader(String name) {
        if (name == null || !installerLoaderBodies.containsKey(name)) return;
        installerExpandedLoader = name;
        currentLoader = name;
        for (var entry : installerLoaderBodies.entrySet()) {
            boolean open = entry.getKey().equals(name);
            entry.getValue().setVisible(open);
            entry.getValue().setManaged(open);
            HBox header = installerLoaderHeaders.get(entry.getKey());
            if (header != null) {
                header.setStyle(open
                        ? "-fx-background-color: -app-hover-2; -fx-background-radius: 10 10 0 0;"
                        : "-fx-background-color: transparent; -fx-background-radius: 10;");
            }
            Label arrow = installerLoaderArrows.get(entry.getKey());
            if (arrow != null) arrow.setText(open ? "▴" : "▾");
        }
        loadInstallerLoaderVersions(name);
    }

    private void loadInstallerLoaderVersions(String name) {
        VBox body = installerLoaderBodies.get(name);
        if (body == null) return;
        body.getChildren().clear();
        Label subtitle = installerLoaderSubtitles.get(name);

        MinecraftVersion game = installerVersionCombo == null ? null : installerVersionCombo.getValue();
        if (game == null) {
            body.getChildren().setAll(installerMessage("先选择 Minecraft 版本"));
            if (subtitle != null) subtitle.setText("等待游戏版本");
            return;
        }
        if ("原版".equals(name)) {
            body.getChildren().setAll(installerMessage("纯原版不需要加载器，直接开始下载即可。"));
            if (subtitle != null) subtitle.setText("纯原版");
            return;
        }
        if ("OptiFine".equals(name) || "LabyMod".equals(name)) {
            body.getChildren().setAll(installerMessage("官网安装器，点「开始下载」会打开官网。"));
            if (subtitle != null) subtitle.setText("官网安装器");
            return;
        }

        body.getChildren().setAll(installerMessage("正在读取 " + name + " 版本…"));
        String mc = game.getId();
        LoaderInstaller.listLoaderVersions(name, mc).thenAccept(versions ->
                javafx.application.Platform.runLater(() -> {
                    if (!name.equals(installerExpandedLoader)) return;
                    if (installerVersionCombo.getValue() != game) return;
                    String selected = installerSelectedVersions.get(name);
                    VBox list = new VBox(4);
                    list.getChildren().add(installerVersionRow(name, null, "自动（最新）", selected == null));
                    for (String version : versions) {
                        list.getChildren().add(installerVersionRow(name, version, version,
                                version.equals(selected)));
                    }
                    body.getChildren().setAll(installerBodyScroll(list));
                    if (subtitle != null) {
                        subtitle.setText(selected == null ? "自动（最新）" : selected);
                    }
                })).exceptionally(ex -> {
                    javafx.application.Platform.runLater(() -> {
                        if (!name.equals(installerExpandedLoader)) return;
                        body.getChildren().setAll(installerMessage("读取版本失败：" + ex.getMessage()));
                    });
                    return null;
                });
    }

    private ScrollPane installerBodyScroll(javafx.scene.Node content) {
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(190);
        scroll.setMinHeight(190);
        scroll.setMaxHeight(190);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;"
                + " -fx-border-color: transparent;");
        return scroll;
    }

    private HBox installerVersionRow(String loader, String version, String text, boolean selected) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(7, 10, 7, 10));
        row.setCursor(Cursor.HAND);
        row.setUserData(version);

        Label mark = new Label(selected ? "●" : "○");
        mark.setStyle("-fx-text-fill: " + (selected ? "-app-accent" : "-app-text-dim-3")
                + "; -fx-font-size: 11px;");
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: -app-text-2; -fx-font-size: 12px;");
        row.getChildren().addAll(mark, label);
        row.setStyle(installerVersionRowStyle(selected));

        row.setOnMouseClicked(e -> {
            installerSelectedVersions.put(loader, version);
            Label subtitle = installerLoaderSubtitles.get(loader);
            if (subtitle != null) subtitle.setText(version == null ? "自动（最新）" : version);
            if (row.getParent() instanceof VBox parent) {
                for (javafx.scene.Node node : parent.getChildren()) {
                    if (!(node instanceof HBox other)) continue;
                    boolean isSelected = java.util.Objects.equals(other.getUserData(), version);
                    other.setStyle(installerVersionRowStyle(isSelected));
                    if (other.getChildren().get(0) instanceof Label dot) {
                        dot.setText(isSelected ? "●" : "○");
                        dot.setStyle("-fx-text-fill: " + (isSelected ? "-app-accent" : "-app-text-dim-3")
                                + "; -fx-font-size: 11px;");
                    }
                }
            }
        });
        return row;
    }

    private String installerVersionRowStyle(boolean selected) {
        return "-fx-background-color: " + (selected ? "-app-hover-2" : "transparent") + ";"
                + " -fx-background-radius: 7; -fx-border-color: transparent;";
    }

    private Label installerMessage(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11.5px; -fx-padding: 8 10;");
        label.setWrapText(true);
        return label;
    }

    private void startInstallerFromPanel(Button trigger) {
        MinecraftVersion game = installerVersionCombo == null ? null : installerVersionCombo.getValue();
        if (game == null) {
            if (installerStatus != null) installerStatus.setText("请先选择 Minecraft 版本。");
            return;
        }
        String loader = installerExpandedLoader == null ? "原版" : installerExpandedLoader;
        if ("OptiFine".equals(loader)) {
            openBrowser("https://optifine.net/downloads");
            if (installerStatus != null) installerStatus.setText("已打开 OptiFine 官网，下载后手动安装。");
            return;
        }
        if ("LabyMod".equals(loader)) {
            openBrowser("https://labymod.net/download");
            if (installerStatus != null) installerStatus.setText("已打开 LabyMod 官网，下载后手动安装。");
            return;
        }
        String loaderVersion = installerSelectedVersions.get(loader);
        setInstallerProgress(true);
        if (installerStatus != null) {
            installerStatus.setText("正在安装 " + game.getId() + " · " + loader
                    + (loaderVersion == null ? "" : " " + loaderVersion) + "…");
        }
        startInstall(game, loader, loaderVersion, trigger);
    }

    /** 版本详情页：点击版本卡打开，而不是只能点安装。 */
    private void openVersionDetail(MinecraftVersion version) {
        if (versionDetailPanel == null) {
            versionDetailPanel = new VBox(12);
            versionDetailPanel.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            versionDetailPanel.setAlignment(Pos.TOP_LEFT);
            downloadContentArea.getChildren().add(versionDetailPanel);
        }
        versionDetailPanel.getChildren().setAll(buildVersionDetail(version));
        showDownloadPanel(versionDetailPanel);
    }

    private VBox buildVersionDetail(MinecraftVersion version) {
        File gameDir = new File(config.getGameDir());
        boolean installed = VersionProfile.isInstalled(gameDir, version.getId());

        Button back = new Button("返回列表");
        back.setGraphic(Icons.node("back", 13, "#9ca3af"));
        back.setGraphicTextGap(4);
        back.setStyle("-fx-background-color: transparent; -fx-text-fill: -app-text-dim;"
                + " -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 2 4;");
        back.setOnAction(e -> showDownloadPanel(minecraftPanel));

        Label heading = new Label("版本详情");
        heading.setStyle("-fx-text-fill: -app-text; -fx-font-size: 18px; -fx-font-weight: bold;");
        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);
        HBox top = new HBox(10, back, heading, topSpacer);
        top.setAlignment(Pos.CENTER_LEFT);

        StackPane icon = createBlockIcon(version.getType());
        icon.setPrefSize(64, 64);
        icon.setMinSize(64, 64);
        icon.setMaxSize(64, 64);

        Label title = new Label(version.getId());
        title.setStyle("-fx-text-fill: -app-text-strong; -fx-font-size: 24px; -fx-font-weight: bold;");
        Label badge = new Label("release".equals(version.getType()) ? "正式版" : "预览版");
        badge.setStyle("-fx-text-fill: " + ("release".equals(version.getType()) ? "#4CAF50" : "#FF9800")
                + "; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-color: rgba(255,255,255,0.06);"
                + " -fx-padding: 3 9; -fx-background-radius: 6;");
        HBox titleRow = new HBox(10, title, badge);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        String time = formatReleaseTime(version.getReleaseTime());
        Label subtitle = new Label((time.isEmpty() ? "Minecraft " + version.getId() : "发布于 " + time)
                + " · " + (installed ? "已安装" : "未安装"));
        subtitle.setStyle("-fx-text-fill: -app-text-dim-2; -fx-font-size: 12px;");
        VBox heroInfo = new VBox(5, titleRow, subtitle);
        heroInfo.setAlignment(Pos.CENTER_LEFT);

        Button install = new Button(installed ? "重新安装" : "安装此版本");
        install.setStyle("-fx-background-color: linear-gradient(to bottom right, #d1d5db, #9ca3af);"
                + " -fx-text-fill: #111827; -fx-font-weight: bold; -fx-background-radius: 9;"
                + " -fx-padding: 8 18; -fx-cursor: hand;");
        install.setOnAction(e -> openVersionInstaller(version));

        Button launch = new Button("启动此版本");
        launch.setStyle("-fx-background-color: transparent; -fx-text-fill: -app-text-2;"
                + " -fx-border-color: -app-border-3; -fx-border-radius: 9; -fx-background-radius: 9;"
                + " -fx-padding: 8 18; -fx-cursor: hand;");
        launch.setDisable(!installed);
        launch.setOnAction(e -> {
            addInstalledToCombo(version.getId());
            switchPage("launch");
            Toast.ok("已切换到 " + version.getId());
        });

        HBox actions = new HBox(10, install, launch);
        actions.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(heroInfo, Priority.ALWAYS);
        HBox hero = new HBox(18, icon, heroInfo, actions);
        hero.setAlignment(Pos.CENTER_LEFT);
        hero.setPadding(new Insets(18));
        hero.setStyle("-fx-background-color: -app-card; -fx-background-radius: 14;"
                + " -fx-border-color: -app-border-2; -fx-border-radius: 14;");

        VBox meta = new VBox(8,
                detailRow("版本 ID", version.getId()),
                detailRow("类型", "release".equals(version.getType()) ? "正式版" : "快照 / 预览版"),
                detailRow("发布时间", time.isEmpty() ? "未知" : time),
                detailRow("安装状态", installed ? "已安装" : "未安装"),
                detailRow("实例目录", new File(new File(gameDir, "versions"), version.getId()).getAbsolutePath()));
        meta.setPadding(new Insets(16));
        meta.setStyle("-fx-background-color: -app-card-alt; -fx-background-radius: 12;"
                + " -fx-border-color: -app-border-2; -fx-border-radius: 12;");

        Label loaderTitle = new Label("直接安装加载器");
        loaderTitle.setStyle("-fx-text-fill: -app-text-2; -fx-font-size: 12px; -fx-font-weight: bold;");
        HBox loaderChips = new HBox(8);
        for (String loader : new String[]{"Fabric", "Forge", "NeoForge", "Quilt", "原版"}) {
            Button chip = new Button(loader);
            chip.setStyle("-fx-background-color: -app-row; -fx-text-fill: -app-text-2;"
                    + " -fx-background-radius: 8; -fx-border-color: -app-border-soft; -fx-border-radius: 8;"
                    + " -fx-padding: 6 12; -fx-font-size: 11.5px; -fx-cursor: hand;");
            chip.setOnAction(e -> openVersionInstaller(version, loader));
            loaderChips.getChildren().add(chip);
        }
        VBox loaderCard = new VBox(10, loaderTitle, loaderChips);
        loaderCard.setPadding(new Insets(14));
        loaderCard.setStyle("-fx-background-color: -app-card; -fx-background-radius: 12;"
                + " -fx-border-color: -app-border-2; -fx-border-radius: 12;");

        return new VBox(10, top, hero, meta, loaderCard);
    }

    private HBox detailRow(String label, String value) {
        Label key = new Label(label);
        key.setMinWidth(74);
        key.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11px;");
        Label val = new Label(value);
        val.setStyle("-fx-text-fill: -app-text-2; -fx-font-size: 12px;");
        val.setWrapText(true);
        HBox row = new HBox(12, key, val);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    /** 从版本列表进入：同一个内容区展示安装页，不弹窗。 */
    private void openVersionInstaller(MinecraftVersion version) {
        openVersionInstaller(version, "Fabric");
    }

    private void openVersionInstaller(MinecraftVersion version, String loader) {
        if (installerPanel == null) {
            installerPanel = createInstallerPanel();
            downloadContentArea.getChildren().add(installerPanel);
        }
        showDownloadPanel(installerPanel);
        fillInstallerPublicVersions(version);
        if (installerNameField != null) {
            installerNameField.setText(version.getId());
        }
        expandInstallerLoader(loader == null || loader.isBlank() ? "Fabric" : loader);
        if (installerTitle != null) installerTitle.setText("安装 Minecraft " + version.getId());
    }

    private void fillInstallerPublicVersions(MinecraftVersion selected) {
        if (installerVersionCombo == null) return;
        java.util.List<MinecraftVersion> versions = allMinecraftVersions.stream()
                .filter(v -> "release".equals(v.getType())).collect(java.util.stream.Collectors.toList());
        if (selected != null && versions.stream().noneMatch(v -> v.getId().equals(selected.getId()))) {
            versions.add(0, selected);
        }
        installerVersionCombo.getItems().setAll(versions);
        if (selected != null) installerVersionCombo.setValue(selected);
    }

    private void updateInstallerTitle() {
        if (installerTitle == null || installerVersionCombo == null) return;
        MinecraftVersion game = installerVersionCombo.getValue();
        installerTitle.setText(game == null ? "安装新实例" : "安装 Minecraft " + game.getId());
    }

    /** 「世界」页：Modrinth 没有 world 分类，明确给出可用入口，避免显示成整合包。 */
    private VBox createWorldsNoticePanel() {
        VBox panel = new VBox(14);
        panel.setPadding(new Insets(4));
        panel.setMaxWidth(760);

        Label title = new Label("世界");
        title.setStyle("-fx-text-fill: -app-text; -fx-font-size: 20px; -fx-font-weight: bold;");
        Label hint = new Label("Modrinth 的搜索接口没有「世界」分类，地图 / 存档需要从 CurseForge 或社区下载后放进 saves 目录。");
        hint.setStyle("-fx-text-fill: -app-text-dim-2; -fx-font-size: 13px;");
        hint.setWrapText(true);

        Button curse = new Button("打开 CurseForge 世界分类");
        curse.setStyle("-fx-background-color: #9ca3af; -fx-text-fill: #111827; -fx-font-weight: bold;"
                + " -fx-background-radius: 9; -fx-padding: 8 18; -fx-cursor: hand;");
        curse.setOnAction(e -> openBrowser("https://www.curseforge.com/minecraft/worlds"));

        Button saves = new Button("打开存档目录");
        saves.setStyle("-fx-background-color: -app-border-4; -fx-text-fill: -app-text-2;"
                + " -fx-background-radius: 9; -fx-padding: 8 18; -fx-cursor: hand;");
        saves.setOnAction(e -> openDir(new File(config.getGameDir(), "saves")));

        HBox actions = new HBox(10, curse, saves);
        panel.getChildren().addAll(title, hint, actions);
        return panel;
    }

    /** 「收藏夹」页：列出收藏的实例，点一下切回启动页。 */
    private VBox createFavoritesPanel() {
        VBox panel = new VBox(12);
        panel.setPadding(new Insets(4));
        panel.setMaxWidth(760);

        Label title = new Label("收藏夹");
        title.setStyle("-fx-text-fill: -app-text; -fx-font-size: 20px; -fx-font-weight: bold;");
        Label hint = new Label("在「实例选择」里点实例右侧的 ☆ 收藏，这里会列出来。");
        hint.setStyle("-fx-text-fill: -app-text-dim-2; -fx-font-size: 12px;");

        favoritesSearch = new TextField();
        favoritesSearch.setPromptText("搜索收藏的实例或资源");
        favoritesSearch.setPrefWidth(320);
        favoritesSearch.textProperty().addListener((obs, old, value) -> refreshFavoritesPanel());

        favoritesSortCombo = new ComboBox<>(FXCollections.observableArrayList("最近收藏", "名称", "下载量"));
        favoritesSortCombo.setValue("最近收藏");
        favoritesSortCombo.setPrefWidth(120);
        favoritesSortCombo.valueProperty().addListener((obs, old, value) -> refreshFavoritesPanel());
        HBox favoritesTools = new HBox(10, favoritesSearch, favoritesSortCombo);
        favoritesTools.setAlignment(Pos.CENTER_LEFT);

        favoritesList = new VBox(8);
        favoritesResourceList = new VBox(8);

        Label instanceTitle = new Label("收藏的实例");
        instanceTitle.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 12px; -fx-font-weight: bold;");
        Label resourceTitle = new Label("收藏的资源");
        resourceTitle.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 12px; -fx-font-weight: bold;"
                + " -fx-padding: 8 0 0 0;");

        panel.getChildren().addAll(title, hint, favoritesTools,
                instanceTitle, favoritesList, resourceTitle, favoritesResourceList);
        return panel;
    }

    private void refreshFavoritesPanel() {
        if (favoritesList == null) return;
        favoritesList.getChildren().clear();
        File gameDir = new File(config.getGameDir());
        List<Instance> favorites = Instance.list(gameDir).stream()
                .filter(instance -> InstanceMeta.isFavorite(gameDir, instance.id)).toList();
        favoritesQuery = favoritesSearch == null || favoritesSearch.getText() == null
                ? "" : favoritesSearch.getText().trim().toLowerCase();

        if (favorites.isEmpty() && Favorites.list().isEmpty()) {
            favoritesList.getChildren().add(favoritesEmptyGuide());
            if (favoritesResourceList != null) favoritesResourceList.getChildren().clear();
            return;
        }
        List<Instance> filtered = favorites.stream()
                .filter(instance -> favoritesQuery.isEmpty()
                        || InstanceMeta.name(gameDir, instance.id).toLowerCase().contains(favoritesQuery)
                        || instance.id.toLowerCase().contains(favoritesQuery))
                .toList();
        if (filtered.isEmpty()) {
            Label empty = new Label(favorites.isEmpty() ? "还没有收藏的实例。" : "没有匹配的收藏实例。");
            empty.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 13px;");
            favoritesList.getChildren().add(empty);
            renderFavoriteResources();
            return;
        }
        java.util.Map<String, java.util.List<Instance>> instanceGroups = new java.util.LinkedHashMap<>();
        for (Instance instance : filtered) {
            instanceGroups.computeIfAbsent(instance.loader, key -> new ArrayList<>()).add(instance);
        }
        for (var instanceGroup : instanceGroups.entrySet()) {
            String groupKey = "实例:" + instanceGroup.getKey();
            boolean open = config.getFavoriteGroups().getOrDefault(groupKey, true);
            Label groupHeader = new Label((open ? "▾ " : "▸ ") + instanceGroup.getKey()
                    + "（" + instanceGroup.getValue().size() + "）");
            groupHeader.setStyle("-fx-text-fill: -app-text-dim-2; -fx-font-size: 11px; -fx-font-weight: bold;"
                    + " -fx-padding: 8 0 2 0; -fx-cursor: hand;");
            groupHeader.setOnMouseClicked(e -> {
                config.getFavoriteGroups().put(groupKey, !open);
                config.save();
                refreshFavoritesPanel();
            });
            VBox instanceBody = new VBox(8);
            instanceBody.setVisible(open);
            instanceBody.setManaged(open);
            favoritesList.getChildren().addAll(groupHeader, instanceBody);

            for (Instance instance : instanceGroup.getValue()) {
            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(10, 14, 10, 14));
            row.getStyleClass().add("instance-row");
            row.setCursor(Cursor.HAND);

            Label icon = new Label(instance.loader.substring(0, 1));
            icon.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;"
                    + " -fx-background-color: -app-text-dim-2; -fx-background-radius: 8; -fx-padding: 7 10;");

            VBox info = new VBox(2);
            Label name = new Label(InstanceMeta.name(gameDir, instance.id));
            name.setStyle("-fx-text-fill: -app-text; -fx-font-size: 13px;");
            Label sub = new Label(instance.id + " · " + instance.subtitle());
            sub.setStyle("-fx-text-fill: -app-text-dim-3; -fx-font-size: 11px;");
            info.getChildren().addAll(name, sub);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Label action = new Label("切换到启动页 ›");
            action.setStyle("-fx-text-fill: #d1d5db; -fx-font-size: 12px;");

            row.getChildren().addAll(icon, info, spacer, action);
            row.setOnMouseClicked(e -> {
                selectInstance(instance);
                switchPage("launch");
            });
            instanceBody.getChildren().add(row);
            }
        }
        renderFavoriteResources();
    }

    /** 收藏夹空状态：告诉用户怎么收藏，并给出两个入口。 */
    private VBox favoritesEmptyGuide() {
        VBox guide = new VBox(10);
        guide.setPadding(new Insets(16, 18, 16, 18));
        guide.setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-background-radius: 12;"
                + " -fx-border-color: rgba(255,255,255,0.06); -fx-border-radius: 12;");
        Label title = new Label("这里还什么都没有");
        title.setStyle("-fx-text-fill: -app-text-2; -fx-font-size: 14px; -fx-font-weight: bold;");
        Label hint = new Label("在「下载」页点卡片右侧的 ☆ 收藏资源；在「实例选择」里点实例右侧的 ☆ 收藏实例。");
        hint.setStyle("-fx-text-fill: -app-text-dim-2; -fx-font-size: 12px;");
        hint.setWrapText(true);

        Button toDownload = new Button("去下载页找资源");
        toDownload.setStyle("-fx-background-color: #9ca3af; -fx-text-fill: #111827; -fx-font-weight: bold;"
                + " -fx-background-radius: 9; -fx-padding: 7 16; -fx-cursor: hand;");
        toDownload.setOnAction(e -> switchPage("download"));
        Button toInstances = new Button("去实例选择");
        toInstances.setStyle("-fx-background-color: -app-border-4; -fx-text-fill: -app-text-2;"
                + " -fx-background-radius: 9; -fx-padding: 7 16; -fx-cursor: hand;");
        toInstances.setOnAction(e -> switchPage("instances"));

        HBox actions = new HBox(10, toDownload, toInstances);
        guide.getChildren().addAll(title, hint, actions);
        return guide;
    }

    /** 收藏的资源（Modrinth 项目），可直接下载或取消收藏。 */
    private void renderFavoriteResources() {
        if (favoritesResourceList == null) return;
        favoritesResourceList.getChildren().clear();
        List<Favorites.Fav> favs = new ArrayList<>(Favorites.list().stream()
                .filter(fav -> favoritesQuery.isEmpty()
                        || (fav.title() != null && fav.title().toLowerCase().contains(favoritesQuery))
                        || (fav.type() != null && fav.type().toLowerCase().contains(favoritesQuery)))
                .toList());
        String sort = favoritesSortCombo == null || favoritesSortCombo.getValue() == null
                ? "最近收藏" : favoritesSortCombo.getValue();
        switch (sort) {
            case "名称" -> favs.sort(Comparator.comparing(
                    fav -> fav.title() == null ? "" : fav.title().toLowerCase()));
            case "下载量" -> favs.sort(Comparator.comparingLong(Favorites.Fav::downloads).reversed());
            default -> favs.sort(Comparator.comparingLong(Favorites.Fav::time).reversed());
        }
        if (favs.isEmpty()) {
            Label empty = new Label(Favorites.list().isEmpty()
                    ? "还没有收藏的资源。在下载页的卡片上点 ☆ 即可收藏。"
                    : "没有匹配的收藏资源。");
            empty.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 12px;");
            empty.setWrapText(true);
            favoritesResourceList.getChildren().add(empty);
            return;
        }
        java.util.Map<String, java.util.List<Favorites.Fav>> grouped = new java.util.LinkedHashMap<>();
        for (String label : new String[]{"整合包", "模组", "资源包", "光影包", "数据包", "世界", "资源"}) {
            java.util.List<Favorites.Fav> group = favs.stream()
                    .filter(fav -> typeLabel(fav.type()).equals(label)).toList();
            if (!group.isEmpty()) grouped.put(label, group);
        }
        for (var groupEntry : grouped.entrySet()) {
            String groupKey = groupEntry.getKey();
            boolean open = config.getFavoriteGroups().getOrDefault(groupKey, true);
            Label groupLabel = new Label((open ? "▾ " : "▸ ") + groupKey
                    + "（" + groupEntry.getValue().size() + "）");
            groupLabel.setStyle("-fx-text-fill: -app-text-dim-2; -fx-font-size: 11px; -fx-font-weight: bold;"
                    + " -fx-padding: 8 0 2 0; -fx-cursor: hand;");
            groupLabel.setOnMouseClicked(e -> {
                config.getFavoriteGroups().put(groupKey, !open);
                config.save();
                renderFavoriteResources();
            });
            VBox groupBody = new VBox(8);
            groupBody.setVisible(open);
            groupBody.setManaged(open);
            favoritesResourceList.getChildren().addAll(groupLabel, groupBody);
            for (Favorites.Fav fav : groupEntry.getValue()) {
            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(10, 14, 10, 14));
            row.getStyleClass().add("instance-row");

            Label icon = new Label(typeLabel(fav.type()).substring(0, 1));
            icon.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;"
                    + " -fx-background-color: -app-text-dim-2; -fx-background-radius: 8; -fx-padding: 7 9;");

            VBox info = new VBox(2);
            Label name = new Label(fav.title());
            name.setStyle("-fx-text-fill: -app-text; -fx-font-size: 13px;");
            Label sub = new Label(typeLabel(fav.type()) + " · ⬇ " + formatCount(fav.downloads()) + " · Modrinth");
            sub.setStyle("-fx-text-fill: -app-text-dim-3; -fx-font-size: 11px;");
            info.getChildren().addAll(name, sub);
            HBox.setHgrow(info, Priority.ALWAYS);

            Button download = new Button("下载");
            download.setStyle("-fx-background-color: #9ca3af; -fx-text-fill: #111827; -fx-font-weight: bold;"
                    + " -fx-background-radius: 8; -fx-padding: 6 16; -fx-cursor: hand;");
            download.setMinWidth(Region.USE_PREF_SIZE);
            download.setOnAction(e -> fetchVersionsAndDownload(fav.id(), fav.title(), fav.type()));

            Button remove = new Button();
            remove.setGraphic(Icons.node("star", 16, "#eab308"));
            remove.setStyle("-fx-background-color: transparent; -fx-text-fill: #eab308;"
                    + " -fx-font-size: 17px; -fx-cursor: hand; -fx-padding: 2 6;");
            remove.setOnAction(e -> {
                Favorites.remove(fav.id());
                renderFavoriteResources();
                Toast.info("已取消收藏 " + fav.title());
            });

            row.getChildren().addAll(icon, info, remove, download);
            groupBody.getChildren().add(row);
            }
        }
    }

    private String typeLabel(String type) {
        if (type == null) return "资源";
        return switch (type) {
            case "mod" -> "模组";
            case "resourcepack" -> "资源包";
            case "shader" -> "光影包";
            case "datapack" -> "数据包";
            case "world" -> "世界";
            default -> "整合包";
        };
    }

    private static String formatCount(long count) {
        if (count >= 100_000_000) return (count / 100_000_000) + " 亿";
        if (count >= 10_000) return (count / 10_000) + " 万";
        return String.valueOf(count);
    }

    private void showInstaller(String loader) {
        if (installerVersionCombo == null) return;
        if (installerVersionCombo.getItems().isEmpty()) {
            java.util.List<MinecraftVersion> releases = allMinecraftVersions.stream()
                    .filter(v -> "release".equals(v.getType())).collect(java.util.stream.Collectors.toList());
            installerVersionCombo.getItems().setAll(releases);
            if (!releases.isEmpty()) installerVersionCombo.setValue(releases.get(0));
        }
        updateInstallerTitle();
        if (loader != null && installerLoaderBodies.containsKey(loader)) {
            expandInstallerLoader(loader);
        } else if (installerExpandedLoader == null) {
            expandInstallerLoader("Fabric");
        }
    }

    /** 切换游戏版本后清空已选加载器版本，并重新读取当前展开的加载器。 */
    private void refreshInstallerLoaderVersions() {
        if (installerVersionCombo == null) return;
        installerSelectedVersions.clear();
        updateInstallerTitle();
        if (installerExpandedLoader != null) {
            loadInstallerLoaderVersions(installerExpandedLoader);
        }
    }

    private void executeInstallProcess(MinecraftVersion version, String loader, String loaderVersion,
                                        javafx.scene.control.ButtonBase btn) {
        final File gameDir = new File(config.getGameDir());
        final String id = version.getId();
        final String jsonUrl = version.getUrl();

        CompletableFuture.runAsync(() -> {
            java.util.function.Consumer<String> log = msg -> Platform.runLater(() -> logArea.appendText(msg + "\n"));
            try {
                // 1. 先把原版装好（JSON / client / 库 / 资源 / natives）
                VersionProfile baseProfile = ensureInstalled(id, jsonUrl, log);

                // 2. 再按选择装加载器
                String resultId = id;
                if (!loader.startsWith("原版")) {
                    log.accept("🔧 正在安装 " + loader + "（版本 " + id + "）...");
                    String javaPath = resolveJavaPath(gameDir, baseProfile.getJavaMajorVersion(), log);
                    resultId = switch (loader) {
                        case "Fabric" -> LoaderInstaller.installFabric(gameDir, id, loaderVersion, log).get(10, TimeUnit.MINUTES);
                        case "Quilt" -> LoaderInstaller.installQuilt(gameDir, id, loaderVersion, log).get(10, TimeUnit.MINUTES);
                        case "Forge" -> LoaderInstaller.installForge(gameDir, id, javaPath, loaderVersion, log).get(30, TimeUnit.MINUTES);
                        case "NeoForge" -> LoaderInstaller.installNeoForge(gameDir, id, javaPath, loaderVersion, log).get(30, TimeUnit.MINUTES);
                        default -> id;
                    };
                }

                final String installedId = resultId;
                Platform.runLater(() -> {
                    logArea.appendText("✅ 安装完成: " + installedId + "，已加入版本下拉框\n");
                    addInstalledToCombo(installedId);
                    btn.setDisable(false);
                    btn.setText("开始下载");
                    setInstallerProgress(false);
                   if (installerStatus != null) installerStatus.setText("安装完成：" + installedId);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    logArea.appendText("❌ 安装失败: " + e.getMessage() + "\n");
                    btn.setDisable(false);
                    btn.setText("开始下载");
                    setInstallerProgress(false);
                   if (installerStatus != null) installerStatus.setText("安装失败：" + e.getMessage());
                });
            }
        });
    }

    /** Forge/NeoForge 的安装器需要 java 可执行文件。 */
    private String resolveJavaPath(File gameDir, int majorVersion, java.util.function.Consumer<String> log) throws Exception {
        if (config.getJavaPath() != null && !config.getJavaPath().isBlank()) {
            return config.getJavaPath();
        }
        return JavaManager.ensureJavaAsync(gameDir, majorVersion,
                msg -> Platform.runLater(() -> logArea.appendText(msg + "\n"))).get(30, TimeUnit.MINUTES);
    }

    private void loadMinecraftVersions() {
        populateLatestVersions((VBox) ((VBox) minecraftPanel.getChildren().get(0)).getChildren().get(1));
        populateMinecraftCategories();
    }

    // ================== 现代感版本卡片 Cell ==================
    class McVersionCell extends ListCell<MinecraftVersion> {
        private HBox card;
        private StackPane blockPane;
        private Label title;
        private Label typeLabel;
        private Button installBtn;

        public McVersionCell() {
            super();
            card = new HBox(20);
            card.setPadding(new Insets(15, 20, 15, 20));
            card.setStyle("-fx-background-color: -app-hover-2; -fx-background-radius: 8;");
            card.setCursor(Cursor.HAND);
            card.getStyleClass().add("mod-card");

            card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: #333; -fx-background-radius: 8;"));
            card.setOnMouseExited(e -> card.setStyle("-fx-background-color: -app-hover-2; -fx-background-radius: 8;"));

            blockPane = new StackPane();
            blockPane.setPrefSize(50, 50);

            VBox info = new VBox(6);
            title = new Label();
            title.setStyle("-fx-text-fill: -app-text-strong; -fx-font-size: 18px; -fx-font-weight: bold;");

            typeLabel = new Label();
            typeLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 2 8; -fx-background-radius: 4;");

            info.getChildren().addAll(title, typeLabel);
            HBox.setHgrow(info, Priority.ALWAYS);

            installBtn = new Button("详情");
            installBtn.setStyle("-fx-background-color: #9ca3af; -fx-text-fill: #111827; -fx-font-weight: bold;"
                    + " -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 8 22;");
            installBtn.setMinWidth(Region.USE_PREF_SIZE);
            installBtn.setOnAction(e -> {
                if (getItem() != null) openVersionDetail(getItem());
            });
            card.setOnMouseClicked(e -> {
                if (getItem() != null) openVersionDetail(getItem());
            });

            card.getChildren().addAll(blockPane, info, installBtn);
        }

        @Override
        protected void updateItem(MinecraftVersion version, boolean empty) {
            super.updateItem(version, empty);
            if (empty || version == null) {
                setGraphic(null);
                return;
            }

            boolean installed = VersionProfile.isInstalled(new File(config.getGameDir()), version.getId());
            title.setText(version.getId() + (installed ? "   · 已安装" : ""));

            blockPane.getChildren().clear();
            if (version.getType().equals("release")) {
                typeLabel.setText("正式版");
                typeLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 12px; -fx-font-weight: bold; -fx-background-color: rgba(76, 175, 80, 0.1); -fx-padding: 2 8; -fx-background-radius: 4;");
            } else {
                typeLabel.setText("快照版");
                typeLabel.setStyle("-fx-text-fill: #FF9800; -fx-font-size: 12px; -fx-font-weight: bold; -fx-background-color: rgba(255, 152, 0, 0.1); -fx-padding: 2 8; -fx-background-radius: 4;");
            }

            blockPane.getChildren().add(createBlockIcon(version.getType()));

            installBtn.setText("详情");
            installBtn.setDisable(false);

            setGraphic(card);
        }
    }

    // ================== 社区资源 Cell (彻底修复文字截断和按钮压缩) ==================
    class ModpackCell extends ListCell<ModpackProject> {
        private HBox card;
        private StackPane iconPane;
        private Label titleLabel;
        private Label descLabel;
        private Label authorLabel;
        private Label dlLabel;
        private Label timeLabel;
        private Label srcLabel;
        private Button downloadBtn;
        private Button starBtn;

        public ModpackCell() {
            super();
            card = new HBox(12);
            card.setPadding(new Insets(10, 14, 10, 14));
            card.setStyle("-fx-background-color: linear-gradient(to bottom, -app-card-top, -app-card-alt2);"
                    + " -fx-background-radius: 14; -fx-border-color: -app-border-soft; -fx-border-radius: 14;");
            card.setCursor(Cursor.HAND);

            // 【核心修复】将卡片宽度绑定到 ListView 宽度，防止出现横向滚动条
            card.prefWidthProperty().bind(modpackListView.widthProperty().subtract(30));
            card.setMaxWidth(Double.MAX_VALUE);

            Motion.hoverScale(card, 1.004);
            card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: -app-hover-2;"
                    + " -fx-background-radius: 14; -fx-border-color: -app-border-3; -fx-border-radius: 14;"));
            card.setOnMouseExited(e -> card.setStyle("-fx-background-color: linear-gradient(to bottom, -app-card-top, -app-card-alt2);"
                    + " -fx-background-radius: 14; -fx-border-color: -app-border-soft; -fx-border-radius: 14;"));

            iconPane = new StackPane();
            iconPane.setPrefSize(58, 58);
            iconPane.setMinSize(58, 58);
            iconPane.setStyle("-fx-background-color: -app-panel; -fx-background-radius: 12;");

            VBox info = new VBox(6);
            HBox.setHgrow(info, Priority.ALWAYS);
            info.setMaxWidth(Double.MAX_VALUE);

            titleLabel = new Label();
            titleLabel.setStyle("-fx-text-fill: -app-text-strong; -fx-font-size: 17px; -fx-font-weight: bold;");
            // 强制单行显示，超出用 ...
            titleLabel.setWrapText(false);
            titleLabel.setMaxWidth(Double.MAX_VALUE);

            descLabel = new Label();
            descLabel.setStyle("-fx-text-fill: -app-text-muted-3; -fx-font-size: 12px;");
            // 强制单行显示，超出用 ...
            descLabel.setWrapText(false);
            descLabel.setMaxWidth(Double.MAX_VALUE);

            HBox meta = new HBox(10);
            authorLabel = new Label();
            dlLabel = new Label();
            timeLabel = new Label();
            srcLabel = new Label();
            for (Label l : new Label[]{authorLabel, dlLabel, timeLabel, srcLabel}) {
                l.setStyle("-fx-text-fill: -app-text-muted-2; -fx-font-size: 12px; -fx-font-weight: bold;");
            }
            meta.getChildren().addAll(authorLabel, dlLabel, timeLabel, srcLabel);

            info.getChildren().addAll(titleLabel, descLabel, meta);

            downloadBtn = new Button("下载");
            downloadBtn.setStyle("-fx-background-color: #9ca3af; -fx-text-fill: #111827; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 8 20;");
            // 【核心修复】设置最小宽度，防止按钮被压缩成 ...
            downloadBtn.setMinWidth(Region.USE_PREF_SIZE);

            downloadBtn.setOnMouseEntered(e -> downloadBtn.setStyle("-fx-background-color: #6b7280; -fx-text-fill: #111827; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 8 20;"));
            downloadBtn.setOnMouseExited(e -> downloadBtn.setStyle("-fx-background-color: #9ca3af; -fx-text-fill: #111827; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 8 20;"));

            starBtn = new Button();
            starBtn.setGraphic(Icons.node("star", 15, "#6b7280"));
            starBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: -app-text-dim-2;"
                    + " -fx-font-size: 18px; -fx-cursor: hand; -fx-padding: 0;");
            starBtn.setMinSize(26, 26);
            starBtn.setPrefSize(26, 26);

            HBox actions = new HBox(4, starBtn, downloadBtn);
            actions.setAlignment(Pos.CENTER_RIGHT);
            actions.setMinWidth(Region.USE_PREF_SIZE);
            card.getChildren().addAll(iconPane, info, actions);
        }

        @Override
        protected void updateItem(ModpackProject project, boolean empty) {
            super.updateItem(project, empty);
            if (empty || project == null) {
                setGraphic(null);
                return;
            }

            titleLabel.setText(project.getTitle());
            descLabel.setText(project.getDescription());
            authorLabel.setText("👤 " + project.getAuthor());
            dlLabel.setText("⬇ " + project.getFormattedDownloads());
            timeLabel.setText("⏱ " + project.getUpdated());
            srcLabel.setText("🌐 " + project.getSource());

            // 【核心修复】使用异步加载，防止网络阻塞UI线程
            iconPane.getChildren().clear();
            Label placeholder = new Label(project.getTitle().substring(0, 1).toUpperCase());
            placeholder.setStyle("-fx-text-fill: -app-text-muted-2; -fx-font-size: 28px; -fx-font-weight: bold;");
            iconPane.getChildren().add(placeholder);

            if (project.getIconUrl() != null && !project.getIconUrl().isEmpty()) {
                String urlStr = project.getIconUrl();
                if (!urlStr.startsWith("http")) {
                    urlStr = "https://" + urlStr;
                }
                final String finalUrl = urlStr;

                CompletableFuture.runAsync(() -> {
                    try {
                        HttpRequest req = HttpRequest.newBuilder(URI.create(IconProxy.resolve(finalUrl)))
                                .timeout(java.time.Duration.ofSeconds(10))
                                .GET().build();
                        HttpResponse<InputStream> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofInputStream());
                        if (resp.statusCode() == 200) {
                            byte[] bytes = resp.body().readAllBytes();
                            Platform.runLater(() -> {
                                // 防止复用导致图片错乱
                                if (getItem() == project) {
                                    Image img = decodeIcon(bytes);
                                    if (img != null && !img.isError()) {
                                        ImageView iconView = new ImageView(img);
                                        iconView.setFitWidth(70);
                                        iconView.setFitHeight(70);
                                        Rectangle clip = new Rectangle(70, 70);
                                        clip.setArcWidth(20); clip.setArcHeight(20);
                                        iconView.setClip(clip);
                                        iconPane.getChildren().setAll(iconView);
                                    }
                                }
                            });
                        }
                    } catch (Exception ignored) {}
                });
            }

            boolean isMod = "mod".equals(project.getProjectType());
            downloadBtn.setText("下载");
            downloadBtn.setOnAction(e -> {
                Toast.info("开始解析 " + project.getTitle() + " 的下载地址…");
                logArea.appendText("📥 开始解析 [" + project.getTitle() + "] 的下载链路...\n");
                fetchVersionsAndDownload(project.getId(), project.getTitle(), project.getProjectType());
            });
            card.setOnMouseClicked(e -> {
                if (isMod) openCommunityModDetail(project);
            });

            boolean favorite = Favorites.contains(project.getId());
            starBtn.setGraphic(Icons.node("star", 15, favorite ? "#eab308" : "#6b7280"));
            starBtn.setOnAction(e -> {
                e.consume();
                Favorites.toggle(new Favorites.Fav(project.getId(), project.getTitle(),
                        project.getProjectType(), project.getIconUrl(), project.getDownloads(), System.currentTimeMillis()));
                boolean now = Favorites.contains(project.getId());
                starBtn.setGraphic(Icons.node("star", 15, now ? "#eab308" : "#6b7280"));
                Toast.ok(now ? "已收藏 " + project.getTitle() : "已取消收藏 " + project.getTitle());
            });

            setGraphic(card);
        }
    }

    // ================== 图标解码 ==================

    /** 先按 JavaFX 原生解码；失败（Modrinth 的 .webp）再用 ImageIO + webp 插件兜底。 */
    private Image decodeIcon(byte[] bytes) {
        try {
            Image image = new Image(new ByteArrayInputStream(bytes));
            if (!image.isError() && image.getWidth() > 0) return image;
        } catch (Exception ignored) {
        }
        try {
            java.awt.image.BufferedImage buffered =
                    javax.imageio.ImageIO.read(new ByteArrayInputStream(bytes));
            if (buffered != null && buffered.getWidth() > 0) return toFxImage(buffered);
        } catch (Exception ignored) {
        }
        return null;
    }

    private WritableImage toFxImage(java.awt.image.BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();
        WritableImage image = new WritableImage(width, height);
        PixelWriter writer = image.getPixelWriter();
        int[] row = new int[width];
        for (int y = 0; y < height; y++) {
            source.getRGB(0, y, width, 1, row, 0, width);
            for (int x = 0; x < width; x++) {
                writer.setArgb(x, y, row[x]);
            }
        }
        return image;
    }

    // ================== 社区资源检索逻辑 ==================
    /** 源不可用时的轻提示：不打断、不要求确认，15 秒内只提示一次。 */
    private void notifySourcesUnavailable(String message) {
        long now = System.currentTimeMillis();
        if (now - lastMirrorNoticeAt < 15000) return;
        lastMirrorNoticeAt = now;
        Toast.info(message);
    }

    private void performSearch() {
        searchOffset = 0;
        searchTotal = -1;
        loadingMoreResults = false;
        modpackData.clear();
        if (loadMoreHint != null) {
            loadMoreHint.setText("");
        }
        statusBox.setVisible(true);
        statusLabel.setText("正在从 Modrinth 获取数据…（失败自动切 MCIM 镜像）");
        loadSearchPage(true);
    }

    /**
     * 拉一页社区资源（Modrinth 搜索）。
     * 滚动到底 / 点「加载更多」时接着拉，列表不再只有固定 20 个。
     */
    private void loadSearchPage(boolean first) {
        if (loadingMoreResults) return;
        if (searchTotal >= 0 && searchOffset >= searchTotal) return;
        loadingMoreResults = true;
        final int offset = searchOffset;

        String query = searchField.getText() == null ? "" : searchField.getText().trim();
        String gameVer = gameVersionCombo.getValue();
        String loader = loaderCombo.getValue();
        String sort = sortCombo.getValue();

        final String pType;
        if (currentProjectType.equals("模组")) pType = "mod";
        else if (currentProjectType.equals("资源包")) pType = "resourcepack";
        else if (currentProjectType.equals("光影包")) pType = "shader";
        else if (currentProjectType.equals("数据包")) pType = "datapack";
        else pType = "modpack";

        JsonArray facets = new JsonArray();
        JsonArray typeArr = new JsonArray();
        typeArr.add("project_type:" + pType);
        facets.add(typeArr);
        if (gameVer != null && !gameVer.equals("任意")) {
            JsonArray verArr = new JsonArray();
            verArr.add("versions:" + gameVer);
            facets.add(verArr);
        }
        if (!currentProjectType.equals("数据包") && loader != null && !loader.equals("任意")) {
            JsonArray loaderArr = new JsonArray();
            loaderArr.add("categories:" + loader.toLowerCase());
            facets.add(loaderArr);
        }

        String index = "relevance";
        if ("下载量".equals(sort)) index = "downloads";
        else if ("更新时间".equals(sort)) index = "updated";

        try {
            String encodedFacets = URLEncoder.encode(facets.toString(), StandardCharsets.UTF_8.toString());
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
            String urlStr = "https://api.modrinth.com/v2/search?query=" + encodedQuery
                    + "&facets=" + encodedFacets + "&index=" + index
                    + "&limit=24&offset=" + offset;

            HttpRequest req = HttpRequest.newBuilder(URI.create(urlStr))
                    .header("User-Agent", "PCL-CE-Launcher/1.0")
                    .GET()
                    .timeout(java.time.Duration.ofSeconds(15))
                    .build();

            CompletableFuture.supplyAsync(() -> {
                try {
                    HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
                    return resp.body();
                } catch (Exception e) {
                    return null;
                }
            }).thenAccept(body -> Platform.runLater(() -> {
                loadingMoreResults = false;
                if (body == null || !body.startsWith("{")) {
                    statusBox.setVisible(true);
                    statusBox.getChildren().setAll(statusLabel);
                    statusLabel.setText("加载失败：资源站不可用，点击这里重试");
                    statusBox.setOnMouseClicked(e -> {
                        statusLabel.setText("正在重新加载...");
                        loadSearchPage(true);
                    });
                    updateLoadMoreButton();
                    return;
                }
                statusBox.setVisible(false);
                JsonObject json = new Gson().fromJson(body, JsonObject.class);
                JsonArray hits = json.getAsJsonArray("hits");
                if (json.has("total_hits") && !json.get("total_hits").isJsonNull()) {
                    searchTotal = json.get("total_hits").getAsInt();
                }
                if (hits == null || hits.size() == 0) {
                    if (first) statusLabel.setText("未找到匹配的项目。");
                    updateLoadMoreButton();
                    return;
                }

                for (int i = 0; i < hits.size(); i++) {
                    JsonObject hit = hits.get(i).getAsJsonObject();
                    String id = hit.get("project_id").getAsString();
                    boolean exists = modpackData.stream().anyMatch(p -> id.equals(p.getId()));
                    if (exists) continue;
                    String title = hit.get("title").getAsString();
                    String desc = hit.get("description").getAsString();
                    String author = hit.get("author").getAsString();
                    String iconUrl = hit.has("icon_url") && !hit.get("icon_url").isJsonNull()
                            ? hit.get("icon_url").getAsString() : null;
                    long downloads = hit.get("downloads").getAsLong();
                    String updated = hit.get("date_modified").getAsString().substring(0, 10);
                    modpackData.add(new ModpackProject(id, title, desc, author, iconUrl, downloads, updated,
                            "Modrinth", pType));
                }
                searchOffset = offset + hits.size();
                if (searchTotal < 0 && hits.size() < 24) searchTotal = searchOffset;
                updateLoadMoreButton();
            })).exceptionally(ex -> {
                Platform.runLater(() -> {
                    loadingMoreResults = false;
                    statusBox.setVisible(true);
                    statusBox.getChildren().setAll(statusLabel);
                    statusLabel.setText("请求失败：" + ex.getMessage() + "（点击重试）");
                    statusBox.setOnMouseClicked(ev -> {
                        statusLabel.setText("正在重新加载...");
                        loadSearchPage(true);
                    });
                    updateLoadMoreButton();
                });
                return null;
            });
        } catch (Exception e) {
            loadingMoreResults = false;
            statusLabel.setText("请求参数构建失败: " + e.getMessage());
        }
    }

    private void updateLoadMoreButton() {
        if (loadMoreHint == null) return;
        boolean hasMore = searchTotal < 0 || searchOffset < searchTotal;
        if (modpackData.isEmpty()) {
            loadMoreHint.setText("");
            return;
        }
        loadMoreHint.setText(hasMore
                ? "已显示 " + modpackData.size() + (searchTotal >= 0 ? " / " + searchTotal : "") + " · 滚动到底自动加载"
                : "已加载全部 · 共 " + modpackData.size() + " 个");
    }

    /** 让用户在安装前选择整合包 / 资源版本。 */
    private int chooseModpackVersion(String title, JsonArray versions) {
        java.util.List<String> labels = new java.util.ArrayList<>();
        int limit = Math.min(30, versions.size());
        for (int i = 0; i < limit; i++) {
            JsonObject version = versions.get(i).getAsJsonObject();
            String number = version.has("version_number") ? version.get("version_number").getAsString() : "?";
            String type = version.has("version_type") ? version.get("version_type").getAsString() : "";
            String games = "";
            if (version.has("game_versions") && version.get("game_versions").isJsonArray()) {
                JsonArray gameArray = version.getAsJsonArray("game_versions");
                java.util.List<String> parts = new java.util.ArrayList<>();
                for (int j = 0; j < Math.min(3, gameArray.size()); j++) {
                    parts.add(gameArray.get(j).getAsString());
                }
                games = String.join(", ", parts);
            }
            labels.add(number + "  ·  " + type + (games.isBlank() ? "" : "  ·  " + games));
        }
        if (labels.isEmpty()) return -1;
        javafx.scene.control.ChoiceDialog<String> dialog =
                new javafx.scene.control.ChoiceDialog<>(labels.get(0), labels);
        dialog.setTitle(title + " 版本选择");
        dialog.setHeaderText("选择要安装的版本（默认最新）");
        dialog.setContentText("版本：");
        java.util.Optional<String> picked = dialog.showAndWait();
        return picked.map(labels::indexOf).orElse(-1);
    }

    /** 下载页模组卡：在下载内容区直接打开集成详情页，返回时保留浏览进度。 */
    private void openCommunityModDetail(com.example.demo.ModpackProject project) {
        String id = currentInstanceId();
        if (id == null) {
            Toast.info("先选择一个实例，再查看模组详情");
            return;
        }
        if (modDetailPanel == null) {
            modDetailPanel = new VBox(10);
            modDetailPanel.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            modDetailPanel.setAlignment(Pos.TOP_LEFT);
            downloadContentArea.getChildren().add(modDetailPanel);
        }

        Instance instance = Instance.of(new File(config.getGameDir()), id);
        VBox page = buildModPageShell(project, instance);
        modDetailPanel.getChildren().setAll(page);
        showDownloadPanel(modDetailPanel);

        String url = "https://api.modrinth.com/v2/project/" + project.getId() + "/version";
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", "PCL-CE-Launcher/1.0")
                .GET().timeout(java.time.Duration.ofSeconds(15)).build();
        CompletableFuture.supplyAsync(() -> {
            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                return response.statusCode() == 200 ? response.body() : null;
            } catch (Exception e) {
                return null;
            }
        }).thenAccept(body -> Platform.runLater(() -> {
            page.getChildren().removeIf(node -> node instanceof Label label
                    && label.getText() != null && label.getText().startsWith("正在读取"));
            if (body == null || !body.startsWith("[")) {
                Label error = new Label("版本列表加载失败，请检查网络后重试。");
                error.setStyle("-fx-text-fill: #f87171; -fx-font-size: 12px;");
                page.getChildren().add(error);
                return;
            }
            JsonArray all = new Gson().fromJson(body, JsonArray.class);
            page.getChildren().add(buildModVersionList(project, all, "全部", "全部"));
        }));
    }

    private VBox buildModPageShell(com.example.demo.ModpackProject project, Instance instance) {
        VBox page = new VBox(10);
        page.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        Button back = new Button("返回下载");
        back.setGraphic(Icons.node("back", 13, "#9ca3af"));
        back.setGraphicTextGap(4);
        back.setStyle("-fx-background-color: transparent; -fx-text-fill: -app-text-dim;"
                + " -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 2 4;");
        back.setOnAction(e -> showDownloadPanel(communityPanel));

        Label title = new Label(project.getTitle());
        title.setStyle("-fx-text-fill: -app-text; -fx-font-size: 20px; -fx-font-weight: bold;");
        Label source = new Label(project.getSource());
        source.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11px;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox top = new HBox(10, back, title, spacer, source);
        top.setAlignment(Pos.CENTER_LEFT);

        Label desc = new Label(project.getDescription());
        desc.setWrapText(true);
        desc.setStyle("-fx-text-fill: -app-text-2; -fx-font-size: 13px;");
        Label filter = new Label("显示全部游戏版本与加载器，按版本/加载器折叠；不会自动套用当前实例。");
        filter.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11px;");
        filter.setWrapText(true);
        VBox hero = new VBox(6, desc, filter);
        hero.setPadding(new Insets(14));
        hero.setStyle("-fx-background-color: -app-card; -fx-background-radius: 12;"
                + " -fx-border-color: -app-border-2; -fx-border-radius: 12;");

        Label loading = new Label("正在读取可安装版本…");
        loading.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 12px;");
        page.getChildren().addAll(top, hero, loading);
        return page;
    }

    private VBox buildModVersionList(com.example.demo.ModpackProject project, JsonArray versions,
                                     String ignoredMc, String ignoredLoader) {
        VBox box = new VBox(8);
        Label heading = new Label("全部可下载文件（" + versions.size() + "）· 按游戏版本 / 加载器折叠");
        heading.setStyle("-fx-text-fill: -app-text-2; -fx-font-size: 12px; -fx-font-weight: bold;");
        box.getChildren().add(heading);

        java.util.LinkedHashMap<String, java.util.List<JsonObject>> groups = new java.util.LinkedHashMap<>();
        for (int i = 0; i < versions.size(); i++) {
            JsonObject version = versions.get(i).getAsJsonObject();
            String mcGroup = firstJsonValue(version, "game_versions");
            String loaderGroup = firstJsonValue(version, "loaders");
            String key = mcGroup + " / " + loaderGroup;
            groups.computeIfAbsent(key, k -> new java.util.ArrayList<>()).add(version);
        }
        boolean first = true;
        for (var entry : groups.entrySet()) {
            VBox rows = new VBox(6);
            for (JsonObject version : entry.getValue()) {
                rows.getChildren().add(buildModVersionRow(project, version));
            }
            ScrollPane groupScroll = new ScrollPane(rows);
            groupScroll.setFitToWidth(true);
            groupScroll.setPrefViewportHeight(Math.min(300, 74 * entry.getValue().size()));
            groupScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-border-color: transparent;");
            javafx.scene.control.TitledPane pane = new javafx.scene.control.TitledPane(
                    entry.getKey() + "  ·  " + entry.getValue().size() + " 个文件", groupScroll);
            pane.setExpanded(first);
            first = false;
            pane.setStyle("-fx-text-fill: -app-text-2; -fx-font-size: 12px;");
            box.getChildren().add(pane);
        }
        if (versions.isEmpty()) {
            Label hint = new Label("这个项目没有可下载文件。");
            hint.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 12px;");
            box.getChildren().add(hint);
        }
        ScrollPane scroll = new ScrollPane(box);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        VBox wrapper = new VBox(scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return wrapper;
    }

    private String firstJsonValue(JsonObject object, String key) {
        if (!object.has(key) || !object.get(key).isJsonArray()) return "其他";
        JsonArray array = object.getAsJsonArray(key);
        return array.isEmpty() ? "其他" : array.get(0).getAsString();
    }

    private HBox buildModVersionRow(com.example.demo.ModpackProject project, JsonObject version) {
        String number = jsonString(version, "version_number");
        String type = jsonString(version, "version_type");
        JsonArray files = version.has("files") && version.get("files").isJsonArray()
                ? version.getAsJsonArray("files") : new JsonArray();
        String fileName = "";
        long size = 0;
        if (!files.isEmpty()) {
            JsonObject file = files.get(0).getAsJsonObject();
            fileName = jsonString(file, "filename");
            size = file.has("size") ? file.get("size").getAsLong() : 0;
        }
        int deps = version.has("dependencies") && version.get("dependencies").isJsonArray()
                ? version.getAsJsonArray("dependencies").size() : 0;

        Label name = new Label(number.isBlank() ? "未知版本" : number);
        name.setStyle("-fx-text-fill: -app-text-strong; -fx-font-size: 13px; -fx-font-weight: bold;");
        Label typeLabel = new Label("release".equals(type) ? "正式版" : type);
        typeLabel.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 10px;");
        HBox titleRow = new HBox(8, name, typeLabel);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label fileLabel = new Label((fileName.isBlank() ? "无文件名" : fileName)
                + (size > 0 ? " · " + (size / 1024) + " KB" : ""));
        fileLabel.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11px;");
        Label meta = new Label("支持 " + jsonArrayText(version, "game_versions")
                + " · " + jsonArrayText(version, "loaders")
                + (deps > 0 ? " · 前置 " + deps + " 个" : ""));
        meta.setStyle("-fx-text-fill: -app-text-dim-2; -fx-font-size: 10.5px;");
        VBox info = new VBox(3, titleRow, fileLabel, meta);
        HBox.setHgrow(info, Priority.ALWAYS);

        Button download = new Button("下载此文件");
        download.setStyle("-fx-background-color: #14b8a6; -fx-text-fill: white; -fx-font-weight: bold;"
                + " -fx-background-radius: 8; -fx-padding: 6 14; -fx-cursor: hand;");
        download.setOnAction(e -> {
            download.setDisable(true);
            download.setText("下载中…");
            downloadModVersion(version, project.getTitle(), download);
        });

        HBox row = new HBox(12, info, download);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 12, 10, 12));
        row.setStyle("-fx-background-color: -app-row2; -fx-background-radius: 10;"
                + " -fx-border-color: -app-border-soft; -fx-border-radius: 10;");
        return row;
    }

    private void downloadModVersion(JsonObject version, String title, Button button) {
        try {
            JsonArray files = version.getAsJsonArray("files");
            if (files == null || files.isEmpty()) throw new RuntimeException("该版本没有文件");
            JsonObject target = files.get(0).getAsJsonObject();
            String url = jsonString(target, "url");
            String fileName = jsonString(target, "filename");
            File mods = InstanceDirs.mods(new File(config.getGameDir()), currentInstanceId(), config.isIsolation());
            if (!mods.isDirectory()) mods.mkdirs();
            File save = new File(mods, fileName);
            logArea.appendText("📥 下载 " + title + " → " + fileName + "\n");
            downloadFileAsync(url, save, logArea, saved -> {
                logArea.appendText("✅ 已安装到 mods: " + saved.getName()
                        + "（前置依赖可在实例 → 模组里检查）\n");
                if (button != null) {
                    button.setText("已下载");
                    button.setDisable(false);
                }
            });
        } catch (Exception e) {
            logArea.appendText("❌ 下载失败: " + e.getMessage() + "\n");
            if (button != null) {
                button.setDisable(false);
                button.setText("下载此文件");
            }
        }
    }

    private boolean containsJson(JsonObject object, String key, String value) {
        if (value == null || value.isBlank() || !object.has(key) || !object.get(key).isJsonArray()) return false;
        for (var element : object.getAsJsonArray(key)) {
            if (value.equalsIgnoreCase(element.getAsString())) return true;
        }
        return false;
    }

    private String jsonString(JsonObject object, String key) {
        return object != null && object.has(key) && !object.get(key).isJsonNull()
                ? object.get(key).getAsString() : "";
    }

    private String jsonArrayText(JsonObject object, String key) {
        if (!object.has(key) || !object.get(key).isJsonArray()) return "未知";
        StringBuilder out = new StringBuilder();
        for (var element : object.getAsJsonArray(key)) {
            if (out.length() > 0) out.append(" / ");
            out.append(element.getAsString());
        }
        return out.length() == 0 ? "未知" : out.toString();
    }

    private void fetchVersionsAndDownload(String projectId, String projectTitle, String projectType) {
        String urlStr = "https://api.modrinth.com/v2/project/" + projectId + "/version";
        HttpRequest req = HttpRequest.newBuilder(URI.create(urlStr))
                .header("User-Agent", "PCL-CE-Launcher/1.0")
                .GET()
                .timeout(java.time.Duration.ofSeconds(15))
                .build();

        CompletableFuture.supplyAsync(() -> {
            try {
                HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
                return resp.body();
            } catch (Exception e) {
                return null;
            }
        }).thenAccept(body -> {
            Platform.runLater(() -> {
                if (body == null || !body.startsWith("[")) {
                    logArea.appendText("❌ 获取 " + projectTitle + " 版本列表失败。\n");
                    return;
                }
                JsonArray arr = new Gson().fromJson(body, JsonArray.class);
                if (arr.size() == 0) {
                    logArea.appendText("❌ " + projectTitle + " 暂无可用版本。\n");
                    return;
                }

                int chosen = chooseModpackVersion(projectTitle, arr);
                if (chosen < 0) return;
                JsonObject latestVersion = arr.get(chosen).getAsJsonObject();
                String versionName = latestVersion.get("version_number").getAsString();
                JsonArray files = latestVersion.getAsJsonArray("files");

                if (files != null && files.size() > 0) {
                    JsonObject targetFile = null;
                    for (int i = 0; i < files.size(); i++) {
                        JsonObject file = files.get(i).getAsJsonObject();
                        if (file.has("primary") && file.get("primary").getAsBoolean()) {
                            targetFile = file;
                            break;
                        }
                    }
                    if (targetFile == null) targetFile = files.get(0).getAsJsonObject();

                    String downloadUrl = targetFile.get("url").getAsString();
                    String fileName = targetFile.get("filename").getAsString();
                    long fileSize = targetFile.get("size").getAsLong() / 1024;

                    logArea.appendText("🎯 找到适配版本: v" + versionName + " (" + fileName + ", " + fileSize + " KB)\n");
                    logArea.appendText("🔗 下载地址: " + downloadUrl + "\n");

                    File instanceRoot = InstanceDirs.root(new File(config.getGameDir()),
                            currentInstanceId(), config.isIsolation());
                    File targetDir = "整合包".equals(projectType)
                            ? new File(new File(config.getGameDir(), "downloads"), "modpack")
                            : new File(instanceRoot, installDirFor(projectType));
                    if (!targetDir.exists()) targetDir.mkdirs();
                    File saveFile = new File(targetDir, fileName);

                    downloadFileAsync(downloadUrl, saveFile, logArea, saved -> {
                        if (saved.getName().toLowerCase().endsWith(".mrpack")) {
                            installMrpack(saved);
                        } else if ("模组".equals(projectType) || "资源包".equals(projectType)
                                || "光影包".equals(projectType) || "数据包".equals(projectType)) {
                            logArea.appendText("✅ 已放入 " + installDirFor(projectType) + "/，启动游戏即可生效\n");
                        }
                    });
                } else {
                    logArea.appendText("⚠️ 该版本下未找到可下载的文件。\n");
                }
            });
        });
    }

    /** 不同资源类型装到游戏目录里对应的文件夹，而不是全塞进 downloads。 */
    private String installDirFor(String projectType) {
        return switch (projectType) {
            case "模组" -> "mods";
            case "资源包" -> "resourcepacks";
            case "光影包" -> "shaderpacks";
            case "数据包" -> "datapacks";
            default -> "downloads/modpack";
        };
    }

    // ================== 整合包（.mrpack）安装 ==================

    /** 线程安全 + 空安全的日志输出（后台线程调用会自动切回 UI 线程）。 */
    private void logLine(String text) {
        String line = text.endsWith("\n") ? text : text + "\n";
        if (logArea == null) {
            System.out.print("[log] " + line);
            return;
        }
        if (Platform.isFxApplicationThread()) {
            logArea.appendText(line);
        } else {
            Platform.runLater(() -> logArea.appendText(line));
        }
    }

    /** 追加日志时保留用户手动滚动的位置（只有本来就在底部才自动跟随）。 */
    private void appendKeepingScroll(String line) {
        double before = logArea.getScrollTop();
        boolean wasAtBottom = before >= 0.985;
        logArea.appendText(line);
        if (!wasAtBottom) {
            logArea.setScrollTop(before);
        }
    }

    /** 给面板回调用的日志入口。 */
    private void logMessage(String message) {
        logLine(message);
    }

    private void importModpackDialog() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("导入整合包");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("整合包", "*.zip", "*.mrpack"),
                new FileChooser.ExtensionFilter("所有文件", "*.*"));
        File file = chooser.showOpenDialog(stageRef);
        if (file != null) importModpack(file);
    }

    /** 导入 PCL-CE / Modrinth / CurseForge / HMCL 整合包：装好游戏与加载器后解包到实例目录。 */
    private void importModpack(File archive) {
        logLine("📦 开始导入整合包: " + archive.getName());
        CompletableFuture.runAsync(() -> {
            File gameDir = new File(config.getGameDir());
            try {
                ModpackImport.Pack pack = ModpackImport.inspect(archive);
                logLine("   名称: " + pack.name() + "  |  格式: " + pack.format()
                        + "  |  游戏: " + (pack.gameVersion().isBlank() ? "未知" : pack.gameVersion())
                        + (pack.hasLoader() ? "  |  " + pack.loader() + " " + pack.loaderVersion() : "  |  原版"));
                String instanceId = null;
                if (!pack.gameVersion().isBlank()) {
                    MinecraftVersion mc = allMinecraftVersions.stream()
                            .filter(v -> v.getId().equals(pack.gameVersion())).findFirst().orElse(null);
                    if (mc == null) {
                        logLine("⚠ 版本清单里没有 " + pack.gameVersion() + "，只解包文件，请手动安装对应版本");
                    } else {
                        ensureInstalled(mc.getId(), mc.getUrl(), this::logLine);
                        if (pack.hasLoader()) {
                            String javaPath = loaderJava(gameDir, pack.loader());
                            instanceId = switch (pack.loader()) {
                                case "fabric" -> LoaderInstaller.installFabric(
                                        gameDir, mc.getId(), pack.loaderVersion(), this::logLine).get();
                                case "quilt" -> LoaderInstaller.installQuilt(
                                        gameDir, mc.getId(), pack.loaderVersion(), this::logLine).get();
                                case "forge" -> LoaderInstaller.installForge(
                                        gameDir, mc.getId(), javaPath, pack.loaderVersion(), this::logLine).get();
                                case "neoforge" -> LoaderInstaller.installNeoForge(
                                        gameDir, mc.getId(), javaPath, pack.loaderVersion(), this::logLine).get();
                                default -> mc.getId();
                            };
                        } else {
                            instanceId = mc.getId();
                        }
                    }
                }
                File target = instanceId == null
                        ? gameDir : InstanceDirs.root(gameDir, instanceId, config.isIsolation());
                if (!target.isDirectory() && !target.mkdirs()) {
                    throw new RuntimeException("无法创建实例目录: " + target.getAbsolutePath());
                }
                int count = ModpackImport.extract(archive, target, this::logLine);
                Files.writeString(new File(target, "modpack-source.txt").toPath(),
                        "name=" + pack.name() + "\nformat=" + pack.format()
                                + "\narchive=" + archive.getAbsolutePath() + "\n", StandardCharsets.UTF_8);
                if (instanceId != null) {
                    InstanceMeta.set(gameDir, instanceId, pack.name(), pack.format() + " 导入");
                }
                logLine("✅ 导入完成: " + pack.name() + " → " + target.getAbsolutePath()
                        + "（" + count + " 个文件）");
                Platform.runLater(() -> {
                    refreshInstalledVersions();
                    Toast.ok("已导入整合包: " + pack.name());
                    switchPage("instances");
                });
            } catch (Exception e) {
                logLine("❌ 导入失败: " + e.getMessage());
                Platform.runLater(() -> Toast.error("导入失败，详情见日志"));
            }
        });
    }

    private String loaderJava(File gameDir, String loader) {
        int major = "neoforge".equals(loader) ? 21 : 17;
        File bundled = new File(new File(new File(gameDir, "java"), "zulu" + major), "bin/java.exe");
        if (bundled.isFile()) return bundled.getAbsolutePath();
        if (config.getJavaPath() != null && !config.getJavaPath().isBlank()) {
            return config.getJavaPath();
        }
        try {
            return JavaManager.ensureJavaAsync(gameDir, major, this::logLine)
                    .get(300, TimeUnit.SECONDS);
        } catch (Exception e) {
            return bundled.getAbsolutePath();
        }
    }

    private void installMrpack(File mrpack) {
        logLine("📦 开始安装整合包: " + mrpack.getName());
        CompletableFuture.runAsync(() -> {
            File gameDir = new File(config.getGameDir());
            try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(mrpack)) {
                java.util.zip.ZipEntry indexEntry = zip.getEntry("modrinth.index.json");
                if (indexEntry == null) {
                    logLine("❌ 不是有效的 .mrpack（缺少 modrinth.index.json）");
                    return;
                }
                String indexJson = new String(zip.getInputStream(indexEntry).readAllBytes(), StandardCharsets.UTF_8);
                JsonObject index = com.google.gson.JsonParser.parseString(indexJson).getAsJsonObject();

                String packName = index.has("name") ? index.get("name").getAsString() : mrpack.getName();
                logLine("   整合包: " + packName);

                if (index.has("dependencies") && index.get("dependencies").isJsonObject()) {
                    JsonObject deps = index.getAsJsonObject("dependencies");
                    StringBuilder line = new StringBuilder();
                    for (Map.Entry<String, com.google.gson.JsonElement> entry : deps.entrySet()) {
                        line.append(entry.getKey()).append("=").append(entry.getValue().getAsString()).append("  ");
                    }
                    logLine("   依赖: " + line.toString().trim());
                }

                int downloaded = 0;
                int failed = 0;
                int skipped = 0;
                JsonArray files = index.has("files") ? index.getAsJsonArray("files") : new JsonArray();
                for (com.google.gson.JsonElement el : files) {
                    if (!el.isJsonObject()) continue;
                    JsonObject file = el.getAsJsonObject();
                    if (!file.has("path") || !file.has("downloads")) continue;
                    String path = file.get("path").getAsString();
                    JsonArray urls = file.getAsJsonArray("downloads");
                    if (urls == null || urls.size() == 0) continue;
                    File target = new File(gameDir, path);
                    if (!target.toPath().normalize().startsWith(gameDir.toPath())) continue;
                    if (target.isFile() && target.length() > 1024) {
                        skipped++;
                        continue;
                    }
                    long size = Downloader.downloadFileSync(urls.get(0).getAsString(), target);
                    if (size > 0) downloaded++;
                    else failed++;
                }

                int overrideCount = extractZipDir(zip, "overrides/", gameDir)
                        + extractZipDir(zip, "client-overrides/", gameDir);

                logLine("✅ 整合包安装完成: " + packName);
                logLine("   整合包文件 " + (downloaded + skipped) + " 个（新下载 " + downloaded
                        + "，已存在 " + skipped + "），覆盖文件 " + overrideCount + " 个");
                if (failed > 0) {
                    logLine("   ⚠️ " + failed + " 个文件下载失败，再点一次下载会断点续补");
                }
                logLine("   若整合包指定了加载器版本，请先在「下载 → Minecraft」里安装对应版本");
            } catch (Exception e) {
                logLine("❌ 整合包安装失败: " + e.getMessage());
            }
        });
    }

    private int extractZipDir(java.util.zip.ZipFile zip, String prefix, File targetDir) throws Exception {
        int count = 0;
        java.util.Enumeration<? extends java.util.zip.ZipEntry> entries = zip.entries();
        while (entries.hasMoreElements()) {
            java.util.zip.ZipEntry entry = entries.nextElement();
            String entryName = entry.getName().replace('\\', '/');
            if (!entryName.startsWith(prefix) || entry.isDirectory()) continue;
            File outFile = new File(targetDir, entryName.substring(prefix.length()));
            if (!outFile.toPath().normalize().startsWith(targetDir.toPath())) continue;
            File parent = outFile.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            try (java.io.InputStream in = zip.getInputStream(entry);
                 java.io.FileOutputStream out = new java.io.FileOutputStream(outFile)) {
                in.transferTo(out);
            }
            count++;
        }
        return count;
    }

    private void downloadFileAsync(String url, File targetFile, TextArea logArea) {
        downloadFileAsync(url, targetFile, logArea, null);
    }

    private void downloadFileAsync(String url, File targetFile, TextArea logArea,
                                   java.util.function.Consumer<File> onDone) {
        logArea.appendText("📥 正在下载: " + targetFile.getName() + "\n");
        CompletableFuture.runAsync(() -> {
            try {
                long size = Downloader.downloadFileSync(url, targetFile);
                Platform.runLater(() -> {
                    if (size <= 0) {
                        logArea.appendText("❌ 下载失败: " + targetFile.getName() + "\n");
                        return;
                    }
                    logArea.appendText("✅ 下载完成: " + targetFile.getAbsolutePath() + "\n");
                    if (onDone != null) onDone.accept(targetFile);
                });
            } catch (Exception e) {
                Platform.runLater(() -> logArea.appendText("❌ 下载失败: " + e.getMessage() + "\n"));
            }
        });
    }

    // ================== 启动页业务逻辑 ==================
    /** 从本机注册表恢复登录状态（同机免重复登录，换机不带走）。 */
    /** 登录后收起输入区，只保留玩家名；未登录时恢复登录表单。 */
    private void applySessionUi(boolean loggedIn) {
        if (loginCard == null) return;
        loginCard.setVisible(!loggedIn);
        loginCard.setManaged(!loggedIn);
        userTypeLabel.setVisible(false);
        userTypeLabel.setManaged(false);
        uuidLabel.setVisible(!loggedIn);
        uuidLabel.setManaged(!loggedIn);
        logoutButton.setVisible(loggedIn);
        logoutButton.setManaged(loggedIn);
        if (playerInfoBox != null) {
            playerInfoBox.setVisible(loggedIn);
            playerInfoBox.setManaged(loggedIn);
        }
    }

    private Label infoCaption(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 10px; -fx-font-weight: bold;");
        return label;
    }

    private HBox infoLine(String key, Label value) {
        Label keyLabel = new Label(key);
        keyLabel.setMinWidth(62);
        keyLabel.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 10.5px;");
        value.setStyle("-fx-text-fill: -app-text-2; -fx-font-size: 11px;");
        value.setMaxWidth(205);
        HBox row = new HBox(8, keyLabel, value);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private void updatePlayerInfo(String type, String server, UserSession session) {
        if (playerInfoBox == null) return;
        playerInfoType.setText(type == null || type.isBlank() ? "玩家" : type);
        String uuid = session != null && session.getUuid() != null
                ? session.getUuid().toString().substring(0, 13) + "…" : "—";
        playerInfoUuid.setText(uuid);
        String host = server == null ? "" : server;
        try {
            if (host.startsWith("http")) {
                String parsed = java.net.URI.create(host).getHost();
                if (parsed != null) host = parsed;
            }
        } catch (Exception ignored) {
        }
        playerInfoServer.setText(host.isBlank() ? "官方 / 离线" : host);
        String instance = currentInstanceId();
        playerInfoInstance.setText(instance == null ? "未选择" : instance);
        long seconds = 0;
        try {
            if (instance != null) seconds = Playtime.seconds(new File(config.getGameDir()), instance);
        } catch (Exception ignored) {
        }
        playerInfoPlaytime.setText(Playtime.format(seconds));
    }

    private void setGameStatus(boolean running) {
        if (gameStatusDot == null) return;
        gameStatusDot.setFill(Color.web(running ? "#4CAF50" : "#6b7280"));
        gameStatusDot.setOpacity(1);
        if (gameStatusLabel != null) gameStatusLabel.setText(running ? "游戏运行中" : "就绪");
        if (gameStatusPulse != null) {
            gameStatusPulse.stop();
            gameStatusPulse = null;
        }
        if (running && Motion.isEnabled()) {
            gameStatusPulse = new javafx.animation.FadeTransition(javafx.util.Duration.seconds(1.1), gameStatusDot);
            gameStatusPulse.setFromValue(1);
            gameStatusPulse.setToValue(0.35);
            gameStatusPulse.setAutoReverse(true);
            gameStatusPulse.setCycleCount(javafx.animation.Animation.INDEFINITE);
            gameStatusPulse.play();
        }
    }

    private void logout() {
        currentSession = null;
        SessionStore.clear();
        usernameLabel.setText("未登录");
        userTypeLabel.setText("请在下方登录");
        uuidLabel.setText("");
        avatarImageView.setImage(null);
        passwordField.clear();
        loginBtn.setDisable(false);
        loginBtnLabel.setText("登录");
        loginTypeCombo.setValue("离线登录");
        applySessionUi(false);
        logMessage("已退出登录");
    }

    private void restoreSessionFromRegistry() {
        applySessionUi(false);
        SessionStore.Saved saved = SessionStore.load();
        if (saved == null || saved.username().isBlank()) return;
        try {
            UserSession session = new UserSession();
            session.setUsername(saved.username());
            session.setAccessToken(saved.accessToken());
            session.setSkinUrl(saved.skinUrl());
            session.setUserType(saved.userType());
            session.setAuthServerUrl(saved.server());
            session.setType("正版登录 (微软)".equals(saved.loginType())
                    ? UserSession.LoginType.MICROSOFT
                    : "第三方外置登录".equals(saved.loginType())
                    ? UserSession.LoginType.THIRD_PARTY : UserSession.LoginType.OFFLINE);
            try {
                if (!saved.uuid().isBlank()) session.setUuid(java.util.UUID.fromString(saved.uuid()));
            } catch (Exception ignored) {
                // UUID 坏了就当没有
            }
            currentSession = session;
            usernameLabel.setText(saved.username());
            applySessionUi(true);
            userTypeLabel.setText(saved.userType().isBlank() ? saved.loginType() : saved.userType());
            updatePlayerInfo(saved.userType().isBlank() ? saved.loginType() : saved.userType(),
                    saved.server(), session);
            userTypeLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #4CAF50;");
            if (session.getUuid() != null) {
                uuidLabel.setText("UUID: " + session.getUuid().toString().substring(0, 13) + "...");
            }
            if (loginTypeComboRef != null) {
                loginTypeComboRef.setValue(loginTypeComboRef.getItems().contains(saved.loginType())
                        ? saved.loginType() : "离线登录");
                boolean thirdParty = "第三方外置登录".equals(saved.loginType());
                if (serverFieldRef != null) {
                    serverFieldRef.setVisible(thirdParty);
                    serverFieldRef.setManaged(thirdParty);
                    if (!saved.server().isBlank()) serverFieldRef.getEditor().setText(saved.server());
                }
            }
            if (saved.skinUrl() != null && !saved.skinUrl().isBlank()) {
                loadSkinHead(saved.skinUrl());
            }
            logArea.appendText("🔐 已从本机注册表恢复登录: " + saved.username() + "\n");
        } catch (Exception e) {
            logArea.appendText("⚠ 登录状态恢复失败: " + e.getMessage() + "\n");
        }
    }
    private void handleLogin(ComboBox<String> loginTypeCombo, ComboBox<String> serverField,
                             TextField usernameField, PasswordField passwordField, Button loginBtn) {
        String type = loginTypeCombo.getValue();
        String username = usernameField.getText().trim();
        String serverUrl = serverField.getEditor().getText().trim();
        String password = passwordField.getText().trim();

        if (!type.equals("正版登录 (微软)") && username.isEmpty()) {
            logArea.appendText("⚠️ 请输入账号/玩家名\n");
            return;
        }
        if (type.equals("第三方外置登录") && (serverUrl.isEmpty() || password.isEmpty())) {
            logArea.appendText("⚠️ 请填写完整的验证服务器地址和密码\n");
            return;
        }

        loginBtn.setDisable(true);
        logArea.appendText("正在以 [" + type + "] 登录...\n");

        AuthManager.loginAsync(type, username, password, serverUrl,
                msg -> Platform.runLater(() -> logArea.appendText(msg + "\n"))).thenAccept(session -> {
            Platform.runLater(() -> {
                loginBtn.setDisable(false);
                if (session != null) {
                    usernameLabel.setText(session.getUsername());
                    applySessionUi(true);
                    userTypeLabel.setText(type);
                    updatePlayerInfo(type, serverUrl, session);
                    userTypeLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #4CAF50;");
                    uuidLabel.setText("UUID: " + session.getUuid().toString().substring(0, 13) + "...");
                    currentSession = session;
                    SessionStore.save(session, type, serverUrl);

                    if (versionComboBox.getItems() != null && !versionComboBox.getItems().isEmpty()) {
                        if (versionComboBox.getValue() == null) {
                            versionComboBox.getSelectionModel().selectFirst();
                        }
                    }

                    if (session.getSkinUrl() != null) {
                        loadSkinHead(session.getSkinUrl());
                    }
                }
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> {
                loginBtn.setDisable(false);
                logArea.appendText("❌ 登录取消，请检查网络或服务器地址后重试。\n");
            });
            return null;
        });
    }

    private void loadSkinHead(String skinUrl) {
        logArea.appendText("🖼️ 正在加载并裁剪头部头像...\n");
        Image skinImage = new Image(skinUrl, true);
        skinImage.progressProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.doubleValue() == 1.0 && !skinImage.isError()) {
                try {
                    int scale = (int) skinImage.getWidth() / 64;
                    if (scale < 1) scale = 1;
                    WritableImage croppedFace = new WritableImage(skinImage.getPixelReader(), 8 * scale, 8 * scale, 8 * scale, 8 * scale);

                    int targetSize = 100;
                    WritableImage clearFace = new WritableImage(targetSize, targetSize);
                    PixelReader reader = croppedFace.getPixelReader();
                    PixelWriter writer = clearFace.getPixelWriter();
                    for (int y = 0; y < targetSize; y++) {
                        for (int x = 0; x < targetSize; x++) {
                            writer.setColor(x, y, reader.getColor((x * 8 * scale) / targetSize, (y * 8 * scale) / targetSize));
                        }
                    }
                    avatarImageView.setImage(clearFace);
                    logArea.appendText("🎨 高清头部贴图加载成功\n");
                } catch (Exception ex) {
                    logArea.appendText("❌ 头像裁剪失败: " + ex.getMessage() + "\n");
                }
            }
        });
    }

    // ================== 工具 / 实例 ==================



    private void openBrowser(String url) {
        try {
            java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
        } catch (Exception e) {
            logMessage("❌ 打开浏览器失败: " + e.getMessage());
        }
    }

    private void openDir(File dir) {
        try {
            if (!dir.isDirectory() && !dir.mkdirs()) {
                logArea.appendText("❌ 目录不存在且无法创建: " + dir.getAbsolutePath() + "\n");
                return;
            }
            java.awt.Desktop.getDesktop().open(dir);
        } catch (Exception e) {
            try {
                new ProcessBuilder("explorer.exe", dir.getAbsolutePath()).start();
            } catch (Exception ex) {
                logArea.appendText("❌ 打开目录失败: " + e.getMessage() + "\n");
            }
        }
    }

    // ================== 安装与启动 ==================

    private void handleLaunch() {
        MinecraftVersion selectedVersion = versionComboBox.getValue();
        if (selectedVersion == null || currentSession == null) {
            logArea.appendText("⚠️ 请先选择版本并登录！\n");
            return;
        }

        final String id = selectedVersion.getId();
        final String jsonUrl = selectedVersion.getUrl();
        final File gameDir = new File(config.getGameDir());

        launchBtn.setDisable(true);
        if (launchMainLabel != null) launchMainLabel.setText("准备中…");
        logArea.appendText("准备启动版本: " + id + "\n");

        CompletableFuture.runAsync(() -> {
            java.util.function.Consumer<String> log = msg -> Platform.runLater(() -> logArea.appendText(msg + "\n"));
            try {
                VersionProfile profile = ensureInstalled(id, jsonUrl, log);
                preflight(profile, log);
                if (config.isAutoRepairLibraries()) {
                    Integrity.Report report = null;
                    if (toolsPanel != null) {
                        Integrity.Report cached = toolsPanel.freshReport();
                        if (cached != null) {
                            report = new Integrity.Report(0, cached.issues().stream()
                                    .filter(issue -> issue.instanceId().equals(profile.getId())).toList());
                            log.accept("📋 复用工具页的完整性检查结果（" + report.issues().size() + " 个缺失）");
                        }
                    }
                    if (report == null) {
                        report = Integrity.checkProfile(gameDir, profile.getId(), profile,
                                config.getAssetSampleCount());
                    }
                    if (!report.issues().isEmpty()) {
                        log.accept("🔧 启动前发现 " + report.issues().size() + " 个缺失文件，正在自动补全…");
                        int fixed = Integrity.repair(gameDir, report.issues());
                        int remaining = report.issues().size() - fixed;
                        log.accept("🔧 已补全 " + fixed + "/" + report.issues().size()
                                + (remaining > 0 ? "，仍有 " + remaining + " 个缺失" : ""));
                        if (remaining > 0 && !confirmLaunchWithMissing(remaining)) {
                            Platform.runLater(() -> {
                                launchBtn.setDisable(false);
                                if (launchMainLabel != null) launchMainLabel.setText("启动游戏");
                            });
                            return;
                        }
                    }
                }
                log.accept("⏳ 等待所有下载任务完成后再启动游戏…");
                if (!DownloadManager.awaitIdle(30L * 60 * 1000)) {
                    log.accept("⚠ 仍有下载任务未完成，已取消本次启动；请等待下载完成后重试。");
                    Platform.runLater(() -> {
                        launchBtn.setDisable(false);
                        if (launchMainLabel != null) launchMainLabel.setText("启动游戏");
                    });
                    return;
                }
                log.accept("✅ 下载任务全部完成，开始启动游戏。");
                Platform.runLater(() -> startGame(profile));
            } catch (Exception e) {
                Platform.runLater(() -> {
                    logArea.appendText("❌ 启动前准备失败: " + e.getMessage() + "\n");
                    launchBtn.setDisable(false);
                    if (launchMainLabel != null) launchMainLabel.setText("启动游戏");
                });
            }
        });
    }

    /** 保证某个版本在本地可用：版本 JSON、客户端 jar、依赖库、资源、natives。 */
    private VersionProfile ensureInstalled(String id, String jsonUrl, java.util.function.Consumer<String> log) throws Exception {
        File gameDir = new File(config.getGameDir());
        File jsonFile = VersionProfile.jsonFile(gameDir, id);
        if (!jsonFile.isFile()) {
            if (jsonUrl == null) throw new Exception("本地没有 " + id + " 的版本文件，远端清单里也没有它的地址");
            log.accept("📄 获取版本 JSON: " + id);
            String json = VersionManager.fetchVersionDetailsAsync(jsonUrl).get(120, TimeUnit.SECONDS);
            Files.createDirectories(jsonFile.getParentFile().toPath());
            Files.writeString(jsonFile.toPath(), json, StandardCharsets.UTF_8);
        }

        VersionProfile profile = VersionProfile.load(gameDir, id);
        log.accept("🧩 版本 " + profile.getId() + "（基于 " + profile.getRootId() + "）");

        File versionDir = new File(new File(gameDir, "versions"), profile.getRootId());
        File clientJar = new File(versionDir, profile.getRootId() + ".jar");
        String clientUrl = profile.getClientUrl();
        if ((!clientJar.isFile() || clientJar.length() == 0) && clientUrl != null) {
            String mirrorUrl = "https://bmclapi2.bangbang93.com/version/" + profile.getRootId() + "/client";
            boolean mirrorFirst = !"official".equals(config.getDownloadSource());
            if (mirrorFirst) {
                log.accept("📥 通过 BMCLAPI 镜像下载客户端 jar...");
                DownloadManager.Stats mirrorStats = DownloadManager.downloadRanged(
                        new DownloadManager.Task(mirrorUrl, clientJar, profile.getClientSha1(),
                                profile.getClientSize()), 4,
                        (done, total, bytes) -> updateProgress(done, total, bytes));
                if (mirrorStats.failed > 0 || !clientJar.isFile() || clientJar.length() == 0) {
                    log.accept("⚠ 镜像下载失败，切换 Mojang 官方源…");
                }
            }
            if (!clientJar.isFile() || clientJar.length() == 0) {
                log.accept("📥 通过 Mojang 官方源下载客户端 jar...");
                DownloadManager.Stats stats = DownloadManager.downloadRanged(
                        new DownloadManager.Task(clientUrl, clientJar,
                                profile.getClientSha1(), profile.getClientSize()), 4,
                        (done, total, bytes) -> updateProgress(done, total, bytes));
                if (stats.failed > 0 || !clientJar.isFile()) {
                    throw new Exception("客户端 jar 下载失败，请检查网络后重试");
                }
            }
        }

        downloadLibraries(profile, gameDir, log);
        ensureAssets(profile, gameDir, log);
        extractNatives(profile, new File(gameDir, "libraries"), gameDir);

        // 继承版本（Forge/Fabric）准备一份同名 jar：Forge 的 ignoreList 认的是 ${version_name}.jar
        if (!profile.getId().equals(profile.getRootId()) && clientJar.isFile()) {
            File childDir = new File(new File(gameDir, "versions"), profile.getId());
            File childJar = new File(childDir, profile.getId() + ".jar");
            if (!childJar.isFile() || childJar.length() != clientJar.length()) {
                try {
                    Files.createDirectories(childDir.toPath());
                    Files.copy(clientJar.toPath(), childJar.toPath(),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                } catch (Exception ignored) {
                }
            }
        }
        return profile;
    }

    private void downloadLibraries(VersionProfile profile, File gameDir, java.util.function.Consumer<String> log) throws Exception {
        File libsDir = new File(gameDir, "libraries");
        List<DownloadManager.Task> tasks = new ArrayList<>();

        for (JsonObject lib : profile.classpathLibraries()) {
            String path = VersionProfile.libraryPath(lib);
            String url = VersionProfile.libraryUrl(lib);
            if (path != null && url != null) {
                tasks.add(new DownloadManager.Task(url, new File(libsDir, path),
                        VersionProfile.librarySha1(lib), VersionProfile.librarySize(lib)));
            }
        }
        for (JsonObject lib : profile.nativeLibraries()) {
            String[] artifact = nativeArtifact(lib);
            if (artifact != null) {
                tasks.add(new DownloadManager.Task(artifact[0], new File(libsDir, artifact[1]), null, -1));
            }
        }

        log.accept("📦 依赖库 " + tasks.size() + " 个，校验中...");
        DownloadManager.Stats stats = DownloadManager.downloadAll(tasks, 16,
                (done, total, bytes) -> updateProgress(done, total, bytes));
        String summary = "   依赖库就绪：新下载 " + stats.downloaded + "，已存在 " + stats.skipped;
        if (stats.failed > 0) summary += "，失败 " + stats.failed;
        log.accept(summary);
        if (stats.failed > 0) {
            synchronized (stats.failures) {
                for (String failedUrl : stats.failures) {
                    log.accept("   ✗ " + failedUrl);
                }
            }
            throw new Exception("有 " + stats.failed + " 个依赖库没下载成功，请检查网络后重试（已下载的会保留）");
        }
    }

    /** 旧式 natives 条目返回 {url, path}；新式条目直接用 artifact。 */
    private String[] nativeArtifact(JsonObject lib) {
        try {
            JsonObject downloads = lib.getAsJsonObject("downloads");
            if (lib.has("natives") && lib.get("natives").isJsonObject() && downloads != null
                    && downloads.has("classifiers")) {
                JsonObject natives = lib.getAsJsonObject("natives");
                String key = natives.has("windows") ? natives.get("windows").getAsString() : null;
                if (key == null) return null;
                key = key.replace("${arch}", "64");
                JsonObject classifiers = downloads.getAsJsonObject("classifiers");
                if (!classifiers.has(key)) return null;
                JsonObject artifact = classifiers.getAsJsonObject(key);
                return new String[]{artifact.get("url").getAsString(), artifact.get("path").getAsString()};
            }
            String path = VersionProfile.libraryPath(lib);
            String url = VersionProfile.libraryUrl(lib);
            if (path != null && url != null && path.endsWith("-natives-windows.jar")) {
                return new String[]{url, path};
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private void ensureAssets(VersionProfile profile, File gameDir, java.util.function.Consumer<String> log) throws Exception {
        String indexId = profile.getAssetIndexId();
        File indexFile = new File(gameDir, "assets/indexes/" + indexId + ".json");
        boolean validIndex = false;
        if (indexFile.isFile() && indexFile.length() > 2) {
            try (java.io.InputStream in = Files.newInputStream(indexFile.toPath())) {
                int b;
                do {
                    b = in.read();
                } while (b == ' ' || b == '\n' || b == '\r' || b == '\t');
                validIndex = b == '{';
            } catch (Exception ignored) {
            }
        }
        if (!validIndex && indexFile.exists()) {
            log.accept("索引文件损坏，正在重新下载: " + indexId);
            indexFile.delete();
        }
        if (!indexFile.isFile()) {
            String indexUrl = profile.getAssetIndexUrl();
            if (indexUrl == null) return;
            log.accept("📄 下载资源索引 " + indexId + "...");
            DownloadManager.downloadAll(
                    List.of(new DownloadManager.Task(indexUrl, indexFile, null, -1)), 1, null);
        }
        if (!indexFile.isFile()) return;

        AssetIndex index = new Gson().fromJson(new FileReader(indexFile), AssetIndex.class);
        if (index == null || index.getObjects() == null || index.getObjects().isEmpty()) return;

        File objectsDir = new File(gameDir, "assets/objects");
        List<DownloadManager.Task> tasks = new ArrayList<>();
        for (AssetIndex.AssetObject object : index.getObjects().values()) {
            String hash = object.getHash();
            if (hash == null || hash.length() < 2) continue;
            File target = new File(objectsDir, hash.substring(0, 2) + "/" + hash);
            tasks.add(new DownloadManager.Task(
                    "https://resources.download.minecraft.net/" + hash.substring(0, 2) + "/" + hash,
                    target, hash, object.getSize()));
        }
        if (tasks.isEmpty()) return;

        log.accept("🎨 资源文件 " + tasks.size() + " 个，校验中（首次会比较久）...");
        DownloadManager.Stats stats = DownloadManager.downloadAll(tasks, 32,
                (done, total, bytes) -> updateProgress(done, total, bytes));
        String summary = "   资源就绪：新下载 " + stats.downloaded + "，已存在 " + stats.skipped;
        if (stats.failed > 0) summary += "，失败 " + stats.failed;
        log.accept(summary);
        if (stats.failed > 0) {
            log.accept("   ⚠️ 部分资源没下载成功，游戏仍可启动（可能缺音效/语言文件）");
        }
    }

    private void extractNatives(VersionProfile profile, File libsDir, File gameDir) {
        extractNatives(profile, libsDir, gameDir,
                InstanceDirs.natives(gameDir, profile.getId(), config.isIsolation()));
    }

    private void extractNatives(VersionProfile profile, File libsDir, File gameDir, File nativesDir) {
        if (!nativesDir.exists()) nativesDir.mkdirs();
        int extracted = 0;
        for (JsonObject lib : profile.nativeLibraries()) {
            String[] artifact = nativeArtifact(lib);
            if (artifact == null) continue;
            File jarFile = new File(libsDir, artifact[1]);
            if (!jarFile.isFile()) continue;
            // 1.19+ 的启动参数把 natives 拆成 <natives>/java、/jna、/lwjgl、/netty 这几个子目录
            File target = nativesSubDir(nativesDir, artifact[1]);
            if (!target.exists()) target.mkdirs();
            extracted += extractDlls(jarFile, target);
            // 兼容 1.18 及更早版本：它们只读取 java.library.path 根目录
            if (!target.equals(nativesDir)) extracted += extractDlls(jarFile, nativesDir);
        }
        if (extracted > 0) {
            System.out.println("[natives] 解压出 " + extracted + " 个 dll -> " + nativesDir.getAbsolutePath());
        }
    }

    /** 按库名决定 natives 子目录，和官方启动参数的 java.library.path 对应。 */
    private File nativesSubDir(File nativesDir, String libraryPath) {
        String lower = libraryPath.toLowerCase();
        if (lower.contains("lwjgl")) return new File(nativesDir, "lwjgl");
        if (lower.contains("jna")) return new File(nativesDir, "jna");
        if (lower.contains("netty")) return new File(nativesDir, "netty");
        return new File(nativesDir, "java");
    }

    /** 把 jar 里的 dll 解到 natives 目录（压平到根目录），返回解压数量。 */
    private int extractDlls(File jarFile, File nativesDir) {
        int count = 0;
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(jarFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                // 新格式 natives jar 内部是 windows/x64/org/lwjgl/xxx.dll，必须压平，
                // 否则 -Djava.library.path 找不到（它不会递归子目录）。
                String entryName = entry.getName().replace('\\', '/');
                int slash = entryName.lastIndexOf('/');
                String fileName = slash >= 0 ? entryName.substring(slash + 1) : entryName;
                if (!fileName.toLowerCase().endsWith(".dll")) continue;

                File outFile = new File(nativesDir, fileName);
                if (!outFile.toPath().normalize().startsWith(nativesDir.toPath())) continue;
                try (FileOutputStream fos = new FileOutputStream(outFile)) {
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = zis.read(buffer)) != -1) fos.write(buffer, 0, len);
                }
                count++;
            }
        } catch (Exception ignored) {
        }
        return count;
    }

    // ================== 进度与自检 ==================

    /** 下载进度回调（可能来自任意线程）。 */
    private void updateProgress(int done, int total, long bytes) {
        Platform.runLater(() -> updateDownloadBar(new DownloadManager.Status(done, total, bytes,
                downloadFileLabel == null ? "" : downloadFileLabel.getText(), 0)));
    }

    /** 更新启动页底部的下载面板：当前文件、进度、数量、总大小、速度。 */
    private void updateDownloadBar(DownloadManager.Status status) {
        if (downloadProgress == null || downloadFileLabel == null) return;
        if (downloadBarPanel != null) {
            downloadBarPanel.setVisible(true);
            downloadBarPanel.setManaged(true);
        }
        if (status.total() <= 0 || status.done() >= status.total()) {
            downloadFileLabel.setText("空闲");
            downloadFileLabel.setVisible(true);
            downloadFileLabel.setManaged(true);
            downloadStatsLabel.setVisible(false);
            downloadStatsLabel.setManaged(false);
            downloadProgress.setVisible(false);
            downloadProgress.setManaged(false);
            downloadProgress.setProgress(0);
            return;
        }
        downloadStatsLabel.setVisible(true);
        downloadStatsLabel.setManaged(true);
        downloadProgress.setVisible(true);
        downloadProgress.setManaged(true);
        double ratio = Math.min(1.0, (double) status.done() / status.total());
        downloadProgress.setProgress(ratio);
        downloadFileLabel.setText(status.file() == null || status.file().isBlank()
                ? "正在下载…"
                : "正在下载：" + status.file());
        double mb = status.bytes() / 1048576.0;
        String speed;
        long bps = status.speedBps();
        if (bps <= 0) {
            speed = "—";
        } else if (bps >= 1048576) {
            speed = String.format(java.util.Locale.ROOT, "%.1f MB/s", bps / 1048576.0);
        } else {
            speed = (bps / 1024) + " KB/s";
        }
        downloadStatsLabel.setText(status.done() + "/" + status.total() + " 个文件 · "
                + String.format(java.util.Locale.ROOT, "%.1f MB", mb) + " · " + speed);
    }

    /** 启动前自检：目录可写、磁盘空间、Java 版本。 */
    private void preflight(VersionProfile profile, java.util.function.Consumer<String> log) throws Exception {
        File gameDir = new File(config.getGameDir());
        if (!gameDir.isDirectory() && !gameDir.mkdirs()) {
            throw new Exception("游戏目录不可用: " + gameDir.getAbsolutePath());
        }
        File probe = new File(gameDir, ".write-test");
        try {
            Files.writeString(probe.toPath(), "ok", StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new Exception("游戏目录不可写: " + gameDir.getAbsolutePath());
        } finally {
            probe.delete();
        }

        long usable = gameDir.getUsableSpace();
        if (usable > 0 && usable < 300L * 1024 * 1024) {
            throw new Exception("磁盘剩余空间不足 300MB（当前 " + (usable / 1024 / 1024) + " MB）");
        }
        log.accept("🧪 自检通过：目录可写，磁盘剩余 " + (usable / 1024 / 1024 / 1024) + " GB，需要 Java "
                + profile.getJavaMajorVersion());
    }

    /** 读取 java 可执行文件的版本大号（解析失败返回 -1）。 */
    private int javaMajorVersion(String javaPath) {
        try {
            Process process = new ProcessBuilder(javaPath, "-version")
                    .redirectErrorStream(true).start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    java.util.regex.Matcher matcher = java.util.regex.Pattern
                            .compile("version \"(\\d+)(?:\\.(\\d+))?").matcher(line);
                    if (matcher.find()) {
                        int major = Integer.parseInt(matcher.group(1));
                        if (major == 1 && matcher.group(2) != null) {
                            major = Integer.parseInt(matcher.group(2));
                        }
                        return major;
                    }
                }
            }
            process.waitFor(5, TimeUnit.SECONDS);
        } catch (Exception ignored) {
        }
        return -1;
    }

    /** 启动失败时给一个「去校验文件」的入口。 */
    /** 首次启动显示全屏引导页，而不是弹窗。 */
    private void updateGeekTelemetry() {
        if (geekTelemetry == null) return;
        long seconds = Math.max(0, (System.currentTimeMillis() - appStartedAt) / 1000);
        String up = String.format(java.util.Locale.ROOT, "%02d:%02d:%02d",
                seconds / 3600, (seconds % 3600) / 60, seconds % 60);
        Runtime runtime = Runtime.getRuntime();
        long heap = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
        geekTelemetry.setText("[SYS] UP " + up + " · HEAP " + heap + "M · THR " + Thread.activeCount());
    }

    /** 关闭窗口：询问 / 最小化到托盘 / 直接退出。 */
    private void requestClose() {
        if (quitting) return;
        String action = config.getCloseAction();
        if ("tray".equals(action) && installTray()) {
            hideToTray();
            return;
        }
        if ("exit".equals(action)) {
            quitLauncher();
            return;
        }
        showCloseChoice();
    }

    private void showCloseChoice() {
        if (stageRef == null) {
            quitLauncher();
            return;
        }
        boolean canTray = java.awt.SystemTray.isSupported();
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.CONFIRMATION);
        alert.initOwner(stageRef);
        alert.setTitle("Neko Craft Launcher");
        alert.setHeaderText("终止启动器？");
        javafx.scene.control.ButtonType tray = new javafx.scene.control.ButtonType(
                "最小化到托盘", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        javafx.scene.control.ButtonType exit = new javafx.scene.control.ButtonType(
                "退出启动器", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        javafx.scene.control.ButtonType cancel = new javafx.scene.control.ButtonType(
                "取消", javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
        if (canTray) alert.getButtonTypes().setAll(tray, exit, cancel);
        else alert.getButtonTypes().setAll(exit, cancel);
        alert.getDialogPane().getStyleClass().add("dialog-pane");
        if (THEME_URL != null) alert.getDialogPane().getStylesheets().add(THEME_URL);

        javafx.scene.control.CheckBox remember = new javafx.scene.control.CheckBox("记住我的选择");
        remember.setStyle("-fx-text-fill: -app-text-2;");
        Label note = new Label(gameRunning
                ? "游戏正在运行。最小化到托盘后，启动器会继续在后台守护进程。"
                : "最小化到托盘后，启动器仍会在后台运行。");
        note.setWrapText(true);
        note.setStyle("-fx-text-fill: -app-text-dim; -fx-font-size: 12px;");
        VBox content = new VBox(10, note, remember);
        content.setPadding(new Insets(6, 0, 0, 0));
        alert.getDialogPane().setContent(content);

        javafx.scene.control.ButtonType result = alert.showAndWait().orElse(cancel);
        if (result == cancel) return;
        if (remember.isSelected()) {
            config.setCloseAction(result == tray ? "tray" : "exit");
            config.save();
        }
        if (result == tray) {
            if (installTray()) hideToTray();
        } else if (result == exit) {
            quitLauncher();
        }
    }

    private boolean installTray() {
        if (trayInstalled) return true;
        if (!java.awt.SystemTray.isSupported()) return false;
        try {
            java.awt.Image image = null;
            try (java.io.InputStream in = HelloApplication.class.getResourceAsStream("/icon.png")) {
                if (in != null) image = javax.imageio.ImageIO.read(in);
            }
            if (image == null) {
                java.awt.image.BufferedImage fallback = new java.awt.image.BufferedImage(
                        16, 16, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                java.awt.Graphics2D g = fallback.createGraphics();
                g.setColor(new java.awt.Color(225, 29, 46));
                g.fillOval(1, 1, 14, 14);
                g.setColor(new java.awt.Color(255, 207, 106));
                g.fillOval(6, 6, 4, 4);
                g.dispose();
                image = fallback;
            }
            java.awt.PopupMenu menu = new java.awt.PopupMenu();
            java.awt.MenuItem show = new java.awt.MenuItem("显示 Neko Craft Launcher");
            show.addActionListener(e -> Platform.runLater(this::restoreFromTray));
            java.awt.MenuItem quit = new java.awt.MenuItem("退出启动器");
            quit.addActionListener(e -> Platform.runLater(this::quitLauncher));
            menu.add(show);
            menu.addSeparator();
            menu.add(quit);
            trayIcon = new java.awt.TrayIcon(image, "Neko Craft Launcher", menu);
            trayIcon.setImageAutoSize(true);
            trayIcon.addActionListener(e -> Platform.runLater(this::restoreFromTray));
            java.awt.SystemTray.getSystemTray().add(trayIcon);
            trayInstalled = true;
            return true;
        } catch (Exception ex) {
            logArea.appendText("⚠ 系统托盘不可用: " + ex.getMessage() + "\n");
            return false;
        }
    }

    private void hideToTray() {
        if (stageRef == null || trayIcon == null) return;
        stageRef.hide();
        trayIcon.displayMessage("Neko Craft Launcher",
                gameRunning ? "已最小化到托盘，游戏进程仍在守护中。" : "已最小化到托盘。",
                java.awt.TrayIcon.MessageType.INFO);
    }

    private void restoreFromTray() {
        if (stageRef == null) return;
        stageRef.show();
        stageRef.setIconified(false);
        stageRef.toFront();
        stageRef.requestFocus();
    }

    private void quitLauncher() {
        if (quitting) return;
        quitting = true;
        if (trayIcon != null) {
            try {
                java.awt.SystemTray.getSystemTray().remove(trayIcon);
            } catch (Exception ignored) {
            }
            trayIcon = null;
        }
        SingleInstance.release();
        Platform.exit();
        System.exit(0);
    }

    private void maybeOfferPerformanceOptimization() {
        if (config.isPerformanceAsked() || config.isPerformanceApplied()
                || !PerformanceOptimizer.isWindows() || stageRef == null) return;
        if (stageRef.getScene() == null) return;
        File gameDir = new File(config.getGameDir());
        if (PerformanceOptimizer.hasBackup(gameDir)) {
            config.setPerformanceAsked(true);
            config.save();
            return;
        }
        config.setPerformanceAsked(true);
        config.save();

        javafx.scene.layout.StackPane layer = (javafx.scene.layout.StackPane) stageRef.getScene().getRoot();
        FirstRunPanel panel = new FirstRunPanel(config,
                message -> logArea.appendText(message + "\n"),
                () -> layer.getChildren().removeIf(node -> node instanceof FirstRunPanel));
        layer.getChildren().add(panel);
    }

    private void showLaunchFailureHint(String message) {
        javafx.scene.control.ButtonType go = new javafx.scene.control.ButtonType("去校验文件");
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.ERROR, "", go,
                javafx.scene.control.ButtonType.CLOSE);
        alert.setTitle("启动失败");
        alert.setHeaderText("启动失败");
        alert.setContentText(message + "\n\n可以先去工具页校验并补全缺失文件，再重试启动。");
        if (alert.showAndWait().orElse(javafx.scene.control.ButtonType.CLOSE) == go) {
            switchPage("tools");
            if (toolsPanel != null) toolsPanel.startLibraryCheck();
        }
    }

    /** 启动前仍有缺失文件时，让用户决定是否继续。 */
    private boolean confirmLaunchWithMissing(int missing) {
        java.util.concurrent.CompletableFuture<Boolean> answer = new java.util.concurrent.CompletableFuture<>();
        Platform.runLater(() -> {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.CONFIRMATION);
            alert.setTitle("启动前检查");
            alert.setHeaderText("仍有 " + missing + " 个文件缺失");
            alert.setContentText("缺失文件可能导致启动失败，是否仍然启动？");
            alert.getButtonTypes().setAll(javafx.scene.control.ButtonType.OK,
                    javafx.scene.control.ButtonType.CANCEL);
            answer.complete(alert.showAndWait().orElse(javafx.scene.control.ButtonType.CANCEL)
                    == javafx.scene.control.ButtonType.OK);
        });
        try {
            return answer.get(180, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            return false;
        }
    }

    private void startGame(VersionProfile profile) {
        launchPrepareStart = System.currentTimeMillis();
        File gameDir = new File(config.getGameDir());
        File nativesDir = InstanceDirs.natives(gameDir, profile.getId(), config.isIsolation());

        CompletableFuture<File> injectorFuture = currentSession.getType() == UserSession.LoginType.THIRD_PARTY
                ? AuthlibInjectorManager.ensureInjectorAsync(gameDir, msg -> Platform.runLater(() -> logArea.appendText(msg + "\n")))
                : CompletableFuture.completedFuture(null);

        int requiredJava = profile.getJavaMajorVersion();
        if (config.getJavaPath() != null && !config.getJavaPath().isBlank()) {
            logArea.appendText("☕ 版本 " + profile.getId() + " 需要 Java " + requiredJava
                    + "，使用设置中指定的 Java：" + config.getJavaPath() + "\n");
        } else {
            logArea.appendText("☕ 版本 " + profile.getId() + " 需要 Java " + requiredJava
                    + "，未指定路径，准备 Azul Zulu 高性能运行时…\n");
        }
        CompletableFuture<String> javaFuture;
        if (config.getJavaPath() != null && !config.getJavaPath().isBlank()) {
            javaFuture = CompletableFuture.completedFuture(config.getJavaPath());
        } else {
            javaFuture = JavaManager.ensureJavaAsync(gameDir, requiredJava,
                    msg -> Platform.runLater(() -> logArea.appendText(msg + "\n")));
        }

        injectorFuture.thenCombine(javaFuture, (injector, javaPath) -> new Object[]{injector, javaPath})
                .thenAccept(res -> Platform.runLater(() -> {
                    try {
                        String javaPath = (String) res[1];
                        int actual = javaMajorVersion(javaPath);
                        int required = profile.getJavaMajorVersion();
                        if (actual > 0 && actual < required) {
                            logArea.appendText("⚠️ 该版本需要 Java " + required + "，当前用的是 Java " + actual
                                    + "，大概率会崩；可在「设置」里指定 Java 路径\n");
                        } else if (actual > 0) {
                            logArea.appendText("☕ Java " + actual + "（需要 " + required + "）\n");
                        }
                        InstanceOverrides.Override override =
                                InstanceOverrides.get(gameDir, profile.getId());
                        LauncherConfig launchConfig = config.copyForLaunch(
                                override.memoryMb(), override.jvmArgs(), override.javaPath());
                        if (!override.isEmpty()) {
                            logArea.appendText("🎛 使用实例级启动设置"
                                    + (override.memoryMb() != null ? "（内存 " + override.memoryMb() + " MB）" : "")
                                    + (override.jvmArgs() != null && !override.jvmArgs().isBlank()
                                    ? "（JVM 参数）" : "")
                                    + (override.javaPath() != null && !override.javaPath().isBlank()
                                    ? "（Java 路径）" : "") + "\n");
                        }
                        if (config.isSmartPerformance()) {
                            PerformanceTuner.Plan plan = PerformanceTuner.plan(launchConfig.getMemoryMb());
                            launchConfig.setJvmArgs(PerformanceTuner.merge(
                                    launchConfig.getJvmArgs(), plan.jvmArgs()));
                            logArea.appendText("⚡ 智能性能：" + plan.summary() + "\n");
                        }
                        List<String> command = LaunchCommand.build(profile, currentSession, launchConfig, gameDir,
                                javaPath, nativesDir, (File) res[0]);
                        gameInstanceId = profile.getId();
                        gameStartAt = System.currentTimeMillis();
                        startProcess(command, gameDir);
                    } catch (Exception e) {
                        logArea.appendText("❌ 启动失败: " + e.getMessage() + "\n");
                        showLaunchFailureHint("启动失败: " + e.getMessage());
                        launchBtn.setDisable(false);
                        if (launchMainLabel != null) launchMainLabel.setText("启动游戏");
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        logArea.appendText("❌ 环境准备失败: " + ex.getMessage() + "\n");
                        launchBtn.setDisable(false);
                        if (launchMainLabel != null) launchMainLabel.setText("启动游戏");
                    });
                    return null;
                });
    }

    /** 按设置调整游戏进程优先级（PowerShell，失败不影响启动）。 */
    private void applyGamePriority(long pid) {
        String level = config.getGamePriority();
        if (level == null || "normal".equals(level)) return;
        String priority = "above".equals(level) ? "AboveNormal" : "High";
        String friendly = "above".equals(level) ? "高于正常" : "高";
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                String command = "$p=Get-Process -Id " + pid + " -ErrorAction SilentlyContinue;"
                        + " if($p){$p.PriorityClass='" + priority + "'};"
                        + " (Get-Process -Id " + pid + " -ErrorAction SilentlyContinue).PriorityClass";
                Process probe = new ProcessBuilder("powershell", "-NoProfile", "-WindowStyle", "Hidden",
                        "-Command", command).redirectErrorStream(true).start();
                String output = new String(probe.getInputStream().readAllBytes(),
                        java.nio.charset.StandardCharsets.UTF_8).trim();
                int code = probe.waitFor();
                javafx.application.Platform.runLater(() -> {
                    if (code == 0 && output.toLowerCase(java.util.Locale.ROOT).contains("priority")) {
                        logArea.appendText("⚡ 游戏进程优先级已设为" + friendly + "（PID " + pid + "）\n");
                    } else {
                        logArea.appendText("⚠ 进程优先级设置未确认（不影响游戏）："
                                + (output.isBlank() ? "退出码 " + code : output) + "\n");
                    }
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() ->
                        logArea.appendText("⚠ 进程优先级设置失败（不影响游戏）：" + e.getMessage() + "\n"));
            }
        });
    }

    private void startProcess(List<String> command, File gameDir) throws Exception {        List<String> masked = new ArrayList<>(command);
        for (int i = 0; i < masked.size(); i++) {
            if ("--accessToken".equals(masked.get(i)) && i + 1 < masked.size()) masked.set(i + 1, "******");
        }
        String joined = String.join(" ", masked);
        logArea.appendText("🚀 " + (joined.length() > 4000 ? joined.substring(0, 4000) + " ..." : joined) + "\n");

        lastLaunchLogStart = logArea.getLength();
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(gameDir);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        gameProcess = process;
        gameRunning = true;
        setGameStatus(true);

        launchBtn.setDisable(false);
        if (launchMainLabel != null) launchMainLabel.setText("启动游戏");
        logArea.appendText("✅ 游戏进程已启动 (pid " + process.pid() + ")\n");
        if (launchPrepareStart > 0) {
            long prepareMs = System.currentTimeMillis() - launchPrepareStart;
            lastLaunchPrepMs = prepareMs;
            logArea.appendText("⏱ 启动准备耗时 "
                    + String.format(java.util.Locale.ROOT, "%.1f", prepareMs / 1000.0) + " 秒\n");
            launchPrepareStart = 0;
        }
        applyGamePriority(process.pid());

        if (config.isMinimizeOnLaunch() && stageRef != null) {
            minimizedForGame = true;
            stageRef.setIconified(true);
        }
        if (config.isPauseVisualsInGame() && ambientBackground != null) {
            ambientBackground.stop();
        }

        if (testMode) {
            testMode = false;
            javafx.animation.PauseTransition autoClose = new javafx.animation.PauseTransition(Duration.seconds(45));
            autoClose.setOnFinished(ev -> {
                logArea.appendText(testSuccess
                        ? "🧪 测试结果：游戏窗口已成功创建 ✓\\n"
                        : "🧪 测试结果：等待 45 秒仍未看到游戏窗口，可能启动失败\\n");
                if (process.isAlive()) {
                    process.descendants().forEach(ProcessHandle::destroyForcibly);
                    process.destroyForcibly();
                }
            });
            autoClose.play();
        }

        process.onExit().thenAccept(exited -> Platform.runLater(() -> {
            gameRunning = false;
            setGameStatus(false);
            if (stageRef != null && !stageRef.isShowing() && trayIcon != null) {
                trayIcon.displayMessage("Neko Craft Launcher", "游戏进程已退出。",
                        java.awt.TrayIcon.MessageType.INFO);
            }
            if (minimizedForGame && stageRef != null) {
                minimizedForGame = false;
                stageRef.setIconified(false);
            }
            if (config.isPauseVisualsInGame() && ambientBackground != null
                    && (stageRef == null || !stageRef.isIconified())) {
                ambientBackground.start();
            }
            String recent = "";
            try {
                recent = logArea.getText(lastLaunchLogStart, logArea.getLength());
            } catch (Exception ignored) {
            }
            long sessionSeconds = 0;
            if (gameStartAt > 0 && gameInstanceId != null) {
                sessionSeconds = Math.max(1, (System.currentTimeMillis() - gameStartAt) / 1000);
                File base = new File(config.getGameDir());
                Playtime.add(base, gameInstanceId, sessionSeconds);
                logArea.appendText("⏱ 本次游戏时长 " + Playtime.format(sessionSeconds)
                        + "（累计 " + Playtime.format(Playtime.seconds(base, gameInstanceId)) + "）\n");
                gameStartAt = 0;
            }
            boolean crashLike = exited.exitValue() != 0
                    || recent.contains("Exception") || recent.contains("Caused by:")
                    || recent.contains("Mixin") || recent.contains("FATAL");
            LaunchHistory.record(new File(config.getGameDir()), new LaunchHistory.Entry(
                    System.currentTimeMillis(), gameInstanceId, lastLaunchPrepMs,
                    sessionSeconds, exited.exitValue(), crashLike));
            lastLaunchPrepMs = 0;
            refreshLaunchInsight();
            logArea.appendText(crashLike
                    ? "⚠ 游戏异常退出（日志里检测到异常，退出码 " + exited.exitValue() + "）\n"
                    : "🏁 游戏已退出（退出码 " + exited.exitValue() + "）\n");
            launchBtn.setDisable(false);
            if (launchMainLabel != null) launchMainLabel.setText("启动游戏");
            if (crashLike) {
                File crashDir = new File(gameDir, "crash-reports");
                File[] reports = crashDir.listFiles((d, n) -> n.endsWith(".txt"));
                if (reports != null && reports.length > 0) {
                    Arrays.sort(reports, Comparator.comparingLong(File::lastModified).reversed());
                    logArea.appendText("💥 崩溃报告: " + reports[0].getAbsolutePath() + "\n");
                } else {
                    logArea.appendText("💥 非正常退出，可在「工具 → 打开日志目录」里看 latest.log\n");
                }
                logArea.appendText("   如果是缺文件导致，去「工具 → 百宝箱 → 校验库文件」可自动补齐\n");
                CrashAnalyzer.Report report = CrashAnalyzer.analyze(gameDir);
                logArea.appendText(CrashAnalyzer.toText(report));
                if (report.hasFindings()) {
                    CrashReportDialog.show(stageRef, report);
                } else {
                    showLaunchFailureHint("游戏异常退出（退出码 " + exited.exitValue() + "）");
                }
            }
        }));

        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    final String outputLine = line;
                    Platform.runLater(() -> logArea.appendText("[Game] " + outputLine + "\n"));
                }
            } catch (Exception ignored) {
            }
        }, "game-output").start();
    }

    /** 当前选中的实例 id（没有选择就用第一个已安装的）。 */
    private String currentInstanceId() {
        if (versionComboBox != null && versionComboBox.getValue() != null) {
            return versionComboBox.getValue().getId();
        }
        List<String> installed = VersionProfile.installedIds(new File(config.getGameDir()));
        return installed.isEmpty() ? null : installed.get(0);
    }

    /** 切换当前游戏目录（多目录支持）。 */
    private void switchGameDir(File dir) {
        if (dir == null) return;
        File root = detectMinecraftRoot(dir);
        if (root == null) {
            Toast.error("没有检测到 Minecraft 目录：请选择包含 versions 的 .minecraft 文件夹");
            logMessage("❌ 导入失败：未在 " + dir.getAbsolutePath() + " 找到 versions");
            return;
        }
        config.setGameDir(root.getAbsolutePath());
        config.addGameDir(root.getAbsolutePath());
        config.save();
        refreshInstalledVersions();
        int count = VersionProfile.installedIds(root).size();
        if (count == 0) {
            Toast.warn("目录已切换，但没有发现已安装版本：请确认 versions 内有版本 JSON");
            logMessage("⚠ 已切换目录，但未发现已安装版本: " + root.getAbsolutePath());
        } else {
            Toast.ok("已导入 " + count + " 个版本：" + root.getName());
            logMessage("✅ 已导入游戏目录 " + root.getAbsolutePath() + "，版本数 " + count);
        }
    }

    /** 兼容 PCL / 官方启动器：自动向下寻找 .minecraft 或 versions 目录。 */
    private File detectMinecraftRoot(File selected) {
        if (selected == null || !selected.isDirectory()) return null;
        File[] direct = {selected, new File(selected, ".minecraft")};
        for (File candidate : direct) {
            if (hasVersions(candidate)) return candidate;
        }
        File[] children = selected.listFiles(File::isDirectory);
        if (children != null) {
            for (File child : children) {
                if (hasVersions(child)) return child;
                File dot = new File(child, ".minecraft");
                if (hasVersions(dot)) return dot;
            }
        }
        return null;
    }

    private boolean hasVersions(File dir) {
        File versions = new File(dir, "versions");
        if (!versions.isDirectory()) return false;
        File[] subs = versions.listFiles(File::isDirectory);
        if (subs == null) return false;
        for (File sub : subs) {
            if (new File(sub, sub.getName() + ".json").isFile()) return true;
        }
        return false;
    }

    /** 实例选择页点了某个实例。 */
    private void selectInstance(Instance instance) {
        addInstalledToCombo(instance.id);
        config.setLastInstance(instance.id);
        config.save();
        if (instanceSettingsPanel != null) instanceSettingsPanel.setInstance(instance.id);
        logMessage("🎮 已选择实例: " + instance.id + "（" + instance.subtitle() + "）");
        switchPage("launch");
    }

    /** 补全文件：重新校验并下载缺失的版本 JSON / 客户端 / 依赖库 / 资源。 */
    private void repairInstance(String id) {
        logMessage("🔧 开始补全实例 " + id + " ...");
        CompletableFuture.runAsync(() -> {
            try {
                ensureInstalled(id, null, msg -> Platform.runLater(() -> logMessage(msg)));
                Platform.runLater(() -> logMessage("✅ 实例 " + id + " 文件已补全"));
            } catch (Exception e) {
                Platform.runLater(() -> logMessage("❌ 补全失败: " + e.getMessage()));
            }
        });
    }

    /** 导出启动脚本（PCL 的「导出启动脚本」，方便排查启动问题）。 */
    private void exportLaunchScript(String id) {
        if (currentSession == null) {
            logMessage("⚠️ 请先登录，再导出启动脚本（需要账号参数）");
            return;
        }
        try {
            File gameDir = new File(config.getGameDir());
            VersionProfile profile = VersionProfile.load(gameDir, id);
            File instanceDir = InstanceDirs.root(gameDir, id, config.isIsolation());
            String java = config.getJavaPath() == null || config.getJavaPath().isBlank()
                    ? new File(new File(gameDir, "java"), "zulu" + profile.getJavaMajorVersion() + "/bin/java.exe").getAbsolutePath()
                    : config.getJavaPath();
            List<String> command = LaunchCommand.build(profile, currentSession, config, gameDir, instanceDir,
                    java, InstanceDirs.natives(gameDir, id, config.isIsolation()), null);

            StringBuilder script = new StringBuilder("@echo off\r\n");
            script.append("cd /d \"").append(instanceDir.getAbsolutePath()).append("\"\r\n");
            script.append("rem 由 Neko Craft Launcher 导出，用于排查启动问题\r\n");
            for (String arg : command) {
                script.append(arg.contains(" ") ? "\"" + arg + "\"" : arg).append(" ");
            }
            script.append("\r\npause\r\n");

            instanceDir.mkdirs();
            File out = new File(instanceDir, "launch-" + id + ".bat");
            Files.writeString(out.toPath(), script.toString(), StandardCharsets.UTF_8);
            logMessage("📜 启动脚本已导出: " + out.getAbsolutePath());
        } catch (Exception e) {
            logMessage("❌ 导出失败: " + e.getMessage());
        }
    }

    /** 删除实例（versions/<id> 整个目录）。 */
    private void deleteInstance(String id) {
        File dir = new File(new File(config.getGameDir(), "versions"), id);
        if (!dir.isDirectory()) {
            logMessage("⚠️ 实例目录不存在: " + dir.getAbsolutePath());
            return;
        }
        CompletableFuture.runAsync(() -> {
            boolean ok = deleteRecursively(dir);
            Platform.runLater(() -> {
                if (ok) {
                    logMessage("🗑️ 已删除实例 " + id);
                    refreshInstalledVersions();
                    if (instanceSettingsPanel != null) instanceSettingsPanel.setInstance(currentInstanceId());
                    switchPage("launch");
                } else {
                    logMessage("❌ 删除失败（文件可能被占用）: " + id);
                }
            });
        });
    }

    /** 检查单个实例的库文件完整性，缺失可一键补齐。 */
    private void checkInstance(Instance instance) {
        File game = new File(config.getGameDir());
        Toast.info("正在检查实例 " + instance.id + " …");
        CompletableFuture.runAsync(() -> {
            Integrity.Report report = Integrity.check(game, instance.id);
            Platform.runLater(() -> {
                if (report.issues().isEmpty()) {
                    Toast.ok(instance.id + " 完整（检查 " + report.checked() + " 个库）");
                    logMessage("✅ 实例 " + instance.id + " 库文件完整");
                    return;
                }
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                        javafx.scene.control.Alert.AlertType.CONFIRMATION);
                alert.setTitle("实例完整性");
                alert.setHeaderText("实例 " + instance.id + " 缺失 " + report.issues().size()
                        + " 个库文件（共检查 " + report.checked() + " 个）");
                alert.setContentText("是否立即下载补齐？");
                if (alert.showAndWait().orElse(javafx.scene.control.ButtonType.CANCEL)
                        != javafx.scene.control.ButtonType.OK) {
                    return;
                }
                Toast.info("正在补齐 " + instance.id + " 的库文件…");
                CompletableFuture.runAsync(() -> {
                    int ok = Integrity.repair(game, report.issues());
                    Platform.runLater(() -> {
                        Toast.ok("已补齐 " + ok + "/" + report.issues().size());
                        logMessage("🔧 实例 " + instance.id + " 补齐库文件 "
                                + ok + "/" + report.issues().size());
                    });
                });
            });
        });
    }

    /** 克隆实例：复制 versions/&lt;id&gt; 到新目录，并把 JSON 里的 id 改成新 id。 */
    private void cloneInstance(String id, boolean copyData) {
        File versions = new File(config.getGameDir(), "versions");
        File source = new File(versions, id);
        if (!source.isDirectory()) {
            logMessage("⚠️ 实例目录不存在: " + source.getAbsolutePath());
            return;
        }
        String candidate = id + "-copy";
        int index = 2;
        while (new File(versions, candidate).exists()) candidate = id + "-copy" + index++;
        final String newId = candidate;
        final File target = new File(versions, newId);
        final boolean withData = copyData;
        logMessage("📄 正在克隆实例 " + id + " → " + newId + (withData ? "（含数据）" : "（仅版本）"));

        CompletableFuture.runAsync(() -> {
            try {
                Path src = source.toPath();
                Path dst = target.toPath();
                try (var stream = java.nio.file.Files.walk(src)) {
                    stream.forEach(path -> {
                        try {
                            Path rel = src.relativize(path);
                            if (!withData && rel.getNameCount() > 0
                                    && CLONE_DATA_DIRS.contains(rel.getName(0).toString())) {
                                return;
                            }
                            Path to = dst.resolve(rel);
                            if (java.nio.file.Files.isDirectory(path)) {
                                java.nio.file.Files.createDirectories(to);
                            } else {
                                java.nio.file.Files.copy(path, to,
                                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                            }
                        } catch (Exception ignored) {
                            // 单个文件失败不中断整体复制
                        }
                    });
                }
                File json = new File(target, id + ".json");
                if (json.isFile()) {
                    JsonObject object = com.google.gson.JsonParser.parseString(
                            java.nio.file.Files.readString(json.toPath())).getAsJsonObject();
                    object.addProperty("id", newId);
                    java.nio.file.Files.writeString(json.toPath(),
                            new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(object));
                    json.renameTo(new File(target, newId + ".json"));
                }
                Platform.runLater(() -> {
                    refreshInstalledVersions();
                    if (instancesPanel != null) instancesPanel.setSelectedId(currentInstanceId());
                    Toast.ok("已克隆实例 → " + newId);
                    logMessage("✅ 已克隆实例 " + id + " → " + newId);
                });
            } catch (Exception e) {
                Platform.runLater(() -> logMessage("❌ 克隆失败: " + e.getMessage()));
            }
        });
    }

    /** 测试游戏：正常启动，45 秒后自动关闭并报告是否成功创建窗口。 */
    private void testLaunchInstance(String id) {
        if (currentSession == null) {
            logMessage("⚠️ 请先登录，再测试启动");
            return;
        }
        MinecraftVersion selected = versionComboBox.getValue();
        if (selected == null || !selected.getId().equals(id)) {
            addInstalledToCombo(id);
        }
        logMessage("🧪 测试启动 " + id + "（45 秒后自动关闭）...");
        testMode = true;
        testSuccess = false;
        switchPage("launch");
        handleLaunch();
    }

    /** 重装：仅对原版实例有效（加载器实例需要重新跑安装器）。 */
    private void reinstallInstance(String id) {
        VersionProfile probe;
        try {
            probe = VersionProfile.load(new File(config.getGameDir()), id);
        } catch (Exception e) {
            logMessage("❌ 读取实例失败: " + e.getMessage());
            return;
        }
        if (!probe.getId().equals(probe.getRootId())) {
            logMessage("♻️ 这是加载器实例（基于 " + probe.getRootId() + "），请在「下载 → Minecraft」里重新安装对应加载器");
            return;
        }
        logMessage("♻️ 开始重装 " + id + "（会重新下载客户端与依赖）");
        CompletableFuture.runAsync(() -> {
            try {
                String url = VersionManager.fetchVersionListAsync().get(60, TimeUnit.SECONDS).stream()
                        .filter(v -> v.getId().equals(id)).findFirst()
                        .map(MinecraftVersion::getUrl).orElse(null);
                if (url == null) throw new Exception("远端清单里没有 " + id);
                File dir = new File(new File(config.getGameDir(), "versions"), id);
                deleteRecursively(dir);
                ensureInstalled(id, url, msg -> Platform.runLater(() -> logMessage(msg)));
                Platform.runLater(() -> {
                    logMessage("✅ 重装完成: " + id);
                    refreshInstalledVersions();
                });
            } catch (Exception e) {
                Platform.runLater(() -> logMessage("❌ 重装失败: " + e.getMessage()));
            }
        });
    }

    private boolean deleteRecursively(File file) {
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                if (!deleteRecursively(child)) return false;
            }
        }
        return file.delete();
    }

    /** 打开实例设置页（tab 可以是 overview / settings / mods / resourcepacks / saves）。 */
    private void openInstanceSettings(String tab) {
        String id = currentInstanceId();
        if (id == null) {
            logMessage("⚠️ 还没有任何实例，先去「下载」页安装一个版本");
            switchPage("download");
            return;
        }
        if (instanceSettingsPanel == null) {
            instanceSettingsPanel = new InstanceSettingsPanel(new File(config.getGameDir()), config, id,
                    this::logMessage, () -> switchPage("launch"),
                    this::repairInstance, this::exportLaunchScript, this::deleteInstance,
                    this::testLaunchInstance, this::reinstallInstance);
            pages.put("instance-settings", instanceSettingsPanel);
        }
        instanceSettingsPanel.setInstance(id);
        instanceSettingsPanel.showTab(tab);
        navHighlight = null;
        switchPage("instance-settings");
    }

    /** 悬浮刷新按钮：重新拉取远端版本列表。 */
    private void reloadVersionsAsync() {
        logMessage("🔄 重新加载版本列表...");
        VersionManager.fetchVersionListAsync().thenAccept(list -> Platform.runLater(() -> {
            if (list == null || list.isEmpty()) return;
            allMinecraftVersions.setAll(list);
            List<MinecraftVersion> releases = list.stream()
                    .filter(v -> "release".equals(v.getType())).toList();
            versionComboBox.getItems().setAll(releases);
            if (!releases.isEmpty()) versionComboBox.getSelectionModel().selectFirst();
            refreshInstalledVersions();
            logMessage("✅ 版本列表已刷新（" + list.size() + " 个）");
        })).exceptionally(ex -> {
            Platform.runLater(() -> logMessage("❌ 刷新失败: " + ex.getMessage()));
            return null;
        });
    }

    private void refreshInstalledVersions() {
        File gameDir = new File(config.getGameDir());
        List<String> installed = VersionProfile.installedIds(gameDir);
        suppressInstancePersist = true;
        try {
            MinecraftVersion previous = versionComboBox == null ? null : versionComboBox.getValue();
            boolean keepUserChoice = userPickedInstance && previous != null && installed.contains(previous.getId());
            for (String id : installed) {
                addInstalledToCombo(id);
            }
            if (versionComboBox == null) return;
            if (keepUserChoice) {
                // 用户刚手动选过，刷新列表时保持不动
                versionComboBox.setValue(previous);
                userPickedInstance = false;
                return;
            }
            userPickedInstance = false;
            // 优先「上次使用的实例」，其次第一个已安装实例
            String remembered = config.getLastInstance();
            String preferred = remembered != null && installed.contains(remembered)
                    ? remembered
                    : (installed.isEmpty() ? null : installed.get(0));
            if (preferred != null) {
                selectComboVersion(preferred);
            }
        } finally {
            suppressInstancePersist = false;
        }
    }

    private void selectComboVersion(String id) {
        if (versionComboBox == null) return;
        for (MinecraftVersion version : versionComboBox.getItems()) {
            if (version.getId().equals(id)) {
                versionComboBox.setValue(version);
                return;
            }
        }
    }

    private void addInstalledToCombo(String id) {
        if (versionComboBox == null) return;
        for (MinecraftVersion version : versionComboBox.getItems()) {
            if (version.getId().equals(id)) {
                versionComboBox.setValue(version);
                return;
            }
        }
        MinecraftVersion installed = MinecraftVersion.installed(id);
        versionComboBox.getItems().add(0, installed);
        versionComboBox.setValue(installed);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
