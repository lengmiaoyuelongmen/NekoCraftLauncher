package com.example.demo.core.launch;

import com.example.demo.core.download.DownloadManager;
import com.example.demo.core.meta.VersionProfile;

import java.io.File;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * 启动准备阶段在任务之间传递的上下文：版本档案、目录、日志、进度、取消信号。
 *
 * <p>UI 只负责把 {@code log} 接到日志区、把 {@code progress} 接到进度条、
 * 把 {@code cancelled} 接到「取消启动」按钮，核心不知道 JavaFX 的存在。
 */
public final class LaunchContext {

    private final VersionProfile profile;
    private final File gameDir;
    private final File instanceDir;
    private final Consumer<String> log;
    private final DownloadManager.Progress progress;
    private final BooleanSupplier cancelled;

    public LaunchContext(VersionProfile profile, File gameDir, File instanceDir,
                         Consumer<String> log, DownloadManager.Progress progress,
                         BooleanSupplier cancelled) {
        this.profile = profile;
        this.gameDir = gameDir;
        this.instanceDir = instanceDir;
        this.log = log == null ? message -> { } : log;
        this.progress = progress;
        this.cancelled = cancelled;
    }

    public VersionProfile profile() {
        return profile;
    }

    public File gameDir() {
        return gameDir;
    }

    /** 该实例的游戏目录（版本隔离时是 versions/&lt;id&gt;）。 */
    public File instanceDir() {
        return instanceDir;
    }

    public void log(String message) {
        log.accept(message);
    }

    public DownloadManager.Progress progress() {
        return progress;
    }

    /** 用户点了「取消启动」就抛出 {@link LaunchCancelledException}，由 UI 统一收尾。 */
    public void checkCancelled() {
        if (cancelled != null && cancelled.getAsBoolean()) {
            throw new LaunchCancelledException();
        }
    }

    public boolean isCancelled() {
        return cancelled != null && cancelled.getAsBoolean();
    }
}
