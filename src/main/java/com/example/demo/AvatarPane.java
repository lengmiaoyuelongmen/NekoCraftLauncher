package com.example.demo;

import java.io.File;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.scene.Cursor;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseButton;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

/**
 * 账户卡顶部的头像框：点击选择头像、右键菜单切换皮肤/自定义、拖入图片预览。
 *
 * <p>从 HelloApplication 抽出的独立组件；真正的选择/切换动作由主类回调完成。
 */
public final class AvatarPane extends StackPane {

  private final LauncherConfig config;
  private final ImageView imageView = new ImageView();
  private final Runnable onChoose;
  private final Runnable onToggleMode;

  public AvatarPane(
      LauncherConfig config,
      Supplier<UserSession> session,
      Consumer<String> log,
      Runnable onChoose,
      Runnable onToggleMode) {
    this.config = config;
    this.onChoose = onChoose;
    this.onToggleMode = onToggleMode;

    imageView.setFitWidth(104.0);
    imageView.setFitHeight(104.0);
    imageView.setSmooth(false);
    Rectangle clip = new Rectangle(104.0, 104.0);
    clip.setArcWidth(14.0);
    clip.setArcHeight(14.0);
    imageView.setClip(clip);

    getChildren().addAll(Icons.node("user", 42.0, "#6b7280"), imageView);
    setStyle(
        "-fx-background-color: linear-gradient(to bottom right, rgba(255,255,255,0.10),"
            + " rgba(255,255,255,0.03)); -fx-background-radius: 16; -fx-border-color:"
            + " rgba(156,163,175,0.55); -fx-border-radius: 16; -fx-border-width: 1.5;"
            + " -fx-padding: 4; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 18,"
            + " 0.15, 0, 6);");
    setPrefSize(112.0, 112.0);
    setMinSize(112.0, 112.0);
    setMaxSize(112.0, 112.0);
    Tooltip.install(this, new Tooltip("点击选择头像，或把 PNG/JPG 拖到这里"));
    setCursor(Cursor.HAND);

    setOnMouseClicked(
        e -> {
          if (e.getButton() == MouseButton.SECONDARY) {
            ContextMenu menu = new ContextMenu();
            MenuItem choose = new MenuItem("选择自定义头像");
            choose.setOnAction(ev -> run(onChoose));
            MenuItem toggle = new MenuItem(config.isAvatarUseSkin() ? "切换为自定义头像" : "切换为皮肤头像");
            toggle.setOnAction(ev -> run(onToggleMode));
            MenuItem reset = new MenuItem("恢复默认头像");
            reset.setOnAction(
                ev -> {
                  config.setAvatarImage("");
                  config.setAvatarUseSkin(false);
                  config.save();
                  imageView.setImage(null);
                  Toast.ok("已恢复默认头像");
                });
            menu.getItems().addAll(choose, toggle, reset);
            menu.show(this, e.getScreenX(), e.getScreenY());
            e.consume();
            return;
          }
          UserSession current = session == null ? null : session.get();
          boolean hasSkin =
              current != null && current.getSkinUrl() != null && !current.getSkinUrl().isBlank();
          if (hasSkin && config.getAvatarImage() != null && !config.getAvatarImage().isBlank()) {
            run(onToggleMode);
          } else {
            run(onChoose);
          }
        });

    setOnDragOver(
        event -> {
          if (event.getDragboard().hasFiles()
              || event.getDragboard().hasUrl()
              || event.getDragboard().hasString()) {
            event.acceptTransferModes(TransferMode.COPY);
          }
          event.consume();
        });
    setOnDragDropped(
        event -> {
          Dragboard board = event.getDragboard();
          try {
            Image skin = null;
            if (board.hasFiles()) {
              for (File file : board.getFiles()) {
                String name = file.getName().toLowerCase(Locale.ROOT);
                if (!name.endsWith(".png") && !name.endsWith(".jpg") && !name.endsWith(".jpeg")) {
                  continue;
                }
                skin = new Image(file.toURI().toString(), true);
                break;
              }
            } else if (board.hasUrl()) {
              skin = new Image(board.getUrl(), true);
            }
            if (skin != null && !skin.isError()) {
              imageView.setImage(skin);
              Toast.ok("皮肤已加载预览");
              if (log != null) {
                log.accept("🧵 已载入皮肤预览");
              }
            }
          } catch (Exception ex) {
            Toast.error("皮肤图片读取失败");
          }
          event.setDropCompleted(true);
          event.consume();
        });
  }

  /** 主类更新头像时用。 */
  public ImageView imageView() {
    return imageView;
  }

  /** 皮肤头像加载完成后设置图片。 */
  public void setImage(Image image) {
    imageView.setImage(image);
  }

  private static void run(Runnable action) {
    if (action != null) {
      action.run();
    }
  }
}
