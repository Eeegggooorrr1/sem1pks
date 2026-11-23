package dasein.sem1pks.repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface RefreshSessionRepository {

    void replace(UUID userId, String tokenHash, OffsetDateTime expiresAt);

    Optional<UUID> findUnrevokedOwner(String tokenHash);

    Optional<UUID> consume(String tokenHash);

    Optional<UUID> revoke(String tokenHash);

    void revokeByUser(UUID userId);
}
