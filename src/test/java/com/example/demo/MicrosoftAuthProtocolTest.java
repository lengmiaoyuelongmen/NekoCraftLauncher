package com.example.demo;

import com.example.demo.core.auth.MicrosoftAuthProtocol;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 微软登录协议：表单体、设备码、轮询、XBL/XSTS/Minecraft、档案。 */
class MicrosoftAuthProtocolTest {

    @Test
    void buildsFormBodies() {
        String device = MicrosoftAuthProtocol.deviceCodeBody();
        assertTrue(device.contains("client_id=" + MicrosoftAuthProtocol.CLIENT_ID), device);
        assertTrue(device.contains("scope=XboxLive.signin%20offline_access"), device);

        String token = MicrosoftAuthProtocol.tokenBody("dev-123");
        assertTrue(token.contains("device_code=dev-123"), token);
        assertTrue(token.contains("grant_type=urn:ietf:params:oauth:grant-type:device_code"), token);
    }

    @Test
    void parsesDeviceCode() {
        String json = """
                {"device_code":"dev-1","user_code":"ABCD-EFGH",
                 "verification_uri":"https://microsoft.com/link","interval":5,"expires_in":900}
                """;
        MicrosoftAuthProtocol.DeviceCode code = MicrosoftAuthProtocol.parseDeviceCode(json);

        assertEquals("dev-1", code.deviceCode());
        assertEquals("ABCD-EFGH", code.userCode());
        assertEquals("https://microsoft.com/link", code.verificationUri());
        assertEquals(5, code.interval());
    }

    @Test
    void rejectsDeviceCodeWithoutCode() {
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> MicrosoftAuthProtocol.parseDeviceCode("{\"error\":\"invalid_client\"}"));
        assertTrue(error.getMessage().contains("设备码"), error.getMessage());
    }

    @Test
    void distinguishesPendingFromError() {
        assertEquals(MicrosoftAuthProtocol.PollResult.PENDING,
                MicrosoftAuthProtocol.pollResult("{\"error\":\"authorization_pending\"}"));
        assertEquals(MicrosoftAuthProtocol.PollResult.TOKEN,
                MicrosoftAuthProtocol.pollResult("{\"access_token\":\"tok\"}"));
        assertEquals(MicrosoftAuthProtocol.PollResult.ERROR,
                MicrosoftAuthProtocol.pollResult("{\"error\":\"expired_token\",\"error_description\":\"code expired\"}"));
        assertEquals(MicrosoftAuthProtocol.PollResult.ERROR,
                MicrosoftAuthProtocol.pollResult("not json"));

        assertEquals("code expired",
                MicrosoftAuthProtocol.pollError("{\"error\":\"expired_token\",\"error_description\":\"code expired\"}"));
    }

    @Test
    void parsesXboxLiveTokenAndUhs() {
        String json = """
                {"Token":"xbl-token","DisplayClaims":{"xui":[{"uhs":"1234567890"}]}}
                """;
        MicrosoftAuthProtocol.XboxLive live = MicrosoftAuthProtocol.parseXboxLive(json);

        assertEquals("xbl-token", live.token());
        assertEquals("1234567890", live.uhs());
        assertTrue(MicrosoftAuthProtocol.xboxLiveBody("ms-token").contains("d=ms-token"));
    }

    @Test
    void rejectsXboxLiveWithoutUhs() {
        assertThrows(IllegalStateException.class,
                () -> MicrosoftAuthProtocol.parseXboxLive("{\"Token\":\"t\"}"));
    }

    @Test
    void explainsXstsErrors() {
        assertTrue(MicrosoftAuthProtocol.xstsReason(2148916233L).contains("没有注册 Xbox"));
        assertTrue(MicrosoftAuthProtocol.xstsReason(2148916238L).contains("未成年"));
        assertTrue(MicrosoftAuthProtocol.xstsReason(2148916235L).contains("地区"));
        assertTrue(MicrosoftAuthProtocol.xstsReason(42L).contains("未知"));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> MicrosoftAuthProtocol.parseXstsToken("{\"XErr\":2148916233}"));
        assertTrue(error.getMessage().contains("没有注册 Xbox"), error.getMessage());
    }

    @Test
    void parsesXstsAndMinecraftTokens() {
        assertEquals("xsts-token",
                MicrosoftAuthProtocol.parseXstsToken("{\"Token\":\"xsts-token\"}"));
        assertTrue(MicrosoftAuthProtocol.xstsBody("xbl-token").contains("xbl-token"));
        assertTrue(MicrosoftAuthProtocol.minecraftBody("uhs-1", "xsts-token")
                .contains("XBL3.0 x=uhs-1;xsts-token"));
        assertEquals("mc-token",
                MicrosoftAuthProtocol.parseMinecraftToken("{\"access_token\":\"mc-token\"}"));
        assertThrows(IllegalStateException.class,
                () -> MicrosoftAuthProtocol.parseMinecraftToken("{}"));
    }

    @Test
    void parsesProfileAndFormatsUuid() {
        String json = """
                {"name":"Steve","id":"069a79f444e94726a5befca90e38aaf5"}
                """;
        MicrosoftAuthProtocol.Profile profile = MicrosoftAuthProtocol.parseProfile(json);

        assertEquals("Steve", profile.name());
        assertEquals(UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5"), profile.uuid());

        assertThrows(IllegalStateException.class,
                () -> MicrosoftAuthProtocol.parseProfile("{}"), "没买游戏时应给出明确提示");
    }
}
