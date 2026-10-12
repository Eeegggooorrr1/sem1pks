package dasein.sem1pks.console.dto.listing;

public record ListingResponse(long id, String title, String description, java.math.BigDecimal price,
                              String status, String category, long ownerId, String createdAt) {}
