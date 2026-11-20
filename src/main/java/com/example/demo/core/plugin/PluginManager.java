package com.example.demo.core.plugin;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.Consumer;

import com.example.demo.core.app.AppPaths;

/** 从 plugins/*.jar 加载 NekoPlugin 实现。 */
public final class PluginManager {

    private static volatile List<PluginContext.Command> commands = List.of();
    private static volatile int loadedPlugins = 0;

    private PluginManager() {
    }

    /** 已注册的插件命令（命令面板用）。 */
    public static List<PluginContext.Command> commands() {
        return commands;
    }

    /** 成功初始化的插件数量（「插件」页与日志用）。 */
    public static int loadedPlugins() {
        return loadedPlugins;
    }

    public static List<PluginContext.Command> load(Consumer<String> log) {
        File dataDir = new File(AppPaths.dataDir(), "plugins");
        List<File> jars = new ArrayList<>();
        collect(new File(AppPaths.baseDir(), "plugins"), jars);
        collect(dataDir, jars);

        List<PluginContext.Command> collected = new ArrayList<>();
        int loaded = 0;
        for (File jar : jars) {
            try {
                URLClassLoader loader = new URLClassLoader(new URL[]{jar.toURI().toURL()},
                        PluginManager.class.getClassLoader());
                ServiceLoader<NekoPlugin> services = ServiceLoader.load(NekoPlugin.class, loader);
                for (NekoPlugin plugin : services) {
                    // 每个插件一个上下文：各自的配置文件和事件订阅互不干扰
                    PluginContext context = new PluginContext(dataDir, plugin.name(), log);
                    plugin.init(context);
                    collected.addAll(context.commands());
                    loaded++;
                    if (log != null) {
                        log.accept("🔌 已加载插件: " + plugin.name()
                                + (context.commands().isEmpty() ? "" : "（命令 " + context.commands().size() + " 条）"));
                    }
                }
            } catch (Exception e) {
                if (log != null) {
                    log.accept("⚠ 插件加载失败 " + jar.getName() + ": " + e.getMessage());
                }
            }
        }
        commands = List.copyOf(collected);
        loadedPlugins = loaded;
        return commands;
    }

    /** 收集目录下的 jar；两个目录可能有同名文件，按绝对路径去重。 */
    private static void collect(File dir, List<File> out) {
        File[] files = dir.listFiles((d, name) -> name.toLowerCase(java.util.Locale.ROOT).endsWith(".jar"));
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (!out.contains(file)) {
                out.add(file);
            }
        }
    }
}
