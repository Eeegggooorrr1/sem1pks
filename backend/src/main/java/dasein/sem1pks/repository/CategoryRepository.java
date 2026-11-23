package dasein.sem1pks.repository;

import dasein.sem1pks.dto.response.CategoryView;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository {

    List<CategoryView> findAll();

    Optional<CategoryView> findById(UUID id);

    Optional<UUID> findIdByCode(String code);

    void insert(CategoryView category);

    void update(UUID id, String name, boolean enabled);
}
