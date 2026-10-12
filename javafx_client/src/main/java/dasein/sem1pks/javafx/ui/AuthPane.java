package dasein.sem1pks.javafx.ui;

import dasein.sem1pks.javafx.api.ApiClient;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

final class AuthPane extends VBox {
    private final ApiClient api;
    private final Runnable authenticated;
    private final Runnable guest;
    private final Consumer<Throwable> error;

    @FXML
    private TextField loginEmail;
    @FXML
    private PasswordField loginPassword;
    @FXML
    private Button loginButton;
    @FXML
    private TextField registerUsername;
    @FXML
    private TextField registerEmail;
    @FXML
    private PasswordField registerPassword;
    @FXML
    private Button registerButton;

    AuthPane(ApiClient api, Runnable authenticated, Runnable guest, Consumer<Throwable> error) {
        this.api = api;
        this.authenticated = authenticated;
        this.guest = guest;
        this.error = error;
        FxmlView.load(this, "/fxml/auth.fxml");
    }

    @FXML
    private void login() {
        if (loginEmail.getText().isBlank() || loginPassword.getText().isBlank()) {
            UiSupport.error("Введите email и пароль");
            return;
        }
        UiTasks.run(loginButton,
                () -> api.accounts().login(loginEmail.getText().trim(), loginPassword.getText()),
                account -> authenticated.run(),
                error);
    }

    @FXML
    private void register() {
        if (registerUsername.getText().isBlank()
                || registerEmail.getText().isBlank()
                || registerPassword.getText().isBlank()) {
            UiSupport.error("Заполните все поля");
            return;
        }
        UiTasks.run(registerButton,
                () -> api.accounts().register(
                        registerUsername.getText().trim(),
                        registerEmail.getText().trim(),
                        registerPassword.getText()),
                account -> authenticated.run(),
                error);
    }

    @FXML
    private void openGuestMode() {
        guest.run();
    }
}
