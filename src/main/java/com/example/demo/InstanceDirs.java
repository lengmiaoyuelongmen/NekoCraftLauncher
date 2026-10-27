package com.example.demo;

import java.io.File;

/**
 * 实例目录解析：兼容 PCL-CE 的版本隔离布局。
 * PCL 隔离版本把 mods/config/saves 等放在 versions/<id>/ 下；
 * 部分启动器还会再嵌套一层 .minecraft，这里会优先识别已有内容目录。
 */
public final class InstanceDirs {

    private InstanceDirs() {
    }

    public static File versionDir(File baseGameDir, String instanceId) {
        return new File(new File(baseGameDir, "versions"), instanceId);
    }

    /** 已有 PCL / HMCL 隔离内容目录（versions/<id> 或 versions/<id>/.minecraft）。 */
    private static File isolatedContent(File baseGameDir, String instanceId) {
        if (instanceId == null || instanceId.isBlank()) return null;
        File versionDir = versionDir(baseGameDir, instanceId);
        File nested = new File(versionDir, ".minecraft");
        for (File candidate : new File[]{nested, versionDir}) {
            if (looksLikeGameContent(candidate)) return candidate;
        }
        return null;
    }

    private static boolean looksLikeGameContent(File dir) {
        if (dir == null || !dir.isDirectory()) return false;
        for (String name : new String[]{"mods", "config", "resourcepacks", "shaderpacks", "saves"}) {
            File child = new File(dir, name);
            if (child.isDirectory()) {
                String[] entries = child.list();
                if (entries != null && entries.length > 0) return true;
            }
        }
        for (String name : new String[]{"options.txt", "PCL", "PCL.ini"}) {
            if (new File(dir, name).exists()) return true;
        }
        return false;
    }

    /** 是否已经存在 PCL / HMCL 的版本隔离内容。 */
    public static boolean hasIsolatedContent(File baseGameDir, String instanceId) {
        return isolatedContent(baseGameDir, instanceId) != null;
    }

    public static File root(File baseGameDir, String instanceId, boolean isolated) {
        if (instanceId == null || instanceId.isBlank()) return baseGameDir;
        File existing = isolatedContent(baseGameDir, instanceId);
        if (existing != null) return existing;
        return isolated ? versionDir(baseGameDir, instanceId) : baseGameDir;
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
