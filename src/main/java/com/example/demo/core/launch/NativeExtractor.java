package com.example.demo.core.launch;

import com.example.demo.core.meta.VersionProfile;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * 原生库（natives）解压。
 *
 * <p>启动器只要把 {@code libraries} 里带 {@code -natives-windows.jar} 的包解开、把 dll 放到
 * {@code natives_directory}，游戏才能跑起来。这里刻意按子目录（lwjgl / jna / netty / java）分开，
 * 避免同名 dll 互相覆盖。
 *
 * <p>纯文件操作，没有 UI 依赖，可以直接单测。
 */
public final class NativeExtractor {

    private NativeExtractor() {
    }

    /**
     * 一条原生库：下载地址、libraries 下的相对路径、校验信息（老格式没有 sha1/size 时为 null/-1）。
     */
    public record NativeArtifact(String url, String path, String sha1, long size) {
    }

    /**
     * 把该版本所有原生库解压到 {@code nativesDir}。
     *
     * @return 本次新解出的 dll 数量（已存在的不重复解压）
     */
    public static int extract(VersionProfile profile, File libsDir, File nativesDir) {
        if (nativesDir != null && !nativesDir.exists()) {
            nativesDir.mkdirs();
        }
        int extracted = 0;
        for (JsonObject lib : profile.nativeLibraries()) {
            NativeArtifact artifact = nativeArtifact(lib);
            if (artifact == null) {
                continue;
            }
            File jarFile = new File(libsDir, artifact.path());
            if (!jarFile.isFile()) {
                continue;
            }
            File target = subDir(nativesDir, artifact.path());
            if (!target.exists()) {
                target.mkdirs();
            }
            extracted += extractDlls(jarFile, target);
            // 1.7.10 之前的部分库把 dll 放在 jar 根目录，同时也需要直接放在 natives 根
            if (!target.equals(nativesDir)) {
                extracted += extractDlls(jarFile, nativesDir);
            }
        }
        return extracted;
    }

    /**
     * 找出一条原生库的下载地址与 libraries 相对路径。
     *
     * <p>优先用版本 JSON 里的 {@code downloads.classifiers.windows}（1.19+ 都是这样，
     * 路径带 {@code natives-windows}），没有时退回老格式的 {@code -natives-windows.jar}。
     *
     * @return 不是原生库时返回 {@code null}
     */
    public static NativeArtifact nativeArtifact(JsonObject lib) {
        try {
            JsonObject downloads = lib.getAsJsonObject("downloads");
            if (lib.has("natives")
                    && lib.get("natives").isJsonObject()
                    && downloads != null
                    && downloads.has("classifiers")) {
                JsonObject natives = lib.getAsJsonObject("natives");
                String key = natives.has("windows") ? natives.get("windows").getAsString() : null;
                if (key == null) {
                    return null;
                }
                key = key.replace("${arch}", "64");
                JsonObject classifiers = downloads.getAsJsonObject("classifiers");
                if (!classifiers.has(key)) {
                    return null;
                }
                JsonObject artifact = classifiers.getAsJsonObject(key);
                String sha1 = artifact.has("sha1") ? artifact.get("sha1").getAsString() : null;
                long size = artifact.has("size") ? artifact.get("size").getAsLong() : -1L;
                return new NativeArtifact(
                        artifact.get("url").getAsString(),
                        artifact.get("path").getAsString(),
                        sha1,
                        size);
            }
            String path = VersionProfile.libraryPath(lib);
            String url = VersionProfile.libraryUrl(lib);
            if (path != null && url != null && path.endsWith("-natives-windows.jar")) {
                return new NativeArtifact(url, path,
                        VersionProfile.librarySha1(lib), VersionProfile.librarySize(lib));
            }
        } catch (Exception ignored) {
            // 结构异常的库直接当没有原生文件
        }
        return null;
    }

    /** 按库名落到子目录：同名 dll（如不同版本的 lwjgl）不会互相覆盖。 */
    public static File subDir(File nativesDir, String libraryPath) {
        String lower = libraryPath.toLowerCase();
        if (lower.contains("lwjgl")) {
            return new File(nativesDir, "lwjgl");
        }
        if (lower.contains("jna")) {
            return new File(nativesDir, "jna");
        }
        if (lower.contains("netty")) {
            return new File(nativesDir, "netty");
        }
        return new File(nativesDir, "java");
    }

    /**
     * 只解压 {@code .dll}，大小一致就跳过。
     *
     * <p>用 {@link ZipFile} 而不是 {@code ZipInputStream}：后者要解压每个 entry 才能跳过，
     * 光扫 LWJGL 的 jar 就要几百毫秒。这里通常一个文件都不用解，因此必须够便宜。
     */
    public static int extractDlls(File jarFile, File nativesDir) {
        int count = 0;
        try (ZipFile zip = new ZipFile(jarFile)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String entryName = entry.getName().replace('\\', '/');
                int slash = entryName.lastIndexOf('/');
                String fileName = slash >= 0 ? entryName.substring(slash + 1) : entryName;
                if (!fileName.toLowerCase().endsWith(".dll")) {
                    continue;
                }
                File outFile = new File(nativesDir, fileName);
                if (!outFile.toPath().normalize().startsWith(nativesDir.toPath())) {
                    continue;
                }
                // 不用比较 mtime：压缩包重新下载后 mtime 会变新，会导致每次都重解压
                if (outFile.isFile() && (entry.getSize() < 0 || outFile.length() == entry.getSize())) {
                    continue;
                }
                try (InputStream in = zip.getInputStream(entry);
                     FileOutputStream out = new FileOutputStream(outFile)) {
                    in.transferTo(out);
                }
                count++;
            }
        } catch (Exception ignored) {
            // 解压失败不影响启动，游戏会自己报缺 dll
        }
        return count;
    }
}
