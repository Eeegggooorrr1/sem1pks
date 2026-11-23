package dasein.sem1pks.dto.response.order;

import dasein.sem1pks.domain.OrderStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID listingId,
        UUID buyerId,
        UUID sellerId,
        OrderStatus status,
        LocalDateTime orderDate,
        String listingTitle
) {}
