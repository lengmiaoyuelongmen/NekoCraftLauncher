package com.example.demo;

import javafx.application.Application;

import com.example.demo.core.app.LauncherConfig;
import com.example.demo.ui.shell.HelloApplication;
public class Launcher {

    /** 进程启动时刻，用来量「到首帧」的真实耗时。 */
    private static final long START_NANOS = System.nanoTime();

    /** 当前进程从启动到现在的毫秒数（性能日志用）。 */
    public static long sinceStartMs() {
        return (System.nanoTime() - START_NANOS) / 1_000_000L;
    }

    public static void main(String[] args) {
        // 这些属性必须在 JavaFX 工具包初始化之前设置，否则会被忽略。
        System.setProperty("prism.lcdtext", "true");
        if (System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("win")) {
            // 使用 Windows 系统证书库，避免 Java 自带 cacerts 缺失导致更新下载 SSL 失败
            System.setProperty("javax.net.ssl.trustStoreType", "WINDOWS-ROOT");
        }
        LauncherConfig config = LauncherConfig.load();
        if (!config.isHardwareAcceleration()) {
            // 软件渲染：显卡驱动有问题 / 远程桌面 / 花屏时的兜底方案
            System.setProperty("prism.order", "sw");
            System.setProperty("prism.verbose", "false");
        }
        Application.launch(HelloApplication.class, args);
    }
}
