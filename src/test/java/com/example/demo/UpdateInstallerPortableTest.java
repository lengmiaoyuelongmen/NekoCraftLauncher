package com.example.demo;

import com.example.demo.core.app.UpdateInstaller;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 免安装版就地更新：脚本要把 zip 解压到临时目录、覆盖到安装根目录、再拉起启动器，
 * 而且绝不能碰 game\ 与 config.json。
 */
class UpdateInstallerPortableTest {

    @Test
    void portableScriptExpandsCopiesAndRelaunches(@TempDir Path temp) throws Exception {
        File root = temp.resolve("app-dir").toFile();
        assertTrue(root.mkdirs());
        Files.writeString(new File(root, "NekoCraftLauncher.exe").toPath(), "old", StandardCharsets.UTF_8);
        Files.writeString(new File(root, "config.json").toPath(), "{\"keep\":true}", StandardCharsets.UTF_8);

        File zip = temp.resolve("NekoCraftLauncher-9.9.9-win64.zip").toFile();
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(zip.toPath()))) {
            out.putNextEntry(new ZipEntry("app/mc-launcher.jar"));
            out.write("new-jar".getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
            out.putNextEntry(new ZipEntry("NekoCraftLauncher.exe"));
            out.write("new-exe".getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }

        File script = UpdateInstaller.preparePortableScript(zip, root, new File(root, "NekoCraftLauncher.exe"));
        String body = Files.readString(script.toPath(), StandardCharsets.UTF_8);

        assertTrue(body.contains("Expand-Archive"), body);
        assertTrue(body.contains("Copy-Item"), body);
        assertTrue(body.contains(root.getAbsolutePath()), body);
        assertTrue(body.contains("Start-Process"), "装完要重新拉起启动器: " + body);
        assertTrue(body.contains("Start-Sleep -Seconds 2"), "必须先等本进程退出: " + body);
        assertFalse(body.contains("game"), "不能碰 game 目录: " + body);

        assertTrue(script.delete());
    }

    @Test
    void installedLayoutIsDetectedByPath() {
        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData == null) {
            return;
        }
        File installed = new File(localAppData, "NekoCraftLauncher/NekoCraftLauncher.exe");
        File portable = new File("C:/NekoPortable/NekoCraftLauncher.exe");
        assertTrue(UpdateInstaller.isInstalledLayout(installed));
        assertFalse(UpdateInstaller.isInstalledLayout(portable));
    }
}
