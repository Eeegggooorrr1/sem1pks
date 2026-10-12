package dasein.sem1pks.dto.response;

import java.math.BigDecimal;

public record StatisticsResponse(
        long usersCount,
        long ordersCount,
        long listingsCount,
        BigDecimal averageListingPrice,
        BigDecimal maxListingPrice
) {
}
