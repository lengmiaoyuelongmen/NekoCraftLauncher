package com.example.demo.core.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 实例 id ↔ 信息解析。
 *
 * <p>版本目录名把加载器和版本拼在一起：{@code 1.20.1-forge-47.2.0}、
 * {@code fabric-loader-0.20.1-26.3}、{@code 26.4-snapshot-5}……下载页的「游戏版本」筛选
 * 需要从中还原出原版版本号。
 */
public final class InstanceId {

    private static final Pattern VERSION = Pattern.compile("^(1\\.\\d+(?:\\.\\d+)?|\\d+\\.\\d+(?:\\.\\d+)?)");

    private InstanceId() {
    }

    /** 从实例 id 里取原版版本号；认不出来返回 {@code null}。 */
    public static String mcVersion(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        String value = id;
        int forge = value.indexOf("-forge-");
        if (forge > 0) {
            value = value.substring(0, forge);
        }
        if (value.startsWith("fabric-loader-") || value.startsWith("quilt-loader-")) {
            value = value.substring(value.lastIndexOf('-') + 1);
        }
        Matcher matcher = VERSION.matcher(value);
        return matcher.find() ? matcher.group(1) : null;
    }
}
