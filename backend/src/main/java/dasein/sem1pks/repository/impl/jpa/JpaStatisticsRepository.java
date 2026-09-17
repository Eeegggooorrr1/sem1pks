package dasein.sem1pks.repository.impl.jpa;

import dasein.sem1pks.domain.User;
import dasein.sem1pks.repository.StatisticsRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.stereotype.Component;

@Component
@Profile("jpa")
public interface JpaStatisticsRepository extends Repository<User, Long>, StatisticsRepository {

    @Override
    @Query(value = """
            SELECT
                (SELECT COUNT(*) FROM users) AS "usersCount",
                (SELECT COUNT(*) FROM orders) AS "ordersCount",
                (SELECT COUNT(*) FROM listings) AS "listingsCount"
            """, nativeQuery = true)
    StatisticsProjection getStatistics();
}
