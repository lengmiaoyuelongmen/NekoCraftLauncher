package com.example.demo.core.meta;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 加载器版本与 Minecraft 版本的匹配判断。
 *
 * <p>Forge / NeoForge 的版本号里带着 MC 版本（`1.20.1-47.4.10`、`26.3-66.1.0`），
 * 装错就会得到一个起不来的实例；Fabric / Quilt 的 loader 与 MC 解耦，只要求非空。
 */
public final class LoaderCompatibility {

    public enum Loader {
        FABRIC("Fabric"),
        QUILT("Quilt"),
        FORGE("Forge"),
        NEOFORGE("NeoForge");

        private final String label;

        Loader(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    public record Result(boolean ok, String message) {
        public static Result pass() {
            return new Result(true, "");
        }

        public static Result fail(String message) {
            return new Result(false, message);
        }
    }

    private static final Pattern MC_PREFIX =
            Pattern.compile("^(\\d+(?:\\.\\d+)+)-(.+)$");

    private LoaderCompatibility() {
    }

    /**
     * 校验加载器版本是否配得上这个 MC 版本。
     *
     * @param loader 加载器
     * @param minecraftVersion 目标 MC 版本（如 1.20.1 / 26.3）
     * @param loaderVersion 加载器版本（如 1.20.1-47.4.10 / 0.15.11）
     */
    public static Result validate(Loader loader, String minecraftVersion, String loaderVersion) {
        if (loader == null) {
            return Result.pass();
        }
        String mc = minecraftVersion == null ? "" : minecraftVersion.trim();
        String version = loaderVersion == null ? "" : loaderVersion.trim();
        if (version.isEmpty()) {
            return Result.fail("请先选择 " + loader.label() + " 版本");
        }
        if (loader == Loader.FABRIC || loader == Loader.QUILT) {
            // Fabric/Quilt 的 loader 与 MC 版本无关
            return Result.pass();
        }
        Matcher matcher = MC_PREFIX.matcher(version);
        if (!matcher.matches()) {
            // 只给了构建号（如 47.4.10）：无法判断，放行
            return Result.pass();
        }
        String versionMc = matcher.group(1);
        if (mc.isEmpty() || mc.equals(versionMc)) {
            return Result.pass();
        }
        return Result.fail(
                loader.label()
                        + " "
                        + version
                        + " 属于 Minecraft "
                        + versionMc
                        + "，与所选版本 "
                        + mc
                        + " 不匹配，请选择 "
                        + mc
                        + " 对应的构建");
    }

    /** 从版本列表里挑出与该 MC 版本匹配的（Forge/NeoForge 用前缀判断，Fabric/Quilt 全留）。 */
    public static List<String> matching(Loader loader, String minecraftVersion, List<String> versions) {
        if (versions == null) {
            return List.of();
        }
        return versions.stream()
                .filter(version -> validate(loader, minecraftVersion, version).ok())
                .toList();
    }

    /** 按界面上的加载器名（「Fabric」「Forge」…）解析。 */
    public static Loader fromLabel(String label) {
        if (label == null) {
            return null;
        }
        String normalized = label.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "fabric" -> Loader.FABRIC;
            case "quilt" -> Loader.QUILT;
            case "forge" -> Loader.FORGE;
            case "neoforge" -> Loader.NEOFORGE;
            default -> null;
        };
    }
}
