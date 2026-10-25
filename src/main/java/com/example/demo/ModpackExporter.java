package com.example.demo;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** 导出 PCL / Modrinth 可导入的 .mrpack 整合包。 */
public final class ModpackExporter {

    private ModpackExporter() {
    }

    public static void export(File gameDir, Instance instance, File output, boolean includeSaves) throws IOException {
        JsonObject index = new JsonObject();
        index.addProperty("formatVersion", 1);
        index.addProperty("game", "minecraft");
        index.addProperty("versionId", instance.id);
        index.addProperty("name", instance.id);
        index.addProperty("summary", "由 Neko Craft Launcher 导出的整合包");
        index.add("files", new com.google.gson.JsonArray());
        JsonObject deps = new JsonObject();
        deps.addProperty("minecraft", instance.mcVersion);
        String loaderKey = switch (instance.loader == null ? "" : instance.loader.toLowerCase(Locale.ROOT)) {
            case "fabric" -> "fabric-loader";
            case "quilt" -> "quilt-loader";
            case "forge" -> "forge";
            case "neoforge" -> "neoforge";
            default -> "";
        };
        if (!loaderKey.isBlank() && instance.loaderVersion != null && !instance.loaderVersion.isBlank()) {
            deps.addProperty(loaderKey, instance.loaderVersion);
        }
        index.add("dependencies", deps);

        File source = InstanceDirs.root(gameDir, instance.id, true);
        File[] roots = includeSaves
                ? new File[]{new File(source, "mods"), new File(source, "config"),
                    new File(source, "defaultconfigs"), new File(source, "resourcepacks"),
                    new File(source, "shaderpacks"), new File(source, "saves"), new File(source, "options.txt")}
                : new File[]{new File(source, "mods"), new File(source, "config"),
                    new File(source, "defaultconfigs"), new File(source, "resourcepacks"),
                    new File(source, "shaderpacks"), new File(source, "options.txt")};

        try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(output))) {
            zip.putNextEntry(new ZipEntry("modrinth.index.json"));
            zip.write(new GsonBuilder().setPrettyPrinting().create().toJson(index)
                    .getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            for (File root : roots) {
                if (root == null || !root.exists()) continue;
                if (root.isFile()) {
                    addFile(zip, root, "overrides/" + root.getName());
                } else {
                    addTree(zip, root, "overrides/" + root.getName());
                }
            }
        }
    }

    private static void addTree(ZipOutputStream zip, File dir, String prefix) throws IOException {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File file : files) {
            String name = prefix + "/" + file.getName();
            if (file.isDirectory()) addTree(zip, file, name);
            else addFile(zip, file, name);
        }
    }

    private static void addFile(ZipOutputStream zip, File file, String name) throws IOException {
        zip.putNextEntry(new ZipEntry(name.replace('\\', '/')));
        try (InputStream in = new FileInputStream(file)) {
            in.transferTo(zip);
        }
        zip.closeEntry();
    }
}
