package com.example.demo;

public class MinecraftVersion {
    private String id;
    private String type;
    private String url;
    private String releaseTime;

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getUrl() {
        return url;
    }

    public String getReleaseTime() {
        return releaseTime;
    }

    // 【核心修复】让下拉框直接显示版本号（如 1.20.4）
    @Override
    public String toString() {
        return id;
    }

    /** 本地已安装的版本（没有远端 URL，type 标记为 installed）。 */
    public static MinecraftVersion installed(String id) {
        MinecraftVersion version = new MinecraftVersion();
        version.id = id;
        version.type = "installed";
        version.url = null;
        return version;
    }
}