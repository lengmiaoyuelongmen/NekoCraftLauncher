package com.example.demo;

import com.example.demo.core.download.DownloadManager;
import com.example.demo.core.download.Integrity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 完整性检查：缺失 / 大小不符 / SHA1 不符，以及深度校验开关。 */
class IntegrityTest {

    private static final String LIB_PATH = "com/example/lib/1.0/lib-1.0.jar";

    /** 造一个只有一条库依赖的最小游戏目录，返回库文件。 */
    private File gameDir(Path temp, String content) throws Exception {
        File game = temp.toFile();
        Path versionDir = Files.createDirectories(temp.resolve("versions").resolve("1.20.1"));
        Path lib = temp.resolve("libraries").resolve(LIB_PATH);
        Files.createDirectories(lib.getParent());
        Files.writeString(lib, content, StandardCharsets.UTF_8);
        String sha1 = DownloadManager.sha1(lib.toFile());
        long size = Files.size(lib);

        String json = """
                {
                  "id": "1.20.1",
                  "type": "release",
                  "mainClass": "net.minecraft.client.main.Main",
                  "assetIndex": {"id": "12", "url": "https://example.com/12.json"},
                  "downloads": {"client": {"url": "https://example.com/client.jar"}},
                  "libraries": [
                    {"name": "com.example:lib:1.0",
                     "downloads": {"artifact": {"path": "%s",
                       "url": "https://example.com/lib.jar", "sha1": "%s", "size": %d}}}
                  ]
                }
                """.formatted(LIB_PATH, sha1, size);
        Files.writeString(versionDir.resolve("1.20.1.json"), json, StandardCharsets.UTF_8);
        return lib.toFile();
    }

    private List<Integrity.Issue> libraryIssues(Integrity.Report report) {
        return report.issues().stream()
                .filter(i -> i.kind() == Integrity.Kind.LIBRARY)
                .toList();
    }

    @Test
    void healthyFileHasNoIssue(@TempDir Path temp) throws Exception {
        gameDir(temp, "good-content");

        Integrity.Report report = Integrity.check(temp.toFile(), "1.20.1", 0, true);

        assertEquals(List.of(), libraryIssues(report), "内容正确时不应报库文件问题");
    }

    @Test
    void missingFileIsReported(@TempDir Path temp) throws Exception {
        File lib = gameDir(temp, "good-content");
        Files.delete(lib.toPath());

        Integrity.Report report = Integrity.check(temp.toFile(), "1.20.1", 0, true);

        List<Integrity.Issue> issues = libraryIssues(report);
        assertEquals(1, issues.size());
        assertEquals(Integrity.Reason.MISSING, issues.get(0).reason());
        assertEquals("库文件", issues.get(0).label());
    }

    @Test
    void sizeMismatchIsReported(@TempDir Path temp) throws Exception {
        File lib = gameDir(temp, "good-content");
        Files.writeString(lib.toPath(), "short", StandardCharsets.UTF_8);

        List<Integrity.Issue> issues = libraryIssues(Integrity.check(temp.toFile(), "1.20.1", 0, false));

        assertEquals(1, issues.size());
        assertEquals(Integrity.Reason.SIZE, issues.get(0).reason());
        assertTrue(issues.get(0).label().contains("大小不符"), issues.get(0).label());
    }

    @Test
    void sameSizeDifferentContentOnlyCaughtBySha1(@TempDir Path temp) throws Exception {
        File lib = gameDir(temp, "good-content");
        // 长度相同、内容不同：只比大小是发现不了的
        Files.writeString(lib.toPath(), "evil-content", StandardCharsets.UTF_8);
        assertEquals("good-content".length(), "evil-content".length());

        List<Integrity.Issue> shallow = libraryIssues(Integrity.check(temp.toFile(), "1.20.1", 0, false));
        assertEquals(List.of(), shallow, "只比大小时这种损坏查不出来（旧行为）");

        List<Integrity.Issue> deep = libraryIssues(Integrity.check(temp.toFile(), "1.20.1", 0, true));
        assertEquals(1, deep.size(), "深度校验必须发现 SHA1 不符");
        assertEquals(Integrity.Reason.SHA1, deep.get(0).reason());
        assertTrue(deep.get(0).label().contains("SHA1 不符"), deep.get(0).label());
    }

    @Test
    void emptyFileCountsAsMissing(@TempDir Path temp) throws Exception {
        File lib = gameDir(temp, "good-content");
        Files.write(lib.toPath(), new byte[0]);

        List<Integrity.Issue> issues = libraryIssues(Integrity.check(temp.toFile(), "1.20.1", 0, false));

        assertEquals(1, issues.size());
        assertEquals(Integrity.Reason.MISSING, issues.get(0).reason(), "0 字节视为缺失");
    }
}
