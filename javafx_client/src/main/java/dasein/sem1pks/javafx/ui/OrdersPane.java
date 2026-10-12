package dasein.sem1pks.javafx.ui;

import dasein.sem1pks.javafx.api.ApiClient;
import dasein.sem1pks.javafx.model.OrderResponse;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

final class OrdersPane extends BorderPane implements Refreshable {
    private static final String PURCHASES = "Мои покупки";
    private static final String SALES = "Заказы покупателей";

    private final ApiClient api;
    private final Consumer<Throwable> error;

    @FXML
    private ComboBox<String> direction;
    @FXML
    private ListView<OrderResponse> orders;

    OrdersPane(ApiClient api, Consumer<Throwable> error) {
        this.api = api;
        this.error = error;
        FxmlView.load(this, "/fxml/orders.fxml");
    }

    @FXML
    private void initialize() {
        direction.getItems().addAll(PURCHASES, SALES);
        direction.setValue(PURCHASES);
        direction.setOnAction(event -> load());
        orders.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(OrderResponse order, boolean empty) {
                super.updateItem(order, empty);
                setText(null);
                setGraphic(empty || order == null ? null : orderCard(order));
            }
        });
        load();
    }

    @FXML
    private void load() {
        String value = SALES.equals(direction.getValue()) ? "INCOMING" : "OUTGOING";
        UiTasks.run(orders,
                () -> api.orders().find(value),
                result -> orders.setItems(FXCollections.observableArrayList(result)),
                error);
    }

    @Override
    public void refresh() {
        load();
    }

    private VBox orderCard(OrderResponse order) {
        Label title = new Label("Заказ #" + order.id() + " · объявление #" + order.listingId());
        title.getStyleClass().add("card-title");

        Label status = new Label(UiSupport.statusName(order.status()));
        status.getStyleClass().addAll("status-label", "status-" + order.status().toLowerCase());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox heading = new HBox(10, title, spacer, status);
        heading.setAlignment(Pos.CENTER_LEFT);

        String participant = SALES.equals(direction.getValue())
                ? "Покупатель: #" + order.buyerId()
                : "Продавец: #" + order.sellerId();
        Label details = new Label(participant + "   ·   Создан: " + UiSupport.date(order.orderDate()));
        details.getStyleClass().add("muted-text");

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);
        if (SALES.equals(direction.getValue()) && "PENDING".equals(order.status())) {
            actions.getChildren().add(actionButton("Подтвердить", "primary-button",
                    () -> api.orders().confirm(order.id())));
        }
        if (PURCHASES.equals(direction.getValue()) && "CONFIRMED".equals(order.status())) {
            actions.getChildren().add(actionButton("Завершить", "primary-button",
                    () -> api.orders().complete(order.id())));
        }
        if (!"COMPLETED".equals(order.status()) && !"CANCELLED".equals(order.status())) {
            Button cancel = new Button("Отменить");
            cancel.getStyleClass().add("danger-button");
            cancel.setOnAction(event -> {
                if (UiSupport.confirm("Отменить заказ #" + order.id() + "?")) {
                    UiTasks.run(cancel, () -> api.orders().cancel(order.id()), this::load, error);
                }
            });
            actions.getChildren().add(cancel);
        }

        VBox card = new VBox(10, heading, details, actions);
        card.getStyleClass().add("content-card");
        card.setPadding(new Insets(14));
        return card;
    }

    private Button actionButton(String text, String style, Runnable operation) {
        Button button = new Button(text);
        button.getStyleClass().add(style);
        button.setOnAction(event -> UiTasks.run(button, operation, this::load, error));
        return button;
    }
}
