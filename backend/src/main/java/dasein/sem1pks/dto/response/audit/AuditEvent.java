package dasein.sem1pks.dto.response.audit;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AuditEvent(
        UUID id,
        UUID userId,
        String eventType,
        String entityType,
        UUID entityId,
        OffsetDateTime createdAt,
        String description,
        String correlationId
) {}
