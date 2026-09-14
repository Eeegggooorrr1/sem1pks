package dasein.sem1pks.repository.impl.jdbc;

import dasein.sem1pks.domain.User;
import dasein.sem1pks.domain.UserRole;
import dasein.sem1pks.mapper.UserRowMapper;
import dasein.sem1pks.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.Optional;

@Repository
@Profile("jdbc")
@RequiredArgsConstructor
public class JdbcUserRepository implements UserRepository {

    private final DataSource dataSource;
    private final UserRowMapper userRowMapper;

    @Override
    public User save(User user) {
        if (user.getId() == null) {
            return insert(user);
        }

        return update(user);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        String sql = """
                SELECT
                    id,
                    username,
                    email,
                    password_hash,
                    is_blocked,
                    role
                FROM users
                WHERE email = ?
                """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, email);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }

                return Optional.of(userRowMapper.map(resultSet));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find user by email", e);
        }
    }

    @Override
    public boolean existsByEmail(String email) {
        String sql = """
                SELECT 1
                FROM users
                WHERE email = ?
                LIMIT 1
                """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, email);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to check user existence by email", e);
        }
    }

    private User insert(User user) {
        String sql = """
                INSERT INTO users (
                    username,
                    email,
                    password_hash,
                    is_blocked,
                    role
                )
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     sql,
                     Statement.RETURN_GENERATED_KEYS
             )) {

            statement.setString(1, user.getUsername());
            statement.setString(2, user.getEmail());
            statement.setString(3, user.getPasswordHash());
            statement.setBoolean(4, user.isBlocked());
            statement.setString(5, user.getRole().name());

            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (!generatedKeys.next()) {
                    throw new SQLException("Failed to retrieve generated user id");
                }

                user.setId(generatedKeys.getLong(1));
            }

            return user;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert user", e);
        }
    }

    @Override
    public Optional<User> findById(Long id) {
        String sql = """
            SELECT
                id,
                username,
                email,
                password_hash,
                is_blocked,
                role
            FROM users
            WHERE id = ?
            """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }

                return Optional.of(userRowMapper.map(resultSet));
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to find user: " + id,
                    e
            );
        }
    }

    private User update(User user) {
        String sql = """
                UPDATE users
                SET
                    username = ?,
                    email = ?,
                    password_hash = ?,
                    is_blocked = ?,
                    role = ?
                WHERE id = ?
                """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, user.getUsername());
            statement.setString(2, user.getEmail());
            statement.setString(3, user.getPasswordHash());
            statement.setBoolean(4, user.isBlocked());
            statement.setString(5, user.getRole().name());
            statement.setLong(6, user.getId());

            int updatedRows = statement.executeUpdate();

            if (updatedRows == 0) {
                throw new IllegalArgumentException(
                        "User not found: " + user.getId()
                );
            }

            return user;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update user", e);
        }
    }
}