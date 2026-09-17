package dasein.sem1pks.bootstrap;

import dasein.sem1pks.domain.*;
import dasein.sem1pks.repository.*;
import dasein.sem1pks.util.EmailNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BootstrapService {
    private final BootstrapProperties properties;
    private final UserRepository users;
    private final ListingRepository listings;
    private final OrderRepository orders;
    private final PasswordEncoder passwords;
    private final JdbcTemplate jdbc;

    @Transactional
    public void initialize() {
        jdbc.execute("LOCK TABLE bootstrap_history IN EXCLUSIVE MODE");
        String email = EmailNormalizer.normalize(properties.adminEmail());
        if (email == null || !email.contains("@")) throw new IllegalStateException("Bootstrap admin email is required");
        var existing = users.findByEmail(email);
        if (existing.isPresent()) {
            if (existing.get().getRole() != UserRole.ADMIN)
                throw new IllegalStateException("Bootstrap admin email belongs to a non-admin account");
        } else {
            validatePassword(properties.adminPassword());
            User admin = user("Administrator", email, passwords.encode(properties.adminPassword()));
            admin.setRole(UserRole.ADMIN);
            users.save(admin);
        }
        if (!properties.demoEnabled() || Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM bootstrap_history WHERE name='demo-v1')", Boolean.class))) return;
        validatePassword(properties.demoPassword());
        String hash = passwords.encode(properties.demoPassword());
        List<User> demoUsers = new ArrayList<>();
        for (int i=1; i<=5; i++) {
            String demoEmail = "demo" + i + "@example.com";
            if (users.existsByEmail(demoEmail)) throw new IllegalStateException("Demo email already exists: " + demoEmail);
            demoUsers.add(users.save(user("Demo " + i, demoEmail, hash)));
        }
        String[] titles = {"Велосипед", "Книжная полка", "Монитор", "Набор книг", "Настольная лампа",
                           "Гитара", "Рюкзак", "Кресло", "Фотоаппарат", "Самокат"};
        List<Listing> demoListings = new ArrayList<>();
        for (int i=0; i<titles.length; i++) {
            Listing listing = new Listing();
            listing.setTitle(titles[i]);
            listing.setDescription("Демонстрационное объявление " + (i+1));
            listing.setPrice(BigDecimal.valueOf((i+1)*1000L));
            listing.setUser(demoUsers.get(i%5));
            listing.setCategory(ListingCategory.OTHER);
            demoListings.add(listings.save(listing));
        }
        for (int i=0; i<5; i++) {
            Order order = new Order();
            order.setListing(demoListings.get(i));
            order.setBuyer(demoUsers.get((i+1)%5));
            orders.save(order);
        }
        jdbc.update("INSERT INTO bootstrap_history(name) VALUES ('demo-v1')");
    }

    private void validatePassword(String value) {
        if (value == null || value.length()<8 || value.getBytes(StandardCharsets.UTF_8).length>72)
            throw new IllegalStateException("Bootstrap password must contain at least 8 characters and at most 72 UTF-8 bytes");
    }
    private User user(String name, String email, String hash) {
        User user = new User();
        user.setUsername(name); user.setEmail(email); user.setPasswordHash(hash);
        return user;
    }
}
