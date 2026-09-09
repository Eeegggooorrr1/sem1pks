package dasein.sem1pks.dto.response;

import dasein.sem1pks.domain.OrderStatus;

import java.time.LocalDateTime;

public record OrderResponse(
        Long id,
        Long listingId,
        Long buyerId,
        Long sellerId,
        OrderStatus status,
        LocalDateTime orderDate
) {}
