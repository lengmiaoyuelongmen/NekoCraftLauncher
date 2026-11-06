package com.example.demo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import com.example.demo.core.launch.LaunchPlan;
import com.example.demo.core.launch.LaunchPlanner;
import com.example.demo.core.meta.VersionProfile;
import com.example.demo.core.model.UserSession;
import com.example.demo.core.app.LauncherConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 启动命令生成：占位符替换、rules 过滤、窗口参数。 */
class LaunchPlannerTest {

    private VersionProfile profile(Path temp) throws Exception {
        File gameDir = temp.toFile();
        Path versionDir = Files.createDirectories(temp.resolve("versions").resolve("1.20.4"));
        Files.writeString(versionDir.resolve("1.20.4.json"), """
                {
                  "id": "1.20.4",
                  "type": "release",
                  "mainClass": "net.minecraft.client.main.Main",
                  "assetIndex": {"id": "12", "url": "https://example.com/12.json"},
                  "downloads": {"client": {"url": "https://example.com/client.jar", "size": 3, "sha1": "x"}},
                  "libraries": [{"name": "com.example:lib:1.0"}],
                  "arguments": {
                    "jvm": ["-Djava.library.path=${natives_directory}", "-cp", "${classpath}"],
                    "game": [
                      "--username", "${auth_player_name}",
                      "--uuid", "${auth_uuid}",
                      "--gameDir", "${game_directory}",
                      "--version", "${version_name}",
                      {"rules": [{"action": "allow", "features": {"is_demo_user": true}}], "value": "--demo"},
                      {"rules": [{"action": "allow", "features": {"has_custom_resolution": true}}],
                       "value": ["--width", "${resolution_width}", "--height", "${resolution_height}"]}
                    ]
                  }
                }
                """);
        // 客户端 jar 必须存在且非空，否则 LaunchPlanner 会拒绝
        Files.write(versionDir.resolve("1.20.4.jar"), new byte[]{1, 2, 3, 4});
        return VersionProfile.load(gameDir, "1.20.4");
    }

    private UserSession session() {
        UserSession session = new UserSession();
        session.setType(UserSession.LoginType.OFFLINE);
        session.setUsername("Steve");
        session.setUuid(UUID.randomUUID());
        session.setAccessToken("token-123");
        session.setUserType("legacy");
        return session;
    }

    @Test
    void substitutesPlaceholdersAndRespectsRules(@TempDir Path temp) throws Exception {
        VersionProfile profile = profile(temp);
        LauncherConfig config = new LauncherConfig();
        config.setMemoryMb(4096);
        config.setWindowWidth(1280);
        config.setWindowHeight(720);

        LaunchPlan plan = LaunchPlanner.plan(profile, session(), config, temp.toFile(),
                "java", temp.resolve("natives").toFile(), null);
        List<String> command = plan.command();

        assertTrue(command.contains("-Xmx4096M"), "内存参数应来自配置");
        assertTrue(command.contains("Steve"), "username 占位符应被替换");
        assertTrue(command.contains("--width") && command.contains("1280"), "自定义分辨率应开启");
        assertTrue(command.contains("720"));
        assertFalse(command.contains("--demo"), "未启用 demo 时该条件参数必须被过滤");
        assertFalse(command.stream().anyMatch(a -> a.contains("${")), "不应残留未替换的占位符");
        assertTrue(command.contains("net.minecraft.client.main.Main"), "主类必须在命令里");
    }

    @Test
    void usesLegacyMinecraftArguments(@TempDir Path temp) throws Exception {
        File gameDir = temp.toFile();
        Path versionDir = Files.createDirectories(temp.resolve("versions").resolve("1.7.10"));
        Files.writeString(versionDir.resolve("1.7.10.json"), """
                {
                  "id": "1.7.10",
                  "type": "release",
                  "mainClass": "net.minecraft.client.main.Main",
                  "assetIndex": {"id": "1.7.10"},
                  "libraries": [],
                  "minecraftArguments": "--username ${auth_player_name} --gameDir ${game_directory}"
                }
                """);
        Files.write(versionDir.resolve("1.7.10.jar"), new byte[]{1});
        VersionProfile profile = VersionProfile.load(gameDir, "1.7.10");

        LaunchPlan plan = LaunchPlanner.plan(profile, session(), new LauncherConfig(), gameDir,
                "java", temp.resolve("natives").toFile(), null);
        List<String> command = plan.command();

        assertTrue(command.contains("Steve"), "旧版 minecraftArguments 也要替换占位符");
        assertTrue(command.contains("--gameDir"));
    }

    @Test
    void appendsCustomGameArguments(@TempDir Path temp) throws Exception {
        VersionProfile profile = profile(temp);
        LauncherConfig config = new LauncherConfig();
        config.setGameArgs("--width 1920 --height 1080");

        List<String> command = LaunchPlanner
                .plan(profile, session(), config, temp.toFile(), "java",
                        temp.resolve("natives").toFile(), null)
                .command();

        int lang = command.indexOf("--lang");
        int width = command.indexOf("--width");
        assertTrue(width > 0, "自定义游戏参数必须进命令行: " + command);
        assertTrue(command.contains("1920") && command.contains("1080"));
        assertTrue(width < lang || lang < 0, "游戏参数应在 --lang 之前，保持版本参数在前");
    }
}
