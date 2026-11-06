package com.example.demo.core.launch;

import com.example.demo.core.download.GameFiles;

/** 依赖库 + 原生库 jar：按版本 JSON 的 sha1/size 增量补齐。 */
public final class LibrariesTask implements LaunchTask {

    @Override
    public String name() {
        return "依赖库";
    }

    @Override
    public void run(LaunchContext context) throws Exception {
        GameFiles.ensureLibraries(
                context.profile(), context.gameDir(), context::log, context.progress());
    }
}
