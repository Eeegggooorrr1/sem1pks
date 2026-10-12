package dasein.sem1pks.javafx.ui;

import dasein.sem1pks.javafx.api.ApiClient;
import dasein.sem1pks.javafx.model.AccountResponse;
import dasein.sem1pks.javafx.model.StatisticsResponse;
import dasein.sem1pks.javafx.session.Session;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

final class OverviewPane extends VBox implements Refreshable {
    private final ApiClient api;
    private final Session session;
    private final Consumer<Throwable> error;

    @FXML
    private Label username;
    @FXML
    private Label email;
    @FXML
    private Label role;
    @FXML
    private TableView<StatisticRow> statisticsTable;
    @FXML
    private TableColumn<StatisticRow, String> metricColumn;
    @FXML
    private TableColumn<StatisticRow, String> valueColumn;
    @FXML
    private Button refreshButton;
    @FXML
    private VBox adminBox;
    @FXML
    private Button exportButton;
    private final ObservableList<StatisticRow> statisticRows = FXCollections.observableArrayList();

    OverviewPane(ApiClient api, Session session, Consumer<Throwable> error) {
        this.api = api;
        this.session = session;
        this.error = error;
        FxmlView.load(this, "/fxml/overview.fxml");
    }

    @FXML
    private void initialize() {
        AccountResponse account = session.account();
        username.setText("Имя: " + account.username());
        email.setText("Email: " + account.email());
        role.setText("Роль: " + (account.isAdmin() ? "Администратор" : "Пользователь"));
        metricColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().name()));
        valueColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().value()));
        statisticsTable.setItems(statisticRows);
        adminBox.setVisible(account.isAdmin());
        adminBox.setManaged(account.isAdmin());
        loadStatistics();
    }

    @FXML
    private void loadStatistics() {
        UiTasks.run(refreshButton, api.statistics()::get, this::showStatistics, error);
    }

    @Override
    public void refresh() {
        loadStatistics();
    }

    private void showStatistics(StatisticsResponse statistics) {
        statisticRows.setAll(
                new StatisticRow("Пользователей", Long.toString(statistics.usersCount())),
                new StatisticRow("Объявлений", Long.toString(statistics.listingsCount())),
                new StatisticRow("Заказов", Long.toString(statistics.ordersCount())),
                new StatisticRow("Средняя цена объявления", UiSupport.price(statistics.averageListingPrice())),
                new StatisticRow("Максимальная цена объявления", UiSupport.price(statistics.maxListingPrice()))
        );
    }

    @FXML
    private void export() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Сохранить архив");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("ZIP-архив", "*.zip"));
        chooser.setInitialFileName("export-"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm")) + ".zip");

        String exportDir = System.getenv("EXPORT_DIR");
        if (exportDir != null && Files.isDirectory(Path.of(exportDir))) {
            chooser.setInitialDirectory(Path.of(exportDir).toFile());
        }

        File selected = chooser.showSaveDialog(getScene().getWindow());
        if (selected == null) {
            return;
        }
        UiTasks.run(exportButton,
                () -> api.admin().export(selected.toPath()),
                path -> UiSupport.info("Архив сохранён: " + path.toAbsolutePath()),
                error);
    }
}
