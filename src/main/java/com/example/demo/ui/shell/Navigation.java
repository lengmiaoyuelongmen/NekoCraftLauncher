package com.example.demo.ui.shell;

import java.util.function.Consumer;

/**
 * 极简的页面跳转入口。
 *
 * <p>设置页这些子面板只拿得到 config，拿不到主窗口；用一个静态绑定把「跳到某页」
 * 暴露出去，比给每个面板塞一堆回调干净。
 */
public final class Navigation {

    private static Consumer<String> handler;

    private Navigation() {
    }

    public static void bind(Consumer<String> pageHandler) {
        handler = pageHandler;
    }

    public static void go(String page) {
        Consumer<String> current = handler;
        if (current != null && page != null) {
            current.accept(page);
        }
    }
}
