package com.example.demo.core.plugin;

/**
 * 插件可以订阅的宿主事件。
 *
 * <p>事件是「只通知、不干预」的：钩子抛异常会被吞掉并记一行日志，不会影响启动器本身。
 * 这样插件能做出「启动前提醒」「启动后统计」这类功能，又没有能力把主流程改坏。
 */
public enum PluginHook {

    /** 即将启动游戏；payload 是实例 id。 */
    LAUNCH_START("launch-start"),

    /** 游戏进程结束；payload 是实例 id。 */
    LAUNCH_FINISH("launch-finish"),

    /** 页面切换完成；payload 是页面 key（launch / download / settings …）。 */
    PAGE_SWITCH("page-switch");

    private final String id;

    PluginHook(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
