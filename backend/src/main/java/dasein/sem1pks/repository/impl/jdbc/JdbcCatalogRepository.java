package dasein.sem1pks.repository.impl.jdbc;

import dasein.sem1pks.domain.ListingStatus;
import dasein.sem1pks.domain.Priority;
import dasein.sem1pks.dto.request.listing.ListingFilter.Direction;
import dasein.sem1pks.dto.request.listing.ListingFilter;
import dasein.sem1pks.dto.request.listing.ListingInput;
import dasein.sem1pks.dto.response.common.Page;
import dasein.sem1pks.dto.response.listing.ListingView;
import dasein.sem1pks.exception.conflict.CatalogConflictException;
import dasein.sem1pks.repository.CatalogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Array;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class JdbcCatalogRepository implements CatalogRepository {

    static final String PRIORITY_SQL = """
            CASE
                WHEN l.featured THEN 'HIGH'
                WHEN l.price >= 10000 THEN 'MEDIUM'
                ELSE 'LOW'
            END
            """;

    private static final String FROM = " FROM listings l JOIN categories c ON c.id = l.category_id";
    private static final String SELECT = """
            SELECT l.*, c.name AS category_name,
                   ARRAY(SELECT tag FROM listing_tags WHERE listing_id = l.id ORDER BY tag) AS tags,
                   %s AS priority
            """.formatted(PRIORITY_SQL) + FROM;

    private static final RowMapper<ListingView> LISTING_MAPPER = (row, index) -> {
        Array array = row.getArray("tags");
        Set<String> tags;
        try {
            tags = new LinkedHashSet<>(Arrays.asList((String[]) array.getArray()));
        } finally {
            array.free();
        }

        return new ListingView(
                row.getObject("id", UUID.class),
                row.getString("code"),
                row.getString("title"),
                row.getString("description"),
                row.getBigDecimal("price"),
                ListingStatus.valueOf(row.getString("status")),
                row.getObject("category_id", UUID.class),
                row.getString("category_name"),
                row.getBoolean("featured"),
                tags,
                Priority.valueOf(row.getString("priority")),
                row.getBoolean("archived"),
                row.getObject("user_id", UUID.class),
                row.getTimestamp("created_at").toLocalDateTime(),
                row.getTimestamp("updated_at").toLocalDateTime(),
                row.getLong("version")
        );
    };

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public Optional<ListingView> find(UUID id) {
        return jdbc.query(SELECT + " WHERE l.id = :id", Map.of("id", id), LISTING_MAPPER)
                .stream().findFirst();
    }

    @Override
    public Optional<ListingView> findCode(String code) {
        return jdbc.query(SELECT + " WHERE l.code = :code", Map.of("code", code), LISTING_MAPPER)
                .stream().findFirst();
    }

    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Page<ListingView> search(ListingFilter filter) {
        Query query = where(filter);
        long total = jdbc.queryForObject("SELECT count(*)" + FROM + query.sql(), query.parameters(), Long.class);
        var parameters = query.parameters()
                .addValue("limit", filter.getSize())
                .addValue("offset", (long) filter.getPage() * filter.getSize());
        List<ListingView> items = jdbc.query(
                SELECT + query.sql() + order(filter) + " LIMIT :limit OFFSET :offset",
                parameters, LISTING_MAPPER);

        return new Page<>(items, total, filter.getPage(), filter.getSize());
    }

    @Override
    public List<ListingView> all(ListingFilter filter) {
        Query query = where(filter);
        return jdbc.query(SELECT + query.sql() + order(filter), query.parameters(), LISTING_MAPPER);
    }

    @Override
    public UUID insert(UUID owner, ListingInput data) {
        UUID id = UUID.randomUUID();
        var parameters = parameters(data).addValue("id", id).addValue("owner", owner);
        jdbc.update("""
                INSERT INTO listings (
                    id, code, title, description, price, status, category_id, featured, user_id, created_at, version
                )
                VALUES (
                    :id, :code, :title, :description, :price, :status, :category, :featured, :owner, CURRENT_TIMESTAMP, 0
                )
                """, parameters);
        replaceTags(id, data.tags());
        return id;
    }

    @Override
    public void update(UUID id, long version, ListingInput data) {
        var parameters = parameters(data).addValues(Map.of("id", id, "version", version));
        requireCurrentVersion("""
                UPDATE listings SET
                    code = :code,
                    title = :title,
                    description = :description,
                    price = :price,
                    status = :status,
                    category_id = :category,
                    featured = :featured,
                    version = version + 1
                WHERE id = :id AND version = :version
                """, parameters);
        replaceTags(id, data.tags());
    }

    @Override
    public void archive(UUID id, long version, boolean archived) {
        requireCurrentVersion("""
                UPDATE listings SET archived = :archived, version = version + 1
                WHERE id = :id AND version = :version
                """, forVersion(id, version).addValue("archived", archived));
    }

    @Override
    public void close(UUID id, long version) {
        requireCurrentVersion("""
                UPDATE listings SET status = 'CLOSED', version = version + 1
                WHERE id = :id AND version = :version
                """, forVersion(id, version));
    }

    @Override
    public void delete(UUID id, long version) {
        requireCurrentVersion("DELETE FROM listings WHERE id = :id AND version = :version", forVersion(id, version));
    }

    private void requireCurrentVersion(String sql, SqlParameterSource parameters) {
        if (jdbc.update(sql, parameters) != 1) {
            throw new CatalogConflictException("Запись изменена другим пользователем");
        }
    }

    private MapSqlParameterSource forVersion(UUID id, long version) {
        return new MapSqlParameterSource("id", id).addValue("version", version);
    }

    private void replaceTags(UUID id, Set<String> tags) {
        jdbc.update("DELETE FROM listing_tags WHERE listing_id = :id", Map.of("id", id));
        for (String tag : tags) {
            jdbc.update(
                    "INSERT INTO listing_tags (listing_id, tag) VALUES (:id, :tag)",
                    Map.of("id", id, "tag", tag));
        }
    }

    private MapSqlParameterSource parameters(ListingInput data) {
        return new MapSqlParameterSource()
                .addValue("code", data.code())
                .addValue("title", data.title().trim())
                .addValue("description", data.description())
                .addValue("price", data.price())
                .addValue("status", data.status().name())
                .addValue("category", data.categoryId())
                .addValue("featured", data.featured());
    }

    private String order(ListingFilter filter) {
        String column = switch (filter.getSortBy()) {
            case TITLE -> "l.title";
            case PRICE -> "l.price";
            case CREATED_AT -> "l.created_at";
            case UPDATED_AT -> "l.updated_at";
        };
        String direction = filter.getDirection() == Direction.ASC ? "ASC" : "DESC";

        return " ORDER BY %s %s, l.id ASC".formatted(column, direction);
    }

    private Query where(ListingFilter filter) {
        var conditions = new StringBuilder(" WHERE TRUE");
        var parameters = new MapSqlParameterSource();

        if (filter.getSearch() != null && !filter.getSearch().isBlank()) {
            conditions.append("""
                     AND (l.title ILIKE :search ESCAPE '!' OR l.description ILIKE :search ESCAPE '!'
                          OR l.code ILIKE :search ESCAPE '!')
                    """);
            parameters.addValue("search", LikePattern.contains(filter.getSearch()));
        }

        addEquals(conditions, parameters, "c.id", "category", filter.getCategoryId());
        addEquals(conditions, parameters, "l.featured", "featured", filter.getFeatured());
        addEquals(conditions, parameters, "l.archived", "archived", filter.getArchived());

        if (filter.getStatus() != null) {
            addEquals(conditions, parameters, "l.status", "status", filter.getStatus().name());
        }
        if (filter.getPriority() != null) {
            addEquals(conditions, parameters, "(" + PRIORITY_SQL + ")", "priority", filter.getPriority().name());
        }
        if (filter.getTag() != null) {
            conditions.append("""
                     AND EXISTS (
                         SELECT 1 FROM listing_tags t WHERE t.listing_id = l.id AND t.tag = :tag
                     )
                    """);
            parameters.addValue("tag", filter.getTag());
        }
        if (filter.getPrice() != null) {
            String operator = switch (filter.getPriceOp()) {
                case EQ -> "=";
                case GT -> ">";
                case GTE -> ">=";
                case LT -> "<";
                case LTE -> "<=";
            };
            conditions.append(" AND l.price ").append(operator).append(" :price");
            parameters.addValue("price", filter.getPrice());
        }

        return new Query(conditions.toString(), parameters);
    }

    private void addEquals(
            StringBuilder conditions,
            MapSqlParameterSource parameters,
            String column,
            String parameter,
            Object value
    ) {
        if (value != null) {
            conditions.append(" AND ").append(column).append(" = :").append(parameter);
            parameters.addValue(parameter, value);
        }
    }

    private record Query(
            String sql,
            MapSqlParameterSource parameters
    ) {}
}
