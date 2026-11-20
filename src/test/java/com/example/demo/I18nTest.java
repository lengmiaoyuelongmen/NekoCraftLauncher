package com.example.demo;

import com.example.demo.core.i18n.I18n;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 界面文案：默认中文、切英文、缺 key 的兜底、两份语言包条目对齐。 */
class I18nTest {

    @AfterEach
    void restoreChinese() {
        I18n.setLocale(I18n.ZH);
    }

    @Test
    void defaultsToChinese() {
        I18n.setLocale(null);
        assertEquals(I18n.ZH, I18n.locale());
        assertEquals("启动", I18n.t("nav.launch"));
        assertEquals("就绪", I18n.t("status.ready"));
        assertFalse(I18n.isEnglish());
    }

    @Test
    void switchesToEnglish() {
        I18n.setLocale("en_US");
        assertTrue(I18n.isEnglish());
        assertEquals("Launch", I18n.t("nav.launch"));
        assertEquals("Downloads", I18n.t("nav.download"));
        assertEquals("Ready", I18n.t("status.ready"));
    }

    @Test
    void acceptsLanguageTagsLoosely() {
        I18n.setLocale("en-GB");
        assertTrue(I18n.isEnglish());
        I18n.setLocale("zh-Hans");
        assertFalse(I18n.isEnglish());
    }

    @Test
    void missingKeyReturnsTheKeyInsteadOfThrowing() {
        assertEquals("nav.does-not-exist", I18n.t("nav.does-not-exist"));
        assertEquals("", I18n.t(""));
        assertEquals("默认中文", I18n.t("nav.does-not-exist", "默认中文"));
    }

    @Test
    void englishBundleCoversEveryChineseKey() {
        Set<String> zh = keys(Locale.SIMPLIFIED_CHINESE);
        Set<String> en = keys(Locale.US);
        Set<String> missing = new TreeSet<>(zh);
        missing.removeAll(en);
        assertTrue(missing.isEmpty(), "英文语言包缺少：" + missing);
    }

    private static Set<String> keys(Locale locale) {
        ResourceBundle bundle = ResourceBundle.getBundle("i18n.messages", locale);
        return new TreeSet<>(bundle.keySet());
    }

    @Test
    void shellLabelsActuallyComeFromTheBundle() {
        I18n.setLocale(I18n.EN);
        for (String key : new String[] {"nav.launch", "nav.download", "nav.advisor",
                "nav.settings", "nav.tools", "status.running", "action.stopGame"}) {
            String value = I18n.t(key);
            assertFalse(value.equals(key), "缺少条目：" + key);
            assertFalse(value.matches(".*[\\u4e00-\\u9fff].*"), "英文包里不该有中文：" + key + "=" + value);
        }
    }
}
