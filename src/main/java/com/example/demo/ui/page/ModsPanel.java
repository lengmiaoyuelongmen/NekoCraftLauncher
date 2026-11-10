package com.example.demo.ui.page;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.stage.FileChooser;
import java.nio.file.StandardCopyOption;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import com.example.demo.core.app.AppPaths;
import com.example.demo.core.app.LauncherConfig;
import com.example.demo.core.download.CurseForgeClient;
import com.example.demo.core.download.DownloadManager;
import com.example.demo.core.download.IconProxy;
import com.example.demo.core.model.Favorites;
import com.example.demo.core.mod.BulkDelete;
import com.example.demo.core.mod.ModDependencyScanner;
import com.example.demo.core.mod.ModpackImport;
import com.example.demo.ui.shell.Icons;
import com.example.demo.ui.shell.InAppDialog;
import com.example.demo.ui.shell.Toast;
/**
 * 模组页：左侧本地模组（实时刷新），右侧 Modrinth 在线搜索并一键安装。
 * 全部在启动器内完成，不弹新窗口。
 */
public final class ModsPanel extends VBox {

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(10))
            .build();

    private File modsDir;
    private final LauncherConfig config;
    private final Consumer<String> log;

    private final VBox localList = new VBox(6);
    private ScrollPane localScroll;
    private final Label localCount = new Label();
    private final VBox remoteList = new VBox(6);
    private final StackPane contentHost = new StackPane();
    private VBox localPage;
    private VBox remotePage;
    private String localQuery = "";
    private HBox listBody;
    private final Label status = new Label("");
    private final TextField searchField = new TextField();
    private final ComboBox<String> sourceCombo = new ComboBox<>();
    private ScrollPane remoteScroll;
    private final Label loadMoreHint = new Label();

    /** 在线搜索分页状态：Modrinth 一次只给一页，滚动到底自动续拉。 */
    private static final int PAGE_SIZE = 20;
    private int searchOffset;
    private int searchTotal = -1;
    private boolean loadingMore;
    private final Set<String> loadedProjectIds = new HashSet<>();

    /** 上次扫描的快照，用来判断"变没变"，避免无谓重建 UI。 */
    private String lastSignature = "";
    private Timeline watcher;
    private final Map<String, Image> iconCache = new HashMap<>();

    /** 模组 -> [projectId, versionId, title]，用于更新检测 */
    private final Map<String, String[]> modIndex = new LinkedHashMap<>();
    private final Set<String> updatable = new HashSet<>();
    private final Set<String> selectedMods = new java.util.LinkedHashSet<>();
    private long lastSourceNoticeAt;
    private final Label updateHint = new Label();

    /** 当前实例的版本与加载器，用于自动筛选（null 表示不筛选）。 */
    private String filterMcVersion;
    private String filterLoader;
    private final Label filterHint = new Label();

    /** 前置依赖：解析结果 + 缺失项（modId -> 需要它的模组） */
    private List<ModDependencyScanner.ModInfo> modInfos = List.of();
    private final Map<String, List<String>> missingDeps = new LinkedHashMap<>();
    private final Map<String, List<String>> missingByFile = new LinkedHashMap<>();
    private final Label depsHint = new Label();
    private String metaSignature = "";

    /** @param modsDir 直接就是 mods 目录（调用方用 InstanceDirs.mods(...) 计算） */
    public ModsPanel(File modsDir, LauncherConfig config, Consumer<String> log) {
        this.modsDir = modsDir;
        this.config = config;
        this.log = log;
        if (!modsDir.isDirectory()) modsDir.mkdirs();

        setSpacing(12);
        setPadding(new Insets(22, 24, 22, 24));

        Label title = new Label("模组");
        title.setStyle("-fx-text-fill: -app-text-strong; -fx-font-size: 22px; -fx-font-weight: bold;");
        Label subtitle = new Label("本地模组实时监控 · 在线搜索先选版本再下载");
        subtitle.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 12px;");

        // 单列布局：搜索 + 工具栏 + 全部列表（对齐 PCL / HMCL 的资源列表观感）
        VBox localCard = new VBox(10);
        localCard.getStyleClass().add("neko-card");
        localCard.setPadding(new Insets(16));
        localCard.setMaxWidth(Double.MAX_VALUE);

        TextField localSearch = new TextField();
        localSearch.setPromptText("搜索资源：名称 / 描述 / 标签");
        localSearch.setPrefHeight(36);
        localSearch.setMaxWidth(Double.MAX_VALUE);
        localSearch.textProperty().addListener((o, a, b) -> {
            localQuery = b == null ? "" : b.trim().toLowerCase(Locale.ROOT);
            lastSignature = "";
            refreshLocal(true);
        });

        Button openDir = miniButton("打开文件夹");
        openDir.setOnAction(e -> openModsDir());
        Button installFile = miniButton("从文件安装");
        installFile.setOnAction(e -> installFromFile());
        Button downloadNew = miniButton("下载新资源");
        downloadNew.getStyleClass().add("btn-primary");
        downloadNew.setOnAction(e -> showRemotePage(true));
        CheckBox selectAll = new CheckBox("全选");
        selectAll.setStyle("-fx-text-fill: -app-text-dim; -fx-font-size: 12px;");
        selectAll.selectedProperty().addListener((o, a, b) -> {
            selectedMods.clear();
            if (b) {
                File[] current = modsDir.listFiles((dir, name) -> {
                    String lower = name.toLowerCase(Locale.ROOT);
                    return lower.endsWith(".jar") || lower.endsWith(".jar.disabled");
                });
                if (current != null) {
                    for (File file : current) selectedMods.add(file.getName());
                }
            }
            lastSignature = "";
            refreshLocal(true);
        });
        Button exportInfo = miniButton("导出信息");
        exportInfo.setOnAction(e -> exportModsInfo());
        Button relations = miniButton("模组关系");
        relations.setOnAction(e -> showModRelations());

        javafx.scene.layout.FlowPane toolbar = new javafx.scene.layout.FlowPane(
                8, 6, openDir, installFile, downloadNew, selectAll, exportInfo, relations);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        Label allTitle = new Label("全部");
        allTitle.setStyle("-fx-text-fill: -app-text-2; -fx-font-size: 14px; -fx-font-weight: bold;");
        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        Label sortLabel = new Label("排序");
        sortLabel.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11px;");
        ComboBox<String> sortCombo = new ComboBox<>();
        sortCombo.getItems().addAll("资源名称", "文件大小");
        sortCombo.setValue("资源名称");
        sortCombo.setPrefWidth(110);
        HBox listHeader = new HBox(8, allTitle, localCount, headerSpacer, sortLabel, sortCombo);
        listHeader.setAlignment(Pos.CENTER_LEFT);

        // 批量操作收成第二行，保持首行干净
        javafx.scene.layout.FlowPane bulkRow = new javafx.scene.layout.FlowPane(8, 6);
        Button enableSelected = miniButton("启用选中");
        enableSelected.setOnAction(e -> setSelectedEnabled(true));
        Button disableSelected = miniButton("禁用选中");
        disableSelected.setOnAction(e -> setSelectedEnabled(false));
        Button deleteSelected = miniButton("删除选中");
        deleteSelected.setStyle("-fx-background-color: #7f1d1d; -fx-text-fill: #fecaca; -fx-cursor: hand;"
                + " -fx-background-radius: 6; -fx-padding: 4 10; -fx-font-size: 12px;");
        deleteSelected.setOnAction(e -> deleteSelectedMods());
        Button enableAllBtn = miniButton("全启用");
        enableAllBtn.setOnAction(e -> setAllEnabled(true));
        Button disableAllBtn = miniButton("全禁用");
        disableAllBtn.setOnAction(e -> setAllEnabled(false));
        Button deleteDisabledBtn = miniButton("删除已禁用");
        deleteDisabledBtn.setStyle("-fx-background-color: #7f1d1d; -fx-text-fill: #fecaca; -fx-cursor: hand;"
                + " -fx-background-radius: 6; -fx-padding: 4 10; -fx-font-size: 12px;");
        deleteDisabledBtn.setOnAction(e -> deleteDisabledMods());
        bulkRow.getChildren().addAll(enableSelected, disableSelected, deleteSelected,
                enableAllBtn, disableAllBtn, deleteDisabledBtn);
        bulkRow.setAlignment(Pos.CENTER_LEFT);

        localScroll = new ScrollPane(localList);
        localScroll.setFitToWidth(true);
        localScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(localScroll, Priority.ALWAYS);

        depsHint.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11px;");
        depsHint.setWrapText(true);
        depsHint.setMaxWidth(Double.MAX_VALUE);
        HBox depsRow = new HBox(8, depsHint);
        depsRow.setAlignment(Pos.CENTER_LEFT);

        localPage = new VBox(10, localSearch, toolbar, listHeader, bulkRow, updateHint, depsRow, localScroll);
        localPage.setMaxWidth(Double.MAX_VALUE);

        // ---------- 在线页：独立集成页面（从「下载新资源」进入） ----------
        VBox remoteCard = new VBox(10);
        remoteCard.getStyleClass().add("neko-card");
        remoteCard.setPadding(new Insets(16));
        Button back = new Button("返回列表");
        back.setGraphic(Icons.node("back", 12.0, "#9ca3af"));
        back.setStyle("-fx-background-color: transparent; -fx-text-fill: -app-text-dim;"
                + " -fx-cursor: hand; -fx-font-size: 12px;");
        back.setOnAction(e -> showRemotePage(false));
        Label remoteTitle = new Label("在线模组");
        remoteTitle.setStyle("-fx-text-fill: -app-text-2; -fx-font-size: 15px; -fx-font-weight: bold;");
        Region remoteSpacer = new Region();
        HBox.setHgrow(remoteSpacer, Priority.ALWAYS);
        HBox remoteHeader = new HBox(10, back, remoteTitle, remoteSpacer);
        remoteHeader.setAlignment(Pos.CENTER_LEFT);

        sourceCombo.getItems().addAll("自动", "Modrinth", "CurseForge");
        sourceCombo.setValue("CurseForge");
        sourceCombo.setPrefWidth(120);
        sourceCombo.setStyle("-fx-background-radius: 8;");
        searchField.setPromptText("搜索模组，例如 sodium / jei / create");
        searchField.setPrefHeight(34);
        HBox.setHgrow(searchField, Priority.ALWAYS);
        Button searchBtn = new Button("搜索");
        searchBtn.setStyle("-fx-background-color: #9ca3af; -fx-text-fill: #111827; -fx-font-weight: bold;"
                + " -fx-background-radius: 8; -fx-padding: 7 20; -fx-cursor: hand;");
        searchBtn.setOnAction(e -> search());
        searchField.setOnAction(e -> search());
        HBox searchBar = new HBox(10, searchField, sourceCombo, searchBtn);
        sourceCombo.valueProperty().addListener((o, a, b) -> search());

        status.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 12px;");
        ScrollPane remoteScroll = new ScrollPane(remoteList);
        remoteScroll.setFitToWidth(true);
        remoteScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(remoteScroll, Priority.ALWAYS);
        this.remoteScroll = remoteScroll;
        remoteScroll.vvalueProperty().addListener((obs, old, value) -> {
            if (value.doubleValue() > 0.9 && !loadingMore
                    && (searchTotal < 0 || searchOffset < searchTotal)) {
                loadPage();
            }
        });
        loadMoreHint.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11px;");
        Region statusSpacer = new Region();
        HBox.setHgrow(statusSpacer, Priority.ALWAYS);
        Button toTop = miniButton("↑ 顶部");
        toTop.setOnAction(e -> remoteScroll.setVvalue(0));
        HBox statusRow = new HBox(8, status, statusSpacer, loadMoreHint, toTop);
        statusRow.setAlignment(Pos.CENTER_LEFT);
        filterHint.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 11px;");
        remoteCard.getChildren().addAll(remoteHeader, filterHint, searchBar, statusRow, remoteScroll);
        remotePage = remoteCard;
        remotePage.setVisible(false);
        remotePage.setManaged(false);

        contentHost.getChildren().addAll(localPage, remotePage);
        VBox.setVgrow(contentHost, Priority.ALWAYS);
        getChildren().addAll(title, subtitle, contentHost);

        loadIndex();
        refreshLocal(true);
        remoteList.getChildren().add(hint("还没有搜索。输入关键字后点搜索，或直接看热门模组。"));
        search();
        startWatcher();
    }

    /** 每 2 秒扫一次 mods 目录，有变化才重建列表 —— 这就是"实时加载"。 */
    /** 由实例设置页调用：按版本 + 加载器自动筛选在线模组。 */
    /** 切换实例时换掉 mods 目录（版本隔离）。 */
    public void refreshNow() {
        lastSignature = "";
        metaSignature = "";
        refreshLocal(true);
    }

    public void setModsDir(File dir) {
        if (dir == null || dir.equals(this.modsDir)) return;
        this.modsDir = dir;
        if (!dir.isDirectory()) dir.mkdirs();
        lastSignature = "";
        metaSignature = "";
        refreshLocal(true);
    }

    public void setFilter(String mcVersion, String loader) {
        if (java.util.Objects.equals(this.filterMcVersion, mcVersion)
                && java.util.Objects.equals(this.filterLoader, loader)) {
            return;
        }
        this.filterMcVersion = mcVersion;
        this.filterLoader = loader;
        String text = "未筛选（显示全部模组）";
        if (mcVersion != null || loader != null) {
            StringBuilder sb = new StringBuilder("已按 ");
            sb.append(mcVersion == null ? "任意版本" : mcVersion);
            if (loader != null) sb.append(" · ").append(loader);
            sb.append(" 自动筛选");
            text = sb.toString();
        }
        filterHint.setText(text);
        search();
    }

    private void startWatcher() {
        watcher = new Timeline(new KeyFrame(Duration.seconds(5), e -> {
            if (isVisible()) refreshLocal(false);
        }));
        watcher.setCycleCount(Animation.INDEFINITE);
        visibleProperty().addListener((obs, was, now) -> {
            if (now) {
                lastSignature = "";
                refreshLocal(true);
                watcher.play();
            } else {
                watcher.stop();
            }
        });
        if (isVisible()) watcher.play();
    }

    private String signature(File[] files) {
        StringBuilder sb = new StringBuilder();
        if (files != null) {
            for (File file : files) {
                sb.append(file.getName()).append(':').append(file.length()).append(':')
                        .append(file.lastModified()).append(';');
            }
        }
        return sb.toString();
    }

    /** 在「本地列表」与「在线页」之间切换（在线页是独立集成页面）。 */
    private void showRemotePage(boolean online) {
        if (localPage == null || remotePage == null) return;
        localPage.setVisible(!online);
        localPage.setManaged(!online);
        remotePage.setVisible(online);
        remotePage.setManaged(online);
        if (online && remoteList.getChildren().isEmpty()) {
            search();
        }
    }

    /** 从本地 jar 文件安装到 mods 目录。 */
    private void installFromFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("选择模组 jar");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("模组文件", "*.jar"));
        File pick = chooser.showOpenDialog(getScene() == null ? null : getScene().getWindow());
        if (pick == null) return;
        try {
            File target = new File(modsDir, pick.getName());
            Files.copy(pick.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            log.accept("已从文件安装 " + pick.getName());
            Toast.ok("已安装 " + pick.getName());
            lastSignature = "";
            refreshLocal(true);
        } catch (Exception e) {
            Toast.error("安装失败：" + e.getMessage());
        }
    }

    /** 导出已安装模组清单到 logs/mods-info.txt。 */
    private void exportModsInfo() {
        File[] files = modsDir.listFiles((d, n) -> {
            String lower = n.toLowerCase(Locale.ROOT);
            return lower.endsWith(".jar") || lower.endsWith(".jar.disabled");
        });
        if (files == null) files = new File[0];
        Arrays.sort(files, Comparator.comparing(File::getName));
        StringBuilder sb = new StringBuilder("模组清单（" + files.length + " 个）\n");
        for (File file : files) {
            boolean disabled = file.getName().toLowerCase(Locale.ROOT).endsWith(".disabled");
            sb.append(disabled ? "[禁用] " : "[启用] ")
                    .append(file.getName())
                    .append("  ")
                    .append(file.length() / 1024)
                    .append(" KB\n");
        }
        try {
            File logs = new File(config.getGameDir(), "logs");
            logs.mkdirs();
            File out = new File(logs, "mods-info.txt");
            Files.writeString(out.toPath(), sb.toString(), StandardCharsets.UTF_8);
            log.accept("已导出模组清单: " + out.getAbsolutePath());
            Toast.ok("模组清单已导出到 logs/mods-info.txt");
        } catch (Exception e) {
            Toast.error("导出失败：" + e.getMessage());
        }
    }

    /** 模组关系：把前置依赖扫描结果用弹窗展示。 */
    private void showModRelations() {
        String text = depsHint == null || depsHint.getText() == null || depsHint.getText().isBlank()
                ? "没有检测到需要说明的前置依赖。"
                : depsHint.getText();
        InAppDialog.alert("模组关系", text);
    }

    private void refreshLocal(boolean force) {
        try {
            ModpackImport.flattenMods(modsDir, msg -> status.setText(msg));
        } catch (Exception ignored) {
        }
        File[] files = modsDir.listFiles((d, n) -> {
            String lower = n.toLowerCase();
            return lower.endsWith(".jar") || lower.endsWith(".jar.disabled");
        });
        if (files == null) files = new File[0];
        Arrays.sort(files, Comparator.comparing(File::getName));
        if (!localQuery.isBlank()) {
            String q = localQuery;
            files = Arrays.stream(files)
                    .filter(f -> f.getName().toLowerCase(Locale.ROOT).contains(q))
                    .toArray(File[]::new);
        }

        String signature = signature(files);
        if (!force && signature.equals(lastSignature)) return;
        lastSignature = signature;
        refreshMetadata(signature);

        double keptScroll = localScroll == null ? 0 : localScroll.getVvalue();
        localList.getChildren().clear();
        if (localScroll != null) Platform.runLater(() -> localScroll.setVvalue(keptScroll));
        long enabled = Arrays.stream(files).filter(f -> !f.getName().toLowerCase().endsWith(".disabled")).count();
        localCount.setText("共 " + files.length + " 个 · 启用 " + enabled);

        if (files.length == 0) {
            localList.getChildren().add(hint("mods 目录还是空的。右边搜一个装进来，或者直接把 jar 拖进 "
                    + modsDir.getAbsolutePath()));
            return;
        }
        // 按「已启用 / 已禁用」分组，避免启用和禁用的 jar 混在一起
        java.util.List<File> enabledFiles = new java.util.ArrayList<>();
        java.util.List<File> disabledFiles = new java.util.ArrayList<>();
        for (File file : files) {
            if (file.getName().toLowerCase().endsWith(".disabled")) {
                disabledFiles.add(file);
            } else {
                enabledFiles.add(file);
            }
        }
        if (!enabledFiles.isEmpty()) {
            localList.getChildren().add(modsSection("已启用 · " + enabledFiles.size()));
            for (File file : enabledFiles) {
                localList.getChildren().add(localRow(file));
            }
        }
        if (!disabledFiles.isEmpty()) {
            localList.getChildren().add(modsSection("已禁用 · " + disabledFiles.size()));
            for (File file : disabledFiles) {
                localList.getChildren().add(localRow(file));
            }
        }
        // 列表项入场：错落淡入，避免刷新时整块"啪"地出现
        java.util.List<javafx.scene.Node> rows = new java.util.ArrayList<>();
        for (javafx.scene.Node node : localList.getChildren()) {
            if (node instanceof HBox) {
                rows.add(node);
            }
        }
        if (!rows.isEmpty()) {
            com.example.demo.ui.shell.Motion.stagger(rows);
        }
    }

    private Label modsSection(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: -app-text-dim-3; -fx-font-size: 11px; -fx-font-weight: bold;");
        label.setPadding(new Insets(10, 4, 2, 4));
        return label;
    }

    // ================== 前置依赖 ==================

    /** 目录内容变了才重新读一遍 jar 元数据（每 2 秒的轮询不重复解析）。 */
    private void refreshMetadata(String signature) {
        if (signature.equals(metaSignature)) return;
        metaSignature = signature;
        modInfos = ModDependencyScanner.scan(modsDir);
        missingDeps.clear();
        missingDeps.putAll(ModDependencyScanner.findMissingDependencies(modInfos));
        missingByFile.clear();
        for (ModDependencyScanner.ModInfo info : modInfos) {
            List<String> missing = ModDependencyScanner.missingFor(info, modInfos);
            if (!missing.isEmpty()) missingByFile.put(info.file().getName(), missing);
        }
        updateDepsHint();
    }

    private void updateDepsHint() {
        if (depsHint == null) return;
        if (missingDeps.isEmpty()) {
            depsHint.setText(modInfos.isEmpty() ? "" : "✅ 前置依赖齐全");
            depsHint.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11px;");
            return;
        }
        StringBuilder text = new StringBuilder("⚠ 缺少前置：");
        int shown = 0;
        for (String id : missingDeps.keySet()) {
            if (shown > 0) text.append("、");
            text.append(id);
            if (++shown >= 3) break;
        }
        if (missingDeps.size() > 3) text.append(" 等 ").append(missingDeps.size()).append(" 个");
        depsHint.setText(text.toString());
        depsHint.setStyle("-fx-text-fill: #ffb74d; -fx-font-size: 11px;");
    }
    /** 新装的模组如果还有缺失前置，按设置自动补装（最多递归两层，防止环）。 */
        private HBox localRow(File file) {
        boolean disabled = file.getName().toLowerCase().endsWith(".disabled");
        String displayName = disabled
                ? file.getName().substring(0, file.getName().length() - ".disabled".length())
                : file.getName();

        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 10, 8, 10));
        row.setStyle("-fx-background-color: -app-row3; -fx-background-radius: 8;");

        Label name = new Label(displayName);
        name.setStyle("-fx-text-fill: " + (disabled ? "-app-text-muted" : "-app-text-2") + "; -fx-font-size: 13px;");
        name.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(name, Priority.ALWAYS);

        Label size = new Label(file.length() < 1024
                ? Math.max(1, file.length()) + " B"
                : (file.length() / 1024) + " KB");
        size.setMinWidth(Region.USE_PREF_SIZE);
        size.setStyle("-fx-text-fill: -app-text-muted-2; -fx-font-size: 11px;");

        Button toggle = miniButton(disabled ? "启用" : "禁用");
        toggle.setOnAction(e -> {
            File next = disabled
                    ? new File(file.getParentFile(), file.getName().substring(0, file.getName().length() - 9))
                    : new File(file.getParentFile(), file.getName() + ".disabled");
            if (file.renameTo(next)) {
                log("🧩 " + (disabled ? "已启用 " : "已禁用 ") + next.getName());
            } else {
                log("❌ 重命名失败（文件可能被占用）: " + file.getName());
            }
            lastSignature = "";
            refreshLocal(true);
        });

        Button delete = miniButton("删除");
        delete.setStyle("-fx-background-color: #7f1d1d; -fx-text-fill: #fecaca; -fx-cursor: hand;"
                + " -fx-background-radius: 6; -fx-padding: 4 10; -fx-font-size: 12px;");
        delete.setOnAction(e -> {
            if (file.delete()) {
                log("🗑️ 已删除 " + file.getName());
            } else {
                log("❌ 删除失败: " + file.getName());
            }
            lastSignature = "";
            refreshLocal(true);
        });

        if (updatable.contains(file.getName())) {
            Button update = miniButton("更新");
            update.setStyle("-fx-background-color: #9ca3af; -fx-text-fill: #111827; -fx-cursor: hand;"
                    + " -fx-background-radius: 6; -fx-padding: 4 10; -fx-font-size: 12px;");
            update.setOnAction(e -> updateMod(file, update));
            row.getChildren().add(update);
        }
        List<String> missingDepsOfFile = missingByFile.get(file.getName());
        if (missingDepsOfFile != null && !missingDepsOfFile.isEmpty()) {
            Label depTag = new Label("缺前置");
            depTag.setStyle("-fx-text-fill: #ffb74d; -fx-font-size: 10px;"
                    + " -fx-background-color: rgba(255,183,77,0.14); -fx-background-radius: 4; -fx-padding: 2 6;");
            depTag.setTooltip(new Tooltip("缺少: " + String.join("、", missingDepsOfFile)));
            row.getChildren().add(depTag);
        }
        CheckBox select = new CheckBox();
        select.setSelected(selectedMods.contains(file.getName()));
        select.selectedProperty().addListener((o, a, b) -> {
            if (b) selectedMods.add(file.getName());
            else selectedMods.remove(file.getName());
        });
        row.getChildren().addAll(select, name, size, toggle, delete);
        return row;
    }

    // ================== 在线搜索 / 安装 ==================

    /** 供 AI 助手 / 外部调用：带关键词打开搜索。 */
    public void searchFor(String query) {
        if (query == null || query.isBlank()) return;
        searchField.setText(query.trim());
        search();
    }

    /** 重新搜索：清空列表，从第一页开始。 */
    private void search() {
        loadedProjectIds.clear();
        searchOffset = 0;
        searchTotal = -1;
        loadingMore = false;
        remoteList.getChildren().clear();
        status.setText("正在从 Modrinth 获取…");
        loadMoreHint.setText("");
        loadPage();
    }

    /** 拉一页在线结果；滚动到底自动续拉。 */
    /** 源不可用轻提示：不打断、不要求确认，15 秒内只提醒一次。 */
    private void notifySourceUnavailable(String message) {
        long now = System.currentTimeMillis();
        if (now - lastSourceNoticeAt < 15000) return;
        lastSourceNoticeAt = now;
        Toast.info(message);
    }

    private void searchCurseForge(boolean reset) {
        if (reset) {
            searchOffset = 0;
            searchTotal = -1;
            loadingMore = true;
            remoteList.getChildren().clear();
            loadedProjectIds.clear();
            loadMoreHint.setText("");
            status.setText("正在从 MCIM 镜像获取…");
        } else {
            if (loadingMore) return;
            if (searchTotal >= 0 && searchOffset >= searchTotal) return;
            loadingMore = true;
            loadMoreHint.setText("加载中…");
            status.setText("正在从 MCIM 镜像加载更多…");
        }
        final int offset = searchOffset;
        final String query = searchField.getText() == null ? "" : searchField.getText().trim();
        CompletableFuture.runAsync(() -> {
            try {
                CurseForgeClient.MirrorPage page = CurseForgeClient.searchMirror(
                        query, filterMcVersion, filterLoader, offset, PAGE_SIZE);
                Platform.runLater(() -> {
                    for (CurseForgeClient.Hit hit : page.hits()) {
                        if (!loadedProjectIds.add("cf:" + hit.id())) continue;
                        remoteList.getChildren().add(cfRow(hit));
                    }
                    searchOffset = offset + page.hits().size();
                    searchTotal = page.total();
                    loadingMore = false;
                    updateRemoteFooter();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    loadingMore = false;
                    status.setText("MCIM 不可用，切换到 Modrinth…");
                    Toast.info("MCIM 镜像无响应，自动切到 Modrinth");
                    if (!"Modrinth".equals(sourceCombo.getValue())) {
                        sourceCombo.setValue("Modrinth");
                    } else {
                        updateRemoteFooter();
                    }
                });
            }
        });
    }

    private HBox cfRow(CurseForgeClient.Hit hit) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8));
        row.setStyle("-fx-background-color: -app-row2; -fx-background-radius: 10;");

        StackPane icon = new StackPane();
        icon.setPrefSize(46, 46);
        icon.setMinSize(46, 46);
        icon.setStyle("-fx-background-color: -app-row3; -fx-background-radius: 10;");
        Label letter = new Label(hit.name().isEmpty() ? "?" : hit.name().substring(0, 1).toUpperCase());
        letter.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 20px; -fx-font-weight: bold;");
        icon.getChildren().add(letter);
        loadIcon(icon, hit.iconUrl(), hit.name());

        VBox info = new VBox(3);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label name = new Label(hit.name());
        name.setStyle("-fx-text-fill: -app-text-strong; -fx-font-size: 14px; -fx-font-weight: bold;");
        Label desc = new Label(hit.summary());
        desc.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11.5px;");
        desc.setMaxWidth(520);
        Label meta = new Label("⤓ " + formatCount(hit.downloads())
                + (hit.author().isBlank() ? "" : "   👤 " + hit.author())
                + "   ⛏ CurseForge 镜像");
        meta.setStyle("-fx-text-fill: -app-text-dim-2; -fx-font-size: 11px;");
        info.getChildren().addAll(name, desc, meta);

        Button detail = new Button("详情");
        detail.setStyle("-fx-background-color: #9ca3af; -fx-text-fill: #111827; -fx-font-weight: bold;"
                + " -fx-background-radius: 8; -fx-padding: 6 18; -fx-cursor: hand;");
        detail.setOnAction(e -> openCfDetail(hit));
        row.setOnMouseClicked(e -> openCfDetail(hit));
        Button quick = new Button("安装");
        quick.setStyle("-fx-background-color: #e11d2e; -fx-text-fill: white; -fx-font-weight: bold;"
                + " -fx-background-radius: 8; -fx-padding: 6 12; -fx-cursor: hand;");
        quick.setMinWidth(Region.USE_PREF_SIZE);
        quick.setOnAction(e -> installCfMirror(hit, quick));
        quick.setOnMouseClicked(e -> e.consume());
        row.getChildren().addAll(icon, info, quick, detail);
        return row;
    }

    private void installCfMirror(CurseForgeClient.Hit hit, Button button) {
        CompletableFuture.runAsync(() -> {
            List<CurseForgeClient.MirrorFile> files = List.of();
            try {
                files = CurseForgeClient.mirrorFiles(hit.id(), filterMcVersion, filterLoader);
            } catch (Exception ignored) {
                // 落到 cfwidget
            }
            if (files.isEmpty()) {
                Platform.runLater(() -> button.setText("cfwidget 查询…"));
                try {
                    CurseForgeClient.Project project = CurseForgeClient.lookup(hit.slug());
                    List<CurseForgeClient.FileEntry> matched =
                            CurseForgeClient.matching(project, filterMcVersion, filterLoader);
                    List<CurseForgeClient.MirrorFile> converted = new ArrayList<>();
                    for (CurseForgeClient.FileEntry entry : matched) {
                        converted.add(new CurseForgeClient.MirrorFile(entry.id(), entry.name(), entry.name(),
                                entry.directUrl(), "release".equals(entry.type()) ? 1 : 2, 0));
                    }
                    files = converted;
                } catch (Exception ignored) {
                    // 保持空列表
                }
            }
            final List<CurseForgeClient.MirrorFile> result = files;
            Platform.runLater(() -> {
                if (result.isEmpty()) {
                    button.setDisable(false);
                    button.setText("安装");
                    log("❌ CurseForge 查询失败（MCIM 与 cfwidget 都没有可用文件）");
                    notifySourceUnavailable("CurseForge 源暂时不可用");
                    return;
                }
                int chosen = chooseMirrorFile(hit.name(), result);
                if (chosen < 0) {
                    button.setDisable(false);
                    button.setText("安装");
                    return;
                }
                button.setText("下载中…");
                downloadMirrorFile(result.get(chosen), hit, button);
            });
        });
    }

    private int chooseMirrorFile(String title, List<CurseForgeClient.MirrorFile> files) {
        List<String> labels = new ArrayList<>();
        int limit = Math.min(30, files.size());
        for (int i = 0; i < limit; i++) labels.add(files.get(i).label());
        if (labels.isEmpty()) return -1;
        javafx.scene.control.ChoiceDialog<String> dialog =
                new javafx.scene.control.ChoiceDialog<>(labels.get(0), labels);
        dialog.setTitle(title + " · CurseForge 文件");
        dialog.setHeaderText("选择要安装的文件（已按当前实例筛选）");
        dialog.setContentText("文件：");
        return dialog.showAndWait().map(labels::indexOf).orElse(-1);
    }

    private void downloadMirrorFile(CurseForgeClient.MirrorFile file, CurseForgeClient.Hit hit, Button button) {
        CompletableFuture.runAsync(() -> {
            try {
                File out = new File(modsDir, file.fileName());
                List<String> urls = file.urls();
                String[] labels = {"官方 CDN", "edge 镜像", "mediafilez 镜像", "MCIM 文件镜像"};
                boolean ok = false;
                for (int i = 0; i < urls.size(); i++) {
                    final String label = i < labels.length ? labels[i] : "备用源";
                    Platform.runLater(() -> button.setText("从 " + label + " 下载…"));
                    DownloadManager.Stats stats = DownloadManager.downloadAll(
                            List.of(new DownloadManager.Task(urls.get(i), out, null, -1)), 1, null);
                    if (stats.failed == 0 && out.isFile() && out.length() > 0) {
                        ok = true;
                        final String used = label;
                        Platform.runLater(() -> log("⬇ 下载源：" + used));
                        break;
                    }
                }
                if (!ok) {
                    Toast.info("下载源暂时不可用，稍后再试");
                    throw new RuntimeException("所有下载地址都失败");
                }
                Downloaded downloaded = new Downloaded(file.fileName(), String.valueOf(hit.id()), "", hit.name());
                Platform.runLater(() -> {
                    button.setDisable(false);
                    button.setText("安装");
                    rememberDownloaded(downloaded);
                    log("✅ 已从 CurseForge 镜像安装 " + file.fileName());
                    Toast.ok("已安装 " + hit.name());
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    button.setDisable(false);
                    button.setText("安装");
                    log("❌ CurseForge 下载失败: " + e.getMessage());
                });
            }
        });
    }
    private void loadPage() {
        if (loadingMore) return;
        if (searchTotal >= 0 && searchOffset >= searchTotal) return;
        loadingMore = true;
        final int offset = searchOffset;
        final String query = searchField.getText() == null ? "" : searchField.getText().trim();
        loadMoreHint.setText(offset == 0 ? "" : "加载中…");

        CompletableFuture.runAsync(() -> {
            try {
                JsonArray facets = new JsonArray();
                JsonArray type = new JsonArray();
                type.add("project_type:mod");
                facets.add(type);
                if (filterMcVersion != null) {
                    JsonArray versions = new JsonArray();
                    versions.add("versions:" + filterMcVersion);
                    facets.add(versions);
                }
                if (filterLoader != null) {
                    JsonArray loaders = new JsonArray();
                    loaders.add("categories:" + filterLoader);
                    facets.add(loaders);
                }
                String url = "https://api.modrinth.com/v2/search?query="
                        + URLEncoder.encode(query, StandardCharsets.UTF_8)
                        + "&facets=" + URLEncoder.encode(facets.toString(), StandardCharsets.UTF_8)
                        + "&index=downloads&limit=" + PAGE_SIZE + "&offset=" + offset;
                HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                        .timeout(java.time.Duration.ofSeconds(20))
                        .header("User-Agent", "MCLauncher/1.0")
                        .GET().build();
                HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
                JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                JsonArray hits = json.getAsJsonArray("hits");
                int total = json.has("total_hits") && !json.get("total_hits").isJsonNull()
                        ? json.get("total_hits").getAsInt() : -1;

                List<JsonObject> page = new ArrayList<>();
                if (hits != null) {
                    for (JsonElement element : hits) {
                        page.add(element.getAsJsonObject());
                    }
                }
                Platform.runLater(() -> {
                    loadingMore = false;
                    if (page.isEmpty()) {
                        if (offset == 0) {
                            status.setText("没有找到匹配的模组");
                            loadMoreHint.setText("");
                        }
                        return;
                    }
                    for (JsonObject hit : page) {
                        String projectId = hit.has("project_id") ? hit.get("project_id").getAsString() : "";
                        if (!projectId.isBlank() && !loadedProjectIds.add(projectId)) continue;
                        remoteList.getChildren().add(remoteRow(hit));
                    }
                    searchOffset = offset + page.size();
                    if (total >= 0) {
                        searchTotal = total;
                    } else if (page.size() < PAGE_SIZE) {
                        searchTotal = searchOffset;
                    }
                    updateRemoteFooter();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    loadingMore = false;
                    status.setText("加载失败: " + e.getMessage());
                    updateRemoteFooter();
                });
            }
        });
    }

    private void updateRemoteFooter() {
        boolean hasMore = searchTotal < 0 || searchOffset < searchTotal;
        status.setText(searchTotal < 0
                ? "已加载 " + searchOffset + " 个"
                : "共 " + formatCount(searchTotal) + " 个 · 已加载 " + searchOffset + " 个");
        loadMoreHint.setText(hasMore ? "滚动到底自动加载" : "已全部加载");
    }

    private HBox remoteRow(JsonObject hit) {
        String projectId = hit.has("project_id") ? hit.get("project_id").getAsString() : "";
        String slug = hit.has("slug") ? hit.get("slug").getAsString() : projectId;
        String title = hit.has("title") ? hit.get("title").getAsString() : "未知";
        String description = hit.has("description") ? hit.get("description").getAsString() : "";
        String iconUrl = hit.has("icon_url") && !hit.get("icon_url").isJsonNull()
                ? hit.get("icon_url").getAsString() : null;
        long downloads = hit.has("downloads") ? hit.get("downloads").getAsLong() : 0;

        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10));
        row.getStyleClass().add("mod-row");

        StackPane icon = new StackPane();
        icon.setPrefSize(46, 46);
        icon.setMinSize(46, 46);
        icon.setStyle("-fx-background-color: -app-row2; -fx-background-radius: 10;");
        Label letter = new Label(title.isEmpty() ? "?" : title.substring(0, 1).toUpperCase());
        letter.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 20px; -fx-font-weight: bold;");
        icon.getChildren().add(letter);
        loadIcon(icon, iconUrl, title);

        VBox info = new VBox(3);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label name = new Label(title);
        name.setStyle("-fx-text-fill: -app-text-strong; -fx-font-size: 14px; -fx-font-weight: bold;");
        Label desc = new Label(description);
        desc.setStyle("-fx-text-fill: -app-text-muted-3; -fx-font-size: 12px;");
        desc.setMaxWidth(Double.MAX_VALUE);
        Label meta = new Label("⬇ " + formatCount(downloads));
        meta.setStyle("-fx-text-fill: -app-text-muted-2; -fx-font-size: 11px;");
        info.getChildren().addAll(name, desc, meta);

        Button star = new Button();
        star.setGraphic(Icons.node("star", 16, Favorites.contains(projectId) ? "#eab308" : "#6b7280"));
        star.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 0;");
        star.setMinSize(26, 26);
        star.setPrefSize(26, 26);
        star.setOnAction(e -> {
            Favorites.toggle(new Favorites.Fav(projectId, title, "mod", iconUrl, downloads, System.currentTimeMillis()));
            boolean now = Favorites.contains(projectId);
            star.setGraphic(Icons.node("star", 16, now ? "#eab308" : "#6b7280"));
            Toast.ok(now ? "已收藏 " + title : "已取消收藏 " + title);
        });

        Button detail = miniButton("详情");
        detail.setOnAction(e -> openModDetail(projectId, title));
        row.setOnMouseClicked(e -> openModDetail(projectId, title));
        Button quick = new Button("安装");
        quick.setStyle("-fx-background-color: #e11d2e; -fx-text-fill: white; -fx-font-weight: bold;"
                + " -fx-background-radius: 8; -fx-padding: 5 10; -fx-cursor: hand; -fx-font-size: 11px;");
        quick.setMinWidth(Region.USE_PREF_SIZE);
        quick.setOnAction(e -> installMod(projectId, slug, title, quick));
        quick.setOnMouseClicked(e -> e.consume());
        row.getChildren().addAll(icon, info, star, quick, detail);
        return row;
    }
    public void openProjectDetail(String projectId, String title) {
        openModDetail(projectId, title);
    }

    /** 打开模组详情页（不是弹窗，也不是直接安装最新版）。 */
    private void openModDetail(String projectId, String title) {
        VBox page = detailShell(title, "Modrinth");
        showModPage(page);
        CompletableFuture.runAsync(() -> {
            try {
                JsonObject project = getJsonObject("https://api.modrinth.com/v2/project/"
                        + URLEncoder.encode(projectId, StandardCharsets.UTF_8));
                JsonArray versions = fetchVersions(projectId);
                Platform.runLater(() -> showModPage(buildModDetailPage(projectId, title, project, versions)));
            } catch (Exception e) {
                Platform.runLater(() -> {
                    page.getChildren().add(hint("详情加载失败：" + e.getMessage()));
                    logLater("❌ 详情加载失败: " + e.getMessage());
                });
            }
        });
    }

    private void showModPage(VBox page) {
        contentHost.getChildren().setAll(page);
    }

    private void showModList() {
        contentHost.getChildren().setAll(listBody);
    }

    private VBox detailShell(String title, String source) {
        VBox page = new VBox(10);
        page.setPadding(new Insets(4, 2, 4, 2));
        page.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        Button back = new Button("返回模组列表");
        back.setGraphic(Icons.node("back", 13, "#9ca3af"));
        back.setGraphicTextGap(4);
        back.setStyle("-fx-background-color: transparent; -fx-text-fill: -app-text-dim;"
                + " -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 2 4;");
        back.setOnAction(e -> showModList());

        Label heading = new Label(title);
        heading.setStyle("-fx-text-fill: -app-text; -fx-font-size: 18px; -fx-font-weight: bold;");
        Label sourceLabel = new Label(source);
        sourceLabel.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11px;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox top = new HBox(10, back, heading, spacer, sourceLabel);
        top.setAlignment(Pos.CENTER_LEFT);
        page.getChildren().add(top);
        return page;
    }

    private VBox buildModDetailPage(String projectId, String title, JsonObject project, JsonArray versions) {
        VBox page = detailShell(title, "Modrinth");
        Label desc = new Label(project == null ? "" : str(project, "description"));
        desc.setWrapText(true);
        desc.setStyle("-fx-text-fill: -app-text-2; -fx-font-size: 13px;");

        String metaText = "当前实例过滤：" + (filterMcVersion == null || filterMcVersion.isBlank()
                ? "未指定版本" : filterMcVersion)
                + " / " + (filterLoader == null || filterLoader.isBlank() ? "未指定加载器" : filterLoader);
        Label meta = new Label(metaText);
        meta.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11px;");

        VBox hero = new VBox(6, desc, meta);
        hero.setPadding(new Insets(14));
        hero.setStyle("-fx-background-color: -app-card; -fx-background-radius: 12;"
                + " -fx-border-color: -app-border-2; -fx-border-radius: 12;");

        Label versionTitle = new Label("选择版本（按当前实例过滤）");
        versionTitle.setStyle("-fx-text-fill: -app-text-2; -fx-font-size: 12px; -fx-font-weight: bold;");
        VBox list = new VBox(6);
        int limit = versions == null ? 0 : Math.min(30, versions.size());
        for (int i = 0; i < limit; i++) {
            JsonObject version = versions.get(i).getAsJsonObject();
            list.getChildren().add(modVersionRow(projectId, title, version));
        }
        if (limit == 0) {
            list.getChildren().add(hint("当前实例没有匹配版本，可以先在实例设置里调整版本/加载器。"));
        }
        VBox content = new VBox(10, hero, versionTitle, list);
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        page.getChildren().add(scroll);
        return page;
    }

    private HBox modVersionRow(String projectId, String title, JsonObject version) {
        String number = str(version, "version_number");
        String type = str(version, "version_type");
        JsonArray files = version.has("files") && version.get("files").isJsonArray()
                ? version.getAsJsonArray("files") : new JsonArray();
        String fileName = "";
        long size = 0;
        if (!files.isEmpty()) {
            JsonObject file = files.get(0).getAsJsonObject();
            fileName = str(file, "filename");
            size = file.has("size") ? file.get("size").getAsLong() : 0;
        }
        StringBuilder games = new StringBuilder();
        if (version.has("game_versions") && version.get("game_versions").isJsonArray()) {
            for (JsonElement element : version.getAsJsonArray("game_versions")) {
                if (games.length() > 0) games.append(" / ");
                games.append(element.getAsString());
            }
        }
        StringBuilder loaders = new StringBuilder();
        if (version.has("loaders") && version.get("loaders").isJsonArray()) {
            for (JsonElement element : version.getAsJsonArray("loaders")) {
                if (loaders.length() > 0) loaders.append(" / ");
                loaders.append(element.getAsString());
            }
        }
        int deps = version.has("dependencies") && version.get("dependencies").isJsonArray()
                ? version.getAsJsonArray("dependencies").size() : 0;

        Label titleLabel = new Label(number.isBlank() ? "未知版本" : number);
        titleLabel.setStyle("-fx-text-fill: -app-text-strong; -fx-font-size: 13px; -fx-font-weight: bold;");
        Label typeLabel = new Label("release".equals(type) ? "正式版" : ("beta".equals(type) ? "测试版" : type));
        typeLabel.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 10px;");
        HBox titleRow = new HBox(8, titleLabel, typeLabel);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label fileLabel = new Label((fileName.isBlank() ? "无文件名" : fileName)
                + (size > 0 ? " · " + (size / 1024) + " KB" : ""));
        fileLabel.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11px;");
        Label compat = new Label((games.length() == 0 ? "版本未知" : games.toString())
                + " · " + (loaders.length() == 0 ? "加载器未知" : loaders.toString())
                + (deps > 0 ? " · 含 " + deps + " 个前置" : ""));
        compat.setStyle("-fx-text-fill: -app-text-dim-2; -fx-font-size: 10.5px;");
        VBox info = new VBox(3, titleRow, fileLabel, compat);
        HBox.setHgrow(info, Priority.ALWAYS);

        Button download = new Button("下载此文件");
        download.setStyle("-fx-background-color: #9ca3af; -fx-text-fill: #111827; -fx-font-weight: bold;"
                + " -fx-background-radius: 8; -fx-padding: 6 14; -fx-cursor: hand;");
        download.setOnAction(e -> {
            download.setDisable(true);
            download.setText("下载中…");
            installVersionAsync(version, projectId, title);
        });

        HBox row = new HBox(12, info, download);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 12, 10, 12));
        row.setStyle("-fx-background-color: -app-row2; -fx-background-radius: 10;"
                + " -fx-border-color: -app-border-soft; -fx-border-radius: 10;");
        return row;
    }

    /** CurseForge 镜像详情：先选文件再下载。 */
    private void openCfDetail(CurseForgeClient.Hit hit) {
        VBox page = detailShell(hit.name(), "CurseForge 镜像");
        showModPage(page);
        CompletableFuture.runAsync(() -> {
            List<CurseForgeClient.MirrorFile> files = List.of();
            String summary = hit.summary();
            try {
                List<CurseForgeClient.MirrorFile> fetched =
                        CurseForgeClient.mirrorFiles(hit.id(), filterMcVersion, filterLoader);
                if (fetched != null) files = fetched;
                CurseForgeClient.Project project = CurseForgeClient.lookup(hit.slug());
                if (project != null && project.summary() != null) summary = project.summary();
            } catch (Exception ignored) {
            }
            final List<CurseForgeClient.MirrorFile> result = files;
            final String desc = summary;
            Platform.runLater(() -> showModPage(buildCfDetailPage(hit, desc, result)));
        });
    }

    private VBox buildCfDetailPage(CurseForgeClient.Hit hit, String summary, List<CurseForgeClient.MirrorFile> files) {
        VBox page = detailShell(hit.name(), "CurseForge 镜像");
        Label desc = new Label(summary == null ? "" : summary);
        desc.setWrapText(true);
        desc.setStyle("-fx-text-fill: -app-text-2; -fx-font-size: 13px;");
        Label meta = new Label("当前实例过滤：" + (filterMcVersion == null ? "未指定版本" : filterMcVersion)
                + " / " + (filterLoader == null ? "未指定加载器" : filterLoader));
        meta.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11px;");
        VBox hero = new VBox(6, desc, meta);
        hero.setPadding(new Insets(14));
        hero.setStyle("-fx-background-color: -app-card; -fx-background-radius: 12;"
                + " -fx-border-color: -app-border-2; -fx-border-radius: 12;");

        VBox list = new VBox(6);
        int limit = Math.min(30, files.size());
        for (int i = 0; i < limit; i++) {
            CurseForgeClient.MirrorFile file = files.get(i);
            Label name = new Label(file.displayName());
            name.setStyle("-fx-text-fill: -app-text-strong; -fx-font-size: 12px; -fx-font-weight: bold;");
            Label info = new Label(file.label());
            info.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11px;");
            VBox text = new VBox(2, name, info);
            HBox.setHgrow(text, Priority.ALWAYS);
            Button download = new Button("下载此文件");
            download.setStyle("-fx-background-color: #9ca3af; -fx-text-fill: #111827; -fx-font-weight: bold;"
                    + " -fx-background-radius: 8; -fx-padding: 6 14; -fx-cursor: hand;");
            download.setOnAction(e -> {
                download.setDisable(true);
                download.setText("下载中…");
                downloadMirrorFile(file, hit, download);
            });
            HBox row = new HBox(12, text, download);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(10, 12, 10, 12));
            row.setStyle("-fx-background-color: -app-row2; -fx-background-radius: 10;"
                    + " -fx-border-color: -app-border-soft; -fx-border-radius: 10;");
            list.getChildren().add(row);
        }
        if (limit == 0) list.getChildren().add(hint("没有匹配当前实例的文件。"));
        ScrollPane scroll = new ScrollPane(list);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        page.getChildren().addAll(hero, scroll);
        return page;
    }

    private void installVersionAsync(JsonObject version, String projectId, String title) {
        Toast.info("开始安装 " + title);
        CompletableFuture.runAsync(() -> {
            try {
                Downloaded downloaded = downloadFromVersion(version, projectId, title);
                long size = new File(modsDir, downloaded.fileName()).length();
                Platform.runLater(() -> {
                    rememberDownloaded(downloaded);
                    log("✅ 已安装模组 " + downloaded.fileName() + "（" + (size / 1024) + " KB）");
                    Toast.ok("已安装 " + downloaded.title());
                });
            } catch (Exception e) {
                logLater("❌ 安装失败: " + e.getMessage());
            }
        });
    }

    /** 双源安装：按来源选择先试 CurseForge（cfwidget），失败再回退 Modrinth。 */
    private void installMod(String projectId, String slug, String title, Button button) {
        String source = sourceCombo.getValue() == null ? "自动" : sourceCombo.getValue();
        if ("Modrinth".equals(source)) {
            installModrinth(projectId, title, button);
            return;
        }
        button.setDisable(true);
        button.setText("CF 查询…");
        CompletableFuture.runAsync(() -> {
            CurseForgeClient.Project project = CurseForgeClient.lookup(slug);
            List<CurseForgeClient.FileEntry> files =
                    CurseForgeClient.matching(project, filterMcVersion, filterLoader);
            if (!files.isEmpty()) {
                Platform.runLater(() -> {
                    int chosen = chooseCurseForgeFile(title, files);
                    if (chosen < 0) {
                        button.setDisable(false);
                        button.setText("安装");
                        return;
                    }
                    button.setText("安装中…");
                    installCurseForgeFile(files.get(chosen), title, button);
                });
                return;
            }
            Platform.runLater(() -> {
                button.setDisable(false);
                button.setText("安装");
                if ("CurseForge".equals(source)) {
                    Toast.error("CurseForge 上没有匹配 " + filterMcVersion + " / " + filterLoader + " 的文件");
                } else {
                    installModrinth(projectId, title, button);
                }
            });
        });
    }

    private int chooseCurseForgeFile(String title, List<CurseForgeClient.FileEntry> files) {
        List<String> labels = new ArrayList<>();
        int limit = Math.min(30, files.size());
        for (int i = 0; i < limit; i++) {
            labels.add(files.get(i).label());
        }
        if (labels.isEmpty()) return -1;
        javafx.scene.control.ChoiceDialog<String> dialog =
                new javafx.scene.control.ChoiceDialog<>(labels.get(0), labels);
        dialog.setTitle(title + " · CurseForge 文件");
        dialog.setHeaderText("选择 CurseForge 文件（已按当前实例筛选）");
        dialog.setContentText("文件：");
        return dialog.showAndWait().map(labels::indexOf).orElse(-1);
    }

    private void installCurseForgeFile(CurseForgeClient.FileEntry file, String title, Button button) {
        Toast.info("从 CurseForge 安装 " + title);
        CompletableFuture.runAsync(() -> {
            try {
                File out = new File(modsDir, file.name());
                boolean ok = false;
                for (String url : file.urls()) {
                    DownloadManager.Stats stats = DownloadManager.downloadAll(
                            List.of(new DownloadManager.Task(url, out, null, -1)), 1, null);
                    if (stats.failed == 0 && out.isFile() && out.length() > 0) {
                        ok = true;
                        break;
                    }
                }
                if (!ok) {
                    throw new RuntimeException("下载失败（所有 CDN 地址都不可用）");
                }
                Downloaded downloaded = new Downloaded(file.name(), "", "", title);
                Platform.runLater(() -> {
                    button.setDisable(false);
                    button.setText("安装");
                    rememberDownloaded(downloaded);
                    log("✅ 已从 CurseForge 安装 " + file.name());
                    Toast.ok("已从 CurseForge 安装 " + title);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    button.setDisable(false);
                    button.setText("安装");
                    log("❌ CurseForge 安装失败: " + e.getMessage());
                });
            }
        });
    }
    private void installModrinth(String projectId, String title, Button button) {
        Toast.info("获取 " + title + " 的版本列表…");
        button.setDisable(true);
        button.setText("选择版本…");
        CompletableFuture.runAsync(() -> {
            try {
                JsonArray versions = fetchVersions(projectId);
                if (versions == null || versions.isEmpty()) {
                    throw new RuntimeException("没有匹配当前版本/加载器的文件");
                }
                Platform.runLater(() -> {
                    int chosen = chooseModVersion(title, versions);
                    if (chosen < 0) {
                        button.setDisable(false);
                        button.setText("安装");
                        return;
                    }
                    JsonObject version = versions.get(chosen).getAsJsonObject();
                    button.setText("安装中…");
                    CompletableFuture.runAsync(() -> {
                        try {
                            Downloaded downloaded = downloadFromVersion(version, projectId, title);
                            long size = new File(modsDir, downloaded.fileName()).length();
                            Platform.runLater(() -> {
                                button.setDisable(false);
                                button.setText("安装");
                                rememberDownloaded(downloaded);
                                log("✅ 已安装模组 " + downloaded.fileName() + "（" + (size / 1024) + " KB）");
                                Toast.ok("已安装 " + downloaded.title());
                            });
                        } catch (Exception e) {
                            Platform.runLater(() -> {
                                button.setDisable(false);
                                button.setText("安装");
                                log("❌ 安装失败: " + e.getMessage());
                            });
                        }
                    });
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    button.setDisable(false);
                    button.setText("安装");
                    log("❌ 获取版本失败: " + e.getMessage());
                });
            }
        });
    }

    private JsonArray fetchVersions(String projectRef) throws Exception {
        String url = "https://api.modrinth.com/v2/project/"
                + URLEncoder.encode(projectRef, StandardCharsets.UTF_8) + "/version" + filterQuery();
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(java.time.Duration.ofSeconds(20))
                .header("User-Agent", "MCLauncher/1.0")
                .GET().build();
        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) return null;
        return JsonParser.parseString(response.body()).getAsJsonArray();
    }

    /** 版本选择对话框：显示版本号 / 适配游戏版本 / 加载器 / 类型。 */
    private int chooseModVersion(String title, JsonArray versions) {
        java.util.List<String> labels = new java.util.ArrayList<>();
        int limit = Math.min(30, versions.size());
        for (int i = 0; i < limit; i++) {
            JsonObject version = versions.get(i).getAsJsonObject();
            String number = str(version, "version_number");
            String type = str(version, "version_type");
            String games = "";
            if (version.has("game_versions") && version.get("game_versions").isJsonArray()) {
                JsonArray array = version.getAsJsonArray("game_versions");
                java.util.List<String> parts = new java.util.ArrayList<>();
                for (int j = 0; j < Math.min(3, array.size()); j++) {
                    parts.add(array.get(j).getAsString());
                }
                games = String.join(", ", parts);
            }
            String loaders = "";
            if (version.has("loaders") && version.get("loaders").isJsonArray()) {
                JsonArray array = version.getAsJsonArray("loaders");
                java.util.List<String> parts = new java.util.ArrayList<>();
                for (int j = 0; j < Math.min(2, array.size()); j++) {
                    parts.add(array.get(j).getAsString());
                }
                loaders = String.join(", ", parts);
            }
            String label = (number.isBlank() ? "?" : number)
                    + (games.isBlank() ? "" : "  ·  " + games)
                    + (loaders.isBlank() ? "" : "  ·  " + loaders)
                    + ("release".equals(type) ? "" : "  ·  " + type);
            labels.add(label);
        }
        if (labels.isEmpty()) return -1;
        javafx.scene.control.ChoiceDialog<String> dialog =
                new javafx.scene.control.ChoiceDialog<>(labels.get(0), labels);
        dialog.setTitle(title + " 版本选择");
        dialog.setHeaderText("选择要安装的文件版本（默认最新）");
        dialog.setContentText("版本：");
        java.util.Optional<String> picked = dialog.showAndWait();
        return picked.map(labels::indexOf).orElse(-1);
    }

    /** 从 Modrinth 下载一个模组文件到 mods 目录（不碰 UI，可在后台线程调用）。 */
    private Downloaded downloadProjectVersion(String projectRef, String title) throws Exception {
        JsonObject version = fetchVersion(projectRef);
        if (version == null) throw new RuntimeException("Modrinth 上没有匹配当前版本/加载器的文件");
        return downloadFromVersion(version, projectRef, title);
    }

    private Downloaded downloadFromVersion(JsonObject version, String projectRef, String title) throws Exception {
        JsonArray files = version.getAsJsonArray("files");
        if (files == null || files.isEmpty()) throw new RuntimeException("这个版本没有可下载文件");

        JsonObject target = null;
        for (JsonElement element : files) {
            JsonObject file = element.getAsJsonObject();
            if (file.has("primary") && file.get("primary").getAsBoolean()) {
                target = file;
                break;
            }
        }
        if (target == null) target = files.get(0).getAsJsonObject();

        String url = target.get("url").getAsString();
        String fileName = target.get("filename").getAsString();
        String sha1 = null;
        long expected = target.has("size") ? target.get("size").getAsLong() : -1;
        if (target.has("hashes") && target.get("hashes").isJsonObject()) {
            JsonObject hashes = target.getAsJsonObject("hashes");
            if (hashes.has("sha1")) sha1 = hashes.get("sha1").getAsString();
        }

        File out = new File(modsDir, fileName);
        DownloadManager.Stats stats = DownloadManager.downloadAll(
                List.of(new DownloadManager.Task(url, out, sha1, expected)), 1, null);
        boolean ok = stats.failed == 0 && out.isFile() && out.length() > 0;
        if (!ok) throw new RuntimeException("下载失败: " + fileName);

        String resolvedProject = str(version, "project_id");
        if (resolvedProject.isBlank()) resolvedProject = projectRef;
        String versionId = str(version, "id");
        String resolvedTitle = title == null || title.isBlank() ? resolvedProject : title;
        return new Downloaded(fileName, resolvedProject, versionId, resolvedTitle);
    }

    private void rememberDownloaded(Downloaded downloaded) {
        Runnable task = () -> {
            modIndex.put(downloaded.fileName(), new String[]{
                    downloaded.projectId(), downloaded.versionId(), downloaded.title()});
            saveIndex();
            lastSignature = "";
            refreshLocal(true);
        };
        if (Platform.isFxApplicationThread()) task.run();
        else Platform.runLater(task);
    }

    /** 用 modId 当 slug 查 Modrinth；查不到就退回搜索接口（取第一条）。 */
    private Resolved resolveProject(String modId) throws Exception {
        JsonObject project = getJsonObject("https://api.modrinth.com/v2/project/"
                + URLEncoder.encode(modId, StandardCharsets.UTF_8));
        if (project != null) {
            String title = str(project, "title");
            return new Resolved(str(project, "id"), title.isBlank() ? modId : title);
        }

        JsonArray facets = new JsonArray();
        JsonArray type = new JsonArray();
        type.add("project_type:mod");
        facets.add(type);
        if (filterLoader != null) {
            JsonArray loaders = new JsonArray();
            loaders.add("categories:" + filterLoader);
            facets.add(loaders);
        }
        if (filterMcVersion != null) {
            JsonArray versions = new JsonArray();
            versions.add("versions:" + filterMcVersion);
            facets.add(versions);
        }
        String url = "https://api.modrinth.com/v2/search?query="
                + URLEncoder.encode(modId, StandardCharsets.UTF_8)
                + "&facets=" + URLEncoder.encode(facets.toString(), StandardCharsets.UTF_8)
                + "&limit=1";
        JsonObject search = getJsonObject(url);
        if (search == null || !search.has("hits")) return null;
        JsonArray hits = search.getAsJsonArray("hits");
        if (hits == null || hits.isEmpty()) return null;
        JsonObject hit = hits.get(0).getAsJsonObject();
        return new Resolved(str(hit, "project_id"), str(hit, "title"));
    }

    private JsonObject getJsonObject(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(java.time.Duration.ofSeconds(15))
                .header("User-Agent", "MCLauncher/1.0")
                .GET().build();
        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) return null;
        JsonElement element = JsonParser.parseString(response.body());
        return element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    private JsonObject fetchVersion(String projectRef) throws Exception {
        String url = "https://api.modrinth.com/v2/project/"
                + URLEncoder.encode(projectRef, StandardCharsets.UTF_8) + "/version" + filterQuery();
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(java.time.Duration.ofSeconds(20))
                .header("User-Agent", "MCLauncher/1.0")
                .GET().build();
        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) return null;
        JsonArray versions = JsonParser.parseString(response.body()).getAsJsonArray();
        return versions.isEmpty() ? null : versions.get(0).getAsJsonObject();
    }

    private static String str(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) return "";
        JsonElement element = object.get(key);
        return element.isJsonPrimitive() ? element.getAsString() : "";
    }

    private void logLater(String message) {
        Platform.runLater(() -> log(message));
    }

    /** 下载完成的一个模组文件（后台线程 -> UI 线程传递用）。 */
    private record Downloaded(String fileName, String projectId, String versionId, String title) {
    }

    /** Modrinth 项目（id + 显示名）。 */
    private record Resolved(String id, String title) {
    }

    /** Modrinth 版本查询参数：只在指定版本/加载器里挑文件。 */
    private String filterQuery() {
        StringBuilder sb = new StringBuilder();
        if (filterLoader != null) {
            sb.append("?loaders=").append(java.net.URLEncoder.encode("[\"" + filterLoader + "\"]", StandardCharsets.UTF_8));
        }
        if (filterMcVersion != null) {
            sb.append(sb.length() == 0 ? "?" : "&")
                    .append("game_versions=")
                    .append(java.net.URLEncoder.encode("[\"" + filterMcVersion + "\"]", StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    private File iconCacheFile(String url) {
        return new File(new File(AppPaths.dataDir(), "icon-cache"),
                Integer.toHexString(url == null ? 0 : url.hashCode()) + ".img");
    }

    private void loadIcon(StackPane pane, String iconUrl, String title) {
        if (iconUrl == null || iconUrl.isBlank()) return;
        Image cached = iconCache.get(iconUrl);
        if (cached != null) {
            showIcon(pane, cached);
            return;
        }
        File iconCacheFile = iconCacheFile(iconUrl);
        if (iconCacheFile.isFile()) {
            try {
                Image disk = new Image(iconCacheFile.toURI().toString());
                if (!disk.isError()) {
                    iconCache.put(iconUrl, disk);
                    showIcon(pane, disk);
                    return;
                }
            } catch (Exception ignored) {
            }
        }
        CompletableFuture.runAsync(() -> {
            try {
                byte[] bytes = IconProxy.fetch(CLIENT, iconUrl);
                if (bytes == null) return;
                Image image = decode(bytes);
                if (image == null) return;
                iconCache.put(iconUrl, image);
                try {
                    File cacheFile = iconCacheFile(iconUrl);
                    cacheFile.getParentFile().mkdirs();
                    java.nio.file.Files.write(cacheFile.toPath(), bytes);
                } catch (Exception ignored) {
                }
                Platform.runLater(() -> showIcon(pane, image));
            } catch (Exception ignored) {
            }
        });
    }

    private void showIcon(StackPane pane, Image image) {
        ImageView view = new ImageView(image);
        view.setFitWidth(46);
        view.setFitHeight(46);
        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(46, 46);
        clip.setArcWidth(18);
        clip.setArcHeight(18);
        view.setClip(clip);
        pane.getChildren().setAll(view);
    }

    /** JavaFX 不支持 webp，回退到 ImageIO（webp 插件在依赖里）。 */
    private Image decode(byte[] bytes) {
        try {
            Image image = new Image(new ByteArrayInputStream(bytes));
            if (!image.isError() && image.getWidth() > 0) return image;
        } catch (Exception ignored) {
        }
        try {
            java.awt.image.BufferedImage buffered = javax.imageio.ImageIO.read(new ByteArrayInputStream(bytes));
            if (buffered != null && buffered.getWidth() > 0) {
                javafx.scene.image.WritableImage out =
                        new javafx.scene.image.WritableImage(buffered.getWidth(), buffered.getHeight());
                javafx.scene.image.PixelWriter writer = out.getPixelWriter();
                int[] row = new int[buffered.getWidth()];
                for (int y = 0; y < buffered.getHeight(); y++) {
                    buffered.getRGB(0, y, buffered.getWidth(), 1, row, 0, buffered.getWidth());
                    for (int x = 0; x < buffered.getWidth(); x++) {
                        writer.setArgb(x, y, row[x]);
                    }
                }
                return out;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    // ================== 小工具 ==================

    private void openModsDir() {
        try {
            java.awt.Desktop.getDesktop().open(modsDir);
        } catch (Exception ignored) {
            try {
                new ProcessBuilder("explorer.exe", modsDir.getAbsolutePath()).start();
            } catch (Exception ignored2) {
            }
        }
    }

    // ================== 批量操作 ==================

    private void setAllEnabled(boolean enabled) {
        File[] files = modsDir.listFiles((dir, name) -> {
            String lower = name.toLowerCase(Locale.ROOT);
            return lower.endsWith(".jar") || lower.endsWith(".jar.disabled");
        });
        int changed = 0;
        if (files != null) {
            for (File file : files) {
                String name = file.getName();
                boolean disabled = name.toLowerCase(Locale.ROOT).endsWith(".disabled");
                if (disabled == enabled) continue;
                File target = enabled
                        ? new File(modsDir, name.substring(0, name.length() - ".disabled".length()))
                        : new File(modsDir, name + ".disabled");
                if (file.renameTo(target)) changed++;
            }
        }
        log("🧩 已" + (enabled ? "启用" : "禁用") + " " + changed + " 个模组");
        Toast.ok("已" + (enabled ? "启用" : "禁用") + " " + changed + " 个模组");
        lastSignature = "";
        metaSignature = "";
        refreshLocal(true);
    }

    private void deleteDisabledMods() {
        File[] files = modsDir.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".jar.disabled"));
        int total = files == null ? 0 : files.length;
        int removed = 0;
        if (files != null) {
            for (File file : files) {
                if (file.delete()) removed++;
            }
        }
        BulkDelete result = new BulkDelete(total, removed);
        log(result.logMessage("禁用模组"));
        if (result.shouldWarn()) {
            Toast.error(result.toastMessage("禁用模组"));
        } else {
            Toast.ok(result.toastMessage("禁用模组"));
        }
        lastSignature = "";
        metaSignature = "";
        refreshLocal(true);
    }

    private void setSelectedEnabled(boolean enabled) {
        if (selectedMods.isEmpty()) {
            Toast.info("先勾选要操作的模组");
            return;
        }
        int attempted = 0;
        int changed = 0;
        for (String name : new ArrayList<>(selectedMods)) {
            File file = new File(modsDir, name);
            if (!file.isFile()) continue;
            boolean disabled = name.toLowerCase(Locale.ROOT).endsWith(".disabled");
            if (disabled == enabled) continue;
            attempted++;
            File target = enabled
                    ? new File(modsDir, name.substring(0, name.length() - ".disabled".length()))
                    : new File(modsDir, name + ".disabled");
            if (file.renameTo(target)) changed++;
        }
        String verb = enabled ? "启用" : "禁用";
        BulkDelete result = new BulkDelete(attempted, changed);
        log("🧩 " + result.logMessage("已" + verb, "选中模组").replace("🗑️ ", ""));
        if (result.shouldWarn()) {
            Toast.error(result.toastMessage(verb, "模组"));
        } else {
            Toast.ok(result.toastMessage(verb, "模组"));
        }
        selectedMods.clear();
        lastSignature = "";
        metaSignature = "";
        refreshLocal(true);
    }

    private void deleteSelectedMods() {
        if (selectedMods.isEmpty()) {
            Toast.info("先勾选要删除的模组");
            return;
        }
        InAppDialog.confirm(null,
                "删除选中模组", "确定删除选中的 " + selectedMods.size() + " 个模组吗？文件会直接从 mods 目录删除，无法恢复。",
                "删除",
                () -> {
            int total = selectedMods.size();
            int removed = 0;
            for (String name : new ArrayList<>(selectedMods)) {
                if (new File(modsDir, name).delete()) removed++;
            }
            BulkDelete result = new BulkDelete(total, removed);
            log(result.logMessage("选中模组"));
            if (result.shouldWarn()) {
                Toast.error(result.toastMessage("模组"));
            } else {
                Toast.ok(result.toastMessage("模组"));
            }
            selectedMods.clear();
            lastSignature = "";
            metaSignature = "";
            refreshLocal(true);
                });
    }

    // ================== 模组更新 ==================

    private File indexFile() {
        File parent = modsDir.getParentFile();
        return new File(parent == null ? modsDir : parent, "mods-index.json");
    }

    private void loadIndex() {
        modIndex.clear();
        File file = indexFile();
        if (!file.isFile()) return;
        try (java.io.Reader reader = new java.io.FileReader(file)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            for (String key : root.keySet()) {
                JsonObject meta = root.getAsJsonObject(key);
                modIndex.put(key, new String[]{
                        meta.get("projectId").getAsString(),
                        meta.get("versionId").getAsString(),
                        meta.has("title") ? meta.get("title").getAsString() : key});
            }
        } catch (Exception ignored) {
        }
    }

    /** 把模组版本记录写盘；写失败返回 false（调用方要提示，否则下次更新会重复装）。 */
    private boolean saveIndex() {
        JsonObject root = new JsonObject();
        for (Map.Entry<String, String[]> entry : modIndex.entrySet()) {
            JsonObject meta = new JsonObject();
            meta.addProperty("projectId", entry.getValue()[0]);
            meta.addProperty("versionId", entry.getValue()[1]);
            meta.addProperty("title", entry.getValue()[2]);
            root.add(entry.getKey(), meta);
        }
        try (java.io.Writer writer = new java.io.FileWriter(indexFile())) {
            new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(root, writer);
            return true;
        } catch (Exception e) {
            log("⚠ 模组版本记录没能保存（下次更新可能重复）: " + e.getMessage());
            return false;
        }
    }

    private void checkUpdates() {
        if (modIndex.isEmpty()) {
            updateHint.setText("这些模组不是用本启动器安装的，没有版本记录，无法检查更新");
            return;
        }
        updateHint.setText("正在检查更新...");
        CompletableFuture.runAsync(() -> {
            int count = 0;
            for (Map.Entry<String, String[]> entry : new LinkedHashMap<>(modIndex).entrySet()) {
                String fileName = entry.getKey();
                String projectId = entry.getValue()[0];
                String installed = entry.getValue()[1];
                try {
                    JsonObject latest = fetchLatestVersion(projectId);
                    if (latest == null) continue;
                    if (!latest.get("id").getAsString().equals(installed)) {
                        updatable.add(fileName);
                        count++;
                    } else {
                        updatable.remove(fileName);
                    }
                } catch (Exception ignored) {
                }
            }
            int n = count;
            Platform.runLater(() -> {
                updateHint.setText(n == 0 ? "所有模组都是最新的" : n + " 个模组可更新");
                lastSignature = "";
                refreshLocal(true);
            });
        });
    }

    private JsonObject fetchLatestVersion(String projectId) {
        try {
            HttpRequest request = HttpRequest.newBuilder(
                            URI.create("https://api.modrinth.com/v2/project/" + projectId + "/version" + filterQuery()))
                    .timeout(java.time.Duration.ofSeconds(20))
                    .header("User-Agent", "MCLauncher/1.0")
                    .GET().build();
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            JsonArray versions = JsonParser.parseString(response.body()).getAsJsonArray();
            return versions.size() == 0 ? null : versions.get(0).getAsJsonObject();
        } catch (Exception e) {
            return null;
        }
    }

    private void updateMod(File oldFile, Button button) {
        String[] meta = modIndex.get(oldFile.getName());
        if (meta == null) return;
        button.setDisable(true);
        button.setText("更新中");
        CompletableFuture.runAsync(() -> {
            try {
                JsonObject latest = fetchLatestVersion(meta[0]);
                if (latest == null) throw new RuntimeException("没有可用版本");
                JsonArray files = latest.getAsJsonArray("files");
                JsonObject target = null;
                for (JsonElement element : files) {
                    JsonObject file = element.getAsJsonObject();
                    if (file.has("primary") && file.get("primary").getAsBoolean()) {
                        target = file;
                        break;
                    }
                }
                if (target == null) target = files.get(0).getAsJsonObject();

                String url = target.get("url").getAsString();
                String fileName = target.get("filename").getAsString();
                File out = new File(modsDir, fileName);
                DownloadManager.Stats stats = DownloadManager.downloadAll(
                        List.of(new DownloadManager.Task(url, out, null, -1)), 1, null);
                if (stats.failed > 0) throw new RuntimeException("下载失败");
                if (!oldFile.getName().equals(fileName)) oldFile.delete();

                String versionId = latest.get("id").getAsString();
                Platform.runLater(() -> {
                    modIndex.remove(oldFile.getName());
                    modIndex.put(fileName, new String[]{meta[0], versionId, meta[2]});
                    saveIndex();
                    updatable.remove(oldFile.getName());
                    log("✅ 已更新 " + meta[2] + " → " + fileName);
                    lastSignature = "";
                    refreshLocal(true);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    button.setDisable(false);
                    button.setText("更新");
                    log("❌ 更新失败: " + e.getMessage());
                });
            }
        });
    }

    private void log(String message) {
        if (log != null) log.accept(message);
    }

    private static Label hint(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: -app-text-muted-2; -fx-font-size: 12px;");
        label.setWrapText(true);
        return label;
    }

    private static Button miniButton(String text) {
        Button button = new Button(text);
        button.setStyle("-fx-background-color: #333; -fx-text-fill: #ccc; -fx-cursor: hand;"
                + " -fx-background-radius: 6; -fx-padding: 4 10; -fx-font-size: 12px;");
        button.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        button.setCursor(Cursor.HAND);
        return button;
    }

    private static String formatCount(long count) {
        if (count >= 100_000_000) return (count / 100_000_000) + " 亿";
        if (count >= 10_000) return (count / 10_000) + " 万";
        return String.valueOf(count);
    }
}
