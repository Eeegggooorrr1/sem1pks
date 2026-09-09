package dasein.sem1pks.repository.impl.jpa;

import dasein.sem1pks.domain.Order;
import dasein.sem1pks.repository.OrderRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Profile("jpa")
public interface JpaOrderRepository
        extends JpaRepository<Order, Long>, OrderRepository {

    @Override
    Optional<Order> findById(Long id);

    @Override
    @Query("""
            SELECT o
            FROM Order o
            JOIN o.listing l
            WHERE l.user.id = :userId
            ORDER BY
                CASE o.status
                    WHEN dasein.sem1pks.domain.OrderStatus.CONFIRMED THEN 0
                    WHEN dasein.sem1pks.domain.OrderStatus.PENDING THEN 1
                    WHEN dasein.sem1pks.domain.OrderStatus.COMPLETED THEN 2
                    WHEN dasein.sem1pks.domain.OrderStatus.CANCELLED THEN 3
                END,
                o.orderDate DESC
            """)
    List<Order> findIncomingOrders(@Param("userId") Long userId);

    @Override
    @Query("""
            SELECT o
            FROM Order o
            WHERE o.buyer.id = :userId
            ORDER BY
                CASE o.status
                    WHEN dasein.sem1pks.domain.OrderStatus.CONFIRMED THEN 0
                    WHEN dasein.sem1pks.domain.OrderStatus.PENDING THEN 1
                    WHEN dasein.sem1pks.domain.OrderStatus.COMPLETED THEN 2
                    WHEN dasein.sem1pks.domain.OrderStatus.CANCELLED THEN 3
                END,
                o.orderDate DESC
            """)
    List<Order> findOutgoingOrders(@Param("userId") Long userId);
}