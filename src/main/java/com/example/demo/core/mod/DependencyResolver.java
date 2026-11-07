package com.example.demo.core.mod;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * 递归补齐模组前置：装完一批后重新扫描，把新暴露出来的前置继续装，直到没有缺失或达到上限。
 *
 * <p>只做调度，不碰网络与文件；安装动作与「还缺什么」都由调用方注入，便于单测。
 */
public final class DependencyResolver {

    /** 安装一个前置。 */
    @FunctionalInterface
    public interface Fetcher {
        DependencyInstaller.Result install(String modId);
    }

    /** 重新扫描，返回「还缺什么」。 */
    @FunctionalInterface
    public interface Scanner {
        Map<String, List<String>> missing();
    }

    /** 一次补齐的结果。 */
    public record Summary(int installed, int failed, int skipped, int passes, boolean complete) {
        public String message() {
            StringBuilder text = new StringBuilder();
            text.append("安装 ").append(installed).append(" 个");
            if (failed > 0) {
                text.append("，失败 ").append(failed).append(" 个");
            }
            if (skipped > 0) {
                text.append("，跳过 ").append(skipped).append(" 个");
            }
            text.append("，共 ").append(passes).append(" 轮");
            text.append(complete ? "；依赖已补齐" : "；仍有缺失");
            return text.toString();
        }
    }

    private DependencyResolver() {
    }

    /**
     * 反复安装直到没有缺失。
     *
     * @param initial 一开始发现的前置 id
     * @param fetcher 安装动作
     * @param scanner 重新扫描（每轮结束后调用）
     * @param maxInstalls 最多安装多少个，防止依赖环或异常数据把流程拖死
     * @param log 进度回调（可为 null）
     */
    public static Summary resolve(
            List<String> initial,
            Fetcher fetcher,
            Scanner scanner,
            int maxInstalls,
            Consumer<String> log) {
        Deque<String> queue = new ArrayDeque<>();
        Set<String> seen = new LinkedHashSet<>();
        if (initial != null) {
            for (String id : initial) {
                if (id != null && !id.isBlank() && seen.add(id)) {
                    queue.add(id);
                }
            }
        }
        int installed = 0;
        int failed = 0;
        int skipped = 0;
        int passes = 0;
        int limit = Math.max(1, maxInstalls);

        while (!queue.isEmpty()) {
            passes++;
            List<String> batch = new ArrayList<>(queue);
            queue.clear();
            for (String modId : batch) {
                if (installed + failed >= limit) {
                    skipped += batch.size() - batch.indexOf(modId);
                    if (log != null) {
                        log.accept("⚠ 已达单次安装上限 " + limit + " 个，剩余前置留待下次启动处理");
                    }
                    break;
                }
                DependencyInstaller.Result result = fetcher.install(modId);
                if (result != null && result.ok()) {
                    installed++;
                } else {
                    failed++;
                }
                if (log != null) {
                    log.accept((result != null && result.ok() ? "✅ " : "❌")
                            + " " + modId + "：" + (result == null ? "无结果" : result.message()));
                }
            }
            if (installed + failed >= limit) {
                break;
            }
            // 重新扫描：新装进来的前置可能又带来新的依赖
            Map<String, List<String>> stillMissing = scanner == null ? Map.of() : scanner.missing();
            for (String next : stillMissing.keySet()) {
                if (next != null && !next.isBlank() && seen.add(next)) {
                    queue.add(next);
                }
            }
        }
        Map<String, List<String>> left = scanner == null ? Map.of() : scanner.missing();
        boolean complete = left.isEmpty();
        return new Summary(installed, failed, skipped, passes, complete);
    }
}
