package dasein.sem1pks.repository;

import dasein.sem1pks.domain.Order;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findById(Long id);

    List<Order> findIncomingOrders(Long userId);

    List<Order> findOutgoingOrders(Long userId);
}
