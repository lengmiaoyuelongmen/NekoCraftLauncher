package com.example.demo;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * 下载页「世界」分类的提示面板：说明 Modrinth 没有世界分类，并给出 CurseForge / 存档目录入口。
 *
 * <p>从 HelloApplication 抽出，两个跳转动作以回调传入。
 */
public final class WorldsNoticePanel extends VBox {

  public WorldsNoticePanel(Runnable onOpenCurseForge, Runnable onOpenSaves) {
    super(14.0);
    setPadding(new Insets(4.0));
    setMaxWidth(760.0);

    Label title = new Label("世界");
    title.setStyle("-fx-text-fill: -app-text; -fx-font-size: 20px; -fx-font-weight: bold;");
    Label hint = new Label("Modrinth 的搜索接口没有「世界」分类，地图 / 存档需要从 CurseForge" + " 或社区下载后放进 saves 目录。");
    hint.setStyle("-fx-text-fill: -app-text-dim-2; -fx-font-size: 13px;");
    hint.setWrapText(true);

    Button curse = new Button("打开 CurseForge 世界分类");
    curse.setStyle(
        "-fx-background-color: #9ca3af; -fx-text-fill: #111827; -fx-font-weight: bold;"
            + " -fx-background-radius: 9; -fx-padding: 8 18; -fx-cursor: hand;");
    if (onOpenCurseForge != null) {
      curse.setOnAction(e -> onOpenCurseForge.run());
    }
    Button saves = new Button("打开存档目录");
    saves.setStyle(
        "-fx-background-color: -app-border-4; -fx-text-fill: -app-text-2;"
            + " -fx-background-radius: 9; -fx-padding: 8 18; -fx-cursor: hand;");
    if (onOpenSaves != null) {
      saves.setOnAction(e -> onOpenSaves.run());
    }
    HBox actions = new HBox(10.0, curse, saves);

    getChildren().addAll(title, hint, actions);
  }
}
