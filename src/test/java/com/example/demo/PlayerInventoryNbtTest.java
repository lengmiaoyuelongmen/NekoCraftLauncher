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
    @Test
    void enchantmentsCanBeWrittenAndCleared(@TempDir Path root) throws Exception {
        File dat = playerDat(root, "bbbbbbbb-1111-2222-3333-444444444444");
        // 给 0 号槽位的剑加锋利 V + 击退 II
        PlayerInventory.applyEnchants(dat, 0, List.of(
                new PlayerInventory.Enchant("minecraft:sharpness", 5),
                new PlayerInventory.Enchant("minecraft:knockback", 2)));

        List<PlayerInventory.Enchant> read = PlayerInventory.readEnchants(dat, 0);
        assertEquals(2, read.size(), read.toString());
        assertTrue(read.stream().anyMatch(e -> e.id().equals("minecraft:sharpness") && e.level() == 5));

        // 清空
        PlayerInventory.applyEnchants(dat, 0, List.of());
        assertTrue(PlayerInventory.readEnchants(dat, 0).isEmpty());
        // 其它槽位不受影响
        assertTrue(PlayerInventory.readEnchants(dat, 5).isEmpty());
    }
    @Test
    void customNameAndDurabilityRoundTrip(@TempDir Path root) throws Exception {
        File dat = playerDat(root, "cccccccc-1111-2222-3333-444444444444");
        PlayerInventory.applyMeta(dat, 0, "喵剑", 7, 1561);

        PlayerInventory.ItemMeta meta = PlayerInventory.readMeta(dat, 0);
        assertEquals(7, meta.damage());
        assertEquals(1561, meta.maxDamage());
        assertTrue(meta.customName().contains("喵剑"), meta.customName());

        // 清掉自定义名与耐久
        PlayerInventory.applyMeta(dat, 0, "", 0, 0);
        PlayerInventory.ItemMeta cleared = PlayerInventory.readMeta(dat, 0);
        assertEquals(0, cleared.damage());
        assertTrue(cleared.customName().isEmpty());
        // 物品本身还在
        assertEquals(2, PlayerInventory.read(dat).size());
    }
    @Test
    void loreLinesRoundTrip(@TempDir Path root) throws Exception {
        File dat = playerDat(root, "dddddddd-1111-2222-3333-444444444444");
        PlayerInventory.applyLore(dat, 5, List.of("来自服务器的礼物", "第 2 行"));

        List<String> lore = PlayerInventory.readLore(dat, 5);
        assertEquals(2, lore.size(), lore.toString());
        assertTrue(lore.get(0).contains("来自服务器的礼物"), lore.get(0));
        assertTrue(lore.get(0).contains("color"), "应当带上灰色样式: " + lore.get(0));

        PlayerInventory.applyLore(dat, 5, List.of());
        assertTrue(PlayerInventory.readLore(dat, 5).isEmpty());
        // 苹果本身还在
        assertEquals(2, PlayerInventory.read(dat).size());
    }
}
