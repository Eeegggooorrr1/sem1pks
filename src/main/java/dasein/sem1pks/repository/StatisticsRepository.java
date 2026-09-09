package dasein.sem1pks.repository;

import dasein.sem1pks.domain.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

public interface StatisticsRepository extends Repository<User, Long> {

    @Query(value = """
            SELECT
                (SELECT COUNT(*) FROM users) AS "usersCount",
                (SELECT COUNT(*) FROM orders) AS "ordersCount",
                (SELECT COUNT(*) FROM listings) AS "listingsCount"
            """, nativeQuery = true)
    StatisticsProjection getStatistics();

    interface StatisticsProjection {

        long getUsersCount();

        long getOrdersCount();

        long getListingsCount();
    }
}