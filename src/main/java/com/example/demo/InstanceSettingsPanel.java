package com.example.demo;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.io.File;
import java.util.function.Consumer;

/**
 * 实例设置页（对标 PCL）：左侧子导航（概览/设置/模组/资源包/存档），右侧内容。
 */
public final class InstanceSettingsPanel extends HBox {

    private final File gameDir;
    private final LauncherConfig config;
    private final Consumer<String> log;
    private final Runnable onBack;

    private final StackPane content = new StackPane();
    private final VBox navBox = new VBox(2);
    private final Label breadcrumb = new Label();
    private String tab = "overview";
    private ModuleHeader instanceHeader;
    private StackPane screenshotOverlay;
    private boolean navPointerInside;
    private double navMouseX;
    private double navMouseY;
    private String instanceId;

    private VBox overviewTab;
    private final SettingsPanel settingsTab;
    private final ModsPanel modsTab;
    private VBox resourcepackTab;
    private VBox savesTab;

    private final Consumer<String> onRepair;
    private final Consumer<String> onExportScript;
    private final Consumer<String> onDelete;
    private final Consumer<String> onTest;
    private final Consumer<String> onReinstall;

    public InstanceSettingsPanel(File gameDir, LauncherConfig config, String instanceId,
                                 Consumer<String> log, Runnable onBack,
                                 Consumer<String> onRepair, Consumer<String> onExportScript,
                                 Consumer<String> onDelete, Consumer<String> onTest,
                                 Consumer<String> onReinstall) {
        this.onRepair = onRepair;
        this.onExportScript = onExportScript;
        this.onDelete = onDelete;
        this.onTest = onTest;
        this.onReinstall = onReinstall;
        this.gameDir = gameDir;
        this.config = config;
        this.instanceId = instanceId;
        this.log = log;
        this.onBack = onBack;

        setSpacing(0);
        addEventFilter(javafx.scene.input.MouseEvent.MOUSE_MOVED, e -> {
            navPointerInside = true;
            navMouseX = e.getSceneX();
            navMouseY = e.getSceneY();
            refreshNavStyles();
        });
        addEventFilter(javafx.scene.input.MouseEvent.MOUSE_EXITED, e -> {
            navPointerInside = false;
            refreshNavStyles();
        });

        // ---------- 左侧 ----------
        VBox side = new VBox(0);
        side.setPrefWidth(196);
        side.setMinWidth(196);
        side.setStyle("-fx-background-color: -app-nav-2; -fx-border-color: transparent -app-row3 transparent transparent;");
        side.setPadding(new Insets(14, 0, 0, 0));

        HBox back = new HBox(6);
        back.setAlignment(Pos.CENTER_LEFT);
        back.setPadding(new Insets(6, 16, 12, 18));
        back.setCursor(Cursor.HAND);
        Label backArrow = new Label("‹");
        backArrow.setStyle("-fx-text-fill: -app-text-dim; -fx-font-size: 18px;");
        Label backText = new Label("返回启动页");
        backText.setStyle("-fx-text-fill: -app-text-dim; -fx-font-size: 12px;");
        back.getChildren().addAll(backArrow, backText);
        back.setOnMouseEntered(e -> backText.setStyle("-fx-text-fill: -app-text-2; -fx-font-size: 12px;"));
        back.setOnMouseExited(e -> backText.setStyle("-fx-text-fill: -app-text-dim; -fx-font-size: 12px;"));
        back.setOnMouseClicked(e -> {
            if (onBack != null) onBack.run();
        });

        breadcrumb.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11px;");
        breadcrumb.setPadding(new Insets(0, 18, 10, 20));
        updateBreadcrumb();

        side.getChildren().addAll(back, breadcrumb);
        side.getChildren().add(sectionLabel("游戏本体"));
        side.getChildren().add(navItem("overview", "概览"));
        side.getChildren().add(navItem("settings", "设置"));
        side.getChildren().add(navItem("modify", "修改"));
        side.getChildren().add(navItem("export", "导出"));
        side.getChildren().add(sectionLabel("游戏资源"));
        side.getChildren().add(navItem("saves", "存档"));
        side.getChildren().add(navItem("screenshots", "截图"));
        side.getChildren().add(navItem("mods", "模组"));
        side.getChildren().add(navItem("resourcepacks", "资源包"));
        side.getChildren().add(navItem("shaderpacks", "光影包"));
        side.getChildren().add(navItem("schematics", "投影原型图"));
        side.getChildren().add(navItem("servers", "服务器"));

        // ---------- 右侧预构建 ----------
        settingsTab = new SettingsPanel(config, log);
        modsTab = new ModsPanel(InstanceDirs.mods(gameDir, instanceId == null ? "" : instanceId, config.isIsolation()), config, log);

        applyModFilter();
        content.setStyle("-fx-background-color: transparent;");
        HBox.setHgrow(content, Priority.ALWAYS);

        instanceHeader = new ModuleHeader("06 / INSTANCE", "实例设置",
                "版本 · 加载器 · 资源", "layers", ModuleHeader.Badge.STACK,
                instanceId == null ? "" : instanceId);
        HBox headerWrap = new HBox(instanceHeader);
        headerWrap.setPadding(new Insets(14, 22, 0, 22));
        VBox right = new VBox(headerWrap, content);
        VBox.setVgrow(content, Priority.ALWAYS);
        HBox.setHgrow(right, Priority.ALWAYS);
        getChildren().addAll(side, right);
        select("overview");
    }

    public void setInstance(String instanceId) {
        this.instanceId = instanceId;
        updateBreadcrumb();
        if (instanceHeader != null) instanceHeader.setMeta(instanceId);
        applyModFilter();
        select(tab);
    }

    /** 把当前实例的版本 + 加载器告诉模组页，用于自动筛选。 */
    private void applyModFilter() {
        if (modsTab == null) return;
        Instance instance = Instance.of(gameDir, instanceId == null ? "" : instanceId);
        modsTab.setModsDir(InstanceDirs.mods(gameDir, instance.id, config.isIsolation()));
        String loader = switch (instance.loader) {
            case "Fabric" -> "fabric";
            case "Forge" -> "forge";
            case "NeoForge" -> "neoforge";
            case "Quilt" -> "quilt";
            default -> null;
        };
        modsTab.setFilter(instance.mcVersion, loader);
    }

    public void showTab(String tabName) {
        select(tabName);
    }

    /** 从下载页打开某个模组的详情页（复用实例模组页的界面）。 */
    public void openModDetail(String projectId, String title) {
        select("mods");
        if (modsTab != null) modsTab.openProjectDetail(projectId, title);
    }

    private void updateBreadcrumb() {
        breadcrumb.setText("实例设置 · " + (instanceId == null ? "-" : instanceId));
    }

    private void select(String tabName) {
        this.tab = tabName;
        navPointerInside = false;
        for (Node node : navBox.getChildren()) {
            if (node.getUserData() instanceof String key) {
                boolean active = key.equals(tabName);
                node.setStyle("-fx-background-color: "
                        + (active ? (Theme.isLight() ? "#ffffff" : "#232730") : "transparent") + ";");
                if (node instanceof HBox box) {
                    box.getChildren().get(0).setStyle("-fx-fill: " + (active ? "#9ca3af" : "transparent") + ";");
                    for (javafx.scene.Node child : box.getChildren()) {
                        if (child instanceof Label label) {
                            label.setStyle("-fx-text-fill: " + (active ? "-app-text" : "-app-text-dim")
                                    + "; -fx-font-size: 12px;");
                            break;
                        }
                    }
                }
            }
        }
        boolean isolated = config.isIsolation();
        String id = instanceId == null ? "" : instanceId;
        File instanceRoot = InstanceDirs.root(gameDir, id, isolated);
        Node page = switch (tabName) {
            case "settings" -> buildInstanceSettingsTab(instanceRoot);
            case "mods" -> modsTab;
            case "modify" -> buildModifyTab(instanceRoot);
            case "export" -> buildExportTab(instanceRoot);
            case "resourcepacks" -> buildFolderTab(InstanceDirs.resourcepacks(gameDir, id, isolated), "资源包");
            case "shaderpacks" -> buildFolderTab(InstanceDirs.shaderpacks(gameDir, id, isolated), "光影包");
            case "saves" -> buildFolderTab(InstanceDirs.saves(gameDir, id, isolated), "存档");
            case "screenshots" -> buildScreenshotGallery(new File(instanceRoot, "screenshots"));
            case "schematics" -> buildFolderTab(new File(instanceRoot, "schematics"), "投影原型图");
            case "servers" -> buildServersTab(instanceRoot);
            default -> buildOverview();
        };
        content.getChildren().setAll(page);
        Motion.pageIn(page, 8);
        refreshNavStyles();
    }

    private VBox buildOverview() {
        VBox page = new VBox(14);
        page.setPadding(new Insets(22, 24, 22, 24));

        Instance instance = Instance.of(gameDir, instanceId == null ? "" : instanceId);

        HBox head = new HBox(12);
        head.setAlignment(Pos.CENTER_LEFT);
        StackPane icon = new StackPane();
        icon.setPrefSize(44, 44);
        icon.setMinSize(44, 44);
        icon.setStyle("-fx-background-color: -app-text-dim-2; -fx-background-radius: 9;");
        Label letter = new Label(instance.loader.substring(0, 1));
        letter.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");
        icon.getChildren().add(letter);

        VBox titles = new VBox(3);
        Label name = new Label(InstanceMeta.name(gameDir, instance.id));
        name.setStyle("-fx-text-fill: -app-text; -fx-font-size: 19px; -fx-font-weight: bold;");
        String desc = InstanceMeta.description(gameDir, instance.id);
        Label sub = new Label(instance.id + " · " + instance.subtitle()
                + (desc == null || desc.isBlank() ? "" : " · " + desc));
        sub.setStyle("-fx-text-fill: -app-text-dim-3; -fx-font-size: 12px;");
        titles.getChildren().addAll(name, sub);
        head.getChildren().addAll(icon, titles);

        VBox info = card("实例信息", new String[][]{
                {"游戏版本", instance.mcVersion},
                {"加载器", instance.loader + ("原版".equals(instance.loader) ? "" : " " + instance.loaderVersion)},
                {"实例目录", InstanceDirs.root(gameDir, instance.id, config.isIsolation()).getAbsolutePath()},
                {"客户端 jar", new File(new File(new File(gameDir, "versions"), instance.mcVersion),
                        instance.mcVersion + ".jar").getAbsolutePath()}
        });

        int modCount = countFiles(InstanceDirs.mods(gameDir, instance.id, config.isIsolation()), ".jar");
        int packCount = countFiles(InstanceDirs.resourcepacks(gameDir, instance.id, config.isIsolation()), ".zip");
        VBox counts = card("内容统计", new String[][]{
                {"模组", modCount + " 个"},
                {"资源包", packCount + " 个"},
                {"存档", countDirs(InstanceDirs.saves(gameDir, instance.id, config.isIsolation())) + " 个"}
        });

        java.io.File[] recentCrashFiles = new File(gameDir, "crash-reports")
                .listFiles((d, n) -> n.endsWith(".txt"));
        long recentCrashes = 0;
        if (recentCrashFiles != null) {
            long weekAgo = System.currentTimeMillis() - 7L * 24 * 3600 * 1000;
            for (File crash : recentCrashFiles) {
                if (crash.lastModified() >= weekAgo) recentCrashes++;
            }
        }
        int healthScore = 100;
        java.util.List<String> healthTips = new java.util.ArrayList<>();
        int memoryMb = config.getMemoryMb();
        if (memoryMb < 2048) {
            healthScore -= 15;
            healthTips.add("最大内存低于 2 GB，大型整合包容易卡顿");
        }
        if (modCount >= 80 && memoryMb < 4096) {
            healthScore -= 10;
            healthTips.add("模组较多但内存低于 4 GB，建议提高分配");
        }
        if (recentCrashes > 0) {
            int penalty = (int) Math.min(30, recentCrashes * 10);
            healthScore -= penalty;
            healthTips.add("近 7 天有 " + recentCrashes + " 次崩溃，可先跑崩溃诊断");
        }
        if (packCount > 120) {
            healthScore -= 5;
            healthTips.add("资源包超过 120 个，加载会变慢");
        }
        healthScore = Math.max(20, healthScore);
        String healthColor = healthScore >= 85 ? "#4CAF50" : healthScore >= 70 ? "#d1a054" : "#e06c75";
        Label scoreLabel = new Label(String.valueOf(healthScore));
        scoreLabel.setStyle("-fx-text-fill: " + healthColor + "; -fx-font-size: 32px; -fx-font-weight: bold;");
        Label scoreUnit = new Label(" / 100");
        scoreUnit.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11px;");
        HBox scoreLine = new HBox(2, scoreLabel, scoreUnit);
        scoreLine.setAlignment(Pos.BASELINE_LEFT);
        Label healthState = new Label(healthScore >= 85 ? "运行状态良好" : healthScore >= 70 ? "有可优化项" : "存在风险");
        healthState.setStyle("-fx-text-fill: " + healthColor + "; -fx-font-size: 12px; -fx-font-weight: bold;");
        javafx.scene.control.ProgressBar healthBar = new javafx.scene.control.ProgressBar(healthScore / 100.0);
        healthBar.setPrefWidth(240);
        healthBar.setStyle("-fx-accent: " + healthColor + ";");
        VBox healthText = new VBox(4, healthState, healthBar);
        healthText.setAlignment(Pos.CENTER_LEFT);
        Region healthSpacer = new Region();
        HBox.setHgrow(healthSpacer, Priority.ALWAYS);
        HBox healthTop = new HBox(18, scoreLine, healthText, healthSpacer);
        healthTop.setAlignment(Pos.CENTER_LEFT);
        VBox healthCard = card("实例健康", new String[][]{});
        healthCard.getChildren().add(healthTop);
        if (healthTips.isEmpty()) {
            Label good = new Label("配置合理，当前没有明显风险项。");
            good.setStyle("-fx-text-fill: -app-text-dim-3; -fx-font-size: 11.5px;");
            healthCard.getChildren().add(good);
        } else {
            for (String tip : healthTips) {
                Label line = new Label("• " + tip);
                line.setStyle("-fx-text-fill: -app-text-dim-2; -fx-font-size: 11.5px;");
                line.setWrapText(true);
                healthCard.getChildren().add(line);
            }
        }

        HBox shortcuts = new HBox(10);
        Button openInstance = ghostButton("打开实例文件夹");
        openInstance.setOnAction(e -> openDir(InstanceDirs.root(gameDir, instance.id, config.isIsolation())));
        Button openMods = ghostButton("打开模组文件夹");
        openMods.setOnAction(e -> openDir(InstanceDirs.mods(gameDir, instance.id, config.isIsolation())));
        Button goMods = ghostButton("管理模组");
        goMods.setOnAction(e -> select("mods"));
        shortcuts.getChildren().addAll(openInstance, openMods, goMods);

        HBox personal = new HBox(10);
        Button rename = ghostButton("修改实例名");
        rename.setOnAction(e -> {
            java.util.Optional<String> value = Dialogs.ask(getScene() == null ? null : getScene().getWindow(),
                    "修改实例名", "给这个实例起个名字", InstanceMeta.name(gameDir, instance.id));
            value.ifPresent(text -> {
                InstanceMeta.set(gameDir, instance.id, text.isBlank() ? instance.id : text.trim(),
                        InstanceMeta.description(gameDir, instance.id));
                log("✏️ 实例已重命名: " + text);
                select("overview");
            });
        });
        Button setDesc = ghostButton("修改实例描述");
        setDesc.setOnAction(e -> {
            java.util.Optional<String> value = Dialogs.ask(getScene() == null ? null : getScene().getWindow(),
                    "修改实例描述", "一句话说明这个实例（可留空）", InstanceMeta.description(gameDir, instance.id));
            value.ifPresent(text -> {
                InstanceMeta.set(gameDir, instance.id, InstanceMeta.name(gameDir, instance.id), text.trim());
                log("✏️ 实例描述已更新");
                select("overview");
            });
        });
        personal.getChildren().addAll(rename, setDesc);

        HBox advanced = new HBox(10);
        Button repair = ghostButton("补全文件");
        repair.setOnAction(e -> {
            if (onRepair != null) onRepair.accept(instance.id);
        });
        Button export = ghostButton("导出启动脚本");
        export.setOnAction(e -> {
            if (onExportScript != null) onExportScript.accept(instance.id);
        });
        Button remove = ghostButton("删除实例");
        remove.setStyle("-fx-background-color: #7f1d1d; -fx-text-fill: #fecaca; -fx-background-radius: 8;"
                + " -fx-padding: 7 14; -fx-cursor: hand; -fx-font-size: 12px;");
        remove.setOnAction(e -> {
            boolean ok = Dialogs.confirm(getScene() == null ? null : getScene().getWindow(),
                    "删除实例", "确定删除 " + instance.id + " 吗？",
                    "会删除 versions/" + instance.id + " 整个目录（客户 jar、模组、存档等），不可恢复。");
            if (ok && onDelete != null) onDelete.accept(instance.id);
        });
        Button repairCore = ghostButton("修复核心");
        repairCore.setOnAction(e -> {
            File natives = InstanceDirs.natives(gameDir, instance.id, config.isIsolation());
            File[] dlls = natives.listFiles();
            int count = 0;
            if (dlls != null) {
                for (File f : dlls) {
                    if (f.isDirectory()) {
                        File[] inner = f.listFiles((d, n) -> n.toLowerCase().endsWith(".dll"));
                        if (inner != null) for (File file : inner) if (file.delete()) count++;
                    } else if (f.getName().toLowerCase().endsWith(".dll")) {
                        if (f.delete()) count++;
                    }
                }
            }
            log("🧹 已清理 " + count + " 个 dll，正在重新解压核心文件...");
            if (onRepair != null) onRepair.accept(instance.id);
        });
        Button test = ghostButton("测试游戏");
        test.setOnAction(e -> {
            if (onTest != null) onTest.accept(instance.id);
        });
        Button reinstall = ghostButton("重装");
        reinstall.setOnAction(e -> {
            boolean ok = Dialogs.confirm(getScene() == null ? null : getScene().getWindow(),
                    "重装实例", "确定重装 " + instance.id + " 吗？",
                    "会删除该实例的文件并重新下载客户端与依赖（mods/config/saves 也会被清空）。");
            if (ok && onReinstall != null) onReinstall.accept(instance.id);
        });
        advanced.getChildren().addAll(repair, repairCore, test, reinstall, export, remove);

        VBox personalCard = card("个性化", new String[][]{});
        personalCard.getChildren().add(personal);
        InstanceOverrides.Override override = InstanceOverrides.get(gameDir, instance.id);
        VBox launchCard = card("启动设置（本实例）", new String[][]{});
        javafx.scene.control.CheckBox useMemory = new javafx.scene.control.CheckBox("覆盖最大内存");
        javafx.scene.control.Spinner<Integer> memorySpinner = new javafx.scene.control.Spinner<>(
                512, 16384, override.memoryMb() == null ? 2048 : override.memoryMb(), 512);
        memorySpinner.setPrefWidth(120);
        useMemory.setSelected(override.memoryMb() != null);
        memorySpinner.setDisable(!useMemory.isSelected());
        useMemory.selectedProperty().addListener((o, a, b) -> memorySpinner.setDisable(!b));
        javafx.scene.control.Button smartMemory = ghostButton("智能分配");
        smartMemory.setOnAction(e -> {
            MemoryAdvisor.Suggestion suggestion = MemoryAdvisor.suggest(
                    InstanceDirs.mods(gameDir, instance.id, config.isIsolation()));
            useMemory.setSelected(true);
            memorySpinner.getValueFactory().setValue(suggestion.memoryMb());
            Toast.ok("建议 " + suggestion.memoryMb() + " MB：" + suggestion.reason());
        });
        javafx.scene.control.TextField jvmField = new javafx.scene.control.TextField(
                override.jvmArgs() == null ? "" : override.jvmArgs());
        jvmField.setPromptText("留空 = 使用全局 JVM 参数");
        HBox.setHgrow(jvmField, Priority.ALWAYS);
        javafx.scene.control.TextField javaField = new javafx.scene.control.TextField(
                override.javaPath() == null ? "" : override.javaPath());
        javaField.setPromptText("留空 = 使用全局 Java；可填 java.exe 完整路径");
        HBox.setHgrow(javaField, Priority.ALWAYS);
        javafx.scene.control.Button saveOverride = ghostButton("保存本实例设置");
        saveOverride.setOnAction(e -> {
            Integer memory = useMemory.isSelected() ? memorySpinner.getValue() : null;
            InstanceOverrides.save(gameDir, instance.id, memory, jvmField.getText(), javaField.getText());
            Toast.ok("已保存本实例启动设置");
            log("🎛 实例 " + instance.id + " 启动设置已保存");
        });
        javafx.scene.control.Button clearOverride = ghostButton("恢复全局");
        clearOverride.setOnAction(e -> {
            InstanceOverrides.save(gameDir, instance.id, null, "", "");
            useMemory.setSelected(false);
            memorySpinner.setDisable(true);
            jvmField.clear();
            javaField.clear();
            Toast.ok("已恢复为全局设置");
        });
        launchCard.getChildren().addAll(
                new HBox(10, useMemory, memorySpinner, smartMemory), jvmField, javaField,
                new HBox(10, saveOverride, clearOverride));
        VBox advancedCard = card("高级管理", new String[][]{});
        advancedCard.getChildren().add(advanced);

        page.getChildren().addAll(head, info, counts, healthCard, shortcuts, personalCard, launchCard, advancedCard);

        ScrollPane scroll = new ScrollPane(page);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox wrap = new VBox(scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return wrap;
    }

    /** 把本地文件复制进实例目录（资源包 / 光影包等）。 */
    private void installFiles(File dir) {
        javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
        chooser.setTitle("选择要安装的文件");
        chooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter(
                "资源文件", "*.zip", "*.jar", "*.mcpack", "*.litemod"));
        java.util.List<File> chosen = chooser.showOpenMultipleDialog(
                getScene() == null ? null : getScene().getWindow());
        if (chosen == null || chosen.isEmpty()) return;
        if (!dir.isDirectory() && !dir.mkdirs()) {
            Toast.error("无法创建目录: " + dir.getAbsolutePath());
            return;
        }
        int copied = 0;
        for (File file : chosen) {
            try {
                java.nio.file.Files.copy(file.toPath(),
                        new File(dir, file.getName()).toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                copied++;
            } catch (Exception e) {
                Toast.error("复制失败: " + file.getName());
            }
        }
        Toast.ok("已安装 " + copied + " 个文件");
        select(tab);
    }

    private void openUrl(String url) {
        try {
            java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
        } catch (Exception e) {
            Toast.error("打开链接失败: " + e.getMessage());
        }
    }

    /** 实例级启动设置：只覆盖当前实例，不再嵌一套全局设置页。 */
    private ScrollPane buildInstanceSettingsTab(File instanceRoot) {
        VBox page = new VBox(14);
        page.setPadding(new Insets(22, 24, 22, 24));

        Label head = new Label("启动设置");
        head.setStyle("-fx-text-fill: -app-text; -fx-font-size: 19px; -fx-font-weight: bold;");
        Label hint = new Label("这些覆盖只对当前实例生效；留空或取消勾选表示跟随全局设置。");
        hint.setStyle("-fx-text-fill: -app-text-dim-3; -fx-font-size: 12px;");
        hint.setWrapText(true);

        InstanceOverrides.Override override = InstanceOverrides.get(gameDir, instanceId);
        VBox card = card("内存与启动参数", new String[][]{});

        javafx.scene.control.CheckBox useMemory = new javafx.scene.control.CheckBox("覆盖最大内存");
        useMemory.setStyle("-fx-text-fill: -app-text-2;");
        useMemory.setSelected(override.memoryMb() != null);
        javafx.scene.control.Spinner<Integer> memorySpinner = new javafx.scene.control.Spinner<>(
                512, 32768, override.memoryMb() == null ? 4096 : override.memoryMb(), 512);
        memorySpinner.setPrefWidth(150);
        memorySpinner.setDisable(!useMemory.isSelected());
        useMemory.selectedProperty().addListener((o, a, b) -> memorySpinner.setDisable(!b));

        javafx.scene.control.TextField jvmField = new javafx.scene.control.TextField(override.jvmArgs() == null ? "" : override.jvmArgs());
        jvmField.setPromptText("留空 = 使用全局 JVM 参数");
        javafx.scene.control.TextField javaField = new javafx.scene.control.TextField(override.javaPath() == null ? "" : override.javaPath());
        javaField.setPromptText("留空 = 使用全局 Java");
        HBox.setHgrow(jvmField, Priority.ALWAYS);
        HBox.setHgrow(javaField, Priority.ALWAYS);

        Button browseJava = ghostButton("浏览");
        browseJava.setOnAction(e -> {
            javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
            chooser.setTitle("选择 java.exe");
            chooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Java", "java.exe"));
            File picked = chooser.showOpenDialog(getScene() == null ? null : getScene().getWindow());
            if (picked != null) javaField.setText(picked.getAbsolutePath());
        });

        Label status = new Label("");
        status.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 11.5px;");

        Button save = new Button("保存到本实例");
        save.setStyle("-fx-background-color: linear-gradient(to bottom right, #d1d5db, #9ca3af);"
                + " -fx-text-fill: #111827; -fx-font-weight: bold; -fx-background-radius: 9;"
                + " -fx-padding: 8 20; -fx-cursor: hand;");
        save.setOnAction(e -> {
            InstanceOverrides.save(gameDir, instanceId,
                    useMemory.isSelected() ? memorySpinner.getValue() : null,
                    jvmField.getText(), javaField.getText());
            status.setText("已保存本实例设置");
            log("🎛 实例 " + instanceId + " 启动设置已保存");
        });

        Button clear = ghostButton("恢复全局");
        clear.setOnAction(e -> {
            InstanceOverrides.save(gameDir, instanceId, null, "", "");
            useMemory.setSelected(false);
            memorySpinner.setDisable(true);
            jvmField.clear();
            javaField.clear();
            status.setText("已恢复为全局设置");
        });

        Label global = new Label("全局：" + config.getMemoryMb() + " MB · "
                + (config.getJavaPath() == null || config.getJavaPath().isBlank()
                ? "自动选择 Java" : config.getJavaPath()));
        global.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11px;");
        global.setWrapText(true);

        card.getChildren().addAll(
                new HBox(10, useMemory, memorySpinner),
                jvmField, new HBox(8, javaField, browseJava),
                new HBox(10, save, clear), status, global);

        VBox instanceCard = card("实例位置", new String[][]{
                {"实例目录", instanceRoot.getAbsolutePath()},
                {"客户端 jar", new File(new File(gameDir, "versions"), instanceId + "/" + instanceId + ".jar").getAbsolutePath()}
        });
        Button openFolder = ghostButton("打开实例文件夹");
        openFolder.setOnAction(e -> openDir(instanceRoot));
        instanceCard.getChildren().add(openFolder);

        page.getChildren().addAll(head, hint, card, instanceCard);
        ScrollPane scroll = new ScrollPane(page);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-border-color: transparent;");
        return scroll;
    }

    /** 截图页：缩略图网格，而不是普通文件列表。 */
    private ScrollPane buildScreenshotGallery(File dir) {
        VBox page = new VBox(14);
        page.setPadding(new Insets(22, 24, 22, 24));

        File[] files = dir.isDirectory()
                ? dir.listFiles(file -> file.isFile() && file.getName().toLowerCase(java.util.Locale.ROOT)
                        .matches(".*\\.(png|jpg|jpeg|webp)$"))
                : null;
        if (files == null) files = new File[0];
        java.util.Arrays.sort(files, java.util.Comparator.comparingLong(File::lastModified).reversed());

        Label head = new Label("截图");
        head.setStyle("-fx-text-fill: -app-text; -fx-font-size: 19px; -fx-font-weight: bold;");
        Label count = new Label(files.length + " 张");
        count.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11px;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button refresh = ghostButton("刷新");
        refresh.setOnAction(e -> select("screenshots"));
        Button open = ghostButton("打开文件夹");
        open.setOnAction(e -> openDir(dir));

        HBox top = new HBox(10, head, count, spacer, refresh, open);
        top.setAlignment(Pos.CENTER_LEFT);

        if (files.length == 0) {
            Label empty = new Label("这个文件夹还是空的\n安装或放入文件后会显示在这里。");
            empty.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 12px;");
            VBox emptyCard = new VBox(8, empty);
            emptyCard.setPadding(new Insets(24));
            emptyCard.setStyle("-fx-background-color: linear-gradient(to bottom, -app-card-top, -app-card-alt2);"
                    + " -fx-background-radius: 16; -fx-border-color: -app-border-soft; -fx-border-radius: 16;");
            page.getChildren().addAll(top, emptyCard);
            ScrollPane scroll = new ScrollPane(page);
            scroll.setFitToWidth(true);
            scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-border-color: transparent;");
            return scroll;
        }

        javafx.scene.layout.FlowPane grid = new javafx.scene.layout.FlowPane(12, 12);
        grid.setPrefWrapLength(760);
        final File[] galleryFiles = files;
        int limit = Math.min(60, galleryFiles.length);
        for (int i = 0; i < limit; i++) {
            final int shotIndex = i;
            File file = galleryFiles[i];
            javafx.scene.image.Image image = new javafx.scene.image.Image(
                    file.toURI().toString(), 360, 202, true, true, true);
            javafx.scene.image.ImageView view = new javafx.scene.image.ImageView(image);
            view.setFitWidth(190);
            view.setFitHeight(107);
            view.setPreserveRatio(false);
            Rectangle clip = new Rectangle(190, 107);
            clip.setArcWidth(16);
            clip.setArcHeight(16);
            view.setClip(clip);

            Label name = new Label(file.getName());
            name.setStyle("-fx-text-fill: -app-text-dim-2; -fx-font-size: 10.5px;");
            name.setMaxWidth(190);
            VBox tile = new VBox(6, view, name);
            tile.setPadding(new Insets(8));
            tile.setStyle("-fx-background-color: linear-gradient(to bottom, -app-card-top, -app-card-alt2);"
                    + " -fx-background-radius: 14; -fx-border-color: -app-border-soft; -fx-border-radius: 14;");
            tile.setCursor(javafx.scene.Cursor.HAND);
            tile.setOnMouseEntered(e -> tile.setStyle("-fx-background-color: -app-hover-2;"
                    + " -fx-background-radius: 14; -fx-border-color: -app-border-3; -fx-border-radius: 14;"));
            tile.setOnMouseExited(e -> tile.setStyle("-fx-background-color: linear-gradient(to bottom, -app-card-top, -app-card-alt2);"
                    + " -fx-background-radius: 14; -fx-border-color: -app-border-soft; -fx-border-radius: 14;"));
            tile.setOnMouseClicked(e -> showScreenshotPreview(galleryFiles, shotIndex));
            Motion.hoverLift(tile);
            grid.getChildren().add(tile);
        }
        if (files.length > limit) {
            Label more = new Label("仅显示最近 " + limit + " 张，共 " + files.length + " 张");
            more.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11px;");
            page.getChildren().addAll(top, grid, more);
        } else {
            page.getChildren().addAll(top, grid);
        }

        ScrollPane scroll = new ScrollPane(page);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-border-color: transparent;");
        return scroll;
    }

    /** 应用内截图画廊预览：左右切换、Esc 关闭、可调用系统查看器。 */
    private void showScreenshotPreview(File[] files, int startIndex) {
        if (screenshotOverlay != null || getScene() == null
                || !(getScene().getRoot() instanceof StackPane layer) || files.length == 0) return;

        StackPane overlay = new StackPane();
        overlay.setFocusTraversable(true);
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.86);");
        overlay.setAlignment(Pos.CENTER);

        javafx.scene.image.ImageView view = new javafx.scene.image.ImageView();
        view.setPreserveRatio(true);
        view.setFitWidth(780);
        view.setFitHeight(480);
        view.setSmooth(true);

        Label name = new Label();
        name.setStyle("-fx-text-fill: #e5e7eb; -fx-font-size: 12.5px; -fx-font-weight: bold;");
        Label counter = new Label();
        counter.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 11px;");
        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);
        Button systemOpen = new Button("系统打开");
        systemOpen.setStyle("-fx-background-color: rgba(255,255,255,0.10); -fx-text-fill: #e5e7eb;"
                + " -fx-background-radius: 8; -fx-padding: 5 12; -fx-font-size: 11px; -fx-cursor: hand;");
        HBox info = new HBox(10, name, counter, topSpacer, systemOpen);
        info.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(10, view, info);
        card.setMaxWidth(800);
        card.setMaxHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        card.setPadding(new Insets(10));
        card.setStyle("-fx-background-color: rgba(18,20,24,0.75); -fx-background-radius: 16;"
                + " -fx-border-color: rgba(255,255,255,0.08); -fx-border-radius: 16;");

        Button prev = new Button();
        prev.setGraphic(Icons.node("back", 20, "#e5e7eb"));
        prev.setStyle(iconCircleStyle());
        Button next = new Button();
        next.setGraphic(Icons.node("chevron-right", 20, "#e5e7eb"));
        next.setStyle(iconCircleStyle());
        HBox stage = new HBox(18, prev, card, next);
        stage.setAlignment(Pos.CENTER);
        stage.setMaxWidth(960);

        Button close = new Button();
        close.setGraphic(Icons.node("close", 16, "#e5e7eb"));
        close.setStyle(iconCircleStyle());
        StackPane.setAlignment(close, Pos.TOP_RIGHT);
        StackPane.setMargin(close, new Insets(18, 20, 0, 0));

        final int[] index = {Math.max(0, Math.min(startIndex, files.length - 1))};
        Runnable update = () -> {
            File file = files[index[0]];
            javafx.scene.image.Image image = new javafx.scene.image.Image(
                    file.toURI().toString(), 1560, 960, true, true, true);
            view.setImage(image);
            name.setText(file.getName());
            counter.setText((index[0] + 1) + " / " + files.length);
        };
        update.run();

        prev.setOnAction(e -> {
            index[0] = (index[0] - 1 + files.length) % files.length;
            update.run();
        });
        next.setOnAction(e -> {
            index[0] = (index[0] + 1) % files.length;
            update.run();
        });
        systemOpen.setOnAction(e -> {
            try {
                java.awt.Desktop.getDesktop().open(files[index[0]]);
            } catch (Exception ex) {
                Toast.error("打开截图失败：" + ex.getMessage());
            }
        });
        close.setOnAction(e -> closeScreenshotPreview());
        overlay.setOnMouseClicked(e -> {
            if (e.getTarget() == overlay) closeScreenshotPreview();
        });
        stage.setOnMouseClicked(e -> e.consume());
        overlay.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                closeScreenshotPreview();
            } else if (e.getCode() == javafx.scene.input.KeyCode.LEFT) {
                index[0] = (index[0] - 1 + files.length) % files.length;
                update.run();
            } else if (e.getCode() == javafx.scene.input.KeyCode.RIGHT) {
                index[0] = (index[0] + 1) % files.length;
                update.run();
            }
            e.consume();
        });

        overlay.getChildren().addAll(stage, close);
        layer.getChildren().add(overlay);
        screenshotOverlay = overlay;
        overlay.setOpacity(0);
        card.setScaleX(0.97);
        card.setScaleY(0.97);
        javafx.animation.FadeTransition fade = new javafx.animation.FadeTransition(
                javafx.util.Duration.millis(160), overlay);
        fade.setToValue(1);
        javafx.animation.ScaleTransition scale = new javafx.animation.ScaleTransition(
                javafx.util.Duration.millis(190), card);
        scale.setToX(1);
        scale.setToY(1);
        scale.setInterpolator(Motion.SPRING);
        fade.play();
        scale.play();
        javafx.application.Platform.runLater(overlay::requestFocus);
    }

    private String iconCircleStyle() {
        return "-fx-background-color: rgba(255,255,255,0.10); -fx-background-radius: 22;"
                + " -fx-min-width: 44; -fx-min-height: 44; -fx-max-width: 44; -fx-max-height: 44;"
                + " -fx-cursor: hand; -fx-padding: 0;";
    }

    private void closeScreenshotPreview() {
        StackPane overlay = screenshotOverlay;
        screenshotOverlay = null;
        if (overlay == null) return;
        javafx.animation.FadeTransition fade = new javafx.animation.FadeTransition(
                javafx.util.Duration.millis(120), overlay);
        fade.setToValue(0);
        fade.setOnFinished(e -> {
            if (overlay.getParent() instanceof StackPane parent) parent.getChildren().remove(overlay);
        });
        fade.play();
    }

    private VBox buildFolderTab(File dir, String title) {
        VBox page = new VBox(12);
        page.setPadding(new Insets(22, 24, 22, 24));

        Label head = new Label(title);
        head.setStyle("-fx-text-fill: -app-text; -fx-font-size: 19px; -fx-font-weight: bold;");
        Label path = new Label(dir.getAbsolutePath());
        path.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11px;");

        VBox list = new VBox(6);
        File[] files = dir.listFiles();
        if (files == null || files.length == 0) {
            boolean resource = "资源包".equals(title) || "光影包".equals(title);
            VBox empty = new VBox(10);
            empty.setPadding(new Insets(18, 20, 18, 20));
            empty.setStyle("-fx-background-color: -app-card; -fx-background-radius: 10;"
                    + " -fx-border-color: -app-border; -fx-border-radius: 10;");
            Label emptyTitle = new Label(resource ? "尚未安装" + title : "这个文件夹还是空的");
            emptyTitle.setStyle("-fx-text-fill: -app-text-2; -fx-font-size: 14px; -fx-font-weight: bold;");
            Label emptyHint = new Label(resource
                    ? "你可以从文件安装，或去下载页找找喜欢的" + title + "。"
                    : "安装或放入文件后会显示在这里。");
            emptyHint.setStyle("-fx-text-fill: -app-text-dim-3; -fx-font-size: 12px;");
            empty.getChildren().addAll(emptyTitle, emptyHint);
            if (resource) {
                HBox actions = new HBox(10);
                Button fromFile = ghostButton("从文件安装");
                fromFile.setOnAction(e -> installFiles(dir));
                Button download = ghostButton("下载新资源");
                download.setOnAction(e -> openUrl("resourcepacks".equals(tab)
                        ? "https://modrinth.com/resourcepacks" : "https://modrinth.com/shaders"));
                Button openFolder = ghostButton("打开文件夹");
                openFolder.setOnAction(e -> openDir(dir));
                actions.getChildren().addAll(fromFile, download, openFolder);
                empty.getChildren().add(actions);
            }
            list.getChildren().add(empty);
        } else {
            java.util.Arrays.sort(files, java.util.Comparator.comparing(File::getName));
            for (File file : files) {
                HBox row = new HBox(10);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(8, 12, 8, 12));
                row.setStyle("-fx-background-color: -app-card; -fx-background-radius: 8;");
                Label fileName = new Label(file.getName());
                fileName.setStyle("-fx-text-fill: -app-text-2; -fx-font-size: 12px;");
                HBox.setHgrow(fileName, Priority.ALWAYS);
                Label size = new Label(file.isDirectory() ? "文件夹" : (file.length() / 1024) + " KB");
                size.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11px;");
                row.getChildren().addAll(fileName, size);
                list.getChildren().add(row);
            }
        }

        Button open = ghostButton("打开文件夹");
        open.setOnAction(e -> openDir(dir));

        HBox actions = new HBox(10, open);
        if ("saves".equals(tab)) {
            Button backup = ghostButton("备份全部存档");
            backup.setOnAction(e -> backupSaves(dir));
            Button backupFolder = ghostButton("打开备份目录");
            backupFolder.setOnAction(e -> openDir(new File(gameDir, "backups/saves")));
            actions.getChildren().addAll(backup, backupFolder);
        }

        ScrollPane scroll = new ScrollPane(list);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox wrap = new VBox(10, head, path, actions, scroll);
        wrap.setPadding(new Insets(22, 24, 22, 24));
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return wrap;
    }

    /** 把整个存档目录压缩到 backups/saves，文件名带实例与时间。 */
    private void backupSaves(File savesDir) {
        File[] worlds = savesDir.listFiles();
        if (worlds == null || worlds.length == 0) {
            Toast.info("存档目录为空，无需备份");
            return;
        }
        File backupDir = new File(gameDir, "backups/saves");
        if (!backupDir.isDirectory() && !backupDir.mkdirs()) {
            Toast.error("无法创建备份目录");
            return;
        }
        String stamp = new java.text.SimpleDateFormat("yyyyMMdd-HHmmss").format(new java.util.Date());
        File out = new File(backupDir, instanceId + "-" + stamp + ".zip");
        log("💾 正在备份 " + worlds.length + " 个存档到 " + out.getName());
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                zipDirectory(savesDir, out);
                javafx.application.Platform.runLater(() -> {
                    Toast.ok("存档备份完成");
                    log("✅ 存档备份: " + out.getAbsolutePath());
                });
            } catch (Exception ex) {
                javafx.application.Platform.runLater(() -> {
                    Toast.error("备份失败：" + ex.getMessage());
                    log("❌ 存档备份失败: " + ex.getMessage());
                });
            }
        });
    }

    private VBox card(String title, String[][] rows) {
        VBox box = new VBox(8);
        box.setStyle("-fx-background-color: linear-gradient(to bottom, -app-card-top, -app-card-alt2);"
                + " -fx-background-radius: 16; -fx-border-color: -app-border-soft; -fx-border-radius: 16;"
                + " -fx-padding: 16 18; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.24), 18, 0.1, 0, 5);");
        Label head = new Label(title);
        head.setStyle("-fx-text-fill: -app-text-2; -fx-font-size: 13px; -fx-font-weight: bold;");
        box.getChildren().add(head);
        for (String[] row : rows) {
            HBox line = new HBox(10);
            Label key = new Label(row[0]);
            key.setStyle("-fx-text-fill: -app-text-dim-3; -fx-font-size: 12px;");
            key.setMinWidth(90);
            Label value = new Label(row[1]);
            value.setStyle("-fx-text-fill: -app-text-2; -fx-font-size: 12px;");
            value.setWrapText(true);
            HBox.setHgrow(value, Priority.ALWAYS);
            line.getChildren().addAll(key, value);
            box.getChildren().add(line);
        }
        return box;
    }

    private HBox navItem(String key, String text) {
        HBox item = new HBox(10);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(8, 18, 8, 20));
        item.setCursor(Cursor.HAND);
        item.setUserData(key);

        Rectangle bar = new Rectangle(2, 14, Color.TRANSPARENT);
        bar.setArcWidth(2);
        bar.setArcHeight(2);
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: -app-text-dim; -fx-font-size: 12px;");
        String iconName = switch (key) {
            case "overview" -> "layers";
            case "settings" -> "gear";
            case "modify" -> "edit";
            case "export" -> "download";
            case "saves" -> "folder";
            case "screenshots" -> "image";
            case "mods" -> "package";
            case "resourcepacks" -> "package";
            case "shaderpacks" -> "sparkles";
            case "schematics" -> "layers";
            case "servers" -> "server";
            default -> "dot";
        };
        item.getChildren().addAll(bar, Icons.node(iconName, 13, "#7b818b"), label);

        item.setOnMouseEntered(e -> {
            if (!key.equals(tab)) item.setStyle("-fx-background-color: "
                    + (Theme.isLight() ? "#eef2f7" : "-app-hover-2") + ";");
        });
        item.setOnMouseExited(e -> {
            if (!key.equals(tab)) item.setStyle("-fx-background-color: transparent;");
        });
        item.setOnMouseClicked(e -> select(key));
        navBox.getChildren().add(item);
        return item;
    }

    /** 鼠标移动时重算 hover，避免点击/布局变化后旧 hover 高亮残留。 */
    private void refreshNavStyles() {
        if (navBox == null) return;
        for (javafx.scene.Node node : navBox.getChildren()) {
            if (!(node instanceof HBox item) || !(node.getUserData() instanceof String key)) continue;
            boolean active = key.equals(tab);
            boolean hovered = false;
            String activeColor = Theme.isLight() ? "#ffffff" : "#232730";
            item.setStyle("-fx-background-color: "
                    + (active ? activeColor : (hovered ? "-app-hover-2" : "transparent")) + ";");
        }
    }

    private Label sectionLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11px; -fx-font-weight: bold;");
        label.setPadding(new Insets(10, 18, 4, 20));
        return label;
    }

    private Button ghostButton(String text) {
        Button button = new Button(text);
        button.setStyle("-fx-background-color: -app-row3; -fx-text-fill: -app-text-2; -fx-background-radius: 8;"
                + " -fx-padding: 7 14; -fx-cursor: hand; -fx-font-size: 12px;");
        return button;
    }

    /** 修改页：实例名 / 描述 / 图标说明。 */
    private VBox buildModifyTab(File instanceRoot) {
        VBox page = new VBox(14);
        page.setPadding(new Insets(22, 24, 22, 24));

        Label head = new Label("修改");
        head.setStyle("-fx-text-fill: -app-text; -fx-font-size: 19px; -fx-font-weight: bold;");
        Label hint = new Label("只影响启动器里显示的名字和描述，不会改版本 id。");
        hint.setStyle("-fx-text-fill: -app-text-dim-3; -fx-font-size: 12px;");

        String id = instanceId == null ? "" : instanceId;
        javafx.scene.control.TextField nameField = new javafx.scene.control.TextField(InstanceMeta.name(gameDir, id));
        nameField.setPromptText("实例名");
        String renameFieldStyle = "-fx-text-fill: -app-text; -fx-prompt-text-fill: -app-text-muted;"
                + " -fx-background-color: -app-row3; -fx-background-radius: 8; -fx-padding: 8 10;";
        nameField.setStyle(renameFieldStyle);
        javafx.scene.control.TextField descField = new javafx.scene.control.TextField(InstanceMeta.description(gameDir, id));
        descField.setStyle(renameFieldStyle);
        descField.setPromptText("一句话描述（可留空）");

        Label saved = new Label("");
        saved.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 12px;");

        Button save = new Button("保存");
        save.setStyle("-fx-background-color: #9ca3af; -fx-text-fill: #111827; -fx-font-weight: bold;"
                + " -fx-background-radius: 8; -fx-padding: 8 24; -fx-cursor: hand;");
        save.setOnAction(e -> {
            InstanceMeta.set(gameDir, id, nameField.getText().trim().isBlank() ? id : nameField.getText().trim(),
                    descField.getText().trim());
            saved.setText("已保存 ✓");
            log("✏️ 实例信息已更新: " + nameField.getText().trim());
            select("overview");
        });

        VBox form = new VBox(10,
                labeledField("实例名", nameField),
                labeledField("实例描述", descField),
                labeledField("实例 ID（只读）", new javafx.scene.control.Label(id)));
        form.setStyle("-fx-background-color: -app-card; -fx-background-radius: 10; -fx-border-color: -app-border;"
                + " -fx-border-radius: 10; -fx-padding: 16;");
        form.setMaxWidth(620);

        HBox actions = new HBox(10, saved, save);
        actions.setAlignment(Pos.CENTER_LEFT);

        page.getChildren().addAll(head, hint, form, actions);
        return page;
    }

    /** 导出页：导出启动脚本 / 打包实例。 */
    private VBox buildExportTab(File instanceRoot) {
        VBox page = new VBox(14);
        page.setPadding(new Insets(22, 24, 22, 24));

        Label head = new Label("导出");
        head.setStyle("-fx-text-fill: -app-text; -fx-font-size: 19px; -fx-font-weight: bold;");
        Label hint = new Label("导出启动脚本用于排查启动问题；导出实例会把整个实例目录打成 zip。");
        hint.setStyle("-fx-text-fill: -app-text-dim-3; -fx-font-size: 12px;");
        hint.setWrapText(true);

        Button script = ghostButton("导出启动脚本");
        script.setOnAction(e -> {
            if (onExportScript != null) onExportScript.accept(instanceId);
        });

        Button pack = ghostButton("导出实例压缩包");
        pack.setOnAction(e -> java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                File out = new File(instanceRoot.getParentFile(), instanceId + ".zip");
                zipDirectory(instanceRoot, out);
                javafx.application.Platform.runLater(() -> log("📦 实例已导出: " + out.getAbsolutePath()));
            } catch (Exception ex) {
                javafx.application.Platform.runLater(() -> log("❌ 导出失败: " + ex.getMessage()));
            }
        }));

        Button open = ghostButton("打开实例文件夹");
        open.setOnAction(e -> openDir(instanceRoot));

        HBox buttons = new HBox(10, script, pack, open);
        page.getChildren().addAll(head, hint, buttons);
        return page;
    }

    /** 服务器页：servers.dat 信息 + 打开目录。 */
    private VBox buildServersTab(File instanceRoot) {
        VBox page = new VBox(14);
        page.setPadding(new Insets(22, 24, 22, 24));

        Label head = new Label("服务器");
        head.setStyle("-fx-text-fill: -app-text; -fx-font-size: 19px; -fx-font-weight: bold;");
        File servers = new File(instanceRoot, "servers.dat");
        Label info = new Label(servers.isFile()
                ? "servers.dat 存在（" + (servers.length() / 1024) + " KB）：" + servers.getAbsolutePath()
                : "还没有 servers.dat。进游戏 → 多人游戏 → 添加服务器后会生成。");
        info.setStyle("-fx-text-fill: -app-text-dim; -fx-font-size: 12px;");
        info.setWrapText(true);

        Button open = ghostButton("打开实例文件夹");
        open.setOnAction(e -> openDir(instanceRoot));
        Button openServers = ghostButton("打开 servers.dat 所在目录");
        openServers.setOnAction(e -> openDir(instanceRoot));

        page.getChildren().addAll(head, info, new HBox(10, open, openServers));
        return page;
    }

    private VBox labeledField(String label, Node control) {
        Label tag = new Label(label);
        tag.setStyle("-fx-text-fill: -app-text-dim; -fx-font-size: 12px;");
        return new VBox(5, tag, control);
    }

    /** 把目录打包成 zip（导出实例用）。 */
    private void zipDirectory(File dir, File out) throws Exception {
        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(
                new java.io.FileOutputStream(out))) {
            java.nio.file.Path base = dir.toPath();
            java.util.List<java.nio.file.Path> paths = new java.util.ArrayList<>();
            try (java.util.stream.Stream<java.nio.file.Path> stream = java.nio.file.Files.walk(base)) {
                stream.filter(java.nio.file.Files::isRegularFile).forEach(paths::add);
            }
            for (java.nio.file.Path path : paths) {
                String entryName = base.relativize(path).toString().replace('\\', '/');
                zos.putNextEntry(new java.util.zip.ZipEntry(entryName));
                java.nio.file.Files.copy(path, zos);
                zos.closeEntry();
            }
        }
    }

    private void log(String message) {
        if (log != null) log.accept(message);
    }

    private int countFiles(File dir, String suffix) {
        File[] files = dir.listFiles((d, n) -> n.toLowerCase().endsWith(suffix));
        return files == null ? 0 : files.length;
    }

    private int countDirs(File dir) {
        File[] files = dir.listFiles(File::isDirectory);
        return files == null ? 0 : files.length;
    }

    private void openDir(File dir) {
        try {
            if (!dir.isDirectory() && !dir.mkdirs()) return;
            java.awt.Desktop.getDesktop().open(dir);
        } catch (Exception e) {
            try {
                new ProcessBuilder("explorer.exe", dir.getAbsolutePath()).start();
            } catch (Exception ignored) {
            }
        }
    }
}
