package com.example.demo.ui.component;

import com.example.demo.core.auth.AccountStore;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;
import java.util.function.Consumer;

/**
 * 「账户管理」对话框：列出已保存账户，可切换 / 删除 / 添加。
 *
 * <p>从主类搬出来。切换与删除的真正动作由调用方提供；删除后对话框自己重建列表，
 * 不需要调用方再递归打开一次。
 */
public final class AccountManagerDialog {

    private final Stage owner;
    private final Consumer<AccountStore.Account> onSwitch;
    private final Consumer<AccountStore.Account> onRemove;
    private final Runnable onAdd;
    private final Dialog<ButtonType> dialog = new Dialog<>();
    private final VBox list = new VBox(8.0);

    private AccountManagerDialog(Stage owner, Consumer<AccountStore.Account> onSwitch,
                                 Consumer<AccountStore.Account> onRemove, Runnable onAdd) {
        this.owner = owner;
        this.onSwitch = onSwitch;
        this.onRemove = onRemove;
        this.onAdd = onAdd;
    }

    public static void show(Stage owner, Consumer<AccountStore.Account> onSwitch,
                            Consumer<AccountStore.Account> onRemove, Runnable onAdd) {
        new AccountManagerDialog(owner, onSwitch, onRemove, onAdd).open();
    }

    private void open() {
        if (owner != null) {
            dialog.initOwner(owner);
        }
        dialog.setTitle("账户管理");
        dialog.setHeaderText("已保存的账户");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        rebuild();
        dialog.showAndWait();
    }

    private void rebuild() {
        list.getChildren().clear();
        List<AccountStore.Account> accounts = AccountStore.list();
        if (accounts.isEmpty()) {
            Label empty = new Label("还没有保存的账户，登录后会自动保存。");
            empty.setStyle("-fx-text-fill: -app-text-muted;");
            list.getChildren().add(empty);
        }
        for (AccountStore.Account account : accounts) {
            list.getChildren().add(row(account));
        }
        Button add = new Button("添加账户");
        add.setOnAction(e -> {
            dialog.close();
            if (onAdd != null) {
                onAdd.run();
            }
        });
        dialog.getDialogPane().setContent(new VBox(12.0, list, add));
    }

    private HBox row(AccountStore.Account account) {
        Label name = new Label(account.username());
        name.setStyle("-fx-text-fill: -app-text; -fx-font-size: 13px; -fx-font-weight: bold;");
        String texture = (account.skinPath().isBlank() ? "" : " · 有皮肤")
                + (account.capePath().isBlank() ? "" : " · 有披风");
        Label meta = new Label(account.loginType()
                + (account.server().isBlank() ? "" : " · " + account.server())
                + texture);
        meta.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 10px;");
        VBox info = new VBox(2.0, name, meta);
        HBox.setHgrow(info, Priority.ALWAYS);

        Button use = new Button("切换");
        use.setOnAction(e -> {
            dialog.close();
            if (onSwitch != null) {
                onSwitch.accept(account);
            }
        });
        Button remove = new Button("删除");
        remove.setStyle("-fx-background-color: transparent; -fx-text-fill: #f87171;"
                + " -fx-cursor: hand;");
        remove.setOnAction(e -> {
            if (onRemove != null) {
                onRemove.accept(account);
            }
            rebuild();
        });

        HBox row = new HBox(10.0, info, use, remove);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8.0, 10.0, 8.0, 10.0));
        row.setStyle("-fx-background-color: -app-row2; -fx-background-radius: 9;");
        return row;
    }
}
