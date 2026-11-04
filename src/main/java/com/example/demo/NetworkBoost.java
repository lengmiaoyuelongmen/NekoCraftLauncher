package com.example.demo;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 网络加速：给启动器下载走的路由做一点低风险优化。
 *
 * <p>只做两件事：
 * <ul>
 *   <li>探测几个公共 DNS（TCP 53 端口握手耗时），挑最快的那个
 *   <li>通过 {@code netsh} 把它设成本机当前联网网卡的 DNS（需要管理员权限），失败就只报告不修改
 * </ul>
 * 不做 hosts 改写、不装驱动、不动代理。
 */
public final class NetworkBoost {

    /** 候选 DNS：阿里 / 腾讯 / 114 / Cloudflare。 */
    public static final String[][] CANDIDATES = {
        {"阿里 DNS", "223.5.5.5"},
        {"腾讯 DNS", "119.29.29.29"},
        {"114 DNS", "114.114.114.114"},
        {"Cloudflare", "1.1.1.1"}
    };

    public record Probe(String name, String server, long latencyMs, boolean reachable) {}

    public record Result(boolean ok, String message, Probe chosen, String currentDns) {}

    private NetworkBoost() {
    }

    /** 并发探测候选 DNS，按延迟排序返回。 */
    public static List<Probe> probeAll() {
        List<CompletableFuture<Probe>> futures = new ArrayList<>();
        for (String[] candidate : CANDIDATES) {
            futures.add(
                    CompletableFuture.supplyAsync(
                            () -> {
                                long start = System.nanoTime();
                                try (Socket socket = new Socket()) {
                                    socket.connect(new InetSocketAddress(candidate[1], 53), 1500);
                                    long ms = (System.nanoTime() - start) / 1_000_000L;
                                    return new Probe(candidate[0], candidate[1], ms, true);
                                } catch (Exception e) {
                                    return new Probe(candidate[0], candidate[1], -1L, false);
                                }
                            }));
        }
        List<Probe> probes = new ArrayList<>();
        for (CompletableFuture<Probe> future : futures) {
            try {
                probes.add(future.get(3, TimeUnit.SECONDS));
            } catch (Exception ignored) {
                // 单个候选失败不影响其它
            }
        }
        probes.sort(
                Comparator.comparing((Probe p) -> !p.reachable())
                        .thenComparingLong(p -> p.reachable() ? p.latencyMs() : Long.MAX_VALUE));
        return probes;
    }

    /** 读出当前联网网卡的名字（中文系统也可能是「以太网」「WLAN」）。 */
    public static String activeAdapter() {
        String output = run("netsh", "interface", "ipv4", "show", "interfaces");
        if (output == null) {
            return null;
        }
        // 列：Idx  Met  MTU  State  Name；名字可能带空格，用正则整体取，避免按空格切错列
        java.util.regex.Pattern row =
                java.util.regex.Pattern.compile(
                        "^\\s*(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(connected|disconnected|已连接|已断开|已连接\\s*)\\s+(.+)$");
        String best = null;
        long bestMetric = Long.MAX_VALUE;
        for (String line : output.split("\n")) {
            java.util.regex.Matcher m = row.matcher(line.trim());
            if (!m.matches()) {
                continue;
            }
            String state = m.group(4).trim();
            boolean up = state.startsWith("connected") || state.startsWith("已连接");
            if (!up) {
                continue;
            }
            String name = m.group(5).trim();
            if (name.isEmpty()) {
                continue;
            }
            String lower = name.toLowerCase(Locale.ROOT);
            // 环回 / 虚拟网卡不作为目标，否则会把 DNS 写到错的地方
            if (lower.contains("loopback") || name.contains("环回")) {
                continue;
            }
            long metric;
            try {
                metric = Long.parseLong(m.group(2));
            } catch (NumberFormatException e) {
                metric = Long.MAX_VALUE;
            }
            if (metric < bestMetric) {
                bestMetric = metric;
                best = name;
            }
        }
        return best;
    }

    /** 读取当前 DNS（第一项），读不到返回空串。 */
    public static String currentDns() {
        String adapter = activeAdapter();
        String output =
                adapter == null
                        ? run("netsh", "interface", "ipv4", "show", "dnsservers")
                        : run("netsh", "interface", "ipv4", "show", "dnsservers", "name=" + quote(adapter));
        if (output == null) {
            return "";
        }
        // 指定网卡后，输出里第一个 IPv4 就是首选 DNS；
        // 注意「通过 DHCP 配置的 DNS 服务器:  192.168.1.1」这一行同时含标签和地址。
        java.util.regex.Pattern ip =
                java.util.regex.Pattern.compile(".*?((?:\\d{1,3}\\.){3}\\d{1,3}).*");
        for (String raw : output.split("\n")) {
            String line = raw.trim();
            if (line.isEmpty()) {
                continue;
            }
            java.util.regex.Matcher m = ip.matcher(line);
            if (m.matches()) {
                return m.group(1);
            }
        }
        return "";
    }

    /** netsh 的 name 参数带空格时必须加引号，否则会解析失败。 */
    private static String quote(String adapter) {
        return "name=\"" + adapter + "\"";
    }

    /** 探测 + 应用最快 DNS。需要管理员权限；失败时返回可读原因。 */
    public static Result autoApply() {
        String adapter = activeAdapter();
        if (adapter == null) {
            return new Result(false, "没有找到已联网的网卡，跳过 DNS 优化", null, "");
        }
        String before = currentDns();
        List<Probe> probes = probeAll();
        Probe chosen = probes.stream().filter(Probe::reachable).findFirst().orElse(null);
        if (chosen == null) {
            return new Result(false, "所有候选 DNS 都不可达，可能被防火墙拦了 53 端口", null, before);
        }
        run(
                "netsh",
                "interface",
                "ipv4",
                "set",
                "dnsservers",
                "name=" + quote(adapter),
                "static",
                chosen.server(),
                "primary");
        // 读回校验：真写进去了才算成功（netsh 的失败文案随语言变化，不能用来判断）
        flushDns();
        String after = currentDns();
        boolean ok = chosen.server().equals(after);
        if (!ok) {
            return new Result(
                    false,
                    "设置 DNS 需要管理员权限；已探测到最快的 " + chosen.name() + "（" + chosen.server()
                            + "，" + chosen.latencyMs() + " ms），可手动填写",
                    chosen,
                    before);
        }
        return new Result(
                true,
                "已把「" + adapter + "」的 DNS 设为 " + chosen.name() + "（" + chosen.server() + "，"
                        + chosen.latencyMs() + " ms）",
                chosen,
                before);
    }

    /** 恢复成自动获取（DHCP）。 */
    public static Result reset() {
        String adapter = activeAdapter();
        if (adapter == null) {
            return new Result(false, "没有找到已联网的网卡", null, "");
        }
        run(
                "netsh",
                "interface",
                "ipv4",
                "set",
                "dnsservers",
                "name=" + quote(adapter),
                "source=dhcp");
        flushDns();
        String check =
                run(
                        "netsh",
                        "interface",
                        "ipv4",
                        "show",
                        "dnsservers",
                        "name=" + quote(adapter));
        boolean ok =
                check != null
                        && (check.toLowerCase(Locale.ROOT).contains("dhcp")
                                || check.contains("通过 DHCP")
                                || check.contains("动态"));
        if (ok) {
            return new Result(true, "已恢复「" + adapter + "」为自动获取 DNS", null, "");
        }
        return new Result(false, "恢复失败，可能需要管理员权限", null, "");
    }

    /** 清空 DNS 缓存（不改配置，普通权限通常也能跑）。 */
    public static boolean flushDns() {
        return run("ipconfig", "/flushdns") != null;
    }

    private static String run(String... command) {
        try {
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.redirectErrorStream(true);
            Process process = builder.start();
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(process.getInputStream(), Charset.defaultCharset()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append('\n');
                }
            }
            process.waitFor(8, TimeUnit.SECONDS);
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    /** 给界面用的一行摘要。 */
    public static String summary() {
        List<Probe> probes = probeAll();
        StringBuilder sb = new StringBuilder();
        for (Probe probe : probes) {
            if (sb.length() > 0) {
                sb.append("   ·   ");
            }
            sb.append(probe.name())
                    .append(probe.reachable() ? " " + probe.latencyMs() + "ms" : " 不可达");
        }
        return sb.toString();
    }

    /** 当前是否已指向候选 DNS。 */
    public static boolean usingCandidate() {
        String dns = currentDns();
        if (dns.isEmpty()) {
            return false;
        }
        for (String[] candidate : CANDIDATES) {
            if (candidate[1].equals(dns)) {
                return true;
            }
        }
        return false;
    }

    /** 便于日志：网卡名 + 当前 DNS。 */
    public static String describe() {
        String adapter = activeAdapter();
        String dns = currentDns();
        return (adapter == null ? "未知网卡" : adapter) + " · DNS " + (dns.isEmpty() ? "自动" : dns);
    }

    static File markerFile(File gameDir) {
        return new File(gameDir, "cache/network-boost.txt");
    }
}
