package com.example.demo;

import com.example.demo.core.app.RingSegments;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 环形进度的分段几何：槽角、弧长、起始角、进度落段。 */
class RingSegmentsTest {

    @Test
    void slotIsAFullCircleDividedEvenly() {
        assertEquals(360.0, RingSegments.slotDegrees(1), 1e-9);
        assertEquals(40.0, RingSegments.slotDegrees(9), 1e-9);
        assertEquals(360.0, RingSegments.slotDegrees(0), 1e-9, "0 段时退化成整圈");
    }

    @Test
    void sweepLeavesRoomForTheGap() {
        assertEquals(36.0, RingSegments.sweepDegrees(9, 4.0), 1e-9);
        assertEquals(40.0, RingSegments.sweepDegrees(9, 0), 1e-9);
    }

    @Test
    void hugeGapsAreClampedInsteadOfInvertingTheArc() {
        double sweep = RingSegments.sweepDegrees(9, 1000);
        assertTrue(sweep >= 0.5, "弧长不能变成 0 或负数：" + sweep);
    }

    @Test
    void segmentsStartWhereThePreviousOneEnds() {
        double gap = 4.0;
        double first = RingSegments.startAngle(9, 0, gap);
        double second = RingSegments.startAngle(9, 1, gap);
        assertEquals(90 - 2.0, first, 1e-9, "第一段从正上方略往右开始");
        assertEquals(88.0 - 40.0, second, 1e-9);
    }

    @Test
    void progressMapsToTheRightSegment() {
        assertEquals(0, RingSegments.stageOfProgress(9, 0.0));
        assertEquals(0, RingSegments.stageOfProgress(9, 0.11));
        assertEquals(4, RingSegments.stageOfProgress(9, 0.5));
        assertEquals(8, RingSegments.stageOfProgress(9, 0.99));
        assertEquals(8, RingSegments.stageOfProgress(9, 1.0), "100% 落在最后一段，不能越界");
        assertEquals(8, RingSegments.stageOfProgress(9, 5.0), "越界输入要被钳住");
    }
}
