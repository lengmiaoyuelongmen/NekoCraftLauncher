package com.example.demo.core.launch;

/**
 * 游戏进程退出后的判定：是被启动器停掉的、正常退出、还是崩溃，以及对应的文案与会话时长。
 *
 * <p>主类原来在 onExit 的回调里用三层嵌套三元表达式算这些，藏得很深也不好测。
 */
public final class GameExit {

    public enum Kind {
        /** 用户点了「结束游戏」。 */
        FORCED_STOP,
        /** 退出码 0。 */
        NORMAL,
        /** 非零退出码。 */
        CRASH
    }

    private GameExit() {
    }

    public static Kind kind(boolean forcedStop, int exitCode) {
        if (forcedStop) {
            return Kind.FORCED_STOP;
        }
        return exitCode == 0 ? Kind.NORMAL : Kind.CRASH;
    }

    /** 崩溃标记：主动结束不算崩溃，即使退出码非零。 */
    public static boolean crashLike(boolean forcedStop, int exitCode) {
        return kind(forcedStop, exitCode) == Kind.CRASH;
    }

    /** 控制台日志的一行。 */
    public static String logMessage(Kind kind, int exitCode) {
        return switch (kind) {
            case FORCED_STOP -> "🛑 游戏已被启动器主动结束。\n";
            case NORMAL -> "🏁 游戏已正常退出（退出码 " + exitCode + "）\n";
            case CRASH -> "⚠ 游戏异常退出（退出码 " + exitCode + "）\n";
        };
    }

    /** 最小化到托盘时的提示。 */
    public static String trayMessage(Kind kind) {
        return switch (kind) {
            case FORCED_STOP -> "Game stopped by launcher.";
            case NORMAL -> "Game exited normally.";
            case CRASH -> "Game crashed - check the crash report.";
        };
    }

    /** 本次游戏时长（秒）：没记录开始时间返回 0，至少 1 秒。 */
    public static long sessionSeconds(long startAt, long now) {
        if (startAt <= 0L) {
            return 0L;
        }
        return Math.max(1L, (now - startAt) / 1000L);
    }
}
