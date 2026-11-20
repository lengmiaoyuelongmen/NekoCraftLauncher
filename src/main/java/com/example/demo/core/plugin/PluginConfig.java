package com.example.demo.core.plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Properties;

/**
 * 插件自己的小型持久化：一个插件一个 properties 文件。
 *
 * <p>放 properties 而不是 JSON，是为了让插件作者用 JDK 自带的 API 就能读写，不依赖 Gson；
 * 文件位于 {@code <数据目录>/plugins/<插件 id>.properties}。
 */
public final class PluginConfig {

    private final File file;
    private final Properties properties = new Properties();

    PluginConfig(File file) {
        this.file = file;
        if (file.isFile()) {
            try (InputStream in = Files.newInputStream(file.toPath())) {
                properties.load(in);
            } catch (IOException ignored) {
                // 读不出来就当空配置，插件自己会看到默认值
            }
        }
    }

    /** 读一个值；没有时返回默认值。 */
    public String get(String key, String defaultValue) {
        if (key == null || key.isBlank()) {
            return defaultValue;
        }
        return properties.getProperty(key, defaultValue);
    }

    /** 读整数；解析失败返回默认值。 */
    public int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(get(key, String.valueOf(defaultValue)).trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /** 写一个值（只改内存，{@link #save()} 才落盘）。 */
    public void put(String key, String value) {
        if (key == null || key.isBlank() || value == null) {
            return;
        }
        properties.setProperty(key, value);
    }

    /** 写整数。 */
    public void putInt(String key, int value) {
        put(key, Integer.toString(value));
    }

    /**
     * 落盘。
     *
     * @return 是否写成功（磁盘只读/被占用时返回 false，插件可以据此提示用户）
     */
    public boolean save() {
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
                return false;
            }
            try (OutputStream out = Files.newOutputStream(file.toPath())) {
                properties.store(out, "Neko Craft Launcher plugin config");
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /** 配置文件位置，日志里能直接告诉用户。 */
    public File file() {
        return file;
    }
}
