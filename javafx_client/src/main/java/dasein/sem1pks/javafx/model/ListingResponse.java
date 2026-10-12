package dasein.sem1pks.javafx.model;

import java.math.BigDecimal;

public record ListingResponse(
        long id,
        String title,
        String description,
        BigDecimal price,
        String status,
        String category,
        long ownerId,
        String createdAt
) {
}
