package com.example.demo;

import com.example.demo.core.server.ServerFiles;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 在线玩家靠 server-console.log 的 joined / left 事件推断。 */
class ServerOnlinePlayersTest {

    @Test
    void parsesJoinAndLeaveEvents(@TempDir Path dir) throws Exception {
        String log = String.join("\n",
                "[12:00:00] [Server thread/INFO]: Steve joined the game",
                "[12:00:10] [Server thread/INFO]: Alex joined the game",
                "[12:01:00] [Server thread/INFO]: Steve left the game",
                "[12:02:00] [Server thread/INFO]: Steve joined the game");
        Files.writeString(new File(dir.toFile(), "server-console.log").toPath(), log,
                StandardCharsets.UTF_8);

        List<String> online = ServerFiles.onlinePlayers(dir.toFile());
        assertEquals(2, online.size(), online.toString());
        assertTrue(online.contains("Steve"));
        assertTrue(online.contains("Alex"));
    }

    @Test
    void missingLogMeansNobodyOnline(@TempDir Path dir) {
        assertTrue(ServerFiles.onlinePlayers(dir.toFile()).isEmpty());
    }
}
