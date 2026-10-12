package dasein.sem1pks.javafx.model;

import java.math.BigDecimal;

public record ListingUpdateRequest(String title, BigDecimal price, String category) {
}
