package com.example.demo;

import com.example.demo.core.game.JavaVersionProbe;
import com.example.demo.core.launch.LaunchPreflight;
import com.example.demo.core.model.GameDirDetector;
import com.example.demo.core.model.LanguageConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 启动前的几个小工具：语言配置、游戏目录识别、Java 版本、自检。 */
class LaunchUtilitiesTest {

    // ---------- LanguageConfig ----------

    @Test
    void writesChineseLanguageWhenMissing(@TempDir Path temp) throws Exception {
        LanguageConfig.ensureChinese(temp.toFile());

        String text = Files.readString(temp.resolve("options.txt"), StandardCharsets.UTF_8);
        assertEquals("lang:zh_cn\n", text);
    }

    @Test
    void replacesExistingLanguage(@TempDir Path temp) throws Exception {
        Files.writeString(temp.resolve("options.txt"), "fov:0.5\nlang:en_us\n", StandardCharsets.UTF_8);

        LanguageConfig.ensureChinese(temp.toFile());

        String text = Files.readString(temp.resolve("options.txt"), StandardCharsets.UTF_8);
        assertTrue(text.contains("lang:zh_cn"), text);
        assertTrue(text.contains("fov:0.5"), "其它设置必须保留");
        assertTrue(!text.contains("lang:en_us"), "旧语言行应被替换");
    }

    @Test
    void appendsLanguageWhenAbsent(@TempDir Path temp) throws Exception {
        Files.writeString(temp.resolve("options.txt"), "fov:0.5", StandardCharsets.UTF_8);

        LanguageConfig.ensureChinese(temp.toFile());

        String text = Files.readString(temp.resolve("options.txt"), StandardCharsets.UTF_8);
        assertTrue(text.contains("fov:0.5") && text.contains("lang:zh_cn"), text);
    }

    // ---------- GameDirDetector ----------

    private File versioned(Path dir) throws Exception {
        Path version = Files.createDirectories(dir.resolve("versions/1.20.1"));
        Files.writeString(version.resolve("1.20.1.json"), "{}", StandardCharsets.UTF_8);
        return dir.toFile();
    }

    @Test
    void detectsDirectAndNestedGameDirs(@TempDir Path temp) throws Exception {
        File direct = versioned(temp.resolve("mc"));
        assertEquals(direct, GameDirDetector.detect(direct), "选中的就是游戏目录");

        File parent = Files.createDirectories(temp.resolve("parent")).toFile();
        File nested = versioned(temp.resolve("parent/game"));
        assertEquals(nested, GameDirDetector.detect(parent), "应能找到子目录里的游戏");
        assertNull(GameDirDetector.detect(Files.createDirectories(temp.resolve("empty")).toFile()));
        assertNull(GameDirDetector.detect(null));
    }

    @Test
    void detectsDotMinecraft(@TempDir Path temp) throws Exception {
        File home = Files.createDirectories(temp.resolve("home")).toFile();
        File game = versioned(home.toPath().resolve(".minecraft"));

        assertEquals(game, GameDirDetector.detect(home), "home 目录应识别到 .minecraft");
    }

    // ---------- JavaVersionProbe ----------

    @Test
    void parsesJavaVersions() {
        assertEquals(8, JavaVersionProbe.parse("java version \"1.8.0_401\""));
        assertEquals(17, JavaVersionProbe.parse("openjdk version \"17.0.2\" 2022-01-18"));
        assertEquals(21, JavaVersionProbe.parse("openjdk version \"21\" 2023-09-19"));
        assertEquals(-1, JavaVersionProbe.parse("no version here"));
        assertEquals(-1, JavaVersionProbe.parse(null));
    }

    // ---------- LaunchPreflight ----------

    @Test
    void passesOnWritableDirectory(@TempDir Path temp) {
        LaunchPreflight.Report report = LaunchPreflight.check(temp.toFile(), 21);

        assertEquals(temp.toFile(), report.gameDir());
        assertEquals(21, report.requiredJava());
        assertTrue(report.usableBytes() > 0);
        assertTrue(!Files.exists(temp.resolve(".write-test")), "探测文件要清理掉");
    }

    @Test
    void rejectsMissingDirectory(@TempDir Path temp) throws Exception {
        File file = temp.resolve("not-a-dir").toFile();
        Files.writeString(file.toPath(), "x", StandardCharsets.UTF_8);

        assertThrows(IllegalStateException.class, () -> LaunchPreflight.check(file, 8));
        assertThrows(IllegalStateException.class, () -> LaunchPreflight.check(null, 8));
    }
}
