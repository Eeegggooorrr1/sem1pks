package dasein.sem1pks.repository.impl.jdbc;

import dasein.sem1pks.dto.response.Analytics;
import dasein.sem1pks.dto.response.Group;
import dasein.sem1pks.repository.AnalyticsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JdbcAnalyticsRepository implements AnalyticsRepository {

    private static final RowMapper<Group> GROUP_MAPPER =
            (row, index) -> new Group(row.getString("key"), row.getLong("count"));

    private final JdbcTemplate jdbc;

    @Override
    public Analytics get() {
        var byCategory = jdbc.query("""
                SELECT category_id::text AS key, count(*) AS count
                FROM listings GROUP BY category_id ORDER BY category_id
                """, GROUP_MAPPER);
        var byStatus = jdbc.query("""
                SELECT status AS key, count(*) AS count
                FROM listings GROUP BY status ORDER BY status
                """, GROUP_MAPPER);
        var byPriority = jdbc.query("""
                SELECT %s AS key, count(*) AS count
                FROM listings l GROUP BY 1 ORDER BY 1
                """.formatted(JdbcCatalogRepository.PRIORITY_SQL), GROUP_MAPPER);

        return jdbc.queryForObject("""
                SELECT count(*) AS total,
                       count(*) FILTER (WHERE NOT archived) AS active,
                       count(*) FILTER (WHERE archived) AS archived,
                       count(*) FILTER (WHERE featured) AS featured,
                       coalesce(avg(price), 0) AS average
                FROM listings
                """, (row, index) -> new Analytics(
                row.getLong("total"),
                row.getLong("active"),
                row.getLong("archived"),
                row.getLong("featured"),
                row.getBigDecimal("average"),
                byCategory,
                byStatus,
                byPriority
        ));
    }
}
