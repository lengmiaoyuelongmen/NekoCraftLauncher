package com.example.demo.core.launch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 启动准备管线：按顺序执行 {@link LaunchTask}，每步前检查取消、每步后记录耗时。
 *
 * <p>对标 Prism 的 LaunchTask 链 / PCL2 的 LoaderTask 链：把「检查 → 补齐文件 → 解压 → 起进程」
 * 拆成互不依赖的小步，UI 只订阅一条管线，不用再关心中间细节。
 *
 * <p>失败策略是「快速失败」：某一步抛异常，后面的步骤不再执行，异常原样上抛。
 */
public final class LaunchPipeline {

    private final List<LaunchTask> tasks = new ArrayList<>();

    public LaunchPipeline add(LaunchTask task) {
        tasks.add(Objects.requireNonNull(task, "task"));
        return this;
    }

    /** 已注册的任务名，测试与日志用。 */
    public List<String> taskNames() {
        List<String> names = new ArrayList<>(tasks.size());
        for (LaunchTask task : tasks) {
            names.add(task.name());
        }
        return Collections.unmodifiableList(names);
    }

    public int size() {
        return tasks.size();
    }

    /**
     * 依次执行所有任务。
     *
     * @throws Exception 某一步失败时原样抛出；用户取消抛 {@link LaunchCancelledException}
     */
    public void run(LaunchContext context) throws Exception {
        for (LaunchTask task : tasks) {
            context.checkCancelled();
            long startedAt = System.nanoTime();
            task.run(context);
            long elapsedMs = Math.max(0L, (System.nanoTime() - startedAt) / 1_000_000L);
            context.log("⏱ " + task.name() + " " + elapsedMs + " ms");
        }
    }
}
