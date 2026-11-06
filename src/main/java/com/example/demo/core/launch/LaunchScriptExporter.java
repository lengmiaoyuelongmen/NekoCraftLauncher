package com.example.demo.core.launch;

import com.example.demo.core.app.LauncherConfig;
import com.example.demo.core.meta.VersionProfile;
import com.example.demo.core.model.UserSession;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

/**
 * 导出一个可直接双击运行的 {@code .bat} 启动脚本。
 *
 * <p>用途是排障：用户反馈「启动不了」时，让他跑这个脚本，就能看到和启动器完全一致的命令行、
 * 以及 JVM 的原始报错。命令本身由 {@link LaunchPlanner} 生成，不会和正常启动逻辑分叉。
 */
public final class LaunchScriptExporter {

    private LaunchScriptExporter() {
    }

    /**
     * @param instanceDir 该实例的游戏目录（版本隔离时是 versions/&lt;id&gt;）
     * @return 写出的 .bat 文件
     */
    public static File export(VersionProfile profile, UserSession session, LauncherConfig config,
                              File gameDir, File instanceDir, String javaPath, File nativesDir)
            throws Exception {
        List<String> command = LaunchPlanner
                .plan(profile, session, config, gameDir, instanceDir, javaPath, nativesDir, null)
                .command();

        StringBuilder script = new StringBuilder("@echo off\r\n");
        script.append("cd /d \"").append(instanceDir.getAbsolutePath()).append("\"\r\n");
        script.append("rem 由 Neko Craft Launcher 导出，用于排查启动问题\r\n");
        for (String arg : command) {
            script.append(arg.contains(" ") ? "\"" + arg + "\"" : arg).append(' ');
        }
        script.append("\r\npause\r\n");

        if (!instanceDir.isDirectory() && !instanceDir.mkdirs()) {
            throw new IllegalStateException("实例目录无法创建: " + instanceDir.getAbsolutePath());
        }
        File out = new File(instanceDir, "launch-" + profile.getId() + ".bat");
        Files.writeString(out.toPath(), script.toString(), StandardCharsets.UTF_8);
        return out;
    }
}
