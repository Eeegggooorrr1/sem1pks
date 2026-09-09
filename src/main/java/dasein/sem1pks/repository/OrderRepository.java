package dasein.sem1pks.repository;

import dasein.sem1pks.domain.Order;
import dasein.sem1pks.domain.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

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
