package com.example.demo.core.launch;

import com.example.demo.core.download.GameFiles;

/** 原版客户端 jar：缺了就没法启动，先镜像后官方源。 */
public final class ClientJarTask implements LaunchTask {

    private final String downloadSource;

    /** @param downloadSource auto / official / mirror */
    public ClientJarTask(String downloadSource) {
        this.downloadSource = downloadSource;
    }

    @Override
    public String name() {
        return "客户端 jar";
    }

    @Override
    public void run(LaunchContext context) throws Exception {
        GameFiles.ensureClientJar(
                context.profile(), context.gameDir(), downloadSource, context::log, context.progress());
    }
}
