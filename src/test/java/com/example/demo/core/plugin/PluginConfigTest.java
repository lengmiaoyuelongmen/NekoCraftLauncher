package com.example.demo.core.plugin;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 插件配置与上下文：默认值、落盘、重读、文件名安全。 */
class PluginConfigTest {

    @TempDir
    Path tempDir;

    @Test
    void missingKeysFallBackToDefaults() {
        PluginConfig config = new PluginConfig(new File(tempDir.toFile(), "empty.properties"));

        assertEquals("默认", config.get("nothing", "默认"));
        assertEquals(7, config.getInt("nothing", 7));
        assertEquals(7, config.getInt("bad", 7));
        config.put("bad", "not-a-number");
        assertEquals(7, config.getInt("bad", 7), "解析失败要退回默认值，而不是抛异常");
    }

    @Test
    void valuesSurviveAReload() {
        File file = new File(tempDir.toFile(), "counter.properties");
        PluginConfig first = new PluginConfig(file);
        first.putInt("launches", 3);
        first.put("note", "你好");
        assertTrue(first.save());

        PluginConfig second = new PluginConfig(file);
        assertEquals(3, second.getInt("launches", 0));
        assertEquals("你好", second.get("note", ""));
    }

    @Test
    void saveCreatesMissingDirectories() {
        File nested = new File(tempDir.toFile(), "a/b/c/config.properties");
        PluginConfig config = new PluginConfig(nested);
        config.put("k", "v");

        assertTrue(config.save());
        assertTrue(nested.isFile());
    }

    @Test
    void blankKeysAndNullValuesAreIgnored() {
        File file = new File(tempDir.toFile(), "x.properties");
        PluginConfig config = new PluginConfig(file);

        config.put("", "v");
        config.put("k", null);

        assertFalse(config.get("k", "").equals("v"));
        assertEquals("", config.get("", ""));
    }

    @Test
    void contextNamesTheFileAfterThePlugin() throws Exception {
        PluginContext context = new PluginContext(tempDir.toFile(), "Hello 插件", null);

        File file = context.config().file();

        assertTrue(file.getName().endsWith(".properties"));
        assertFalse(file.getName().contains(" "), "插件名里的空格要洗掉：" + file.getName());
        assertTrue(file.getParentFile().getAbsolutePath()
                .startsWith(tempDir.toFile().getAbsolutePath()));
        assertTrue(Files.isDirectory(tempDir.toFile().toPath()));
    }

    @Test
    void contextRegistersCommandsAndIgnoresBadOnes() {
        PluginContext context = new PluginContext(tempDir.toFile(), "p", null);

        context.addCommand("标题", "提示", () -> { });
        context.addCommand("", "空标题", () -> { });
        context.addCommand("没有动作", "x", null);

        assertEquals(1, context.commands().size());
        assertEquals("标题", context.commands().get(0).title());
    }

    @Test
    void contextHooksReachTheBus() {
        PluginHooks.reset();
        try {
            PluginContext context = new PluginContext(tempDir.toFile(), "钩子插件", null);
            List<String> pages = new ArrayList<>();
            context.on(PluginHook.PAGE_SWITCH, pages::add);

            PluginHooks.fire(PluginHook.PAGE_SWITCH, "tools");

            assertEquals(List.of("tools"), pages);
        } finally {
            PluginHooks.reset();
        }
    }
}
