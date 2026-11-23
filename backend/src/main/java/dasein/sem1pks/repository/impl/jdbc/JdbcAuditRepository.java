package dasein.sem1pks.repository.impl.jdbc;

import dasein.sem1pks.dto.request.AuditFilter.Direction;
import dasein.sem1pks.dto.request.AuditFilter;
import dasein.sem1pks.dto.response.AuditEvent;
import dasein.sem1pks.dto.response.Page;
import dasein.sem1pks.repository.AuditRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class JdbcAuditRepository implements AuditRepository {

    private static final String SEARCH = """
            WHERE description ILIKE ? ESCAPE '!' OR event_type ILIKE ? ESCAPE '!'
            """;

    private static final RowMapper<AuditEvent> EVENT_MAPPER = (row, index) -> new AuditEvent(
            row.getObject("id", UUID.class),
            row.getObject("user_id", UUID.class),
            row.getString("event_type"),
            row.getString("entity_type"),
            row.getObject("entity_id", UUID.class),
            row.getObject("created_at", OffsetDateTime.class),
            row.getString("description"),
            row.getString("correlation_id")
    );

    private final JdbcTemplate jdbc;

    @Override
    public void insert(AuditEvent event) {
        jdbc.update("""
                INSERT INTO audit_events (
                    id, user_id, event_type, entity_type, entity_id, created_at, description, correlation_id
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                event.id(), event.userId(), event.eventType(), event.entityType(),
                event.entityId(), event.createdAt(), event.description(), event.correlationId());
    }

    @Override
    public Page<AuditEvent> search(AuditFilter filter) {
        String pattern = LikePattern.contains(filter.search());
        String column = switch (filter.sortBy()) {
            case CREATED_AT -> "created_at";
            case EVENT_TYPE -> "event_type";
            case ENTITY_TYPE -> "entity_type";
        };
        String direction = filter.direction() == Direction.ASC ? "ASC" : "DESC";
        String ordering = "ORDER BY %s %s, id ASC LIMIT ? OFFSET ?".formatted(column, direction);
        long offset = (long) filter.page() * filter.size();

        var events = jdbc.query(
                "SELECT * FROM audit_events " + SEARCH + ordering,
                EVENT_MAPPER, pattern, pattern, filter.size(), offset);
        long total = jdbc.queryForObject(
                "SELECT count(*) FROM audit_events " + SEARCH,
                Long.class, pattern, pattern);

        return new Page<>(events, total, filter.page(), filter.size());
    }
}
