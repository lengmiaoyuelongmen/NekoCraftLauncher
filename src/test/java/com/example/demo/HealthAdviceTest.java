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
}
