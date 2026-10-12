package dasein.sem1pks.javafx.model;

import java.math.BigDecimal;

public record StatisticsResponse(
        long usersCount,
        long ordersCount,
        long listingsCount,
        BigDecimal averageListingPrice,
        BigDecimal maxListingPrice
) {
}
