package com.example.demo;

import com.example.demo.core.mod.DependencyInstaller;
import com.example.demo.core.mod.DependencyResolver;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 递归补前置的调度：多轮发现、失败不死循环、上限保护。 */
class DependencyResolverTest {

    private static DependencyInstaller.Result ok(String id) {
        return DependencyInstaller.Result.ok(id, id + ".jar");
    }

    private static DependencyInstaller.Result fail(String id) {
        return DependencyInstaller.Result.fail(id, "没有适配版本");
    }

    @Test
    void installsEverythingInOneRoundWhenNothingNewAppears() {
        List<String> installed = new ArrayList<>();
        Map<String, List<String>> missing = new LinkedHashMap<>();
        DependencyResolver.Summary summary =
                DependencyResolver.resolve(
                        List.of("jei"),
                        id -> {
                            installed.add(id);
                            missing.remove(id);
                            return ok(id);
                        },
                        () -> missing,
                        10,
                        null);
        assertEquals(List.of("jei"), installed);
        assertEquals(1, summary.installed());
        assertEquals(0, summary.failed());
        assertTrue(summary.complete());
    }

    @Test
    void keepsGoingWhenNewDependenciesAppear() {
        List<String> installed = new ArrayList<>();
        // 装 a 之后冒出 b，装 b 之后收干净
        Map<String, List<String>> missing = new LinkedHashMap<>();
        missing.put("a", List.of("x"));
        DependencyResolver.Summary summary =
                DependencyResolver.resolve(
                        List.of("a"),
                        id -> {
                            installed.add(id);
                            missing.remove(id);
                            if ("a".equals(id)) {
                                missing.put("b", List.of("a"));
                            }
                            return ok(id);
                        },
                        () -> Map.copyOf(missing),
                        10,
                        null);
        assertEquals(List.of("a", "b"), installed);
        assertEquals(2, summary.installed());
        assertEquals(2, summary.passes());
        assertTrue(summary.complete());
    }

    @Test
    void failedInstallDoesNotLoopForever() {
        List<String> attempts = new ArrayList<>();
        Map<String, List<String>> missing = new LinkedHashMap<>();
        missing.put("ghost", List.of("mod"));
        DependencyResolver.Summary summary =
                DependencyResolver.resolve(
                        List.of("ghost"),
                        id -> {
                            attempts.add(id);
                            return fail(id);
                        },
                        () -> Map.copyOf(missing),
                        10,
                        null);
        assertEquals(List.of("ghost"), attempts, "同一个 id 只尝试一次");
        assertEquals(1, summary.failed());
        assertFalse(summary.complete());
    }

    @Test
    void respectsTheInstallBudget() {
        Map<String, List<String>> missing = new LinkedHashMap<>();
        for (int i = 0; i < 20; i++) {
            missing.put("dep" + i, List.of("mod"));
        }
        List<String> installed = new ArrayList<>();
        DependencyResolver.Summary summary =
                DependencyResolver.resolve(
                        new ArrayList<>(missing.keySet()),
                        id -> {
                            installed.add(id);
                            return ok(id);
                        },
                        () -> Map.copyOf(missing),
                        3,
                        null);
        assertEquals(3, installed.size(), "最多装 maxInstalls 个");
        assertEquals(3, summary.installed());
        assertTrue(summary.skipped() > 0);
    }

    @Test
    void duplicateInitialIdsAreCollapsed() {
        List<String> installed = new ArrayList<>();
        DependencyResolver.Summary summary =
                DependencyResolver.resolve(
                        List.of("a", "a", " ", "b"),
                        id -> {
                            installed.add(id);
                            return ok(id);
                        },
                        Map::of,
                        10,
                        null);
        assertEquals(List.of("a", "b"), installed);
        assertEquals(2, summary.installed());
        assertTrue(summary.complete());
    }

    @Test
    void summaryMessageReadsNaturally() {
        assertEquals("安装 2 个，共 1 轮；依赖已补齐",
                new DependencyResolver.Summary(2, 0, 0, 1, true).message());
        assertEquals("安装 1 个，失败 2 个，跳过 3 个，共 2 轮；仍有缺失",
                new DependencyResolver.Summary(1, 2, 3, 2, false).message());
    }
}
