package dasein.sem1pks.javafx.ui;

import dasein.sem1pks.javafx.model.ListingResponse;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;

final class ListingCard {
    private ListingCard() {
    }

    static VBox create(ListingResponse listing, List<Button> actions) {
        Label title = new Label(listing.title());
        title.getStyleClass().add("card-title");
        Label price = new Label(UiSupport.price(listing.price()));
        price.getStyleClass().add("price-label");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox top = new HBox(12, title, spacer, price);
        top.setAlignment(Pos.CENTER_LEFT);

        Label meta = new Label(
                UiSupport.categoryName(listing.category()) + " · "
                        + UiSupport.statusName(listing.status()) + " · "
                        + UiSupport.date(listing.createdAt()) + " · владелец #" + listing.ownerId()
        );
        meta.getStyleClass().add("muted-text");

        Label description = new Label(listing.description() == null || listing.description().isBlank()
                ? "Без описания"
                : listing.description());
        description.setWrapText(true);

        VBox card = new VBox(8, top, meta, description);
        card.getStyleClass().add("listing-card");
        if (!actions.isEmpty()) {
            HBox buttons = new HBox(8);
            buttons.getChildren().addAll(actions);
            card.getChildren().add(buttons);
        }
        return card;
    }
}
