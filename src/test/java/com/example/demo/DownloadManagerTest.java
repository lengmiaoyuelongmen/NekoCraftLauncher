package com.example.demo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 下载/校验引擎。 */
class DownloadManagerTest {

    @Test
    void computesSha1(@TempDir Path temp) throws Exception {
        File file = temp.resolve("hello.txt").toFile();
        Files.writeString(file.toPath(), "hello", StandardCharsets.UTF_8);
        assertEquals("aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d", DownloadManager.sha1(file));
    }

    @Test
    void skipsFilesThatAreAlreadyComplete(@TempDir Path temp) throws Exception {
        File file = temp.resolve("complete.bin").toFile();
        Files.write(file.toPath(), new byte[]{1, 2, 3, 4, 5});
        DownloadManager.Task task = new DownloadManager.Task(
                "http://127.0.0.1:1/never-used", file, null, 5);

        DownloadManager.Stats stats = DownloadManager.downloadAll(List.of(task), 1, null);

        assertEquals(1, stats.skipped, "大小一致的文件不应重新下载");
        assertEquals(0, stats.downloaded);
        assertEquals(0, stats.failed);
    }

    @Test
    void countsFailuresForUnreachableHost(@TempDir Path temp) throws Exception {
        File file = temp.resolve("missing.bin").toFile();
        DownloadManager.Task task = new DownloadManager.Task(
                "http://127.0.0.1:1/nope", file, null, 10);

        DownloadManager.Stats stats = DownloadManager.downloadAll(List.of(task), 1, null);

        assertEquals(1, stats.failed, "连不上应计为失败");
        assertTrue(!file.exists());
    }
}
