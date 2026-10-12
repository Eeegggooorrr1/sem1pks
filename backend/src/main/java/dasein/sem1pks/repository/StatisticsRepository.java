package dasein.sem1pks.repository;

import java.math.BigDecimal;

public interface StatisticsRepository {

    StatisticsProjection getStatistics();

    default BigDecimal getAverageListingPrice() {
        return BigDecimal.ZERO;
    }

    default BigDecimal getMaxListingPrice() {
        return BigDecimal.ZERO;
    }

    interface StatisticsProjection {

        long getUsersCount();

        long getOrdersCount();

        long getListingsCount();
    }
}
