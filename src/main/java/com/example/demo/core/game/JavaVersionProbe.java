package com.example.demo.core.game;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 探测一个 java.exe 的 Java 主版本。
 *
 * <p>输出格式要兼容三种：
 * <ul>
 *   <li>{@code java version "1.8.0_401"} → 8
 *   <li>{@code openjdk version "17.0.2"} → 17
 *   <li>{@code openjdk version "21"} → 21
 * </ul>
 */
public final class JavaVersionProbe {

    private static final Pattern VERSION = Pattern.compile("version \"(\\d+)(?:\\.(\\d+))?");

    private JavaVersionProbe() {
    }

    /** 从 {@code java -version} 的输出里解析主版本；解析不出来返回 -1。 */
    public static int parse(String versionOutput) {
        if (versionOutput == null || versionOutput.isBlank()) {
            return -1;
        }
        Matcher matcher = VERSION.matcher(versionOutput);
        if (!matcher.find()) {
            return -1;
        }
        int major = Integer.parseInt(matcher.group(1));
        if (major == 1 && matcher.group(2) != null) {
            major = Integer.parseInt(matcher.group(2));
        }
        return major;
    }

    /** 实际执行 {@code java -version}；失败返回 -1。 */
    public static int probe(String javaPath) {
        if (javaPath == null || javaPath.isBlank()) {
            return -1;
        }
        try {
            Process process = new ProcessBuilder(javaPath, "-version")
                    .redirectErrorStream(true)
                    .start();
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append('\n');
                    int major = parse(line);
                    if (major > 0) {
                        return major;
                    }
                }
            }
            process.waitFor(5L, TimeUnit.SECONDS);
            return parse(output.toString());
        } catch (Exception e) {
            return -1;
        }
    }
}
