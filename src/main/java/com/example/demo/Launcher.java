package com.example.demo;

import javafx.application.Application;

public class Launcher {
    public static void main(String[] args) {
        // 这些属性必须在 JavaFX 工具包初始化之前设置，否则会被忽略。
        LauncherConfig config = LauncherConfig.load();
        if (!config.isHardwareAcceleration()) {
            // 软件渲染：显卡驱动有问题 / 远程桌面 / 花屏时的兜底方案
            System.setProperty("prism.order", "sw");
            System.setProperty("prism.verbose", "false");
        }
        Application.launch(HelloApplication.class, args);
    }
}
