package dasein.sem1pks.javafx.model;

import java.math.BigDecimal;

public record ListingCreateRequest(String title, String description, BigDecimal price, String category) {
}
