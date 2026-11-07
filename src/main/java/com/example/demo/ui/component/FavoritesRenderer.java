package com.example.demo.ui.component;

import com.example.demo.core.mod.ProjectFormat;
import com.example.demo.core.model.Favorites;
import com.example.demo.ui.shell.Icons;
import com.example.demo.ui.shell.Toast;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 收藏夹列表：按类型分组、可折叠，支持搜索与三种排序。
 *
 * <p>从主类搬出；分组折叠状态、下载动作与重绘由 {@link Host} 提供。
 */
public final class FavoritesRenderer {

    public interface Host {
        boolean groupOpen(String group);

        void setGroupOpen(String group, boolean open);

        void download(Favorites.Fav fav);

        /** 收藏变化后重绘整个列表。 */
        void refresh();
    }

    private static final String[] GROUPS = {"整合包", "模组", "资源包", "光影包", "数据包", "世界", "资源"};

    private final Host host;

    public FavoritesRenderer(Host host) {
        this.host = host;
    }

    /**
     * 重绘列表。
     *
     * @param container 目标容器
     * @param query     搜索词（小写，空串表示不过滤）
     * @param sort      排序方式：名称 / 下载量 / 其他视为「最近收藏」
     */
    public void render(VBox container, String query, String sort) {
        if (container == null) {
            return;
        }
        container.getChildren().clear();
        String keyword = query == null ? "" : query;
        List<Favorites.Fav> favs = new ArrayList<>(Favorites.list().stream()
                .filter(fav -> keyword.isEmpty()
                        || fav.title() != null && fav.title().toLowerCase().contains(keyword)
                        || fav.type() != null && fav.type().toLowerCase().contains(keyword))
                .toList());

        String ordering = sort == null ? "最近收藏" : sort;
        switch (ordering) {
            case "名称" -> favs.sort(Comparator.comparing(
                    fav -> fav.title() == null ? "" : fav.title().toLowerCase(Locale.ROOT)));
            case "下载量" -> favs.sort(Comparator.comparingLong(Favorites.Fav::downloads).reversed());
            default -> favs.sort(Comparator.comparingLong(Favorites.Fav::time).reversed());
        }

        if (favs.isEmpty()) {
            Label empty = new Label(Favorites.list().isEmpty()
                    ? "还没有收藏的资源。在下载页的卡片上点 ☆ 即可收藏。"
                    : "没有匹配的收藏资源。");
            empty.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 12px;");
            empty.setWrapText(true);
            container.getChildren().add(empty);
            return;
        }

        Map<String, List<Favorites.Fav>> grouped = new LinkedHashMap<>();
        for (String label : GROUPS) {
            List<Favorites.Fav> group = favs.stream()
                    .filter(fav -> ProjectFormat.typeLabel(fav.type()).equals(label))
                    .toList();
            if (!group.isEmpty()) {
                grouped.put(label, group);
            }
        }

        for (Map.Entry<String, List<Favorites.Fav>> entry : grouped.entrySet()) {
            String groupKey = entry.getKey();
            boolean open = host.groupOpen(groupKey);
            Label groupLabel = new Label(
                    (open ? "▾ " : "▸ ") + groupKey + "（" + entry.getValue().size() + "）");
            groupLabel.setStyle("-fx-text-fill: -app-text-dim-2; -fx-font-size: 11px;"
                    + " -fx-font-weight: bold; -fx-padding: 8 0 2 0; -fx-cursor: hand;");
            groupLabel.setOnMouseClicked(e -> {
                host.setGroupOpen(groupKey, !open);
                host.refresh();
            });
            VBox groupBody = new VBox(8.0);
            groupBody.setVisible(open);
            groupBody.setManaged(open);
            container.getChildren().addAll(groupLabel, groupBody);

            for (Favorites.Fav fav : entry.getValue()) {
                groupBody.getChildren().add(row(fav));
            }
        }
    }

    private HBox row(Favorites.Fav fav) {
        HBox row = new HBox(12.0);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10.0, 14.0, 10.0, 14.0));
        row.getStyleClass().add("instance-row");

        Label icon = new Label(ProjectFormat.typeLabel(fav.type()).substring(0, 1));
        icon.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;"
                + " -fx-background-color: -app-text-dim-2; -fx-background-radius: 8;"
                + " -fx-padding: 7 9;");

        VBox info = new VBox(2.0);
        Label name = new Label(fav.title());
        name.setStyle("-fx-text-fill: -app-text; -fx-font-size: 13px;");
        Label sub = new Label(ProjectFormat.typeLabel(fav.type())
                + " · ⬇ " + ProjectFormat.count(fav.downloads()) + " · Modrinth");
        sub.setStyle("-fx-text-fill: -app-text-dim-3; -fx-font-size: 11px;");
        info.getChildren().addAll(name, sub);
        HBox.setHgrow(info, Priority.ALWAYS);

        Button download = new Button("下载");
        download.setStyle("-fx-background-color: #9ca3af; -fx-text-fill: #111827;"
                + " -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 6 16;"
                + " -fx-cursor: hand;");
        download.setMinWidth(Double.NEGATIVE_INFINITY);
        download.setOnAction(e -> host.download(fav));

        Button remove = new Button();
        remove.setGraphic(Icons.node("star", 16.0, "#eab308"));
        remove.setStyle("-fx-background-color: transparent; -fx-text-fill: #eab308;"
                + " -fx-font-size: 17px; -fx-cursor: hand; -fx-padding: 2 6;");
        remove.setOnAction(e -> {
            Favorites.remove(fav.id());
            host.refresh();
            Toast.info("已取消收藏 " + fav.title());
        });

        row.getChildren().addAll(icon, info, remove, download);
        return row;
    }
}
