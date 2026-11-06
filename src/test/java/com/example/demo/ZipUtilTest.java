package com.example.demo;

import com.example.demo.core.download.ZipUtil;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** zip 打包/解包：路径穿越防护、过滤、目录打包。 */
class ZipUtilTest {

    private File zipWith(Path dir, String... entries) throws Exception {
        File zip = dir.resolve("test.zip").toFile();
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip))) {
            for (String entry : entries) {
                out.putNextEntry(new ZipEntry(entry));
                out.write(("content-of-" + entry).getBytes(StandardCharsets.UTF_8));
                out.closeEntry();
            }
        }
        return zip;
    }

    @Test
    void unzipsNestedEntries(@TempDir Path temp) throws Exception {
        File zip = zipWith(temp, "a.txt", "dir/b.txt", "dir/deep/c.txt");
        File target = temp.resolve("out").toFile();

        int count = ZipUtil.unzip(zip, target);

        assertEquals(3, count);
        assertTrue(new File(target, "a.txt").isFile());
        assertTrue(new File(target, "dir/deep/c.txt").isFile());
    }

    @Test
    void blocksPathTraversalEntries(@TempDir Path temp) throws Exception {
        File zip = zipWith(temp, "ok.txt", "../escape.txt", "..\\escape2.txt", "dir/../../escape3.txt");
        File target = temp.resolve("out").toFile();

        int count = ZipUtil.unzip(zip, target);

        assertEquals(1, count, "只有正常条目会被解出");
        assertFalse(new File(temp.toFile(), "escape.txt").exists(), "不能写到目标目录之外");
        assertFalse(new File(temp.toFile(), "escape2.txt").exists());
        assertFalse(new File(temp.toFile().getParentFile(), "escape3.txt").exists());
        assertTrue(new File(target, "ok.txt").isFile());
    }

    @Test
    void honorsEntryFilter(@TempDir Path temp) throws Exception {
        File zip = zipWith(temp, "keep.jar", "skip.txt");
        File target = temp.resolve("out").toFile();

        int count = ZipUtil.unzip(zip, target, name -> name.endsWith(".jar"));

        assertEquals(1, count);
        assertTrue(new File(target, "keep.jar").isFile());
        assertFalse(new File(target, "skip.txt").exists());
    }

    @Test
    void rejectsMissingArchive(@TempDir Path temp) {
        assertThrows(IOException.class,
                () -> ZipUtil.unzip(temp.resolve("nope.zip").toFile(), temp.resolve("out").toFile()));
    }

    @Test
    void zipsDirectoryAndRoundTrips(@TempDir Path temp) throws Exception {
        Path source = Files.createDirectories(temp.resolve("src/nested"));
        Files.writeString(source.resolve("x.txt"), "x", StandardCharsets.UTF_8);
        Files.writeString(temp.resolve("src/top.txt"), "top", StandardCharsets.UTF_8);
        File zip = temp.resolve("out.zip").toFile();

        ZipUtil.zipDirectory(temp.resolve("src").toFile(), zip);

        File restored = temp.resolve("restored").toFile();
        assertEquals(2, ZipUtil.unzip(zip, restored));
        assertEquals("top", Files.readString(new File(restored, "top.txt").toPath(), StandardCharsets.UTF_8));
        assertTrue(new File(restored, "nested/x.txt").isFile());
    }
}
