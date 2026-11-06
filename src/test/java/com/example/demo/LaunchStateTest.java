package com.example.demo;

import com.example.demo.core.launch.LaunchState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 启动状态机：空闲 → 准备 → 运行 → 空闲，以及取消/停止请求。 */
class LaunchStateTest {

    @Test
    void beginsPreparingOnceAtATime() {
        LaunchState state = new LaunchState();
        assertEquals(LaunchState.Phase.IDLE, state.phase());
        assertFalse(state.isLaunching());

        assertTrue(state.beginPrepare(), "空闲时应该能开始启动");
        assertEquals(LaunchState.Phase.PREPARING, state.phase());
        assertTrue(state.isLaunching());

        assertFalse(state.beginPrepare(), "准备中不能再启动第二个");
    }

    @Test
    void cancelOnlyWorksWhilePreparing() {
        LaunchState state = new LaunchState();
        state.beginPrepare();
        state.cancel();
        assertTrue(state.isCancelRequested(), "准备阶段可以取消");

        state.clearCancel();
        state.markRunning();
        state.cancel();
        assertFalse(state.isCancelRequested(), "游戏已经起来了，取消按钮不该再生效");
    }

    @Test
    void markingRunningImpliesLaunching() {
        LaunchState state = new LaunchState();
        state.beginPrepare();
        state.markRunning();

        assertEquals(LaunchState.Phase.RUNNING, state.phase());
        assertTrue(state.isLaunching());
        assertTrue(state.isRunning());
        assertFalse(state.isPreparing());
    }

    @Test
    void finishResetsEverything() {
        LaunchState state = new LaunchState();
        state.beginPrepare();
        state.cancel();
        state.requestStop();

        state.finish();

        assertEquals(LaunchState.Phase.IDLE, state.phase());
        assertFalse(state.isLaunching());
        assertFalse(state.isRunning());
        assertFalse(state.isCancelRequested());
        assertFalse(state.isStopRequested());
        assertTrue(state.beginPrepare(), "结束后必须能再次启动");
    }

    @Test
    void stopRequestIsIndependentOfPhase() {
        LaunchState state = new LaunchState();
        state.markRunning(); // 直接进入运行态

        state.requestStop();
        assertTrue(state.isStopRequested());

        state.clearStop();
        assertFalse(state.isStopRequested());
    }
}
