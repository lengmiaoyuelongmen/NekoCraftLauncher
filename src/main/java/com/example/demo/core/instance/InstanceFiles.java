package com.example.demo.core.instance;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Stream;

/** 实例目录的文件操作：递归删除、带过滤的复制。纯文件逻辑，可单测。 */
public final class InstanceFiles {

    private InstanceFiles() {
    }

    /**
     * 递归删除目录（或文件）。删除失败（文件被占用）返回 {@code false}，不抛异常。
     *
     * @return true 表示目标已不存在
     */
    public static boolean deleteTree(File target) {
        if (target == null || !target.exists()) {
            return true;
        }
        try (Stream<Path> walk = Files.walk(target.toPath())) {
            for (Path path : walk.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        } catch (Exception ignored) {
            // 删除失败时以最终状态为准
        }
        return !target.exists();
    }

    /**
     * 复制目录树（覆盖同名文件）。
     *
     * @param skipTopLevel 需要跳过的顶层目录名（例如只复制版本、不复制 mods/saves）
     */
    public static void copyTree(File from, File to, Set<String> skipTopLevel) throws IOException {
        if (from == null || !from.isDirectory()) {
            throw new IOException("源目录不存在: " + from);
        }
        Path base = from.toPath();
        try (Stream<Path> walk = Files.walk(base)) {
            for (Path path : walk.toList()) {
                Path relative = base.relativize(path);
                if (!relative.toString().isEmpty()
                        && skipTopLevel != null
                        && relative.getNameCount() > 0
                        && skipTopLevel.contains(relative.getName(0).toString())) {
                    continue;
                }
                Path target = to.toPath().resolve(relative);
                if (Files.isDirectory(path)) {
                    Files.createDirectories(target);
                } else {
                    Path parent = target.getParent();
                    if (parent != null) {
                        Files.createDirectories(parent);
                    }
                    Files.copy(path, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }
}
