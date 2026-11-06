package com.example.demo;

import com.example.demo.core.launch.ServerAddress;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** 「加入服务器」地址解析：域名、端口、IPv6。 */
class ServerAddressTest {

    @Test
    void plainHostUsesDefaultPort() {
        ServerAddress address = ServerAddress.parse("mc.example.com");
        assertEquals("mc.example.com", address.host());
        assertEquals(25565, address.port());
    }

    @Test
    void hostWithPort() {
        ServerAddress address = ServerAddress.parse("mc.example.com:25566");
        assertEquals("mc.example.com", address.host());
        assertEquals(25566, address.port());
    }

    @Test
    void ipv6WithBrackets() {
        ServerAddress withPort = ServerAddress.parse("[::1]:25567");
        assertEquals("::1", withPort.host());
        assertEquals(25567, withPort.port());

        ServerAddress withoutPort = ServerAddress.parse("[2001:db8::1]");
        assertEquals("2001:db8::1", withoutPort.host());
        assertEquals(25565, withoutPort.port());
    }

    @Test
    void rejectsBlankOrInvalid() {
        assertNull(ServerAddress.parse(null));
        assertNull(ServerAddress.parse("  "));
        assertNull(ServerAddress.parse("host:abc"));
        assertNull(ServerAddress.parse("host:70000"));
        assertNull(ServerAddress.parse("[unclosed"));
    }

    @Test
    void buildsLaunchArguments() {
        assertArrayEquals(new String[]{"--server", "mc.example.com", "--port", "25565"},
                ServerAddress.parse("mc.example.com").launchArguments());
    }
}
