package com.example.demo;

import com.example.demo.core.instance.ImportRollback;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 导入失败/取消时该删什么：绝不动用户已有实例。 */
class ImportRollbackTest {

    @Test
    void reusedInstanceIsNeverDeleted() {
        ImportRollback.Plan plan = ImportRollback.planFor("1.12.2", null);
        assertFalse(plan.deleteInstanceDir(), "复用实例不能删目录");
        assertFalse(plan.deleteVersionDir());
        assertTrue(plan.note().contains("保留原目录"));
    }

    @Test
    void newlyCreatedVersionIsDeleted() {
        ImportRollback.Plan plan = ImportRollback.planFor("fabric-loader-0.20.1-26.3", "fabric-loader-0.20.1-26.3");
        assertTrue(plan.deleteVersionDir());
        assertFalse(plan.deleteInstanceDir(), "实例目录就是这个版本目录，删一次就够");
        assertTrue(plan.note().contains("清理"));
    }

    @Test
    void separateInstanceDirIsAlsoRemoved() {
        ImportRollback.Plan plan = ImportRollback.planFor("26.3", "fabric-loader-0.20.1-26.3");
        assertTrue(plan.deleteInstanceDir());
        assertTrue(plan.deleteVersionDir());
    }

    @Test
    void unknownInstanceDoesNothing() {
        for (String id : new String[] {null, "", "   "}) {
            ImportRollback.Plan plan = ImportRollback.planFor(id, null);
            assertFalse(plan.deleteInstanceDir());
            assertFalse(plan.deleteVersionDir());
            assertEquals("", plan.note());
        }
    }
}
