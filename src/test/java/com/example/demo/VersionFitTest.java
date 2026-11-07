package com.example.demo;

import com.example.demo.core.mod.VersionFit;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 社区资源版本与当前实例的适配判断。 */
class VersionFitTest {

    private VersionFit.Candidate candidate(String number, String type, List<String> games, List<String> loaders) {
        return new VersionFit.Candidate(number, type, games, loaders);
    }

    @Test
    void detectLoaderPrefersTheMoreSpecificLoader() {
        assertEquals(VersionFit.Loader.NEOFORGE,
                VersionFit.detectLoader("cpw.mods.bootstraplauncher.BootstrapLauncher",
                        List.of("net.neoforged:neoforge:66.1.0", "net.minecraftforge:forgespi")));
        assertEquals(VersionFit.Loader.QUILT,
                VersionFit.detectLoader("org.quiltmc.loader.impl.launch.knot.KnotClient",
                        List.of("org.quiltmc:quilt-loader:0.20.0", "net.fabricmc:intermediary")));
        assertEquals(VersionFit.Loader.FABRIC,
                VersionFit.detectLoader("net.fabricmc.loader.impl.launch.knot.KnotClient",
                        List.of("net.fabricmc:fabric-loader:0.15.11")));
        assertEquals(VersionFit.Loader.FORGE,
                VersionFit.detectLoader("net.minecraftforge.client.loading.ClientModLoader",
                        List.of("net.minecraftforge:forge:47.4.10")));
        assertEquals(VersionFit.Loader.VANILLA,
                VersionFit.detectLoader("net.minecraft.client.main.Main", List.of("com.mojang:authlib:1.5.25")));
        assertEquals(VersionFit.Loader.VANILLA, VersionFit.detectLoader(null, null));
    }

    @Test
    void compatibleNeedsMatchingGameVersionAndLoader() {
        VersionFit.Target target = VersionFit.Target.of("1.12.2", VersionFit.Loader.VANILLA);
        assertTrue(VersionFit.compatible(candidate("1.0", "release", List.of("1.12.2"), List.of("datapack")), target));
        assertFalse(VersionFit.compatible(candidate("2.0", "release", List.of("26.3"), List.of("neoforge")), target));

        VersionFit.Target fabric = VersionFit.Target.of("1.20.1", VersionFit.Loader.FABRIC);
        assertTrue(VersionFit.compatible(candidate("1", "release", List.of("1.20.1"), List.of("fabric")), fabric));
        assertFalse(VersionFit.compatible(candidate("1", "release", List.of("1.20.1"), List.of("forge")), fabric));
    }

    @Test
    void emptyConstraintsAreTreatedAsCompatible() {
        VersionFit.Target unknown = VersionFit.Target.of("", VersionFit.Loader.VANILLA);
        assertTrue(VersionFit.compatible(candidate("1", "release", List.of("1.20.1"), List.of("fabric")), unknown),
                "目标没有游戏版本时不应排除任何版本");
        assertTrue(VersionFit.compatible(candidate("1", "release", List.of(), List.of()), unknown));
    }

    @Test
    void rankedPutsCompatibleReleaseFirst() {
        VersionFit.Target target = VersionFit.Target.of("1.12.2", VersionFit.Loader.VANILLA);
        List<VersionFit.Candidate> versions = List.of(
                candidate("26.3-neoforge", "release", List.of("26.3"), List.of("neoforge")),
                candidate("1.12.2-beta", "beta", List.of("1.12.2"), List.of("datapack")),
                candidate("1.20.1-release", "release", List.of("1.20.1"), List.of("datapack")),
                candidate("1.12.2-release", "release", List.of("1.12.2"), List.of("datapack")));
        List<Integer> order = VersionFit.ranked(versions, target);
        assertEquals(3, order.get(0), "1.12.2 的正式版应排第一");
        assertEquals(1, order.get(1), "1.12.2 的 beta 排第二");
        assertEquals(3, VersionFit.bestMatch(versions, target));
    }

    @Test
    void bestMatchFallsBackToMinusOne() {
        VersionFit.Target target = VersionFit.Target.of("1.7.10", VersionFit.Loader.FORGE);
        List<VersionFit.Candidate> versions = List.of(
                candidate("a", "release", List.of("1.20.1"), List.of("fabric")),
                candidate("b", "release", List.of("26.3"), List.of("neoforge")));
        assertEquals(-1, VersionFit.bestMatch(versions, target));
        assertEquals(-1, VersionFit.bestMatch(List.of(), target));
        assertEquals(-1, VersionFit.bestMatch(null, target));
    }

    @Test
    void fitLabelExplainsTheMismatch() {
        VersionFit.Target target = VersionFit.Target.of("1.12.2", VersionFit.Loader.VANILLA);
        assertEquals("适配当前实例",
                VersionFit.fitLabel(candidate("1", "release", List.of("1.12.2"), List.of()), target));
        assertEquals("需要 MC 26.3",
                VersionFit.fitLabel(candidate("2", "release", List.of("26.3"), List.of()), target));
        assertEquals("",
                VersionFit.fitLabel(candidate("1", "release", List.of("1.12.2"), List.of()), null));
    }

    @Test
    void targetLabelReadsNaturally() {
        assertEquals("1.12.2 · 原版", VersionFit.Target.of("1.12.2", VersionFit.Loader.VANILLA).label());
        assertEquals("1.20.1 · Fabric", VersionFit.Target.of("1.20.1", VersionFit.Loader.FABRIC).label());
        assertEquals("未指定版本 · 原版", VersionFit.Target.of(null, null).label());
    }
}
