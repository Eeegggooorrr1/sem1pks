package dasein.sem1pks.repository.impl.jdbc;

import dasein.sem1pks.repository.BootstrapRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JdbcBootstrapRepository implements BootstrapRepository {

    private final JdbcTemplate jdbc;

    @Override
    public void lockHistory() {
        jdbc.execute("LOCK TABLE bootstrap_history IN EXCLUSIVE MODE");
    }

    @Override
    public boolean isApplied(String name) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM bootstrap_history WHERE name = ?)", Boolean.class, name));
    }

    @Override
    public void markApplied(String name) {
        jdbc.update("INSERT INTO bootstrap_history (name) VALUES (?)", name);
    }
}
