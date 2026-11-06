/*
 * Decompiled with CFR 0.152.
 */
package com.example.demo.ui.shell;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.CopyOption;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import javafx.util.StringConverter;
import javax.imageio.ImageIO;

import com.example.demo.core.launch.LaunchPlan;
import com.example.demo.core.launch.LaunchPlanner;
import com.example.demo.core.launch.NativeExtractor;
import com.example.demo.core.meta.VersionManager;
import com.example.demo.core.meta.VersionProfile;
import com.example.demo.core.model.AssetIndex;
import com.example.demo.core.model.Instance;
import com.example.demo.core.model.InstanceDirs;
import com.example.demo.core.model.InstanceSettings;
import com.example.demo.core.model.LaunchHistory;
import com.example.demo.core.model.MinecraftVersion;
import com.example.demo.core.model.ModpackProject;
import com.example.demo.core.model.Playtime;
import com.example.demo.core.model.UserSession;
import com.example.demo.core.ai.DeepSeekClient;
import com.example.demo.core.app.LauncherConfig;
import com.example.demo.core.app.SingleInstance;
import com.example.demo.core.auth.AuthlibInjectorManager;
import com.example.demo.core.download.DownloadManager;
import com.example.demo.core.download.IconProxy;
import com.example.demo.core.download.Integrity;
import com.example.demo.core.game.CrashAnalyzer;
import com.example.demo.core.game.JavaManager;
import com.example.demo.core.game.LoaderInstaller;
import com.example.demo.core.runtime.PerformanceOptimizer;
import com.example.demo.core.runtime.PerformanceTuner;
import com.example.demo.core.ai.LocalAiAssistant;
import com.example.demo.core.app.AppPaths;
import com.example.demo.core.auth.AccountStore;
import com.example.demo.core.auth.AuthManager;
import com.example.demo.core.auth.SessionState;
import com.example.demo.core.auth.SessionStore;
import com.example.demo.core.game.PortableExporter;
import com.example.demo.core.launch.GameCommandSender;
import com.example.demo.core.launch.LaunchCancelledException;
import com.example.demo.core.model.Favorites;
import com.example.demo.core.model.InstanceMeta;
import com.example.demo.core.model.LoginCardData;
import com.example.demo.core.runtime.HardwareInfo;
import com.example.demo.core.runtime.MemoryAdvisor;
import com.example.demo.core.runtime.NetworkBoost;
import com.example.demo.core.app.Bootstrap;
import com.example.demo.core.mod.ModConflictChecker;
import com.example.demo.core.mod.ModpackExporter;
import com.example.demo.core.mod.ModpackImport;
import com.example.demo.core.mod.ServerPackExporter;
import com.example.demo.core.plugin.PluginContext;
import com.example.demo.core.plugin.PluginManager;
import com.example.demo.ui.component.AccountInfoCard;
import com.example.demo.ui.component.BlockIcons;
import com.example.demo.ui.component.CommandBar;
import com.example.demo.ui.component.ConsoleToolbar;
import com.example.demo.ui.component.CrashReportDialog;
import com.example.demo.ui.component.DownloadFilterBar;
import com.example.demo.ui.component.DownloadNavItem;
import com.example.demo.ui.component.DownloadStatusBar;
import com.example.demo.ui.component.DownloadResultView;
import com.example.demo.ui.component.LaunchAccountColumn;
import com.example.demo.ui.component.LaunchAdvisorBar;
import com.example.demo.ui.component.LaunchHeroButton;
import com.example.demo.ui.component.LaunchOverviewPane;
import com.example.demo.ui.component.LoginFormPane;
import com.example.demo.ui.component.MinecraftVersionCell;
import com.example.demo.ui.component.ModpackCell;
import com.example.demo.ui.component.SkinPreviewDialog;
import com.example.demo.ui.component.WorldsNoticePanel;
import com.example.demo.ui.page.FirstRunPanel;
import com.example.demo.ui.page.InstallProgressPage;
import com.example.demo.ui.page.InstanceSettingsPanel;
import com.example.demo.ui.page.InstancesPanel;
import com.example.demo.ui.page.LogsPanel;
import com.example.demo.ui.page.SettingsPanel;
import com.example.demo.ui.page.ToolsPanel;
import com.example.demo.core.launch.AssetsTask;
import com.example.demo.core.launch.ClientJarTask;
import com.example.demo.core.launch.LaunchContext;
import com.example.demo.core.launch.LaunchPipeline;
import com.example.demo.core.launch.LibrariesTask;
import com.example.demo.core.launch.NativesTask;
import com.example.demo.core.auth.LoginCardParser;
import com.example.demo.core.game.ImportErrors;
import com.example.demo.core.launch.LaunchScriptExporter;
import com.example.demo.core.instance.InstanceCloner;
import com.example.demo.core.instance.InstanceFiles;
import com.example.demo.core.game.JavaVersionProbe;
import com.example.demo.core.launch.LaunchPreflight;
import com.example.demo.core.model.GameDirDetector;
import com.example.demo.core.model.LanguageConfig;
import com.example.demo.core.launch.LaunchState;
import com.example.demo.core.download.ModrinthSearch;
import com.example.demo.core.download.SearchSession;
import com.example.demo.core.mod.ProjectFormat;
import com.example.demo.core.model.InstanceId;
import com.example.demo.core.launch.ServerAddress;
import com.example.demo.ui.page.DownloadPage;
import com.example.demo.ui.component.DownloadTasksDialog;
import com.example.demo.core.meta.ReleaseTime;
import com.example.demo.ui.page.VersionDetailPage;
import com.example.demo.ui.page.InstallerPage;
import com.example.demo.ui.page.LaunchPage;
import com.example.demo.ui.page.MinecraftVersionsPage;
import com.example.demo.ui.component.AccountManagerDialog;
import com.example.demo.ui.page.ModDetailPage;
public class HelloApplication extends Application {
  private static final Set<String> CLONE_DATA_DIRS =
      Set.of(
          "mods",
          "config",
          "defaultconfigs",
          "saves",
          "screenshots",
          "resourcepacks",
          "shaderpacks",
          "logs",
          "crash-reports",
          "schematics");
  private UserSession currentSession;
  private StackPane contentStack;
  private VBox rootPane;
  private VBox launchPanel;
  private HBox downloadPanel;
  private final Map<String, Node> pages = new LinkedHashMap<String, Node>();
  private final Map<String, Button> navButtons = new LinkedHashMap<String, Button>();
  private Stage primaryStage;
  private String currentPage;
  private boolean downloadInitialized;
  private InstancesPanel instancesPanel;
  private InstanceSettingsPanel instanceSettingsPanel;
  private ToolsPanel toolsPanel;
  private SettingsPanel settingsPanel;
  private String navHighlight;
  private boolean testMode;
  private WindowChrome windowChrome;
  private DownloadPage downloadPage;
  private VersionDetailPage versionDetailPage;
  private InstallerPage installerPage;
  private MinecraftVersionsPage minecraftVersionsPage;
  private ModDetailPage modDetailPage;
  private Stage stageRef;
  private volatile boolean testSuccess;
  private Rectangle navIndicator;
  private HBox navBox;
  private boolean suppressInstancePersist;
  private boolean userPickedInstance;
  private Label usernameLabel;
  private Label userTypeLabel;
  private Label uuidLabel;
  private LaunchHeroButton launchBtn;
  private ComboBox<MinecraftVersion> versionComboBox;
  private TextArea logArea;
  private VBox downloadBarPanel;
  private int lastLaunchLogStart;
  private long gameStartAt;
  private long launchPrepareStart;
  private long lastLaunchPrepMs;
  /** 最近一次预览请求，供加载页的「重试」真正重新拉取。 */
  private String lastVersionProjectId;
  private String lastVersionProjectTitle;
  private String lastVersionProjectType;
  /** 版本列表加载页的重试按钮（失败时才显示）。 */
  private Button versionRetryBtn;
  /** 版本列表加载页的提示文字（点下载后先出页面，再填内容）。 */
  private Label versionLoadingLabel;
  /** 版本列表内存缓存：同一个项目 10 分钟内不重复请求。 */
  private final Map<String, JsonArray> versionListCache = new java.util.concurrent.ConcurrentHashMap<>();
  private final Map<String, Long> versionListCacheAt = new java.util.concurrent.ConcurrentHashMap<>();
  /** 集成式弹窗宿主层。 */
  private StackPane dialogLayer;
  /** 启动准备是否正在进行 / 是否被用户要求停止。 */
  private final LaunchState launchState = new LaunchState();
  /** 启动页底部下载条（文件名 / 进度 / 速度）。 */
  private DownloadBarView downloadBarView;

  /** 启动前完整性检查的最近结果：同一实例 5 分钟内复用，避免每次启动都全量扫描。 */
  private Integrity.Report lastIntegrityReport;
  private long lastIntegrityAt;
  private String lastIntegrityProfile;
  private boolean minimizedForGame;

  private boolean quitting;
  private Label geekTelemetry;
  private final long appStartedAt = System.currentTimeMillis();
  private AmbientBackground ambientBackground;
  private String gameInstanceId;
  private ComboBox<String> loginTypeComboRef;
  private ComboBox<String> serverFieldRef;
  private VBox loginCard;
  private LaunchAccountColumn accountColumn;
  private LoginFormPane loginForm;
  private HBox launchBottomRow;
  private Button logoutButton;
  private Circle gameStatusDot;
  private Label gameStatusLabel;
  private FadeTransition gameStatusPulse;
  private Button stopGameButton;
  /** 启动准备阶段的「取消启动」胶囊（顶栏，不占实例按钮那一行）。 */
  private Button cancelLaunchButton;
  private File lastDownloadedFile;
  private StackPane dropOverlay;
  private Label dropTitle;
  private Label dropHint;
  private AccountInfoCard accountInfo;
  private CommandBar commandBar;
  private boolean aiBusy;
  private final List<DeepSeekClient.Message> aiHistory = new ArrayList<DeepSeekClient.Message>();
  private TextArea aiChatArea;
  private StackPane logPane;
  private Button logToggle;
  private ConsoleToolbar consoleToolbar;
  private HBox logActionsBar;
  private LaunchAdvisorBar launchInsightBar;
  private LaunchOverviewPane launchOverviewPane;
  /** 概览的滚动容器：窗口变矮时滚动，而不是把内容挤到命令栏上。 */
  private javafx.scene.control.ScrollPane launchOverviewScroll;
  private VBox consoleBox;
  private boolean logExpanded;
  private Node currentPageNode;
  private String pendingJoinAddress;
  private Process gameProcess;

  private ProgressBar downloadProgress;
  private Label downloadFileLabel;
  private Label downloadStatsLabel;
  private Label launchVersionLabel;
  private ImageView avatarImageView;
  private VBox favoritesPanel;
  private VBox worldsNoticePanel;
  private VBox favoritesList;
  private VBox favoritesResourceList;
  private TextField favoritesSearch;
  private ComboBox<String> favoritesSortCombo;
  private String favoritesQuery = "";
  private VBox communityPanel;
  private VBox minecraftPanel;
  private VBox installerPanel;
  private VBox versionDetailPanel;
  private VBox modDetailPanel;
  private final Map<String, ComboBox<String>> installerLoaderCombos =
      new LinkedHashMap<String, ComboBox<String>>();
  private String currentProjectType = "整合包";
  private DownloadFilterBar filterBar;
  private DownloadResultView resultView;
  private final Map<String, List<ModpackProject>> prewarmedCommunity =
      new ConcurrentHashMap<String, List<ModpackProject>>();
  private final Set<String> prewarmStarted = ConcurrentHashMap.newKeySet();
  private final CompletableFuture<Void> communityPrewarmDone = new CompletableFuture<>();
  private volatile boolean importCancelled;
  private Button importCancelButton;
  private int communityPrewarmDoneCount;
  private static final Semaphore ICON_SLOTS = new Semaphore(12);
  private final Map<String, Image> iconMemoryCache =
      new LinkedHashMap<String, Image>(128, 0.75f, true) {

        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Image> eldest) {
          return this.size() > 240;
        }
      };
  private static final Map<String, Integer> PAGE_ORDER =
      Map.of("launch", 0, "download", 1, "settings", 2, "tools", 3);
  private volatile String lastSearchSource = "Modrinth";
  private ObservableList<ModpackProject> modpackData = FXCollections.observableArrayList();
  private int searchOffset;
  private int searchTotal = -1;
  private boolean loadingMoreResults;
  private PauseTransition searchWatchdog;
  private final Map<String, List<ModpackProject>> searchPageCache =
      Collections.synchronizedMap(
          new LinkedHashMap<String, List<ModpackProject>>(64, 0.75f, true) {

            @Override
            protected boolean removeEldestEntry(Map.Entry<String, List<ModpackProject>> eldest) {
              return this.size() > 80;
            }
          });
  private ObservableList<MinecraftVersion> allMinecraftVersions =
      FXCollections.observableArrayList();
  private static final HttpClient httpClient =
      HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(15L)).build();
  private boolean isSearching = false;
  private static final Duration FAST_ANIM = Duration.millis(150.0);
  private static final Interpolator SMOOTH_INTERPOLATOR = Interpolator.SPLINE(0.25, 0.1, 0.25, 1.0);
  private final LauncherConfig config = LauncherConfig.load();

  {
    // 下载引擎跟随设置（检测不到 aria2c 时会自动回退内置下载器）
    DownloadManager.setAria2Enabled(this.config.isAria2Enabled());
    // 加载器安装器同样跟随下载源：official 直连，其它走 BMCLAPI maven 镜像
    LoaderInstaller.setMavenMirror(
        "official".equals(this.config.getDownloadSource())
            ? null
            : "https://bmclapi2.bangbang93.com/maven/");
  }
  public static String THEME_URL = null;
  private StackPane commandPaletteOverlay;

  private File resolveGameDir() {
    File configured = new File(this.config.getGameDir());
    if (configured.isAbsolute()) {
      return configured;
    }
    File resolved = new File(AppPaths.dataDir(), this.config.getGameDir()).getAbsoluteFile();
    this.config.setGameDir(resolved.getAbsolutePath());
    return resolved;
  }

  @Override
  public void start(Stage stage) {
    List<MinecraftVersion> cachedVersions;
    VBox root;
    AmbientBackground ambient;
    this.stageRef = stage;
    File bootstrapGameDir = this.resolveGameDir();
    Bootstrap.ensureGameDir(bootstrapGameDir, null);
    if (!SingleInstance.acquire(bootstrapGameDir)) {
      String reason = SingleInstance.lastError();
      boolean alreadyRunning = reason.contains("持有锁");
      Alert running =
          new Alert(
              Alert.AlertType.WARNING,
              alreadyRunning
                  ? "启动器已经在运行，请先关闭已打开的窗口。"
                  : "无法创建单实例锁文件，可能是游戏目录不可写：\n" + reason,
              ButtonType.OK);
      running.setTitle("Neko Craft Launcher");
      running.setHeaderText(alreadyRunning ? "已有实例在运行" : "无法获取运行锁");
      running.showAndWait();
      Platform.exit();
      return;
    }
    stage.setOnCloseRequest(
        e -> {
          e.consume();
          this.requestClose();
        });
    try (InputStream iconStream2 = HelloApplication.class.getResourceAsStream("/icon.png"); ) {
      if (iconStream2 != null) {
        stage.getIcons().add(new Image(iconStream2));
      }
    } catch (Exception iconStream2) {
      // empty catch block
    }
    Theme.setCurrent(Theme.byName(this.config.getAccentColor()));
    Theme.setLight(this.config.isLightTheme());
    Motion.configure(this.config.isAnimations(), this.config.getAnimationSpeed());
    HBox topBar = this.createTopBar();
    this.contentStack = new StackPane();
    this.contentStack.setAlignment(Pos.TOP_LEFT);
    this.launchPanel = this.createLaunchPanel();
    this.downloadPanel = this.createDownloadPanel();
    this.downloadPanel.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
    this.pages.put("launch", this.launchPanel);
    this.pages.put("download", this.downloadPanel);
    this.launchPanel.setId("launch-root");
    this.applyLaunchBackground();
    this.launchPanel.setMaxWidth(Double.MAX_VALUE);
    this.launchPanel.setMaxHeight(Double.MAX_VALUE);
    this.downloadPanel.setMaxWidth(Double.MAX_VALUE);
    this.downloadPanel.setMaxHeight(Double.MAX_VALUE);
    this.downloadPanel.setVisible(false);
    this.downloadPanel.setOpacity(0.0);
    this.ambientBackground = ambient = new AmbientBackground();
    ambient.setManaged(false);
    ambient.widthProperty().bind(this.contentStack.widthProperty());
    ambient.heightProperty().bind(this.contentStack.heightProperty());
    this.contentStack.getChildren().add(ambient);
    this.contentStack.getChildren().addAll(this.launchPanel, this.downloadPanel);
    this.primaryStage = stage;
    this.rootPane = root = new VBox(topBar, this.contentStack);
    VBox.setVgrow(this.contentStack, Priority.ALWAYS);
    this.contentStack.setMinSize(0.0, 0.0);
    root.setStyle(
        "-fx-background-color: linear-gradient(to bottom, -app-root-1 0%, -app-root-2 32%,"
            + " -app-root-3 100%); -fx-background-radius: 16; -fx-border-width: 0;"
            + " -fx-background-insets: 0;");
    root.setMinSize(0.0, 0.0);
    root.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
    Rectangle resizeGrip = new Rectangle(18.0, 18.0, javafx.scene.paint.Color.web("#ffffff", 0.1));
    resizeGrip.setArcWidth(6.0);
    resizeGrip.setArcHeight(6.0);
    resizeGrip.setCursor(Cursor.SE_RESIZE);
    StackPane rootWrap = new StackPane(root, resizeGrip);
    rootWrap.setAlignment(Pos.TOP_LEFT);
    this.installResizeHandles(rootWrap, stage);
    this.dropOverlay = new StackPane();
    this.dropOverlay.setVisible(false);
    this.dropOverlay.setManaged(false);
    this.dropOverlay.setMouseTransparent(true);
    this.dropOverlay.setStyle("-fx-background-color: rgba(5,20,18,0.72);");
    this.dropTitle = new Label("松开鼠标导入整合包");
    this.dropTitle.setStyle("-fx-text-fill: #ccfbf1; -fx-font-size: 24px; -fx-font-weight: bold;");
    this.dropHint = new Label("支持 .zip / .mrpack，直接拖入窗口即可安装");
    this.dropHint.setStyle("-fx-text-fill: rgba(204,251,241,0.78); -fx-font-size: 12px;");
    VBox dropBox =
        new VBox(10.0, Icons.node("download", 42.0, "#5eead4"), this.dropTitle, this.dropHint);
    dropBox.setAlignment(Pos.CENTER);
    dropBox.setStyle(
        "-fx-background-color: rgba(14,47,38,0.92); -fx-background-radius: 20; -fx-border-color:"
            + " rgba(94,234,212,0.65); -fx-border-radius: 20; -fx-padding: 34 44;");
    this.dropOverlay.getChildren().add(dropBox);
    rootWrap.getChildren().add(this.dropOverlay);
    StackPane.setAlignment(resizeGrip, Pos.BOTTOM_RIGHT);
    StackPane.setMargin(resizeGrip, new Insets(0.0, 6.0, 6.0, 0.0));
    double[] gripStart = new double[4];
    resizeGrip.setOnMousePressed(
        e -> {
          gripStart[0] = e.getScreenX();
          gripStart[1] = e.getScreenY();
          gripStart[2] = stage.getWidth();
          gripStart[3] = stage.getHeight();
        });
    resizeGrip.setOnMouseDragged(
        e -> {
          Rectangle2D vb = HelloApplication.screenBounds(stage);
          stage.setWidth(
              Math.min(
                  vb.getWidth(), Math.max(980.0, gripStart[2] + e.getScreenX() - gripStart[0])));
          stage.setHeight(
              Math.min(
                  vb.getHeight(), Math.max(640.0, gripStart[3] + e.getScreenY() - gripStart[1])));
        });
    rootWrap.setStyle("-fx-background-color: transparent;");
    Canvas crt = new Canvas();
    crt.setMouseTransparent(true);
    crt.widthProperty().bind(rootWrap.widthProperty());
    crt.heightProperty().bind(rootWrap.heightProperty());
    Runnable paintCrt =
        () -> {
          double w = crt.getWidth();
          double h = crt.getHeight();
          if (w < 4.0 || h < 4.0) {
            return;
          }
          GraphicsContext g = crt.getGraphicsContext2D();
          g.clearRect(0.0, 0.0, w, h);
          g.setFill(javafx.scene.paint.Color.web("#ff3b30", 0.018));
          for (double y = 0.0; y < h; y += 3.0) {
            g.fillRect(0.0, y, w, 1.0);
          }
          g.setStroke(javafx.scene.paint.Color.web("#ff5a48", 0.22));
          g.setLineWidth(1.0);
          double inset = 12.0;
          double len = 18.0;
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
    // 集成式弹窗的宿主层：所有确认/输入/选择框都渲染在这一层，不再弹系统窗口
    this.dialogLayer = new StackPane();
    InAppDialog.setGlobalHost(this.dialogLayer);
    this.dialogLayer.setPickOnBounds(false);
    this.dialogLayer.setVisible(true);
    // 弹窗层直接叠在原根节点上：外面再包一层 StackPane 会破坏窗口圆角与描边
    rootWrap.getChildren().add(this.dialogLayer);
    Scene scene = new Scene(rootWrap, 1060.0, 792.0);
    scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
    scene.addEventFilter(
        DragEvent.DRAG_OVER,
        event -> {
          File archive = this.firstImportArchive(event.getDragboard());
          File jar = this.firstModJar(event.getDragboard());
          if (archive == null && jar == null) {
            return;
          }
          event.acceptTransferModes(TransferMode.COPY);
          if (this.dropOverlay != null) {
            this.dropTitle.setText(jar != null ? "松开安装模组" : "松开鼠标导入整合包");
            this.dropHint.setText(
                jar != null ? "支持 .jar，可一次拖入多个文件" : "支持 .zip / .mrpack，直接拖入窗口即可安装");
            this.dropOverlay.setVisible(true);
          }
          event.consume();
        });
    scene.addEventFilter(
        DragEvent.DRAG_EXITED,
        event -> {
          if (this.dropOverlay != null) {
            this.dropOverlay.setVisible(false);
          }
        });
    scene.addEventFilter(
        DragEvent.DRAG_DROPPED,
        event -> {
          if (this.dropOverlay != null) {
            this.dropOverlay.setVisible(false);
          }
          File archive = this.firstImportArchive(event.getDragboard());
          List<File> jars = this.modJars(event.getDragboard());
          if (archive == null && jars.isEmpty()) {
            return;
          }
          event.setDropCompleted(true);
          event.consume();
          if (!jars.isEmpty()) {
            this.installDroppedMods(jars);
          } else {
            this.importModpack(archive);
          }
        });
    stage.initStyle(StageStyle.TRANSPARENT);
    stage.setMinWidth(980.0);
    stage.setMinHeight(680.0);
    stage
        .widthProperty()
        .addListener(
            (o, old, value) -> {
              Rectangle2D vb = HelloApplication.screenBounds(stage);
              if (value.doubleValue() > vb.getWidth()) {
                stage.setWidth(vb.getWidth());
              }
            });
    stage
        .heightProperty()
        .addListener(
            (o, old, value) -> {
              Rectangle2D vb = HelloApplication.screenBounds(stage);
              if (value.doubleValue() > vb.getHeight()) {
                stage.setHeight(vb.getHeight());
              }
            });
    scene.setOnKeyPressed(
        e -> {
          if (e.isControlDown() && e.isShiftDown() && e.getCode() == KeyCode.DIGIT0) {
            stage.setWidth(1060.0);
            stage.setHeight(792.0);
            stage.centerOnScreen();
            e.consume();
            return;
          }
          if (e.isControlDown() && e.getCode() == KeyCode.K) {
            this.showCommandPalette();
            e.consume();
            return;
          }
          if (e.isControlDown() && e.getCode() == KeyCode.Q) {
            this.requestClose();
            e.consume();
            return;
          }
          switch (e.getCode()) {
            case F1:
              {
                this.switchPage("launch");
                break;
              }
            case F2:
              {
                this.switchPage("instances");
                break;
              }
            case F3:
              {
                this.openInstanceSettings("overview");
                break;
              }
            case F4:
              {
                this.openInstanceSettings("mods");
                break;
              }
            case F5:
              {
                this.switchPage("download");
                break;
              }
            case F6:
              {
                this.switchPage("settings");
                break;
              }
            case F7:
              {
                this.switchPage("tools");
                break;
              }
            case F8:
              {
                this.switchPage("settings");
                if (this.settingsPanel == null) break;
                this.settingsPanel.openTab("查看日志");
                break;
              }
            case F9:
              {
                this.switchPage("launch");
                this.applyLogExpanded(!this.logExpanded);
                break;
              }
          }
        });
    try {
      URL cssUrl = HelloApplication.class.getResource("/com/example/demo/app.css");
      if (cssUrl != null) {
        scene.getStylesheets().add(cssUrl.toExternalForm());
        THEME_URL = cssUrl.toExternalForm();
      }
    } catch (Exception e2) {
      System.err.println("[theme] 全局样式加载失败，界面会回退到默认样式");
    }
    Theme.bindScene(scene);
    Toast.install(rootWrap);
    Platform.setImplicitExit(false);
    stage.setOpacity(Math.max(0.7, Math.min(1.0, this.config.getUiOpacity())));
    stage.setTitle("Neko Craft Launcher");
    stage
        .iconifiedProperty()
        .addListener(
            (obs, was, iconified) -> {
              if (iconified.booleanValue()) {
                ambient.stop();
              } else if (!this.launchState.isRunning() || !this.config.isPauseVisualsInGame()) {
                ambient.start();
              }
            });
    BootScreen boot =
        new BootScreen(
            () -> Platform.runLater(this::maybeOfferPerformanceOptimization), () -> true);
    rootWrap.getChildren().add(boot);
    stage.setScene(scene);
    stage.show();

    // 网络加速：启动后异步测速并配置 DNS（失败只写日志，不影响使用）
    if (this.config.isNetworkBoost()) {
      CompletableFuture.supplyAsync(NetworkBoost::autoApply)
          .thenAccept(
              result ->
                  Platform.runLater(
                      () -> {
                        if (result.ok()) {
                          this.logLine("🌐 网络加速：" + result.message());
                          if (result.chosen() != null) {
                            this.config.setPreferredDns(result.chosen().server());
                            this.config.save();
                          }
                        } else {
                          this.logLine("🌐 网络加速未生效：" + result.message());
                        }
                      }));
    }
    stage.centerOnScreen();
    this.switchPage("launch");
    CompletableFuture.runAsync(() -> PluginManager.load(this::logMessage));
    Bootstrap.ensureGameDir(
        new File(this.config.getGameDir()),
        message -> Platform.runLater(() -> this.logArea.appendText(message + "\n")));
    LauncherLog.attach(new File(this.config.getGameDir()), this.logArea);
    if (new File(AppPaths.baseDir(), ".portable").isFile()) {
      this.logArea.appendText("便携模式：不读取本机账户注册表，请重新登录\n");
    } else {
      this.restoreSessionFromRegistry();
    }
    File detectGameDir = new File(this.config.getGameDir());
    CompletableFuture.runAsync(
        () -> {
          HardwareInfo.Detect hardware = HardwareInfo.detect(detectGameDir);
          MemoryAdvisor.Suggestion suggestion =
              MemoryAdvisor.suggest(
                  InstanceDirs.mods(
                      detectGameDir,
                      this.config.getLastInstance(),
                      this.isIsolated(detectGameDir, this.config.getLastInstance())));
          Platform.runLater(
              () -> {
                this.logArea.appendText("🖥 硬件识别：" + hardware.summary() + "\n");
                this.logArea.appendText(
                    "💡 智能推荐内存：" + suggestion.memoryMb() + " MB（" + suggestion.reason() + "）\n");
              });
        });
    String autoImport = System.getProperty("mc.import");
    if (autoImport != null && !autoImport.isBlank()) {
      Platform.runLater(
          () -> {
            this.logArea.appendText("🧪 自动导入测试: " + autoImport + "\n");
            this.importModpack(new File(autoImport));
          });
    }
    DownloadManager.addStatusListener(
        status -> Platform.runLater(() -> this.updateDownloadBar(status)));
    this.updateGeekTelemetry();
    Timeline telemetryTimer =
        new Timeline(
            new KeyFrame(Duration.seconds(3.0), e -> this.updateGeekTelemetry(), new KeyValue[0]));
    telemetryTimer.setCycleCount(-1);
    telemetryTimer.play();
    this.refreshInstalledVersions();
    if (!this.config.isHardwareAcceleration()) {
      this.logArea.appendText("🖥 已按设置禁用硬件加速（软件渲染）。如果界面明显变卡，可在 设置 → 启动器 里重新开启。\n");
    }
    if ((cachedVersions = VersionManager.loadCachedList()) != null && !cachedVersions.isEmpty()) {
      this.applyVersionList(cachedVersions, false);
    }

    VersionManager.fetchVersionListAsync()
        .thenAccept(
            versionList -> Platform.runLater(() -> this.applyVersionList(versionList, true)))
        .exceptionally(
            ex -> {
              Platform.runLater(
                  () -> {
                    if (this.versionComboBox.getItems().isEmpty()) {
                      this.versionComboBox.setPromptText("版本列表加载失败");
                    }
                    this.logArea.appendText("❌ 版本列表加载失败: " + ex.getMessage() + "\n");
                    this.logArea.appendText("   已安装的版本仍然可以直接启动。\n");
                  });
              return null;
            });
  }

  private void applyVersionList(List<MinecraftVersion> versionList, boolean announce) {
    if (versionList == null || versionList.isEmpty()) {
      return;
    }
    this.allMinecraftVersions.setAll((Collection<MinecraftVersion>) versionList);
    List<MinecraftVersion> releases =
        versionList.stream().filter(v -> "release".equals(v.getType())).toList();
    if (releases.isEmpty()) {
      if (announce) {
        this.logArea.appendText("⚠️ 版本列表里没有正式版，只拿到快照。\n");
      }
      return;
    }
    this.versionComboBox.getItems().setAll((Collection<MinecraftVersion>) releases);
    this.suppressInstancePersist = true;
    try {
      this.versionComboBox.getSelectionModel().selectFirst();
      this.refreshInstalledVersions();
    } finally {
      this.suppressInstancePersist = false;
    }
    if (announce) {
      this.logArea.appendText(
          "✅ 版本列表加载完成（共 " + versionList.size() + " 个版本，" + releases.size() + " 个正式版）\n");
    }
  }

  private HBox createTopBar() {
    String[][] entries;
    HBox topBar = new HBox(14.0);
    topBar.setAlignment(Pos.CENTER_LEFT);
    topBar.getStyleClass().add("app-topbar");
    topBar.setPadding(new Insets(0.0, 6.0, 0.0, 18.0));
    topBar.setPrefHeight(58.0);
    topBar.setMinHeight(58.0);
    Rectangle mark = new Rectangle(3.0, 18.0);
    mark.setArcWidth(3.0);
    mark.setArcHeight(3.0);
    mark.setStyle("-fx-fill: #14b8a6;");
    Label appTitle = new Label("Neko Craft Launcher");
    appTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
    appTitle.setTextFill(javafx.scene.paint.Color.web("#e8eaee"));
    Label badge = new Label("NEKO");
    badge.setStyle("-fx-font-size: 10.5px; -fx-font-weight: bold;");
    badge.setTextFill(javafx.scene.paint.Color.web("#5eead4"));
    Label liveClock = new Label();
    liveClock.setStyle(
        "-fx-text-fill: #7b818b; -fx-font-size: 10.5px; -fx-font-family: 'Cascadia Mono',"
            + " 'Consolas', monospace;");
    DateTimeFormatter clockFormat = DateTimeFormatter.ofPattern("HH:mm:ss");
    liveClock.setText(LocalTime.now().format(clockFormat));
    Timeline clockTimer =
        new Timeline(
            new KeyFrame(
                Duration.seconds(1.0),
                e -> {
                  if (liveClock.isVisible()) {
                    liveClock.setText(LocalTime.now().format(clockFormat));
                  }
                },
                new KeyValue[0]));
    clockTimer.setCycleCount(-1);
    clockTimer.play();
    this.gameStatusDot = new Circle(4.0, javafx.scene.paint.Color.web("#6b7280"));
    this.gameStatusLabel = new Label("就绪");
    this.gameStatusLabel.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 10px;");
    HBox statusChip = new HBox(6.0, this.gameStatusDot, this.gameStatusLabel);
    statusChip.setAlignment(Pos.CENTER_LEFT);
    statusChip.setPadding(new Insets(3.0, 9.0, 3.0, 9.0));
    statusChip.setStyle(
        "-fx-background-color: rgba(255,255,255,0.045); -fx-background-radius: 10;"
            + " -fx-border-color: rgba(255,255,255,0.06); -fx-border-radius: 10;");
    this.stopGameButton = new Button("结束游戏");
    this.stopGameButton.setStyle(
        "-fx-background-color: rgba(248,113,113,0.16); -fx-text-fill: #fecaca; -fx-border-color:"
            + " rgba(248,113,113,0.55); -fx-border-radius: 8; -fx-background-radius: 8;"
            + " -fx-font-size: 10.5px; -fx-padding: 3 10; -fx-cursor: hand;");
    this.stopGameButton.setVisible(false);
    this.stopGameButton.setManaged(false);
    this.stopGameButton.setOnAction(e -> this.killGameProcess());
    this.cancelLaunchButton = new Button("取消启动");
    this.cancelLaunchButton.setStyle(
        "-fx-background-color: transparent; -fx-text-fill: -app-text-dim;"
            + " -fx-border-color: rgba(148,163,184,0.45); -fx-border-radius: 8;"
            + " -fx-background-radius: 8; -fx-font-size: 10.5px; -fx-padding: 3 10;"
            + " -fx-cursor: hand;");
    this.cancelLaunchButton.setVisible(false);
    this.cancelLaunchButton.setManaged(false);
    this.cancelLaunchButton.setOnAction(e -> this.stopLaunch());
    HBox brand =
        new HBox(
            8.0,
            mark,
            appTitle,
            Icons.node("star", 13.0, "#ffcf6a"),
            badge,
            liveClock,
            statusChip,
            this.cancelLaunchButton,
            this.stopGameButton);
    brand.setAlignment(Pos.CENTER_LEFT);
    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);
    this.navBox = new HBox(2.0);
    this.navBox.setAlignment(Pos.CENTER);
    this.navIndicator = new Rectangle(0.0, 2.0);
    this.navIndicator.setArcWidth(2.0);
    this.navIndicator.setArcHeight(2.0);
    this.navIndicator.setStyle("-fx-fill: linear-gradient(to right, #d1d5db, #9ca3af);");
    this.navIndicator.setMouseTransparent(true);
    this.navIndicator.setVisible(false);
    this.navIndicator.setManaged(false);
    StackPane navPane = new StackPane(this.navIndicator, this.navBox);
    navPane.setAlignment(Pos.CENTER);
    StackPane.setAlignment(this.navIndicator, Pos.BOTTOM_LEFT);
    navPane.setPickOnBounds(false);
    for (String[] entry :
        entries =
            new String[][] {
              {"launch", "启动", "play"},
              {"download", "下载", "download"},
              {"advisor", "顾问", "message"},
              {"settings", "设置", "gear"},
              {"tools", "工具", "tools"}
            }) {
      Button button = this.createTopButton(entry[1], entry[2]);
      button.setUserData(entry[2]);
      button.setOnAction(e -> this.switchPage(entry[0]));
      this.navButtons.put(entry[0], button);
      Motion.press(button);
      this.navBox.getChildren().add(button);
    }
    Button minimize = this.windowButton(WindowIcon.MINIMIZE);
    minimize.setOnAction(e -> this.primaryStage.setIconified(true));
    Button close = this.windowButton(WindowIcon.CLOSE);
    close.setOnAction(e -> this.requestClose());
    HBox windowControls = new HBox(0.0, minimize, close);
    windowControls.setAlignment(Pos.CENTER_RIGHT);
    windowControls.getStyleClass().add("window-controls");
    topBar
        .widthProperty()
        .addListener(
            (obs, old, width) -> {
              boolean compact = width.doubleValue() < 1080.0;
              badge.setVisible(!compact);
              badge.setManaged(!compact);
              liveClock.setVisible(!compact);
              liveClock.setManaged(!compact);
              statusChip.setVisible(!compact);
              statusChip.setManaged(!compact);
              brand.setSpacing(compact ? 5.0 : 8.0);
              this.navBox.setSpacing(compact ? 0.0 : 2.0);
            });
    double[] dragOffset = new double[2];
    topBar.setOnMousePressed(
        e -> {
          dragOffset[0] = e.getSceneX();
          dragOffset[1] = e.getSceneY();
        });
    topBar.setOnMouseDragged(
        e -> {
          if (this.primaryStage.isMaximized()) {
            return;
          }
          this.primaryStage.setX(e.getScreenX() - dragOffset[0]);
          this.primaryStage.setY(e.getScreenY() - dragOffset[1]);
          HelloApplication.clampStagePosition(this.primaryStage);
        });
    topBar.setOnMouseClicked(
        e -> {
          if (e.getClickCount() == 2 && !(e.getTarget() instanceof Button)) {
            this.primaryStage.setWidth(1060.0);
            this.primaryStage.setHeight(792.0);
            this.primaryStage.centerOnScreen();
          }
        });
    Button paletteBtn = new Button();
    paletteBtn.setGraphic(Icons.node("search", 14.0, "#c7ccd4"));
    paletteBtn.setPrefSize(30.0, 30.0);
    paletteBtn.setMinSize(30.0, 30.0);
    paletteBtn.setMaxSize(30.0, 30.0);
    paletteBtn.setStyle(
        "-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 0;"
            + " -fx-background-radius: 8;");
    paletteBtn.setTooltip(new Tooltip("命令面板  Ctrl+K"));
    paletteBtn.setOnMouseEntered(
        e ->
            paletteBtn.setStyle(
                "-fx-background-color: rgba(255,255,255,0.10); -fx-cursor: hand; -fx-padding: 0;"
                    + " -fx-background-radius: 8;"));
    paletteBtn.setOnMouseExited(
        e ->
            paletteBtn.setStyle(
                "-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 0;"
                    + " -fx-background-radius: 8;"));
    paletteBtn.setOnAction(e -> this.showCommandPalette());
    topBar.getChildren().addAll(brand, spacer, navPane, paletteBtn, windowControls);
    return topBar;
  }

  private String windowButtonStyle() {
    return "-fx-background-color: transparent; -fx-text-fill: white; -fx-cursor: hand; -fx-padding:"
        + " 0; -fx-background-radius: 0;";
  }

  private Button windowButton(WindowIcon icon) {
    Button button = new Button();
    button.setGraphic(this.windowIcon(icon));
    button.setPrefSize(46.0, 32.0);
    button.setMinSize(46.0, 32.0);
    button.setMaxSize(46.0, 32.0);
    button.setFocusTraversable(false);
    button.setStyle(this.windowButtonStyle());
    button.getStyleClass().add("window-control");
    button.setOnMouseEntered(
        e ->
            button.setStyle(
                "-fx-background-color: "
                    + (icon == WindowIcon.CLOSE ? "#e81123" : "rgba(255,255,255,0.12)")
                    + "; -fx-cursor: hand; -fx-padding: 0; -fx-background-radius: 0;"));
    button.setOnMouseExited(e -> button.setStyle(this.windowButtonStyle()));
    return button;
  }

  private Node windowIcon(WindowIcon icon) {
    Pane pane = new Pane();
    pane.setPrefSize(14.0, 14.0);
    pane.setMinSize(14.0, 14.0);
    pane.setMaxSize(14.0, 14.0);
    Function<String, SVGPath> path =
        content -> {
          SVGPath svg = new SVGPath();
          svg.setContent(content);
          svg.setFill(javafx.scene.paint.Color.TRANSPARENT);
          svg.setStroke(javafx.scene.paint.Color.WHITE);
          svg.setStrokeWidth(1.1);
          svg.setStrokeLineCap(StrokeLineCap.SQUARE);
          return svg;
        };
    switch (icon.ordinal()) {
      case 0:
        {
          pane.getChildren().add(path.apply("M2 7 H12"));
          break;
        }
      case 1:
        {
          pane.getChildren().add(path.apply("M2.5 2.5 H11.5 V11.5 H2.5 Z"));
          break;
        }
      case 2:
        {
          pane.getChildren().add(path.apply("M5 2.5 H11.5 V9"));
          pane.getChildren().add(path.apply("M2.5 5 H9 V11.5 H2.5 Z"));
          break;
        }
      case 3:
        {
          pane.getChildren().add(path.apply("M2.5 2.5 L11.5 11.5 M11.5 2.5 L2.5 11.5"));
        }
    }
    return pane;
  }

  private Button createTopButton(String text, String icon) {
    Button button = new Button(text);
    button.setGraphic(Icons.node(icon, 14.0, "#c7ccd4"));
    button.setContentDisplay(ContentDisplay.LEFT);
    button.setGraphicTextGap(6.0);
    button.getStyleClass().add("nav-btn");
    return button;
  }

  private Node createPage(String name) {
    File gameDir = new File(this.config.getGameDir());
    return switch (name) {
      case "launch" -> this.launchPanel;
      case "download" -> this.downloadPanel;
      case "advisor" -> this.createAdvisorPanel();
      case "instances" -> {
        this.instancesPanel =
            new InstancesPanel(
                gameDir,
                this.currentInstanceId(),
                this::selectInstance,
                this::logMessage,
                this.config,
                this::switchGameDir,
                instance -> this.deleteInstance(instance.id),
                (instance, copyData) -> this.cloneInstance(instance.id, (boolean) copyData),
                this::checkInstance,
                instance -> {
                  this.selectInstance(instance);
                  this.openInstanceSettings("overview");
                },
                this::importModpackDialog,
                this::exportInstance,
                () -> this.switchPage("launch"));
        yield this.instancesPanel;
      }
      case "settings" -> {
        this.settingsPanel = new SettingsPanel(this.config, this::logMessage);
        yield this.settingsPanel;
      }
      case "tools", "optimize" -> {
        if (this.toolsPanel == null) {
          this.toolsPanel =
              new ToolsPanel(this.config, this::logMessage, this::refreshInstalledVersions);
        }
        if ("optimize".equals(name)) {
          this.toolsPanel.openPerformanceTab();
        }
        yield this.toolsPanel;
      }
      case "logs" -> new LogsPanel(new File(this.config.getGameDir()), this::logMessage);
      default -> this.launchPanel;
    };
  }

  private void switchPage(String pageName) {
    Node target = this.pages.computeIfAbsent(pageName, this::createPage);
    if (!this.contentStack.getChildren().contains(target)) {
      target.setVisible(false);
      target.setOpacity(0.0);
      this.contentStack.getChildren().add(target);
    }
    String highlight = this.navHighlight != null ? this.navHighlight : pageName;
    this.navHighlight = null;
    this.applyNavHighlight(highlight);
    if (this.currentPage != null && this.currentPage.equals(pageName) && target.isVisible()) {
      return;
    }
    Node current = this.currentPageNode;
    this.currentPage = pageName;
    if (current == target) {
      target.setVisible(true);
      target.setOpacity(1.0);
      this.currentPageNode = target;
      return;
    }
    if (current != null) {
      Motion.pageOut(current, null);
    }
    if ("download".equals(pageName) && !this.downloadInitialized) {
      this.downloadInitialized = true;
      this.ensureCommunityPrewarm();
      Platform.runLater(
          () -> {
            this.performSearch();
            this.alignDownloadNavIndicator();
          });
    }
    target.setVisible(true);
    this.currentPageNode = target;
    for (int i = 1; i < this.contentStack.getChildren().size(); ++i) {
      Node child = this.contentStack.getChildren().get(i);
      if (child == target) continue;
      child.setVisible(false);
      child.setOpacity(0.0);
    }
    Motion.pageIn(target);
    this.applyLaunchBackground();
    if ("launch".equals(pageName)) {
      this.applyAiMode();
      if (this.launchInsightBar != null) {
        this.launchInsightBar.refresh();
      }
    }
  }

  private void applyLaunchBackground() {
    String path = this.config.getBackgroundImage();
    File file = path == null || path.isBlank() ? null : new File(path);
    this.applyPanelBackground(this.launchPanel, file);
    this.applyPanelBackground(this.downloadPanel, file);
    this.applyPanelBackground(this.settingsPanel, file);
    this.applyPanelBackground(this.toolsPanel, file);
    this.applyPanelBackground(this.instancesPanel, file);
    this.applyPanelBackground(this.instanceSettingsPanel, file);
    this.applyPanelBackground(this.communityPanel, file);
    this.applyPanelBackground(this.minecraftPanel, file);
  }

  private void applyPanelBackground(Node panel, File file) {
    if (panel == null) {
      return;
    }
    String style = panel.getStyle() == null ? "" : panel.getStyle();
    style =
        style
            .replaceAll("(?i)-fx-background-image\\s*:[^;]*;?", "")
            .replaceAll("(?i)-fx-background-size\\s*:[^;]*;?", "")
            .replaceAll("(?i)-fx-background-position\\s*:[^;]*;?", "");
    if (file != null && file.isFile()) {
      style =
          style
              + " -fx-background-image: url('"
              + String.valueOf(file.toURI())
              + "'); -fx-background-size: cover; -fx-background-position: center center;";
    }
    panel.setStyle(style.trim());
  }

  private void applyNavHighlight(String highlight) {
    for (Map.Entry<String, Button> entry : this.navButtons.entrySet()) {
      Object object;
      Button button = entry.getValue();
      boolean active = entry.getKey().equals(highlight);
      button.getStyleClass().remove("nav-btn-active");
      if (active) {
        button.getStyleClass().add("nav-btn-active");
      }
      if (!((object = button.getUserData()) instanceof String)) continue;
      String iconName = (String) object;
      button.setGraphic(Icons.node(iconName, 14.0, active ? "#5eead4" : "#b8c7d9"));
    }
    this.moveNavIndicator(highlight);
  }

  private void moveNavIndicator(String key) {
    if (this.navIndicator == null || this.navBox == null) {
      return;
    }
    Button active = this.navButtons.get(key);
    if (active == null) {
      return;
    }
    Platform.runLater(
        () -> {
          double targetX = active.getLayoutX();
          double targetWidth = active.getWidth();
          Timeline timeline =
              new Timeline(
                  new KeyFrame(
                      Duration.millis(260.0),
                      new KeyValue(this.navIndicator.translateXProperty(), targetX, Motion.SPRING),
                      new KeyValue(this.navIndicator.widthProperty(), targetWidth, Motion.SPRING)));
          timeline.play();
        });
  }

  private void alignDownloadNavIndicator() {
    if (this.downloadPage != null) {
      this.downloadPage.alignIndicator();
    }
  }

  private VBox createAdvisorPanel() {
    Label title = new Label("启动顾问");
    title.setStyle("-fx-text-fill: -app-text; -fx-font-size: 22px; -fx-font-weight: bold;");
    Label desc = new Label("启动建议、稳定性记录和最近异常会在这里独立显示，不再占用启动页。");
    desc.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 12px;");
    desc.setWrapText(true);
    VBox card = new VBox(12.0, title, desc);
    card.setPadding(new Insets(18.0));
    card.setStyle(
        "-fx-background-color: -app-card; -fx-background-radius: 18; -fx-border-color:"
            + " -app-border-2; -fx-border-radius: 18;");
    if (this.launchInsightBar != null) {
      card.getChildren().add(this.launchInsightBar);
    }
    Button refresh = new Button("刷新建议");
    refresh.setStyle(
        "-fx-background-color: -app-row3; -fx-text-fill: -app-text-2; -fx-background-radius: 8;"
            + " -fx-padding: 6 14; -fx-cursor: hand;");
    refresh.setOnAction(
        e -> {
          if (this.launchInsightBar != null) {
            this.launchInsightBar.refresh();
          }
        });
    HBox actions = new HBox(8.0, refresh);
    card.getChildren().add(actions);
    VBox page = new VBox(16.0, card);
    page.setPadding(new Insets(22.0));
    ScrollPane scroll = new ScrollPane(page);
    scroll.setFitToWidth(true);
    scroll.setStyle(
        "-fx-background: transparent; -fx-background-color: transparent; -fx-border-color:"
            + " transparent;");
    VBox wrapper = new VBox(scroll);
    VBox.setVgrow(scroll, Priority.ALWAYS);
    return wrapper;
  }

  private void applyLogExpanded(boolean expanded) {
    this.logExpanded = expanded;
    this.refreshLaunchBody();
  }

  private void refreshLaunchBody() {
    if (this.logPane == null || this.consoleBox == null) {
      return;
    }
    boolean aiOn = this.config.isAiEnabled();
    boolean showLog = !aiOn && this.logExpanded;
    boolean showOverview = !aiOn && !this.logExpanded;
    HelloApplication.setFlexible(this.aiChatArea, aiOn);
    HelloApplication.setFlexible(
        this.launchOverviewScroll == null ? this.launchOverviewPane : this.launchOverviewScroll,
        showOverview);
    this.logPane.setVisible(showLog);
    this.logPane.setManaged(showLog);
    this.logPane.setMinHeight(showLog ? 180.0 : 0.0);
    this.logPane.setPrefHeight(showLog ? -1.0 : 0.0);
    this.logPane.setMaxHeight(showLog ? Double.MAX_VALUE : 0.0);
    VBox.setVgrow(this.logPane, showLog ? Priority.ALWAYS : Priority.NEVER);
    // 只有日志展开时才让 consoleBox 撑高；AI 模式下撑高会把命令栏顶到一大片空白里，
    // 聊天区才是那个该占满剩余高度的容器。
    boolean growConsole = showLog;
    this.consoleBox.setMaxHeight(growConsole ? Double.MAX_VALUE : -1.0);
    VBox.setVgrow(this.consoleBox, growConsole ? Priority.ALWAYS : Priority.NEVER);
    if (this.logToggle != null) {
      this.logToggle.setText(this.logExpanded ? "收起日志" : "展开日志");
    }
    if (this.logActionsBar != null) {
      // AI 模式下日志区不显示，这一排按钮（收起日志/清空/复制…）也没意义
      this.logActionsBar.setVisible(!aiOn);
      this.logActionsBar.setManaged(!aiOn);
      if (!aiOn) {
        for (Node node : this.logActionsBar.getChildren()) {
          if (node == this.logToggle
              || node
                  == this.logActionsBar.getChildren().get(this.logActionsBar.getChildren().size() - 1)
              || node instanceof Region && !(node instanceof Button)) continue;
          node.setVisible(this.logExpanded);
          node.setManaged(this.logExpanded);
        }
      }
    }
    if (showOverview && this.launchOverviewPane != null) {
      this.launchOverviewPane.refresh();
    }
    this.consoleBox.requestLayout();
  }

  private static void setFlexible(Node node, boolean on) {
    if (node == null) {
      return;
    }
    node.setVisible(on);
    node.setManaged(on);
    if (node instanceof Region) {
      Region region = (Region) node;
      region.setMinHeight(0.0);
      region.setPrefHeight(on ? -1.0 : 0.0);
      region.setMaxHeight(on ? Double.MAX_VALUE : 0.0);
    }
    VBox.setVgrow(node, on ? Priority.ALWAYS : Priority.NEVER);
  }

  private VBox createLaunchPanel() {
    HBox logActions;
    VBox downloadBar;
    this.accountColumn =
        new LaunchAccountColumn(
            this.config,
            new LaunchAccountColumn.Callbacks() {
              @Override
              public void log(String text) {
                HelloApplication.this.logLine(text);
              }

              @Override
              public void openSkinPreview() {
                HelloApplication.this.openSkinPreview();
              }

              @Override
              public void showAccountManager() {
                HelloApplication.this.showAccountManager();
              }

              @Override
              public void showHealthCheck() {
                HelloApplication.this.showHealthCheck();
              }

              @Override
              public void chooseAvatar() {
                HelloApplication.this.chooseAvatar();
              }

              @Override
              public void toggleAvatarMode() {
                HelloApplication.this.toggleAvatarMode();
              }

              @Override
              public void launch() {
                HelloApplication.this.handleLaunch();
              }

              @Override
              public void openInstances() {
                HelloApplication.this.switchPage("instances");
              }

              @Override
              public void openInstanceSettings() {
                HelloApplication.this.openInstanceSettings("overview");
              }

              @Override
              public void logout() {
                HelloApplication.this.logout();
              }

              @Override
              public void submitLogin(LoginFormPane form) {
                HelloApplication.this.handleLogin(
                    form.typeCombo(),
                    form.serverCombo(),
                    form.username(),
                    form.password(),
                    form.loginButton());
              }

              @Override
              public void loginTypeChanged(String type) {
                HelloApplication.this.resizeForLoginType(type);
              }

              @Override
              public UserSession session() {
                return HelloApplication.this.currentSession;
              }

              @Override
              public LoginCardData parseLoginCard(String text) {
                return HelloApplication.this.parseLoginCard(text);
              }

              @Override
              public String normalizeServer(String server) {
                return HelloApplication.this.normalizeLittleSkinServer(server);
              }
            });
    this.avatarImageView = this.accountColumn.avatarPane().imageView();
    this.applyAvatarOverride();
    this.usernameLabel = this.accountColumn.usernameLabel();
    this.userTypeLabel = this.accountColumn.userTypeLabel();
    this.uuidLabel = this.accountColumn.uuidLabel();
    this.loginForm = this.accountColumn.loginForm();
    this.loginCard = this.loginForm;
    this.loginTypeComboRef = this.loginForm.typeCombo();
    this.serverFieldRef = this.loginForm.serverCombo();
    this.logoutButton = this.accountColumn.logoutButton();
    this.accountInfo = this.accountColumn.accountInfo();
    this.launchBtn = this.accountColumn.launchButton();
    this.launchVersionLabel = this.launchBtn.versionLabel();
    this.launchBottomRow = this.accountColumn.bottomRow();
    this.versionComboBox = new ComboBox<>();
    this.versionComboBox.setPromptText("加载版本中...");
    this.versionComboBox.setMaxWidth(Double.MAX_VALUE);
    this.versionComboBox.setPrefHeight(36.0);
    this.versionComboBox.setMinHeight(36.0);
    this.versionComboBox.setStyle("-fx-padding: 5;");
    this.versionComboBox.setConverter(
        new StringConverter<MinecraftVersion>() {

          @Override
          public String toString(MinecraftVersion o) {
            return o == null ? "" : o.getId();
          }

          @Override
          public MinecraftVersion fromString(String s) {
            return null;
          }
        });
    this.versionComboBox.setButtonCell(new MinecraftVersionCell());
    this.versionComboBox.setVisible(false);
    this.versionComboBox.setManaged(false);
    VBox userArea = this.accountColumn;
    VBox logContainer = new VBox(4.0);
    logContainer.setStyle(
        "-fx-background-color: -app-card; -fx-background-radius: 16; -fx-padding: 6;"
            + " -fx-border-color: -app-border-2; -fx-border-width: 1; -fx-border-radius: 16;"
            + " -fx-effect: dropshadow(gaussian, rgba(15,23,42,0.07), 20, 0.08, 0, 6);");
    this.logArea = new TextArea();
    // 下载器的源站提示（慢源切换、CDN 源站错误）直接写进日志
    DownloadManager.setMessageSink(this::logLine);
    this.logArea.setEditable(false);
    this.logArea.setWrapText(true);
    this.logArea.setStyle(
        "-fx-control-inner-background: -app-bg-2; -fx-background-color: -app-bg-2; -fx-text-fill:"
            + " -app-text-dim; -fx-font-family: 'Microsoft YaHei UI', 'Segoe UI Variable Text',"
            + " 'Segoe UI'; -fx-background-radius: 12 12 0 0; -fx-border-color: transparent;"
            + " -fx-font-size: 11.5px;");
    VBox.setVgrow(this.logArea, Priority.ALWAYS);
    this.aiChatArea = new TextArea();
    this.aiChatArea.setEditable(false);
    this.aiChatArea.setWrapText(true);
    this.aiChatArea.setPrefHeight(120.0);
    this.aiChatArea.setMinHeight(100.0);
    this.aiChatArea.setMaxHeight(160.0);
    this.aiChatArea.setStyle(
        "-fx-control-inner-background: -app-card-alt; -fx-text-fill: -app-text; -fx-font-size:"
            + " 11.5px; -fx-background-radius: 10; -fx-border-color: -app-border-soft;"
            + " -fx-border-radius: 10;");
    this.aiChatArea.setVisible(false);
    this.aiChatArea.setManaged(false);
    DownloadStatusBar downloadStatusBar = new DownloadStatusBar(this::cancelImport);
    this.downloadFileLabel = downloadStatusBar.fileLabel();
    this.downloadStatsLabel = downloadStatusBar.statsLabel();
    this.downloadProgress = downloadStatusBar.progress();
    this.geekTelemetry = downloadStatusBar.telemetry();
    this.importCancelButton = downloadStatusBar.cancelButton();
    this.downloadBarPanel = downloadBar = downloadStatusBar.node();
    this.downloadBarView =
        new DownloadBarView(
            this.downloadProgress, this.downloadFileLabel, this.downloadStatsLabel,
            this.downloadBarPanel);
    Button refreshFab = new Button();
    refreshFab.setGraphic(Icons.node("refresh", 16.0, "#9ca3af"));
    refreshFab.setStyle(
        "-fx-background-color: linear-gradient(to bottom right, #d1d5db, #6b7280); -fx-text-fill:"
            + " #111827; -fx-font-size: 17px; -fx-background-radius: 20; -fx-min-width: 40;"
            + " -fx-min-height: 40; -fx-cursor: hand;");
    Motion.press(refreshFab);
    refreshFab.setOnMouseEntered(
        e ->
            refreshFab.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #e5e7eb, #9ca3af);"
                    + " -fx-text-fill: #111827; -fx-font-size: 17px; -fx-background-radius: 20;"
                    + " -fx-min-width: 40; -fx-min-height: 40; -fx-cursor: hand;"));
    refreshFab.setOnMouseExited(
        e ->
            refreshFab.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #d1d5db, #6b7280);"
                    + " -fx-text-fill: #111827; -fx-font-size: 17px; -fx-background-radius: 20;"
                    + " -fx-min-width: 40; -fx-min-height: 40; -fx-cursor: hand;"));
    refreshFab.setOnAction(e -> this.reloadVersionsAsync());
    this.logPane = new StackPane(this.logArea, refreshFab);
    StackPane.setAlignment(refreshFab, Pos.BOTTOM_RIGHT);
    StackPane.setMargin(refreshFab, new Insets(0.0, 12.0, 12.0, 0.0));
    VBox.setVgrow(this.logPane, Priority.ALWAYS);
    this.logPane.setMinHeight(0.0);
    this.logArea.setMinHeight(0.0);
    this.consoleToolbar =
        new ConsoleToolbar(
            new ConsoleToolbar.Host() {
              @Override
              public void toggleLog() {
                HelloApplication.this.applyLogExpanded(!HelloApplication.this.logExpanded);
              }

              @Override
              public void clearLog() {
                HelloApplication.this.logArea.clear();
              }

              @Override
              public void copyLog() {
                ClipboardContent content = new ClipboardContent();
                content.putString(HelloApplication.this.logArea.getText());
                Clipboard.getSystemClipboard().setContent(content);
                Toast.ok("日志已复制");
              }

              @Override
              public void openLogDir() {
                HelloApplication.this.openDir(
                    new File(HelloApplication.this.config.getGameDir(), "logs"));
              }

              @Override
              public void showShortcuts() {
                InAppDialog.alert("Neko Craft Launcher 快捷键", ConsoleToolbar.shortcutHelp());
              }
            });
    this.logToggle = this.consoleToolbar.toggleButton();
    this.launchOverviewPane =
        new LaunchOverviewPane(
            this.config,
            this::currentInstanceId,
            () -> this.switchPage("instances"),
            () -> this.openInstanceSettings("overview"),
            () -> this.switchPage("download"),
            () -> this.switchPage("advisor"));
    this.launchOverviewScroll = new javafx.scene.control.ScrollPane(this.launchOverviewPane);
    this.launchOverviewScroll.setFitToWidth(true);
    this.launchOverviewScroll.setHbarPolicy(
        javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
    this.launchOverviewScroll.setVbarPolicy(
        javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED);
    this.launchOverviewScroll.setMinHeight(0.0);
    this.launchOverviewScroll.setStyle(
        "-fx-background: transparent; -fx-background-color: transparent;"
            + " -fx-border-color: transparent; -fx-padding: 0;");
    VBox.setVgrow(this.launchOverviewScroll, Priority.ALWAYS);
    this.logActionsBar =
        logActions =
            this.consoleToolbar.node();
    this.commandBar = new CommandBar(this.config, this::handleCommandInput, this::onAiModeToggled);
    HBox commandBar = this.commandBar;
    this.launchInsightBar =
        new LaunchAdvisorBar(
            this.config,
            () -> {
              this.switchPage("tools");
              if (this.toolsPanel != null) {
                this.toolsPanel.openHistoryTab();
              }
            });
    this.consoleBox = new VBox(0.0, this.logPane, commandBar);
    this.consoleBox.setFillWidth(true);
    logContainer
        .getChildren()
        .addAll(
            this.aiChatArea,
            this.launchOverviewScroll,
            this.consoleBox,
            logActions,
            downloadBar);
    HBox.setHgrow(logContainer, Priority.ALWAYS);
    // 默认收起日志，先把「启动概览」露出来；想看实时输出点一下「展开日志」
    this.applyLogExpanded(false);
    // 黄金比例分栏与模块标题在 ui.page.LaunchPage：这里只准备两列内容
    logContainer.setMinWidth(0.0);
    VBox launchPanel = LaunchPage.build(userArea, logContainer);
    this.loginForm
        .loginButton()
        .setOnAction(
            e ->
                this.handleLogin(
                    this.loginForm.typeCombo(),
                    this.loginForm.serverCombo(),
                    this.loginForm.username(),
                    this.loginForm.password(),
                    this.loginForm.loginButton()));
    this.versionComboBox
        .valueProperty()
        .addListener(
            (obs, old, value) -> {
              this.launchVersionLabel.setText(
                  value == null
                      ? ""
                      : InstanceMeta.name(new File(this.config.getGameDir()), value.getId()));
              this.updateInstanceInfo(value == null ? null : value.getId());
              // 概览里的「当前实例」跟着下拉走，否则版本列表异步加载完会一直显示旧值
              if (this.launchOverviewPane != null) {
                this.launchOverviewPane.refresh();
              }
              if (value != null && !this.suppressInstancePersist) {
                this.userPickedInstance = true;
                this.config.setLastInstance(value.getId());
                this.config.save();
              }
            });
    this.launchBtn.setOnAction(e -> this.handleLaunch());
    Motion.stagger(List.of(userArea, logContainer));
    return launchPanel;
  }

  /** 命令栏提交：AI 开启时发给模型，否则当启动器 / 游戏命令执行。 */
  private void handleCommandInput(String text) {
    if (this.config.isAiEnabled()) {
      this.sendAiMessage(text);
    } else {
      this.executeCommand(text);
    }
  }

  /** 命令栏里点了 AI 开关：同步界面并写日志。 */
  private void onAiModeToggled() {
    this.applyAiMode();
    this.logArea.appendText(
        this.config.isAiEnabled()
            ? "🧠 DeepSeek AI 已开启：命令栏输入将发给模型\n"
            : "⌘ DeepSeek AI 已关闭：命令栏恢复为启动器 / 游戏命令\n");
  }

  /** 皮肤 / 披风预览：打开对话框并把预览贴图同步到账户存储。 */
  private void openSkinPreview() {
    SkinPreviewDialog.show(
        this.stageRef,
        this.config,
        this.currentSession == null ? null : this.currentSession.getSkinUrl());
    if (this.currentSession != null && this.currentSession.getUsername() != null) {
      AccountStore.setTexture(
          this.currentSession.getUsername(),
          this.config.getPreviewSkinPath(),
          this.config.getPreviewCapePath());
    }
  }

  /** 登录方式切换后按需要调整窗口高度（第三方外置登录面板更高）。 */
  private void resizeForLoginType(String selected) {
    boolean is3rd = "第三方外置登录".equals(selected);
    if (this.stageRef == null || this.stageRef.isMaximized()) {
      return;
    }
    double lockedHeight = this.stageRef.getHeight();
    double lockedWidth = this.stageRef.getWidth();
    Platform.runLater(
        () -> {
          if (this.stageRef != null && !this.stageRef.isMaximized()) {
            double maxHeight = Screen.getPrimary().getVisualBounds().getHeight() - 40.0;
            double target =
                is3rd
                    ? Math.min(maxHeight, Math.max(lockedHeight, 830.0))
                    : Math.min(maxHeight, Math.max(lockedHeight, 792.0));
            if (Math.abs(this.stageRef.getHeight() - target) > 1.0) {
              this.stageRef.setHeight(target);
            }
            if (Math.abs(this.stageRef.getWidth() - lockedWidth) > 1.0) {
              this.stageRef.setWidth(lockedWidth);
            }
          }
        });
  }

  private void applyAiMode() {
    if (this.commandBar == null) {
      return;
    }
    boolean on = this.config.isAiEnabled();
    this.commandBar.setAiMode(on);
    if (this.aiChatArea != null) {
      this.aiChatArea.setPrefHeight(on ? 320.0 : 120.0);
    }
    this.refreshLaunchBody();
  }

  private void appendAiChat(String text) {
    if (this.aiChatArea == null) {
      return;
    }
    this.aiChatArea.setVisible(true);
    this.aiChatArea.setManaged(true);
    String existing = this.aiChatArea.getText();
    String next = (existing == null || existing.isBlank() ? "" : existing + "\n\n") + text;
    if (next.length() > 24000) {
      next = next.substring(next.length() - 24000);
    }
    this.aiChatArea.setText(next);
    this.aiChatArea.setScrollTop(Double.MAX_VALUE);
  }

  private void sendAiMessage(String text) {
    if (this.aiBusy) {
      this.logArea.appendText("🧠 上一条还在回答，请稍候。\n");
      return;
    }
    String modQuery = this.aiModSearchQuery(text);
    if (modQuery != null) {
      this.logArea.appendText("🧠 你：" + text + "\n");
      this.appendAiChat("你：" + text + "\n\n猫娘助手：好，我先去 Modrinth 找「" + modQuery + "」，找到后你确认一下就装到当前实例。");
      this.aiOfferModInstall(modQuery, text);
      return;
    }
    if (this.config.getAiApiKey().isBlank()) {
      this.logArea.appendText("🧠 你：" + text + "\n");
      this.logArea.appendText("🔌 未检测到 DeepSeek Key，本次用本地助手回答（设置 → AI 填 Key 可切到在线模型）\n");
      this.aiHistory.add(new DeepSeekClient.Message("user", text));
      this.trimAiHistory();
      String reply2 = LocalAiAssistant.answer(text, new File(this.config.getGameDir()));
      this.aiHistory.add(new DeepSeekClient.Message("assistant", reply2));
      this.trimAiHistory();
      this.appendAiChat("你：" + text + "\n\n猫娘助手：" + reply2);
      return;
    }
    this.aiBusy = true;
    this.logArea.appendText("🧠 你：" + text + "\n");
    this.logArea.appendText("🤖 DeepSeek 正在思考…\n");
    this.aiHistory.add(new DeepSeekClient.Message("user", text));
    this.trimAiHistory();
    ArrayList<DeepSeekClient.Message> messages = new ArrayList<DeepSeekClient.Message>();
    messages.add(new DeepSeekClient.Message("system", this.aiSystemPrompt()));
    messages.addAll(this.aiHistory);
    String key = this.config.getAiApiKey();
    String base = this.config.getAiBaseUrl();
    String model = this.config.getAiModel();

    CompletableFuture.supplyAsync(
            () -> {
              try {
                return DeepSeekClient.chat(key, base, model, messages);
              } catch (Exception ex) {
                throw new CompletionException(ex);
              }
            })
        .thenAccept(
            reply ->
                Platform.runLater(
                    () -> {
                      this.aiBusy = false;
                      this.aiHistory.add(new DeepSeekClient.Message("assistant", reply));
                      this.trimAiHistory();
                      this.appendAiChat("你：" + text + "\n\nDeepSeek：" + reply);
                    }))
        .exceptionally(
            ex -> {
              Platform.runLater(
                  () -> {
                    this.aiBusy = false;
                    Throwable cause = ex;
                    while (cause.getCause() != null) {
                      cause = cause.getCause();
                    }
                    this.appendAiChat("猫娘助手：请求失败 —— " + cause.getMessage());
                  });
              return null;
            });
  }

  /** AI 识别到「装个 XX 模组」：搜索 Modrinth，弹确认框，确认后直接下载安装到当前实例。 */
  private void aiOfferModInstall(String query, String rawText) {
    String mc = this.extractMcVersion(this.currentInstanceId());
    JsonArray facets = new JsonArray();
    JsonArray typeArr = new JsonArray();
    typeArr.add("project_type:mod");
    facets.add(typeArr);
    if (mc != null && !mc.isBlank()) {
      JsonArray verArr = new JsonArray();
      verArr.add("versions:" + mc);
      facets.add(verArr);
    }
    String base;
    try {
      base =
          "https://api.modrinth.com/v2/search?query="
              + URLEncoder.encode(query, StandardCharsets.UTF_8.toString())
              + "&facets="
              + URLEncoder.encode(facets.toString(), StandardCharsets.UTF_8.toString())
              + "&index=relevance&limit=6";
    } catch (Exception e) {
      this.aiFallbackToModsPage(query);
      return;
    }
    String mirror =
        base.replace("https://api.modrinth.com/v2", "https://mod.mcimirror.top/modrinth/v2");
    this.logLine("🧩 正在搜索模组：" + query + "（当前实例 " + (mc == null ? "未知版本" : mc) + "）");
    this.alternateSearchBodies(base, mirror, 0)
        .orTimeout(10L, TimeUnit.SECONDS)
        .thenAccept(body -> Platform.runLater(() -> this.aiShowInstallChoice(query, body)))
        .exceptionally(
            ex -> {
              Platform.runLater(
                  () -> {
                    this.logLine("⚠ 模组搜索失败：" + ex.getMessage());
                    this.aiFallbackToModsPage(query);
                  });
              return null;
            });
  }

  /** 搜索结果确认框：选一个就直接走下载安装流程。 */
  private void aiShowInstallChoice(String query, String body) {
    if (body == null || !body.startsWith("{")) {
      this.aiFallbackToModsPage(query);
      return;
    }
    List<String> ids = new ArrayList<>();
    List<String> titles = new ArrayList<>();
    List<String> labels = new ArrayList<>();
    try {
      JsonObject json = JsonParser.parseString(body).getAsJsonObject();
      JsonArray hits = json.has("hits") ? json.getAsJsonArray("hits") : null;
      if (hits != null) {
        for (int i = 0; i < hits.size(); i++) {
          JsonObject hit = hits.get(i).getAsJsonObject();
          String id =
              hit.has("project_id")
                  ? hit.get("project_id").getAsString()
                  : (hit.has("slug") ? hit.get("slug").getAsString() : "");
          String title = hit.has("title") ? hit.get("title").getAsString() : "";
          if (id.isBlank() || title.isBlank()) continue;
          long downloads = hit.has("downloads") ? hit.get("downloads").getAsLong() : 0L;
          ids.add(id);
          titles.add(title);
          labels.add(title + "   ·   下载 " + String.format(Locale.ROOT, "%,d", downloads));
        }
      }
    } catch (Exception ignored) {
      // 解析失败当作没搜到
    }
    if (ids.isEmpty()) {
      this.aiFallbackToModsPage(query);
      return;
    }
    List<String> idsFinal = ids;
    List<String> titlesFinal = titles;
    InAppDialog.choose(
        this.dialogLayer,
        "安装模组",
        "找到这些和「" + query + "」相关的模组：",
        labels,
        pick -> {
          int index = labels.indexOf(pick);
          if (index < 0) {
            this.aiFallbackToModsPage(query);
            return;
          }
          this.logLine("📥 开始安装：" + titlesFinal.get(index));
          this.fetchVersionsAndDownload(idsFinal.get(index), titlesFinal.get(index), "mod");
        });
  }

  /** 搜索不可用时退回原来的做法：打开模组页并带上搜索词。 */
  private void aiFallbackToModsPage(String query) {
    this.logLine("🧩 打开模组页搜索：" + query);
    this.openInstanceSettings("mods");
    if (this.instanceSettingsPanel != null) {
      this.instanceSettingsPanel.openModsSearch(query);
    }
  }

  /** 把「帮我装个 xxx 模组」这类话解析成搜索词；疑问句不处理，交给模型。 */
  private String aiModSearchQuery(String text) {
    if (text == null) return null;
    String t = text.trim();
    if (t.isEmpty()) return null;
    String lower = t.toLowerCase(java.util.Locale.ROOT);
    if (t.contains("怎么")
        || t.contains("如何")
        || t.contains("为什么")
        || t.contains("什么")
        || t.contains("吗")
        || t.contains("？")
        || t.contains("?")) {
      return null;
    }
    boolean wants =
        t.contains("装") || t.contains("安装") || t.contains("下载") || lower.contains("install");
    boolean isMod = t.contains("模组") || t.contains("插件") || lower.contains("mod");
    if (!wants || !isMod) return null;
    String q = t;
    for (String noise :
        new String[] {
          "帮我", "帮忙", "请", "给我", "我想", "我要", "能不能", "可以", "安装一下", "安装", "装一下", "装个", "装一个", "装",
          "下载", "一下", "一个", "这个", "那个", "模组", "插件", "mod", "Mod", "MOD", "吧", "呗", "：", ":", "、",
          "。", "，", "！"
        }) {
      q = q.replace(noise, " ");
    }
    q = q.replaceAll("\\s+", " ").trim();
    if (q.isBlank() || q.length() > 40) return null;
    return q;
  }

  private String aiSystemPrompt() {
    int versions = 0;
    try {
      versions = VersionProfile.installedIds(new File(this.config.getGameDir())).size();
    } catch (Exception exception) {
      // empty catch block
    }
    return "你是「Neko Craft Launcher」内置的 AI 助手，用中文简洁回答，优先给出可操作步骤。当前游戏目录："
        + this.config.getGameDir()
        + "；已安装实例数："
        + versions
        + "；当前实例："
        + this.currentInstanceId()
        + "。可以回答 Minecraft 模组、报错、启动器使用等问题；不确定时明确说明。";
  }

  private void trimAiHistory() {
    while (this.aiHistory.size() > 10) {
      this.aiHistory.remove(0);
    }
  }

  private void showCommandPalette() {
    if (this.commandPaletteOverlay != null
        || this.stageRef == null
        || this.stageRef.getScene() == null) {
      return;
    }
    Parent parent = this.stageRef.getScene().getRoot();
    if (!(parent instanceof StackPane)) {
      return;
    }
    StackPane layer = (StackPane) parent;
    StackPane overlay = new StackPane();
    overlay.setStyle("-fx-background-color: rgba(4,7,12,0.62);");
    StackPane.setAlignment(overlay, Pos.CENTER);
    VBox card = new VBox(10.0);
    card.setPrefWidth(560.0);
    card.setMaxWidth(560.0);
    card.setMaxHeight(Double.NEGATIVE_INFINITY);
    card.setPadding(new Insets(14.0));
    card.setStyle(
        "-fx-background-color: linear-gradient(to bottom, -app-card-top, -app-card-alt2);"
            + " -fx-background-radius: 18; -fx-border-color: -app-border-soft; -fx-border-radius:"
            + " 18; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.45), 40, 0.2, 0, 12);");
    TextField search = new TextField();
    search.setPromptText("搜索命令、页面、操作…");
    search.setPrefHeight(40.0);
    search.setStyle(
        "-fx-background-color: -app-input; -fx-text-fill: -app-text; -fx-prompt-text-fill:"
            + " -app-text-muted; -fx-background-radius: 12; -fx-border-color: -app-border-3;"
            + " -fx-border-radius: 12; -fx-padding: 8 12; -fx-font-size: 13px;");
    VBox list = new VBox(4.0);
    Label hint = new Label("↑↓ 选择 · Enter 执行 · Esc 关闭");
    hint.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 10.5px;");
    ArrayList<PaletteAction> all = new ArrayList<PaletteAction>();
    for (PluginContext.Command command : PluginManager.commands()) {
      all.add(new PaletteAction("sparkles", command.title(), command.hint(), command.action()));
    }
    all.add(new PaletteAction("play", "启动游戏", "立即启动当前实例", this::handleLaunch));
    all.add(new PaletteAction("layers", "实例选择", "切换、收藏或导入实例", () -> this.switchPage("instances")));
    all.add(
        new PaletteAction(
            "gear", "实例设置", "打开当前实例的概览", () -> this.openInstanceSettings("overview")));
    all.add(
        new PaletteAction("download", "下载中心", "模组 / 整合包 / 资源", () -> this.switchPage("download")));
    all.add(
        new PaletteAction(
            "download",
            "版本列表",
            "Minecraft 版本下载与安装",
            () -> {
              this.switchPage("download");
              this.showDownloadPanel(this.minecraftPanel);
              this.loadMinecraftVersions();
            }));
    all.add(new PaletteAction("gear", "启动器设置", "Java、内存、外观", () -> this.switchPage("settings")));
    all.add(new PaletteAction("tools", "工具", "环境、缓存、诊断", () -> this.switchPage("tools")));
    all.add(
        new PaletteAction(
            "tools",
            "启动记录",
            "启动耗时、异常率、会话历史",
            () -> {
              this.switchPage("tools");
              if (this.toolsPanel != null) {
                this.toolsPanel.openHistoryTab();
              }
            }));
    all.add(
        new PaletteAction(
            "sparkles",
            "性能优化",
            "打开可逆优化工具页",
            () -> {
              this.switchPage("tools");
              if (this.toolsPanel != null) {
                this.toolsPanel.openPerformanceTab();
              }
            }));
    all.add(
        new PaletteAction(
            "folder",
            "打开游戏目录",
            this.config.getGameDir(),
            () -> this.openDir(new File(this.config.getGameDir()))));
    all.add(
        new PaletteAction(
            "terminal",
            "打开日志目录",
            "查看 latest.log / launcher.log",
            () -> this.openDir(new File(this.config.getGameDir(), "logs"))));
    all.add(
        new PaletteAction(
            "sparkles",
            "切换 AI 模式",
            "命令栏切换 DeepSeek / 本地助手",
            () -> {
              this.config.setAiEnabled(!this.config.isAiEnabled());
              this.config.save();
              this.applyAiMode();
              Toast.info(this.config.isAiEnabled() ? "AI 模式已开启" : "AI 模式已关闭");
            }));
    all.add(
        new PaletteAction(
            "warning",
            "崩溃诊断",
            "分析最近一次崩溃",
            () -> {
              CrashAnalyzer.Report report =
                  CrashAnalyzer.analyze(new File(this.config.getGameDir()));
              this.logArea.appendText(CrashAnalyzer.toText(report));
              CrashReportDialog.show(this.stageRef, report);
            }));
    int[] selected = new int[] {0};
    ArrayList<HBox> rows = new ArrayList<>();
    Runnable paintSelection =
        () -> {
          for (int i = 0; i < rows.size(); ++i) {
            boolean active = i == selected[0];
            rows.get(i)
                .setStyle(
                    "-fx-background-color: "
                        + (active ? "-app-hover-2" : "transparent")
                        + "; -fx-background-radius: 9;");
          }
        };
    Runnable runSelected =
        () -> {
          if (rows.isEmpty()) {
            return;
          }
          int index = Math.max(0, Math.min(selected[0], rows.size() - 1));
          PaletteAction action = (PaletteAction) rows.get(index).getUserData();
          this.closeCommandPalette();
          if (action != null && action.action() != null) {
            action.action().run();
          }
        };
    Runnable refresh =
        () -> {
          String query = search.getText() == null ? "" : search.getText().trim().toLowerCase();
          list.getChildren().clear();
          rows.clear();
          for (PaletteAction action : all) {
            String haystack = (action.title() + " " + action.hint()).toLowerCase();
            if (!query.isEmpty() && !haystack.contains(query)) continue;
            HBox row = new HBox(10.0);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(8.0, 10.0, 8.0, 10.0));
            row.setUserData(action);
            row.setCursor(Cursor.HAND);
            StackPane icon = Icons.node(action.icon(), 15.0, "#c7ccd4");
            Label title = new Label(action.title());
            title.setStyle(
                "-fx-text-fill: -app-text-2; -fx-font-size: 12.5px; -fx-font-weight: bold;");
            Label sub = new Label(action.hint());
            sub.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 10.5px;");
            Region sp = new Region();
            HBox.setHgrow(sp, Priority.ALWAYS);
            row.getChildren().addAll(icon, title, sub, sp);
            row.setOnMouseEntered(
                e -> {
                  selected[0] = rows.indexOf(row);
                  paintSelection.run();
                });
            row.setOnMouseClicked(
                e -> {
                  this.closeCommandPalette();
                  action.action().run();
                });
            rows.add(row);
            list.getChildren().add(row);
            if (rows.size() < 9) continue;
            break;
          }
          selected[0] = 0;
          paintSelection.run();
        };
    search.textProperty().addListener((o, a, b) -> refresh.run());
    search.setOnKeyPressed(
        e -> {
          if (e.getCode() == KeyCode.DOWN) {
            if (!rows.isEmpty()) {
              selected[0] = (selected[0] + 1) % rows.size();
            }
            paintSelection.run();
            e.consume();
          } else if (e.getCode() == KeyCode.UP) {
            if (!rows.isEmpty()) {
              selected[0] = (selected[0] - 1 + rows.size()) % rows.size();
            }
            paintSelection.run();
            e.consume();
          } else if (e.getCode() == KeyCode.ENTER) {
            runSelected.run();
            e.consume();
          } else if (e.getCode() == KeyCode.ESCAPE) {
            this.closeCommandPalette();
            e.consume();
          }
        });
    card.getChildren().addAll(search, list, hint);
    overlay.getChildren().add(card);
    card.setOnMouseClicked(e -> e.consume());
    overlay.setOnMouseClicked(
        e -> {
          if (e.getTarget() == overlay) {
            this.closeCommandPalette();
          }
        });
    layer.getChildren().add(overlay);
    this.commandPaletteOverlay = overlay;
    refresh.run();
    overlay.setOpacity(0.0);
    card.setScaleX(0.97);
    card.setScaleY(0.97);
    FadeTransition fade = new FadeTransition(Duration.millis(160.0), overlay);
    fade.setToValue(1.0);
    ScaleTransition scale = new ScaleTransition(Duration.millis(180.0), card);
    scale.setToX(1.0);
    scale.setToY(1.0);
    scale.setInterpolator(Motion.SPRING);
    fade.play();
    scale.play();
    Platform.runLater(search::requestFocus);
  }

  private void closeCommandPalette() {
    StackPane overlay = this.commandPaletteOverlay;
    this.commandPaletteOverlay = null;
    if (overlay == null) {
      return;
    }
    FadeTransition fade = new FadeTransition(Duration.millis(120.0), overlay);
    fade.setToValue(0.0);
    fade.setOnFinished(
        e -> {
          Parent patt0$temp = overlay.getParent();
          if (patt0$temp instanceof StackPane) {
            StackPane parent = (StackPane) patt0$temp;
            parent.getChildren().remove(overlay);
          }
        });
    fade.play();
  }

  private void executeCommand(String raw) {
    String text = raw.trim();
    if (text.isEmpty()) {
      return;
    }
    if (this.commandBar != null) {
      this.commandBar.pushHistory(text);
    }
    this.logArea.appendText("⌘ " + text + "\n");
    if (text.startsWith("/")) {
      this.sendCommandToGame(text.substring(1).trim());
      return;
    }
    String[] parts = text.split("\\s+", 2);
    String command = parts[0].toLowerCase(Locale.ROOT);
    String arg = parts.length > 1 ? parts[1].trim() : "";
    switch (command) {
      case "help":
      case "?":
        {
          this.logArea.appendText(
              "⌘ 启动器命令：\n"
                  + "  help                显示本帮助\n"
                  + "  launch              启动当前实例\n"
                  + "  kill                结束当前游戏进程\n"
                  + "  versions            列出已安装版本\n"
                  + "  java                显示 Java 路径\n"
                  + "  memory <MB>         修改最大内存（下次启动生效）\n"
                  + "  open [子目录]        打开游戏目录 / 指定子目录\n"
                  + "  crash               诊断最近一次崩溃\n"
                  + "  import <文件>        导入 .zip / .mrpack 整合包\n"
                  + "  clear               清空日志\n"
                  + "  /<命令>             发送到游戏聊天（需游戏窗口，单人需开作弊）\n");
          break;
        }
      case "launch":
        {
          if (this.launchBtn == null) break;
          this.launchBtn.fire();
          break;
        }
      case "kill":
        {
          this.killGameProcess();
          break;
        }
      case "versions":
        {
          File dir = new File(this.config.getGameDir(), "versions");
          File[] dirs = dir.listFiles(File::isDirectory);
          if (dirs == null || dirs.length == 0) {
            this.logArea.appendText("没有已安装的版本\n");
            break;
          }
          Arrays.sort(dirs, Comparator.comparing(File::getName));
          StringBuilder list = new StringBuilder("已安装 ").append(dirs.length).append(" 个版本：");
          for (File d : dirs) {
            list.append("\n  · ").append(d.getName());
          }
          this.logArea.appendText(list.append("\n").toString());
          break;
        }
      case "java":
        {
          this.logArea.appendText(
              "Java 路径："
                  + this.config.getJavaPath()
                  + "\n启动器 Java："
                  + System.getProperty("java.version")
                  + "（"
                  + System.getProperty("java.vendor")
                  + "）\n");
          break;
        }
      case "memory":
        {
          try {
            int mb = Integer.parseInt(arg);
            if (mb < 512 || mb > 32768) {
              throw new NumberFormatException();
            }
            this.config.setMemoryMb(mb);
            this.config.save();
            this.logArea.appendText("✅ 最大内存已设为 " + mb + " MB（下次启动生效）\n");
          } catch (NumberFormatException ex) {
            this.logArea.appendText("用法：memory 4096（512–32768 MB）\n");
          }
          break;
        }
      case "import":
        {
          File file = new File(arg);
          if (!file.isFile()) {
            this.logArea.appendText("找不到整合包：" + arg + "\n");
            break;
          }
          this.importModpack(file);
          break;
        }
      case "crash":
        {
          CrashAnalyzer.Report report = CrashAnalyzer.analyze(new File(this.config.getGameDir()));
          this.logArea.appendText(CrashAnalyzer.toText(report));
          CrashReportDialog.show(this.stageRef, report);
          break;
        }
      case "open":
        {
          File target =
              arg.isEmpty()
                  ? new File(this.config.getGameDir())
                  : new File(this.config.getGameDir(), arg);
          try {
            if (!target.exists() && !target.mkdirs()) {
              this.logArea.appendText("目录不存在：" + target.getAbsolutePath() + "\n");
              return;
            }
            Desktop.getDesktop().open(target);
            this.logArea.appendText("📂 已打开 " + target.getAbsolutePath() + "\n");
          } catch (Exception ex) {
            this.logArea.appendText("打开失败：" + ex.getMessage() + "\n");
          }
          break;
        }
      case "clear":
        {
          this.logArea.clear();
          break;
        }
      default:
        {
          this.logArea.appendText("未知命令：" + command + "（输入 help 查看）\n");
        }
    }
  }

  private void sendCommandToGame(String command) {
    if (command == null || command.isBlank()) {
      this.logArea.appendText("用法：/give @s diamond 64\n");
      return;
    }
    if (this.gameProcess == null || !this.gameProcess.isAlive()) {
      this.logArea.appendText("⚠ 没有正在运行的游戏进程，先启动游戏\n");
      return;
    }
    this.logArea.appendText("📨 发送到游戏：/" + command + "\n");
    CompletableFuture.runAsync(
        () -> {
          boolean ok = GameCommandSender.send(this.gameProcess, command);
          Platform.runLater(
              () ->
                  this.logArea.appendText(
                      ok ? "✅ 已发送（单人需开启作弊，服务器需要 OP）\n" : "⚠ 发送失败：没找到游戏窗口或窗口未响应\n"));
        });
  }

  private void killGameProcess() {
    if (this.gameProcess == null || !this.gameProcess.isAlive()) {
      this.logArea.appendText("没有正在运行的游戏进程\n");
      return;
    }
    long pid = this.gameProcess.pid();
    this.launchState.requestStop();
    this.gameProcess.descendants().forEach(ProcessHandle::destroyForcibly);
    this.gameProcess.destroyForcibly();
    this.setGameStatus(false);
    this.logArea.appendText("🛑 已结束游戏进程 (pid " + pid + ")\n");
  }

  private HBox createDownloadPanel() {
    this.downloadPage =
        new DownloadPage(
            new DownloadPage.Host() {
              @Override
              public Node communityPanel() {
                return HelloApplication.this.communityPanelNode();
              }

              @Override
              public Node minecraftPanel() {
                return HelloApplication.this.minecraftPanelNode();
              }

              @Override
              public Node worldsNoticePanel() {
                return HelloApplication.this.worldsNoticePanelNode();
              }

              @Override
              public Node favoritesPanel() {
                return HelloApplication.this.favoritesPanelNode();
              }

              @Override
              public void onSelect(String text) {
                HelloApplication.this.onDownloadSelect(text);
              }
            });
    return this.downloadPage.build();
  }

  private StackPane downloadContentArea() {
    return this.downloadPage == null ? null : this.downloadPage.contentArea();
  }

  private Node communityPanelNode() {
    if (this.communityPanel == null) {
      this.communityPanel = this.createCommunityPanel();
    }
    return this.communityPanel;
  }

  private Node minecraftPanelNode() {
    if (this.minecraftPanel == null) {
      this.minecraftPanel = this.createMinecraftPanel();
    }
    return this.minecraftPanel;
  }

  private Node worldsNoticePanelNode() {
    if (this.worldsNoticePanel == null) {
      this.worldsNoticePanel = this.createWorldsNoticePanel();
    }
    return this.worldsNoticePanel;
  }

  private Node favoritesPanelNode() {
    if (this.favoritesPanel == null) {
      this.favoritesPanel = this.createFavoritesPanel();
    }
    return this.favoritesPanel;
  }

  /** 下载页左侧分类被选中后的业务分发（导航与指示器由 DownloadPage 负责）。 */
  private void onDownloadSelect(String text) {
    boolean installer =
        text.equals("Fabric")
            || text.equals("Quilt")
            || text.equals("Forge")
            || text.equals("NeoForge")
            || text.equals("OptiFine");
    if (installer) {
      if (text.equals("OptiFine")) {
        this.openBrowser("https://optifine.net/downloads");
        Toast.info("OptiFine 官网安装器需要手动运行");
        return;
      }
      if (this.installerPanel == null) {
        this.installerPanel = this.createInstallerPanel();
        this.downloadContentArea().getChildren().add(this.installerPanel);
      }
      this.showDownloadPanel(this.installerPanel);
      this.showInstaller(text);
      return;
    }
    if (text.equals("Minecraft")) {
      this.showDownloadPanel(this.minecraftPanelNode());
      this.loadMinecraftVersions();
    } else if (text.equals("世界")) {
      this.showDownloadPanel(this.worldsNoticePanelNode());
    } else if (text.equals("收藏夹")) {
      this.refreshFavoritesPanel();
      this.showDownloadPanel(this.favoritesPanelNode());
    } else {
      this.currentProjectType = text;
      this.showDownloadPanel(this.communityPanelNode());
      this.filterBar.setSearchPrompt("搜索 " + text + "...");
      this.filterBar.setSuppressSearch(true);
      try {
        this.filterBar.sourceCombo().setValue("全部");
        this.filterBar.tagCombo().setValue("全部");
        this.filterBar.sortCombo().setValue("默认");
        this.filterBar.loaderCombo().setValue(text.equals("数据包") ? "DataPack" : "任意");
      } finally {
        this.filterBar.setSuppressSearch(false);
      }
      this.performSearch();
    }
  }

  private void showDownloadPanel(Node target) {
    if (target != null && this.downloadPage != null) {
      this.downloadPage.show(target);
    }
  }

  private void configureVersionLoaderFilters() {
    if (this.filterBar.gameVersionCombo() == null || this.filterBar.loaderCombo() == null) {
      return;
    }
    LinkedHashSet<String> versions =
        new LinkedHashSet<String>(this.filterBar.gameVersionCombo().getItems());
    for (String id : VersionProfile.installedIds(new File(this.config.getGameDir()))) {
      String mc = this.extractMcVersion(id);
      if (mc == null || mc.isBlank()) continue;
      versions.add(mc);
    }
    this.filterBar.gameVersionCombo().getItems().setAll((Collection<String>) versions);
    this.filterBar.gameVersionCombo().setValue("任意");
    this.filterBar.loaderCombo().setValue("任意");
  }

  private String extractMcVersion(String id) {
    return InstanceId.mcVersion(id);
  }

  private VBox createCommunityPanel() {
    VBox panel = new VBox(10.0);
    panel.setAlignment(Pos.TOP_LEFT);
    VBox.setVgrow(panel, Priority.ALWAYS);
    this.filterBar =
        new DownloadFilterBar(
            this.config, this::performSearch, this::showDownloadTasks, this::logLine);
    // 面板还没建完（状态浮层等在后面），先屏蔽下拉变化触发的搜索
    this.filterBar.setSuppressSearch(true);
    this.configureVersionLoaderFilters();
    this.resultView =
        new DownloadResultView(
            new ModpackCell.Host() {
              @Override
              public java.util.Map<String, javafx.scene.image.Image> iconCache() {
                return iconMemoryCache;
              }

              @Override
              public javafx.scene.image.Image decodeIcon(byte[] bytes) {
                return HelloApplication.this.decodeIcon(bytes);
              }

              @Override
              public java.net.http.HttpClient httpClient() {
                return httpClient;
              }

              @Override
              public void log(String text) {
                HelloApplication.this.logLine(text);
              }

              @Override
              public void download(ModpackProject project) {
                HelloApplication.this.fetchVersionsAndDownload(
                    project.getId(), project.getTitle(), project.getProjectType());
              }

              @Override
              public void openDetail(ModpackProject project) {
                HelloApplication.this.openCommunityModDetail(project);
              }
            },
            this.modpackData,
            () -> this.loadSearchPage(false));
    panel.getChildren().addAll(this.filterBar, this.resultView);
    this.filterBar.setSuppressSearch(false);
    return panel;
  }

  private VBox createMinecraftPanel() {
    if (this.minecraftVersionsPage == null) {
      this.minecraftVersionsPage =
          new MinecraftVersionsPage(
              this.allMinecraftVersions,
              this.config.getGameDir(),
              version -> HelloApplication.this.openVersionDetail(version));
    }
    return this.minecraftVersionsPage.build();
  }

  private void setInstallerStatus(String text) {
    if (this.installerPage != null) {
      this.installerPage.setStatus(text);
    }
  }

  private void setInstallerProgress(boolean running) {
    if (this.installerPage != null) {
      this.installerPage.setBusy(running);
    }
  }

  private void startInstall(
      MinecraftVersion version, String loader, String loaderVersion, ButtonBase trigger) {
    trigger.setDisable(true);
    trigger.setText("安装中...");
    this.setInstallerProgress(true);
    this.logMessage(
        "📥 开始安装 Minecraft "
            + version.getId()
            + "（"
            + loader
            + (loaderVersion == null ? " · 最新加载器" : " · " + loaderVersion)
            + "）");
    this.executeInstallProcess(version, loader, loaderVersion, trigger);
  }

  private VBox createInstallerPanel() {
    this.installerPage =
        new InstallerPage(
            new InstallerPage.Host() {
              @Override
              public void backToList() {
                HelloApplication.this.showDownloadPanel(HelloApplication.this.minecraftPanelNode());
              }

              @Override
              public void startInstall(Button trigger) {
                HelloApplication.this.startInstallerFromPanel(trigger);
              }
            });
    return this.installerPage.build();
  }

  private void startInstallerFromPanel(Button trigger) {
    if (this.installerPage == null) {
      return;
    }
    MinecraftVersion game = this.installerPage.selectedGame();
    if (game == null) {
      this.installerPage.setStatus("请先选择 Minecraft 版本。");
      return;
    }
    String loader = this.installerPage.expandedLoader() == null
        ? "原版" : this.installerPage.expandedLoader();
    if ("OptiFine".equals(loader)) {
      this.openBrowser("https://optifine.net/downloads");
      this.installerPage.setStatus("已打开 OptiFine 官网，下载后手动安装。");
      return;
    }
    if ("LabyMod".equals(loader)) {
      this.openBrowser("https://labymod.net/download");
      this.installerPage.setStatus("已打开 LabyMod 官网，下载后手动安装。");
      return;
    }
    String loaderVersion = this.installerPage.selectedLoaderVersion(loader);
    this.setInstallerProgress(true);
    this.installerPage.setStatus("正在安装 " + game.getId() + " · " + loader
        + (loaderVersion == null ? "" : " " + loaderVersion) + "…");
    this.startInstall(game, loader, loaderVersion, trigger);
  }

  private void openVersionDetail(MinecraftVersion version) {
    if (this.versionDetailPanel == null) {
      this.versionDetailPanel = new VBox(12.0);
      this.versionDetailPanel.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
      this.versionDetailPanel.setAlignment(Pos.TOP_LEFT);
      this.downloadContentArea().getChildren().add(this.versionDetailPanel);
    }
    if (this.versionDetailPage == null) {
      this.versionDetailPage =
          new VersionDetailPage(
              new VersionDetailPage.Host() {
                @Override
                public String gameDir() {
                  return HelloApplication.this.config.getGameDir();
                }

                @Override
                public void backToList() {
                  HelloApplication.this.showDownloadPanel(HelloApplication.this.minecraftPanelNode());
                }

                @Override
                public void openInstaller(MinecraftVersion version, String loader) {
                  if (loader == null) {
                    HelloApplication.this.openVersionInstaller(version);
                  } else {
                    HelloApplication.this.openVersionInstaller(version, loader);
                  }
                }

                @Override
                public void launchVersion(String versionId) {
                  HelloApplication.this.addInstalledToCombo(versionId);
                  HelloApplication.this.switchPage("launch");
                  Toast.ok("已切换到 " + versionId);
                }
              });
    }
    this.versionDetailPanel.getChildren().setAll(this.versionDetailPage.build(version));
    this.showDownloadPanel(this.versionDetailPanel);
  }

  private void openVersionInstaller(MinecraftVersion version) {
    this.openVersionInstaller(version, "Fabric");
  }

  private void openVersionInstaller(MinecraftVersion version, String loader) {
    if (this.installerPanel == null) {
      this.installerPanel = this.createInstallerPanel();
      this.downloadContentArea().getChildren().add(this.installerPanel);
    }
    this.showDownloadPanel(this.installerPanel);
    List<MinecraftVersion> releases =
        this.allMinecraftVersions.stream()
            .filter(v -> "release".equals(v.getType()))
            .collect(Collectors.toList());
    this.installerPage.setGameVersions(releases, version);
    this.installerPage.setInstanceName(version.getId());
    this.installerPage.showFor(loader == null || loader.isBlank() ? "Fabric" : loader);
    this.installerPage.updateTitle();
  }

  private VBox createWorldsNoticePanel() {
    return new WorldsNoticePanel(
        () -> this.openBrowser("https://www.curseforge.com/minecraft/worlds"),
        () -> this.openDir(new File(this.config.getGameDir(), "saves")));
  }

  private VBox createFavoritesPanel() {
    VBox panel = new VBox(12.0);
    panel.setPadding(new Insets(4.0));
    panel.setMaxWidth(760.0);
    Label title = new Label("收藏夹");
    title.setStyle("-fx-text-fill: -app-text; -fx-font-size: 20px; -fx-font-weight: bold;");
    Label hint = new Label("在「实例选择」里点实例右侧的 ☆ 收藏，这里会列出来。");
    hint.setStyle("-fx-text-fill: -app-text-dim-2; -fx-font-size: 12px;");
    this.favoritesSearch = new TextField();
    this.favoritesSearch.setPromptText("搜索收藏的实例或资源");
    this.favoritesSearch.setPrefWidth(320.0);
    this.favoritesSearch
        .textProperty()
        .addListener((obs, old, value) -> this.refreshFavoritesPanel());
    this.favoritesSortCombo =
        new ComboBox<String>(FXCollections.observableArrayList("最近收藏", "名称", "下载量"));
    this.favoritesSortCombo.setValue("最近收藏");
    this.favoritesSortCombo.setPrefWidth(120.0);
    this.favoritesSortCombo
        .valueProperty()
        .addListener((obs, old, value) -> this.refreshFavoritesPanel());
    HBox favoritesTools = new HBox(10.0, this.favoritesSearch, this.favoritesSortCombo);
    favoritesTools.setAlignment(Pos.CENTER_LEFT);
    this.favoritesList = new VBox(8.0);
    this.favoritesResourceList = new VBox(8.0);
    Label instanceTitle = new Label("收藏的实例");
    instanceTitle.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 12px; -fx-font-weight: bold;");
    Label resourceTitle = new Label("收藏的资源");
    resourceTitle.setStyle(
        "-fx-text-fill: #9ca3af; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 8 0 0"
            + " 0;");
    panel
        .getChildren()
        .addAll(
            title,
            hint,
            favoritesTools,
            instanceTitle,
            this.favoritesList,
            resourceTitle,
            this.favoritesResourceList);
    return panel;
  }

  private void refreshFavoritesPanel() {
    if (this.favoritesList == null) {
      return;
    }
    this.favoritesList.getChildren().clear();
    File gameDir = new File(this.config.getGameDir());
    List<Instance> favorites =
        Instance.list(gameDir).stream()
            .filter(instance -> InstanceMeta.isFavorite(gameDir, instance.id))
            .toList();
    String string =
        this.favoritesQuery =
            this.favoritesSearch == null || this.favoritesSearch.getText() == null
                ? ""
                : this.favoritesSearch.getText().trim().toLowerCase();
    if (favorites.isEmpty() && Favorites.list().isEmpty()) {
      this.favoritesList.getChildren().add(this.favoritesEmptyGuide());
      if (this.favoritesResourceList != null) {
        this.favoritesResourceList.getChildren().clear();
      }
      return;
    }
    List<Instance> filtered =
        favorites.stream()
            .filter(
                instance ->
                    this.favoritesQuery.isEmpty()
                        || InstanceMeta.name(gameDir, instance.id)
                            .toLowerCase()
                            .contains(this.favoritesQuery)
                        || instance.id.toLowerCase().contains(this.favoritesQuery))
            .toList();
    if (filtered.isEmpty()) {
      Label empty = new Label(favorites.isEmpty() ? "还没有收藏的实例。" : "没有匹配的收藏实例。");
      empty.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 13px;");
      this.favoritesList.getChildren().add(empty);
      this.renderFavoriteResources();
      return;
    }
    LinkedHashMap<String, List<Instance>> instanceGroups = new LinkedHashMap<>();
    for (Instance instance2 : filtered) {
      instanceGroups.computeIfAbsent(instance2.loader, key -> new ArrayList<>()).add(instance2);
    }
    for (Map.Entry<String, List<Instance>> entry : instanceGroups.entrySet()) {
      String groupKey = "实例:" + entry.getKey();
      boolean open = this.config.getFavoriteGroups().getOrDefault(groupKey, true);
      Label groupHeader =
          new Label((open ? "▾ " : "▸ ") + entry.getKey() + "（" + entry.getValue().size() + "）");
      groupHeader.setStyle(
          "-fx-text-fill: -app-text-dim-2; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding:"
              + " 8 0 2 0; -fx-cursor: hand;");
      groupHeader.setOnMouseClicked(
          e -> {
            this.config.getFavoriteGroups().put(groupKey, !open);
            this.config.save();
            this.refreshFavoritesPanel();
          });
      VBox instanceBody = new VBox(8.0);
      instanceBody.setVisible(open);
      instanceBody.setManaged(open);
      this.favoritesList.getChildren().addAll(groupHeader, instanceBody);
      for (Instance instance3 : entry.getValue()) {
        HBox row = new HBox(12.0);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10.0, 14.0, 10.0, 14.0));
        row.getStyleClass().add("instance-row");
        row.setCursor(Cursor.HAND);
        Label icon = new Label(instance3.loader.substring(0, 1));
        icon.setStyle(
            "-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;"
                + " -fx-background-color: -app-text-dim-2; -fx-background-radius: 8; -fx-padding: 7"
                + " 10;");
        VBox info = new VBox(2.0);
        Label name = new Label(InstanceMeta.name(gameDir, instance3.id));
        name.setStyle("-fx-text-fill: -app-text; -fx-font-size: 13px;");
        Label sub = new Label(instance3.id + " · " + instance3.subtitle());
        sub.setStyle("-fx-text-fill: -app-text-dim-3; -fx-font-size: 11px;");
        info.getChildren().addAll(name, sub);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label action = new Label("切换到启动页 ›");
        action.setStyle("-fx-text-fill: #d1d5db; -fx-font-size: 12px;");
        row.getChildren().addAll(icon, info, spacer, action);
        row.setOnMouseClicked(
            e -> {
              this.selectInstance(instance3);
              this.switchPage("launch");
            });
        instanceBody.getChildren().add(row);
      }
    }
    this.renderFavoriteResources();
  }

  private VBox favoritesEmptyGuide() {
    VBox guide = new VBox(10.0);
    guide.setPadding(new Insets(16.0, 18.0, 16.0, 18.0));
    guide.setStyle(
        "-fx-background-color: -app-card-alt; -fx-background-radius: 16; -fx-border-color:"
            + " -app-border-soft; -fx-border-radius: 16;");
    StackPane icon =
        new StackPane(Icons.node("star", 30.0, Theme.isLight() ? "#b6c2d2" : "#6b7688"));
    icon.setPrefSize(44.0, 44.0);
    icon.setMinSize(44.0, 44.0);
    icon.setMaxSize(44.0, 44.0);
    Label title = new Label("这里还什么都没有");
    title.setStyle("-fx-text-fill: -app-text-2; -fx-font-size: 14px; -fx-font-weight: bold;");
    Label hint = new Label("在「下载」页点卡片右侧的 ☆ 收藏资源；在「实例选择」里点实例右侧的 ☆ 收藏实例。");
    hint.setStyle("-fx-text-fill: -app-text-dim-2; -fx-font-size: 12px;");
    hint.setWrapText(true);
    Button toDownload = new Button("去下载页找资源");
    toDownload.getStyleClass().addAll("btn-primary", "btn-sm");
    toDownload.setOnAction(e -> this.switchPage("download"));
    Button toInstances = new Button("去实例选择");
    toInstances.getStyleClass().addAll("btn-secondary", "btn-sm");
    toInstances.setOnAction(e -> this.switchPage("instances"));
    HBox actions = new HBox(10.0, toDownload, toInstances);
    guide.getChildren().addAll(icon, title, hint, actions);
    return guide;
  }

  private void renderFavoriteResources() {
    String sort;
    if (this.favoritesResourceList == null) {
      return;
    }
    this.favoritesResourceList.getChildren().clear();
    ArrayList<Favorites.Fav> favs =
        new ArrayList<Favorites.Fav>(
            Favorites.list().stream()
                .filter(
                    fav ->
                        this.favoritesQuery.isEmpty()
                            || fav.title() != null
                                && fav.title().toLowerCase().contains(this.favoritesQuery)
                            || fav.type() != null
                                && fav.type().toLowerCase().contains(this.favoritesQuery))
                .toList());
    switch (sort =
        this.favoritesSortCombo == null || this.favoritesSortCombo.getValue() == null
            ? "最近收藏"
            : this.favoritesSortCombo.getValue()) {
      case "名称":
        {
          favs.sort(
              Comparator.comparing(fav -> fav.title() == null ? "" : fav.title().toLowerCase()));
          break;
        }
      case "下载量":
        {
          favs.sort(Comparator.comparingLong(Favorites.Fav::downloads).reversed());
          break;
        }
      default:
        {
          favs.sort(Comparator.comparingLong(Favorites.Fav::time).reversed());
        }
    }
    if (favs.isEmpty()) {
      Label empty =
          new Label(Favorites.list().isEmpty() ? "还没有收藏的资源。在下载页的卡片上点 ☆ 即可收藏。" : "没有匹配的收藏资源。");
      empty.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 12px;");
      empty.setWrapText(true);
      this.favoritesResourceList.getChildren().add(empty);
      return;
    }
    LinkedHashMap<String, List<Favorites.Fav>> grouped =
        new LinkedHashMap<String, List<Favorites.Fav>>();
    for (String label : new String[] {"整合包", "模组", "资源包", "光影包", "数据包", "世界", "资源"}) {
      List<Favorites.Fav> group =
          favs.stream().filter(fav -> this.typeLabel(fav.type()).equals(label)).toList();
      if (group.isEmpty()) continue;
      grouped.put(label, group);
    }
    for (Map.Entry<String, List<Favorites.Fav>> entry : grouped.entrySet()) {
      String groupKey = entry.getKey();
      boolean open = this.config.getFavoriteGroups().getOrDefault(groupKey, true);
      Label groupLabel =
          new Label((open ? "▾ " : "▸ ") + groupKey + "（" + entry.getValue().size() + "）");
      groupLabel.setStyle(
          "-fx-text-fill: -app-text-dim-2; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding:"
              + " 8 0 2 0; -fx-cursor: hand;");
      groupLabel.setOnMouseClicked(
          e -> {
            this.config.getFavoriteGroups().put(groupKey, !open);
            this.config.save();
            this.renderFavoriteResources();
          });
      VBox groupBody = new VBox(8.0);
      groupBody.setVisible(open);
      groupBody.setManaged(open);
      this.favoritesResourceList.getChildren().addAll(groupLabel, groupBody);
      for (Favorites.Fav fav2 : entry.getValue()) {
        HBox row = new HBox(12.0);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10.0, 14.0, 10.0, 14.0));
        row.getStyleClass().add("instance-row");
        Label icon = new Label(this.typeLabel(fav2.type()).substring(0, 1));
        icon.setStyle(
            "-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;"
                + " -fx-background-color: -app-text-dim-2; -fx-background-radius: 8; -fx-padding: 7"
                + " 9;");
        VBox info = new VBox(2.0);
        Label name = new Label(fav2.title());
        name.setStyle("-fx-text-fill: -app-text; -fx-font-size: 13px;");
        Label sub =
            new Label(
                this.typeLabel(fav2.type())
                    + " · ⬇ "
                    + HelloApplication.formatCount(fav2.downloads())
                    + " · Modrinth");
        sub.setStyle("-fx-text-fill: -app-text-dim-3; -fx-font-size: 11px;");
        info.getChildren().addAll(name, sub);
        HBox.setHgrow(info, Priority.ALWAYS);
        Button download = new Button("下载");
        download.setStyle(
            "-fx-background-color: #9ca3af; -fx-text-fill: #111827; -fx-font-weight: bold;"
                + " -fx-background-radius: 8; -fx-padding: 6 16; -fx-cursor: hand;");
        download.setMinWidth(Double.NEGATIVE_INFINITY);
        download.setOnAction(
            e -> this.fetchVersionsAndDownload(fav2.id(), fav2.title(), fav2.type()));
        Button remove = new Button();
        remove.setGraphic(Icons.node("star", 16.0, "#eab308"));
        remove.setStyle(
            "-fx-background-color: transparent; -fx-text-fill: #eab308; -fx-font-size: 17px;"
                + " -fx-cursor: hand; -fx-padding: 2 6;");
        remove.setOnAction(
            e -> {
              Favorites.remove(fav2.id());
              this.renderFavoriteResources();
              Toast.info("已取消收藏 " + fav2.title());
            });
        row.getChildren().addAll(icon, info, remove, download);
        groupBody.getChildren().add(row);
      }
    }
  }

  private String typeLabel(String type) {
    return ProjectFormat.typeLabel(type);
  }

  private static String formatCount(long count) {
    return ProjectFormat.count(count);
  }

  private void showInstaller(String loader) {
    if (this.installerPage == null) {
      return;
    }
    if (!this.installerPage.hasGameVersions()) {
      List<MinecraftVersion> releases =
          this.allMinecraftVersions.stream()
              .filter(v -> "release".equals(v.getType()))
              .collect(Collectors.toList());
      this.installerPage.setGameVersions(releases, null);
      this.installerPage.selectFirstGameVersion();
    }
    this.installerPage.showFor(loader);
  }

  private void executeInstallProcess(
      MinecraftVersion version, String loader, String loaderVersion, ButtonBase btn) {
    File gameDir = new File(this.config.getGameDir());
    String id = version.getId();
    String jsonUrl = version.getUrl();
    CompletableFuture.runAsync(
        () -> {
          Consumer<String> log =
              msg -> Platform.runLater(() -> this.logArea.appendText(msg + "\n"));
          try {
            VersionProfile baseProfile = this.ensureInstalled(id, jsonUrl, log);
            String resultId = id;
            if (!loader.startsWith("原版")) {
              log.accept("🔧 正在安装 " + loader + "（版本 " + id + "）...");
              String javaPath =
                  this.resolveJavaPath(gameDir, baseProfile.getJavaMajorVersion(), log);
              resultId =
                  switch (loader) {
                    case "Fabric" ->
                        LoaderInstaller.installFabric(gameDir, id, loaderVersion, log)
                            .get(10L, TimeUnit.MINUTES);
                    case "Quilt" ->
                        LoaderInstaller.installQuilt(gameDir, id, loaderVersion, log)
                            .get(10L, TimeUnit.MINUTES);
                    case "Forge" ->
                        LoaderInstaller.installForge(gameDir, id, javaPath, loaderVersion, log)
                            .get(30L, TimeUnit.MINUTES);
                    case "NeoForge" ->
                        LoaderInstaller.installNeoForge(gameDir, id, javaPath, loaderVersion, log)
                            .get(30L, TimeUnit.MINUTES);
                    default -> id;
                  };
            }
            String installedId = resultId;
            Platform.runLater(
                () -> {
                  this.logArea.appendText("✅ 安装完成: " + installedId + "，已加入版本下拉框\n");
                  this.addInstalledToCombo(installedId);
                  btn.setDisable(false);
                  btn.setText("开始下载");
                  this.setInstallerProgress(false);
                  this.setInstallerStatus("安装完成：" + installedId);
                });
          } catch (Exception e) {
            Platform.runLater(
                () -> {
                  this.logArea.appendText("❌ 安装失败: " + e.getMessage() + "\n");
                  btn.setDisable(false);
                  btn.setText("开始下载");
                  this.setInstallerProgress(false);
                  this.setInstallerStatus("安装失败：" + e.getMessage());
                });
          }
        });
  }

  private String resolveJavaPath(File gameDir, int majorVersion, Consumer<String> log)
      throws Exception {
    if (this.config.getJavaPath() != null && !this.config.getJavaPath().isBlank()) {
      return this.config.getJavaPath();
    }
    return JavaManager.ensureJavaAsync(
            gameDir,
            majorVersion,
            msg -> Platform.runLater(() -> this.logArea.appendText(msg + "\n")))
        .get(30L, TimeUnit.MINUTES);
  }

  private void loadMinecraftVersions() {
    if (this.minecraftVersionsPage != null) {
      this.minecraftVersionsPage.refresh();
    }
  }

  private void ensureCommunityPrewarm() {
    if (this.communityPrewarmDone.isDone()) {
      return;
    }
    if (this.resultView.prewarm() != null) {
      this.resultView.prewarm().show();
    }
    if (this.prewarmStarted.isEmpty()) {
      this.prewarmCommunityResources();
    }
  }

  private synchronized void advanceCommunityPrewarm() {
    ++this.communityPrewarmDoneCount;
    int total = 10;
    int percent = Math.min(100, this.communityPrewarmDoneCount * 100 / total);
    Platform.runLater(
        () -> {
          if (this.resultView.prewarm() != null) {
            this.resultView.prewarm().setProgress(percent);
          }
          if (this.communityPrewarmDoneCount >= total && this.resultView.prewarm() != null) {
            this.resultView.prewarm().hide();
          }
        });
    if (this.communityPrewarmDoneCount >= total) {
      this.communityPrewarmDone.complete(null);
    }
  }

  private void prewarmCommunityResources() {
    this.communityPrewarmDone.completeOnTimeout(null, 45L, TimeUnit.SECONDS);
    this.communityPrewarmDone.thenRun(
        () ->
            Platform.runLater(
                () -> {
                  if (this.resultView.prewarm() != null) {
                    this.resultView.prewarm().hide();
                  }
                }));
    String[] preloadTypes = new String[] {"mod", "modpack", "datapack", "resourcepack", "shader"};
    AtomicInteger pending = new AtomicInteger(preloadTypes.length);
    for (String type : preloadTypes) {
      if (!this.prewarmStarted.add(type)) {
        this.advanceCommunityPrewarm();
        continue;
      }
      String facets = "[\"project_type:" + type + "\"]";
      String url =
          "https://api.modrinth.com/v2/search?query=&facets="
              + URLEncoder.encode("[" + facets + "]", StandardCharsets.UTF_8)
              + "&index=relevance&limit=40&offset=0";
      String mirrorUrl =
          url.replace("https://api.modrinth.com/v2", "https://mod.mcimirror.top/modrinth/v2");
      boolean prepareIcons = true;
      String sourceUrl = prepareIcons ? mirrorUrl : url;

      this.alternateSearchBodies(url, sourceUrl, type.equals("modpack") ? 40 : 0)
          .thenAccept(
              body -> {
                ArrayList<ModpackProject> projects = new ArrayList<ModpackProject>();
                try {
                  JsonObject json;
                  JsonArray hits;
                  if (body != null
                      && body.startsWith("{")
                      && (hits =
                              (json = new Gson().fromJson(body, JsonObject.class))
                                  .getAsJsonArray("hits"))
                          != null) {
                    for (int i = 0; i < hits.size(); ++i) {
                      JsonObject hit = hits.get(i).getAsJsonObject();
                      String icon =
                          hit.has("icon_url") && !hit.get("icon_url").isJsonNull()
                              ? hit.get("icon_url").getAsString()
                              : null;
                      String updated =
                          hit.has("date_modified")
                              ? hit.get("date_modified").getAsString().substring(0, 10)
                              : "";
                      projects.add(
                          new ModpackProject(
                              hit.get("project_id").getAsString(),
                              hit.get("title").getAsString(),
                              hit.get("description").getAsString(),
                              hit.get("author").getAsString(),
                              icon,
                              hit.get("downloads").getAsLong(),
                              updated,
                              "Modrinth",
                              type));
                    }
                  }
                } catch (Exception json) {
                  // empty catch block
                }
                ArrayList<ModpackProject> prepared = projects;
                Platform.runLater(
                    () -> {
                      if (!prepared.isEmpty()) {
                        this.prewarmedCommunity.put(type, prepared);
                      }
                      // 数据到手就推进一格进度
                      this.advanceCommunityPrewarm();
                      if (prepareIcons) {
                        // 图标继续在后台下，不再阻塞进度条
                        this.prepareIconsAndRun(prepared, () -> {});
                      }
                      this.advanceCommunityPrewarm();
                    });
              })
          .exceptionally(
              ex -> {
                this.advanceCommunityPrewarm();
                return null;
              });
    }
  }

  private void prefetchIcons(List<ModpackProject> projects) {
    for (ModpackProject project : projects) {
      String key = project.getIconUrl();
      if (key == null || key.isBlank() || this.iconMemoryCache.containsKey(key)) continue;
      String urlStr = key.startsWith("http") ? key : "https://" + key;
      CompletableFuture.runAsync(() -> this.fetchIconForPrefetch(urlStr, key));
    }
  }

  private Image decodeIcon(byte[] bytes) {
    try {
      Image image = new Image(new ByteArrayInputStream(bytes));
      if (!image.isError() && image.getWidth() > 0.0) {
        return image;
      }
    } catch (Exception image) {
      // empty catch block
    }
    try {
      BufferedImage buffered = ImageIO.read(new ByteArrayInputStream(bytes));
      if (buffered != null && buffered.getWidth() > 0) {
        return this.toFxImage(buffered);
      }
    } catch (Exception exception) {
      // empty catch block
    }
    return null;
  }

  private WritableImage toFxImage(BufferedImage source) {
    int width = source.getWidth();
    int height = source.getHeight();
    WritableImage image = new WritableImage(width, height);
    PixelWriter writer = image.getPixelWriter();
    int[] row = new int[width];
    for (int y = 0; y < height; ++y) {
      source.getRGB(0, y, width, 1, row, 0, width);
      for (int x = 0; x < width; ++x) {
        writer.setArgb(x, y, row[x]);
      }
    }
    return image;
  }

  private void performSearch() {
    List<ModpackProject> ready;
    String warmedType = this.apiProjectType();
    String loaderFilter = this.filterBar.loaderCombo().getValue();
    boolean loaderDefault =
        "任意".equals(loaderFilter)
            || "datapack".equals(warmedType) && "DataPack".equals(loaderFilter);
    boolean defaultFilters =
        (this.filterBar.searchField().getText() == null
                || this.filterBar.searchField().getText().trim().isEmpty())
            && "任意".equals(this.filterBar.gameVersionCombo().getValue())
            && loaderDefault
            && "默认".equals(this.filterBar.sortCombo().getValue());
    List<ModpackProject> list =
        ready = defaultFilters ? this.prewarmedCommunity.get(warmedType) : null;
    if (ready != null && !ready.isEmpty()) {
      this.modpackData.setAll((Collection<ModpackProject>) ready);
      this.searchOffset = ready.size();
      this.searchTotal = -1;
      this.loadingMoreResults = false;
      this.resultView.status().hide();
      this.setLoadMoreHint();
      this.prefetchIcons(ready);
      return;
    }
    this.searchOffset = 0;
    this.searchTotal = -1;
    this.loadingMoreResults = false;
    this.modpackData.clear();
    if (this.resultView != null) {
      this.resultView.setLoadMoreHintText("");
    }
    this.resultView.status().showLoading("正在从 Modrinth 获取数据…（失败自动切 MCIM 镜像）");
    this.loadSearchPage(true);
  }

  private CompletableFuture<String> fetchSearchBody(String url) {
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            HttpRequest req =
                HttpRequest.newBuilder(URI.create(url))
                    .header("User-Agent", "PCL-CE-Launcher/1.0")
                    .GET()
                    .timeout(java.time.Duration.ofSeconds(8L))
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            return resp.statusCode() == 200 ? resp.body() : null;
          } catch (Exception e) {
            return null;
          }
        });
  }

  private CompletableFuture<String> alternateSearchBodies(
      String officialUrl, String mirrorUrl, int offset) {
    boolean evenPage = Math.max(0, offset) / 40 % 2 == 0;
    String primary = evenPage ? officialUrl : mirrorUrl;
    String backup = evenPage ? mirrorUrl : officialUrl;
    return this.fetchSearchBody(primary)
        .thenCompose(
            body -> {
              if (body != null && body.startsWith("{")) {
                return CompletableFuture.completedFuture(body);
              }
              return this.fetchSearchBody(backup);
            });
  }

  private void prepareIconsAndRun(List<ModpackProject> projects, Runnable onReady) {
    ArrayList<ModpackProject> missing = new ArrayList<ModpackProject>();
    for (ModpackProject project : projects) {
      String key = project.getIconUrl();
      if (key == null || key.isBlank() || this.iconMemoryCache.containsKey(key)) continue;
      missing.add(project);
    }
    if (missing.isEmpty()) {
      onReady.run();
      return;
    }
    AtomicBoolean done = new AtomicBoolean();
    AtomicInteger remaining = new AtomicInteger(missing.size());
    int total = missing.size();
    PauseTransition timeout = new PauseTransition(Duration.seconds(8.0));
    Runnable finish =
        () -> {
          timeout.stop();
          if (done.compareAndSet(false, true)) {
            onReady.run();
          }
        };
    timeout.setOnFinished(e -> finish.run());
    timeout.play();
    for (ModpackProject project : missing) {
      String key = project.getIconUrl();
      String urlStr = key.startsWith("http") ? key : "https://" + key;
      CompletableFuture.runAsync(
          () -> this.fetchIconForPrepare(remaining, finish, urlStr, key, total));
    }
  }

  private void loadSearchPage(boolean first) {
    if (this.loadingMoreResults) {
      return;
    }
    if (!SearchSession.hasMore(this.searchOffset, this.searchTotal)) {
      return;
    }
    this.loadingMoreResults = true;
    if (this.searchWatchdog == null) {
      this.searchWatchdog = new PauseTransition(Duration.seconds(12.0));
      this.searchWatchdog.setOnFinished(
          e -> {
            if (!this.loadingMoreResults) {
              return;
            }
            this.loadingMoreResults = false;
            this.resultView.status().showText("获取超时，请检查网络后点击重试");
            this.resultView
                .status()
                .setRetry(
                    () -> {
                      this.resultView.status().showLoading("正在重新加载...");
                      this.loadSearchPage(true);
                    });
            this.updateLoadMoreButton();
          });
    }
    this.searchWatchdog.playFromStart();
    int offset = this.searchOffset;
    String query =
        this.filterBar.searchField().getText() == null
            ? ""
            : this.filterBar.searchField().getText().trim();
    String gameVer = this.filterBar.gameVersionCombo().getValue();
    String loader = this.filterBar.loaderCombo().getValue();
    String sort = this.filterBar.sortCombo().getValue();
    String pType = ModrinthSearch.projectType(this.currentProjectType);
    String pageKey = pType + "|" + query + "|" + gameVer + "|" + loader + "|" + sort + "|" + offset;
    List<ModpackProject> cachedPage = this.searchPageCache.get(pageKey);
    if (cachedPage != null && cachedPage.isEmpty()) {
      this.searchPageCache.remove(pageKey);
      cachedPage = null;
    }
    if (cachedPage != null) {
      this.loadingMoreResults = false;
      this.resultView.status().hide();
      List<ModpackProject> addedCache =
          SearchSession.mergeUnique(this.modpackData, cachedPage);
      this.searchOffset = offset + cachedPage.size();
      this.updateLoadMoreButton();
      this.prefetchIcons(addedCache);
      return;
    }
    try {
      String urlStr = ModrinthSearch.buildUrl(query, pType, gameVer, loader, sort, offset);
      String effectiveMirror = ModrinthSearch.mirrorUrl(urlStr, pType);

      this.alternateSearchBodies(urlStr, effectiveMirror, offset)
          .orTimeout(10L, TimeUnit.SECONDS)
          .thenAccept(
              body ->
                  Platform.runLater(
                      () -> {
                        JsonObject json;
                        if (this.searchWatchdog != null) {
                          this.searchWatchdog.stop();
                        }
                        this.loadingMoreResults = false;
                        if (body == null || !body.startsWith("{")) {
                          this.resultView.status().showText("加载失败：资源站不可用，点击这里重试");
                          this.resultView
                              .status()
                              .setRetry(
                                  () -> {
                                    this.resultView.status().showLoading("正在重新加载...");
                                    this.loadSearchPage(true);
                                  });
                          this.updateLoadMoreButton();
                          return;
                        }
                        this.resultView.status().showLoading("正在准备社区资源…");
                        try {
                          json = new Gson().fromJson(body, JsonObject.class);
                        } catch (Exception parseError) {
                          this.loadingMoreResults = false;
                          this.resultView.status().showText("资源数据解析失败，点击重试");
                          this.resultView.status().setRetry(() -> this.loadSearchPage(true));
                          return;
                        }
                        ModrinthSearch.Page page;
                        try {
                          java.util.Set<String> existingIds = new java.util.HashSet<>();
                          for (ModpackProject known : this.modpackData) {
                            existingIds.add(known.getId());
                          }
                          page = ModrinthSearch.parse(body, pType, existingIds);
                        } catch (Exception parseError) {
                          this.loadingMoreResults = false;
                          this.resultView.status().showText("资源数据解析失败，点击重试");
                          this.resultView.status().setRetry(() -> this.loadSearchPage(true));
                          return;
                        }
                        if (page.totalHits() >= 0) {
                          this.searchTotal = page.totalHits();
                        }
                        if (page.rawHits() == 0) {
                          this.loadingMoreResults = false;
                          if (this.searchWatchdog != null) {
                            this.searchWatchdog.stop();
                          }
                          this.resultView.status().showText(first ? "未找到匹配的项目，请调整筛选条件" : "没有更多结果");
                          this.updateLoadMoreButton();
                          return;
                        }
                        this.modpackData.addAll(page.projects());
                        this.searchPageCache.put(pageKey, page.projects());
                        this.searchOffset = offset + page.rawHits();
                        if (this.searchTotal < 0 && page.rawHits() < 40) {
                          this.searchTotal = this.searchOffset;
                        }
                        this.resultView.status().hide();
                        if (this.searchWatchdog != null) {
                          this.searchWatchdog.stop();
                        }
                        this.loadingMoreResults = false;
                        this.updateLoadMoreButton();
                        this.prefetchIcons(page.projects());
                      }))
          .exceptionally(
              ex -> {
                Platform.runLater(
                    () -> {
                      if (this.searchWatchdog != null) {
                        this.searchWatchdog.stop();
                      }
                      this.loadingMoreResults = false;
                      this.resultView.status().showText("请求失败：" + ex.getMessage() + "（点击重试）");
                      this.resultView
                          .status()
                          .setRetry(
                              () -> {
                                this.loadingMoreResults = false;
                                this.resultView.status().showLoading("正在重新加载...");
                                this.loadSearchPage(true);
                              });
                      this.updateLoadMoreButton();
                    });
                return null;
              });
    } catch (Exception e2) {
      this.loadingMoreResults = false;
      this.resultView.status().showText("请求参数构建失败: " + e2.getMessage());
    }
  }

  private String apiProjectType() {
    if ("模组".equals(this.currentProjectType)) {
      return "mod";
    }
    if ("资源包".equals(this.currentProjectType)) {
      return "resourcepack";
    }
    if ("光影包".equals(this.currentProjectType)) {
      return "shader";
    }
    if ("数据包".equals(this.currentProjectType)) {
      return "datapack";
    }
    return "modpack";
  }

  private void setLoadMoreHint() {
    this.updateLoadMoreButton();
  }

  private void updateLoadMoreButton() {
    if (this.resultView == null) {
      return;
    }
    boolean hasMore = this.searchTotal < 0 || this.searchOffset < this.searchTotal;
    if (this.modpackData.isEmpty()) {
      this.resultView.setLoadMoreHintText("");
      return;
    }
    this.resultView.setLoadMoreHintText(
        hasMore
            ? "已显示 "
                + this.modpackData.size()
                + (this.searchTotal >= 0 ? " / " + this.searchTotal : "")
                + " · 滚动到底自动加载"
            : "已加载全部 · 共 " + this.modpackData.size() + " 个");
  }

  private void chooseModpackVersion(String title, JsonArray versions, java.util.function.IntConsumer onChosen) {
    int limit = Math.min(60, versions.size());
    ArrayList<String> labels = new ArrayList<>();
    ArrayList<String> numbers = new ArrayList<>();
    ArrayList<String> types = new ArrayList<>();
    ArrayList<String> gamesList = new ArrayList<>();
    ArrayList<String> dates = new ArrayList<>();
    ArrayList<Long> downloadsList = new ArrayList<>();
    ArrayList<String> changelogs = new ArrayList<>();
    for (int i = 0; i < limit; ++i) {
      JsonObject version = versions.get(i).getAsJsonObject();
      String number =
          version.has("version_number") ? version.get("version_number").getAsString() : "?";
      String type = version.has("version_type") ? version.get("version_type").getAsString() : "";
      String games = "";
      if (version.has("game_versions") && version.get("game_versions").isJsonArray()) {
        JsonArray gameArray = version.getAsJsonArray("game_versions");
        ArrayList<String> parts = new ArrayList<>();
        for (int j = 0; j < Math.min(4, gameArray.size()); ++j) {
          parts.add(gameArray.get(j).getAsString());
        }
        games = String.join(", ", parts);
      }
      String date = version.has("date_published") ? version.get("date_published").getAsString() : "";
      if (date.length() > 10) date = date.substring(0, 10);
      long downloads = version.has("downloads") ? version.get("downloads").getAsLong() : 0L;
      String changelog = version.has("changelog") ? version.get("changelog").getAsString() : "";
      numbers.add(number);
      types.add(type);
      gamesList.add(games);
      dates.add(date);
      downloadsList.add(downloads);
      changelogs.add(changelog);
      labels.add(number + "   ·   " + (type.isBlank() ? "release" : type) + (date.isBlank() ? "" : "   ·   " + date));
    }
    if (labels.isEmpty()) {
      onChosen.accept(-1);
      return;
    }

    javafx.scene.control.ListView<Integer> list = new javafx.scene.control.ListView<>();
    for (int i = 0; i < labels.size(); i++) {
      list.getItems().add(i);
    }
    list.getSelectionModel().selectFirst();
    // 版本行：类型徽章带颜色，悬停高亮由 CSS 的 .version-row:hover 负责
    list.setCellFactory(
        view ->
            new javafx.scene.control.ListCell<>() {
              @Override
              protected void updateItem(Integer index, boolean empty) {
                super.updateItem(index, empty);
                if (empty || index == null) {
                  setText(null);
                  setGraphic(null);
                  return;
                }
                String type = types.isEmpty() ? "" : types.get(Math.min(index, types.size() - 1));
                String badgeText = type.isBlank() ? "release" : type;
                String badgeColor =
                    switch (badgeText) {
                      case "release" -> "#22c55e";
                      case "beta" -> "#f59e0b";
                      case "alpha" -> "#ef4444";
                      default -> "#94a3b8";
                    };
                javafx.scene.control.Label badge = new javafx.scene.control.Label(badgeText);
                badge.setStyle(
                    "-fx-text-fill: " + badgeColor + "; -fx-font-size: 10px; -fx-font-weight: bold;"
                        + " -fx-background-color: " + badgeColor + "22; -fx-background-radius: 6;"
                        + " -fx-padding: 1 6;");
                javafx.scene.control.Label text =
                    new javafx.scene.control.Label(numbers.get(index));
                text.setStyle("-fx-text-fill: -app-text; -fx-font-size: 12.5px;");
                javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
                javafx.scene.layout.HBox.setHgrow(spacer, Priority.ALWAYS);
                javafx.scene.control.Label date =
                    new javafx.scene.control.Label(
                        dates.get(index).isBlank() ? "" : dates.get(index));
                date.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11px;");
                javafx.scene.control.Label games =
                    new javafx.scene.control.Label(
                        gamesList.isEmpty() || index >= gamesList.size()
                            ? ""
                            : gamesList.get(index));
                games.setStyle("-fx-text-fill: -app-text-dim; -fx-font-size: 10.5px;");
                javafx.scene.layout.VBox texts =
                    new javafx.scene.layout.VBox(1, text, games);
                javafx.scene.layout.HBox.setHgrow(texts, Priority.ALWAYS);
                javafx.scene.layout.HBox row =
                    new javafx.scene.layout.HBox(10, badge, texts, date);
                row.setAlignment(Pos.CENTER_LEFT);
                setText(null);
                setGraphic(row);
                if (!getStyleClass().contains("version-row")) {
                  getStyleClass().add("version-row");
                }
              }
            });
    list.setPrefWidth(300);
    list.setPrefHeight(240);

    Label detailTitle = new Label();
    detailTitle.setStyle("-fx-text-fill: -app-text; -fx-font-size: 13px; -fx-font-weight: bold;");
    Label detailMeta = new Label();
    detailMeta.setStyle("-fx-text-fill: -app-text-dim; -fx-font-size: 11px;");
    detailMeta.setWrapText(true);
    TextArea detailLog = new TextArea();
    detailLog.setEditable(false);
    detailLog.setWrapText(true);
    detailLog.setMinWidth(0);
    detailLog.setPrefColumnCount(48);
    detailLog.setMinHeight(0);
    detailLog.setPrefRowCount(10);
    detailLog.setStyle(
        "-fx-font-size: 12px; -fx-background-color: transparent;"
            + " -fx-control-inner-background: transparent; -fx-text-fill: -app-text-2;"
            + " -fx-background-insets: 0; -fx-padding: 0;");
    VBox.setVgrow(detailLog, Priority.ALWAYS);
    VBox detail = new VBox(8, detailTitle, detailMeta, detailLog);
    detail.setPadding(new Insets(12));
    detail.setStyle(
        "-fx-background-color: -app-card; -fx-background-radius: 16;"
            + " -fx-border-color: -app-border-soft; -fx-border-radius: 16;");

    Runnable refreshDetail =
        () -> {
          int index = list.getSelectionModel().getSelectedIndex();
          if (index < 0 || index >= labels.size()) {
            return;
          }
          String type = types.get(index);
          detailTitle.setText(numbers.get(index) + "  (" + (type.isBlank() ? "release" : type) + ")");
          detailMeta.setText(
              "游戏版本："
                  + (gamesList.get(index).isBlank() ? "未标注" : gamesList.get(index))
                  + "\n发布："
                  + (dates.get(index).isBlank() ? "未知" : dates.get(index))
                  + "   ·   下载 "
                  + String.format(Locale.ROOT, "%,d", downloadsList.get(index)));
          String changelog = changelogs.get(index);
          if (changelog == null || changelog.isBlank()) {
            detailLog.setText("这个版本没有提供更新说明。");
          } else {
            String text = changelog.replaceAll("<[^>]+>", "").trim();
            if (text.length() > 1200) {
              text = text.substring(0, 1200) + "…";
            }
            detailLog.setText(text);
          }
        };
    list.getSelectionModel().selectedIndexProperty().addListener((obs, old, now) -> refreshDetail.run());
    refreshDetail.run();

    // 左：版本列表卡片，右：更新内容卡片，尺寸和圆角跟全局一致
    list.setPrefWidth(252);
    list.setMinWidth(180);
    list.setMaxWidth(252);
    list.setMinHeight(0);
    list.setFixedCellSize(46);
    list.setStyle(
        "-fx-background-color: transparent; -fx-control-inner-background: transparent;"
            + " -fx-border-color: transparent; -fx-padding: 0;");
    VBox.setVgrow(list, Priority.ALWAYS);

    VBox listCard = new VBox(list);
    listCard.setPadding(new Insets(8));
    listCard.setMinWidth(180);
    listCard.setPrefWidth(268);
    listCard.setMaxWidth(268);
    listCard.setStyle(
        "-fx-background-color: -app-card; -fx-background-radius: 16;"
            + " -fx-border-color: -app-border-soft; -fx-border-radius: 16;");
    VBox.setVgrow(listCard, Priority.ALWAYS);

    detail.setMinWidth(0);
    HBox.setHgrow(detail, Priority.ALWAYS);
    VBox.setVgrow(detail, Priority.ALWAYS);

    HBox body = new HBox(14, listCard, detail);
    body.setAlignment(Pos.TOP_LEFT);
    VBox.setVgrow(body, Priority.ALWAYS);
    // 右侧详情宽度 = body 宽 - 列表卡片 268 - 间距 14
    detail.maxWidthProperty()
        .bind(javafx.beans.binding.Bindings.max(220, body.widthProperty().subtract(282)));

    Button back = new Button("‹  返回");
    back.getStyleClass().addAll("btn-secondary", "btn-sm");
    Label heading = new Label(title);
    heading.setStyle("-fx-text-fill: -app-text; -fx-font-size: 16px; -fx-font-weight: bold;");
    Label subtitle = new Label("选择要安装的版本 · 右侧是该版本的更新内容");
    subtitle.setStyle("-fx-text-fill: -app-text-dim; -fx-font-size: 11px;");
    VBox headingBox = new VBox(2, heading, subtitle);
    Region headerSpacer = new Region();
    HBox.setHgrow(headerSpacer, Priority.ALWAYS);
    HBox header = new HBox(12, back, headingBox, headerSpacer);
    header.setAlignment(Pos.CENTER_LEFT);
    header.setPadding(new Insets(0, 0, 6, 0));

    Button download = new Button("下载此版本");
    download.getStyleClass().addAll("btn-primary", "btn-sm");
    Label tip = new Label("下载后会询问是否立即安装到当前实例");
    tip.setStyle("-fx-text-fill: -app-text-dim; -fx-font-size: 11px;");
    Region footerSpacer = new Region();
    HBox.setHgrow(footerSpacer, Priority.ALWAYS);
    HBox footer = new HBox(10, tip, footerSpacer, download);
    footer.setAlignment(Pos.CENTER_LEFT);
    footer.setPadding(new Insets(6, 0, 0, 0));

    VBox page = new VBox(10, header, body, footer);
    page.setPadding(new Insets(0));
    page.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
    page.getStyleClass().add("version-page");
    // 页面宽度跟随下载区，避免被内部长文本撑宽

    this.downloadContentArea().getChildren().removeIf(n -> n.getStyleClass().contains("version-page"));

    Runnable goBack =
        () -> {
          this.showDownloadPanel(this.communityPanel);
          onChosen.accept(-1);
        };
    back.setOnAction(e -> goBack.run());

    // 选中版本后先问一句再下载，避免误触直接开始拉包
    java.util.function.IntConsumer askDownload =
        index -> {
          int safe = Math.max(0, Math.min(index, labels.size() - 1));
          String versionName = numbers.get(safe);
          InAppDialog.confirm(
              this.dialogLayer,
              "下载 " + title + " " + versionName + "？",
              "游戏版本："
                  + (gamesList.get(safe).isBlank() ? "未标注" : gamesList.get(safe))
                  + "\n确认后开始获取下载地址。",
              "开始下载",
              () -> {
                this.showDownloadPanel(this.communityPanel);
                onChosen.accept(safe);
              });
        };
    download.setOnAction(e -> askDownload.accept(list.getSelectionModel().getSelectedIndex()));
    list.setOnMouseClicked(
        e -> {
          if (e.getClickCount() == 2) {
            askDownload.accept(list.getSelectionModel().getSelectedIndex());
          }
        });
    list.setOnKeyPressed(
        e -> {
          if (e.getCode() == javafx.scene.input.KeyCode.ENTER) {
            askDownload.accept(list.getSelectionModel().getSelectedIndex());
          }
        });
    this.showDownloadPanel(page);
  }

  private void openCommunityModDetail(ModpackProject project) {
    this.logLine("🖱 打开预览: " + project.getTitle() + " / " + project.getProjectType());
    // 整合包点卡片就是"预览"：直接进版本页，选版本后才下载
    String pt = project.getProjectType();
    if ("整合包".equals(pt) || "modpack".equalsIgnoreCase(pt)) {
      this.fetchVersionsAndDownload(
          project.getId(), project.getTitle(), project.getProjectType());
      return;
    }
    String id = this.currentInstanceId();
    if (id == null) {
      Toast.info("先选择一个实例，再查看模组详情");
      return;
    }
    if (this.modDetailPanel == null) {
      this.modDetailPanel = new VBox(10.0);
      this.modDetailPanel.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
      this.modDetailPanel.setAlignment(Pos.TOP_LEFT);
      this.downloadContentArea().getChildren().add(this.modDetailPanel);
    }
    Instance instance = Instance.of(new File(this.config.getGameDir()), id);
    VBox page = this.buildModPageShell(project, instance);
    this.modDetailPanel.getChildren().setAll(page);
    this.showDownloadPanel(this.modDetailPanel);
    String url = "https://api.modrinth.com/v2/project/" + project.getId() + "/version";
    HttpRequest request =
        HttpRequest.newBuilder(URI.create(url))
            .header("User-Agent", "PCL-CE-Launcher/1.0")
            .GET()
            .timeout(java.time.Duration.ofSeconds(15L))
            .build();
    CompletableFuture.supplyAsync(
            () -> {
              try {
                HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                return response.statusCode() == 200 ? response.body() : null;
              } catch (Exception e) {
                return null;
              }
            })
        .thenAccept(
            body ->
                Platform.runLater(
                    () -> {
                      page.getChildren()
                          .removeIf(
                              node -> {
                                Label label;
                                return node instanceof Label
                                    && (label = (Label) node).getText() != null
                                    && label.getText().startsWith("正在读取");
                              });
                      if (body == null || !body.startsWith("[")) {
                        Label error = new Label("版本列表加载失败，请检查网络后重试。");
                        error.setStyle("-fx-text-fill: #f87171; -fx-font-size: 12px;");
                        page.getChildren().add(error);
                        return;
                      }
                      JsonArray all = new Gson().fromJson(body, JsonArray.class);
                      page.getChildren().add(this.buildModVersionList(project, all, "全部", "全部"));
                    }));
  }

  private VBox buildModPageShell(ModpackProject project, Instance instance) {
    if (this.modDetailPage == null) {
      this.modDetailPage =
          new ModDetailPage(
              new ModDetailPage.Host() {
                @Override
                public void back() {
                  HelloApplication.this.showDownloadPanel(HelloApplication.this.communityPanelNode());
                }

                @Override
                public Node versionRow(ModpackProject project, JsonObject version) {
                  return HelloApplication.this.buildModVersionRow(project, version);
                }
              });
    }
    return this.modDetailPage.shell(project);
  }

  private VBox buildModVersionList(
      ModpackProject project, JsonArray versions, String ignoredMc, String ignoredLoader) {
    if (this.modDetailPage == null) {
      return new VBox();
    }
    return this.modDetailPage.versionList(project, versions);
  }

  private HBox buildModVersionRow(ModpackProject project, JsonObject version) {
    String number = this.jsonString(version, "version_number");
    String type = this.jsonString(version, "version_type");
    JsonArray files =
        version.has("files") && version.get("files").isJsonArray()
            ? version.getAsJsonArray("files")
            : new JsonArray();
    String fileName = "";
    long size = 0L;
    if (!files.isEmpty()) {
      JsonObject file = files.get(0).getAsJsonObject();
      fileName = this.jsonString(file, "filename");
      size = file.has("size") ? file.get("size").getAsLong() : 0L;
    }
    int deps =
        version.has("dependencies") && version.get("dependencies").isJsonArray()
            ? version.getAsJsonArray("dependencies").size()
            : 0;
    Label name = new Label(number.isBlank() ? "未知版本" : number);
    name.setStyle("-fx-text-fill: -app-text-strong; -fx-font-size: 13px; -fx-font-weight: bold;");
    Label typeLabel = new Label("release".equals(type) ? "正式版" : type);
    typeLabel.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 10px;");
    HBox titleRow = new HBox(8.0, name, typeLabel);
    titleRow.setAlignment(Pos.CENTER_LEFT);
    Label fileLabel =
        new Label(
            (fileName.isBlank() ? "无文件名" : fileName)
                + (size > 0L ? " · " + size / 1024L + " KB" : ""));
    fileLabel.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11px;");
    Label meta =
        new Label(
            "支持 "
                + this.jsonArrayText(version, "game_versions")
                + " · "
                + this.jsonArrayText(version, "loaders")
                + (deps > 0 ? " · 前置 " + deps + " 个" : ""));
    meta.setStyle("-fx-text-fill: -app-text-dim-2; -fx-font-size: 10.5px;");
    VBox info = new VBox(3.0, titleRow, fileLabel, meta);
    HBox.setHgrow(info, Priority.ALWAYS);
    Button download = new Button("下载此文件");
    download.setStyle(
        "-fx-background-color: #14b8a6; -fx-text-fill: white; -fx-font-weight: bold;"
            + " -fx-background-radius: 8; -fx-padding: 6 14; -fx-cursor: hand;");
    download.setOnAction(
        e -> {
          download.setDisable(true);
          download.setText("下载中…");
          this.downloadModVersion(version, project.getTitle(), download);
        });
    HBox row = new HBox(12.0, info, download);
    row.setAlignment(Pos.CENTER_LEFT);
    row.setPadding(new Insets(10.0, 12.0, 10.0, 12.0));
    row.setStyle(
        "-fx-background-color: -app-row2; -fx-background-radius: 10; -fx-border-color:"
            + " -app-border-soft; -fx-border-radius: 10;");
    return row;
  }

  private void downloadModVersion(JsonObject version, String title, Button button) {
    block4:
    {
      try {
        JsonArray files = version.getAsJsonArray("files");
        if (files == null || files.isEmpty()) {
          throw new RuntimeException("该版本没有文件");
        }
        JsonObject target = files.get(0).getAsJsonObject();
        String url = this.jsonString(target, "url");
        String fileName = this.jsonString(target, "filename");
        File mods =
            InstanceDirs.mods(
                new File(this.config.getGameDir()),
                this.currentInstanceId(),
                this.isIsolated(this.currentInstanceId()));
        if (!mods.isDirectory()) {
          mods.mkdirs();
        }
        File save = new File(mods, fileName);
        this.downloadBarView.setExpectedBytes(target.has("size") ? target.get("size").getAsLong() : -1L);
        this.logArea.appendText("📥 下载 " + title + " → " + fileName + "\n");
        this.downloadFileAsync(
            url,
            save,
            this.logArea,
            saved -> {
              this.logArea.appendText("✅ 已安装到 mods: " + saved.getName() + "（前置依赖可在实例 → 模组里检查）\n");
              if (button != null) {
                button.setText("已下载");
                button.setDisable(false);
              }
            });
      } catch (Exception e) {
        this.logArea.appendText("❌ 下载失败: " + e.getMessage() + "\n");
        if (button == null) break block4;
        button.setDisable(false);
        button.setText("下载此文件");
      }
    }
  }

  private String jsonString(JsonObject object, String key) {
    return object != null && object.has(key) && !object.get(key).isJsonNull()
        ? object.get(key).getAsString()
        : "";
  }

  private String jsonArrayText(JsonObject object, String key) {
    if (!object.has(key) || !object.get(key).isJsonArray()) {
      return "未知";
    }
    StringBuilder out = new StringBuilder();
    for (JsonElement element : object.getAsJsonArray(key)) {
      if (out.length() > 0) {
        out.append(" / ");
      }
      out.append(element.getAsString());
    }
    return out.length() == 0 ? "未知" : out.toString();
  }

  /** 点卡片/下载后立刻显示的版本页骨架，避免网络慢时看起来"没反应"。 */
  private void showVersionLoadingPage(String title) {
    Button back = new Button("‹  返回");
    back.getStyleClass().addAll("btn-secondary", "btn-sm");
    Label heading = new Label(title);
    heading.setStyle("-fx-text-fill: -app-text; -fx-font-size: 16px; -fx-font-weight: bold;");
    Label subtitle = new Label("正在读取可安装的版本…");
    subtitle.setStyle("-fx-text-fill: -app-text-dim; -fx-font-size: 11px;");
    VBox headingBox = new VBox(2, heading, subtitle);
    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);
    HBox header = new HBox(12, back, headingBox, spacer);
    header.setAlignment(Pos.CENTER_LEFT);

    versionLoadingLabel = new Label("正在读取版本列表…");
    versionLoadingLabel.setStyle("-fx-text-fill: -app-text-2; -fx-font-size: 13px;");
    Button retry = new Button("重新获取");
    retry.getStyleClass().addAll("btn-secondary", "btn-sm");
    retry.setVisible(false);
    retry.setManaged(false);
    retry.setOnAction(
        e -> {
          if (this.lastVersionProjectId == null) {
            this.showDownloadPanel(this.communityPanel);
            return;
          }
          // 清掉缓存，真正重新访问数据源
          this.versionListCache.remove(this.lastVersionProjectId);
          this.versionListCacheAt.remove(this.lastVersionProjectId);
          this.fetchVersionsAndDownload(
              this.lastVersionProjectId, this.lastVersionProjectTitle, this.lastVersionProjectType);
        });
    versionRetryBtn = retry;
    VBox center = new VBox(12, versionLoadingLabel, retry);
    center.setAlignment(Pos.CENTER);
    VBox.setVgrow(center, Priority.ALWAYS);

    VBox page = new VBox(10, header, center);
    page.setPadding(new Insets(6, 10, 6, 6));
    page.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
    page.getStyleClass().add("version-page");
    this.downloadContentArea().getChildren().removeIf(n -> n.getStyleClass().contains("version-page"));
    back.setOnAction(e -> this.showDownloadPanel(this.communityPanel));
    this.showDownloadPanel(page);
  }

  private void fetchVersionsAndDownload(String projectId, String projectTitle, String projectType) {
    this.lastVersionProjectId = projectId;
    this.lastVersionProjectTitle = projectTitle;
    this.lastVersionProjectType = projectType;
    // 先把页面顶出来，网络慢也立刻有反馈；失败时页面里可以重试
    this.showVersionLoadingPage(projectTitle);
    JsonArray cachedVersions = this.versionListCache.get(projectId);
    Long cachedAt = this.versionListCacheAt.get(projectId);
    if (cachedVersions != null && cachedAt != null && System.currentTimeMillis() - cachedAt < 10 * 60 * 1000L) {
      Platform.runLater(
          () ->
              this.chooseModpackVersion(
                  projectTitle,
                  cachedVersions,
                  chosen -> {
                    if (chosen >= 0) {
                      this.downloadChosenVersion(projectTitle, projectType, cachedVersions, chosen);
                    }
                  }));
      return;
    }
    String urlStr = "https://api.modrinth.com/v2/project/" + projectId + "/version";
    String mirrorStr = urlStr.replace("https://api.modrinth.com/v2", "https://mod.mcimirror.top/modrinth/v2");
    // 镜像和官方并行竞速：谁先返回有效 JSON 就用谁，避免串行等待拖慢
    java.util.function.Supplier<String> fetchOne =
        () -> {
          java.util.concurrent.CompletableFuture<String> winner = new java.util.concurrent.CompletableFuture<>();
          for (String url : new String[] {mirrorStr, urlStr}) {
            CompletableFuture.runAsync(
                () -> {
                  try {
                    HttpRequest req =
                        HttpRequest.newBuilder(URI.create(url))
                            .header("User-Agent", "PCL-CE-Launcher/1.0")
                            .GET()
                            .timeout(java.time.Duration.ofSeconds(6L))
                            .build();
                    HttpResponse<String> resp =
                        httpClient.send(req, HttpResponse.BodyHandlers.ofString());
                    if (resp.statusCode() == 200
                        && resp.body() != null
                        && resp.body().startsWith("[")) {
                      winner.complete(resp.body());
                    }
                  } catch (Exception ignored) {
                    // 另一个源还有机会
                  }
                });
          }
          try {
            return winner.get(9L, java.util.concurrent.TimeUnit.SECONDS);
          } catch (Exception e) {
            return null;
          }
        };
    CompletableFuture.supplyAsync(fetchOne)
        .thenAccept(
            body ->
                Platform.runLater(
                    () -> {
                      if (body == null || !body.startsWith("[")) {
                        this.logArea.appendText("❌ 获取 " + projectTitle + " 版本列表失败。\n");
                        if (this.versionLoadingLabel != null) {
                          this.versionLoadingLabel.setText("读取版本列表失败，检查网络后点「重新获取」");
                        }
                        if (this.versionRetryBtn != null) {
                          this.versionRetryBtn.setVisible(true);
                          this.versionRetryBtn.setManaged(true);
                        }
                        return;
                      }
                      JsonArray arr = new Gson().fromJson(body, JsonArray.class);
                      if (arr.size() > 0) {
                        this.versionListCache.put(projectId, arr);
                        this.versionListCacheAt.put(projectId, System.currentTimeMillis());
                      }
                      if (arr.size() == 0) {
                        this.logArea.appendText("❌ " + projectTitle + " 暂无可用版本。\n");
                        if (this.versionLoadingLabel != null) {
                          this.versionLoadingLabel.setText("这个项目暂时没有可下载的版本");
                        }
                        if (this.versionRetryBtn != null) {
                          this.versionRetryBtn.setVisible(true);
                          this.versionRetryBtn.setManaged(true);
                        }
                        return;
                      }
                      this.chooseModpackVersion(
                          projectTitle,
                          arr,
                          chosen -> {
                            if (chosen >= 0) {
                              this.downloadChosenVersion(projectTitle, projectType, arr, chosen);
                            }
                          });
                    }));
  }

  private String installDirFor(String projectType) {
    return switch (projectType) {
      case "模组" -> "mods";
      case "资源包" -> "resourcepacks";
      case "光影包" -> "shaderpacks";
      case "数据包" -> "datapacks";
      default -> "downloads/modpack";
    };
  }

  private final StringBuilder logBuffer = new StringBuilder();
  private PauseTransition logFlush;

  private void logLine(String text) {
    if (text == null) return;
    String line = text.endsWith("\n") ? text : text + "\n";
    if (this.logArea == null) {
      System.out.print("[log] " + line);
      return;
    }
    if (!Platform.isFxApplicationThread()) {
      Platform.runLater(() -> this.logLine(line));
      return;
    }
    // 下载/启动时日志是按行刷的，直接 appendText 每行都会触发一次文本区布局；
    // 这里先攒 90ms 再整块写入，行数多时能省掉大量布局与滚动计算。
    this.logBuffer.append(line);
    if (this.logBuffer.length() > 200_000) {
      this.flushLogBuffer();
      return;
    }
    if (this.logFlush == null) {
      this.logFlush = new PauseTransition(Duration.millis(90));
      this.logFlush.setOnFinished(e -> this.flushLogBuffer());
    }
    this.logFlush.playFromStart();
  }

  private void flushLogBuffer() {
    if (this.logBuffer.length() == 0) return;
    String chunk = this.logBuffer.toString();
    this.logBuffer.setLength(0);
    this.appendKeepingScroll(chunk);
  }

  /** 日志区最多保留的字符数；超出后从头部裁掉一段，避免长时间运行内存无限增长。 */
  private static final int LOG_MAX_CHARS = 400_000;

  private void appendKeepingScroll(String line) {
    double before = this.logArea.getScrollTop();
    boolean wasAtBottom = before >= 0.985;
    this.logArea.appendText(line);
    int length = this.logArea.getLength();
    if (length > LOG_MAX_CHARS) {
      this.logArea.replaceText(0, length - LOG_MAX_CHARS / 2, "");
    }
    if (!wasAtBottom) {
      this.logArea.setScrollTop(before);
    }
  }

  private void logMessage(String message) {
    this.logLine(message);
  }

  private void exportInstance(Instance instance) {
    if (instance == null) {
      return;
    }
    CheckBox includeSaves = new CheckBox("同时包含存档");
    CheckBox includeLauncher = new CheckBox("带启动器（导出为便携文件夹，启动器和 .minecraft 放在同一目录）");
    CheckBox serverPack = new CheckBox("导出服务端文件包（mods / config / scripts，自动过滤常见客户端模组）");
    InAppDialog.custom(
        this.dialogLayer,
        "导出整合包",
        new VBox(
            10.0,
            new Label("默认导出 .mrpack，可直接拖入 PCL 安装。"),
            new Label("勾选「带启动器」后会导出文件夹：启动器 + .minecraft + 启动脚本。"),
            includeSaves,
            includeLauncher,
            serverPack),
        List.of(
            InAppDialog.Action.secondary("取消", null),
            InAppDialog.Action.primary(
                "导出",
                () -> {
    File out;
    boolean withLauncher = includeLauncher.isSelected();
    boolean withSaves = includeSaves.isSelected();
    String safeName = instance.id.replaceAll("[^0-9A-Za-z._-]", "_");
    if (serverPack.isSelected()) {
      File instanceRoot =
          InstanceDirs.root(
              new File(this.config.getGameDir()), instance.id, this.isIsolated(instance.id));
      FileChooser chooser = new FileChooser();
      chooser.setTitle("导出服务端文件包");
      chooser
          .getExtensionFilters()
          .add(new FileChooser.ExtensionFilter("ZIP 压缩包 (*.zip)", "*.zip"));
      chooser.setInitialFileName(safeName + "-server.zip");
      File target = chooser.showSaveDialog(this.stageRef);
      if (target == null) {
        return;
      }
      if (!target.getName().toLowerCase(Locale.ROOT).endsWith(".zip")) {
        target = new File(target.getParentFile(), target.getName() + ".zip");
      }
      File output = target;
      Toast.info("正在导出服务端文件包…");
      CompletableFuture.runAsync(
          () -> {
            try {
              ServerPackExporter.Result result =
                  ServerPackExporter.export(
                      instanceRoot, output, true, msg -> this.logLine(msg), null);
              Platform.runLater(
                  () -> {
                    Toast.ok(
                        "服务端包完成："
                            + result.files()
                            + " 个文件，过滤 "
                            + result.skippedClientMods()
                            + " 个客户端模组");
                    this.logLine("✅ 服务端包: " + output.getAbsolutePath());
                    this.openDir(output.getParentFile());
                  });
            } catch (Exception ex) {
              Platform.runLater(
                  () -> {
                    Toast.error("服务端包导出失败：" + ex.getMessage());
                    this.logLine("❌ 服务端包导出失败: " + ex.getMessage());
                  });
            }
          });
      return;
    }
    if (withLauncher) {
      DirectoryChooser dirChooser = new DirectoryChooser();
      dirChooser.setTitle("选择导出位置");
      File parent = dirChooser.showDialog(this.stageRef);
      if (parent == null) {
        return;
      }
      out = new File(parent, safeName + "-portable");
      int suffix = 2;
      while (out.exists()) {
        out = new File(parent, safeName + "-portable-" + suffix++);
      }
    } else {
      FileChooser chooser = new FileChooser();
      chooser.setTitle("导出整合包");
      chooser
          .getExtensionFilters()
          .add(new FileChooser.ExtensionFilter("Modrinth 整合包 (*.mrpack)", "*.mrpack"));
      chooser.setInitialFileName(safeName + ".mrpack");
      out = chooser.showSaveDialog(this.stageRef);
      if (out == null) {
        return;
      }
      if (!out.getName().toLowerCase(Locale.ROOT).endsWith(".mrpack")) {
        out = new File(out.getParentFile(), out.getName() + ".mrpack");
      }
    }
    File target = out;
    ProgressBar exportProgress = new ProgressBar(0.0);
    exportProgress.setMaxWidth(Double.MAX_VALUE);
    Label exportStatus = new Label("正在准备导出…");
    exportStatus.setStyle("-fx-text-fill: -app-text-2; -fx-font-size: 12px;");
    Dialog<?> progressDialog = new Dialog<>();
    progressDialog.initOwner(this.stageRef);
    progressDialog.initModality(Modality.APPLICATION_MODAL);
    progressDialog.setTitle("正在导出");
    progressDialog.setHeaderText(withLauncher ? "正在导出便携整合包" : "正在导出 Modrinth 整合包");
    progressDialog.getDialogPane().setContent(new VBox(12.0, exportStatus, exportProgress));
    progressDialog.getDialogPane().setPrefWidth(440.0);
    AtomicBoolean exportDone = new AtomicBoolean(false);
    progressDialog.setOnCloseRequest(
        ev -> {
          if (!exportDone.get()) {
            ev.consume();
          }
        });
    progressDialog
        .getDialogPane()
        .addEventFilter(
            KeyEvent.KEY_PRESSED,
            ev -> {
              if (ev.getCode() == KeyCode.ESCAPE && !exportDone.get()) {
                ev.consume();
              }
            });
    progressDialog.show();
    DoubleConsumer onProgress =
        p ->
            Platform.runLater(
                () -> {
                  exportProgress.setProgress(Math.max(0.0, Math.min(1.0, p)));
                  exportStatus.setText("正在导出… " + (int) Math.round(p * 100.0) + "%");
                });
    this.logMessage(
        withLauncher ? "正在导出便携整合包: " + target.getName() : "正在导出整合包: " + target.getName());

    CompletableFuture.runAsync(
            () -> {
              try {
                if (withLauncher) {
                  PortableExporter.export(
                      new File(this.config.getGameDir()), instance, target, withSaves, onProgress);
                } else {
                  ModpackExporter.export(
                      new File(this.config.getGameDir()), instance, target, withSaves, onProgress);
                }
              } catch (Exception ex) {
                throw new CompletionException(ex);
              }
            })
        .thenAccept(
            v ->
                Platform.runLater(
                    () -> {
                      exportDone.set(true);
                      progressDialog.setOnCloseRequest(null);
                      try {
                        if (progressDialog.getDialogPane().getScene() != null
                            && progressDialog.getDialogPane().getScene().getWindow() != null) {
                          progressDialog.getDialogPane().getScene().getWindow().hide();
                        }
                      } catch (Exception exception) {
                        // empty catch block
                      }
                      progressDialog.close();
                      InAppDialog.alert(
                          "导出完成",
                          (withLauncher ? "便携整合包已生成：\n" : "Modrinth 整合包已生成：\n")
                              + target.getAbsolutePath());
                      this.logMessage("✅ 导出完成: " + target.getAbsolutePath());
                    }))
        .exceptionally(
            ex -> {
              Throwable cause = ex;
              while (cause.getCause() != null) {
                cause = cause.getCause();
              }
              Throwable root = cause;
              Platform.runLater(
                  () -> {
                    exportDone.set(true);
                    progressDialog.setOnCloseRequest(null);
                    try {
                      if (progressDialog.getDialogPane().getScene() != null
                          && progressDialog.getDialogPane().getScene().getWindow() != null) {
                        progressDialog.getDialogPane().getScene().getWindow().hide();
                      }
                    } catch (Exception exception) {
                      // empty catch block
                    }
                    progressDialog.close();
                    Toast.error(
                        "导出失败: " + (root.getMessage() == null ? "未知错误" : root.getMessage()));
                    this.logMessage("❌ 导出失败: " + root.getMessage());
                  });
              return null;
            });
                  })),
        null);
  }

  private File firstModJar(Dragboard board) {
    List<File> jars = this.modJars(board);
    return jars.isEmpty() ? null : jars.get(0);
  }

  private List<File> modJars(Dragboard board) {
    ArrayList<File> jars = new ArrayList<File>();
    if (board == null || !board.hasFiles()) {
      return jars;
    }
    for (File file : board.getFiles()) {
      String name = file.getName().toLowerCase(Locale.ROOT);
      if (!name.endsWith(".jar") && !name.endsWith(".jar.disabled")) continue;
      jars.add(file);
    }
    return jars;
  }

  private void installDroppedMods(List<File> jars) {
    File game = new File(this.config.getGameDir());
    File mods = InstanceDirs.mods(game, this.currentInstanceId(), this.isIsolated(this.currentInstanceId()));
    if (!mods.isDirectory()) {
      mods.mkdirs();
    }
    int installed = 0;
    for (File jar : jars) {
      try {
        File target = new File(mods, jar.getName());
        int suffix = 2;
        while (target.isFile() && target.length() != jar.length()) {
          String name = jar.getName();
          int dot =
              name.toLowerCase(Locale.ROOT).endsWith(".jar.disabled")
                  ? name.length() - ".jar.disabled".length()
                  : name.length() - 4;
          target = new File(mods, name.substring(0, dot) + "-" + suffix++ + name.substring(dot));
        }
        if (target.isFile()) continue;
        Files.copy(jar.toPath(), target.toPath(), new CopyOption[0]);
        ++installed;
        this.logMessage("📦 拖入模组: " + target.getName());
      } catch (Exception e) {
        this.logMessage("❌ 安装拖入模组失败: " + jar.getName() + " - " + e.getMessage());
      }
    }
    if (installed > 0) {
      Toast.ok("已安装 " + installed + " 个模组");
    } else {
      Toast.warn("没有新增模组（可能已存在）");
    }
  }

  private File firstImportArchive(Dragboard board) {
    if (board == null || !board.hasFiles()) {
      return null;
    }
    for (File file : board.getFiles()) {
      String name = file.getName().toLowerCase(Locale.ROOT);
      if (!name.endsWith(".zip") && !name.endsWith(".mrpack") && !name.endsWith(".mcpack"))
        continue;
      return file;
    }
    return null;
  }

  private void importModpackDialog() {
    File dir;
    FileChooser chooser = new FileChooser();
    chooser.setTitle("导入整合包");
    String lastDir = this.config.getLastImportDir();
    if (lastDir != null && !lastDir.isBlank() && (dir = new File(lastDir)).isDirectory()) {
      chooser.setInitialDirectory(dir);
    }
    chooser
        .getExtensionFilters()
        .addAll(
            new FileChooser.ExtensionFilter("整合包", "*.zip", "*.mrpack"),
            new FileChooser.ExtensionFilter("所有文件", "*.*"));
    File file = chooser.showOpenDialog(this.stageRef);
    if (file != null) {
      this.config.setLastImportDir(
          file.getParentFile() == null ? "" : file.getParentFile().getAbsolutePath());
      this.config.save();
      this.importModpack(file);
    }
  }

  /**
   * 查找已经安装好的「同一 MC 版本 + 加载器」实例，用于导入整合包时复用、避免重复下载。
   *
   * @return 实例 id，没有匹配时返回 null
   */
  private String findInstalledInstance(
      File gameDir, String mcVersion, String loader, String loaderVersion) {
    String wanted = loader == null ? "" : loader.toLowerCase(Locale.ROOT);
    boolean anyLoader = "vanilla".equals(wanted) || "原版".equals(loader) || wanted.isBlank();
    for (String id : VersionProfile.installedIds(gameDir)) {
      if (id == null || !id.contains(mcVersion)) {
        continue;
      }
      String lower = id.toLowerCase(Locale.ROOT);
      boolean matches;
      if (anyLoader) {
        matches = !lower.contains("forge") && !lower.contains("fabric") && !lower.contains("quilt");
      } else {
        matches =
            switch (wanted) {
              case "fabric" -> lower.contains("fabric-loader");
              case "quilt" -> lower.contains("quilt");
              case "neoforge" -> lower.contains("neoforge");
              case "forge" -> lower.contains("forge") && !lower.contains("neoforge");
              default -> false;
            };
      }
      if (!matches) {
        continue;
      }
      if (loaderVersion != null
          && !loaderVersion.isBlank()
          && !id.contains(loaderVersion)
          && !lower.contains(loaderVersion.toLowerCase(Locale.ROOT))) {
        continue;
      }
      return id;
    }
    return null;
  }

  private void importModpack(File archive) {
    this.logLine("📦 开始导入整合包: " + archive.getName());
    this.importCancelled = false;
    InstallProgressPage window =
        new InstallProgressPage(
            "正在安装：" + archive.getName(),
            List.of("检查整合包", "准备游戏版本", "安装加载器", "解包文件", "完成"));
    window.setOnCancel(() -> this.importCancelled = true);
    window.setOnBack(() -> this.switchPage("instances"));
    // 安装进度页挂在下载页里，从实例页手动导入时先切过去，否则用户看不到进度
    if (!"download".equals(this.currentPage)) {
      this.switchPage("download");
    }
    this.showDownloadPanel(window.node());
    window.setStep(0, "run");
    window.setProgress(-1);

    // 把下载器的实时状态接到页面的「下载进度」条上（文件数 / 字节 / 速度 / 当前文件）
    final java.util.function.Consumer<DownloadManager.Status> dlListener =
        status ->
            Platform.runLater(
                () ->
                    window.setDownloadItems(
                        status.done(),
                        status.total(),
                        status.bytes(),
                        status.totalBytes(),
                        status.speedBps(),
                        status.file()));
    DownloadManager.addStatusListener(dlListener);
    final Runnable stopDlListener = () -> DownloadManager.removeStatusListener(dlListener);
    CompletableFuture.runAsync(
        () -> {
          File gameDir = new File(this.config.getGameDir());
          Consumer<String> sink =
              text -> {
                this.logLine(text);
                Platform.runLater(
                    () -> {
                      window.setStatus(text);
                      // Forge/NeoForge installer 会打印 "Downloading library from ..."，
                      // 这种下载不经过 DownloadManager，用不确定进度 + 文件名提示
                      if (text != null && text.contains("Downloading")) {
                        String url = text.substring(text.indexOf("Downloading"));
                        int fromIndex = text.indexOf("http");
                        window.setDownloadActivity(fromIndex >= 0 ? text.substring(fromIndex) : url);
                      }
                    });
              };

          // 失败自动重装一次；两次都失败就把这次新建的实例/版本清理掉，
          // 避免留下「只有原版、没有加载器或模组」的半成品。
          final int maxAttempts = 2;
          java.util.Set<String> versionsBefore =
              new java.util.HashSet<>(VersionProfile.installedIds(gameDir));

          for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            File staging = null;
            String instanceId = null;
            String createdInstance = null;
            ModpackImport.Pack pack = null;
            int fileCount = 0;
            try {
              if (attempt > 1) {
                sink.accept("♻ 上次安装失败，正在重新安装（第 " + attempt + " 次）…");
                Platform.runLater(
                    () -> {
                      window.setStep(0, "run");
                      window.setStep(1, "idle");
                      window.setStep(2, "idle");
                      window.setStep(3, "idle");
                      window.setStep(4, "idle");
                      window.setProgress(-1);
                    });
              }

              pack = ModpackImport.inspect(archive);
              this.checkImportCancelled();
              sink.accept(
                  "名称: "
                      + pack.name()
                      + "  |  格式: "
                      + pack.format()
                      + "  |  游戏: "
                      + (pack.gameVersion().isBlank() ? "未声明" : pack.gameVersion())
                      + (pack.hasLoader()
                          ? "  |  " + pack.loader() + " " + pack.loaderVersion()
                          : "  |  原版"));
              Platform.runLater(() -> window.setStep(0, "ok"));

              if (!pack.gameVersion().isBlank()) {
                Platform.runLater(() -> window.setStep(1, "run"));
                final String wantedGame = pack.gameVersion();
                // 已经装过同一个「MC 版本 + 加载器」就直接复用，避免重复导入时重新下载游戏与加载器
                String preinstalled =
                    this.findInstalledInstance(
                        gameDir, pack.gameVersion(), pack.loader(), pack.loaderVersion());
                MinecraftVersion mc =
                    this.allMinecraftVersions.stream()
                        .filter(v -> v.getId().equals(wantedGame))
                        .findFirst()
                        .orElse(null);
                if (preinstalled != null) {
                  instanceId = preinstalled;
                  sink.accept("✔ 已存在实例 " + preinstalled + "，跳过游戏/加载器下载");
                  Platform.runLater(() -> window.setStep(1, "ok"));
                } else if (mc == null) {
                  sink.accept("⚠ 版本清单里没有 " + pack.gameVersion() + "，改为导入到已有实例");
                  Platform.runLater(() -> window.setStep(1, "skip"));
                } else {
                  this.ensureInstalled(mc.getId(), mc.getUrl(), this::logLine);
                  instanceId = mc.getId();
                  Platform.runLater(() -> window.setStep(1, "ok"));
                }
              } else {
                sink.accept("整合包里没有声明游戏版本，需要选择一个已有实例");
                Platform.runLater(() -> window.setStep(1, "skip"));
              }

              // 没有可用实例时（无版本清单 / 包没声明版本），让用户从已安装实例里选一个
              if (instanceId == null) {
                instanceId = this.askImportTarget(pack.name());
                if (instanceId == null) {
                  throw new ImportCancelledException();
                }
                Platform.runLater(() -> window.setStep(1, "ok"));
              }

              if (pack.hasLoader() && pack.gameVersion().isBlank()) {
                Platform.runLater(() -> window.setStep(2, "skip"));
              } else if (pack.hasLoader() && !this.hasLoaderFor(gameDir, instanceId, pack.loader())) {
                Platform.runLater(() -> window.setStep(2, "run"));
                String mcTarget = instanceId;
                MinecraftVersion mc =
                    this.allMinecraftVersions.stream()
                        .filter(v -> v.getId().equals(mcTarget))
                        .findFirst()
                        .orElse(null);
                String mcId = mc == null ? instanceId : mc.getId();
                String javaPath = this.loaderJava(gameDir, pack.loader());
                instanceId =
                    switch (pack.loader()) {
                      case "fabric" ->
                          LoaderInstaller.installFabric(
                                  gameDir, mcId, pack.loaderVersion(), sink)
                              .get();
                      case "quilt" ->
                          LoaderInstaller.installQuilt(
                                  gameDir, mcId, pack.loaderVersion(), sink)
                              .get();
                      case "forge" ->
                          LoaderInstaller.installForge(
                                  gameDir, mcId, javaPath, pack.loaderVersion(), sink)
                              .get();
                      case "neoforge" ->
                          LoaderInstaller.installNeoForge(
                                  gameDir, mcId, javaPath, pack.loaderVersion(), sink)
                              .get();
                      default -> mcId;
                    };
                if (!versionsBefore.contains(instanceId)) {
                  createdInstance = instanceId;
                }
                Platform.runLater(() -> window.setStep(2, "ok"));
              } else {
                Platform.runLater(() -> window.setStep(2, "skip"));
              }

              // 导入：加载器实例依赖原版父版本，缺了后面必然起不来
              this.ensureParentVersion(gameDir, instanceId, sink);
              this.checkImportCancelled();
              File target = InstanceDirs.root(gameDir, instanceId, true);
              // 先解到临时目录，全部成功后再合并：中途失败不会污染实例目录
              File stageRoot = new File(gameDir, "cache");
              stageRoot.mkdirs();
              staging = java.nio.file.Files.createTempDirectory(stageRoot.toPath(), "modpack-").toFile();
              Platform.runLater(
                  () -> {
                    window.setStep(3, "run");
                    window.setStatus("正在解包 " + archive.getName() + "…");
                  });
              int count = ModpackImport.extract(archive, staging, sink);
              this.checkImportCancelled();
              count += ModpackImport.installIndexedFiles(archive, staging, sink);
              ModpackImport.flattenMods(staging, sink);
              this.checkImportCancelled();

              // 合并到实例目录
              if (!target.isDirectory() && !target.mkdirs()) {
                throw new RuntimeException("无法创建实例目录: " + target.getAbsolutePath());
              }
              copyTree(staging, target);
              fileCount = count;
              Platform.runLater(() -> window.setStep(3, "ok"));

              Files.writeString(
                  new File(target, "modpack-source.txt").toPath(),
                  "name="
                      + pack.name()
                      + "\nformat="
                      + pack.format()
                      + "\narchive="
                      + archive.getAbsolutePath()
                      + "\n",
                  StandardCharsets.UTF_8);
              InstanceMeta.set(gameDir, instanceId, pack.name(), pack.format() + " 导入");
              sink.accept(
                  "导入完成: " + pack.name() + " → " + target.getAbsolutePath() + "（" + count + " 个文件）");

              // 安装成功后删掉安装包（只删启动器自己下载目录里的，不动用户自己的文件）
              deletePackArchive(archive);

              String importedId = instanceId;
              String packName = pack.name();
              int installedFiles = count;
              Platform.runLater(
                  () -> {
                    stopDlListener.run();
                    window.clearDownload();
                    window.setStep(4, "ok");
                    window.finish(
                        true, "安装完成，共 " + installedFiles + " 个文件，点左上角「返回」去实例列表");
                    this.refreshInstalledVersions();
                    Toast.ok("已导入整合包: " + packName);
                    this.config.setLastInstance(importedId);
                    this.config.save();
                    if (this.instancesPanel != null) {
                      this.instancesPanel.setSelectedId(importedId);
                    }
                  });
              return;
            } catch (ImportCancelledException cancelled) {
              this.importCancelled = false;
              deleteTree(staging);
              rollbackCreatedInstance(gameDir, instanceId, createdInstance);
              Platform.runLater(
                  () -> {
                    stopDlListener.run();
                    window.clearDownload();
                    window.finish(false, "已取消");
                  });
              return;
            } catch (Exception e) {
              deleteTree(staging);
              String message = rootMessage(e);
              this.logLine("❌ 第 " + attempt + " 次安装失败: " + message);
              for (Throwable cause = e.getCause(); cause != null; cause = cause.getCause()) {
                if (cause.getMessage() != null && !message.contains(cause.getMessage())) {
                  this.logLine("   ↳ " + cause.getMessage());
                }
              }
              rollbackCreatedInstance(gameDir, instanceId, createdInstance);
              if (attempt >= maxAttempts) {
                String hint = importFailureHint(message);
                this.logLine("❌ 导入失败: " + message + (hint.isEmpty() ? "" : "（" + hint + "）"));
                String finalMessage = message;
                Platform.runLater(
                    () -> {
                      stopDlListener.run();
                      window.clearDownload();
                      window.finish(
                          false,
                          "导入失败（已重试 " + maxAttempts + " 次）：" + finalMessage
                              + (hint.isEmpty() ? "" : " · " + hint));
                      Toast.error("导入失败：" + finalMessage);
                    });
                return;
              }
              try {
                Thread.sleep(800L);
              } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
              }
            }
          }
        });
  }

  /** 取最内层异常的消息，避免只显示 "RuntimeException: ..." 这种壳。 */
  private static String rootMessage(Throwable error) {
    return ImportErrors.rootMessage(error);
  }

  /** 按异常类型给出可操作提示。 */
  private static String importFailureHint(String message) {
    return ImportErrors.hint(message);
  }

  /** 递归复制目录内容（覆盖同名文件）。 */
  private static void copyTree(File from, File to) throws java.io.IOException {
    if (from == null || !from.isDirectory()) {
      return;
    }
    java.nio.file.Path base = from.toPath();
    try (java.util.stream.Stream<java.nio.file.Path> walk = java.nio.file.Files.walk(base)) {
      for (java.nio.file.Path path : walk.toList()) {
        java.nio.file.Path relative = base.relativize(path);
        File target = ModpackImport.safeTarget(to, relative.toString());
        if (target == null) continue;
        if (java.nio.file.Files.isDirectory(path)) {
          target.mkdirs();
        } else {
          File parent = target.getParentFile();
          if (parent != null) {
            parent.mkdirs();
          }
          java.nio.file.Files.copy(path, target.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
      }
    }
  }

  /** 静默删除临时目录。 */
  private static void deleteTree(File dir) {
    InstanceFiles.deleteTree(dir);
  }

  /** 失败时把这次安装新建的实例和版本目录清掉，避免留下半成品实例。 */
  private void rollbackCreatedInstance(File gameDir, String instanceId, String createdVersion) {
    if (instanceId == null) {
      return;
    }
    try {
      File instanceDir = InstanceDirs.root(gameDir, instanceId, true);
      deleteTree(instanceDir);
      this.logLine("↩ 已清理本次创建的实例目录: " + instanceId);
    } catch (Exception ignored) {
      // 忽略
    }
    if (createdVersion != null) {
      deleteTree(new File(new File(gameDir, "versions"), createdVersion));
      this.logLine("↩ 已清理本次安装的加载器版本: " + createdVersion);
    }
  }

  /** 安装成功后删除安装包：只删启动器下载目录里的，用户自己选的文件不动。 */
  private void deletePackArchive(File archive) {
    if (archive == null || !archive.isFile()) {
      return;
    }
    try {
      String packDir =
          new File(new File(this.config.getGameDir(), "downloads"), "modpack")
              .getCanonicalPath();
      if (archive.getCanonicalPath().startsWith(packDir)) {
        if (archive.delete()) {
          this.logLine("🧹 已删除安装包: " + archive.getName());
        }
      } else {
        this.logLine("ℹ 安装包不在启动器下载目录，保留原文件: " + archive.getAbsolutePath());
      }
    } catch (Exception ignored) {
      // 删除失败不影响安装结果
    }
  }

  /**
   * 加载器实例的版本 JSON 会 inheritsFrom 原版；原版缺失时游戏直接起不来。
   * 这里在导入/启动前检查并自动补装父版本。
   */
  private void ensureParentVersion(File gameDir, String instanceId, Consumer<String> log) {
    if (instanceId == null || instanceId.isBlank()) {
      return;
    }
    File json = VersionProfile.jsonFile(gameDir, instanceId);
    if (!json.isFile()) {
      return;
    }
    try {
      JsonObject obj =
          JsonParser.parseString(Files.readString(json.toPath(), StandardCharsets.UTF_8))
              .getAsJsonObject();
      if (!obj.has("inheritsFrom")) {
        return;
      }
      String parent = obj.get("inheritsFrom").getAsString();
      if (VersionProfile.jsonFile(gameDir, parent).isFile()) {
        return;
      }
      MinecraftVersion mc =
          this.allMinecraftVersions.stream()
              .filter(v -> v.getId().equals(parent))
              .findFirst()
              .orElse(null);
      if (mc == null) {
        log.accept("⚠ 缺少父版本 " + parent + "，版本清单里也没有，无法自动补装");
        return;
      }
      log.accept("🧩 实例缺少父版本 " + parent + "，正在自动补装…");
      this.ensureInstalled(mc.getId(), mc.getUrl(), log);
      log.accept("✔ 父版本 " + parent + " 已补装");
    } catch (Exception e) {
      log.accept("⚠ 检查父版本失败: " + e.getMessage());
    }
  }

  /** 该实例是否已经装上了对应的加载器（避免重复安装）。 */
  private boolean hasLoaderFor(File gameDir, String instanceId, String loader) {
    if (instanceId == null) {
      return false;
    }
    String lower = instanceId.toLowerCase(Locale.ROOT);
    return switch (loader == null ? "" : loader) {
      case "fabric" -> lower.contains("fabric");
      case "quilt" -> lower.contains("quilt");
      case "neoforge" -> lower.contains("neoforge");
      case "forge" -> lower.contains("forge") && !lower.contains("neoforge");
      default -> false;
    };
  }

  /**
   * 让用户挑一个已有实例作为导入目标（在后台线程里阻塞等待对话框）。
   *
   * @return 选中的实例 id；取消返回 null
   */
  private String askImportTarget(String packName) {
    List<String> ids = VersionProfile.installedIds(new File(this.config.getGameDir()));
    if (ids.isEmpty()) {
      this.logLine("❌ 还没有可用的游戏版本，请先在下载页安装一个版本，或导入一个声明了游戏版本的整合包");
      return null;
    }
    java.util.concurrent.CompletableFuture<String> future = new java.util.concurrent.CompletableFuture<>();
    Platform.runLater(
        () -> {
          javafx.scene.control.ChoiceDialog<String> dialog =
              new javafx.scene.control.ChoiceDialog<>(ids.get(0), ids);
          dialog.setTitle("选择导入目标");
          dialog.setHeaderText("「" + packName + "」没有可用的游戏版本");
          dialog.setContentText("导入到哪个实例：");
          dialog.setGraphic(null);
          if (this.stageRef != null) {
            dialog.initOwner(this.stageRef);
          }
          Theme.applyDialogStyles(dialog.getDialogPane());
          dialog.setOnHidden(e -> future.complete(dialog.getResult()));
          dialog.show();
        });
    try {
      return future.get(5, TimeUnit.MINUTES);
    } catch (Exception e) {
      return null;
    }
  }


  private void checkImportCancelled() {
    if (this.importCancelled) {
      throw new ImportCancelledException();
    }
  }

  private String loaderJava(File gameDir, String loader) {
    int major = "neoforge".equals(loader) ? 21 : 17;
    File bundled = new File(new File(new File(gameDir, "java"), "zulu" + major), "bin/java.exe");
    if (bundled.isFile()) {
      return bundled.getAbsolutePath();
    }
    if (this.config.getJavaPath() != null && !this.config.getJavaPath().isBlank()) {
      return this.config.getJavaPath();
    }
    try {
      String resolved =
          JavaManager.ensureJavaAsync(gameDir, major, this::logLine).get(300L, TimeUnit.SECONDS);
      if (resolved != null && new File(resolved).isFile()) {
        return resolved;
      }
    } catch (Exception e) {
      this.logLine("⚠ 内置 Java " + major + " 下载失败，改用启动器自带的运行时执行安装器");
    }
    // 下载失败时别返回一个不存在的路径，否则 ProcessBuilder 直接 CreateProcess error=2
    File runtime = new File(System.getProperty("java.home", ""), "bin/java.exe");
    if (runtime.isFile()) {
      return runtime.getAbsolutePath();
    }
    return bundled.getAbsolutePath();
  }

  private void downloadFileAsync(String url, File targetFile, TextArea logArea) {
    this.downloadFileAsync(url, targetFile, logArea, null);
  }

  private void downloadFileAsync(
      String url, File targetFile, TextArea logArea, Consumer<File> onDone) {
    downloadFileAsync(url, targetFile, logArea, onDone, null);
  }

  private void downloadFileAsync(
      String url,
      File targetFile,
      TextArea logArea,
      Consumer<File> onDone,
      Consumer<String> onFail) {
    logArea.appendText("📥 正在下载: " + targetFile.getName() + "\n");
    CompletableFuture.runAsync(
        () -> {
          try {
            DownloadManager.Stats stats =
                DownloadManager.downloadAll(
                    List.of(new DownloadManager.Task(url, targetFile, null, -1L)), 1, null);
            long size = targetFile.length();
            Platform.runLater(
                () -> {
                  if (stats.failed > 0 || size <= 0L) {
                    logArea.appendText("❌ 下载失败: " + targetFile.getName() + "\n");
                    if (onFail != null) {
                      onFail.accept("下载失败：" + targetFile.getName());
                    }
                    return;
                  }
                  this.lastDownloadedFile = targetFile;
                  logArea.appendText("✅ 下载完成: " + targetFile.getAbsolutePath() + "\n");
                  Toast.ok("下载完成：" + targetFile.getName() + "，点右上角下载按钮查看位置");
                  if (onDone != null) {
                    onDone.accept(targetFile);
                  }
                });
          } catch (Exception e) {
            Platform.runLater(
                () -> {
                  logArea.appendText("❌ 下载失败: " + e.getMessage() + "\n");
                  if (onFail != null) {
                    onFail.accept("下载失败：" + e.getMessage());
                  }
                });
          }
        });
  }

  private void applySessionUi(boolean loggedIn) {
    if (this.loginCard == null) {
      return;
    }
    this.loginCard.setVisible(!loggedIn);
    this.loginCard.setManaged(!loggedIn);
    this.userTypeLabel.setVisible(false);
    this.userTypeLabel.setManaged(false);
    this.uuidLabel.setVisible(!loggedIn);
    this.uuidLabel.setManaged(!loggedIn);
    this.logoutButton.setVisible(loggedIn);
    this.logoutButton.setManaged(loggedIn);
    if (this.accountInfo != null) {
      this.accountInfo.setVisible(loggedIn);
      this.accountInfo.setManaged(loggedIn);
    }
  }

  private void updateInstanceInfo(String instanceId) {
    if (this.accountInfo != null) {
      this.accountInfo.updateInstance(instanceId);
    }
  }

  private void updatePlayerInfo(String type, String server, UserSession session) {
    if (this.accountInfo == null) {
      return;
    }
    String uuid =
        session != null && session.getUuid() != null
            ? session.getUuid().toString().substring(0, 13) + "…"
            : "—";
    String host = server == null ? "" : server;
    try {
      String parsed;
      if (host.startsWith("http") && (parsed = URI.create(host).getHost()) != null) {
        host = parsed;
      }
    } catch (Exception parsed) {
      // empty catch block
    }
    this.accountInfo.updateAccount(type, uuid, host);
    String instance = this.currentInstanceId();
    this.accountInfo.updateInstance(instance);
    long seconds = 0L;
    try {
      if (instance != null) {
        seconds = Playtime.seconds(new File(this.config.getGameDir()), instance);
      }
    } catch (Exception exception) {
      // empty catch block
    }
    this.accountInfo.updatePlaytime(seconds);
  }

  private void setGameStatus(boolean running) {
    if (this.stopGameButton != null) {
      this.stopGameButton.setVisible(running);
      this.stopGameButton.setManaged(running);
    }
    if (this.gameStatusDot == null) {
      return;
    }
    this.gameStatusDot.setFill(javafx.scene.paint.Color.web(running ? "#4CAF50" : "#6b7280"));
    this.gameStatusDot.setOpacity(1.0);
    if (this.gameStatusLabel != null) {
      this.gameStatusLabel.setText(running ? "游戏运行中" : "就绪");
    }
    if (this.gameStatusPulse != null) {
      this.gameStatusPulse.stop();
      this.gameStatusPulse = null;
    }
    if (running && Motion.isEnabled()) {
      this.gameStatusPulse = new FadeTransition(Duration.seconds(1.1), this.gameStatusDot);
      this.gameStatusPulse.setFromValue(1.0);
      this.gameStatusPulse.setToValue(0.35);
      this.gameStatusPulse.setAutoReverse(true);
      this.gameStatusPulse.setCycleCount(-1);
      this.gameStatusPulse.play();
    }
  }

  private boolean applyAvatarOverride() {
    String path;
    if (this.config != null && this.config.isAvatarUseSkin()) {
      return false;
    }
    String string = path = this.config == null ? "" : this.config.getAvatarImage();
    if (path == null || path.isBlank()) {
      return false;
    }
    File file = new File(path);
    if (!file.isFile()) {
      return false;
    }
    try {
      Image image = new Image(file.toURI().toString(), 160.0, 160.0, true, true);
      if (image.isError()) {
        return false;
      }
      this.avatarImageView.setImage(image);
      return true;
    } catch (Exception ignored) {
      return false;
    }
  }

  private void toggleAvatarMode() {
    if (this.currentSession == null
        || this.currentSession.getSkinUrl() == null
        || this.currentSession.getSkinUrl().isBlank()) {
      this.chooseAvatar();
      return;
    }
    this.config.setAvatarUseSkin(!this.config.isAvatarUseSkin());
    this.config.save();
    if (this.config.isAvatarUseSkin()) {
      this.loadSkinHead(this.currentSession.getSkinUrl());
      Toast.ok("已切换为皮肤头像");
    } else if (!this.applyAvatarOverride()) {
      this.avatarImageView.setImage(null);
      Toast.ok("暂无自定义头像");
    } else {
      Toast.ok("已切换为自定义头像");
    }
  }

  private void chooseAvatar() {
    FileChooser chooser = new FileChooser();
    chooser.setTitle("选择头像图片");
    chooser
        .getExtensionFilters()
        .add(
            new FileChooser.ExtensionFilter(
                "图片 (*.png, *.jpg, *.jpeg)", "*.png", "*.jpg", "*.jpeg"));
    File file = chooser.showOpenDialog(this.stageRef);
    if (file == null) {
      return;
    }
    try {
      Image image = new Image(file.toURI().toString(), 160.0, 160.0, true, true);
      if (image.isError()) {
        Toast.error("头像图片读取失败");
        return;
      }
      this.avatarImageView.setImage(image);
      this.config.setAvatarImage(file.getAbsolutePath());
      this.config.save();
      Toast.ok("头像已更新");
    } catch (Exception ex) {
      Toast.error("头像图片读取失败");
    }
  }

  private LoginCardData parseLoginCard(String text) {
    return LoginCardParser.parse(text);
  }

  private String firstJsonString(JsonObject object, String... keys) {
    for (String key : keys) {
      if (!object.has(key) || object.get(key).isJsonNull() || !object.get(key).isJsonPrimitive())
        continue;
      return object.get(key).getAsString();
    }
    return "";
  }

  private String normalizeLittleSkinServer(String server) {
    return LoginCardParser.normalizeServer(server);
  }

  private void showHealthCheck() {
    File[] reports;
    String id = this.currentInstanceId();
    File game = new File(this.config.getGameDir());
    boolean versionOk = id != null && VersionProfile.isInstalled(game, id);
    File mods = InstanceDirs.mods(game, id, this.isIsolated(id));
    int modCount = 0;
    File[] jars =
        mods.listFiles(
            (d, n) -> {
              String lower = n.toLowerCase(Locale.ROOT);
              return lower.endsWith(".jar") || lower.endsWith(".jar.disabled");
            });
    if (jars != null) {
      modCount = jars.length;
    }
    boolean hasCrash =
        (reports = new File(game, "crash-reports").listFiles((d, n) -> n.endsWith(".txt"))) != null
            && reports.length > 0;
    List<String> conflicts = ModConflictChecker.check(mods);
    Dialog<ButtonType> dialog = new Dialog<>();
    dialog.initOwner(this.stageRef);
    dialog.setTitle("一键体检");
    dialog.setHeaderText(id == null ? "还没有选择实例" : "实例 " + id);
    VBox rows =
        new VBox(
            8.0,
            this.healthRow(
                versionOk ? "正常" : "缺失", versionOk ? "版本文件存在" : "版本 JSON / 客户端文件不完整", versionOk),
            this.healthRow("正常", "Java 由启动器自动选择", true),
            this.healthRow(modCount > 0 || versionOk ? "正常" : "提示", modCount + " 个模组文件", versionOk),
            this.healthRow(
                hasCrash ? "有记录" : "正常",
                (hasCrash ? "发现 " + reports.length + " 条崩溃记录" : "未发现崩溃记录"),
                !hasCrash),
            this.healthRow(
                conflicts.isEmpty() ? "正常" : "冲突",
                (conflicts.isEmpty()
                    ? "未发现重复 modId"
                    : "发现 " + conflicts.size() + " 个模组冲突：" + conflicts.get(0)),
                conflicts.isEmpty()));
    Button fullCheck = new Button("运行完整性检查");
    fullCheck.setDisable(id == null);
    fullCheck.setOnAction(
        e -> {
          if (id != null) {
            this.checkInstance(Instance.of(game, id));
          }
        });
    Button openCrash = new Button("打开崩溃目录");
    openCrash.setDisable(!hasCrash);
    openCrash.setOnAction(e -> this.openDir(new File(game, "crash-reports")));
    HBox actions = new HBox(8.0, fullCheck, openCrash);
    dialog.getDialogPane().setContent(new VBox(12.0, rows, actions));
    dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
    dialog.showAndWait();
  }

  private HBox healthRow(String level, String message, boolean ok) {
    Label dot = new Label(ok ? "●" : "●");
    dot.setStyle("-fx-text-fill: " + (ok ? "#4CAF50" : "#FF9800") + "; -fx-font-size: 14px;");
    Label levelLabel = new Label(level);
    levelLabel.setMinWidth(48.0);
    levelLabel.setStyle(
        "-fx-text-fill: "
            + (ok ? "#4CAF50" : "#FF9800")
            + "; -fx-font-size: 11px; -fx-font-weight: bold;");
    Label text = new Label(message);
    text.setStyle("-fx-text-fill: -app-text-2; -fx-font-size: 12px;");
    HBox row = new HBox(8.0, dot, levelLabel, text);
    row.setAlignment(Pos.CENTER_LEFT);
    row.setPadding(new Insets(7.0, 10.0, 7.0, 10.0));
    row.setStyle("-fx-background-color: -app-row2; -fx-background-radius: 8;");
    return row;
  }

  private void showAccountManager() {
    AccountManagerDialog.show(
        this.stageRef,
        this::switchAccount,
        account -> AccountStore.remove(account.username()),
        this::logout);
  }

  private void switchAccount(AccountStore.Account account) {
    SessionState.Restored restored = SessionState.fromAccount(account);
    if (restored == null) {
      return;
    }
    this.currentSession = restored.session();
    SessionStore.save(restored.session(), restored.loginType(), restored.server());
    this.usernameLabel.setText(account.username());
    this.applySessionUi(true);
    this.userTypeLabel.setText(restored.displayType());
    this.userTypeLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #4CAF50;");
    this.updatePlayerInfo(restored.displayType(), restored.server(), restored.session());
    String shortUuid = SessionState.shortUuid(restored.session());
    this.uuidLabel.setText(shortUuid.isEmpty() ? "" : "UUID: " + shortUuid);
    if (this.loginTypeComboRef != null) {
      this.loginTypeComboRef.setValue(restored.loginType());
    }
    if (this.serverFieldRef != null && !restored.server().isBlank()) {
      this.serverFieldRef.getEditor().setText(restored.server());
    }
    this.config.setPreviewSkinPath(account.skinPath());
    this.config.setPreviewCapePath(account.capePath());
    this.config.save();
    if (account.skinUrl() != null && !account.skinUrl().isBlank()) {
      this.loadSkinHead(account.skinUrl());
    }
    Toast.ok("已切换到账户：" + account.username());
    this.logMessage("👤 已切换账户: " + account.username());
  }

  private void logout() {
    this.currentSession = null;
    SessionStore.clear();
    this.usernameLabel.setText("未登录");
    this.userTypeLabel.setText("请在下方登录");
    this.uuidLabel.setText("");
    if (!this.applyAvatarOverride()) {
      this.avatarImageView.setImage(null);
    }
    this.loginForm.password().clear();
    this.loginForm.loginButton().setDisable(false);
    this.loginForm.loginLabel().setText("登录");
    this.loginForm.typeCombo().setValue("离线登录");
    this.applySessionUi(false);
    this.logMessage("已退出登录");
  }

  private void restoreSessionFromRegistry() {
    this.applySessionUi(false);
    SessionState.Restored restored = SessionState.fromSaved(SessionStore.load());
    if (restored == null) {
      return;
    }
    try {
      this.currentSession = restored.session();
      this.usernameLabel.setText(restored.session().getUsername());
      this.applySessionUi(true);
      this.userTypeLabel.setText(restored.displayType());
      this.updatePlayerInfo(restored.displayType(), restored.server(), restored.session());
      this.userTypeLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #4CAF50;");
      String shortUuid = SessionState.shortUuid(restored.session());
      this.uuidLabel.setText(shortUuid.isEmpty() ? "" : "UUID: " + shortUuid);
      if (this.loginTypeComboRef != null) {
        this.loginTypeComboRef.setValue(
            this.loginTypeComboRef.getItems().contains(restored.loginType())
                ? restored.loginType()
                : "离线登录");
        boolean thirdParty = "第三方外置登录".equals(restored.loginType());
        if (this.serverFieldRef != null) {
          this.serverFieldRef.setVisible(thirdParty);
          this.serverFieldRef.setManaged(thirdParty);
          if (!restored.server().isBlank()) {
            this.serverFieldRef.getEditor().setText(restored.server());
          }
        }
      }
      String skinUrl = restored.session().getSkinUrl();
      if (skinUrl != null && !skinUrl.isBlank()) {
        this.loadSkinHead(skinUrl);
      }
      this.logArea.appendText(
          "🔐 已从本机注册表恢复登录: " + restored.session().getUsername() + "\n");
    } catch (Exception e) {
      this.logArea.appendText("⚠ 登录状态恢复失败: " + e.getMessage() + "\n");
    }
  }

  private void handleLogin(
      ComboBox<String> loginTypeCombo,
      ComboBox<String> serverField,
      TextField usernameField,
      PasswordField passwordField,
      Button loginBtn) {
    String type = loginTypeCombo.getValue();
    String username = usernameField.getText().trim();
    String serverUrl = serverField.getEditor().getText().trim();
    String password = passwordField.getText().trim();
    if (!type.equals("正版登录 (微软)") && username.isEmpty()) {
      Toast.warn("请输入账号或玩家名");
      usernameField.requestFocus();
      return;
    }
    if (type.equals("第三方外置登录") && (serverUrl.isEmpty() || password.isEmpty())) {
      Toast.warn(serverUrl.isEmpty() ? "请填写验证服务器地址" : "请填写密码");
      if (serverUrl.isEmpty()) {
        serverField.requestFocus();
      } else {
        passwordField.requestFocus();
      }
      return;
    }
    loginBtn.setDisable(true);
    this.logArea.appendText("正在以 [" + type + "] 登录...\n");

    AuthManager.loginAsync(
            type,
            username,
            password,
            serverUrl,
            msg -> Platform.runLater(() -> this.logArea.appendText(msg + "\n")))
        .thenAccept(
            session ->
                Platform.runLater(
                    () -> {
                      loginBtn.setDisable(false);
                      if (session != null) {
                        this.usernameLabel.setText(session.getUsername());
                        this.applySessionUi(true);
                        this.userTypeLabel.setText(type);
                        this.updatePlayerInfo(type, serverUrl, session);
                        this.userTypeLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #4CAF50;");
                        this.uuidLabel.setText("UUID: " + SessionState.shortUuid(session));
                        this.currentSession = session;
                        SessionStore.save(session, type, serverUrl);
                        AccountStore.upsert(session, type, serverUrl);
                        if (this.versionComboBox.getItems() != null
                            && !this.versionComboBox.getItems().isEmpty()
                            && this.versionComboBox.getValue() == null) {
                          this.versionComboBox.getSelectionModel().selectFirst();
                        }
                        if (session.getSkinUrl() != null) {
                          this.loadSkinHead(session.getSkinUrl());
                        }
                      }
                    }))
        .exceptionally(
            ex -> {
              Platform.runLater(
                  () -> {
                    loginBtn.setDisable(false);
                    this.logArea.appendText("❌ 登录取消，请检查网络或服务器地址后重试。\n");
                  });
              return null;
            });
  }

  private void loadSkinHead(String skinUrl) {
    this.logArea.appendText("🖼️ 正在加载并裁剪头部头像...\n");
    Image skinImage = new Image(skinUrl, true);
    skinImage
        .progressProperty()
        .addListener(
            (obs, oldVal, newVal) -> {
              if (newVal.doubleValue() == 1.0 && !skinImage.isError()) {
                try {
                  int scale = (int) skinImage.getWidth() / 64;
                  if (scale < 1) {
                    scale = 1;
                  }
                  WritableImage croppedFace =
                      new WritableImage(
                          skinImage.getPixelReader(), 8 * scale, 8 * scale, 8 * scale, 8 * scale);
                  int targetSize = 100;
                  WritableImage clearFace = new WritableImage(targetSize, targetSize);
                  PixelReader reader = croppedFace.getPixelReader();
                  PixelWriter writer = clearFace.getPixelWriter();
                  for (int y = 0; y < targetSize; ++y) {
                    for (int x = 0; x < targetSize; ++x) {
                      writer.setColor(
                          x,
                          y,
                          reader.getColor(x * 8 * scale / targetSize, y * 8 * scale / targetSize));
                    }
                  }
                  if (!this.applyAvatarOverride()) {
                    this.avatarImageView.setImage(clearFace);
                  }
                  this.logArea.appendText("🎨 高清头部贴图加载成功\n");
                } catch (Exception ex) {
                  this.logArea.appendText("❌ 头像裁剪失败: " + ex.getMessage() + "\n");
                }
              }
            });
  }

  private void openBrowser(String url) {
    try {
      Desktop.getDesktop().browse(URI.create(url));
    } catch (Exception e) {
      this.logMessage("❌ 打开浏览器失败: " + e.getMessage());
    }
  }

  /** 取消整合包导入：停下载、复位按钮、记一条日志。 */
  private void cancelImport() {
    this.importCancelled = true;
    DownloadManager.cancelAll();
    if (this.importCancelButton != null) {
      this.importCancelButton.setDisable(true);
      this.importCancelButton.setText("正在取消…");
    }
    this.logMessage("⏹ 已请求取消整合包导入");
  }

  private void showDownloadTasks() {
    DownloadTasksDialog.show(
        this.stageRef,
        DownloadManager.status(),
        this.lastDownloadedFile,
        this::openDir);
  }

  private static void clampStagePosition(Stage stage) {
    Rectangle2D vb = HelloApplication.screenBounds(stage);
    double w = Math.max(1.0, stage.getWidth());
    double h = Math.max(1.0, stage.getHeight());
    double minVisible = 180.0;
    double maxX = Math.max(vb.getMinX(), vb.getMaxX() - Math.min(minVisible, w));
    double maxY = Math.max(vb.getMinY(), vb.getMaxY() - 48.0);
    double x = Math.max(vb.getMinX(), Math.min(stage.getX(), maxX));
    double y = Math.max(vb.getMinY(), Math.min(stage.getY(), maxY));
    if (x != stage.getX()) {
      stage.setX(x);
    }
    if (y != stage.getY()) {
      stage.setY(y);
    }
  }

  private static Rectangle2D screenBounds(Stage stage) {
    Screen screen =
        Screen.getScreensForRectangle(
                stage.getX(), stage.getY(), Math.max(1.0, stage.getWidth()), 1.0)
            .stream()
            .findFirst()
            .orElse(Screen.getPrimary());
    return screen.getVisualBounds();
  }

  private void installResizeHandles(StackPane wrap, Stage stage) {
    String[] cursors =
        new String[] {
          "N_RESIZE",
          "S_RESIZE",
          "W_RESIZE",
          "E_RESIZE",
          "NW_RESIZE",
          "NE_RESIZE",
          "SW_RESIZE",
          "SE_RESIZE"
        };
    for (int i = 0; i < cursors.length; ++i) {
      boolean right;
      String cursorName = cursors[i];
      Region handle = new Region();
      Cursor cursor =
          switch (cursorName) {
            case "N_RESIZE" -> Cursor.N_RESIZE;
            case "S_RESIZE" -> Cursor.S_RESIZE;
            case "W_RESIZE" -> Cursor.W_RESIZE;
            case "E_RESIZE" -> Cursor.E_RESIZE;
            case "NW_RESIZE" -> Cursor.NW_RESIZE;
            case "NE_RESIZE" -> Cursor.NE_RESIZE;
            case "SW_RESIZE" -> Cursor.SW_RESIZE;
            default -> Cursor.SE_RESIZE;
          };
      handle.setCursor(cursor);
      handle.setStyle("-fx-background-color: transparent;");
      handle.setPickOnBounds(true);
      boolean top = cursorName.startsWith("N");
      boolean bottom = cursorName.startsWith("S");
      boolean left =
          cursorName.startsWith("W")
              || cursorName.equals("NW_RESIZE")
              || cursorName.equals("SW_RESIZE");
      boolean bl =
          right =
              cursorName.startsWith("E")
                  || cursorName.equals("NE_RESIZE")
                  || cursorName.equals("SE_RESIZE");
      if (cursorName.equals("N_RESIZE") || cursorName.equals("S_RESIZE")) {
        handle.setMinHeight(5.0);
        handle.setPrefHeight(5.0);
        handle.setMaxHeight(5.0);
        handle.setMaxWidth(Double.MAX_VALUE);
      } else if (cursorName.equals("W_RESIZE") || cursorName.equals("E_RESIZE")) {
        handle.setMinWidth(5.0);
        handle.setPrefWidth(5.0);
        handle.setMaxWidth(5.0);
        handle.setMaxHeight(Double.MAX_VALUE);
      } else {
        handle.setMinSize(12.0, 12.0);
        handle.setPrefSize(12.0, 12.0);
        handle.setMaxSize(12.0, 12.0);
      }
      if (top) {
        StackPane.setAlignment(
            handle, left ? Pos.TOP_LEFT : (right ? Pos.TOP_RIGHT : Pos.TOP_CENTER));
      } else if (bottom) {
        StackPane.setAlignment(
            handle, left ? Pos.BOTTOM_LEFT : (right ? Pos.BOTTOM_RIGHT : Pos.BOTTOM_CENTER));
      } else {
        StackPane.setAlignment(handle, left ? Pos.CENTER_LEFT : Pos.CENTER_RIGHT);
      }
      if (top) {
        StackPane.setMargin(handle, new Insets(0.0, 0.0, 0.0, 0.0));
      }
      double[] start = new double[6];
      handle.setOnMousePressed(
          e -> {
            if (stage.isMaximized()) {
              return;
            }
            start[0] = e.getScreenX();
            start[1] = e.getScreenY();
            start[2] = stage.getX();
            start[3] = stage.getY();
            start[4] = stage.getWidth();
            start[5] = stage.getHeight();
            e.consume();
          });
      handle.setOnMouseDragged(
          e -> {
            if (stage.isMaximized()) {
              return;
            }
            double dx = e.getScreenX() - start[0];
            double dy = e.getScreenY() - start[1];
            double minW = 980.0;
            double minH = 640.0;
            Rectangle2D vb = HelloApplication.screenBounds(stage);
            double maxW = vb.getWidth();
            double maxH = vb.getHeight();
            if (left) {
              double w = Math.min(maxW, Math.max(minW, start[4] - dx));
              double nx = start[2] + (start[4] - w);
              stage.setX(Math.max(vb.getMinX(), nx));
              stage.setWidth(w);
            } else if (right) {
              stage.setWidth(Math.min(maxW, Math.max(minW, start[4] + dx)));
            }
            if (top) {
              double h = Math.min(maxH, Math.max(minH, start[5] - dy));
              double ny = start[3] + (start[5] - h);
              stage.setY(Math.max(vb.getMinY(), ny));
              stage.setHeight(h);
            } else if (bottom) {
              stage.setHeight(Math.min(maxH, Math.max(minH, start[5] + dy)));
            }
            e.consume();
          });
      wrap.getChildren().add(handle);
    }
  }

  private void openDir(File dir) {
    try {
      if (!dir.isDirectory() && !dir.mkdirs()) {
        this.logArea.appendText("❌ 目录不存在且无法创建: " + dir.getAbsolutePath() + "\n");
        return;
      }
      Desktop.getDesktop().open(dir);
    } catch (Exception e) {
      try {
        new ProcessBuilder("explorer.exe", dir.getAbsolutePath()).start();
      } catch (Exception ex) {
        this.logArea.appendText("❌ 打开目录失败: " + e.getMessage() + "\n");
      }
    }
  }

  /** 运行中「■ 停止」按钮：取消启动或结束本次运行状态。 */
  private void stopLaunch() {
    if (!this.launchState.isLaunching()) {
      return;
    }
    this.launchState.cancel();
    this.logLine("⏹ 正在停止…");
    // 先收起顶栏的取消按钮，等取消真正生效时再由 finishLaunching 恢复主按钮
    if (this.cancelLaunchButton != null) {
      this.cancelLaunchButton.setVisible(false);
      this.cancelLaunchButton.setManaged(false);
    }
  }

  /** 统一切换启动按钮与停止按钮的显示状态。 */
  private void setLaunchingUi(boolean launching) {
    if (this.accountColumn != null) {
      this.accountColumn.launchButton().setLaunching(launching);
    }
    if (this.cancelLaunchButton != null) {
      // 只在「启动准备中」显示；游戏进程已经起来后就没有可取消的准备了
      boolean showCancel = launching && !this.launchState.isRunning();
      this.cancelLaunchButton.setVisible(showCancel);
      this.cancelLaunchButton.setManaged(showCancel);
    }
  }

  /** 当前是否已经有游戏进程在跑（用于阻止重复启动）。 */
  private boolean isGameProcessAlive() {
    return this.launchState.isRunning() && this.gameProcess != null && this.gameProcess.isAlive();
  }

  /** 启动入口的统一检查：已经在启动或已有游戏在跑时，给出提示并拦下。 */
  private boolean blockIfAlreadyRunning() {
    if (this.isGameProcessAlive()) {
      this.logLine("⚠ 已有游戏在运行，请先点顶栏「结束游戏」再启动新的实例");
      Toast.info("已有游戏在运行，请先结束当前游戏");
      return true;
    }
    if (this.launchState.isLaunching()) {
      this.logLine("🎮 游戏正在运行中，如需中止请点顶栏的「取消启动」");
      return true;
    }
    return false;
  }

  private void handleLaunch() {
    // 每次只能启动一个：已经在启动流程里，或者已经有游戏进程在跑，都直接拦下
    if (this.blockIfAlreadyRunning()) {
      return;
    }
    MinecraftVersion selectedVersion = this.versionComboBox.getValue();
    if (selectedVersion == null || this.currentSession == null) {
      this.logArea.appendText("⚠️ 请先选择版本并登录！\n");
      return;
    }
    String id = selectedVersion.getId();
    String jsonUrl = selectedVersion.getUrl();
    File gameDir = new File(this.config.getGameDir());
    this.launchState.beginPrepare();
    // 记录本次会话开始时间：崩溃诊断只认这之后生成的 crash-report
    CrashAnalyzer.markSessionStart(System.currentTimeMillis());
    this.setLaunchingUi(true);
    this.logArea.appendText("准备启动版本: " + id + "（运行中可点「■ 停止」中止）\n");
    CompletableFuture.runAsync(
        () -> {
          Consumer<String> log =
              msg -> Platform.runLater(() -> this.logArea.appendText(msg + "\n"));
          try {
            // 启动前再确认一次父版本（历史实例可能是缺父版本的半成品）
            this.ensureParentVersion(gameDir, id, log);
            VersionProfile profile = this.ensureInstalled(id, jsonUrl, log);
            this.checkLaunchCancelled();
            this.preflight(profile, log);
            if (this.config.isAutoRepairLibraries()) {
              Integrity.Report cached;
              Integrity.Report report = null;
              long now = System.currentTimeMillis();
              // 5 分钟内刚查过同一个实例就直接复用，别每次启动都把几百个文件再扫一遍
              if (this.lastIntegrityReport != null
                  && profile.getId().equals(this.lastIntegrityProfile)
                  && now - this.lastIntegrityAt < 5 * 60 * 1000L) {
                report = this.lastIntegrityReport;
                log.accept(
                    "📋 复用 "
                        + Math.max(1L, (now - this.lastIntegrityAt) / 1000L)
                        + " 秒前的完整性检查结果（"
                        + report.issues().size()
                        + " 个缺失）");
              }
              if (this.toolsPanel != null && (cached = this.toolsPanel.freshReport()) != null) {
                report =
                    new Integrity.Report(
                        0,
                        cached.issues().stream()
                            .filter(issue -> issue.instanceId().equals(profile.getId()))
                            .toList());
                log.accept("📋 复用工具页的完整性检查结果（" + report.issues().size() + " 个缺失）");
              }
              if (report == null) {
                long tCheck = System.nanoTime();
                report =
                    Integrity.checkProfile(
                        gameDir, profile.getId(), profile, this.config.getAssetSampleCount());
                log.accept("⏱ 完整性检查耗时 " + (System.nanoTime() - tCheck) / 1_000_000L + " ms");
                this.lastIntegrityReport = report;
                this.lastIntegrityAt = System.currentTimeMillis();
                this.lastIntegrityProfile = profile.getId();
              }
              this.checkLaunchCancelled();
              if (!report.issues().isEmpty()) {
                log.accept("🔧 启动前发现 " + report.issues().size() + " 个缺失文件，正在自动补全…");
                int fixed = Integrity.repair(gameDir, report.issues());
                // 修复过就作废缓存，否则同一批文件 5 分钟内会被重复报成「缺失」
                this.lastIntegrityReport = null;
                this.lastIntegrityAt = 0L;
                int remaining = report.issues().size() - fixed;
                log.accept(
                    "🔧 已补全 "
                        + fixed
                        + "/"
                        + report.issues().size()
                        + (remaining > 0 ? "，仍有 " + remaining + " 个缺失" : ""));
                if (remaining > 0 && !this.confirmLaunchWithMissing(remaining)) {
                  this.finishLaunching();
                  return;
                }
              }
            }
            this.checkLaunchCancelled();
            log.accept("⏳ 等待所有下载任务完成后再启动游戏…");
            if (!this.awaitDownloadsStoppable(1800000L)) {
              if (this.launchState.isCancelRequested()) {
                throw new LaunchCancelledException();
              }
              log.accept("⚠ 仍有下载任务未完成，已取消本次启动；请等待下载完成后重试。");
              this.finishLaunching();
              return;
            }
            this.checkLaunchCancelled();
            log.accept("✅ 下载任务全部完成，开始启动游戏。");
            Platform.runLater(() -> this.startGame(profile));
          } catch (LaunchCancelledException stopped) {
            Platform.runLater(
                () -> {
                  this.logArea.appendText("⏹ 已停止启动（未启动游戏）\n");
                  this.finishLaunching();
                });
          } catch (Exception e) {
            Platform.runLater(
                () -> {
                  this.logArea.appendText("❌ 启动前准备失败: " + e.getMessage() + "\n");
                  this.finishLaunching();
                });
          }
        });
  }

  /** 启动准备阶段的中断点。 */
  private void checkLaunchCancelled() {
    if (this.launchState.isCancelRequested()) {
      throw new LaunchCancelledException();
    }
  }

  /** 等待下载队列清空，期间每秒检查一次是否被要求停止。 */
  private boolean awaitDownloadsStoppable(long timeoutMs) {
    long deadline = System.currentTimeMillis() + timeoutMs;
    while (System.currentTimeMillis() < deadline) {
      if (this.launchState.isCancelRequested()) {
        return false;
      }
      if (DownloadManager.awaitIdle(1000L)) {
        return true;
      }
    }
    return false;
  }

  /** 结束「启动准备」状态，恢复按钮。 */
  private void finishLaunching() {
    this.launchState.finish();
    // 调用点有的在后台线程（下载/准备回调），界面更新必须切回 FX 线程
    Runnable uiUpdate =
        () -> {
          this.setLaunchingUi(false);
          if (this.launchBtn != null) {
            this.launchBtn.setDisable(false);
          }
        };
    if (Platform.isFxApplicationThread()) {
      uiUpdate.run();
    } else {
      Platform.runLater(uiUpdate);
    }
  }

  private VersionProfile ensureInstalled(String id, String jsonUrl, Consumer<String> log)
      throws Exception {
    File gameDir = new File(this.config.getGameDir());
    File jsonFile = VersionProfile.jsonFile(gameDir, id);
    if (!jsonFile.isFile()) {
      if (jsonUrl == null) {
        throw new Exception("本地没有 " + id + " 的版本文件，远端清单里也没有它的地址");
      }
      log.accept("📄 获取版本 JSON: " + id);
      String json = VersionManager.fetchVersionDetailsAsync(jsonUrl).get(120L, TimeUnit.SECONDS);
      Files.createDirectories(jsonFile.getParentFile().toPath(), new FileAttribute<?>[0]);
      Files.writeString(
          jsonFile.toPath(), (CharSequence) json, StandardCharsets.UTF_8, new OpenOption[0]);
    }
    VersionProfile profile = VersionProfile.load(gameDir, id);
    log.accept("🧩 版本 " + profile.getId() + "（基于 " + profile.getRootId() + "）");
    // 隔离状态按实例算：实例自己的 instance.json 优先，否则跟随全局设置
    boolean isolated =
        InstanceSettings.effectiveIsolation(gameDir, profile.getId(), this.config.isIsolation());
    File instanceDir = InstanceDirs.root(gameDir, profile.getId(), isolated);
    LaunchContext context =
        new LaunchContext(
            profile,
            gameDir,
            instanceDir,
            log,
            (done, total, bytes) -> this.updateProgress(done, total, bytes),
            () -> this.launchState.isCancelRequested());
    // 启动准备管线：客户端 jar → 依赖库 → 资源 → 原生库（每一步都会打耗时）
    new LaunchPipeline()
        .add(new ClientJarTask(this.config.getDownloadSource()))
        .add(new LibrariesTask())
        .add(new AssetsTask())
        .add(new NativesTask(isolated))
        .run(context);
    // 加载器实例需要一份和父版本同名的 client jar（Forge 的 ignoreList 依赖这个文件名）
    File clientJar = new File(new File(new File(gameDir, "versions"), profile.getRootId()),
        profile.getRootId() + ".jar");
    File childDir = new File(new File(gameDir, "versions"), profile.getId());
    File childJar = new File(childDir, profile.getId() + ".jar");
    if (profile.getId().equals(profile.getRootId()) || !clientJar.isFile()) {
      return profile;
    }
    if (!childJar.isFile() || childJar.length() != clientJar.length()) {
      try {
        Files.createDirectories(childDir.toPath(), new FileAttribute<?>[0]);
        Files.copy(clientJar.toPath(), childJar.toPath(), StandardCopyOption.REPLACE_EXISTING);
      } catch (Exception ignored) {
        // 复制失败时游戏会自己报缺文件，不阻塞启动
      }
    }
    return profile;
  }

  private void updateProgress(int done, int total, long bytes) {
    this.downloadBarView.progress(done, total, bytes);
  }

  private void updateDownloadBar(DownloadManager.Status status) {
    this.downloadBarView.update(status);
  }

  private void preflight(VersionProfile profile, Consumer<String> log) throws Exception {
    File gameDir = new File(this.config.getGameDir());
    LaunchPreflight.Report report =
        LaunchPreflight.check(gameDir, profile.getJavaMajorVersion());
    log.accept(
        "🧪 自检通过：目录可写，磁盘剩余 "
            + report.usableBytes() / 1024L / 1024L / 1024L
            + " GB，需要 Java "
            + report.requiredJava());
  }

  /*
   * Enabled aggressive block sorting
   * Enabled unnecessary exception pruning
   * Enabled aggressive exception aggregation
   */
  private int javaMajorVersion(String javaPath) {
    return JavaVersionProbe.probe(javaPath);
  }

  private void updateGeekTelemetry() {
    if (this.geekTelemetry == null) {
      return;
    }
    // 启动页不可见时没必要每 3 秒重排这一行
    if (this.launchPanel != null && !this.launchPanel.isVisible()) {
      return;
    }
    long seconds = Math.max(0L, (System.currentTimeMillis() - this.appStartedAt) / 1000L);
    String up =
        String.format(
            Locale.ROOT, "%02d:%02d:%02d", seconds / 3600L, seconds % 3600L / 60L, seconds % 60L);
    Runtime runtime = Runtime.getRuntime();
    long heap = (runtime.totalMemory() - runtime.freeMemory()) / 1024L / 1024L;
    this.geekTelemetry.setText(
        "[SYS] UP " + up + " · HEAP " + heap + "M · THR " + Thread.activeCount());
  }

  private void requestClose() {
    this.chrome().requestClose();
  }

  private boolean installTray() {
    return this.chrome().installTray();
  }

  private void hideToTray() {
    this.chrome().hideToTray();
  }

  private void restoreFromTray() {
    this.chrome().restoreFromTray();
  }

  private void quitLauncher() {
    if (this.quitting) {
      return;
    }
    this.quitting = true;
    if (this.gameProcess != null && this.gameProcess.isAlive()) {
      this.launchState.requestStop();
      this.gameProcess.descendants().forEach(ProcessHandle::destroyForcibly);
      this.gameProcess.destroyForcibly();
    }
    this.chrome().removeTray();
    SingleInstance.release();
    Platform.exit();
    System.exit(0);
  }

  /** 窗口收尾（关闭行为 / 托盘）延迟创建：stage 还没建好时也能调用。 */
  private WindowChrome chrome() {
    if (this.windowChrome == null) {
      this.windowChrome =
          new WindowChrome(
              new WindowChrome.Host() {
                @Override
                public LauncherConfig config() {
                  return HelloApplication.this.config;
                }

                @Override
                public Stage stage() {
                  return HelloApplication.this.stageRef;
                }

                @Override
                public StackPane dialogLayer() {
                  return HelloApplication.this.dialogLayer;
                }

                @Override
                public boolean gameRunning() {
                  return HelloApplication.this.launchState.isRunning();
                }

                @Override
                public void log(String message) {
                  HelloApplication.this.logArea.appendText(message + "\n");
                }

                @Override
                public void quit() {
                  HelloApplication.this.quitLauncher();
                }
              });
    }
    return this.windowChrome;
  }

  private void maybeOfferPerformanceOptimization() {
    if (this.config.isPerformanceAsked()
        || this.config.isPerformanceApplied()
        || !PerformanceOptimizer.isWindows()
        || this.stageRef == null) {
      return;
    }
    if (this.stageRef.getScene() == null) {
      return;
    }
    File gameDir = new File(this.config.getGameDir());
    if (PerformanceOptimizer.hasBackup(gameDir)) {
      this.config.setPerformanceAsked(true);
      this.config.save();
      return;
    }
    this.config.setPerformanceAsked(true);
    this.config.save();
    StackPane layer = (StackPane) this.stageRef.getScene().getRoot();
    FirstRunPanel panel =
        new FirstRunPanel(
            this.config,
            message -> this.logArea.appendText(message + "\n"),
            () -> layer.getChildren().removeIf(node -> node instanceof FirstRunPanel));
    layer.getChildren().add(panel);
  }

  private void showLaunchFailureHint(String message) {
    Label text = new Label(message + "\n\n可以先去工具页校验并补全缺失文件，再重试启动。");
    text.setWrapText(true);
    text.setStyle("-fx-text-fill: -app-text-2; -fx-font-size: 12.5px;");
    InAppDialog.custom(
        this.dialogLayer,
        "启动失败",
        text,
        List.of(
            InAppDialog.Action.secondary("关闭", null),
            InAppDialog.Action.primary(
                "去校验文件",
                () -> {
                  this.switchPage("tools");
                  if (this.toolsPanel != null) {
                    this.toolsPanel.startLibraryCheck();
                  }
                })),
        null);
  }

  private boolean confirmLaunchWithMissing(int missing) {
    CompletableFuture<Boolean> answer = new CompletableFuture<>();
    Label text = new Label("缺失文件可能导致启动失败，是否仍然启动？");
    text.setWrapText(true);
    text.setStyle("-fx-text-fill: -app-text-2; -fx-font-size: 12.5px;");
    Platform.runLater(
        () ->
            InAppDialog.custom(
                this.dialogLayer,
                "启动前检查：仍有 " + missing + " 个文件缺失",
                text,
                List.of(
                    InAppDialog.Action.secondary("取消", () -> answer.complete(false)),
                    InAppDialog.Action.primary("仍然启动", () -> answer.complete(true))),
                () -> answer.complete(false)));
    try {
      return answer.get(180L, TimeUnit.SECONDS);
    } catch (Exception e) {
      return false;
    }
  }

  private void ensureChineseLanguage(File instanceDir) {
    LanguageConfig.ensureChinese(instanceDir);
  }

  private void joinServer(String address) {
    if (address == null || address.isBlank()) {
      return;
    }
    this.pendingJoinAddress = address.trim();
    this.logMessage("🌐 准备启动并加入服务器: " + this.pendingJoinAddress);
    this.handleLaunch();
  }

  /** 启动前的环境准备结果：authlib-injector 文件（离线登录为 null）+ 选好的 java.exe。 */
  private record LaunchEnv(File injector, String javaPath) {}

  private void startGame(VersionProfile profile) {
    // 准备阶段是异步的，落地前再确认一次，避免并发准备出两个进程
    if (this.isGameProcessAlive()) {
      this.logLine("⚠ 已有游戏在运行，本次启动已取消");
      this.finishLaunching();
      return;
    }
    this.launchPrepareStart = System.currentTimeMillis();
    File gameDir = new File(this.config.getGameDir());
    boolean isolated =
        InstanceSettings.effectiveIsolation(gameDir, profile.getId(), this.config.isIsolation());
    File nativesDir = InstanceDirs.natives(gameDir, profile.getId(), isolated);
    CompletableFuture<File> injectorFuture =
        this.currentSession.getType() == UserSession.LoginType.THIRD_PARTY
            ? AuthlibInjectorManager.ensureInjectorAsync(
                gameDir, msg -> Platform.runLater(() -> this.logArea.appendText(msg + "\n")))
            : CompletableFuture.completedFuture((File) null);
    int requiredJava = profile.getJavaMajorVersion();
    if (this.config.getJavaPath() != null && !this.config.getJavaPath().isBlank()) {
      this.logArea.appendText(
          "☕ 版本 "
              + profile.getId()
              + " 需要 Java "
              + requiredJava
              + "，使用设置中指定的 Java："
              + this.config.getJavaPath()
              + "\n");
    } else {
      this.logArea.appendText(
          "☕ 版本 " + profile.getId() + " 需要 Java " + requiredJava + "，未指定路径，准备 Azul Zulu 高性能运行时…\n");
    }
    CompletableFuture<String> javaFuture =
        this.config.getJavaPath() != null && !this.config.getJavaPath().isBlank()
            ? CompletableFuture.completedFuture(this.config.getJavaPath())
            : JavaManager.ensureJavaAsync(
                gameDir,
                requiredJava,
                msg -> Platform.runLater(() -> this.logArea.appendText(msg + "\n")));
    injectorFuture
        .thenCombine(javaFuture, (injector, javaPath) -> new LaunchEnv(injector, javaPath))
        .thenAccept(
            env ->
                Platform.runLater(
                    () -> {
                      try {
                        String javaPath = env.javaPath();
                        int actual = this.javaMajorVersion(javaPath);
                        int required = profile.getJavaMajorVersion();
                        if (actual > 0 && actual < required) {
                          this.logArea.appendText(
                              "⚠️ 该版本需要 Java "
                                  + required
                                  + "，当前用的是 Java "
                                  + actual
                                  + "，大概率会崩；可在「设置」里指定 Java 路径\n");
                        } else if (actual > 0) {
                          this.logArea.appendText("☕ Java " + actual + "（需要 " + required + "）\n");
                        }
                        InstanceSettings.Settings override =
                            InstanceSettings.get(gameDir, profile.getId());
                        LauncherConfig launchConfig =
                            this.config.copyForLaunch(
                                override.memoryMb(),
                                override.jvmArgs(),
                                override.javaPath(),
                                override.gameArgs());
                        if (!override.isEmpty()) {
                          this.logArea.appendText(
                              "🎛 使用实例级启动设置"
                                  + (override.memoryMb() != null
                                      ? "（内存 " + override.memoryMb() + " MB）"
                                      : "")
                                  + (override.jvmArgs() != null && !override.jvmArgs().isBlank()
                                      ? "（JVM 参数）"
                                      : "")
                                  + (override.javaPath() != null && !override.javaPath().isBlank()
                                      ? "（Java 路径）"
                                      : "")
                                  + "\n");
                        }
                        if (this.config.isSmartPerformance()) {
                          PerformanceTuner.Plan plan =
                              PerformanceTuner.plan(launchConfig.getMemoryMb());
                          launchConfig.setJvmArgs(
                              PerformanceTuner.merge(launchConfig.getJvmArgs(), plan.jvmArgs()));
                          this.logArea.appendText("⚡ 智能性能：" + plan.summary() + "\n");
                        }
                        File instanceDir =
                            InstanceDirs.root(gameDir, profile.getId(), isolated);
                        this.ensureChineseLanguage(instanceDir);
                        LaunchPlan plan =
                            LaunchPlanner.plan(
                                profile,
                                this.currentSession,
                                launchConfig,
                                gameDir,
                                instanceDir,
                                javaPath,
                                nativesDir,
                                env.injector());
                        if (this.pendingJoinAddress != null && !this.pendingJoinAddress.isBlank()) {
                          ServerAddress join = ServerAddress.parse(this.pendingJoinAddress);
                          this.pendingJoinAddress = null;
                          if (join != null) {
                            plan = plan.append(join.launchArguments());
                          }
                        }
                        this.gameInstanceId = profile.getId();
                        this.gameStartAt = System.currentTimeMillis();
                        this.startProcess(plan);
                      } catch (Exception e) {
                        this.logArea.appendText("❌ 启动失败: " + e.getMessage() + "\n");
                        this.showLaunchFailureHint("启动失败: " + e.getMessage());
                        this.launchBtn.setDisable(false);
                        // 必须复位，否则「正在运行」会一直挂着、后面再也启动不了
                        this.finishLaunching();
                      }
                    }))
        .exceptionally(
            ex -> {
              Platform.runLater(
                  () -> {
                    this.logArea.appendText("❌ 环境准备失败: " + ex.getMessage() + "\n");
                    this.launchBtn.setDisable(false);
                    this.finishLaunching();
                  });
              return null;
            });
  }

  private void applyGamePriority(long pid) {
    String level = this.config.getGamePriority();
    if (level == null || "normal".equals(level)) {
      return;
    }
    String priority = "above".equals(level) ? "AboveNormal" : "High";
    String friendly = "above".equals(level) ? "高于正常" : "高";
    CompletableFuture.runAsync(
        () -> {
          try {
            String command =
                "$p=Get-Process -Id "
                    + pid
                    + " -ErrorAction SilentlyContinue; if($p){$p.PriorityClass='"
                    + priority
                    + "'}; (Get-Process -Id "
                    + pid
                    + " -ErrorAction SilentlyContinue).PriorityClass";
            Process probe =
                new ProcessBuilder(
                        "powershell", "-NoProfile", "-WindowStyle", "Hidden", "-Command", command)
                    .redirectErrorStream(true)
                    .start();
            String output =
                new String(probe.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            int code = probe.waitFor();
            Platform.runLater(
                () -> {
                  if (code == 0 && output.toLowerCase(Locale.ROOT).contains("priority")) {
                    this.logArea.appendText("⚡ 游戏进程优先级已设为" + friendly + "（PID " + pid + "）\n");
                  } else {
                    this.logArea.appendText(
                        "⚠ 进程优先级设置未确认（不影响游戏）："
                            + (output.isBlank() ? "退出码 " + code : output)
                            + "\n");
                  }
                });
          } catch (Exception e) {
            Platform.runLater(
                () -> this.logArea.appendText("⚠ 进程优先级设置失败（不影响游戏）：" + e.getMessage() + "\n"));
          }
        });
  }

  private void startProcess(LaunchPlan plan) throws Exception {
    if (this.isGameProcessAlive()) {
      throw new IllegalStateException("已有游戏进程在运行，已阻止重复启动");
    }
    File gameDir = plan.workingDirectory();
    this.logArea.appendText("🚀 " + plan.maskedCommand() + "\n");
    this.lastLaunchLogStart = this.logArea.getLength();
    // 真正 spawn 之前最后确认一次用户有没有在准备阶段点过取消
    this.checkLaunchCancelled();
    Process process = plan.start();
    this.gameProcess = process;
    this.launchState.markRunning();
    this.setGameStatus(true);
    // 游戏已起来：主按钮保持「正在运行」，停止按钮收起（进程已在跑，不能靠它取消）
    this.setLaunchingUi(true);
    if (this.cancelLaunchButton != null) {
      this.cancelLaunchButton.setVisible(false);
      this.cancelLaunchButton.setManaged(false);
    }
    this.logArea.appendText("✅ 游戏进程已启动 (pid " + process.pid() + ")\n");
    if (this.launchPrepareStart > 0L) {
      long prepareMs;
      this.lastLaunchPrepMs = prepareMs = System.currentTimeMillis() - this.launchPrepareStart;
      this.logArea.appendText(
          "⏱ 启动准备耗时 " + String.format(Locale.ROOT, "%.1f", (double) prepareMs / 1000.0) + " 秒\n");
      this.launchPrepareStart = 0L;
    }
    this.applyGamePriority(process.pid());
    if (this.config.isMinimizeOnLaunch() && this.stageRef != null) {
      this.minimizedForGame = true;
      this.stageRef.setIconified(true);
    }
    if (this.config.isPauseVisualsInGame() && this.ambientBackground != null) {
      this.ambientBackground.stop();
    }
    if (this.testMode) {
      this.testMode = false;
      PauseTransition autoClose = new PauseTransition(Duration.seconds(45.0));
      autoClose.setOnFinished(
          ev -> {
            this.logArea.appendText(
                this.testSuccess ? "🧪 测试结果：游戏窗口已成功创建 ✓\\n" : "🧪 测试结果：等待 45 秒仍未看到游戏窗口，可能启动失败\\n");
            if (process.isAlive()) {
              process.descendants().forEach(ProcessHandle::destroyForcibly);
              process.destroyForcibly();
            }
          });
      autoClose.play();
    }
    process
        .onExit()
        .thenAccept(
            exited ->
                Platform.runLater(
                    () -> {
                      boolean forcedStop = this.launchState.isStopRequested();
                      this.launchState.clearStop();
                      this.launchState.finish();
                      this.setLaunchingUi(false);
                      this.setGameStatus(false);
                      if (this.stageRef != null && !this.stageRef.isShowing()) {
                        String trayMessage =
                            forcedStop
                                ? "Game stopped by launcher."
                                : (exited.exitValue() == 0
                                    ? "Game exited normally."
                                    : "Game crashed - check the crash report.");
                        this.chrome().notifyTray(trayMessage, !forcedStop && exited.exitValue() != 0);
                      }
                      if (this.minimizedForGame && this.stageRef != null) {
                        this.minimizedForGame = false;
                        this.stageRef.setIconified(false);
                      }
                      if (this.config.isPauseVisualsInGame()
                          && this.ambientBackground != null
                          && (this.stageRef == null || !this.stageRef.isIconified())) {
                        this.ambientBackground.start();
                      }
                      String recent = "";
                      try {
                        recent =
                            this.logArea.getText(this.lastLaunchLogStart, this.logArea.getLength());
                      } catch (Exception exception) {
                        // empty catch block
                      }
                      long sessionSeconds = 0L;
                      if (this.gameStartAt > 0L && this.gameInstanceId != null) {
                        sessionSeconds =
                            Math.max(1L, (System.currentTimeMillis() - this.gameStartAt) / 1000L);
                        File base = new File(this.config.getGameDir());
                        Playtime.add(base, this.gameInstanceId, sessionSeconds);
                        this.logArea.appendText(
                            "⏱ 本次游戏时长 "
                                + Playtime.format(sessionSeconds)
                                + "（累计 "
                                + Playtime.format(Playtime.seconds(base, this.gameInstanceId))
                                + "）\n");
                        this.gameStartAt = 0L;
                      }
                      boolean crashLike = !forcedStop && exited.exitValue() != 0;
                      LaunchHistory.record(
                          new File(this.config.getGameDir()),
                          new LaunchHistory.Entry(
                              System.currentTimeMillis(),
                              this.gameInstanceId,
                              this.lastLaunchPrepMs,
                              sessionSeconds,
                              exited.exitValue(),
                              crashLike));
                      this.lastLaunchPrepMs = 0L;
                      if (this.launchInsightBar != null) {
                        this.launchInsightBar.refresh();
                      }
                      this.logArea.appendText(
                          (forcedStop
                              ? "🛑 游戏已被启动器主动结束。\n"
                              : (crashLike
                                  ? "⚠ 游戏异常退出（退出码 " + exited.exitValue() + "）\n"
                                  : "🏁 游戏已正常退出（退出码 " + exited.exitValue() + "）\n")));
                      this.launchBtn.setDisable(false);
                      if (crashLike) {
                        File crashDir = new File(gameDir, "crash-reports");
                        final long sessionFrom = this.gameStartAt;
                        File[] reports =
                            crashDir.listFiles(
                                (d, n) ->
                                    n.endsWith(".txt")
                                        && (sessionFrom <= 0
                                            || new File(d, n).lastModified() >= sessionFrom));
                        if (reports != null && reports.length > 0) {
                          Arrays.sort(
                              reports, Comparator.comparingLong(File::lastModified).reversed());
                          this.logArea.appendText(
                              "💥 崩溃报告: " + reports[0].getAbsolutePath() + "\n");
                        } else {
                          this.logArea.appendText("💥 非正常退出，可在「工具 → 打开日志目录」里看 latest.log\n");
                        }
                        this.logArea.appendText("   如果是缺文件导致，去「工具 → 百宝箱 → 校验库文件」可自动补齐\n");
                        CrashAnalyzer.Report report = CrashAnalyzer.analyze(gameDir);
                        this.logArea.appendText(CrashAnalyzer.toText(report));
                        if (report.hasFindings()) {
                          CrashReportDialog.show(this.stageRef, report);
                        } else {
                          this.showLaunchFailureHint("游戏异常退出（退出码 " + exited.exitValue() + "）");
                        }
                      }
                    }));
    new Thread(
            () -> {
              try (BufferedReader reader =
                  new BufferedReader(
                      new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)); ) {
                String line;
                while ((line = reader.readLine()) != null) {
                  String outputLine = line;
                  Platform.runLater(() -> this.logArea.appendText("[Game] " + outputLine + "\n"));
                }
              } catch (Exception exception) {
                // empty catch block
              }
            },
            "game-output")
        .start();
  }

  private String currentInstanceId() {
    if (this.versionComboBox != null && this.versionComboBox.getValue() != null) {
      return (this.versionComboBox.getValue()).getId();
    }
    List<String> installed = VersionProfile.installedIds(new File(this.config.getGameDir()));
    return installed.isEmpty() ? null : installed.get(0);
  }

  /** 该实例实际生效的隔离状态：实例自己的 instance.json 可覆盖全局开关。 */
  private boolean isIsolated(String instanceId) {
    return InstanceSettings.effectiveIsolation(
        new File(this.config.getGameDir()), instanceId, this.config.isIsolation());
  }

  /** 指定游戏目录时判断隔离（例如首次启动检测目录，config 里还没切过去）。 */
  private boolean isIsolated(File gameDir, String instanceId) {
    return InstanceSettings.effectiveIsolation(gameDir, instanceId, this.config.isIsolation());
  }

  private void switchGameDir(File dir) {
    if (dir == null) {
      return;
    }
    File root = this.detectMinecraftRoot(dir);
    if (root == null) {
      Toast.error("没有检测到 Minecraft 目录：请选择包含 versions 的 .minecraft 文件夹");
      this.logMessage("❌ 导入失败：未在 " + dir.getAbsolutePath() + " 找到 versions");
      return;
    }
    File currentDir = new File(this.config.getGameDir()).getAbsoluteFile();
    if (root.getAbsoluteFile().equals(currentDir)) {
      return;
    }
    this.config.setGameDir(root.getAbsolutePath());
    this.config.addGameDir(root.getAbsolutePath());
    this.config.save();
    this.refreshInstalledVersions();
    int count = VersionProfile.installedIds(root).size();
    if (count == 0) {
      Toast.warn("目录已切换，但没有发现已安装版本：请确认 versions 内有版本 JSON");
      this.logMessage("⚠ 已切换目录，但未发现已安装版本: " + root.getAbsolutePath());
    } else {
      Toast.ok("已导入 " + count + " 个版本：" + root.getName());
      this.logMessage("✅ 已导入游戏目录 " + root.getAbsolutePath() + "，版本数 " + count);
    }
  }

  private File detectMinecraftRoot(File selected) {
    return GameDirDetector.detect(selected);
  }

  private boolean hasVersions(File dir) {
    return GameDirDetector.hasVersions(dir);
  }

  private void selectInstance(Instance instance) {
    this.addInstalledToCombo(instance.id);
    this.config.setLastInstance(instance.id);
    this.config.save();
    if (this.instanceSettingsPanel != null) {
      this.instanceSettingsPanel.setInstance(instance.id);
    }
    this.logMessage("🎮 已选择实例: " + instance.id + "（" + instance.subtitle() + "）");
    this.switchPage("launch");
  }

  private void repairInstance(String id) {
    this.logMessage("🔧 开始补全实例 " + id + " ...");
    CompletableFuture.runAsync(
        () -> {
          try {
            this.ensureInstalled(id, null, msg -> Platform.runLater(() -> this.logMessage(msg)));
            Platform.runLater(() -> this.logMessage("✅ 实例 " + id + " 文件已补全"));
          } catch (Exception e) {
            Platform.runLater(() -> this.logMessage("❌ 补全失败: " + e.getMessage()));
          }
        });
  }

  private void exportLaunchScript(String id) {
    if (this.currentSession == null) {
      this.logMessage("⚠️ 请先登录，再导出启动脚本（需要账号参数）");
      return;
    }
    try {
      File gameDir = new File(this.config.getGameDir());
      VersionProfile profile = VersionProfile.load(gameDir, id);
      String java =
          this.config.getJavaPath() == null || this.config.getJavaPath().isBlank()
              ? new File(new File(gameDir, "java"),
                      "zulu" + profile.getJavaMajorVersion() + "/bin/java.exe").getAbsolutePath()
              : this.config.getJavaPath();
      File out =
          LaunchScriptExporter.export(
              profile,
              this.currentSession,
              this.config,
              gameDir,
              InstanceDirs.root(gameDir, id, this.isIsolated(id)),
              java,
              InstanceDirs.natives(gameDir, id, this.isIsolated(id)));
      this.logMessage("📜 启动脚本已导出: " + out.getAbsolutePath());
    } catch (Exception e) {
      this.logMessage("❌ 导出失败: " + e.getMessage());
    }
  }

  private void deleteInstance(String id) {
    File dir = new File(new File(this.config.getGameDir(), "versions"), id);
    if (!dir.isDirectory()) {
      this.logMessage("⚠️ 实例目录不存在: " + dir.getAbsolutePath());
      return;
    }
    CompletableFuture.runAsync(
        () -> {
          boolean ok = this.deleteRecursively(dir);
          Platform.runLater(
              () -> {
                if (ok) {
                  this.logMessage("🗑️ 已删除实例 " + id);
                  this.refreshInstalledVersions();
                  if (this.instanceSettingsPanel != null) {
                    this.instanceSettingsPanel.setInstance(this.currentInstanceId());
                  }
                  this.switchPage("launch");
                } else {
                  this.logMessage("❌ 删除失败（文件可能被占用）: " + id);
                }
              });
        });
  }

  private void checkInstance(Instance instance) {
    File game = new File(this.config.getGameDir());
    Toast.info("正在检查实例 " + instance.id + " …");
    CompletableFuture.runAsync(
        () -> {
          Integrity.Report report = Integrity.check(game, instance.id);
          Platform.runLater(
              () -> {
                if (report.issues().isEmpty()) {
                  Toast.ok(instance.id + " 完整（检查 " + report.checked() + " 个库）");
                  this.logMessage("✅ 实例 " + instance.id + " 库文件完整");
                  return;
                }
                Label question = new Label("是否立即下载补齐？");
                question.setWrapText(true);
                question.setStyle("-fx-text-fill: -app-text-2; -fx-font-size: 12.5px;");
                InAppDialog.custom(
                    this.dialogLayer,
                    "实例 " + instance.id + " 缺失 " + report.issues().size()
                        + " 个库文件（共检查 " + report.checked() + " 个）",
                    question,
                    List.of(
                        InAppDialog.Action.secondary("稍后", null),
                        InAppDialog.Action.primary(
                            "立即补齐",
                            () -> {
                              Toast.info("正在补齐 " + instance.id + " 的库文件…");
                              CompletableFuture.runAsync(
                                  () -> {
                                    int ok = Integrity.repair(game, report.issues());
                                    Platform.runLater(
                          () -> {
                            Toast.ok("已补齐 " + ok + "/" + report.issues().size());
                            this.logMessage(
                                "🔧 实例 "
                                    + instance.id
                                    + " 补齐库文件 "
                                    + ok
                                    + "/"
                                    + report.issues().size());
                                  });
                                });
                            })),
                    null);
              });
        });
  }

  private void cloneInstance(String id, boolean copyData) {
    File versions = new File(this.config.getGameDir(), "versions");
    if (!new File(versions, id).isDirectory()) {
      this.logMessage("⚠️ 实例目录不存在: " + new File(versions, id).getAbsolutePath());
      return;
    }
    this.logMessage("📄 正在克隆实例 " + id + (copyData ? "（含数据）" : "（仅版本）"));
    CompletableFuture.runAsync(
        () -> {
          try {
            File target = InstanceCloner.duplicate(new File(this.config.getGameDir()), id, copyData);
            Platform.runLater(
                () -> {
                  this.refreshInstalledVersions();
                  if (this.instancesPanel != null) {
                    this.instancesPanel.setSelectedId(this.currentInstanceId());
                  }
                  Toast.ok("已克隆实例 → " + target.getName());
                  this.logMessage("✅ 已克隆实例 " + id + " → " + target.getName());
                });
          } catch (Exception e) {
            Platform.runLater(() -> this.logMessage("❌ 克隆失败: " + e.getMessage()));
          }
        });
  }

  private void testLaunchInstance(String id) {
    if (this.currentSession == null) {
      this.logMessage("⚠️ 请先登录，再测试启动");
      return;
    }
    MinecraftVersion selected = this.versionComboBox.getValue();
    if (selected == null || !selected.getId().equals(id)) {
      this.addInstalledToCombo(id);
    }
    this.logMessage("🧪 测试启动 " + id + "（45 秒后自动关闭）...");
    this.testMode = true;
    this.testSuccess = false;
    this.switchPage("launch");
    this.handleLaunch();
  }

  private void reinstallInstance(String id) {
    VersionProfile probe;
    try {
      probe = VersionProfile.load(new File(this.config.getGameDir()), id);
    } catch (Exception e) {
      this.logMessage("❌ 读取实例失败: " + e.getMessage());
      return;
    }
    if (!probe.getId().equals(probe.getRootId())) {
      this.logMessage("♻️ 这是加载器实例（基于 " + probe.getRootId() + "），请在「下载 → Minecraft」里重新安装对应加载器");
      return;
    }
    this.logMessage("♻️ 开始重装 " + id + "（会重新下载客户端与依赖）");
    CompletableFuture.runAsync(
        () -> {
          try {
            String url =
                VersionManager.fetchVersionListAsync().get(60L, TimeUnit.SECONDS).stream()
                    .filter(v -> v.getId().equals(id))
                    .findFirst()
                    .map(MinecraftVersion::getUrl)
                    .orElse(null);
            if (url == null) {
              throw new Exception("远端清单里没有 " + id);
            }
            File dir = new File(new File(this.config.getGameDir(), "versions"), id);
            this.deleteRecursively(dir);
            this.ensureInstalled(id, url, msg -> Platform.runLater(() -> this.logMessage(msg)));
            Platform.runLater(
                () -> {
                  this.logMessage("✅ 重装完成: " + id);
                  this.refreshInstalledVersions();
                });
          } catch (Exception e) {
            Platform.runLater(() -> this.logMessage("❌ 重装失败: " + e.getMessage()));
          }
        });
  }

  private boolean deleteRecursively(File file) {
    return InstanceFiles.deleteTree(file);
  }

  private void openInstanceSettings(String tab) {
    String id = this.currentInstanceId();
    if (id == null) {
      this.logMessage("⚠️ 还没有任何实例，先去「下载」页安装一个版本");
      this.switchPage("download");
      return;
    }
    if (this.instanceSettingsPanel == null) {
      this.instanceSettingsPanel =
          new InstanceSettingsPanel(
              new File(this.config.getGameDir()),
              this.config,
              id,
              this::logMessage,
              () -> this.switchPage("launch"),
              this::repairInstance,
              this::exportLaunchScript,
              this::deleteInstance,
              this::testLaunchInstance,
              this::reinstallInstance,
              this::joinServer);
      this.pages.put("instance-settings", this.instanceSettingsPanel);
    }
    this.instanceSettingsPanel.setInstance(id);
    this.instanceSettingsPanel.showTab(tab);
    this.navHighlight = null;
    this.switchPage("instance-settings");
  }

  private void reloadVersionsAsync() {
    this.logMessage("🔄 重新加载版本列表...");

    VersionManager.fetchVersionListAsync()
        .thenAccept(
            list ->
                Platform.runLater(
                    () -> {
                      if (list == null || list.isEmpty()) {
                        return;
                      }
                      this.allMinecraftVersions.setAll((Collection<MinecraftVersion>) list);
                      List<MinecraftVersion> releases =
                          list.stream().filter(v -> "release".equals(v.getType())).toList();
                      this.versionComboBox
                          .getItems()
                          .setAll((Collection<MinecraftVersion>) releases);
                      if (!releases.isEmpty()) {
                        this.versionComboBox.getSelectionModel().selectFirst();
                      }
                      this.refreshInstalledVersions();
                      this.logMessage("✅ 版本列表已刷新（" + list.size() + " 个）");
                    }))
        .exceptionally(
            ex -> {
              Platform.runLater(() -> this.logMessage("❌ 刷新失败: " + ex.getMessage()));
              return null;
            });
  }

  private void refreshInstalledVersions() {
    File gameDir = new File(this.config.getGameDir());
    List<String> installed = VersionProfile.installedIds(gameDir);
    this.suppressInstancePersist = true;
    try {
      String preferred = null;
      MinecraftVersion previous =
          this.versionComboBox == null ? null : this.versionComboBox.getValue();
      boolean keepUserChoice =
          this.userPickedInstance && previous != null && installed.contains(previous.getId());
      for (String id : installed) {
        this.addInstalledToCombo(id);
      }
      if (this.versionComboBox == null) {
        return;
      }
      if (keepUserChoice) {
        this.versionComboBox.setValue(previous);
        this.userPickedInstance = false;
        return;
      }
      this.userPickedInstance = false;
      String remembered = this.config.getLastInstance();
      String string =
          remembered != null && installed.contains(remembered)
              ? remembered
              : (preferred = installed.isEmpty() ? null : installed.get(0));
      if (preferred != null) {
        this.selectComboVersion(preferred);
      }
    } finally {
      this.suppressInstancePersist = false;
    }
  }

  private void selectComboVersion(String id) {
    if (this.versionComboBox == null) {
      return;
    }
    for (MinecraftVersion version : this.versionComboBox.getItems()) {
      if (!version.getId().equals(id)) continue;
      this.versionComboBox.setValue(version);
      return;
    }
  }

  private void addInstalledToCombo(String id) {
    if (this.versionComboBox == null) {
      return;
    }
    for (MinecraftVersion version : this.versionComboBox.getItems()) {
      if (!version.getId().equals(id)) continue;
      this.versionComboBox.setValue(version);
      return;
    }
    MinecraftVersion installed = MinecraftVersion.installed(id);
    this.versionComboBox.getItems().add(0, installed);
    this.versionComboBox.setValue(installed);
  }

  public static void main(String[] args) {
    HelloApplication.launch(args);
  }

  private void fetchIconForPrepare(
      AtomicInteger remaining, Runnable finish, String urlStr, String key, int total) {
    try {
      ICON_SLOTS.acquire();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      Platform.runLater(
          () -> {
            if (remaining.decrementAndGet() <= 0) {
              finish.run();
            }
          });
      return;
    }
    try {
      byte[] iconBytes = IconProxy.fetch(httpClient, urlStr);
      if (iconBytes != null) {
        final byte[] finalBytes = iconBytes;
        Platform.runLater(
            () -> {
              Image img = this.decodeIcon(finalBytes);
              if (img != null && !img.isError()) {
                this.iconMemoryCache.put(key, img);
              }
              int left = remaining.decrementAndGet();
              this.resultView.status().setText("正在预渲染图标 " + (total - left) + "/" + total + "…");
              if (left <= 0) {
                finish.run();
              }
            });
      } else {
        Platform.runLater(
            () -> {
              if (remaining.decrementAndGet() <= 0) {
                finish.run();
              }
            });
      }
    } catch (Exception ignored) {
      Platform.runLater(
          () -> {
            if (remaining.decrementAndGet() <= 0) {
              finish.run();
            }
          });
    } finally {
      ICON_SLOTS.release();
    }
  }

  private void fetchIconForPrefetch(String urlStr, String key) {
    try {
      ICON_SLOTS.acquire();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return;
    }
    try {
      byte[] iconBytes = IconProxy.fetch(httpClient, urlStr);
      if (iconBytes != null) {
        final byte[] finalBytes = iconBytes;
        Platform.runLater(
            () -> {
              Image img = this.decodeIcon(finalBytes);
              if (img != null && !img.isError()) {
                this.iconMemoryCache.put(key, img);
              }
            });
      }
    } catch (Exception exception) {
    } finally {
      ICON_SLOTS.release();
    }
  }

  private static enum WindowIcon {
    MINIMIZE,
    MAXIMIZE,
    RESTORE,
    CLOSE;
  }

  private record PaletteAction(String icon, String title, String hint, Runnable action) {}

  private static final class ImportCancelledException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private ImportCancelledException() {}
  }

  /** 用户选定版本后真正开始下载/安装。 */
  private void downloadChosenVersion(
      String projectTitle, String projectType, JsonArray arr, int chosen) {
JsonObject latestVersion = arr.get(chosen).getAsJsonObject();
String versionName = latestVersion.get("version_number").getAsString();
JsonArray files = latestVersion.getAsJsonArray("files");
if (files != null && files.size() > 0) {
  File targetDir;
  JsonObject targetFile = null;
  for (int i = 0; i < files.size(); ++i) {
    JsonObject file = files.get(i).getAsJsonObject();
    if (!file.has("primary") || !file.get("primary").getAsBoolean()) continue;
    targetFile = file;
    break;
  }
  if (targetFile == null) {
    targetFile = files.get(0).getAsJsonObject();
  }
  String downloadUrl = targetFile.get("url").getAsString();
  String fileName = targetFile.get("filename").getAsString();
  long fileSize = targetFile.get("size").getAsLong() / 1024L;
  this.downloadBarView.setExpectedBytes(targetFile.get("size").getAsLong());
  this.logArea.appendText(
      "🎯 找到适配版本: v"
          + versionName
          + " ("
          + fileName
          + ", "
          + fileSize
          + " KB)\n");
  this.logArea.appendText("🔗 下载地址: " + downloadUrl + "\n");
  File instanceRoot =
      InstanceDirs.root(
          new File(this.config.getGameDir()),
          this.currentInstanceId(),
          this.isIsolated(this.currentInstanceId()));
  File file =
      targetDir =
          "整合包".equals(projectType)
              ? new File(
                  new File(this.config.getGameDir(), "downloads"), "modpack")
              : new File(instanceRoot, this.installDirFor(projectType));
  if (!targetDir.exists()) {
    targetDir.mkdirs();
  }
  File saveFile = new File(targetDir, fileName);
  boolean isModpack =
      fileName.toLowerCase(Locale.ROOT).endsWith(".mrpack") || "整合包".equals(projectType);
  InstallProgressPage downloadPage = null;
  java.util.function.Consumer<DownloadManager.Status> dlListener = null;
  if (isModpack) {
    // 点「下载」立刻进专用页面，不再让用户在列表里干等
    downloadPage =
        new InstallProgressPage(
            "正在下载：" + fileName, List.of("下载整合包", "安装到实例", "完成"));
    downloadPage.setStep(0, "run");
    downloadPage.setProgress(-1);
    downloadPage.setOnCancel(DownloadManager::cancelAll);
    downloadPage.setOnBack(() -> this.switchPage("instances"));
    if (!"download".equals(this.currentPage)) {
      this.switchPage("download");
    }
    this.showDownloadPanel(downloadPage.node());
    final InstallProgressPage pageRef = downloadPage;
    dlListener =
        status ->
            Platform.runLater(
                () ->
                    pageRef.setDownloadItems(
                        status.done(),
                        status.total(),
                        status.bytes(),
                        status.totalBytes(),
                        status.speedBps(),
                        status.file()));
    DownloadManager.addStatusListener(dlListener);
  }
  final InstallProgressPage pageForDone = downloadPage;
  final java.util.function.Consumer<DownloadManager.Status> listenerForDone = dlListener;
  java.util.function.Consumer<String> onFail =
      pageForDone == null
          ? null
          : reason -> {
            if (listenerForDone != null) {
              DownloadManager.removeStatusListener(listenerForDone);
            }
            pageForDone.setStep(0, "fail");
            pageForDone.clearDownload();
            pageForDone.finish(false, reason + "，点「返回」重试或检查网络");
          };
  this.downloadFileAsync(
      downloadUrl,
      saveFile,
      this.logArea,
      saved -> {
        if (listenerForDone != null) {
          DownloadManager.removeStatusListener(listenerForDone);
        }
        if (pageForDone != null) {
          pageForDone.clearDownload();
          pageForDone.setStep(0, "ok");
          pageForDone.setStep(1, "run");
        }
        if (saved.getName().toLowerCase().endsWith(".mrpack")) {
          this.importModpack(saved);
        } else if ("模组".equals(projectType)
            || "资源包".equals(projectType)
            || "光影包".equals(projectType)
            || "数据包".equals(projectType)) {
          this.logArea.appendText(
              "✅ 已放入 " + this.installDirFor(projectType) + "/，启动游戏即可生效\n");
        }
      },
      onFail);
} else {
  this.logArea.appendText("⚠️ 该版本下未找到可下载的文件。\n");
}
  }

}
