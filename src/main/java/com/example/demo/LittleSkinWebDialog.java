package com.example.demo;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Window;

/** 在启动器内打开 LittleSkin 材质管理（内嵌 WebView）。 */
public final class LittleSkinWebDialog {

    private LittleSkinWebDialog() {
    }

    public static void show(Window owner) {
        javafx.scene.control.Dialog<Void> dialog = new javafx.scene.control.Dialog<>();
        if (owner != null) dialog.initOwner(owner);
        dialog.setTitle("LittleSkin 材质管理");

        Label hint = new Label("登录 LittleSkin 后即可在下方直接上传皮肤和披风；完成后关闭窗口。");
        hint.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11px;");
        hint.setWrapText(true);

        WebView web = new WebView();
        web.setPrefSize(920, 620);
        WebEngine engine = web.getEngine();
        engine.load("https://littleskin.cn/user/player");

        Button back = new Button("后退");
        back.setOnAction(e -> {
            if (engine.getHistory().getCurrentIndex() > 0) engine.getHistory().go(-1);
        });
        Button refresh = new Button("刷新");
        refresh.setOnAction(e -> engine.reload());
        Button home = new Button("角色材质页");
        home.setOnAction(e -> engine.load("https://littleskin.cn/user/player"));
        HBox tools = new HBox(8, back, refresh, home);
        tools.setAlignment(Pos.CENTER_LEFT);

        VBox content = new VBox(8, hint, tools, web);
        VBox.setVgrow(web, Priority.ALWAYS);
        content.setPadding(new Insets(4));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(javafx.scene.control.ButtonType.CLOSE);
        if (owner != null && owner.getScene() != null) {
            dialog.getDialogPane().getStylesheets().addAll(owner.getScene().getStylesheets());
        }
        dialog.showAndWait();
    }
}
