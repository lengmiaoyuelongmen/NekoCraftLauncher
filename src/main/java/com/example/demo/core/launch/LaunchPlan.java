package com.example.demo.core.launch;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 一条可直接执行的启动计划：工作目录 + 命令行参数。
 *
 * <p>不可变对象：{@link #append(String...)} 返回新实例，准备阶段可以逐步补参数（例如最后的
 * {@code --server}），而不会出现「同一个 List 被三个线程同时改」的问题。
 *
 * <p>启动器只需要三件事：把计划记进日志（{@link #maskedCommand()}）、交给
 * {@link #start()} 起进程、失败时把 {@link #javaExecutable()} 报给用户。
 */
public record LaunchPlan(File workingDirectory, List<String> command) {

    public LaunchPlan {
        command = List.copyOf(command);
    }

    public static LaunchPlan of(File workingDirectory, List<String> command) {
        return new LaunchPlan(workingDirectory, command);
    }

    /** 追加若干参数，返回新计划。 */
    public LaunchPlan append(String... extra) {
        if (extra == null || extra.length == 0) {
            return this;
        }
        List<String> merged = new ArrayList<>(command);
        for (String arg : extra) {
            if (arg != null) {
                merged.add(arg);
            }
        }
        return new LaunchPlan(workingDirectory, merged);
    }

    /** 日志用命令行：access token 脱敏、超长截断。 */
    public String maskedCommand() {
        List<String> masked = new ArrayList<>(command);
        for (int i = 0; i < masked.size(); i++) {
            if ("--accessToken".equals(masked.get(i)) && i + 1 < masked.size()) {
                masked.set(i + 1, "******");
            }
        }
        String joined = String.join(" ", masked);
        return joined.length() > 4000 ? joined.substring(0, 4000) + " ..." : joined;
    }

    /** 按计划启动进程：工作目录、stderr 合并进 stdout。 */
    public Process start() throws IOException {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(workingDirectory);
        builder.redirectErrorStream(true);
        return builder.start();
    }

    /** 命令行第一个元素（通常是 java 路径），空计划返回空串。 */
    public String javaExecutable() {
        return command.isEmpty() ? "" : command.get(0);
    }
}
