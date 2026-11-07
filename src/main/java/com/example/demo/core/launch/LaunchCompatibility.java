package com.example.demo.core.launch;

import java.util.ArrayList;
import java.util.List;

/**
 * 启动前的兼容性检查：Java 版本够不够、客户端 jar 在不在、java 路径是否可用。
 *
 * <p>业界启动器的做法是「先拦后启动」：能明确判断出必然崩溃的情况直接拦住并给出
 * 可执行建议，而不是警告一句就继续启动、等用户看崩溃日志。这里只做纯判断，
 * 弹窗与日志由界面层负责。
 */
public final class LaunchCompatibility {

    public enum Severity {
        /** 必然起不来，应当阻止启动。 */
        BLOCK,
        /** 可能有问题，提示后仍可启动。 */
        WARN
    }

    public enum Kind {
        JAVA_PATH_INVALID,
        JAVA_TOO_OLD,
        JAVA_UNKNOWN,
        CLIENT_JAR_MISSING,
        VERSION_JSON_MISSING
    }

    /** 一条检查结果。 */
    public record Issue(Kind kind, Severity severity, String message) {
    }

    private LaunchCompatibility() {
    }

    /**
     * 执行检查。
     *
     * @param requiredJava 版本 JSON 要求的 Java 主版本
     * @param actualJava 实际检测到的 Java 主版本（0 表示探测失败）
     * @param javaPath 解析出的 java 可执行文件路径（空表示没有）
     * @param clientJarExists 版本客户端 jar 是否存在
     * @param versionJsonExists 版本 JSON 是否存在
     */
    public static List<Issue> check(
            int requiredJava,
            int actualJava,
            String javaPath,
            boolean clientJarExists,
            boolean versionJsonExists) {
        List<Issue> issues = new ArrayList<>();
        if (!versionJsonExists) {
            issues.add(new Issue(Kind.VERSION_JSON_MISSING, Severity.BLOCK,
                    "版本文件缺失，建议重新安装该版本"));
        }
        boolean hasPath = javaPath != null && !javaPath.isBlank();
        if (!hasPath) {
            issues.add(new Issue(Kind.JAVA_PATH_INVALID, Severity.BLOCK,
                    "没有找到可用的 Java，请在「设置 → Java」里指定路径或让启动器自动下载"));
        }
        if (actualJava == 0 && hasPath) {
            issues.add(new Issue(Kind.JAVA_UNKNOWN, Severity.WARN,
                    "无法识别 Java 版本，仍会尝试启动"));
        }
        if (actualJava > 0 && requiredJava > 0 && actualJava < requiredJava) {
            issues.add(new Issue(Kind.JAVA_TOO_OLD, Severity.BLOCK,
                    "该版本需要 Java " + requiredJava + "，当前是 Java " + actualJava
                            + "，请换成 Java " + requiredJava + " 或更高版本"));
        }
        if (!clientJarExists) {
            issues.add(new Issue(Kind.CLIENT_JAR_MISSING, Severity.BLOCK,
                    "缺少客户端 jar，可在「工具 → 百宝箱 → 校验库文件」里补齐"));
        }
        return issues;
    }

    /** 是否存在会阻止启动的问题。 */
    public static boolean blocking(List<Issue> issues) {
        if (issues == null) {
            return false;
        }
        for (Issue issue : issues) {
            if (issue.severity() == Severity.BLOCK) {
                return true;
            }
        }
        return false;
    }

    /** 第一件阻止启动的事，用来做弹窗标题；没有就返回 null。 */
    public static Issue firstBlocking(List<Issue> issues) {
        if (issues == null) {
            return null;
        }
        for (Issue issue : issues) {
            if (issue.severity() == Severity.BLOCK) {
                return issue;
            }
        }
        return null;
    }

    /** 把多条问题拼成一段可读文本（每行一条）。 */
    public static String describe(List<Issue> issues) {
        if (issues == null || issues.isEmpty()) {
            return "";
        }
        StringBuilder text = new StringBuilder();
        for (Issue issue : issues) {
            text.append(issue.message()).append('\n');
        }
        return text.toString().trim();
    }
}
