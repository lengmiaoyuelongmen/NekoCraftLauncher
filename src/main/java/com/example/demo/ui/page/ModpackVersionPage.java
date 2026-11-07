package com.example.demo.ui.page;

import com.example.demo.ui.shell.Dialogs;
import com.example.demo.ui.shell.InAppDialog;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Locale;
import java.util.function.IntConsumer;

/**
 * 整合包版本选择页：左侧版本列表（带类型徽章）+ 右侧详情/更新日志，底部返回/下载。
 *
 * <p>从主类的 chooseModpackVersion 搬出；页面挂载、返回与对话框宿主由 {@link Host} 提供。
 */
public final class ModpackVersionPage {

    public interface Host {
        /** 下载区容器（用于清掉上一个版本页）。 */
        javafx.scene.layout.StackPane contentArea();

        /** 给确认对话框用的宿主节点。 */
        javafx.scene.layout.StackPane dialogOwner();

        /** 把页面显示到下载区。 */
        void showPage(Node page);

        /** 回到社区列表。 */
        void backToCommunity();
    }

    private final Host host;

    public ModpackVersionPage(Host host) {
        this.host = host;
    }

    /** 打开版本选择页；用户选中后通过 {@code onChosen} 回传索引（-1 表示取消）。 */
    public void show(String title, JsonArray versions, IntConsumer onChosen) {
  int limit = Math.min(60, versions.size());
  ArrayList<String> labels = new ArrayList<>();
  ArrayList<String> numbers = new ArrayList<>();
  ArrayList<String> types = new ArrayList<>();
  ArrayList<String> gamesList = new ArrayList<>();
  ArrayList<String> dates = new ArrayList<>();
  ArrayList<Long> downloadsList = new ArrayList<>();
  ArrayList<String> changelogs = new ArrayList<>();
  for (int i = 0; i < limit; ++i) {
    JsonObject version = versions.get(i).getAsJsonObject();
    String number =
        version.has("version_number") ? version.get("version_number").getAsString() : "?";
    String type = version.has("version_type") ? version.get("version_type").getAsString() : "";
    String games = "";
    if (version.has("game_versions") && version.get("game_versions").isJsonArray()) {
      JsonArray gameArray = version.getAsJsonArray("game_versions");
      ArrayList<String> parts = new ArrayList<>();
      for (int j = 0; j < Math.min(4, gameArray.size()); ++j) {
        parts.add(gameArray.get(j).getAsString());
      }
      games = String.join(", ", parts);
    }
    String date = version.has("date_published") ? version.get("date_published").getAsString() : "";
    if (date.length() > 10) date = date.substring(0, 10);
    long downloads = version.has("downloads") ? version.get("downloads").getAsLong() : 0L;
    String changelog = version.has("changelog") ? version.get("changelog").getAsString() : "";
    numbers.add(number);
    types.add(type);
    gamesList.add(games);
    dates.add(date);
    downloadsList.add(downloads);
    changelogs.add(changelog);
    labels.add(number + "   ·   " + (type.isBlank() ? "release" : type) + (date.isBlank() ? "" : "   ·   " + date));
  }
  if (labels.isEmpty()) {
    onChosen.accept(-1);
    return;
  }

  javafx.scene.control.ListView<Integer> list = new javafx.scene.control.ListView<>();
  for (int i = 0; i < labels.size(); i++) {
    list.getItems().add(i);
  }
  list.getSelectionModel().selectFirst();
  // 版本行：类型徽章带颜色，悬停高亮由 CSS 的 .version-row:hover 负责
  list.setCellFactory(
      view ->
          new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(Integer index, boolean empty) {
              super.updateItem(index, empty);
              if (empty || index == null) {
                setText(null);
                setGraphic(null);
                return;
              }
              String type = types.isEmpty() ? "" : types.get(Math.min(index, types.size() - 1));
              String badgeText = type.isBlank() ? "release" : type;
              String badgeColor =
                  switch (badgeText) {
                    case "release" -> "#22c55e";
                    case "beta" -> "#f59e0b";
                    case "alpha" -> "#ef4444";
                    default -> "#94a3b8";
                  };
              javafx.scene.control.Label badge = new javafx.scene.control.Label(badgeText);
              badge.setStyle(
                  "-fx-text-fill: " + badgeColor + "; -fx-font-size: 10px; -fx-font-weight: bold;"
                      + " -fx-background-color: " + badgeColor + "22; -fx-background-radius: 6;"
                      + " -fx-padding: 1 6;");
              javafx.scene.control.Label text =
                  new javafx.scene.control.Label(numbers.get(index));
              text.setStyle("-fx-text-fill: -app-text; -fx-font-size: 12.5px;");
              javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
              javafx.scene.layout.HBox.setHgrow(spacer, Priority.ALWAYS);
              javafx.scene.control.Label date =
                  new javafx.scene.control.Label(
                      dates.get(index).isBlank() ? "" : dates.get(index));
              date.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11px;");
              javafx.scene.control.Label games =
                  new javafx.scene.control.Label(
                      gamesList.isEmpty() || index >= gamesList.size()
                          ? ""
                          : gamesList.get(index));
              games.setStyle("-fx-text-fill: -app-text-dim; -fx-font-size: 10.5px;");
              javafx.scene.layout.VBox texts =
                  new javafx.scene.layout.VBox(1, text, games);
              javafx.scene.layout.HBox.setHgrow(texts, Priority.ALWAYS);
              javafx.scene.layout.HBox row =
                  new javafx.scene.layout.HBox(10, badge, texts, date);
              row.setAlignment(Pos.CENTER_LEFT);
              setText(null);
              setGraphic(row);
              if (!getStyleClass().contains("version-row")) {
                getStyleClass().add("version-row");
              }
            }
          });
  list.setPrefWidth(300);
  list.setPrefHeight(240);

  Label detailTitle = new Label();
  detailTitle.setStyle("-fx-text-fill: -app-text; -fx-font-size: 13px; -fx-font-weight: bold;");
  Label detailMeta = new Label();
  detailMeta.setStyle("-fx-text-fill: -app-text-dim; -fx-font-size: 11px;");
  detailMeta.setWrapText(true);
  TextArea detailLog = new TextArea();
  detailLog.setEditable(false);
  detailLog.setWrapText(true);
  detailLog.setMinWidth(0);
  detailLog.setPrefColumnCount(48);
  detailLog.setMinHeight(0);
  detailLog.setPrefRowCount(10);
  detailLog.setStyle(
      "-fx-font-size: 12px; -fx-background-color: transparent;"
          + " -fx-control-inner-background: transparent; -fx-text-fill: -app-text-2;"
          + " -fx-background-insets: 0; -fx-padding: 0;");
  VBox.setVgrow(detailLog, Priority.ALWAYS);
  VBox detail = new VBox(8, detailTitle, detailMeta, detailLog);
  detail.setPadding(new Insets(12));
  detail.setStyle(
      "-fx-background-color: -app-card; -fx-background-radius: 16;"
          + " -fx-border-color: -app-border-soft; -fx-border-radius: 16;");

  Runnable refreshDetail =
      () -> {
        int index = list.getSelectionModel().getSelectedIndex();
        if (index < 0 || index >= labels.size()) {
          return;
        }
        String type = types.get(index);
        detailTitle.setText(numbers.get(index) + "  (" + (type.isBlank() ? "release" : type) + ")");
        detailMeta.setText(
            "游戏版本："
                + (gamesList.get(index).isBlank() ? "未标注" : gamesList.get(index))
                + "\n发布："
                + (dates.get(index).isBlank() ? "未知" : dates.get(index))
                + "   ·   下载 "
                + String.format(Locale.ROOT, "%,d", downloadsList.get(index)));
        String changelog = changelogs.get(index);
        if (changelog == null || changelog.isBlank()) {
          detailLog.setText("这个版本没有提供更新说明。");
        } else {
          String text = changelog.replaceAll("<[^>]+>", "").trim();
          if (text.length() > 1200) {
            text = text.substring(0, 1200) + "…";
          }
          detailLog.setText(text);
        }
      };
  list.getSelectionModel().selectedIndexProperty().addListener((obs, old, now) -> refreshDetail.run());
  refreshDetail.run();

  // 左：版本列表卡片，右：更新内容卡片，尺寸和圆角跟全局一致
  list.setPrefWidth(252);
  list.setMinWidth(180);
  list.setMaxWidth(252);
  list.setMinHeight(0);
  list.setFixedCellSize(46);
  list.setStyle(
      "-fx-background-color: transparent; -fx-control-inner-background: transparent;"
          + " -fx-border-color: transparent; -fx-padding: 0;");
  VBox.setVgrow(list, Priority.ALWAYS);

  VBox listCard = new VBox(list);
  listCard.setPadding(new Insets(8));
  listCard.setMinWidth(180);
  listCard.setPrefWidth(268);
  listCard.setMaxWidth(268);
  listCard.setStyle(
      "-fx-background-color: -app-card; -fx-background-radius: 16;"
          + " -fx-border-color: -app-border-soft; -fx-border-radius: 16;");
  VBox.setVgrow(listCard, Priority.ALWAYS);

  detail.setMinWidth(0);
  HBox.setHgrow(detail, Priority.ALWAYS);
  VBox.setVgrow(detail, Priority.ALWAYS);

  HBox body = new HBox(14, listCard, detail);
  body.setAlignment(Pos.TOP_LEFT);
  VBox.setVgrow(body, Priority.ALWAYS);
  // 右侧详情宽度 = body 宽 - 列表卡片 268 - 间距 14
  detail.maxWidthProperty()
      .bind(javafx.beans.binding.Bindings.max(220, body.widthProperty().subtract(282)));

  Button back = new Button("‹  返回");
  back.getStyleClass().addAll("btn-secondary", "btn-sm");
  Label heading = new Label(title);
  heading.setStyle("-fx-text-fill: -app-text; -fx-font-size: 16px; -fx-font-weight: bold;");
  Label subtitle = new Label("选择要安装的版本 · 右侧是该版本的更新内容");
  subtitle.setStyle("-fx-text-fill: -app-text-dim; -fx-font-size: 11px;");
  VBox headingBox = new VBox(2, heading, subtitle);
  Region headerSpacer = new Region();
  HBox.setHgrow(headerSpacer, Priority.ALWAYS);
  HBox header = new HBox(12, back, headingBox, headerSpacer);
  header.setAlignment(Pos.CENTER_LEFT);
  header.setPadding(new Insets(0, 0, 6, 0));

  Button download = new Button("下载此版本");
  download.getStyleClass().addAll("btn-primary", "btn-sm");
  Label tip = new Label("下载后会询问是否立即安装到当前实例");
  tip.setStyle("-fx-text-fill: -app-text-dim; -fx-font-size: 11px;");
  Region footerSpacer = new Region();
  HBox.setHgrow(footerSpacer, Priority.ALWAYS);
  HBox footer = new HBox(10, tip, footerSpacer, download);
  footer.setAlignment(Pos.CENTER_LEFT);
  footer.setPadding(new Insets(6, 0, 0, 0));

  VBox page = new VBox(10, header, body, footer);
  page.setPadding(new Insets(0));
  page.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
  page.getStyleClass().add("version-page");
  // 页面宽度跟随下载区，避免被内部长文本撑宽

  host.contentArea().getChildren().removeIf(n -> n.getStyleClass().contains("version-page"));

  Runnable goBack =
      () -> {
        host.backToCommunity();
        onChosen.accept(-1);
      };
  back.setOnAction(e -> goBack.run());

  // 选中版本后先问一句再下载，避免误触直接开始拉包
  java.util.function.IntConsumer askDownload =
      index -> {
        int safe = Math.max(0, Math.min(index, labels.size() - 1));
        String versionName = numbers.get(safe);
        InAppDialog.confirm(
            host.dialogOwner(),
            "下载 " + title + " " + versionName + "？",
            "游戏版本："
                + (gamesList.get(safe).isBlank() ? "未标注" : gamesList.get(safe))
                + "\n确认后开始获取下载地址。",
            "开始下载",
            () -> {
              host.backToCommunity();
              onChosen.accept(safe);
            });
      };
  download.setOnAction(e -> askDownload.accept(list.getSelectionModel().getSelectedIndex()));
  list.setOnMouseClicked(
      e -> {
        if (e.getClickCount() == 2) {
          askDownload.accept(list.getSelectionModel().getSelectedIndex());
        }
      });
  list.setOnKeyPressed(
      e -> {
        if (e.getCode() == javafx.scene.input.KeyCode.ENTER) {
          askDownload.accept(list.getSelectionModel().getSelectedIndex());
        }
      });
  host.showPage(page);
    }
}
