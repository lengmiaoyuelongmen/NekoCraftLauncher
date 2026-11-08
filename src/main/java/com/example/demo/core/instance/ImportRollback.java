package com.example.demo.core.instance;

/**
 * 导入失败/取消时该删什么。
 *
 * <p>关键约束：**不能删用户已有的实例目录**。导入可以复用一个已存在的实例（同一个
 * MC 版本 + 加载器），这时失败或取消只应清掉本次解包留下的临时内容，删掉实例目录
 * 会把玩家的 mods / config / saves 一起带走。
 */
public final class ImportRollback {

    /**
     * @param deleteInstanceDir 是否删除实例目录
     * @param deleteVersionDir 是否删除本次新装的版本目录
     */
    public record Plan(boolean deleteInstanceDir, boolean deleteVersionDir, String note) {
    }

    private ImportRollback() {
    }

    /**
     * @param instanceId 目标实例 id（可能为 null，表示还没确定）
     * @param createdVersion 本次导入新建的版本 id；复用时为 null
     */
    public static Plan planFor(String instanceId, String createdVersion) {
        if (instanceId == null || instanceId.isBlank()) {
            return new Plan(false, false, "");
        }
        if (createdVersion == null || createdVersion.isBlank()) {
            return new Plan(false, false, "复用的实例，保留原目录：" + instanceId);
        }
        boolean sameDir = createdVersion.equals(instanceId);
        return new Plan(
                !sameDir,
                true,
                sameDir
                        ? "清理本次新建的实例：" + instanceId
                        : "清理本次新建的实例 " + instanceId + " 与版本 " + createdVersion);
    }
}
