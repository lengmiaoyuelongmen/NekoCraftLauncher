package com.example.demo.core.mod;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.function.Consumer;

/**
 * 自动补前置：按 modId 去 Modrinth 查版本，挑出适配当前实例的文件下载到 mods 目录。
 *
 * <p>网络失败会先试官方源再试镜像；任何一个环节失败都只返回结果对象，不抛异常，
 * 由界面层汇总展示。
 */
public final class DependencyInstaller {

    /** 单个前置的安装结果。 */
    public record Result(String modId, String filename, String message, boolean ok) {
        public static Result ok(String modId, String filename) {
            return new Result(modId, filename, "已安装 " + filename, true);
        }

        public static Result fail(String modId, String message) {
            return new Result(modId, "", message, false);
        }
    }

    private DependencyInstaller() {
    }

    /**
     * 下载一个前置。
     *
     * @param modId Modrinth 的项目 id 或 slug（模组依赖里写的一般就是 slug）
     * @param modsDir 目标 mods 目录
     * @param target 当前实例的 MC 版本与加载器
     * @param http 复用启动器的 HttpClient
     * @param log 日志回调（可为 null）
     */
    public static Result installOne(
            String modId,
            File modsDir,
            VersionFit.Target target,
            HttpClient http,
            Consumer<String> log) {
        if (modId == null || modId.isBlank()) {
            return Result.fail("", "依赖 id 为空");
        }
        String body = null;
        for (String url : new String[] {
                DependencyPick.officialUrl(modId), DependencyPick.mirrorUrl(modId)}) {
            body = fetch(http, url);
            if (body != null && body.startsWith("[")) {
                break;
            }
            body = null;
        }
        if (body == null) {
            return Result.fail(modId, "查询 Modrinth 失败（官方与镜像都不可用）");
        }
        DependencyPick.Artifact artifact = DependencyPick.pick(body, target);
        if (artifact == null) {
            return Result.fail(modId, "没有适配 " + target.label() + " 的版本");
        }
        if (modsDir == null || (!modsDir.isDirectory() && !modsDir.mkdirs())) {
            return Result.fail(modId, "无法创建 mods 目录");
        }
        File modFile = new File(modsDir, artifact.filename());
        if (modFile.isFile() && modFile.length() > 0) {
            return Result.ok(modId, artifact.filename() + "（已存在）");
        }
        Path temp = null;
        try {
            temp = Files.createTempFile(modsDir.toPath(), "dep-", ".part");
            HttpRequest request = HttpRequest.newBuilder(URI.create(artifact.url()))
                    .timeout(Duration.ofMinutes(5))
                    .GET()
                    .build();
            HttpResponse<Path> response =
                    http.send(request, HttpResponse.BodyHandlers.ofFile(temp));
            if (response.statusCode() != 200) {
                Files.deleteIfExists(temp);
                return Result.fail(modId, "下载失败（HTTP " + response.statusCode() + "）");
            }
            Files.move(temp, modFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            if (log != null) {
                log.accept("✅ 已安装前置 " + modId + " → " + artifact.filename());
            }
            return Result.ok(modId, artifact.filename());
        } catch (Exception e) {
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp);
                } catch (Exception ignored) {
                    // 临时文件清理失败不影响结果
                }
            }
            return Result.fail(modId, "下载失败：" + e.getMessage());
        }
    }

    private static String fetch(HttpClient http, String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .header("User-Agent", "PCL-CE-Launcher/1.0")
                    .timeout(Duration.ofSeconds(20))
                    .GET()
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return response.body();
            }
        } catch (Exception ignored) {
            // 换下一个源
        }
        return null;
    }
}
