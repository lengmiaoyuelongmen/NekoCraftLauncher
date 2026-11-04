package com.example.demo;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * 下载页首次打开时的「正在预渲染社区资源…」遮罩：进度条 + 百分比文案。
 *
 * <p>进度条用 450ms 线性补间往目标值追，避免 20% 一跳的僵硬感；
 * 文案跟进度条同步刷新，不会出现「条走字不走」。
 */
public final class PrewarmOverlay extends StackPane {

  private static final Duration EASE = Duration.millis(450);

  private final Label label = new Label("正在预渲染社区资源… 0%");
  private final ProgressBar bar = new ProgressBar(0.0);
  private Timeline tween;
  private double target = 0;

  public PrewarmOverlay() {
    bar.setPrefWidth(300.0);
    bar.setPrefHeight(6.0);
    bar.setStyle("-fx-accent: -app-accent;");
    label.setStyle("-fx-text-fill: -app-text-2; -fx-font-size: 12px;");
    VBox box = new VBox(10.0, label, bar);
    box.setAlignment(Pos.CENTER);
    box.setStyle(
        "-fx-background-color: -app-card; -fx-background-radius: 14; -fx-border-color:"
            + " -app-border-soft; -fx-border-radius: 14; -fx-padding: 18 24;");
    getChildren().add(box);
    setStyle("-fx-background-color: rgba(0,0,0,0.12); -fx-background-radius: 16;");
    // 预渲染只是提示，不能吃掉列表上的点击（否则用户点卡片没反应）
    setMouseTransparent(true);
    setVisible(false);
    setManaged(false);
    bar.progressProperty()
        .addListener(
            (obs, old, now) -> {
              int percent = (int) Math.round(now.doubleValue() * 100);
              label.setText(
                  percent >= 100 ? "预渲染完成" : "正在预渲染社区资源… " + percent + "%");
            });
  }

  /** 开始预渲染：显示遮罩并清零进度。 */
  public void show() {
    stopTween();
    bar.setProgress(0.0);
    target = 0;
    label.setText("正在预渲染社区资源… 0%");
    setVisible(true);
    setManaged(true);
  }

  /** 更新百分比（0-100）：用线性补间追到目标值。 */
  public void setProgress(int percent) {
    double clamped = Math.max(0, Math.min(100, percent)) / 100.0;
    if (clamped <= target) {
      return;
    }
    target = clamped;
    stopTween();
    tween =
        new Timeline(
            new KeyFrame(
                EASE,
                new KeyValue(bar.progressProperty(), target, Interpolator.LINEAR)));
    tween.play();
  }

  /** 预渲染结束：进度推到 100% 后再隐藏。 */
  public void hide() {
    setProgress(100);
    javafx.animation.PauseTransition wait =
        new javafx.animation.PauseTransition(Duration.millis(520));
    wait.setOnFinished(
        e -> {
          setVisible(false);
          setManaged(false);
        });
    wait.play();
  }

  private void stopTween() {
    if (tween != null) {
      tween.stop();
      tween = null;
    }
  }

}
