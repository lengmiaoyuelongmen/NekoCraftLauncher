package com.example.demo.core.app;

import java.util.Locale;

/**
 * 命令面板里的启动器命令解析：只把一行文字拆成「命令 + 参数」，不执行任何动作。
 *
 * <p>从主类的 executeCommand 里抽出来，便于单测；执行仍由界面层负责。
 */
public final class LauncherCommands {

    public static final int MIN_MEMORY_MB = 512;
    public static final int MAX_MEMORY_MB = 32768;

    /** 解析结果。 */
    public sealed interface Parsed {
    }

    /** 空输入（不产生任何动作，也不写历史）。 */
    public record Empty() implements Parsed {
    }

    /** 以 / 开头：发给游戏聊天。 */
    public record GameCommand(String command) implements Parsed {
    }

    public record Help() implements Parsed {
    }

    public record Launch() implements Parsed {
    }

    public record Kill() implements Parsed {
    }

    public record Versions() implements Parsed {
    }

    public record Java() implements Parsed {
    }

    /** 合法的内存设置。 */
    public record Memory(int mb) implements Parsed {
    }

    /** 内存参数非法（缺省、非数字、超范围）。 */
    public record InvalidMemory(String arg) implements Parsed {
    }

    /** 打开游戏目录或子目录；{@code sub} 为空表示根目录。 */
    public record Open(String sub) implements Parsed {
    }

    /** 导入整合包文件。 */
    public record Import(String path) implements Parsed {
    }

    public record Crash() implements Parsed {
    }

    public record Clear() implements Parsed {
    }

    public record Unknown(String name) implements Parsed {
    }

    private LauncherCommands() {
    }

    public static Parsed parse(String raw) {
        String text = raw == null ? "" : raw.trim();
        if (text.isEmpty()) {
            return new Empty();
        }
        if (text.startsWith("/")) {
            return new GameCommand(text.substring(1).trim());
        }
        String[] parts = text.split("\\s+", 2);
        String command = parts[0].toLowerCase(Locale.ROOT);
        String arg = parts.length > 1 ? parts[1].trim() : "";
        return switch (command) {
            case "help", "?" -> new Help();
            case "launch" -> new Launch();
            case "kill" -> new Kill();
            case "versions" -> new Versions();
            case "java" -> new Java();
            case "memory" -> memory(arg);
            case "open" -> new Open(arg);
            case "import" -> new Import(arg);
            case "crash" -> new Crash();
            case "clear" -> new Clear();
            default -> new Unknown(command);
        };
    }

    private static Parsed memory(String arg) {
        try {
            int mb = Integer.parseInt(arg);
            if (mb < MIN_MEMORY_MB || mb > MAX_MEMORY_MB) {
                return new InvalidMemory(arg);
            }
            return new Memory(mb);
        } catch (NumberFormatException e) {
            return new InvalidMemory(arg);
        }
    }

    /** help 命令的完整说明。 */
    public static String helpText() {
        return "⌘ 启动器命令：\n"
                + "  help                显示本帮助\n"
                + "  launch              启动当前实例\n"
                + "  kill                结束当前游戏进程\n"
                + "  versions            列出已安装版本\n"
                + "  java                显示 Java 路径\n"
                + "  memory <MB>         修改最大内存（下次启动生效）\n"
                + "  open [子目录]        打开游戏目录 / 指定子目录\n"
                + "  crash               诊断最近一次崩溃\n"
                + "  import <文件>        导入 .zip / .mrpack 整合包\n"
                + "  clear               清空日志\n"
                + "  /<命令>             发送到游戏聊天（需游戏窗口，单人需开作弊）\n";
    }

    /** memory 参数非法时的提示。 */
    public static String memoryUsage() {
        return "用法：memory 4096（" + MIN_MEMORY_MB + "–" + MAX_MEMORY_MB + " MB）\n";
    }
}
