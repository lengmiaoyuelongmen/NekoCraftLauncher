package com.example.demo.core.app;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.List;
import java.util.Locale;

/**
 * 全局代理设置。
 *
 * <p>java.net.http.HttpClient 默认使用 {@link ProxySelector#getDefault()}，
 * 所以在启动早期把它换掉，所有没显式指定 proxy 的请求（搜索、图标、下载、版本列表）
 * 都会走同一个代理，不用挨个改十几个 HttpClient。
 *
 * <p>支持的写法：{@code 127.0.0.1:7890}、{@code http://127.0.0.1:7890}、{@code socks5://127.0.0.1:7891}。
 * 留空即直连。
 */
public final class ProxySettings {

    private static volatile String current = "";
    private static final ProxySelector SYSTEM_DEFAULT = ProxySelector.getDefault();

    private ProxySettings() {
    }

    /** 应用（或清空）全局代理；非法写法会被忽略并保留直连。 */
    public static synchronized void apply(String raw) {
        current = raw == null ? "" : raw.trim();
        Parsed parsed = parse(current);
        if (parsed == null) {
            if (current.isEmpty()) {
                ProxySelector.setDefault(SYSTEM_DEFAULT);
            }
            return;
        }
        Proxy.Type type = parsed.socks ? Proxy.Type.SOCKS : Proxy.Type.HTTP;
        InetSocketAddress address = new InetSocketAddress(parsed.host, parsed.port);
        ProxySelector.setDefault(new ProxySelector() {
            @Override
            public List<Proxy> select(URI uri) {
                return List.of(new Proxy(type, address));
            }

            @Override
            public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
                // 失败就交给请求方处理，不在这里重试
            }
        });
    }

    /** 当前生效的写法（供设置页回显）。 */
    public static String current() {
        return current;
    }

    /** 校验写法是否可用（设置页保存时用）。 */
    public static boolean isValid(String raw) {
        String value = raw == null ? "" : raw.trim();
        return value.isEmpty() || parse(value) != null;
    }

    private record Parsed(String host, int port, boolean socks) {
    }

    private static Parsed parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim();
        boolean socks = value.toLowerCase(Locale.ROOT).startsWith("socks");
        int schemeEnd = value.indexOf("://");
        if (schemeEnd >= 0) {
            value = value.substring(schemeEnd + 3);
        }
        int slash = value.indexOf('/');
        if (slash >= 0) {
            value = value.substring(0, slash);
        }
        int colon = value.lastIndexOf(':');
        if (colon <= 0 || colon == value.length() - 1) {
            return null;
        }
        String host = value.substring(0, colon);
        String portText = value.substring(colon + 1);
        int port;
        try {
            port = Integer.parseInt(portText);
        } catch (NumberFormatException e) {
            return null;
        }
        if (host.isBlank() || port <= 0 || port > 65535) {
            return null;
        }
        return new Parsed(host, port, socks);
    }
}
