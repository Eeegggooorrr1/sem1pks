package dasein.sem1pks.dto.response;

import dasein.sem1pks.domain.ListingCategory;
import dasein.sem1pks.domain.ListingStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ListingResponse(
        Long id,
        String title,
        String description,
        BigDecimal price,
        ListingStatus status,
        ListingCategory category,
        Long ownerId,
        LocalDateTime createdAt
) {}