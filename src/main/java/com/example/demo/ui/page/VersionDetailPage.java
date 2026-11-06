package com.example.demo.ui.page;

import com.example.demo.core.meta.ReleaseTime;
import com.example.demo.core.meta.VersionProfile;
import com.example.demo.ui.component.BlockIcons;
import com.example.demo.ui.shell.Icons;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.File;

import com.example.demo.core.model.MinecraftVersion;
/**
 * 版本详情页：图标 + 版本号 + 安装状态 + 元信息 + 「直接安装加载器」快捷入口。
 *
 * <p>从主类拆出来的第二块页面级代码。页面本身不碰下载/启动逻辑，
 * 只把三个动作转给 {@link Host}：返回列表、安装（可带加载器）、启动该版本。
 */
public final class VersionDetailPage {

    public interface Host {
        /** 游戏根目录（用来显示实例目录、判断是否已安装）。 */
        String gameDir();

        /** 返回版本列表。 */
        void backToList();

        /** 安装此版本；{@code loader} 为 null 表示原版安装流程。 */
        void openInstaller(MinecraftVersion version, String loader);

        /** 启动已安装的这个版本。 */
        void launchVersion(String versionId);
    }

    private final Host host;

    public VersionDetailPage(Host host) {
        this.host = host;
    }

    public VBox build(MinecraftVersion version) {
        File gameDir = new File(host.gameDir());
        boolean installed = VersionProfile.isInstalled(gameDir, version.getId());

        Button back = new Button("返回列表");
        back.setGraphic(Icons.node("back", 13.0, "#9ca3af"));
        back.setGraphicTextGap(4.0);
        back.setStyle("-fx-background-color: transparent; -fx-text-fill: -app-text-dim;"
                + " -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 2 4;");
        back.setOnAction(e -> host.backToList());

        Label heading = new Label("版本详情");
        heading.setStyle("-fx-text-fill: -app-text; -fx-font-size: 18px; -fx-font-weight: bold;");
        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);
        HBox top = new HBox(10.0, back, heading, topSpacer);
        top.setAlignment(Pos.CENTER_LEFT);

        StackPane icon = BlockIcons.create(version.getType());
        icon.setPrefSize(64.0, 64.0);
        icon.setMinSize(64.0, 64.0);
        icon.setMaxSize(64.0, 64.0);

        Label title = new Label(version.getId());
        title.setStyle("-fx-text-fill: -app-text-strong; -fx-font-size: 24px; -fx-font-weight: bold;");
        Label badge = new Label("release".equals(version.getType()) ? "正式版" : "预览版");
        badge.setStyle("-fx-text-fill: "
                + ("release".equals(version.getType()) ? "#4CAF50" : "#FF9800")
                + "; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-color:"
                + " rgba(255,255,255,0.06); -fx-padding: 3 9; -fx-background-radius: 6;");
        HBox titleRow = new HBox(10.0, title, badge);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        String time = ReleaseTime.format(version.getReleaseTime());
        Label subtitle = new Label(
                (time.isEmpty() ? "Minecraft " + version.getId() : "发布于 " + time)
                        + " · " + (installed ? "已安装" : "未安装"));
        subtitle.setStyle("-fx-text-fill: -app-text-dim-2; -fx-font-size: 12px;");
        VBox heroInfo = new VBox(5.0, titleRow, subtitle);
        heroInfo.setAlignment(Pos.CENTER_LEFT);

        Button install = new Button(installed ? "重新安装" : "安装此版本");
        install.setStyle("-fx-background-color: linear-gradient(to bottom right, #d1d5db, #9ca3af);"
                + " -fx-text-fill: #111827; -fx-font-weight: bold; -fx-background-radius: 9;"
                + " -fx-padding: 8 18; -fx-cursor: hand;");
        install.setOnAction(e -> host.openInstaller(version, null));

        Button launch = new Button("启动此版本");
        launch.setStyle("-fx-background-color: transparent; -fx-text-fill: -app-text-2;"
                + " -fx-border-color: -app-border-3; -fx-border-radius: 9;"
                + " -fx-background-radius: 9; -fx-padding: 8 18; -fx-cursor: hand;");
        launch.setDisable(!installed);
        launch.setOnAction(e -> host.launchVersion(version.getId()));

        HBox actions = new HBox(10.0, install, launch);
        actions.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(heroInfo, Priority.ALWAYS);
        HBox hero = new HBox(18.0, icon, heroInfo, actions);
        hero.setAlignment(Pos.CENTER_LEFT);
        hero.setPadding(new Insets(18.0));
        hero.setStyle("-fx-background-color: -app-card; -fx-background-radius: 16;"
                + " -fx-border-color: -app-border-2; -fx-border-radius: 14;");

        VBox meta = new VBox(8.0,
                detailRow("版本 ID", version.getId()),
                detailRow("类型", "release".equals(version.getType()) ? "正式版" : "快照 / 预览版"),
                detailRow("发布时间", time.isEmpty() ? "未知" : time),
                detailRow("安装状态", installed ? "已安装" : "未安装"),
                detailRow("实例目录",
                        new File(new File(gameDir, "versions"), version.getId()).getAbsolutePath()));
        meta.setPadding(new Insets(16.0));
        meta.setStyle("-fx-background-color: -app-card-alt; -fx-background-radius: 12;"
                + " -fx-border-color: -app-border-2; -fx-border-radius: 12;");

        Label loaderTitle = new Label("直接安装加载器");
        loaderTitle.setStyle("-fx-text-fill: -app-text-2; -fx-font-size: 12px; -fx-font-weight: bold;");
        HBox loaderChips = new HBox(8.0);
        for (String loader : new String[]{"Fabric", "Forge", "NeoForge", "Quilt", "原版"}) {
            Button chip = new Button(loader);
            chip.setStyle("-fx-background-color: -app-row; -fx-text-fill: -app-text-2;"
                    + " -fx-background-radius: 8; -fx-border-color: -app-border-soft;"
                    + " -fx-border-radius: 8; -fx-padding: 6 12; -fx-font-size: 11.5px;"
                    + " -fx-cursor: hand;");
            chip.setOnAction(e -> host.openInstaller(version, loader));
            loaderChips.getChildren().add(chip);
        }
        VBox loaderCard = new VBox(10.0, loaderTitle, loaderChips);
        loaderCard.setPadding(new Insets(14.0));
        loaderCard.setStyle("-fx-background-color: -app-card; -fx-background-radius: 12;"
                + " -fx-border-color: -app-border-2; -fx-border-radius: 12;");

        return new VBox(10.0, top, hero, meta, loaderCard);
    }

    private HBox detailRow(String label, String value) {
        Label key = new Label(label);
        key.setMinWidth(74.0);
        key.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 11px;");
        Label val = new Label(value);
        val.setStyle("-fx-text-fill: -app-text-2; -fx-font-size: 12px;");
        val.setWrapText(true);
        HBox row = new HBox(12.0, key, val);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }
}
