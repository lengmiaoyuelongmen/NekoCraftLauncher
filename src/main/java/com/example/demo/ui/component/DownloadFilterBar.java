package com.example.demo.ui.component;

import java.util.function.Consumer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import com.example.demo.core.app.LauncherConfig;
import com.example.demo.core.download.SourcePinger;
import com.example.demo.ui.shell.Icons;
import com.example.demo.ui.shell.Theme;
import com.example.demo.ui.shell.Toast;
/**
 * 下载页顶部的搜索 + 筛选条：来源 / 标签 / 游戏版本 / 加载器 / 排序 + 刷新 / 测速 / 任务按钮。
 *
 * <p>从 HelloApplication 抽出的独立组件，下拉变化和搜索动作通过回调交给主类。
 */
public final class DownloadFilterBar extends VBox {

  private final LauncherConfig config;
  private final Runnable onSearch;
  private final Consumer<String> log;

  private final TextField searchField = new TextField();
  private final Timeline searchDebounce;
  private final VBox filterBox = new VBox(8.0);
  private final ComboBox<String> sourceCombo;
  private final ComboBox<String> tagCombo;
  private final ComboBox<String> gameVersionCombo;
  private final ComboBox<String> loaderCombo;
  private final ComboBox<String> sortCombo;
  private boolean suppressSearch;

  public DownloadFilterBar(
      LauncherConfig config, Runnable onSearch, Runnable onShowTasks, Consumer<String> log) {
    super(10.0);
    this.config = config;
    this.onSearch = onSearch;
    this.log = log;
    setAlignment(Pos.TOP_LEFT);
    VBox.setVgrow(this, Priority.ALWAYS);

    HBox searchBar = new HBox(15.0);
    searchBar.setAlignment(Pos.CENTER_LEFT);
    searchField.setPromptText("搜索 整合包...");
    searchField.setPrefHeight(40.0);
    searchField.setMaxWidth(Double.MAX_VALUE);
    HBox.setHgrow(searchField, Priority.ALWAYS);
    searchField.setStyle("-fx-font-size: 14px; -fx-padding: 0 15;");
    searchDebounce = new Timeline(new KeyFrame(Duration.millis(500.0), e -> fireSearch()));
    searchDebounce.setCycleCount(1);
    searchField.textProperty().addListener((obs, old, text) -> searchDebounce.playFromStart());

    Button searchBtn = new Button("搜索");
    searchBtn.getStyleClass().add("btn-primary");
    searchBtn.setMinWidth(Double.NEGATIVE_INFINITY);
    searchBtn.setOnAction(
        e -> {
          searchDebounce.stop();
          fireSearch();
        });
    searchField.setOnAction(
        e -> {
          searchDebounce.stop();
          fireSearch();
        });
    searchBar.getChildren().addAll(searchField, searchBtn);

    filterBox.setStyle(
        "-fx-background-color: linear-gradient(to bottom, -app-card-top, -app-card-alt2);"
            + " -fx-padding: 12 14; -fx-background-radius: 16; -fx-border-color:"
            + " -app-border-soft; -fx-border-radius: 16; -fx-effect:"
            + " dropshadow(gaussian, rgba(0,0,0,0.22), 18, 0.1, 0, 5);");

    FlowPane row1 = new FlowPane(10.0, 8.0);
    row1.setPrefWrapLength(860.0);
    sourceCombo = filterCombo("全部", "Modrinth", "CurseForge");
    tagCombo = filterCombo("全部", "轻量", "冒险", "硬核");
    gameVersionCombo = filterCombo("任意", "1.21", "1.20.1", "1.19.2", "1.16.5", "1.12.2", "1.7.10");
    loaderCombo = filterCombo("任意", "Fabric", "Forge", "Quilt", "NeoForge", "DataPack");
    sortCombo = filterCombo("默认", "下载量", "更新时间");

    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);
    Button refreshBtn = iconButton("refresh", "刷新列表");
    refreshBtn.setOnAction(e -> fireSearch());
    Button speedBtn = new Button("测速");
    speedBtn.setStyle(
        "-fx-background-color: transparent; -fx-text-fill: #9ca3af; -fx-font-size: 11px;"
            + " -fx-cursor: hand; -fx-padding: 2 6;");
    speedBtn.setTooltip(new Tooltip("测试 Modrinth / MCIM / CurseForge 镜像延迟"));
    speedBtn.setOnAction(e -> runSpeedTest(speedBtn));
    Button taskBtn = iconButton("download", "下载任务与最近下载位置");
    if (onShowTasks != null) {
      taskBtn.setOnAction(e -> onShowTasks.run());
    }

    FlowPane row2 = new FlowPane(12.0, 8.0);
    row2.setPrefWrapLength(860.0);
    row2.getChildren()
        .addAll(labeled("来源", sourceCombo), labeled("标签", tagCombo), labeled("排序", sortCombo));
    row1.getChildren()
        .addAll(
            labeled("游戏版本", gameVersionCombo),
            labeled("加载器", loaderCombo),
            spacer,
            speedBtn,
            refreshBtn,
            taskBtn);
    filterBox.getChildren().addAll(row1, row2);

    sourceCombo.setPrefWidth(125.0);
    sourceCombo.setMinWidth(110.0);
    tagCombo.setPrefWidth(125.0);
    tagCombo.setMinWidth(110.0);
    gameVersionCombo.setPrefWidth(155.0);
    gameVersionCombo.setMinWidth(135.0);
    loaderCombo.setPrefWidth(165.0);
    loaderCombo.setMinWidth(145.0);
    sortCombo.setPrefWidth(135.0);
    sortCombo.setMinWidth(115.0);

    getChildren().addAll(searchBar, filterBox);
  }

  private void fireSearch() {
    if (!suppressSearch && onSearch != null) {
      onSearch.run();
    }
  }

  private ComboBox<String> filterCombo(String defaultVal, String... items) {
    ComboBox<String> combo = new ComboBox<>();
    combo.getItems().add(defaultVal);
    combo.getItems().addAll(items);
    combo.setValue(defaultVal);
    combo.setStyle("-fx-background-radius: 6; -fx-font-size: 13px;");
    combo.setButtonCell(darkCell());
    combo.valueProperty().addListener((obs, oldVal, newVal) -> fireSearch());
    return combo;
  }

  private <T> ListCell<T> darkCell() {
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

  private HBox labeled(String text, ComboBox<String> combo) {
    Label label = new Label(text);
    HBox box = new HBox(6.0, label, combo);
    box.setAlignment(Pos.CENTER_LEFT);
    label.setStyle("-fx-text-fill: -app-text-muted-3; -fx-font-size: 12px;");
    return box;
  }

  private Button iconButton(String icon, String tooltip) {
    Button button = new Button();
    button.setGraphic(Icons.node(icon, 15.0, "#9ca3af"));
    button.setStyle(
        "-fx-background-color: transparent; -fx-text-fill: #aaa; -fx-font-size: 16px;"
            + " -fx-cursor: hand;");
    button.setTooltip(new Tooltip(tooltip));
    return button;
  }

  /** 后台测速并自动选择最快下载源。 */
  private void runSpeedTest(Button speedBtn) {
    speedBtn.setDisable(true);
    speedBtn.setText("测速中…");
    Task<java.util.List<SourcePinger.Result>> task =
        new Task<>() {
          @Override
          protected java.util.List<SourcePinger.Result> call() {
            return SourcePinger.test();
          }
        };
    task.setOnSucceeded(
        e -> {
          speedBtn.setDisable(false);
          speedBtn.setText("测速");
          java.util.List<SourcePinger.Result> results = task.getValue();
          if (results == null || results.isEmpty()) {
            Toast.warn("测速失败：没有可用结果");
            return;
          }
          StringBuilder text = new StringBuilder();
          for (SourcePinger.Result result : results) {
            text.append(result.name())
                .append("  ")
                .append(result.ok() ? result.millis() + "ms" : "失败")
                .append("\n");
            if (log != null) {
              log.accept(
                  (result.ok() ? "🌐 " : "⚠ ")
                      + result.name()
                      + ": "
                      + (result.ok() ? result.millis() + "ms" : result.note()));
            }
          }
          SourcePinger.Result best = results.get(0);
          String selected =
              best.name().contains("MCIM")
                  ? "mirror"
                  : (best.name().contains("CurseForge") ? "auto" : "official");
          config.setDownloadSource(selected);
          config.save();
          Toast.ok("最快源：" + best.name() + " " + best.millis() + "ms，已自动选用");
          if (log != null) {
            log.accept("🌐 已自动选择下载源: " + best.name() + "（" + selected + "）");
          }
        });
    task.setOnFailed(
        e -> {
          speedBtn.setDisable(false);
          speedBtn.setText("测速");
          Throwable ex = task.getException();
          Toast.error("测速失败：" + (ex == null ? "未知错误" : ex.getMessage()));
        });
    Thread thread = new Thread(task, "source-ping");
    thread.setDaemon(true);
    thread.start();
  }

  public TextField searchField() {
    return searchField;
  }

  public ComboBox<String> sourceCombo() {
    return sourceCombo;
  }

  public ComboBox<String> tagCombo() {
    return tagCombo;
  }

  public ComboBox<String> gameVersionCombo() {
    return gameVersionCombo;
  }

  public ComboBox<String> loaderCombo() {
    return loaderCombo;
  }

  public ComboBox<String> sortCombo() {
    return sortCombo;
  }

  public void setSearchPrompt(String text) {
    searchField.setPromptText(text);
  }

  /** 程序化改下拉值时用，避免触发搜索。 */
  public void setSuppressSearch(boolean suppress) {
    this.suppressSearch = suppress;
  }
}
