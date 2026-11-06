package com.example.demo;

import com.example.demo.core.model.InstanceId;
import com.example.demo.core.model.LaunchFormat;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** 启动概览的展示格式与实例 id 解析。 */
class LaunchFormatTest {

    @Test
    void durationFormats() {
        assertEquals("—", LaunchFormat.duration(0));
        assertEquals("—", LaunchFormat.duration(-5));
        assertEquals("288 ms", LaunchFormat.duration(288));
        assertEquals("1.6 s", LaunchFormat.duration(1600));
        assertEquals("93.0 s", LaunchFormat.duration(93000));
    }

    @Test
    void clockFormats() {
        assertEquals("0 分", LaunchFormat.clock(0));
        assertEquals("0 分", LaunchFormat.clock(59));
        assertEquals("2 分", LaunchFormat.clock(125));
        assertEquals("1 时 5 分", LaunchFormat.clock(3900));
    }

    @Test
    void instanceIdExtractsMcVersion() {
        assertEquals("1.20.1", InstanceId.mcVersion("1.20.1-forge-47.2.0"));
        assertEquals("26.3", InstanceId.mcVersion("fabric-loader-0.20.1-26.3"));
        assertEquals("26.3", InstanceId.mcVersion("quilt-loader-0.20.0-26.3"));
        assertEquals("26.4", InstanceId.mcVersion("26.4-snapshot-5"));
        assertNull(InstanceId.mcVersion(null));
        assertNull(InstanceId.mcVersion("  "));
        assertNull(InstanceId.mcVersion("custom-build"));
    }
}
