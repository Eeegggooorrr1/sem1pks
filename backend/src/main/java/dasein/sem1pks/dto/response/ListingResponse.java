package dasein.sem1pks.dto.response;

import dasein.sem1pks.domain.ListingStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ListingResponse(
        UUID id,
        String title,
        String description,
        BigDecimal price,
        ListingStatus status,
        UUID categoryId,
        UUID ownerId,
        LocalDateTime createdAt
) {}
