package com.example.demo;

import com.example.demo.core.game.JavaRuntimeInstaller;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** JDK 压缩包安装：剥掉顶层目录、校验 java.exe、sha256。 */
class JavaRuntimeInstallerTest {

    private File zip(Path dir, String entryPrefix) throws Exception {
        File zip = dir.resolve("jdk.zip").toFile();
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip))) {
            out.putNextEntry(new ZipEntry(entryPrefix + "bin/java.exe"));
            out.write("fake-java".getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
            out.putNextEntry(new ZipEntry(entryPrefix + "release"));
            out.write("JAVA_VERSION=\"21\"".getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }
        return zip;
    }

    @Test
    void stripsSingleTopLevelDirectory(@TempDir Path temp) throws Exception {
        File gameDir = Files.createDirectories(temp.resolve("game")).toFile();
        File zip = zip(gameDir.toPath(), "jdk-21.0.13+9/");
        File runtimeDir = new File(gameDir, "java/zulu21");

        File javaExe = JavaRuntimeInstaller.install(zip, runtimeDir);

        assertEquals(new File(runtimeDir, "bin/java.exe"), javaExe);
        assertTrue(javaExe.isFile(), "java.exe 必须落在 <runtime>/bin 下");
        assertTrue(new File(runtimeDir, "release").isFile(), "release 文件也要被保留");
        assertTrue(!new File(runtimeDir, "jdk-21.0.13+9").exists(), "顶层目录应被剥掉");
        assertTrue(!new File(gameDir, "java/zulu21.tmp").exists(), "临时目录要清理掉");
    }

    @Test
    void handlesZipWithoutRootDirectory(@TempDir Path temp) throws Exception {
        File gameDir = Files.createDirectories(temp.resolve("game")).toFile();
        File zip = zip(gameDir.toPath(), "");
        File runtimeDir = new File(gameDir, "java/zulu21");

        File javaExe = JavaRuntimeInstaller.install(zip, runtimeDir);

        assertTrue(javaExe.isFile());
    }

    @Test
    void rejectsZipWithoutJavaExe(@TempDir Path temp) throws Exception {
        File gameDir = Files.createDirectories(temp.resolve("game")).toFile();
        File zip = gameDir.toPath().resolve("bad.zip").toFile();
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip))) {
            out.putNextEntry(new ZipEntry("readme.txt"));
            out.write("nope".getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }

        assertThrows(IOException.class,
                () -> JavaRuntimeInstaller.install(zip, new File(gameDir, "java/zulu21")));
    }

    @Test
    void computesSha256(@TempDir Path temp) throws Exception {
        File file = temp.resolve("payload.bin").toFile();
        Files.writeString(file.toPath(), "hello", StandardCharsets.UTF_8);

        // sha256("hello")
        assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
                JavaRuntimeInstaller.sha256(file));
    }
}
