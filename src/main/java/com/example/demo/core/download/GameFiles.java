package com.example.demo.core.download;

import com.example.demo.core.launch.NativeExtractor;
import com.example.demo.core.meta.VersionProfile;
import com.example.demo.core.model.AssetIndex;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 「启动前把文件准备好」的纯逻辑：客户端 jar、依赖库、资源、原生库。
 *
 * <p>从 {@code HelloApplication} 里搬出来，只依赖 core；日志用 {@link Consumer}，进度用
 * {@link DownloadManager.Progress}，因此可以脱离 UI 单测，也能被 CLI / 测试工具复用。
 *
 * <p>每个方法都保证：文件已存在且校验通过就跳过，不重复下载。
 */
public final class GameFiles {

    private static final Gson GSON = new Gson();

    private GameFiles() {
    }

    /**
     * 确保原版客户端 jar 存在（先镜像、再官方源）。
     *
     * @param downloadSource auto / official / mirror
     */
    public static void ensureClientJar(VersionProfile profile, File gameDir, String downloadSource,
                                       Consumer<String> log, DownloadManager.Progress progress)
            throws Exception {
        File versionDir = new File(new File(gameDir, "versions"), profile.getRootId());
        File clientJar = new File(versionDir, profile.getRootId() + ".jar");
        String clientUrl = profile.getClientUrl();
        if (clientJar.isFile() && clientJar.length() != 0L) {
            return;
        }
        if (clientUrl == null) {
            throw new IllegalStateException("版本 JSON 里没有客户端下载地址: " + profile.getRootId());
        }
        String mirrorUrl = "https://bmclapi2.bangbang93.com/version/" + profile.getRootId() + "/client";
        boolean mirrorFirst = !"official".equals(downloadSource);
        if (mirrorFirst) {
            log.accept("📥 通过 BMCLAPI 镜像下载客户端 jar...");
            DownloadManager.Stats mirrorStats = DownloadManager.downloadRanged(
                    new DownloadManager.Task(mirrorUrl, clientJar, profile.getClientSha1(), profile.getClientSize()),
                    4, progress);
            if (mirrorStats.failed > 0 || !clientJar.isFile() || clientJar.length() == 0L) {
                log.accept("⚠ 镜像下载失败，切换 Mojang 官方源…");
            }
        }
        if (!clientJar.isFile() || clientJar.length() == 0L) {
            log.accept("📥 通过 Mojang 官方源下载客户端 jar...");
            DownloadManager.Stats stats = DownloadManager.downloadRanged(
                    new DownloadManager.Task(clientUrl, clientJar, profile.getClientSha1(), profile.getClientSize()),
                    4, progress);
            if (stats.failed > 0 || !clientJar.isFile()) {
                throw new IllegalStateException("客户端 jar 下载失败，请检查网络后重试");
            }
        }
    }

    /** 依赖库 + 原生库 jar：按版本 JSON 的 sha1/size 增量补齐，失败会指出具体 URL。 */
    public static void ensureLibraries(VersionProfile profile, File gameDir,
                                       Consumer<String> log, DownloadManager.Progress progress)
            throws Exception {
        File libsDir = new File(gameDir, "libraries");
        List<DownloadManager.Task> tasks = new ArrayList<>();
        for (JsonObject lib : profile.classpathLibraries()) {
            String path = VersionProfile.libraryPath(lib);
            String url = VersionProfile.libraryUrl(lib);
            if (path == null || url == null) {
                continue;
            }
            tasks.add(new DownloadManager.Task(url, new File(libsDir, path),
                    VersionProfile.librarySha1(lib), VersionProfile.librarySize(lib)));
        }
        for (JsonObject lib : profile.nativeLibraries()) {
            NativeExtractor.NativeArtifact artifact = NativeExtractor.nativeArtifact(lib);
            if (artifact == null) {
                continue;
            }
            tasks.add(new DownloadManager.Task(artifact.url(), new File(libsDir, artifact.path()),
                    artifact.sha1(), artifact.size()));
        }
        log.accept("📦 依赖库 " + tasks.size() + " 个，校验中...");
        DownloadManager.Stats stats = DownloadManager.downloadAll(tasks, 16, progress);
        String summary = "   依赖库就绪：新下载 " + stats.downloaded + "，已存在 " + stats.skipped;
        if (stats.failed > 0) {
            summary = summary + "，失败 " + stats.failed;
        }
        log.accept(summary);
        if (stats.failed > 0) {
            List<String> failures;
            synchronized (stats.failures) {
                failures = new ArrayList<>(stats.failures);
            }
            for (String failedUrl : failures) {
                log.accept("   ✗ " + failedUrl);
            }
            throw new IllegalStateException(
                    "有 " + stats.failed + " 个依赖库没下载成功，请检查网络后重试（已下载的会保留）");
        }
    }

    /**
     * 资源文件：索引没变就整轮跳过（5000+ 个文件逐个 stat 要一秒多）。
     * 资源下载失败不算致命错误 —— 游戏能启动，只是可能缺音效/语言。
     */
    public static void ensureAssets(VersionProfile profile, File gameDir,
                                    Consumer<String> log, DownloadManager.Progress progress) {
        String indexId = profile.getAssetIndexId();
        if (indexId == null || indexId.isBlank()) {
            return;
        }
        File indexFile = new File(gameDir, "assets/indexes/" + indexId + ".json");
        File objectsDir = new File(gameDir, "assets/objects");
        File stampFile = new File(gameDir, "assets/.verified-" + indexId + ".stamp");
        String signature = indexId + ":" + indexFile.length() + ":" + indexFile.lastModified()
                + ":" + objectsDir.isDirectory();
        if (stampFile.isFile() && objectsDir.isDirectory()) {
            try {
                if (signature.equals(Files.readString(stampFile.toPath(), StandardCharsets.UTF_8).trim())) {
                    log.accept("   资源索引未变化，跳过逐文件校验（" + indexId + "）");
                    return;
                }
            } catch (Exception ignored) {
                // 读不到 stamp 就当没校验过，走完整流程
            }
        }
        if (indexFile.isFile() && !looksLikeJson(indexFile)) {
            log.accept("索引文件损坏，正在重新下载: " + indexId);
            indexFile.delete();
        }
        if (!indexFile.isFile()) {
            String indexUrl = profile.getAssetIndexUrl();
            if (indexUrl == null) {
                return;
            }
            log.accept("📄 下载资源索引 " + indexId + "...");
            DownloadManager.downloadAll(
                    List.of(new DownloadManager.Task(indexUrl, indexFile, null, -1L)), 1, null);
        }
        if (!indexFile.isFile()) {
            return;
        }
        AssetIndex index;
        try (Reader reader = new FileReader(indexFile, StandardCharsets.UTF_8)) {
            index = GSON.fromJson(reader, AssetIndex.class);
        } catch (Exception broken) {
            return;
        }
        if (index == null || index.getObjects() == null || index.getObjects().isEmpty()) {
            return;
        }
        List<DownloadManager.Task> tasks = new ArrayList<>();
        for (AssetIndex.AssetObject object : index.getObjects().values()) {
            String hash = object.getHash();
            if (hash == null || hash.length() < 2) {
                continue;
            }
            File target = new File(objectsDir, hash.substring(0, 2) + "/" + hash);
            tasks.add(new DownloadManager.Task(
                    "https://resources.download.minecraft.net/" + hash.substring(0, 2) + "/" + hash,
                    target, hash, object.getSize()));
        }
        if (tasks.isEmpty()) {
            return;
        }
        log.accept("🎨 资源文件 " + tasks.size() + " 个，校验中（首次会比较久）...");
        DownloadManager.Stats stats = DownloadManager.downloadAll(tasks, 32, progress);
        String summary = "   资源就绪：新下载 " + stats.downloaded + "，已存在 " + stats.skipped;
        if (stats.failed > 0) {
            summary = summary + "，失败 " + stats.failed;
        }
        log.accept(summary);
        if (stats.failed > 0) {
            log.accept("   ⚠️ 部分资源没下载成功，游戏仍可启动（可能缺音效/语言文件）");
        } else {
            try {
                Files.writeString(stampFile.toPath(), signature, StandardCharsets.UTF_8);
            } catch (Exception ignored) {
                // 写不了 stamp 只是下次还会再校验一遍，不影响启动
            }
        }
    }

    private static boolean looksLikeJson(File file) {
        if (file.length() <= 2L) {
            return false;
        }
        try (InputStream in = Files.newInputStream(file.toPath(), new OpenOption[0])) {
            int b;
            while ((b = in.read()) == ' ' || b == '\n' || b == '\r' || b == '\t') {
                // 跳过 BOM / 空白
            }
            return b == '{';
        } catch (Exception e) {
            return false;
        }
    }
}
