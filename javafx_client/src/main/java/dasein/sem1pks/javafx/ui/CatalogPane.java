package dasein.sem1pks.javafx.ui;

import dasein.sem1pks.javafx.api.ApiClient;
import dasein.sem1pks.javafx.model.ListingResponse;
import dasein.sem1pks.javafx.session.Session;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

final class CatalogPane extends BorderPane implements Refreshable {
    private static final String ALL_CATEGORIES = "Все категории";

    private final ApiClient api;
    private final Session session;
    private final Consumer<Throwable> error;

    @FXML
    private TextField prefix;
    @FXML
    private ComboBox<String> category;
    @FXML
    private ComboBox<String> sort;
    @FXML
    private ListView<ListingResponse> listings;

    CatalogPane(ApiClient api, Session session, Consumer<Throwable> error) {
        this.api = api;
        this.session = session;
        this.error = error;
        FxmlView.load(this, "/fxml/catalog.fxml");
    }

    @FXML
    private void initialize() {
        category.getItems().add(ALL_CATEGORIES);
        category.getItems().addAll(UiSupport.categories().values());
        category.setValue(ALL_CATEGORIES);
        sort.getItems().addAll("Сначала новые", "Сначала старые", "Сначала дешевле", "Сначала дороже");
        sort.setValue("Сначала новые");

        listings.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(ListingResponse item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                setGraphic(empty || item == null ? null : ListingCard.create(item, actionsFor(item)));
            }
        });
        load();
    }

    @FXML
    private void load() {
        String categoryValue = ALL_CATEGORIES.equals(category.getValue())
                ? null
                : UiSupport.valueForCategoryName(category.getValue());
        String sortBy = sort.getValue().contains("дешевле") || sort.getValue().contains("дороже")
                ? "PRICE"
                : "CREATED_AT";
        String sortDirection = sort.getValue().contains("старые") || sort.getValue().contains("дешевле")
                ? "ASC"
                : "DESC";

        UiTasks.run(listings,
                () -> api.listings().search(prefix.getText().trim(), categoryValue, sortBy, sortDirection),
                result -> listings.setItems(FXCollections.observableArrayList(result)),
                error);
    }

    @Override
    public void refresh() {
        load();
    }

    private List<Button> actionsFor(ListingResponse listing) {
        List<Button> buttons = new ArrayList<>();
        if (session.isAuthenticated()
                && listing.ownerId() != session.account().id()
                && "ACTIVE".equals(listing.status())) {
            Button order = new Button("Заказать");
            order.getStyleClass().add("primary-button");
            order.setOnAction(event -> UiTasks.run(order,
                    () -> api.orders().create(listing.id()),
                    response -> UiSupport.info("Заказ создан"),
                    error));
            buttons.add(order);
        }

        if (session.isAuthenticated() && session.account().isAdmin() && !"CLOSED".equals(listing.status())) {
            Button close = new Button("Закрыть как администратор");
            close.getStyleClass().add("danger-button");
            close.setOnAction(event -> {
                if (!UiSupport.confirm("Закрыть объявление #" + listing.id() + "?")) {
                    return;
                }
                UiTasks.run(close, () -> api.listings().closeAsAdmin(listing.id()), this::load, error);
            });
            buttons.add(close);
        }
        return buttons;
    }
}
