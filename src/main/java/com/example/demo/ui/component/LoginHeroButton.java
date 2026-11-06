package com.example.demo.ui.component;

import java.io.InputStream;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

import com.example.demo.ui.shell.Motion;
/**
 * 账户卡里的「登录」大按钮：绿色横幅图片 + 叠加文字（微软授权时文案会变）。
 *
 * <p>从 HelloApplication 抽出，主类通过 {@link #label()} 更新文案。
 */
public final class LoginHeroButton extends Button {

  private final Label label = new Label("登录");

  public LoginHeroButton() {
    label.setTranslateY(3.0);
    label.setStyle(
        "-fx-text-fill: #6fae65; -fx-font-weight: bold; -fx-font-size: 24px;"
            + " -fx-font-family: 'Microsoft YaHei UI', 'Segoe UI'; -fx-effect:"
            + " dropshadow(gaussian, rgba(255,255,255,0.65), 3, 0.2, 0, 1);");

    ImageView banner = new ImageView();
    try (InputStream in = LoginHeroButton.class.getResourceAsStream("/login-button.png")) {
      if (in != null) {
        banner.setImage(new Image(in));
      }
    } catch (Exception ignored) {
      // 图片缺失时只显示文字，不影响登录
    }
    banner.setFitWidth(280.0);
    banner.setPreserveRatio(true);
    banner.setSmooth(true);

    StackPane graphic = new StackPane(banner, label);
    graphic.setAlignment(Pos.CENTER);
    setGraphic(graphic);
    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
    setDefaultButton(true);
    setMaxWidth(Double.MAX_VALUE);
    setStyle("-fx-background-color: transparent; -fx-padding: 0; -fx-cursor: hand;");
    Motion.press(this);
    setOnMouseEntered(e -> setOpacity(0.88));
    setOnMouseExited(e -> setOpacity(1.0));
  }

  /** 按钮上的文字标签（「登录」/「开始微软授权」）。 */
  public Label label() {
    return label;
  }
}
