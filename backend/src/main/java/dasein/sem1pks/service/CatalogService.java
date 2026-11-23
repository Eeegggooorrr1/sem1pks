package dasein.sem1pks.service;

import dasein.sem1pks.domain.ListingStatus;
import dasein.sem1pks.dto.request.listing.ListingFilter;
import dasein.sem1pks.dto.request.listing.ListingInput;
import dasein.sem1pks.dto.request.listing.UpdateListing;
import dasein.sem1pks.dto.response.common.Page;
import dasein.sem1pks.dto.response.listing.ListingView;
import dasein.sem1pks.exception.conflict.CatalogConflictException;
import dasein.sem1pks.exception.forbidden.CatalogAccessDeniedException;
import dasein.sem1pks.exception.notfound.CatalogEntryNotFoundException;
import dasein.sem1pks.repository.CatalogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CatalogService {

    private final CatalogRepository repository;
    private final CategoryService categories;
    private final AuditService audit;

    public Page<ListingView> search(ListingFilter filter) {
        if (!isAdmin()) {
            filter.setArchived(false);
        }
        return repository.search(filter);
    }

    public ListingView get(UUID id) {
        return repository.find(id).orElseThrow(CatalogEntryNotFoundException::new);
    }

    public ListingView visible(UUID id) {
        var listing = get(id);
        if (listing.archived() && !isAdmin()) {
            throw new CatalogEntryNotFoundException();
        }
        return listing;
    }

    @Transactional
    public ListingView create(UUID actor, ListingInput data) {
        categories.checkAssignable(data.categoryId(), null);

        UUID id = repository.insert(actor, data);
        audit.record(actor, "LISTING_CREATE", "LISTING", id, "Объявление создано");
        return get(id);
    }

    @Transactional
    public ListingView update(UUID actor, UUID id, UpdateListing request) {
        var listing = editable(actor, id, request.version());
        if (!isAdmin() && listing.status() == ListingStatus.CLOSED) {
            throw new CatalogConflictException("Закрытое объявление нельзя изменить");
        }

        var data = request.data();
        categories.checkAssignable(data.categoryId(), listing.categoryId());

        repository.update(id, request.version(), data);
        audit.record(actor, "LISTING_UPDATE", "LISTING", id, "Объявление изменено");
        return get(id);
    }

    @Transactional
    public ListingView archive(UUID actor, UUID id, long version, boolean archived) {
        requireAdmin();
        editable(actor, id, version);

        repository.archive(id, version, archived);
        String event = archived ? "LISTING_ARCHIVE" : "LISTING_RESTORE";
        String description = archived ? "Объявление архивировано" : "Объявление восстановлено";
        audit.record(actor, event, "LISTING", id, description);
        return get(id);
    }

    @Transactional
    public ListingView close(UUID actor, UUID id, long version) {
        var listing = editable(actor, id, version);
        if (listing.status() == ListingStatus.CLOSED) {
            return listing;
        }

        repository.close(id, version);
        audit.record(actor, "LISTING_CLOSE", "LISTING", id, "Объявление закрыто");
        return get(id);
    }

    @Transactional
    public void delete(UUID actor, UUID id, long version) {
        requireAdmin();
        editable(actor, id, version);

        repository.delete(id, version);
        audit.record(actor, "LISTING_DELETE", "LISTING", id, "Объявление удалено");
    }

    private ListingView editable(UUID actor, UUID id, long version) {
        var listing = visible(id);
        if (!listing.ownerId().equals(actor) && !isAdmin()) {
            throw new CatalogAccessDeniedException();
        }
        if (listing.version() != version) {
            throw new CatalogConflictException("Запись изменена другим пользователем");
        }
        return listing;
    }

    private void requireAdmin() {
        if (!isAdmin()) {
            throw new CatalogAccessDeniedException();
        }
    }

    public static boolean isAdmin() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null
                && auth.getAuthorities().stream().anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }
}
