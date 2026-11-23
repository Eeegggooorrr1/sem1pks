package dasein.sem1pks.service;

import dasein.sem1pks.exception.unauthorized.InvalidCredentialsException;
import dasein.sem1pks.repository.RefreshSessionRepository;
import dasein.sem1pks.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshSessionService {

    private final RefreshSessionRepository sessions;
    private final UserRepository users;
    private final AuditService audit;
    private final SecureRandom random = new SecureRandom();

    public String issue(UUID userId) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        sessions.replace(userId, hash(token), OffsetDateTime.now().plusDays(7));
        return token;
    }

    public void lockAccount(String email) {
        users.lockByEmail(email);
    }

    public void lockAccount(UUID userId) {
        users.lockById(userId);
    }

    public UUID consume(String token) {
        String tokenHash = hash(token);
        UUID userId = sessions.findUnrevokedOwner(tokenHash)
                .orElseThrow(InvalidCredentialsException::new);

        lockAccount(userId);

        return sessions.consume(tokenHash).orElseThrow(InvalidCredentialsException::new);
    }

    @Transactional
    public void revoke(String token) {
        sessions.revoke(hash(token)).ifPresent(userId ->
                audit.record(userId, "LOGOUT", "USER", userId, "Выход из системы"));
    }

    public void revokeUser(UUID userId) {
        sessions.revokeByUser(userId);
    }

    private String hash(String token) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
