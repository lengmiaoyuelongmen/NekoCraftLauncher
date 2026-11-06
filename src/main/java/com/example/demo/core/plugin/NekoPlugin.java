package com.example.demo.core.plugin;

/** 第三方插件入口：打包为 jar，放入 plugins/ 并通过 ServiceLoader 注册。 */
public interface NekoPlugin {
    String id();

    String name();

    void init(PluginContext context) throws Exception;
}
