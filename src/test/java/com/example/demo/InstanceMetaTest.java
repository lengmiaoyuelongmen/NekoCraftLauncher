package com.example.demo;

import com.example.demo.core.model.InstanceMeta;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 实例显示名 / 描述：导入整合包时不能覆盖已有实例的名字。 */
class InstanceMetaTest {

    @Test
    void setWithNullNameKeepsExistingName(@TempDir File gameDir) throws Exception {
        InstanceMeta.set(gameDir, "1.12.2", "Multi File Pack", "Modrinth 导入");
        // 导入另一个整合包复用同一实例时，只更新描述
        InstanceMeta.set(gameDir, "1.12.2", null, "Mrpack 导入");
        assertEquals("Multi File Pack", InstanceMeta.name(gameDir, "1.12.2"));
        assertEquals("Mrpack 导入", InstanceMeta.description(gameDir, "1.12.2"));
    }

    @Test
    void setWithNameRenamesInstance(@TempDir File gameDir) {
        InstanceMeta.set(gameDir, "1.20.1", "旧名字", "旧描述");
        InstanceMeta.set(gameDir, "1.20.1", "新名字", "新描述");
        assertEquals("新名字", InstanceMeta.name(gameDir, "1.20.1"));
        assertEquals("新描述", InstanceMeta.description(gameDir, "1.20.1"));
    }

    @Test
    void unknownInstanceFallsBackToId(@TempDir File gameDir) {
        assertEquals("1.7.10", InstanceMeta.name(gameDir, "1.7.10"));
        assertEquals("", InstanceMeta.description(gameDir, "1.7.10"));
        assertFalse(InstanceMeta.isFavorite(gameDir, "1.7.10"));
    }

    @Test
    void favoriteFlagRoundTrips(@TempDir File gameDir) {
        InstanceMeta.set(gameDir, "1.8.9", "PvP", "手装");
        InstanceMeta.setFavorite(gameDir, "1.8.9", true);
        assertTrue(InstanceMeta.isFavorite(gameDir, "1.8.9"));
        InstanceMeta.setFavorite(gameDir, "1.8.9", false);
        assertFalse(InstanceMeta.isFavorite(gameDir, "1.8.9"));
        assertEquals("PvP", InstanceMeta.name(gameDir, "1.8.9"), "改收藏不影响名字");
    }

    @Test
    void fileStaysUtf8Json(@TempDir File gameDir) throws Exception {
        InstanceMeta.set(gameDir, "1.12.2", "测试整合包 TestPack", "Modrinth 导入");
        String raw = Files.readString(new File(gameDir, "instances.json").toPath(),
                StandardCharsets.UTF_8);
        assertTrue(raw.contains("测试整合包 TestPack"), "中文必须按 UTF-8 落盘");
        assertTrue(raw.contains("Modrinth 导入"));
    }
}
