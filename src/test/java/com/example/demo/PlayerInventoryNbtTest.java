package com.example.demo;

import com.example.demo.core.server.Nbt;
import com.example.demo.core.server.PlayerInventory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** NBT 往返 + 玩家背包读写。 */
class PlayerInventoryNbtTest {

    private static File playerDat(Path gameRoot, String uuid) throws Exception {
        File data = new File(gameRoot.toFile(), "world/playerdata");
        assertTrue(data.mkdirs());
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("Slot", (byte) 0);
        item.put("id", "minecraft:diamond_sword");
        item.put("Count", (byte) 1);
        Map<String, Object> apple = new LinkedHashMap<>();
        apple.put("Slot", (byte) 5);
        apple.put("id", "minecraft:apple");
        apple.put("Count", (byte) 12);
        List<Object> inventory = new ArrayList<>();
        inventory.add(item);
        inventory.add(apple);

        Map<String, Object> tree = new LinkedHashMap<>();
        tree.put("Health", (short) 20);
        tree.put("XpLevel", 7);
        tree.put("Inventory", inventory);
        File dat = new File(data, uuid + ".dat");
        Nbt.write(dat, tree);
        return dat;
    }

    @Test
    void nbtRoundTripsThroughGzip(@TempDir Path root) throws Exception {
        File dat = playerDat(root, "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        Map<String, Object> reread = Nbt.read(dat);
        assertEquals((short) 20, ((Number) reread.get("Health")).shortValue());
        assertEquals(7, ((Number) reread.get("XpLevel")).intValue());
        assertTrue(reread.get("Inventory") instanceof List<?>);
    }

    @Test
    void readsAndEditsInventoryAndKeepsOtherTags(@TempDir Path root) throws Exception {
        File dat = playerDat(root, "11111111-2222-3333-4444-555555555555");
        List<PlayerInventory.Slot> slots = PlayerInventory.read(dat);
        assertEquals(2, slots.size());
        assertEquals("minecraft:diamond_sword", slots.get(0).itemId());
        assertEquals(12, slots.get(1).count());

        // 苹果改成 3 个，剑删掉，再塞 5 个金苹果
        Map<Integer, Integer> edits = new LinkedHashMap<>();
        edits.put(5, 3);
        edits.put(0, 0);
        List<PlayerInventory.Slot> adds = new ArrayList<>();
        adds.add(new PlayerInventory.Slot(0, "minecraft:golden_apple", 5));

        List<PlayerInventory.Slot> after = PlayerInventory.apply(dat, edits, adds);
        assertEquals(2, after.size(), after.toString());
        assertTrue(after.stream().anyMatch(s -> s.itemId().equals("minecraft:apple") && s.count() == 3),
                after.toString());
        assertTrue(after.stream().anyMatch(s -> s.itemId().equals("minecraft:golden_apple")
                && s.count() == 5), after.toString());
        assertTrue(after.stream().noneMatch(s -> s.itemId().equals("minecraft:diamond_sword")));

        // 其它标签没被写坏
        Map<String, Object> reread = Nbt.read(dat);
        assertEquals((short) 20, ((Number) reread.get("Health")).shortValue());
        assertEquals(7, ((Number) reread.get("XpLevel")).intValue());

        // 写回前留了备份
        File backups = new File(root.toFile(), "server-backups");
        assertNotNull(backups.listFiles(), "应当留下备份目录");
        assertTrue(backups.listFiles().length == 1);
    }

    @Test
    void findsDatFileFromUuidWithOrWithoutDashes(@TempDir Path root) throws Exception {
        File dat = playerDat(root, "abcdefab-1111-2222-3333-444444444444");
        assertEquals(dat, PlayerInventory.datFile(root.toFile(), "abcdefab-1111-2222-3333-444444444444"));
        assertEquals(dat, PlayerInventory.datFile(root.toFile(), "ABCDEFAB11112222333344444444 4444".replace(" ", "")));
    }
}
