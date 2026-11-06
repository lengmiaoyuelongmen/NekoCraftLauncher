package com.example.demo;

import com.example.demo.core.auth.AccountStore;
import com.example.demo.core.auth.SessionState;
import com.example.demo.core.auth.SessionStore;
import com.example.demo.core.model.UserSession;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/** 会话映射：登录类型、UUID、展示文案。 */
class SessionStateTest {

    @Test
    void mapsLoginTypeText() {
        assertEquals(UserSession.LoginType.MICROSOFT, SessionState.typeOf("正版登录 (微软)"));
        assertEquals(UserSession.LoginType.THIRD_PARTY, SessionState.typeOf("第三方外置登录"));
        assertEquals(UserSession.LoginType.OFFLINE, SessionState.typeOf("离线登录"));
        assertEquals(UserSession.LoginType.OFFLINE, SessionState.typeOf(null));
        assertEquals(UserSession.LoginType.OFFLINE, SessionState.typeOf(""));
    }

    @Test
    void displayTypeFallsBackToLoginType() {
        assertEquals("离线登录", SessionState.displayType("", "离线登录"));
        assertEquals("离线登录", SessionState.displayType(null, "离线登录"));
        assertEquals("mojang", SessionState.displayType("mojang", "第三方外置登录"));
    }

    @Test
    void restoresFromSavedRegistryValue() {
        SessionStore.Saved saved = new SessionStore.Saved(
                "第三方外置登录", "Aurora_From_the", "882600c2-98af-4f7c-9c6a-112233445566",
                "token-1", "https://littleskin.cn/api/yggdrasil", "", "");
        SessionState.Restored restored = SessionState.fromSaved(saved);

        assertNotNull(restored);
        assertEquals("Aurora_From_the", restored.session().getUsername());
        assertEquals(UserSession.LoginType.THIRD_PARTY, restored.session().getType());
        assertEquals(UUID.fromString("882600c2-98af-4f7c-9c6a-112233445566"),
                restored.session().getUuid());
        assertEquals("https://littleskin.cn/api/yggdrasil", restored.server());
        assertEquals("第三方外置登录", restored.displayType());
    }

    @Test
    void savedWithBrokenUuidStillRestores() {
        SessionStore.Saved saved = new SessionStore.Saved(
                "离线登录", "Neko", "not-a-uuid", "", "", "", "mojang");
        SessionState.Restored restored = SessionState.fromSaved(saved);

        assertNotNull(restored);
        assertNull(restored.session().getUuid());
        assertEquals("mojang", restored.displayType());
    }

    @Test
    void blankSavedReturnsNull() {
        assertNull(SessionState.fromSaved(null));
        assertNull(SessionState.fromSaved(new SessionStore.Saved("离线登录", "  ", "", "", "", "", "")));
    }

    @Test
    void restoresFromStoredAccount() {
        AccountStore.Account account = new AccountStore.Account(
                "Aurora_From_the", "882600c2-98af-4f7c-9c6a-112233445566", "token",
                "离线登录", "mojang", "https://littleskin.cn/api/yggdrasil", "", "", "");
        SessionState.Restored restored = SessionState.fromAccount(account);

        assertNotNull(restored);
        assertEquals(UserSession.LoginType.OFFLINE, restored.session().getType());
        assertEquals("mojang", restored.displayType());
        assertEquals("https://littleskin.cn/api/yggdrasil", restored.server());
    }

    @Test
    void shortUuidTruncates() {
        UserSession session = new UserSession();
        session.setUuid(UUID.fromString("882600c2-98af-4f7c-9c6a-112233445566"));
        assertEquals("882600c2-98af...", SessionState.shortUuid(session));
        assertEquals("", SessionState.shortUuid(new UserSession()));
        assertEquals("", SessionState.shortUuid(null));
    }
}
