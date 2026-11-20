package com.example.demo.core.plugin;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** 插件事件总线：广播、顺序、异常隔离、清空。 */
class PluginHooksTest {

    @AfterEach
    void cleanup() {
        PluginHooks.reset();
        PluginHooks.setErrorReporter(null);
    }

    @Test
    void firesToEveryListenerInRegistrationOrder() {
        List<String> seen = new ArrayList<>();
        PluginHooks.register(PluginHook.PAGE_SWITCH, "a", payload -> seen.add("a:" + payload));
        PluginHooks.register(PluginHook.PAGE_SWITCH, "b", payload -> seen.add("b:" + payload));

        PluginHooks.fire(PluginHook.PAGE_SWITCH, "download");

        assertEquals(List.of("a:download", "b:download"), seen);
        assertEquals(2, PluginHooks.listenerCount(PluginHook.PAGE_SWITCH));
    }

    @Test
    void otherHooksAreNotTriggered() {
        List<String> pages = new ArrayList<>();
        List<String> launches = new ArrayList<>();
        PluginHooks.register(PluginHook.PAGE_SWITCH, "p", pages::add);
        PluginHooks.register(PluginHook.LAUNCH_START, "l", launches::add);

        PluginHooks.fire(PluginHook.LAUNCH_START, "1.12.2");

        assertEquals(List.of("1.12.2"), launches);
        assertEquals(List.of(), pages);
    }

    @Test
    void aBrokenPluginCannotBreakTheHostOrOtherPlugins() {
        List<String> seen = new ArrayList<>();
        AtomicReference<String> reportedWhere = new AtomicReference<>();
        PluginHooks.setErrorReporter((where, error) -> reportedWhere.set(where));

        PluginHooks.register(PluginHook.LAUNCH_START, "坏插件", payload -> {
            throw new IllegalStateException("boom");
        });
        PluginHooks.register(PluginHook.LAUNCH_START, "好插件", seen::add);

        PluginHooks.fire(PluginHook.LAUNCH_START, "26.3");

        assertEquals(List.of("26.3"), seen, "坏插件抛异常后，后续插件仍然收到事件");
        assertEquals("插件 坏插件 在 launch-start 钩子上出错", reportedWhere.get());
    }

    @Test
    void nullPayloadBecomesEmptyString() {
        AtomicReference<String> payload = new AtomicReference<>();
        PluginHooks.register(PluginHook.LAUNCH_FINISH, "x", payload::set);

        PluginHooks.fire(PluginHook.LAUNCH_FINISH, null);

        assertEquals("", payload.get());
    }

    @Test
    void firingWithoutListenersIsANoOp() {
        PluginHooks.fire(PluginHook.PAGE_SWITCH, "settings");
        assertEquals(0, PluginHooks.listenerCount(PluginHook.PAGE_SWITCH));
    }

    @Test
    void resetRemovesEverything() {
        PluginHooks.register(PluginHook.PAGE_SWITCH, "a", payload -> { });
        PluginHooks.reset();
        assertNull(null);
        assertEquals(0, PluginHooks.listenerCount(PluginHook.PAGE_SWITCH));
    }
}
