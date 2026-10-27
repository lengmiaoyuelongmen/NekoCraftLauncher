package com.example.demo;

import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.Optional;

/** 统一风格的输入 / 确认框。 */
public final class Dialogs {

    private Dialogs() {
    }

    public static Optional<String> ask(Window owner, String title, String header, String initial) {
        Dialog<String> dialog = new Dialog<>();
        if (owner != null) dialog.initOwner(owner);
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill: -app-text; -fx-font-size: 16px; -fx-font-weight: bold;");
        Label subtitle = new Label(header == null ? "" : header);
        subtitle.setWrapText(true);
        subtitle.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11px;");
        TextField field = new TextField(initial == null ? "" : initial);
        field.setStyle("-fx-background-color: -app-row3; -fx-text-fill: -app-text;"
                + " -fx-prompt-text-fill: -app-text-muted; -fx-background-radius: 9;"
                + " -fx-border-color: -app-border-soft; -fx-border-radius: 9; -fx-padding: 8 10;");
        field.setPrefWidth(360);
        VBox content = new VBox(9, titleLabel, subtitle, field);
        content.setPadding(new Insets(4, 0, 2, 0));
        dialog.getDialogPane().setContent(content);
        ButtonType ok = new ButtonType("保存", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);
        if (HelloApplication.THEME_URL != null) {
            dialog.getDialogPane().getStylesheets().add(HelloApplication.THEME_URL);
        }
        dialog.setResultConverter(button -> button == ok ? field.getText() : null);
        javafx.application.Platform.runLater(() -> {
            field.requestFocus();
            field.selectAll();
        });
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
