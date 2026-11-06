package com.example.demo.ui.shell;

import javafx.geometry.Rectangle2D;
import javafx.scene.control.Dialog;
import javafx.stage.Screen;

/** 让弹窗不超过当前屏幕可用区域。 */
public final class UiFit {
    private UiFit() {
    }

    public static void dialog(Dialog<?> dialog, double prefWidth, double prefHeight) {
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        double width = Math.max(420, Math.min(prefWidth, bounds.getWidth() - 60));
        double height = Math.max(320, Math.min(prefHeight, bounds.getHeight() - 120));
        dialog.getDialogPane().setPrefWidth(width);
        dialog.getDialogPane().setPrefHeight(height);
        dialog.getDialogPane().setMaxWidth(width);
        dialog.getDialogPane().setMaxHeight(height);
    }
}
