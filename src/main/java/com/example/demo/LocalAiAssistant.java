package com.example.demo;

import java.io.File;
import java.util.List;

/**
 * 未配置 DeepSeek API Key 时的本地免费助手。
 * 只回答问题，不联网，也不冒充在线模型。
 */
public final class LocalAiAssistant {

    private LocalAiAssistant() {
    }

    public static String answer(String input, File gameDir) {
        String q = input == null ? "" : input.toLowerCase();
        if (q.isBlank()) return "可以直接问：怎么装模组、整合包怎么导入、Java 用哪个版本、崩溃怎么排查。";

        if (contains(q, "你好", "hi", "hello", "在吗")) {
            return "你好，我是启动器内置的免费本地助手。没填 DeepSeek API Key 时由我值班，"
                    + "可以回答启动器使用、Java、内存、模组、整合包和常见崩溃问题。"
                    + "想要完整模型回答，可以在 设置 → AI 填写 Key。";
        }
        if (contains(q, "怎么装", "安装mod", "安装模组", "装模组", "加模组")) {
            return "装模组推荐走启动器的下载页：\n"
                    + "1. 下载 → 模组，搜索模组名\n"
                    + "2. 选择与游戏版本、加载器匹配的文件下载\n"
                    + "3. 下载完成后回实例设置 → 模组确认已加载\n"
                    + "如果是 Forge/Fabric 实例，模组必须和加载器版本匹配；不确认就先用「检查前置」。";
        }
        if (contains(q, "整合包", "modpack", "curseforge", "pcl")) {
            return "整合包可以直接导入：\n"
                    + "1. 实例选择 → 导入整合包\n"
                    + "2. 选 .zip / .mrpack\n"
                    + "3. 启动器会解析版本、加载器，并把 overrides 解到实例目录\n"
                    + "导入后先看实例设置里的加载器版本是否和整合包要求一致。";
        }
        if (contains(q, "java", "jdk")) {
            return "Java 版本按游戏版本选：\n"
                    + "· 1.20.1 及更早：Java 17\n"
                    + "· 1.20.2 ~ 1.20.6：Java 21\n"
                    + "· 1.21+：Java 21\n"
                    + "启动器可以自动下载 Azul Java；设置 → Java 里也能手动指定。";
        }
        if (contains(q, "内存", "oom", "outofmemory", "卡顿", "掉帧", "fps")) {
            return "内存和卡顿建议：\n"
                    + "· 8GB 内存的机器给游戏 4~6GB 就够，不要拉满\n"
                    + "· 模组多再往上加；优先装 Sodium / Lithium 这类优化模组\n"
                    + "· 设置里开启「智能性能」，会自动加 G1GC 参数\n"
                    + "· 如果是 OutOfMemoryError，先把最大内存调大 1~2GB 再试";
        }
        if (contains(q, "崩溃", "报错", "crash", "exception", "闪退", "打不开", "日志")) {
            File gameDirSafe = gameDir == null ? new File(".") : gameDir;
            CrashAnalyzer.Report report = CrashAnalyzer.analyze(gameDirSafe);
            StringBuilder out = new StringBuilder("我看了最近的崩溃信息：");
            out.append(report.summary()).append("\n");
            if (report.findings().isEmpty()) {
                out.append("没有识别到明确错误类型。可以先看 logs/latest.log，"
                        + "或者把日志导出成反馈包发出来。\n");
            } else {
                for (CrashAnalyzer.Finding finding : report.findings()) {
                    out.append("· ").append(finding.title()).append("：")
                            .append(finding.suggestion()).append("\n");
                }
            }
            out.append("也可以点工具页的「崩溃诊断」看完整建议。");
            return out.toString();
        }
        if (contains(q, "forge", "fabric", "neoforge", "quilt", "加载器")) {
            return "加载器选择：\n"
                    + "· Fabric：轻量，适合优化模组和大多数现代模组\n"
                    + "· Forge：老牌，整合包多，1.12.2 / 1.16.5 / 1.20.1 常见\n"
                    + "· NeoForge：1.20.2+ 的 Forge 分支，新整合包常见\n"
                    + "· Quilt：Fabric 的衍生，兼容部分 Fabric 模组\n"
                    + "同一个实例只装一种加载器，不要混装。";
        }
        if (contains(q, "下载", "安装版本", "装游戏")) {
            return "下载页流程：下载 → Minecraft → 选版本 → 安装。\n"
                    + "在安装页里可以选原版 / Fabric / Forge / NeoForge / Quilt，"
                    + "再展开选加载器版本，最后点「开始下载」。\n"
                    + "安装完成后实例会自动出现在启动页和实例选择里。";
        }
        if (contains(q, "登录", "离线", "正版", "账号")) {
            return "启动页支持离线登录：填玩家名即可进入离线模式。\n"
                    + "正版登录入口在登录方式下拉框里；登录状态会保存在本机注册表，下次启动自动恢复。\n"
                    + "多人服需要正版验证时，请使用正版账号登录。";
        }
        if (contains(q, "隔离", "mods 不生效", "模组不生效", "config")) {
            return "版本隔离开启后，每个实例有独立的 mods / config / saves。\n"
                    + "如果模组不生效，先确认模组放在当前实例目录，而不是全局 game/mods。\n"
                    + "实例设置 → 概览里可以直接打开实例文件夹。";
        }

        int versions = 0;
        try {
            List<String> ids = VersionProfile.installedIds(gameDir == null ? new File(".") : gameDir);
            versions = ids.size();
        } catch (Exception ignored) {
        }
        return "这个问题本地免费助手暂时没有专门答案。\n"
                + "当前已安装 " + versions + " 个实例；你可以换一种问法，"
                + "或者到 设置 → AI 填写 DeepSeek API Key，用完整模型回答。";
    }

    private static boolean contains(String text, String... keys) {
        for (String key : keys) {
            if (text.contains(key)) return true;
        }
        return false;
    }
}
