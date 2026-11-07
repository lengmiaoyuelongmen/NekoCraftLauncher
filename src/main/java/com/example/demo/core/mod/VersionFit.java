package com.example.demo.core.mod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * 按「当前实例」挑选社区资源版本：先看游戏版本与加载器是否匹配，再按发布类型和下载量排序。
 *
 * <p>业界启动器（Prism / HMCL / PCL）都会做这件事——否则用户点「下载」只会拿到最新版，
 * 而最新版往往属于另一个 Minecraft 版本或别的加载器。
 */
public final class VersionFit {

    /** 加载器种类（原版为空）。 */
    public enum Loader {
        VANILLA("原版"),
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

        /** Modrinth 里这个加载器的标识。 */
        public String id() {
            return switch (this) {
                case VANILLA -> null;
                case FABRIC -> "fabric";
                case QUILT -> "quilt";
                case FORGE -> "forge";
                case NEOFORGE -> "neoforge";
            };
        }
    }

    /** 当前实例的适配目标。 */
    public record Target(String gameVersion, Loader loader) {
        public static Target of(String gameVersion, Loader loader) {
            return new Target(gameVersion == null ? "" : gameVersion.trim(), loader == null ? Loader.VANILLA : loader);
        }

        public String label() {
            return (gameVersion.isBlank() ? "未指定版本" : gameVersion) + " · " + loader.label();
        }
    }

    /** 一个可选版本。 */
    public record Candidate(
            String versionNumber,
            String versionType,
            List<String> gameVersions,
            List<String> loaders) {

        public Candidate {
            gameVersions = gameVersions == null ? List.of() : List.copyOf(gameVersions);
            loaders = loaders == null ? List.of() : List.copyOf(loaders);
        }
    }

    private VersionFit() {
    }

    /**
     * 从版本 JSON 的 mainClass / 库列表猜加载器。
     *
     * <p>顺序有讲究：NeoForge 也带 forge 的库，Quilt 也带 fabric 的库，所以先判更具体的。
     */
    public static Loader detectLoader(String mainClass, List<String> libraryNames) {
        String main = mainClass == null ? "" : mainClass.toLowerCase(Locale.ROOT);
        String libs = libraryNames == null ? "" : String.join(" ", libraryNames).toLowerCase(Locale.ROOT);
        if (libs.contains("net.neoforged") || main.contains("neoforged")) {
            return Loader.NEOFORGE;
        }
        if (libs.contains("org.quiltmc") || main.contains("quilt")) {
            return Loader.QUILT;
        }
        if (libs.contains("net.fabricmc") || main.contains("fabric")) {
            return Loader.FABRIC;
        }
        if (libs.contains("net.minecraftforge") || main.contains("minecraftforge")) {
            return Loader.FORGE;
        }
        return Loader.VANILLA;
    }

    /** 版本是否适配目标（游戏版本、加载器都要对得上；目标缺省时视为都适配）。 */
    public static boolean compatible(Candidate candidate, Target target) {
        if (candidate == null) {
            return false;
        }
        if (target != null && !target.gameVersion().isBlank()) {
            boolean gameOk = candidate.gameVersions().isEmpty()
                    || candidate.gameVersions().contains(target.gameVersion());
            if (!gameOk) {
                return false;
            }
        }
        String wantedLoader = target == null ? null : target.loader().id();
        if (wantedLoader != null && !candidate.loaders().isEmpty()) {
            return candidate.loaders().contains(wantedLoader);
        }
        return true;
    }

    /** 排序分：适配 > 不适配；正式版 > beta > alpha；下载量高的靠前。 */
    public static long score(Candidate candidate, Target target) {
        long score = compatible(candidate, target) ? 1_000_000_000L : 0L;
        String type = candidate.versionType() == null ? "" : candidate.versionType().toLowerCase(Locale.ROOT);
        score += switch (type) {
            case "release", "" -> 300_000L;
            case "beta" -> 200_000L;
            case "alpha" -> 100_000L;
            default -> 50_000L;
        };
        return score;
    }

    /**
     * 排序后的版本索引（稳定排序，索引指回原列表）。
     *
     * <p>适配的排前面；同档里正式版优先，其次按列表原顺序（Modrinth 一般按发布时间倒序）。
     */
    public static List<Integer> ranked(List<Candidate> candidates, Target target) {
        List<Integer> indices = new ArrayList<>();
        if (candidates == null) {
            return indices;
        }
        for (int i = 0; i < candidates.size(); i++) {
            indices.add(i);
        }
        indices.sort(Comparator
                .comparingLong((Integer i) -> -score(candidates.get(i), target))
                .thenComparingInt(i -> i));
        return indices;
    }

    /** 第一个适配的版本索引；没有就返回 -1。 */
    public static int bestMatch(List<Candidate> candidates, Target target) {
        for (int index : ranked(candidates, target)) {
            if (compatible(candidates.get(index), target)) {
                return index;
            }
        }
        return -1;
    }

    /** 版本行上显示适配状态用的短标签。 */
    public static String fitLabel(Candidate candidate, Target target) {
        if (target == null) {
            return "";
        }
        if (compatible(candidate, target)) {
            return "适配当前实例";
        }
        if (!target.gameVersion().isBlank()
                && !candidate.gameVersions().isEmpty()
                && !candidate.gameVersions().contains(target.gameVersion())) {
            return "需要 MC " + String.join(", ", candidate.gameVersions().stream().limit(3).toList());
        }
        String wantedLoader = target.loader().id();
        if (wantedLoader != null && !candidate.loaders().isEmpty()
                && !candidate.loaders().contains(wantedLoader)) {
            return "需要 " + String.join(", ", candidate.loaders());
        }
        return "不适配当前实例";
    }
}
