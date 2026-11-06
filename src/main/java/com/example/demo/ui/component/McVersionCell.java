package com.example.demo.ui.component;

import com.example.demo.core.model.MinecraftVersion;
import com.example.demo.core.meta.VersionProfile;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.File;
import java.util.function.Consumer;

/** 版本列表里的一行：图标 + 版本号（含已安装标记）+ 类型徽标 + 详情按钮。 */
public final class McVersionCell extends ListCell<MinecraftVersion> {

    private final Consumer<MinecraftVersion> openDetail;
    private final String gameDir;
    private final HBox card = new HBox(20.0);
    private final StackPane blockPane = new StackPane();
    private final Label title = new Label();
    private final Label typeLabel = new Label();
    private final Button detailBtn = new Button("详情");

    public McVersionCell(String gameDir, Consumer<MinecraftVersion> openDetail) {
        this.gameDir = gameDir;
        this.openDetail = openDetail;

        card.setPadding(new Insets(15.0, 20.0, 15.0, 20.0));
        card.setStyle("-fx-background-color: -app-hover-2; -fx-background-radius: 8;");
        card.setCursor(Cursor.HAND);
        card.getStyleClass().add("mod-card");
        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color: #333; -fx-background-radius: 8;"));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color: -app-hover-2; -fx-background-radius: 8;"));
        blockPane.setPrefSize(50.0, 50.0);

        VBox info = new VBox(6.0);
        title.setStyle("-fx-text-fill: -app-text-strong; -fx-font-size: 18px; -fx-font-weight: bold;");
        typeLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 2 8;"
                + " -fx-background-radius: 4;");
        info.getChildren().addAll(title, typeLabel);
        HBox.setHgrow(info, Priority.ALWAYS);

        detailBtn.setStyle("-fx-background-color: #9ca3af; -fx-text-fill: #111827;"
                + " -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand;"
                + " -fx-padding: 8 22;");
        detailBtn.setMinWidth(Double.NEGATIVE_INFINITY);
        detailBtn.setOnAction(e -> {
            if (getItem() != null) {
                openDetail.accept(getItem());
            }
        });
        card.setOnMouseClicked(e -> {
            if (getItem() != null) {
                openDetail.accept(getItem());
            }
        });
        card.getChildren().addAll(blockPane, info, detailBtn);
    }

    @Override
    protected void updateItem(MinecraftVersion version, boolean empty) {
        super.updateItem(version, empty);
        if (empty || version == null) {
            setGraphic(null);
            return;
        }
        boolean installed = VersionProfile.isInstalled(new File(gameDir), version.getId());
        title.setText(version.getId() + (installed ? "   · 已安装" : ""));
        blockPane.getChildren().clear();
        if ("release".equals(version.getType())) {
            typeLabel.setText("正式版");
            typeLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 12px;"
                    + " -fx-font-weight: bold; -fx-background-color: rgba(76, 175, 80, 0.1);"
                    + " -fx-padding: 2 8; -fx-background-radius: 4;");
        } else {
            typeLabel.setText("快照版");
            typeLabel.setStyle("-fx-text-fill: #FF9800; -fx-font-size: 12px;"
                    + " -fx-font-weight: bold; -fx-background-color: rgba(255, 152, 0, 0.1);"
                    + " -fx-padding: 2 8; -fx-background-radius: 4;");
        }
        blockPane.getChildren().add(BlockIcons.create(version.getType()));
        detailBtn.setText("详情");
        detailBtn.setDisable(false);
        setGraphic(card);
    }
}
