package com.example.demo.core.launch;

/**
 * 启动前置拦截规则：同一时刻只允许一个启动流程 / 一个游戏进程。
 *
 * <p>主类原来用两个 if + 两段文案做这件事；文案和判定放在一起，便于单测与保持一致。
 */
public final class LaunchGuards {

    /** 为什么被拦下。 */
    public enum Block {
        /** 已经有一个游戏进程在跑。 */
        GAME_RUNNING,
        /** 启动流程正在进行（准备阶段）。 */
        LAUNCHING,
        /** 可以启动。 */
        NONE
    }

    private LaunchGuards() {
    }

    public static Block blockReason(boolean gameProcessAlive, boolean launching) {
        if (gameProcessAlive) {
            return Block.GAME_RUNNING;
        }
        if (launching) {
            return Block.LAUNCHING;
        }
        return Block.NONE;
    }

    /** 日志里的一行说明；可启动时返回空串。 */
    public static String logMessage(Block block) {
        return switch (block) {
            case GAME_RUNNING -> "⚠ 已有游戏在运行，请先点顶栏「结束游戏」再启动新的实例";
            case LAUNCHING -> "🎮 游戏正在运行中，如需中止请点顶栏的「取消启动」";
            case NONE -> "";
        };
    }

    /** 给 Toast 的短提示；可启动或不需要提示时返回空串。 */
    public static String toastMessage(Block block) {
        return block == Block.GAME_RUNNING ? "已有游戏在运行，请先结束当前游戏" : "";
    }
}
