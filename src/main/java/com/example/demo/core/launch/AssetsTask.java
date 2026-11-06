package com.example.demo.core.launch;

import com.example.demo.core.download.GameFiles;

/** 资源文件（音效/语言/字体）：索引没变就整轮跳过。 */
public final class AssetsTask implements LaunchTask {

    @Override
    public String name() {
        return "资源文件";
    }

    @Override
    public void run(LaunchContext context) {
        GameFiles.ensureAssets(context.profile(), context.gameDir(), context::log, context.progress());
    }
}
