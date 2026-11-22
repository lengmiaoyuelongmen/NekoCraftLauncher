package com.example.demo.core.game;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 真机回归：本机若已装 JDK 21，准备运行时必须直接复用它，
 * 不能再走「下载 200MB Zulu」那条路（用户就是卡在这里）。
 */
class JavaEnsureLocalProbeTest {

    @Test
    void reusesInstalledJdk21InsteadOfDownloading(@TempDir Path gameDir) throws Exception {
        java.io.File local = JavaManager.findLocalJava(21);
        if (local == null) {
            System.out.println("PROBE 本机没有 JDK 21，跳过（CI 环境正常）");
            return;
        }
        List<String> logs = new ArrayList<>();
        String path = JavaManager.ensureJavaAsync(gameDir.toFile(), 21, logs::add)
                .get(120, TimeUnit.SECONDS);
        System.out.println("PROBE resolved=" + path);
        logs.forEach(line -> System.out.println("PROBE log " + line));
        assertTrue(path.toLowerCase().endsWith("java.exe"), path);
        assertTrue(logs.stream().anyMatch(l -> l.contains("使用本机已安装的 Java 21")),
                "应当复用本机 JDK，日志里没有看到: " + logs);
        assertTrue(logs.stream().noneMatch(l -> l.contains("下载")),
                "复用本机 JDK 时不应触发下载: " + logs);
    }
}
