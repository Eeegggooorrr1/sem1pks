package dasein.sem1pks.repository.impl.jdbc;

import dasein.sem1pks.domain.Listing;
import dasein.sem1pks.domain.ListingCategory;
import dasein.sem1pks.domain.ListingStatus;
import dasein.sem1pks.mapper.ListingRowMapper;
import dasein.sem1pks.repository.ListingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Sort;

import java.sql.*;
import java.util.Optional;

@Repository
@Profile("jdbc")
@RequiredArgsConstructor
public class JdbcListingRepository implements ListingRepository {

    private final DataSource dataSource;
    private final ListingRowMapper listingRowMapper;

    @Override
    public Listing save(Listing listing) {
        if (listing.getId() == null) {
            return insert(listing);
        }

        return update(listing);
    }

    @Override
    public Optional<Listing> findById(Long id) {
        String sql = """
                SELECT
                    id,
                    title,
                    description,
                    price,
                    status,
                    user_id,
                    category,
                    created_at
                FROM listings
                WHERE id = ?
                """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }

                return Optional.of(listingRowMapper.map(resultSet));
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to find listing: " + id,
                    e
            );
        }
    }

    @Override
    public List<Listing> findByUserIdOrderByCreatedAtDesc(Long userId) {
        String sql = """
                SELECT
                    id,
                    title,
                    description,
                    price,
                    status,
                    user_id,
                    category,
                    created_at
                FROM listings
                WHERE user_id = ?
                ORDER BY created_at DESC
                """;

        List<Listing> listings = new ArrayList<>();

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, userId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    listings.add(listingRowMapper.map(resultSet));
                }
            }

            return listings;

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to find listings for user: " + userId,
                    e
            );
        }
    }

    @Override
    public List<Listing> search(
            String prefix,
            ListingCategory category,
            ListingStatus closedStatus,
            ListingStatus soldStatus,
            Sort sort
    ) {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    id,
                    title,
                    description,
                    price,
                    status,
                    user_id,
                    category,
                    created_at
                FROM listings
                WHERE status <> ?
                  AND (? IS NULL OR LOWER(title) LIKE LOWER(?))
                  AND (? IS NULL OR category = ?)
                ORDER BY
                    CASE
                        WHEN status = ? THEN 1
                        ELSE 0
                    END ASC
                """);

        appendSort(sql, sort);

        List<Listing> listings = new ArrayList<>();

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {

            int index = 1;

            statement.setString(index++, closedStatus.name());

            if (prefix == null) {
                statement.setNull(index++, Types.VARCHAR);
                statement.setNull(index++, Types.VARCHAR);
            } else {
                String pattern = prefix + "%";
                statement.setString(index++, pattern);
                statement.setString(index++, pattern);
            }

            if (category == null) {
                statement.setNull(index++, Types.VARCHAR);
                statement.setNull(index++, Types.VARCHAR);
            } else {
                statement.setString(index++, category.name());
                statement.setString(index++, category.name());
            }

            statement.setString(index, soldStatus.name());

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    listings.add(listingRowMapper.map(resultSet));
                }
            }

            return listings;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to search listings", e);
        }
    }

    private void appendSort(StringBuilder sql, Sort sort) {
        for (Sort.Order order : sort) {
            String column = switch (order.getProperty()) {
                case "title" -> "title";
                case "price" -> "price";
                case "createdAt" -> "created_at";
                case "status" -> "status";
                case "category" -> "category";
                default -> throw new IllegalArgumentException(
                        "Unsupported listing sort property: "
                                + order.getProperty()
                );
            };

            sql.append(", ")
                    .append(column)
                    .append(order.isAscending() ? " ASC" : " DESC");
        }

        sql.append(", id DESC");
    }

    private Listing insert(Listing listing) {
        String sql = """
                INSERT INTO listings (
                    title,
                    description,
                    price,
                    status,
                    user_id,
                    category,
                    created_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     sql,
                     Statement.RETURN_GENERATED_KEYS
             )) {

            statement.setString(1, listing.getTitle());
            statement.setString(2, listing.getDescription());
            statement.setBigDecimal(3, listing.getPrice());
            statement.setString(4, listing.getStatus().name());
            statement.setLong(5, listing.getUser().getId());
            statement.setString(6, listing.getCategory().name());

            if (listing.getCreatedAt() == null) {
                listing.setCreatedAt(
                        java.time.LocalDateTime.now()
                );
            }

            statement.setTimestamp(
                    7,
                    Timestamp.valueOf(listing.getCreatedAt())
            );

            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (!generatedKeys.next()) {
                    throw new SQLException(
                            "Failed to retrieve generated listing id"
                    );
                }

                listing.setId(generatedKeys.getLong(1));
            }

            return listing;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert listing", e);
        }
    }

    private Listing update(Listing listing) {
        String sql = """
                UPDATE listings
                SET
                    title = ?,
                    description = ?,
                    price = ?,
                    status = ?,
                    user_id = ?,
                    category = ?
                WHERE id = ?
                """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, listing.getTitle());
            statement.setString(2, listing.getDescription());
            statement.setBigDecimal(3, listing.getPrice());
            statement.setString(4, listing.getStatus().name());
            statement.setLong(5, listing.getUser().getId());
            statement.setString(6, listing.getCategory().name());
            statement.setLong(7, listing.getId());

            int updatedRows = statement.executeUpdate();

            if (updatedRows == 0) {
                throw new IllegalArgumentException(
                        "Listing not found: " + listing.getId()
                );
            }

            return listing;

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to update listing: " + listing.getId(),
                    e
            );
        }
    }
}
