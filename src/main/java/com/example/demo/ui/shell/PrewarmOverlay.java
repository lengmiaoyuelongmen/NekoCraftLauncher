package com.example.demo.ui.shell;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 * 下载页首次打开时的「正在预渲染社区资源…」遮罩：进度条 + 百分比文案。
 *
 * <p>进度条用 450ms 线性补间往目标值追，避免 20% 一跳的僵硬感；
 * 文案跟进度条同步刷新，不会出现「条走字不走」。条上还有一道循环扫光，
 * 让"停在同一百分比等待"时也能看出仍在工作。
 */
public final class PrewarmOverlay extends StackPane {

  private static final Duration EASE = Duration.millis(450);
  private static final double BAR_WIDTH = 300.0;

  private final Label label = new Label("正在预渲染社区资源… 0%");
  private final ProgressBar bar = new ProgressBar(0.0);
  private final Rectangle shine = new Rectangle(64, 7);
  private Timeline tween;
  private Timeline shimmer;
  private double target = 0;

  public PrewarmOverlay() {
    bar.setPrefWidth(BAR_WIDTH);
    bar.setPrefHeight(7.0);
    bar.getStyleClass().add("prewarm-bar");

    shine.setArcWidth(7);
    shine.setArcHeight(7);
    shine.setFill(javafx.scene.paint.Color.rgb(255, 255, 255, 0.38));
    shine.setMouseTransparent(true);
    StackPane barWrap = new StackPane(bar, shine);
    barWrap.setAlignment(Pos.CENTER_LEFT);
    barWrap.setMaxWidth(BAR_WIDTH);
    barWrap.setPrefWidth(BAR_WIDTH);
    // 扫光不能超出进度条本身
    Rectangle clip = new Rectangle(BAR_WIDTH, 7);
    clip.setArcWidth(7);
    clip.setArcHeight(7);
    barWrap.setClip(clip);

    label.setStyle("-fx-text-fill: -app-text-2; -fx-font-size: 12px;");
    VBox box = new VBox(10.0, label, barWrap);
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
    startShimmer();
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
          stopShimmer();
          setVisible(false);
          setManaged(false);
        });
    wait.play();
  }

  /** 条上循环扫光：即使百分比停住，也能看出还在加载。 */
  private void startShimmer() {
    stopShimmer();
    shine.setTranslateX(-64);
    shimmer =
        new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(shine.translateXProperty(), -64)),
            new KeyFrame(
                Duration.millis(1100),
                new KeyValue(shine.translateXProperty(), BAR_WIDTH, Interpolator.LINEAR)));
    shimmer.setCycleCount(Timeline.INDEFINITE);
    shimmer.play();
  }

  private void stopShimmer() {
    if (shimmer != null) {
      shimmer.stop();
      shimmer = null;
    }
  }

  private void stopTween() {
    if (tween != null) {
      tween.stop();
      tween = null;
    }
  }
}
