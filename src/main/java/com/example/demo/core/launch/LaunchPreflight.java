package com.example.demo.core.launch;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * 启动前自检：游戏目录存在、可写、磁盘空间够。
 *
 * <p>这些检查以前散在主类里，失败时抛出中文异常让 UI 直接显示；现在抽成纯逻辑，方便单测。
 */
public final class LaunchPreflight {

    /** 至少留 300MB，避免下到一半磁盘满。 */
    public static final long MIN_FREE_BYTES = 300L * 1024 * 1024;

    /** 自检通过后的信息（供日志展示）。 */
    public record Report(File gameDir, long usableBytes, int requiredJava) {
    }

    private LaunchPreflight() {
    }

    /**
     * @throws IllegalStateException 目录不可用 / 不可写 / 空间不足，message 直接给用户看
     */
    public static Report check(File gameDir, int requiredJava) {
        if (gameDir == null) {
            throw new IllegalStateException("游戏目录未设置");
        }
        if (!gameDir.isDirectory() && !gameDir.mkdirs()) {
            throw new IllegalStateException("游戏目录不可用: " + gameDir.getAbsolutePath());
        }
        File probe = new File(gameDir, ".write-test");
        try {
            Files.writeString(probe.toPath(), "ok", StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("游戏目录不可写: " + gameDir.getAbsolutePath(), e);
        } finally {
            if (probe.exists() && !probe.delete()) {
                probe.deleteOnExit();
            }
        }
        long usable = gameDir.getUsableSpace();
        if (usable > 0L && usable < MIN_FREE_BYTES) {
            throw new IllegalStateException("磁盘剩余空间不足 300MB（当前 "
                    + usable / 1024L / 1024L + " MB）");
        }
        return new Report(gameDir, usable, requiredJava);
    }
}
