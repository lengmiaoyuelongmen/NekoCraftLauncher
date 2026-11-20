package com.example.demo.core.auth;

/**
 * 一条登录线路：某个阶段的一个公开接口地址。
 *
 * @param stage    所属阶段
 * @param label    给人看的名字（日志里显示「微软 / login.microsoft.com」这种）
 * @param url      完整地址
 * @param clientId 该线路绑定的 Azure 公共 client id；不需要的线路为 {@code null}
 * @param priority 同阶段内的小优先级，数字小的优先（仅在没有实测延迟时作为次序）
 */
public record AuthEndpoint(AuthStage stage, String label, String url, String clientId, int priority) {

    /** 健康表的主键：同阶段同地址同 client id 视为同一条线路。 */
    public String key() {
        return stage.name() + '|' + url + '|' + (clientId == null ? "" : clientId);
    }

    /** 是否需要 client id（微软 OAuth 的线路需要，Xbox/Minecraft 的不需要）。 */
    public boolean usesClientId() {
        return clientId != null && !clientId.isBlank();
    }

    /** 主机名，日志里比完整 URL 好读。 */
    public String host() {
        int start = url.indexOf("://");
        int from = start >= 0 ? start + 3 : 0;
        int slash = url.indexOf('/', from);
        return slash > 0 ? url.substring(from, slash) : url.substring(from);
    }

    @Override
    public String toString() {
        return label + " (" + host() + ")";
    }
}
