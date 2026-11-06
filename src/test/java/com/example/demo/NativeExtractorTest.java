package com.example.demo;

import com.example.demo.core.launch.NativeExtractor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 原生库解压：只取 dll、已存在不重复解压、按库名分目录。 */
class NativeExtractorTest {

    /** 造一个只有 dll + 一个无关文件的 natives jar。 */
    private File nativesJar(Path dir) throws Exception {
        File jar = dir.resolve("lwjgl-natives.jar").toFile();
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(jar))) {
            out.putNextEntry(new ZipEntry("lwjgl.dll"));
            out.write(new byte[]{1, 2, 3});
            out.closeEntry();
            out.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
            out.write("x".getBytes());
            out.closeEntry();
        }
        return jar;
    }

    @Test
    void extractsOnlyDlls(@TempDir Path temp) throws Exception {
        File jar = nativesJar(temp);
        File target = Files.createDirectories(temp.resolve("natives")).toFile();

        assertEquals(1, NativeExtractor.extractDlls(jar, target));
        File dll = new File(target, "lwjgl.dll");
        assertTrue(dll.isFile(), "dll 应被解出");
        assertEquals(3, dll.length());
        assertFalse(new File(target, "MANIFEST.MF").exists(), "非 dll 文件不应解出");
    }

    @Test
    void skipsAlreadyExtracted(@TempDir Path temp) throws Exception {
        File jar = nativesJar(temp);
        File target = Files.createDirectories(temp.resolve("natives")).toFile();

        assertEquals(1, NativeExtractor.extractDlls(jar, target));
        assertEquals(0, NativeExtractor.extractDlls(jar, target), "同尺寸文件不应重复解压");
    }

    @Test
    void groupsByLibraryName(@TempDir Path temp) {
        File root = temp.toFile();
        assertEquals("lwjgl",
                NativeExtractor.subDir(root,
                        "org/lwjgl/lwjgl/3.3.1/lwjgl-3.3.1-natives-windows.jar").getName());
        assertEquals("jna",
                NativeExtractor.subDir(root,
                        "net/java/dev/jna/jna/5.12.1/jna-5.12.1-natives-windows.jar").getName());
        assertEquals("netty",
                NativeExtractor.subDir(root,
                        "io/netty/netty-transport-native-epoll/4.1/netty-transport-native-epoll-natives-windows.jar")
                        .getName());
        assertEquals("java",
                NativeExtractor.subDir(root, "org/example/foo/1.0/foo-natives-windows.jar").getName());
    }
}
