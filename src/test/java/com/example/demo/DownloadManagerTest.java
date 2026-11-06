package com.example.demo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.example.demo.core.download.DownloadManager;
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

    @Test
    void verifySha1ForcesRedownloadOfSameSizeCorruptFile(@TempDir Path temp) throws Exception {
        File file = temp.resolve("index.json").toFile();
        Files.writeString(file.toPath(), "{}", StandardCharsets.UTF_8);
        long size = file.length();
        // 期望的是别的内容：大小一样、SHA1 不同
        String expected = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

        DownloadManager.Task plain = new DownloadManager.Task(
                "http://127.0.0.1:1/x", file, expected, size);
        assertEquals(1, DownloadManager.downloadAll(List.of(plain), 1, null).skipped,
                "默认只比大小，同尺寸文件会被当成完整");

        DownloadManager.Task strict = new DownloadManager.Task(
                "http://127.0.0.1:1/x", file, expected, size, true);
        DownloadManager.Stats stats = DownloadManager.downloadAll(List.of(strict), 1, null);
        assertEquals(0, stats.skipped, "verifySha1=true 时不能因为大小一致就跳过");
        assertEquals(1, stats.failed, "下载不成功应计失败");
    }
}
