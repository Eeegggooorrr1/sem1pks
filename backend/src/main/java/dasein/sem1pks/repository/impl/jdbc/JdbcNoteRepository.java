package dasein.sem1pks.repository.impl.jdbc;

import dasein.sem1pks.dto.response.note.NoteView;
import dasein.sem1pks.repository.NoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class JdbcNoteRepository implements NoteRepository {

    private static final String SELECT = """
            SELECT n.*, u.username
            FROM listing_notes n JOIN users u ON u.id = n.author_id
            WHERE n.listing_id = ?
            """;

    private static final RowMapper<NoteView> NOTE_MAPPER = (row, index) -> new NoteView(
            row.getObject("id", UUID.class),
            row.getObject("author_id", UUID.class),
            row.getString("username"),
            row.getString("body"),
            row.getBoolean("pinned"),
            row.getObject("created_at", OffsetDateTime.class),
            row.getObject("updated_at", OffsetDateTime.class)
    );

    private final JdbcTemplate jdbc;

    @Override
    public List<NoteView> findByListing(UUID listingId) {
        return jdbc.query(
                SELECT + "ORDER BY n.pinned DESC, n.created_at, n.id",
                NOTE_MAPPER, listingId);
    }

    @Override
    public Optional<NoteView> find(UUID listingId, UUID noteId) {
        return jdbc.query(SELECT + "AND n.id = ?", NOTE_MAPPER, listingId, noteId)
                .stream().findFirst();
    }

    @Override
    public void insert(UUID id, UUID listingId, UUID authorId, String body, boolean pinned) {
        jdbc.update("""
                INSERT INTO listing_notes (id, listing_id, author_id, body, pinned)
                VALUES (?, ?, ?, ?, ?)
                """, id, listingId, authorId, body, pinned);
    }

    @Override
    public void update(UUID id, String body, boolean pinned) {
        jdbc.update("""
                UPDATE listing_notes SET body = ?, pinned = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """, body, pinned, id);
    }

    @Override
    public void delete(UUID id) {
        jdbc.update("DELETE FROM listing_notes WHERE id = ?", id);
    }
}
