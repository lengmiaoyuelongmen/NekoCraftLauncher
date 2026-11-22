package com.example.demo.ui.component;

import com.example.demo.core.i18n.I18n;

import java.io.File;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import com.example.demo.core.model.InstanceId;
import com.example.demo.core.model.LaunchFormat;
import com.example.demo.core.model.LaunchHistory;
import com.example.demo.core.app.LauncherConfig;
import com.example.demo.ui.shell.Motion;
/**
 * 启动页在日志收起时显示的「启动概览」：启动统计、最近一次启动、启动记录、当前实例和快捷操作。
 *
 * <p>从 HelloApplication 里抽出来的独立组件，只依赖配置、当前实例和几个跳转回调，方便单独维护。
 */
public final class LaunchOverviewPane extends VBox {

    private static final Pattern MC_VERSION =
            Pattern.compile("^(1\\.\\d+(?:\\.\\d+)?|\\d+\\.\\d+(?:\\.\\d+)?)");

    private final LauncherConfig config;
    private final Supplier<String> currentInstance;

    private final Label statValue = new Label("—");
    private final Label envJava = new Label("—");
    private final Label envMemory = new Label("—");
    private final Label envDisk = new Label("—");
    private final Label envHead = new Label("—");
    private final Label statSub = caption("—");
    private final Label lastValue = new Label("—");
    private final Label totalValue = new Label("—");
    private final Label lastSub = caption("—");
    private final Label totalSub = caption("—");
    private final Label instanceValue = new Label("—");
    private final Label instanceSub = caption("—");
    private final VBox historyBox = new VBox(2.0);
    private Label instanceMem = new Label();
    private Label instanceDir = new Label();
    private Label instanceMods = new Label();
    private final javafx.scene.control.CheckBox onlyFailed = new javafx.scene.control.CheckBox(I18n.t("overview.onlyFailed", "只看失败"));
    private final Label lastReason = new Label();
    private final Button fixEnvButton = new Button(I18n.t("overview.env.fix", "一键修复"));
    private final HBox failActions = new HBox(8.0);

    public LaunchOverviewPane(
            LauncherConfig config,
            Supplier<String> currentInstance,
            Runnable openInstances,
            Runnable openInstanceSettings,
            Runnable openDownload,
            Runnable openAdvisor,
            Runnable openHealth,
            Runnable openSettings,
            Runnable openLogs) {
        super(12.0);
        this.config = config;
        this.currentInstance = currentInstance;

        Label title = new Label("启动概览");
        title.setStyle("-fx-text-fill: -app-text; -fx-font-size: 15px; -fx-font-weight: bold;");
        Label tip = new Label("日志已收起 · 点「展开日志」查看实时输出");
        tip.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 10.5px;");
        Region headSpacer = new Region();
        HBox.setHgrow(headSpacer, Priority.ALWAYS);
        HBox head = new HBox(10.0, title, headSpacer, tip);
        head.setAlignment(Pos.CENTER_LEFT);

        statValue.setStyle("-fx-text-fill: -app-text; -fx-font-size: 16px; -fx-font-weight: bold;");
        lastValue.setStyle("-fx-text-fill: -app-text; -fx-font-size: 16px; -fx-font-weight: bold;");
        instanceValue.setStyle(
                "-fx-text-fill: -app-text; -fx-font-size: 14px; -fx-font-weight: bold;");
        instanceValue.setTextOverrun(OverrunStyle.ELLIPSIS);
        instanceValue.setMaxWidth(Double.MAX_VALUE);

        // 第一张卡改成「运行环境」：Java / 内存 / 磁盘一眼看完，不健康就点进去改
        envJava.setStyle(envLineStyle());
        envMemory.setStyle(envLineStyle());
        envDisk.setStyle(envLineStyle());
        envHead.setStyle("-fx-text-fill: -app-text; -fx-font-size: 13px; -fx-font-weight: bold;");
        fixEnvButton.getStyleClass().addAll("btn-secondary", "btn-sm");
        fixEnvButton.setVisible(false);
        fixEnvButton.setManaged(false);
        fixEnvButton.setOnAction(e -> fixEnvironment());
        VBox statCard = card("运行环境", envHead, envJava, envMemory, envDisk, fixEnvButton);
        lastReason.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 11px;");
        lastReason.setWrapText(true);
        Button openLogsBtn = new Button(I18n.t("overview.openLogs", "日志"));
        openLogsBtn.getStyleClass().addAll("btn-ghost", "btn-sm");
        openLogsBtn.setOnAction(e -> openLogs.run());
        Button openCrash = new Button(I18n.t("overview.openCrash", "报告"));
        openCrash.getStyleClass().addAll("btn-ghost", "btn-sm");
        openCrash.setOnAction(e -> {
            try {
                File reports = new File(new File(config.getGameDir()), "crash-reports");
                if (!reports.isDirectory()) {
                    reports.mkdirs();
                }
                java.awt.Desktop.getDesktop().open(reports);
            } catch (Exception ignored) {
                // 打不开就忽略
            }
        });
        Button diagnose = new Button(I18n.t("overview.diagnose", "诊断"));
        diagnose.getStyleClass().addAll("btn-secondary", "btn-sm");
        diagnose.setOnAction(e -> showDiagnosis());
        failActions.getChildren().setAll(openLogsBtn, diagnose, openCrash);
        failActions.setVisible(false);
        failActions.setManaged(false);
        lastReason.setMaxWidth(210);
        VBox lastCard = card("最近一次启动", lastValue, lastSub, lastReason, failActions);
        totalValue.setStyle(
                "-fx-text-fill: -app-text; -fx-font-size: 16px; -fx-font-weight: bold;");
        VBox totalCard = card("累计游玩", totalValue, totalSub);
        makeCardClickable(statCard, openSettings);
        makeCardClickable(lastCard, openHealth);
        makeCardClickable(totalCard, openAdvisor);
        // 两张统计卡等宽撑满一行，右侧不再留空
        statCard.setMaxWidth(Double.MAX_VALUE);
        lastCard.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(statCard, Priority.ALWAYS);
        HBox.setHgrow(lastCard, Priority.ALWAYS);
        totalCard.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(totalCard, Priority.ALWAYS);
        HBox statRow = new HBox(12.0, statCard, lastCard, totalCard);
        statRow.setFillHeight(true);
        statRow.setMinHeight(140.0);
        statRow.setPrefHeight(140.0);
        statRow.setMaxHeight(140.0);

        HBox historyHead =
                new HBox(
                        10.0,
                        headCell("状态", 40.0, false),
                        headCell("时间", 78.0, false),
                        headCell("实例", 0.0, false),
                        headCell("耗时", 74.0, true));
        historyHead.setPadding(new Insets(0.0, 2.0, 2.0, 2.0));
        Button seeAll = new Button("查看全部记录 →");
        seeAll.getStyleClass().add("btn-ghost");
        seeAll.setStyle("-fx-background-color: -app-row3; -fx-text-fill: -app-text-2;"
                + " -fx-background-radius: 8; -fx-border-radius: 8; -fx-padding: 5 12;"
                + " -fx-font-size: 11px; -fx-cursor: hand;");
        seeAll.setOnAction(e -> openAdvisor.run());
        onlyFailed.setStyle("-fx-text-fill: -app-text-dim-2; -fx-font-size: 10.5px;");
        onlyFailed.selectedProperty().addListener((o, a, b) -> refresh());
        Region footGap = new Region();
        HBox.setHgrow(footGap, Priority.ALWAYS);
        HBox historyFoot = new HBox(8.0, onlyFailed, footGap, seeAll);
        historyFoot.setAlignment(Pos.CENTER_RIGHT);
        VBox historyCard = card("最近启动记录", historyHead, historyBox, historyFoot);
        HBox.setHgrow(historyCard, Priority.ALWAYS);

        VBox instanceCard = card("当前实例", instanceValue, instanceSub);
        instanceMem = new Label();
        instanceMem.setStyle("-fx-text-fill: -app-text-dim-2; -fx-font-size: 11px;");
        instanceMem.setText(I18n.t("overview.instance.memory", "分配内存 ") + config.getMemoryMb() + " MB");
        instanceDir = new Label();
        instanceDir.setStyle("-fx-text-fill: -app-text-dim-2; -fx-font-size: 11px;");
        instanceDir.setText(I18n.t("overview.instance.dir", "目录 ") + config.getGameDir());
        instanceDir.setTextOverrun(OverrunStyle.ELLIPSIS);
        instanceDir.setMaxWidth(Double.MAX_VALUE);
        instanceMods = new Label();
        instanceMods.setStyle("-fx-text-fill: -app-text-dim-2; -fx-font-size: 11px;");
        Button openDirBtn = new Button(I18n.t("overview.openDir", "打开目录"));
        openDirBtn.getStyleClass().addAll("btn-ghost", "btn-sm");
        openDirBtn.setOnAction(e -> {
            try {
                java.awt.Desktop.getDesktop().open(new File(config.getGameDir()));
            } catch (Exception ignored) {
                // 打不开就算了
            }
        });
        instanceCard.getChildren().addAll(instanceMem, instanceMods, instanceDir, openDirBtn);

        GridPane quick = new GridPane();
        quick.setHgap(8.0);
        quick.setVgap(8.0);
        quick.add(quickButton("实例选择", openInstances), 0, 0);
        quick.add(quickButton("实例设置", openInstanceSettings), 1, 0);
        quick.add(quickButton("下载", openDownload), 0, 1);
        quick.add(quickButton("一键体检", openHealth), 1, 1);
        for (int i = 0; i < 2; i++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(50.0);
            quick.getColumnConstraints().add(column);
        }
        VBox quickCard = card("快捷操作", quick);
        VBox.setVgrow(instanceCard, Priority.ALWAYS);
        VBox.setVgrow(quickCard, Priority.NEVER);
        VBox sideColumn = new VBox(12.0, instanceCard, quickCard);
        sideColumn.setMinWidth(180.0);
        sideColumn.setPrefWidth(233.0);
        sideColumn.setMaxWidth(280.0);

        // 宽窗口：记录卡 + 右侧栏并排；窄窗口：侧栏折到记录卡下面，避免列被压成省略号
        VBox bodyWrap = new VBox(12.0);
        VBox.setVgrow(bodyWrap, Priority.ALWAYS);
        historyCard.setMinWidth(0.0);
        historyCard.setMaxWidth(Double.MAX_VALUE);
        sideColumn.setMinWidth(0.0);
        sideColumn.setMaxWidth(Double.MAX_VALUE);
        Runnable applyBodyLayout =
                () -> {
                  boolean stacked = getWidth() > 0 && getWidth() < 540.0;
                  if (stacked) {
                    bodyWrap.getChildren().setAll(historyCard, sideColumn);
                    VBox.setVgrow(historyCard, Priority.NEVER);
                    VBox.setVgrow(sideColumn, Priority.NEVER);
                    return;
                  }
                  HBox row = new HBox(12.0, historyCard, sideColumn);
                  row.setFillHeight(true);
                  HBox.setHgrow(historyCard, Priority.ALWAYS);
                  VBox.setVgrow(row, Priority.ALWAYS);
                  VBox.setVgrow(historyCard, Priority.ALWAYS);
                  VBox.setVgrow(sideColumn, Priority.ALWAYS);
                  bodyWrap.getChildren().setAll(row);
                };
        applyBodyLayout.run();
        widthProperty().addListener((observable, oldWidth, newWidth) -> applyBodyLayout.run());

        getChildren().addAll(head, statRow, bodyWrap);
        setPadding(new Insets(6.0, 4.0, 2.0, 4.0));
        setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(this, Priority.ALWAYS);
        refresh();
    }

    /** 重新读取启动历史并刷新所有数字。 */
    public void refresh() {
        File dir = new File(config.getGameDir());
        LaunchHistory.Summary summary = LaunchHistory.summary(dir);
        List<LaunchHistory.Entry> allEntries = LaunchHistory.recent(dir);
        List<LaunchHistory.Entry> entries = allEntries;
        if (onlyFailed.isSelected()) {
            entries = entries.stream().filter(e -> e.crash()).toList();
        }
        if (summary.total() == 0) {
            statValue.setText("还没有记录");
            statSub.setText("启动一次后会生成稳定性画像");
            lastValue.setText("暂无");
            lastSub.setText("首次启动后显示结果与耗时");
        } else {
            statValue.setText(summary.total() + " 次 · 成功率 " + summary.successRate() + "%");
            statSub.setText(
                    "崩溃 " + summary.crashes() + " 次 · 平均准备 " + LaunchFormat.duration(summary.avgPrepMs()));
            LaunchHistory.Entry last = entries.isEmpty() ? null : entries.get(0);
            if (last == null) {
                lastValue.setText("暂无");
                lastSub.setText("—");
            } else {
                lastValue.setText(time(last.time()));
                lastReason.setText(last.crash() ? crashReason(dir) : "");
                failActions.setVisible(last.crash());
                failActions.setManaged(last.crash());
                lastSub.setText(
                        LaunchHistory.outcomeLabel(last)
                                + " · 准备 "
                                + LaunchFormat.duration(last.prepMs())
                                + (last.sessionSeconds() > 0L
                                        ? " · 游戏 " + LaunchFormat.clock(last.sessionSeconds())
                                        : ""));
            }
        }

        refreshEnvironment();

        // 内存/目录每次刷新都重读，避免「一键修复」后卡片还显示旧值
        instanceMem.setText(I18n.t("overview.instance.memory", "分配内存 ") + config.getMemoryMb() + " MB");
        instanceDir.setText(I18n.t("overview.instance.dir", "目录 ") + config.getGameDir());

        String instance = currentInstance == null ? null : currentInstance.get();
        if (instance == null || instance.isBlank()) {
            instance = config.getLastInstance();
        }
        if (instance == null || instance.isBlank()) {
            instanceValue.setText("未选择实例");
            instanceSub.setText("去「实例选择」挑一个版本");
        } else {
            instanceValue.setText(instance);
            String mc = InstanceId.mcVersion(instance);
            instanceSub.setText("MC " + (mc == null ? "—" : mc) + " · " + loader(instance));
            instanceMods.setText(I18n.t("overview.mods", "模组 ") + countMods(instance) + I18n.t("overview.modsUnit", " 个"));
        }

        // 累计游玩永远按全部记录算，不跟着「只看失败」的筛选变
        long totalSession = 0L;
        int played = 0;
        for (LaunchHistory.Entry entry : allEntries) {
            if (entry.sessionSeconds() > 0L) {
                totalSession += entry.sessionSeconds();
                played++;
            }
        }
        if (played == 0) {
            totalValue.setText("—");
            totalSub.setText(entries.isEmpty() ? "还没有记录" : "记录里没有游玩时长");
        } else {
            totalValue.setText(LaunchFormat.clock(totalSession));
            totalSub.setText("共 " + played + " 次有游玩时长");
        }

        historyBox.getChildren().clear();
        if (entries.isEmpty()) {
            Label empty = new Label(I18n.t("empty.launchHistory", "还没有启动记录，启动一次后会显示在这里"));
            empty.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11px;");
            historyBox.getChildren().add(empty);
        } else {
            int limit = Math.min(12, entries.size());
            for (int i = 0; i < limit; i++) {
                historyBox.getChildren().add(historyRow(entries.get(i)));
            }
        }
    }

    private HBox historyRow(LaunchHistory.Entry entry) {
        boolean crashed = LaunchHistory.isCrash(entry);
        String stateText = crashed ? "异常" : (entry.stopped() ? "主动" : "正常");
        Label state = new Label(stateText);
        state.setMinWidth(40.0);
        state.setStyle(
                "-fx-text-fill: "
                        + (crashed ? "#e11d48" : (entry.stopped() ? "#6b7280" : "#0f9d6e"))
                        + "; -fx-font-size: 11px; -fx-font-weight: bold;");
        Label rowTime = new Label(time(entry.time()));
        rowTime.setMinWidth(78.0);
        rowTime.setStyle("-fx-text-fill: -app-text-2; -fx-font-size: 11px;");
        Label instance = new Label(entry.instanceId() == null ? "—" : entry.instanceId());
        instance.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11px;");
        instance.setTextOverrun(OverrunStyle.ELLIPSIS);
        instance.setMinWidth(0.0);
        instance.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(instance, Priority.ALWAYS);
        String costText = LaunchFormat.duration(entry.prepMs());
        if (entry.sessionSeconds() > 0L) {
            costText = costText + " · " + LaunchFormat.clock(entry.sessionSeconds());
        }
        Label cost = new Label(costText);
        cost.setMinWidth(74.0);
        cost.setAlignment(Pos.CENTER_RIGHT);
        cost.setStyle("-fx-text-fill: -app-text-dim-2; -fx-font-size: 10.5px;");
        HBox row = new HBox(10.0, state, rowTime, instance, cost);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(3.0, 6.0, 3.0, 6.0));
        // 反馈：整行悬停时给底色，鼠标经过能看出"这一行是一条记录"
        row.getStyleClass().add("history-row");
        return row;
    }

    private static Label headCell(String text, double minWidth, boolean right) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: -app-text-dim-3; -fx-font-size: 10px;");
        if (minWidth > 0.0) {
            label.setMinWidth(minWidth);
        } else {
            label.setMinWidth(0.0);
            label.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(label, Priority.ALWAYS);
        }
        if (right) {
            label.setAlignment(Pos.CENTER_RIGHT);
        }
        return label;
    }

    /** 环境卡的三行样式：小字号、次要色，异常行由 refreshEnvironment 换成警示色。 */
    private static String envLineStyle() {
        return "-fx-text-fill: -app-text-dim-2; -fx-font-size: 11px;";
    }

    /** 读 Java / 内存 / 磁盘，给一句能直接行动的结论。 */
    private void refreshEnvironment() {
        String javaPath = config.getJavaPath();
        if (javaPath == null || javaPath.isBlank()) {
            envJava.setText(I18n.t("overview.env.javaAuto", "Java：自动（优先用本机已装 JDK）"));
            envJava.setStyle(envLineStyle());
        } else {
            File exe = new File(javaPath);
            envJava.setText(I18n.t("overview.env.javaSet", "Java：") + exe.getName()
                    + " · " + exe.getParentFile().getParentFile().getName());
            envJava.setStyle(envLineStyle());
        }

        int memory = config.getMemoryMb();
        envMemory.setText(I18n.t("overview.env.memory", "内存：") + memory + " MB");
        boolean tooLow = memory < 2048;
        envMemory.setStyle(tooLow
                ? "-fx-text-fill: #f59e0b; -fx-font-size: 11px;"
                : envLineStyle());

        File gameDir = new File(config.getGameDir());
        long freeBytes = gameDir.getUsableSpace();
        double freeGb = freeBytes / 1024.0 / 1024.0 / 1024.0;
        String freeText = String.format(java.util.Locale.ROOT, "%.1f GB", freeGb);
        envDisk.setText(I18n.t("overview.env.disk", "磁盘剩余：") + freeText);
        boolean lowDisk = freeGb < 2.0;
        envDisk.setStyle(lowDisk
                ? "-fx-text-fill: #ef4444; -fx-font-size: 11px;"
                : envLineStyle());

        String head;
        if (lowDisk) {
            head = I18n.t("overview.env.headBad", "需要处理：磁盘快满了");
        } else if (tooLow) {
            head = I18n.t("overview.env.headWarn", "建议调高内存");
        } else {
            head = I18n.t("overview.env.headOk", "环境就绪 · 点这里调整");
        }
        // 只有「能自动修」的问题才给按钮：内存过低；磁盘满、Java 缺失只能人工处理
        fixEnvButton.setVisible(tooLow);
        fixEnvButton.setManaged(tooLow);
        envHead.setText(head);
        envHead.setStyle("-fx-text-fill: "
                + (lowDisk ? "#ef4444" : (tooLow ? "#f59e0b" : "-app-text"))
                + "; -fx-font-size: 13px; -fx-font-weight: bold;");
    }

    /** 数一下当前实例的模组数量；读不到就返回 0。 */
    private int countMods(String instance) {
        try {
            File mods = com.example.demo.core.model.InstanceDirs.mods(
                    new File(config.getGameDir()), instance, config.isIsolation());
            File[] files = mods.listFiles((dir, name) -> name.toLowerCase(java.util.Locale.ROOT).endsWith(".jar"));
            return files == null ? 0 : files.length;
        } catch (Exception e) {
            return 0;
        }
    }
    /** 一键修复能修的部分：内存低于下限时按推荐值调好并保存。 */
    private void fixEnvironment() {
        int recommended = 4096;
        try {
            recommended = com.example.demo.core.runtime.MemoryAdvisor.suggest(
                    com.example.demo.core.model.InstanceDirs.mods(
                            new File(config.getGameDir()),
                            config.getLastInstance(),
                            config.isIsolation())).memoryMb();
        } catch (Exception ignored) {
            // 拿不到推荐值就用 4096
        }
        int target = Math.max(config.getMemoryMb(), recommended);
        config.setMemoryMb(target);
        config.save();
        refresh();
        com.example.demo.ui.shell.Toast.ok(I18n.t("overview.env.fixed", "已把内存调到 ") + target + " MB");
    }

    /** 诊断用的环境快照：实例 / Java / 内存 / 模组 / 系统。 */
    private String environmentSnapshot() {
        String instance = currentInstance == null ? null : currentInstance.get();
        if (instance == null || instance.isBlank()) {
            instance = config.getLastInstance();
        }
        String javaPath = config.getJavaPath();
        String javaText = javaPath == null || javaPath.isBlank()
                ? I18n.t("overview.env.javaAuto", "Java：自动（优先用本机已装 JDK）").replace("Java：", "")
                : new File(javaPath).getName() + "（实例级/全局指定）";
        StringBuilder builder = new StringBuilder();
        builder.append(I18n.t("overview.diagEnv", "【环境】")).append('\n');
        builder.append(I18n.t("overview.diagInstance", "实例：")).append(instance == null ? "—" : instance).append('\n');
        builder.append(I18n.t("overview.diagJava", "Java：")).append(javaText).append('\n');
        builder.append(I18n.t("overview.diagMemory", "内存：")).append(config.getMemoryMb()).append(" MB").append('\n');
        if (instance != null && !instance.isBlank()) {
            builder.append(I18n.t("overview.diagMods", "模组：")).append(countMods(instance)).append('\n');
        }
        builder.append(I18n.t("overview.diagSystem", "系统："))
                .append(System.getProperty("os.name")).append(' ')
                .append(System.getProperty("os.version")).append(" / ")
                .append(System.getProperty("os.arch")).append('\n');
        builder.append(I18n.t("overview.diagDir", "目录：")).append(config.getGameDir());
        return builder.toString();
    }
    /** 启动报错检测：把崩溃报告里的结论整理成一份可复制的诊断。 */
    private void showDiagnosis() {
        File gameDir = new File(config.getGameDir());
        String text;
        try {
            com.example.demo.core.game.CrashAnalyzer.Report report =
                    com.example.demo.core.game.CrashAnalyzer.analyze(gameDir);
            text = report == null ? "没有找到崩溃报告。" : com.example.demo.core.game.CrashAnalyzer.toText(report);
        } catch (Exception e) {
            text = "读取崩溃报告失败：" + e.getMessage();
        }
        // 把环境信息拼在崩溃报告前面：复现问题常常就差这几行
        StringBuilder withEnv = new StringBuilder(environmentSnapshot());
        withEnv.append(System.lineSeparator()).append(System.lineSeparator()).append(text);
        text = withEnv.toString();
        javafx.scene.control.TextArea area = new javafx.scene.control.TextArea(text);
        area.setEditable(false);
        area.setWrapText(true);
        area.setPrefRowCount(12);
        area.setStyle("-fx-font-family: 'Consolas','Microsoft YaHei',monospace; -fx-font-size: 11px;");
        final String diagnosisText = text;
        javafx.scene.layout.VBox box = new javafx.scene.layout.VBox(8, area);
        box.setPrefWidth(560);
        com.example.demo.ui.shell.InAppDialog.customSized(
                com.example.demo.ui.shell.InAppDialog.globalHost(),
                I18n.t("overview.diagnoseTitle", "启动报错诊断"),
                box,
                java.util.List.of(
                        com.example.demo.ui.shell.InAppDialog.Action.primary(
                                I18n.t("overview.diagnoseCopy", "复制诊断"), () -> {
                                    javafx.scene.input.ClipboardContent content =
                                            new javafx.scene.input.ClipboardContent();
                                    content.putString(diagnosisText);
                                    javafx.scene.input.Clipboard.getSystemClipboard().setContent(content);
                                    com.example.demo.ui.shell.Toast.ok(
                                            I18n.t("overview.diagnoseCopied", "诊断已复制到剪贴板"));
                                }),
                        com.example.demo.ui.shell.InAppDialog.Action.secondary(
                                I18n.t("common.close", "关闭"), null)),
                null,
                600,
                420);
    }
    /** 从崩溃报告里提一句最关键的错误；读不到就返回空。 */
    private String crashReason(File gameDir) {
        try {
            var report = com.example.demo.core.game.CrashAnalyzer.analyze(gameDir);
            if (report == null || report.findings() == null || report.findings().isEmpty()) {
                return "";
            }
            String title = report.findings().get(0).title();
            if (title == null) {
                return "";
            }
            return title.length() > 90 ? title.substring(0, 90) + "…" : title;
        } catch (Exception e) {
            return "";
        }
    }
    private static Label caption(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 10.5px;");
        label.setTextOverrun(OverrunStyle.ELLIPSIS);
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }

    /** 统计卡可点：点一下跳到启动记录页。 */
    private static void makeCardClickable(VBox card, Runnable action) {
        if (action == null) return;
        card.setCursor(javafx.scene.Cursor.HAND);
        card.setOnMouseClicked(e -> action.run());
        com.example.demo.ui.shell.Motion.hoverCard(card);
    }

    private static VBox card(String title, Node... rows) {
        Label head = new Label(title);
        head.setStyle(
                "-fx-text-fill: -app-text-muted; -fx-font-size: 10.5px; -fx-font-weight: bold;");
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        VBox card = new VBox(6.0);
        card.getChildren().add(head);
        card.getChildren().addAll(rows);
        card.getChildren().add(spacer);
        card.setPadding(new Insets(14.0));
        card.setMaxWidth(Double.MAX_VALUE);
        card.setMaxHeight(Double.MAX_VALUE);
        card.getStyleClass().add("neko-card");
        VBox.setVgrow(card, Priority.ALWAYS);
        return card;
    }

    private static Button quickButton(String text, Runnable action) {
        Button button = new Button(text);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setMaxHeight(Double.MAX_VALUE);
        button.getStyleClass().add("btn-secondary");
        button.setStyle("-fx-padding: 8 10; -fx-font-size: 11.5px;");
        if (action != null) {
            button.setOnAction(e -> action.run());
        }
        Motion.press(button);
        return button;
    }

    private static String loader(String instanceId) {
        String lower = instanceId.toLowerCase(java.util.Locale.ROOT);
        return lower.contains("neoforge")
                ? "NeoForge"
                : lower.contains("forge")
                        ? "Forge"
                        : lower.contains("fabric")
                                ? "Fabric"
                                : lower.contains("quilt") ? "Quilt" : "原版";
    }

    private static String time(long epochMs) {
        java.time.LocalDateTime time =
                java.time.Instant.ofEpochMilli(epochMs)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDateTime();
        java.time.LocalDate today = java.time.LocalDate.now();
        String clock = String.format("%02d:%02d", time.getHour(), time.getMinute());
        if (time.toLocalDate().equals(today)) {
            return "今天 " + clock;
        }
        if (time.toLocalDate().equals(today.minusDays(1))) {
            return "昨天 " + clock;
        }
        return String.format("%02d-%02d %s", time.getMonthValue(), time.getDayOfMonth(), clock);
    }


}
