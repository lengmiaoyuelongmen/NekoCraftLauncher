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

    /**
     * Java 最大内存和物理内存是否匹配。
     *
     * @param maxHeapMb 配置（或自动分配）的最大堆内存
     * @param physicalMb 本机物理内存（0 = 读不到，跳过判断）
     */
    public static Line heapLine(int maxHeapMb, long physicalMb) {
        if (physicalMb <= 0 || maxHeapMb <= 0) {
            return new Line(true, "未知", "读不到物理内存，跳过内存上限检查");
        }
        // 给系统和显存留出余量：超过物理内存的 80% 就容易触发整机交换
        long safeLimit = Math.round(physicalMb * 0.8);
        String base = "-Xmx " + maxHeapMb + " MB（本机 " + physicalMb + " MB）";
        if (maxHeapMb > physicalMb) {
            return new Line(
                    false,
                    "超出物理内存",
                    base + "，超过本机内存，启动会失败或疯狂交换，建议降到 " + safeLimit + " MB 以内");
        }
        if (maxHeapMb > safeLimit) {
            return new Line(
                    false,
                    "偏高",
                    base + "，已占物理内存 80% 以上，系统和其他程序可能被挤爆，建议 " + safeLimit + " MB 以内");
        }
        return new Line(true, "正常", base);
    }

    /** 把体检结果渲染成可导出的文本。 */
    public static String render(String header, java.util.List<Line> lines) {
        StringBuilder out = new StringBuilder();
        if (header != null && !header.isBlank()) {
            out.append(header).append(System.lineSeparator());
            out.append("────────────────────").append(System.lineSeparator());
        }
        for (Line line : lines) {
            boolean plain = line.level() == null
                    || line.level().isBlank()
                    || "正常".equals(line.level());
            out.append(line.ok() ? "[正常] " : "[注意] ");
            if (!plain) {
                out.append(line.level()).append("  ");
            }
            out.append(line.message()).append(System.lineSeparator());
        }
        return out.toString();
    }}
