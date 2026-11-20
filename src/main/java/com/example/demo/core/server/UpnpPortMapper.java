package com.example.demo.core.server;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * UPnP IGD 端口映射：让外网能直接连进来，不用手动进路由器后台。
 *
 * <p>流程：SSDP 组播发现网关 → 拉设备描述 XML → 找 WANIPConnection / WANPPPConnection
 * 服务的 controlURL → 发 SOAP 的 AddPortMapping。全程只依赖 JDK，不引第三方库。
 *
 * <p>失败是常态（路由器关 UPnP、双 NAT、运营商级 NAT），所以每个环节都返回可读原因，
 * 由 UI 如实显示，不假装成功。
 */
public final class UpnpPortMapper {

    private static final String SSDP_ADDRESS = "239.255.255.250";
    private static final int SSDP_PORT = 1900;
    /** 本机是家用网关时不要走系统代理，否则请求会发到代理上。 */
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(4))
            .proxy(new ProxySelector() {
                @Override
                public List<Proxy> select(URI uri) {
                    return List.of(Proxy.NO_PROXY);
                }

                @Override
                public void connectFailed(URI uri, SocketAddress sa, java.io.IOException ioe) {
                }
            })
            .build();

    private UpnpPortMapper() {
    }

    /** 映射结果：成功时带上网关给出的公网 IP，失败时带上原因。 */
    public record Result(boolean ok, String externalIp, String message) {
        static Result fail(String message) {
            return new Result(false, "", message);
        }
    }

    private record Gateway(String controlUrl, String serviceType, String localIp) {
    }

    /** 添加 TCP 端口映射。 */
    public static Result map(int port, String description) {
        try {
            Gateway gateway = discover();
            if (gateway == null) {
                return Result.fail("没有发现支持 UPnP 的网关（路由器可能关闭了 UPnP）");
            }
            String body = soapEnvelope(gateway.serviceType(), "AddPortMapping",
                    "<NewRemoteHost></NewRemoteHost>"
                            + "<NewExternalPort>" + port + "</NewExternalPort>"
                            + "<NewProtocol>TCP</NewProtocol>"
                            + "<NewInternalPort>" + port + "</NewInternalPort>"
                            + "<NewInternalClient>" + gateway.localIp() + "</NewInternalClient>"
                            + "<NewEnabled>1</NewEnabled>"
                            + "<NewPortMappingDescription>" + escape(description) + "</NewPortMappingDescription>"
                            + "<NewLeaseDuration>0</NewLeaseDuration>");
            HttpResponse<String> response = call(gateway.controlUrl(), gateway.serviceType(),
                    "AddPortMapping", body);
            if (response.statusCode() / 100 != 2) {
                String reason = errorText(response.body());
                return Result.fail("网关拒绝映射（" + response.statusCode() + "）"
                        + (reason.isEmpty() ? "" : "：" + reason));
            }
            String external = externalIp(gateway);
            return new Result(true, external,
                    external.isEmpty() ? "映射已添加，但读不到公网 IP" : "映射成功，公网地址 " + external + ":" + port);
        } catch (Exception e) {
            return Result.fail("UPnP 映射失败：" + (e.getMessage() == null ? e.toString() : e.getMessage()));
        }
    }

    /** 删除映射（退出前清理，避免路由器里留一堆规则）。 */
    public static Result unmap(int port) {
        try {
            Gateway gateway = discover();
            if (gateway == null) {
                return Result.fail("没有发现支持 UPnP 的网关");
            }
            String body = soapEnvelope(gateway.serviceType(), "DeletePortMapping",
                    "<NewRemoteHost></NewRemoteHost>"
                            + "<NewExternalPort>" + port + "</NewExternalPort>"
                            + "<NewProtocol>TCP</NewProtocol>");
            HttpResponse<String> response = call(gateway.controlUrl(), gateway.serviceType(),
                    "DeletePortMapping", body);
            return response.statusCode() / 100 == 2
                    ? new Result(true, "", "已删除端口映射")
                    : Result.fail("删除失败（" + response.statusCode() + "）");
        } catch (Exception e) {
            return Result.fail("删除失败：" + (e.getMessage() == null ? e.toString() : e.getMessage()));
        }
    }

    // ==================== 发现 ====================

    private static Gateway discover() throws Exception {
        String location = ssdpSearch();
        if (location == null) {
            return null;
        }
        String xml = fetch(location);
        if (xml == null) {
            return null;
        }
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        document.getDocumentElement().normalize();
        URI base = URI.create(location);

        // 优先 WANIPConnection，其次 WANPPPConnection
        for (String wanted : new String[]{"WANIPConnection", "WANPPPConnection"}) {
            NodeList services = document.getElementsByTagName("service");
            for (int i = 0; i < services.getLength(); i++) {
                Element service = (Element) services.item(i);
                String type = text(service, "serviceType");
                if (type == null || !type.contains(wanted)) {
                    continue;
                }
                String control = text(service, "controlURL");
                if (control == null || control.isBlank()) {
                    continue;
                }
                String absolute = base.resolve(control.trim()).toString();
                return new Gateway(absolute, type.trim(), PortableServerManager.localIp());
            }
        }
        // 有些网关把 WAN 服务放在子设备里，用 getElementsByTagName 已经覆盖；
        // 走到这里说明真没有
        return null;
    }

    private static String ssdpSearch() {
        // 组播常常被路由器/防火墙拦掉；再对默认网关补一次单播 M-SEARCH，
        // 不少固件只在单播时回包
        for (String host : candidateHosts()) {
            String location = searchHost(host);
            if (location != null) {
                return location;
            }
        }
        return null;
    }

    private static java.util.List<String> candidateHosts() {
        java.util.List<String> hosts = new java.util.ArrayList<>();
        hosts.add(SSDP_ADDRESS);
        String gateway = defaultGateway();
        if (gateway != null && !gateway.isBlank() && !hosts.contains(gateway)) {
            hosts.add(gateway);
        }
        return hosts;
    }

    /** 从 ipconfig 里读默认网关（JDK 没有直接暴露这个信息）。 */
    private static String defaultGateway() {
        try {
            ProcessBuilder builder = new ProcessBuilder("ipconfig");
            builder.redirectErrorStream(true);
            Process process = builder.start();
            String output = new String(process.getInputStream().readAllBytes(),
                    java.nio.charset.Charset.defaultCharset());
            process.waitFor();
            java.util.regex.Matcher matcher = java.util.regex.Pattern
                    .compile("(?m)^\\s*(?:默认网关|Default Gateway)[^:]*:\\s*(\\d{1,3}(?:\\.\\d{1,3}){3})")
                    .matcher(output);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception ignored) {
            // 读不到就算了，只用组播
        }
        return "";
    }

    private static String searchHost(String host) {
        String message = "M-SEARCH * HTTP/1.1\r\n"
                + "HOST: " + SSDP_ADDRESS + ":" + SSDP_PORT + "\r\n"
                + "MAN: \"ssdp:discover\"\r\n"
                + "MX: 2\r\n"
                + "ST: urn:schemas-upnp-org:device:InternetGatewayDevice:1\r\n\r\n";
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(1800);
            byte[] payload = message.getBytes(StandardCharsets.UTF_8);
            socket.send(new DatagramPacket(payload, payload.length,
                    InetAddress.getByName(host), SSDP_PORT));
            byte[] buffer = new byte[4096];
            long deadline = System.currentTimeMillis() + 1900;
            while (System.currentTimeMillis() < deadline) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                try {
                    socket.receive(packet);
                } catch (java.net.SocketTimeoutException timeout) {
                    break;
                }
                String text = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                Matcher matcher = Pattern.compile("(?im)^location:\\s*(\\S+)").matcher(text);
                if (matcher.find()) {
                    return matcher.group(1).trim();
                }
            }
        } catch (Exception ignored) {
            // 这个地址没响应，换下一个
        }
        return null;
    }

    private static String fetch(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200 ? response.body() : null;
        } catch (Exception e) {
            return null;
        }
    }

    // ==================== SOAP ====================

    private static HttpResponse<String> call(String controlUrl, String serviceType, String action,
                                             String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(controlUrl))
                .timeout(Duration.ofSeconds(6))
                .header("Content-Type", "text/xml; charset=\"utf-8\"")
                .header("SOAPAction", "\"" + serviceType + "#" + action + "\"")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        return CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }

    /** 包内可见，便于单测：拼 SOAP 信封。 */
    public static String soapEnvelope(String serviceType, String action, String args) {
        return "<?xml version=\"1.0\"?>\r\n"
                + "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\""
                + " s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">"
                + "<s:Body><u:" + action + " xmlns:u=\"" + serviceType + "\">"
                + args
                + "</u:" + action + "></s:Body></s:Envelope>";
    }

    private static String externalIp(Gateway gateway) throws Exception {
        String body = soapEnvelope(gateway.serviceType(), "GetExternalIPAddress", "");
        HttpResponse<String> response = call(gateway.controlUrl(), gateway.serviceType(),
                "GetExternalIPAddress", body);
        if (response.statusCode() / 100 != 2) {
            return "";
        }
        Matcher matcher = Pattern.compile("<NewExternalIPAddress>([^<]*)</NewExternalIPAddress>")
                .matcher(response.body());
        return matcher.find() ? matcher.group(1).trim() : "";
    }

    /** 从 SSDP 响应里取 LOCATION（诊断与单测都直接调它）。 */
    public static String parseLocation(String ssdpResponse) {
        if (ssdpResponse == null) {
            return null;
        }
        Matcher matcher = Pattern.compile("(?im)^location:\\s*(\\S+)").matcher(ssdpResponse);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    private static String errorText(String body) {
        if (body == null) {
            return "";
        }
        Matcher matcher = Pattern.compile("<errorDescription>([^<]*)</errorDescription>").matcher(body);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        // 有些固件只给错误码
        Matcher code = Pattern.compile("<errorCode>(\\d+)</errorCode>").matcher(body);
        return code.find() ? "错误码 " + code.group(1) : "";
    }

    private static String text(Element parent, String tag) {
        NodeList nodes = parent.getElementsByTagName(tag);
        if (nodes.getLength() == 0) {
            return null;
        }
        String value = nodes.item(0).getTextContent();
        return value == null ? null : value.trim();
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** 本机是否在 NAT 后面（公网 IP 与局域网 IP 不同即视为在 NAT 后）。 */
    public static boolean behindNat(String publicIp, String localIp) {
        return publicIp != null && !publicIp.isBlank() && !publicIp.equals(localIp);
    }

    static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
