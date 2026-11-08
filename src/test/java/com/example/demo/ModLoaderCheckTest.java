package com.example.demo;

import com.example.demo.core.mod.ModLoaderCheck;
import com.example.demo.core.mod.VersionFit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 模组加载器家族识别与实例匹配判断。 */
class ModLoaderCheckTest {

    private static File jar(Path dir, String name, String entry, String content) throws Exception {
        Path file = dir.resolve(name);
        try (OutputStream out = Files.newOutputStream(file);
             ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry(entry));
            zip.write(content.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return file.toFile();
    }

    @Test
    void detectsFamilies(@TempDir Path dir) throws Exception {
        assertEquals(ModLoaderCheck.Family.FABRIC,
                ModLoaderCheck.detect(jar(dir, "a.jar", "fabric.mod.json", "{}")));
        assertEquals(ModLoaderCheck.Family.QUILT,
                ModLoaderCheck.detect(jar(dir, "b.jar", "quilt.mod.json", "{}")));
        assertEquals(ModLoaderCheck.Family.FORGE,
                ModLoaderCheck.detect(jar(dir, "c.jar", "META-INF/mods.toml", "modLoader=\"javafml\"")));
        assertEquals(ModLoaderCheck.Family.FORGE,
                ModLoaderCheck.detect(jar(dir, "d.jar", "mcmod.info", "[]")));
        assertEquals(ModLoaderCheck.Family.NEOFORGE,
                ModLoaderCheck.detect(jar(dir, "f.jar", "META-INF/neoforge.mods.toml", "x")));
        assertEquals(ModLoaderCheck.Family.UNKNOWN,
                ModLoaderCheck.detect(jar(dir, "e.jar", "com/example/Main.class", "x")));
    }

    @Test
    void compatibilityMatrix() {
        assertTrue(ModLoaderCheck.compatible(ModLoaderCheck.Family.FABRIC, VersionFit.Loader.FABRIC));
        assertTrue(ModLoaderCheck.compatible(ModLoaderCheck.Family.FABRIC, VersionFit.Loader.QUILT),
                "Quilt 能跑 Fabric 模组");
        assertFalse(ModLoaderCheck.compatible(ModLoaderCheck.Family.FABRIC, VersionFit.Loader.FORGE));
        assertFalse(ModLoaderCheck.compatible(ModLoaderCheck.Family.FORGE, VersionFit.Loader.NEOFORGE),
                "Forge 模组不能进 NeoForge");
        assertTrue(ModLoaderCheck.compatible(ModLoaderCheck.Family.NEOFORGE, VersionFit.Loader.NEOFORGE));
        assertFalse(ModLoaderCheck.compatible(ModLoaderCheck.Family.NEOFORGE, VersionFit.Loader.FORGE));
        assertFalse(ModLoaderCheck.compatible(ModLoaderCheck.Family.FORGE, VersionFit.Loader.VANILLA),
                "原版加载不了任何模组");
        assertTrue(ModLoaderCheck.compatible(ModLoaderCheck.Family.UNKNOWN, VersionFit.Loader.FORGE),
                "认不出来的不误报");
    }

    @Test
    void scanSkipsDisabledAndMatchesOnlyWrongOnes(@TempDir Path dir) throws Exception {
        jar(dir, "sodium.jar", "fabric.mod.json", "{}");
        jar(dir, "jei.jar", "META-INF/mods.toml", "x");
        jar(dir, "old.jar.disabled", "META-INF/mods.toml", "x");

        List<ModLoaderCheck.Finding> wrong =
                ModLoaderCheck.scan(dir.toFile(), VersionFit.Loader.FORGE);
        assertEquals(1, wrong.size(), "只报启用的 Fabric 模组");
        assertEquals("sodium.jar", wrong.get(0).fileName());
        assertEquals(ModLoaderCheck.Family.FABRIC, wrong.get(0).family());

        assertEquals(1, ModLoaderCheck.scan(dir.toFile(), VersionFit.Loader.FABRIC).size(),
                "Fabric 实例里那个 Forge 模组同样要报出来");
        assertEquals(1, ModLoaderCheck.scan(dir.toFile(), VersionFit.Loader.QUILT).size());
    }

    @Test
    void describeMentionsVanillaAndMismatch(@TempDir Path dir) throws Exception {
        jar(dir, "sodium.jar", "fabric.mod.json", "{}");
        List<ModLoaderCheck.Finding> findings =
                ModLoaderCheck.scan(dir.toFile(), VersionFit.Loader.VANILLA);
        assertEquals(1, findings.size());
        assertTrue(ModLoaderCheck.describe(findings, VersionFit.Loader.VANILLA).contains("原版"));

        List<ModLoaderCheck.Finding> forge =
                ModLoaderCheck.scan(dir.toFile(), VersionFit.Loader.FORGE);
        String message = ModLoaderCheck.describe(forge, VersionFit.Loader.FORGE);
        assertTrue(message.contains("sodium.jar"), message);
        assertTrue(message.contains("Fabric"), message);
        assertTrue(message.contains("Forge"), message);

        assertEquals("", ModLoaderCheck.describe(List.of(), VersionFit.Loader.FABRIC));
    }
}
