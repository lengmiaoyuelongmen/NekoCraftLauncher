package com.example.demo;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * 工具页：学习 PCL-CE 的「左侧页签 + 右侧内容」结构。
 * 页签：环境 / 缓存 / 快捷入口 / 百宝箱。
 */
public final class ToolsPanel extends VBox {

    private final LauncherConfig config;
    private final Consumer<String> log;
    private final Runnable reloadVersions;
    private final Label info = new Label();
    private final Label checkResult = new Label();
    private Integrity.Report lastReport;
    private long lastReportAt;

    private final Map<String, Node> pages = new LinkedHashMap<>();
    private final Map<String, HBox> tabs = new LinkedHashMap<>();
    private final StackPane content = new StackPane();

    public ToolsPanel(LauncherConfig config, Consumer<String> log, Runnable reloadVersions) {
        this.config = config;
        this.log = log;
        this.reloadVersions = reloadVersions;
        setSpacing(0);

        VBox side = new VBox(2);
        side.setPrefWidth(190);
        side.setMinWidth(190);
        side.setMaxWidth(190);
        side.setPadding(new Insets(14, 8, 12, 8));
        side.setStyle("-fx-background-color: -app-nav;"
                + " -fx-border-color: transparent -app-border-5 transparent transparent;");
        side.getChildren().addAll(
                sectionLabel("启动器"),
                tabItem("环境"),
                tabItem("性能优化"),
                tabItem("启动记录"),
                tabItem("缓存"),
                sectionLabel("其他"),
                tabItem("快捷入口"),
                tabItem("百宝箱"));

        pages.put("环境", buildEnvironment());
        pages.put("性能优化", buildPerformance());
        pages.put("启动记录", buildLaunchHistory());
        pages.put("缓存", buildCache());
        pages.put("快捷入口", buildShortcuts());
        pages.put("百宝箱", buildToolbox());

        content.setStyle("-fx-background-color: transparent;");
        HBox.setHgrow(content, Priority.ALWAYS);
        VBox.setVgrow(content, Priority.ALWAYS);

        ModuleHeader toolsHeader = new ModuleHeader("04 / TOOLBOX", "工具",
                "环境 · 缓存 · 诊断", "sparkles", ModuleHeader.Badge.DIAMOND, "Windows · Java 21");
        VBox.setMargin(toolsHeader, new Insets(12, 22, 2, 22));
        VBox right = new VBox(toolsHeader, content);
        VBox.setVgrow(content, Priority.ALWAYS);
        HBox.setHgrow(right, Priority.ALWAYS);

        HBox body = new HBox(side, right);
        VBox.setVgrow(body, Priority.ALWAYS);
        getChildren().add(body);

        checkResult.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11px;");
        checkResult.setWrapText(true);
        select("环境");
    }

    /** 外部入口（启动失败提示用）：切到百宝箱并开始校验。 */
    public void startLibraryCheck() {
        select("百宝箱");
        verifyLibrariesWithRepair();
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
        item.setOnMouseClicked(e -> select(name));
        tabs.put(name, item);
        return item;
    }

    public void openPerformanceTab() {
        select("性能优化");
    }

    public void openHistoryTab() {
        select("启动记录");
    }

    private void select(String name) {
        for (Map.Entry<String, HBox> entry : tabs.entrySet()) {
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
        Node page = pages.get(name);
        if (page == null) return;
        content.getChildren().setAll(page);
        Motion.pageIn(page, 8);
        if (page instanceof ScrollPane scroll) {
            Platform.runLater(() -> scroll.setVvalue(0));
        }
    }

    // ================== 内容 ==================

    private ScrollPane buildEnvironment() {
        refreshInfo();
        info.setStyle("-fx-text-fill: -app-text-dim-2; -fx-font-size: 12px;");
        info.setWrapText(true);

        VBox card = card(
                section("运行环境"),
                info,
                new HBox(10,
                        smallButton("重新读取已安装版本", e -> {
                            if (reloadVersions != null) reloadVersions.run();
                            refreshInfo();
                            log("🔄 已重新读取已安装版本");
                        }),
                        smallButton("打开版本目录", e -> openDir(new File(gameDir(), "versions"))),
                        smallButton("崩溃诊断", e -> {
                            CrashAnalyzer.Report report = CrashAnalyzer.analyze(gameDir());
                            log.accept(CrashAnalyzer.toText(report));
                            CrashReportDialog.show(getScene() == null ? null : getScene().getWindow(), report);
                        })));
        return scroll(card);
    }

    private ScrollPane buildPerformance() {
        Label intro = new Label("参考 BoosterX 的优化思路，只改可逆、低风险的项：电源计划、Game DVR、游戏模式、窗口透明。不会关闭安全软件或系统服务。");
        intro.setWrapText(true);
        intro.setStyle("-fx-text-fill: -app-text-dim; -fx-font-size: 12px;");

        CheckBox power = new CheckBox("切换到高性能电源计划");
        CheckBox dvr = new CheckBox("关闭 Game DVR 后台录制");
        CheckBox gameMode = new CheckBox("开启 Windows 游戏模式");
        CheckBox transparency = new CheckBox("关闭窗口透明效果");
        for (CheckBox box : new CheckBox[]{power, dvr, gameMode, transparency}) {
            box.setSelected(true);
            box.setStyle("-fx-text-fill: -app-text-2;");
        }

        Label status = new Label(PerformanceOptimizer.hasBackup(gameDir())
                ? "检测到优化备份，可随时恢复原设置。"
                : "尚未应用优化；修改前会自动记录原值。");
        status.setWrapText(true);
        status.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11.5px;");

        Button apply = smallButton("应用推荐优化", e -> {
            ((Button) e.getSource()).setDisable(true);
            log.accept("⚡ 正在应用性能优化...");
            CompletableFuture.supplyAsync(() -> PerformanceOptimizer.apply(gameDir(),
                            power.isSelected(), dvr.isSelected(), gameMode.isSelected(), transparency.isSelected()))
                    .thenAccept(result -> Platform.runLater(() -> {
                        ((Button) e.getSource()).setDisable(false);
                        for (PerformanceOptimizer.Step step : result.steps()) {
                            log.accept((step.ok() ? "✅ " : "⚠️ ") + step.name() + "：" + step.detail());
                        }
                        if (result.applied() > 0) {
                            config.setPerformanceApplied(true);
                            config.setPerformanceAsked(true);
                            config.save();
                        }
                        status.setText(result.anyFailed()
                                ? "部分项目失败，可查看日志；已成功的项目可以在下面恢复。"
                                : "已应用 " + result.applied() + " 项优化，可随时恢复。");
                        Toast.ok("性能优化完成：" + result.applied() + " 项");
                    })).exceptionally(ex -> {
                        Platform.runLater(() -> {
                            ((Button) e.getSource()).setDisable(false);
                            Toast.error("优化失败：" + ex.getMessage());
                        });
                        return null;
                    });
        });

        Button restore = smallButton("恢复优化前设置", e -> {
            if (!PerformanceOptimizer.hasBackup(gameDir())) {
                Toast.info("没有可恢复的优化备份");
                return;
            }
            ((Button) e.getSource()).setDisable(true);
            CompletableFuture.supplyAsync(() -> PerformanceOptimizer.restore(gameDir()))
                    .thenAccept(result -> Platform.runLater(() -> {
                        ((Button) e.getSource()).setDisable(false);
                        for (PerformanceOptimizer.Step step : result.steps()) {
                            log.accept((step.ok() ? "↩️ " : "⚠️ ") + step.name() + "：" + step.detail());
                        }
                        config.setPerformanceApplied(false);
                        config.save();
                        status.setText("已尝试恢复优化前的电源计划和注册表值。");
                        Toast.ok("已恢复优化前设置");
                    })).exceptionally(ex -> {
                        Platform.runLater(() -> {
                            ((Button) e.getSource()).setDisable(false);
                            Toast.error("恢复失败：" + ex.getMessage());
                        });
                        return null;
                    });
        });

        javafx.scene.control.ComboBox<String> priorityBox = new javafx.scene.control.ComboBox<>();
        priorityBox.getItems().addAll("普通", "高于正常", "高");
        priorityBox.setValue("above".equals(config.getGamePriority()) ? "高于正常"
                : "high".equals(config.getGamePriority()) ? "高" : "普通");
        priorityBox.setPrefWidth(150);
        priorityBox.valueProperty().addListener((o, a, value) -> {
            config.setGamePriority(switch (value) {
                case "高于正常" -> "above";
                case "高" -> "high";
                default -> "normal";
            });
            config.save();
        });

        CheckBox minimizeBox = new CheckBox("启动游戏后最小化启动器");
        minimizeBox.setSelected(config.isMinimizeOnLaunch());
        minimizeBox.setStyle("-fx-text-fill: -app-text-2;");
        minimizeBox.selectedProperty().addListener((o, a, value) -> {
            config.setMinimizeOnLaunch(value);
            config.save();
        });

        CheckBox pauseVisualsBox = new CheckBox("游戏运行时暂停背景动画");
        pauseVisualsBox.setSelected(config.isPauseVisualsInGame());
        pauseVisualsBox.setStyle("-fx-text-fill: -app-text-2;");
        pauseVisualsBox.selectedProperty().addListener((o, a, value) -> {
            config.setPauseVisualsInGame(value);
            config.save();
        });

        Label priorityLabel = new Label("游戏进程优先级");
        priorityLabel.setStyle("-fx-text-fill: -app-text-2; -fx-font-size: 12px;");
        HBox priorityRow = new HBox(10, priorityLabel, priorityBox);
        priorityRow.setAlignment(Pos.CENTER_LEFT);

        VBox processCard = card(
                section("游戏进程"),
                priorityRow,
                minimizeBox,
                pauseVisualsBox,
                hint("优先级独立于智能优化：普通 / 高于正常 / 高。启动后异步设置，失败不会阻止游戏。"));

        VBox card = card(
                new Label("性能优化") {{
                    setStyle("-fx-text-fill: -app-text; -fx-font-size: 15px; -fx-font-weight: bold;");
                }},
                intro,
                power, dvr, gameMode, transparency,
                new HBox(10, apply, restore),
                status,
                processCard);
        return scroll(card);
    }

    private ScrollPane buildLaunchHistory() {
        File dir = gameDir();
        java.util.List<LaunchHistory.Entry> entries = LaunchHistory.recent(dir);
        LaunchHistory.Summary summary = LaunchHistory.summary(dir);

        HBox stats = new HBox(10,
                historyStat("启动次数", String.valueOf(summary.total()), "-app-text"),
                historyStat("异常退出", String.valueOf(summary.crashes()),
                        summary.crashes() == 0 ? "#4CAF50" : "#e06c75"),
                historyStat("成功率", summary.successRate() + "%",
                        summary.successRate() >= 90 ? "#4CAF50" : "#d1a054"),
                historyStat("平均准备", summary.avgPrepMs() <= 0 ? "—"
                        : String.format(java.util.Locale.ROOT, "%.1f s", summary.avgPrepMs() / 1000.0),
                        "-app-text"));

        VBox list = new VBox(6);
        if (entries.isEmpty()) {
            Label empty = new Label("还没有启动记录。启动一次游戏后，这里会记录准备耗时、会话时长和异常情况。");
            empty.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 12px;");
            empty.setWrapText(true);
            list.getChildren().add(empty);
        } else {
            java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("MM-dd HH:mm");
            int shown = Math.min(15, entries.size());
            for (int i = 0; i < shown; i++) {
                LaunchHistory.Entry entry = entries.get(i);
                HBox row = new HBox(10);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(8, 12, 8, 12));
                row.setStyle("-fx-background-color: -app-row; -fx-background-radius: 9;");
                javafx.scene.shape.Circle dot = new javafx.scene.shape.Circle(4,
                        javafx.scene.paint.Color.web(entry.crash() ? "#e06c75" : "#4CAF50"));
                Label time = new Label(java.time.Instant.ofEpochMilli(entry.time())
                        .atZone(java.time.ZoneId.systemDefault()).format(fmt));
                time.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 10.5px;");
                time.setMinWidth(78);
                Label instance = new Label(entry.instanceId() == null || entry.instanceId().isBlank()
                        ? "未知实例" : entry.instanceId());
                instance.setStyle("-fx-text-fill: -app-text-2; -fx-font-size: 11.5px;");
                HBox.setHgrow(instance, Priority.ALWAYS);
                String prep = entry.prepMs() <= 0 ? "准备 —"
                        : String.format(java.util.Locale.ROOT, "准备 %.1fs", entry.prepMs() / 1000.0);
                String session = entry.sessionSeconds() <= 0 ? "会话 —"
                        : "会话 " + Playtime.format(entry.sessionSeconds());
                Label detail = new Label(prep + " · " + session + " · 退出码 " + entry.exitCode());
                detail.setStyle("-fx-text-fill: -app-text-dim-2; -fx-font-size: 10.5px;");
                row.getChildren().addAll(dot, time, instance, detail);
                list.getChildren().add(row);
            }
        }

        Button refresh = smallButton("刷新", e -> select("启动记录"));
        Button clear = smallButton("清空记录", e -> {
            LaunchHistory.clear(dir);
            Toast.ok("启动记录已清空");
            select("启动记录");
        });
        VBox card = card(
                section("启动记录"),
                hint("仅保存在本机 game/launch-history.json；记录准备耗时、会话时长、退出码和异常标记。"),
                stats,
                section("最近启动"),
                list,
                new HBox(10, refresh, clear));
        return scroll(card);
    }

    private VBox historyStat(String label, String value, String color) {
        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 20px; -fx-font-weight: bold;");
        Label nameLabel = new Label(label);
        nameLabel.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 10.5px;");
        VBox box = new VBox(3, valueLabel, nameLabel);
        box.setPadding(new Insets(10, 14, 10, 14));
        box.setStyle("-fx-background-color: -app-row; -fx-background-radius: 10;");
        HBox.setHgrow(box, Priority.ALWAYS);
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    private ScrollPane buildCache() {
        VBox card = card(
                section("缓存"),
                hint("natives 会在下次启动时自动重新解压；版本列表缓存删掉后下次刷新会重新拉取。"),
                new HBox(10,
                        smallButton("清空 natives 缓存", e -> clearNatives()),
                        smallButton("打开缓存目录", e -> openDir(new File("cache"))),
                        smallButton("删除版本列表缓存", e -> clearVersionCache())));
        return scroll(card);
    }

    private ScrollPane buildShortcuts() {
        VBox dirs = new VBox(8,
                action("打开游戏目录", () -> openDir(gameDir())),
                action("打开日志目录", () -> openDir(new File(gameDir(), "logs"))),
                action("打开模组目录", () -> openDir(new File(gameDir(), "mods"))),
                action("打开存档目录", () -> openDir(new File(gameDir(), "saves"))),
                action("打开截图目录", () -> openDir(new File(gameDir(), "screenshots"))),
                action("打开配置文件位置", () -> openDir(new File(".").getAbsoluteFile())));

        VBox links = new VBox(8,
                action("Modrinth", () -> openBrowser("https://modrinth.com")),
                action("CurseForge", () -> openBrowser("https://www.curseforge.com/minecraft")),
                action("PCL-CE 仓库", () -> openBrowser("https://github.com/PCL-Community/PCL-CE")));

        VBox card = card(section("目录"), dirs, section("链接"), links);
        return scroll(card);
    }

    private ScrollPane buildToolbox() {
        VBox card = card(
                section("小工具"),
                hint("百宝箱：把常用但不想占主界面的操作集中到这里。"),
                new HBox(10,
                        smallButton("网络诊断", e -> diagnoseNetwork()),
                        smallButton("复制环境信息", e -> copyEnvironment()),
                        smallButton("导出日志 zip", e -> exportLogs()),
                        smallButton("打开 Java 目录", e -> openDir(new File(gameDir(), "java")))),
                new HBox(10,
                        smallButton("清理无效实例目录", e -> cleanInvalidInstances()),
                        smallButton("统计游戏目录大小", e -> computeGameSize()),
                        smallButton("校验库文件", e -> verifyLibrariesWithRepair())),
                new HBox(10,
                        smallButton("导出完整性报告", e -> exportIntegrityReport())),
                checkResult,
                hint("环境信息包含启动器版本、Java、系统、目录和实例数量，方便反馈问题时贴给开发者。"));
        return scroll(card);
    }

    /** 删除 versions 下没有同名 JSON 的目录（先确认）。 */
    /** 网络诊断：并发测试登录、Modrinth、皮肤站连通性与延迟。 */
    private void diagnoseNetwork() {
        log.accept("🌐 正在测试登录与资源站连通性…");
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            String[][] targets = {
                    {"Mojang 登录", "https://authserver.mojang.com/"},
                    {"Modrinth API", "https://api.modrinth.com/"},
                    {"LittleSkin", "https://littleskin.cn/"}
            };
            StringBuilder report = new StringBuilder("网络诊断结果\n");
            for (String[] target : targets) {
                long start = System.nanoTime();
                int status = -1;
                String note;
                try {
                    java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                            .connectTimeout(java.time.Duration.ofSeconds(4)).build();
                    java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder(
                                    java.net.URI.create(target[1]))
                            .timeout(java.time.Duration.ofSeconds(5))
                            .method("HEAD", java.net.http.HttpRequest.BodyPublishers.noBody()).build();
                    status = client.send(request, java.net.http.HttpResponse.BodyHandlers.discarding()).statusCode();
                    note = (System.nanoTime() - start) / 1_000_000 + " ms";
                } catch (Exception ex) {
                    note = "不可达（" + (ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage()) + "）";
                }
                report.append(target[0]).append("：HTTP ").append(status).append(" · ").append(note).append("\n");
                log.accept("   " + target[0] + " → HTTP " + status + " · " + note);
            }
            String text = report.toString();
            javafx.application.Platform.runLater(() -> {
                Toast.info("网络诊断完成，结果已写入日志");
                log.accept(text);
            });
        });
    }

    private void cleanInvalidInstances() {
        File versions = new File(gameDir(), "versions");
        java.util.List<File> invalid = new java.util.ArrayList<>();
        File[] dirs = versions.listFiles(File::isDirectory);
        if (dirs != null) {
            for (File dir : dirs) {
                if (!new File(dir, dir.getName() + ".json").isFile()) invalid.add(dir);
            }
        }
        if (invalid.isEmpty()) {
            Toast.info("没有无效实例目录");
            return;
        }
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.CONFIRMATION);
        alert.setTitle("清理无效实例目录");
        alert.setHeaderText("发现 " + invalid.size() + " 个没有版本 JSON 的目录");
        alert.setContentText("会删除这些目录（不可恢复），确定继续？");
        if (alert.showAndWait().orElse(javafx.scene.control.ButtonType.CANCEL)
                != javafx.scene.control.ButtonType.OK) {
            return;
        }
        int removed = 0;
        for (File dir : invalid) {
            if (deleteRecursively(dir)) removed++;
        }
        log("🧹 已清理 " + removed + " 个无效实例目录");
        Toast.ok("已清理 " + removed + " 个无效实例目录");
        refreshInfo();
    }

    private boolean deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) deleteRecursively(child);
            }
        }
        return file.delete();
    }

    /** 校验每个已安装实例 JSON 里引用的库文件是否都在 libraries/ 下。 */
    private void verifyLibraries() {
        Toast.info("正在校验库文件…");
        File game = gameDir();
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            java.util.List<String> missing = new java.util.ArrayList<>();
            int checked = 0;
            for (String id : VersionProfile.installedIds(game)) {
                try {
                    VersionProfile profile = VersionProfile.load(game, id);
                    for (com.google.gson.JsonObject library : profile.classpathLibraries()) {
                        String path = libraryPath(library);
                        if (path == null) continue;
                        checked++;
                        File file = new File(new File(game, "libraries"), path.replace('/', File.separatorChar));
                        if (!file.isFile()) missing.add(id + " → " + path);
                    }
                } catch (Exception ignored) {
                    // 解析失败的实例跳过
                }
            }
            final int total = checked;
            final java.util.List<String> absent = missing;
            Platform.runLater(() -> {
                if (absent.isEmpty()) {
                    Toast.ok("库文件完整（检查 " + total + " 个）");
                    log("✅ 库文件校验通过，共检查 " + total + " 个");
                } else {
                    Toast.warn("缺失 " + absent.size() + " 个库文件（共检查 " + total + " 个）");
                    log("⚠ 缺失库文件 " + absent.size() + " 个：" + String.join(" | ",
                            absent.stream().limit(5).toList()));
                }
            });
        });
    }

    private String libraryPath(com.google.gson.JsonObject library) {
        if (library.has("name")) {
            String name = library.get("name").getAsString();
            if (library.has("downloads") && library.get("downloads").isJsonObject()) {
                com.google.gson.JsonObject downloads = library.getAsJsonObject("downloads");
                if (downloads.has("artifact") && downloads.get("artifact").isJsonObject()) {
                    com.google.gson.JsonObject artifact = downloads.getAsJsonObject("artifact");
                    if (artifact.has("path")) return artifact.get("path").getAsString();
                }
            }
            String[] parts = name.split(":");
            if (parts.length >= 3) {
                String group = parts[0].replace('.', '/');
                return group + "/" + parts[1] + "/" + parts[2] + "/" + parts[1] + "-" + parts[2] + ".jar";
            }
        }
        return null;
    }

    /** 导出完整性报告到游戏目录下的 txt。 */
    private void exportIntegrityReport() {
        Toast.info("正在生成完整性报告…");
        File game = gameDir();
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            Integrity.Report report = Integrity.check(game, null, config.getAssetSampleCount());
            StringBuilder text = new StringBuilder();
            text.append("Neko Craft Launcher · 完整性报告\n");
            text.append("时间: ").append(java.time.LocalDateTime.now()).append('\n');
            text.append("启动器: Neko Craft Launcher 1.1.0\n");
            text.append("Java: ").append(System.getProperty("java.version"))
                    .append(" (").append(System.getProperty("java.vendor")).append(")\n");
            text.append("系统: ").append(System.getProperty("os.name")).append(' ')
                    .append(System.getProperty("os.version")).append(' ')
                    .append(System.getProperty("os.arch")).append('\n');
            text.append("内存: ").append(config.getMemoryMb()).append(" MB")
                    .append(" | 下载源: ").append(config.getDownloadSource())
                    .append(" | 并发: ").append(config.getMaxConcurrent()).append('\n');
            text.append("游戏目录: ").append(game.getAbsolutePath()).append('\n');
            text.append("实例数: ").append(VersionProfile.installedIds(game).size()).append('\n');
            text.append("实例 Java 需求:\n");
            for (String id : VersionProfile.installedIds(game)) {
                try {
                    VersionProfile profile = VersionProfile.load(game, id);
                    int required = profile.getJavaMajorVersion();
                    JavaScanner.JavaInfo match = JavaScanner.bestMatch(game, required);
                    text.append("  - ").append(id).append("：需要 Java ").append(required)
                            .append(match == null ? "（本机未检测到匹配版本）"
                                    : "（本机匹配: " + match.label() + "）")
                            .append('\n');
                } catch (Exception ignored) {
                    // 解析失败就跳过这个实例
                }
            }
            text.append("检查文件: ").append(report.checked()).append(" 个（库 / 客户端 jar / 资源索引）\n");
            text.append("缺失: ").append(report.issues().size()).append(" 个\n");
            java.util.Map<String, java.util.List<Integrity.Issue>> byInstance = new java.util.LinkedHashMap<>();
            for (Integrity.Issue issue : report.issues()) {
                byInstance.computeIfAbsent(issue.instanceId(), key -> new java.util.ArrayList<>()).add(issue);
            }
            for (var entry : byInstance.entrySet()) {
                text.append("\n实例 ").append(entry.getKey()).append("：")
                        .append(entry.getValue().size()).append(" 个缺失\n");
                for (Integrity.Issue issue : entry.getValue()) {
                    text.append("  - [").append(issue.label()).append("] ")
                            .append(issue.path()).append('\n');
                }
            }
            String stamp = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            File reportsDir = new File(game, "reports");
            if (!reportsDir.isDirectory() && !reportsDir.mkdirs()) {
                Platform.runLater(() -> Toast.error("无法创建报告目录: " + reportsDir.getAbsolutePath()));
                return;
            }
            File out = new File(reportsDir, "integrity-report-" + stamp + ".txt");
            try {
                java.nio.file.Files.writeString(out.toPath(), text.toString());

                // 每个实例再单独出一份，方便单独贴给服主/开发者
                int perInstance = 0;
                for (String id : VersionProfile.installedIds(game)) {
                    java.util.List<Integrity.Issue> issues = report.issues().stream()
                            .filter(issue -> issue.instanceId().equals(id)).toList();
                    StringBuilder part = new StringBuilder();
                    part.append("实例: ").append(id).append('\n');
                    part.append("时间: ").append(java.time.LocalDateTime.now()).append('\n');
                    part.append("检查文件: ").append(report.checked()).append(" 个（全部实例合计）\n");
                    part.append("该实例缺失: ").append(issues.size()).append(" 个\n\n");
                    for (Integrity.Issue issue : issues) {
                        part.append('[').append(issue.label()).append("] ")
                                .append(issue.path()).append('\n');
                    }
                    File partFile = new File(reportsDir,
                            "integrity-" + sanitizeFileName(id) + "-" + stamp + ".txt");
                    java.nio.file.Files.writeString(partFile.toPath(), part.toString());
                    perInstance++;
                }

                int instanceCount = perInstance;
                Platform.runLater(() -> {
                    copyToClipboard(out.getAbsolutePath());
                    Toast.ok("报告已导出（含 " + instanceCount + " 个分实例文件）并复制路径");
                    log("📄 完整性报告: " + out.getAbsolutePath());
                });
            } catch (Exception e) {
                Platform.runLater(() -> Toast.error("导出失败: " + e.getMessage()));
            }
        });
    }

    /** 校验并可选一键补齐缺失库文件（走 DownloadManager）。 */
    private void verifyLibrariesWithRepair() {
        Toast.info("正在校验库文件…");
        File game = gameDir();
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            Integrity.Report report = Integrity.check(game, null, config.getAssetSampleCount());
            Platform.runLater(() -> {
                lastReport = report;
                lastReportAt = System.currentTimeMillis();
                updateCheckResultClick();
                if (report.issues().isEmpty()) {
                    checkResult.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 11px;");
                    checkResult.setText("检查 " + report.checked() + " 个文件 · 无缺失 ✓");
                    Toast.ok("文件完整（检查 " + report.checked() + " 个）");
                    log("✅ 完整性校验通过，共检查 " + report.checked() + " 个");
                    return;
                }
                checkResult.setStyle("-fx-text-fill: #ffb74d; -fx-font-size: 11px;");
                checkResult.setText("检查 " + report.checked() + " 个文件 · 缺失 "
                        + report.issues().size() + " 个（点击查看明细）");
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                        javafx.scene.control.Alert.AlertType.CONFIRMATION);
                alert.setTitle("完整性校验");
                alert.setHeaderText("缺失 " + report.issues().size() + " 个文件（共检查 "
                        + report.checked() + " 个）");
                alert.setContentText("是否立即下载补齐？");
                if (alert.showAndWait().orElse(javafx.scene.control.ButtonType.CANCEL)
                        != javafx.scene.control.ButtonType.OK) {
                    return;
                }
                Toast.info("正在补齐文件…");
                java.util.concurrent.CompletableFuture.runAsync(() -> {
                    int ok = Integrity.repair(game, report.issues());
                    Integrity.Report after = Integrity.check(game, null, config.getAssetSampleCount());
                    Platform.runLater(() -> {
                        lastReport = after;
                        lastReportAt = System.currentTimeMillis();
                        updateCheckResultClick();
                        checkResult.setStyle("-fx-text-fill: "
                                + (after.issues().isEmpty() ? "#9ca3af" : "#ffb74d")
                                + "; -fx-font-size: 11px;");
                        checkResult.setText("已补齐 " + ok + "/" + report.issues().size()
                                + " · 剩余缺失 " + after.issues().size() + " 个"
                                + (after.issues().isEmpty() ? "" : "（点击查看明细）"));
                        Toast.ok("已补齐 " + ok + "/" + report.issues().size() + " 个文件");
                        log("🔧 补齐文件 " + ok + "/" + report.issues().size());
                    });
                });
            });
        });
    }

    /** 有缺失时让结果行可点，弹出明细。 */
    private void updateCheckResultClick() {
        boolean hasIssues = lastReport != null && !lastReport.issues().isEmpty();
        checkResult.setCursor(hasIssues ? Cursor.HAND : Cursor.DEFAULT);
        checkResult.setOnMouseClicked(hasIssues ? e -> showIssueDetails() : null);
    }

    private void showIssueDetails() {
        if (lastReport == null || lastReport.issues().isEmpty()) return;
        StringBuilder text = new StringBuilder();
        StringBuilder plain = new StringBuilder();
        int shown = 0;
        for (Integrity.Issue issue : lastReport.issues()) {
            if (shown >= 30) {
                text.append("… 还有 ").append(lastReport.issues().size() - shown).append(" 项\n");
                break;
            }
            shown++;
            text.append('[').append(issue.label()).append("] ").append(issue.instanceId()).append('\n')
                    .append("    ").append(issue.path()).append('\n');
            plain.append('[').append(issue.label()).append("] ").append(issue.instanceId())
                    .append(' ').append(issue.path()).append('\n');
        }
        javafx.scene.control.TextArea area = new javafx.scene.control.TextArea(text.toString());
        area.setEditable(false);
        area.setPrefSize(580, 320);
        javafx.scene.control.ButtonType copy = new javafx.scene.control.ButtonType(
                "复制缺失列表", javafx.scene.control.ButtonBar.ButtonData.OTHER);
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.INFORMATION, "", copy,
                javafx.scene.control.ButtonType.CLOSE);
        alert.setTitle("缺失明细");
        alert.setHeaderText("共 " + lastReport.issues().size() + " 项缺失（检查 "
                + lastReport.checked() + " 个文件）");
        alert.getDialogPane().setContent(area);
        alert.showAndWait().ifPresent(clicked -> {
            if (clicked == copy) {
                copyToClipboard(plain.toString());
                Toast.ok("缺失列表已复制（" + lastReport.issues().size() + " 项）");
            }
        });
    }

    /** 最近一次校验结果（60 秒内有效），供启动前自检复用。 */
    public Integrity.Report freshReport() {
        if (lastReport == null) return null;
        return System.currentTimeMillis() - lastReportAt <= 60_000 ? lastReport : null;
    }

    private static String sanitizeFileName(String name) {
        return name == null ? "instance" : name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    /** 优先用 AWT 系统剪贴板（JavaFX 剪贴板在部分环境退出后会丢）。 */
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

    /** 后台统计游戏目录大小。 */
    private void computeGameSize() {
        Toast.info("正在统计游戏目录大小…");
        File dir = gameDir();
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            long bytes = 0;
            try (var stream = java.nio.file.Files.walk(dir.toPath())) {
                bytes = stream.filter(java.nio.file.Files::isRegularFile)
                        .mapToLong(path -> {
                            try {
                                return java.nio.file.Files.size(path);
                            } catch (Exception e) {
                                return 0;
                            }
                        }).sum();
            } catch (Exception ignored) {
                // 统计失败按 0 处理
            }
            long mb = bytes / 1024 / 1024;
            Platform.runLater(() -> {
                Toast.ok("游戏目录约 " + mb + " MB");
                log("📦 游戏目录大小约 " + mb + " MB");
            });
        });
    }

    // ================== 操作 ==================

    private File gameDir() {
        return new File(config.getGameDir());
    }

    private void log(String message) {
        if (log != null) log.accept(message);
    }

    private void refreshInfo() {
        File game = gameDir();
        long usableGb = game.isDirectory() ? game.getUsableSpace() / 1024 / 1024 / 1024 : -1;
        long installed = VersionProfile.installedIds(game).size();
        File mods = new File(game, "mods");
        File[] modFiles = mods.listFiles((d, n) -> n.toLowerCase().endsWith(".jar"));
        File javaDir = new File(game, "java");
        File[] jdks = javaDir.listFiles(File::isDirectory);
        info.setText("游戏目录：" + game.getAbsolutePath()
                + "\n已安装版本：" + installed + " 个"
                + "　|　模组：" + (modFiles == null ? 0 : modFiles.length) + " 个"
                + "　|　运行环境：" + (jdks == null ? 0 : jdks.length) + " 个"
                + "\n可用空间：" + (usableGb < 0 ? "未知" : usableGb + " GB")
                + "　|　Java：" + System.getProperty("java.version")
                + "　|　系统：" + System.getProperty("os.name") + " " + System.getProperty("os.arch"));
    }

    private void clearNatives() {
        File nativesDir = new File(gameDir(), "natives");
        int count = 0;
        File[] children = nativesDir.listFiles();
        if (children != null) {
            for (File child : children) {
                if (child.isDirectory()) {
                    File[] inner = child.listFiles((d, n) -> n.toLowerCase().endsWith(".dll"));
                    if (inner != null) {
                        for (File file : inner) {
                            if (file.delete()) count++;
                        }
                    }
                } else if (child.getName().toLowerCase().endsWith(".dll")) {
                    if (child.delete()) count++;
                }
            }
        }
        log("🧹 已清理 " + count + " 个 dll，下次启动会重新解压");
        Toast.ok("已清理 " + count + " 个 natives 文件");
        refreshInfo();
    }

    private void clearVersionCache() {
        File cache = new File("cache/version_manifest.json");
        boolean deleted = cache.isFile() && cache.delete();
        Toast.ok(deleted ? "已删除版本列表缓存" : "没有版本列表缓存");
        log(deleted ? "🧹 已删除版本列表缓存" : "ℹ 没有版本列表缓存");
    }

    private void copyEnvironment() {
        String text = "Neko Craft Launcher 1.1.0\n"
                + "Java: " + System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")\n"
                + "OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version")
                + " " + System.getProperty("os.arch") + "\n"
                + "游戏目录: " + gameDir().getAbsolutePath() + "\n"
                + "工作目录: " + new File(".").getAbsolutePath() + "\n"
                + "实例数: " + VersionProfile.installedIds(gameDir()).size();
        copyToClipboard(text);
        Toast.ok("环境信息已复制到剪贴板");
    }

    private void exportLogs() {
        File logs = new File(gameDir(), "logs");
        if (!logs.isDirectory()) {
            Toast.warn("还没有日志目录");
            return;
        }
        File target = new File(gameDir(), "logs-export-" + System.currentTimeMillis() + ".zip");
        try {
            ZipUtil.zipDirectory(logs, target);
            Toast.ok("日志已导出：" + target.getName());
            log("日志已导出: " + target.getAbsolutePath());
            openDir(gameDir());
        } catch (Exception e) {
            Toast.error("导出失败: " + e.getMessage());
        }
    }

    private HBox action(String text, Runnable action) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 12, 10, 12));
        row.getStyleClass().add("instance-row");
        row.setCursor(Cursor.HAND);
        row.setOnMouseClicked(e -> {
            try {
                action.run();
            } catch (Exception ex) {
                log("❌ 操作失败: " + ex.getMessage());
            }
        });

        Label label = new Label(text);
        label.setStyle("-fx-text-fill: -app-text-2; -fx-font-size: 13px;");
        HBox.setHgrow(label, Priority.ALWAYS);
        Label arrow = new Label("›");
        arrow.setStyle("-fx-text-fill: -app-text-dim-3; -fx-font-size: 16px;");
        row.getChildren().addAll(label, arrow);
        return row;
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

    private ScrollPane scroll(Node node) {
        VBox wrap = new VBox(14, node);
        wrap.setPadding(new Insets(18, 22, 18, 22));
        wrap.setAlignment(Pos.TOP_CENTER);
        ScrollPane scroll = new ScrollPane(wrap);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        return scroll;
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

    private static Label section(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 12px; -fx-font-weight: bold;");
        label.setPadding(new Insets(6, 0, 0, 0));
        return label;
    }

    private static Label hint(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11px;");
        label.setWrapText(true);
        return label;
    }

    private void openDir(File dir) {
        try {
            if (!dir.isDirectory() && !dir.mkdirs()) {
                log("❌ 目录不存在且无法创建: " + dir.getAbsolutePath());
                return;
            }
            java.awt.Desktop.getDesktop().open(dir);
        } catch (Exception e) {
            try {
                new ProcessBuilder("explorer.exe", dir.getAbsolutePath()).start();
            } catch (Exception ex) {
                log("❌ 打开目录失败: " + e.getMessage());
            }
        }
    }

    private void openBrowser(String url) {
        try {
            java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
        } catch (Exception e) {
            log("❌ 打开浏览器失败: " + e.getMessage());
        }
    }
}
