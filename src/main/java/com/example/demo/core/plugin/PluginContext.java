package com.example.demo.core.plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 插件可用的受限上下文：日志、自己的数据目录、命令面板命令、事件钩子、自带配置。
 *
 * <p>刻意不给「改界面 / 改启动参数」的能力 —— 插件能做事，但不能把主流程改坏。
 */
public final class PluginContext {

    public record Command(String title, String hint, Runnable action) {
    }

    private final File dataDir;
    private final String pluginName;
    private final Consumer<String> logger;
    private final List<Command> commands = new ArrayList<>();
    private final PluginConfig config;

    PluginContext(File dataDir, String pluginName, Consumer<String> logger) {
        this.dataDir = dataDir;
        this.pluginName = pluginName == null || pluginName.isBlank() ? "unknown" : pluginName;
        this.logger = logger;
        this.config = new PluginConfig(new File(dataDir, safeFileName(this.pluginName) + ".properties"));
    }

    public File dataDir() {
        return dataDir;
    }

    public String pluginName() {
        return pluginName;
    }

    /** 本插件自己的持久化配置。 */
    public PluginConfig config() {
        return config;
    }

    public void log(String message) {
        if (logger != null) {
            logger.accept("[插件] " + message);
        }
    }

    public void addCommand(String title, String hint, Runnable action) {
        if (title == null || title.isBlank() || action == null) {
            return;
        }
        commands.add(new Command(title, hint == null ? "" : hint, action));
    }

    /**
     * 订阅宿主事件。payload 含义见 {@link PluginHook}。
     *
     * <p>回调在宿主线程上同步执行，别在里面做耗时操作（写盘、联网）。
     */
    public void on(PluginHook hook, Consumer<String> listener) {
        PluginHooks.register(hook, pluginName, listener);
    }

    public List<Command> commands() {
        return List.copyOf(commands);
    }

    /** 插件名可能带空格/斜杠，落成文件名前先洗一遍。 */
    private static String safeFileName(String name) {
        String safe = name.replaceAll("[\\\\/:*?\"<>|\\s]+", "_");
        return safe.isBlank() ? "plugin" : safe;
    }
}
