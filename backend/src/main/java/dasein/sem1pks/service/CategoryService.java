package dasein.sem1pks.service;

import dasein.sem1pks.dto.request.CategoryInput;
import dasein.sem1pks.dto.response.CategoryView;
import dasein.sem1pks.exception.badrequest.InvalidCatalogDataException;
import dasein.sem1pks.exception.notfound.CatalogEntryNotFoundException;
import dasein.sem1pks.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository repository;
    private final AuditService audit;

    public List<CategoryView> list() {
        return repository.findAll();
    }

    public CategoryView get(UUID id) {
        return repository.findById(id).orElseThrow(CatalogEntryNotFoundException::new);
    }

    public UUID idByCode(String code) {
        return repository.findIdByCode(code).orElseThrow(CatalogEntryNotFoundException::new);
    }

    @Transactional
    public CategoryView create(UUID actor, CategoryInput data) {
        UUID id = UUID.randomUUID();
        repository.insert(new CategoryView(id, data.code(), data.name().trim(), data.enabled()));

        audit.record(actor, "CATEGORY_CREATE", "CATEGORY", id, "Категория создана");
        return get(id);
    }

    @Transactional
    public CategoryView update(UUID actor, UUID id, CategoryInput data) {
        CategoryView existing = get(id);
        if (!existing.code().equals(data.code())) {
            throw new InvalidCatalogDataException("Код категории изменить нельзя");
        }

        repository.update(id, data.name().trim(), data.enabled());
        audit.record(actor, "CATEGORY_UPDATE", "CATEGORY", id, "Категория изменена");
        return get(id);
    }

    public void checkAssignable(UUID id, UUID previousCategory) {
        CategoryView category = get(id);
        if (!category.enabled() && !id.equals(previousCategory)) {
            throw new InvalidCatalogDataException("Категория отключена");
        }
    }
}
