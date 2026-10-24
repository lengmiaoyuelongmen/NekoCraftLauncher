package com.example.demo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 模组前置依赖解析：Fabric JSON / Forge TOML / 缺失计算。 */
class ModDependencyScannerTest {

    @Test
    void parsesFabricMetadataAndDependencies(@TempDir Path temp) throws Exception {
        File jar = jar(temp, "sodium-0.5.8.jar", "fabric.mod.json", """
                {
                  "id": "sodium",
                  "name": "Sodium",
                  "version": "0.5.8",
                  "depends": {
                    "fabricloader": ">=0.14.0",
                    "minecraft": "~1.20.1",
                    "java": ">=17",
                    "cloth-config2": ">=11.0.0"
                  }
                }
                """);

        ModDependencyScanner.ModInfo info = ModDependencyScanner.scanJar(jar);

        assertEquals("sodium", info.modId());
        assertEquals("Sodium", info.name());
        assertEquals("0.5.8", info.version());
        assertEquals("Fabric", info.loader());
        assertTrue(info.isEnabled());
        assertEquals(4, info.deps().size());
        assertTrue(info.deps().stream().anyMatch(d -> d.modId().equals("cloth-config2") && d.mandatory()));
    }

    @Test
    void parsesForgeTomlAndSkipsOptionalDependency(@TempDir Path temp) throws Exception {
        File jar = jar(temp, "jei-1.20.1.jar", "META-INF/mods.toml", """
                modLoader = "javafml"
                loaderVersion = "[47,)"
                license = "MIT"

                [[mods]]
                modId = "jei"
                version = "15.2.0.27"
                displayName = "Just Enough Items"

                [[dependencies.jei]]
                modId = "forge"
                mandatory = true
                versionRange = "[47,)"
                ordering = "NONE"
                side = "BOTH"

                [[dependencies.jei]]
                modId = "minecraft"
                mandatory = true
                versionRange = "[1.20.1,1.21)"

                [[dependencies.jei]]
                modId = "optional-thing"
                mandatory = false
                versionRange = "*"
                """);

        ModDependencyScanner.ModInfo info = ModDependencyScanner.scanJar(jar);

        assertEquals("jei", info.modId());
        assertEquals("Just Enough Items", info.name());
        assertEquals("Forge", info.loader());
        assertEquals(3, info.deps().size());
        assertFalse(info.deps().stream()
                .filter(d -> d.modId().equals("optional-thing"))
                .findFirst().orElseThrow().mandatory(), "mandatory=false 的依赖不算缺失");
    }

    @Test
    void reportsMissingDependenciesWithAliasesAndPlatformFiltering(@TempDir Path temp) throws Exception {
        jar(temp, "sodium.jar", "fabric.mod.json", """
                {"id":"sodium","name":"Sodium","version":"1.0",
                 "depends":{"cloth-config2":">=11","fabricloader":">=0.14","minecraft":"1.20.1"}}
                """);
        jar(temp, "iris.jar", "fabric.mod.json", """
                {"id":"iris","name":"Iris Shaders","version":"1.0",
                 "depends":{"sodium":">=0.5","fabric-api":"*"}}
                """);
        jar(temp, "fabric-api.jar", "fabric.mod.json", """
                {"id":"fabric-api","name":"Fabric API","version":"0.90"}
                """);

        List<ModDependencyScanner.ModInfo> mods = ModDependencyScanner.scan(temp.toFile());
        assertEquals(3, mods.size());

        Map<String, List<String>> missing = ModDependencyScanner.findMissingDependencies(mods);

        assertTrue(missing.containsKey("cloth-config"), "cloth-config2 应归一化成 cloth-config: " + missing);
        assertEquals(List.of("Sodium"), missing.get("cloth-config"));
        assertFalse(missing.containsKey("sodium"), "已安装的 sodium 不算缺失");
        assertFalse(missing.containsKey("fabric-api"), "已安装的 fabric-api 不算缺失");
        assertFalse(missing.containsKey("minecraft"), "游戏本身不是前置模组");
    }

    @Test
    void ignoredDisabledModsDoNotRequireDependencies(@TempDir Path temp) throws Exception {
        jar(temp, "disabled-mod.jar.disabled", "fabric.mod.json", """
                {"id":"disabled-mod","name":"Disabled Mod","version":"1.0",
                 "depends":{"cloth-config2":"*"}}
                """);

        List<ModDependencyScanner.ModInfo> mods = ModDependencyScanner.scan(temp.toFile());
        assertEquals(1, mods.size());
        assertFalse(mods.get(0).isEnabled(), "文件名以 .disabled 结尾应视为禁用");
        assertTrue(ModDependencyScanner.findMissingDependencies(mods).isEmpty(),
                "禁用的模组不参与前置检查");
    }

    @Test
    void keepsUnknownJarInList(@TempDir Path temp) throws Exception {
        File jar = jar(temp, "random.jar", "some/file.txt", "hello");
        ModDependencyScanner.ModInfo info = ModDependencyScanner.scanJar(jar);
        assertNotNull(info);
        assertEquals(null, info.modId());
        assertEquals("random", info.name());
    }

    @Test
    void embeddedJarSatisfiesDependency(@TempDir Path temp) throws Exception {
        // 模拟 JEI：mods.toml 依赖 mezz_config，但这个前置是用 JarJar 内嵌的
        byte[] nested = jarBytes("META-INF/mods.toml", """
                modLoader = "javafml"
                [[mods]]
                modId = "mezz_config"
                version = "0.6.6"
                """);
        Path jei = temp.resolve("jei.jar");
        try (OutputStream out = Files.newOutputStream(jei);
             ZipOutputStream zip = new ZipOutputStream(out, StandardCharsets.UTF_8)) {
            zip.putNextEntry(new ZipEntry("META-INF/mods.toml"));
            zip.write("""
                    modLoader = "javafml"
                    [[mods]]
                    modId = "jei"
                    displayName = "Just Enough Items"
                    version = "15.74.0.228"

                    [[dependencies.jei]]
                    modId = "mezz_config"
                    mandatory = true
                    versionRange = "[0.6.3,1.0.0)"
                    """.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("META-INF/jarjar/mezz_config-0.6.6.jar"));
            zip.write(nested);
            zip.closeEntry();
        }

        List<ModDependencyScanner.ModInfo> mods = ModDependencyScanner.scan(temp.toFile());

        assertEquals(1, mods.size());
        assertEquals(List.of("mezz_config"), mods.get(0).embeddedModIds());
        assertTrue(ModDependencyScanner.findMissingDependencies(mods).isEmpty(),
                "内嵌前置不应被判定为缺失");
    }

    private static byte[] jarBytes(String entry, String content) throws Exception {
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(buffer, StandardCharsets.UTF_8)) {
            zip.putNextEntry(new ZipEntry(entry));
            zip.write(content.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return buffer.toByteArray();
    }

    private static File jar(Path dir, String name, String entry, String content) throws Exception {
        Path path = dir.resolve(name);
        try (OutputStream out = Files.newOutputStream(path);
             ZipOutputStream zip = new ZipOutputStream(out, StandardCharsets.UTF_8)) {
            zip.putNextEntry(new ZipEntry(entry));
            zip.write(content.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return path.toFile();
    }
}
