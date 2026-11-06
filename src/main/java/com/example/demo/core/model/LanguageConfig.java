package com.example.demo.core.model;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * 游戏语言配置：首次启动把 {@code options.txt} 里的语言设成简体中文，已经是中文就不动。
 *
 * <p>只做「补一条 lang」这一件事，不覆盖用户的其它设置；写失败也不影响启动。
 */
public final class LanguageConfig {

    private LanguageConfig() {
    }

    public static void ensureChinese(File instanceDir) {
        if (instanceDir == null) {
            return;
        }
        try {
            if (!instanceDir.isDirectory() && !instanceDir.mkdirs()) {
                return;
            }
            File options = new File(instanceDir, "options.txt");
            if (!options.isFile()) {
                Files.writeString(options.toPath(), "lang:zh_cn\n", StandardCharsets.UTF_8);
                return;
            }
            String text = Files.readString(options.toPath(), StandardCharsets.UTF_8);
            text = text.contains("lang:")
                    ? text.replaceAll("(?m)^lang:.*$", "lang:zh_cn")
                    : text + (text.endsWith("\n") ? "" : "\n") + "lang:zh_cn\n";
            Files.writeString(options.toPath(), text, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            // 写不了 options.txt 不影响启动，游戏会用自己的默认值
        }
    }
}
