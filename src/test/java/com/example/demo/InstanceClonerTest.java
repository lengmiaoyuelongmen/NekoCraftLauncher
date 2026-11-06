package com.example.demo;

import com.example.demo.core.instance.InstanceCloner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 克隆实例：命名、版本 JSON 改写、是否带数据。 */
class InstanceClonerTest {

    private File gameDir(Path temp) throws Exception {
        Path version = Files.createDirectories(temp.resolve("versions/1.20.1"));
        Files.writeString(version.resolve("1.20.1.json"), """
                {"id": "1.20.1", "type": "release", "mainClass": "net.minecraft.client.main.Main"}
                """);
        Files.write(version.resolve("1.20.1.jar"), new byte[]{1, 2, 3});
        Path mods = Files.createDirectories(version.resolve("mods"));
        Files.writeString(mods.resolve("sodium.jar"), "mod");
        Path saves = Files.createDirectories(version.resolve("saves/world"));
        Files.writeString(saves.resolve("level.dat"), "data");
        return temp.toFile();
    }

    @Test
    void clonesVersionOnlyByDefault(@TempDir Path temp) throws Exception {
        File game = gameDir(temp);

        File target = InstanceCloner.duplicate(game, "1.20.1", false);

        assertEquals("1.20.1-copy", target.getName());
        assertTrue(new File(target, "1.20.1-copy.json").isFile(), "版本 JSON 应改名");
        assertTrue(new File(target, "1.20.1.jar").isFile(), "客户端 jar 要复制");
        assertFalse(new File(target, "mods").exists(), "默认不复制模组");
        assertFalse(new File(target, "saves").exists(), "默认不复制存档");

        String json = Files.readString(new File(target, "1.20.1-copy.json").toPath(), StandardCharsets.UTF_8);
        assertTrue(json.contains("\"id\": \"1.20.1-copy\""), "JSON 里的 id 必须改成新实例: " + json);
        assertFalse(new File(target, "1.20.1.json").exists(), "旧名字的 JSON 不应留下");
    }

    @Test
    void copiesDataWhenAsked(@TempDir Path temp) throws Exception {
        File game = gameDir(temp);

        File target = InstanceCloner.duplicate(game, "1.20.1", true);

        assertTrue(new File(target, "mods/sodium.jar").isFile(), "带数据时要复制模组");
        assertTrue(new File(target, "saves/world/level.dat").isFile(), "带数据时要复制存档");
    }

    @Test
    void picksFreeNameOnCollision(@TempDir Path temp) throws Exception {
        File game = gameDir(temp);

        File first = InstanceCloner.duplicate(game, "1.20.1", false);
        File second = InstanceCloner.duplicate(game, "1.20.1", false);

        assertEquals("1.20.1-copy", first.getName());
        assertEquals("1.20.1-copy2", second.getName(), "重名时自动加序号");
    }
}
