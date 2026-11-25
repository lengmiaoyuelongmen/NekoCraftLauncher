package com.example.demo.core.server;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 玩家背包（{@code playerdata/<uuid>.dat}）的读取与编辑。
 *
 * <p>只处理 {@code Inventory} 列表里的槽位、物品 id 与数量；其余标签（生命、经验、末影箱…）
 * 原样保留，所以改完不会把存档写坏。写回前会在 {@code server-backups/} 里留一份带时间戳的副本。
 */
public final class PlayerInventory {

    /** 一个槽位。 */
    public record Slot(int slot, String itemId, int count) {
    }

    private PlayerInventory() {
    }

    /** playerdata 目录。 */
    public static File playerDataDir(File serverDir) {
        return new File(new File(serverDir, "world"), "playerdata");
    }

    /** 按 uuid 找存档文件（大小写不敏感、去掉连字符也认）。 */
    public static File datFile(File serverDir, String uuid) {
        if (uuid == null || uuid.isBlank()) {
            return null;
        }
        String wanted = uuid.replace("-", "").toLowerCase(java.util.Locale.ROOT);
        File dir = playerDataDir(serverDir);
        File[] files = dir.listFiles((d, name) -> name.toLowerCase(java.util.Locale.ROOT).endsWith(".dat"));
        if (files == null) {
            return null;
        }
        for (File file : files) {
            String base = file.getName().substring(0, file.getName().length() - 4)
                    .replace("-", "").toLowerCase(java.util.Locale.ROOT);
            if (base.equals(wanted)) {
                return file;
            }
        }
        return null;
    }

    /** 读取背包（离线存档找不到就返回空列表）。 */
    @SuppressWarnings("unchecked")
    public static List<Slot> read(File dat) throws IOException {
        List<Slot> slots = new ArrayList<>();
        if (dat == null || !dat.isFile()) {
            return slots;
        }
        Map<String, Object> root = Nbt.read(dat);
        Object inventory = root.get("Inventory");
        if (!(inventory instanceof List<?> list)) {
            return slots;
        }
        for (Object element : list) {
            if (!(element instanceof Map<?, ?> entry)) {
                continue;
            }
            Map<String, Object> item = (Map<String, Object>) entry;
            int slot = item.get("Slot") instanceof Number number ? number.intValue() : -1;
            String id = item.get("id") instanceof String text ? text : "";
            int count = item.get("Count") instanceof Number number ? number.intValue() : 0;
            if (slot >= 0 && !id.isBlank()) {
                slots.add(new Slot(slot, id, count));
            }
        }
        slots.sort((a, b) -> Integer.compare(a.slot(), b.slot()));
        return slots;
    }

    /**
     * 应用修改并写回。
     *
     * @param edits 槽位 → 新数量；数量 ≤ 0 表示删除该槽位
     * @param adds  要新增的物品（放进最小的空槽位）
     * @return 实际写入后的背包
     */
    @SuppressWarnings("unchecked")
    public static List<Slot> apply(File dat, Map<Integer, Integer> edits, List<Slot> adds)
            throws IOException {
        if (dat == null || !dat.isFile()) {
            throw new IOException("找不到玩家存档文件");
        }
        backup(dat);
        Map<String, Object> root = Nbt.read(dat);
        List<Object> inventory = root.get("Inventory") instanceof List<?> list
                ? (List<Object>) list
                : new ArrayList<>();
        for (Map.Entry<Integer, Integer> edit : edits.entrySet()) {
            int slot = edit.getKey();
            int want = edit.getValue();
            for (int i = 0; i < inventory.size(); i++) {
                if (!(inventory.get(i) instanceof Map<?, ?> raw)) {
                    continue;
                }
                Map<String, Object> item = (Map<String, Object>) raw;
                int current = item.get("Slot") instanceof Number number ? number.intValue() : -1;
                if (current != slot) {
                    continue;
                }
                if (want <= 0) {
                    inventory.remove(i);
                } else {
                    item.put("Count", (byte) Math.min(want, 64));
                }
                break;
            }
        }
        for (Slot add : adds) {
            int slot = freeSlot(inventory);
            if (slot < 0) {
                break;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("Slot", (byte) slot);
            item.put("id", add.itemId());
            item.put("Count", (byte) Math.max(1, Math.min(add.count(), 64)));
            inventory.add(item);
        }
        root.put("Inventory", inventory);
        Nbt.write(dat, root);
        return read(dat);
    }

    /** 一条附魔：id 形如 {@code minecraft:sharpness}，等级 1~255。 */
    public record Enchant(String id, int level) {
    }

    /**
     * 批量给「同一物品 id 的所有槽位」覆盖同一组附魔（一次读、一次写、一次备份）。
     *
     * @return 实际改动的槽位数量
     */
    @SuppressWarnings("unchecked")
    public static int applyEnchantsToAll(File dat, String itemId, List<Enchant> enchants)
            throws IOException {
        if (itemId == null || itemId.isBlank()) {
            return 0;
        }
        Map<String, Object> root = Nbt.read(dat);
        if (!(root.get("Inventory") instanceof List<?> rawList)) {
            return 0;
        }
        List<Object> inventory = (List<Object>) rawList;
        int changed = 0;
        for (Object element : inventory) {
            if (!(element instanceof Map<?, ?> raw)) {
                continue;
            }
            Map<String, Object> item = (Map<String, Object>) raw;
            if (!(item.get("id") instanceof String id) || !id.equalsIgnoreCase(itemId)) {
                continue;
            }
            Map<String, Object> tag = item.get("tag") instanceof Map<?, ?> rawTag
                    ? (Map<String, Object>) rawTag
                    : new LinkedHashMap<>();
            if (enchants == null || enchants.isEmpty()) {
                tag.remove("Enchantments");
            } else {
                List<Object> list = new ArrayList<>();
                for (Enchant enchant : enchants) {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("id", enchant.id());
                    entry.put("lvl", (short) Math.max(1, Math.min(255, enchant.level())));
                    list.add(entry);
                }
                tag.put("Enchantments", list);
            }
            if (tag.isEmpty()) {
                item.remove("tag");
            } else {
                item.put("tag", tag);
            }
            changed++;
        }
        if (changed > 0) {
            backup(dat);
            Nbt.write(dat, root);
        }
        return changed;
    }
    /** 读取某个槽位的附魔（没有附魔返回空列表）。 */
    @SuppressWarnings("unchecked")
    public static List<Enchant> readEnchants(File dat, int slot) throws IOException {
        List<Enchant> result = new ArrayList<>();
        Map<String, Object> item = findItem(Nbt.read(dat), slot);
        if (item == null) {
            return result;
        }
        Object tag = item.get("tag");
        if (!(tag instanceof Map<?, ?> tagMap)) {
            return result;
        }
        Object enchants = ((Map<String, Object>) tagMap).get("Enchantments");
        if (!(enchants instanceof List<?> list)) {
            return result;
        }
        for (Object element : list) {
            if (!(element instanceof Map<?, ?> raw)) {
                continue;
            }
            Map<String, Object> enchant = (Map<String, Object>) raw;
            String id = enchant.get("id") instanceof String text ? text : "";
            int level = enchant.get("lvl") instanceof Number number ? number.intValue() : 0;
            if (!id.isBlank()) {
                result.add(new Enchant(id, level));
            }
        }
        return result;
    }

    /** 覆盖某个槽位的附魔列表；传空列表等于清空附魔。 */
    @SuppressWarnings("unchecked")
    public static void applyEnchants(File dat, int slot, List<Enchant> enchants) throws IOException {
        Map<String, Object> root = Nbt.read(dat);
        Map<String, Object> item = findItem(root, slot);
        if (item == null) {
            throw new IOException("槽位 " + slot + " 没有物品");
        }
        Map<String, Object> tag = item.get("tag") instanceof Map<?, ?> rawTag
                ? (Map<String, Object>) rawTag
                : new LinkedHashMap<>();
        if (enchants == null || enchants.isEmpty()) {
            tag.remove("Enchantments");
        } else {
            List<Object> list = new ArrayList<>();
            for (Enchant enchant : enchants) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("id", enchant.id());
                entry.put("lvl", (short) Math.max(1, Math.min(255, enchant.level())));
                list.add(entry);
            }
            tag.put("Enchantments", list);
        }
        if (tag.isEmpty()) {
            item.remove("tag");
        } else {
            item.put("tag", tag);
        }
        backup(dat);
        Nbt.write(dat, root);
    }

    /** 物品附加信息：自定义名与耐久。 */
    public record ItemMeta(String customName, int damage, int maxDamage) {
    }

    /** 读取自定义名 / 耐久（没有就返回空串与 0）。 */
    @SuppressWarnings("unchecked")
    public static ItemMeta readMeta(File dat, int slot) throws IOException {
        Map<String, Object> item = findItem(Nbt.read(dat), slot);
        if (item == null) {
            return new ItemMeta("", 0, 0);
        }
        int damage = item.get("Damage") instanceof Number number ? number.intValue() : 0;
        String name = "";
        int maxDamage = 0;
        if (item.get("tag") instanceof Map<?, ?> rawTag) {
            Map<String, Object> tag = (Map<String, Object>) rawTag;
            if (tag.get("display") instanceof Map<?, ?> rawDisplay) {
                Map<String, Object> display = (Map<String, Object>) rawDisplay;
                if (display.get("Name") instanceof String text) {
                    name = text;
                }
            }
            if (tag.get("Damage") instanceof Number number) {
                maxDamage = number.intValue();
            }
        }
        return new ItemMeta(name, damage, maxDamage);
    }

    /**
     * 写入自定义名与已损耗耐久；{@code maxDamage} 用于把上限写进 tag（避免物品在游戏里报错）。
     * 空名字表示清掉自定义名。
     */
    @SuppressWarnings("unchecked")
    public static void applyMeta(File dat, int slot, String customName, int damage, int maxDamage)
            throws IOException {
        Map<String, Object> root = Nbt.read(dat);
        Map<String, Object> item = findItem(root, slot);
        if (item == null) {
            throw new IOException("槽位 " + slot + " 没有物品");
        }
        Map<String, Object> tag = item.get("tag") instanceof Map<?, ?> rawTag
                ? (Map<String, Object>) rawTag
                : new LinkedHashMap<>();
        if (customName == null || customName.isBlank()) {
            tag.remove("display");
        } else {
            Map<String, Object> display = tag.get("display") instanceof Map<?, ?> rawDisplay
                    ? (Map<String, Object>) rawDisplay
                    : new LinkedHashMap<>();
            // 1.13+ 用 JSON 文本作为名字，这里按 JSON 字符串存，兼容大多数服务端
            display.put("Name", "{\"text\":\"" + customName.replace("\"", "\\\"") + "\"}");
            tag.put("display", display);
        }
        if (maxDamage > 0) {
            tag.put("Damage", maxDamage);
        } else {
            tag.remove("Damage");
        }
        if (tag.isEmpty()) {
            item.remove("tag");
        } else {
            item.put("tag", tag);
        }
        if (damage > 0) {
            item.put("Damage", damage);
        } else {
            item.remove("Damage");
        }
        backup(dat);
        Nbt.write(dat, root);
    }
    /** 读取物品描述行（tag.display.Lore，每行一个 JSON 文本）。 */
    @SuppressWarnings("unchecked")
    public static List<String> readLore(File dat, int slot) throws IOException {
        List<String> lines = new ArrayList<>();
        Map<String, Object> item = findItem(Nbt.read(dat), slot);
        if (item == null || !(item.get("tag") instanceof Map<?, ?> rawTag)) {
            return lines;
        }
        Map<String, Object> tag = (Map<String, Object>) rawTag;
        if (!(tag.get("display") instanceof Map<?, ?> rawDisplay)) {
            return lines;
        }
        Map<String, Object> display = (Map<String, Object>) rawDisplay;
        if (!(display.get("Lore") instanceof List<?> list)) {
            return lines;
        }
        for (Object element : list) {
            if (element instanceof String text) {
                lines.add(text);
            }
        }
        return lines;
    }

    /** 覆盖描述行；传空列表等于清掉 Lore。 */
    @SuppressWarnings("unchecked")
    public static void applyLore(File dat, int slot, List<String> lore) throws IOException {
        Map<String, Object> root = Nbt.read(dat);
        Map<String, Object> item = findItem(root, slot);
        if (item == null) {
            throw new IOException("槽位 " + slot + " 没有物品");
        }
        Map<String, Object> tag = item.get("tag") instanceof Map<?, ?> rawTag
                ? (Map<String, Object>) rawTag
                : new LinkedHashMap<>();
        if (lore == null || lore.isEmpty()) {
            if (tag.get("display") instanceof Map<?, ?> rawDisplay) {
                Map<String, Object> display = (Map<String, Object>) rawDisplay;
                display.remove("Lore");
                if (display.isEmpty()) {
                    tag.remove("display");
                }
            }
        } else {
            Map<String, Object> display = tag.get("display") instanceof Map<?, ?> rawDisplay
                    ? (Map<String, Object>) rawDisplay
                    : new LinkedHashMap<>();
            List<Object> list = new ArrayList<>();
            for (String line : lore) {
                list.add("{\"text\":\"" + line.replace("\"", "\\\"") + "\",\"color\":\"gray\",\"italic\":false}");
            }
            display.put("Lore", list);
            tag.put("display", display);
        }
        if (tag.isEmpty()) {
            item.remove("tag");
        } else {
            item.put("tag", tag);
        }
        backup(dat);
        Nbt.write(dat, root);
    }
    /**
     * 把某个槽位移动到目标槽位（目标有物品就交换）。
     *
     * @return 是否真的改动了文件
     */
    @SuppressWarnings("unchecked")
    public static boolean moveSlot(File dat, int fromSlot, int toSlot) throws IOException {
        if (fromSlot == toSlot) {
            return false;
        }
        Map<String, Object> root = Nbt.read(dat);
        Map<String, Object> from = findItem(root, fromSlot);
        if (from == null) {
            return false;
        }
        Map<String, Object> to = findItem(root, toSlot);
        from.put("Slot", (byte) toSlot);
        if (to != null) {
            to.put("Slot", (byte) fromSlot);
        }
        backup(dat);
        Nbt.write(dat, root);
        return true;
    }
    /** 在根的 Inventory 里按槽位找物品。 */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> findItem(Map<String, Object> root, int slot) {
        if (!(root.get("Inventory") instanceof List<?> list)) {
            return null;
        }
        for (Object element : list) {
            if (!(element instanceof Map<?, ?> raw)) {
                continue;
            }
            Map<String, Object> item = (Map<String, Object>) raw;
            if (item.get("Slot") instanceof Number number && number.intValue() == slot) {
                return item;
            }
        }
        return null;
    }
    private static int freeSlot(List<Object> inventory) {
        java.util.Set<Integer> used = new java.util.HashSet<>();
        for (Object element : inventory) {
            if (element instanceof Map<?, ?> raw && raw.get("Slot") instanceof Number number) {
                used.add(number.intValue());
            }
        }
        for (int slot = 0; slot < 36; slot++) {
            if (!used.contains(slot)) {
                return slot;
            }
        }
        return -1;
    }

    /** 写回前留一份副本：server-backups/<name>-<时间戳>.bak。 */
    static File backup(File dat) throws IOException {
        File dir = new File(dat.getParentFile().getParentFile().getParentFile(),
                "server-backups");
        if (!dir.isDirectory() && !dir.mkdirs()) {
            throw new IOException("无法创建备份目录：" + dir);
        }
        String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        File copy = new File(dir, dat.getName() + "-" + stamp + ".bak");
        Files.copy(dat.toPath(), copy.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return copy;
    }

    /** 删除 id 里的命名空间前缀，界面上好看一点。 */
    public static String shortId(String itemId) {
        if (itemId == null) {
            return "";
        }
        int colon = itemId.indexOf(':');
        return colon >= 0 ? itemId.substring(colon + 1) : itemId;
    }
}
