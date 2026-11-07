package com.example.demo.ui.component;

import com.example.demo.core.instance.InstanceFiles;
import com.example.demo.core.mod.ModpackExporter;
import com.example.demo.core.game.PortableExporter;
import com.example.demo.core.mod.ServerPackExporter;
import com.example.demo.core.model.InstanceDirs;
import com.example.demo.core.model.Instance;
import com.example.demo.ui.shell.InAppDialog;
import com.example.demo.ui.shell.Toast;
import javafx.application.Platform;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.ProgressBar;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Modality;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.layout.StackPane;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletionException;
import java.util.function.DoubleConsumer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 「导出整合包」对话框与三种导出方式：.mrpack、带启动器的便携文件夹、服务端文件包。
 *
 * <p>从主类搬出；游戏目录、隔离开关、日志与「打开目录」由 {@link Host} 提供。
 */
public final class ExportInstanceDialog {

    public interface Host {
        File gameDir();

        boolean isolated(String instanceId);

        void log(String text);

        void openDir(File dir);
    }

    private final Stage owner;
    private final StackPane layer;
    private final Host host;

    public ExportInstanceDialog(Stage owner, StackPane layer, Host host) {
        this.owner = owner;
        this.layer = layer;
        this.host = host;
    }

    public void show(Instance instance) {
  if (instance == null) {
    return;
  }
  CheckBox includeSaves = new CheckBox("同时包含存档");
  CheckBox includeLauncher = new CheckBox("带启动器（导出为便携文件夹，启动器和 .minecraft 放在同一目录）");
  CheckBox serverPack = new CheckBox("导出服务端文件包（mods / config / scripts，自动过滤常见客户端模组）");
  InAppDialog.custom(
      layer,
      "导出整合包",
      new VBox(
          10.0,
          new Label("默认导出 .mrpack，可直接拖入 PCL 安装。"),
          new Label("勾选「带启动器」后会导出文件夹：启动器 + .minecraft + 启动脚本。"),
          includeSaves,
          includeLauncher,
          serverPack),
      List.of(
          InAppDialog.Action.secondary("取消", null),
          InAppDialog.Action.primary(
              "导出",
              () -> {
  File out;
  boolean withLauncher = includeLauncher.isSelected();
  boolean withSaves = includeSaves.isSelected();
  String safeName = instance.id.replaceAll("[^0-9A-Za-z._-]", "_");
  if (serverPack.isSelected()) {
    File instanceRoot =
        InstanceDirs.root(
            host.gameDir(), instance.id, host.isolated(instance.id));
    FileChooser chooser = new FileChooser();
    chooser.setTitle("导出服务端文件包");
    chooser
        .getExtensionFilters()
        .add(new FileChooser.ExtensionFilter("ZIP 压缩包 (*.zip)", "*.zip"));
    chooser.setInitialFileName(safeName + "-server.zip");
    File target = chooser.showSaveDialog(owner);
    if (target == null) {
      return;
    }
    if (!target.getName().toLowerCase(Locale.ROOT).endsWith(".zip")) {
      target = new File(target.getParentFile(), target.getName() + ".zip");
    }
    File output = target;
    Toast.info("正在导出服务端文件包…");
    CompletableFuture.runAsync(
        () -> {
          try {
            ServerPackExporter.Result result =
                ServerPackExporter.export(
                    instanceRoot, output, true, msg -> host.log(msg), null);
            Platform.runLater(
                () -> {
                  Toast.ok(
                      "服务端包完成："
                          + result.files()
                          + " 个文件，过滤 "
                          + result.skippedClientMods()
                          + " 个客户端模组");
                  host.log("✅ 服务端包: " + output.getAbsolutePath());
                  host.openDir(output.getParentFile());
                });
          } catch (Exception ex) {
            Platform.runLater(
                () -> {
                  Toast.error("服务端包导出失败：" + ex.getMessage());
                  host.log("❌ 服务端包导出失败: " + ex.getMessage());
                });
          }
        });
    return;
  }
  if (withLauncher) {
    DirectoryChooser dirChooser = new DirectoryChooser();
    dirChooser.setTitle("选择导出位置");
    File parent = dirChooser.showDialog(owner);
    if (parent == null) {
      return;
    }
    out = new File(parent, safeName + "-portable");
    int suffix = 2;
    while (out.exists()) {
      out = new File(parent, safeName + "-portable-" + suffix++);
    }
  } else {
    FileChooser chooser = new FileChooser();
    chooser.setTitle("导出整合包");
    chooser
        .getExtensionFilters()
        .add(new FileChooser.ExtensionFilter("Modrinth 整合包 (*.mrpack)", "*.mrpack"));
    chooser.setInitialFileName(safeName + ".mrpack");
    out = chooser.showSaveDialog(owner);
    if (out == null) {
      return;
    }
    if (!out.getName().toLowerCase(Locale.ROOT).endsWith(".mrpack")) {
      out = new File(out.getParentFile(), out.getName() + ".mrpack");
    }
  }
  File target = out;
  ProgressBar exportProgress = new ProgressBar(0.0);
  exportProgress.setMaxWidth(Double.MAX_VALUE);
  Label exportStatus = new Label("正在准备导出…");
  exportStatus.setStyle("-fx-text-fill: -app-text-2; -fx-font-size: 12px;");
  Dialog<?> progressDialog = new Dialog<>();
  progressDialog.initOwner(owner);
  progressDialog.initModality(Modality.APPLICATION_MODAL);
  progressDialog.setTitle("正在导出");
  progressDialog.setHeaderText(withLauncher ? "正在导出便携整合包" : "正在导出 Modrinth 整合包");
  progressDialog.getDialogPane().setContent(new VBox(12.0, exportStatus, exportProgress));
  progressDialog.getDialogPane().setPrefWidth(440.0);
  AtomicBoolean exportDone = new AtomicBoolean(false);
  progressDialog.setOnCloseRequest(
      ev -> {
        if (!exportDone.get()) {
          ev.consume();
        }
      });
  progressDialog
      .getDialogPane()
      .addEventFilter(
          KeyEvent.KEY_PRESSED,
          ev -> {
            if (ev.getCode() == KeyCode.ESCAPE && !exportDone.get()) {
              ev.consume();
            }
          });
  progressDialog.show();
  DoubleConsumer onProgress =
      p ->
          Platform.runLater(
              () -> {
                exportProgress.setProgress(Math.max(0.0, Math.min(1.0, p)));
                exportStatus.setText("正在导出… " + (int) Math.round(p * 100.0) + "%");
              });
  host.log(
      withLauncher ? "正在导出便携整合包: " + target.getName() : "正在导出整合包: " + target.getName());

  CompletableFuture.runAsync(
          () -> {
            try {
              if (withLauncher) {
                PortableExporter.export(
                    host.gameDir(), instance, target, withSaves, onProgress);
              } else {
                ModpackExporter.export(
                    host.gameDir(), instance, target, withSaves, onProgress);
              }
            } catch (Exception ex) {
              throw new CompletionException(ex);
            }
          })
      .thenAccept(
          v ->
              Platform.runLater(
                  () -> {
                    exportDone.set(true);
                    progressDialog.setOnCloseRequest(null);
                    try {
                      if (progressDialog.getDialogPane().getScene() != null
                          && progressDialog.getDialogPane().getScene().getWindow() != null) {
                        progressDialog.getDialogPane().getScene().getWindow().hide();
                      }
                    } catch (Exception exception) {
                      // empty catch block
                    }
                    progressDialog.close();
                    InAppDialog.alert(
                        "导出完成",
                        (withLauncher ? "便携整合包已生成：\n" : "Modrinth 整合包已生成：\n")
                            + target.getAbsolutePath());
                    host.log("✅ 导出完成: " + target.getAbsolutePath());
                  }))
      .exceptionally(
          ex -> {
            Throwable cause = ex;
            while (cause.getCause() != null) {
              cause = cause.getCause();
            }
            Throwable root = cause;
            Platform.runLater(
                () -> {
                  exportDone.set(true);
                  progressDialog.setOnCloseRequest(null);
                  try {
                    if (progressDialog.getDialogPane().getScene() != null
                        && progressDialog.getDialogPane().getScene().getWindow() != null) {
                      progressDialog.getDialogPane().getScene().getWindow().hide();
                    }
                  } catch (Exception exception) {
                    // empty catch block
                  }
                  progressDialog.close();
                  Toast.error(
                      "导出失败: " + (root.getMessage() == null ? "未知错误" : root.getMessage()));
                  host.log("❌ 导出失败: " + root.getMessage());
                });
            return null;
          });
                })),
      null);
    }
}
