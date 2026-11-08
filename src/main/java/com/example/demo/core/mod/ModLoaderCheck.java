package com.example.demo.core.mod;



import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * 模组与实例加载器是否匹配（Fabric 的模组丢进 Forge 实例是最常见的启动崩溃原因之一）。
 *
 * <p>只按 jar 里的元数据文件判断，不去解析具体版本号：
 * <ul>
 *   <li>{@code fabric.mod.json} → Fabric</li>
 *   <li>{@code quilt.mod.json} → Quilt（Quilt 兼容 Fabric）</li>
 *   <li>{@code META-INF/neoforge.mods.toml} → NeoForge</li>
 *   <li>{@code META-INF/mods.toml} / {@code mcmod.info} → Forge</li>
 * </ul>
 */
public final class ModLoaderCheck {

    /** 模组自身声明的加载器家族。 */
    public enum Family {
        FABRIC("Fabric"),
        QUILT("Quilt"),
        FORGE("Forge"),
        NEOFORGE("NeoForge"),
        UNKNOWN("未知");

        private final String label;

        Family(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    /**
     * @param fileName jar 文件名（不含目录）
     * @param family 该模组声明的加载器家族
     */
    public record Finding(String fileName, Family family) {
    }

    private ModLoaderCheck() {
    }

    /** 读 jar 元数据判断家族；读不了就当未知（不误报）。 */
    public static Family detect(File jar) {
        if (jar == null || !jar.isFile()) {
            return Family.UNKNOWN;
        }
        try (ZipFile zip = new ZipFile(jar)) {
            if (zip.getEntry("quilt.mod.json") != null) {
                return Family.QUILT;
            }
            if (zip.getEntry("fabric.mod.json") != null) {
                return Family.FABRIC;
            }
            if (zip.getEntry("META-INF/neoforge.mods.toml") != null) {
                return Family.NEOFORGE;
            }
            if (zip.getEntry("META-INF/mods.toml") != null || zip.getEntry("mcmod.info") != null) {
                return Family.FORGE;
            }
        } catch (Exception ignored) {
            // 坏 jar / 不是 zip：交给完整性检查，这里不重复报
        }
        return Family.UNKNOWN;
    }

    /** 该家族的模组能不能在目标加载器上跑。 */
    public static boolean compatible(Family family, VersionFit.Loader target) {
        if (family == Family.UNKNOWN || target == null) {
            return true;
        }
        return switch (target) {
            case FABRIC -> family == Family.FABRIC;
            case QUILT -> family == Family.FABRIC || family == Family.QUILT;
            case FORGE -> family == Family.FORGE;
            case NEOFORGE -> family == Family.NEOFORGE;
            // 原版根本没有加载器，任何模组都装不上
            case VANILLA -> false;
        };
    }

    /**
     * 扫 mods 目录，返回与目标加载器不匹配的启用的模组。
     *
     * <p>{@code .jar.disabled} 不参与判断：用户已经手动关掉了，不会被加载。
     */
    public static List<Finding> scan(File modsDir, VersionFit.Loader target) {
        List<Finding> out = new ArrayList<>();
        if (modsDir == null || target == null) {
            return out;
        }
        File[] jars =
                modsDir.listFiles(
                        (dir, name) -> {
                            String lower = name.toLowerCase(Locale.ROOT);
                            return lower.endsWith(".jar");
                        });
        if (jars == null) {
            return out;
        }
        for (File jar : jars) {
            Family family = detect(jar);
            if (!compatible(family, target)) {
                out.add(new Finding(jar.getName(), family));
            }
        }
        return out;
    }

    /** 给体检面板用的一句话。 */
    public static String describe(List<Finding> findings, VersionFit.Loader target) {
        if (findings == null || findings.isEmpty()) {
            return "";
        }
        Finding first = findings.get(0);
        if (target == VersionFit.Loader.VANILLA) {
            return "实例是原版，却装了 " + findings.size() + " 个模组（原版加载不了模组）：" + first.fileName();
        }
        String more = findings.size() > 1 ? " 等 " + findings.size() + " 个" : "";
        return first.fileName()
                + " 是 "
                + first.family().label()
                + " 模组，当前实例是 "
                + target.label()
                + more;
    }
}
