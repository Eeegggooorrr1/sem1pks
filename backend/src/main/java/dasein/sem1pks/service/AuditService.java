package dasein.sem1pks.service;

import dasein.sem1pks.dto.request.audit.AuditFilter;
import dasein.sem1pks.dto.response.audit.AuditEvent;
import dasein.sem1pks.dto.response.common.Page;
import dasein.sem1pks.repository.AuditRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditRepository repository;

    public void record(UUID userId, String event, String entityType, UUID entityId, String description) {
        repository.insert(new AuditEvent(
                UUID.randomUUID(), userId, event, entityType, entityId,
                OffsetDateTime.now(), description, MDC.get("correlationId")
        ));
    }

    public Page<AuditEvent> search(AuditFilter filter) {
        return repository.search(filter);
    }
}
