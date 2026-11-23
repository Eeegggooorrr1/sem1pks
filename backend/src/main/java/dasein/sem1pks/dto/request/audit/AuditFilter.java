package dasein.sem1pks.dto.request.audit;

public record AuditFilter(
        String search,
        int page,
        int size,
        Direction direction,
        SortBy sortBy
) {
    public enum Direction { ASC, DESC }

    public enum SortBy { CREATED_AT, EVENT_TYPE, ENTITY_TYPE }
}
