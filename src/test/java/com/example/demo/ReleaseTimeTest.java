package com.example.demo;

import com.example.demo.core.meta.ReleaseTime;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 发布时间格式化：ISO → 本地时间；异常输入不能抛。 */
class ReleaseTimeTest {

    @Test
    void formatsIsoTimestamp() {
        String formatted = ReleaseTime.format("2026-09-15T12:00:00Z");
        assertTrue(formatted.matches("\\d{4}/\\d{2}/\\d{2} \\d{2}:\\d{2}"), formatted);
        assertTrue(formatted.startsWith("2026/09/1"), formatted);
    }

    @Test
    void blankReturnsEmpty() {
        assertEquals("", ReleaseTime.format(null));
        assertEquals("", ReleaseTime.format(""));
        assertEquals("", ReleaseTime.format("   "));
    }

    @Test
    void garbageFallsBackToDatePrefix() {
        assertEquals("2026/09/15", ReleaseTime.format("2026-09-15 19:23:00"));
        // 非 ISO 输入：取前 10 个字符、把 '-' 换成 '/'，保证界面上不会出现一整串时间戳
        assertEquals("not/a/date", ReleaseTime.format("not-a-date"));
        assertEquals("garbage123", ReleaseTime.format("garbage123456"));
    }
}
