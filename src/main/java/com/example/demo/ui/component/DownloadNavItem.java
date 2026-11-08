package com.example.demo.ui.component;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import com.example.demo.ui.shell.Icons;
import com.example.demo.ui.shell.Motion;
import com.example.demo.ui.shell.Theme;

/**
 * 下载页左侧导航的一项：符号图标 + 文案，负责 idle / hover / active 三种状态。
 *
 * <p>符号化：每个分类用各自的矢量图标（模组=layers、整合包=package、光影=sparkles…），
 * 悬停时图标轻微放大、文字右移，选中时高亮 + 指示条（由 DownloadPage 负责）。
 */
public final class DownloadNavItem extends HBox {

  private static final String IDLE_ITEM = "-fx-background-color: transparent;";
  private static final String ACTIVE_ITEM =
      "-fx-background-color: -app-hover-2; -fx-background-radius: 0 8 8 0;"
          + " -fx-background-insets: 2 8 2 0;";
  private static final String HOVER_ITEM = "-fx-background-color: -app-hover-2;";

  private final Node iconNode;
  private final Label label;
  private boolean active;

  public DownloadNavItem(String text) {
    this(text, "dot");
  }

  public DownloadNavItem(String text, String iconKey) {
    super(10.0);
    setAlignment(Pos.CENTER_LEFT);
    setPadding(new Insets(12.0, 25.0, 12.0, 25.0));
    setCursor(Cursor.HAND);
    setMaxWidth(Double.MAX_VALUE);
    label = new Label(text);
    label.setMaxWidth(Double.MAX_VALUE);
    HBox.setHgrow(label, Priority.ALWAYS);
    iconNode = Icons.node(iconKey == null ? "dot" : iconKey, 14.0, "#9aa3ad");
    iconNode.setOpacity(0.72);
    getChildren().addAll(iconNode, label);
    setActive(false);

    setOnMouseEntered(
        e -> {
          if (!active) {
            setStyle(HOVER_ITEM);
            slide(4.0);
          }
          scaleIcon(1.18);
        });
    setOnMouseExited(
        e -> {
          if (!active) {
            setStyle(IDLE_ITEM);
            slide(0.0);
          }
          scaleIcon(1.0);
        });
    Motion.press(this);
  }

  /** 文案轻微右移的悬停反馈；关闭动画时直接跳到目标位置。 */
  private void slide(double dx) {
    if (!Motion.isEnabled()) {
      label.setTranslateX(dx);
      return;
    }
    javafx.animation.TranslateTransition transition =
        new javafx.animation.TranslateTransition(Motion.FAST, label);
    transition.setToX(dx);
    transition.setInterpolator(Motion.OUT);
    transition.play();
  }

  /** 图标缩放的悬停反馈：比整体放大更克制，也更容易看清图标。 */
  private void scaleIcon(double scale) {
    if (!Motion.isEnabled()) {
      iconNode.setScaleX(scale);
      iconNode.setScaleY(scale);
      return;
    }
    javafx.animation.ScaleTransition transition =
        new javafx.animation.ScaleTransition(Motion.FAST, iconNode);
    transition.setToX(scale);
    transition.setToY(scale);
    transition.setInterpolator(Motion.OUT);
    transition.play();
  }

  /** 选中状态：白色加粗文案 + 高亮底，图标提亮。 */
  public void setActive(boolean active) {
    this.active = active;
    label.setStyle(
        active
            ? "-fx-text-fill: "
                + (Theme.isLight() ? "#111827" : "#ffffff")
                + "; -fx-font-size: 15px; -fx-font-weight: bold;"
            : "-fx-text-fill: #999; -fx-font-size: 15px;");
    iconNode.setOpacity(active ? 1.0 : 0.72);
    setStyle(active ? ACTIVE_ITEM : IDLE_ITEM);
  }

  public boolean isActive() {
    return active;
  }

  public Label labelNode() {
    return label;
  }

  public Node iconNode() {
    return iconNode;
  }
}
