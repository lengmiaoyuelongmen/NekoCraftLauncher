package com.example.demo;

import com.example.demo.core.game.ImportErrors;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 异常翻译：取最内层原因 + 按关键字给建议。 */
class ImportErrorsTest {

    @Test
    void unwrapsNestedCauses() {
        Throwable error = new CompletionException(new UncheckedIOException(
                new IOException("磁盘空间不足")));

        assertEquals("磁盘空间不足", ImportErrors.rootMessage(error));
    }

    @Test
    void fallsBackToClassNameWhenNoMessage() {
        assertEquals("IllegalStateException",
                ImportErrors.rootMessage(new IllegalStateException()));
        assertEquals("", ImportErrors.rootMessage(null));
    }

    @Test
    void suggestsFixesByKeyword() {
        assertTrue(ImportErrors.hint("java.lang.RuntimeException: no java").contains("Java 路径"));
        assertTrue(ImportErrors.hint("HTTP connect timed out").contains("镜像优先"));
        assertTrue(ImportErrors.hint("磁盘空间不足").contains("磁盘空间"));
        assertTrue(ImportErrors.hint("Access is denied").contains("权限"));
        assertEquals("", ImportErrors.hint("未知错误"));
    }
}
