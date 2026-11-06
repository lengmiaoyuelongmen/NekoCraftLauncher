package com.example.demo.ui.page;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 整合包安装进度页：两条进度——「下载进度」（字节 / 速度 / 预计剩余）和「安装进度」（步骤）。
 *
 * <p>API 与原先的 InstallWindow 保持一致：setStep / setProgress / setStatus / finish。
 */
public final class InstallProgressPage {

    private final VBox root = new VBox(12);
    private final VBox stepBox = new VBox(8);
    private final Label statusLabel = new Label("准备中…");

    private final ProgressBar downloadBar = new ProgressBar(0);
    private final Label downloadInfo = new Label("暂无下载任务");
    private final ProgressBar installBar = new ProgressBar(0);
    private final Label installInfo = new Label("准备中…");

    private final List<Label> stepIcons = new ArrayList<>();
    private final List<Label> stepTexts = new ArrayList<>();
    private final List<String> stepNames = new ArrayList<>();
    private final Button back = new Button("‹  返回");
    private final Button cancel = new Button("取消安装");
    private Runnable onCancel;
    private Runnable onBack;
    private boolean finished;

    public InstallProgressPage(String title, List<String> steps) {
        back.getStyleClass().addAll("btn-secondary", "btn-sm");
        cancel.getStyleClass().addAll("btn-secondary", "btn-sm");
        back.setOnAction(e -> {
            if (finished) {
                if (onBack != null) onBack.run();
                return;
            }
            statusLabel.setText("安装仍在后台继续，完成后会提示");
            if (onBack != null) onBack.run();
        });
        cancel.setOnAction(e -> {
            statusLabel.setText("正在取消…");
            cancel.setDisable(true);
            if (onCancel != null) onCancel.run();
        });

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill: -app-text; -fx-font-size: 15px; -fx-font-weight: bold;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(10, back, titleLabel, spacer, cancel);
        header.setAlignment(Pos.CENTER_LEFT);

        for (String step : steps) {
            Label icon = new Label("○");
            icon.setMinWidth(18);
            icon.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 12px;");
            Label text = new Label(step);
            text.setStyle("-fx-text-fill: -app-text-dim; -fx-font-size: 12px;");
            HBox row = new HBox(8, icon, text);
            row.setAlignment(Pos.CENTER_LEFT);
            stepBox.getChildren().add(row);
            stepIcons.add(icon);
            stepTexts.add(text);
            stepNames.add(step);
        }

        statusLabel.setStyle("-fx-text-fill: -app-text-2; -fx-font-size: 11.5px;");
        statusLabel.setWrapText(true);

        downloadBar.setMaxWidth(Double.MAX_VALUE);
        downloadBar.setPrefHeight(8);
        installBar.setMaxWidth(Double.MAX_VALUE);
        installBar.setPrefHeight(8);
        downloadInfo.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11px;");
        installInfo.setStyle("-fx-text-fill: -app-text-2; -fx-font-size: 11px;");

        VBox downloadBox = section("下载进度", downloadBar, downloadInfo);
        VBox installBox = section("安装进度", installBar, installInfo);

        root.setPadding(new Insets(6, 8, 6, 8));
        root.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        root.getChildren().addAll(header, downloadBox, installBox, stepBox, statusLabel);
    }

    private static VBox section(String title, ProgressBar bar, Label info) {
        Label head = new Label(title);
        head.setStyle("-fx-text-fill: -app-text-2; -fx-font-size: 11.5px; -fx-font-weight: bold;");
        VBox box = new VBox(4, head, bar, info);
        box.setFillWidth(true);
        return box;
    }

    public VBox node() {
        return root;
    }

    public void setOnCancel(Runnable action) {
        this.onCancel = action;
    }

    public void setOnBack(Runnable action) {
        this.onBack = action;
    }

    // ==================== 下载进度 ====================

    /** 文件数 + 已下载/总字节 + 速度 + 预计剩余，来自 DownloadManager 的实时状态。 */
    public void setDownloadItems(
            int done, int total, long bytes, long totalBytes, long speedBps, String file) {
        if (total <= 0 && bytes <= 0) {
            clearDownload();
            return;
        }
        long shownBytes = totalBytes > 0 ? Math.min(bytes, totalBytes) : bytes;
        if (totalBytes > 0) {
            downloadBar.setProgress(Math.max(0, Math.min(1, bytes / (double) totalBytes)));
        } else if (total > 0) {
            downloadBar.setProgress(Math.max(0, Math.min(1, done / (double) total)));
        } else {
            downloadBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        }
        StringBuilder text = new StringBuilder();
        if (totalBytes > 0) {
            text.append(formatBytes(shownBytes)).append(" / ").append(formatBytes(totalBytes));
        } else if (bytes > 0) {
            text.append(formatBytes(bytes));
        }
        if (total > 0) {
            if (text.length() > 0) text.append(" · ");
            text.append(done).append('/').append(total).append(" 个文件");
        }
        if (speedBps > 0) {
            if (text.length() > 0) text.append(" · ");
            text.append(formatBytes(speedBps)).append("/s");
            if (totalBytes > bytes) {
                String eta = formatEta(totalBytes - bytes, speedBps);
                if (!eta.isEmpty()) {
                    text.append(" · ").append(eta);
                }
            }
        }
        String name = shortName(file);
        if (!name.isEmpty()) {
            text.append(" · ").append(name);
        }
        downloadInfo.setText(text.toString());
    }

    /** 只有一个不透明活动（如 Forge installer 在下载库文件）时的不确定进度。 */
    public void setDownloadActivity(String fileName) {
        downloadBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        downloadInfo.setText("正在下载：" + shortName(fileName));
    }

    public void clearDownload() {
        downloadBar.setProgress(0);
        downloadInfo.setText("暂无下载任务");
    }

    // ==================== 安装进度 ====================

    public void setInstallProgress(double value, String text) {
        if (value < 0) {
            installBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        } else {
            installBar.setProgress(Math.max(0, Math.min(1, value)));
        }
        if (text != null && !text.isBlank()) {
            installInfo.setText(text);
        }
    }

    public void setProgress(double value) {
        setInstallProgress(value, null);
    }

    public void setStep(int index, String state) {
        if (index < 0 || index >= stepIcons.size()) {
            return;
        }
        Label icon = stepIcons.get(index);
        Label text = stepTexts.get(index);
        String name = stepNames.get(index);
        int total = Math.max(1, stepIcons.size());
        double base = index / (double) total;
        double done = (index + 1) / (double) total;
        switch (state) {
            case "run" -> {
                icon.setText("●");
                icon.setStyle("-fx-text-fill: -app-accent; -fx-font-size: 12px;");
                text.setStyle("-fx-text-fill: -app-text; -fx-font-size: 12px; -fx-font-weight: bold;");
                setInstallProgress(base + 0.5 / total, "正在进行：" + name + "…");
            }
            case "ok" -> {
                icon.setText("✔");
                icon.setStyle("-fx-text-fill: #22c55e; -fx-font-size: 12px;");
                text.setStyle("-fx-text-fill: -app-text-2; -fx-font-size: 12px;");
                setInstallProgress(done, "已完成：" + name);
            }
            case "fail" -> {
                icon.setText("✘");
                icon.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12px;");
                text.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12px; -fx-font-weight: bold;");
                setInstallProgress(base, "失败：" + name);
            }
            case "skip" -> {
                icon.setText("—");
                icon.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 12px;");
                text.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 12px;");
                setInstallProgress(done, "已跳过：" + name);
            }
            default -> {
                icon.setText("○");
                icon.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 12px;");
                text.setStyle("-fx-text-fill: -app-text-dim; -fx-font-size: 12px;");
                setInstallProgress(base, "等待：" + name);
            }
        }
    }

    public void setStatus(String text) {
        statusLabel.setText(text);
    }

    public void finish(boolean ok, String message) {
        finished = true;
        setInstallProgress(ok ? 1 : 0, ok ? "安装完成" : "安装失败");
        statusLabel.setText(message);
        statusLabel.setStyle(
                ok
                        ? "-fx-text-fill: #22c55e; -fx-font-size: 11.5px;"
                        : "-fx-text-fill: #ef4444; -fx-font-size: 11.5px;");
        cancel.setDisable(true);
        cancel.setVisible(false);
        cancel.setManaged(false);
        back.setText("‹  返回实例列表");
    }

    // ==================== 格式化 ====================

    private static String shortName(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        int slash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return slash >= 0 && slash + 1 < path.length() ? path.substring(slash + 1) : path;
    }

    public static String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024L * 1024L) {
            return String.format(Locale.ROOT, "%.1f KB", bytes / 1024.0);
        }
        if (bytes < 1024L * 1024L * 1024L) {
            return String.format(Locale.ROOT, "%.1f MB", bytes / 1048576.0);
        }
        return String.format(Locale.ROOT, "%.2f GB", bytes / 1073741824.0);
    }

    /** 预计剩余时间：给「x MB/s + 已下载」用的剩余字节数算出来。 */
    public static String formatEta(long remainingBytes, long speedBps) {
        if (remainingBytes <= 0 || speedBps <= 0) {
            return "";
        }
        long seconds = Math.max(1, remainingBytes / speedBps);
        if (seconds < 60) {
            return "剩余约 " + seconds + " 秒";
        }
        if (seconds < 3600) {
            return "剩余约 " + (seconds / 60) + " 分 " + (seconds % 60) + " 秒";
        }
        return "剩余约 " + (seconds / 3600) + " 小时 " + ((seconds % 3600) / 60) + " 分";
    }
}
