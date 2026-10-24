package com.example.demo;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * 崩溃诊断：读取最新的 crash-report 或 latest.log，匹配常见错误并给出可操作建议。
 */
public final class CrashAnalyzer {

    public record Finding(String title, String detail, String suggestion) {
    }

    public record Report(String summary, List<Finding> findings, File source) {
        public boolean hasFindings() {
            return !findings.isEmpty();
        }
    }

    private CrashAnalyzer() {
    }

    public static Report analyze(File gameDir) {
        File crash = newestCrash(gameDir);
        File log = new File(new File(gameDir, "logs"), "latest.log");
        File source = crash != null ? crash : (log.isFile() ? log : null);
        String text = source == null ? "" : readTail(source, 600_000);
        List<Finding> findings = new ArrayList<>();

        rule(text, findings,
                new String[]{"java.lang.OutOfMemoryError", "OutOfMemoryError"},
                "内存不足",
                "游戏堆内存耗尽。",
                "在设置 → 启动里调大最大内存，或关闭其他占内存的程序。");
        rule(text, findings,
                new String[]{"UnsupportedClassVersionError", "class file version"},
                "Java 版本不匹配",
                "当前 Java 版本低于模组/游戏要求。",
                "安装对应 Java（1.20.1 用 17，1.21+ 用 21），并在设置里指定。");
        rule(text, findings,
                new String[]{"Missing or unsupported mandatory dependencies", "requires"},
                "缺少前置模组",
                "模组依赖没有安装或版本不对。",
                "打开实例设置 → 模组，点「检查前置」补齐依赖。");
        rule(text, findings,
                new String[]{"Duplicate mods", "DuplicateModsFoundException", "duplicate mod"},
                "模组重复",
                "同一个模组存在多个版本。",
                "在实例设置 → 模组里删除旧版本，只保留一个。");
        rule(text, findings,
                new String[]{"Mixin apply failed", "MixinTransformerError", "mixin apply failed"},
                "Mixin 冲突",
                "模组注入失败，通常是加载器/模组版本不匹配。",
                "确认模组支持当前游戏版本和加载器版本，逐个禁用排查。");
        rule(text, findings,
                new String[]{"NoSuchMethodError", "NoSuchFieldError", "NoClassDefFoundError"},
                "模组或加载器版本不匹配",
                "运行期找不到类/方法，常见于 Forge/Fabric 版本不对。",
                "检查加载器版本与模组要求的版本是否一致。");
        rule(text, findings,
                new String[]{"GLFW error", "Failed to create window", "OpenGL"},
                "显卡 / 窗口创建失败",
                "显卡驱动或硬件加速有问题。",
                "更新显卡驱动；远程桌面下可在设置里关闭硬件加速。");
        rule(text, findings,
                new String[]{"AccessDeniedException", "拒绝访问"},
                "文件权限不足",
                "启动器或游戏无法写入目录。",
                "把启动器/游戏目录放到有写权限的位置，或关闭占用文件的程序。");
        rule(text, findings,
                new String[]{"Could not find or load main class", "ClassNotFoundException"},
                "启动文件缺失",
                "客户端 jar 或依赖不完整。",
                "工具 → 修复核心 / 校验库文件，或重新安装该版本。");
        rule(text, findings,
                new String[]{"The game crashed whilst"},
                "游戏崩溃",
                "游戏捕获到未处理的异常。",
                "查看 crash-report 的堆栈，确认最近安装的模组/资源包。");

        String summary = findings.isEmpty()
                ? (source == null ? "没有找到 crash-report 或 latest.log" : "没有识别到明确的错误类型")
                : findings.get(0).title();
        return new Report(summary, findings, source);
    }

    private static void rule(String text, List<Finding> out, String[] keys,
                             String title, String detail, String suggestion) {
        for (String key : keys) {
            if (text.contains(key)) {
                out.add(new Finding(title, detail, suggestion));
                return;
            }
        }
    }

    private static File newestCrash(File gameDir) {
        File dir = new File(gameDir, "crash-reports");
        File[] reports = dir.listFiles((d, n) -> n.endsWith(".txt"));
        if (reports == null || reports.length == 0) return null;
        return Arrays.stream(reports)
                .max(Comparator.comparingLong(File::lastModified)).orElse(null);
    }

    private static String readTail(File file, int maxBytes) {
        try {
            byte[] all = Files.readAllBytes(file.toPath());
            int start = Math.max(0, all.length - maxBytes);
            return new String(all, start, all.length - start, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    public static String toText(Report report) {
        StringBuilder out = new StringBuilder();
        out.append("崩溃诊断：").append(report.summary()).append("\n");
        if (report.source() != null) out.append("日志：").append(report.source().getAbsolutePath()).append("\n");
        for (Finding finding : report.findings()) {
            out.append("\n【").append(finding.title()).append("】\n")
                    .append(finding.detail()).append("\n建议：").append(finding.suggestion()).append("\n");
        }
        return out.toString();
    }
}
