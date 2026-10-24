package com.example.demo;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Window;

import java.util.Optional;

/** 统一样式的输入/确认框（跟随启动器暗色主题）。 */
public final class Dialogs {

    private Dialogs() {
    }

    public static Optional<String> ask(Window owner, String title, String header, String initial) {
        TextInputDialog dialog = new TextInputDialog(initial == null ? "" : initial);
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        dialog.setContentText("");
        if (owner != null) dialog.initOwner(owner);
        if (HelloApplication.THEME_URL != null) {
            dialog.getDialogPane().getStylesheets().add(HelloApplication.THEME_URL);
        }
        return dialog.showAndWait();
    }

    public static boolean confirm(Window owner, String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        if (owner != null) alert.initOwner(owner);
        if (HelloApplication.THEME_URL != null) {
            alert.getDialogPane().getStylesheets().add(HelloApplication.THEME_URL);
        }
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
}
