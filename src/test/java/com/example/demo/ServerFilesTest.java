package com.example.demo;

import com.example.demo.core.server.ServerFiles;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 玩家名单的增删：重复添加不重复写，移除按名字且大小写不敏感。 */
class ServerFilesTest {

    @Test
    void addOpIsIdempotentAndRemoveDropsOnlyThatPlayer(@TempDir Path temp) throws Exception {
        File dir = temp.toFile();
        ServerFiles.addOp(dir, "Steve", "");
        ServerFiles.addOp(dir, "Alex", "");
        ServerFiles.addOp(dir, "steve", "");
        assertEquals(2, ServerFiles.readOps(dir).size(), "同名（忽略大小写）不能重复写入");

        ServerFiles.removeOp(dir, "STEVE");
        var left = ServerFiles.readOps(dir);
        assertEquals(1, left.size());
        assertEquals("Alex", left.get(0).name());
    }

    @Test
    void removeFromMissingFileIsNoOp(@TempDir Path temp) throws Exception {
        ServerFiles.removeWhitelist(temp.toFile(), "Nobody");
        assertTrue(ServerFiles.readWhitelist(temp.toFile()).isEmpty());
    }

    @Test
    void whitelistRoundTrip(@TempDir Path temp) throws Exception {
        File dir = temp.toFile();
        ServerFiles.addWhitelist(dir, "Notch", "");
        assertEquals(1, ServerFiles.readWhitelist(dir).size());
        ServerFiles.removeWhitelist(dir, "notch");
        assertTrue(ServerFiles.readWhitelist(dir).isEmpty());
    }
}