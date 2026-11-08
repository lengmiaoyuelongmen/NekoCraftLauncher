package com.example.demo.core.mod;

/**
 * 批量删除的结果：成功几个、失败几个、该怎么提示。
 *
 * <p>以前无论删掉几个都弹「已删除 N 个」，文件被游戏占用时用户会以为已经删干净。
 */
public record BulkDelete(int attempted, int deleted) {

    public BulkDelete {
        attempted = Math.max(0, attempted);
        deleted = Math.max(0, Math.min(deleted, attempted));
    }

    public int failed() {
        return attempted - deleted;
    }

    public boolean complete() {
        return failed() == 0;
    }

    /** 写进日志的一行。 */
    public String logMessage(String what) {
        if (failed() == 0) {
            return "🗑️ 已删除 " + deleted + " 个" + what;
        }
        return "🗑️ 已删除 " + deleted + " 个" + what + "，失败 " + failed() + " 个（可能被占用）";
    }

    /** 弹给用户的一句话。 */
    public String toastMessage(String what) {
        if (complete()) {
            return "已删除 " + deleted + " 个" + what;
        }
        return "有 " + failed() + " 个" + what + "删除失败，可能被游戏或资源管理器占用";
    }

    /** 失败时应该用错误提示，而不是"成功"。 */
    public boolean shouldWarn() {
        return !complete();
    }
}
