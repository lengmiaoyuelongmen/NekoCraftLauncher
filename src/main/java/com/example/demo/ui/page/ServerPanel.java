package com.example.demo.ui.page;

import com.example.demo.core.app.LauncherConfig;
import com.example.demo.core.i18n.I18n;
import com.example.demo.core.server.PortableServerManager;
import com.example.demo.ui.shell.Motion;
import com.example.demo.ui.shell.Toast;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 便携服务器页：地址 / 历史记录 / 创建进度 / 独立日志。
 *
 * <p>为什么单独一页：服务端日志是持续刷屏的流，挤在启动器的运行日志里既看不清也互相覆盖。
 * 这里给它一块独立区域，主日志只保留启动器的信息。
 */
public final class ServerPanel {

    private final VBox root = new VBox(12);
    private final LauncherConfig config;
    private final File gameDir;

    private final Label publicLabel = new Label("检测中…");
    private final Label localLabel = new Label("—");
    private final Label natHint = new Label("");
    private final Label statusLabel = new Label(I18n.t("server.status.idle", "当前没有运行中的服务端"));
    private final VBox listBox = new VBox(8);
    private final ProgressBar progress = new ProgressBar(0);
    private final Label progressLabel = new Label("");
    private final TextArea logArea = new TextArea();

    private final TextField portField = new TextField("25565");
    private final TextField memoryField = new TextField("4096");
    private final TextField motdField = new TextField("Neko Craft Server");
    private final CheckBox onlineBox = new CheckBox(I18n.t("server.form.online", "正版验证 (online-mode)"));

    private Process running;
    private String runningId;
    private int currentPort = 25565;

    public ServerPanel(LauncherConfig config, File gameDir) {
        this.config = config;
        this.gameDir = gameDir;
        build();
        refreshPublicIp();
        refreshList();
    }

    public VBox node() {
        return root;
    }

    // ==================== 布局 ====================

    private void build() {
        root.setPadding(new Insets(4, 2, 4, 2));
        root.getChildren().addAll(headerCard(), addressCard(), listCard(), createCard(), logCard());
        VBox.setVgrow(root.getChildren().get(4), Priority.ALWAYS);
    }

    private VBox headerCard() {
        Label title = new Label(I18n.t("server.title", "便携服务器"));
        title.setStyle("-fx-text-fill: -app-text; -fx-font-size: 16px; -fx-font-weight: bold;");
        Label sub = new Label(I18n.t("server.subtitle",
                "用当前实例生成独立服务端，日志在这里看，地址可以一键复制发给朋友"));
        sub.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11.5px;");
        statusLabel.setStyle("-fx-text-fill: -app-text-2; -fx-font-size: 11.5px;");
        VBox box = new VBox(4, title, sub, statusLabel);
        box.getStyleClass().add("neko-card");
        box.setPadding(new Insets(14));
        return box;
    }

    private VBox addressCard() {
        publicLabel.setStyle("-fx-text-fill: -app-text; -fx-font-size: 14px; -fx-font-weight: bold;");
        localLabel.setStyle("-fx-text-fill: -app-text; -fx-font-size: 14px; -fx-font-weight: bold;");
        natHint.setStyle("-fx-text-fill: #f59e0b; -fx-font-size: 11px;");
        natHint.setWrapText(true);

        Button refreshIp = small(I18n.t("server.ip.refresh", "重新检测公网 IP"), e -> refreshPublicIp());
        Button copyPublic = small(I18n.t("server.ip.copyPublic", "复制公网地址"),
                e -> copy(publicLabel.getText()));
        Button copyLocal = small(I18n.t("server.ip.copyLocal", "复制局域网地址"),
                e -> copy(localLabel.getText()));
        Button firewall = small(I18n.t("server.firewall", "放行防火墙端口"), e -> allowFirewall());

        VBox box = card(
                section(I18n.t("server.addr.title", "连接地址")),
                row(I18n.t("server.addr.public", "公网地址"), publicLabel, copyPublic, refreshIp),
                row(I18n.t("server.addr.local", "局域网地址"), localLabel, copyLocal, firewall),
                natHint);
        return box;
    }

    private VBox listCard() {
        VBox box = card(section(I18n.t("server.list.title", "开过的服务端")), listBox);
        return box;
    }

    private VBox createCard() {
        portField.setPrefColumnCount(6);
        memoryField.setPrefColumnCount(6);
        motdField.setPrefColumnCount(18);
        progress.setPrefWidth(260);

        Button create = new Button(I18n.t("server.create.run", "创建便携服务端"));
        create.getStyleClass().addAll("btn-primary", "btn-sm");
        create.setOnAction(e -> createServer(create));

        VBox box = card(
                section(I18n.t("server.create.title", "从当前实例创建")),
                new HBox(10, new Label(I18n.t("server.form.port", "端口")), portField,
                        new Label(I18n.t("server.form.memory", "最大内存 (MB)")), memoryField),
                new HBox(10, new Label("MOTD"), motdField, onlineBox),
                new HBox(10, create, progress, progressLabel));
        return box;
    }

    private VBox logCard() {
        logArea.setEditable(false);
        logArea.setWrapText(false);
        logArea.setStyle("-fx-font-family: 'Consolas','Microsoft YaHei',monospace; -fx-font-size: 11px;"
                + " -fx-control-inner-background: #0b1416; -fx-text-fill: #c7f9ee;");
        logArea.setPrefRowCount(12);
        VBox.setVgrow(logArea, Priority.ALWAYS);

        Button clear = small(I18n.t("server.log.clear", "清空日志"), e -> logArea.clear());
        Button copyLog = small(I18n.t("server.log.copy", "复制日志"),
                e -> copy(logArea.getText()));
        HBox actions = new HBox(8, clear, copyLog);
        actions.setAlignment(Pos.CENTER_RIGHT);

        VBox box = card(
                section(I18n.t("server.log.title", "服务端日志（独立）")),
                logArea,
                actions);
        VBox.setVgrow(box, Priority.ALWAYS);
        return box;
    }

    // ==================== 数据 ====================

    private void refreshList() {
        listBox.getChildren().clear();
        List<PortableServerManager.Profile> profiles = PortableServerManager.load(gameDir);
        if (profiles.isEmpty()) {
            Label empty = new Label(I18n.t("server.list.empty", "还没有创建过服务端，下面点「创建便携服务端」"));
            empty.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11.5px;");
            listBox.getChildren().add(empty);
            return;
        }
        for (PortableServerManager.Profile profile : profiles) {
            listBox.getChildren().add(profileRow(profile));
        }
        Motion.staggerRows(listBox.getChildren(), 24, 8);
    }

    private HBox profileRow(PortableServerManager.Profile profile) {
        Label name = new Label(profile.id());
        name.setStyle("-fx-text-fill: -app-text; -fx-font-size: 12.5px; -fx-font-weight: bold;");
        Label meta = new Label(profile.loader() + " " + profile.mcVersion() + " · "
                + I18n.t("server.row.port", "端口") + " " + profile.port());
        meta.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11px;");
        VBox text = new VBox(2, name, meta);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        boolean isRunning = running != null && running.isAlive() && profile.id().equals(runningId);
        Button start = small(isRunning ? I18n.t("server.row.stop", "停止") : I18n.t("server.row.start", "启动"),
                e -> {
                    if (running != null && running.isAlive() && profile.id().equals(runningId)) {
                        stopServer();
                    } else {
                        startServer(profile);
                    }
                });
        Button open = small(I18n.t("server.row.open", "打开目录"), e -> openDir(new File(profile.dir())));
        Button remove = small(I18n.t("server.row.remove", "移除记录"), e -> {
            PortableServerManager.forget(gameDir, profile.id());
            refreshList();
        });

        HBox row = new HBox(10, text, spacer, start, open, remove);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("neko-card");
        row.setPadding(new Insets(10, 12, 10, 12));
        return row;
    }

    // ==================== 动作 ====================

    private void createServer(Button trigger) {
        int port;
        int memory;
        try {
            port = Integer.parseInt(portField.getText().trim());
            memory = Math.max(512, Integer.parseInt(memoryField.getText().trim()));
        } catch (NumberFormatException e) {
            Toast.error(I18n.t("server.error.number", "端口或内存不是数字"));
            return;
        }
        String instanceId = config.getLastInstance();
        if (instanceId == null || instanceId.isBlank()) {
            Toast.warn(I18n.t("server.error.instance", "请先在启动页选一个实例"));
            return;
        }
        currentPort = port;
        trigger.setDisable(true);
        progress.setProgress(0);
        progressLabel.setText(I18n.t("server.create.starting", "准备中…"));
        String motd = motdField.getText();
        boolean online = onlineBox.isSelected();
        boolean isolation = com.example.demo.core.model.InstanceSettings.effectiveIsolation(
                gameDir, instanceId, config.isIsolation());

        CompletableFuture.runAsync(() -> {
            try {
                PortableServerManager.create(gameDir, instanceId, isolation, port, memory, online, motd,
                        p -> Platform.runLater(() -> {
                            progress.setProgress(p.percent());
                            progressLabel.setText(p.step() + " " + Math.round(p.percent() * 100) + "%");
                        }));
                Platform.runLater(() -> {
                    trigger.setDisable(false);
                    progressLabel.setText(I18n.t("server.create.done", "创建完成"));
                    Toast.ok(I18n.t("server.create.ok", "服务端已就绪，可以点「启动」"));
                    refreshList();
                    refreshAddresses();
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    trigger.setDisable(false);
                    progressLabel.setText(I18n.t("server.create.failed", "创建失败"));
                    appendLog(I18n.t("server.create.failedLog", "[创建失败] ") + ex.getMessage());
                    Toast.error(I18n.t("server.create.failedToast", "创建失败：") + ex.getMessage());
                });
            }
        });
    }

    private void startServer(PortableServerManager.Profile profile) {
        if (running != null && running.isAlive()) {
            Toast.warn(I18n.t("server.error.busy", "已有服务端在运行，先停止它"));
            return;
        }
        currentPort = profile.port();
        try {
            runningId = profile.id();
            appendLog("=== " + profile.id() + " · " + profile.loader() + " " + profile.mcVersion()
                    + " · port " + profile.port() + " ===");
            running = PortableServerManager.start(profile, line -> Platform.runLater(() -> appendLog(line)));
            statusLabel.setText(I18n.t("server.status.running", "运行中：") + profile.id()
                    + " · " + I18n.t("server.status.pid", "pid ") + running.pid());
            refreshList();
            running.onExit().thenAccept(p -> Platform.runLater(() -> {
                statusLabel.setText(I18n.t("server.status.stopped", "服务端已停止（exit ") + p.exitValue() + ")");
                appendLog(I18n.t("server.status.exited", "[服务端已退出] code=") + p.exitValue());
                running = null;
                runningId = null;
                refreshList();
            }));
        } catch (Exception e) {
            appendLog(I18n.t("server.error.startFailed", "[启动失败] ") + e.getMessage());
            Toast.error(I18n.t("server.error.startFailedToast", "启动失败：") + e.getMessage());
        }
    }

    private void stopServer() {
        if (running != null && running.isAlive()) {
            running.destroy();
            appendLog(I18n.t("server.log.stopping", "[正在停止服务端…]"));
        }
    }

    private void allowFirewall() {
        int port = currentPort;
        CompletableFuture.runAsync(() -> {
            String error = PortableServerManager.allowFirewall(port);
            Platform.runLater(() -> {
                if (error.isEmpty()) {
                    Toast.ok(I18n.t("server.firewall.ok", "已放行 TCP ") + port);
                } else {
                    Toast.warn(I18n.t("server.firewall.failed", "放行失败（需要管理员权限）：") + error);
                }
            });
        });
    }

    private void refreshPublicIp() {
        publicLabel.setText(I18n.t("server.ip.detecting", "检测中…"));
        CompletableFuture.supplyAsync(PortableServerManager::publicIp).thenAccept(ip ->
                Platform.runLater(() -> {
                    if (ip == null || ip.isBlank()) {
                        publicLabel.setText(I18n.t("server.ip.unknown", "未检测到（网络受限）"));
                        natHint.setText(I18n.t("server.ip.hintFailed",
                                "拿不到公网 IP：检查网络或代理；局域网内仍可用下面的地址。"));
                        return;
                    }
                    publicLabel.setText(ip + ":" + currentPort);
                    refreshAddresses();
                }));
    }

    private void refreshAddresses() {
        String local = PortableServerManager.localIp();
        localLabel.setText(local + ":" + currentPort);
        String publicText = publicLabel.getText();
        String publicIp = publicText == null ? "" : publicText.split(":")[0];
        if (publicIp.isEmpty() || publicIp.equals(local)) {
            natHint.setText(I18n.t("server.ip.hintSame",
                    "公网地址与局域网地址一致：本机可能直接持有公网 IP，朋友可直接连。"));
        } else {
            natHint.setText(I18n.t("server.ip.hintNat",
                    "公网 IP 与局域网 IP 不同：你在路由器后面。需要在路由器上把 TCP "
                            + currentPort + " 映射到本机 " + local + "，否则外网连不上。"));
        }
    }

    private void appendLog(String line) {
        if (line == null) {
            return;
        }
        logArea.appendText(line + System.lineSeparator());
        // 只保留最后 3000 行，避免长时间运行把内存吃满
        String text = logArea.getText();
        if (text.length() > 400_000) {
            logArea.setText(text.substring(text.length() - 300_000));
        }
        logArea.positionCaret(logArea.getLength());
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

    // ==================== 小工具 ====================

    private VBox card(javafx.scene.Node... children) {
        VBox box = new VBox(10, children);
        box.getStyleClass().add("neko-card");
        box.setPadding(new Insets(14));
        return box;
    }

    private Label section(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: -app-text-2; -fx-font-size: 12px; -fx-font-weight: bold;");
        return label;
    }

    private HBox row(String label, Label value, Button... buttons) {
        Label name = new Label(label);
        name.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11.5px;");
        name.setMinWidth(76);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox box = new HBox(10, name, value, spacer);
        box.getChildren().addAll(buttons);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private Button small(String text, javafx.event.EventHandler<javafx.event.ActionEvent> action) {
        Button button = new Button(text);
        button.getStyleClass().addAll("btn-secondary", "btn-sm");
        button.setOnAction(action);
        return button;
    }
}
