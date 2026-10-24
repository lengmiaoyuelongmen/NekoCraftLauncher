package com.example.demo;

import java.io.File;

/**
 * 实例目录解析。
 * 开启版本隔离时，每个实例有独立的 gameDir（mods/config/saves/logs 都在里面），
 * 只共享 libraries 与 assets —— 这和 PCL 的版本隔离一致。
 */
public final class InstanceDirs {

    private InstanceDirs() {
    }

    public static File root(File baseGameDir, String instanceId, boolean isolated) {
        if (!isolated || instanceId == null || instanceId.isBlank()) return baseGameDir;
        return new File(new File(baseGameDir, "versions"), instanceId);
    }

    public static File mods(File baseGameDir, String instanceId, boolean isolated) {
        return new File(root(baseGameDir, instanceId, isolated), "mods");
    }

    public static File resourcepacks(File baseGameDir, String instanceId, boolean isolated) {
        return new File(root(baseGameDir, instanceId, isolated), "resourcepacks");
    }

    public static File shaderpacks(File baseGameDir, String instanceId, boolean isolated) {
        return new File(root(baseGameDir, instanceId, isolated), "shaderpacks");
    }

    public static File saves(File baseGameDir, String instanceId, boolean isolated) {
        return new File(root(baseGameDir, instanceId, isolated), "saves");
    }

    public static File natives(File baseGameDir, String instanceId, boolean isolated) {
        return new File(root(baseGameDir, instanceId, isolated), "natives");
    }

    public static File libraries(File baseGameDir) {
        return new File(baseGameDir, "libraries");
    }

    public static File assets(File baseGameDir) {
        return new File(baseGameDir, "assets");
    }
}
