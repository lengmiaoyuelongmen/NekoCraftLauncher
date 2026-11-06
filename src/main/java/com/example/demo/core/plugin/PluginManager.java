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

    private PluginManager() {
    }

    public static List<PluginContext.Command> commands() {
        return commands;
    }

    public static List<PluginContext.Command> load(Consumer<String> log) {
        PluginContext context = new PluginContext(new File(AppPaths.dataDir(), "plugins"), log);
        List<File> jars = new ArrayList<>();
        collect(new File(AppPaths.baseDir(), "plugins"), jars);
        collect(new File(AppPaths.dataDir(), "plugins"), jars);
        for (File jar : jars) {
            try {
                URLClassLoader loader = new URLClassLoader(new URL[]{jar.toURI().toURL()},
                        PluginManager.class.getClassLoader());
                ServiceLoader<NekoPlugin> services = ServiceLoader.load(NekoPlugin.class, loader);
                for (NekoPlugin plugin : services) {
                    plugin.init(context);
                    if (log != null) log.accept("🔌 已加载插件: " + plugin.name());
                }
            } catch (Exception e) {
                if (log != null) log.accept("⚠ 插件加载失败 " + jar.getName() + ": " + e.getMessage());
            }
        }
        commands = context.commands();
        return commands;
    }

    private static void collect(File dir, List<File> out) {
        File[] files = dir.listFiles((d, n) -> n.toLowerCase(java.util.Locale.ROOT).endsWith(".jar"));
        if (files != null) {
            for (File file : files) if (!out.contains(file)) out.add(file);
        }
    }
}
