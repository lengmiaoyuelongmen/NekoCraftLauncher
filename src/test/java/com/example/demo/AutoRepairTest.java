package com.example.demo;

import com.example.demo.core.launch.AutoRepair;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 启动前自动补全的结果与文案。 */
class AutoRepairTest {

    @Test
    void countsAndMessages() {
        AutoRepair repair = new AutoRepair(5, 3);
        assertEquals(2, repair.remaining());
        assertFalse(repair.complete());
        assertTrue(repair.needsConfirmation());
        assertEquals("🔧 启动前发现 5 个缺失文件，正在自动补全…", repair.startMessage());
        assertEquals("🔧 已补全 3/5，仍有 2 个缺失", repair.resultMessage());
    }

    @Test
    void completeRepairHasNoRemainder() {
        AutoRepair repair = new AutoRepair(4, 4);
        assertEquals(0, repair.remaining());
        assertTrue(repair.complete());
        assertFalse(repair.needsConfirmation());
        assertEquals("🔧 已补全 4/4", repair.resultMessage());
    }

    @Test
    void nothingToRepairIsTriviallyComplete() {
        AutoRepair repair = new AutoRepair(0, 0);
        assertTrue(repair.complete());
        assertEquals("🔧 已补全 0/0", repair.resultMessage());
    }

    @Test
    void rejectsImpossibleNumbers() {
        assertThrows(IllegalArgumentException.class, () -> new AutoRepair(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> new AutoRepair(2, 3), "补全数不能超过问题数");
        assertThrows(IllegalArgumentException.class, () -> new AutoRepair(2, -1));
    }
}
