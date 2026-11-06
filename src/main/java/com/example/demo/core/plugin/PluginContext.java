package com.example.demo.core.plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** 插件可用的受限上下文。 */
public final class PluginContext {

    public record Command(String title, String hint, Runnable action) {}

    private final File dataDir;
    private final Consumer<String> logger;
    private final List<Command> commands = new ArrayList<>();

    PluginContext(File dataDir, Consumer<String> logger) {
        this.dataDir = dataDir;
        this.logger = logger;
    }

    public File dataDir() {
        return dataDir;
    }

    public void log(String message) {
        if (logger != null) logger.accept("[插件] " + message);
    }

    public void addCommand(String title, String hint, Runnable action) {
        if (title == null || title.isBlank() || action == null) return;
        commands.add(new Command(title, hint == null ? "" : hint, action));
    }

    public List<Command> commands() {
        return List.copyOf(commands);
    }
}
