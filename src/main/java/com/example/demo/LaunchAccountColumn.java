package com.example.demo;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * 启动页左侧的账户卡整列：头像、用户名、登录表单、账户信息、启动按钮、底部两个入口。
 *
 * <p>从 HelloApplication 的 createLaunchPanel 抽出。组件只负责布局与样式， 具体动作（登录、退出、切页）通过 {@link Callbacks} 交回主类。
 */
public final class LaunchAccountColumn extends VBox {

  /** 主类需要提供的动作。 */
  public interface Callbacks {
    void log(String text);

    void openSkinPreview();

    void showAccountManager();

    void showHealthCheck();

    void chooseAvatar();

    void toggleAvatarMode();

    void launch();

    void openInstances();

    void openInstanceSettings();

    void logout();

    void submitLogin(LoginFormPane form);

    void loginTypeChanged(String type);

    UserSession session();

    LoginCardData parseLoginCard(String text);

    String normalizeServer(String server);
  }

  private final AvatarPane avatarPane;
  private final Label usernameLabel = new Label("未登录");
  private final Label userTypeLabel = new Label("请在下方登录");
  private final Label uuidLabel = new Label("");
  private LoginFormPane loginForm;
  private final Button logoutButton = new Button("退出登录");
  private final AccountInfoCard accountInfo = new AccountInfoCard();
  private final LaunchHeroButton launchButton;
  private final HBox bottomRow;

  public LaunchAccountColumn(LauncherConfig config, Callbacks callbacks) {
    super(0.0);
    setPrefWidth(340.0);
    setMinWidth(300.0);
    setMaxWidth(380.0);
    setMaxHeight(Double.MAX_VALUE);
    setAlignment(Pos.CENTER);
    setStyle(
        "-fx-background-color: -app-card; -fx-background-radius: 16; -fx-padding: 14 16 6"
            + " 16; -fx-border-color: -app-border-2; -fx-border-width: 1;"
            + " -fx-border-radius: 16; -fx-effect: dropshadow(gaussian,"
            + " rgba(15,23,42,0.07), 20, 0.08, 0, 6);");

    avatarPane =
        new AvatarPane(
            config,
            callbacks::session,
            callbacks::log,
            callbacks::chooseAvatar,
            callbacks::toggleAvatarMode);

    usernameLabel.setStyle("-fx-font-size: 25px; -fx-font-weight: bold; -fx-text-fill: -app-text;");
    usernameLabel.setMaxWidth(Double.MAX_VALUE);
    usernameLabel.setAlignment(Pos.CENTER);
    usernameLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
    usernameLabel.setWrapText(false);
    userTypeLabel.setStyle("-fx-font-size: 12.5px; -fx-text-fill: -app-text-dim-2;");
    uuidLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #5c6572;");

    loginForm =
        new LoginFormPane(
            callbacks::parseLoginCard,
            callbacks::normalizeServer,
            () -> callbacks.submitLogin(loginForm),
            callbacks::loginTypeChanged);

    logoutButton.setStyle(
        "-fx-background-color: transparent; -fx-text-fill: -app-text-muted;"
            + " -fx-font-size: 11px; -fx-cursor: hand; -fx-padding: 0;");
    logoutButton.setVisible(false);
    logoutButton.setManaged(false);
    logoutButton.setOnAction(e -> callbacks.logout());

    HBox accountTools =
        new AccountToolsBar(
            callbacks::openSkinPreview, callbacks::showAccountManager, callbacks::showHealthCheck);

    launchButton = new LaunchHeroButton(this);

    Button instSelect = new Button("实例选择");
    Button instSettings = new Button("实例设置");
    for (Button b : new Button[] {instSelect, instSettings}) {
      b.setMaxWidth(Double.MAX_VALUE);
      String normalStyle =
          "-fx-background-color: transparent; -fx-text-fill: -app-text-2;"
              + " -fx-font-size: 13px; -fx-border-color: -app-border-soft;"
              + " -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 7 9;"
              + " -fx-cursor: hand;";
      String hoverStyle =
          "-fx-background-color: -app-hover-2; -fx-text-fill: -app-text;"
              + " -fx-font-size: 13px; -fx-border-color: -app-accent;"
              + " -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 7 9;"
              + " -fx-cursor: hand;";
      b.setStyle(normalStyle);
      HBox.setHgrow(b, Priority.ALWAYS);
      Motion.press(b);
      b.setOnMouseEntered(e -> b.setStyle(hoverStyle));
      b.setOnMouseExited(e -> b.setStyle(normalStyle));
    }
    instSelect.setOnAction(e -> callbacks.openInstances());
    instSettings.setOnAction(e -> callbacks.openInstanceSettings());
    bottomRow = new HBox(10.0, instSelect, instSettings);
    bottomRow.setAlignment(Pos.CENTER);
    launchButton.setOnAction(e -> callbacks.launch());

    Region profileSpacer = new Region();
    profileSpacer.setMinHeight(0.0);
    profileSpacer.setPrefHeight(0.0);
    profileSpacer.setMaxHeight(Double.MAX_VALUE);
    VBox.setVgrow(profileSpacer, Priority.ALWAYS);

    getChildren()
        .addAll(
            avatarPane,
            usernameLabel,
            userTypeLabel,
            uuidLabel,
            accountTools,
            loginForm,
            logoutButton,
            accountInfo,
            profileSpacer,
            launchButton,
            bottomRow);
    VBox.setMargin(avatarPane, new Insets(6.0, 0.0, 0.0, 0.0));
    VBox.setMargin(usernameLabel, new Insets(6.0, 0.0, 0.0, 0.0));
    VBox.setMargin(accountTools, new Insets(10.0, 0.0, 0.0, 0.0));
    VBox.setMargin(loginForm, new Insets(10.0, 0.0, 0.0, 0.0));
    VBox.setMargin(launchButton, new Insets(16.0, 0.0, 0.0, 0.0));
    VBox.setMargin(bottomRow, new Insets(10.0, 0.0, 6.0, 0.0));
  }

  public AvatarPane avatarPane() {
    return avatarPane;
  }

  public Label usernameLabel() {
    return usernameLabel;
  }

  public Label userTypeLabel() {
    return userTypeLabel;
  }

  public Label uuidLabel() {
    return uuidLabel;
  }

  public LoginFormPane loginForm() {
    return loginForm;
  }

  public Button logoutButton() {
    return logoutButton;
  }

  public AccountInfoCard accountInfo() {
    return accountInfo;
  }

  public LaunchHeroButton launchButton() {
    return launchButton;
  }

  public HBox bottomRow() {
    return bottomRow;
  }
}
