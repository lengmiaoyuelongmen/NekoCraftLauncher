package com.example.demo.ui.page;

import com.example.demo.core.app.LauncherConfig;
import com.example.demo.core.i18n.I18n;
import com.example.demo.core.server.PortReachability;
import com.example.demo.core.server.PortableServerManager;
import com.example.demo.core.server.ServerFiles;
import com.example.demo.core.server.UpnpPortMapper;
import com.example.demo.ui.shell.Motion;
import com.example.demo.ui.shell.Toast;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 服务器控制台（参考主流面板的布局）。
 *
 * <p>左侧服务端列表（带运行状态点），右侧是选中服的面板：
 * 顶部状态 + 启停/重启，下面是 控制台 / 配置 / 玩家 / 信息 四个标签页。
 *
 * <p>为什么要这么改：原来的单列布局把「新建、地址、日志、命令」全堆在一页，
 * 开两个服之后就找不到谁是谁；主流面板都是「左侧选服、右侧看板」的结构。
 */
public final class ServerPanel {

    private final VBox root = new VBox(10);
    private final LauncherConfig config;
    private final File gameDir;

    private final VBox listBox = new VBox(6);
    private final StackPane content = new StackPane();

    private final Map<String, PortableServerManager.RunningServer> running = new LinkedHashMap<>();
    private final Map<String, StringBuilder> logs = new LinkedHashMap<>();

    // 资源曲线：CPU / 内存各保留最近 60 个采样点
    private final Canvas chart = new Canvas(280, 58);
    private final java.util.Deque<Double> cpuHistory = new java.util.ArrayDeque<>();
    private final java.util.Deque<Double> memHistory = new java.util.ArrayDeque<>();
    private final Map<String, com.example.demo.core.server.ServerMetrics.CpuSampler> samplers =
            new LinkedHashMap<>();
    private final Map<String, Long> lastBackupAt = new LinkedHashMap<>();
    private final Map<String, Long> lastRestartAt = new LinkedHashMap<>();
    private javafx.animation.Timeline samplerTimer;
    private javafx.animation.Timeline scheduleTimer;
    private String selectedId;
    private boolean creating;

    // 右侧控件（选中某个服后才填内容）
    private final Label titleLabel = new Label();
    private final Label statusPill = new Label();
    private final Label addressLocal = new Label("—");
    private final Label addressPublic = new Label("—");
    private final TextArea console = new TextArea();
    private final TextField commandField = new TextField();
    private final TabPane tabs = new TabPane();
    private final Label hintLine = new Label("");

    public ServerPanel(LauncherConfig config, File gameDir) {
        this.config = config;
        this.gameDir = gameDir;
        build();
        refreshList();
    }

    public VBox node() {
        return root;
    }

    // ==================== 骨架 ====================

    private void build() {
        root.setPadding(new Insets(8, 18, 16, 18));

        // 与其它页面共用同一个模块头（编号 / 图形 / 水印），避免这一页自成一套视觉
        com.example.demo.ui.shell.ModuleHeader header =
                new com.example.demo.ui.shell.ModuleHeader(
                        "05 / SERVER",
                        I18n.t("server.title", "便携服务器"),
                        I18n.t("server.subtitle2", "实例 · 端口 · 日志 · 备份"),
                        "server",
                        com.example.demo.ui.shell.ModuleHeader.Badge.SQUARE,
                        "LAN / WAN");

        VBox sidebar = new VBox(10);
        sidebar.setPrefWidth(268);
        sidebar.setMinWidth(236);
        sidebar.getStyleClass().add("neko-card");
        sidebar.setPadding(new Insets(12));

        Label title = new Label(I18n.t("server.panel.list", "服务端"));
        title.setStyle("-fx-text-fill: -app-text; -fx-font-size: 12.5px; -fx-font-weight: bold;");
        Button create = new Button(I18n.t("server.panel.new", "＋ 新建"));
        create.getStyleClass().addAll("btn-ghost", "btn-sm");
        create.setOnAction(e -> showCreateForm());
        // 一键开服：当前实例没有服务端就建一个再启动，省掉「新建 → 填表 → 启动」三步
        Button quick = new Button(I18n.t("server.panel.quickStart", "⚡ 开服"));
        quick.getStyleClass().addAll("btn-primary", "btn-sm");
        quick.setOnAction(e -> quickStart(quick));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox head = new HBox(8, title, spacer, quick, create);
        head.setAlignment(Pos.CENTER_LEFT);

        ScrollPane scroll = new ScrollPane(listBox);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-padding: 0;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        Label listHint = new Label(I18n.t("server.panel.listHint", "点一行切换，绿点表示运行中"));
        listHint.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 10.5px;");
        listHint.setWrapText(true);

        sidebar.getChildren().addAll(head, scroll, listHint);

        VBox main = new VBox(10);
        HBox.setHgrow(main, Priority.ALWAYS);
        main.setMinWidth(0);
        main.getChildren().add(content);
        VBox.setVgrow(content, Priority.ALWAYS);
        content.getChildren().add(placeholder());

        HBox body = new HBox(12, sidebar, main);
        VBox.setVgrow(body, Priority.ALWAYS);

        root.getChildren().addAll(header, body);
    }

    /**
     * 一键开服：优先用当前实例对应的已有服务端；没有就按默认端口 + 配置内存建一个再启动。
     *
     * <p>把「新建 → 填端口/内存 → 创建 → 启动」压成一次点击，新手不用知道这些细节。
     */
    private void quickStart(Button trigger) {
        String instanceId = config.getLastInstance();
        if (instanceId == null || instanceId.isBlank()) {
            Toast.warn(I18n.t("server.error.instance", "请先在启动页选一个实例"));
            return;
        }
        var existing = PortableServerManager.load(gameDir).stream()
                .filter(p -> instanceId.equals(p.name()))
                .findFirst()
                .orElse(null);
        if (existing != null) {
            selectedId = existing.id();
            refreshList();
            select(existing);
            if (running.containsKey(existing.id())) {
                Toast.info(I18n.t("server.panel.quickRunning", "这个服务端已经在运行了"));
            } else {
                startServer(existing);
            }
            return;
        }
        trigger.setDisable(true);
        int port = nextFreePort();
        int memory = Math.max(1024, config.getMemoryMb());
        boolean isolation = com.example.demo.core.model.InstanceSettings.effectiveIsolation(
                gameDir, instanceId, config.isIsolation());
        Toast.info(I18n.t("server.panel.quickCreating", "正在为 ") + instanceId
                + I18n.t("server.panel.quickCreatingTail", " 创建服务端…"));
        CompletableFuture.runAsync(() -> {
            try {
                PortableServerManager.Profile created = PortableServerManager.create(
                        gameDir, instanceId, isolation, port, memory, false, "Neko Server " + instanceId,
                        p -> { });
                Platform.runLater(() -> {
                    trigger.setDisable(false);
                    selectedId = created.id();
                    refreshList();
                    select(created);
                    appendLog(created.id(), I18n.t("server.panel.quickCreated", "[已创建并准备启动] ") + port);
                    startServer(created);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    trigger.setDisable(false);
                    Toast.error(I18n.t("server.create.failedToast", "创建失败：") + ex.getMessage());
                });
            }
        });
    }

    /** 从 25565 往上找一个没被已有服务端占用的端口。 */
    private int nextFreePort() {
        var used = new java.util.HashSet<Integer>();
        PortableServerManager.load(gameDir).forEach(p -> used.add(p.port()));
        running.keySet().forEach(id -> {
            var profile = PortableServerManager.load(gameDir).stream()
                    .filter(p -> p.id().equals(id)).findFirst().orElse(null);
            if (profile != null) {
                used.add(profile.port());
            }
        });
        for (int port = 25565; port < 25665; port++) {
            if (!used.contains(port)) {
                return port;
            }
        }
        return 25565;
    }
    private VBox placeholder() {
        Label label = new Label(I18n.t("server.panel.empty", "从左侧选一个服务端，或点「＋ 新建」"));
        label.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 12.5px;");
        VBox box = new VBox(label);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    private void refreshList() {
        listBox.getChildren().clear();
        List<PortableServerManager.Profile> profiles = PortableServerManager.load(gameDir);
        if (profiles.isEmpty()) {
            Label empty = new Label(I18n.t("server.list.empty", "还没有服务端，点右上角「＋ 新建」"));
            empty.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11.5px;");
            empty.setWrapText(true);
            listBox.getChildren().add(empty);
            return;
        }
        for (PortableServerManager.Profile profile : profiles) {
            listBox.getChildren().add(listRow(profile));
        }
        Motion.staggerRows(listBox.getChildren(), 22, 10);
        if (selectedId == null || profiles.stream().noneMatch(p -> p.id().equals(selectedId))) {
            select(profiles.get(0));
        }
    }

    private HBox listRow(PortableServerManager.Profile profile) {
        boolean alive = running.containsKey(profile.id());
        Label dot = new Label(alive ? "●" : "○");
        dot.setStyle("-fx-text-fill: " + (alive ? "#22c55e" : "#94a3b8") + "; -fx-font-size: 10px;");
        Label name = new Label(profile.id());
        name.setStyle("-fx-text-fill: -app-text; -fx-font-size: 12px;"
                + (profile.id().equals(selectedId) ? " -fx-font-weight: bold;" : ""));
        Label meta = new Label(profile.loader() + " " + profile.mcVersion() + " · " + profile.port());
        meta.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 10.5px;");
        VBox text = new VBox(1, name, meta);
        HBox row = new HBox(8, dot, text);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(7, 8, 7, 8));
        row.setStyle("-fx-background-radius: 9; -fx-background-color: "
                + (profile.id().equals(selectedId) ? "-app-hover-2" : "transparent") + ";");
        row.setOnMouseClicked(e -> select(profile));
        return row;
    }

    private void select(PortableServerManager.Profile profile) {
        selectedId = profile.id();
        refreshList();
        content.getChildren().setAll(detail(profile));
    }

    /** 首次选中某个服时，把 server-console.log 的尾部读进来 —— 面板该有历史，而不是空白。 */
    private void loadHistory(PortableServerManager.Profile profile) {
        if (logs.containsKey(profile.id())) {
            return;
        }
        File logFile = new File(profile.dir(), "server-console.log");
        StringBuilder buffer = new StringBuilder();
        if (logFile.isFile()) {
            try {
                List<String> lines = java.nio.file.Files.readAllLines(logFile.toPath(),
                        java.nio.charset.StandardCharsets.UTF_8);
                int from = Math.max(0, lines.size() - 200);
                for (int i = from; i < lines.size(); i++) {
                    buffer.append(lines.get(i)).append('\n');
                }
            } catch (Exception ignored) {
                // 读不到就空白开始
            }
        }
        logs.put(profile.id(), buffer);
    }
    // ==================== 右侧面板 ====================

    private VBox detail(PortableServerManager.Profile profile) {
        VBox box = new VBox(10);

        titleLabel.setText(profile.id());
        titleLabel.setStyle("-fx-text-fill: -app-text; -fx-font-size: 15px; -fx-font-weight: bold;");
        updatePill(profile.id());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        boolean alive = running.containsKey(profile.id());
        Button startStop = new Button(alive
                ? I18n.t("server.row.stop", "停止")
                : I18n.t("server.row.start", "启动"));
        startStop.getStyleClass().addAll(alive ? "btn-secondary" : "btn-primary", "btn-sm");
        startStop.setOnAction(e -> {
            if (running.containsKey(profile.id())) {
                stopServer(profile.id());
            } else {
                startServer(profile);
            }
        });
        Button restart = new Button(I18n.t("server.panel.restart", "重启"));
        restart.getStyleClass().addAll("btn-secondary", "btn-sm");
        restart.setOnAction(e -> restartServer(profile));
        Button open = new Button(I18n.t("server.row.open", "打开目录"));
        open.getStyleClass().addAll("btn-secondary", "btn-sm");
        open.setOnAction(e -> openDir(new File(profile.dir())));
        Button forget = new Button(I18n.t("server.row.remove", "移除记录"));
        forget.getStyleClass().addAll("btn-ghost", "btn-sm");
        forget.setOnAction(e -> {
            if (running.containsKey(profile.id())) {
                Toast.warn(I18n.t("server.panel.stopFirst", "先停止再移除"));
                return;
            }
            PortableServerManager.forget(gameDir, profile.id());
            selectedId = null;
            refreshList();
        });

        HBox header = new HBox(8, titleLabel, statusPill, spacer, startStop, restart, open, forget);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("neko-card");
        header.setPadding(new Insets(12));

        addressLocal.setText(localAddress(profile));
        addressPublic.setText(publicAddress(profile));
        Button copyLocal = chip(I18n.t("server.ip.copyLocal", "复制局域网地址"),
                () -> copy(addressLocal.getText()));
        Button copyPublic = chip(I18n.t("server.ip.copyPublic", "复制公网地址"),
                () -> copy(addressPublic.getText()));
        Label lan = new Label(I18n.t("server.addr.local", "局域网") + " ");
        lan.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11px;");
        Label pub = new Label(I18n.t("server.addr.public", "公网") + " ");
        pub.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11px;");
        addressLocal.setStyle("-fx-text-fill: -app-text; -fx-font-size: 12px; -fx-font-weight: bold;");
        addressPublic.setStyle("-fx-text-fill: -app-text; -fx-font-size: 12px; -fx-font-weight: bold;");
        HBox chips = new HBox(8, lan, addressLocal, copyLocal,
                new Label("  "), pub, addressPublic, copyPublic);
        chips.setAlignment(Pos.CENTER_LEFT);

        tabs.getTabs().setAll(
                tab(I18n.t("server.panel.tab.console", "控制台"), consolePane(profile)),
                tab(I18n.t("server.panel.tab.config", "配置"), configPane(profile)),
                tab(I18n.t("server.panel.tab.players", "玩家"), playersPane(profile)),
                tab(I18n.t("server.panel.tab.files", "文件"), filesPane(profile)),
                tab(I18n.t("server.panel.tab.info", "信息"), infoPane(profile)));
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        if (!tabs.getStyleClass().contains("server-tabs")) {
            tabs.getStyleClass().add("server-tabs");
        }
        VBox.setVgrow(tabs, Priority.ALWAYS);

        box.getChildren().addAll(header, chips, tabs);
        VBox.setVgrow(box, Priority.ALWAYS);
        return box;
    }

    private Tab tab(String title, javafx.scene.Node content) {
        Tab tab = new Tab(title, content);
        return tab;
    }

    private void updatePill(String id) {
        boolean alive = running.containsKey(id);
        statusPill.setText(alive
                ? I18n.t("server.pill.running", "运行中")
                : I18n.t("server.pill.stopped", "已停止"));
        statusPill.setStyle("-fx-font-size: 10.5px; -fx-padding: 2 8; -fx-background-radius: 8;"
                + " -fx-background-color: " + (alive ? "rgba(34,197,94,0.16)" : "rgba(148,163,184,0.18)")
                + "; -fx-text-fill: " + (alive ? "#22c55e" : "#94a3b8") + ";");
    }

    // ==================== 控制台 ====================

    private VBox consolePane(PortableServerManager.Profile profile) {
        console.setEditable(false);
        console.setWrapText(false);
        if (!console.getStyleClass().contains("server-console")) {
            console.getStyleClass().add("server-console");
        }
        StringBuilder existing = logs.get(profile.id());
        console.setText(existing == null ? "" : existing.toString());
        VBox.setVgrow(console, Priority.ALWAYS);

        commandField.setPromptText(I18n.t("server.console.prompt", "输入命令，例如 list / op 玩家名 / stop"));
        HBox.setHgrow(commandField, Priority.ALWAYS);
        commandField.setOnAction(e -> sendCommand(profile));
        Button send = new Button(I18n.t("server.console.send", "发送"));
        send.getStyleClass().addAll("btn-primary", "btn-sm");
        send.setOnAction(e -> sendCommand(profile));

        HBox quick = new HBox(6,
                quickCommand(profile, "list"),
                quickCommand(profile, "save-all"),
                quickCommand(profile, "whitelist list"),
                quickCommand(profile, "stop"));
        Button clear = new Button(I18n.t("server.log.clear", "清空日志"));
        clear.getStyleClass().addAll("btn-ghost", "btn-sm");
        clear.setOnAction(e -> {
            console.clear();
            StringBuilder buffer = logs.get(profile.id());
            if (buffer != null) {
                buffer.setLength(0);
            }
        });
        Button copyLog = new Button(I18n.t("server.log.copy", "复制日志"));
        copyLog.getStyleClass().addAll("btn-ghost", "btn-sm");
        copyLog.setOnAction(e -> copy(console.getText()));
        HBox input = new HBox(6, commandField, send, clear, copyLog);
        input.setAlignment(Pos.CENTER_LEFT);

        hintLine.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 10.5px;");
        hintLine.setText(I18n.t("server.panel.consoleHint",
                "日志写入 server-console.log；命令直接发给服务端，回显会带上 > 前缀"));

        drawChart();
        if (running.containsKey(profile.id())) {
            startSampling(profile);
        }
        VBox box = new VBox(8, chart, console, quick, input, hintLine);
        box.setPadding(new Insets(10));
        VBox.setVgrow(box, Priority.ALWAYS);
        return box;
    }

    private Button quickCommand(PortableServerManager.Profile profile, String command) {
        Button button = new Button(command);
        button.getStyleClass().addAll("btn-secondary", "btn-sm");
        button.setOnAction(e -> {
            PortableServerManager.RunningServer handle = running.get(profile.id());
            if (handle == null || !handle.isAlive()) {
                Toast.warn(I18n.t("server.console.notRunning", "这个服务端没有在运行"));
                return;
            }
            try {
                handle.send(command);
                appendLog(profile.id(), "> " + command);
            } catch (Exception ex) {
                Toast.error(I18n.t("server.console.failed", "发送失败：") + ex.getMessage());
            }
        });
        return button;
    }

    // ==================== 配置 ====================

    private ScrollPane configPane(PortableServerManager.Profile profile) {
        File dir = new File(profile.dir());
        Map<String, String> props = ServerFiles.readProperties(dir);
        TextField port = new TextField(props.getOrDefault("server-port", String.valueOf(profile.port())));
        TextField motd = new TextField(props.getOrDefault("motd", "Neko Craft Server"));
        TextField maxPlayers = new TextField(props.getOrDefault("max-players", "20"));
        TextField viewDistance = new TextField(props.getOrDefault("view-distance", "8"));
        CheckBox online = new CheckBox(I18n.t("server.form.online", "正版验证 (online-mode)"));
        online.setSelected(!"false".equalsIgnoreCase(props.getOrDefault("online-mode", "true")));
        ComboBox<String> difficulty = new ComboBox<>();
        difficulty.getItems().addAll("peaceful", "easy", "normal", "hard");
        // 1.12 的 difficulty 存数字（0-3），1.13+ 允许名字：显示统一成名字，写回时还原数字
        String rawDifficulty = props.getOrDefault("difficulty", "1").trim();
        String[] difficultyNames = {"peaceful", "easy", "normal", "hard"};
        String difficultyName = rawDifficulty.matches("\\d+")
                ? difficultyNames[Math.min(3, Math.max(0, Integer.parseInt(rawDifficulty)))]
                : rawDifficulty.toLowerCase(java.util.Locale.ROOT);
        difficulty.setValue(difficultyName);

        port.setPrefColumnCount(6);
        maxPlayers.setPrefColumnCount(4);
        viewDistance.setPrefColumnCount(4);
        motd.setPrefColumnCount(22);

        Button save = new Button(I18n.t("server.panel.save", "保存配置"));
        save.getStyleClass().addAll("btn-primary", "btn-sm");
        Label result = new Label();
        result.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11px;");
        save.setOnAction(e -> {
            try {
                int newPort = Integer.parseInt(port.getText().trim());
                Map<String, String> updates = new LinkedHashMap<>();
                updates.put("server-port", String.valueOf(newPort));
                updates.put("motd", motd.getText() == null ? "" : motd.getText().trim());
                updates.put("max-players", maxPlayers.getText().trim());
                updates.put("view-distance", viewDistance.getText().trim());
                updates.put("online-mode", String.valueOf(online.isSelected()));
                int difficultyIndex = java.util.List.of("peaceful", "easy", "normal", "hard")
                        .indexOf(difficulty.getValue());
                updates.put("difficulty", String.valueOf(Math.max(0, difficultyIndex)));
                ServerFiles.writeProperties(dir, updates);
                result.setText(I18n.t("server.panel.saved",
                        "已保存，重启服务端后生效（端口变更需要重新映射）"));
                Toast.ok(I18n.t("server.panel.savedShort", "配置已保存"));
            } catch (Exception ex) {
                result.setText(I18n.t("server.panel.saveFailed", "保存失败：") + ex.getMessage());
            }
        });

        VBox form = new VBox(10,
                sectionLabel(I18n.t("server.panel.configTitle", "server.properties（常用项）")),
                row(I18n.t("server.form.port", "端口"), port),
                row("MOTD", motd),
                row(I18n.t("server.panel.maxPlayers", "最大人数"), maxPlayers),
                row(I18n.t("server.panel.viewDistance", "视距"), viewDistance),
                row(I18n.t("server.panel.difficulty", "难度"), difficulty),
                online,
                new HBox(10, save, result),
                new Label(I18n.t("server.panel.configHint",
                        "只改这里列出的键，其余键保持原样；改完点保存，再回控制台重启。")));
        form.setPadding(new Insets(12));
        return scroll(form);
    }

    // ==================== 玩家 ====================

    private VBox playersPane(PortableServerManager.Profile profile) {
        File dir = new File(profile.dir());
        VBox lists = new VBox(10);
        Runnable[] reload = new Runnable[1];
        reload[0] = () -> lists.getChildren().setAll(
                playerSection(I18n.t("server.panel.ops", "OP 列表"), ServerFiles.readOps(dir), true,
                        dir, true, reload[0]),
                playerSection(I18n.t("server.panel.whitelist", "白名单"),
                        ServerFiles.readWhitelist(dir), false, dir, false, reload[0]),
                playerSection(I18n.t("server.panel.banned", "封禁列表"),
                        ServerFiles.readBanned(dir), false, dir, false, reload[0]));
        reload[0].run();

        TextField name = new TextField();
        name.setPromptText(I18n.t("server.panel.playerName", "玩家名"));
        name.setPrefColumnCount(14);
        Button addOp = new Button(I18n.t("server.panel.addOp", "加为 OP"));
        addOp.getStyleClass().addAll("btn-secondary", "btn-sm");
        addOp.setOnAction(e -> {
            String player = name.getText() == null ? "" : name.getText().trim();
            if (player.isEmpty()) {
                return;
            }
            try {
                ServerFiles.addOp(dir, player, "");
                Toast.ok(I18n.t("server.panel.opAdded", "已写入 ops.json，进服或 /reload 后生效"));
                name.clear();
                reload[0].run();
            } catch (Exception ex) {
                Toast.error(I18n.t("server.panel.saveFailed", "保存失败：") + ex.getMessage());
            }
        });
        Button addWhite = new Button(I18n.t("server.panel.addWhitelist", "加入白名单"));
        addWhite.getStyleClass().addAll("btn-secondary", "btn-sm");
        addWhite.setOnAction(e -> {
            String player = name.getText() == null ? "" : name.getText().trim();
            if (player.isEmpty()) {
                return;
            }
            try {
                ServerFiles.addWhitelist(dir, player, "");
                Toast.ok(I18n.t("server.panel.whitelistAdded", "已写入 whitelist.json"));
                name.clear();
                reload[0].run();
            } catch (Exception ex) {
                Toast.error(I18n.t("server.panel.saveFailed", "保存失败：") + ex.getMessage());
            }
        });
        Button refresh = new Button(I18n.t("server.panel.refresh", "刷新"));
        refresh.getStyleClass().addAll("btn-ghost", "btn-sm");
        refresh.setOnAction(e -> reload[0].run());

        VBox box = new VBox(10, lists, new HBox(8, name, addOp, addWhite, refresh));
        box.setPadding(new Insets(12));
        return box;
    }

    private VBox playerSection(String title, List<ServerFiles.PlayerEntry> entries, boolean withLevel,
                               File dir, boolean ops, Runnable onChange) {
        Label head = new Label(title + " (" + entries.size() + ")");
        head.setStyle("-fx-text-fill: -app-text-2; -fx-font-size: 11.5px; -fx-font-weight: bold;");
        VBox box = new VBox(4, head);
        if (entries.isEmpty()) {
            Label empty = new Label("—");
            empty.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11px;");
            box.getChildren().add(empty);
        }
        for (ServerFiles.PlayerEntry entry : entries) {
            String text = entry.name() + (withLevel && !entry.level().isEmpty() ? "  · 等级 " + entry.level() : "")
                    + (entry.reason().isEmpty() ? "" : "  ·  " + entry.reason());
            Label label = new Label(text);
            label.setStyle("-fx-text-fill: -app-text-2; -fx-font-size: 11px;");
            box.getChildren().add(label);
        }
        return box;
    }

    // ==================== 信息 ====================

    private ScrollPane infoPane(PortableServerManager.Profile profile) {
        Label upnpHint = new Label();
        upnpHint.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11px;");
        upnpHint.setWrapText(true);
        Label reachHint = new Label();
        reachHint.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11px;");
        reachHint.setWrapText(true);

        Button upnp = new Button(I18n.t("server.upnp.map", "自动映射端口（UPnP）"));
        upnp.getStyleClass().addAll("btn-secondary", "btn-sm");
        upnp.setOnAction(e -> {
            upnp.setDisable(true);
            upnpHint.setText(I18n.t("server.upnp.working", "正在通过 UPnP 向路由器申请端口映射…"));
            CompletableFuture.supplyAsync(() -> UpnpPortMapper.map(profile.port(), "Neko Craft Launcher"))
                    .thenAccept(result -> Platform.runLater(() -> {
                        upnp.setDisable(false);
                        upnpHint.setText(result.ok()
                                ? I18n.t("server.upnp.ok", "已自动映射 TCP ") + profile.port()
                                : I18n.t("server.upnp.failed", "自动映射没成功：") + result.message());
                    }));
        });
        Button unmap = new Button(I18n.t("server.upnp.unmap", "取消映射"));
        unmap.getStyleClass().addAll("btn-secondary", "btn-sm");
        unmap.setOnAction(e -> CompletableFuture.supplyAsync(() -> UpnpPortMapper.unmap(profile.port()))
                .thenAccept(result -> Platform.runLater(() -> upnpHint.setText(result.message()))));

        Button reach = new Button(I18n.t("server.reach.check", "检测外网是否可连"));
        reach.getStyleClass().addAll("btn-secondary", "btn-sm");
        reach.setOnAction(e -> {
            reach.setDisable(true);
            String host = publicAddress(profile).split(":")[0];
            reachHint.setText(I18n.t("server.reach.checking", "正在请外部节点连一次 ") + host + " …");
            CompletableFuture.supplyAsync(() -> PortReachability.check(host, profile.port()))
                    .thenAccept(result -> Platform.runLater(() -> {
                        reach.setDisable(false);
                        reachHint.setText(result.reason());
                        reachHint.setStyle("-fx-font-size: 11px; -fx-text-fill: "
                                + (result.reachable() ? "#22c55e"
                                : (result.conclusive() ? "#ef4444" : "#f59e0b")) + ";");
                    }));
        });
        Button firewall = new Button(I18n.t("server.firewall", "放行防火墙端口"));
        firewall.getStyleClass().addAll("btn-secondary", "btn-sm");
        firewall.setOnAction(e -> CompletableFuture.runAsync(() ->
                        PortableServerManager.allowFirewall(profile.port()))
                .thenRun(() -> Platform.runLater(() ->
                        Toast.ok(I18n.t("server.firewall.ok", "已放行 TCP ") + profile.port()))));

        VBox info = new VBox(6,
                kv(I18n.t("server.panel.loader", "加载器"), profile.loader()),
                kv(I18n.t("server.panel.mcVersion", "游戏版本"), profile.mcVersion()),
                kv(I18n.t("server.form.port", "端口"), String.valueOf(profile.port())),
                kv(I18n.t("server.form.memory", "最大内存"), profile.memoryMb() + " MB"),
                kv(I18n.t("server.panel.dir", "目录"), profile.dir()),
                kv(I18n.t("server.panel.lastStarted", "最后启动"), profile.lastStarted() <= 0 ? "—"
                        : new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm")
                        .format(new java.util.Date(profile.lastStarted()))));

        VBox box = new VBox(12,
                info,
                sectionLabel(I18n.t("server.panel.network", "外网访问")),
                new HBox(8, upnp, unmap, reach, firewall),
                upnpHint, reachHint,
                scheduleSection(profile),
                backupSection(profile));
        box.setPadding(new Insets(12));
        return scroll(box);
    }

    private HBox kv(String key, String value) {
        Label k = new Label(key);
        k.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11.5px;");
        k.setMinWidth(84);
        Label v = new Label(value);
        v.setStyle("-fx-text-fill: -app-text-2; -fx-font-size: 11.5px;");
        v.setWrapText(true);
        return new HBox(10, k, v);
    }

    // ==================== 新建 ====================

    private void showCreateForm() {
        if (creating) {
            return;
        }
        TextField port = new TextField("25565");
        TextField memory = new TextField("4096");
        TextField motd = new TextField("Neko Craft Server");
        CheckBox online = new CheckBox(I18n.t("server.form.online", "正版验证 (online-mode)"));
        ProgressBar progress = new ProgressBar(0);
        progress.setPrefWidth(240);
        Label step = new Label();
        step.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11px;");
        port.setPrefColumnCount(6);
        memory.setPrefColumnCount(6);

        Button run = new Button(I18n.t("server.create.run", "创建便携服务端"));
        run.getStyleClass().addAll("btn-primary", "btn-sm");
        Button cancel = new Button(I18n.t("server.panel.cancel", "取消"));
        cancel.getStyleClass().addAll("btn-ghost", "btn-sm");
        cancel.setOnAction(e -> {
            content.getChildren().setAll(placeholder());
            if (selectedId != null) {
                PortableServerManager.load(gameDir).stream()
                        .filter(p -> p.id().equals(selectedId))
                        .findFirst().ifPresent(this::select);
            }
        });
        run.setOnAction(e -> {
            String instanceId = config.getLastInstance();
            if (instanceId == null || instanceId.isBlank()) {
                Toast.warn(I18n.t("server.error.instance", "请先在启动页选一个实例"));
                return;
            }
            int portValue;
            int memoryValue;
            try {
                portValue = Integer.parseInt(port.getText().trim());
                memoryValue = Math.max(512, Integer.parseInt(memory.getText().trim()));
            } catch (NumberFormatException ex) {
                Toast.error(I18n.t("server.error.number", "端口或内存不是数字"));
                return;
            }
            creating = true;
            run.setDisable(true);
            boolean isolation = com.example.demo.core.model.InstanceSettings.effectiveIsolation(
                    gameDir, instanceId, config.isIsolation());
            CompletableFuture.runAsync(() -> {
                try {
                    PortableServerManager.Profile created = PortableServerManager.create(
                            gameDir, instanceId, isolation, portValue, memoryValue,
                            online.isSelected(), motd.getText(),
                            p -> Platform.runLater(() -> {
                                progress.setProgress(p.percent());
                                step.setText(p.step() + " " + Math.round(p.percent() * 100) + "%");
                            }));
                    Platform.runLater(() -> {
                        creating = false;
                        run.setDisable(false);
                        Toast.ok(I18n.t("server.create.ok", "服务端已就绪，可以点「启动」"));
                        selectedId = created.id();
                        refreshList();
                        select(created);
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        creating = false;
                        run.setDisable(false);
                        step.setText(I18n.t("server.create.failed", "创建失败：") + ex.getMessage());
                    });
                }
            });
        });

        VBox form = new VBox(10,
                new Label(I18n.t("server.create.title", "从当前实例创建")),
                row(I18n.t("server.form.port", "端口"), port),
                row(I18n.t("server.form.memory", "最大内存 (MB)"), memory),
                row("MOTD", motd),
                online,
                new HBox(10, run, cancel),
                new HBox(10, progress, step));
        form.setPadding(new Insets(12));
        VBox card = new VBox(form);
        card.getStyleClass().add("neko-card");
        card.setMaxWidth(560);
        content.getChildren().setAll(card);
    }

    // ==================== 运行控制 ====================

    private void startServer(PortableServerManager.Profile profile) {
        try {
            appendLog(profile.id(), "=== " + profile.id() + " · " + profile.loader() + " "
                    + profile.mcVersion() + " · port " + profile.port() + " ===");
            PortableServerManager.RunningServer handle = PortableServerManager.start(profile,
                    line -> Platform.runLater(() -> appendLog(profile.id(), line)));
            running.put(profile.id(), handle);
            startSampling(profile);
            ensureScheduler();
            updatePillAfterSelect(profile);
            handle.process().onExit().thenAccept(p -> Platform.runLater(() -> {
                running.remove(profile.id());
                if (running.isEmpty()) {
                    stopSampling();
                }
                appendLog(profile.id(), I18n.t("server.status.exited", "[服务端已退出] code=") + p.exitValue());
                updatePillAfterSelect(profile);
            }));
        } catch (Exception e) {
            appendLog(profile.id(), I18n.t("server.error.startFailed", "[启动失败] ") + e.getMessage());
            Toast.error(I18n.t("server.error.startFailedToast", "启动失败：") + e.getMessage());
        }
    }

    private void stopServer(String id) {
        PortableServerManager.RunningServer handle = running.get(id);
        if (handle == null) {
            return;
        }
        appendLog(id, I18n.t("server.log.stopping", "[正在停止服务端…]"));
        handle.stop();
        CompletableFuture.delayedExecutor(10, java.util.concurrent.TimeUnit.SECONDS)
                .execute(() -> {
                    if (handle.isAlive()) {
                        handle.process().destroyForcibly();
                    }
                });
    }

    private void restartServer(PortableServerManager.Profile profile) {
        PortableServerManager.RunningServer handle = running.get(profile.id());
        if (handle == null || !handle.isAlive()) {
            startServer(profile);
            return;
        }
        appendLog(profile.id(), I18n.t("server.panel.restarting", "[重启中…]"));
        handle.process().onExit().thenRun(() ->
                Platform.runLater(() -> startServer(profile)));
        handle.stop();
    }

    private void updatePillAfterSelect(PortableServerManager.Profile profile) {
        updatePill(profile.id());
        refreshList();
        if (profile.id().equals(selectedId)) {
            content.getChildren().setAll(detail(profile));
        }
    }

    private void sendCommand(PortableServerManager.Profile profile) {
        PortableServerManager.RunningServer handle = running.get(profile.id());
        if (handle == null || !handle.isAlive()) {
            Toast.warn(I18n.t("server.console.notRunning", "这个服务端没有在运行"));
            return;
        }
        String command = commandField.getText();
        if (command == null || command.isBlank()) {
            return;
        }
        try {
            handle.send(command);
            appendLog(profile.id(), "> " + command.trim());
            commandField.clear();
        } catch (Exception e) {
            Toast.error(I18n.t("server.console.failed", "发送失败：") + e.getMessage());
        }
    }

    // ==================== 日志 ====================

    private void appendLog(String id, String line) {
        if (line == null) {
            return;
        }
        StringBuilder buffer = logs.computeIfAbsent(id, key -> new StringBuilder());
        buffer.append(line).append('\n');
        if (buffer.length() > 400_000) {
            buffer.delete(0, buffer.length() - 300_000);
        }
        if (id.equals(selectedId)) {
            console.appendText(line + System.lineSeparator());
            if (console.getText().length() > 400_000) {
                String text = console.getText();
                console.setText(text.substring(text.length() - 300_000));
            }
            console.positionCaret(console.getLength());
        }
    }

    // ==================== 文件 ====================

    private VBox filesPane(PortableServerManager.Profile profile) {
        File root = new File(profile.dir());
        Label pathLabel = new Label(root.getAbsolutePath());
        pathLabel.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11px;");
        VBox entries = new VBox(4);
        TextArea editor = new TextArea();
        editor.setVisible(false);
        editor.setManaged(false);
        editor.setPrefRowCount(10);
        editor.setStyle("-fx-font-family: 'Consolas','Microsoft YaHei',monospace; -fx-font-size: 11.5px;");
        Label editorPath = new Label();
        editorPath.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 10.5px;");
        editorPath.setVisible(false);
        editorPath.setManaged(false);

        final File[] current = {root};
        Runnable[] reload = new Runnable[1];
        reload[0] = () -> {
            entries.getChildren().clear();
            pathLabel.setText(current[0].getAbsolutePath());
            File[] files = current[0].listFiles();
            if (files == null) {
                return;
            }
            java.util.Arrays.sort(files, (a, b) -> {
                if (a.isDirectory() != b.isDirectory()) {
                    return a.isDirectory() ? -1 : 1;
                }
                return a.getName().compareToIgnoreCase(b.getName());
            });
            for (File file : files) {
                Label name = new Label((file.isDirectory() ? "▸ " : "· ") + file.getName());
                name.setStyle("-fx-text-fill: -app-text-2; -fx-font-size: 11.5px;");
                Label size = new Label(file.isDirectory() ? ""
                        : humanSize(file.length()));
                size.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 10.5px;");
                Region gap = new Region();
                HBox.setHgrow(gap, Priority.ALWAYS);
                HBox row = new HBox(8, name, gap, size);
                row.setPadding(new Insets(3, 6, 3, 6));
                row.setOnMouseClicked(e -> {
                    if (file.isDirectory()) {
                        current[0] = file;
                        reload[0].run();
                    } else if (file.length() <= 512 * 1024) {
                        try {
                            editor.setText(java.nio.file.Files.readString(file.toPath(),
                                    java.nio.charset.StandardCharsets.UTF_8));
                            editorPath.setText(file.getAbsolutePath());
                            editor.setVisible(true);
                            editor.setManaged(true);
                            editorPath.setVisible(true);
                            editorPath.setManaged(true);
                        } catch (Exception ex) {
                            Toast.error(I18n.t("server.panel.fileReadFailed", "读取失败：") + ex.getMessage());
                        }
                    } else {
                        openDir(file.getParentFile());
                    }
                });
                entries.getChildren().add(row);
            }
        };
        reload[0].run();

        Button up = new Button(I18n.t("server.panel.up", "上级目录"));
        up.getStyleClass().addAll("btn-secondary", "btn-sm");
        up.setOnAction(e -> {
            File parent = current[0].getParentFile();
            if (parent != null && parent.getAbsolutePath().startsWith(root.getAbsolutePath())) {
                current[0] = parent;
                reload[0].run();
            }
        });
        Button refresh = new Button(I18n.t("server.panel.refresh", "刷新"));
        refresh.getStyleClass().addAll("btn-ghost", "btn-sm");
        refresh.setOnAction(e -> reload[0].run());
        Button open = new Button(I18n.t("server.row.open", "打开目录"));
        open.getStyleClass().addAll("btn-ghost", "btn-sm");
        open.setOnAction(e -> openDir(current[0]));
        Button save = new Button(I18n.t("server.panel.fileSave", "保存文件"));
        save.getStyleClass().addAll("btn-primary", "btn-sm");
        save.setVisible(false);
        save.setManaged(false);
        save.setOnAction(e -> {
            if (editorPath.getText() == null || editorPath.getText().isBlank()) {
                return;
            }
            try {
                java.nio.file.Files.writeString(java.nio.file.Path.of(editorPath.getText()),
                        editor.getText(), java.nio.charset.StandardCharsets.UTF_8);
                Toast.ok(I18n.t("server.panel.fileSaved", "文件已保存"));
            } catch (Exception ex) {
                Toast.error(I18n.t("server.panel.saveFailed", "保存失败：") + ex.getMessage());
            }
        });

        ScrollPane scroll = new ScrollPane(entries);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(200);
        VBox box = new VBox(8, new HBox(8, up, refresh, open), pathLabel, scroll,
                editorPath, editor, save);
        box.setPadding(new Insets(12));
        VBox.setVgrow(editor, Priority.ALWAYS);
        return box;
    }

    private static String humanSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return String.format(java.util.Locale.ROOT, "%.1f KB", bytes / 1024.0);
        }
        return String.format(java.util.Locale.ROOT, "%.1f MB", bytes / 1024.0 / 1024.0);
    }

    // ==================== 资源曲线 ====================

    private void startSampling(PortableServerManager.Profile profile) {
        stopSampling();
        samplerTimer = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(2), e -> sampleOnce(profile)));
        samplerTimer.setCycleCount(javafx.animation.Animation.INDEFINITE);
        samplerTimer.play();
        sampleOnce(profile);
    }

    private void stopSampling() {
        if (samplerTimer != null) {
            samplerTimer.stop();
            samplerTimer = null;
        }
    }

    private void sampleOnce(PortableServerManager.Profile profile) {
        PortableServerManager.RunningServer handle = running.get(profile.id());
        if (handle == null || !handle.isAlive()) {
            return;
        }
        var cpuSampler = samplers.computeIfAbsent(profile.id(),
                key -> new com.example.demo.core.server.ServerMetrics.CpuSampler());
        double cpu = cpuSampler.sample(handle.process());
        long mem = profile.id().equals(selectedId) ? com.example.demo.core.server.ServerMetrics.memoryMb(handle.pid()) : -1;
        if (profile.id().equals(selectedId)) {
            push(cpuHistory, cpu);
            if (mem > 0) {
                push(memHistory, (double) mem);
            }
            drawChart();
        }
    }

    private void push(java.util.Deque<Double> deque, double value) {
        deque.addLast(value);
        while (deque.size() > 60) {
            deque.removeFirst();
        }
    }

    private void drawChart() {
        var g = chart.getGraphicsContext2D();
        double w = chart.getWidth();
        double h = chart.getHeight();
        g.clearRect(0, 0, w, h);
        g.setFill(javafx.scene.paint.Color.web("#0b1416"));
        g.fillRoundRect(0, 0, w, h, 8, 8);
        drawLine(g, cpuHistory, 100, javafx.scene.paint.Color.web("#5eead4"), w, h);
        drawLine(g, memHistory, Math.max(512, maxOf(memHistory)), javafx.scene.paint.Color.web("#f59e0b"), w, h);
        g.setFill(javafx.scene.paint.Color.web("#94a3b8"));
        g.setFont(javafx.scene.text.Font.font("Consolas", 9.5));
        g.fillText("CPU " + Math.round(lastOf(cpuHistory)) + "%   MEM " + Math.round(lastOf(memHistory)) + " MB", 8, 12);
    }

    private static double maxOf(java.util.Deque<Double> deque) {
        double max = 1;
        for (double value : deque) {
            max = Math.max(max, value);
        }
        return max;
    }

    private static double lastOf(java.util.Deque<Double> deque) {
        return deque.isEmpty() ? 0 : deque.peekLast();
    }

    private static void drawLine(javafx.scene.canvas.GraphicsContext g, java.util.Deque<Double> values,
                                 double scale, javafx.scene.paint.Color color, double w, double h) {
        if (values.size() < 2) {
            return;
        }
        g.setStroke(color);
        g.setLineWidth(1.4);
        double step = w / 59.0;
        int index = 0;
        g.beginPath();
        for (double value : values) {
            double x = w - (values.size() - 1 - index) * step;
            double y = h - 4 - Math.min(1, value / scale) * (h - 18);
            if (index == 0) {
                g.moveTo(x, y);
            } else {
                g.lineTo(x, y);
            }
            index++;
        }
        g.stroke();
    }

    // ==================== 定时任务 ====================

    private VBox scheduleSection(PortableServerManager.Profile profile) {
        var schedule = com.example.demo.core.server.ServerMetrics.readSchedule(new File(profile.dir()));
        TextField restart = new TextField(String.valueOf(schedule.restartMinutes()));
        TextField backup = new TextField(String.valueOf(schedule.backupMinutes()));
        restart.setPrefColumnCount(5);
        backup.setPrefColumnCount(5);
        Label hint = new Label();
        hint.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 10.5px;");
        hint.setText(I18n.t("server.panel.scheduleHint", "单位分钟，0 = 关闭；备份写到 backups/，保留最近 10 份"));

        Button save = new Button(I18n.t("server.panel.saveSchedule", "保存计划"));
        save.getStyleClass().addAll("btn-secondary", "btn-sm");
        save.setOnAction(e -> {
            try {
                int r = Integer.parseInt(restart.getText().trim());
                int b = Integer.parseInt(backup.getText().trim());
                com.example.demo.core.server.ServerMetrics.writeSchedule(new File(profile.dir()),
                        new com.example.demo.core.server.ServerMetrics.Schedule(r, b));
                ensureScheduler();
                Toast.ok(I18n.t("server.panel.scheduleSaved", "定时任务已保存"));
            } catch (NumberFormatException ex) {
                Toast.error(I18n.t("server.error.number", "端口或内存不是数字"));
            }
        });

        return new VBox(8,
                sectionLabel(I18n.t("server.panel.schedule", "定时任务")),
                row(I18n.t("server.panel.everyRestart", "每 N 分钟重启"), restart),
                row(I18n.t("server.panel.everyBackup", "每 N 分钟备份"), backup),
                new HBox(8, save),
                hint);
    }

    private void ensureScheduler() {
        if (scheduleTimer != null) {
            return;
        }
        scheduleTimer = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(30), e -> runSchedules()));
        scheduleTimer.setCycleCount(javafx.animation.Animation.INDEFINITE);
        scheduleTimer.play();
    }

    private void runSchedules() {
        for (var entry : java.util.List.copyOf(running.entrySet())) {
            String id = entry.getKey();
            PortableServerManager.Profile profile = PortableServerManager.load(gameDir).stream()
                    .filter(p -> p.id().equals(id)).findFirst().orElse(null);
            if (profile == null) {
                continue;
            }
            var schedule = com.example.demo.core.server.ServerMetrics.readSchedule(new File(profile.dir()));
            long now = System.currentTimeMillis();
            if (schedule.backupMinutes() > 0) {
                long last = lastBackupAt.getOrDefault(id, now);
                if (now - last >= schedule.backupMinutes() * 60_000L) {
                    lastBackupAt.put(id, now);
                    java.util.concurrent.CompletableFuture.runAsync(() -> {
                        try {
                            File zip = com.example.demo.core.server.ServerMetrics.backupWorld(new File(profile.dir()));
                            Platform.runLater(() -> appendLog(id,
                                    I18n.t("server.panel.backupDone", "[自动备份] ") + zip.getName()));
                        } catch (Exception ex) {
                            Platform.runLater(() -> appendLog(id,
                                    I18n.t("server.panel.backupFailed", "[备份失败] ") + ex.getMessage()));
                        }
                    });
                }
            }
            if (schedule.restartMinutes() > 0) {
                long last = lastRestartAt.getOrDefault(id, now);
                if (now - last >= schedule.restartMinutes() * 60_000L) {
                    lastRestartAt.put(id, now);
                    appendLog(id, I18n.t("server.panel.autoRestart", "[按计划重启]"));
                    restartServer(profile);
                }
            }
        }
    }
    // ==================== 备份 ====================

    private VBox backupSection(PortableServerManager.Profile profile) {
        File dir = new File(profile.dir());
        VBox list = new VBox(4);
        Label empty = new Label(I18n.t("server.panel.backupEmpty", "还没有备份"));
        empty.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11px;");

        Runnable[] reload = new Runnable[1];
        reload[0] = () -> {
            list.getChildren().clear();
            var backups = com.example.demo.core.server.ServerMetrics.listBackups(dir);
            if (backups.isEmpty()) {
                list.getChildren().add(empty);
                return;
            }
            for (File zip : backups) {
                Label name = new Label(zip.getName());
                name.setStyle("-fx-text-fill: -app-text-2; -fx-font-size: 11px;");
                Label meta = new Label(humanSize(zip.length()) + " · "
                        + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm")
                        .format(new java.util.Date(zip.lastModified())));
                meta.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 10.5px;");
                Button restore = new Button(I18n.t("server.panel.backupRestore", "恢复"));
                restore.getStyleClass().addAll("btn-secondary", "btn-sm");
                restore.setOnAction(e -> {
                    if (running.containsKey(profile.id())) {
                        Toast.warn(I18n.t("server.panel.stopFirst", "先停止再移除"));
                        return;
                    }
                    try {
                        com.example.demo.core.server.ServerMetrics.restoreBackup(dir, zip);
                        Toast.ok(I18n.t("server.panel.backupRestored", "已恢复到 ") + zip.getName());
                    } catch (Exception ex) {
                        Toast.error(I18n.t("server.panel.backupRestoreFailed", "恢复失败：") + ex.getMessage());
                    }
                });
                Button delete = new Button(I18n.t("server.panel.backupDelete", "删除"));
                delete.getStyleClass().addAll("btn-ghost", "btn-sm");
                delete.setOnAction(e -> {
                    //noinspection ResultOfMethodCallIgnored
                    zip.delete();
                    reload[0].run();
                });
                Region gap = new Region();
                HBox.setHgrow(gap, Priority.ALWAYS);
                HBox row = new HBox(8, new VBox(1, name, meta), gap, restore, delete);
                row.setAlignment(Pos.CENTER_LEFT);
                list.getChildren().add(row);
            }
        };
        reload[0].run();

        Button backupNow = new Button(I18n.t("server.panel.backupNow", "立即备份"));
        backupNow.getStyleClass().addAll("btn-secondary", "btn-sm");
        backupNow.setOnAction(e -> {
            backupNow.setDisable(true);
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                try {
                    File zip = com.example.demo.core.server.ServerMetrics.backupWorld(dir);
                    Platform.runLater(() -> {
                        backupNow.setDisable(false);
                        reload[0].run();
                        Toast.ok(I18n.t("server.panel.backupCreated", "已备份：") + zip.getName());
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        backupNow.setDisable(false);
                        Toast.error(I18n.t("server.panel.backupFailedToast", "备份失败：") + ex.getMessage());
                    });
                }
            });
        });

        return new VBox(8,
                sectionLabel(I18n.t("server.panel.backups", "备份")),
                new HBox(8, backupNow),
                list);
    }
    // ==================== 地址与小工具 ====================

    private String localAddress(PortableServerManager.Profile profile) {
        return PortableServerManager.localIp() + ":" + profile.port();
    }

    private String publicAddress(PortableServerManager.Profile profile) {
        String ip = PortableServerManager.publicIp();
        return (ip == null || ip.isBlank() ? I18n.t("server.ip.unknown", "未检测到") : ip)
                + ":" + profile.port();
    }

    private Button chip(String text, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().addAll("btn-ghost", "btn-sm");
        button.setOnAction(e -> action.run());
        return button;
    }

    private void copy(String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        Clipboard.getSystemClipboard().setContent(content);
        Toast.ok(I18n.t("server.copied", "已复制：") + text);
    }

    private static void openDir(File dir) {
        try {
            java.awt.Desktop.getDesktop().open(dir);
        } catch (Exception ignored) {
            // 打不开就算了
        }
    }

    private Label sectionLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: -app-text-2; -fx-font-size: 11.5px; -fx-font-weight: bold;");
        return label;
    }
    private HBox row(String label, javafx.scene.Node control) {
        Label name = new Label(label);
        name.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11.5px;");
        name.setMinWidth(110);
        HBox box = new HBox(10, name, control);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private ScrollPane scroll(javafx.scene.Node node) {
        ScrollPane pane = new ScrollPane(node);
        pane.setFitToWidth(true);
        pane.setStyle("-fx-background-color: transparent;");
        return pane;
    }
}
