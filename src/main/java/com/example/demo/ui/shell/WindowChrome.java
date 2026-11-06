package com.example.demo.ui.shell;

import com.example.demo.core.app.LauncherConfig;
import javafx.application.Platform;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 窗口收尾：关闭行为（询问 / 托盘 / 直接退出）、系统托盘、托盘通知。
 *
 * <p>从主类里搬出来，主类只保留「真正退出」这一步（杀游戏、释放单例锁、退出进程），
 * 通过 {@link Host#quit()} 回调。这样窗口生命周期和业务逻辑分开，托盘代码也不再淹没在 8k 行里。
 */
public final class WindowChrome {

    /** 主类需要提供的最小能力。 */
    public interface Host {
        LauncherConfig config();

        Stage stage();

        StackPane dialogLayer();

        boolean gameRunning();

        void log(String message);

        /** 真正退出：结束游戏进程、释放单例锁、Platform.exit。 */
        void quit();
    }

    private final Host host;
    private boolean trayInstalled;
    private TrayIcon trayIcon;

    public WindowChrome(Host host) {
        this.host = host;
    }

    /** 用户点了窗口关闭按钮（或 Alt+F4）：按配置决定行为。 */
    public void requestClose() {
        LauncherConfig config = host.config();
        String action = config.getCloseAction();
        if ("tray".equals(action) && installTray()) {
            hideToTray();
            return;
        }
        if ("exit".equals(action)) {
            host.quit();
            return;
        }
        showCloseChoice();
    }

    private void showCloseChoice() {
        if (host.stage() == null) {
            host.quit();
            return;
        }
        boolean canTray = SystemTray.isSupported();
        CheckBox remember = new CheckBox("记住我的选择");
        remember.setStyle("-fx-text-fill: -app-text-2;");
        Label note = new Label(host.gameRunning()
                ? "游戏正在运行。最小化到托盘后，启动器会继续在后台守护进程。"
                : "最小化到托盘后，启动器仍会在后台运行。");
        note.setWrapText(true);
        note.setStyle("-fx-text-fill: -app-text-dim; -fx-font-size: 12px;");
        VBox content = new VBox(10.0, note, remember);

        List<InAppDialog.Action> actions = new ArrayList<>();
        actions.add(InAppDialog.Action.danger("退出启动器", () -> {
            if (remember.isSelected()) {
                host.config().setCloseAction("exit");
                host.config().save();
            }
            host.quit();
        }));
        if (canTray) {
            actions.add(InAppDialog.Action.cyan("最小化到托盘", () -> {
                if (remember.isSelected()) {
                    host.config().setCloseAction("tray");
                    host.config().save();
                }
                if (installTray()) {
                    hideToTray();
                }
            }));
        }
        actions.add(InAppDialog.Action.secondary("取消", null));
        InAppDialog.custom(host.dialogLayer(), "终止启动器？", content, actions, null);
    }

    /** 安装系统托盘图标；不支持或失败时返回 false。 */
    public boolean installTray() {
        if (trayInstalled) {
            return true;
        }
        if (!SystemTray.isSupported()) {
            return false;
        }
        try {
            BufferedImage image = null;
            try (InputStream in = WindowChrome.class.getResourceAsStream("/icon.png")) {
                if (in != null) {
                    image = ImageIO.read(in);
                }
            }
            if (image == null) {
                BufferedImage fallback = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = fallback.createGraphics();
                g.setColor(new Color(225, 29, 46));
                g.fillOval(1, 1, 14, 14);
                g.setColor(new Color(255, 207, 106));
                g.fillOval(6, 6, 4, 4);
                g.dispose();
                image = fallback;
            }
            PopupMenu menu = new PopupMenu();
            MenuItem show = new MenuItem("Show Neko Launcher");
            show.addActionListener(e -> Platform.runLater(this::restoreFromTray));
            MenuItem quit = new MenuItem("Quit");
            quit.addActionListener(e -> Platform.runLater(host::quit));
            menu.add(show);
            menu.addSeparator();
            menu.add(quit);
            trayIcon = new TrayIcon(image, "Neko Craft Launcher", menu);
            trayIcon.setImageAutoSize(true);
            trayIcon.addActionListener(e -> Platform.runLater(this::restoreFromTray));
            SystemTray.getSystemTray().add(trayIcon);
            trayInstalled = true;
            return true;
        } catch (Exception ex) {
            host.log("⚠ 系统托盘不可用: " + ex.getMessage());
            return false;
        }
    }

    public void hideToTray() {
        if (host.stage() == null || trayIcon == null) {
            return;
        }
        host.stage().hide();
        trayIcon.displayMessage("Neko Craft Launcher",
                host.gameRunning() ? "Minimized to tray; game is still running." : "Minimized to tray.",
                TrayIcon.MessageType.INFO);
    }

    public void restoreFromTray() {
        Stage stage = host.stage();
        if (stage == null) {
            return;
        }
        stage.show();
        stage.setIconified(false);
        stage.toFront();
        stage.requestFocus();
    }

    /** 托盘气泡通知（游戏退出时用）；没装托盘就什么都不做。 */
    public void notifyTray(String message, boolean warning) {
        if (trayIcon == null) {
            return;
        }
        trayIcon.displayMessage("Neko Craft Launcher", message,
                warning ? TrayIcon.MessageType.WARNING : TrayIcon.MessageType.INFO);
    }

    /** 退出前把托盘图标摘掉，避免任务栏里留一个死图标。 */
    public void removeTray() {
        if (trayIcon == null) {
            return;
        }
        try {
            SystemTray.getSystemTray().remove(trayIcon);
        } catch (Exception ignored) {
            // 系统托盘已经回收
        }
        trayIcon = null;
        trayInstalled = false;
    }

    public boolean isTrayInstalled() {
        return trayInstalled;
    }
}
