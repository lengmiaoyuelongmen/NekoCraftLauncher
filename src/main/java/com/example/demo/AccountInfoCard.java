package com.example.demo;

import java.util.Locale;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;

import com.example.demo.core.model.Playtime;
import com.example.demo.core.app.AppVersion;
/**
 * 账户卡里的「账号信息」面板：类型 / UUID / 验证服务器 / 加载器 / 游戏版本 / 当前实例 / 游戏时长。
 *
 * <p>从 HelloApplication 抽出的独立组件，主类只需要调用几个 update 方法。
 */
public final class AccountInfoCard extends VBox {

  private final Label typeValue = new Label("—");
  private final Label uuidValue = new Label("—");
  private final Label serverValue = new Label("—");
  private final Label instanceValue = new Label("—");
  private final Label versionValue = new Label("—");
  private final Label loaderValue = new Label("—");
  private final Label playtimeValue = new Label("—");

  public AccountInfoCard() {
    super(7.0);
    setPadding(new Insets(12.0));
    setStyle(
        "-fx-background-color: rgba(255,255,255,0.028); -fx-background-radius: 10;"
            + " -fx-border-color: rgba(255,255,255,0.05); -fx-border-radius: 10;");

    GridPane grid = new GridPane();
    grid.setHgap(18.0);
    grid.setVgap(7.0);
    ColumnConstraints half1 = new ColumnConstraints();
    ColumnConstraints half2 = new ColumnConstraints();
    half1.setPercentWidth(50.0);
    half2.setPercentWidth(50.0);
    grid.getColumnConstraints().addAll(half1, half2);
    for (int i = 0; i < 5; i++) {
      RowConstraints row = new RowConstraints();
      row.setPercentHeight(20.0);
      grid.getRowConstraints().add(row);
    }
    grid.add(infoLine("类型", typeValue), 0, 0);
    grid.add(infoLine("UUID", uuidValue), 1, 0);
    grid.add(infoLine("验证服务器", serverValue), 0, 1);
    grid.add(infoLine("加载器", loaderValue), 1, 1);
    grid.add(infoLine("游戏版本", versionValue), 0, 2);
    grid.add(infoLine("启动器", new Label(AppVersion.CURRENT)), 1, 2);
    HBox instanceRow = infoLine("当前实例", instanceValue);
    GridPane.setColumnSpan(instanceRow, 2);
    grid.add(instanceRow, 0, 3);
    HBox playtimeRow = infoLine("游戏时长", playtimeValue);
    GridPane.setColumnSpan(playtimeRow, 2);
    grid.add(playtimeRow, 0, 4);
    grid.setMaxHeight(Double.MAX_VALUE);
    VBox.setVgrow(grid, Priority.ALWAYS);

    getChildren().addAll(caption("账号信息"), grid);
    setMaxWidth(Double.MAX_VALUE);
    setPrefHeight(176.0);
    setMinHeight(120.0);
    setMaxHeight(Double.MAX_VALUE);
    setVisible(false);
    setManaged(false);
  }

  /** 更新账户基本信息。 */
  public void updateAccount(String type, String uuid, String server) {
    typeValue.setText(type == null || type.isBlank() ? "玩家" : type);
    uuidValue.setText(uuid == null || uuid.isBlank() ? "—" : uuid);
    String host = server == null ? "" : server.trim();
    serverValue.setText(host.isBlank() ? "官方 / 离线" : host);
  }

  /** 更新当前实例：同时推算 MC 版本和加载器。 */
  public void updateInstance(String instanceId) {
    instanceValue.setText(instanceId == null || instanceId.isBlank() ? "未选择" : instanceId);
    if (instanceId == null || instanceId.isBlank()) {
      versionValue.setText("—");
      loaderValue.setText("—");
      return;
    }
    String mc = mcVersion(instanceId);
    versionValue.setText(mc == null ? "—" : mc);
    String lower = instanceId.toLowerCase(Locale.ROOT);
    loaderValue.setText(
        lower.contains("neoforge")
            ? "NeoForge"
            : lower.contains("forge")
                ? "Forge"
                : lower.contains("fabric") ? "Fabric" : lower.contains("quilt") ? "Quilt" : "原版");
  }

  public void updatePlaytime(long seconds) {
    playtimeValue.setText(Playtime.format(seconds));
  }

  private static Label caption(String text) {
    Label label = new Label(text);
    label.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 10.5px; -fx-font-weight: bold;");
    return label;
  }

  private static HBox infoLine(String key, Label value) {
    Label keyLabel = new Label(key);
    keyLabel.setMinWidth(62.0);
    keyLabel.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 10.5px;");
    value.setStyle("-fx-text-fill: -app-text-2; -fx-font-size: 11px;");
    value.setMaxWidth(Double.MAX_VALUE);
    HBox.setHgrow(value, Priority.ALWAYS);
    HBox row = new HBox(8.0, keyLabel, value);
    row.setAlignment(Pos.CENTER_LEFT);
    return row;
  }

  /** 从实例 id 里提取 MC 版本号（兼容 fabric-loader-… 与 …-forge-…）。 */
  private static String mcVersion(String id) {
    String value = id;
    int forge = value.indexOf("-forge-");
    if (forge > 0) {
      value = value.substring(0, forge);
    }
    if (value.startsWith("fabric-loader-")) {
      value = value.substring(value.lastIndexOf('-') + 1);
    }
    java.util.regex.Matcher matcher =
        java.util.regex.Pattern.compile("^(1\\.\\d+(?:\\.\\d+)?|\\d+\\.\\d+(?:\\.\\d+)?)")
            .matcher(value);
    return matcher.find() ? matcher.group(1) : null;
  }
}
