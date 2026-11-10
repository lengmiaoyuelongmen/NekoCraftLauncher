package com.example.demo.ui.component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.stage.Popup;

import com.example.demo.core.app.LauncherConfig;
import com.example.demo.ui.shell.CommandCompleter;
import com.example.demo.ui.shell.Icons;
import com.example.demo.ui.shell.Motion;
/**
 * 启动页底部命令栏：AI 开关、命令输入、Tab 补全和历史上下键。
 *
 * <p>从 HelloApplication 抽出的独立组件；提交内容通过 {@link Consumer} 交给主类决定走 AI 还是命令。
 */
public final class CommandBar extends HBox {

  private final LauncherConfig config;
  private final TextField field = new TextField();
  private final StackPane prefix = new StackPane(Icons.node("terminal", 14.0, "#9ca3af"));
  private final Button aiToggle = new Button();
  private final List<String> history = new ArrayList<>();
  private int historyIndex;

  private final Runnable onAiToggle;

  public CommandBar(
      LauncherConfig config,
      Consumer<String> onSubmit,
      Runnable onAiToggle,
      Runnable onOpenKeySettings) {
    super(8.0);
    this.config = config;
    this.onAiToggle = onAiToggle;

    prefix.setPrefSize(18.0, 18.0);
    prefix.setMinSize(18.0, 18.0);
    prefix.setMaxSize(18.0, 18.0);

    field.setPromptText("命令：help 查看启动器命令；/give @s diamond 64 发送到游戏");
    field.setPrefHeight(32.0);
    field.setStyle(
        "-fx-background-color: -app-input; -fx-text-fill: -app-text;"
            + " -fx-prompt-text-fill: -app-text-muted; -fx-background-radius: 10;"
            + " -fx-border-color: -app-border-soft; -fx-border-radius: 10;"
            + " -fx-padding: 6 10; -fx-font-size: 12px; -fx-font-family: 'Microsoft"
            + " YaHei UI', 'Segoe UI';");
    HBox.setHgrow(field, Priority.ALWAYS);

    Button send = new Button("发送");
    send.getStyleClass().addAll("btn-primary", "btn-sm");
    Motion.press(send);

    Runnable submit =
        () -> {
          String text = field.getText();
          if (text == null || text.isBlank()) {
            return;
          }
          field.clear();
          if (onSubmit != null) {
            onSubmit.accept(text);
          }
        };

    Popup completePopup = new Popup();
    completePopup.setAutoHide(true);
    completePopup.setAutoFix(true);
    ListView<CommandCompleter.Completion> completeList = new ListView<>();
    completeList.setMaxHeight(190.0);
    completeList.setFocusTraversable(false);
    completeList.setStyle(
        "-fx-background-color: #1b1e24; -fx-control-inner-background: #1b1e24;"
            + " -fx-background-radius: 10; -fx-border-color: #333a46;"
            + " -fx-border-radius: 10;");
    completeList.setCellFactory(
        view ->
            new ListCell<CommandCompleter.Completion>() {
              @Override
              protected void updateItem(CommandCompleter.Completion item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                  setGraphic(null);
                  return;
                }
                Label value = new Label(item.value());
                value.setStyle(
                    "-fx-text-fill: #e8eaee; -fx-font-size: 12px;"
                        + " -fx-font-family: 'Cascadia Mono', 'Consolas',"
                        + " monospace;");
                Label desc = new Label(item.description());
                desc.setStyle("-fx-text-fill: #7b8494; -fx-font-size: 10px;");
                HBox box = new HBox(10.0, value, desc);
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box);
              }
            });
    completePopup.getContent().add(completeList);

    Consumer<CommandCompleter.Completion> applyCompletion =
        completion -> {
          field.setText(CommandCompleter.apply(field.getText(), completion.value()));
          field.positionCaret(field.getText().length());
          completePopup.hide();
        };
    Consumer<Integer> moveSelection =
        step -> {
          int size = completeList.getItems().size();
          if (size == 0) {
            return;
          }
          int index = completeList.getSelectionModel().getSelectedIndex();
          index = Math.floorMod(index + step, size);
          completeList.getSelectionModel().select(index);
          completeList.scrollTo(index);
        };

    field.setOnAction(e -> submit.run());
    send.setOnAction(e -> submit.run());
    field
        .textProperty()
        .addListener(
            (o, a, b) -> {
              if (completePopup.isShowing()) {
                completePopup.hide();
              }
            });
    completeList.setOnMouseClicked(
        e -> {
          CommandCompleter.Completion item = completeList.getSelectionModel().getSelectedItem();
          if (item != null) {
            applyCompletion.accept(item);
          }
        });
    field.setOnKeyPressed(
        e -> {
          if (config.isAiEnabled()) {
            return;
          }
          KeyCode code = e.getCode();
          if (code == KeyCode.TAB) {
            if (completePopup.isShowing()) {
              moveSelection.accept(1);
              e.consume();
              return;
            }
            List<CommandCompleter.Completion> items =
                CommandCompleter.complete(field.getText(), new File(config.getGameDir()));
            if (items.isEmpty()) {
              return;
            }
            if (items.size() == 1) {
              applyCompletion.accept(items.get(0));
              e.consume();
              return;
            }
            completeList.getItems().setAll(items);
            completeList.setPrefWidth(Math.max(340.0, field.getWidth()));
            completeList.setPrefHeight(Math.min(190, items.size() * 28 + 8));
            completeList.getSelectionModel().select(0);
            Bounds bounds = field.localToScreen(field.getBoundsInLocal());
            if (bounds != null) {
              completePopup.show(field, bounds.getMinX(), bounds.getMaxY() + 4.0);
            }
            e.consume();
            return;
          }
          if (code == KeyCode.ESCAPE && completePopup.isShowing()) {
            completePopup.hide();
            e.consume();
            return;
          }
          if (completePopup.isShowing() && (code == KeyCode.UP || code == KeyCode.DOWN)) {
            moveSelection.accept(code == KeyCode.DOWN ? 1 : -1);
            e.consume();
            return;
          }
          if (completePopup.isShowing() && code == KeyCode.ENTER) {
            CommandCompleter.Completion item = completeList.getSelectionModel().getSelectedItem();
            if (item != null) {
              applyCompletion.accept(item);
            }
            e.consume();
            return;
          }
          if (history.isEmpty()) {
            return;
          }
          if (code == KeyCode.UP) {
            historyIndex = Math.max(0, historyIndex - 1);
            field.setText(history.get(historyIndex));
            field.positionCaret(field.getText().length());
            e.consume();
          } else if (code == KeyCode.DOWN) {
            historyIndex = Math.min(history.size(), historyIndex + 1);
            field.setText(historyIndex >= history.size() ? "" : history.get(historyIndex));
            field.positionCaret(field.getText().length());
            e.consume();
          }
        });

    aiToggle.setMinWidth(Double.NEGATIVE_INFINITY);
    aiToggle.setTooltip(new Tooltip("开启后命令栏内容发给 DeepSeek；关闭后作为启动器 / 游戏命令"));
    aiToggle.setOnAction(
        e -> {
          config.setAiEnabled(!config.isAiEnabled());
          config.save();
          if (onAiToggle != null) {
            onAiToggle.run();
          }
        });

    Button apiKey = new Button("密钥");
    apiKey.setTooltip(new Tooltip("前往 设置 → AI 填写 DeepSeek API Key"));
    apiKey.getStyleClass().add("icon-ghost");
    apiKey.setStyle("-fx-text-fill: -app-text-dim; -fx-font-size: 11px;");
    apiKey.setOnAction(
        e -> {
          if (onOpenKeySettings != null) {
            onOpenKeySettings.run();
          }
        });

    getChildren().addAll(aiToggle, apiKey, prefix, field, send);
    setAlignment(Pos.CENTER_LEFT);
    setPadding(new Insets(5.0, 6.0, 5.0, 6.0));
    setStyle(
        "-fx-background-color: -app-bg-2; -fx-background-radius: 0 0 12 12;"
            + " -fx-border-color: rgba(127,127,127,0.18) transparent transparent"
            + " transparent; -fx-border-width: 1 0 0 0;");
    setAiMode(config.isAiEnabled());
  }

  /** 按 AI 开关状态更新按钮、前缀图标和输入提示。 */
  public void setAiMode(boolean on) {
    aiToggle.setText(on ? "AI 开" : "AI 关");
    aiToggle.setGraphic(Icons.node(on ? "sparkles" : "terminal", 12.0, on ? "#111827" : "#9ca3af"));
    aiToggle.setContentDisplay(ContentDisplay.LEFT);
    aiToggle.setGraphicTextGap(5.0);
    aiToggle.setStyle(
        on
            ? "-fx-background-color: -app-accent; -fx-text-fill: #06251f;"
                + " -fx-font-weight: bold; -fx-background-radius: 8;"
                + " -fx-padding: 5 10; -fx-font-size: 11px; -fx-cursor: hand;"
            : "-fx-background-color: transparent; -fx-text-fill: -app-text-2;"
                + " -fx-border-color: -app-border-3; -fx-border-radius: 8;"
                + " -fx-background-radius: 8; -fx-padding: 5 10;"
                + " -fx-font-size: 11px; -fx-cursor: hand;");
    prefix
        .getChildren()
        .setAll(Icons.node(on ? "sparkles" : "terminal", 14.0, on ? "#d1d5db" : "#9ca3af"));
    field.setPromptText(
        on
            ? (config.getAiApiKey().isBlank()
                ? "本地免费助手：问启动器 / MC 问题；设置 → AI 可填 Key"
                : "问 DeepSeek：例如「这个报错怎么解决？」")
            : "命令：help 查看启动器命令；/give @s diamond 64 发送到游戏");
  }

  /** 记录一条已执行的命令，供上下键翻历史。 */
  public void pushHistory(String text) {
    if (text == null || text.isBlank()) {
      return;
    }
    if (history.isEmpty() || !history.get(history.size() - 1).equals(text)) {
      history.add(text);
    }
    historyIndex = history.size();
  }

  /** 输入框本身，主类聚焦时用。 */
  public TextField field() {
    return field;
  }
}
