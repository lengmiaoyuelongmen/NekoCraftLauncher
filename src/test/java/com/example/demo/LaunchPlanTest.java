package com.example.demo;

import com.example.demo.core.launch.LaunchPlan;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 启动计划：不可变、可追加参数、日志脱敏。 */
class LaunchPlanTest {

    @Test
    void appendReturnsNewPlan(@TempDir Path temp) {
        LaunchPlan base = LaunchPlan.of(temp.toFile(), List.of("java", "-jar", "mc.jar"));

        LaunchPlan joined = base.append("--server", "mc.example.com", "--port", "25565");

        assertNotSame(base, joined);
        assertEquals(3, base.command().size(), "原计划不能被修改");
        assertEquals(7, joined.command().size());
        assertEquals("--server", joined.command().get(3));
        assertEquals("mc.example.com", joined.command().get(4));
        assertEquals("25565", joined.command().get(6));
        assertEquals(temp.toFile(), joined.workingDirectory());
    }

    @Test
    void appendWithNullsIsIgnored(@TempDir Path temp) {
        LaunchPlan plan = LaunchPlan.of(temp.toFile(), List.of("java"));
        assertEquals(1, plan.append((String[]) null).command().size());
        assertEquals(3, plan.append("--x", null, "--y").command().size(), "null 参数被忽略");
    }

    @Test
    void masksAccessToken(@TempDir Path temp) {
        LaunchPlan plan = LaunchPlan.of(temp.toFile(),
                List.of("java", "--accessToken", "super-secret", "--username", "Steve"));

        String masked = plan.maskedCommand();

        assertFalse(masked.contains("super-secret"), "日志里不能出现明文 token");
        assertTrue(masked.contains("******"));
        assertTrue(masked.contains("Steve"), "其他参数保持可见");
        assertEquals("java", plan.javaExecutable());
    }

    @Test
    void commandIsImmutable(@TempDir Path temp) {
        LaunchPlan plan = LaunchPlan.of(temp.toFile(), new java.util.ArrayList<>(List.of("java")));
        boolean threw = false;
        try {
            plan.command().add("oops");
        } catch (UnsupportedOperationException e) {
            threw = true;
        }
        assertTrue(threw, "command() 必须返回不可变视图");
    }

    @Test
    void startsProcessInWorkingDirectory(@TempDir Path temp) throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(
                System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("win"),
                "只用 Windows 自带的 cmd 验证工作目录");

        LaunchPlan plan = LaunchPlan.of(temp.toFile(), List.of("cmd.exe", "/c", "cd"));

        Process process = plan.start();
        String output = new String(process.getInputStream().readAllBytes(),
                java.nio.charset.StandardCharsets.UTF_8);
        assertEquals(0, process.waitFor());
        String expected = temp.toFile().getAbsolutePath().toLowerCase(java.util.Locale.ROOT);
        assertTrue(output.toLowerCase(java.util.Locale.ROOT).contains(expected),
                "进程应在计划的工作目录里启动，实际输出: " + output);
    }
}
