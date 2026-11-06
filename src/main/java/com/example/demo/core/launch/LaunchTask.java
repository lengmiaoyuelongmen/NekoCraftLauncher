package com.example.demo.core.launch;

/**
 * 启动管线里的一步。实现类只依赖 {@link LaunchContext}，不碰 UI。
 *
 * <p>约定：可以抛异常表示这一步失败（例如依赖库下载失败），管线会把它继续上抛；
 * 用户取消用 {@link LaunchContext#checkCancelled()}。
 */
public interface LaunchTask {

    /** 写进日志/耗时统计的名字，例如「依赖库」。 */
    String name();

    void run(LaunchContext context) throws Exception;
}
