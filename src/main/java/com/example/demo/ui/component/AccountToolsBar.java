package com.example.demo.ui.component;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;

import com.example.demo.ui.shell.Motion;
/**
 * 账户卡里头像下方那排小按钮：皮肤 / 披风预览、账户管理、一键体检。
 *
 * <p>从 HelloApplication 抽出，动作通过回调传入。
 */
public final class AccountToolsBar extends HBox {

  private static final String BUTTON_STYLE =
      "-fx-background-color: transparent; -fx-text-fill: -app-text-muted;"
          + " -fx-font-size: 10.5px; -fx-border-color: -app-border-soft;"
          + " -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 3 10;"
          + " -fx-cursor: hand;";

  public AccountToolsBar(
      Runnable onSkinPreview, Runnable onAccountManager, Runnable onHealthCheck) {
    super(6.0);
    getChildren()
        .addAll(
            button("皮肤 / 披风预览", onSkinPreview),
            button("账户管理", onAccountManager),
            button("一键体检", onHealthCheck));
    setAlignment(Pos.CENTER);
  }

  private static Button button(String text, Runnable action) {
    Button button = new Button(text);
    button.getStyleClass().add("btn-ghost");
    if (action != null) {
      button.setOnAction(e -> action.run());
    }
    Motion.press(button);
    return button;
  }
}
