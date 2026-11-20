package com.example.demo.ui.component;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import com.example.demo.core.model.ModpackProject;
import com.example.demo.ui.shell.PrewarmOverlay;
/**
 * 下载页的结果区：列表 + 加载/空状态浮层 + 预渲染遮罩 +「回到顶部 / 已显示 N 条」栏。
 *
 * <p>从 HelloApplication 抽出。滚动到底部时会防抖触发 {@code onNeedMore}，由主类加载下一页。
 */
public final class DownloadResultView extends VBox {

  private final ListView<ModpackProject> list;
  private final ModpackCell.Host host;
  private final DownloadStatusPane status = new DownloadStatusPane();
  private final PrewarmOverlay prewarm = new PrewarmOverlay();
  private final Label loadMoreHint = new Label();
  private final PauseTransition loadMoreDebounce = new PauseTransition(Duration.millis(260.0));
  private javafx.animation.Timeline hookRetry;
  private final StackPane listPane;

  public DownloadResultView(
      ModpackCell.Host host, ObservableList<ModpackProject> data, Runnable onNeedMore) {
    super(8.0);
    this.host = host;
    setAlignment(Pos.TOP_LEFT);
    VBox.setVgrow(this, Priority.ALWAYS);

    list = new ListView<>(data);
    list.setMinWidth(0);
    // 卡片宽度是绑定 list 宽度算出来的，ListView 再用卡片宽度反推 pref 会形成循环放大
    list.setPrefWidth(0);
    list.setFixedCellSize(88.0);
    list.setCache(true);
    list.setCacheHint(javafx.scene.CacheHint.SPEED);
    list.setStyle(
        "-fx-background-color: transparent; -fx-control-inner-background: transparent;"
            + " -fx-border-color: transparent;");
    list.setMaxHeight(Double.MAX_VALUE);
    list.setCellFactory(param -> new ModpackCell(host, list.widthProperty()));

    // 兜底：即使某个子控件吃掉了点击，也把整行点击翻译成"打开预览"
    list.addEventHandler(
        javafx.scene.input.MouseEvent.MOUSE_CLICKED,
        e -> {
          javafx.scene.Node node = e.getTarget() instanceof javafx.scene.Node n ? n : null;
          while (node != null && !(node instanceof ModpackCell)) {
            node = node.getParent();
          }
          if (node instanceof ModpackCell cell && cell.getItem() != null) {
            host.openDetail(cell.getItem());
          }
        });

    loadMoreDebounce.setOnFinished(
        e -> {
          if (onNeedMore != null) {
            onNeedMore.run();
          }
        });
    list.skinProperty().addListener((obs, old, skin) -> Platform.runLater(this::hookScroll));
    listPane = new StackPane(list, status, prewarm);
    VBox.setVgrow(listPane, Priority.ALWAYS);
    HBox.setHgrow(listPane, Priority.ALWAYS);

    loadMoreHint.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 12px;");
    Button backToTop = new Button("回到顶部");
    backToTop.getStyleClass().addAll("btn-secondary", "btn-sm");
    backToTop.setOnAction(e -> list.scrollTo(0));
    HBox moreBar = new HBox(10.0, backToTop, loadMoreHint);
    moreBar.setAlignment(Pos.CENTER);

    getChildren().addAll(listPane, moreBar);
  }

  /** 给竖直滚动条挂监听：滚到底部附近时防抖请求下一页。 */
  private void hookScroll() {
    javafx.scene.control.ScrollBar scrollBar = findVerticalBar();
    if (scrollBar == null) {
      // 皮肤还没建好时 lookup 会是 null，之前只试一次就再也不挂了 —— 滚到底自然没反应
      scheduleHookRetry();
      return;
    }
    if (scrollBar.getProperties().put("mc.hooked", Boolean.TRUE) != null) {
      return;
    }
    scrollBar
        .valueProperty()
        .addListener(
            (obs, old, value) -> {
              if (value.doubleValue() > 0.94) {
                loadMoreDebounce.playFromStart();
              }
            });
    // 兜底：某些皮肤下滑块值可能停在 1.0 不再变化，用滚动事件再判一次
    list.addEventFilter(
        javafx.scene.input.ScrollEvent.SCROLL,
        event -> {
          javafx.scene.control.ScrollBar bar = findVerticalBar();
          if (bar != null && bar.getValue() >= 0.999 && event.getDeltaY() < 0) {
            loadMoreDebounce.playFromStart();
          }
        });
  }

  private javafx.scene.control.ScrollBar findVerticalBar() {
    for (javafx.scene.Node node : list.lookupAll(".scroll-bar")) {
      if (node instanceof javafx.scene.control.ScrollBar bar
          && bar.getOrientation() == javafx.geometry.Orientation.VERTICAL
          && bar.isVisible()) {
        return bar;
      }
    }
    return null;
  }

  private void scheduleHookRetry() {
    if (hookRetry != null && hookRetry.getStatus() == javafx.animation.Animation.Status.RUNNING) {
      return;
    }
    hookRetry = new javafx.animation.Timeline(
        new javafx.animation.KeyFrame(javafx.util.Duration.millis(400), e -> hookScroll()));
    hookRetry.setCycleCount(25);
    hookRetry.play();
  }

  public ListView<ModpackProject> listView() {
    return list;
  }

  public DownloadStatusPane status() {
    return status;
  }

  public PrewarmOverlay prewarm() {
    return prewarm;
  }

  /** 「已显示 N / 共 M · 滚动到底自动加载」那行提示。 */
  public void setLoadMoreHintText(String text) {
    loadMoreHint.setText(text == null ? "" : text);
  }

  public void scrollToTop() {
    list.scrollTo(0);
  }
}
