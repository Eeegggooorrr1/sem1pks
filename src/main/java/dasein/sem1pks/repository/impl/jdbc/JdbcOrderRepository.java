package dasein.sem1pks.repository.impl.jdbc;

import dasein.sem1pks.domain.Listing;
import dasein.sem1pks.domain.Order;
import dasein.sem1pks.domain.OrderStatus;
import dasein.sem1pks.domain.User;
import dasein.sem1pks.mapper.OrderRowMapper;
import dasein.sem1pks.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@Repository
@Profile("jdbc")
@RequiredArgsConstructor
public class JdbcOrderRepository implements OrderRepository {

    private final DataSource dataSource;
    private final OrderRowMapper orderRowMapper;

    @Override
    public Order save(Order order) {
        if (order.getId() == null) {
            return insert(order);
        }

        return update(order);
    }

    @Override
    public List<Order> findIncomingOrders(Long userId) {
        String sql = """
                SELECT
                    o.id,
                    o.listing_id,
                    o.buyer_id,
                    o.status,
                    o.order_date
                FROM orders o
                JOIN listings l ON l.id = o.listing_id
                WHERE l.user_id = ?
                ORDER BY
                    CASE o.status
                        WHEN 'CONFIRMED' THEN 0
                        WHEN 'PENDING' THEN 1
                        WHEN 'COMPLETED' THEN 2
                        WHEN 'CANCELLED' THEN 3
                    END,
                    o.order_date DESC
                """;

        List<Order> orders = new ArrayList<>();

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, userId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Order order = orderRowMapper.map(resultSet);

                    order.setListing(createListingReference(
                            resultSet.getLong("listing_id")
                    ));

                    order.setBuyer(createUserReference(
                            resultSet.getLong("buyer_id")
                    ));

                    orders.add(order);
                }
            }

            return orders;

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to find incoming orders for user: " + userId,
                    e
            );
        }
    }

    @Override
    public List<Order> findOutgoingOrders(Long userId) {
        String sql = """
                SELECT
                    o.id,
                    o.listing_id,
                    o.buyer_id,
                    o.status,
                    o.order_date
                FROM orders o
                WHERE o.buyer_id = ?
                ORDER BY
                    CASE o.status
                        WHEN 'CONFIRMED' THEN 0
                        WHEN 'PENDING' THEN 1
                        WHEN 'COMPLETED' THEN 2
                        WHEN 'CANCELLED' THEN 3
                    END,
                    o.order_date DESC
                """;

        List<Order> orders = new ArrayList<>();

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, userId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Order order = orderRowMapper.map(resultSet);

                    order.setListing(createListingReference(
                            resultSet.getLong("listing_id")
                    ));

                    order.setBuyer(createUserReference(
                            resultSet.getLong("buyer_id")
                    ));

                    orders.add(order);
                }
            }

            return orders;

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to find outgoing orders for user: " + userId,
                    e
            );
        }
    }

    private Order insert(Order order) {
        String sql = """
                INSERT INTO orders (
                    listing_id,
                    buyer_id,
                    status,
                    order_date
                )
                VALUES (?, ?, ?, ?)
                """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     sql,
                     Statement.RETURN_GENERATED_KEYS
             )) {

            statement.setLong(1, order.getListing().getId());
            statement.setLong(2, order.getBuyer().getId());
            statement.setString(3, order.getStatus().name());

            if (order.getOrderDate() == null) {
                order.setOrderDate(java.time.LocalDateTime.now());
            }

            statement.setTimestamp(
                    4,
                    Timestamp.valueOf(order.getOrderDate())
            );

            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (!generatedKeys.next()) {
                    throw new SQLException(
                            "Failed to retrieve generated order id"
                    );
                }

                order.setId(generatedKeys.getLong(1));
            }

            return order;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert order", e);
        }
    }

    @Override
    public Optional<Order> findById(Long id) {
        String sql = """
            SELECT
                id,
                listing_id,
                buyer_id,
                status,
                order_date
            FROM orders
            WHERE id = ?
            """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }

                Order order = orderRowMapper.map(resultSet);

                order.setListing(createListingReference(
                        resultSet.getLong("listing_id")
                ));

                order.setBuyer(createUserReference(
                        resultSet.getLong("buyer_id")
                ));

                return Optional.of(order);
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to find order: " + id,
                    e
            );
        }
    }
    private Order update(Order order) {
        String sql = """
                UPDATE orders
                SET
                    listing_id = ?,
                    buyer_id = ?,
                    status = ?,
                    order_date = ?
                WHERE id = ?
                """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, order.getListing().getId());
            statement.setLong(2, order.getBuyer().getId());
            statement.setString(3, order.getStatus().name());

            if (order.getOrderDate() == null) {
                order.setOrderDate(java.time.LocalDateTime.now());
            }

            statement.setTimestamp(
                    4,
                    Timestamp.valueOf(order.getOrderDate())
            );

            statement.setLong(5, order.getId());

            int updatedRows = statement.executeUpdate();

            if (updatedRows == 0) {
                throw new IllegalArgumentException(
                        "Order not found: " + order.getId()
                );
            }

            return order;

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to update order: " + order.getId(),
                    e
            );
        }
    }

    private Listing createListingReference(Long id) {
        Listing listing = new Listing();
        listing.setId(id);
        return listing;
    }

    private User createUserReference(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }
}
