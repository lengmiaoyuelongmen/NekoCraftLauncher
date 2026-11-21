package com.example.demo;

import com.example.demo.core.server.ServerMetrics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 备份列表与恢复：恢复要先保住旧世界，再解压新世界。 */
class ServerBackupTest {

    @Test
    void restoreReplacesWorldAndKeepsTheOldOneAside(@TempDir Path temp) throws Exception {
        File dir = temp.toFile();
        File world = new File(dir, "world");
        assertTrue(world.mkdirs());
        Files.writeString(new File(world, "level.dat").toPath(), "old", StandardCharsets.UTF_8);
        File zip = ServerMetrics.backupWorld(dir);
        assertTrue(zip.isFile());

        Files.writeString(new File(world, "level.dat").toPath(), "changed", StandardCharsets.UTF_8);
        ServerMetrics.restoreBackup(dir, zip);

        String restored = Files.readString(new File(world, "level.dat").toPath(), StandardCharsets.UTF_8);
        assertEquals("old", restored, "恢复后应当是备份里的内容");

        File[] aside = dir.listFiles((d, n) -> n.startsWith("world.before-restore-"));
        assertTrue(aside != null && aside.length == 1, "旧世界要改名保留，不能直接删");
    }

    @Test
    void listBackupsIsNewestFirst(@TempDir Path temp) throws Exception {
        File dir = temp.toFile();
        File world = new File(dir, "world");
        assertTrue(world.mkdirs());
        Files.writeString(new File(world, "a.txt").toPath(), "1", StandardCharsets.UTF_8);
        File first = ServerMetrics.backupWorld(dir);
        Thread.sleep(1100);
        File second = ServerMetrics.backupWorld(dir);

        var backups = ServerMetrics.listBackups(dir);
        assertEquals(2, backups.size());
        assertEquals(second.getName(), backups.get(0).getName());
        assertEquals(first.getName(), backups.get(1).getName());
    }

    @Test
    void missingZipIsRejected(@TempDir Path temp) {
        try {
            ServerMetrics.restoreBackup(temp.toFile(), new File(temp.toFile(), "nope.zip"));
            throw new AssertionError("不存在的备份应当被拒绝");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("备份"), e.getMessage());
        }
    }
}