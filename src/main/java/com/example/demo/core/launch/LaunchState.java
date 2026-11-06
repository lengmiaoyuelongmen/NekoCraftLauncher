package com.example.demo.core.launch;

/**
 * 启动状态机：空闲 → 准备中 → 运行中 → 空闲。
 *
 * <p>以前这些状态散在主类的四个布尔量里（launching / gameRunning / launchCancelled /
 * gameStopRequested），组合起来有十几种非法状态。现在只有三个合法阶段，外加两个请求标记：
 *
 * <pre>
 *   IDLE --beginPrepare()--> PREPARING --markRunning()--> RUNNING --finish()--> IDLE
 *     ^                          |                           |
 *     +---------- finish() ------+------------ finish() ------+
 * </pre>
 *
 * <p>线程安全：启动准备在后台线程跑，UI 在 JavaFX 线程读状态，所以字段都是 volatile +
 * 状态迁移方法 synchronized。
 */
public final class LaunchState {

    public enum Phase {
        /** 没有启动任务。 */
        IDLE,
        /** 正在准备（下载/校验/解压），可以取消。 */
        PREPARING,
        /** 游戏进程已经起来。 */
        RUNNING
    }

    private volatile Phase phase = Phase.IDLE;
    private volatile boolean cancelRequested;
    private volatile boolean stopRequested;

    /** 尝试进入「准备中」；已经在启动/运行中返回 false。 */
    public synchronized boolean beginPrepare() {
        if (phase != Phase.IDLE) {
            return false;
        }
        phase = Phase.PREPARING;
        cancelRequested = false;
        stopRequested = false;
        return true;
    }

    /** 进程已创建。 */
    public synchronized void markRunning() {
        phase = Phase.RUNNING;
        cancelRequested = false;
    }

    /** 回到空闲：进程退出、准备失败、用户取消都走这里。 */
    public synchronized void finish() {
        phase = Phase.IDLE;
        cancelRequested = false;
        stopRequested = false;
    }

    /** 用户点了「取消启动」（只在准备阶段有效）。 */
    public synchronized void cancel() {
        if (phase == Phase.PREPARING) {
            cancelRequested = true;
        }
    }

    /** 用户要求结束游戏进程。 */
    public void requestStop() {
        stopRequested = true;
    }

    public void clearStop() {
        stopRequested = false;
    }

    public void clearCancel() {
        cancelRequested = false;
    }

    public boolean isCancelRequested() {
        return cancelRequested;
    }

    public boolean isStopRequested() {
        return stopRequested;
    }

    public Phase phase() {
        return phase;
    }

    /** 是否处于启动流程（准备中或运行中）。 */
    public boolean isLaunching() {
        return phase != Phase.IDLE;
    }

    /** 游戏进程是否处于运行阶段。 */
    public boolean isRunning() {
        return phase == Phase.RUNNING;
    }

    public boolean isPreparing() {
        return phase == Phase.PREPARING;
    }
}
