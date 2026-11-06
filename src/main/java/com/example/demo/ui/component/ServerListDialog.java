package com.example.demo.ui.component;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Window;

import java.io.File;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.example.demo.ui.shell.InAppDialog;
import com.example.demo.ui.shell.UiFit;
/** 启动器侧服务器列表，支持保存地址和一键启动并加入。 */
public final class ServerListDialog {

    public record Entry(String name, String address) {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private ServerListDialog() {
    }

    /** 旧入口：有集成式宿主就渲染在主窗口里。 */
    public static void show(Window owner, File gameDir, Consumer<String> onJoin) {
        StackPane host = InAppDialog.globalHost();
        if (host != null) {
            showInApp(host, gameDir, onJoin);
            return;
        }
        Dialog<Void> dialog = new Dialog<>();
        if (owner != null) dialog.initOwner(owner);
        dialog.setTitle("服务器列表");
        dialog.getDialogPane().setContent(buildContent(gameDir, null));
        UiFit.dialog(dialog, 720, 480);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        if (owner != null && owner.getScene() != null) {
            dialog.getDialogPane().getStylesheets().addAll(owner.getScene().getStylesheets());
        }
        dialog.showAndWait();
    }

    public static void showInApp(StackPane host, File gameDir, Consumer<String> onJoin) {
        if (host == null) host = InAppDialog.globalHost();
        if (host == null) return;
        java.util.concurrent.atomic.AtomicReference<ListView<Entry>> listHolder =
                new java.util.concurrent.atomic.AtomicReference<>();
        Node content = buildContent(gameDir, listHolder);
        ((Region) content).setPrefHeight(360);
        InAppDialog.customSized(
                host,
                "服务器列表",
                content,
                List.of(
                        InAppDialog.Action.secondary("关闭", null),
                        InAppDialog.Action.primary(
                                "启动并加入",
                                () -> {
                                    Entry entry =
                                            listHolder.get() == null
                                                    ? null
                                                    : listHolder.get().getSelectionModel().getSelectedItem();
                                    if (entry != null && onJoin != null) {
                                        onJoin.accept(entry.address());
                                    }
                                })),
                null,
                780,
                540);
    }

    private static Node buildContent(
            File gameDir, java.util.concurrent.atomic.AtomicReference<ListView<Entry>> listOut) {
        File file = new File(gameDir, "servers.json");
        List<Entry> entries = load(file);
        ListView<Entry> list = new ListView<>();
        list.getItems().setAll(entries);
        list.setCellFactory(v -> new ListCell<>() {
            @Override
            protected void updateItem(Entry e, boolean empty) {
                super.updateItem(e, empty);
                setText(empty || e == null ? null : "🌐  " + e.name() + "    " + e.address());
                setStyle("-fx-text-fill: -app-text-2; -fx-font-size: 11px;");
            }
        });
        if (listOut != null) listOut.set(list);
        TextField name = new TextField();
        name.setPromptText("服务器名称");
        TextField address = new TextField();
        address.setPromptText("example.com 或 example.com:25565");
        HBox.setHgrow(name, Priority.ALWAYS);
        HBox.setHgrow(address, Priority.ALWAYS);
        Button add = button("添加");
        add.setOnAction(e -> {
            String a = address.getText().trim();
            if (a.isBlank()) return;
            String n = name.getText().trim();
            entries.add(new Entry(n.isBlank() ? a : n, a));
            save(file, entries);
            list.getItems().setAll(entries);
            name.clear();
            address.clear();
        });
        Button remove = button("删除");
        remove.setOnAction(e -> {
            Entry entry = list.getSelectionModel().getSelectedItem();
            if (entry == null) return;
            entries.remove(entry);
            save(file, entries);
            list.getItems().setAll(entries);
        });
        HBox form = new HBox(8, name, address, add);
        HBox tools = new HBox(8, remove);
        tools.setAlignment(Pos.CENTER_LEFT);
        Label hint = new Label("地址支持 host 或 host:port；「启动并加入」会用当前实例启动并直接连进去。");
        hint.setStyle("-fx-text-fill: -app-text-dim; -fx-font-size: 11px;");
        hint.setWrapText(true);
        VBox content = new VBox(10, hint, form, list, tools);
        content.setPadding(new Insets(4));
        VBox.setVgrow(list, Priority.ALWAYS);
        return content;
    }

    private static Button button(String text) {
        Button b = new Button(text);
        b.getStyleClass().addAll("btn-secondary", "btn-sm");
        return b;
    }

    private static List<Entry> load(File file) {
        try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            List<Entry> list = GSON.fromJson(reader, new TypeToken<List<Entry>>() { }.getType());
            return list == null ? new ArrayList<>() : list;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private static void save(File file, List<Entry> entries) {
        try (Writer writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            GSON.toJson(entries, writer);
        } catch (Exception ignored) {
        }
    }
}
