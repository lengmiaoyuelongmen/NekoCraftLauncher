package com.example.demo;

import com.example.demo.core.app.LauncherConfig;
import com.example.demo.core.launch.LaunchScriptExporter;
import com.example.demo.core.meta.VersionProfile;
import com.example.demo.core.model.UserSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** 导出启动脚本：内容和真实启动命令一致，能直接双击运行。 */
class LaunchScriptExporterTest {

    private VersionProfile profile(Path temp) throws Exception {
        Path versionDir = Files.createDirectories(temp.resolve("versions").resolve("1.20.4"));
        Files.writeString(versionDir.resolve("1.20.4.json"), """
                {
                  "id": "1.20.4",
                  "type": "release",
                  "mainClass": "net.minecraft.client.main.Main",
                  "assetIndex": {"id": "12", "url": "https://example.com/12.json"},
                  "libraries": [],
                  "arguments": {
                    "jvm": ["-Djava.library.path=${natives_directory}", "-cp", "${classpath}"],
                    "game": ["--username", "${auth_player_name}", "--version", "${version_name}"]
                  }
                }
                """);
        Files.write(versionDir.resolve("1.20.4.jar"), new byte[]{1, 2, 3, 4});
        return VersionProfile.load(temp.toFile(), "1.20.4");
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
    void writesRunnableBatchFile(@TempDir Path temp) throws Exception {
        VersionProfile profile = profile(temp);
        LauncherConfig config = new LauncherConfig();
        config.setMemoryMb(3072);
        File instanceDir = Files.createDirectories(temp.resolve("versions").resolve("1.20.4")).toFile();

        File out = LaunchScriptExporter.export(profile, session(), config, temp.toFile(), instanceDir,
                "C:\\\\jdk21\\\\bin\\\\java.exe", temp.resolve("natives").toFile());

        assertTrue(out.isFile(), "脚本应写到实例目录");
        String script = Files.readString(out.toPath());
        assertTrue(script.startsWith("@echo off"), "批处理开头必须是 @echo off");
        assertTrue(script.contains("cd /d \"" + instanceDir.getAbsolutePath() + "\""), "先切到实例目录");
        assertTrue(script.contains("-Xmx3072M"), "内存参数来自配置");
        assertTrue(script.contains("net.minecraft.client.main.Main"), "主类必须在命令行里");
        assertTrue(script.contains("--username Steve"), "账号占位符会被替换");
        assertTrue(script.endsWith("pause\r\n"), "结尾 pause，双击后能看到报错");
    }
}
