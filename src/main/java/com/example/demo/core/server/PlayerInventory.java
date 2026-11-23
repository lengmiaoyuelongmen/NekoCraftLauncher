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
