package dasein.sem1pks.repository;

import dasein.sem1pks.dto.request.AuditFilter;
import dasein.sem1pks.dto.response.AuditEvent;
import dasein.sem1pks.dto.response.Page;

public interface AuditRepository {

    void insert(AuditEvent event);

    Page<AuditEvent> search(AuditFilter filter);
}
