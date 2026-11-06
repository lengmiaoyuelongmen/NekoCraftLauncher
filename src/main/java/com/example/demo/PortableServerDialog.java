package com.example.demo;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Window;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;

import com.example.demo.core.model.InstanceDirs;
import com.example.demo.core.download.DownloadManager;
import com.example.demo.core.meta.VersionProfile;
import com.example.demo.core.model.Instance;
/** 从实例创建便携服务端目录，并可选启动。 */
public final class PortableServerDialog {

    private PortableServerDialog() {
    }

    public static void show(Window owner, File gameDir, String instanceId, boolean isolation) {
        StackPane host = InAppDialog.globalHost();
        if (host != null) {
            showInApp(host, gameDir, instanceId, isolation);
            return;
        }
        InAppDialog.customSized(
                null,
                "便携联机服务器",
                buildContent(gameDir, instanceId, isolation),
                List.of(InAppDialog.Action.secondary("关闭", null)),
                null,
                560,
                480);
    }

    public static void showInApp(StackPane host, File gameDir, String instanceId, boolean isolation) {
        if (host == null) host = InAppDialog.globalHost();
        if (host == null) return;
        InAppDialog.customSized(
                host,
                "便携联机服务器",
                buildContent(gameDir, instanceId, isolation),
                List.of(InAppDialog.Action.secondary("关闭", null)),
                null,
                560,
                480);
    }

    private static Node buildContent(File gameDir, String instanceId, boolean isolation) {
        TextField port = new TextField("25565");
        TextField memory = new TextField("4096");
        TextField motd = new TextField("Neko Craft Server");
        CheckBox online = new CheckBox("正版验证 (online-mode)");
        online.setSelected(true);
        Label status = new Label("将复制 mods / config / scripts，并生成 start.bat。");
        status.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11px;");
        status.setWrapText(true);
        Button create = new Button("创建服务器");
        Button start = new Button("创建并启动");
        create.getStyleClass().addAll("btn-secondary", "btn-sm");
        start.getStyleClass().addAll("btn-primary", "btn-sm");
        HBox buttons = new HBox(8, create, start);
        VBox form = new VBox(8,
                new Label("端口"), port,
                new Label("最大内存 (MB)"), memory,
                new Label("MOTD"), motd,
                online, buttons, status);
        form.setPadding(new Insets(4));
        create.setOnAction(e -> createAsync(gameDir, instanceId, isolation, port, memory, motd, online, status,
                create, start, false));
        start.setOnAction(e -> createAsync(gameDir, instanceId, isolation, port, memory, motd, online, status,
                create, start, true));
        return form;
    }

    private static void createAsync(File gameDir, String instanceId, boolean isolation,
                                    TextField port, TextField memory,
                                    TextField motd, CheckBox online, Label status, Button create,
                                    Button start, boolean launch) {
        create.setDisable(true);
        start.setDisable(true);
        status.setText("正在创建服务器目录…");
        int portValue;
        int memoryValue;
        try {
            portValue = Integer.parseInt(port.getText().trim());
            memoryValue = Math.max(512, Integer.parseInt(memory.getText().trim()));
        } catch (Exception ex) {
            status.setText("端口或内存格式错误");
            create.setDisable(false);
            start.setDisable(false);
            return;
        }
        new Thread(() -> {
            try {
                File root = InstanceDirs.root(gameDir, instanceId, isolation);
                String safe = instanceId.replaceAll("[^0-9A-Za-z._-]", "_");
                File dest = new File(new File(gameDir, "portable-servers"), safe);
                if (dest.exists()) {
                    dest = new File(new File(gameDir, "portable-servers"),
                            safe + "-" + System.currentTimeMillis() / 1000);
                }
                Files.createDirectories(dest.toPath());
                copyDir(new File(root, "mods"), new File(dest, "mods"));
                for (String dir : new String[]{"config", "defaultconfigs", "scripts", "kubejs", "datapacks"}) {
                    copyDir(new File(root, dir), new File(dest, dir));
                }
                Files.writeString(new File(dest, "server.properties").toPath(),
                        "server-port=" + portValue + "\nmotd=" + motd.getText().trim()
                                + "\nonline-mode=" + online.isSelected() + "\n", StandardCharsets.UTF_8);
                Files.writeString(new File(dest, "eula.txt").toPath(),
                        "# 阅读 https://aka.ms/MinecraftEULA 后把 false 改为 true\n"
                                + "eula=false\n", StandardCharsets.UTF_8);
                String jarLine = "java -Xms1G -Xmx" + memoryValue + "M -jar server.jar nogui";
                Files.writeString(new File(dest, "start.bat").toPath(),
                        "@echo off\r\nchcp 65001 >nul\r\ntitle Neko Portable Server\r\n"
                                + jarLine + "\r\npause\r\n", StandardCharsets.UTF_8);
                Files.writeString(new File(dest, "README.txt").toPath(),
                        "Neko 便携服务端\r\n"
                                + "1. 阅读 eula.txt 并接受 EULA\r\n"
                                + "2. 原版实例会自动下载 server.jar；模组实例请安装对应加载器服务端\r\n"
                                + "3. 双击 start.bat 启动\r\n"
                                + "4. 端口 " + portValue + "，记得在防火墙放行\r\n", StandardCharsets.UTF_8);

                Instance instance = Instance.of(gameDir, instanceId);
                boolean downloaded = false;
                if ("原版".equals(instance.loader)) {
                    downloaded = downloadVanillaServer(gameDir, instanceId, dest);
                }
                final File serverDir = dest;
                final boolean hasJar = downloaded;
                Platform.runLater(() -> {
                    create.setDisable(false);
                    start.setDisable(false);
                    status.setText(hasJar
                            ? "创建完成，已下载 server.jar：" + serverDir.getAbsolutePath()
                            : "创建完成，需手动放入 server.jar：" + serverDir.getAbsolutePath());
                    if (launch) startServer(serverDir);
                    else openDir(serverDir);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    create.setDisable(false);
                    start.setDisable(false);
                    status.setText("创建失败：" + ex.getMessage());
                });
            }
        }, "portable-server").start();
    }

    private static boolean downloadVanillaServer(File gameDir, String instanceId, File dest) {
        try {
            String root = VersionProfile.load(gameDir, instanceId).getRootId();
            JsonObject json = JsonParser.parseString(Files.readString(
                    VersionProfile.jsonFile(gameDir, root).toPath(), StandardCharsets.UTF_8)).getAsJsonObject();
            JsonObject server = json.getAsJsonObject("downloads").getAsJsonObject("server");
            String url = server.get("url").getAsString();
            String sha1 = server.has("sha1") ? server.get("sha1").getAsString() : null;
            long size = server.has("size") ? server.get("size").getAsLong() : -1;
            DownloadManager.Stats stats = DownloadManager.downloadAll(
                    List.of(new DownloadManager.Task(url, new File(dest, "server.jar"), sha1, size)), 1, null);
            return stats.downloaded > 0;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static void startServer(File dir) {
        try {
            new ProcessBuilder("cmd", "/c", "start", "\"Neko Portable Server\"", "cmd", "/k", "start.bat")
                    .directory(dir).start();
        } catch (Exception ignored) {
        }
    }

    private static void openDir(File dir) {
        try {
            new ProcessBuilder("explorer.exe", dir.getAbsolutePath()).start();
        } catch (Exception ignored) {
        }
    }

    private static void copyDir(File from, File to) throws Exception {
        if (!from.isDirectory()) return;
        Files.walk(from.toPath()).forEach(path -> {
            try {
                java.nio.file.Path target = to.toPath().resolve(from.toPath().relativize(path));
                if (Files.isDirectory(path)) Files.createDirectories(target);
                else {
                    Files.createDirectories(target.getParent());
                    Files.copy(path, target, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (Exception ignored) {
            }
        });
    }
}
