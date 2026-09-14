package dasein.sem1pks.repository.impl.jdbc;

import dasein.sem1pks.repository.StatisticsRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Repository
@Profile("jdbc")
public class JdbcStatisticsRepository implements StatisticsRepository {

    private final DataSource dataSource;

    public JdbcStatisticsRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public StatisticsProjection getStatistics() {
        String sql = """
                SELECT
                    (SELECT COUNT(*) FROM users) AS users_count,
                    (SELECT COUNT(*) FROM orders) AS orders_count,
                    (SELECT COUNT(*) FROM listings) AS listings_count
                """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            if (!resultSet.next()) {
                throw new SQLException(
                        "Statistics query returned no rows"
                );
            }

            long usersCount = resultSet.getLong("users_count");
            long ordersCount = resultSet.getLong("orders_count");
            long listingsCount = resultSet.getLong("listings_count");

            return new StatisticsProjection() {

                @Override
                public long getUsersCount() {
                    return usersCount;
                }

                @Override
                public long getOrdersCount() {
                    return ordersCount;
                }

                @Override
                public long getListingsCount() {
                    return listingsCount;
                }
            };

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to load statistics",
                    e
            );
        }
    }
}
