package com.example.demo.core.app;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

/**
 * 单实例锁：同一个游戏目录只允许一个启动器实例，避免两个进程同时写 config / 下载。
 */
public final class SingleInstance {

    private static FileChannel channel;
    private static FileLock lock;

    private SingleInstance() {
    }

    /** 上一次 acquire 失败的真实原因（供界面区分「已在运行」和「目录不可写」）。 */
    private static String lastError = "";

    public static String lastError() {
        return lastError == null ? "" : lastError;
    }

    public static boolean acquire(File gameDir) {
        lastError = "";
        FileChannel ch = null;
        try {
            File file = new File(gameDir, ".launcher.lock");
            ch = new FileOutputStream(file, true).getChannel();
            FileLock acquired = ch.tryLock();
            if (acquired == null) {
                // 这才是真的「已有实例在运行」
                lastError = "已有启动器实例持有锁";
                ch.close();
                return false;
            }
            channel = ch;
            lock = acquired;
            return true;
        } catch (java.nio.channels.OverlappingFileLockException overlap) {
            lastError = "已有启动器实例持有锁";
            closeQuietly(ch);
            return false;
        } catch (Exception e) {
            // 权限/磁盘问题不能被误报成「已在运行」
            lastError = "无法创建锁文件：" + e.getMessage();
            closeQuietly(ch);
            return false;
        }
    }

    private static void closeQuietly(FileChannel ch) {
        if (ch == null) return;
        try {
            ch.close();
        } catch (IOException ignored) {
            // 关闭失败无所谓
        }
    }

    public static void release() {
        try {
            if (lock != null) lock.release();
            if (channel != null) channel.close();
        } catch (IOException ignored) {
            // 退出阶段忽略
        }
    }
}
