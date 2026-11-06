package com.example.demo.core.model;

import java.io.File;

/**
 * 从一个目录里找 Minecraft 根目录（带 versions/*.json 的那一层）。
 *
 * <p>用户的目录结构五花八门：直接选 {@code .minecraft}、选它上一级、选一个装着多个整合包的父目录，
 * 都要能认出来。顺序：自己 → 自己的 {@code .minecraft} → 子目录 → 子目录的 {@code .minecraft}。
 */
public final class GameDirDetector {

    private GameDirDetector() {
    }

    /** @return 识别出的游戏根目录；不是 Minecraft 目录时返回 {@code null} */
    public static File detect(File selected) {
        if (selected == null || !selected.isDirectory()) {
            return null;
        }
        for (File candidate : new File[]{selected, new File(selected, ".minecraft")}) {
            if (hasVersions(candidate)) {
                return candidate;
            }
        }
        File[] children = selected.listFiles(File::isDirectory);
        if (children != null) {
            for (File child : children) {
                if (hasVersions(child)) {
                    return child;
                }
                File dot = new File(child, ".minecraft");
                if (hasVersions(dot)) {
                    return dot;
                }
            }
        }
        return null;
    }

    /** 该目录下是否存在「版本目录 + 版本 JSON」的实例。 */
    public static boolean hasVersions(File dir) {
        if (dir == null) {
            return false;
        }
        File versions = new File(dir, "versions");
        if (!versions.isDirectory()) {
            return false;
        }
        File[] subs = versions.listFiles(File::isDirectory);
        if (subs == null) {
            return false;
        }
        for (File sub : subs) {
            if (new File(sub, sub.getName() + ".json").isFile()) {
                return true;
            }
            File[] jsons = sub.listFiles(f -> f.isFile() && f.getName().endsWith(".json"));
            if (jsons != null && jsons.length > 0) {
                return true;
            }
        }
        return false;
    }
}
