package dasein.sem1pks.javafx.ui;

import dasein.sem1pks.javafx.model.ListingResponse;
import dasein.sem1pks.javafx.model.ListingUpdateRequest;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Window;

import java.math.BigDecimal;
import java.util.Optional;

final class ListingEditDialog {
    private final ListingResponse listing;
    private final DialogPane pane;
    private ListingUpdateRequest validatedRequest;

    @FXML
    private TextField title;
    @FXML
    private TextField price;
    @FXML
    private ComboBox<String> category;

    private ListingEditDialog(ListingResponse listing) {
        this.listing = listing;
        pane = FxmlView.load("/fxml/listing-edit.fxml", this);
    }

    static Optional<ListingUpdateRequest> show(Window owner, ListingResponse listing) {
        return new ListingEditDialog(listing).show(owner);
    }

    @FXML
    private void initialize() {
        category.getItems().addAll(UiSupport.categories().values());
        title.setText(listing.title());
        price.setText(listing.price().stripTrailingZeros().toPlainString());
        category.setValue(UiSupport.categoryName(listing.category()));
    }

    private Optional<ListingUpdateRequest> show(Window owner) {
        ButtonType saveType = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
        pane.getButtonTypes().setAll(saveType, cancelType);
        pane.getStylesheets().add(ListingEditDialog.class.getResource("/styles.css").toExternalForm());

        Dialog<ListingUpdateRequest> dialog = new Dialog<>();
        dialog.setTitle("Редактирование объявления");
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setDialogPane(pane);

        Node saveButton = pane.lookupButton(saveType);
        saveButton.getStyleClass().add("primary-button");
        pane.lookupButton(cancelType).getStyleClass().add("secondary-button");
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            try {
                validatedRequest = readRequest();
            } catch (IllegalArgumentException exception) {
                UiSupport.error(exception.getMessage());
                event.consume();
            }
        });

        dialog.setResultConverter(button -> saveType.equals(button) ? validatedRequest : null);
        return dialog.showAndWait();
    }

    private ListingUpdateRequest readRequest() {
        String newTitle = title.getText().trim();
        if (newTitle.isBlank()) {
            throw new IllegalArgumentException("Введите название объявления");
        }
        if (newTitle.length() > 200) {
            throw new IllegalArgumentException("Название не должно превышать 200 символов");
        }

        BigDecimal newPrice;
        try {
            newPrice = new BigDecimal(price.getText().trim().replace(',', '.'));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Введите корректную цену");
        }
        if (newPrice.signum() <= 0) {
            throw new IllegalArgumentException("Цена должна быть положительной");
        }
        if (category.getValue() == null) {
            throw new IllegalArgumentException("Выберите категорию");
        }

        return new ListingUpdateRequest(
                newTitle,
                newPrice,
                UiSupport.valueForCategoryName(category.getValue())
        );
    }
}
