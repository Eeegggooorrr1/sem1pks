package dasein.sem1pks.bootstrap;

import dasein.sem1pks.domain.User;
import dasein.sem1pks.domain.UserRole;
import dasein.sem1pks.repository.BootstrapRepository;
import dasein.sem1pks.repository.UserRepository;
import dasein.sem1pks.util.EmailNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class BootstrapService {
    private final BootstrapProperties properties;
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final BootstrapRepository bootstrap;

    @Transactional
    public void initialize() {
        bootstrap.lockHistory();
        ensureAdmin();

        if (properties.demoEnabled() && !bootstrap.isApplied("demo-v1")) {
            seedDemoUsers();
        }
    }

    private void ensureAdmin() {
        String email = EmailNormalizer.normalize(properties.adminEmail());
        if (email == null || !email.contains("@")) {
            throw new IllegalStateException("Bootstrap admin email is required");
        }

        var existing = users.findByEmail(email);
        if (existing.isPresent()) {
            if (existing.get().getRole() != UserRole.ADMIN) {
                throw new IllegalStateException("Bootstrap admin email belongs to a non-admin account");
            }
        } else {
            validatePassword(properties.adminPassword());
            User admin = user("Administrator", email, passwords.encode(properties.adminPassword()));
            admin.setRole(UserRole.ADMIN);
            users.save(admin);
        }
    }

    private void seedDemoUsers() {
        validatePassword(properties.demoPassword());
        String hash = passwords.encode(properties.demoPassword());

        for (int i = 1; i <= 5; i++) {
            String email = "demo" + i + "@example.com";
            if (users.existsByEmail(email)) {
                throw new IllegalStateException("Demo email already exists: " + email);
            }
            users.save(user("Demo " + i, email, hash));
        }

        bootstrap.markApplied("demo-v1");
    }

    private void validatePassword(String value) {
        if (value == null || value.length() < 8 || value.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException(
                    "Bootstrap password must contain at least 8 characters and at most 72 UTF-8 bytes");
        }
    }

    private User user(String name, String email, String hash) {
        User user = new User();
        user.setUsername(name);
        user.setEmail(email);
        user.setPasswordHash(hash);
        return user;
    }
}
