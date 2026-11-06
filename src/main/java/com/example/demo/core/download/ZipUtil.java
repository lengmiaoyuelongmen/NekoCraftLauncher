package com.example.demo.core.download;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * 目录打包 / 解包工具（导出日志、导出实例、安装 Java 运行时共用）。
 *
 * <p>解包一律做 <b>zip-slip 防护</b>：条目规范化后必须仍落在目标目录内，否则跳过。
 * 这不是洁癖 —— 整合包/第三方压缩包里带 {@code ../} 条目是真实存在的攻击面。
 */
public final class ZipUtil {

    private ZipUtil() {
    }

    /** 把目录打成 zip（保留相对路径）。 */
    public static void zipDirectory(File dir, File out) throws IOException {
        Path base = dir.toPath();
        List<Path> paths = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(base)) {
            stream.filter(Files::isRegularFile).forEach(paths::add);
        }
        try (OutputStream raw = Files.newOutputStream(out.toPath());
             ZipOutputStream zos = new ZipOutputStream(raw)) {
            for (Path path : paths) {
                String entryName = base.relativize(path).toString().replace('\\', '/');
                zos.putNextEntry(new ZipEntry(entryName));
                Files.copy(path, zos);
                zos.closeEntry();
            }
        }
    }

    /** 解包全部条目。 */
    public static int unzip(File zip, File targetDir) throws IOException {
        return unzip(zip, targetDir, name -> true);
    }

    /**
     * 解包，只保留 {@code filter} 认可的条目。
     *
     * @return 实际写出的文件数
     */
    public static int unzip(File zip, File targetDir, Predicate<String> filter) throws IOException {
        if (zip == null || !zip.isFile()) {
            throw new IOException("压缩包不存在: " + zip);
        }
        Path base = targetDir.toPath().toAbsolutePath().normalize();
        if (!Files.isDirectory(base)) {
            Files.createDirectories(base);
        }
        int written = 0;
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zip.toPath()))) {
            ZipEntry entry;
            byte[] buffer = new byte[16384];
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName().replace('\\', '/');
                if (name.isBlank() || !filter.test(name)) {
                    continue;
                }
                Path target = base.resolve(name).normalize();
                // zip-slip 防护：解析后的路径必须还在目标目录里
                if (!target.startsWith(base)) {
                    continue;
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                    continue;
                }
                Path parent = target.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                try (FileOutputStream out = new FileOutputStream(target.toFile())) {
                    int read;
                    while ((read = zis.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                }
                written++;
            }
        }
        return written;
    }
}
