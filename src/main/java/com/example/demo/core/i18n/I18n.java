package com.example.demo.core.i18n;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * 启动器界面文案。
 *
 * <p>现状（2.10.0）：先把「壳层」接进来——顶栏导航、运行状态、结束/取消按钮、命令面板提示、
 * 设置里的语言页。页面正文仍是中文，切换语言后不会变成半句英文的残页。
 *
 * <p>设计上只有两条规则：
 * <ul>
 *   <li>找不到 key 就原样返回 key（宁可露出 {@code nav.launch} 也不抛异常把界面搞崩）；</li>
 *   <li>语言是进程级状态，切换后需要重启才完全生效——文案在控件创建时就取好了。</li>
 * </ul>
 */
public final class I18n {

    /** 默认语言。 */
    public static final String ZH = "zh_CN";
    /** 英文。 */
    public static final String EN = "en_US";

    private static volatile String locale = ZH;
    private static volatile ResourceBundle bundle = load(ZH);

    private I18n() {
    }

    /** 设置界面语言；{@code null}/空值按中文处理。 */
    public static void setLocale(String language) {
        String normalized = normalize(language);
        if (normalized.equals(locale) && bundle != null) {
            return;
        }
        locale = normalized;
        bundle = load(normalized);
    }

    /** 当前语言代码。 */
    public static String locale() {
        return locale;
    }

    /** 是否英文界面。 */
    public static boolean isEnglish() {
        return EN.equals(locale);
    }

    /**
     * 取一条文案。
     *
     * @param key 形如 {@code nav.launch}
     * @return 对应语言的值；缺失时返回 key 本身
     */
    public static String t(String key) {
        if (key == null || key.isBlank()) {
            return "";
        }
        ResourceBundle current = bundle;
        if (current != null) {
            try {
                return current.getString(key);
            } catch (MissingResourceException ignored) {
                // 落到下面的兜底
            }
        }
        return key;
    }

    /** 带默认值的取文案：英文缺条目时退回中文，避免露出 key。 */
    public static String t(String key, String fallbackChinese) {
        ResourceBundle current = bundle;
        if (current != null) {
            try {
                return current.getString(key);
            } catch (MissingResourceException ignored) {
                return fallbackChinese;
            }
        }
        return fallbackChinese;
    }

    static String normalize(String language) {
        if (language == null) {
            return ZH;
        }
        String value = language.trim().toLowerCase(Locale.ROOT);
        if (value.startsWith("en")) {
            return EN;
        }
        return ZH;
    }

    private static ResourceBundle load(String language) {
        Locale target = EN.equals(language) ? Locale.US : Locale.SIMPLIFIED_CHINESE;
        try {
            return ResourceBundle.getBundle("i18n.messages", target);
        } catch (MissingResourceException e) {
            return null;
        }
    }
}
