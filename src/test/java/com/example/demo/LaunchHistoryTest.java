package com.example.demo;

import com.example.demo.core.model.LaunchHistory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 启动历史：统计口径、崩溃判定、条数上限。这些数字直接显示在启动页概览上。 */
class LaunchHistoryTest {

    private LaunchHistory.Entry entry(long time, long prepMs, long sessionSeconds, int exitCode,
                                     boolean crash) {
        return new LaunchHistory.Entry(time, "1.12.2", prepMs, sessionSeconds, exitCode, crash);
    }

    private LaunchHistory.Entry stoppedEntry(long time) {
        return new LaunchHistory.Entry(time, "1.12.2", 500, 30, 0, false, true);
    }

    @Test
    void emptyHistoryLooksPerfect(@TempDir Path temp) {
        LaunchHistory.Summary summary = LaunchHistory.summary(temp.toFile());
        assertEquals(0, summary.total());
        assertEquals(0, summary.crashes());
        assertEquals(0, summary.avgPrepMs());
        assertEquals(100, summary.successRate(), "没有记录时显示 100% 成功率");
    }

    @Test
    void summaryCountsCrashesAndAveragesPrep(@TempDir Path temp) {
        LaunchHistory.record(temp.toFile(), entry(1, 1000, 60, 0, false));
        LaunchHistory.record(temp.toFile(), entry(2, 3000, 0, 1, true));
        LaunchHistory.record(temp.toFile(), entry(3, 0, 30, 0, false));

        LaunchHistory.Summary summary = LaunchHistory.summary(temp.toFile());
        assertEquals(3, summary.total());
        assertEquals(1, summary.crashes());
        assertEquals(2000, summary.avgPrepMs(), "prepMs<=0 的记录不参与平均准备时间");
        assertEquals(67, summary.successRate(), "2/3 成功 → 67%");
    }

    @Test
    void crashFlagWithoutEvidenceIsNotACrash(@TempDir Path temp) {
        // 旧版本把「日志里出现异常」也记成 crash，正常退出也被标红；这里按退出码修正
        LaunchHistory.record(temp.toFile(), entry(1, 500, 120, 0, true));

        LaunchHistory.Summary summary = LaunchHistory.summary(temp.toFile());
        assertEquals(0, summary.crashes(), "退出码 0 且有过会话时长 → 不算崩溃");
        assertEquals(100, summary.successRate());
    }

    @Test
    void crashJudgementMatrix() {
        assertFalse(LaunchHistory.isCrash(entry(1, 100, 60, 0, false)), "没标崩溃");
        assertTrue(LaunchHistory.isCrash(entry(2, 100, 60, 1, true)), "非零退出码");
        assertTrue(LaunchHistory.isCrash(entry(3, 100, 0, 0, true)), "会话时长 0");
        assertFalse(LaunchHistory.isCrash(entry(4, 100, 60, 0, true)), "正常退出");
    }

    @Test
    void recentReturnsNewestFirst(@TempDir Path temp) {
        LaunchHistory.record(temp.toFile(), entry(1, 100, 10, 0, false));
        LaunchHistory.record(temp.toFile(), entry(2, 200, 20, 0, false));

        List<LaunchHistory.Entry> entries = LaunchHistory.recent(temp.toFile());
        assertEquals(2, entries.size());
        assertEquals(2, entries.get(0).time(), "最后记录的在最前面");
    }

    @Test
    void keepsOnlyMostRecentSixty(@TempDir Path temp) {
        for (int i = 0; i < 65; i++) {
            LaunchHistory.record(temp.toFile(), entry(i, 100, 10, 0, false));
        }
        List<LaunchHistory.Entry> entries = LaunchHistory.recent(temp.toFile());
        assertEquals(60, entries.size());
        assertEquals(64, entries.get(0).time(), "最老的记录被裁掉");
        assertEquals(5, entries.get(59).time());
    }

    @Test
    void stoppedGameIsRecordedSeparately(@TempDir Path temp) {
        LaunchHistory.record(temp.toFile(), stoppedEntry(1));

        LaunchHistory.Entry saved = LaunchHistory.recent(temp.toFile()).get(0);
        assertTrue(saved.stopped(), "主动结束要写进记录（旧数据默认 false）");
        assertEquals("主动结束", LaunchHistory.outcomeLabel(saved));
        assertFalse(LaunchHistory.isCrash(saved), "主动结束不算崩溃");

        LaunchHistory.Summary summary = LaunchHistory.summary(temp.toFile());
        assertEquals(1, summary.total());
        assertEquals(0, summary.crashes());
        assertEquals(100, summary.successRate());
    }

    @Test
    void outcomeLabels() {
        assertEquals("正常结束", LaunchHistory.outcomeLabel(entry(1, 100, 60, 0, false)));
        assertEquals("异常结束", LaunchHistory.outcomeLabel(entry(1, 100, 0, 1, true)));
        assertEquals("主动结束", LaunchHistory.outcomeLabel(stoppedEntry(1)));
    }

    @Test
    void clearEmptiesHistory(@TempDir Path temp) {
        LaunchHistory.record(temp.toFile(), entry(1, 100, 10, 0, false));
        LaunchHistory.clear(temp.toFile());
        assertTrue(LaunchHistory.recent(temp.toFile()).isEmpty());
        assertEquals(0, LaunchHistory.summary(temp.toFile()).total());
    }
}
