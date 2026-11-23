package dasein.sem1pks.service;

import dasein.sem1pks.dto.request.bulk.BulkAction;
import dasein.sem1pks.dto.request.bulk.BulkItem;
import dasein.sem1pks.dto.request.bulk.BulkRequest;
import dasein.sem1pks.dto.request.listing.ListingInput;
import dasein.sem1pks.dto.request.listing.UpdateListing;
import dasein.sem1pks.dto.response.bulk.BulkError;
import dasein.sem1pks.dto.response.bulk.BulkResult;
import dasein.sem1pks.exception.DomainException;
import dasein.sem1pks.exception.badrequest.InvalidCatalogDataException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BulkService {

    private final CatalogService listings;

    public BulkResult apply(UUID actor, BulkRequest request) {
        validate(request);

        List<UUID> successful = new ArrayList<>();
        List<BulkError> errors = new ArrayList<>();
        for (BulkItem item : request.items()) {
            try {
                applyItem(actor, item, request);
                successful.add(item.id());
            } catch (DomainException ex) {
                errors.add(new BulkError(item.id(), ex.getErrorCode(), ex.getMessage()));
            } catch (DataIntegrityViolationException ex) {
                errors.add(new BulkError(item.id(), "CONFLICT", "Операция конфликтует с текущими данными"));
            }
        }

        return new BulkResult(successful, errors);
    }

    private void validate(BulkRequest request) {
        BulkAction action = request.action();
        if (action == BulkAction.STATUS && request.status() == null) {
            throw new InvalidCatalogDataException("Выберите новый статус.");
        }
        if ((action == BulkAction.ADD_TAG || action == BulkAction.REMOVE_TAG) && request.tag() == null) {
            throw new InvalidCatalogDataException("Укажите тег.");
        }

        long distinctItems = request.items().stream().map(BulkItem::id).distinct().count();
        if (distinctItems != request.items().size()) {
            throw new InvalidCatalogDataException("Укажите каждое объявление один раз.");
        }
    }

    private void applyItem(UUID actor, BulkItem item, BulkRequest request) {
        switch (request.action()) {
            case ARCHIVE -> listings.archive(actor, item.id(), item.version(), true);
            case RESTORE -> listings.archive(actor, item.id(), item.version(), false);
            case STATUS, ADD_TAG, REMOVE_TAG -> updateListing(actor, item, request);
        }
    }

    private void updateListing(UUID actor, BulkItem item, BulkRequest request) {
        var listing = listings.get(item.id());
        var status = request.action() == BulkAction.STATUS ? request.status() : listing.status();
        var tags = new LinkedHashSet<>(listing.tags());

        if (request.action() == BulkAction.ADD_TAG) {
            tags.add(request.tag());
        }
        if (request.action() == BulkAction.REMOVE_TAG) {
            tags.remove(request.tag());
        }
        if (tags.size() > 20) {
            throw new InvalidCatalogDataException("Не больше 20 тегов");
        }

        var input = new ListingInput(
                listing.code(),
                listing.title(),
                listing.description(),
                listing.price(),
                status,
                listing.categoryId(),
                listing.featured(),
                tags
        );
        listings.update(actor, item.id(), new UpdateListing(item.version(), input));
    }
}
