package com.example.demo;

/** 用户点了「停止启动」时抛出，用于中断启动前的准备流程。 */
public final class LaunchCancelledException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public LaunchCancelledException() {
        super("启动已停止");
    }
}
