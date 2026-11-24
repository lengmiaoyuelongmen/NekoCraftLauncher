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
    @Test
    void parsesListCommandResponse(@TempDir Path dir) throws Exception {
        String log = String.join("\n",
                "[12:00:00] [Server thread/INFO]: There are 2 of a max of 20 players online: Steve, Alex",
                "[12:00:05] [Server thread/INFO]: There are 0 of a max of 20 players online: ");
        java.util.List<String> all = ServerFiles.parseListResponse(log);
        // 两行都命中，合并后应为 Steve/Alex（第二行没有名字）
        assertTrue(all.contains("Steve"));
        assertTrue(all.contains("Alex"));
        assertEquals(2, all.size(), all.toString());

        java.util.List<String> none = ServerFiles.parseListResponse(
                "[x] There are 0 of a max of 20 players online: ");
        assertTrue(none.isEmpty());
    }
}
