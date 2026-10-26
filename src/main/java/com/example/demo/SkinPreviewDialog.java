package com.example.demo;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;

/** 本地皮肤/披风预览，不上传材质站。 */
public final class SkinPreviewDialog {

    private SkinPreviewDialog() {
    }

    public static void show(Window owner, LauncherConfig config, String accountSkinUrl) {
        Dialog<Void> dialog = new Dialog<>();
        if (owner != null) dialog.initOwner(owner);
        dialog.setTitle("皮肤 / 披风预览");
        dialog.setHeaderText("本地预览，不会上传到皮肤站");

        Canvas canvas = new Canvas(320, 380);
        Image[] skin = {load(config.getPreviewSkinPath())};
        Image[] cape = {load(config.getPreviewCapePath())};
        Runnable redraw = () -> draw(canvas, skin[0], cape[0]);
        redraw.run();

        Label hint = new Label();
        hint.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11px;");
        hint.setWrapText(true);

        Button chooseSkin = new Button("选择皮肤 PNG");
        chooseSkin.setOnAction(e -> {
            File file = chooser(owner, "选择皮肤");
            if (file == null) return;
            Image image = load(file.getAbsolutePath());
            if (image == null) return;
            skin[0] = image;
            config.setPreviewSkinPath(file.getAbsolutePath());
            config.save();
            redraw.run();
            hint.setText("已载入皮肤：" + file.getName());
        });

        Button chooseCape = new Button("选择披风 PNG");
        chooseCape.setOnAction(e -> {
            File file = chooser(owner, "选择披风");
            if (file == null) return;
            Image image = load(file.getAbsolutePath());
            if (image == null) return;
            cape[0] = image;
            config.setPreviewCapePath(file.getAbsolutePath());
            config.save();
            redraw.run();
            hint.setText("已载入披风：" + file.getName());
        });

        Button account = new Button("使用当前账号皮肤");
        account.setDisable(accountSkinUrl == null || accountSkinUrl.isBlank());
        account.setOnAction(e -> {
            Image image = new Image(accountSkinUrl, true);
            image.progressProperty().addListener((obs, old, value) -> {
                if (value.doubleValue() >= 1 && !image.isError()) {
                    skin[0] = image;
                    redraw.run();
                    hint.setText("已载入当前账号皮肤");
                }
            });
        });

        Button clear = new Button("清除");
        clear.setOnAction(e -> {
            skin[0] = null;
            cape[0] = null;
            config.setPreviewSkinPath("");
            config.setPreviewCapePath("");
            config.save();
            redraw.run();
            hint.setText("已清除预览");
        });

        VBox controls = new VBox(10, new Label("模型预览"), chooseSkin, chooseCape, account, clear, hint);
        controls.setPrefWidth(230);
        HBox content = new HBox(18, canvas, controls);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(javafx.scene.control.ButtonType.CLOSE);
        dialog.getDialogPane().getStyleClass().add("dialog-pane");
        if (owner != null && owner.getScene() != null) {
            dialog.getDialogPane().getStylesheets().addAll(owner.getScene().getStylesheets());
        }
        redraw.run();
        dialog.showAndWait();
    }

    private static File chooser(Window owner, String title) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG 图片", "*.png"));
        return chooser.showOpenDialog(owner);
    }

    private static Image load(String path) {
        if (path == null || path.isBlank()) return null;
        File file = new File(path);
        if (!file.isFile()) return null;
        Image image = new Image(file.toURI().toString());
        return image.isError() ? null : image;
    }

    private static void draw(Canvas canvas, Image skin, Image cape) {
        GraphicsContext g = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        g.clearRect(0, 0, w, h);
        g.setFill(Color.web("#e8f8df"));
        g.fillRoundRect(0, 0, w, h, 22, 22);
        g.setFill(Color.web("#7aa76b"));
        g.fillText("Neko Skin Preview", 20, 24);

        double unit = 9.0;
        double cx = 138;
        double top = 58;
        if (cape != null) {
            g.save();
            g.setGlobalAlpha(0.95);
            g.drawImage(cape, 0, 0, cape.getWidth(), cape.getHeight(),
                    cx + 74, top + 44, 62, 104);
            g.restore();
        }
        if (skin == null) {
            g.setFill(Color.web("#b8c4b5"));
            g.fillRoundRect(cx - 36, top + 4, 80, 80, 10, 10);
            g.fillRoundRect(cx - 36, top + 88, 80, 112, 10, 10);
            g.fillRoundRect(cx - 64, top + 88, 24, 100, 8, 8);
            g.fillRoundRect(cx + 48, top + 88, 24, 100, 8, 8);
            g.fillRoundRect(cx - 36, top + 204, 34, 104, 8, 8);
            g.fillRoundRect(cx + 4, top + 204, 34, 104, 8, 8);
            g.setFill(Color.web("#566b52"));
            g.fillText("选择皮肤 PNG 开始预览", 74, 365);
            return;
        }

        double tex = skin.getWidth() / 64.0;
        part(g, skin, tex, 4, 20, 4, 12, cx - 54, top + 204, unit);   // right leg
        part(g, skin, tex, 20, 52, 4, 12, cx + 18, top + 204, unit);  // left leg
        part(g, skin, tex, 20, 20, 8, 12, cx - 36, top + 88, unit);   // body
        part(g, skin, tex, 44, 20, 4, 12, cx + 36, top + 88, unit);   // right arm
        part(g, skin, tex, 36, 52, 4, 12, cx - 72, top + 88, unit);   // left arm
        part(g, skin, tex, 8, 8, 8, 8, cx - 36, top + 4, unit);       // head
        part(g, skin, tex, 40, 8, 8, 8, cx - 36, top + 4, unit);      // hat overlay
        g.setFill(Color.web("#566b52"));
        g.fillText("本地预览 · 不会上传", 82, 365);
    }

    private static void part(GraphicsContext g, Image skin, double tex,
                             int u, int v, int sw, int sh,
                             double x, double y, double scale) {
        g.drawImage(skin, u * tex, v * tex, sw * tex, sh * tex,
                x, y, sw * scale, sh * scale);
    }
}
