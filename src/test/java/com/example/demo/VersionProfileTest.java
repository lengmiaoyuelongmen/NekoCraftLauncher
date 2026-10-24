package com.example.demo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 版本档案读取与 inheritsFrom 继承合并。 */
class VersionProfileTest {

    @Test
    void mergesChildProfileWithParent(@TempDir Path temp) throws Exception {
        File gameDir = temp.toFile();
        Path versions = Files.createDirectories(gameDir.toPath().resolve("versions").resolve("1.20.4"));
        Files.createDirectories(gameDir.toPath().resolve("versions").resolve("fabric-test"));

        Files.writeString(versions.resolve("1.20.4.json"), """
                {
                  "id": "1.20.4",
                  "type": "release",
                  "mainClass": "net.minecraft.client.main.Main",
                  "assetIndex": {"id": "12", "url": "https://example.com/12.json"},
                  "javaVersion": {"majorVersion": 17},
                  "libraries": [
                    {"name": "com.example:base:1.0"},
                    {"name": "com.example:only-mac:1.0", "rules": [{"action": "allow", "os": {"name": "osx"}}]}
                  ],
                  "arguments": {
                    "game": ["--username", "${auth_player_name}", "--version", "${version_name}"],
                    "jvm": ["-Djava.library.path=${natives_directory}", "-cp", "${classpath}"]
                  }
                }
                """);

        Files.writeString(gameDir.toPath().resolve("versions").resolve("fabric-test").resolve("fabric-test.json"), """
                {
                  "id": "fabric-test",
                  "inheritsFrom": "1.20.4",
                  "mainClass": "net.fabricmc.loader.impl.launch.knot.KnotClient",
                  "libraries": [{"name": "net.fabricmc:fabric-loader:0.15.0"}],
                  "arguments": {"game": ["--fabric"]}
                }
                """);

        VersionProfile profile = VersionProfile.load(gameDir, "fabric-test");

        assertEquals("fabric-test", profile.getId());
        assertEquals("1.20.4", profile.getRootId(), "根版本应该是原版 1.20.4");
        assertEquals("net.fabricmc.loader.impl.launch.knot.KnotClient", profile.getMainClass(), "子档案覆盖 mainClass");
        assertEquals(17, profile.getJavaMajorVersion(), "Java 版本来自父档案");
        assertEquals("12", profile.getAssetIndexId(), "assetIndex 来自父档案");
        assertEquals("https://example.com/12.json", profile.getAssetIndexUrl());

        // 父库 + 子库；mac 专用库在 Windows 上应被 rules 过滤掉
        assertEquals(2, profile.classpathLibraries().size());
        assertEquals(5, profile.gameArguments().size(), "父 4 个 game 参数 + 子 1 个");
        assertNotNull(profile.jvmArguments());
    }

    @Test
    void rejectsCircularInheritance(@TempDir Path temp) throws Exception {
        Path dir = Files.createDirectories(temp.resolve("versions").resolve("a"));
        Files.createDirectories(temp.resolve("versions").resolve("b"));
        Files.writeString(dir.resolve("a.json"), "{\"id\":\"a\",\"inheritsFrom\":\"b\"}");
        Files.writeString(temp.resolve("versions").resolve("b").resolve("b.json"),
                "{\"id\":\"b\",\"inheritsFrom\":\"a\"}");

        boolean threw = false;
        try {
            VersionProfile.load(temp.toFile(), "a");
        } catch (Exception e) {
            threw = true;
            assertTrue(e.getMessage().contains("循环") || e.getMessage().contains("循环继承"),
                    "错误信息应说明循环继承: " + e.getMessage());
        }
        assertTrue(threw, "循环继承必须报错而不是死循环");
    }
}
