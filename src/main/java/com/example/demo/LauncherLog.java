package com.example.demo;

import javafx.scene.control.TextArea;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

/**
 * 把启动页日志同步写到 game/logs/launcher.log，供反馈包/排查使用；超过 5MB 自动轮转一份。
 */
public final class LauncherLog {

    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static File file;

    private LauncherLog() {
    }

    public static synchronized void attach(File gameDir, TextArea area) {
        if (area == null) return;
        File dir = new File(gameDir, "logs");
        if (!dir.isDirectory() && !dir.mkdirs()) return;
        file = new File(dir, "launcher.log");
        rotateIfNeeded(file);
        area.textProperty().addListener((obs, oldText, newText) -> append(oldText, newText));
    }

    private static synchronized void append(String oldText, String newText) {
        if (file == null || newText == null) return;
        String delta = newText.length() >= (oldText == null ? 0 : oldText.length())
                ? newText.substring(oldText == null ? 0 : oldText.length())
                : newText;
        if (delta.isEmpty()) return;
        try {
            Files.writeString(file.toPath(), delta, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            if (file.length() > MAX_BYTES) rotateIfNeeded(file);
        } catch (Exception ignored) {
            // 日志写失败不影响界面
        }
    }

    private static void rotateIfNeeded(File current) {
        try {
            if (!current.isFile() || current.length() <= MAX_BYTES) return;
            File old = new File(current.getParentFile(), "launcher.1.log");
            Files.deleteIfExists(old.toPath());
            Files.move(current.toPath(), old.toPath());
        } catch (Exception ignored) {
            // 忽略
        }
    }

    public static File currentFile() {
        return file;
    }
}
