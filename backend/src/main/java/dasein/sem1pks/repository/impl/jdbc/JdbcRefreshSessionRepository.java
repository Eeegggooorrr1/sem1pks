package dasein.sem1pks.repository.impl.jdbc;

import dasein.sem1pks.repository.RefreshSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class JdbcRefreshSessionRepository implements RefreshSessionRepository {

    private static final RowMapper<UUID> OWNER_MAPPER = (row, index) -> row.getObject("user_id", UUID.class);

    private final JdbcTemplate jdbc;

    @Override
    public void replace(UUID userId, String tokenHash, OffsetDateTime expiresAt) {
        jdbc.update("""
                INSERT INTO refresh_sessions (user_id, token_hash, expires_at)
                VALUES (?, ?, ?)
                ON CONFLICT (user_id) DO UPDATE SET
                    token_hash = EXCLUDED.token_hash,
                    expires_at = EXCLUDED.expires_at,
                    revoked_at = NULL
                """, userId, tokenHash, expiresAt);
    }

    @Override
    public Optional<UUID> findUnrevokedOwner(String tokenHash) {
        return jdbc.query("""
                SELECT user_id FROM refresh_sessions
                WHERE token_hash = ? AND revoked_at IS NULL
                """, OWNER_MAPPER, tokenHash).stream().findFirst();
    }

    @Override
    public Optional<UUID> consume(String tokenHash) {
        return jdbc.query("""
                UPDATE refresh_sessions SET revoked_at = CURRENT_TIMESTAMP
                WHERE token_hash = ? AND revoked_at IS NULL AND expires_at > clock_timestamp()
                RETURNING user_id
                """, OWNER_MAPPER, tokenHash).stream().findFirst();
    }

    @Override
    public Optional<UUID> revoke(String tokenHash) {
        return jdbc.query("""
                UPDATE refresh_sessions SET revoked_at = CURRENT_TIMESTAMP
                WHERE token_hash = ? AND revoked_at IS NULL
                RETURNING user_id
                """, OWNER_MAPPER, tokenHash).stream().findFirst();
    }

    @Override
    public void revokeByUser(UUID userId) {
        jdbc.update("""
                UPDATE refresh_sessions SET revoked_at = CURRENT_TIMESTAMP
                WHERE user_id = ? AND revoked_at IS NULL
                """, userId);
    }
}
