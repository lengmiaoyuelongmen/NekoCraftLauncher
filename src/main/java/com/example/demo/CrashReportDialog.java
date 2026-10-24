package com.example.demo;

import javafx.event.ActionEvent;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.io.File;

/** 崩溃诊断弹窗：黑底白字，给原因和建议，可复制/打开日志。 */
public final class CrashReportDialog {

    private CrashReportDialog() {
    }

    public static void show(Window owner, CrashAnalyzer.Report report) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("崩溃诊断");
        if (owner != null) dialog.initOwner(owner);
        if (HelloApplication.THEME_URL != null) {
            dialog.getDialogPane().getStylesheets().add(HelloApplication.THEME_URL);
        }

        VBox content = new VBox(10);
        content.setStyle("-fx-padding: 16; -fx-background-color: -app-bg-2;");
        Label summary = new Label(report.summary());
        summary.setWrapText(true);
        summary.setStyle("-fx-text-fill: -app-text; -fx-font-size: 15px; -fx-font-weight: bold;");
        content.getChildren().add(summary);
        if (report.source() != null) {
            Label source = new Label(report.source().getAbsolutePath());
            source.setWrapText(true);
            source.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11px;");
            content.getChildren().add(source);
        }
        for (CrashAnalyzer.Finding finding : report.findings()) {
            Label title = new Label(finding.title());
            title.setStyle("-fx-text-fill: -app-text-2; -fx-font-size: 13px; -fx-font-weight: bold;");
            Label detail = new Label(finding.detail());
            detail.setWrapText(true);
            detail.setStyle("-fx-text-fill: -app-text-dim; -fx-font-size: 12px;");
            Label suggestion = new Label("建议：" + finding.suggestion());
            suggestion.setWrapText(true);
            suggestion.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 12px;");
            content.getChildren().addAll(title, detail, suggestion);
        }
        if (report.findings().isEmpty()) {
            Label empty = new Label("可以打开日志查看完整堆栈，或把日志发到反馈群。");
            empty.setWrapText(true);
            empty.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 12px;");
            content.getChildren().add(empty);
        }

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setPrefSize(600, 360);
        scroll.setStyle("-fx-background: -app-bg-2; -fx-background-color: -app-bg-2;");
        dialog.getDialogPane().setContent(scroll);

        ButtonType copy = new ButtonType("复制诊断", ButtonBar.ButtonData.OTHER);
        ButtonType open = new ButtonType("打开日志", ButtonBar.ButtonData.OTHER);
        dialog.getDialogPane().getButtonTypes().addAll(copy, open, ButtonType.CLOSE);
        dialog.getDialogPane().lookupButton(copy).addEventFilter(ActionEvent.ACTION, event -> {
            ClipboardContent clipboard = new ClipboardContent();
            clipboard.putString(CrashAnalyzer.toText(report));
            Clipboard.getSystemClipboard().setContent(clipboard);
            Toast.ok("诊断信息已复制");
            event.consume();
        });
        dialog.getDialogPane().lookupButton(open).addEventFilter(ActionEvent.ACTION, event -> {
            if (report.source() != null) {
                try {
                    java.awt.Desktop.getDesktop().open(report.source().getParentFile());
                } catch (Exception e) {
                    Toast.error("打开失败：" + e.getMessage());
                }
            }
            event.consume();
        });
        dialog.showAndWait();
    }
}
