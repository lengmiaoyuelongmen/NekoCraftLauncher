package com.example.demo.core.launch;

/**
 * 体检面板里两行结论的纯逻辑：Java 够不够、内存够不够。
 *
 * <p>抽出来是为了让文案和阈值可测——UI 只负责把结果画成一行。
 */
public final class HealthAdvice {

    /**
     * @param ok 是否正常（false 时面板显示橙色提示）
     * @param level 左侧标签
     * @param message 说明文字
     */
    public record Line(boolean ok, String level, String message) {
    }

    private HealthAdvice() {
    }

    /** 版本没有声明 Java 要求。 */
    public static final int NO_REQUIREMENT = 0;

    /**
     * @param required 版本 JSON 要求的 Java 主版本（0 = 未知）
     * @param available 扫描到的最合适 Java 主版本（0 = 没找到）
     */
    public static Line javaLine(int required, int available) {
        if (required <= 0) {
            return new Line(true, "未知", "该版本没有写明 Java 要求，启动时按主类自动选择");
        }
        if (available <= 0) {
            return new Line(
                    false,
                    "缺 Java",
                    "没找到 Java " + required + "，启动时会自动下载（也可以在「设置 → Java」手动指定）");
        }
        if (available < required) {
            return new Line(
                    false,
                    "版本过低",
                    "只找到 Java " + available + "，该版本需要 Java " + required + " 或更高");
        }
        return new Line(true, "正常", "Java " + available + "（该版本要求 " + required + "）");
    }

    /**
     * @param configured 实际会用的内存（自动分配时传建议值）
     * @param suggested 建议内存
     * @param auto 是否自动分配
     * @param reason 建议理由
     */
    public static Line memoryLine(int configured, int suggested, boolean auto, String reason) {
        String trimmed =
                reason == null ? "" : reason.replaceAll("→\\s*建议\\s*\\d+\\s*MB.*$", "").trim();
        String base =
                (auto ? "自动分配 " : "手动设置 ")
                        + configured
                        + " MB（建议 "
                        + suggested
                        + " MB"
                        + (trimmed.isEmpty() ? "" : "，" + trimmed)
                        + "）";
        // 低于建议的 75% 才提示：差一点不影响游玩，别制造焦虑
        boolean ok = configured >= Math.max(1024, (int) Math.round(suggested * 0.75));
        if (ok) {
            return new Line(true, "正常", base);
        }
        return new Line(false, "偏低", base + "，大型整合包可能卡顿或 OOM");
    }
}
