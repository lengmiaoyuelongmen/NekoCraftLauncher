package com.example.demo;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.function.Function;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.VBox;

/**
 * 把登录卡片文件（txt）拖到登录面板上自动填表并登录。
 *
 * <p>从 HelloApplication 抽出的静态工具；解析、服务器地址规范化和提交登录由主类以回调传入。
 */
public final class LoginCardDrop {

  private static final String NORMAL_STYLE =
      "-fx-background-color: rgba(255,255,255,0.035); -fx-background-radius: 12;"
          + " -fx-border-color: rgba(255,255,255,0.05); -fx-border-radius: 12;";
  private static final String HOVER_STYLE =
      "-fx-background-color: rgba(20,184,166,0.12); -fx-background-radius: 12;"
          + " -fx-border-color: #14b8a6; -fx-border-radius: 12;";

  private LoginCardDrop() {}

  public static void install(
      VBox card,
      ComboBox<String> typeCombo,
      ComboBox<String> serverField,
      TextField usernameField,
      PasswordField passwordField,
      Function<String, LoginCardData> parser,
      Function<String, String> serverNormalizer,
      Runnable submitLogin) {
    card.setOnDragOver(
        event -> {
          if (event.getDragboard().hasFiles()) {
            event.acceptTransferModes(TransferMode.COPY);
            card.setStyle(HOVER_STYLE);
          }
          event.consume();
        });
    card.setOnDragExited(
        event -> {
          card.setStyle(NORMAL_STYLE);
          event.consume();
        });
    card.setOnDragDropped(
        event -> {
          boolean ok = false;
          if (event.getDragboard().hasFiles()) {
            for (File file : event.getDragboard().getFiles()) {
              try {
                String text = Files.readString(file.toPath(), StandardCharsets.UTF_8);
                LoginCardData data = parser.apply(text);
                if (data == null || data.username().isBlank()) {
                  continue;
                }
                typeCombo.setValue(typeCombo.getItems().get(1));
                String server =
                    serverNormalizer == null
                        ? data.server()
                        : serverNormalizer.apply(data.server());
                if (server != null && !server.isBlank()) {
                  serverField.setValue(server);
                  serverField.getEditor().setText(server);
                }
                usernameField.setText(data.username());
                passwordField.setText(data.password());
                ok = true;
                Toast.ok("已读取登录卡片，正在登录…");
                if (submitLogin != null) {
                  submitLogin.run();
                }
                break;
              } catch (Exception ignored) {
                // 文件读不出来就试下一个
              }
            }
          }
          card.setStyle(NORMAL_STYLE);
          event.setDropCompleted(ok);
          event.consume();
        });
  }
}
