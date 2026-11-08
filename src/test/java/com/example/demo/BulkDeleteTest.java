package com.example.demo;

import com.example.demo.core.mod.BulkDelete;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 批量删除的结果统计与提示文案。 */
class BulkDeleteTest {

    @Test
    void allDeletedIsSuccess() {
        BulkDelete result = new BulkDelete(3, 3);
        assertTrue(result.complete());
        assertFalse(result.shouldWarn());
        assertEquals(0, result.failed());
        assertEquals("🗑️ 已删除 3 个模组", result.logMessage("模组"));
        assertEquals("已删除 3 个模组", result.toastMessage("模组"));
    }

    @Test
    void partialFailureIsReported() {
        BulkDelete result = new BulkDelete(5, 2);
        assertEquals(3, result.failed());
        assertFalse(result.complete());
        assertTrue(result.shouldWarn(), "有失败时必须给出警告而不是成功提示");
        assertTrue(result.logMessage("模组").contains("失败 3 个"));
        assertTrue(result.toastMessage("模组").contains("3 个模组删除失败"));
    }

    @Test
    void nothingDeletedStillReportsNothingDone() {
        BulkDelete result = new BulkDelete(2, 0);
        assertEquals(2, result.failed());
        assertTrue(result.toastMessage("模组").contains("2 个"));
    }

    @Test
    void countsAreSanitised() {
        BulkDelete negative = new BulkDelete(-1, -1);
        assertEquals(0, negative.attempted());
        assertEquals(0, negative.deleted());
        assertTrue(negative.complete());

        BulkDelete overCount = new BulkDelete(2, 9);
        assertEquals(2, overCount.deleted(), "删除数不能超过尝试数");
        assertTrue(overCount.complete());
    }
}
