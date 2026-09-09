package dasein.sem1pks.service;

import dasein.sem1pks.domain.*;
import dasein.sem1pks.dto.request.OrderDirection;
import dasein.sem1pks.dto.response.OrderResponse;
import dasein.sem1pks.exception.conflict.OrderCreateConflictException;
import dasein.sem1pks.exception.conflict.OrderStateConflictException;
import dasein.sem1pks.exception.forbidden.OrderAccessDeniedException;
import dasein.sem1pks.exception.notfound.AccountNotFoundException;
import dasein.sem1pks.exception.notfound.ListingNotFoundException;
import dasein.sem1pks.exception.notfound.OrderNotFoundException;
import dasein.sem1pks.repository.ListingRepository;
import dasein.sem1pks.repository.OrderRepository;
import dasein.sem1pks.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;

    @Transactional
    public OrderResponse createOrder(
            Long buyerId,
            Long listingId
    ) {
        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new AccountNotFoundException(buyerId));

        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ListingNotFoundException(listingId));

        if (listing.getUser().getId().equals(buyerId)) {
            throw new OrderCreateConflictException(
                    "Нельзя оставить заказ на свое объявление"
            );
        }

        if (listing.getStatus() != ListingStatus.ACTIVE) {
            throw new OrderCreateConflictException(
                    "Нельзя оставить заказ на закрытое объявление"
            );
        }

        Order order = new Order();
        order.setListing(listing);
        order.setBuyer(buyer);
        order.setStatus(OrderStatus.PENDING);

        return toDto(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse confirmOrder(
            Long sellerId,
            Long orderId
    ) {
        Order order = getOrder(orderId);

        checkSeller(order, sellerId);

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new OrderStateConflictException();
        }


        order.setStatus(OrderStatus.CONFIRMED);

        return toDto(order);
    }

    @Transactional
    public OrderResponse completeOrder(
            Long buyerId,
            Long orderId
    ) {
        Order order = getOrder(orderId);

        checkBuyer(order, buyerId);

        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new OrderStateConflictException();
        }

        order.setStatus(OrderStatus.COMPLETED);

        return toDto(order);
    }

    @Transactional
    public void cancelOrder(
            Long userId,
            Long orderId
    ) {
        Order order = getOrder(orderId);

        checkParticipant(order, userId);

        if (order.getStatus() == OrderStatus.COMPLETED) {
            throw new OrderStateConflictException();
        }

        order.setStatus(OrderStatus.CANCELLED);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> findOrders(Long userId, OrderDirection direction) {
        List<Order> orders = switch (direction) {
            case INCOMING -> orderRepository.findIncomingOrders(userId);
            case OUTGOING -> orderRepository.findOutgoingOrders(userId);
        };

        return orders.stream().map(this::toDto).toList();
    }

    private Order getOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    private void checkSeller(Order order, Long userId) {
        if (!order.getListing().getUser().getId().equals(userId)) {
            throw new OrderAccessDeniedException(order.getId());
        }
    }

    private void checkBuyer(Order order, Long userId) {
        if (!order.getBuyer().getId().equals(userId)) {
            throw new OrderAccessDeniedException(order.getId());
        }
    }

    private void checkParticipant(Order order, Long userId) {
        boolean isBuyer = order.getBuyer().getId().equals(userId);
        boolean isSeller = order.getListing().getUser().getId().equals(userId);

        if (!isBuyer && !isSeller) {throw new OrderAccessDeniedException(order.getId());}
    }

    private OrderResponse toDto(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getListing().getId(),
                order.getBuyer().getId(),
                order.getListing().getUser().getId(),
                order.getStatus(),
                order.getOrderDate()
        );
    }
}
