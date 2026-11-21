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

/** 定时任务配置的读写，以及 world 备份打包。 */
class ServerMetricsTest {

    @Test
    void scheduleRoundTrips(@TempDir Path temp) {
        File dir = temp.toFile();
        assertFalse(ServerMetrics.readSchedule(dir).anyEnabled(), "没有文件时默认全关");

        ServerMetrics.writeSchedule(dir, new ServerMetrics.Schedule(120, 30));
        ServerMetrics.Schedule loaded = ServerMetrics.readSchedule(dir);
        assertEquals(120, loaded.restartMinutes());
        assertEquals(30, loaded.backupMinutes());
        assertTrue(loaded.anyEnabled());
    }

    @Test
    void brokenScheduleFileFallsBackToDisabled(@TempDir Path temp) throws Exception {
        File dir = temp.toFile();
        Files.writeString(new File(dir, "neko-schedule.json").toPath(), "{ not json", StandardCharsets.UTF_8);
        assertFalse(ServerMetrics.readSchedule(dir).anyEnabled(), "坏文件不能让面板崩");
    }

    @Test
    void backupZipsTheWorldFolder(@TempDir Path temp) throws Exception {
        File dir = temp.toFile();
        File world = new File(dir, "world");
        assertTrue(world.mkdirs());
        Files.writeString(new File(world, "level.dat").toPath(), "not-really-nbt", StandardCharsets.UTF_8);
        Files.writeString(new File(world, "session.lock").toPath(), "1", StandardCharsets.UTF_8);

        File zip = ServerMetrics.backupWorld(dir);
        assertTrue(zip.isFile(), "备份文件应存在");
        assertTrue(zip.length() > 0, "备份不能是空包");
        assertTrue(zip.getName().startsWith("world-") && zip.getName().endsWith(".zip"));
        assertTrue(new File(dir, "backups").isDirectory());
    }

    @Test
    void backupWithoutWorldFailsWithMessage(@TempDir Path temp) {
        try {
            ServerMetrics.backupWorld(temp.toFile());
            throw new AssertionError("没有 world 目录时应当抛异常");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("world"), "错误信息要能看懂：" + e.getMessage());
        }
    }
}
