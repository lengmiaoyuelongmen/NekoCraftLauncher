package com.example.demo.ui.component;

import java.io.File;
import java.util.List;
import java.util.Locale;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

import com.example.demo.core.model.LaunchHistory;
import com.example.demo.core.app.LauncherConfig;
import com.example.demo.ui.shell.Icons;
/**
 * 启动页顶部的「启动顾问」横条：根据启动历史给出一句建议，并显示上次准备耗时与成功率。
 *
 * <p>从 HelloApplication 抽出的独立组件，只依赖配置和一个「打开记录」回调。
 */
public final class LaunchAdvisorBar extends HBox {

    private final LauncherConfig config;
    private final Label tip = new Label();
    private final Label metrics = new Label();

    public LaunchAdvisorBar(LauncherConfig config, Runnable openHistory) {
        super(8.0);
        this.config = config;

        StackPane icon = Icons.node("sparkles", 14.0, "#9ca3af");
        Label title = new Label("启动顾问");
        title.setStyle("-fx-text-fill: -app-text-2; -fx-font-size: 11px; -fx-font-weight: bold;");
        tip.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 10.5px;");
        tip.setWrapText(false);
        metrics.setStyle("-fx-text-fill: -app-text-dim-3; -fx-font-size: 10px;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button history = new Button("记录");
        history.setStyle(
                "-fx-background-color: rgba(255,255,255,0.06); -fx-text-fill: -app-text-dim;"
                        + " -fx-background-radius: 7; -fx-padding: 3 10; -fx-font-size: 10.5px;"
                        + " -fx-cursor: hand;");
        if (openHistory != null) {
            history.setOnAction(e -> openHistory.run());
        }
        getChildren().addAll(icon, title, tip, spacer, metrics, history);
        setAlignment(Pos.CENTER_LEFT);
        setPadding(new Insets(5.0, 12.0, 5.0, 12.0));
        setStyle(
                "-fx-background-color: linear-gradient(to right, rgba(255,255,255,0.055),"
                        + " rgba(255,255,255,0.018)); -fx-background-radius: 11; -fx-border-color:"
                        + " rgba(255,255,255,0.07); -fx-border-radius: 11;");
        refresh();
    }

    /** 重新读取启动历史并更新建议文案。 */
    public void refresh() {
        File dir = new File(config.getGameDir());
        List<LaunchHistory.Entry> entries = LaunchHistory.recent(dir);
        if (entries.isEmpty()) {
            tip.setText("还没有启动记录，启动一次后会生成稳定性画像");
            metrics.setText("");
            return;
        }
        int total = entries.size();
        int crashes = 0;
        long prepSum = 0L;
        int prepCount = 0;
        for (LaunchHistory.Entry entry : entries) {
            if (LaunchHistory.isCrash(entry)) {
                crashes++;
            }
            if (entry.prepMs() <= 0L) {
                continue;
            }
            prepSum += entry.prepMs();
            prepCount++;
        }
        int rate = Math.round((float) (total - crashes) * 100.0f / (float) total);
        long avgPrep = prepCount == 0 ? 0L : prepSum / prepCount;
        long lastPrep = entries.get(0).prepMs();
        metrics.setText(
                "上次准备 "
                        + (lastPrep <= 0L
                                ? "—"
                                : String.format(Locale.ROOT, "%.1fs", lastPrep / 1000.0))
                        + " · 成功率 "
                        + rate
                        + "%");
        if (rate < 80) {
            tip.setText("最近异常率偏高，建议先跑一次崩溃诊断");
        } else if (lastPrep > 5000L) {
            tip.setText("启动准备偏慢，可检查 Java 与磁盘占用");
        } else if (avgPrep > 0L) {
            tip.setText(
                    "运行稳定，平均准备 " + String.format(Locale.ROOT, "%.1fs", avgPrep / 1000.0));
        } else {
            tip.setText("运行稳定，继续游戏吧");
        }
    }
}
