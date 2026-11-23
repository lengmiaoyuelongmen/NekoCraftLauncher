package com.example.demo.core.server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * 最小可用的 NBT 读写，只为实现「编辑玩家背包」这类需求。
 *
 * <p>Minecraft 的 {@code playerdata/<uuid>.dat} 是 gzip 压缩的 NBT。这里支持
 * Compound / List / 基础标量（Byte/Short/Int/Long/Float/Double/String/ByteArray/IntArray），
 * 读写都保持根 Compound 的结构，不认识的部分原样保留（整棵树读进来再整体写回）。
 *
 * <p>值用 Java 类型表示：Byte/Short/Integer/Long/Float/Double/String/byte[]/int[]/
 * {@code Map<String,Object>}（有序）/ {@code List<Object>}（带元素类型）。
 */
public final class Nbt {

    /** NBT 标签类型。 */
    public static final int TAG_END = 0;
    public static final int TAG_BYTE = 1;
    public static final int TAG_SHORT = 2;
    public static final int TAG_INT = 3;
    public static final int TAG_LONG = 4;
    public static final int TAG_FLOAT = 5;
    public static final int TAG_DOUBLE = 6;
    public static final int TAG_BYTE_ARRAY = 7;
    public static final int TAG_STRING = 8;
    public static final int TAG_LIST = 9;
    public static final int TAG_COMPOUND = 10;
    public static final int TAG_INT_ARRAY = 11;

    private Nbt() {
    }

    /** 根 Compound 的读写入口。 */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> read(File file) throws IOException {
        try (GZIPInputStream gzip = new GZIPInputStream(Files.newInputStream(file.toPath()))) {
            DataInputStream in = new DataInputStream(gzip);
            int type = in.readUnsignedByte();
            if (type != TAG_COMPOUND) {
                throw new IOException("根标签不是 Compound（type=" + type + "）");
            }
            in.readUTF();
            return (Map<String, Object>) readPayload(in, TAG_COMPOUND);
        }
    }

    public static void write(File file, Map<String, Object> root) throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("无法创建目录：" + parent);
        }
        try (GZIPOutputStream gzip = new GZIPOutputStream(Files.newOutputStream(file.toPath()))) {
            DataOutputStream out = new DataOutputStream(gzip);
            out.writeByte(TAG_COMPOUND);
            out.writeUTF("");
            writePayload(out, TAG_COMPOUND, root);
        }
    }

    // ---------- 读 ----------

    private static Object readPayload(DataInputStream in, int type) throws IOException {
        switch (type) {
            case TAG_BYTE:
                return in.readByte();
            case TAG_SHORT:
                return in.readShort();
            case TAG_INT:
                return in.readInt();
            case TAG_LONG:
                return in.readLong();
            case TAG_FLOAT:
                return in.readFloat();
            case TAG_DOUBLE:
                return in.readDouble();
            case TAG_BYTE_ARRAY: {
                byte[] data = new byte[Math.max(0, in.readInt())];
                in.readFully(data);
                return data;
            }
            case TAG_STRING:
                return in.readUTF();
            case TAG_LIST: {
                int elementType = in.readUnsignedByte();
                int size = in.readInt();
                List<Object> list = new ArrayList<>(Math.max(0, Math.min(size, 4096)));
                for (int i = 0; i < size; i++) {
                    list.add(readPayload(in, elementType));
                }
                return list;
            }
            case TAG_COMPOUND: {
                Map<String, Object> map = new LinkedHashMap<>();
                while (true) {
                    int childType = in.readUnsignedByte();
                    if (childType == TAG_END) {
                        return map;
                    }
                    String name = in.readUTF();
                    map.put(name, readPayload(in, childType));
                }
            }
            case TAG_INT_ARRAY: {
                int[] data = new int[Math.max(0, in.readInt())];
                for (int i = 0; i < data.length; i++) {
                    data[i] = in.readInt();
                }
                return data;
            }
            default:
                throw new IOException("不支持的 NBT 标签类型：" + type);
        }
    }

    // ---------- 写 ----------

    private static void writePayload(DataOutputStream out, int type, Object value) throws IOException {
        switch (type) {
            case TAG_BYTE:
                out.writeByte(((Number) value).intValue());
                break;
            case TAG_SHORT:
                out.writeShort(((Number) value).intValue());
                break;
            case TAG_INT:
                out.writeInt(((Number) value).intValue());
                break;
            case TAG_LONG:
                out.writeLong(((Number) value).longValue());
                break;
            case TAG_FLOAT:
                out.writeFloat(((Number) value).floatValue());
                break;
            case TAG_DOUBLE:
                out.writeDouble(((Number) value).doubleValue());
                break;
            case TAG_BYTE_ARRAY: {
                byte[] data = (byte[]) value;
                out.writeInt(data.length);
                out.write(data);
                break;
            }
            case TAG_STRING:
                out.writeUTF((String) value);
                break;
            case TAG_LIST: {
                List<?> list = (List<?>) value;
                int elementType = list.isEmpty() ? TAG_END : typeOf(list.get(0));
                out.writeByte(elementType);
                out.writeInt(list.size());
                for (Object element : list) {
                    writePayload(out, elementType, element);
                }
                break;
            }
            case TAG_COMPOUND: {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) value;
                for (Map.Entry<String, Object> entry : map.entrySet()) {
                    int childType = typeOf(entry.getValue());
                    out.writeByte(childType);
                    out.writeUTF(entry.getKey());
                    writePayload(out, childType, entry.getValue());
                }
                out.writeByte(TAG_END);
                break;
            }
            case TAG_INT_ARRAY: {
                int[] data = (int[]) value;
                out.writeInt(data.length);
                for (int item : data) {
                    out.writeInt(item);
                }
                break;
            }
            default:
                throw new IOException("不支持的 NBT 标签类型：" + type);
        }
    }

    /** Java 值 → NBT 类型。 */
    public static int typeOf(Object value) {
        if (value instanceof Byte) return TAG_BYTE;
        if (value instanceof Short) return TAG_SHORT;
        if (value instanceof Integer) return TAG_INT;
        if (value instanceof Long) return TAG_LONG;
        if (value instanceof Float) return TAG_FLOAT;
        if (value instanceof Double) return TAG_DOUBLE;
        if (value instanceof byte[]) return TAG_BYTE_ARRAY;
        if (value instanceof String) return TAG_STRING;
        if (value instanceof List) return TAG_LIST;
        if (value instanceof Map) return TAG_COMPOUND;
        if (value instanceof int[]) return TAG_INT_ARRAY;
        throw new IllegalArgumentException("无法映射到 NBT 类型：" + value.getClass());
    }

    /** 把任意对象树写进字节数组（测试与调试用）。 */
    public static byte[] toBytes(Map<String, Object> root) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(buffer);
        out.writeByte(TAG_COMPOUND);
        out.writeUTF("");
        writePayload(out, TAG_COMPOUND, root);
        out.flush();
        return buffer.toByteArray();
    }

    /** 从字节数组读回（测试与调试用）。 */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> fromBytes(byte[] data) throws IOException {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
        int type = in.readUnsignedByte();
        if (type != TAG_COMPOUND) {
            throw new IOException("根标签不是 Compound");
        }
        in.readUTF();
        return (Map<String, Object>) readPayload(in, TAG_COMPOUND);
    }
}
