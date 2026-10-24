package com.example.demo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** 一个游戏实例：版本 id + 解析出来的原版版本 / 加载器 / 加载器版本。 */
public final class Instance {

    public final String id;
    public final String mcVersion;
    public final String loader;
    public final String loaderVersion;

    public Instance(String id, String mcVersion, String loader, String loaderVersion) {
        this.id = id;
        this.mcVersion = mcVersion;
        this.loader = loader;
        this.loaderVersion = loaderVersion;
    }

    public String subtitle() {
        if ("原版".equals(loader)) return mcVersion;
        return mcVersion + " | " + loader + " " + loaderVersion;
    }

    public static Instance of(File gameDir, String id) {
        String mc = id;
        try {
            mc = VersionProfile.load(gameDir, id).getRootId();
        } catch (Exception ignored) {
        }

        if (id.startsWith("fabric-loader-")) {
            return new Instance(id, mc, "Fabric", strip(id, "fabric-loader-", "-" + mc));
        }
        if (id.startsWith("quilt-loader-")) {
            return new Instance(id, mc, "Quilt", strip(id, "quilt-loader-", "-" + mc));
        }
        int forgeAt = id.indexOf("-forge-");
        if (forgeAt > 0) {
            return new Instance(id, mc, "Forge", id.substring(forgeAt + "-forge-".length()));
        }
        int neoAt = id.indexOf("-neoforge-");
        if (neoAt > 0) {
            return new Instance(id, mc, "NeoForge", id.substring(neoAt + "-neoforge-".length()));
        }
        return new Instance(id, mc, "原版", mc);
    }

    private static String strip(String value, String prefix, String suffix) {
        String rest = value.startsWith(prefix) ? value.substring(prefix.length()) : value;
        if (rest.endsWith(suffix)) rest = rest.substring(0, rest.length() - suffix.length());
        return rest;
    }

    /** versions 目录下所有已安装实例，按 加载器 + 版本 排序。 */
    public static List<Instance> list(File gameDir) {
        List<Instance> result = new ArrayList<>();
        for (String id : VersionProfile.installedIds(gameDir)) {
            result.add(of(gameDir, id));
        }
        result.sort((a, b) -> {
            int byLoader = a.loader.compareTo(b.loader);
            if (byLoader != 0) return byLoader;
            return b.mcVersion.compareTo(a.mcVersion);
        });
        return result;
    }
}
