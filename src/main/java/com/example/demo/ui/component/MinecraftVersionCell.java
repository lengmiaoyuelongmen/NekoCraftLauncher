package com.example.demo.ui.component;

import javafx.scene.control.ListCell;
import javafx.scene.paint.Color;

import com.example.demo.core.model.MinecraftVersion;
import com.example.demo.ui.shell.Theme;
/** 版本下拉框的单元格：显示版本 id，按主题给文字上色。 */
public final class MinecraftVersionCell extends ListCell<MinecraftVersion> {

  @Override
  protected void updateItem(MinecraftVersion item, boolean empty) {
    super.updateItem(item, empty);
    if (empty || item == null) {
      setText(null);
      return;
    }
    setText(item.getId());
    setTextFill(Color.web(Theme.isLight() ? "#1f2937" : "#E0E0E0"));
  }
}
