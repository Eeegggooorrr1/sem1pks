package dasein.sem1pks.dto.response;

import dasein.sem1pks.domain.ListingStatus;
import dasein.sem1pks.domain.Priority;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public record ListingView(
        UUID id,
        String code,
        String title,
        String description,
        BigDecimal price,
        ListingStatus status,
        UUID categoryId,
        String categoryName,
        boolean featured,
        Set<String> tags,
        Priority priority,
        boolean archived,
        UUID ownerId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        long version
) {}
