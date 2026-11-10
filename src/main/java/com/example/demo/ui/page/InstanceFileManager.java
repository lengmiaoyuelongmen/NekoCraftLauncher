package com.example.demo.ui.page;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.stage.Window;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import com.example.demo.ui.shell.InAppDialog;
import com.example.demo.ui.shell.Toast;
import com.example.demo.ui.shell.UiFit;
/**
 * 实例文件管理器：WinSCP 风格双栏。
 *
 * <p>左栏是实例目录，右栏是游戏根目录。点击某一栏把它设为「活动栏」（边框高亮），
 * 底部按钮和拖拽都以活动栏为源、另一栏为目标：
 * <ul>
 *   <li>双击文件夹进入，`↑ 上级` 返回
 *   <li>选中若干个文件/文件夹，直接拖到另一边 = 复制
 *   <li>按钮：复制 / 移动 / 删除 / 新建文件夹 / 刷新
 *   <li>快捷键：F5 复制、F6 移动、Delete 删除
 * </ul>
 */
public final class InstanceFileManager {

    private InstanceFileManager() {
    }

    /** 旧入口保留：没有宿主时退回独立窗口。 */
    public static void show(Window owner, File instanceRoot, File baseGameDir) {
        StackPane host = InAppDialog.globalHost();
        if (host != null) {
            showInApp(host, instanceRoot, baseGameDir);
            return;
        }
        Dialog<Void> dialog = new Dialog<>();
        if (owner != null) dialog.initOwner(owner);
        dialog.setTitle("实例文件管理");
        dialog.setHeaderText(null);
        dialog.getDialogPane().setContent(buildContent(instanceRoot, baseGameDir));
        UiFit.dialog(dialog, 1020, 640);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        if (owner != null && owner.getScene() != null) {
            dialog.getDialogPane().getStylesheets().addAll(owner.getScene().getStylesheets());
        }
        dialog.showAndWait();
    }

    /** 集成式：渲染在主窗口的弹窗层里。 */
    public static void showInApp(StackPane host, File instanceRoot, File baseGameDir) {
        if (host == null) host = InAppDialog.globalHost();
        if (host == null) return;
        Node content = buildContent(instanceRoot, baseGameDir);
        ((Region) content).setPrefHeight(480);
        InAppDialog.customSized(
                host,
                "实例文件管理",
                content,
                List.of(InAppDialog.Action.secondary("关闭", null)),
                null,
                980,
                580);
    }

    private static Node buildContent(File instanceRoot, File baseGameDir) {
        // 用单元素数组模拟「活动栏」的可变引用
        FilePane[] active = new FilePane[1];
        List<FilePane> panes = new ArrayList<>();
        FilePane left = new FilePane(instanceRoot, panes, active);
        FilePane right = new FilePane(baseGameDir, panes, active);
        panes.add(left);
        panes.add(right);
        left.activate();

        HBox body = new HBox(12, left.node(), right.node());
        HBox.setHgrow(left.node(), Priority.ALWAYS);
        HBox.setHgrow(right.node(), Priority.ALWAYS);
        VBox.setVgrow(body, Priority.ALWAYS);

        Button copy = toolButton("复制到另一栏 (F5)");
        copy.setOnAction(e -> transfer(active[0], other(active[0], left, right), false));
        Button move = toolButton("移动到另一栏 (F6)");
        move.setOnAction(e -> transfer(active[0], other(active[0], left, right), true));
        Button delete = toolButton("删除 (Del)");
        delete.setOnAction(e -> deleteSelected(active[0]));
        Button newFolder = toolButton("新建文件夹");
        newFolder.setOnAction(e -> createFolder(active[0]));
        Button refresh = toolButton("刷新");
        refresh.setOnAction(e -> {
            left.refresh();
            right.refresh();
        });

        HBox actions = new HBox(8, copy, move, delete, newFolder, refresh);
        actions.setAlignment(Pos.CENTER_LEFT);
        Label hint = new Label("点击选中文件或文件夹，直接拖到另一栏即可复制；双击文件夹进入。");
        hint.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 10.5px;");
        hint.setWrapText(true);

        VBox content = new VBox(8, body, hint, actions);
        content.setPadding(new Insets(4));
        VBox.setVgrow(body, Priority.ALWAYS);

        // 快捷键：F5 复制、F6 移动、Delete 删除
        content.setOnKeyPressed(
                e -> {
                    if (e.getCode() == KeyCode.F5) {
                        transfer(active[0], other(active[0], left, right), false);
                        e.consume();
                    } else if (e.getCode() == KeyCode.F6) {
                        transfer(active[0], other(active[0], left, right), true);
                        e.consume();
                    } else if (e.getCode() == KeyCode.DELETE) {
                        deleteSelected(active[0]);
                        e.consume();
                    }
                });
        content.setFocusTraversable(true);
        return content;
    }

    private static FilePane other(FilePane active, FilePane left, FilePane right) {
        return active == left ? right : left;
    }

    private static Button toolButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().addAll("btn-secondary", "btn-sm");
        return button;
    }

    /** 把源栏选中的内容复制/移动到目标栏当前目录。 */
    private static void transfer(FilePane source, FilePane target, boolean move) {
        if (source == null || target == null) return;
        List<File> selected = source.selected();
        if (selected.isEmpty()) {
            InAppDialog.alert("还没选中文件", "先在左栏或右栏点选要处理的项目（支持多选）。");
            return;
        }
        int ok = 0;
        List<String> failed = new ArrayList<>();
        for (File item : selected) {
            Path from = item.toPath().toAbsolutePath().normalize();
            Path to = new File(target.current(), item.getName()).toPath().toAbsolutePath().normalize();
            if (to.equals(from)) {
                failed.add(item.getName() + "（源和目标是同一位置，左右两栏可能指向同一个目录）");
                continue;
            }
            if (to.startsWith(from)) {
                failed.add(item.getName() + "（不能复制到自身子目录）");
                continue;
            }
            try {
                if (move) {
                    Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
                } else {
                    copyRecursive(from, to);
                }
                ok++;
            } catch (Exception ex) {
                failed.add(item.getName() + "：" + ex.getMessage());
            }
        }
        source.refresh();
        target.refresh();
        if (!failed.isEmpty()) {
            InAppDialog.alert(
                    "部分项目处理失败",
                    "成功 " + ok + " 个，失败 " + failed.size() + " 个：\n" + String.join("\n", failed));
        }
    }

    private static void deleteSelected(FilePane pane) {
        if (pane == null) return;
        List<File> selected = pane.selected();
        if (selected.isEmpty()) {
            InAppDialog.alert("还没选中文件", "先在左栏或右栏点选要删除的项目。");
            return;
        }
        String names = selected.size() == 1 ? selected.get(0).getName() : selected.size() + " 个项目";
        InAppDialog.confirm(
                null,
                "删除 " + names + "？",
                "目录会连同内容一起删除，不可恢复。",
                "删除",
                () -> {
                    int ok = 0;
                    List<String> failedNames = new ArrayList<>();
                    for (File file : selected) {
                        try {
                            deleteRecursive(file.toPath());
                            ok++;
                        } catch (Exception ex) {
                            failedNames.add(file.getName() + "：" + ex.getMessage());
                        }
                    }
                    pane.refresh();
                    if (failedNames.isEmpty()) {
                        Toast.ok("已删除 " + ok + " 项");
                    } else {
                        InAppDialog.alert(
                                "部分文件没删掉",
                                "成功 " + ok + " 项，失败 " + failedNames.size() + " 项（可能被游戏或其它程序占用）：\n"
                                        + String.join("\n", failedNames));
                    }
                });
    }

    private static void createFolder(FilePane pane) {
        if (pane == null) return;
        InAppDialog.input(
                null,
                "新建文件夹",
                "在 " + pane.current().getAbsolutePath() + " 下创建",
                "新建文件夹",
                name -> {
                    if (name == null || name.isBlank()) return;
                    try {
                        Files.createDirectories(new File(pane.current(), name.trim()).toPath());
                        pane.refresh();
                    } catch (Exception ex) {
                        InAppDialog.error(String.valueOf(ex.getMessage()));
                    }
                });
    }

    private static void copyRecursive(Path from, Path to) throws Exception {
        if (Files.isDirectory(from)) {
            Files.createDirectories(to);
            try (var stream = Files.list(from)) {
                for (Path child : stream.toList()) {
                    copyRecursive(child, to.resolve(child.getFileName().toString()));
                }
            }
        } else {
            Files.createDirectories(to.getParent());
            Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void deleteRecursive(Path path) throws Exception {
        if (Files.isDirectory(path)) {
            try (var stream = Files.list(path)) {
                for (Path child : stream.toList()) deleteRecursive(child);
            }
        }
        Files.deleteIfExists(path);
    }

    /** 一栏：路径头 + 多选列表 + 拖拽支持。 */
    private static final class FilePane {
        private static final String DRAG_PREFIX = "neko-paths:";

        private final VBox root = new VBox(6);
        private final Label path = new Label();
        private final ListView<File> list = new ListView<>();
        private File current;
        private final List<FilePane> panes;
        private final FilePane[] activeRef;

        FilePane(File initial, List<FilePane> panes, FilePane[] activeRef) {
            this.current = initial;
            this.panes = panes;
            this.activeRef = activeRef;
            root.setPadding(new Insets(8));
            root.getStyleClass().add("neko-card");

            path.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 10px;");
            path.setWrapText(true);
            Button up = new Button("↑ 上级");
            up.getStyleClass().addAll("btn-secondary", "btn-sm");
            up.setStyle("-fx-font-size: 10px; -fx-padding: 2 8;");
            up.setOnAction(
                    e -> {
                        File parent = current.getParentFile();
                        if (parent != null) {
                            current = parent;
                            refresh();
                        }
                    });
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            HBox top = new HBox(8, path, spacer, up);
            top.setAlignment(Pos.CENTER_LEFT);

            list.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
            list.setCellFactory(
                    v ->
                            new ListCell<>() {
                                @Override
                                protected void updateItem(File file, boolean empty) {
                                    super.updateItem(file, empty);
                                    setText(
                                            empty || file == null
                                                    ? null
                                                    : (file.isDirectory() ? "📁  " : "📄  ")
                                                            + file.getName());
                                    setStyle("-fx-text-fill: -app-text-2; -fx-font-size: 11.5px;");
                                }
                            });
            list.setOnMousePressed(e -> activate());
            list.setOnMouseClicked(
                    e -> {
                        File file = list.getSelectionModel().getSelectedItem();
                        if (e.getClickCount() == 2 && file != null && file.isDirectory()) {
                            current = file;
                            refresh();
                        }
                    });

            // 拖出：把选中的路径写进 Dragboard
            list.setOnDragDetected(
                    e -> {
                        List<File> selected = selected();
                        if (selected.isEmpty()) return;
                        Dragboard dragboard = list.startDragAndDrop(TransferMode.COPY);
                        ClipboardContent content = new ClipboardContent();
                        StringBuilder sb = new StringBuilder(DRAG_PREFIX);
                        for (File file : selected) {
                            sb.append(file.getAbsolutePath()).append('\n');
                        }
                        content.putString(sb.toString());
                        dragboard.setContent(content);
                        e.consume();
                    });
            // 拖入：另一栏拖过来的内容落在当前目录
            list.setOnDragOver(
                    e -> {
                        Dragboard dragboard = e.getDragboard();
                        if (dragboard.hasString() && dragboard.getString().startsWith(DRAG_PREFIX)) {
                            e.acceptTransferModes(TransferMode.COPY);
                        }
                        e.consume();
                    });
            list.setOnDragDropped(
                    e -> {
                        Dragboard dragboard = e.getDragboard();
                        boolean ok = false;
                        if (dragboard.hasString() && dragboard.getString().startsWith(DRAG_PREFIX)) {
                            String[] paths = dragboard.getString().substring(DRAG_PREFIX.length()).split("\n");
                            int copied = 0;
                            List<String> failedNames = new ArrayList<>();
                            for (String p : paths) {
                                if (p.isBlank()) continue;
                                File file = new File(p.trim());
                                if (!file.exists()) continue;
                                Path from = file.toPath().toAbsolutePath().normalize();
                                Path to = new File(current, file.getName()).toPath().toAbsolutePath().normalize();
                                if (to.equals(from) || to.startsWith(from)) {
                                    failedNames.add(file.getName() + "（不能复制到自身或子目录）");
                                    continue;
                                }
                                try {
                                    copyRecursive(from, to);
                                    copied++;
                                } catch (Exception ex) {
                                    failedNames.add(file.getName() + "：" + ex.getMessage());
                                }
                            }
                            refresh();
                            ok = copied > 0;
                            if (!failedNames.isEmpty()) {
                                InAppDialog.alert(
                                        "部分项目没复制成功",
                                        "成功 " + copied + " 项，失败 " + failedNames.size() + " 项：\n"
                                                + String.join("\n", failedNames));
                            } else if (ok) {
                                Toast.ok("已复制 " + copied + " 项到当前目录");
                            } else {
                                Toast.error("拖拽复制失败");
                            }
                        }
                        e.setDropCompleted(ok);
                        e.consume();
                    });

            VBox.setVgrow(list, Priority.ALWAYS);
            root.getChildren().addAll(top, list);
            refresh();
        }

        Node node() {
            return root;
        }

        File current() {
            return current;
        }

        List<File> selected() {
            return new ArrayList<>(list.getSelectionModel().getSelectedItems());
        }

        /** 设为活动栏：边框高亮，按钮/快捷键以它为准。 */
        void activate() {
            if (panes != null) {
                activeRef[0] = this;
                for (FilePane pane : panes) {
                    pane.applyActive(pane == this);
                }
            }
        }

        void applyActive(boolean active) {
            root.getStyleClass().remove("neko-card-active");
            if (active) {
                root.getStyleClass().add("neko-card-active");
            }
        }

        void refresh() {
            path.setText(current.getAbsolutePath());
            File[] files = current.listFiles();
            if (files == null) files = new File[0];
            Arrays.sort(
                    files,
                    Comparator.comparing((File f) -> !f.isDirectory())
                            .thenComparing(f -> f.getName().toLowerCase(java.util.Locale.ROOT)));
            list.getItems().setAll(files);
        }
    }
}
