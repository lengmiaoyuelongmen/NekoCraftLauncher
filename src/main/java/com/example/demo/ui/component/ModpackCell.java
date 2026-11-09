package com.example.demo.ui.component;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

import com.example.demo.core.model.ModpackProject;
import com.example.demo.core.download.IconProxy;
import com.example.demo.core.model.Favorites;
import com.example.demo.ui.shell.Icons;
import com.example.demo.ui.shell.Motion;
import com.example.demo.ui.shell.Toast;
/**
 * 下载页的模组 / 整合包列表单元格：图标、标题、简介、作者 / 下载量 / 更新时间 / 来源、收藏与下载按钮。
 *
 * <p>从 HelloApplication 的内部类抽出；主类通过 {@link Host} 提供图标缓存、日志和下载入口。
 */
public final class ModpackCell extends ListCell<ModpackProject> {

  /** 主类需要提供的能力。 */
  public interface Host {
    Map<String, Image> iconCache();

    Image decodeIcon(byte[] bytes);

    HttpClient httpClient();

    void log(String text);

    void download(ModpackProject project);

    void openDetail(ModpackProject project);
  }

  private final Host host;
  private final ReadOnlyDoubleProperty listWidth;
  private final HBox card = new HBox(12.0);
  private final StackPane iconPane = new StackPane();
  private final Label titleLabel = new Label();
  private final Label descLabel = new Label();
  private final Label authorLabel = new Label();
  private final Label dlLabel = new Label();
  private final Label timeLabel = new Label();
  private final Label srcLabel = new Label();
  private final Button downloadBtn = new Button("下载");
  private final Button starBtn = new Button();

  /** 首次渲染后就不再播入场动画（ListView 会复用 cell）。 */
  private boolean revealed;

  public ModpackCell(Host host, ReadOnlyDoubleProperty listWidth) {
    this.host = host;
    this.listWidth = listWidth;
    card.setPadding(new Insets(10.0, 14.0, 10.0, 14.0));
    card.setStyle(
        "-fx-background-color: linear-gradient(to bottom, -app-card-top, -app-card-alt2);"
            + " -fx-background-radius: 16; -fx-border-color: -app-border-soft;"
            + " -fx-border-radius: 14;");
    card.setCursor(Cursor.HAND);
    card.prefWidthProperty().bind(listWidth.subtract(30));
    // 同时限制上限：内容再长也不会把右侧按钮挤出可视区
    card.maxWidthProperty().bind(listWidth.subtract(30));
    card.setMinWidth(0);
    Motion.hoverCard(card);

    iconPane.setPrefSize(58.0, 58.0);
    iconPane.setMinSize(58.0, 58.0);
    iconPane.setStyle("-fx-background-color: -app-panel; -fx-background-radius: 12;");

    VBox info = new VBox(6.0);
    HBox.setHgrow(info, Priority.ALWAYS);
    info.setMaxWidth(Double.MAX_VALUE);
    titleLabel.setStyle(
        "-fx-text-fill: -app-text-strong; -fx-font-size: 17px; -fx-font-weight: bold;");
    titleLabel.setWrapText(false);
    titleLabel.setMinWidth(0);
    titleLabel.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
    titleLabel.setMaxWidth(Double.MAX_VALUE);
    descLabel.setStyle("-fx-text-fill: -app-text-muted-3; -fx-font-size: 12px;");
    descLabel.setWrapText(false);
    descLabel.setMinWidth(0);
    descLabel.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
    descLabel.setMaxWidth(Double.MAX_VALUE);

    HBox meta = new HBox(10.0);
    for (Label l : new Label[] {authorLabel, dlLabel, timeLabel, srcLabel}) {
      l.setStyle(
          "-fx-text-fill: -app-text-muted-2; -fx-font-size: 12px; -fx-font-weight:" + " bold;");
    }
    meta.getChildren().addAll(authorLabel, dlLabel, timeLabel, srcLabel);
    info.getChildren().addAll(titleLabel, descLabel, meta);

    downloadBtn.getStyleClass().addAll("btn-secondary", "btn-sm");
    downloadBtn.setMinWidth(Double.NEGATIVE_INFINITY);

    starBtn.setGraphic(Icons.node("star", 15.0, "#6b7280"));
    starBtn.setStyle(
        "-fx-background-color: transparent; -fx-text-fill: -app-text-dim-2;"
            + " -fx-font-size: 18px; -fx-cursor: hand; -fx-padding: 0;");
    starBtn.setMinSize(26.0, 26.0);
    starBtn.setPrefSize(26.0, 26.0);

    HBox actions = new HBox(4.0, starBtn, downloadBtn);
    actions.setAlignment(Pos.CENTER_RIGHT);
    actions.setMinWidth(Double.NEGATIVE_INFINITY);
    card.getChildren().addAll(iconPane, info, actions);

    setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
  }

  private ImageView iconView(Image img) {
    ImageView view = new ImageView(img);
    view.setFitWidth(70.0);
    view.setFitHeight(70.0);
    Rectangle clip = new Rectangle(70.0, 70.0);
    clip.setArcWidth(20.0);
    clip.setArcHeight(20.0);
    view.setClip(clip);
    return view;
  }

  private void showIconFallback(ModpackProject project) {
    String title = project.getTitle();
    Label fallback =
        new Label(title == null || title.isEmpty() ? "?" : title.substring(0, 1).toUpperCase());
    fallback.setStyle(
        "-fx-text-fill: -app-text-muted-2; -fx-font-size: 26px; -fx-font-weight: bold;");
    iconPane.getChildren().setAll(fallback);
  }

  @Override
  protected void updateItem(ModpackProject project, boolean empty) {
    super.updateItem(project, empty);
    if (empty || project == null) {
      setGraphic(null);
      return;
    }
    // 列表项首次出现时淡入 + 上移一点点；cell 复用后不再重复播
    if (!revealed) {
      revealed = true;
      com.example.demo.ui.shell.Motion.reveal(this, 0L);
    }
    if (empty || project == null) {
      setGraphic(null);
      return;
    }
    titleLabel.setText(project.getTitle());
    descLabel.setText(project.getDescription());
    authorLabel.setText("👤 " + project.getAuthor());
    dlLabel.setText("⬇ " + project.getFormattedDownloads());
    timeLabel.setText("⏱ " + project.getUpdated());
    srcLabel.setText("🌐 " + project.getSource());

    iconPane.getChildren().clear();
    String iconKey = project.getIconUrl();
    Image cached = iconKey == null || iconKey.isBlank() ? null : host.iconCache().get(iconKey);
    if (cached != null) {
      iconPane.getChildren().setAll(iconView(cached));
    } else {
      ProgressIndicator loading = new ProgressIndicator();
      loading.setPrefSize(22.0, 22.0);
      loading.setMinSize(22.0, 22.0);
      loading.setMaxSize(22.0, 22.0);
      loading.setStyle("-fx-accent: #14b8a6;");
      iconPane.getChildren().add(loading);
      if (iconKey == null || iconKey.isEmpty()) {
        showIconFallback(project);
      } else {
        String url = iconKey.startsWith("http") ? iconKey : "https://" + iconKey;
        java.util.concurrent.CompletableFuture.runAsync(() -> loadCellIcon(url, iconKey, project));
      }
    }

    boolean isMod = "mod".equals(project.getProjectType());
    downloadBtn.setText("下载");
    downloadBtn.setOnAction(
        e -> {
          Toast.info("开始解析 " + project.getTitle() + " 的下载地址…");
          host.log("📥 开始解析 [" + project.getTitle() + "] 的下载链路...");
          host.download(project);
        });


    boolean favorite = Favorites.contains(project.getId());
    starBtn.setGraphic(Icons.node("star", 15.0, favorite ? "#eab308" : "#6b7280"));
    starBtn.setOnAction(
        e -> {
          e.consume();
          Favorites.toggle(
              new Favorites.Fav(
                  project.getId(),
                  project.getTitle(),
                  project.getProjectType(),
                  project.getIconUrl(),
                  project.getDownloads(),
                  System.currentTimeMillis()));
          boolean now = Favorites.contains(project.getId());
          starBtn.setGraphic(Icons.node("star", 15.0, now ? "#eab308" : "#6b7280"));
          Toast.ok(now ? "已收藏 " + project.getTitle() : "已取消收藏 " + project.getTitle());
        });
    setGraphic(card);
  }

  private void loadCellIcon(String url, String iconKey, ModpackProject project) {
    try {
      byte[] bytes = null;
      for (String candidate : IconProxy.candidates(url)) {
        try {
          HttpRequest req =
              HttpRequest.newBuilder(URI.create(candidate))
                  .timeout(java.time.Duration.ofSeconds(8L))
                  .GET()
                  .build();
          HttpResponse<InputStream> resp =
              host.httpClient().send(req, HttpResponse.BodyHandlers.ofInputStream());
          if (resp.statusCode() == 200) {
            bytes = resp.body().readAllBytes();
            if (bytes.length > 0) {
              break;
            }
          }
        } catch (Exception ignored) {
          // 换下一个源
        }
      }
      final byte[] iconBytes = bytes;
      if (iconBytes != null && iconBytes.length > 0) {
        Platform.runLater(
            () -> {
              Image img = host.decodeIcon(iconBytes);
              if (img != null && !img.isError()) {
                host.iconCache().put(iconKey, img);
                if (getItem() == project) {
                  iconPane.getChildren().setAll(iconView(img));
                }
              } else if (getItem() == project) {
                showIconFallback(project);
              }
            });
      } else {
        Platform.runLater(
            () -> {
              if (getItem() == project) {
                showIconFallback(project);
              }
            });
      }
    } catch (Exception ignored) {
      Platform.runLater(
          () -> {
            if (getItem() == project) {
              showIconFallback(project);
            }
          });
    }
  }

  /** 供主类在列表宽度变化后刷新卡片宽度时使用。 */
  public Node cardNode() {
    return card;
  }
}
