package com.example.demo;

import com.example.demo.core.launch.LaunchCancelledException;
import com.example.demo.core.launch.LaunchContext;
import com.example.demo.core.launch.LaunchPipeline;
import com.example.demo.core.launch.LaunchTask;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 启动管线：顺序、耗时日志、失败即停、取消。 */
class LaunchPipelineTest {

    private LaunchTask task(String name, Runnable body) {
        return new LaunchTask() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public void run(LaunchContext context) {
                body.run();
            }
        };
    }

    private LaunchContext context(Path temp, List<String> log, AtomicBoolean cancelled) {
        return new LaunchContext(null, temp.toFile(), temp.toFile(),
                log::add, null, cancelled::get);
    }

    @Test
    void runsTasksInOrder(@TempDir Path temp) throws Exception {
        List<String> events = new ArrayList<>();
        LaunchPipeline pipeline = new LaunchPipeline()
                .add(task("客户端 jar", () -> events.add("jar")))
                .add(task("依赖库", () -> events.add("libs")))
                .add(task("原生库", () -> events.add("natives")));

        List<String> log = new ArrayList<>();
        pipeline.run(context(temp, log, new AtomicBoolean(false)));

        assertEquals(List.of("jar", "libs", "natives"), events, "任务必须按注册顺序执行");
        assertEquals(List.of("客户端 jar", "依赖库", "原生库"), pipeline.taskNames());
        assertEquals(3, pipeline.size());
    }

    @Test
    void logsOneTimingLinePerTask(@TempDir Path temp) throws Exception {
        List<String> log = new ArrayList<>();
        new LaunchPipeline()
                .add(task("依赖库", () -> { }))
                .add(task("资源文件", () -> { }))
                .run(context(temp, log, new AtomicBoolean(false)));

        assertEquals(2, log.size(), "每个任务一行耗时");
        assertTrue(log.get(0).startsWith("⏱ 依赖库 "), log.get(0));
        assertTrue(log.get(0).endsWith(" ms"), log.get(0));
        assertTrue(log.get(1).startsWith("⏱ 资源文件 "), log.get(1));
    }

    @Test
    void stopsAtFirstFailure(@TempDir Path temp) {
        List<String> events = new ArrayList<>();
        LaunchPipeline pipeline = new LaunchPipeline()
                .add(task("ok", () -> events.add("ok")))
                .add(task("boom", () -> {
                    throw new IllegalStateException("下载失败");
                }))
                .add(task("never", () -> events.add("never")));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> pipeline.run(context(temp, new ArrayList<>(), new AtomicBoolean(false))));

        assertEquals("下载失败", error.getMessage());
        assertEquals(List.of("ok"), events, "失败之后的任务不能再跑");
    }

    @Test
    void cancellationStopsBeforeRunningTasks(@TempDir Path temp) {
        List<String> events = new ArrayList<>();
        LaunchPipeline pipeline = new LaunchPipeline()
                .add(task("a", () -> events.add("a")));

        assertThrows(LaunchCancelledException.class,
                () -> pipeline.run(context(temp, new ArrayList<>(), new AtomicBoolean(true))));

        assertTrue(events.isEmpty(), "已取消时不应该执行任何任务");
    }

    @Test
    void cancellationBetweenTasksThrows(@TempDir Path temp) {
        AtomicBoolean cancelled = new AtomicBoolean(false);
        List<String> events = new ArrayList<>();
        LaunchPipeline pipeline = new LaunchPipeline()
                .add(task("first", () -> {
                    events.add("first");
                    cancelled.set(true); // 模拟用户在这一步点了「取消启动」
                }))
                .add(task("second", () -> events.add("second")));

        assertThrows(LaunchCancelledException.class,
                () -> pipeline.run(context(temp, new ArrayList<>(), cancelled)));

        assertEquals(List.of("first"), events);
        assertTrue(cancelled.get(), "取消标志应保持为真");
    }
}
