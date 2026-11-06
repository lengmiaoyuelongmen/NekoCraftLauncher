package com.example.demo;

import com.example.demo.core.instance.InstanceFiles;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 实例目录操作：递归删除、带过滤的复制。 */
class InstanceFilesTest {

    @Test
    void deletesNestedTree(@TempDir Path temp) throws Exception {
        Path nested = Files.createDirectories(temp.resolve("versions/1.20.1/mods/deep"));
        Files.writeString(nested.resolve("a.jar"), "x");
        Files.writeString(temp.resolve("versions/1.20.1/1.20.1.json"), "{}");

        assertTrue(InstanceFiles.deleteTree(temp.resolve("versions/1.20.1").toFile()));

        assertFalse(Files.exists(temp.resolve("versions/1.20.1")), "整个实例目录都应被删掉");
    }

    @Test
    void deletingMissingPathIsSuccess(@TempDir Path temp) {
        assertTrue(InstanceFiles.deleteTree(temp.resolve("nope").toFile()));
        assertTrue(InstanceFiles.deleteTree(null));
    }

    @Test
    void copiesTreeButSkipsRequestedTopLevelDirs(@TempDir Path temp) throws Exception {
        Path source = Files.createDirectories(temp.resolve("src/mods"));
        Files.writeString(source.resolve("mod.jar"), "mod");
        Files.writeString(temp.resolve("src/version.json"), "{}");
        Files.createDirectories(temp.resolve("src/saves/world"));

        File target = temp.resolve("dst").toFile();
        InstanceFiles.copyTree(temp.resolve("src").toFile(), target, Set.of("mods"));

        assertTrue(new File(target, "version.json").isFile(), "普通文件要复制");
        assertFalse(new File(target, "mods").exists(), "跳过的目录不能出现");
        assertTrue(new File(target, "saves/world").isDirectory(), "其它目录照常复制");
    }
}
