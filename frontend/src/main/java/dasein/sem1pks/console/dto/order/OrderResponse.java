package dasein.sem1pks.console.dto.order;

public record OrderResponse(long id, long listingId, long buyerId, long sellerId, String status, String orderDate) {}
