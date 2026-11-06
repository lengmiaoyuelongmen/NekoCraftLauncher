package com.example.demo.ui.component;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import com.example.demo.ui.shell.Icons;
import com.example.demo.ui.shell.Theme;
/**
 * 下载页列表上的状态浮层：加载中转圈 / 文字提示（可点击重试）。
 *
 * <p>从 HelloApplication 抽出的独立组件，主类只调用 showLoading / showText / hide。
 */
public final class DownloadStatusPane extends StackPane {

  private final VBox box = new VBox(15.0);
  private final ProgressIndicator spinner = new ProgressIndicator();
  private final StackPane iconBox = new StackPane();
  private final Label label = new Label("正在加载...");
  private Runnable retry;

  public DownloadStatusPane() {
    spinner.setMaxSize(60.0, 60.0);
    iconBox.setPrefSize(44.0, 44.0);
    iconBox.setMinSize(44.0, 44.0);
    iconBox.setMaxSize(44.0, 44.0);
    label.setStyle(
        "-fx-text-fill: "
            + (Theme.isLight() ? "#0f172a" : "#e5e7eb")
            + "; -fx-font-size: 14px; -fx-font-weight: bold;");
    box.setAlignment(Pos.CENTER);
    box.getChildren().addAll(spinner, label);
    getChildren().add(box);
    setStyle(
        "-fx-background-color: "
            + (Theme.isLight() ? "rgba(234,244,255,0.34)" : "rgba(7,18,18,0.34)")
            + "; -fx-background-radius: 16;");
    setVisible(false);
    setOnMouseClicked(
        e -> {
          if (retry != null) {
            retry.run();
          }
        });
  }

  /** 显示转圈 + 文案。 */
  public void showLoading(String text) {
    box.getChildren().setAll(spinner, label);
    label.setText(text);
    setVisible(true);
  }

  /** 只显示图标 + 文案（失败/空结果时用），可配合 {@link #setRetry(Runnable)} 点击重试。 */
  public void showText(String text) {
    iconBox
        .getChildren()
        .setAll(
            Icons.node(
                iconFor(text),
                30.0,
                Theme.isLight() ? "#94a3b8" : "#7b8494"));
    box.getChildren().setAll(iconBox, label);
    label.setText(text);
    setVisible(true);
  }

  /** 失败类文案用警示图标，其余（空结果 / 没有更多）用搜索图标。 */
  private static String iconFor(String text) {
    if (text == null) {
      return "search";
    }
    if (text.contains("失败") || text.contains("错误") || text.contains("超时")) {
      return "warning";
    }
    return "search";
  }

  /** 只改文案，不改显示状态。 */
  public void setText(String text) {
    label.setText(text);
  }

  /** 点击浮层时执行的重试动作。 */
  public void setRetry(Runnable action) {
    this.retry = action;
  }

  public void hide() {
    setVisible(false);
    retry = null;
  }
}
