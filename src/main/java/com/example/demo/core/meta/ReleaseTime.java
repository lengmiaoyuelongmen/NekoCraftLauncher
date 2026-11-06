package com.example.demo.core.meta;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** 版本发布时间格式化：版本列表与版本详情页共用。 */
public final class ReleaseTime {

    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");

    private ReleaseTime() {
    }

    /** ISO-8601 → {@code yyyy/MM/dd HH:mm}（本地时区）；解析失败时退化成日期前缀。 */
    public static String format(String iso) {
        if (iso == null || iso.isBlank()) {
            return "";
        }
        try {
            return Instant.parse(iso).atZone(ZoneId.systemDefault()).format(FORMAT);
        } catch (Exception e) {
            return iso.length() >= 10 ? iso.substring(0, 10).replace('-', '/') : iso;
        }
    }
}
