package com.example.demo.core.launch;

/**
 * 服务器地址解析：「加入服务器」时把用户输入拆成 host + port，再拼成 {@code --server/--port}。
 *
 * <p>支持三种写法：
 * <ul>
 *   <li>{@code example.com} → 默认 25565
 *   <li>{@code example.com:25566}
 *   <li>{@code [::1]:25566}（IPv6 必须带方括号，否则冒号分不清是端口还是地址的一部分）
 * </ul>
 */
public record ServerAddress(String host, int port) {

    public static final int DEFAULT_PORT = 25565;

    /** @return 解析结果；地址为空或非法时返回 {@code null} */
    public static ServerAddress parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim();
        if (value.startsWith("[")) {
            int end = value.indexOf(']');
            if (end <= 1) {
                return null;
            }
            String host = value.substring(1, end);
            if (value.length() > end + 1 && value.charAt(end + 1) == ':') {
                int port = parsePort(value.substring(end + 2));
                return port < 0 ? null : new ServerAddress(host, port);
            }
            return new ServerAddress(host, DEFAULT_PORT);
        }
        int colon = value.lastIndexOf(':');
        if (colon > 0) {
            String host = value.substring(0, colon);
            int port = parsePort(value.substring(colon + 1));
            if (port < 0) {
                return null;
            }
            return new ServerAddress(host, port);
        }
        return new ServerAddress(value, DEFAULT_PORT);
    }

    /** 命令行参数：{@code --server host --port port}。 */
    public String[] launchArguments() {
        return new String[]{"--server", host, "--port", String.valueOf(port)};
    }

    private static int parsePort(String text) {
        try {
            int port = Integer.parseInt(text.trim());
            return port > 0 && port <= 65535 ? port : -1;
        } catch (Exception e) {
            return -1;
        }
    }
}
