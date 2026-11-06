package com.example.demo.core.model;

/** 启动页概览里用到的展示格式：耗时、会话时长。 */
public final class LaunchFormat {

    private LaunchFormat() {
    }

    /** 准备耗时：&lt;1s 显示毫秒，否则显示秒并保留一位小数；未知显示破折号。 */
    public static String duration(long ms) {
        if (ms <= 0) {
            return "—";
        }
        if (ms < 1000) {
            return ms + " ms";
        }
        return String.format(java.util.Locale.ROOT, "%.1f s", ms / 1000.0);
    }

    /** 会话时长：分钟 / 小时+分钟。 */
    public static String clock(long seconds) {
        if (seconds <= 0) {
            return "0 分";
        }
        long minutes = seconds / 60;
        if (minutes < 60) {
            return minutes + " 分";
        }
        return (minutes / 60) + " 时 " + (minutes % 60) + " 分";
    }
}
