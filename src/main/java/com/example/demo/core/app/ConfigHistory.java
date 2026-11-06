package com.example.demo.core.app;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;

/** config.json 的本地历史快照（最多保留 20 份）。 */
public final class ConfigHistory {

    private static final int MAX = 20;
    private static long lastSnapshotAt;
    private static String lastHash = "";

    private ConfigHistory() {
    }

    public static File dir() {
        return new File(AppPaths.dataDir(), "config-history");
    }

    public static void snapshot(File config) {
        try {
            if (config == null || !config.isFile()) return;
            long now = System.currentTimeMillis();
            if (now - lastSnapshotAt < 30_000) return;
            String hash = Integer.toHexString(Arrays.hashCode(Files.readAllBytes(config.toPath())));
            if (hash.equals(lastHash)) return;
            File dir = dir();
            if (!dir.isDirectory()) dir.mkdirs();
            String name = "config-" + new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date(now)) + ".json";
            Files.copy(config.toPath(), new File(dir, name).toPath(), StandardCopyOption.REPLACE_EXISTING);
            lastSnapshotAt = now;
            lastHash = hash;
            prune();
        } catch (Exception ignored) {
        }
    }

    public static File[] list() {
        File[] files = dir().listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return new File[0];
        Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
        return files;
    }

    public static void restore(File snapshot, File config) throws Exception {
        Files.copy(snapshot.toPath(), config.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    private static void prune() {
        File[] files = list();
        for (int i = MAX; i < files.length; i++) files[i].delete();
    }
}
