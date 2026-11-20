package com.example.demo.core.auth;

/**
 * 微软登录的六个阶段。
 *
 * <p>每个阶段可用的公开接口不止一个（同一服务通常有多个主机别名），
 * 阶段是「选线路」的最小单位：设备码和令牌轮询必须配同一套主机 + 同一 client id，
 * Xbox Live / XSTS / Minecraft 各自独立。
 */
public enum AuthStage {

    /** 申请设备码（用户在 microsoft.com/link 输入 user_code）。 */
    DEVICE_CODE("设备码"),

    /** 轮询令牌（authorization_pending 属正常，不是故障）。 */
    TOKEN("令牌轮询"),

    /** Xbox Live 用户认证（拿 XBL token + UHS）。 */
    XBOX_LIVE("Xbox Live"),

    /** XSTS 授权（拿 XSTS token）。 */
    XSTS("XSTS"),

    /** 用 XSTS 换 Minecraft access token。 */
    MINECRAFT_LOGIN("Minecraft 登录"),

    /** 读取玩家档案（名字 + UUID）。 */
    PROFILE("玩家档案");

    private final String label;

    AuthStage(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
