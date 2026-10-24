package com.example.demo;

import java.io.File;
import java.util.function.Consumer;

/**
 * 首次启动初始化：游戏目录不随发布包分发，由启动器在运行时创建。
 */
public final class Bootstrap {

    private Bootstrap() {
    }

    /**
     * 确保游戏目录存在，并补齐基础子目录。
     *
     * @return true 表示本次是首次创建
     */
    public static boolean ensureGameDir(File gameDir, Consumer<String> log) {
        if (gameDir == null) return false;
        boolean firstRun = !gameDir.isDirectory();
        if (firstRun && !gameDir.mkdirs() && !gameDir.isDirectory()) {
            if (log != null) {
                log.accept("❌ 无法创建游戏目录: " + gameDir.getAbsolutePath()
                        + "（请把启动器放到有写入权限的目录）");
            }
            return false;
        }
        ensureSub(gameDir, "logs");
        ensureSub(gameDir, "versions");
        if (firstRun && log != null) {
            log.accept("📁 首次启动：已创建游戏目录 " + gameDir.getAbsolutePath());
        }
        return firstRun;
    }

    private static void ensureSub(File parent, String name) {
        File dir = new File(parent, name);
        if (!dir.isDirectory()) dir.mkdirs();
    }
}
