package com.example.demo;

import java.util.function.Consumer;
import java.util.function.Function;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import com.example.demo.core.model.LoginCardData;
/**
 * 账户卡的登录表单：登录方式下拉、验证服务器、账号密码、微软授权提示和登录大按钮。
 *
 * <p>从 HelloApplication 抽出的独立组件；登录方式变化时会回调主类（用来做窗口高度联动）。
 */
public final class LoginFormPane extends VBox {

  private final ComboBox<String> typeCombo = new ComboBox<>();
  private final ComboBox<String> serverCombo = new ComboBox<>();
  private final TextField usernameField = new TextField();
  private final PasswordField passwordField = new PasswordField();
  private final LoginHeroButton loginBtn = new LoginHeroButton();
  private final Label msHint = new Label("将打开浏览器并显示设备代码；需要已购买 Minecraft。");
  private final Consumer<String> onTypeChanged;

  public LoginFormPane(
      Function<String, LoginCardData> parser,
      Function<String, String> serverNormalizer,
      Runnable submitLogin,
      Consumer<String> onTypeChanged) {
    super(8.0);
    this.onTypeChanged = onTypeChanged;
    setPadding(new Insets(12.0));
    setStyle(
        "-fx-background-color: rgba(255,255,255,0.035); -fx-background-radius: 12;"
            + " -fx-border-color: rgba(255,255,255,0.05); -fx-border-radius: 12;");

    typeCombo.getItems().addAll("离线登录", "第三方外置登录", "正版登录 (微软)");
    typeCombo.setValue("离线登录");
    typeCombo.setMaxWidth(Double.MAX_VALUE);
    typeCombo.setButtonCell(darkCell());

    serverCombo.setEditable(true);
    serverCombo.setPromptText("验证服务器地址");
    serverCombo.setMaxWidth(Double.MAX_VALUE);
    serverCombo.setPrefHeight(34.0);
    serverCombo.setVisible(false);
    serverCombo.setManaged(false);
    serverCombo.getItems().addAll("https://littleskin.cn/api/yggdrasil", "https://littleskin.cn");
    serverCombo.setButtonCell(darkCell());

    usernameField.setPromptText("玩家名/邮箱");
    usernameField.setPrefHeight(34.0);
    passwordField.setPromptText("密码");
    passwordField.setPrefHeight(34.0);
    passwordField.setVisible(false);
    passwordField.setManaged(false);

    VBox.setMargin(loginBtn, new Insets(22.0, 0.0, 0.0, 0.0));
    msHint.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 10.5px;");
    msHint.setWrapText(true);
    msHint.setVisible(false);
    msHint.setManaged(false);

    HBox credentialsRow = new HBox(8.0, usernameField, passwordField);
    HBox.setHgrow(usernameField, Priority.ALWAYS);
    HBox.setHgrow(passwordField, Priority.ALWAYS);
    usernameField.setMaxWidth(Double.MAX_VALUE);
    passwordField.setMaxWidth(Double.MAX_VALUE);

    typeCombo.setOnAction(e -> applyType(typeCombo.getValue()));

    getChildren().addAll(typeCombo, serverCombo, credentialsRow, msHint, loginBtn);

    LoginCardDrop.install(
        this,
        typeCombo,
        serverCombo,
        usernameField,
        passwordField,
        parser,
        serverNormalizer,
        submitLogin);
  }

  /** 按登录方式切换显示项，并通知主类做窗口联动。 */
  public void applyType(String selected) {
    boolean is3rd = "第三方外置登录".equals(selected);
    boolean isMs = "正版登录 (微软)".equals(selected);
    serverCombo.setVisible(is3rd);
    serverCombo.setManaged(is3rd);
    passwordField.setVisible(is3rd);
    passwordField.setManaged(is3rd);
    usernameField.setVisible(!isMs);
    usernameField.setManaged(!isMs);
    msHint.setVisible(isMs);
    msHint.setManaged(isMs);
    loginBtn.label().setText(isMs ? "开始微软授权" : "登录");
    if (onTypeChanged != null) {
      onTypeChanged.accept(selected == null ? "" : selected);
    }
  }

  private static <T> ListCell<T> darkCell() {
    return new ListCell<T>() {
      @Override
      protected void updateItem(T item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
          setText(null);
        } else {
          setText(item.toString());
          setTextFill(javafx.scene.paint.Color.web(Theme.isLight() ? "#1f2937" : "#E0E0E0"));
          setStyle("-fx-background-color: transparent;");
        }
      }
    };
  }

  public ComboBox<String> typeCombo() {
    return typeCombo;
  }

  public ComboBox<String> serverCombo() {
    return serverCombo;
  }

  public TextField username() {
    return usernameField;
  }

  public PasswordField password() {
    return passwordField;
  }

  public Button loginButton() {
    return loginBtn;
  }

  public Label loginLabel() {
    return loginBtn.label();
  }
}
