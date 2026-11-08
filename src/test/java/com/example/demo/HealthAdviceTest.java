package com.example.demo;

import com.example.demo.core.launch.HealthAdvice;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 体检里 Java / 内存两行的判断与文案。 */
class HealthAdviceTest {

    @Test
    void javaOkWhenNewEnough() {
        HealthAdvice.Line line = HealthAdvice.javaLine(21, 21);
        assertTrue(line.ok());
        assertTrue(line.message().contains("Java 21"), line.message());
    }

    @Test
    void javaTooOldIsWarning() {
        HealthAdvice.Line line = HealthAdvice.javaLine(21, 8);
        assertFalse(line.ok());
        assertTrue(line.level().contains("过低"), line.level());
        assertTrue(line.message().contains("需要 Java 21"), line.message());
    }

    @Test
    void javaMissingExplainsAutoDownload() {
        HealthAdvice.Line line = HealthAdvice.javaLine(17, 0);
        assertFalse(line.ok());
        assertTrue(line.message().contains("自动下载"), line.message());
    }

    @Test
    void noRequirementIsFine() {
        assertTrue(HealthAdvice.javaLine(0, 8).ok());
    }

    @Test
    void memoryFlagsFarBelowSuggestion() {
        HealthAdvice.Line low = HealthAdvice.memoryLine(1024, 8704, false, "本机 23 GB × 35%");
        assertFalse(low.ok());
        assertTrue(low.message().contains("建议 8704 MB"), low.message());
        assertTrue(low.message().contains("OOM"), low.message());
    }

    @Test
    void memoryAcceptsSlightlyBelowSuggestion() {
        assertTrue(HealthAdvice.memoryLine(7000, 8704, false, "").ok(), "差一点不算问题");
        assertTrue(HealthAdvice.memoryLine(8704, 8704, true, "本机 23 GB").ok());
        assertTrue(HealthAdvice.memoryLine(2048, 2048, true, "").ok(), "建议值本身永远算合格");
    }
    @Test
    void heapAbovePhysicalMemoryIsFlagged() {
        HealthAdvice.Line tooBig = HealthAdvice.heapLine(16384, 8192);
        assertFalse(tooBig.ok());
        assertTrue(tooBig.level().contains("超出"), tooBig.level());
        assertTrue(tooBig.message().contains("建议降到"), tooBig.message());

        HealthAdvice.Line tight = HealthAdvice.heapLine(7000, 8192);
        assertFalse(tight.ok(), "超过物理内存 80% 要提示");
        assertTrue(tight.level().contains("偏高"), tight.level());

        assertTrue(HealthAdvice.heapLine(4096, 8192).ok());
        assertTrue(HealthAdvice.heapLine(2048, 0).ok(), "读不到物理内存时不误报");
    }

    @Test
    void reportListsEveryLine() {
        String text = HealthAdvice.render("实例 1.12.2", java.util.List.of(
                HealthAdvice.javaLine(8, 8),
                HealthAdvice.memoryLine(1024, 8704, false, "")));
        assertTrue(text.contains("实例 1.12.2"), text);
        assertTrue(text.contains("[正常]"), text);
        assertTrue(text.contains("[注意]"), text);
        assertTrue(text.contains("Java 8"), text);
        assertFalse(text.contains("[正常] 正常"), "级别不要和前缀重复：" + text);
    }}
