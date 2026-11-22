package com.example.demo;

import com.example.demo.core.app.UpdateInstaller;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 更新落地：pending 标记 + 安装助手脚本。
 *
 * <p>脚本必须用 jpackage 的静默参数 {@code /quiet}（旧代码误用了 Inno 的 /VERYSILENT，
 * 所以「立即安装」实际什么也没装），并且要在本进程退出之后才装、装完再拉起来。
 */
class UpdateInstallerTest {

    @Test
    void applyScriptUsesJpackageQuietFlagAndRelaunches(@TempDir Path temp) throws Exception {
        File installer = temp.resolve("NekoCraftLauncher-9.9.9.exe").toFile();
        Files.writeString(installer.toPath(), "stub", StandardCharsets.UTF_8);
        File exe = temp.resolve("NekoCraftLauncher.exe").toFile();

        File script = UpdateInstaller.prepareApplyScript(installer, exe);
        String body = Files.readString(script.toPath(), StandardCharsets.UTF_8);

        assertTrue(body.contains("/quiet"), body);
        assertTrue(!body.contains("/VERYSILENT"), "不应再出现 Inno 的参数: " + body);
        assertTrue(body.contains("timeout /t 2"), "必须先等本进程退出: " + body);
        assertTrue(body.contains(installer.getAbsolutePath()), body);
        assertTrue(body.contains(exe.getAbsolutePath()), "装完要重新拉起启动器: " + body);
        assertTrue(body.indexOf("timeout") < body.indexOf("/quiet"),
                "等待要发生在安装之前: " + body);
        // 测试写的是真实更新目录，收尾删掉，别在用户机器上留垃圾
        assertTrue(script.delete());
    }

    @Test
    void pendingMarkerRoundTrips(@TempDir Path ignored) throws Exception {
        File installer = new File(UpdateInstaller.updateDir(), "NekoCraftLauncher-Setup-9.9.9.exe");
        UpdateInstaller.writePending("9.9.9", installer);

        UpdateInstaller.Pending read = UpdateInstaller.readPending();
        assertNotNull(read);
        assertEquals("9.9.9", read.version());
        assertEquals(installer.getAbsolutePath(), read.installerPath());

        UpdateInstaller.clearPending();
        assertNull(UpdateInstaller.readPending());
    }

    @Test
    void stashInstallerCopiesIntoUpdateDir(@TempDir Path temp) throws Exception {
        File downloaded = temp.resolve("downloaded.exe").toFile();
        Files.writeString(downloaded.toPath(), "payload", StandardCharsets.UTF_8);

        File stashed = UpdateInstaller.stashInstaller(downloaded, "9.9.9");
        assertTrue(stashed.isFile(), stashed.getAbsolutePath());
        assertEquals("NekoCraftLauncher-Setup-9.9.9.exe", stashed.getName());
        assertEquals("payload", Files.readString(stashed.toPath(), StandardCharsets.UTF_8));
        assertTrue(stashed.delete());
    }
}
