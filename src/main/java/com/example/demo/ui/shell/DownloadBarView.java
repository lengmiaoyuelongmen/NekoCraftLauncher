package com.example.demo.ui.shell;

import com.example.demo.core.download.DownloadManager;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.Region;

import java.util.Locale;

/**
 * 启动页底部「下载任务」条：文件名 / 进度条 / 速度与剩余百分比 / 停滞提示。
 *
 * <p>把原来散在主类里的 6 个字段（三个节点 + 计划字节 / 上次字节 / 上次变化时间）和两段更新逻辑
 * 收进一个类。节点仍由主类创建（要放进它自己的布局里），这里只负责状态与渲染。
 */
public final class DownloadBarView {

    /** 进度条超过这么久没变化就提示「已停滞」，避免用户以为卡死。 */
    private static final long STALL_MILLIS = 30_000L;

    private final ProgressBar progress;
    private final Label fileLabel;
    private final Label statsLabel;
    private final Region bar;

    private long expectedBytes = -1L;
    private long lastBytes = -1L;
    private long lastChangeAt;

    public DownloadBarView(ProgressBar progress, Label fileLabel, Label statsLabel) {
        this(progress, fileLabel, statsLabel, null);
    }

    public DownloadBarView(ProgressBar progress, Label fileLabel, Label statsLabel, Region bar) {
        this.progress = progress;
        this.fileLabel = fileLabel;
        this.statsLabel = statsLabel;
        this.bar = bar;
    }

    /** 单个文件下载时用它算真实百分比（多文件时进度条转圈）。 */
    public void setExpectedBytes(long bytes) {
        this.expectedBytes = bytes;
    }

    /** 下载线程里的简易进度：切回 FX 线程再渲染。 */
    public void progress(int done, int total, long bytes) {
        Platform.runLater(() -> update(new DownloadManager.Status(
                done,
                total,
                bytes,
                0L,
                fileLabel == null ? "" : fileLabel.getText(),
                0L)));
    }

    /** 根据下载状态刷新三个控件。必须在 FX 线程调用。 */
    public void update(DownloadManager.Status status) {
        if (progress == null || fileLabel == null || statsLabel == null) {
            return;
        }
        if (bar != null) {
            bar.setVisible(true);
            bar.setManaged(true);
        }
        if (status.total() <= 0 || status.done() >= status.total()) {
            fileLabel.setText("空闲");
            fileLabel.setVisible(true);
            fileLabel.setManaged(true);
            statsLabel.setVisible(false);
            statsLabel.setManaged(false);
            progress.setVisible(false);
            progress.setManaged(false);
            progress.setProgress(0.0);
            return;
        }
        long now = System.currentTimeMillis();
        if (status.bytes() != lastBytes) {
            lastBytes = status.bytes();
            lastChangeAt = now;
        } else if (lastChangeAt > 0L && now - lastChangeAt > STALL_MILLIS) {
            progress.setVisible(false);
            progress.setManaged(false);
            statsLabel.setVisible(false);
            statsLabel.setManaged(false);
            fileLabel.setText("下载已停滞，请重新开始下载");
            return;
        }
        statsLabel.setVisible(true);
        statsLabel.setManaged(true);
        progress.setVisible(true);
        progress.setManaged(true);
        boolean multiFile = status.total() > 1;
        double ratio = !multiFile && expectedBytes > 0L
                ? Math.min(1.0, (double) status.bytes() / (double) expectedBytes)
                : Math.min(1.0, (double) status.done() / (double) Math.max(1, status.total()));
        progress.setProgress(multiFile ? -1.0 : ratio);
        fileLabel.setText(status.file() == null || status.file().isBlank()
                ? "正在下载…"
                : "正在下载：" + status.file());
        double mb = (double) status.bytes() / 1048576.0;
        long bps = status.speedBps();
        String speed = bps <= 0L
                ? "—"
                : (bps >= 0x100000L
                        ? String.format(Locale.ROOT, "%.1f MB/s", (double) bps / 1048576.0)
                        : bps / 1024L + " KB/s");
        int percent = (int) Math.round(ratio * 100.0);
        statsLabel.setText(status.done() + "/" + status.total() + " 个文件 · "
                + String.format(Locale.ROOT, "%.1f MB", mb) + " · " + speed + " · " + percent + "%");
    }
}
