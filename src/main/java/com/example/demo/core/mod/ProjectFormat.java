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

    /** 是否是整合包（接受 Modrinth 的 modpack 和界面上的「整合包」）。 */
    public static boolean isModpack(String type) {
        return "modpack".equalsIgnoreCase(type) || "整合包".equals(type);
    }

    /**
     * 这类资源安装到实例里的哪个子目录。
     *
     * <p>同时接受 Modrinth 的英文类型和界面上的中文标签——社区页拿到的是英文
     * （mod / datapack …），而旧代码只判断中文，导致所有非整合包资源都被丢进
     * downloads/modpack。
     */
    public static String installDir(String type) {
        if (type == null || type.isBlank()) {
            return "downloads/modpack";
        }
        return switch (type) {
            case "mod", "模组" -> "mods";
            case "resourcepack", "资源包" -> "resourcepacks";
            case "shader", "光影包" -> "shaderpacks";
            case "datapack", "数据包" -> "datapacks";
            default -> "downloads/modpack";
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
