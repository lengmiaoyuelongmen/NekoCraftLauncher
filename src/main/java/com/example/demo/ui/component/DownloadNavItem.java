package com.example.demo.ui.component;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import com.example.demo.ui.shell.Motion;
import com.example.demo.ui.shell.Theme;
/**
 * 下载页左侧导航的一项：方块图标 + 文案，负责 idle / hover / active 三种状态。
 *
 * <p>从 HelloApplication 的 createDownloadNavItem 抽出，避免外部再用 {@code ((Label) ((HBox)
 * n).getChildren().get(1))} 这种按索引强转的写法。
 */
public final class DownloadNavItem extends HBox {

  private static final String IDLE_ITEM = "-fx-background-color: transparent;";
  private static final String ACTIVE_ITEM =
      "-fx-background-color: -app-hover-2; -fx-background-radius: 0 8 8 0;"
          + " -fx-background-insets: 2 8 2 0;";
  private static final String HOVER_ITEM = "-fx-background-color: -app-hover-2;";

  private final Label icon = new Label("■");
  private final Label label;
  private boolean active;

  public DownloadNavItem(String text) {
    super(12.0);
    setAlignment(Pos.CENTER_LEFT);
    setPadding(new Insets(12.0, 25.0, 12.0, 25.0));
    setCursor(Cursor.HAND);
    setMaxWidth(Double.MAX_VALUE);
    label = new Label(text);
    label.setMaxWidth(Double.MAX_VALUE);
    HBox.setHgrow(label, Priority.ALWAYS);
    getChildren().addAll(icon, label);
    setActive(false);

    setOnMouseEntered(
        e -> {
          if (!active) {
            setStyle(HOVER_ITEM);
            slide(4.0);
          }
        });
    setOnMouseExited(
        e -> {
          if (!active) {
            setStyle(IDLE_ITEM);
            slide(0.0);
          }
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

  /** 选中状态：白色加粗文案 + 高亮底。 */
  public void setActive(boolean active) {
    this.active = active;
    label.setStyle(
        active
            ? "-fx-text-fill: "
                + (Theme.isLight() ? "#111827" : "#ffffff")
                + "; -fx-font-size: 15px; -fx-font-weight: bold;"
            : "-fx-text-fill: #999; -fx-font-size: 15px;");
    icon.setStyle(
        active
            ? "-fx-text-fill: -app-text-2; -fx-font-size: 10px;"
            : "-fx-text-fill: #444; -fx-font-size: 10px;");
    setStyle(active ? ACTIVE_ITEM : IDLE_ITEM);
  }

  public boolean isActive() {
    return active;
  }

  public Label labelNode() {
    return label;
  }

  public Label iconNode() {
    return icon;
  }
}
