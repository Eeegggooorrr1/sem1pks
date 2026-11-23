package dasein.sem1pks.repository.impl.jdbc;

import dasein.sem1pks.repository.ExportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Repository
@RequiredArgsConstructor
public class JdbcExportRepository implements ExportRepository {

    private final JdbcTemplate jdbc;

    @Override
    public void read(Table table, Consumer<List<String>> header, Consumer<List<String>> row) {
        jdbc.query(queryFor(table), result -> {
            List<String> columns = columnNames(result.getMetaData());
            header.accept(columns);

            while (result.next()) {
                row.accept(values(result, columns.size()));
            }

            return null;
        });
    }

    private List<String> columnNames(ResultSetMetaData metadata) throws SQLException {
        List<String> names = new ArrayList<>(metadata.getColumnCount());
        for (int index = 1; index <= metadata.getColumnCount(); index++) {
            names.add(metadata.getColumnLabel(index));
        }
        return names;
    }

    private List<String> values(ResultSet result, int columnCount) throws SQLException {
        List<String> values = new ArrayList<>(columnCount);
        for (int index = 1; index <= columnCount; index++) {
            Object value = result.getObject(index);
            values.add(value == null ? "" : value.toString());
        }
        return values;
    }

    private String queryFor(Table table) {
        return switch (table) {
            case USERS -> "SELECT id, username, email, is_blocked, role FROM users ORDER BY id";
            case LISTINGS -> """
                    SELECT id, code, title, description, price, status, user_id, category_id,
                           featured, archived, created_at, updated_at, version
                    FROM listings ORDER BY id
                    """;
            case ORDERS -> "SELECT id, listing_id, buyer_id, status, order_date, version FROM orders ORDER BY id";
            case CATEGORIES -> "SELECT id, code, name, enabled FROM categories ORDER BY id";
            case NOTES -> """
                    SELECT id, listing_id, author_id, body, pinned, created_at, updated_at
                    FROM listing_notes ORDER BY id
                    """;
            case TAGS -> "SELECT listing_id, tag FROM listing_tags ORDER BY listing_id, tag";
        };
    }
}
