package dasein.sem1pks.javafx.ui;

import dasein.sem1pks.javafx.api.ApiClient;
import dasein.sem1pks.javafx.model.ListingCreateRequest;
import dasein.sem1pks.javafx.model.ListingResponse;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

final class MyListingsPane extends BorderPane implements Refreshable {
    private final ApiClient api;
    private final Consumer<Throwable> error;

    @FXML
    private TextField title;
    @FXML
    private TextArea description;
    @FXML
    private TextField price;
    @FXML
    private ComboBox<String> category;
    @FXML
    private Button createButton;
    @FXML
    private ListView<ListingResponse> listings;

    MyListingsPane(ApiClient api, Consumer<Throwable> error) {
        this.api = api;
        this.error = error;
        FxmlView.load(this, "/fxml/my-listings.fxml");
    }

    @FXML
    private void initialize() {
        category.setItems(FXCollections.observableArrayList(UiSupport.categories().values()));
        category.setValue(UiSupport.categoryName("OTHER"));
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
    private void create() {
        try {
            if (title.getText().isBlank()) {
                throw new IllegalArgumentException("Введите название объявления");
            }
            BigDecimal parsedPrice = new BigDecimal(price.getText().trim().replace(',', '.'));
            ListingCreateRequest request = new ListingCreateRequest(
                    title.getText().trim(),
                    description.getText().trim(),
                    parsedPrice,
                    UiSupport.valueForCategoryName(category.getValue())
            );
            UiTasks.run(createButton, () -> api.listings().create(request), response -> {
                title.clear();
                description.clear();
                price.clear();
                load();
                UiSupport.info("Объявление создано");
            }, error);
        } catch (NumberFormatException exception) {
            UiSupport.error("Введите корректную цену");
        } catch (IllegalArgumentException exception) {
            UiSupport.error(exception.getMessage());
        }
    }

    private void load() {
        UiTasks.run(listings, api.listings()::mine,
                result -> listings.setItems(FXCollections.observableArrayList(result)), error);
    }

    @Override
    public void refresh() {
        load();
    }

    private List<Button> actionsFor(ListingResponse listing) {
        List<Button> buttons = new ArrayList<>();
        if (!"CLOSED".equals(listing.status())) {
            Button edit = new Button("Редактировать");
            edit.getStyleClass().add("secondary-button");
            edit.setOnAction(event -> ListingEditDialog.show(getScene().getWindow(), listing)
                    .ifPresent(request -> UiTasks.run(edit,
                            () -> api.listings().update(listing.id(), request),
                            response -> {
                                load();
                                UiSupport.info("Объявление обновлено");
                            },
                            error)));
            buttons.add(edit);
        }
        if ("ACTIVE".equals(listing.status())) {
            Button sold = new Button("Товар закончился");
            sold.setOnAction(event -> UiTasks.run(sold, () -> api.listings().markSold(listing.id()),
                    response -> load(), error));
            buttons.add(sold);
        }
        if ("SOLD".equals(listing.status())) {
            Button active = new Button("Снова в продаже");
            active.setOnAction(event -> UiTasks.run(active, () -> api.listings().markActive(listing.id()),
                    response -> load(), error));
            buttons.add(active);
        }
        if (!"CLOSED".equals(listing.status())) {
            Button close = new Button("Закрыть объявление");
            close.getStyleClass().add("danger-button");
            close.setOnAction(event -> {
                if (!UiSupport.confirm("Закрыть объявление #" + listing.id() + "?")) {
                    return;
                }
                UiTasks.run(close, () -> api.listings().close(listing.id()), this::load, error);
            });
            buttons.add(close);
        }
        return buttons;
    }
}
