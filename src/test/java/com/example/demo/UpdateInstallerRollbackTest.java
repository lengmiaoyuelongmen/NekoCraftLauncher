package com.example.demo;

import com.example.demo.core.app.UpdateInstaller;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 回滚脚本必须真的把备份换回去。
 *
 * <p>踩过的坑：{@code Copy-Item $src $dest -Recurse} 在 $dest 已存在时会把整个目录塞进去
 * （变成 app\app），看起来"执行成功"但文件没被替换。所以这里直接跑一遍脚本验证内容。
 */
class UpdateInstallerRollbackTest {

    @Test
    void generatedRollbackScriptActuallyRestoresFiles(@TempDir Path temp) throws Exception {
        File root = temp.resolve("root").toFile();
        File appDir = new File(root, "app");
        assertTrue(appDir.mkdirs());
        File cfg = new File(appDir, "NekoCraftLauncher.cfg");
        Files.writeString(cfg.toPath(), "version=1.0.0", StandardCharsets.UTF_8);
        File exe = new File(root, "NekoCraftLauncher.exe");
        Files.writeString(exe.toPath(), "old-exe", StandardCharsets.UTF_8);

        File backup = temp.resolve("backup-app-20260101-000000").toFile();
        File backupApp = new File(backup, "app");
        assertTrue(backupApp.mkdirs());
        Files.writeString(new File(backupApp, "NekoCraftLauncher.cfg").toPath(),
                "version=0.9.0", StandardCharsets.UTF_8);
        Files.writeString(new File(backup, "NekoCraftLauncher.exe").toPath(),
                "restored-exe", StandardCharsets.UTF_8);

        // 先把当前版本改脏，回滚后应恢复成备份里的内容
        Files.writeString(cfg.toPath(), "version=2.0.0", StandardCharsets.UTF_8);
        Files.writeString(exe.toPath(), "new-exe", StandardCharsets.UTF_8);

        File script = UpdateInstaller.prepareRollbackScript(backup, root);
        Process process = new ProcessBuilder("powershell.exe", "-NoProfile",
                "-ExecutionPolicy", "Bypass", "-File", script.getAbsolutePath())
                .redirectErrorStream(true)
                .start();
        assertTrue(process.waitFor(120, TimeUnit.SECONDS), "回滚脚本超时");
        assertEquals(0, process.exitValue());

        assertEquals("version=0.9.0",
                Files.readString(cfg.toPath(), StandardCharsets.UTF_8).trim());
        assertEquals("restored-exe",
                Files.readString(exe.toPath(), StandardCharsets.UTF_8).trim());
        assertTrue(new File(appDir, "app").isDirectory() == false,
                "不能出现 app\\app 这种嵌套复制");
        assertTrue(script.delete());
    }
}
