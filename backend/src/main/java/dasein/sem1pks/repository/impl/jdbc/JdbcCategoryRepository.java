package dasein.sem1pks.repository.impl.jdbc;

import dasein.sem1pks.dto.response.category.CategoryView;
import dasein.sem1pks.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class JdbcCategoryRepository implements CategoryRepository {

    private static final RowMapper<CategoryView> CATEGORY_MAPPER = (row, index) -> new CategoryView(
            row.getObject("id", UUID.class),
            row.getString("code"),
            row.getString("name"),
            row.getBoolean("enabled")
    );

    private final JdbcTemplate jdbc;

    @Override
    public List<CategoryView> findAll() {
        return jdbc.query("SELECT id, code, name, enabled FROM categories ORDER BY name, id", CATEGORY_MAPPER);
    }

    @Override
    public Optional<CategoryView> findById(UUID id) {
        return jdbc.query(
                "SELECT id, code, name, enabled FROM categories WHERE id = ?",
                CATEGORY_MAPPER, id
        ).stream().findFirst();
    }

    @Override
    public Optional<UUID> findIdByCode(String code) {
        return jdbc.query(
                "SELECT id FROM categories WHERE code = ?",
                (row, index) -> row.getObject(1, UUID.class), code
        ).stream().findFirst();
    }

    @Override
    public void insert(CategoryView category) {
        jdbc.update("""
                INSERT INTO categories (id, code, name, enabled) VALUES (?, ?, ?, ?)
                """, category.id(), category.code(), category.name(), category.enabled());
    }

    @Override
    public void update(UUID id, String name, boolean enabled) {
        jdbc.update("UPDATE categories SET name = ?, enabled = ? WHERE id = ?", name, enabled, id);
    }
}
