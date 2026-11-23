package dasein.sem1pks.repository;

import dasein.sem1pks.domain.Order;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findById(UUID id);

    List<Order> findIncomingOrders(UUID userId);

    List<Order> findOutgoingOrders(UUID userId);
}
