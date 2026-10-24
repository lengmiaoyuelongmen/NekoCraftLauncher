package com.example.demo;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.File;
import java.util.ArrayList;
import java.util.StringJoiner;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 按版本 JSON 里的 arguments / rules / 占位符拼出完整的 java 启动命令。
 * 这样原版 1.19+、Fabric、Forge 的启动参数都能正确处理，而不是硬拼旧参数。
 */
public final class LaunchCommand {

    private LaunchCommand() {
    }

    public static List<String> build(VersionProfile profile, UserSession session, LauncherConfig config,
                                     File gameDir, String javaPath, File nativesDir, File injector) throws Exception {
        return build(profile, session, config, gameDir, gameDir, javaPath, nativesDir, injector);
    }

    /**
     * @param gameDir     基础游戏目录（libraries / assets 在这里）
     * @param instanceDir 该实例的 gameDir（版本隔离时是 versions/&lt;id&gt;）
     */
    public static List<String> build(VersionProfile profile, UserSession session, LauncherConfig config,
                                     File gameDir, File instanceDir, String javaPath, File nativesDir,
                                     File injector) throws Exception {

        if (profile.getMainClass() == null || profile.getMainClass().isBlank()) {
            throw new Exception("版本 JSON 缺少 mainClass");
        }

        File libsDir = new File(gameDir, "libraries");
        File rootDir = new File(new File(gameDir, "versions"), profile.getRootId());
        File rootJar = new File(rootDir, profile.getRootId() + ".jar");
        File childDir = new File(new File(gameDir, "versions"), profile.getId());
        File childJar = new File(childDir, profile.getId() + ".jar");
        // 继承版本优先用子版本同名 jar（Forge 的 ignoreList 依赖这个文件名）
        File clientJar = childJar.isFile() && childJar.length() > 0 ? childJar : rootJar;

        // Forge 1.20+ 用 net.minecraft:client:srg 提供游戏类，再把原始 client jar 放进
        // classpath 会出现两个 module 导出同一个包，JVM 会直接崩（ResolutionException）。
        boolean clientFromLibraries = false;
        for (JsonObject lib : profile.classpathLibraries()) {
            String name = lib.has("name") ? lib.get("name").getAsString() : "";
            if (name.startsWith("net.minecraft:client:")) {
                clientFromLibraries = true;
                break;
            }
        }
        if (!clientFromLibraries && (!clientJar.isFile() || clientJar.length() == 0)) {
            throw new Exception("客户端 jar 不存在，请先安装原版 " + profile.getRootId());
        }

        StringBuilder classpath = new StringBuilder();
        int libCount = 0;
        if (!clientFromLibraries) {
            classpath.append(clientJar.getAbsolutePath());
        }
        for (JsonObject lib : profile.classpathLibraries()) {
            String path = VersionProfile.libraryPath(lib);
            if (path == null) continue;
            File jar = new File(libsDir, path);
            if (jar.isFile() && jar.length() > 0) {
                if (classpath.length() > 0) classpath.append(File.pathSeparator);
                classpath.append(jar.getAbsolutePath());
                libCount++;
            }
        }

        Map<String, String> vars = new HashMap<>();
        vars.put("auth_player_name", nvl(session.getUsername()));
        vars.put("version_name", profile.getId());
        vars.put("game_directory", instanceDir.getAbsolutePath());
        vars.put("assets_root", InstanceDirs.assets(gameDir).getAbsolutePath());
        vars.put("assets_index_name", profile.getAssetIndexId());
        vars.put("auth_uuid", session.getUuid() == null ? "" : session.getUuid().toString().replace("-", ""));
        vars.put("auth_access_token", nvl(session.getAccessToken()));
        vars.put("auth_xuid", "");
        vars.put("clientid", "");
        vars.put("user_type", session.getUserType() == null ? "legacy" : session.getUserType());
        vars.put("version_type", profile.getType());
        // 1.7.10 及更早版本要求 --userProperties 是合法 JSON 对象
        vars.put("user_properties", "{}");
        String nativesPath = nativesDir.getAbsolutePath();
        vars.put("natives_directory", nativesPath);
        vars.put("launcher_name", "MCLauncher");
        vars.put("launcher_version", "1.0");
        vars.put("classpath", classpath.toString());
        vars.put("library_directory", libsDir.getAbsolutePath());
        vars.put("classpath_separator", File.pathSeparator);
        vars.put("resolution_width", String.valueOf(config.getWindowWidth()));
        vars.put("resolution_height", String.valueOf(config.getWindowHeight()));

        Map<String, Boolean> features = new HashMap<>();
        features.put("is_demo_user", false);
        features.put("has_custom_resolution", config.getWindowWidth() > 0 && config.getWindowHeight() > 0);
        features.put("has_quick_plays_support", false);
        features.put("is_quick_play_singleplayer", false);
        features.put("is_quick_play_multiplayer", false);
        features.put("is_quick_play_realms", false);

        List<String> command = new ArrayList<>();
        command.add(javaPath);
        command.add("-Xmx" + config.getMemoryMb() + "M");
        command.add("-Dfile.encoding=UTF-8");
        command.add("-Dminecraft.launcher.brand=MCLauncher");

        String extra = config.getJvmArgs() == null ? "" : config.getJvmArgs().trim();
        if (!extra.isEmpty()) {
            for (String arg : extra.split("\\s+")) {
                if (!arg.isBlank()) command.add(arg);
            }
        }

        JsonArray jvm = profile.jvmArguments();
        if (jvm != null && jvm.size() > 0) {
            expand(jvm, vars, features, command);
        } else {
            command.add("-Djava.library.path=" + nativesPath);
            command.add("-cp");
            command.add(classpath.toString());
        }

        if (injector != null && injector.isFile()
                && session.getType() == UserSession.LoginType.THIRD_PARTY) {
            command.add("-javaagent:" + injector.getAbsolutePath() + "=" + nvl(session.getAuthServerUrl()));
        }

        command.add(profile.getMainClass());

        JsonArray game = profile.gameArguments();
        if (game != null && game.size() > 0) {
            expand(game, vars, features, command);
        } else if (profile.legacyMinecraftArguments() != null) {
            for (String token : profile.legacyMinecraftArguments().split(" ")) {
                if (!token.isBlank()) command.add(substitute(token, vars));
            }
        } else {
            throw new Exception("版本 JSON 既没有 arguments.game 也没有 minecraftArguments");
        }

        System.out.println("[launch] " + profile.getId() + " 类路径库数量=" + libCount);
        return command;
    }

    private static void expand(JsonArray array, Map<String, String> vars,
                               Map<String, Boolean> features, List<String> out) {
        for (JsonElement el : array) {
            if (el.isJsonPrimitive()) {
                out.add(substitute(el.getAsString(), vars));
            } else if (el.isJsonObject()) {
                JsonObject obj = el.getAsJsonObject();
                if (!VersionProfile.allowed(obj, features)) continue;
                JsonElement value = obj.get("value");
                if (value == null) continue;
                if (value.isJsonPrimitive()) {
                    out.add(substitute(value.getAsString(), vars));
                } else if (value.isJsonArray()) {
                    for (JsonElement v : value.getAsJsonArray()) {
                        out.add(substitute(v.getAsString(), vars));
                    }
                }
            }
        }
    }

    private static String substitute(String raw, Map<String, String> vars) {
        String result = raw;
        for (Map.Entry<String, String> entry : vars.entrySet()) {
            result = result.replace("${" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }

    private static String nvl(String s) {
        return s == null ? "" : s;
    }


}
