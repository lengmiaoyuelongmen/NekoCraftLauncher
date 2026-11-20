package com.example.demo.core.plugin;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * 插件事件总线：宿主在关键节点 {@link #fire}，插件通过 {@link #register} 订阅。
 *
 * <p>三条硬规则：
 * <ol>
 *   <li>插件异常不会冒泡 —— 一个坏插件不该让游戏启动失败；</li>
 *   <li>订阅者是弱顺序执行（注册顺序），不做并发广播，避免 UI 线程外的竞态；</li>
 *   <li>事件只在启动器真正做了那件事之后发（页面切换完成、进程已退出）。</li>
 * </ol>
 */
public final class PluginHooks {

    private static final Map<PluginHook, List<Consumer<String>>> LISTENERS = new ConcurrentHashMap<>();
    private static volatile BiConsumer<String, Throwable> errorReporter;

    private PluginHooks() {
    }

    /** 设置错误上报（宿主传自己的日志方法进来）。 */
    public static void setErrorReporter(BiConsumer<String, Throwable> reporter) {
        errorReporter = reporter;
    }

    /** 订阅一个事件。 */
    public static void register(PluginHook hook, String pluginName, Consumer<String> listener) {
        if (hook == null || listener == null) {
            return;
        }
        LISTENERS.computeIfAbsent(hook, key -> new CopyOnWriteArrayList<>()).add(payload -> {
            try {
                listener.accept(payload);
            } catch (Throwable error) {
                BiConsumer<String, Throwable> reporter = errorReporter;
                if (reporter != null) {
                    reporter.accept("插件 " + pluginName + " 在 " + hook.id() + " 钩子上出错", error);
                }
            }
        });
    }

    /** 触发事件；没有订阅者时是空操作。 */
    public static void fire(PluginHook hook, String payload) {
        List<Consumer<String>> listeners = LISTENERS.get(hook);
        if (listeners == null || listeners.isEmpty()) {
            return;
        }
        String safePayload = payload == null ? "" : payload;
        for (Consumer<String> listener : listeners) {
            listener.accept(safePayload);
        }
    }

    /** 某个事件当前有多少订阅者（测试与「插件」页展示用）。 */
    public static int listenerCount(PluginHook hook) {
        List<Consumer<String>> listeners = LISTENERS.get(hook);
        return listeners == null ? 0 : listeners.size();
    }

    /** 清空订阅（测试用；正常运行时插件只在启动阶段注册一次）。 */
    public static void reset() {
        LISTENERS.clear();
    }
}
