package com.example.demo;

import javafx.geometry.Pos;
import javafx.scene.*;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.transform.Rotate;

/** JavaFX 3D 皮肤预览：可拖动旋转、滚轮缩放。 */
public final class Skin3DPreview extends StackPane {

    private final Group model = new Group();
    private final Group world = new Group();
    private final Rotate rotateY = new Rotate(0, Rotate.Y_AXIS);
    private final Rotate rotateX = new Rotate(0, Rotate.X_AXIS);
    private final Rotate pose = new Rotate(0, Rotate.Y_AXIS);
    private final javafx.scene.transform.Scale zoom = new javafx.scene.transform.Scale(1, 1, 1);
    private double lastX, lastY;
    private Image skin, cape;

    public Skin3DPreview() {
        setPrefSize(340, 440);
        setMinSize(340, 440);
        setMaxSize(340, 440);
        setStyle("-fx-background-color: linear-gradient(to bottom, #f7fcf4, #e9f5e4);"
                + " -fx-background-radius: 16; -fx-border-color: #cfe5c8; -fx-border-radius: 16;");
        SubScene scene = new SubScene(world, 340, 440, true, SceneAntialiasing.BALANCED);
        scene.setFill(Color.TRANSPARENT);
        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setNearClip(0.1);
        camera.setFarClip(5000);
        camera.setTranslateZ(-950);
        scene.setCamera(camera);
        world.getChildren().addAll(new AmbientLight(Color.web("#f5fff2")),
                new PointLight(Color.WHITE));
        PointLight light = (PointLight) world.getChildren().get(1);
        light.setTranslateX(-200);
        light.setTranslateY(-250);
        light.setTranslateZ(-300);
        model.getTransforms().addAll(rotateY, rotateX, pose, zoom);
        world.getChildren().add(model);
        getChildren().add(scene);

        javafx.scene.control.Label hint = new javafx.scene.control.Label("拖动旋转 · 滚轮缩放");
        hint.setStyle("-fx-text-fill: rgba(90,130,84,0.65); -fx-font-size: 9px;"
                + " -fx-background-color: rgba(255,255,255,0.55); -fx-background-radius: 8; -fx-padding: 2 7;");
        StackPane.setAlignment(hint, Pos.BOTTOM_CENTER);
        StackPane.setMargin(hint, new javafx.geometry.Insets(0, 0, 8, 0));
        getChildren().add(hint);

        setOnMousePressed(e -> {
            lastX = e.getSceneX();
            lastY = e.getSceneY();
        });
        setOnMouseDragged(e -> {
            rotateY.setAngle(rotateY.getAngle() + (e.getSceneX() - lastX) * 0.45);
            rotateX.setAngle(Math.max(-35, Math.min(35,
                    rotateX.getAngle() - (e.getSceneY() - lastY) * 0.35)));
            lastX = e.getSceneX();
            lastY = e.getSceneY();
        });
        setOnScroll(e -> {
            double factor = e.getDeltaY() > 0 ? 1.08 : 0.92;
            double next = Math.max(0.65, Math.min(1.8, zoom.getX() * factor));
            zoom.setX(next);
            zoom.setY(next);
            zoom.setZ(next);
        });
        buildModel();
    }

    public void setImages(Image skinImage, Image capeImage) {
        this.skin = skinImage;
        this.cape = capeImage;
        buildModel();
    }

    private void buildModel() {
        model.getChildren().clear();
        if (skin == null) {
            Box placeholder = new Box(80, 120, 36);
            placeholder.setMaterial(new PhongMaterial(Color.web("#c5d9bf")));
            placeholder.setTranslateY(20);
            model.getChildren().add(placeholder);
            return;
        }
        double tex = skin.getWidth() / 64.0;
        Box head = part(80, 80, 80, crop(8, 8, 8, 8, tex), -100);
        Box body = part(80, 120, 40, crop(20, 20, 8, 12, tex), 10);
        Box rightArm = part(40, 120, 40, crop(44, 20, 4, 12, tex), 10);
        rightArm.setTranslateX(-60);
        Box leftArm = part(40, 120, 40, crop(36, 52, 4, 12, tex), 10);
        leftArm.setTranslateX(60);
        Box rightLeg = part(40, 120, 40, crop(4, 20, 4, 12, tex), 130);
        rightLeg.setTranslateX(-20);
        Box leftLeg = part(40, 120, 40, crop(20, 52, 4, 12, tex), 130);
        leftLeg.setTranslateX(20);
        model.getChildren().addAll(head, body, rightArm, leftArm, rightLeg, leftLeg);
        if (cape != null) {
            WritableImage capeImage = cropImage(cape, 0, 0,
                    (int) cape.getWidth(), (int) cape.getHeight());
            Box capeBox = new Box(78, 118, 4);
            capeBox.setMaterial(colorMaterial(capeImage));
            capeBox.setTranslateY(10);
            capeBox.setTranslateZ(26);
            model.getChildren().add(capeBox);
        }
        model.setTranslateY(20);
        model.setRotationAxis(Rotate.Y_AXIS);
    }

    private Box part(double w, double h, double d, Image texture, double y) {
        Box box = new Box(w, h, d);
        box.setMaterial(colorMaterial(texture));
        box.setTranslateY(y);
        return box;
    }

    private PhongMaterial colorMaterial(Image texture) {
        PhongMaterial material = new PhongMaterial(Color.WHITE);
        material.setDiffuseMap(texture);
        material.setSpecularColor(Color.web("#444444"));
        material.setSpecularPower(8);
        return material;
    }

    private Image crop(int u, int v, int w, int h, double tex) {
        return cropImage(skin, (int) (u * tex), (int) (v * tex), (int) (w * tex), (int) (h * tex));
    }

    private WritableImage cropImage(Image source, int x, int y, int w, int h) {
        int maxW = (int) source.getWidth();
        int maxH = (int) source.getHeight();
        x = Math.max(0, Math.min(x, maxW - 1));
        y = Math.max(0, Math.min(y, maxH - 1));
        w = Math.max(1, Math.min(w, maxW - x));
        h = Math.max(1, Math.min(h, maxH - y));
        WritableImage out = new WritableImage(w, h);
        out.getPixelReader();
        for (int py = 0; py < h; py++) {
            for (int px = 0; px < w; px++) {
                out.getPixelWriter().setArgb(px, py, source.getPixelReader().getArgb(x + px, y + py));
            }
        }
        return out;
    }
}
