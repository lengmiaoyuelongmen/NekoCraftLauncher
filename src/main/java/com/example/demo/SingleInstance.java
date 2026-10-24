package com.example.demo;

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

    public static boolean acquire(File gameDir) {
        try {
            File file = new File(gameDir, ".launcher.lock");
            channel = new FileOutputStream(file, true).getChannel();
            lock = channel.tryLock();
            return lock != null;
        } catch (Exception e) {
            // 锁失败时按“已在运行”处理，避免配置被并发覆盖
            return false;
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
