package dasein.sem1pks.mapper;

import dasein.sem1pks.domain.User;
import dasein.sem1pks.domain.UserRole;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class UserRowMapper {

    public User map(ResultSet resultSet) throws SQLException {
        User user = new User();

        user.setId(resultSet.getLong("id"));
        user.setUsername(resultSet.getString("username"));
        user.setEmail(resultSet.getString("email"));
        user.setPasswordHash(resultSet.getString("password_hash"));
        user.setBlocked(resultSet.getBoolean("is_blocked"));
        user.setRole(
                UserRole.valueOf(resultSet.getString("role"))
        );

        return user;
    }
}
