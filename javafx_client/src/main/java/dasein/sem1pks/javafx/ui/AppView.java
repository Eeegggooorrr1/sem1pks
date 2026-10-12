package dasein.sem1pks.javafx.ui;

import dasein.sem1pks.javafx.api.ApiClient;
import dasein.sem1pks.javafx.api.ApiException;
import dasein.sem1pks.javafx.session.Session;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

public final class AppView extends BorderPane {
    private final ApiClient api;
    private final Session session;
    private boolean guestMode;

    @FXML
    private HBox header;
    @FXML
    private Label accountLabel;
    @FXML
    private Button accountButton;
    @FXML
    private StackPane content;

    public AppView(ApiClient api, Session session) {
        this.api = api;
        this.session = session;
        FxmlView.load(this, "/fxml/app-view.fxml");
        showAuth();
    }

    private void showAuth() {
        header.setVisible(false);
        header.setManaged(false);
        content.getChildren().setAll(
                new AuthPane(api, this::showAuthenticated, () -> showDashboard(true), this::handleError)
        );
    }

    private void showAuthenticated() {
        showDashboard(false);
    }

    private void showDashboard(boolean guest) {
        guestMode = guest;
        header.setVisible(true);
        header.setManaged(true);
        accountLabel.setText(guest
                ? "Гостевой режим"
                : session.account().username() + " · " + session.account().email());
        accountButton.setText(guest ? "Войти" : "Выйти");

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getTabs().add(new Tab("Объявления", new CatalogPane(api, session, this::handleError)));

        if (!guest && session.isAuthenticated()) {
            tabs.getTabs().add(new Tab("Мои объявления", new MyListingsPane(api, this::handleError)));
            tabs.getTabs().add(new Tab("Заказы", new OrdersPane(api, this::handleError)));
            tabs.getTabs().add(new Tab("Обзор", new OverviewPane(api, session, this::handleError)));
        }
        tabs.getSelectionModel().selectedItemProperty().addListener((observable, previous, selected) -> {
            if (selected != null && selected.getContent() instanceof Refreshable refreshable) {
                refreshable.refresh();
            }
        });
        content.getChildren().setAll(tabs);
    }

    @FXML
    private void leaveDashboard() {
        if (!guestMode) {
            api.accounts().logout();
        }
        showAuth();
    }

    private void handleError(Throwable throwable) {
        String message = throwable.getMessage() == null ? "Не удалось выполнить действие" : throwable.getMessage();
        UiSupport.error(message);
        if (throwable instanceof ApiException apiException && apiException.status() == 401) {
            showAuth();
        }
    }
}
