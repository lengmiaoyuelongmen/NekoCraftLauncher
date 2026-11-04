package com.example.demo;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.List;

/** 崩溃诊断弹窗：给原因和建议，可复制/打开日志。 */
public final class CrashReportDialog {

    private CrashReportDialog() {
    }

    /** 旧入口：有集成式宿主就渲染在主窗口里。 */
    public static void show(Window owner, CrashAnalyzer.Report report) {
        StackPane host = InAppDialog.globalHost();
        if (host != null) {
            showInApp(host, report);
            return;
        }
        InAppDialog.customSized(null, "崩溃诊断", buildContent(report), actions(report), null, 640, 460);
    }

    public static void showInApp(StackPane host, CrashAnalyzer.Report report) {
        InAppDialog.customSized(host, "崩溃诊断", buildContent(report), actions(report), null, 640, 460);
    }

    private static List<InAppDialog.Action> actions(CrashAnalyzer.Report report) {
        return List.of(
                InAppDialog.Action.secondary(
                        "打开日志",
                        () -> {
                            if (report.source() != null) {
                                try {
                                    java.awt.Desktop.getDesktop().open(report.source().getParentFile());
                                } catch (Exception e) {
                                    Toast.error("打开失败：" + e.getMessage());
                                }
                            }
                        }),
                InAppDialog.Action.secondary(
                        "复制诊断",
                        () -> {
                            ClipboardContent clipboard = new ClipboardContent();
                            clipboard.putString(CrashAnalyzer.toText(report));
                            Clipboard.getSystemClipboard().setContent(clipboard);
                            Toast.ok("诊断信息已复制");
                        }),
                InAppDialog.Action.primary("知道了", null));
    }

    private static Node buildContent(CrashAnalyzer.Report report) {
        VBox content = new VBox(10);
        content.setStyle("-fx-padding: 4;");
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
        scroll.setPrefSize(580, 340);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        return scroll;
    }
}
