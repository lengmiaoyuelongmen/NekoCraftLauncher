package com.example.demo;

import com.example.demo.core.auth.LoginCardParser;
import com.example.demo.core.model.LoginCardData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** 登录卡片解析：JSON / 文本 / 服务器地址归一化。 */
class LoginCardParserTest {

    @Test
    void parsesJsonCard() {
        LoginCardData card = LoginCardParser.parse("""
                {"server": "https://littleskin.cn/api/yggdrasil", "username": "Steve", "password": "pw"}
                """);

        assertEquals("https://littleskin.cn/api/yggdrasil", card.server());
        assertEquals("Steve", card.username());
        assertEquals("pw", card.password());
    }

    @Test
    void parsesNestedAuthObject() {
        LoginCardData card = LoginCardParser.parse("""
                {"auth": {"apiRoot": "https://example.com/api/yggdrasil", "email": "a@b.c", "passwd": "secret"}}
                """);

        assertEquals("https://example.com/api/yggdrasil", card.server());
        assertEquals("a@b.c", card.username());
        assertEquals("secret", card.password());
    }

    @Test
    void parsesPlainTextCard() {
        LoginCardData card = LoginCardParser.parse("""
                服务器: https://littleskin.cn/api/yggdrasil
                邮箱: neko@example.com
                密码: meow123
                """);

        assertEquals("https://littleskin.cn/api/yggdrasil", card.server());
        assertEquals("neko@example.com", card.username());
        assertEquals("meow123", card.password());
    }

    @Test
    void rejectsTextWithoutCredentials() {
        assertNull(LoginCardParser.parse("这是一段随便的文字"));
        assertNull(LoginCardParser.parse(""));
        assertNull(LoginCardParser.parse(null));
    }

    @Test
    void normalizesServerAddress() {
        assertEquals("https://littleskin.cn/api/yggdrasil",
                LoginCardParser.normalizeServer("littleskin.cn"));
        assertEquals("https://littleskin.cn/api/yggdrasil",
                LoginCardParser.normalizeServer("https://littleskin.cn/"));
        assertEquals("https://example.com",
                LoginCardParser.normalizeServer("https://example.com/"), "去掉尾部斜杠，不补路径");
        assertEquals("", LoginCardParser.normalizeServer("  "));
    }
}
