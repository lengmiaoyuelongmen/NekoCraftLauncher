package com.example.demo.core.launch;

/**
 * 启动前「发现缺失文件 → 自动补全 → 汇报结果」这一步的结果与文案。
 *
 * <p>真正的下载修复仍由 {@code Integrity.repair} 完成；这里只记录补全前后数量，
 * 并把「补全了多少、还差多少、要不要问用户」算清楚，便于单测。
 *
 * @param total    发现的问题数
 * @param repaired 实际补全数
 */
public record AutoRepair(int total, int repaired) {

    public AutoRepair {
        if (total < 0) {
            throw new IllegalArgumentException("total 不能为负: " + total);
        }
        if (repaired < 0 || repaired > total) {
            throw new IllegalArgumentException("repaired 越界: " + repaired + "/" + total);
        }
    }

    /** 还没补全的数量。 */
    public int remaining() {
        return total - repaired;
    }

    /** 是否全部补全。 */
    public boolean complete() {
        return remaining() == 0;
    }

    /** 补全后仍需用户确认（还有缺失）。 */
    public boolean needsConfirmation() {
        return !complete();
    }

    /** 开始补全时的日志。 */
    public String startMessage() {
        return "🔧 启动前发现 " + total + " 个缺失文件，正在自动补全…";
    }

    /** 补全完成的日志。 */
    public String resultMessage() {
        return "🔧 已补全 " + repaired + "/" + total
                + (complete() ? "" : "，仍有 " + remaining() + " 个缺失");
    }
}
