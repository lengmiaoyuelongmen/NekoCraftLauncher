package com.example.demo.core.launch;

import com.example.demo.core.model.InstanceDirs;

import java.io.File;

/** 原生库解压：把 libraries 里的 dll 放进该实例的 natives 目录。 */
public final class NativesTask implements LaunchTask {

    private final boolean isolation;

    /** @param isolation 是否启用版本隔离（决定 natives 放在 versions/&lt;id&gt; 还是游戏根目录） */
    public NativesTask(boolean isolation) {
        this.isolation = isolation;
    }

    @Override
    public String name() {
        return "原生库";
    }

    @Override
    public void run(LaunchContext context) {
        File gameDir = context.gameDir();
        File nativesDir = InstanceDirs.natives(gameDir, context.profile().getId(), isolation);
        int extracted = NativeExtractor.extract(
                context.profile(), new File(gameDir, "libraries"), nativesDir);
        if (extracted > 0) {
            context.log("[natives] 解压出 " + extracted + " 个 dll");
        }
    }
}
