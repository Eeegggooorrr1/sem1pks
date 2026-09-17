package dasein.sem1pks.repository.impl.jdbc;

import dasein.sem1pks.domain.User;
import dasein.sem1pks.mapper.UserRowMapper;
import dasein.sem1pks.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@Profile("jdbc")
@RequiredArgsConstructor
public class JdbcUserRepository implements UserRepository {
    private final JdbcTemplate jdbc;
    private final UserRowMapper mapper;

    @Override
    public User save(User user) {
        if (user.getId() == null) {
            user.setId(jdbc.queryForObject("""
                    INSERT INTO users(username, email, password_hash, is_blocked, role)
                    VALUES (?, ?, ?, ?, ?) RETURNING id
                    """, Long.class, user.getUsername(), user.getEmail(), user.getPasswordHash(),
                    user.isBlocked(), user.getRole().name()));
        } else {
            int updated = jdbc.update("""
                    UPDATE users SET username=?, email=?, password_hash=?, is_blocked=?, role=?
                    WHERE id=?
                    """, user.getUsername(), user.getEmail(), user.getPasswordHash(),
                    user.isBlocked(), user.getRole().name(), user.getId());
            if (updated != 1) {
                throw new EmptyResultDataAccessException(1);
            }
        }
        return user;
    }

    @Override
    public Optional<User> findById(Long id) {
        return jdbc.query("SELECT * FROM users WHERE id=?", (rs, row) -> mapper.map(rs), id)
                .stream().findFirst();
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jdbc.query("SELECT * FROM users WHERE email=?", (rs, row) -> mapper.map(rs), email)
                .stream().findFirst();
    }

    @Override
    public boolean existsByEmail(String email) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM users WHERE email=?)", Boolean.class, email));
    }
}
