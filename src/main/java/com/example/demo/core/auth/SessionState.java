package com.example.demo.core.auth;

import com.example.demo.core.model.UserSession;

import java.util.UUID;

/**
 * 会话状态的「映射层」：把存储里的东西（注册表里的 {@link SessionStore.Saved}、
 * 账户列表里的 {@link AccountStore.Account}）还原成界面能直接用的 {@link UserSession} + 展示字段。
 *
 * <p>以前这几段逻辑分散在主类的 restore / switch / login 三个方法里，各写一遍 UUID 解析和
 * 「登录类型 → 枚举」的映射。集中到这里之后可以单测，界面层只负责把结果贴到控件上。
 */
public final class SessionState {

    /** 还原结果：会话本身 + 登录类型 + 验证服务器 + 展示用的类型文案。 */
    public record Restored(UserSession session, String loginType, String server, String displayType) {
    }

    private SessionState() {
    }

    /** 登录类型文案 → 会话枚举（顺序敏感：「第三方外置登录」不含「微软」）。 */
    public static UserSession.LoginType typeOf(String loginType) {
        if (loginType == null) {
            return UserSession.LoginType.OFFLINE;
        }
        if (loginType.contains("微软")) {
            return UserSession.LoginType.MICROSOFT;
        }
        if (loginType.contains("第三方")) {
            return UserSession.LoginType.THIRD_PARTY;
        }
        return UserSession.LoginType.OFFLINE;
    }

    /** 展示用类型：优先 userType，缺省回落到登录方式。 */
    public static String displayType(String userType, String loginType) {
        return userType == null || userType.isBlank() ? (loginType == null ? "" : loginType) : userType;
    }

    /** UUID 的短展示形式（前 13 位 + 省略号）；没有 UUID 时返回空串。 */
    public static String shortUuid(UserSession session) {
        if (session == null || session.getUuid() == null) {
            return "";
        }
        String full = session.getUuid().toString();
        return full.length() <= 13 ? full : full.substring(0, 13) + "...";
    }

    /** 从注册表里保存的会话还原；用户名空时返回 null。 */
    public static Restored fromSaved(SessionStore.Saved saved) {
        if (saved == null || saved.username() == null || saved.username().isBlank()) {
            return null;
        }
        UserSession session = new UserSession();
        session.setUsername(saved.username());
        session.setAccessToken(saved.accessToken());
        session.setSkinUrl(saved.skinUrl());
        session.setUserType(saved.userType());
        session.setAuthServerUrl(saved.server());
        session.setType(typeOf(saved.loginType()));
        UUID uuid = parseUuid(saved.uuid());
        if (uuid != null) {
            session.setUuid(uuid);
        }
        return new Restored(session, saved.loginType(), saved.server(),
                displayType(saved.userType(), saved.loginType()));
    }

    /** 从保存的账户还原。 */
    public static Restored fromAccount(AccountStore.Account account) {
        if (account == null) {
            return null;
        }
        UserSession session = AccountStore.toSession(account);
        return new Restored(session, account.loginType(), account.server(),
                displayType(account.userType(), account.loginType()));
    }

    /** 登录成功时用：把本次登录的输入与会话绑在一起。 */
    public static Restored of(UserSession session, String loginType, String server) {
        return new Restored(session, loginType, server == null ? "" : server,
                displayType(session == null ? "" : session.getUserType(), loginType));
    }

    private static UUID parseUuid(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw.trim());
        } catch (Exception e) {
            return null;
        }
    }
}
