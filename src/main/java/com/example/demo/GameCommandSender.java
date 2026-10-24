package com.example.demo;

import java.util.concurrent.TimeUnit;

/**
 * 把命令发送到正在运行的 Minecraft 窗口：聚焦窗口 → 按 T 打开聊天 → 输入 → 回车。
 * 依赖游戏窗口能被找到并接受键盘输入（单人需开启作弊，服务器需要 OP）。
 * 失败只返回 false，不影响启动器。
 */
public final class GameCommandSender {

    private GameCommandSender() {
    }

    public static boolean send(Process process, String command) {
        if (process == null || !process.isAlive() || command == null || command.isBlank()) {
            return false;
        }
        String escaped = escape(command.trim());
        String script = """
                Add-Type -AssemblyName System.Windows.Forms
                Add-Type @"
                using System;
                using System.Runtime.InteropServices;
                public class WinCmd {
                    [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr hWnd);
                    [DllImport("user32.dll")] public static extern bool ShowWindow(IntPtr hWnd, int nCmdShow);
                }
                "@
                $p = Get-Process -Id %d -ErrorAction SilentlyContinue
                if ($null -eq $p) { exit 3 }
                $h = $p.MainWindowHandle
                if ($h -eq 0) { exit 4 }
                [WinCmd]::ShowWindow($h, 9) | Out-Null
                [WinCmd]::SetForegroundWindow($h) | Out-Null
                Start-Sleep -Milliseconds 350
                [System.Windows.Forms.SendKeys]::SendWait('t')
                Start-Sleep -Milliseconds 150
                [System.Windows.Forms.SendKeys]::SendWait('%s')
                Start-Sleep -Milliseconds 120
                [System.Windows.Forms.SendKeys]::SendWait('{ENTER}')
                """.formatted(process.pid(), escaped.replace("'", "''"));
        try {
            Process runner = new ProcessBuilder("powershell", "-NoProfile", "-NonInteractive",
                    "-WindowStyle", "Hidden", "-Command", script)
                    .redirectErrorStream(true)
                    .start();
            boolean finished = runner.waitFor(6, TimeUnit.SECONDS);
            return finished && runner.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /** SendKeys 特殊字符转义（+ ^ % ~ ( ) { } [ ]）。 */
    private static String escape(String text) {
        StringBuilder out = new StringBuilder();
        for (char c : text.toCharArray()) {
            switch (c) {
                case '+' -> out.append("{+}");
                case '^' -> out.append("{^}");
                case '%' -> out.append("{%}");
                case '~' -> out.append("{~}");
                case '(' -> out.append("{(}");
                case ')' -> out.append("{)}");
                case '{' -> out.append("{{}");
                case '}' -> out.append("{}}");
                case '[' -> out.append("{[}");
                case ']' -> out.append("{]}");
                default -> out.append(c);
            }
        }
        return out.toString();
    }
}
