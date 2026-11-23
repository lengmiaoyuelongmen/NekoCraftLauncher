package com.example.demo;

import com.example.demo.core.app.UpdateChecker;
import com.example.demo.core.app.UpdateInstaller;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 跨版本提醒的版本差计算，以及备份列表的列举/占用/删除。 */
class UpdateGapAndBackupTest {

    @Test
    void versionGapCountsMinorSteps() {
        assertEquals(0, UpdateChecker.versionGap("2.46.0", "2.46.0"));
        assertEquals(1, UpdateChecker.versionGap("2.47.0", "2.46.0"));
        assertEquals(5, UpdateChecker.versionGap("2.51.3", "2.46.0"));
        // 大版本跳跃按 100 粗算，保证一定超过「3 个小版本」的门槛
        assertEquals(100, UpdateChecker.versionGap("3.0.0", "2.46.0"));
    }

    @Test
    void backupsAreListedNewestFirstAndDeletable(@TempDir Path ignored) throws Exception {
        File root = UpdateInstaller.backupRoot();
        File older = new File(root, "app-19700101-000000");
        File newer = new File(root, "app-29990101-000000");
        assertTrue(older.mkdirs());
        assertTrue(newer.mkdirs());
        Files.writeString(new File(older, "payload.bin").toPath(), "1234567890",
                StandardCharsets.UTF_8);
        older.setLastModified(System.currentTimeMillis() - 60_000L);
        newer.setLastModified(System.currentTimeMillis());

        java.util.List<File> listed = UpdateInstaller.listBackups();
        assertTrue(listed.contains(older) && listed.contains(newer));
        assertTrue(listed.indexOf(newer) < listed.indexOf(older),
                "应当新到旧排序: " + listed);

        assertEquals(10L, UpdateInstaller.backupSize(older));
        assertTrue(UpdateInstaller.deleteBackup(older));
        assertFalse(older.exists());
        assertFalse(UpdateInstaller.listBackups().contains(older));

        assertTrue(UpdateInstaller.deleteBackup(newer));
    }
}
