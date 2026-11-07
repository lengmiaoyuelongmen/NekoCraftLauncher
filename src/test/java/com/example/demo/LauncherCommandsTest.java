package com.example.demo;

import com.example.demo.core.app.LauncherCommands;
import com.example.demo.core.app.LauncherCommands.Parsed;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 命令面板的命令解析。 */
class LauncherCommandsTest {

    @Test
    void emptyInputIsIgnored() {
        assertInstanceOf(LauncherCommands.Empty.class, LauncherCommands.parse(""));
        assertInstanceOf(LauncherCommands.Empty.class, LauncherCommands.parse("   "));
        assertInstanceOf(LauncherCommands.Empty.class, LauncherCommands.parse(null));
    }

    @Test
    void slashGoesToGameChat() {
        Parsed parsed = LauncherCommands.parse("/give @s diamond 64");
        LauncherCommands.GameCommand command = assertInstanceOf(LauncherCommands.GameCommand.class, parsed);
        assertEquals("give @s diamond 64", command.command());
    }

    @Test
    void simpleCommands() {
        assertInstanceOf(LauncherCommands.Help.class, LauncherCommands.parse("help"));
        assertInstanceOf(LauncherCommands.Help.class, LauncherCommands.parse("?"));
        assertInstanceOf(LauncherCommands.Launch.class, LauncherCommands.parse("LAUNCH"));
        assertInstanceOf(LauncherCommands.Kill.class, LauncherCommands.parse("kill"));
        assertInstanceOf(LauncherCommands.Versions.class, LauncherCommands.parse("versions"));
        assertInstanceOf(LauncherCommands.Java.class, LauncherCommands.parse("java"));
        assertInstanceOf(LauncherCommands.Crash.class, LauncherCommands.parse("crash"));
        assertInstanceOf(LauncherCommands.Clear.class, LauncherCommands.parse("clear"));
    }

    @Test
    void memoryParsesAndValidatesRange() {
        assertEquals(4096, assertInstanceOf(LauncherCommands.Memory.class,
                LauncherCommands.parse("memory 4096")).mb());
        assertEquals(LauncherCommands.MIN_MEMORY_MB, assertInstanceOf(LauncherCommands.Memory.class,
                LauncherCommands.parse("memory 512")).mb());
        assertEquals(LauncherCommands.MAX_MEMORY_MB, assertInstanceOf(LauncherCommands.Memory.class,
                LauncherCommands.parse("memory 32768")).mb());

        assertInstanceOf(LauncherCommands.InvalidMemory.class, LauncherCommands.parse("memory 511"));
        assertInstanceOf(LauncherCommands.InvalidMemory.class, LauncherCommands.parse("memory 65536"));
        assertInstanceOf(LauncherCommands.InvalidMemory.class, LauncherCommands.parse("memory abc"));
        assertInstanceOf(LauncherCommands.InvalidMemory.class, LauncherCommands.parse("memory"));
    }

    @Test
    void openAndImportKeepArguments() {
        assertEquals("", assertInstanceOf(LauncherCommands.Open.class,
                LauncherCommands.parse("open")).sub());
        assertEquals("saves", assertInstanceOf(LauncherCommands.Open.class,
                LauncherCommands.parse("open saves")).sub());
        assertEquals("C:/packs/a.mrpack", assertInstanceOf(LauncherCommands.Import.class,
                LauncherCommands.parse("import C:/packs/a.mrpack")).path());
    }

    @Test
    void unknownCommandKeepsNameLowercased() {
        LauncherCommands.Unknown unknown = assertInstanceOf(LauncherCommands.Unknown.class,
                LauncherCommands.parse("FooBar baz"));
        assertEquals("foobar", unknown.name());
    }

    @Test
    void helpTextMentionsEveryCommand() {
        String help = LauncherCommands.helpText();
        for (String command : new String[]{"help", "launch", "kill", "versions", "java",
                "memory", "open", "crash", "import", "clear"}) {
            assertTrue(help.contains(command), "帮助里应包含 " + command);
        }
        assertTrue(LauncherCommands.memoryUsage().contains("512"));
    }
}
