package com.example.demo;

import com.example.demo.core.i18n.I18n;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 界面文案的守卫：中文字面量只降不升，且中英语言包条目对齐。
 *
 * <p>为什么要有这条测试：i18n 不是一次性的活儿，只要没人拦着，新页面就会继续写
 * {@code new Label("中文")}。这里的口径是「已经包进 {@code I18n.t(key, "中文")} 的不算欠账，
 * 其余的中文字面量算」——所以翻译一个页面，基线数字就该往下掉；想加新页面，请顺手写 key。
 */
class I18nCoverageTest {

    /** 与 scripts/i18n-report.ps1 保持同一口径。 */
    private static final Pattern WRAPPED = Pattern.compile("I18n\\.t\\(\\s*\"[^\"]*\"\\s*,\\s*\"[^\"]*\"\\s*\\)");
    private static final Pattern CHINESE_LITERAL =
            Pattern.compile("\"([^\"\\\\\\r\\n]*[\\u4e00-\\u9fff][^\"\\\\\\r\\n]*)\"");

    private static Path projectRoot() {
        Path fromUserDir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        if (Files.isDirectory(fromUserDir.resolve("src/main/java/com/example/demo/ui"))) {
            return fromUserDir;
        }
        return fromUserDir.getParent();
    }

    @Test
    void hardCodedChineseDoesNotGrowBeyondTheBaseline() throws IOException {
        Path ui = projectRoot().resolve("src/main/java/com/example/demo/ui");
        assertTrue(Files.isDirectory(ui), "找不到 UI 源码目录：" + ui);

        int total = 0;
        List<String> offenders = new ArrayList<>();
        List<Path> sources;
        try (Stream<Path> stream = Files.walk(ui)) {
            sources = stream.filter(path -> path.toString().endsWith(".java")).toList();
        }
        for (Path file : sources) {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                Matcher matcher = CHINESE_LITERAL.matcher(WRAPPED.matcher(line).replaceAll("I18n.t(\"\")"));
                while (matcher.find()) {
                    total++;
                    if (offenders.size() < 12) {
                        offenders.add(file.getFileName() + ": " + matcher.group(1));
                    }
                }
            }
        }

        JsonObject baseline = JsonParser.parseString(Files.readString(
                projectRoot().resolve("scripts/i18n-baseline.json"), StandardCharsets.UTF_8)).getAsJsonObject();
        int allowed = baseline.get("total").getAsInt();

        assertTrue(total <= allowed,
                "界面里的中文字面量从基线 " + allowed + " 涨到了 " + total
                        + "。新文案请写成 I18n.t(\"key\", \"中文\")，例如：\n  "
                        + String.join("\n  ", offenders));
    }

    @Test
    void everyChineseKeyHasAnEnglishTranslation() {
        Set<String> zh = keys(Locale.SIMPLIFIED_CHINESE);
        Set<String> en = keys(Locale.US);

        Set<String> missing = new TreeSet<>(zh);
        missing.removeAll(en);

        assertTrue(missing.isEmpty(), "英文语言包缺少这些 key：" + missing);
        assertTrue(zh.size() >= 20, "语言包条目太少，可能没加载到：" + zh.size());
    }

    @Test
    void navigationLayerIsTranslatedInEnglish() {
        I18n.setLocale(I18n.EN);
        try {
            // 顶栏 5 个入口 + 页面标题 + 运行状态，是「英文界面能用」的最低要求
            String[] keys = {"nav.launch", "nav.download", "nav.advisor", "nav.settings", "nav.tools",
                    "page.download.title", "page.instances.title", "page.instanceSettings.title",
                    "page.advisor.title", "page.health.title", "page.worlds.title",
                    "status.ready", "status.running", "action.stopGame", "action.cancelLaunch"};
            for (String key : keys) {
                String value = I18n.t(key);
                assertEquals(value, I18n.t(key, value));
                assertTrue(!value.equals(key), "缺少英文条目：" + key);
                assertTrue(!value.matches(".*[\\u4e00-\\u9fff].*"), "英文条目里仍有中文：" + key + "=" + value);
            }
        } finally {
            I18n.setLocale(I18n.ZH);
        }
    }

    private static Set<String> keys(Locale locale) {
        ResourceBundle bundle = ResourceBundle.getBundle("i18n.messages", locale);
        return new TreeSet<>(bundle.keySet());
    }
}
