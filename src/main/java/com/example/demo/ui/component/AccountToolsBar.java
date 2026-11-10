package com.example.demo.ui.component;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import com.example.demo.ui.shell.Icons;
import com.example.demo.ui.shell.Motion;
/**
 * 账户卡里头像下方那排小按钮：皮肤 / 披风预览、账户管理、退出登录。
 *
 * <p>从 HelloApplication 抽出，动作通过回调传入。
 */
public final class AccountToolsBar extends VBox {

  private final Button logoutButton;

  public AccountToolsBar(
      Runnable onSkinPreview,
      Runnable onAccountManager,
      Runnable onLogout,
      Runnable onHealthCheck) {
    super(6.0);
    Button skin = button("皮肤", "image", "skin", onSkinPreview);
    Button account = button("账户", "user", "account", onAccountManager);
    this.logoutButton = button("退出登录", "back", "logout", onLogout);
    Button health = button("体检", "check", "health", onHealthCheck);
    // 4 个按钮在 340px 宽的卡片里排一行会被截断：拆成两行，每行两个
    HBox row1 = new HBox(6.0, skin, account);
    HBox row2 = new HBox(6.0, logoutButton, health);
    row1.setAlignment(Pos.CENTER);
    row2.setAlignment(Pos.CENTER);
    setAlignment(Pos.CENTER);
    setSpacing(6.0);
    getChildren().setAll(row1, row2);
  }

  /** 未登录时由外层隐藏，登录后恢复显示。 */
  public Button logoutButton() {
    return logoutButton;
  }

  /** 符号化按钮：左边一个图标，右边保留文字（无障碍与 UIA 仍能按名字找到）。 */
  private static Button button(String text, String icon, String key, Runnable action) {
    Button button = new Button(text);
    button.setGraphic(Icons.node(icon, 12.0, "#9aa3ad"));
    button.getStyleClass().add("btn-ghost");
    button.setGraphicTextGap(6.0);
    button.setId("account-tool-" + key);
    button.setAccessibleText(text);
    button.setTooltip(new javafx.scene.control.Tooltip(text));
    if (action != null) {
      button.setOnAction(e -> action.run());
    }
    Motion.press(button);
    Motion.hoverScale(button, 1.04);
    return button;
  }
}
