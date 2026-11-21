package com.example.demo.core.game;

import java.util.Locale;

/**
 * 把异常翻译成用户能看懂的一句话 + 一个可操作的建议。
 *
 * <p>整合包导入/启动失败时经常是一层层包裹的异常（CompletionException → IOException →
 * UncheckedIOException），这里取最内层有信息的那条。
 */
public final class ImportErrors {

    private ImportErrors() {
    }

    /** 取异常链最内层的有效信息；没有 message 时退回类名。 */
    public static String rootMessage(Throwable error) {
        if (error == null) {
            return "";
        }
        Throwable current = error;
        String message = error.getMessage();
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
            if (current.getMessage() != null && !current.getMessage().isBlank()) {
                message = current.getMessage();
            }
        }
        return message == null || message.isBlank() ? error.getClass().getSimpleName() : message;
    }

    /** 按关键字给出可操作建议；没有匹配时返回空串。 */
    public static String hint(String message) {
        String lower = message == null ? "" : message.toLowerCase(Locale.ROOT);
        if (lower.contains("java")) {
            return "内置运行时缺失或不可用，可在 设置 → 启动 → Java 路径 指定 java.exe";
        }
        if (lower.contains("timed out") || lower.contains("connect") || lower.contains("http")
                || lower.contains("下载源") || lower.contains("unreachable")) {
            return "网络不稳定，可在 设置 → 下载源 选「镜像优先」后重试";
        }
        if (lower.contains("space") || lower.contains("空间")) {
            return "磁盘空间不足，清理后再试";
        }
        if (lower.contains("access") || lower.contains("权限") || lower.contains("denied")) {
            return "目录没有写权限，换个目录或以管理员身份运行";
        }
        return "";
    }
}
