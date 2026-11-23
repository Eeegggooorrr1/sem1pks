package dasein.sem1pks.repository;

import dasein.sem1pks.dto.request.audit.AuditFilter;
import dasein.sem1pks.dto.response.audit.AuditEvent;
import dasein.sem1pks.dto.response.common.Page;

public interface AuditRepository {

    void insert(AuditEvent event);

    Page<AuditEvent> search(AuditFilter filter);
}
