package com.example.demo.ui.component;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import com.example.demo.core.model.LoginCardData;
import com.example.demo.core.model.UserSession;
import com.example.demo.core.app.LauncherConfig;
import com.example.demo.ui.shell.Motion;
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
  private final Button logoutButton;
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
    getStyleClass().add("neko-card");
    // 卡片自身留一点内边距，底部两个按钮不会顶到圆角边上
    setPadding(new Insets(6, 16, 14, 16));

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

    AccountToolsBar accountTools =
        new AccountToolsBar(
            callbacks::openSkinPreview,
            callbacks::showAccountManager,
            callbacks::logout,
            callbacks::showHealthCheck);
    this.logoutButton = accountTools.logoutButton();
    this.logoutButton.setVisible(false);
    this.logoutButton.setManaged(false);
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

    // 身份信息居中，底部按钮组固定：空白被上下分摊，不再是"中间一大块空"
    VBox identity = new VBox(0.0, avatarPane, usernameLabel, userTypeLabel, uuidLabel, accountTools, loginForm, accountInfo);
    identity.setAlignment(Pos.CENTER);
    identity.setMinHeight(0.0);
    VBox.setVgrow(identity, Priority.ALWAYS);

    getChildren().addAll(identity, launchButton, bottomRow);
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
