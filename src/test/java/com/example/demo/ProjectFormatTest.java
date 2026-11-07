package com.example.demo;

import com.example.demo.core.mod.ProjectFormat;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 资源类型 → 中文标签 / 安装目录 / 是否整合包。 */
class ProjectFormatTest {

    @Test
    void typeLabelTranslatesModrinthTypes() {
        assertEquals("模组", ProjectFormat.typeLabel("mod"));
        assertEquals("资源包", ProjectFormat.typeLabel("resourcepack"));
        assertEquals("光影包", ProjectFormat.typeLabel("shader"));
        assertEquals("数据包", ProjectFormat.typeLabel("datapack"));
        assertEquals("世界", ProjectFormat.typeLabel("world"));
        assertEquals("整合包", ProjectFormat.typeLabel("modpack"));
        assertEquals("资源", ProjectFormat.typeLabel(null));
        assertEquals("资源", ProjectFormat.typeLabel("  "));
    }

    @Test
    void installDirAcceptsEnglishAndChinese() {
        assertEquals("mods", ProjectFormat.installDir("mod"));
        assertEquals("mods", ProjectFormat.installDir("模组"));
        assertEquals("resourcepacks", ProjectFormat.installDir("resourcepack"));
        assertEquals("resourcepacks", ProjectFormat.installDir("资源包"));
        assertEquals("shaderpacks", ProjectFormat.installDir("shader"));
        assertEquals("shaderpacks", ProjectFormat.installDir("光影包"));
        assertEquals("datapacks", ProjectFormat.installDir("datapack"));
        assertEquals("datapacks", ProjectFormat.installDir("数据包"));
        assertEquals("downloads/modpack", ProjectFormat.installDir("modpack"));
        assertEquals("downloads/modpack", ProjectFormat.installDir("整合包"));
        assertEquals("downloads/modpack", ProjectFormat.installDir(null));
        assertEquals("downloads/modpack", ProjectFormat.installDir("something-else"));
    }

    @Test
    void isModpackUnderstandsBothSpellings() {
        assertTrue(ProjectFormat.isModpack("modpack"));
        assertTrue(ProjectFormat.isModpack("ModPack"));
        assertTrue(ProjectFormat.isModpack("整合包"));
        assertFalse(ProjectFormat.isModpack("mod"));
        assertFalse(ProjectFormat.isModpack("数据包"));
        assertFalse(ProjectFormat.isModpack(null));
    }

    @Test
    void countShrinksLargeNumbers() {
        assertEquals("9999", ProjectFormat.count(9_999L));
        assertEquals("1 万", ProjectFormat.count(10_000L));
        assertEquals("2490 万", ProjectFormat.count(24_900_000L));
        assertEquals("2 亿", ProjectFormat.count(200_000_000L));
    }
}
