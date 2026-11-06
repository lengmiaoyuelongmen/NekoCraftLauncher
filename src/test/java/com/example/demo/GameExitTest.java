package com.example.demo;

import com.example.demo.core.launch.GameExit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 游戏退出后的判定与文案。 */
class GameExitTest {

    @Test
    void kindMatrix() {
        assertEquals(GameExit.Kind.FORCED_STOP, GameExit.kind(true, 0));
        assertEquals(GameExit.Kind.FORCED_STOP, GameExit.kind(true, 1), "主动结束优先于退出码");
        assertEquals(GameExit.Kind.NORMAL, GameExit.kind(false, 0));
        assertEquals(GameExit.Kind.CRASH, GameExit.kind(false, 1));
        assertEquals(GameExit.Kind.CRASH, GameExit.kind(false, -1073741819), "Windows 访问冲突也归为崩溃");
    }

    @Test
    void crashLikeMatchesKind() {
        assertFalse(GameExit.crashLike(true, 1));
        assertFalse(GameExit.crashLike(false, 0));
        assertTrue(GameExit.crashLike(false, 2));
    }

    @Test
    void messages() {
        assertEquals("🛑 游戏已被启动器主动结束。\n", GameExit.logMessage(GameExit.Kind.FORCED_STOP, 0));
        assertEquals("🏁 游戏已正常退出（退出码 0）\n", GameExit.logMessage(GameExit.Kind.NORMAL, 0));
        assertEquals("⚠ 游戏异常退出（退出码 3）\n", GameExit.logMessage(GameExit.Kind.CRASH, 3));

        assertEquals("Game stopped by launcher.", GameExit.trayMessage(GameExit.Kind.FORCED_STOP));
        assertEquals("Game exited normally.", GameExit.trayMessage(GameExit.Kind.NORMAL));
        assertTrue(GameExit.trayMessage(GameExit.Kind.CRASH).contains("crash report"));
    }

    @Test
    void sessionSecondsBoundaries() {
        long now = 1_000_000L;
        assertEquals(0L, GameExit.sessionSeconds(0L, now), "没有开始时间");
        assertEquals(1L, GameExit.sessionSeconds(now, now), "刚启动也算 1 秒");
        assertEquals(1L, GameExit.sessionSeconds(now - 400L, now));
        assertEquals(90L, GameExit.sessionSeconds(now - 90_000L, now));
    }
}
