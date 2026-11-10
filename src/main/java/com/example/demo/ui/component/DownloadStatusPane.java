package com.example.demo.ui.component;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import com.example.demo.ui.shell.Icons;
import com.example.demo.ui.shell.Theme;

/**
 * 下载页列表上的状态浮层：加载中转圈 / 文字提示（可点击重试）。
 *
 * <p>加载时背后铺几行骨架行并做呼吸动画，避免整块空白列表看起来像卡死。
 */
public final class DownloadStatusPane extends StackPane {

  private final VBox box = new VBox(15.0);
  private final ProgressIndicator spinner = new ProgressIndicator();
  private final StackPane iconBox = new StackPane();
  private final Label label = new Label("正在加载...");
  private final VBox skeletonBox = new VBox(10.0);
  private final Timeline breathe;
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

    // 骨架行：只是占位，不接收鼠标
    skeletonBox.setMouseTransparent(true);
    skeletonBox.setPadding(new Insets(6.0, 2.0, 6.0, 2.0));
    for (int i = 0; i < 5; i++) {
      Region row = new Region();
      row.getStyleClass().add("skeleton-row");
      row.setMinHeight(72.0);
      row.setPrefHeight(72.0);
      row.setMaxWidth(Double.MAX_VALUE);
      VBox.setVgrow(row, Priority.NEVER);
      skeletonBox.getChildren().add(row);
    }
    skeletonBox.setVisible(false);
    skeletonBox.setManaged(false);

    getChildren().addAll(skeletonBox, box);
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

    breathe =
        new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(skeletonBox.opacityProperty(), 1.0)),
            new KeyFrame(
                Duration.millis(900),
                new KeyValue(skeletonBox.opacityProperty(), 0.55, Interpolator.EASE_BOTH)),
            new KeyFrame(
                Duration.millis(1800),
                new KeyValue(skeletonBox.opacityProperty(), 1.0, Interpolator.EASE_BOTH)));
    breathe.setCycleCount(Timeline.INDEFINITE);
  }

  /** 显示转圈 + 文案；背后铺骨架行并开始呼吸动画。 */
  public void showLoading(String text) {
    box.getChildren().setAll(spinner, label);
    label.setText(text);
    skeletonBox.setVisible(true);
    skeletonBox.setManaged(true);
    setVisible(true);
    if (breathe.getStatus() != javafx.animation.Animation.Status.RUNNING) {
      breathe.play();
    }
  }

  /** 只显示图标 + 文案（失败/空结果时用），可配合 {@link #setRetry(Runnable)} 点击重试。 */
  public void showText(String text) {
    stopSkeleton();
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
    stopSkeleton();
    setVisible(false);
    retry = null;
  }

  private void stopSkeleton() {
    breathe.stop();
    skeletonBox.setVisible(false);
    skeletonBox.setManaged(false);
  }
}
