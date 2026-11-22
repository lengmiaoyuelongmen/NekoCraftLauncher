package com.example.demo;

import com.example.demo.core.model.InstanceSettingsBundle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 实例设置包：导出只带有覆盖的实例，导入能还原且跳过未知实例。 */
class InstanceSettingsBundleTest {

    private static File versionDir(Path gameDir, String id) throws Exception {
        File dir = new File(gameDir.toFile(), "versions/" + id);
        assertTrue(dir.mkdirs());
        return dir;
    }

    @Test
    void exportThenImportRoundTrips(@TempDir Path temp) throws Exception {
        Path game = temp.resolve("game");
        File a = versionDir(game, "alpha");
        Files.writeString(new File(a, "instance.json").toPath(),
                "{\"memoryMb\":6144,\"jvmArgs\":\"-XX:+UseG1GC\"}", StandardCharsets.UTF_8);
        versionDir(game, "beta"); // 没有 instance.json，应当被跳过

        File zip = temp.resolve("bundle.zip").toFile();
        var exported = InstanceSettingsBundle.exportAll(game.toFile(), zip);
        assertEquals(1, exported.exported());
        assertEquals(1, exported.skipped());
        assertTrue(zip.isFile() && zip.length() > 0);

        // 删掉原文件再用包还原
        assertTrue(new File(a, "instance.json").delete());
        var imported = InstanceSettingsBundle.importAll(game.toFile(), zip);
        assertEquals(1, imported.exported());
        String restored = Files.readString(new File(a, "instance.json").toPath(), StandardCharsets.UTF_8);
        assertTrue(restored.contains("6144"), restored);
    }

    @Test
    void importSkipsInstancesThisMachineDoesNotHave(@TempDir Path temp) throws Exception {
        Path game = temp.resolve("game");
        File a = versionDir(game, "alpha");
        Files.writeString(new File(a, "instance.json").toPath(), "{\"memoryMb\":2048}",
                StandardCharsets.UTF_8);
        File zip = temp.resolve("bundle.zip").toFile();
        InstanceSettingsBundle.exportAll(game.toFile(), zip);

        Path other = temp.resolve("other");
        versionDir(other, "gamma");
        var result = InstanceSettingsBundle.importAll(other.toFile(), zip);
        assertEquals(0, result.exported());
        assertEquals(1, result.skipped());
    }
}
