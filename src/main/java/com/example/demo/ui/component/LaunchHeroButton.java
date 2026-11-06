package com.example.demo.ui.component;

import java.io.InputStream;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.value.ObservableNumberValue;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import com.example.demo.ui.shell.Motion;
/**
 * 启动页中间那张「猫咪胶囊」启动按钮：图片背景 + 居中的「★ 启动游戏」+ 实例名。
 *
 * <p>从 HelloApplication 抽出的独立组件；宽度跟随账户卡宽度（最大 300），点击由主类绑定。
 */
public final class LaunchHeroButton extends Button {

  private boolean launching;
  private final Label titleLabel = new Label("★ 启动游戏");
  private final Label versionLabel = new Label("");

  public LaunchHeroButton(Region widthSource) {
    DoubleBinding bannerWidth =
        Bindings.min(300.0, (ObservableNumberValue) widthSource.widthProperty().subtract(32));

    ImageView art = new ImageView();
    try (InputStream in = LaunchHeroButton.class.getResourceAsStream("/launch-neko.png")) {
      if (in != null) {
        art.setImage(new Image(in));
      }
    } catch (Exception ignored) {
      // 图片缺失时保留空背景，不影响点击
    }
    art.setFitWidth(300.0);
    art.setPreserveRatio(true);
    art.setSmooth(true);
    art.fitWidthProperty().bind(bannerWidth);

    Label title = titleLabel;
    title.setStyle(
        "-fx-text-fill: #5f9e4f; -fx-font-size: 21px; -fx-font-weight: bold;"
            + " -fx-font-family: 'Microsoft YaHei UI', 'Segoe UI';");
    versionLabel.setMaxWidth(Double.MAX_VALUE);
    versionLabel.setAlignment(Pos.CENTER);
    versionLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
    versionLabel.setStyle(
        "-fx-text-fill: #6b8f60; -fx-font-size: 11px;"
            + " -fx-font-family: 'Microsoft YaHei UI', 'Segoe UI';");

    VBox text = new VBox(2.0, title, versionLabel);
    text.setAlignment(Pos.CENTER);
    text.setMouseTransparent(true);
    text.maxWidthProperty().bind(bannerWidth.multiply(0.52));
    text.prefWidthProperty().bind(bannerWidth.multiply(0.52));

    StackPane banner = new StackPane(art, text);
    StackPane.setAlignment(text, Pos.CENTER_LEFT);
    text.translateXProperty().bind(bannerWidth.multiply(0.09));
    // 胶囊上沿有猫耳装饰，文字整体略偏下更像居中
    text.translateYProperty().bind(bannerWidth.multiply(0.045));
    banner.setMaxWidth(300.0);

    setGraphic(banner);
    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
    setMaxWidth(Double.MAX_VALUE);
    setPrefHeight(112.0);
    setMinHeight(112.0);
    setStyle(
        "-fx-background-color: transparent; -fx-padding: 0; -fx-cursor: hand;"
            + " -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
    setEffect(new DropShadow(18.0, javafx.scene.paint.Color.web("#7fb56d", 0.26)));
    Motion.press(this);
    Motion.hoverScale(this, 1.014);
  }

  /** 启动/运行中把主按钮切成「正在运行」的只读状态，停止操作放在下面的小按钮上。 */
  public void setLaunching(boolean launching) {
    this.launching = launching;
    titleLabel.setText(launching ? "● 正在运行" : "★ 启动游戏");
    titleLabel.setStyle(
        launching
            ? "-fx-text-fill: #2f7d6a; -fx-font-size: 21px; -fx-font-weight: bold;"
                + " -fx-font-family: 'Microsoft YaHei UI', 'Segoe UI';"
            : "-fx-text-fill: #5f9e4f; -fx-font-size: 21px; -fx-font-weight: bold;"
                + " -fx-font-family: 'Microsoft YaHei UI', 'Segoe UI';");
    setCursor(launching ? javafx.scene.Cursor.DEFAULT : javafx.scene.Cursor.HAND);
  }

  /** 是否处于启动/运行中状态。 */
  public boolean isLaunching() {
    return launching;
  }

  /** 启动按钮下方的实例名标签，主类切换实例时更新它。 */
  public Label versionLabel() {
    return versionLabel;
  }
}
