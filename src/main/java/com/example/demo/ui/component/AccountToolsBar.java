package com.example.demo.ui.component;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;

import com.example.demo.ui.shell.Icons;
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
            button("皮肤 / 披风预览", "image", "skin", onSkinPreview),
            button("账户管理", "user", "account", onAccountManager),
            button("一键体检", "check", "health", onHealthCheck));
    setAlignment(Pos.CENTER);
  }

  /** 符号化按钮：左边一个图标，右边保留文字（无障碍与 UIA 仍能按名字找到）。 */
  private static Button button(String text, String icon, String key, Runnable action) {
    Button button = new Button(text);
    button.setGraphic(Icons.node(icon, 12.0, "#9aa3ad"));
    button.getStyleClass().add("btn-ghost");
    button.setGraphicTextGap(6.0);
    button.setId("account-tool-" + key);
    button.setTooltip(new javafx.scene.control.Tooltip(text));
    if (action != null) {
      button.setOnAction(e -> action.run());
    }
    Motion.press(button);
    Motion.hoverScale(button, 1.04);
    return button;
  }
}
