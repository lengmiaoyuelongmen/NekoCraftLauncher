package com.example.demo;

import com.example.demo.core.model.InstanceSettings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 实例设置：跟着实例走的 instance.json、旧版迁移、隔离覆盖。 */
class InstanceSettingsTest {

    private File instanceDir(Path temp, String id) throws Exception {
        return Files.createDirectories(temp.resolve("versions").resolve(id)).toFile();
    }

    @Test
    void savesIntoInstanceDirectory(@TempDir Path temp) throws Exception {
        String id = "1.20.1";
        instanceDir(temp, id);

        InstanceSettings.save(temp.toFile(), id, 8192, "-XX:+UseG1GC", "C:\\\\jdk21\\\\bin\\\\java.exe");

        File file = InstanceSettings.file(temp.toFile(), id);
        assertTrue(file.isFile(), "设置应写在 versions/<id>/instance.json");
        assertEquals("instance.json", file.getName());

        InstanceSettings.Settings settings = InstanceSettings.get(temp.toFile(), id);
        assertEquals(8192, settings.memoryMb());
        assertEquals("-XX:+UseG1GC", settings.jvmArgs());
        assertEquals("C:\\\\jdk21\\\\bin\\\\java.exe", settings.javaPath());
        assertNull(settings.isolation(), "没设过隔离就该跟随全局");
        assertFalse(settings.isEmpty());
    }

    @Test
    void clearingAllValuesRemovesFile(@TempDir Path temp) throws Exception {
        String id = "1.12.2";
        instanceDir(temp, id);
        InstanceSettings.save(temp.toFile(), id, 4096, "-Xmx4096M", "");
        assertTrue(InstanceSettings.file(temp.toFile(), id).isFile());

        InstanceSettings.save(temp.toFile(), id, null, "", "");

        assertFalse(InstanceSettings.file(temp.toFile(), id).isFile(), "全空时不应留下空设置文件");
        assertTrue(InstanceSettings.get(temp.toFile(), id).isEmpty());
    }

    @Test
    void migratesLegacyGlobalFile(@TempDir Path temp) throws Exception {
        String id = "1.18.2";
        instanceDir(temp, id);
        File legacy = temp.resolve("instance-settings.json").toFile();
        Files.writeString(legacy.toPath(),
                "{\"1.18.2\":{\"memoryMb\":6144,\"jvmArgs\":\"-XX:+UseZGC\",\"javaPath\":\"C:\\\\zulu17\\\\java.exe\"}}",
                StandardCharsets.UTF_8);

        InstanceSettings.Settings settings = InstanceSettings.get(temp.toFile(), id);

        assertEquals(6144, settings.memoryMb(), "旧版设置必须能读出来");
        assertEquals("-XX:+UseZGC", settings.jvmArgs());
        assertTrue(InstanceSettings.file(temp.toFile(), id).isFile(), "读取时自动迁移到实例目录");
        assertFalse(legacy.isFile(), "迁移完旧表应被清空删除");
    }

    @Test
    void isolationOverrideWinsOverGlobal(@TempDir Path temp) throws Exception {
        String id = "26.3";
        instanceDir(temp, id);

        assertTrue(InstanceSettings.effectiveIsolation(temp.toFile(), id, true), "没覆盖时跟随全局");
        assertFalse(InstanceSettings.effectiveIsolation(temp.toFile(), id, false));

        InstanceSettings.saveIsolation(temp.toFile(), id, true);
        assertTrue(InstanceSettings.effectiveIsolation(temp.toFile(), id, false), "实例覆盖优先");

        InstanceSettings.saveIsolation(temp.toFile(), id, false);
        assertFalse(InstanceSettings.effectiveIsolation(temp.toFile(), id, true), "实例可以强制不隔离");

        InstanceSettings.saveIsolation(temp.toFile(), id, null);
        assertTrue(InstanceSettings.effectiveIsolation(temp.toFile(), id, true), "清掉覆盖后回到全局");
    }

    @Test
    void savesGameArguments(@TempDir Path temp) throws Exception {
        String id = "1.20.4";
        instanceDir(temp, id);

        InstanceSettings.save(temp.toFile(), id, 2048, "", "", "--width 1920 --height 1080");

        InstanceSettings.Settings settings = InstanceSettings.get(temp.toFile(), id);
        assertEquals("--width 1920 --height 1080", settings.gameArgs());
        assertEquals(2048, settings.memoryMb(), "保存游戏参数不能把内存弄丢");
    }

    @Test
    void savingLaunchSettingsKeepsIsolationOverride(@TempDir Path temp) throws Exception {
        String id = "1.7.10";
        instanceDir(temp, id);
        InstanceSettings.saveIsolation(temp.toFile(), id, true);

        InstanceSettings.save(temp.toFile(), id, 2048, "-Xmx2048M", "");

        InstanceSettings.Settings settings = InstanceSettings.get(temp.toFile(), id);
        assertEquals(2048, settings.memoryMb());
        assertEquals(Boolean.TRUE, settings.isolation(), "改内存不能把隔离覆盖弄丢");
    }
}
