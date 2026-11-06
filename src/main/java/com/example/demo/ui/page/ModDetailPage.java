package com.example.demo.ui.page;

import com.example.demo.core.model.ModpackProject;
import com.example.demo.ui.shell.Icons;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 社区模组/资源包详情页：标题 + 简介 + 按「游戏版本 / 加载器」折叠的可下载文件列表。
 *
 * <p>页面壳与分组折叠在这里；每个文件行（含下载动作）由 {@link Host#versionRow} 提供，
 * 下载逻辑仍留在调用方。
 */
public final class ModDetailPage {

    public interface Host {
        /** 返回下载页的社区列表。 */
        void back();

        /** 构建单个可下载文件行。 */
        Node versionRow(ModpackProject project, JsonObject version);
    }

    private final Host host;

    public ModDetailPage(Host host) {
        this.host = host;
    }

    /** 页面骨架：返回按钮 + 标题 + 简介 + 「正在读取…」占位。 */
    public VBox shell(ModpackProject project) {
        VBox page = new VBox(10.0);
        page.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        Button back = new Button("返回下载");
        back.setGraphic(Icons.node("back", 13.0, "#9ca3af"));
        back.setGraphicTextGap(4.0);
        back.setStyle("-fx-background-color: transparent; -fx-text-fill: -app-text-dim;"
                + " -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 2 4;");
        back.setOnAction(e -> host.back());

        Label title = new Label(project.getTitle());
        title.setStyle("-fx-text-fill: -app-text; -fx-font-size: 20px; -fx-font-weight: bold;");
        Label source = new Label(project.getSource());
        source.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11px;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox top = new HBox(10.0, back, title, spacer, source);
        top.setAlignment(Pos.CENTER_LEFT);

        Label desc = new Label(project.getDescription());
        desc.setWrapText(true);
        desc.setStyle("-fx-text-fill: -app-text-2; -fx-font-size: 13px;");
        Label filter = new Label("显示全部游戏版本与加载器，按版本/加载器折叠；不会自动套用当前实例。");
        filter.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11px;");
        filter.setWrapText(true);
        VBox hero = new VBox(6.0, desc, filter);
        hero.setPadding(new Insets(14.0));
        hero.setStyle("-fx-background-color: -app-card; -fx-background-radius: 12;"
                + " -fx-border-color: -app-border-2; -fx-border-radius: 12;");

        Label loading = new Label("正在读取可安装版本…");
        loading.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 12px;");
        page.getChildren().addAll(top, hero, loading);
        return page;
    }

    /** 版本列表：按「游戏版本 / 加载器」分组，每组一个可折叠面板。 */
    public VBox versionList(ModpackProject project, JsonArray versions) {
        VBox box = new VBox(8.0);
        Label heading = new Label("全部可下载文件（" + versions.size() + "）· 按游戏版本 / 加载器折叠");
        heading.setStyle("-fx-text-fill: -app-text-2; -fx-font-size: 12px; -fx-font-weight: bold;");
        box.getChildren().add(heading);

        Map<String, List<JsonObject>> groups = new LinkedHashMap<>();
        for (int i = 0; i < versions.size(); i++) {
            JsonObject version = versions.get(i).getAsJsonObject();
            String key = firstJsonValue(version, "game_versions")
                    + " / " + firstJsonValue(version, "loaders");
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(version);
        }

        boolean first = true;
        for (Map.Entry<String, List<JsonObject>> entry : groups.entrySet()) {
            VBox rows = new VBox(6.0);
            for (JsonObject version : entry.getValue()) {
                rows.getChildren().add(host.versionRow(project, version));
            }
            ScrollPane groupScroll = new ScrollPane(rows);
            groupScroll.setFitToWidth(true);
            groupScroll.setPrefViewportHeight(Math.min(300, 74 * entry.getValue().size()));
            groupScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;"
                    + " -fx-border-color: transparent;");
            TitledPane pane = new TitledPane(
                    entry.getKey() + "  ·  " + entry.getValue().size() + " 个文件", groupScroll);
            pane.setExpanded(first);
            first = false;
            pane.setStyle("-fx-text-fill: -app-text-2; -fx-font-size: 12px;");
            box.getChildren().add(pane);
        }
        if (versions.isEmpty()) {
            Label hint = new Label("这个项目没有可下载文件。");
            hint.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 12px;");
            box.getChildren().add(hint);
        }

        ScrollPane scroll = new ScrollPane(box);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;"
                + " -fx-border-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        VBox wrapper = new VBox(scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return wrapper;
    }

    private static String firstJsonValue(JsonObject object, String key) {
        if (!object.has(key) || !object.get(key).isJsonArray()) {
            return "其他";
        }
        JsonArray array = object.getAsJsonArray(key);
        return array.isEmpty() ? "其他" : array.get(0).getAsString();
    }
}
