package com.example.demo;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** 目录打包工具（导出日志 / 导出实例共用）。 */
public final class ZipUtil {

    private ZipUtil() {
    }

    public static void zipDirectory(File dir, File out) throws Exception {
        Path base = dir.toPath();
        List<Path> paths = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(base)) {
            stream.filter(Files::isRegularFile).forEach(paths::add);
        }
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(out))) {
            for (Path path : paths) {
                String entryName = base.relativize(path).toString().replace('\\', '/');
                zos.putNextEntry(new ZipEntry(entryName));
                Files.copy(path, zos);
                zos.closeEntry();
            }
        }
    }
}
