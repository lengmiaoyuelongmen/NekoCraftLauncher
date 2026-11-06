package com.example.demo.core.mod;

/** 资源类型与下载量的显示格式（列表里用）。 */
public final class ProjectFormat {

    private ProjectFormat() {
    }

    /** Modrinth 的 project_type → 中文标签。 */
    public static String typeLabel(String type) {
        if (type == null || type.isBlank()) {
            return "资源";
        }
        return switch (type) {
            case "mod" -> "模组";
            case "resourcepack" -> "资源包";
            case "shader" -> "光影包";
            case "datapack" -> "数据包";
            case "world" -> "世界";
            default -> "整合包";
        };
    }

    /** 下载量：过万显示「万」，过亿显示「亿」。 */
    public static String count(long downloads) {
        if (downloads >= 100_000_000L) {
            return downloads / 100_000_000L + " 亿";
        }
        if (downloads >= 10_000L) {
            return downloads / 10_000L + " 万";
        }
        return String.valueOf(downloads);
    }
}
