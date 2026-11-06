package com.example.demo;

import com.example.demo.core.mod.ProjectFormat;
import com.example.demo.core.model.InstanceId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** 实例 id 解析与下载页显示格式。 */
class InstanceIdFormatTest {

    @Test
    void extractsMcVersionFromInstanceIds() {
        assertEquals("1.20.1", InstanceId.mcVersion("1.20.1-forge-47.2.0"));
        assertEquals("1.20.1", InstanceId.mcVersion("1.20.1"));
        assertEquals("26.3", InstanceId.mcVersion("fabric-loader-0.20.1-26.3"));
        assertEquals("1.20.4", InstanceId.mcVersion("quilt-loader-0.20.0-1.20.4"));
        assertEquals("26.4", InstanceId.mcVersion("26.4-snapshot-5"));
        assertNull(InstanceId.mcVersion("my-custom-pack"));
        assertNull(InstanceId.mcVersion(null));
        assertNull(InstanceId.mcVersion("  "));
    }

    @Test
    void mapsProjectTypesToChineseLabels() {
        assertEquals("模组", ProjectFormat.typeLabel("mod"));
        assertEquals("资源包", ProjectFormat.typeLabel("resourcepack"));
        assertEquals("光影包", ProjectFormat.typeLabel("shader"));
        assertEquals("数据包", ProjectFormat.typeLabel("datapack"));
        assertEquals("世界", ProjectFormat.typeLabel("world"));
        assertEquals("整合包", ProjectFormat.typeLabel("modpack"));
        assertEquals("资源", ProjectFormat.typeLabel(null));
    }

    @Test
    void formatsDownloadCounts() {
        assertEquals("999", ProjectFormat.count(999));
        assertEquals("1 万", ProjectFormat.count(15_000));
        assertEquals("248 万", ProjectFormat.count(2_480_000));
        assertEquals("1 亿", ProjectFormat.count(123_456_789));
    }
}
