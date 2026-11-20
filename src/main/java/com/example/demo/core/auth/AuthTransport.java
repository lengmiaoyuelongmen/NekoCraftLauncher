package com.example.demo.core.auth;

import java.util.Map;

/**
 * 登录链路的 HTTP 传输层抽象。
 *
 * <p>单独抽出来只有一个目的：让「先检测、失败切换」这套逻辑能用一个假的传输层做单测，
 * 不必真的去登录一次微软账号，也不必依赖网络状态。
 */
public interface AuthTransport {

    /** 一次请求。 */
    record Request(String method, String url, Map<String, String> headers, String body,
                   int timeoutSeconds) {

        public static Request get(String url, int timeoutSeconds) {
            return new Request("GET", url, Map.of("Accept", "*/*"), null, timeoutSeconds);
        }

        public static Request postForm(String url, String body, int timeoutSeconds) {
            return new Request("POST", url,
                    Map.of("Content-Type", "application/x-www-form-urlencoded", "Accept", "application/json"),
                    body, timeoutSeconds);
        }

        public static Request postJson(String url, String body, int timeoutSeconds) {
            return new Request("POST", url,
                    Map.of("Content-Type", "application/json", "Accept", "application/json"),
                    body, timeoutSeconds);
        }

        /** 同一个请求换成另一个地址（切换线路时用）。 */
        public Request withUrl(String otherUrl) {
            return new Request(method, otherUrl, headers, body, timeoutSeconds);
        }
    }

    /** 一次响应。 */
    record Response(int status, String body, long latencyMs) {

        public boolean ok() {
            return status >= 200 && status < 300;
        }

        /** 服务端问题（可以换线路）。 */
        public boolean serverSide() {
            return status >= 500 || status == 429;
        }

        public String shortBody() {
            if (body == null) {
                return "";
            }
            return body.length() <= 160 ? body : body.substring(0, 160) + "…";
        }
    }

    /**
     * 发一次请求。
     *
     * @throws Exception 连接失败、超时、DNS 解析失败等（都算这条线路不可用）
     */
    Response send(Request request) throws Exception;
}
