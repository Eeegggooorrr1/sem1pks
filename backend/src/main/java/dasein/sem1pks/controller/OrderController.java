package dasein.sem1pks.controller;

import dasein.sem1pks.config.CurrentUser;
import dasein.sem1pks.dto.request.order.OrderCreateRequest;
import dasein.sem1pks.dto.request.order.OrderDirection;
import dasein.sem1pks.dto.response.order.OrderResponse;
import dasein.sem1pks.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Validated
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody OrderCreateRequest request
    ) {
        OrderResponse response = orderService.createOrder(currentUser.id(), request.listingId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{orderId}/confirm")
    public ResponseEntity<OrderResponse> confirmOrder(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable UUID orderId
    ) {
        return ResponseEntity.ok(orderService.confirmOrder(currentUser.id(), orderId));
    }

    @PostMapping("/listings/{listingUuid}")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse createByUuid(@AuthenticationPrincipal CurrentUser user,@PathVariable UUID listingUuid) {
        return orderService.createOrder(user.id(), listingUuid);
    }

    @PatchMapping("/{orderId}/complete")
    public ResponseEntity<OrderResponse> completeOrder(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable UUID orderId
    ) {
        return ResponseEntity.ok(orderService.completeOrder(currentUser.id(), orderId));
    }

    @PatchMapping("/{orderId}/cancel")
    public ResponseEntity<Void> cancelOrder(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable UUID orderId
    ) {
        orderService.cancelOrder(currentUser.id(), orderId);

        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> findOrders(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam OrderDirection direction
    ) {
        return ResponseEntity.ok(orderService.findOrders(currentUser.id(), direction));
    }
}
