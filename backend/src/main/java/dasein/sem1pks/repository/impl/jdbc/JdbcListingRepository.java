package dasein.sem1pks.repository.impl.jdbc;

import dasein.sem1pks.domain.Listing;
import dasein.sem1pks.domain.ListingCategory;
import dasein.sem1pks.domain.ListingStatus;
import dasein.sem1pks.mapper.ListingRowMapper;
import dasein.sem1pks.repository.ListingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
@Profile("jdbc")
@RequiredArgsConstructor
public class JdbcListingRepository implements ListingRepository {
    private final JdbcTemplate jdbc;
    private final ListingRowMapper mapper;

    @Override
    public Listing save(Listing listing) {
        if (listing.getId() == null) {
            if (listing.getCreatedAt() == null) {
                listing.setCreatedAt(LocalDateTime.now());
            }
            listing.setId(jdbc.queryForObject("""
                    INSERT INTO listings(title, description, price, status, user_id, category, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id
                    """, Long.class, listing.getTitle(), listing.getDescription(), listing.getPrice(),
                    listing.getStatus().name(), listing.getUser().getId(),
                    listing.getCategory().name(), listing.getCreatedAt()));
        } else {
            int updated = jdbc.update("""
                    UPDATE listings SET title=?, description=?, price=?, status=?, user_id=?,
                                        category=?, version=version+1
                    WHERE id=? AND version=?
                    """, listing.getTitle(), listing.getDescription(), listing.getPrice(),
                    listing.getStatus().name(), listing.getUser().getId(), listing.getCategory().name(),
                    listing.getId(), listing.getVersion());
            if (updated != 1) {
                throw new OptimisticLockingFailureException("Listing was modified concurrently");
            }
            listing.setVersion(listing.getVersion() + 1);
        }
        return listing;
    }

    @Override
    public Optional<Listing> findById(Long id) {
        return jdbc.query("SELECT * FROM listings WHERE id=?", (rs, row) -> mapper.map(rs), id)
                .stream().findFirst();
    }

    @Override
    public List<Listing> findByUserIdOrderByCreatedAtDesc(Long userId) {
        return jdbc.query("SELECT * FROM listings WHERE user_id=? ORDER BY created_at DESC, id DESC",
                (rs, row) -> mapper.map(rs), userId);
    }

    @Override
    public List<Listing> search(String prefix, ListingCategory category, ListingStatus closedStatus,
                                ListingStatus soldStatus, Sort sort) {
        StringBuilder sql = new StringBuilder("SELECT * FROM listings WHERE status<>?");
        List<Object> args = new ArrayList<>();
        args.add(closedStatus.name());
        if (prefix != null) {
            sql.append(" AND lower(title) LIKE lower(?) ESCAPE '!'");
            args.add(prefix + "%");
        }
        if (category != null) {
            sql.append(" AND category=?");
            args.add(category.name());
        }
        sql.append(" ORDER BY CASE WHEN status=? THEN 1 ELSE 0 END");
        args.add(soldStatus.name());

        for (Sort.Order order : sort) {
            String column = switch (order.getProperty()) {
                case "id", "price", "title", "status", "category" -> order.getProperty();
                case "createdAt" -> "created_at";
                default -> throw new IllegalArgumentException("Unsupported sort: " + order.getProperty());
            };
            sql.append(", ").append(column).append(order.isAscending() ? " ASC" : " DESC");
        }
        return jdbc.query(sql.toString(), (rs, row) -> mapper.map(rs), args.toArray());
    }
}
