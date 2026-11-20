package com.example.demo;

import com.example.demo.core.auth.AuthEndpoint;
import com.example.demo.core.auth.AuthEndpoints;
import com.example.demo.core.auth.AuthStage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 公开登录接口清单：候选生成、client id 与主机的追加、按 client id 过滤。 */
class AuthEndpointsTest {

    @Test
    void buildsOneCandidatePerClientIdAndHost() {
        List<String> ids = List.of("id-a", "id-b");
        List<String> hosts = List.of("login.microsoftonline.com", "login.microsoft.com");

        List<AuthEndpoint> device = AuthEndpoints.candidates(AuthStage.DEVICE_CODE, ids, hosts);

        assertEquals(4, device.size(), "2 个 client id × 2 个主机");
        assertTrue(device.stream().allMatch(e -> e.stage() == AuthStage.DEVICE_CODE));
        assertTrue(device.stream().allMatch(e -> e.url().endsWith("/consumers/oauth2/v2.0/devicecode")));
        assertTrue(device.stream().allMatch(AuthEndpoint::usesClientId));
        assertEquals("login.microsoftonline.com", device.get(0).host());
    }

    @Test
    void tokenStageMirrorsDeviceCodeStage() {
        List<AuthEndpoint> token = AuthEndpoints.candidates(AuthStage.TOKEN,
                List.of("id-a"), List.of("login.microsoftonline.com", "login.windows.net"));

        assertEquals(2, token.size());
        assertTrue(token.stream().allMatch(e -> e.url().endsWith("/consumers/oauth2/v2.0/token")));
    }

    @Test
    void xboxStagesHaveSingleOfficialEndpoint() {
        assertEquals(1, AuthEndpoints.candidates(AuthStage.XBOX_LIVE).size());
        assertEquals(1, AuthEndpoints.candidates(AuthStage.XSTS).size());
        assertEquals(1, AuthEndpoints.candidates(AuthStage.MINECRAFT_LOGIN).size());
        assertEquals(1, AuthEndpoints.candidates(AuthStage.PROFILE).size());

        AuthEndpoint xsts = AuthEndpoints.candidates(AuthStage.XSTS).get(0);
        assertFalse(xsts.usesClientId(), "Xbox 阶段不需要 client id");
        assertEquals("https://xsts.auth.xboxlive.com/xsts/authorize", xsts.url());
    }

    @Test
    void forClientIdKeepsPairsTogether() {
        List<AuthEndpoint> device = AuthEndpoints.candidates(AuthStage.DEVICE_CODE,
                List.of("id-a", "id-b"), List.of("login.microsoftonline.com"));

        List<AuthEndpoint> pinned = AuthEndpoints.forClientId(device, "id-b");

        assertEquals(1, pinned.size());
        assertEquals("id-b", pinned.get(0).clientId());
    }

    @Test
    void forClientIdFallsBackWhenUnknown() {
        List<AuthEndpoint> device = AuthEndpoints.candidates(AuthStage.DEVICE_CODE,
                List.of("id-a"), List.of("login.microsoftonline.com"));

        assertEquals(device, AuthEndpoints.forClientId(device, "not-there"));
        assertEquals(device, AuthEndpoints.forClientId(device, null));
    }

    @Test
    void systemPropertiesExtendTheList() {
        String previousIds = System.getProperty("mc.auth.extraClientIds");
        String previousHosts = System.getProperty("mc.auth.extraHosts");
        try {
            System.setProperty("mc.auth.extraClientIds", "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee, not-a-guid");
            System.setProperty("mc.auth.extraHosts", "login.example-mirror.net");

            assertTrue(AuthEndpoints.clientIds().contains("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"));
            assertFalse(AuthEndpoints.clientIds().contains("not-a-guid"), "非法 id 应被丢掉");
            assertTrue(AuthEndpoints.microsoftHosts().contains("login.example-mirror.net"));

            long withExtra = AuthEndpoints.candidates(AuthStage.DEVICE_CODE).size();
            assertTrue(withExtra >= AuthEndpoints.MICROSOFT_HOSTS.size() + 3);
        } finally {
            restore("mc.auth.extraClientIds", previousIds);
            restore("mc.auth.extraHosts", previousHosts);
        }
    }

    @Test
    void defaultClientIdIsAPublicGuidNotTheRetiredOne() {
        assertEquals(List.of(AuthEndpoints.DEFAULT_CLIENT_ID), AuthEndpoints.clientIds());
        assertFalse("00000000402b5328".equals(AuthEndpoints.DEFAULT_CLIENT_ID),
                "旧版微软账号 client id 已被 /consumers 拒绝（AADSTS700016）");
    }

    private static void restore(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }
}
