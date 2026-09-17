package dasein.sem1pks.repository.impl.jdbc;

import dasein.sem1pks.domain.Order;
import dasein.sem1pks.mapper.OrderRowMapper;
import dasein.sem1pks.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@Profile("jdbc")
@RequiredArgsConstructor
public class JdbcOrderRepository implements OrderRepository {
    private static final String SELECT = """
            SELECT o.*, l.user_id AS seller_id FROM orders o JOIN listings l ON l.id=o.listing_id
            """;
    private static final String SORT = """
            ORDER BY CASE o.status WHEN 'CONFIRMED' THEN 0 WHEN 'PENDING' THEN 1
                                   WHEN 'COMPLETED' THEN 2 ELSE 3 END,
                     o.order_date DESC, o.id DESC
            """;
    private final JdbcTemplate jdbc;
    private final OrderRowMapper mapper;

    @Override
    public Order save(Order order) {
        if (order.getId() == null) {
            if (order.getOrderDate() == null) {
                order.setOrderDate(LocalDateTime.now());
            }
            order.setId(jdbc.queryForObject("""
                    INSERT INTO orders(listing_id, buyer_id, status, order_date)
                    VALUES (?, ?, ?, ?) RETURNING id
                    """, Long.class, order.getListing().getId(), order.getBuyer().getId(),
                    order.getStatus().name(), order.getOrderDate()));
        } else {
            int updated = jdbc.update("""
                    UPDATE orders SET status=?, version=version+1 WHERE id=? AND version=?
                    """, order.getStatus().name(), order.getId(), order.getVersion());
            if (updated != 1) {
                throw new OptimisticLockingFailureException("Order was modified concurrently");
            }
            order.setVersion(order.getVersion() + 1);
        }
        return order;
    }

    @Override
    public Optional<Order> findById(Long id) {
        return jdbc.query(SELECT + " WHERE o.id=?", (rs, row) -> mapper.map(rs), id)
                .stream().findFirst();
    }

    @Override
    public List<Order> findIncomingOrders(Long userId) {
        return jdbc.query(SELECT + " WHERE l.user_id=? " + SORT, (rs, row) -> mapper.map(rs), userId);
    }

    @Override
    public List<Order> findOutgoingOrders(Long userId) {
        return jdbc.query(SELECT + " WHERE o.buyer_id=? " + SORT, (rs, row) -> mapper.map(rs), userId);
    }
}
