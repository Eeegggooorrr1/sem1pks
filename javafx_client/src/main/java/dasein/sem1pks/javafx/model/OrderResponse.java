package dasein.sem1pks.javafx.model;

public record OrderResponse(
        long id,
        long listingId,
        long buyerId,
        long sellerId,
        String status,
        String orderDate
) {
}
