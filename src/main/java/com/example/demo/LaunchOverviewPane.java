package com.example.demo;

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

import com.example.demo.core.model.LaunchHistory;
import com.example.demo.core.app.LauncherConfig;
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
    private final Label statSub = caption("—");
    private final Label lastValue = new Label("—");
    private final Label lastSub = caption("—");
    private final Label instanceValue = new Label("—");
    private final Label instanceSub = caption("—");
    private final VBox historyBox = new VBox(2.0);

    public LaunchOverviewPane(
            LauncherConfig config,
            Supplier<String> currentInstance,
            Runnable openInstances,
            Runnable openInstanceSettings,
            Runnable openDownload,
            Runnable openAdvisor) {
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

        VBox statCard = card("启动记录", statValue, statSub);
        VBox lastCard = card("最近一次启动", lastValue, lastSub);
        HBox statRow = new HBox(12.0, statCard, lastCard);
        statRow.setFillHeight(true);
        statRow.setMinHeight(89.0);
        statRow.setPrefHeight(89.0);
        statRow.setMaxHeight(89.0);

        HBox historyHead =
                new HBox(
                        10.0,
                        headCell("状态", 40.0, false),
                        headCell("时间", 78.0, false),
                        headCell("实例", 0.0, false),
                        headCell("耗时", 74.0, true));
        historyHead.setPadding(new Insets(0.0, 2.0, 2.0, 2.0));
        VBox historyCard = card("最近启动记录", historyHead, historyBox);
        HBox.setHgrow(historyCard, Priority.ALWAYS);

        VBox instanceCard = card("当前实例", instanceValue, instanceSub);

        GridPane quick = new GridPane();
        quick.setHgap(8.0);
        quick.setVgap(8.0);
        quick.add(quickButton("实例选择", openInstances), 0, 0);
        quick.add(quickButton("实例设置", openInstanceSettings), 1, 0);
        quick.add(quickButton("下载", openDownload), 0, 1);
        quick.add(quickButton("启动顾问", openAdvisor), 1, 1);
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
        List<LaunchHistory.Entry> entries = LaunchHistory.recent(dir);
        if (summary.total() == 0) {
            statValue.setText("还没有记录");
            statSub.setText("启动一次后会生成稳定性画像");
            lastValue.setText("暂无");
            lastSub.setText("首次启动后显示结果与耗时");
        } else {
            statValue.setText(summary.total() + " 次 · 成功率 " + summary.successRate() + "%");
            statSub.setText(
                    "崩溃 " + summary.crashes() + " 次 · 平均准备 " + duration(summary.avgPrepMs()));
            LaunchHistory.Entry last = entries.isEmpty() ? null : entries.get(0);
            if (last == null) {
                lastValue.setText("暂无");
                lastSub.setText("—");
            } else {
                lastValue.setText(time(last.time()));
                lastSub.setText(
                        (LaunchHistory.isCrash(last) ? "异常结束" : "正常结束")
                                + " · 准备 "
                                + duration(last.prepMs())
                                + (last.sessionSeconds() > 0L
                                        ? " · 游戏 " + clock(last.sessionSeconds())
                                        : ""));
            }
        }

        String instance = currentInstance == null ? null : currentInstance.get();
        if (instance == null || instance.isBlank()) {
            instance = config.getLastInstance();
        }
        if (instance == null || instance.isBlank()) {
            instanceValue.setText("未选择实例");
            instanceSub.setText("去「实例选择」挑一个版本");
        } else {
            instanceValue.setText(instance);
            String mc = mcVersion(instance);
            instanceSub.setText("MC " + (mc == null ? "—" : mc) + " · " + loader(instance));
        }

        historyBox.getChildren().clear();
        if (entries.isEmpty()) {
            Label empty = new Label("还没有启动记录，启动一次后会显示在这里");
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
        Label state = new Label(crashed ? "异常" : "正常");
        state.setMinWidth(40.0);
        state.setStyle(
                "-fx-text-fill: "
                        + (crashed ? "#e11d48" : "#0f9d6e")
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
        String costText = duration(entry.prepMs());
        if (entry.sessionSeconds() > 0L) {
            costText = costText + " · " + clock(entry.sessionSeconds());
        }
        Label cost = new Label(costText);
        cost.setMinWidth(74.0);
        cost.setAlignment(Pos.CENTER_RIGHT);
        cost.setStyle("-fx-text-fill: -app-text-dim-2; -fx-font-size: 10.5px;");
        HBox row = new HBox(10.0, state, rowTime, instance, cost);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(3.0, 2.0, 3.0, 2.0));
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

    private static Label caption(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 10.5px;");
        label.setTextOverrun(OverrunStyle.ELLIPSIS);
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
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
        card.setStyle(
                "-fx-background-color: -app-card-alt; -fx-background-radius: 16; -fx-border-color:"
                        + " -app-border-soft; -fx-border-radius: 14;");
        HBox.setHgrow(card, Priority.ALWAYS);
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

    private static String duration(long ms) {
        if (ms <= 0) {
            return "—";
        }
        if (ms < 1000) {
            return ms + " ms";
        }
        return String.format(java.util.Locale.ROOT, "%.1f s", ms / 1000.0);
    }

    private static String clock(long seconds) {
        if (seconds <= 0) {
            return "0 分";
        }
        long minutes = seconds / 60;
        if (minutes < 60) {
            return minutes + " 分";
        }
        return (minutes / 60) + " 时 " + (minutes % 60) + " 分";
    }

    /** 从实例 id 里提取 MC 版本号（fabric-loader-… / 26.3-forge-… 都兼容）。 */
    private static String mcVersion(String id) {
        if (id == null) {
            return null;
        }
        String value = id;
        int forge = value.indexOf("-forge-");
        if (forge > 0) {
            value = value.substring(0, forge);
        }
        if (value.startsWith("fabric-loader-")) {
            value = value.substring(value.lastIndexOf('-') + 1);
        }
        Matcher matcher = MC_VERSION.matcher(value);
        return matcher.find() ? matcher.group(1) : null;
    }
}
