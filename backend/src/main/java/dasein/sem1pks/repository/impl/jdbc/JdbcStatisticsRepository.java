package dasein.sem1pks.repository.impl.jdbc;

import dasein.sem1pks.repository.StatisticsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@Profile("jdbc")
@RequiredArgsConstructor
public class JdbcStatisticsRepository implements StatisticsRepository {
    private final JdbcTemplate jdbc;

    @Override
    public StatisticsProjection getStatistics() {
        return jdbc.queryForObject("""
                SELECT (SELECT count(*) FROM users),
                       (SELECT count(*) FROM orders),
                       (SELECT count(*) FROM listings)
                """, (rs, row) -> new Counts(rs.getLong(1), rs.getLong(2), rs.getLong(3)));
    }

    private record Counts(long users, long orders, long listings) implements StatisticsProjection {
        @Override
        public long getUsersCount() { return users; }
        @Override
        public long getOrdersCount() { return orders; }
        @Override
        public long getListingsCount() { return listings; }
    }
}
